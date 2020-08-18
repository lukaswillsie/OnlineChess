package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.helper.requesters.LoadGamesRequester;
import com.lukaswillsie.onlinechess.network.threads.LoadGamesThread;
import com.lukaswillsie.onlinechess.network.threads.callers.LoadGamesCaller;

import java.util.List;

/**
 * Handles load game requests for ServerHelper
 */
public class LoadGamesHelper extends SubHelper implements LoadGamesCaller {
    /**
     * Constants used by this object to send Messages to the UI thread.
     */
    private static final int CONNECTION_LOST = -3;
    private static final int SYSTEM_ERROR = -2;
    private static final int SERVER_ERROR = -1;
    private static final int SUCCESS = 0;
    /**
     * Will receive callbacks from this object when the request currently being handled by this
     * object either fails or succeeds. null if there is not active request.
     */
    private LoadGamesRequester requester;

    /**
     * Create a new LoadGamesHelper as part of the given ServerHelper
     *
     * @param container - the ServerHelper that this object is a part of
     */
    LoadGamesHelper(ServerHelper container) {
        super(container);
    }

    /**
     * Sends a load games request to the server.
     *
     * @param username  - the username of the user currently logged in to the app; i.e. the user
     *                  whose games we are loading
     * @param requester - the object that will receive callbacks from us when the request either
     *                  succeeds or fails
     * @throws MultipleRequestException - if this object is already handling a load games request
     *                                  when this method is called
     */
    void loadGames(String username, LoadGamesRequester requester) throws MultipleRequestException {
        if (this.requester != null) {
            throw new MultipleRequestException("Attempted to submit multiple load games requests to ServerHelper");
        }
        this.requester = requester;

        LoadGamesThread thread = new LoadGamesThread(username, this, getOut(), getIn());
        thread.start();
    }

    /**
     * Called by LoadGamesThread if our request fails due to a server error
     */
    @Override
    public void serverError() {
        this.obtainMessage(SERVER_ERROR).sendToTarget();
    }

    /**
     * Called by LoadGamesThread if our request fails due to a system error
     */
    @Override
    public void systemError() {
        this.obtainMessage(SYSTEM_ERROR).sendToTarget();
    }

    /**
     * Called by LoadGamesThread if our request fails due to a loss of connection with the server
     */
    @Override
    public void connectionLost() {
        this.obtainMessage(CONNECTION_LOST).sendToTarget();
    }

    /**
     * Called by LoadGamesThread if our request succeeds.
     *
     * @param games - the list of the user's games that have been sent over by the server
     */
    @Override
    public void success(List<UserGame> games) {
        this.obtainMessage(SUCCESS, games).sendToTarget();
    }

    /**
     * We use this method so that we can give callbacks on the UI thread (as opposed to our worker
     * thread). This ensures that any UI events that have to occur in response to our callback will
     * run on the UI thread. Otherwise, we'd get an exception.
     *
     * @param msg - contains information about the callback to be given
     */
    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case CONNECTION_LOST:
                requester.connectionLost();

                // Allows us to accept another request
                this.requester = null;
                break;
            case SYSTEM_ERROR:
                requester.systemError();

                // Allows us to accept another request
                this.requester = null;
                break;
            case SERVER_ERROR:
                requester.serverError();

                // Allows us to accept another request
                this.requester = null;
                break;
            case SUCCESS:
                requester.success((List<UserGame>) msg.obj);

                // Allows us to accept another request
                this.requester = null;
                break;
        }
    }
}
