package com.lukaswillsie.onlinechess.network.helper.requesters;

/**
 * This interface defines the callback methods that objects need to implement before they can make
 * a joinGame() request of a ServerHelper object
 */
public interface JoinGameRequester extends Requester {
    /**
     * Called if the join game request is met with success
     */
    void success();

    /**
     * Called if the gameID we gave isn't associated with a game, according to the server
     */
    void gameDoesNotExist();

    /**
     * Called if the gameID we gave is associated with a game that cannot be joined because it is
     * full
     */
    void gameFull();

    /**
     * Called if the game we tried to join can't be joined because the user is already in it
     */
    void userInGame();
}
