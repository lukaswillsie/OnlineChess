package com.lukaswillsie.onlinechess.network.threads.callers;

/**
 * This interface defines behaviour that should be common to all objects that are spawning a Thread
 * to do network work for them. No class should implement this interface directly, but rather
 * should implement the interface specific to the request they are making, like LoginCaller (for
 * objects who want to issue login requests).
 *
 * The defined behaviour is simple: be able to handle errors on the server-side, errors by the
 * system, or a loss of connection.
 */
public interface ThreadCaller {
    /**
     * To be called if a problem with the system is encountered during execution of a client Thread.
     * For example, an exception that is unrelated to the server might be thrown by the operating
     * system when a Thread tries to write/read from the socket. We allow the caller, and therefore
     * the front-end of the app, to differentiate between this kind of error and a server error,
     */
    void systemError();

    /**
     * To be called if a client Thread discovers the server to have disconnected in the course of
     * its work.
     */
    void connectionLost();
}
