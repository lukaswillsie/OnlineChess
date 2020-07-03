package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.helper.requesters.ArchiveRequester;
import com.lukaswillsie.onlinechess.network.threads.ReturnCodeThread;
import com.lukaswillsie.onlinechess.network.threads.callers.ReturnCodeCaller;

/**
 * Handles archive requests on behalf of ServerHelper objects. Knows how to ask the server to mark
 * a given game as archived and process the server's response.
 */
public class ArchiveHelper extends SubHelper implements ReturnCodeCaller {
    /**
     * An object representing an archive request. Is a wrapper for data associated with a request,
     * like what object is making the request, and what game they want archived. We use this object
     * primarily because we want to be able to accept multiple archive requests at once, and keeping
     * a queue of ArchiveRequest objects is the most straightforward way of doing this.
     */
    static class ArchiveRequest extends Request {
        /**
         * The gameID associated with the request
         */
        private String gameID;

        /**
         * The object initiating the request, and who will receive the relevant callbacks
         */
        private ArchiveRequester requester;

        /**
         * Create a new ArchiveRequest object.
         *
         * @param gameID - the ID of the game that should be archived as part of the request
         * @param requester - the object making the request, who will receive all relevant callbacks
         */
        ArchiveRequest(String gameID, ArchiveRequester requester) {
            this.requester = requester;
            this.gameID = gameID;
        }
    }

    /*
     * Tag used for logging to the console
     */
    private static final String tag = "ArchiveHelper";

    /**
     * A queue holding all the requests we have yet to finish processing
     */
    private RequestQueue requests = new RequestQueue();

    /*
     * Constants this class uses to communicate with itself through Messages
     */
    private static final int SERVER_ERROR = -3;
    private static final int SYSTEM_ERROR = -2;
    private static final int CONNECTION_LOST = -1;
    private static final int ARCHIVE_SUCCESS = 0;

    /**
     * Create a new SubHelper as part of the given ServerHelper
     *
     * @param container - the ServerHelper that this object is a part of
     */
    ArchiveHelper(ServerHelper container) {
        super(container);
    }

    /**
     * Notifies this object that the request queue has changed; should be called whenever a request
     * has been enqueued or a request has been dequeued.
     *
     * Will check if the head of the queue is inactive, meaning that we finished processing the last
     * request or just added a request to a previously empty queue. Either way, we start to process
     * the new head of the queue.
     */
    private synchronized void requestsChanged() {
        // Grab the first request in our queue
        ArchiveRequest head = (ArchiveRequest) requests.getHead();

        // If the first request in the queue is not active, that is not being processed, we create a
        // thread to deal with it, and set it as active
        if(head != null && !head.isActive()) {
            ReturnCodeThread thread = new ReturnCodeThread(getRequestText(head.gameID), this);
            head.setActive();

            thread.setReader(getIn());
            thread.setWriter(getOut());
            thread.start();
        }
    }

    /**
     * Attempt the archive the given game, and give callbacks to the given requester
     * @param gameID - the ID of the game to try and archive
     * @param requester - the object that will receive callbacks as to the outcome of the request
     */
    public synchronized void archive(String gameID, ArchiveRequester requester) {
        requests.enqueue(new ArchiveRequest(gameID, requester));
        requestsChanged();
    }

    /**
     * Returns the String that needs to be sent to the server to issue an archive request.
     *
     * @param gameID - the ID of the game that we want to archive
     * @return a String ready to be sent to the server as a valid archive request
     */
    private String getRequestText(String gameID) {
        return "archive " + gameID;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch(msg.what) {
            case SERVER_ERROR:
                ((ArchiveRequester) msg.obj).serverError();
                break;
            case SYSTEM_ERROR:
                ((ArchiveRequester) msg.obj).systemError();
                break;
            case CONNECTION_LOST:
                ((ArchiveRequester) msg.obj).connectionLost();
                break;
            case ARCHIVE_SUCCESS:
                ((ArchiveRequester) msg.obj).archiveSuccessful();
                break;
        }
    }

    /**
     * Called by ReturnCodeThread after it receives a response from the server
     *
     * @param code - the code returned by the server
     */
    @Override
    public synchronized void onServerReturn(int code) {
        ArchiveRequest request = (ArchiveRequest) requests.dequeue();

        Message msg;
        switch(code) {
            // If the server is telling us we haven't logged in a user, and therefore can't archive
            // a game on anybody's behalf
            case ReturnCodes.NO_USER:
                Log.i(tag, "Server says we haven't logged in a user. Can't archive.");

                // We only make requests in our app if we already have a user logged in. So this
                // shouldn't happen, but if it does we consider it a server error and treat it as
                // such after logging it
                msg = obtainMessage(SERVER_ERROR, request.requester);
                break;
            // If the server is telling us we formatted our command incorrectly
            case ReturnCodes.FORMAT_INVALID:
                Log.i(tag, "Server says we formatted our command incorrectly. Can't archive.");

                // This shouldn't happen, because we ensure the format of our requests conforms to
                // protocol. If it does, we've logged the problem for debugging, but at runtime we
                // call it a server error
                msg = obtainMessage(SERVER_ERROR, request.requester);
                break;
            // If the server is telling us it encountered an error
            case ReturnCodes.SERVER_ERROR:
                Log.i(tag, "Server says it encountered an error. Can't archive.");

                msg = obtainMessage(SERVER_ERROR, request.requester);
                break;
            // If the server is telling us the game was successfully archived
            case ReturnCodes.Archive.SUCCESS:
                Log.i(tag, "Server says archive successful.");

                msg = obtainMessage(ARCHIVE_SUCCESS, request.requester);
                break;
            // If the server is telling us the game does not exist
            case ReturnCodes.Archive.GAME_DOES_NOT_EXIST:
                Log.i(tag, "Server says game does not exist. Can't archive.");

                // This shouldn't happen, because we only allow the user to archive games that the
                // server has TOLD us already exist. If it does, we treat it as a server error at
                // runtime, after having logged the problem for debugging.
                msg = obtainMessage(SERVER_ERROR, request.requester);
                break;
            // If the server is telling us that our logged in user is not a player in the game we
            // tried to archive
            case ReturnCodes.Archive.USER_NOT_IN_GAME:
                Log.i(tag, "Server says user isn't in game. Can't archive.");

                // This shouldn't happen, because we only allow the user to archive games that the
                // server has told us the user is a player in. So if it happens, we call it a server
                // error
                msg = obtainMessage(SERVER_ERROR, request.requester);
                break;
            // The cases we enumerated above are exhaustive, so any other result falls outside of
            // what the server has said it might return, according to protocol, and is treated by
            // this program as an error server-side
            default:
                Log.i(tag, "Server returned " + code + ", which is outside of archive request protocol");

                msg = obtainMessage(SERVER_ERROR, request.requester);
                break;
        }
        requestsChanged();
        msg.sendToTarget();
    }

    /**
     * Called by ReturnCodeThread if it encounters a system error in the course of its work
     */
    @Override
    public synchronized void systemError() {
        ArchiveRequest request = (ArchiveRequest) requests.dequeue();

        obtainMessage(SYSTEM_ERROR, request.requester).sendToTarget();
        requestsChanged();
    }

    /**
     * Called be ReturnCodeThread if it discovers the connection to the server to have been lost
     * in the midst of its work
     */
    @Override
    public synchronized void connectionLost() {
        ArchiveRequest request = (ArchiveRequest) requests.dequeue();

        obtainMessage(CONNECTION_LOST, request.requester).sendToTarget();
        requestsChanged();
    }
}
