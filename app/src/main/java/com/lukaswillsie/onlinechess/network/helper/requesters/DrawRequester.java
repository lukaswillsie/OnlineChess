package com.lukaswillsie.onlinechess.network.helper.requesters;

/**
 * An instance of this interface must be passed to ServerHelper so that it can receive callbacks
 * relating to a draw request.
 */
public interface DrawRequester extends Requester {
    /**
     * Called if the draw offer/acceptance succeeds
     */
    void drawSuccess();

    /**
     * Called if the game we submitted a draw request in doesn't exist
     */
    void gameDoesNotExist();

    /**
     * Called if the user is not a player in the game we submitted a draw request in
     */
    void userNotInGame();

    /**
     * Called if the user has no opponent in the game we submitted a draw request in
     */
    void noOpponent();

    /**
     * Called if the game we submitted a draw request in is over
     */
    void gameIsOver();

    /**
     * Called if it is not the user's turn in the game we submitted a draw request in
     */
    void notUserTurn();
}
