package com.lukaswillsie.onlinechess.activities.board;

import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.GameData;
import com.lukaswillsie.onlinechess.data.UserGame;

import Chess.com.lukaswillsie.chess.Board;
import Chess.com.lukaswillsie.chess.Colour;
import Chess.com.lukaswillsie.chess.King;
import Chess.com.lukaswillsie.chess.Pair;
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
     * Check if the user can castle, and, if they can, whether the given Move represents the user
     * trying to castle.
     *
     * In our implementation of chess, as in most, the user indicates that they want to castle by
     * moving their king two squares to the left or right. The rook corresponding to the direction
     * of movement (the one that the King is moving towards) then jumps to the opposite side of the
     * King. If the user is able to castle and the given Move represents the user trying to do so,
     * this method returns a Move object that represents the motion undertaken by the Rook as part
     * of the castling operation. That is, the Move object contains the movement from the square
     * the Rook currently occupies to the square that it would end up on after the castle operation
     * is complete.
     *
     * This method returns null if the user cannot castle, or if the given Move does not represent
     * an attempted castle by the user.
     *
     * @param move - the Move to evaluate as a potential castle
     * @return  A Move object representing the motion of the Rook concerned in the castle operation,
     * if the given move is a valid castle move; null otherwise.
     */
    public Move isCastle(Move move) {
        Pair src = move.src;
        Pair dest = move.dest;

        if(board.validSquare(src.first(), src.second()) && board.validSquare(dest.first(), dest.second())) {
            Piece moved = board.getPiece(src.first(), src.second());

            // Here we check if the given move is a castle by checking if: the piece being moved is
            // the player's king, the given move is a valid move for the king, and that the move
            // represents a horizontal displacement of exactly two squares (distinguishes a castle
            // move from a normal king move).
            if(moved instanceof King
            && moved.getMoves().contains(dest)
            && moved.getColour() == getUserColour()
            && (moved.getColumn() - dest.second() == 2 || moved.getColumn() - dest.second() == -2)) {
                // Get the sign of the king's movement (+1 for right, -1 for left)
                int direction = moved.getColumn() - dest.second() / Math.abs(moved.getColumn() - dest.second());

                // Figure out which column the rook being castled with occupies. We asssume, since
                // the King told us that the Move being considered is valid, that there is a Rook
                // that can be castled with in the same row as the King. We just need to determine
                // its column (it could be either to the left of the King or the right)
                int column;
                if(direction > 0) {
                    column = 7;
                }
                else {
                    column = 0;
                }

                Pair rookSrc = new Pair(moved.getRow(), column);
                // After the castle, the Rook stays in the same row as the King, but ends up on the
                // opposite side of him.
                Pair rookDest = new Pair(moved.getRow(), dest.second() - direction);

                return new Move(rookSrc, rookDest);
            }
            else {
                return null;
            }
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
     * Query the colour (black or white) being played by the user
     * @return - The Colour being played by the user in the game being presented by this object
     */
    public Colour getUserColour() {
        return game.getUserColour();
    }
}
