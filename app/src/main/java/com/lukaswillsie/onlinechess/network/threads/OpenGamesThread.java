package com.lukaswillsie.onlinechess.network.threads;

import android.util.Log;

import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.ServerData;
import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.threads.callers.OpenGamesCaller;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * A Thread that handles the sending of an open games request to the server. An open games request
 * is a request that gets a list of all games that currently have one player and are open to all
 * users to join.
 */
public class OpenGamesThread extends NetworkThread {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "OpenGamesThread";
    /**
     * The object that will receive callbacks relevant to this object's request
     */
    private OpenGamesCaller caller;

    /**
     * Create a new OpenGamesThread that will give callbacks to the given caller and conduct network
     * IO operations with the given devices
     *
     * @param caller - will receive callbacks relevant to the state of the open games request
     * @param writer - the device to be used to write to the server
     * @param reader - the device to be used to read from the server
     */
    public OpenGamesThread(OpenGamesCaller caller, PrintWriter writer, DataInputStream reader) {
        super(writer, reader);
        this.caller = caller;
    }

    @Override
    public void run() {
        // Send our request to the server
        this.sendRequest(getRequest());

        // First, the server tells us how many games to expect
        int response;
        try {
            response = readInt();
        } catch (EOFException e) {
            Log.e(tag, "Connection with server closed by server");
            caller.connectionLost();
            return;
        } catch (SocketException e) {
            Log.e(tag, "Connection with server closed");
            caller.connectionLost();
            return;
        } catch (IOException e) {
            Log.e(tag, "IOException reading response from server");
            e.printStackTrace();
            caller.systemError();
            return;
        }

        if (response == ReturnCodes.SERVER_ERROR) {
            Log.e(tag, "Server returned SERVER_ERROR in response to request \"" + getRequest() + "\"");
            caller.serverError();
            return;
        }

        // Now, we repeatedly read as many games, represented as batches of data sent over by the
        // server, as the server has told us to expect
        List<Game> openGames = new ArrayList<>();
        List<Object> data = new ArrayList<>();
        try {
            for (int i = 0; i < response; i++) {
                for (ServerData dataType : ServerData.order) {
                    if (dataType.type == 'i') {
                        int num = 0;
                        num = readInt();
                        data.add(num);
                    } else if (dataType.type == 's') {
                        String line = readLine();
                        data.add(line);
                    }
                }

                Game game = new Game();
                int code = game.initialize(data);
                data = new ArrayList<>();
                if (code == 1) {
                    Log.e(tag, "A game couldn't be initialized from data sent by server");
                    caller.serverError();
                    return;
                }

                openGames.add(game);
            }
        } catch (EOFException e) {
            Log.e(tag, "Connection with server closed by server");
            caller.connectionLost();
            return;
        } catch (SocketException e) {
            Log.e(tag, "Connection with server closed");
            caller.connectionLost();
            return;
        } catch (IOException e) {
            Log.e(tag, "IOException reading from server");
            caller.systemError();
            return;
        }

        caller.openGames(openGames);
    }

    /**
     * Return the command that this Thread should send to the server
     *
     * @return - the exact String that this Thread should send to the server to initiate a request
     */
    private String getRequest() {
        return "opengames";
    }
}
