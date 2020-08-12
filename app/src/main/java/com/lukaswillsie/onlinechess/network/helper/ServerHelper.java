package com.lukaswillsie.onlinechess.network.helper;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.activities.board.Move;
import com.lukaswillsie.onlinechess.activities.board.PieceType;
import com.lukaswillsie.onlinechess.network.helper.requesters.ArchiveRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.Connector;
import com.lukaswillsie.onlinechess.network.helper.requesters.CreateAccountRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.CreateGameRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.DrawRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.ForfeitRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.JoinGameRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.LoadGameRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.LoginRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.MoveRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.OpenGamesRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.PromotionRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.RejectRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.RestoreRequester;
import com.lukaswillsie.onlinechess.network.threads.ConnectThread;
import com.lukaswillsie.onlinechess.network.threads.callers.ConnectCaller;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates the process of connecting to and interacting with the server. The
 * instance that successfully establishes a connection to the server when the app loads is saved in
 * Server, for access by any Activity that needs to submit a request to the server on behalf of the
 * user during the operation of the app.
 * <p>
 * Due to the nature of the requests accepted by the server, I thought that it made the most sense
 * to impose a restriction of one ACTIVE request (that is, one to which the server has not yet
 * responded) per Activity, and an Activity should not transfer control or allow the user to
 * navigate away until the request has been handled.
 * <p>
 * For example, if a user decides to load one of their ongoing games and inputs a move, the app
 * should wait for the server to respond with a success code before allowing the user to go
 * back to the previous screen. That way, if the server responds with an error, the user can be
 * notified of the fact that their move wasn't made.
 * <p>
 * The only type of request that this object handles directly is a connect request. All other
 * requests are handled by SubHelper objects, for example LoginHelper, for which this object acts as
 * a façade.
 * <p>
 * This class extends Handler because it needs a way to bridge the gap between ConnectThreads that
 * it spawns and the UI thread (the UI isn't thread-safe so we can't just call requester's callbacks
 * directly).
 */
public class ServerHelper extends Handler implements ConnectCaller {
    /*
     * Tag used for logging to the console
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

    /*
     * Constants that this object uses to send Messages to itself.
     */
    private static final int CONNECTION_ESTABLISHED = 0;
    private static final int CONNECTION_FAILED = 1;

    /**
     * If there is a currently active connect request being handled by this object, this Connector
     * object will receive callbacks regarding the success or failure of that request. If there is
     * no currently active connect request, this reference is null.
     */
    private Connector requester;

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
    private RestoreHelper restoreHelper;
    private OpenGamesHelper openGamesHelper;
    private JoinGameHelper joinGameHelper;
    private CreateGameHelper createGameHelper;
    private LoadGameHelper loadGameHelper;
    private MoveHelper moveHelper;
    private PromotionHelper promotionHelper;
    private DrawHelper drawHelper;
    private RejectHelper rejectHelper;
    private ForfeitHelper forfeitHelper;

    /*
     * A list of all helpers delegated to by this object, so that they can all be notified at once
     * of events that are common to all of them, like the event of a server disconnect.
     */
    private List<SubHelper> helpers;

    /**
     * Create a new ServerHelper for handling network tasks. As part of the creation process, this
     * object will automatically attempt to create a connection with the server.
     *
     * @param requester - will receive callbacks from this object when the connection attempt
     *                  initiated by this object either succeeds or fails.
     */
    public ServerHelper(Connector requester) {
        this.loginHelper = new LoginHelper(this);
        this.createAccountHelper = new CreateAccountHelper(this);
        this.archiveHelper = new ArchiveHelper(this);
        this.restoreHelper = new RestoreHelper(this);
        this.openGamesHelper = new OpenGamesHelper(this);
        this.joinGameHelper = new JoinGameHelper(this);
        this.createGameHelper = new CreateGameHelper(this);
        this.loadGameHelper = new LoadGameHelper(this);
        this.moveHelper = new MoveHelper(this);
        this.promotionHelper = new PromotionHelper(this);
        this.drawHelper = new DrawHelper(this);
        this.rejectHelper = new RejectHelper(this);
        this.forfeitHelper = new ForfeitHelper(this);


        this.helpers = new ArrayList<>();
        this.helpers.add(loginHelper);
        this.helpers.add(createAccountHelper);
        this.helpers.add(archiveHelper);
        this.helpers.add(restoreHelper);
        this.helpers.add(openGamesHelper);
        this.helpers.add(joinGameHelper);
        this.helpers.add(createGameHelper);
        this.helpers.add(loadGameHelper);
        this.helpers.add(moveHelper);
        this.helpers.add(promotionHelper);
        this.helpers.add(drawHelper);
        this.helpers.add(rejectHelper);
        this.helpers.add(forfeitHelper);

        this.requester = requester;
        ConnectThread thread = new ConnectThread(HOSTNAME, PORT, this);
        thread.start();
    }

    /**
     * Process a login request using the given credentials on behalf of the given requester.
     *
     * @param requester - the Activity wishing to log in a user; will be given callbacks as to the
     *                  state of the request
     * @param username  - the username the user wishes to log in with
     * @param password  - the password the user wishes to log in with
     * @throws MultipleRequestException - if this ServerHelper object already has an ongoing login
     *                                  request
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
     * @param username  - the username that the new account should have
     * @param password  - the password that the new account should have
     * @throws MultipleRequestException - if this ServerHelper object already has an ongoing create
     *                                  account request
     */
    public void createAccount(CreateAccountRequester requester, String username, String password) throws MultipleRequestException {
        this.createAccountHelper.createAccount(requester, username, password);
    }

    /**
     * Process a connect request on behalf of the given requester. Throws an exception if there is
     * an ongoing connect request when this method is called. This could happen if this object
     * was just recently created, and the connection request initiated in the constructor has not
     * yet terminated; or if the last request initiated through a call to connect() has not yet
     * terminated.
     *
     * @param requester - the Activity wishing to establish a connection with the server; will
     *                  receive callbacks relating to the request
     * @throws MultipleRequestException - if this ServerHelper object already has an ongoing connect
     *                                  request when this method is called.
     */
    public void connect(Connector requester) throws MultipleRequestException {
        if (this.requester != null) {
            throw new MultipleRequestException("Tried to make multiple requests of ServerHelper");
        }
        this.requester = requester;

        ConnectThread thread = new ConnectThread(HOSTNAME, PORT, this);
        thread.start();
    }

    /**
     * Attempt to archive the given game by sending a request to the server. requester will receive
     * callbacks relevant to the request.
     *
     * @param gameID    - the ID of the game that should be archived
     * @param requester - the object that will receive callbacks regarding the outcome of the request
     */
    public void archive(String gameID, ArchiveRequester requester) {
        archiveHelper.archive(gameID, requester);
    }

    /**
     * Attempt to restore the given game by sending a request to the server. requester will receive
     * callbacks relevant to the request.
     *
     * @param gameID    - the ID of the game that should be archived
     * @param requester - the object that will receive callbacks regarding the outcome of the
     *                  request
     */
    public void restore(String gameID, RestoreRequester requester) {
        restoreHelper.restore(gameID, requester);
    }

    /**
     * Attempt to get a list of all open games in the system from the server. requester will receive
     * callbacks relevant to the request.
     *
     * @param requester - the object that will receive callbacks relevant to the request
     * @throws MultipleRequestException - if this ServerHelper already has an ongoing getOpenGames
     *                                  request
     */
    public void getOpenGames(OpenGamesRequester requester) throws MultipleRequestException {
        openGamesHelper.getOpenGames(requester);
    }

    /**
     * Attempt to have the user join the game with the given gameID. requester will receive
     * callbacks relating to the request
     *
     * @param requester - will receive callbacks as to the success or failure of the request
     * @param gameID    - the ID of the game to try and join
     * @param username  - the username of the user who is trying to join the given game
     * @throws MultipleRequestException - thrown if another join game request is ongoing when this method
     *                                  is called
     */
    public void joinGame(JoinGameRequester requester, String gameID, String username) throws MultipleRequestException {
        joinGameHelper.joinGame(requester, gameID, username);
    }

    /**
     * Will send a request to the server to try and create a game with the given ID and open status
     *
     * @param requester - the object that will receive callbacks relevant to the request
     * @param gameID    - the ID of the game to be created
     * @param open      - a boolean representing whether or not the game to be created should be "open",
     *                  meaning that anybody can view and join it
     * @param username  - the username of the user trying to create the game
     * @throws MultipleRequestException - if this object is already handling another createGame
     *                                  request when this method is called
     */
    public void createGame(CreateGameRequester requester, String gameID, boolean open, String username) throws MultipleRequestException {
        createGameHelper.createGame(requester, gameID, open, username);
    }

    /**
     * Initiate a request to load the game with the specified gameID. requester will receive
     * callbacks when the request terminates, either successfully or in error.
     *
     * @param requester - the object that will receive the relevant callback when the request
     *                  terminates
     * @param gameID    - the gameID of the game that should be requested
     * @throws MultipleRequestException - if this object is already handling a load game request
     *                                  when this method is called
     */
    public void loadGame(LoadGameRequester requester, String gameID) throws MultipleRequestException {
        this.loadGameHelper.loadGame(requester, gameID);
    }

    /**
     * Sends a move request to the server, trying to make the given move in the given game
     *
     * @param requester - will receive callbacks once the request has been handled
     * @param gameID    - the game to try and make the move in
     * @param move      - represents the move that this object will try and make with its request
     * @throws MultipleRequestException - if this object is already handling a move request when
     *                                  this method is called
     */
    public void move(MoveRequester requester, String gameID, Move move) throws MultipleRequestException {
        moveHelper.move(requester, gameID, move);
    }

    /**
     * Send a promote request to the server.
     *
     * @param requester - will receive callback once the server has responded to the request
     * @param gameID - the game to issue the promotion in
     * @param piece - the type of piece to promote the pawn into
     * @throws MultipleRequestException - if this object is already handling a promotion request
     * when this method is called.
     */
    public void promote(PromotionRequester requester, String gameID, PieceType.PromotePiece piece) throws MultipleRequestException {
        promotionHelper.promote(requester, gameID, piece);
    }

    /**
     * Submit a draw request to the server.
     *
     * Note: the server condenses both the OFFERING of draws and the ACCEPTING of draw offers into
     * one command. So what this request means depends on whether or not there is an active draw
     * offer from the opponent in the specified game.
     *
     * @param requester - will receive a callback once the server has responded to the request
     * @param gameID - the game in which to offer/accept a draw
     * @throws MultipleRequestException - if this object is already handling a draw request when
     * this method is called
     */
    public void draw(DrawRequester requester, String gameID) throws MultipleRequestException {
        drawHelper.draw(requester, gameID);
    }

    /**
     * Submit a reject request to the server.
     *
     * @param requester - will receive a callback once the server has responded to the request
     * @param gameID - the game in which to reject a draw offer
     * @throws MultipleRequestException - if this object is already handling a reject request when
     * this method is called
     */
    public void reject(RejectRequester requester, String gameID) throws MultipleRequestException {
        rejectHelper.reject(requester, gameID);
    }

    /**
     * Submit a forfeit request to the server.
     *
     * @param requester - will receive a callback once the server has responded to the request
     * @param gameID - the game that the user wants to forfeit
     * @throws MultipleRequestException - if this object is already handling a forfeit request when
     * this method is called
     */
    public void forfeit(ForfeitRequester requester, String gameID) throws MultipleRequestException {
        forfeitHelper.forfeit(requester, gameID);
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

            for (SubHelper helper : helpers) {
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
        switch (msg.what) {
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
