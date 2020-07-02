package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;
import android.util.Log;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.network.helper.requesters.ArchiveRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.Connector;
import com.lukaswillsie.onlinechess.network.helper.requesters.CreateAccountRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.LoginRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.Networker;
import com.lukaswillsie.onlinechess.network.threads.callers.ConnectCaller;
import com.lukaswillsie.onlinechess.network.threads.ConnectThread;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates the process of connecting to and interacting with the server. The
 * instance that successfully establishes a connection to the server when the app loads is saved in
 * ChessApplication, for access by any Activity that needs to submit a request to the
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
 *
 * The only type of request that this object handles directly is a connect request. All other
 * requests are handled by SubHelper objects, for example LoginHelper, for which this object acts as
 * a façade.
 *
 * This class extends Handler because it needs a way to bridge the gap between ConnectThreads that
 * it spawns and the UI thread (the UI isn't thread-safe so we can't just call requester's callbacks
 * directly).
 */
public class ServerHelper extends Handler implements ConnectCaller {
    /*
     * Tag used for logging
     */
    private static final String tag = "network.ServerHelper";

    private Connector requester;

    /*
     * The IP address of the machine running the server. I'm using the below address because that's
     * my machine's local IP address on my network.
     */
    private static final String HOSTNAME = "192.168.0.19";

    /*
     * The port that the server is supposed to be listening on
     */
    private static final int PORT = 46751;

    /*
     * Constants that this object uses to send messages to itself.
     */
    private static final int CONNECTION_ESTABLISHED = 0;
    private static final int CONNECTION_FAILED = 1;

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

    /*
     * A direct reference to each of the helpers that this object delegates specific tasks to.
     */
    private LoginHelper loginHelper;
    private CreateAccountHelper createAccountHelper;
    private ArchiveHelper archiveHelper;

    /*
     * A list of all helpers delegated to by this object, so that they can all be notified at once
     * of events that are common to all of them, like the event of a server disconnect.
     */
    private List<SubHelper> helpers;

    /**
     * Create a new ServerHelper for handling network tasks
     */
    public ServerHelper() {
        this.loginHelper = new LoginHelper(this);
        this.createAccountHelper = new CreateAccountHelper(this);
        this.archiveHelper = new ArchiveHelper(this);

        this.helpers = new ArrayList<>();
        this.helpers.add(loginHelper);
        this.helpers.add(createAccountHelper);
        this.helpers.add(archiveHelper);
    }

    /**
     * This method is called by this object's SubHelpers when they discover that the connection with
     * the server has been lost. This object closes the IO resources associated with the server that
     * are shared among all helpers, and then notifies each helper in turn that the IO resources
     * they are using shouldn't be used anymore.
     */
    void connectionLost() {
        try {
            this.socket.close();
            this.in.close();
        } catch (IOException e) {
            Log.e(tag, "Couldn't close socket or DataInputStream");
        }
        this.out.close();

        for(SubHelper helper : helpers) {
            helper.closeIO();
        }
    }

    /**
     * Process a login request using the given credentials on behalf of the given requester.
     *
     * @param requester - the Activity wishing to log in a user; will be given callbacks as to the
     *                  state of the request
     * @param username - the username the user wishes to log in with
     * @param password - the password the user wishes to log in with
     * @throws MultipleRequestException - if this ServerHelper object already has an ongoing request
     */
    public void login(LoginRequester requester, String username, String password) throws MultipleRequestException {
        // Delegate to a LoginHelper and designate the LoginHelper as the active helper
        this.loginHelper.login(requester, username, password);
    }

    /**
     * Process an account creation request by the given requester. Will send a request to the server
     * to create a new account with the given credentials, and give callbacks to the given
     * requester.
     *
     * @param requester - the object that should be notified of the result of the account creation
     *                  request
     * @param username - the username that the new account should have
     * @param password - the password that the new account should have
     * @throws MultipleRequestException - if this ServerHelper object already has an ongoing request
     */
    public void createAccount(CreateAccountRequester requester, String username, String password) throws MultipleRequestException {
        this.createAccountHelper.createAccount(requester, username, password);
    }

    /**
     * Process a connect request on behalf of the given requester.
     * @param requester - the Activity wishing to establish a connection with the server; will
     *                  receive callbacks relating to the request
     * @throws MultipleRequestException - if this ServerHelper object already has an ongoing request
     */
    public void connect(Connector requester) throws MultipleRequestException {
        if(this.requester != null) {
            throw new MultipleRequestException("Tried to make multiple requests of ServerHelper");
        }
        this.requester = requester;

        ConnectThread thread = new ConnectThread(HOSTNAME, PORT, this);
        thread.start();
    }

    public void archive(String gameID, ArchiveRequester requester) throws MultipleRequestException {
        archiveHelper.archive(new ArchiveHelper.ArchiveRequest(gameID, requester));
    }

    /**
     * The ConnectThread created by this object uses this method to communicate that a connection
     * was successfully established using the given Socket.
     *
     * @param socket - the socket representing the successfully established connection
     */
    @Override
    public void connectionEstablished(Socket socket) {
        this.socket = socket;
        try {
            // Create IO devices for communicating with the server and give them to all SubHelpers
            // for subsequent requests
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new DataInputStream(socket.getInputStream());

            for(SubHelper helper : helpers) {
                helper.setInputStream(this.in);
                helper.setOutput(this.out);
            }

            Message message = this.obtainMessage(CONNECTION_ESTABLISHED);
            message.sendToTarget();
        } catch (IOException e) {
            Log.e(tag, "Couldn't instantiate devices to communicate with server");
            Message message = this.obtainMessage(CONNECTION_FAILED);
            message.sendToTarget();
        }
    }

    /**
     * The ConnectThread created by this object uses this method to communicate that a connection
     * could not be established.
     */
    @Override
    public void connectionFailed() {
        Message message = this.obtainMessage(CONNECTION_FAILED);
        message.sendToTarget();
    }

    /**
     * This class uses this method to give callbacks to the UI thread.
     */
    @Override
    public void handleMessage(@NonNull Message msg) {
        switch(msg.what) {
            case CONNECTION_ESTABLISHED:
                requester.connectionEstablished(this);
                this.requester = null;  // Signifies that the connection request is over
                break;
            case CONNECTION_FAILED:
                requester.connectionFailed();
                this.requester = null;  // Signifies that the connection request is over
                break;
        }
    }
}
