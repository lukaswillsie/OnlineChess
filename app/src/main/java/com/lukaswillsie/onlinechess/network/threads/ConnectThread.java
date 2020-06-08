package com.lukaswillsie.onlinechess.network.threads;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.lukaswillsie.onlinechess.network.ServerHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class ConnectThread extends Thread {
    private static final String tag = "ConnectThread";
    private static final int TIMEOUT = 5000;


    private String hostname;
    private int port;
    private ThreadCaller caller;

    public ConnectThread(String hostname, int port, ThreadCaller caller) {
        this.hostname = hostname;
        this.port = port;
        this.caller = caller;
    }

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
            caller.systemError();
        }
    }
}
