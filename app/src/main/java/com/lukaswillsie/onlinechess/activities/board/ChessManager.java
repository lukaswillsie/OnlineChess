package com.lukaswillsie.onlinechess.activities.board;

import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.Display;
import com.lukaswillsie.onlinechess.activities.ErrorDialogFragment;
import com.lukaswillsie.onlinechess.activities.ReconnectListener;
import com.lukaswillsie.onlinechess.activities.Reconnector;
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
public class ChessManager implements BoardDisplay.DisplayListener, MoveRequestListener, ReconnectListener {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "ChessManager";

    /**
     * The object containing all the data about the game that this object is managing
     */
    private  GamePresenter presenter;

    /**
     * The object managing the display for this ChessManager
     */
    private BoardDisplay display;

    /**
     * Will be used to display dialogs to the user if things go wrong
     */
    private GameDialogCreator dialogCreator;

    /**
     * The activity displaying the game that this ChessManager is managing
     */
    private AppCompatActivity activity;

    /**
     * If the user currently has a piece selected (meaning it's their turn and they've tapped one
     * of their pieces), we have a reference to it here. null otherwise.
     */
    private Piece selected;

    /**
     * Keeps track of whether or not the user is currently able to move. In particular, it has to
     * be the user's turn and the user has to have an opponent in their game.
     */
    private boolean userCanMove;

    /**
     * Keeps track of whether or not an active drag event has ended
     */
    private boolean dragEnded = false;

    /**
     * If the user has submitted a move, say by dragging a piece onto a square, but the server
     * hasn't confirmed it for us, we keep a reference to it here until we don't need it anymore
     */
    private Move activeMove;

    /**
     * The ID of the game that this object is managing
     */
    private final String gameID;

    /**
     * The object that will process and send move requests to the server for us
     */
    private MoveRequestHandler moveHandler;

    /**
     * Create a new ChessManager that will manage the game represented by the given GamePresenter
     * and use the given BoardDisplay object to interact with the UI. The BoardDisplay object
     * must already have been built when it is given to this object (.build() must already have been
     * called).
     *
     * @param gameID - the ID of the game that this object is managing
     * @param presenter - a GamePresenter representing the game that this object will manage
     * @param display - the object that this ChessManager will use to interact with the UI
     * @param dialogCreator - the object that this ChessManager will use to create error dialogs,
     *                      when necessary
     * @param activity - the Activity displaying the game that this ChessManager is managing
     */
    ChessManager(String gameID, GamePresenter presenter, BoardDisplay display, GameDialogCreator dialogCreator,
                 AppCompatActivity activity) {
        this.presenter = presenter;
        this.display = display;
        this.dialogCreator = dialogCreator;
        this.moveHandler = new MoveRequestHandler(this, display.getContext());
        this.gameID = gameID;
        this.activity = activity;

        this.display.activate(presenter, this);

        // Initialize our game fields to match the model
        resetFromModel();
    }

    /**
     * Resets this object so that all its game fields exactly mirror the state of the game according
     * to the model. Used if the server rejects a move that we made, forcing us to revert to our
     * base state.
     */
    private void resetFromModel() {
        this.userCanMove = (Integer)presenter.getData(GameData.STATE) == 1
                && ((String)presenter.getData(GameData.OPPONENT)).length() > 0;
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
                if(userCanMove) {
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
                if(userCanMove) {
                    piece = getPiece(row, column);

                    // The user clicked an empty square. They might be issuing a move command.
                    if(piece == null) {
                        Pair tapped = convertCoords(row, column);
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

                                activeMove = new Move(src, tapped);
                                moveHandler.submitMove(activeMove, gameID);
                                this.userCanMove = false;
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

                                activeMove = new Move(src, tapped);
                                moveHandler.submitMove(activeMove, gameID);
                                this.userCanMove = false;
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
                if(userCanMove && dragged != null && dragged.getColour() == presenter.getUserColour()) {
                    Pair src = convertCoords(row, column);
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
                return (selected.getMoves().contains(convertCoords(row, column))
                || new Pair(selected.getRow(), selected.getColumn()).equals(convertCoords(row, column)));
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
                Pair dest = convertCoords(row, column);

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

                activeMove = new Move(src, dest);
                moveHandler.submitMove(activeMove, gameID);

                display.resetSquares();
                this.selected = null;
                this.userCanMove = false;
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

    /**
     * Return a Pair containing the given coordinates, converted from board to screen coordinates
     * or vice versa. The conversion process is exactly the same regardless of which coordinates
     * we're converting from/to, so we only need one method.
     *
     * @param row - the row component of the coordinates to convert
     * @param column - the column component of the coordinates to convert
     * @return A Pair, containing the given set of coordinates converted into its opposite type
     */
    private Pair convertCoords(int row, int column) {
        if(presenter.getUserColour() == Colour.WHITE) {
            return new Pair(row, column);
        }
        else {
            return new Pair(7 - row, 7 - column);
        }
    }

    /**
     * Called if a move we submitted to the server is accepted by the server. We use this method to
     * update our model, all the data we have for the game, now that the move has been confirmed.
     *
     * @param promotionNeeded - whether or not a promotion is now needed as a result of our move
     */
    @Override
    public void moveSuccess(boolean promotionNeeded) {
        /*
         * Need to check here for:
         * 1. Checkmate
         * 2. Stalemate
         * 3. Promotion
         * 4. Maybe increment turn counter
         */
        int code = presenter.makeMove(activeMove);

        // If our model rejects the move, we notify the user and reset the screen.
        // Note that this should never happen for two reasons: first, we're in this method because
        // the server accepted our move as valid; second, we only allow the user to submit moves to
        // the server if we've cleared them with our model beforehand.
        if(code != 0 && code != -1) {
            display.reset();
            this.resetFromModel();
            Display.showSimpleDialog(R.string.model_failure_error_text, display.getContext());
            return;
        }
        // If the server and our model disagree about whether or not a promotion is needed, we do
        // the same thing as above
        else if((promotionNeeded && code == 0) || (!promotionNeeded && code == -1)) {
            display.reset();
            this.resetFromModel();
            Display.showSimpleDialog(R.string.model_failure_error_text, display.getContext());
            return;
        }

        Colour userColour = presenter.getUserColour();
        Colour enemyColour = (userColour == Colour.WHITE) ? Colour.BLACK : Colour.WHITE;

        // If a promotion isn't needed, the user's turn is over and we can check if they've
        // delivered checkmate or maybe a stalemate
        if(!promotionNeeded) {
            if(presenter.isStalemate()) {
                presenter.setData(GameData.DRAWN, 1);
            }
            else if (presenter.isCheckmate()) {
                presenter.setData(GameData.USER_WON, 1);
            }
            // Otherwise, the game isn't over
            else {
                // Increment the turn counter if the user is playing black
                if(userColour == Colour.BLACK) {
                    presenter.setData(GameData.TURN, (Integer)presenter.getData(GameData.TURN) + 1);
                }

                // Set GameData.STATE to 0, which means that it isn't the user's turn anymore
                presenter.setData(GameData.STATE, 0);
            }
        }
        else {
            // Even if a promotion is needed and the user's turn isn't over, it's possible that they
            // could have delivered checkmate with their pawn move. In this case the game is over;
            // we don't bother with the promotion.
            if(presenter.isCheckmate()) {
                presenter.setData(GameData.USER_WON, 1);
            }
            else {
                // TODO: Implement promotions
            }
        }
    }

    /**
     * Called if a move request we submitted fails, for any reason other than a loss of connection.
     * We ensure that all of our moves are correct, to the best of our knowledge, so if a request
     * fails we can't do anything about it (especially if it's a server or system error) other than
     * tell the user and ask for guidance. So we simply notify the user that we couldn't submit
     * their move, and ask them if they want us to try again or simply forget about it.
     */
    @Override
    public void moveFailed() {
        dialogCreator.showErrorDialog(R.string.move_failed_error_text, new ErrorDialogFragment.CancellableErrorDialogListener() {
            @Override
            public void cancel() {
                display.reset();
                activeMove = null;
                resetFromModel();
            }

            @Override
            public void retry() {
                moveHandler.submitMove(activeMove, gameID);
            }
        });
    }

    /**
     * Called if a move request is stymied by a loss of connection with the server
     */
    @Override
    public void connectionLost() {
        dialogCreator.showConnectionLostDialog(R.string.connection_lost_alert, new ErrorDialogFragment.ErrorDialogListener() {
            @Override
            public void retry() {
                new Reconnector(ChessManager.this, activity).reconnect();
            }
        });
    }

    /**
     * If a move request fails because of a connection lost error, and we submit a reconnect
     * request, this method gets called after the reconnect request finishes successfully.
     */
    @Override
    public void reconnectionComplete() {
        moveHandler.submitMove(activeMove, gameID);
    }
}