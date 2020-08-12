package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.helper.requesters.DrawRequester;
import com.lukaswillsie.onlinechess.network.threads.ReturnCodeThread;
import com.lukaswillsie.onlinechess.network.threads.callers.ReturnCodeCaller;

/**
 * Handles draw requests for ServerHelper objects
 */
public class DrawHelper extends SubHelper implements ReturnCodeCaller {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "DrawHelper";
    /**
     * Set each time a new request is submitted; will receive callbacks relating to that request
     */
    private DrawRequester requester;

    /**
     * The ID of the game that we are currently submitting a draw request for
     */
    private String gameID;

    /**
     * Create a new DrawHelper as part of the given ServerHelper
     *
     * @param container - the ServerHelper that this object is a part of
     */
    DrawHelper(ServerHelper container) {
        super(container);
    }

    /**
     * Submit a draw request to the server.
     *
     * Note: the server condenses both the OFFERING of draws and the ACCEPTING of draw offers into
     * one command. So what this request means depends on whether or not there is an active draw
     * offer from the opponent in the specified game.
     *
     * @param requester - will receive a callback once the server has responded to the request
     * @param gameID - the game in which to offer/accept a draw
     * @throws MultipleRequestException - if this object is already handling a draw request when
     * this method is called
     */
    void draw(DrawRequester requester, String gameID) throws MultipleRequestException {
        if(this.requester != null) {
            throw new MultipleRequestException("Tried to submit multiple draw requests to ServerHelper");
        }

        this.requester = requester;
        this.gameID = gameID;

        ReturnCodeThread thread = new ReturnCodeThread(getRequest(gameID), this, getOut(), getIn());
        thread.start();
    }

    /**
     * Return a request String that can be sent to the server to request a draw offer/acceptance in
     * the given game.
     *
     * @param gameID - the game about which to make the request
     * @return A draw request String for the given gameID
     */
    private String getRequest(String gameID) {
        return "draw " + gameID;
    }

    /**
     * Called by ReturnCodeThread after it has received a response from the server.
     *
     * @param code - the code returned by the server
     */
    @Override
    public void onServerReturn(int code) {
        switch (code) {
            case ReturnCodes.NO_USER:
                Log.e(tag, "Server says we haven't logged in a user");

                // Treat this as a server error, because we never make any requests of the server
                // that we need a user to be logged in for unless we've logged in a user
                this.obtainMessage(SERVER_ERROR).sendToTarget();
                break;
            case ReturnCodes.FORMAT_INVALID:
                Log.e(tag, "Server says we formatted our request incorrectly");

                // Treat this as a server error because we always ensure that our commands conform
                // to protocol
                this.obtainMessage(SERVER_ERROR).sendToTarget();
                break;
            case ReturnCodes.SERVER_ERROR:
                Log.e(tag, "Server says it encountered an error");

                this.obtainMessage(SERVER_ERROR).sendToTarget();
                break;
            case ReturnCodes.Draw.SUCCESS:
                Log.e(tag, "Server says draw offer/acceptance in game \"" + gameID + "\" was a success");

                this.obtainMessage(SUCCESS).sendToTarget();
                break;
            case ReturnCodes.Draw.GAME_DOES_NOT_EXIST:
                Log.e(tag, "Server says game \"" + gameID + "\" does not exist");

                this.obtainMessage(GAME_DOES_NOT_EXIST).sendToTarget();
                break;
            case ReturnCodes.Draw.USER_NOT_IN_GAME:
                Log.e(tag, "Server says user is not in game \"" + gameID + "\"");

                this.obtainMessage(USER_NOT_IN_GAME).sendToTarget();
                break;
            case ReturnCodes.Draw.NO_OPPONENT:
                Log.e(tag, "Server says user has no opponent in game \"" + gameID + "\"");

                this.obtainMessage(NO_OPPONENT).sendToTarget();
                break;
            case ReturnCodes.Draw.GAME_IS_OVER:
                Log.e(tag, "Server says game \"" + gameID + "\" is already over");

                this.obtainMessage(GAME_IS_OVER).sendToTarget();
                break;
            case ReturnCodes.Draw.NOT_USER_TURN:
                Log.e(tag, "Server says it is not the user's turn in game \"" + gameID + "\"");

                this.obtainMessage(NOT_USER_TURN).sendToTarget();
                break;
            default:
                Log.e(tag, "Server returned \"" + code + "\", which is outside of protocol");

                this.obtainMessage(SERVER_ERROR).sendToTarget();
                break;
        }
    }

    /**
     * Called if ReturnCodeThread can't process our request because of a system error
     */
    @Override
    public void systemError() {
        this.obtainMessage(SYSTEM_ERROR).sendToTarget();
    }

    /**
     * Called if ReturnCodeThread discovers the connection to the server to have been lost before it
     * can process our request
     */
    @Override
    public void connectionLost() {
        this.obtainMessage(CONNECTION_LOST).sendToTarget();
    }

    /**
     * Constants that this object uses to send Messages to itself
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

    /**
     * We use this method so that we can give callbacks on the UI thread (as opposed to our worker
     * thread). This ensures that any UI events that have to occur in response to our callback will
     * run on the UI thread. Otherwise, we'd get an exception.
     *
     * @param msg - contains information about the callback to be given
     */
    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case SYSTEM_ERROR:
                requester.systemError();

                // Allows us to accept another request
                this.requester = null;
                break;
            case SERVER_ERROR:
                requester.serverError();

                // Allows us to accept another request
                this.requester = null;
                break;
            case CONNECTION_LOST:
                requester.connectionLost();

                // Allows us to accept another request
                this.requester = null;
                break;
            case SUCCESS:
                requester.drawSuccess();

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
        }
    }
}