package com.lukaswillsie.onlinechess.network.threads.callers;

import com.lukaswillsie.onlinechess.data.UserGame;

import java.util.List;

/**
 * Defines what callback methods an object must provide if they want to spawn a LoadGamesThread to
 * send load games requests to the server for them
 */
public interface LoadGamesCaller extends ThreadCaller {
    /**
     * Called if the request fails because the SERVER encounters an error. This could be the case if
     * the server simply tells us it encountered an error through a return code, or if the server
     * sends us a response that doesn't make sense.
     */
    void serverError();

    /**
     * Called once a load games request has finished successfully.
     *
     * @param games - the list of the user's games sent over by the server
     */
    void success(List<UserGame> games);
}
