package com.lukaswillsie.onlinechess.network.threads;

import com.lukaswillsie.onlinechess.data.Game;

import java.net.Socket;
import java.util.List;

public interface ThreadCaller {
    // GENERAL CALLBACKS
    /**
     * To be called if there is a problem with the server. For example, if the server returns -1,
     * indicating that it encountered an error on its end; or if the server returns data that is
     * invalid or inconsistent with protocol.
     */
    void serverError();

    /**
     * To be called if a problem with the system is encountered during execution of a client Thread.
     * For example, an exception that is unrelated to the server might be thrown by the operating
     * system when a Thread tries to write/read from the socket. We allow the caller, and therefore
     * the front-end of the app, to differentiate between these two types of problems.
     */
    void systemError();

    /**
     * To be called if a client Thread discovers the server to have disconnected in the course of
     * its work.
     */
    void connectionLost();



    // CONNECTION-RELATED CALLBACKS
    /**
     * Notifies the object that created the Thread that a connection was successfully established
     * using the given Socket.
     *
     * @param socket - the socket representing the successfully established connection
     */
    void connectionEstablished(Socket socket);

    /**
     * Notifies the object that created the Thread that a connection could not be established.
     */
    void connectionFailed();



    // LOGIN-RELATED CALLBACKS
    /**
     * To be called immediately once the server has confirmed the success of the login request
     */
    void loginSuccess();

    /**
     * To be called immediately once the server has responded that the given username does not exist
     */
    void usernameInvalid();

    /**
     * To be called immediately once the server has responded that the given password is invalid for
     * the given username
     */
    void passwordInvalid();

    /**
     * To be called once the whole login process is complete. That is, the login has been validated
     * by the server and all of the user's game data has been received and processed by the
     * LoginThread.
     *
     * The Thread passes the result, a list of Game objects, to this method.
     */
    void loginComplete(List<Game> games);
}
