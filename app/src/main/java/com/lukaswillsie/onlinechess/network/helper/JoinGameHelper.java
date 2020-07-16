package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.helper.requesters.JoinGameRequester;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;
import com.lukaswillsie.onlinechess.network.threads.ReturnCodeThread;
import com.lukaswillsie.onlinechess.network.threads.callers.ReturnCodeCaller;

/**
 * This class interacts with JoinGameThread to process join game requests on behalf of ServerHelper.
 */
public class JoinGameHelper extends SubHelper implements ReturnCodeCaller {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "JoinGameHelper";

    /**
     * If we are actively handling a request, this holds the name of the game we are tring to join.
     * Otherwise, is null
     */
    private String gameID;

    /**
     * Keeps a reference to the object that made the currently active request, so we can give them
     * callbacks
     */
    private JoinGameRequester requester;

    /**
     * Create a new SubHelper as part of the given ServerHelper
     *
     * @param container - the ServerHelper that this object is a part of
     */
    JoinGameHelper(ServerHelper container) {
        super(container);
    }

    /**
     * Sends a request to the server asking to join the game with the given gameID. requester will
     * receive callbacks once the request has been responded to by the server.
     *
     * @param requester - will receive callbacks regarding the request
     * @param gameID - the ID of the game that we're trying to join
     * @throws MultipleRequestException - thrown if another join game request is already being
     * handled when this method is called
     */
    void joinGame(JoinGameRequester requester, String gameID) throws MultipleRequestException {
        if(this.requester != null) {
            throw new MultipleRequestException("Tried to make multiple requests of JoinGameHelper");
        }

        this.requester = requester;

        ReturnCodeThread thread = new ReturnCodeThread(getRequest(gameID), this, getOut(), getIn());
        thread.start();
    }

    /**
     * Defines the format of the join game requests we send to the server
     *
     * @param gameID - the ID of the game we want to join
     * @return A String that can be sent to the server as part of a request to join the given game
     */
    private String getRequest(String gameID) {
        return "joingame " + gameID;
    }

    /**
     * Constants that this class uses to communicate with the UI thread through Messages
     */
    private static final int SERVER_ERROR = -3;
    private static final int SYSTEM_ERROR = -2;
    private static final int CONNECTION_LOST = -1;
    private static final int SUCCESS = 0;
    private static final int GAME_DOES_NOT_EXIST = 1;
    private static final int GAME_FULL = 2;
    private static final int USER_ALREADY_IN_GAME = 3;

    /**
     * Called by ReturnCodeThread once the server has responded to our request
     *
     * @param code - the code returned by the server
     */
    @Override
    public void onServerReturn(int code) {
        switch (code) {
            case ReturnCodes.FORMAT_INVALID:
                Log.i(tag, "Server says our request was invalidly formatted. Couldn't join game \"" + gameID + "\"");

                // Because our requests should conform strictly to protocol, we treat this as an
                // error server-side
                this.obtainMessage(SERVER_ERROR).sendToTarget();
                break;
            case ReturnCodes.NO_USER:
                // This should never happen, we should never allow a user who hasn't logged in to
                // make a request like this. But we handle this case as a server error and log the
                // issue for debugging
                Log.i(tag, "Server says we don't have a user logged in. Couldn't join game \"" + gameID + "\"");

                this.obtainMessage(SERVER_ERROR).sendToTarget();
                break;
            case ReturnCodes.SERVER_ERROR:
                Log.i(tag, "Server says it encountered an error. Couldn't join game \"" + gameID + "\"");

                this.obtainMessage(SERVER_ERROR).sendToTarget();
                break;
            case ReturnCodes.JoinGame.SUCCESS:
                Log.i(tag, "Server says we successfully joined game \"" + gameID + "\"");

                this.obtainMessage(SUCCESS).sendToTarget();
                break;
            case ReturnCodes.JoinGame.GAME_DOES_NOT_EXIST:
                Log.i(tag, "Server says game \"" + gameID + "\" doesn't exist.");

                this.obtainMessage(GAME_DOES_NOT_EXIST).sendToTarget();
                break;
            case ReturnCodes.JoinGame.GAME_FULL:
                Log.i(tag, "Server says game \"" + gameID + "\" is already full.");

                this.obtainMessage(GAME_FULL).sendToTarget();
                break;
            case ReturnCodes.JoinGame.USER_ALREADY_IN_GAME:
                Log.i(tag, "Server says the user is already in game \"" + gameID + "\"");

                this.obtainMessage(USER_ALREADY_IN_GAME).sendToTarget();
                break;
        }
    }

    /**
     * Called by ReturnCodeThread if it encounters a system error while processing our request
     */
    @Override
    public void systemError() {
        this.obtainMessage(SYSTEM_ERROR).sendToTarget();
    }

    /**
     * Called by ReturnCodeThread if it discovers our connection to the server has been lost while
     * processing our request
     */
    @Override
    public void connectionLost() {
        this.obtainMessage(CONNECTION_LOST).sendToTarget();
    }

    /**
     * JoinGameHelpers use this method to send Messages to themselves. We do this because
     * handleMessage() always gets run on the UI thread. After a worker thread gets back to us with
     * a result, we need to give a callback to our original caller, but can't just call requester's
     * callback methods directly. First, we need to get off the worker thread and onto the UI
     * thread, because any UI operations that occur in response to our callback have to happen on
     * the UI thread, or we'll get an exception.
     *
     * @param msg - contains information about which callback to give requester
     */
    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case SERVER_ERROR:
                requester.serverError();

                // Allows us to accept another request
                this.requester = null;
                break;
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
            case SUCCESS:
                requester.success();

                // Allows us to accept another request
                this.requester = null;
                break;
            case GAME_DOES_NOT_EXIST:
                requester.gameDoesNotExist();

                // Allows us to accept another request
                this.requester = null;
                break;
            case GAME_FULL:
                requester.gameFull();

                // Allows us to accept another request
                this.requester = null;
                break;
            case USER_ALREADY_IN_GAME:
                requester.userInGame();

                // Allows us to accept another request
                this.requester = null;
                break;
        }
    }
}
