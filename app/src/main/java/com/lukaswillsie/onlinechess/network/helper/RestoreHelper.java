package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.helper.requesters.RestoreRequester;
import com.lukaswillsie.onlinechess.network.threads.ReturnCodeThread;
import com.lukaswillsie.onlinechess.network.threads.callers.ReturnCodeCaller;

public class RestoreHelper extends SubHelper implements ReturnCodeCaller {
    /*
     * Tag used for logging to the console
     */
    private static final String tag = "RestoreHelper";
    /*
     * Constants this class uses to communicate with itself through Messages
     */
    private static final int SERVER_ERROR = -3;
    private static final int SYSTEM_ERROR = -2;
    private static final int CONNECTION_LOST = -1;
    private static final int RESTORE_SUCCESS = 0;
    /**
     * A queue of all requests we have yet to process
     */
    private RequestQueue requests = new RequestQueue();

    /**
     * Create a new SubHelper as part of the given ServerHelper
     *
     * @param container - the ServerHelper that this object is a part of
     */
    RestoreHelper(ServerHelper container) {
        super(container);
    }

    /**
     * Notifies this object that the request queue has changed; should be called whenever a request
     * has been enqueued or a request has been dequeued.
     * <p>
     * Will check if the head of the queue is inactive, meaning that we finished processing the last
     * request or just added a request to a previously empty queue. Either way, we start to process
     * the new head of the queue.
     */
    private synchronized void requestsChanged() {
        // Grab the first request in our queue
        RestoreRequest head = (RestoreRequest) requests.getHead();

        // If the first request in the queue is not active, that is, not being processed, we create
        // a thread to deal with it, and set it as active
        if (head != null && !head.isActive()) {
            ReturnCodeThread thread = new ReturnCodeThread(getRequestText(head.gameID), this);
            head.setActive();

            thread.setReader(getIn());
            thread.setWriter(getOut());
            thread.start();
        }
    }

    /**
     * Issues a request to the server attempting to restore the given game. requester will receive
     * callbacks as to the outcome of the request.
     *
     * @param gameID    - the ID of the game to try and restore
     * @param requester - the object that will receive callbacks as to the outcome of the request.
     */
    synchronized void restore(String gameID, RestoreRequester requester) {
        this.requests.enqueue(new RestoreRequest(gameID, requester));
        this.requestsChanged();
    }

    /**
     * Returns the String that needs to be sent to the server to issue a restore request.
     *
     * @param gameID - the ID of the game that we want to restore
     * @return a String ready to be sent to the server as a valid restore request
     */
    private String getRequestText(String gameID) {
        return "restore " + gameID;
    }

    /**
     * Called by ReturnCodeThread after it receives a response from the server
     *
     * @param code - the code returned by the server
     */
    @Override
    public void onServerReturn(int code) {
        RestoreRequest request = (RestoreRequest) this.requests.dequeue();

        Message msg;
        switch (code) {
            // If the server is telling us we haven't logged in a user yet
            case ReturnCodes.NO_USER:
                Log.i(tag, "Server says we haven't logged in a user. Can't restore.");

                // We only make requests in our app if we already have a user logged in. So this
                // shouldn't happen, but if it does we consider it a server error and treat it as
                // such after logging it
                msg = obtainMessage(SERVER_ERROR, request.requester);
                break;
            // If the server is telling us we formatted our command incorrectly
            case ReturnCodes.FORMAT_INVALID:
                Log.i(tag, "Server says we formatted our command incorrectly. Can't restore.");

                // This shouldn't happen, because we ensure the format of our requests conforms to
                // protocol. If it does, we've logged the problem for debugging, but at runtime we
                // call it a server error
                msg = obtainMessage(SERVER_ERROR, request.requester);
                break;
            // If the server is telling us it encountered an error
            case ReturnCodes.SERVER_ERROR:
                Log.i(tag, "Server says it encountered an error. Can't restore.");

                msg = obtainMessage(SERVER_ERROR, request.requester);
                break;
            // If the server is telling us the game was successfully restored
            case ReturnCodes.Restore.SUCCESS:
                Log.i(tag, "Server says restore successful.");

                msg = obtainMessage(RESTORE_SUCCESS, request.requester);
                break;
            // If the server is telling us the game does not exist
            case ReturnCodes.Restore.GAME_DOES_NOT_EXIST:
                Log.i(tag, "Server says game does not exist. Can't restore.");

                // This shouldn't happen, because we only allow the user to restore games that the
                // server has TOLD us already exist. If it does, we treat it as a server error at
                // runtime, after having logged the problem for debugging.
                msg = obtainMessage(SERVER_ERROR, request.requester);
                break;
            // If the server is telling us that our logged in user is not a player in the game we
            // tried to restore
            case ReturnCodes.Restore.USER_NOT_IN_GAME:
                Log.i(tag, "Server says user isn't in game. Can't restore.");

                // This shouldn't happen, because we only allow the user to restore games that the
                // server has told us the user is a player in. So if it happens, we call it a server
                // error
                msg = obtainMessage(SERVER_ERROR, request.requester);
                break;
            // The cases we enumerated above are exhaustive, so any other result falls outside of
            // what the server has said it might return, according to protocol, and is treated by
            // this program as an error server-side
            default:
                Log.i(tag, "Server returned " + code + ", which is outside of restore request protocol");

                msg = obtainMessage(SERVER_ERROR, request.requester);
                break;
        }

        requestsChanged();
        msg.sendToTarget();
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case SERVER_ERROR:
                ((RestoreRequester) msg.obj).serverError();
                break;
            case SYSTEM_ERROR:
                ((RestoreRequester) msg.obj).systemError();
                break;
            case CONNECTION_LOST:
                ((RestoreRequester) msg.obj).connectionLost();
                break;
            case RESTORE_SUCCESS:
                ((RestoreRequester) msg.obj).restoreSuccessful();
                break;
        }
    }

    /**
     * Called by ReturnCodeThread if it encounters a system error in the course of its work
     */
    @Override
    public void systemError() {
        RestoreRequest request = (RestoreRequest) this.requests.dequeue();

        obtainMessage(SYSTEM_ERROR, request.requester).sendToTarget();
        requestsChanged();
    }

    /**
     * Called be ReturnCodeThread if it discovers the connection to the server to have been lost
     * in the midst of its work
     */
    @Override
    public void connectionLost() {
        RestoreRequest request = (RestoreRequest) this.requests.dequeue();

        obtainMessage(CONNECTION_LOST, request.requester).sendToTarget();
        requestsChanged();
    }

    /**
     * Represents a restore request in code. IS es
     */
    static class RestoreRequest extends Request {
        private String gameID;
        private RestoreRequester requester;

        RestoreRequest(String gameID, RestoreRequester requester) {
            this.gameID = gameID;
            this.requester = requester;
        }
    }
}
