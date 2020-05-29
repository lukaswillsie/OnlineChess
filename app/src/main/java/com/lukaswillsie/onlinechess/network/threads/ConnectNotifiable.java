package com.lukaswillsie.onlinechess.network.threads;

import java.net.Socket;

/**
 * Classes wishing to use a ConnectThread to establish a connection with a server must implement
 * this interface so that the thread can notify objects of the class when the attempted connection
 * is resolved, either successfully or unsuccessfully.
 */
public interface ConnectNotifiable {
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
