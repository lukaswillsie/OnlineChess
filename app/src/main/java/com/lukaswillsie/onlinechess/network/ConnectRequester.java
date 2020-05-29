package com.lukaswillsie.onlinechess.network;

import com.lukaswillsie.onlinechess.network.Requester;
import com.lukaswillsie.onlinechess.network.ServerHelper;

/**
 * Activities that wish to establish a connection with the server need to implement this interface
 * so they can receive callbacks from the ServerHelper, who actually handles the connection.
 */
public interface ConnectRequester extends Requester {
    /**
     * After a ServerHelper is tasked with establishing a connection, they will call this method on
     * the success, and pass a reference to themselves so they can be used for future network
     * operations.
     *
     * @param helper - the ServerHelper object that has successfully established a connection with
     *               the server
     */
    void connectionEstablished(ServerHelper helper);

    // TODO: Check for network connection before trying to establish connection in ServerHelper
    void noNetwork();

    /**
     * If a ServerHelper has been tasked by an implementation of this interface to establish a
     * connection, and the connection could not be successfully created, the ServerHelper will
     * call this method so that this event can be handled.
     */
    void connectionFailed();
}
