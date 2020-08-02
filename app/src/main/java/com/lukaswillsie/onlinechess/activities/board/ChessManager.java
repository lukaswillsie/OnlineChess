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
public class ChessManager implements BoardDisplay.DisplayListener{
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
     * Keeps track of whether or not an active drag event has ended
     */
    private boolean dragEnded = false;

    /**
     * Create a new GameManager that will manage the game represented by the given GamePresenter
     * and use the given BoardDisplay object to interact with the UI. The BoardDisplay object
     * must already have been built when it is given to this object (.build() must already have been
     * called).
     *
     * @param presenter - a GamePresenter representing the game that this object will manage
     * @param display - the object that this GameManager will use to interact with the UI
     */
    ChessManager(GamePresenter presenter, BoardDisplay display) {
        this.presenter = presenter;
        this.display = display;

        this.display.activate(presenter, this);

        // Record whether or not it's the user's turn
        this.userTurn = (Integer)presenter.getData(GameData.TURN) == 1;
    }

    @Override
    public boolean onTouch(int row, int column, MotionEvent event) {
        Log.i(tag, "(" + event.getRawX() + ", " + event.getRawY() + ")\n" + event.toString());
        int action = event.getAction();
        Piece piece;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // If the user clicks an ally piece, we want to select that piece and show the
                // squares it can move to immediately. We don't want to wait until after ACTION_UP.
                // This way, if the user clicks a piece and then drags it to move it, the squares
                // that piece can move to will already be highlighted.
                if(userTurn) {
                    piece = getPiece(row, column);

                    // If the user is clicking an ally piece different from the currently selected
                    // piece, they select the new piece
                    if(piece != null && piece.getColour() == presenter.getUserColour()
                            && piece != selected) {
                        this.selected = piece;
                        display.resetSquares();

                        display.selectSquare(row, column);

                        List<Pair> moves = selected.getMoves();

                        // We parse the list of moves into capture moves and normal moves
                        List<Pair> normalMoves = new ArrayList<>();
                        List<Pair> captureMoves = new ArrayList<>();
                        for(Pair move : moves) {
                            if(presenter.getPiece(move.first(), move.second()) == null) {
                                // If the move ends on an empty square, it can still be a capture
                                // move if it's an en passant capture
                                if(presenter.isEnPassant(new Move(new Pair(selected.getRow(), selected.getColumn()), move)) != null) {
                                    captureMoves.add(move);
                                }
                                else {
                                    normalMoves.add(move);
                                }
                            }
                            else {
                                captureMoves.add(move);
                            }
                        }

                        display.highlightSquares(convertToScreenCoords(captureMoves), true);
                        display.highlightSquares(convertToScreenCoords(normalMoves), false);
                    }
                    return true;
                }
                else {
                    return false;
                }
            case MotionEvent.ACTION_UP:
                if(userTurn) {
                    piece = getPiece(row, column);

                    // The user clicked an empty square. They might be issuing a move command.
                    if(piece == null) {
                        Pair tapped = converToBoardCoords(row, column);
                        // If the user has a piece selected and the empty square they are clicking
                        // is a square that that piece can move to, we execute a move
                        if(this.selected != null) {
                            if(selected.getMoves().contains(tapped)) {
                                Pair src = new Pair(selected.getRow(), selected.getColumn());
                                Move move = new Move(src, tapped);

                                // If the user is trying to castle, we handle things slightly
                                // differently, because we also need to move the Rook being castled
                                // with
                                Move rookMove = presenter.isCastle(move);
                                Pair enPassantCapture = presenter.isEnPassant(move);
                                if(rookMove != null) {
                                    // Move the King and Rook, only playing a sound effect for one
                                    // of their moves
                                    display.move(move, false, false);
                                    display.move(rookMove, true, false);
                                }
                                // If the user is doing an en passant capture, we need to remove the
                                // pawn that they are capturing from the board
                                else if(enPassantCapture != null) {
                                    display.move(move, true, true);
                                    display.set(enPassantCapture.first(), enPassantCapture.second(), null, false, false);
                                }
                                // Otherwise, just move the piece to the empty square
                                else {
                                    display.move(new Move(src, tapped), true, false);
                                }


                                this.userTurn = false;
                                this.selected = null;

                                // Now that the user has moved, we can un-highlight all the move
                                // squares we previously had highlighted
                                display.resetSquares();
                                return false;
                            }
                            // If they're just tapping an empty square, we take this to mean that
                            // the user wants to de-select their current selection (if they have
                            // one)
                            else {
                                display.resetSquares();
                                this.selected = null;
                                return false;
                            }
                        }
                        else {
                            return false;
                        }
                    }
                    // The user clicked an enemy piece
                    else if (piece.getColour() != presenter.getUserColour()) {
                        // If the user has a piece selected and they are tapping an opponent piece
                        // that they can capture
                        Pair tapped = new Pair(piece.getRow(), piece.getColumn());
                        if(this.selected != null) {
                            if(selected.getMoves().contains(tapped)) {
                                Pair src = new Pair(selected.getRow(), selected.getColumn());
                                // Move the piece to the square tapped by the user and play a
                                // capture sound effect
                                display.move(new Move(src, tapped), true, true);
                                this.userTurn = false;
                                this.selected = null;

                                // Now that the user has moved, we can un-highlight all the move
                                // squares we previously had highlighted
                                display.resetSquares();
                                return false;
                            }
                            else {
                                this.selected = null;
                                display.resetSquares();
                                return false;
                            }
                        }
                        else {
                            return false;
                        }
                    }
                    // If the user is clicking an ally piece, we've already handled the click in
                    // ACTION_DOWN, so we do nothing here
                    else {
                        return false;
                    }
                }
                else {
                    return false;
                }
            case MotionEvent.ACTION_MOVE:
                Piece dragged = getPiece(row, column);
                if(userTurn && dragged != null && dragged.getColour() == presenter.getUserColour()) {
                    Pair src = converToBoardCoords(row, column);
                    dragEnded = false;
                    display.startDrag(row, column);
                    display.set(src.first(), src.second(), null,false, false);
                }
                return false;
            default:
                return false;
        }
    }

    @Override
    public boolean onDrag(int row, int column, DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                // When a drag starts, the only squares on the board that care about the drag are
                // the squares that the piece being dragged can move to, or the square that the
                // piece being dragged currently occupies. So we only return true for these squares.
                return (selected.getMoves().contains(converToBoardCoords(row, column))
                || new Pair(selected.getRow(), selected.getColumn()).equals(converToBoardCoords(row, column)));
            case DragEvent.ACTION_DRAG_ENTERED:
                // Return true because we don't do anything special here but want to keep getting
                // callbacks
                return true;
            case DragEvent.ACTION_DRAG_LOCATION:
                // Return true because we don't do anything special here but want to keep getting
                // callbacks
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
                // Return true because we don't do anything special here but want to keep getting
                // callbacks
                return true;
            case DragEvent.ACTION_DROP:
                // We've already ensured in the ACTION_DRAG_STARTED case that the only squares that
                // can receive an ACTION_DROP event are ones that the piece being dragged can move
                // to, or the square that the piece being dragged current occupies. So all we have
                // to do is either move the piece being dragged, or return it to its square.
                Pair src = new Pair(selected.getRow(), selected.getColumn());
                Pair dest = converToBoardCoords(row, column);

                // If the user started the drag and then let go on the same square, we simply return
                // the piece to its square on the screen.
                if(src.equals(dest)) {
                    display.set(src.first(), src.second(), selected, false, false);
                    return true;
                }

                // If the move is a normal capture
                if(presenter.getPiece(dest) != null && presenter.getPiece(dest).getColour() != selected.getColour()) {
                    display.set(dest.first(), dest.second(), selected, true, true);
                }
                // If the move is onto an empty square
                else {
                    Move rookMove = presenter.isCastle(new Move(src, dest));
                    Pair enPassantCapture = presenter.isEnPassant(new Move(src, dest));

                    // If the user is castling, we need to move the Rook being castled with, as well
                    // as place the King being moved on the destination square
                    if(rookMove != null) {
                        // Set the King and move the Rook, playing a sound effect for each
                        display.set(dest.first(), dest.second(), selected, true, false);
                        display.move(rookMove, true, false);
                    }
                    // If the user is doing an en passant capture, we need to remove the pawn that they
                    // are capturing from the board
                    else if(enPassantCapture != null) {
                        // Set the pawn performing the capture and erase the pawn being captured,
                        // playing a single capture sound effect
                        display.set(dest.first(), dest.second(), selected, true, true);
                        display.set(enPassantCapture.first(), enPassantCapture.second(), null, false, false);
                    }
                    else {
                        display.set(dest.first(), dest.second(), selected, true, false);
                    }
                }




                display.resetSquares();
                this.selected = null;
                this.userTurn = false;
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                // Once our drag event ends, all the squares that could have been moved to get this
                // method call. We only care about the first one, so we use boolean dragEnded to
                // ensure we only run the below code the first time.
                if(!dragEnded && !event.getResult()) {
                    display.set(selected.getRow(), selected.getColumn(), selected, false, false);
                    dragEnded = true;
                }
                return true;
        }
        return true;
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

    private Pair converToBoardCoords(int row, int column) {
        if(presenter.getUserColour() == Colour.WHITE) {
            return new Pair(row, column);
        }
        else {
            return new Pair(7 - row, 7 - column);
        }
    }
}
