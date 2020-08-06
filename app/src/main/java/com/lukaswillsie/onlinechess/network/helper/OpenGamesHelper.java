package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.network.helper.requesters.OpenGamesRequester;
import com.lukaswillsie.onlinechess.network.threads.OpenGamesThread;
import com.lukaswillsie.onlinechess.network.threads.callers.OpenGamesCaller;

import java.util.List;

/**
 * Handles open games requests for ServerHelper objects
 */
public class OpenGamesHelper extends SubHelper implements OpenGamesCaller {
    /**
     * Ints that this class uses to communicate with itself through Messages
     */
    private static final int SERVER_ERROR = -3;
    private static final int SYSTEM_ERROR = -2;
    private static final int CONNECTION_LOST = -1;
    private static final int SUCCESS = 0;
    /**
     * The object that made the currently active request. null if there is no currently active
     * request.
     */
    private OpenGamesRequester requester;

    /**
     * Create a new SubHelper as part of the given ServerHelper
     *
     * @param container - the ServerHelper that this object is a part of
     */
    OpenGamesHelper(ServerHelper container) {
        super(container);
    }

    /**
     * Start a Thread that will send an open games request to the server. When this object is
     * given notifications regarding the state of the request, they will be passed on to the given
     * requester
     *
     * @param requester - will receive callbacks relevant to the open games request
     * @throws MultipleRequestException
     */
    void getOpenGames(OpenGamesRequester requester) throws MultipleRequestException {
        if (this.requester != null) {
            throw new MultipleRequestException("Tried to make multiple requests of OpenGamesHelper");
        }
        this.requester = requester;

        OpenGamesThread thread = new OpenGamesThread(this, getOut(), getIn());
        thread.start();
    }

    /**
     * Called on successful completion of an OpenGameThread's request
     *
     * @param games - the list of open games sent over by the server
     */
    @Override
    public void openGames(List<Game> games) {
        this.obtainMessage(SUCCESS, games).sendToTarget();
    }

    /**
     * Called if an open games request is met with a server error
     */
    @Override
    public void serverError() {
        this.obtainMessage(SERVER_ERROR).sendToTarget();
    }

    /**
     * Called if an open games request is met with a server error
     */
    @Override
    public void systemError() {
        this.obtainMessage(SYSTEM_ERROR).sendToTarget();
    }

    /**
     * Called if an open games request is stymied by the discovery that the connection with the
     * server has been lost
     */
    @Override
    public void connectionLost() {
        this.obtainMessage(CONNECTION_LOST).sendToTarget();
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case SERVER_ERROR:
                requester.serverError();

                // Allows us to accept another request
                this.requester = null;
                break;
            case SYSTEM_ERROR:
                requester.systemError();

                // Allows us to accept another request
                this.requester = null;
                break;
            case CONNECTION_LOST:
                requester.connectionLost();

                // Allows us to accept another request
                this.requester = null;
                break;
            case SUCCESS:
                requester.openGames((List<Game>) msg.obj);

                // Allows us to accept another request
                this.requester = null;
        }
    }
}
