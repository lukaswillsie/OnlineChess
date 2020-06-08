package com.lukaswillsie.onlinechess.network;

import android.content.Context;
import android.os.Message;
import android.util.Log;
import android.os.Handler;

import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.network.threads.ConnectThread;
import com.lukaswillsie.onlinechess.network.threads.LoginThread;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;
import com.lukaswillsie.onlinechess.network.threads.ThreadCaller;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

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
public class ServerHelper extends Handler implements ThreadCaller                                                                                                                                                                                           {
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

    private static final int SYSTEM_ERROR = -3;
    private static final int CONNECTION_LOST = -2;
    private static final int SERVER_ERROR = -1;
    private static final int CONNECTION_ESTABLISHED = 0;
    private static final int CONNECTION_FAILED = 1;
    private static final int LOGIN_SUCCESS = 2;
    private static final int USERNAME_INVALID = 3;
    private static final int PASSWORD_INVALID = 4;
    private static final int LOGIN_COMPLETE = 5;

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
    private Networker requester;

    /**
     * The operating context
     */
    private Context context;

    public ServerHelper(Context context) {
        this.context = context;
    }


    public void connect(Connector requester) throws MultipleRequestException {
        if(this.requester != null) {
            throw new MultipleRequestException("Activity " + requester.toString() + " tried to " +
                    "connect while request from " + this.requester.toString() + " was active.");
        }

        this.requester = requester;
        ConnectThread thread = new ConnectThread(HOSTNAME, PORT, this);
        thread.start();
    }

    @Override
    public void connectionEstablished(Socket socket) {
        this.socket = socket;
        if(!(requester instanceof Connector)) {
            Log.e(tag,"connectionEstablished() called but requester is not Connector");
            return;
        }

        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
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
        if(!(requester instanceof Connector)) {
            Log.e(tag, "connectionEstablished() called but no Connector made " +
                    "connect request");
            return;
        }

        Message message = this.obtainMessage(CONNECTION_FAILED);
        message.sendToTarget();
    }

    public void login(LoginRequester requester, String username, String password) throws MultipleRequestException {
        if(this.requester != null) {
            throw new MultipleRequestException("Activity " + requester.toString() + " tried to " +
                    "connect while request from " + requester.toString() + " was active.");
        }

        this.requester = requester;
        LoginThread thread = new LoginThread(username, password, this);
        thread.setWriter(this.out);
        thread.setReader(this.in);

        thread.start();
    }

    @Override
    public void loginSuccess() {
        Message message = this.obtainMessage(LOGIN_SUCCESS);
        message.sendToTarget();
    }

    @Override
    public void usernameInvalid() {
        Message message = this.obtainMessage(USERNAME_INVALID);
        message.sendToTarget();
    }

    @Override
    public void passwordInvalid() {
        Message message = this.obtainMessage(PASSWORD_INVALID);
        message.sendToTarget();
    }

    @Override
    public void serverError() {
        Message message = this.obtainMessage(SERVER_ERROR);
        message.sendToTarget();
    }

    @Override
    public void systemError() {

    }

    /**
     * To be called once the whole login process is complete. That is, the login has been validated
     * by the server and all of the user's game data has been received and processed by the
     * LoginThread.
     *
     * The Thread passes the result, a list of Game objects, to this method.
     */
    @Override
    public void loginComplete(List<Game> games) {
        // Save the list of games in ChessApplication for later use by the app

        Message message = this.obtainMessage(LOGIN_COMPLETE, games);
        message.sendToTarget();
    }


    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case SYSTEM_ERROR:
                requester.systemError();
                this.requester = null;  // Allows us to accept another request
                break;
            case CONNECTION_LOST:
                ((Requester)requester).connectionLost();
                this.requester = null;  // Allows us to accept another request
                break;
            case SERVER_ERROR:
                ((Requester)requester).serverError();
                this.requester = null;  // Allows us to accept another request
                break;
            case CONNECTION_ESTABLISHED:
                ((Connector)requester).connectionEstablished(this);
                this.requester = null;  // Allows us to accept another request
                break;
            case CONNECTION_FAILED:
                ((Connector)requester).connectionFailed();
                this.requester = null;  // Allows us to accept another request
                break;
            case LOGIN_SUCCESS:
                ((LoginRequester)requester).loginSuccess();
                break;
            case USERNAME_INVALID:
                ((LoginRequester)requester).usernameInvalid();
                this.requester = null;  // Allows us to accept another request
                break;
            case PASSWORD_INVALID:
                ((LoginRequester)requester).passwordInvalid();
                this.requester = null;  // Allows us to accept another request
                break;
            case LOGIN_COMPLETE:
                ((LoginRequester)requester).loginComplete((List<Game>)message.obj);
                this.requester = null;  // Allows us to accept another request
                break;
        }
    }

    /**
     * This method is called by a client Thread when the Thread discovers that the connection to the
     * server has been lost at any point in the middle of a network request.
     */
    @Override
    public void connectionLost() {
        try {
            this.socket.close();
            this.in.close();
        } catch (IOException e) {
            Log.i(tag, "Couldn't close socket or DataInputStream");
        }
        this.out.close();

        Message msg = this.obtainMessage(CONNECTION_LOST);
        msg.sendToTarget();
    }
}
