package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.helper.requesters.JoinGameRequester;
import com.lukaswillsie.onlinechess.network.threads.JoinGameThread;
import com.lukaswillsie.onlinechess.network.threads.callers.JoinGameCaller;

/**
 * This class interacts with JoinGameThread to process join game requests on behalf of ServerHelper.
 */
public class JoinGameHelper extends SubHelper implements JoinGameCaller {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "JoinGameHelper";
    /**
     * Constants that this class uses to communicate with the UI thread through Messages
     */
    private static final int SERVER_ERROR = -3;
    private static final int SYSTEM_ERROR = -2;
    private static final int CONNECTION_LOST = -1;
    private static final int GAME_JOINED = 0;
    private static final int GAME_DOES_NOT_EXIST = 1;
    private static final int GAME_FULL = 2;
    private static final int USER_ALREADY_IN_GAME = 3;
    private static final int JOIN_GAME_COMPLETE = 4;
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
     * @param gameID    - the ID of the game that we're trying to join
     * @param username  - the username of the user trying to join the given game
     * @throws MultipleRequestException - thrown if another join game request is already being
     *                                  handled when this method is called
     */
    void joinGame(JoinGameRequester requester, String gameID, String username) throws MultipleRequestException {
        if (this.requester != null) {
            throw new MultipleRequestException("Tried to make multiple requests of JoinGameHelper");
        }

        this.requester = requester;

        JoinGameThread thread = new JoinGameThread(this, gameID, username, getOut(), getIn());
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
            case GAME_JOINED:
                requester.gameJoined();

                // We don't reset requester here because there is another callback to come;
                // joinGameComplete() will be called once our request is fully over
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
                requester.userAlreadyInGame();

                // Allows us to accept another request
                this.requester = null;
                break;
            case JOIN_GAME_COMPLETE:
                requester.joinGameComplete((UserGame) msg.obj);

                // Allows us to accept another request
                this.requester = null;
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

    @Override
    public void serverError() {
        this.obtainMessage(SERVER_ERROR).sendToTarget();
    }

    @Override
    public void gameJoined() {
        this.obtainMessage(GAME_JOINED).sendToTarget();
    }

    @Override
    public void gameDoesNotExist() {
        this.obtainMessage(GAME_DOES_NOT_EXIST).sendToTarget();
    }

    @Override
    public void gameFull() {
        this.obtainMessage(GAME_FULL).sendToTarget();
    }

    @Override
    public void userAlreadyInGame() {
        this.obtainMessage(USER_ALREADY_IN_GAME).sendToTarget();
    }

    @Override
    public void joinGameComplete(UserGame game) {
        this.obtainMessage(JOIN_GAME_COMPLETE, game).sendToTarget();
    }
}
