package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.data.ServerData;
import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.helper.requesters.CreateGameRequester;
import com.lukaswillsie.onlinechess.network.threads.ReturnCodeThread;
import com.lukaswillsie.onlinechess.network.threads.callers.ReturnCodeCaller;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles requests to create games for the user on behalf of ServerHelper.
 */
public class CreateGameHelper extends SubHelper implements ReturnCodeCaller {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "CreateGameHelper";
    /**
     * Constants this object uses to communicate with the UI thread through Messages
     */
    private static final int SERVER_ERROR = -3;
    private static final int SYSTEM_ERROR = -2;
    private static final int CONNECTION_LOST = -1;
    private static final int SUCCESS = 0;
    private static final int GAMEID_IN_USE = 1;
    private static final int FORMAT_INVALID = 2;
    /**
     * Keeps a reference to the object that will receive callbacks about the currently active
     * request. null if there is no active request.
     */
    private CreateGameRequester requester;
    /**
     * The ID of the game that we are attempting to join in the currently active request. null
     * if there is no active request.
     */
    private String gameID;
    /**
     * The username of the user responsible for the currently active request (trying to create the
     * game). null if there is no active request.
     */
    private String username;

    /**
     * Create a new SubHelper as part of the given ServerHelper
     *
     * @param container - the ServerHelper that this object is a part of
     */
    CreateGameHelper(ServerHelper container) {
        super(container);
    }

    /**
     * Will send a request to the server to try and create a game with the given ID and open status
     *
     * @param requester - the object that will receive callbacks relevant to the request
     * @param gameID    - the ID of the game to be created
     * @param open      - a boolean representing whether or not the game to be created should be "open",
     *                  meaning that anybody can view and join it
     * @param username  - the username of the user trying to create the game
     * @throws MultipleRequestException - if this object is already handling another createGame
     *                                  request when this method is called
     */
    void createGame(CreateGameRequester requester, String gameID, boolean open, String username) throws MultipleRequestException {
        if (this.requester != null) {
            throw new MultipleRequestException("Tried to make multiple requests of CreateGameHelper");
        }
        this.requester = requester;
        this.gameID = gameID;
        this.username = username;

        ReturnCodeThread thread = new ReturnCodeThread(getRequest(gameID, open), this, getOut(), getIn());
        thread.start();
    }

    /**
     * Return a request String that can be sent to the server to attempt to create a game with the
     * given gameID and open status.
     *
     * @param gameID - the ID of the game to be created
     * @param open   - whether or not the game to be created is "open", meaning anyone can view and
     *               join the game
     * @return A String that can be sent to the server as part of an attempt to create a game with
     * the specified ID and open status
     */
    private String getRequest(String gameID, boolean open) {
        return "creategame " + gameID + " " + ((open) ? "1" : "0");
    }

    /**
     * Called once ReturnCodeThread hears back from the server
     *
     * @param code - the code returned by the server
     */
    @Override
    public void onServerReturn(int code) {
        Message msg;
        switch (code) {
            case ReturnCodes.NO_USER:
                Log.e(tag, "Server says we don't have a user logged in");


                // The way our app is structured, we only let the user issue requests after they've
                // logged in, so this should never happen. At runtime, we call it a server error,
                // after logging the problem for future examination.
                msg = this.obtainMessage(SERVER_ERROR);
                break;
            case ReturnCodes.FORMAT_INVALID:
                Log.e(tag, "Server says our request was invalidly formatted");

                // Since we ensure our commands are formatted exactly as protocol demands, we treat
                // this as a server error at runtime, after logging the problem for debugging
                msg = this.obtainMessage(SERVER_ERROR);
                break;
            case ReturnCodes.SERVER_ERROR:
                Log.e(tag, "Server says it encountered an error");

                msg = this.obtainMessage(SERVER_ERROR);
                break;
            case ReturnCodes.CreateGame.SUCCESS:
                Log.i(tag, "Server says game \"" + gameID + "\" successfully created");

                UserGame game = new UserGame(username);
                List<Object> initialData = new ArrayList<>();
                for (ServerData data : ServerData.order) {
                    if (data == ServerData.WHITE) {
                        initialData.add(username);
                    } else if (data == ServerData.GAMEID) {
                        initialData.add(gameID);
                    } else {
                        initialData.add(data.initial);
                    }
                }

                int result = game.initialize(initialData);
                if (result == 1) {
                    Log.e(tag, "Error initializing a UserGame with the data sent by server");
                    msg = this.obtainMessage(SERVER_ERROR);
                } else {
                    msg = this.obtainMessage(SUCCESS, game);
                }
                break;
            case ReturnCodes.CreateGame.GAMEID_IN_USE:
                Log.i(tag, "Server says gameID \"" + gameID + "\" is in use");

                msg = this.obtainMessage(GAMEID_IN_USE);
                break;
            case ReturnCodes.CreateGame.FORMAT_INVALID:
                Log.i(tag, "Server says gameID \"" + gameID + "\" is invalid");

                msg = this.obtainMessage(FORMAT_INVALID);
                break;
            default:
                Log.e(tag, "Server returned code " + code + ", which is outside protocol");

                // If the server returned a code that doesn't conform to protocol, we consider this
                // an error on the server's part
                msg = this.obtainMessage(SERVER_ERROR);
                break;
        }

        // The request is over so we reset these fields
        this.gameID = null;
        this.username = null;

        msg.sendToTarget();
    }

    /**
     * Called by ReturnCodeThread if it encounters a system error while handling our request
     */
    @Override
    public void systemError() {
        this.obtainMessage(SYSTEM_ERROR).sendToTarget();
    }

    /**
     * Called by ReturnCodeThread if it discovers that our connection to the server has been lost
     * while handling our request
     */
    @Override
    public void connectionLost() {
        this.obtainMessage(CONNECTION_LOST).sendToTarget();
    }

    /**
     * We use this method to give callbacks back to requester. This ensures that any UI operations
     * performed by requester take place on the UI thread, rather than on the worker thread we've
     * spawned, preventing an exception from being thrown.
     *
     * @param msg - contains information about which callback to give to requester, along with any
     *            data that needs to go along with that callback
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
                requester.gameCreated((UserGame) msg.obj);

                // Allows us to accept another request
                this.requester = null;
                break;
            case GAMEID_IN_USE:
                requester.gameIDInUse();

                // Allows us to accept another request
                this.requester = null;
                break;
            case FORMAT_INVALID:
                requester.invalidFormat();

                // Allows us to accept another request
                this.requester = null;
                break;
        }
    }
}
