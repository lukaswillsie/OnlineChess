package com.lukaswillsie.onlinechess.network.threads.callers;

import com.lukaswillsie.onlinechess.data.UserGame;

/**
 * This interface defines what callback methods an object must provide in order to be able to
 * receive callbacks from a GameDataThread object relating to a game data request.
 */
public interface GameDataCaller extends ThreadCaller {
    /**
     * Called if the request is met with an error, server-side
     */
    void serverError();

    /**
     * Called if the request succeeds.
     *
     * @param game - represents the game and all of its data as sent over by the server
     */
    void success(UserGame game);

    /**
     * Called if the request fails because the server says the game we asked for doesn't exist
     */
    void gameDoesNotExist();

    /**
     * Called if the request fails because the server says the user we have logged in is not in the
     * game whose data we requested
     */
    void userNotInGame();
}
