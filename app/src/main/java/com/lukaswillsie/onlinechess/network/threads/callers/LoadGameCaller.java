package com.lukaswillsie.onlinechess.network.threads.callers;

import com.lukaswillsie.onlinechess.data.UserGame;

import Chess.com.lukaswillsie.chess.Board;

/**
 * This interface defines callback methods an object needs to provide if it wants to use a
 * LoadGameThread to send load game requests to the server
 */
public interface LoadGameCaller extends ThreadCaller {
    /**
     * Called if a server error occurs during a dispatched request
     */
    void serverError();

    /**
     * Called after a load game request finishes successfully. The given Board and UserGame objects
     * together will represent the requested game, and will have been initialized successfully from
     * the data sent over by the server.
     *
     * @param board - a Board object successfully initialized to contain the state of the board in
     *              the given game
     * @param game  - a UserGame object initialized to contain all the high-level information about
     *              the game that was requested
     */
    void success(Board board, UserGame game);

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
