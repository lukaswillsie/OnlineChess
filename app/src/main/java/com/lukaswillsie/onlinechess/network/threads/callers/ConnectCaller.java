package com.lukaswillsie.onlinechess.network.threads.callers;

import java.net.Socket;

/**
 * Defines what an object that wants to spawn a ConnectThread to handle a connection to the server
 * needs to be able to handle. Simply defines two callbacks for use by the ConnectThread: one if the
 * connection is successfully established, one otherwise.
 */
public interface ConnectCaller {
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
}
