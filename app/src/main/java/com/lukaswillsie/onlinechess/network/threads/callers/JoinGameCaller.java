package com.lukaswillsie.onlinechess.network.threads.callers;

import com.lukaswillsie.onlinechess.data.UserGame;

/**
 * To use a JoinGameThread to issue join game requests to the server, objects must implement this
 * interface. This allows them to receive callbacks from the Thread as to the success or failure of
 * the request.
 * <p>
 * There are potentially multiple stages to a join game request. First, the Thread requests that the
 * user join the given game. Next, if that was successful, the Thread asks the server for all the
 * data associated with the given game, that way we can add it to the collection of the user's games
 * that we're storing at runtime.
 */
public interface JoinGameCaller extends ThreadCaller {
    /**
     * Called if an error occurs server-side. This could happen if the server simply returns a code
     * telling us it encountered an error, or if the server sends us something that doesn't conform
     * to protocol.
     */
    void serverError();

    /**
     * Called once the server responds and has told us that the game has been successfully joined.
     * NOTE: This does NOT indicate the end of the request; the data associated with the game that
     * was joined still needs to be received from the server. This is just a stop on the way.
     */
    void gameJoined();

    /**
     * Called if the server has told us that the game we tried to join doesn't exist in its records
     */
    void gameDoesNotExist();

    /**
     * Called if the server has told us that the game we tried to join is already full
     */
    void gameFull();

    /**
     * Called if the server tells us that the user is already in the game we tried to join
     */
    void userAlreadyInGame();

    /**
     * Called once the join game request is complete. That is, the server told us that we
     * successfully joined the game for the user and the server sent us all the information we
     * needed about the game.
     *
     * @param game - a UserGame object for the game we just joined, wrapping up all the important
     *             information about the game
     */
    void joinGameComplete(UserGame game);
}
