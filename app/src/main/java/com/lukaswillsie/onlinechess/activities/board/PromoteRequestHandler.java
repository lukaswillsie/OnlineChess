package com.lukaswillsie.onlinechess.activities.board;

import android.util.Log;

import com.lukaswillsie.onlinechess.network.Server;
import com.lukaswillsie.onlinechess.network.helper.MultipleRequestException;
import com.lukaswillsie.onlinechess.network.helper.requesters.PromotionRequester;

class PromoteRequestHandler implements PromotionRequester {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "PromoteRequestHandler";

    /**
     * Given to this object at creation. Receives callbacks whenever this object is asked to handle
     * a promotion request.
     */
    private PromoteRequestListener listener;

    /**
     * Create a new PromoteRequestHandler to process promote request and give the results to the
     * provided listener.
     *
     * @param listener - will receive callbacks regarding any and all promotion requests submitted
     *                 to this object
     */
    PromoteRequestHandler(PromoteRequestListener listener) {
        this.listener = listener;
    }

    /**
     * Submit a promote request.
     *
     * Note: Only one promote request may be submitted at a time. If you submit a promote request,
     * you must wait until you receive a callback for that request before submitting another one. If
     * you do not, a call to this method will log the error and immediately give a callback to the
     * promotionFailed() method of the listener given to this object at creation.
     *
     * @param piece - specifies which piece we are trying to promote a pawn into
     * @param gameID - specifies which game to make the promotion request in
     */
    void promote(PieceType.PromotePiece piece, String gameID) {
        try {
            Server.getServerHelper().promote(this, gameID,piece);
        } catch (MultipleRequestException e) {
            Log.e(tag, "Submitted multiple promote requests to ServerHelper");
        }
    }

    /**
     * Called if a promote request we submitted to a ServerHelper object succeeds
     */
    @Override
    public void promotionSuccess() {
        listener.promotionSuccess();
    }

    /**
     * Called if a promote request we submitted to a ServerHelper object fails due to a loss of
     * connection with the server
     */
    @Override
    public void connectionLost() {
        listener.promotionFailedConnectionLost();
    }

    /**
     * If our promotion request fails for any of the below reasons, something has gone wrong that we
     * cannot fix. Our app only sends promotion requests to the server if we believe them to be
     * valid, based on the game data that the server itself has given us. So if the server rejects
     * a request, we can't really do anything about it, so for simplicity's sake, we log the
     * server's response when it comes in (down in PromoteHelper), and here we just condense all the
     * possible errors into a single callback to our listener notifying them that the promotion
     * couldn't be made.
     */

    @Override
    public void gameDoesNotExist() {
        listener.promotionFailed();
    }

    @Override
    public void userNotInGame() {
        listener.promotionFailed();
    }

    @Override
    public void noOpponent() {
        listener.promotionFailed();
    }

    @Override
    public void gameIsOver() {
        listener.promotionFailed();
    }

    @Override
    public void notUserTurn() {
        listener.promotionFailed();
    }

    @Override
    public void noPromotionToMake() {
        listener.promotionFailed();
    }

    @Override
    public void charRepInvalid() {
        listener.promotionFailed();
    }

    @Override
    public void serverError() {
        listener.promotionFailed();
    }

    @Override
    public void systemError() {
        listener.promotionFailed();
    }
}
