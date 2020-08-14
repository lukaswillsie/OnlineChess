package com.lukaswillsie.onlinechess.network.helper.requesters;

import com.lukaswillsie.onlinechess.data.UserGame;

import java.util.List;

/**
 * Defines the callback methods that must be provided to a ServerHelper object being asked to handle
 * a load games request
 */
public interface LoadGamesRequester extends Requester {
    /**
     * Called once the load games request has succeeded, and the data sent over by the server has
     * been processed and packaged into UserGame objects
     *
     * @param games - the list of the user's games that was sent over by the server
     */
    void success(List<UserGame> games);
}
