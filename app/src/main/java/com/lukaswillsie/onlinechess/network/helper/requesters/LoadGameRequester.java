package com.lukaswillsie.onlinechess.network.helper.requesters;

import Chess.com.lukaswillsie.chess.Board;

/**
 * This interface defines the callbacks that an object must provide if it wants to make a load game
 * request of a ServerHelper object
 */
public interface LoadGameRequester extends Requester {
    /**
     * Called after a load game request finishes successfully. The given Board object will represent
     * the requested game, and will have been initialized successfully from the data sent over by
     * the server.
     *
     * @param board - a Board object successfully initialized to hold all data associated with the
     *              requested game
     */
    void success(Board board);

    /**
     * Called if the server responds to the request by saying the supplied gameID is not associated
     * with a game in its system
     */
    void gameDoesNotExist();

    /**
     * Called if the server responds to the request by saying that the user the app currently has
     * logged in is not a player in the game whose data was requested
     */
    void userNotInGame();
}
