package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.helper.requesters.LoadGameRequester;
import com.lukaswillsie.onlinechess.network.threads.LoadGameThread;
import com.lukaswillsie.onlinechess.network.threads.callers.LoadGameCaller;

import Chess.com.lukaswillsie.chess.Board;

/**
 * This class processes load game requests for ServerHelper.
 */
public class LoadGameHelper extends SubHelper implements LoadGameCaller {
    /*
     * Constants used by this object to send Messages to itself
     */
    private static final int CONNECTION_LOST = -3;
    private static final int SYSTEM_ERROR = -2;
    private static final int SERVER_ERROR = -1;
    private static final int SUCCESS = 0;
    private static final int GAME_DOES_NOT_EXIST = 1;
    private static final int USER_NOT_IN_GAME = 2;
    /**
     * Contains a reference to the object that will receive callbacks relevant to the currently
     * active request. null if there is no active request.
     */
    private LoadGameRequester requester;

    /**
     * We use this to briefly store data given to us by LoadGameThread that we need to give to
     * requester as part of a callback but cannot communicate through Messages. The problem is that
     * a Message can only hold a single Object, while we need to give two (a Board and UserGame
     * object) to requester. So we store the UserGame object here until we've given requester their
     * callback.
     */
    private UserGame game;

    /**
     * Create a new LoadGameHelper as part of the given ServerHelper
     *
     * @param container - the ServerHelper that this object is a part of
     */
    LoadGameHelper(ServerHelper container) {
        super(container);
    }

    /**
     * Initiate a request to load the game with the specified gameID. requester will receive
     * callbacks when the request terminates, either successfully or in error.
     *
     * @param requester - the object that will receive the relevant callback when the request
     *                  terminates
     * @param gameID    - the gameID of the game that should be requested
     * @param username - the username of the user currently logged in to the app
     * @throws MultipleRequestException - if this object is already handling a load game request
     *                                  when this method is called
     */
    void loadGame(LoadGameRequester requester, String gameID, String username) throws MultipleRequestException {
        if (this.requester != null) {
            throw new MultipleRequestException("Submitted multiple requests to LoadGameHelper");
        }
        this.requester = requester;

        LoadGameThread thread = new LoadGameThread(this, gameID, username, getOut(), getIn());
        thread.start();
    }

    /**
     * Called by LoadGameThread if a server error occurs during a request given to LoadGameThread
     */
    @Override
    public void serverError() {
        this.obtainMessage(SERVER_ERROR).sendToTarget();
    }

    /**
     * Is called by LoadGameThread if a request we issued is interrupted by an error originating in
     * the system, rather than the server.
     */
    @Override
    public void systemError() {
        this.obtainMessage(SYSTEM_ERROR).sendToTarget();
    }

    /**
     * To be called if LoadGameThread discovers the server to have disconnected in the course of
     * its work.
     */
    @Override
    public void connectionLost() {
        this.obtainMessage(CONNECTION_LOST).sendToTarget();
    }

    /**
     * Called by LoadGameThread after a load game request finishes successfully. The given Board
     * object will represent the requested game, and will have been initialized successfully from
     * the data sent over by the server.
     *
     * @param board - a Board object successfully initialized to contain the state of the board in
     *              the given game
     * @param game - a UserGame object initialized to contain all the high-level information about
     *             the game that was requested
     */
    @Override
    public void success(Board board, UserGame game) {
        this.game = game;
        this.obtainMessage(SUCCESS, board).sendToTarget();
    }

    /**
     * Called by LoadGameThread if the server responds to the request by saying the supplied gameID
     * is not associated with a game in its system
     */
    @Override
    public void gameDoesNotExist() {
        this.obtainMessage(GAME_DOES_NOT_EXIST).sendToTarget();
    }

    /**
     * Called by LoadGameThread if the server responds to the request by saying that the user the
     * app currently has logged in is not a player in the game whose data was requested
     */
    @Override
    public void userNotInGame() {
        this.obtainMessage(USER_NOT_IN_GAME).sendToTarget();
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
                this.requester.connectionLost();

                // Allows us to accept another request
                this.requester = null;
                break;
            case SYSTEM_ERROR:
                this.requester.systemError();

                // Allows us to accept another request
                this.requester = null;
                break;
            case SERVER_ERROR:
                this.requester.serverError();

                // Allows us to accept another request
                this.requester = null;
                break;
            case SUCCESS:
                this.requester.success((Board) msg.obj, game);

                // Allows us to accept another request
                this.requester = null;
                this.game = game;
                break;
            case GAME_DOES_NOT_EXIST:
                this.requester.gameDoesNotExist();

                // Allows us to accept another request
                this.requester = null;
                break;
            case USER_NOT_IN_GAME:
                this.requester.userNotInGame();

                // Allows us to accept another request
                this.requester = null;
                break;
        }
    }
}
