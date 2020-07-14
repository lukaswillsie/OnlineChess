package com.lukaswillsie.onlinechess.network.threads.callers;

import com.lukaswillsie.onlinechess.data.Game;

import java.util.List;

/**
 * Objects wishing to spawn an OpenGamesThread to get a list of open games from the server need to
 * implement this interface, so that the Thread can give them callbacks relevant to their request
 */
public interface OpenGamesCaller extends ThreadCaller {
    /**
     * Called once the open games request is fully processed.
     *
     * @param games - the list of open games sent over by the server
     */
    void openGames(List<Game> games);

    /**
     * Called if a server error occurs during the course of the request. This represents a couple
     * different possibilities. The server could send over a return code indicating it encountered
     * an error, or it could send over information that doesn't conform to protocol. Either way,
     * something has happened server-side that we can't fix.
     */
    void serverError();
}
