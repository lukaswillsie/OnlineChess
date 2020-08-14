package com.lukaswillsie.onlinechess.network.helper.requesters;

import com.lukaswillsie.onlinechess.data.UserGame;

/**
 * Defines what callback methods an object must implement before they're allowed to make game data
 * requests of ServerHelper objects.
 */
public interface GameDataRequester extends Requester {
    /**
     * Called once the game data request has terminated successfully
     *
     * @param game - the UserGame object that has been built from the data sent over by the server
     */
    void success(UserGame game);

    /**
     * Called if the server responds to the request by saying that the game whose data was requested
     * does not exist
     */
    void gameDoesNotExist();

    /**
     * Called if the server responds to the request by saying that the logged-in user is not a
     * player in the game whose data was requested
     */
    void userNotInGame();
}
