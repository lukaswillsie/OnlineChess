package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.helper.requesters.ArchiveRequester;
import com.lukaswillsie.onlinechess.network.threads.ReturnCodeThread;
import com.lukaswillsie.onlinechess.network.threads.callers.ReturnCodeCaller;

public class ArchiveHelper extends SubHelper implements ReturnCodeCaller {
    static class ArchiveRequest extends Request {
        private String gameID;

        private ArchiveRequester requester;

        ArchiveRequest(String gameID, ArchiveRequester requester) {
            this.requester = requester;
            this.gameID = gameID;
        }

        public String getGameID() {
            return gameID;
        }

        public ArchiveRequester getRequester() {
            return requester;
        }
    }

    /*
     * Tag used for logging to the console
     */
    private static final String tag = "ArchiveHelper";

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

    private synchronized void requestsChanged() {
        // Grab the first request in our queue
        ArchiveRequest head = (ArchiveRequest) requests.getHead();

        // If the first request in the queue is active, we create a thread to deal with it
        if(head != null && !head.isActive()) {
            ReturnCodeThread thread = new ReturnCodeThread(getRequestText(head.gameID), this);
            head.setActive();

            thread.setReader(getIn());
            thread.setWriter(getOut());
            thread.start();
        }
    }

    public synchronized void archive(ArchiveRequest request) {
        requests.enqueue(request);
        requestsChanged();
    }

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
            // If the server is telling us we encountered an error
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
            // this program as an error server-side.
            default:
                Log.i(tag, "Server returned " + code + ", which is outside of archive request protocol");

                msg = obtainMessage(SERVER_ERROR, request.requester);
                break;
        }
        msg.sendToTarget();
        requestsChanged();
    }

    @Override
    public synchronized void systemError() {
        ArchiveRequest request = (ArchiveRequest) requests.dequeue();

        obtainMessage(SYSTEM_ERROR, request.requester).sendToTarget();
        requestsChanged();
    }

    @Override
    public synchronized void connectionLost() {
        ArchiveRequest request = (ArchiveRequest) requests.dequeue();

        obtainMessage(CONNECTION_LOST, request.requester).sendToTarget();
        requestsChanged();
    }
}
