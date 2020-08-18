package com.lukaswillsie.onlinechess.network.threads;

import android.util.Log;

import com.lukaswillsie.onlinechess.data.ServerData;
import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.threads.callers.GameDataCaller;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * This object handles the nitty-gritty details of submitting requests for game data to the server
 * and parsing the server's response
 */
public class GameDataThread extends NetworkThread {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "GameDataThread";

    /**
     * Will receive a callback from this Thread once the server has responded to our request
     */
    private GameDataCaller caller;

    /**
     * The ID of the game whose data we are to request
     */
    private String gameID;

    /**
     * The username of the user currently logged in to the app, whose game data we are fetching
     */
    private String username;

    /**
     * Creates a new GameDataThread that will, when started, submit a request to the server asking
     * for the given game's data
     *
     * @param gameID   - the game whose data we are to request
     * @param username - the username of the user on whose behalf we are requesting the data. Will
     *                 be used to parse the information returned by the server into a UserGame
     *                 object
     * @param caller   - will receive a callback once the request has been processed by the server,
     *                 either successfully or unsuccessfully
     * @param writer   - the device that this NetworkThread will use to write to the server
     * @param reader   - the device that this NetworkThread will use to read from the server
     */
    public GameDataThread(String gameID, String username, GameDataCaller caller, PrintWriter writer, DataInputStream reader) {
        super(writer, reader);
    }

    @Override
    public void run() {
        sendRequest();

        try {
            // The server first tells us whether or not it has accepted our request
            int code = readInt();
            switch (code) {
                case ReturnCodes.NO_USER:
                    Log.e(tag, "Server says we haven't logged in a user");

                    // Treat this as a server error, because we never make this request unless we've
                    // already logged in a user
                    caller.serverError();
                    return;
                case ReturnCodes.FORMAT_INVALID:
                    Log.e(tag, "Server says our command was invalidly formatted");

                    // Treat this as a server error, because we always ensure our commands match
                    // protocol
                    caller.serverError();
                    return;
                case ReturnCodes.SERVER_ERROR:
                    Log.e(tag, "Server says it encountered an error");

                    caller.serverError();
                    return;
                case ReturnCodes.LoadGames.SUCCESS:
                    Log.i(tag, "Server says we can expect to receive all the user's games");
                    break;
                case ReturnCodes.GetGameData.GAME_DOES_NOT_EXIST:
                    Log.e(tag, "Server says game \"" + gameID + "\" does not exist");

                    // Treat this as a server error because we only make requests if we believe them
                    // to be valid, according to data the server has itself given to us
                    caller.serverError();
                    return;
                case ReturnCodes.GetGameData.USER_NOT_IN_GAME:
                    Log.e(tag, "Server says user is not in game \"" + gameID + "\"");

                    // Treat this as a server error because we only make requests if we believe them
                    // to be valid, according to data the server has itself given to us
                    caller.serverError();
                    return;
                // Any other return code does not conform to protocol
                default:
                    Log.i(tag, "Server returned \"" + code + "\", which is outside of protocol");
                    caller.serverError();
                    return;
            }

            List<Object> serverData = new ArrayList<>();
            UserGame game;
            String line;
            for (ServerData data : ServerData.order) {
                if (data.type == 's') {
                    // Note that we're in a try-catch, so we assume that the line returned here
                    // is valid and complete
                    line = this.readLine();
                    serverData.add(line);
                } else if (data.type == 'i') {
                    code = this.readInt();
                    serverData.add(code);
                }
            }

            // Convert the data from the server into a UserGame object
            game = new UserGame(username);
            if (game.initialize(serverData) == 1) {
                Log.e(tag, "A game couldn't be initialized from data sent by server");
                caller.serverError();
                return;
            }

            // Pass the created UserGame to the caller
            caller.success(game);
        } catch (EOFException e) {
            // This means the server has closed their end of the connection. If the server has
            // disconnected, we can't do anything until a new connection has been made. So notify
            // the caller of the problem, and then exit this thread.
            Log.e(tag, "Server has disconnected.");
            caller.connectionLost();
        } catch (SocketException e) {
            // This means some problem occurred with the connection. The server may have crashed,
            // for example. If the server has disconnected, we can't proceed until a new connection
            // has been made. So notify the caller of the problem, and then exit this thread.
            Log.e(tag, "Server has disconnected.");
            caller.connectionLost();
        } catch (IOException e) {
            // This means some OTHER problem has occurred, probably within the system. So, again, we
            // notify the caller before exiting the thread.
            Log.e(tag, "IOException while reading from server");
            e.printStackTrace();
            caller.systemError();
        }
    }

    /**
     * Send a command to the server requesting game data for the game we were given at creation
     */
    private void sendRequest() {
        super.sendRequest("getgamedata " + gameID);
    }
}
