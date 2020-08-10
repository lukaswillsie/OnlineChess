package com.lukaswillsie.onlinechess.network.helper.requesters;

/**
 * Objects must pass an instance of this interface to ServerHelper so that they can make forfeit
 * requests of the server and receive a callback once that request has been responded to
 */
public interface ForfeitRequester extends Requester {
    /**
     * Called if the forfeit succeeds
     */
    void forfeitSuccess();

    /**
     * Called if the game we tried to forfeit doesn't exist
     */
    void gameDoesNotExist();

    /**
     * Called if the user is not a player in the game we tried to forfeit
     */
    void userNotInGame();

    /**
     * Called if the user has no opponent in the game we tried to forfeit
     */
    void noOpponent();

    /**
     * Called if the game we tried to forfeit is over
     */
    void gameIsOver();

    /**
     * Called if it is not the user's turn in the game we tried to forfeit
     */
    void notUserTurn();
}
