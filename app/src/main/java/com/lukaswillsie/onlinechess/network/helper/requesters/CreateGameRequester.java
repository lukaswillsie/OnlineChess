package com.lukaswillsie.onlinechess.network.helper.requesters;

import com.lukaswillsie.onlinechess.data.UserGame;

/**
 * This interface must be implemented by objects wishing to use a ServerHelper object to make a
 * create game request
 */
public interface CreateGameRequester extends Requester {
    /**
     * Called if a create game request fails due to a server error
     */
    void serverError();

    /**
     * Called if the create game request succeeds. A UserGame object representing the newly-created
     * game is given as an argument.
     *
     * @param game - a UserGame object representing the newly-created game
     */
    void gameCreated(UserGame game);

    /**
     * Called if the game we tried to create already exists, according to the server
     */
    void gameExists();

    /**
     * Called if the gameID given as part of the request is invalidly formatted
     */
    void invalidFormat();
}
