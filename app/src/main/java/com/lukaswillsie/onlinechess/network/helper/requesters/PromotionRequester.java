package com.lukaswillsie.onlinechess.network.helper.requesters;

/**
 * This interface allows classes to make promotion requests of a ServerHelper and receive callbacks
 * when that request terminates.
 */
public interface PromotionRequester extends Requester {
    /**
     * Called if the promotion succeeds
     */
    void promotionSuccess();

    /**
     * Called if the game we tried to promote in doesn't exist
     */
    void gameDoesNotExist();

    /**
     * Called if our logged-in user isn't in the game we tried to promote in
     */
    void userNotInGame();

    /**
     * Called if our logged-in user doesn't have an opponent in the game we tried to promote in
     */
    void noOpponent();

    /**
     * Called if the game we tried to promote in is already over
     */
    void gameIsOver();

    /**
     * Called if it isn't the logged-in user's turn in the game we tried to promote in
     */
    void notUserTurn();

    /**
     * Called if no ally pawn needs to be promoted in the game we tried to promote in
     */
    void noPromotionToMake();

    /**
     * Called if the character representation we provide as part of our command was invalid
     */
    void charRepInvalid();
}
