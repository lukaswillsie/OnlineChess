package com.lukaswillsie.onlinechess.network;

import android.os.Message;
import android.util.Log;
import android.os.Handler;

import com.lukaswillsie.onlinechess.network.threads.ConnectNotifiable;
import com.lukaswillsie.onlinechess.network.threads.ConnectThread;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;

/**
 * This class encapsulates the process of connecting to and interacting with the server. The
 * instance that successfully establishes a connection to the server when the app loads is passed
 * around as a serializable extra, for access by any Activity that needs to submit a request to the
 * server on behalf of the user during the operation of the app.
 *
 * Due to the nature of the requests accepted by the server, I thought that it made the most sense
 * to impose a restriction of one ACTIVE request (that is, one to which the server has not yet
 * responded) per Activity, and an Activity should not transfer control or allow the user to
 * navigate away until the request has been handled.
 *
 * For example, if a user decides to load one of their ongoing games and inputs a move, the app
 * should wait for the server to respond with a success code before allowing the user to go
 * back to the previous screen. That way, if the server responds with an error, the user can be
 * notified of the fact that their move wasn't made.
 */
public class ServerHelper extends Handler implements ConnectNotifiable, Serializable {
    /*
     * Tag used for logging
     */
    private static final String tag = "network.ServerHelper";

    /*
     * The IP address of the machine running the server. I'm using the below address because that's
     * my machine's local IP address on my network.
     */
    private static final String HOSTNAME = "192.168.0.19";

    /*
     * The port that the server is supposed to be listening on
     */
    private static final int PORT = 46751;

    private static final int CONNECTION_ESTABLISHED = 0;
    private static final int CONNECTION_FAILED = 1;

    /*
     * This is the code that methods defined in this interface will return if the server's end of the
     * connection is found to have been broken in the course of the method call.
     */
    public static final int SERVER_DISCONNECT = -4;

    /*
     * The socket that represents this object's connection with the server
     */
    private Socket socket;

    /*
     * The device we'll use to write to the server
     */
    private PrintWriter out;

    /*
     * The device we'll use to read from the server
     */
    private DataInputStream in;

    /**
     * The Activity that submitted the current, ongoing request. null if there is no active request.
     * Requester is a tagging interface given to Activities that may need to make a request of a
     * ServerHelper object.
     */
    private Requester requester;

    public void connect(ConnectRequester activity) throws MultipleRequestException {
        if(requester != null) {
            throw new MultipleRequestException("Activity " + activity.toString() + " tried to " +
                    "connect while request from " + requester.toString() + " was active.");
        }

        this.requester = activity;
        ConnectThread thread = new ConnectThread(HOSTNAME, PORT, this);
        thread.start();
    }

    @Override
    public void connectionEstablished(Socket socket) {
        this.socket = socket;
        if(!(requester instanceof ConnectRequester)) {
            Log.e(tag,"connectionEstablished called but requester is not ConnectRequester");
            return;
        }

        try {
            this.out = new PrintWriter(socket.getOutputStream());
            this.in = new DataInputStream(socket.getInputStream());
            Message message = this.obtainMessage(CONNECTION_ESTABLISHED);
            message.sendToTarget();
        } catch (IOException e) {
            Log.e(tag, "Couldn't instantiate devices to communicate with server");
            Message message = this.obtainMessage(CONNECTION_FAILED);
            message.sendToTarget();
        }
    }

    @Override
    public void connectionFailed() {
        ConnectRequester activity;
        try {
            activity = (ConnectRequester) requester;
        }
        catch(ClassCastException e) {
            Log.e(tag, "connectionEstablished() called but no ConnectRequester made " +
                    "connect request");
            return;
        }

        activity.connectionFailed();
    }

    public void login(LoginRequester requester) throws MultipleRequestException {
        // TODO: Create LoginThread and write code to handle login
    }


    private void loginRequest(String username, String password) {
        out.println("login " + username + " " + password);
    }


    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case CONNECTION_ESTABLISHED:
                ((ConnectRequester)requester).connectionEstablished(this);
                break;
            case CONNECTION_FAILED:
                ((ConnectRequester)requester).connectionFailed();
                break;
            default:
                Log.e(tag, "Received invalid message from thread.");
                break;
        }
    }
}
