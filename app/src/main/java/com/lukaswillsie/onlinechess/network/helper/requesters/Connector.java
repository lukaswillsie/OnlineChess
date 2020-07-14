package com.lukaswillsie.onlinechess.network.helper.requesters;


import com.lukaswillsie.onlinechess.network.helper.ServerHelper;

/**
 * Activities that wish to establish a connection with the server need to implement this interface
 * so they can receive callbacks from the ServerHelper, who actually handles the connection.
 * <p>
 * This interface doesn't extend Requester because Requester is for Activities that are sending
 * actual requests to the server. Connectors only seek to establish a connection, with no regard for
 * the current state of any connection, and so shouldn't have to handle the serverError() and
 * connectionLost() methods defined in Requester.
 */
public interface Connector extends Networker {
    /**
     * After a ServerHelper is tasked with establishing a connection, they will call this method on
     * success, and pass a reference to themselves so they can be used for future network
     * operations.
     *
     * @param helper - the ServerHelper object that has successfully established a connection with
     *               the server
     */
    void connectionEstablished(ServerHelper helper);

    /**
     * If a ServerHelper has been tasked by an implementation of this interface to establish a
     * connection, and the connection could not be successfully created, the ServerHelper will
     * call this method so that this event can be handled.
     */
    void connectionFailed();
}
