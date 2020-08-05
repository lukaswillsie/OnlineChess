package com.lukaswillsie.onlinechess.network.threads;

import android.util.Log;

import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.threads.callers.LoadGameCaller;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.Scanner;

import Chess.com.lukaswillsie.chess.Board;

public class LoadGameThread extends NetworkThread {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "LoadGameThread";

    /**
     * The gameID that this thread will request data for when this Thread is run
     */
    private String gameID;

    /**
     * The object that will receive callbacks relevant to this Thread's request
     */
    private LoadGameCaller caller;

    /**
     * Creates a new NetworkThread that will use the given devices to read from and write to the
     * server
     *
     * @param writer - the device that this NetworkThread will use to write to the server
     * @param reader - the device that this NetworkThread will use to read from the server
     */
    public LoadGameThread(LoadGameCaller caller, String gameID, PrintWriter writer, DataInputStream reader) {
        super(writer, reader);
        this.caller = caller;
        this.gameID = gameID;
    }

    @Override
    public void run() {
        this.sendRequest(getRequest(gameID));

        int result;
        try {
            result = this.readInt();
        } catch (EOFException e) {
            Log.e(tag, "Server closed the connection");
            caller.connectionLost();
            return;
        } catch (SocketException e) {
            Log.e(tag, "Connection to server has been lost; server may have crashed");
            caller.connectionLost();
            return;
        } catch (IOException e) {
            Log.e(tag, "IOException while reading from server");
            e.printStackTrace();
            caller.systemError();
            return;
        }

        switch (result) {
            case ReturnCodes.NO_USER:
                Log.e(tag, "Server says we have no user logged in. Can't join game \"" + gameID + "\"");

                // We never allow our app to make request of this sort without logging in a user, so
                // we have no way of dealing with this at runtime other than by calling it a server
                // error, after logging it for debugging purposes
                caller.serverError();
                return;
            case ReturnCodes.FORMAT_INVALID:
                Log.e(tag, "Server says our load game request was invalidly formatted. Can't load game \"" + gameID + "\"");

                // We ensure that our requests conform strictly to protocol, so we have no recourse
                // when an error like this surfaces other than to log it and treat it as an error on
                // the server's part
                caller.serverError();
            case ReturnCodes.SERVER_ERROR:
                Log.i(tag, "Server says it encountered an error. Can't load game \"" + gameID + "\"");

                caller.serverError();
                return;
            case ReturnCodes.LoadGame.SUCCESS:
                Log.i(tag, "Server says we can load game \"" + gameID + "\"");
                break;
            case ReturnCodes.LoadGame.GAME_DOES_NOT_EXIST:
                Log.e(tag, "Server says game \"" + gameID + "\" does not exist. Can't load it.");

                caller.gameDoesNotExist();
                return;
            case ReturnCodes.LoadGame.USER_NOT_IN_GAME:
                Log.e(tag, "Server says our user is not a player in game \"" + gameID + "\"");

                caller.userNotInGame();
                return;
        }

        // Now we have to read from the server all the information it sends over about the game.
        // This protocol is defined in the ChessServer repo on my GitHub. Basically, first come
        // 4 integers, each representing a boolean value. Then come 8 lines of text, which together
        // are a picture of the chessboard, with different characters representing different pieces,
        // empty squares, etc. Finally, there's one more integer. We need to read this data in,
        // and feed it into a Board object via the initialize() method and a Scanner object (as if
        // it's coming from a file)
        StringBuilder data = new StringBuilder();

        try {
            for (int i = 0; i < 4; i++) {
                data.append(this.readInt()).append("\n");
            }

            for (int i = 0; i < 8; i++) {
                data.append(this.readLine()).append("\n");
            }

            data.append(this.readInt()).append("\n");
        } catch (EOFException e) {
            Log.e(tag, "Server closed the connection.");
            caller.connectionLost();
            return;
        } catch (SocketException e) {
            Log.e(tag, "Connection with server has been lost. Server may have crashed.");
            caller.connectionLost();
            return;
        } catch (IOException e) {
            Log.e(tag, "IOException while reading from server.");
            e.printStackTrace();
            caller.systemError();
            return;
        }

        System.out.println(data);
        // Wrap a scanner around the data we read from the server so that Board will accept it
        Scanner scanner = new Scanner(data.toString());
        Board board = new Board();

        // Attempt to initialize board with the data given us by the server
        int boardBuilt = board.initialize(scanner);
        if (boardBuilt == 1) {
            Log.e(tag, "Couldn't create a Board object from data sent over by server");
            caller.serverError();
        } else {
            Log.i(tag, "Successfully created a Board object from data sent by server");
            caller.success(board);
        }
    }

    /**
     * Return the String that should be sent to the server as part of a request to load the game
     * with the given ID.
     *
     * @param gameID - the ID of the game to be loaded
     * @return A String that can be sent to the server as part of a request to load the game with
     * the given ID
     */
    private String getRequest(String gameID) {
        return "loadgame " + gameID;
    }
}
