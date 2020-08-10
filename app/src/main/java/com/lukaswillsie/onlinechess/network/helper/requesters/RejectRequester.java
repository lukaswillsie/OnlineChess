package com.lukaswillsie.onlinechess.network.helper.requesters;

/**
 * An instance of this interface must be passed to ServerHelper as part of a reject request, so that
 * the instance can receive a callback once the server has responded to the request
 */
public interface RejectRequester extends Requester {
    /**
     * Called if the rejection of the draw offer succeeds
     */
    void rejectSuccess();

    /**
     * Called if the game we rejected a draw offer in doesn't exist
     */
    void gameDoesNotExist();

    /**
     * Called if the user is not a player in the game we rejected a draw offer in
     */
    void userNotInGame();

    /**
     * Called if the user has no opponent in the game we rejected a draw offer in
     */
    void noOpponent();

    /**
     * Called if the game we rejected a draw offer in is over
     */
    void gameIsOver();

    /**
     * Called if it is not the user's turn in the game we rejected a draw offer in
     */
    void notUserTurn();

    /**
     * Called if the server says that there is no draw offer to reject in the game we tried to
     * reject one in
     */
    void noDrawOffer();
}
