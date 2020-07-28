package com.lukaswillsie.onlinechess.network.helper.requesters;

/**
 * Objects wishing to use a ServerHelper object to send move requests to the server must implement
 * this interface. It allows the requester to be notified of the success or failure of the request,
 * and be given a reason in the event of a failure.
 */
public interface MoveRequester extends Requester {
    /**
     * Called if the move is successfully made.
     */
    void moveSuccess();

    /**
     * Called if the game that we tried to make a move in does not exist, according to the server's
     * records
     */
    void gameDoesNotExist();

    /**
     * Called if the user we currently have logged in is not a user in the game we tried to make a
     * move in
     */
    void userNotInGame();

    /**
     * Called if the user has no opponent in the game we tried to make a move in, and hence cannot
     * yet make a move
     */
    void noOpponent();

    /**
     * Called if the game we tried to make a move in is already over
     */
    void gameIsOver();

    /**
     * Called if it is not our user's turn in the game we tried to make a move in
     */
    void notUserTurn();

    /**
     * Called if it is our user's turn, but they need to promote rather than make a normal move
     */
    void needToPromote();

    /**
     * Called if is is our user's turn, but because they need to respond to a draw offer, not
     * because it's their turn to make a normal move
     */
    void mustRespondToDraw();

    /**
     * Called if the move we tried to make is invalid (for example, the piece we tried to move
     * cannot move to the square we tried to move it to)
     */
    void moveInvalid();
}
