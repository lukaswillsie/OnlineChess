package com.lukaswillsie.onlinechess.network.helper;

import android.annotation.SuppressLint;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.activities.board.Move;
import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.helper.requesters.MoveRequester;
import com.lukaswillsie.onlinechess.network.threads.ReturnCodeThread;
import com.lukaswillsie.onlinechess.network.threads.callers.ReturnCodeCaller;

import Chess.com.lukaswillsie.chess.Pair;

/**
 * This object handles move requests for ServerHelper objects.
 */
public class MoveHelper extends SubHelper implements ReturnCodeCaller {
    /**
     * Constants that this object uses to send Messages to itself
     */
    public static final int CONNECTION_LOST = -3;
    public static final int SYSTEM_ERROR = -2;
    public static final int SERVER_ERROR = -1;
    public static final int SUCCESS = 0;
    public static final int SUCCESS_PROMOTION_NEEDED = 1;
    public static final int GAME_DOES_NOT_EXIST = 2;
    public static final int USER_NOT_IN_GAME = 3;
    public static final int NO_OPPONENT = 4;
    public static final int GAME_IS_OVER = 5;
    public static final int NOT_USER_TURN = 6;
    public static final int HAS_TO_PROMOTE = 7;
    public static final int RESPOND_TO_DRAW = 8;
    public static final int MOVE_INVALID = 9;
    /**
     * Tag for logging to the console
     */
    private static final String tag = "MoveHelper";
    /**
     * The object that will receive callbacks relevant to the currently active request; null if
     * there is no currently active request
     */
    private MoveRequester requester;
    /**
     * Create a new SubHelper as part of the given ServerHelper
     *
     * @param container - the ServerHelper that this object is a part of
     */
    MoveHelper(ServerHelper container) {
        super(container);
    }

    /**
     * Sends a move request to the server, trying to make the given move in the given game
     *
     * @param requester - will receive callbacks once the request has been handled
     * @param gameID    - the game to try and make the move in
     * @param move      - represents the move that this object will try and make with its request
     * @throws MultipleRequestException - if this object is already handling a request when this
     *                                  method is called
     */
    void move(MoveRequester requester, String gameID, Move move) throws MultipleRequestException {
        if (this.requester != null) {
            throw new MultipleRequestException("Tried to make multiple move requests of MoveRequester");
        }
        this.requester = requester;

        ReturnCodeThread thread = new ReturnCodeThread(getRequest(gameID, move), this, getOut(), getIn());
        thread.start();
    }

    /**
     * Given a gameID and Move object, constructs a String that can be sent to the server as part of
     * a request to make the specified move in the specified game
     *
     * @param gameID - the game to make the move in
     * @param move   - the Move to be made
     * @return A String that can be sent to the server as part of a move request
     */
    @SuppressLint("DefaultLocale")
    private String getRequest(String gameID, Move move) {
        Pair src = move.src;
        Pair dest = move.dest;
        return String.format("move %s %d,%d->%d,%d", gameID, src.first(), src.second(), dest.first(), dest.second());
    }

    /**
     * Called by ReturnCodeThread after it has sent our request to the server and received a return
     * code in response
     *
     * @param code - the code returned by the server
     */
    @Override
    public void onServerReturn(int code) {
        Message msg = this.obtainMessage();
        switch (code) {
            case ReturnCodes.NO_USER:
                Log.i(tag, "Server says we don't have a user logged in");
                msg.what = SERVER_ERROR;
                break;
            case ReturnCodes.FORMAT_INVALID:
                Log.i(tag, "Server says our command was formatted incorrectly");
                msg.what = SERVER_ERROR;
                break;
            case ReturnCodes.SERVER_ERROR:
                Log.i(tag, "Server says it encountered an error");
                msg.what = SERVER_ERROR;
                break;
            case ReturnCodes.Move.SUCCESS:
                Log.i(tag, "Server says move was successfully made");
                msg.what = SUCCESS;
                break;
            case ReturnCodes.Move.SUCCESS_PROMOTION_NEEDED:
                Log.i(tag, "Server says move was successfully made, promotion now needed");
                msg.what = SUCCESS_PROMOTION_NEEDED;
                break;
            case ReturnCodes.Move.GAME_DOES_NOT_EXIST:
                Log.i(tag, "Server says game we tried to move in does not exist");
                msg.what = GAME_DOES_NOT_EXIST;
                break;
            case ReturnCodes.Move.USER_NOT_IN_GAME:
                Log.i(tag, "Server says the user is not in the game we tried to move in");
                msg.what = USER_NOT_IN_GAME;
                break;
            case ReturnCodes.Move.NO_OPPONENT:
                Log.i(tag, "Server says the user has no opponent in the game we tried to " +
                        "move in");
                msg.what = NO_OPPONENT;
                break;
            case ReturnCodes.Move.GAME_IS_OVER:
                Log.i(tag, "Server says the game we tried to move in is over");
                msg.what = GAME_IS_OVER;
                break;
            case ReturnCodes.Move.NOT_USER_TURN:
                Log.i(tag, "Server says it is not the user's turn in the game we tried to " +
                        "move in");
                msg.what = NOT_USER_TURN;
                break;
            case ReturnCodes.Move.HAS_TO_PROMOTE:
                Log.i(tag, "Server says we have to promote, not make a normal move");
                msg.what = HAS_TO_PROMOTE;
                break;
            case ReturnCodes.Move.RESPOND_TO_DRAW:
                Log.i(tag, "Server says we have to respond to a draw offer, not make a " +
                        "normal move");
                msg.what = RESPOND_TO_DRAW;
                break;
            case ReturnCodes.Move.MOVE_INVALID:
                Log.i(tag, "Server says our move was invalid");
                msg.what = MOVE_INVALID;
                break;
            default:
                Log.i(tag, "Server returned " + code + ", which is outside of defined " +
                        "protocols");
                msg.what = SERVER_ERROR;
                break;
        }

        msg.sendToTarget();
    }

    /**
     * Called if our ReturnCodeThread encounters a system error in the course of its work
     */
    @Override
    public void systemError() {
        this.obtainMessage(SYSTEM_ERROR).sendToTarget();
    }

    /**
     * Called if our ReturnCodeThread discovers in the course of its work that the connection to the
     * server has been lost
     */
    @Override
    public void connectionLost() {
        this.obtainMessage(CONNECTION_LOST).sendToTarget();
    }

    /**
     * We use this method to give callbacks to our requester. We use Messages instead of calling
     * requester's method's directly because using Messages ensures that our callbacks run on the
     * UI thread. This prevents an exception in the event that a response to our callback requires
     * an update to the UI.
     *
     * @param msg - contains information about what callback should be called
     */
    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case CONNECTION_LOST:
                requester.connectionLost();

                // Allows us to handle another request
                this.requester = null;
                break;
            case SYSTEM_ERROR:
                requester.systemError();

                // Allows us to handle another request
                this.requester = null;
                break;
            case SERVER_ERROR:
                requester.serverError();

                // Allows us to handle another request
                this.requester = null;
                break;
            case SUCCESS:
                requester.moveSuccess(false);

                // Allows us to handle another request
                this.requester = null;
                break;
            case SUCCESS_PROMOTION_NEEDED:
                requester.moveSuccess(true);

                // Allows us to handle another request
                this.requester = null;
                break;
            case GAME_DOES_NOT_EXIST:
                requester.gameDoesNotExist();

                // Allows us to handle another request
                this.requester = null;
                break;
            case USER_NOT_IN_GAME:
                requester.userNotInGame();

                // Allows us to handle another request
                this.requester = null;
                break;
            case NO_OPPONENT:
                requester.noOpponent();

                // Allows us to handle another request
                this.requester = null;
                break;
            case GAME_IS_OVER:
                requester.gameIsOver();

                // Allows us to handle another request
                this.requester = null;
                break;
            case NOT_USER_TURN:
                requester.notUserTurn();

                // Allows us to handle another request
                this.requester = null;
                break;
            case HAS_TO_PROMOTE:
                requester.needToPromote();

                // Allows us to handle another request
                this.requester = null;
                break;
            case RESPOND_TO_DRAW:
                requester.mustRespondToDraw();

                // Allows us to handle another request
                this.requester = null;
                break;
            case MOVE_INVALID:
                requester.moveInvalid();

                // Allows us to handle another request
                this.requester = null;
                break;
        }
    }
}
