package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.activities.board.PieceType;
import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.helper.requesters.PromotionRequester;
import com.lukaswillsie.onlinechess.network.threads.ReturnCodeThread;
import com.lukaswillsie.onlinechess.network.threads.callers.ReturnCodeCaller;

/**
 * This object handles promotion requests for ServerHelper
 */
public class PromotionHelper extends SubHelper implements ReturnCodeCaller {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "PromotionHelper";
    /**
     * Constants this class uses so that objects can send Messages to themselves
     */
    private static final int SYSTEM_ERROR = -3;
    private static final int CONNECTION_LOST = -2;
    private static final int SERVER_ERROR = -1;
    private static final int SUCCESS = 0;
    private static final int GAME_DOES_NOT_EXIST = 1;
    private static final int USER_NOT_IN_GAME = 2;
    private static final int NO_OPPONENT = 3;
    private static final int GAME_IS_OVER = 4;
    private static final int NOT_USER_TURN = 5;
    private static final int NO_PROMOTION = 6;
    private static final int CHAR_REP_INVALID = 7;
    /**
     * The object that will receive callbacks from us when the request we submit to ReturnCodeThread
     * terminates. null if we are not currently handling a request.
     */
    private PromotionRequester requester;
    /**
     * Create a new SubHelper as part of the given ServerHelper
     *
     * @param container - the ServerHelper that this object is a part of
     */
    PromotionHelper(ServerHelper container) {
        super(container);
    }

    /**
     * Send a promote request to the server.
     *
     * @param requester - will receive callback once the server has responded to the request
     * @param gameID    - the game to issue the promotion in
     * @param piece     - the type of piece that the pawn should be promoted into
     * @throws MultipleRequestException - if this object is already handling a promotion request
     *                                  when this method is called.
     */
    void promote(PromotionRequester requester, String gameID, PieceType.PromotePiece piece) throws MultipleRequestException {
        if (this.requester != null) {
            throw new MultipleRequestException("Submitted multiple promote requests to PromotionHelper");
        }
        this.requester = requester;

        ReturnCodeThread thread = new ReturnCodeThread(getRequest(gameID, piece), this, getOut(), getIn());
        thread.start();
    }

    /**
     * Convert the given gameID and charRep into a promote request.
     *
     * @param gameID - the gameID to attempt to promote in
     * @param piece  - specifies the piece to be promoted into
     * @return A String, a valid promotion request that can be sent to the server
     */
    private String getRequest(String gameID, PieceType.PromotePiece piece) {
        return "promote " + gameID + " " + piece.charRep;
    }

    /**
     * Called by ReturnCodeThread once the server has responded to our request
     *
     * @param code - the code returned by the server
     */
    @Override
    public void onServerReturn(int code) {
        switch (code) {
            case ReturnCodes.NO_USER:
                Log.e(tag, "Server says we don't have a user logged in");

                // Since our app ensures we only ever make requests if we have a user logged in,
                // we treat this as a server error
                this.obtainMessage(SERVER_ERROR).sendToTarget();
                break;
            case ReturnCodes.FORMAT_INVALID:
                Log.e(tag, "Server says our request was invalidly formatted");

                // Since we ensure that we only send validly-formatted request to the server, we
                // treat this as a server error
                this.obtainMessage(SERVER_ERROR).sendToTarget();
                break;
            case ReturnCodes.SERVER_ERROR:
                Log.e(tag, "Server says it encountered an error");

                this.obtainMessage(SERVER_ERROR).sendToTarget();
                break;
            case ReturnCodes.Promote.SUCCESS:
                Log.e(tag, "Server says we successfully promoted a piece");

                this.obtainMessage(SUCCESS).sendToTarget();
                break;
            case ReturnCodes.Promote.GAME_DOES_NOT_EXIST:
                Log.e(tag, "Server says the game we tried to promote in doesn't exist");

                this.obtainMessage(GAME_DOES_NOT_EXIST).sendToTarget();
                break;
            case ReturnCodes.Promote.USER_NOT_IN_GAME:
                Log.e(tag, "Server says our user is not in the game we tried to promote in");

                this.obtainMessage(USER_NOT_IN_GAME).sendToTarget();
                break;
            case ReturnCodes.Promote.NO_OPPONENT:
                Log.e(tag, "Server says the user doesn't have an opponent in the game we " +
                        "tried to promote in");

                this.obtainMessage(NO_OPPONENT).sendToTarget();
                break;
            case ReturnCodes.Promote.GAME_IS_OVER:
                Log.e(tag, "Server says the game we tried to promote in is over");

                this.obtainMessage(GAME_IS_OVER).sendToTarget();
                break;
            case ReturnCodes.Promote.NOT_USER_TURN:
                Log.e(tag, "Server says it isn't our user's turn");

                this.obtainMessage(NOT_USER_TURN).sendToTarget();
                break;
            case ReturnCodes.Promote.NO_PROMOTION:
                Log.e(tag, "Server says there is no promotion to be made, so couldn't " +
                        "promote");

                this.obtainMessage(NO_PROMOTION).sendToTarget();
                break;
            case ReturnCodes.Promote.CHAR_REP_INVALID:
                Log.e(tag, "Server says the charRep we provided is invalid");

                this.obtainMessage(CHAR_REP_INVALID).sendToTarget();
                break;
        }
    }

    /**
     * Called if the request we submitted to ReturnCodeThread is stymied by a system error
     */
    @Override
    public void systemError() {
        this.obtainMessage(SYSTEM_ERROR).sendToTarget();
    }

    /**
     * Called if the request we submitted to ReturnCodeThread is stymied because we lost our
     * connection to the server
     */
    @Override
    public void connectionLost() {
        this.obtainMessage(CONNECTION_LOST).sendToTarget();
    }

    /**
     * Allows us to give callbacks to whoever made a request of us, but run them on the UI thread,
     * instead of our worker thread, because we'd get exceptions if we tried to edit the UI from
     * a thread other than the UI thread.
     *
     * @param msg - contains information about what callback to give to our requester
     */
    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case SYSTEM_ERROR:
                requester.systemError();

                // Allows us to accept another request
                this.requester = null;
                break;
            case CONNECTION_LOST:
                requester.connectionLost();

                // Allows us to accept another request
                this.requester = null;
                break;
            case SERVER_ERROR:
                requester.serverError();

                // Allows us to accept another request
                this.requester = null;
                break;
            case SUCCESS:
                requester.promotionSuccess();

                // Allows us to accept another request
                this.requester = null;
                break;
            case GAME_DOES_NOT_EXIST:
                requester.gameDoesNotExist();

                // Allows us to accept another request
                this.requester = null;
                break;
            case USER_NOT_IN_GAME:
                requester.userNotInGame();

                // Allows us to accept another request
                this.requester = null;
                break;
            case NO_OPPONENT:
                requester.noOpponent();

                // Allows us to accept another request
                this.requester = null;
                break;
            case GAME_IS_OVER:
                requester.gameIsOver();

                // Allows us to accept another request
                this.requester = null;
                break;
            case NOT_USER_TURN:
                requester.notUserTurn();

                // Allows us to accept another request
                this.requester = null;
                break;
            case NO_PROMOTION:
                requester.noPromotionToMake();

                // Allows us to accept another request
                this.requester = null;
                break;
            case CHAR_REP_INVALID:
                requester.charRepInvalid();

                // Allows us to accept another request
                this.requester = null;
                break;
        }
    }
}
