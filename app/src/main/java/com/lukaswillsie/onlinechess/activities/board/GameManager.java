package com.lukaswillsie.onlinechess.activities.board;

import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;

import com.lukaswillsie.onlinechess.data.GameData;

import java.util.ArrayList;
import java.util.List;

import Chess.com.lukaswillsie.chess.Colour;
import Chess.com.lukaswillsie.chess.Pair;
import Chess.com.lukaswillsie.chess.Piece;

/**
 * This class is responsible for managing the state of a game of chess, by processing and responding
 * to actions made by the user. It interacts with the UI at a high-level by using a BoardDisplay
 * object. It accesses data about the game it is managing by using a GamePresenter object.
 */
public class GameManager implements BoardDisplay.DisplayListener {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "GameManager";

    /**
     * The object containing all the data about the game that this object is managing
     */
    private  GamePresenter presenter;

    /**
     * The object managing the display for this GameManager
     */
    private BoardDisplay display;

    /**
     * If the user currently has a piece selected (meaning it's their turn and they've tapped one
     * of their pieces), we have a reference to it here. null otherwise.
     */
    private Piece selected;

    /**
     * Keeps track of whether or not it is currently the user's turn in the game being managed by
     * this object.
     */
    private boolean userTurn;

    /**
     * Create a new GameManager that will manage the game represented by the given GamePresenter
     * and use the given BoardDisplay object to interact with the UI. The BoardDisplay object
     * must already have been built when it is given to this object (.build() must already have been
     * called).
     *
     * @param presenter - a GamePresenter representing the game that this object will manage
     * @param display - the object that this GameManager will use to interact with the UI
     */
    GameManager(GamePresenter presenter, BoardDisplay display) {
        this.presenter = presenter;
        this.display = display;

        this.display.activate(presenter, this);

        // Record whether or not it's the user's turn
        this.userTurn = (Integer)presenter.getData(GameData.TURN) == 1;
    }

    @Override
    public boolean onTouch(int row, int column, MotionEvent event) {
        Log.i(tag, event.toString());
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            // If it's the user's turn, they might be clicking one of their pieces because they want
            // to see where it can move
            if(userTurn) {
                Piece piece = getPiece(row, column);

                if(piece == null) {
                    display.resetSquares();
                    return false;
                }
                else {
                    // We know they're clicking a piece. So they're either clicking the piece they
                    // already have selected, in which case we do nothing because they're just
                    // re-selecting a piece they've already selected; or they're selecting another
                    // one of their pieces; or they're clicking an enemy piece, in which case we
                    // just reset their selection.
                    if(piece == selected) {
                        return true;
                    }
                    else if(piece.getColour() == presenter.getUserColour()) {
                        display.resetSquares();

                        // The user is selecting one of their pieces. So highlight on the screen
                        // the squares that the selected piece can move to.
                        List<Pair> moves = getPiece(row, column).getMoves();
                        display.highlightSquares(convertToScreenCoords(moves));
                        return true;
                    }
                    else {
                        display.resetSquares();
                        return false;
                    }
                }
            }
            else {
                return false;
            }
        }
        else if(event.getAction() == MotionEvent.ACTION_MOVE) {
            display.startDrag(row, column);
            return false;
        }
        else {
            return false;
        }
    }

    @Override
    public boolean onDrag(int row, int column, DragEvent event) {
        return false;
    }

    /**
     * Get the piece occupying the specified square ON THE SCREEN. This method automatically
     * converts from screen coordinates to board coordinates (if the user is playing black, we have
     * to reflect the coordinates across the diagonal before accessing the board). Returns null
     * if the specified square is empty.
     *
     * @param row - the row on the screen occupied by the piece we seek
     * @param column - the column on the screen occupied by the piece we seek
     * @return The piece occupying the specified square on the screen
     */
    private Piece getPiece(int row, int column) {
        if(presenter.getUserColour() == Colour.WHITE) {
            return presenter.getPiece(row, column);
        }
        else {
            return presenter.getPiece(7 - row, 7 - column);
        }
    }

    /**
     * Converts the given list of BOARD squares (where (row,column) = (0,0) always means white's
     * bottom-left corner) into a list of SCREEN squares (where (row,column) = (0,0) means the
     * user's bottom-left corner, which is going to correspond to a different square on the board
     * depending on if the user is black or white).
     *
     * @param squares - the list of board squares to convert
     * @return The given list of board squares converted into screen squares
     */
    private List<Pair> convertToScreenCoords(List<Pair> squares) {
        if(presenter.getUserColour() == Colour.WHITE) {
            return squares;
        }
        else {
            List<Pair> converted = new ArrayList<>();
            for(Pair pair : squares) {
                converted.add(new Pair(7 - pair.first(), 7 - pair.second()));
            }

            return converted;
        }
    }
}
