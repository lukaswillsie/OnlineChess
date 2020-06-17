package com.lukaswillsie.onlinechess.network.threads.callers;

/**
 * This interface must be implemented before an object can spawn a ReturnCodeThread to issue a
 * network request. A ReturnCodeThread is much more simple than a LoginThread or ConnectThread. It
 * sends the request it is given at creation to the server, then waits for the server's response
 * and simply passes that on to the caller. So interacting with a ReturnCodeThread is pretty simple;
 * you just need to be able to handle the error cases outlined in ThreadCaller and you need to be
 * able to receive and process the server's response.
 */
public interface ReturnCodeCaller extends ThreadCaller {
    /**
     * Notifies the caller that the ReturnCodeThread has received a response from the server
     *
     * @param code - the code returned by the server
     */
    void onServerReturn(int code);
}
