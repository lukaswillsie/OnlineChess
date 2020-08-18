package com.lukaswillsie.onlinechess.network.threads;


import android.util.Log;

import com.lukaswillsie.onlinechess.data.ServerData;
import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.threads.callers.LoginCaller;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * This thread, when started, will send a login request to the server, then wait for and interpret
 * the response. The LoginCaller object given to this object at creation will receive callbacks
 * relevant to the state of the login request as it proceedss.
 */
public class LoginThread extends NetworkThread {
    /*
     * Tag used for logging
     */
    private static final String tag = "LoginThread";

    /*
     * The object that should receive callbacks about the state of the login request. Is set in the
     * constructor.
     */
    private LoginCaller caller;

    /**
     * The username and password combination to try and log in with. Set in the constructor.
     */
    private String username;
    private String password;

    /**
     * Create a new LoginThread to execute a login command using the given username and password,
     * and reporting the result to the given caller. This thread should not be run until setWriter()
     * and setReader() have been called.
     *
     * @param caller   - the object that this thread will report back to
     * @param username - the username to try and log in with
     * @param password - the password to try and log in with
     * @param writer   - the device this Thread will use to write to the server
     * @param reader   - the device this Thread will use to read from the server
     */
    public LoginThread(String username, String password, LoginCaller caller, PrintWriter writer, DataInputStream reader) {
        super(writer, reader);
        this.caller = caller;
        this.username = username;
        this.password = password;
    }

    /**
     * Send and process a full login request. A login request consists of about three parts.
     * <p>
     * First, the client (this thread) sends a request in the form "login username password"
     * <p>
     * Second, the server responds, telling the client whether or not the login succeeded, and the
     * reason for failure if there was one. Return codes are defined in the ReturnCodes enum in
     * the network.
     * <p>
     * Third, the server sends data corresponding to every game that the logged in user is involved
     * in. First, it sends an integer equal to how many of these games there are to send. Then it
     * sends them, one after the other, as batches of smaller bits of data, all integers or lines of
     * text, in a consistent order and format described in the data.ServerData enum.
     * <p>
     * Our Thread will be active the whole time the server is engaged with our request. It will
     * notify caller once after the second stage, according to whether the login succeeded or failed.
     * It will then notify the caller after the third stage, passing it a list of Game objects
     * constructed from the batches of data sent over by the server.
     */
    @Override
    public void run() {
        // Send our login request to the server.
        sendRequest("login " + username + " " + password);
        Log.i(tag, "Sent request \"login " + username + " " + password + "\" to server.");

        // We encase this code in a try/catch because our readInt and readLine methods throw any
        // exceptions they encounter while trying to read
        try {
            int code = readInt();

            switch (code) {
                case ReturnCodes.Login.SUCCESS:
                    Log.i(tag, "Login successful for credentials: " + username + "," + password);
                    caller.loginSuccess();
                    break;
                case ReturnCodes.Login.USERNAME_DOES_NOT_EXIST:
                    Log.i(tag, "Username \"" + username + "\" does not exist");
                    caller.usernameInvalid();
                    return;
                case ReturnCodes.Login.PASSWORD_INVALID:
                    Log.i(tag, "Password \"" + password + "\" invalid for username \"" + username + "\"");
                    caller.passwordInvalid();
                    return;
                case ReturnCodes.SERVER_ERROR:
                    Log.e(tag, "Server returned error in response to login request");
                    caller.serverError();
                    return;
                case ReturnCodes.FORMAT_INVALID:
                    Log.e(tag, "Server returned FORMAT_INVALID. Make sure request format " +
                            "conforms to protocol");
                    caller.serverError();
                    return;
                default: // In this case, the server returned a code outside of its defined protocol
                    Log.e(tag, "Server returned code " + code + ". Invalid for login request.");
                    caller.serverError();
                    return;
            }

            // Now we read all the user's game data from the server
            int numGames = readInt();

            // According to protocol, it's possible that the server encounters an error after logging in
            // the user but BEFORE sending games. So we handle that possibility here.
            if (numGames == ReturnCodes.SERVER_ERROR) {
                caller.serverError();
                return;
            }

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
            caller.loginComplete(games);
        } catch (EOFException e) {
            // This means the server has closed their end of the connection.
            //
            // If the server has disconnected, we can't proceed with a login until a new connection
            // has been made. So notify the caller of the problem, and then exit this thread.
            Log.e(tag, "Server has disconnected.");
            caller.connectionLost();
        } catch (SocketException e) {
            // This means some problem occurred with the connection. The server may have crashed,
            // for example.
            //
            // If the server has disconnected, we can't proceed with a login until a new connection
            // has been made. So notify the caller of the problem, and then exit this thread.
            Log.e(tag, "Server has disconnected.");
            caller.connectionLost();
        } catch (IOException e) {
            Log.e(tag, "IOException while reading from server");
            e.printStackTrace();
            caller.systemError();
        }
    }
}
