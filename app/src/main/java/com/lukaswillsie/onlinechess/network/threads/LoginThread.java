package com.lukaswillsie.onlinechess.network.threads;


import android.util.Log;

import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.ServerData;
import com.lukaswillsie.onlinechess.network.ReturnCodes;

import java.io.DataInputStream;
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
public class LoginThread extends Thread {
    /*
     * Tag used for logging
     */
    private static final String tag = "LoginThread";

    /*
     * The IO devices this object will use to communicate with the server. MUST BE SET through
     * setter methods below before the thread is started
     */
    private PrintWriter writer;
    private DataInputStream reader;

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
     * @param caller - the object that this thread will report back to
     * @param username - the username to try and log in with
     * @param password - the password to try and log in with
     */
    public LoginThread(String username, String password, LoginCaller caller) {
        this.caller = caller;
        this.username = username;
        this.password = password;
    }

    /**
     * Give this LoginThread a PrintWriter to use to write to the server
     * @param writer - the PrintWriter this thread should use to write to the server
     */
    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }

    /**
     * Give this LoginThread a DataInputStream to use to read from the server
     * @param reader - the DataInputStream this thread should use to read from the server
     */
    public void setReader(DataInputStream reader) {
        this.reader = reader;
    }

    /**
     * Send and process a full login request. A login request consists of about three parts.
     *
     * First, the client (this thread) sends a request in the form "login username password"
     *
     * Second, the server responds, telling the client whether or not the login succeeded, and the
     * reason for failure if there was one. Return codes are defined in the ReturnCodes enum in
     * this package
     *
     * Third, the server sends data corresponding to every game that the logged in user is involved
     * in. First, it sends an integer equal to how many of these games there are to send. Then it
     * sends them, one after the other, as batches of smaller bits of data, all integers or lines of
     * text, in a consistent order and format described in the data.ServerData enum.
     *
     * Our Thread will be active the whole time the server is engaged with our request. It will
     * notify caller once after the second stage, according to whether the login succeeded or failed.
     * It will then notify the caller after the third stage, passing it a list of Game objects
     * constructed from the batches of data sent over by the server.
     */
    @Override
    public void run() {
        // Send our login request to the server.
        sendRequest();

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

            List<Game> games = new ArrayList<>();
            List<Object> serverData = new ArrayList<>();
            Game game;
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
                        serverData.add(this.readInt());
                    }
                }

                game = new Game(username);
                game.initialize(serverData);

                games.add(game);
            }

            // Pass the compiled list of games to the caller.
            caller.loginComplete(games);
        }
        catch(SocketException e) {
            // If the server has disconnected, we can't proceed with a login until a new connection
            // has been made. So notify the caller of the problem, and then exit this thread.
            Log.e(tag, "Server has disconnected.");
            caller.connectionLost();
        }
        catch (IOException e) {
            Log.e(tag, "IOException while reading from server");
            e.printStackTrace();
            caller.systemError();
        }
    }

    /**
     * Send a login request to the server using the data stored in this object
     */
    private void sendRequest() {
        // TODO: look into what happens when the server shuts down and the writer tries to write
        Log.i(tag, "Sent request \"login " + username + " " + password + "\" to server");
        writer.println("login " + username + " " + password);
    }

    /**
     * Read a single integer from the server and return it
     * @return the integer read from the server
     * @throws SocketException if the server has disconnected when this method tries to read from it
     * @throws IOException if there is some other problem with the read, like a system error
     */
    private int readInt() throws SocketException, IOException {
        // TODO: Look into what happens when the server shuts down and the reader tries to read
        int code = reader.readInt();
        Log.i(tag, "Read: " + code);
        return code;
    }

    /**
     * Reads a single line of input from the server. That is, reads ONE-BYTE chars from the server
     * repeatedly until a network newline, "\r\n", is found.
     *
     * @throws SocketException if the server has disconnected when this method tries to read from it
     * @throws IOException if there is some other problem with the read, like a system error
     */
    private String readLine() throws SocketException, IOException {
        char[] last = {'\0', '\0'};
        StringBuilder builder = new StringBuilder();

        char read;
        while(last[0] != '\r'|| last[1] != '\n') {
            read = (char)reader.read();
            Log.i(tag, "Read: " + read);
            last[0] = last[1];
            last[1] = read;

            builder.append(read);
        }

        // Truncate the builder to omit the "\r\n" at the end of the line
        builder.setLength(builder.length() - 2);
        String line = builder.toString();
        Log.i(tag, "Read: \"" + line + "\"");
        return line;
    }
}
