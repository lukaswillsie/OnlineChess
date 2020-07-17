package com.lukaswillsie.onlinechess.network.helper.requesters;

import com.lukaswillsie.onlinechess.data.UserGame;

/**
 * This interface defines the callback methods that objects need to implement before they can make
 * a joinGame() request of a ServerHelper object.
 *
 * There are potentially multiple stages to a join game request. First, the Thread requests that the
 * user join the given game. Next, if that was successful, the Thread asks the server for the data
 * associated with the given game, that way we can add it to the collection of the user's games
 * that we're storing at runtime.
 */
public interface JoinGameRequester extends Requester {
    /**
     * Called if the join game request is met with success.
     * NOTE: This does NOT indicate the end of the request; the data associated with the game that
     * was joined still needs to be received from the server. This is just a stop on the way.
     */
    void gameJoined();

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
    void userAlreadyInGame();

    /**
     * Called after a join game request has completed successfully. A UserGame object representing
     * the game that was joined is given as an argument for later usage, if needed.
     *
     * @param game - an object containing all the necessary information about the game that was
     * joined
     */
    void joinGameComplete(UserGame game);
}
