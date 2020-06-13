package com.lukaswillsie.onlinechess.network.helper;

import java.io.DataInputStream;
import java.io.PrintWriter;
import android.os.Handler;

/**
 * A SubHelper is an object that a ServerHelper can delegate to for specific tasks. As each server
 * command, of which there are currently 13, has around 5 possible return codes/callbacks, this
 * keeps ServerHelper much more readable than the alternative, while allowing the rest of the app
 * to only depend on one object: ServerHelper.
 *
 * However, implementing a façade here requires a few mechanisms for communication between
 * ServerHelper and its SubHelpers. In particular, all SubHelpers need access to the same set of IO
 * objects for interacting with the server, so we need a way for ServerHelper to distribute these
 * tools. We also need to be sure that when a single SubHelper finds that the connection to the
 * server has been lost, this information can be propagated across all SubHelpers.
 *
 * The ServerHelper also acts as a gateway, accepting requests from outside before handing them off
 * to SubHelpers. It's also therefore responsible for enforcing that only one request be active at
 * a time. So we provide a mechanism for SubHelpers to notify ServerHelper that they've finished
 * with a request.
 */
abstract class SubHelper extends Handler {
    /**
     * The ServerHelper that this SubHelper is a part of
     */
    private ServerHelper container;

    /**
     * Return the DataInputStream this object is using to read from the server
     * @return - the DataInputStream this object is using to read from the server
     */
    DataInputStream getIn() {
        return in;
    }

    /**
     * The DataInputStream this object uses to read from the server
     */
    private DataInputStream in;

    /**
     * Return the PrintWriter this object is using to write to the server
     * @return - the PrintWriter this object is using to write to the server
     */
    PrintWriter getOut() {
        return out;
    }

    /**
     * The PrintWriter this object is using to write to the server
     */
    private PrintWriter out;

    /**
     * Create a new SubHelper as part of the given ServerHelper
     * @param container - the ServerHelper that this object is a part of
     */
    SubHelper(ServerHelper container) {
        this.container = container;
    }

    /**
     * Give this object a new DataInputStream to use for reading from the server
     * @param inputStream - a new DataInputStream for this object to use for reading from the server
     */
    void setInputStream(DataInputStream inputStream) {
        this.in = inputStream;
    }

    /**
     * Give this object a new PrintWriter to use for writing to the server
     * @param writer - a new PrintWriter for this object to use for writing to the server
     */
    void setOutput(PrintWriter writer) {
        this.out = writer;
    }

    /**
     * Notify this object that the IO devices it is using aren't valid anymore
     */
    void closeIO() {
        this.in = null;
        this.out = null;
    }

    /**
     * Notifies the ServerHelper that this object is a part of that the connection with the server
     * has been lost
     */
    void notifyContainerConnectionLost() {
        container.connectionLost();
    }

    /**
     * Notifies the ServerHelper that this object is a part of that the request this object has
     * been handling has been handled
     */
    void notifyContainerRequestOver() {
        container.requestOver(this);
    }
}
