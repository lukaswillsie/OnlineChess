package com.lukaswillsie.onlinechess.activities.board;

/**
 * This interface allows ChessManager objects to notify the UI layer when important events in the
 * game occur, for example if the user has made a move and ended their turn. This ensures that any
 * UI elements that aren't within ChessManager's sphere of influence, but have some sort of
 * dependence on the state of the game, can be updated when necessary.
 */
public interface GameListener {
    /**
     * Called after the user has made a move on the board. This move may have ended the user's turn,
     * but need not have.
     */
    void userMoved();
}
