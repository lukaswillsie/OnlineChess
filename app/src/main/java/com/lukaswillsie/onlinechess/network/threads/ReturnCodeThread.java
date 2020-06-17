package com.lukaswillsie.onlinechess.network.threads;

import android.util.Log;

import com.lukaswillsie.onlinechess.network.threads.callers.ReturnCodeCaller;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;

/**
 * Most server requests are simple: the client makes a request and the server simply responds
 * with a return code, indicating success or various degrees of failure. For requests like this,
 * where the exact same behaviour is required in a number of different possible situations, we use
 * a single thread class that is given a request at creation and simply returns the server's
 * response when run.
 */
public class ReturnCodeThread extends NetworkThread {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "ReturnCodeThread";

    /**
     * The object that this Thread will give the server's response to
     */
    private ReturnCodeCaller caller;

    /**
     * The request that this Thread should send to the server.
     */
    private String request;

    /**
     * Set up a new ReturnCodeThread.
     *
     * NOTE: The superclass methods setWriter and setReader() MUST BE CALLED before this Thread is
     * started.
     *
     * @param request - the request to send to the server when this Thread is run
     * @param caller - the object to report the result back to
     */
    public ReturnCodeThread(String request, ReturnCodeCaller caller) {
        this.caller = caller;
        this.request = request;
    }

    /**
     * Runs this thread. Simply sends the request given to this object at creation to the server,
     * and reports the return code back to the caller.
     */
    @Override
    public void run() {
        this.sendRequest(request);

        try {
            int code = this.readInt();
            caller.onServerReturn(code);
        }
        // These first two exceptions mean that the server has disconnected
        catch(EOFException e) {
            Log.i(tag, "EOFException thrown. Server disconnected.");
            caller.connectionLost();
        }
        catch(SocketException e) {
            Log.i(tag, "SocketException thrown. Server disconnected.");
            caller.connectionLost();
        }
        // This means that there was some other problem, a system problem, with our attempt to read
        catch(IOException e) {
            Log.i(tag, "IOException while reading from server");
            e.printStackTrace();
            caller.systemError();
        }
    }
}
