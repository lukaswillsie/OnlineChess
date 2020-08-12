package com.lukaswillsie.onlinechess.network.threads;

import android.util.Log;

import com.lukaswillsie.onlinechess.data.ServerData;
import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.threads.callers.JoinGameCaller;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class JoinGameThread extends NetworkThread {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "JoinGameThread";

    /**
     * A reference to the object that should receive callbacks relevant to the request we are
     * processing
     */
    private JoinGameCaller caller;

    /**
     * The ID of the game that we are going to try and join
     */
    private String gameID;

    /**
     * The username of the user trying to join the game
     */
    private String username;


    /**
     * Creates a new NetworkThread that will use the given devices to read from and write to the
     * server
     *
     * @param writer - the device that this NetworkThread will use to write to the server
     * @param reader - the device that this NetworkThread will use to read from the server
     */
    public JoinGameThread(JoinGameCaller caller, String gameID, String username, PrintWriter writer, DataInputStream reader) {
        super(writer, reader);
        this.caller = caller;
        this.gameID = gameID;
        this.username = username;
    }

    @Override
    public void run() {
        // First we try and join the game
        this.sendRequest(getJoinRequest(gameID));

        int response;
        try {
            response = this.readInt();
        } catch (EOFException e) {
            Log.i(tag, "Connection to server has been lost on server's end");
            caller.connectionLost();
            return;
        } catch (SocketException e) {
            Log.i(tag, "Connection to server has been lost on server's end");
            caller.connectionLost();
            return;
        } catch (IOException e) {
            Log.i(tag, "There was an IOException reading from socket");
            caller.systemError();
            return;
        }

        // Interpret the server's return code
        switch (response) {
            case ReturnCodes.NO_USER:
                Log.i(tag, "Server says we haven't logged in a user");

                // This shouldn't happen; the app should never make a join game request without
                // first logging in a user. So we treat this as a server error after logging the
                // specific nature of the problem.
                caller.serverError();
                return;
            case ReturnCodes.FORMAT_INVALID:
                Log.i(tag, "Server says our command was invalidly formatted");

                // Because our commands are formatted to exactly conform with protocol, we treat
                // this as an error server-side
                caller.serverError();
                return;
            case ReturnCodes.SERVER_ERROR:
                Log.i(tag, "Server says it encountered an error");

                caller.serverError();
                return;
            case ReturnCodes.JoinGame.SUCCESS:
                Log.i(tag, "Server says we successfully joined game \"" + gameID + "\"");

                caller.gameJoined();
                break;
            case ReturnCodes.JoinGame.GAME_DOES_NOT_EXIST:
                Log.i(tag, "Server says game \"" + gameID + "\" does not exist");

                caller.gameDoesNotExist();
                return;
            case ReturnCodes.JoinGame.GAME_FULL:
                Log.i(tag, "Server says game \"" + gameID + "\" is full");

                caller.gameFull();
                return;
            case ReturnCodes.JoinGame.USER_ALREADY_IN_GAME:
                Log.i(tag, "Server says user is already in game \"" + gameID + "\"");

                caller.userAlreadyInGame();
                return;
            default:
                Log.i(tag, "Server returned \"" + response + "\", which is outside of join game protocol");

                // We treat this as an error server side, because the server isn't conforming to
                // protocol
                caller.serverError();
                return;
        }

        // Now we want to fetch the game's data, so that we have a local record of it
        this.sendRequest(getGameDataRequest(gameID));

        UserGame game = new UserGame(username);
        List<Object> serverData = new ArrayList<>();

        try {
            for (ServerData dataType : ServerData.order) {
                if (dataType.type == 'i') {
                    serverData.add(this.readInt());
                } else if (dataType.type == 's') {
                    serverData.add(this.readLine());
                }
            }
        } catch (EOFException e) {
            Log.i(tag, "EOFException reading from server. Server has closed the connection");

            caller.connectionLost();
            return;
        } catch (SocketException e) {
            Log.i(tag, "SocketException reading from server. Server may have crashed.");

            caller.connectionLost();
            return;
        } catch (IOException e) {
            Log.i(tag, "IOException reading from server. There may have been a system error");
            e.printStackTrace();

            caller.systemError();
            return;
        }

        response = game.initialize(serverData);
        // If the game couldn't be initialized from the data we received from the server
        if (response == 1) {
            caller.serverError();
            return;
        }

        caller.joinGameComplete(game);
    }

    private String getJoinRequest(String gameID) {
        return "joingame " + gameID;
    }

    private String getGameDataRequest(String gameID) {
        return "getgamedata " + gameID;
    }
}
