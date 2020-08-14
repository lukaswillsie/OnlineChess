package com.lukaswillsie.onlinechess.network.threads;

import android.util.Log;

import com.lukaswillsie.onlinechess.data.ServerData;
import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.threads.callers.LoadGameCaller;
import com.lukaswillsie.onlinechess.network.threads.callers.LoadGamesCaller;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class LoadGamesThread extends NetworkThread {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "LoadGamesThread";

    /**
     * Will receive callbacks from this Thread on success or failure of the request being submitted
     * and handled by this Thread
     */
    private LoadGamesCaller caller;

    /**
     * The username of the user who we have logged in with the client; i.e. the one whose games we
     * are requesting.
     */
    private String username;

    /**
     * Create a new LoadGamesThread to send a load games request to the server, giving callbacks to
     * the given LoadGamesCaller.
     *
     * @param username - the username of the user currently logged in with the server
     * @param caller - will receive callbacks from this object relevant to the request
     * @param writer - the device this Thread will use to write to the server
     * @param reader - the device this Thread will use to read from the server
     */
    public LoadGamesThread(String username, LoadGamesCaller caller, PrintWriter writer, DataInputStream reader) {
        super(writer, reader);
        this.caller = caller;
        this.username = username;
    }

    @Override
    public void run() {
        // First, we of course send our request to the server
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
                case ReturnCodes.SERVER_ERROR:
                    caller.serverError();
                    return;
                case ReturnCodes.LoadGames.SUCCESS:
                    Log.i(tag, "Server says we can expect to receive all the user's games");
            }

            // Next, the server tells us how many games to expect
            int numGames = readInt();

            List<UserGame> games = new ArrayList<>();
            List<Object> serverData = new ArrayList<>();
            UserGame game;
            String line;
            // We read a total of numGames batches of data from the server
            for (int i = 0; i < numGames; i++) {
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
                games.add(game);

                serverData = new ArrayList<>();
            }

            // Pass the compiled list of games to the caller.
            caller.success(games);
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
     * Sends a load games request to the server
     */
    private void sendRequest() {
        this.sendRequest("loadgames");
    }
}
