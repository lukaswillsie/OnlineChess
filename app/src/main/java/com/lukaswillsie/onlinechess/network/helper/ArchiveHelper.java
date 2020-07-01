package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.helper.requesters.ArchiveRequester;
import com.lukaswillsie.onlinechess.network.threads.ReturnCodeThread;
import com.lukaswillsie.onlinechess.network.threads.callers.ReturnCodeCaller;

public class ArchiveHelper extends SubHelper implements ReturnCodeCaller{
    private static final String tag = "ArchiveHelper";
    /*
     * Constants this class uses to communicate with itself through Messages
     */
    private static final int SERVER_ERROR = -3;
    private static final int SYSTEM_ERROR = -2;
    private static final int CONNECTION_LOST = -1;
    private static final int ARCHIVE_SUCCESS = 0;

    private ArchiveRequester requester;

    /**
     * Create a new SubHelper as part of the given ServerHelper
     *
     * @param container - the ServerHelper that this object is a part of
     */
    ArchiveHelper(ServerHelper container) {
        super(container);
    }

    public void archive(String gameID, ArchiveRequester requester) {
        ReturnCodeThread thread = new ReturnCodeThread(archiveRequest(gameID), this);
        this.requester = requester;

        thread.setReader(getIn());
        thread.setWriter(getOut());
        thread.start();
    }

    private String archiveRequest(String gameID) {
        return "archive " + gameID;
    }

    @Override
    public void connectionLost() {
        super.connectionLost();

        this.obtainMessage(CONNECTION_LOST).sendToTarget();
    }

    @Override
    public void systemError() {
        super.systemError();

        this.obtainMessage(SYSTEM_ERROR).sendToTarget();
    }

    @Override
    public void onServerReturn(int code) {
        Message msg;
        switch(code) {
            // If the server is telling us we haven't logged in a user, and therefore can't archive
            // a game on anybody's behalf
            case ReturnCodes.NO_USER:
                Log.i(tag, "Server says we haven't logged in a user. Can't archive.");

                // We only make requests in our app if we already have a user logged in. So this
                // shouldn't happen, but if it does we consider it a server error and treat it as
                // such after logging it
                msg = this.obtainMessage(SERVER_ERROR);
                super.serverError();
                break;
            // If the server is telling us we formatted our command incorrectly
            case ReturnCodes.FORMAT_INVALID:
                Log.i(tag, "Server says we formatted our command incorrectly. Can't archive.");

                // This shouldn't happen, because we ensure the format of our requests conforms to
                // protocol. If it does, we've logged the problem for debugging, but at runtime we
                // call it a server error
                msg = this.obtainMessage(SERVER_ERROR);
                super.serverError();
                break;
            // If the server is telling us we encountered an error
            case ReturnCodes.SERVER_ERROR:
                Log.i(tag, "Server says it encountered an error. Can't archive.");

                msg = this.obtainMessage(SERVER_ERROR);
                super.serverError();
                break;
            // If the server is telling us the game was successfully archived
            case ReturnCodes.Archive.SUCCESS:
                Log.i(tag, "Server says archive successful.");

                msg = this.obtainMessage(ARCHIVE_SUCCESS);
                break;
            // If the server is telling us the game does not exist
            case ReturnCodes.Archive.GAME_DOES_NOT_EXIST:
                Log.i(tag, "Server says game does not exist. Can't archive.");

                // This shouldn't happen, because we only allow the user to archive games that the
                // server has TOLD us already exist. If it does, we treat it as a server error at
                // runtime, after having logged the problem for debugging.
                msg = this.obtainMessage(SERVER_ERROR);
                super.serverError();
                break;
            // If the server is telling us that our logged in user is not a player in the game we
            // tried to archive
            case ReturnCodes.Archive.USER_NOT_IN_GAME:
                Log.i(tag, "Server says user isn't in game. Can't archive.");
                // This shouldn't happen, because we only allow the user to archive games that the
                // server has told us the user is a player in. So if it happens, we call it a server
                // error

                msg = this.obtainMessage(SERVER_ERROR);
                super.serverError();
                break;
            // The cases we enumerated above are exhaustive, so any other result falls outside of
            // what the server has said it might return, according to protocol, and is treated by
            // this program as an error server-side.
            default:
                Log.i(tag, "Server returned " + code + ", which is outside of archive request protocol");

                msg = this.obtainMessage(SERVER_ERROR);
                super.serverError();
                break;
        }
        msg.sendToTarget();
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch(msg.what) {
            case SERVER_ERROR:
                requester.serverError();
                break;
            case SYSTEM_ERROR:
                requester.systemError();
                break;
            case CONNECTION_LOST:
                requester.connectionLost();
                break;
            case ARCHIVE_SUCCESS:
                requester.archiveSuccessful();
                break;
        }
    }
}
