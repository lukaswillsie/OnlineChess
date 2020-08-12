package com.lukaswillsie.onlinechess.activities.board;

import android.util.Log;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.data.GameData;
import com.lukaswillsie.onlinechess.data.UserGame;

import Chess.com.lukaswillsie.chess.Bishop;
import Chess.com.lukaswillsie.chess.Board;
import Chess.com.lukaswillsie.chess.Colour;
import Chess.com.lukaswillsie.chess.King;
import Chess.com.lukaswillsie.chess.Knight;
import Chess.com.lukaswillsie.chess.Pair;
import Chess.com.lukaswillsie.chess.Pawn;
import Chess.com.lukaswillsie.chess.Piece;
import Chess.com.lukaswillsie.chess.Queen;
import Chess.com.lukaswillsie.chess.Rook;

/**
 * This class acts essentially just as a wrapper for a UserGame object and a Board object, together
 * holding all the data we need a particular game. This class allows us to access all the Game data
 * we need from one place, rather than two.
 */
public class GamePresenter {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "GamePresenter";

    private UserGame game;
    private Board board;

    GamePresenter(UserGame game, Board board) {
        this.game = game;
        this.board = board;
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
     * Assign the given value to the specified field in this game.
     *
     * @param data - specifies which field to overwrite
     * @param val  - the value to be assigned to the specified field
     */
    public void setData(GameData data, Object val) {
        game.setData(data, val);
    }

    /**
     * Checks whether or not this game is OVER, meaning that either a player has won or the game is
     * drawn
     *
     * @return true if and only if one of the players in this game has won, or the game has already
     * ended by draw
     */
    public boolean gameIsOver() {
        return (Integer) game.getData(GameData.USER_WON) == 1 ||
                (Integer) game.getData(GameData.USER_LOST) == 1 ||
                (Integer) game.getData(GameData.DRAWN) == 1;

    }

    /**
     * Returns the Piece object occupying the given spot on the board. Note: the given coordinates
     * are INDEPENDENT OF the orientation of the board being displayed on the screen. That means,
     * regardless of whether the user is playing black or white, (row, column) = (0, 0) is what
     * white would call the bottom left corner. If the user is black, (row, column) = (0, 0) is the
     * top-right square on the screen.
     * <p>
     * Precondition: Row and column must satisfy 0 <= row <=  7 and 0 <= column <= 7. Returns null
     * if this is not the case.
     *
     * @param row    - the row of the board to access
     * @param column - the column of the board to access
     * @return The Piece representing the specified spot on the board, or null if the specified
     * square is empty. Also returns null if row and column don't satisfy the precondition.
     */
    public Piece getPiece(int row, int column) {
        if (board.validSquare(row, column)) {
            return board.getPiece(row, column);
        } else {
            return null;
        }
    }

    /**
     * Create a dummy piece with the given colour. A dummy piece is a piece that is not actually on
     * the board but thinks it is. We only need this method for one reason: so that we can display
     * pieces on the screen that aren't on the board yet. For example, we use this when handling
     * promotions.
     *
     * @param type - specifies what piece should be created
     * @param colour - the Colour of the dummy piece to be created
     * @return A dummy piece of the given type and Colour
     */
    public Piece createDummyPiece(@NonNull PieceType type, Colour colour) {
        switch (type) {
            case PAWN:
                return new Pawn(0,0, colour, board);
            case ROOK:
                return new Rook(0,0, colour, board);
            case KNIGHT:
                return new Knight(0,0, colour, board);
            case BISHOP:
                return new Bishop(0,0, colour, board);
            case QUEEN:
                return new Queen(0,0, colour, board);
            case KING:
                return new King(0,0, colour, board);
        }

        // This will never be reached because the above switch is exhaustive
        return null;
    }

    /**
     * Identical to getPiece(int row, int column), but accepts a Pair object instead of two
     * integers. The restrictions on the coordinates contained in the Pair are identical to those
     * imposed on row and column in getPiece(int row, int column).
     *
     * @param pair - the Pair specifying the square on the board to access
     * @return The Piece representing the specified spot on the board, or null if the specified
     * square is empty. Also returns null if row and column don't satisfy the precondition.
     */
    public Piece getPiece(Pair pair) {
        return getPiece(pair.first(), pair.second());
    }

    /**
     * Check if the user has checkmated their opponent in this game.
     *
     * @return - true if and only if the colour being played by the user has checkmated the other
     * colour in the game represented by this object
     */
    public boolean isCheckmate() {
        return board.isCheckmate(getUserColour() == Colour.WHITE ? Colour.BLACK : Colour.WHITE);
    }

    /**
     * Check if the game is currently in stalemate.
     *
     * @return true if and only if this game is in stalemate
     */
    public boolean isStalemate() {
        return board.isStalemate();
    }

    /**
     * Check if the user can castle, and, if they can, whether the given Move represents the user
     * trying to castle.
     * <p>
     * In our implementation of chess, as in most, the user indicates that they want to castle by
     * moving their king two squares to the left or right. The rook corresponding to the direction
     * of movement (the one that the King is moving towards) then jumps to the opposite side of the
     * King. If the user is able to castle and the given Move represents the user trying to do so,
     * this method returns a Move object that represents the motion undertaken by the Rook as part
     * of the castling operation. That is, the Move object contains the movement from the square
     * the Rook currently occupies to the square that it would end up on after the castle operation
     * is complete.
     * <p>
     * This method returns null if the user cannot castle, or if the given Move does not represent
     * an attempted castle by the user.
     *
     * @param move - the Move to evaluate as a potential castle
     * @return A Move object representing the motion of the Rook concerned in the castle operation,
     * if the given move is a valid castle move; null otherwise.
     */
    public Move isCastle(Move move) {
        Pair src = move.src;
        Pair dest = move.dest;

        if (board.validSquare(src.first(), src.second()) && board.validSquare(dest.first(), dest.second())) {
            Piece moved = board.getPiece(src.first(), src.second());

            // Here we check if the given move is a castle by checking if: the piece being moved is
            // the player's king, the given move is a valid move for the king, and that the move
            // represents a horizontal displacement of exactly two squares (distinguishes a castle
            // move from a normal king move).
            if (moved instanceof King
                    && moved.getMoves().contains(dest)
                    && moved.getColour() == getUserColour()
                    && (moved.getColumn() - dest.second() == 2 || moved.getColumn() - dest.second() == -2)) {
                // Get the sign of the king's movement (+1 for right, -1 for left)
                int direction = (dest.second() - moved.getColumn() > 0) ? 1 : -1;

                // Figure out which column the rook being castled with occupies. We asssume, since
                // the King told us that the Move being considered is valid, that there is a Rook
                // that can be castled with in the same row as the King. We just need to determine
                // its column (it could be either to the left of the King or the right), determined
                // by which direction the King is moving in
                int column;
                if (direction > 0) {
                    column = 7;
                } else {
                    column = 0;
                }

                Pair rookSrc = new Pair(moved.getRow(), column);
                // After the castle, the Rook stays in the same row as the King, but ends up on the
                // opposite side of him.
                Pair rookDest = new Pair(moved.getRow(), dest.second() - direction);

                return new Move(rookSrc, rookDest);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Determine if the given Move represents a valid en passant capture by the user. If it does,
     * return a Pair object representing the square on the board occupied by the pawn that WOULD BE
     * CAPTURED as part of the given move. Returns null if the given Move does not represent a valid
     * en passant capture by the user.
     *
     * @param move - the Move to be considered as a potential en passant capture
     * @return If the given Move represents a valid en passant capture by the user, returns a Pair
     * object pinpointing on the board the location of the enemy pawn that would be captured as part
     * of the Move. Otherwise, returns null.
     */
    public Pair isEnPassant(Move move) {
        Pair src = move.src;
        Pair dest = move.dest;

        if (board.validSquare(src.first(), src.second()) && board.validSquare(dest.first(), dest.second())) {
            Piece moved = board.getPiece(src.first(), src.second());

            if (moved instanceof Pawn && moved.getColour() == getUserColour() && board.isEnPassant(dest, (Pawn) moved)) {
                // If we're returning the square occupied by the pawn being captured, we know from
                // the definition of en passant that the pawn being captured is, prior to capture,
                // in the same row as the pawn which is capturing it. We also know that the pawn
                // being captured is in the same column that the capturing pawn ENDS UP IN, after
                // the capture.
                // Here's a quick diagram. Before:
                //
                // XX
                // EX
                // pP
                //
                // The 'E' represents where the white pawn (the 'P') can move to do an en passant
                // capture. After:
                // XX
                // PX
                // XX
                // As you can see, the pawn capturing and the pawn getting captured share the same
                // row initially, and the same column after the capture.
                return new Pair(src.first(), dest.second());
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Attempts to make the given Move. Returns a variety of potential error codes if the move
     * cannot be made. Note below that there are TWO possible success codes, -1 and 0.
     *
     * @return -1 if the move is successfully made, and a promotion is now required <br>
     * 0 if the move is successfully made <br>
     * 1 if the move is invalid (source square is empty, move is not valid for
     * the piece at srcSquare, etc.) <br>
     * 2 if the move is being made out of turn <br>
     * 3 if a promotion needs to be handled before any moves can be made
     */
    public int makeMove(Move move) {
        return board.move(move.src, move.dest);
    }

    /**
     * Attempt to issue a promotion request on behalf of the user
     *
     * @param piece - indicates what piece to promote INTO
     * @return  0 if the promotion was successful
     *          1 if the promotion failed
     */
    public int promote(PieceType.PromotePiece piece) {
        // If it isn't the user's turn
        if((Integer) game.getData(GameData.STATE) == 0) {
            Log.e(tag, "Was asked to promote even though it isn't the user's turn");
            return 1;
        }

        int code = board.promote(piece.charRep);
        if(code == 2) {
            Log.e(tag, "PromotePiece " + piece + " has invalid charRep: '" + piece.charRep + "' that was rejected by Board");
        }
        return (code == 0) ? 0 : 1;
    }

    /**
     * Query the colour (black or white) being played by the user
     *
     * @return - The Colour being played by the user in the game being presented by this object
     */
    public Colour getUserColour() {
        return game.getUserColour();
    }
}
