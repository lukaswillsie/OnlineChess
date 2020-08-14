package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.helper.requesters.GameDataRequester;
import com.lukaswillsie.onlinechess.network.threads.GameDataThread;
import com.lukaswillsie.onlinechess.network.threads.callers.GameDataCaller;

public class GameDataHelper extends SubHelper implements GameDataCaller {
    /**
     * The object that will receive callbacks related to this object's currently active request.
     * null if this object is not currently handling a request.
     */
    private GameDataRequester requester;

    /**
     * Create a new SubHelper as part of the given ServerHelper
     *
     * @param container - the ServerHelper that this object is a part of
     */
    GameDataHelper(ServerHelper container) {
        super(container);
    }

    /**
     * Submit a request to the server, asking for the given game's game data.
     *
     * @param requester - will receive a callback once the game data request has terminated, either
     *                  successfully or unsuccessfully
     * @param gameID - the game whose data we are to request
     * @param username - the username of the user who we currently have logged in to the app
     *
     * @throws MultipleRequestException - if this object is already handling a game data request at
     * the time that this method is called
     */
    void getGameData(GameDataRequester requester, String gameID, String username) throws MultipleRequestException {
        if(this.requester != null) {
            throw new MultipleRequestException("Tried to submit multiple game data requests to ServerHelper");
        }
        this.requester = requester;

        GameDataThread thread = new GameDataThread(gameID, username, this, getOut(), getIn());
        thread.start();
    }

    /**
     * Called if the game data request we submit to GameDataThread fails due to an error server-side
     */
    @Override
    public void serverError() {
        this.obtainMessage(SERVER_ERROR).sendToTarget();
    }

    /**
     * Called if the game data request we submit to GameDataThread succeeds
     *
     * @param game - a UserGame object containing all the game data sent over by the server
     */
    @Override
    public void success(UserGame game) {
        this.obtainMessage(SUCCESS, game).sendToTarget();
    }

    /**
     * Called if the game data request we submit to GameDataThread fails because the server says the
     * game whose data we requested doesn't exist
     */
    @Override
    public void gameDoesNotExist() {
        this.obtainMessage(GAME_DOES_NOT_EXIST).sendToTarget();
    }

    /**
     * Called if the game data request we submit to GameDataThread fails because the server says
     * our logged-in user is not a player in the game whose data we requested
     */
    @Override
    public void userNotInGame() {
        this.obtainMessage(USER_NOT_IN_GAME).sendToTarget();
    }

    /**
     * Called if the game data request we submit to GameDataThread fails due to an error with the
     * system
     */
    @Override
    public void systemError() {
        this.obtainMessage(SYSTEM_ERROR).sendToTarget();
    }

    /**
     * Called if the game data request we submit to GameDataThread fails due to a loss of connection
     * with the server
     */
    @Override
    public void connectionLost() {
        this.obtainMessage(CONNECTION_LOST).sendToTarget();
    }

    /**
     * Constants used by this object to send Messages to itself
     */
    private static final int CONNECTION_LOST = -3;
    private static final int SYSTEM_ERROR = -2;
    private static final int SERVER_ERROR = -1;
    private static final int SUCCESS = 0;
    private static final int GAME_DOES_NOT_EXIST = 1;
    private static final int USER_NOT_IN_GAME = 2;

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
            case CONNECTION_LOST:
                requester.connectionLost();

                // Allows us to accept another request
                this.requester = null;
                break;
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
            case SUCCESS:
                requester.success((UserGame) msg.obj);

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
        }
    }
}
