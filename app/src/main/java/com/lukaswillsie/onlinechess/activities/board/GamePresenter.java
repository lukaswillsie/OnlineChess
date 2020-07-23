package com.lukaswillsie.onlinechess.activities.board;

import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.GameData;
import com.lukaswillsie.onlinechess.data.UserGame;

import Chess.com.lukaswillsie.chess.Board;
import Chess.com.lukaswillsie.chess.Colour;
import Chess.com.lukaswillsie.chess.Piece;

/**
 * This class acts essentially just as a wrapper for a UserGame object and a Board object, together
 * holding all the data we need a particular game. This class allows us to access all the Game data
 * we need from one place, rather than two.
 */
public class GamePresenter {
    private UserGame game;
    private Board board;

    GamePresenter(UserGame game, Board board) {
        this.game = game;
        this.board = board;
    }

    /**
     * Returns the Piece object occupying the given spot on the board. Note: the given coordinates
     * are INDEPENDENT OF the orientation of the board being displayed on the screen. That means,
     * regardless of whether the user is playing black or white, (row, column) = (0, 0) is what
     * white would call the bottom left corner. If the user is black, (row, column) = (0, 0) is the
     * top-right square on the screen.
     *
     * Precondition: Row and column must satisfy 0 <= row <=  7 and 0 <= column <= 7. Returns null
     * if this is not the case.
     *
     * @param row - the row of the board to access
     * @param column - the column of the board to access
     * @return The Piece representing the specified spot on the board, or null if the specified
     * square is empty. Also returns null if row and column don't satisfy the precondition.
     */
    public Piece getPiece(int row, int column) {
        if(board.validSquare(row, column)) {
            return board.getPiece(row, column);
        }
        else {
            return null;
        }
    }

    /**
     * Returns the specified piece of information about this game
     *
     * @param data - specifies the piece of information about this game that is desired
     * @return As either an Integer or String, the piece of data requested
     */
    public Object getData(GameData data) {
        return game.getData(data);
    }

    /**
     *
     * @return
     */
    public Colour getUserColour() {
        return game.getUserColour();
    }
}
