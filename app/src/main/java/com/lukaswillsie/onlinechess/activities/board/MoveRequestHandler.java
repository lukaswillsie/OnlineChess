package com.lukaswillsie.onlinechess.activities.board;

import android.content.Context;
import android.util.Log;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.network.helper.requesters.MoveRequester;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;

/**
 * Submits move requests to the server and handles the result for a MoveRequestListener object.
 */
public class MoveRequestHandler implements MoveRequester {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "MoveRequestHandler";

    /**
     * Will receive callbacks from this object when a move request is submitted
     */
    private MoveRequestListener listener;

    /**
     * The context in which this object is operating
     */
    private Context context;

    /**
     * Create a new MoveRequestHandler who will give callbacks to the given listener, operating in
     * the given Context
     * @param listener - will receive callbacks whenever a move request is submitted
     * @param context - the Context in which this object is operating
     */
    MoveRequestHandler(MoveRequestListener listener, Context context) {
        this.listener = listener;
        this.context = context;
    }

    /**
     * Submit a move request.
     *
     * @param move - the move to be made
     * @param gameID - the game to try and make the given move in
     */
    void submitMove(Move move, String gameID) {
        try {
            ((ChessApplication)context.getApplicationContext()).getServerHelper().move(this, gameID, move);
        } catch (MultipleRequestException e) {
            Log.i(tag, "Submitted multiple move requests to ServerHelper");
            listener.moveFailed();
        }
    }

    /**
     * Called if the server confirms that our move request was successful.
     *
     * @param promotionNeeded - true if a promotion is needed now that the move has been made (i.e.
     */
    @Override
    public void moveSuccess(boolean promotionNeeded) {
        listener.moveSuccess(promotionNeeded);
    }

    /**
     * Various callbacks that we might receive if there's an error with our move request. We
     * condense all of these outcomes into a single call to listener.moveFailed() (except for
     * connectionLost() errors)for efficiency, and because we expect the object making the requests
     * to be making valid requests. So if a request fails, we assume that there is a problem with
     * the server. Regardless, though, we have no way to fix problems on the fly if moves we think
     * are fine get rejected by the server, so at runtime we treat them all equally.
     */

    @Override
    public void gameDoesNotExist() {
        listener.moveFailed();
    }

    @Override
    public void userNotInGame() {
        listener.moveFailed();
    }

    @Override
    public void noOpponent() {
        listener.moveFailed();
    }

    @Override
    public void gameIsOver() {
        listener.moveFailed();
    }

    @Override
    public void notUserTurn() {
        listener.moveFailed();
    }

    @Override
    public void needToPromote() {
        listener.moveFailed();
    }

    @Override
    public void mustRespondToDraw() {
        listener.moveFailed();
    }

    @Override
    public void moveInvalid() {
        listener.moveFailed();
    }

    @Override
    public void connectionLost() {
        listener.connectionLost();
    }

    @Override
    public void serverError() {
        listener.moveFailed();
    }

    @Override
    public void systemError() {
        listener.moveFailed();
    }
}
