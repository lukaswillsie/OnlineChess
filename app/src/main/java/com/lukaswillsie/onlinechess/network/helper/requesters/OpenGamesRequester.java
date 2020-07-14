package com.lukaswillsie.onlinechess.network.helper.requesters;

import com.lukaswillsie.onlinechess.data.Game;

import java.util.List;

/**
 * If an object wants to get a list of all open games in the system, it has to implement this
 * interface. This allows it to interact with a ServerHelper and receive callbacks relevant to its
 * request.
 */
public interface OpenGamesRequester extends Requester {
    /**
     * Called when the open games request has been fully processed and a list of Games has been
     * compiled.
     *
     * @param games - the list of all open games in the system sent over by the server
     */
    void openGames(List<Game> games);
}
