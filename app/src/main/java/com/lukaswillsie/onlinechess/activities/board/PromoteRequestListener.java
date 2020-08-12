package com.lukaswillsie.onlinechess.activities.board;

/**
 * Implementing this interface allows objects to make promote requests of a PromotionRequestHandler
 * and receive the result in the form of a callback
 */
public interface PromoteRequestListener {
    /**
     * Called if the promotion request succeeds
     */
    void promotionSuccess();

    /**
     * Called if the promotion request fails for any reason other than a loss of connection
     */
    void promotionFailed();

    /**
     * Called if the promotion request fails due to a loss of connection with the server
     */
    void promotionFailedConnectionLost();
}
