package com.lukaswillsie.onlinechess.network.threads;

import android.util.Log;

import com.lukaswillsie.onlinechess.network.threads.callers.ConnectCaller;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * This thread exists simply to establish connections with the server. Must be given a ConnectCaller
 * object at creation.
 *
 * The ConnectCaller will receive the relevant callback after a successful/unsuccessful connection
 * attempt.
 *
 * A ConnectThread will wait 5000 milliseconds for a connection before timing out and calling the
 * connectionFailed() method of its ConnectCaller.
 */
public class ConnectThread extends Thread {
    /*
     * Tag used for logging
     */
    private static final String tag = "ConnectThread";

    /*
     * How long ConnectThreads will wait for a connection to be established before timing out
     */
    private static final int TIMEOUT = 5000;


    /*
     * The hostname and port of the server that this ConnectThread is to connect to.
     */
    private String hostname;
    private int port;

    /*
     * The object that will receive callbacks from this Thread.
     */
    private ConnectCaller caller;

    /**
     * Create a new ConnectThread, ready when started to establish a connection with the program at
     * the given hostname and port.
     * @param hostname - the name of the server's host machine
     * @param port - the port on which the server this Thread is to connect to is running on
     * @param caller - the object that will receive callbacks relating to the connection attempt
     *               from this thread
     */
    public ConnectThread(String hostname, int port, ConnectCaller caller) {
        this.hostname = hostname;
        this.port = port;
        this.caller = caller;
    }

    /**
     * Run this ConnectThread, and attempt to establish a connection with the server given by the
     * hostname and port values passed to this object's constructor.
     */
    @Override
    public void run() {
        Log.i(tag, "Attempting to connect to server...");
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(hostname, port), TIMEOUT);
            Log.i(tag, "Connection with server established. Notifying caller.");
            caller.connectionEstablished(socket);
        } catch (UnknownHostException e) {
            Log.e(tag,
            "UnknownHostException from Host: \" " + hostname + "\" and Port: " + port);
            caller.connectionFailed();
        } catch(SocketTimeoutException e) {
            Log.e(tag, "Connection to server timed out after " + TIMEOUT + " milliseconds.");
            caller.connectionFailed();
        } catch (IOException e) {
            Log.e(tag,
            "IOException when trying to connect to" + " Host: \" " + hostname + "\" and Port: "
                    + port);
            caller.connectionFailed();
        }
    }
}
