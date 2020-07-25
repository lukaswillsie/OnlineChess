package com.lukaswillsie.onlinechess.activities.board;

import android.content.Context;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.viewpager.widget.ViewPager;

import com.lukaswillsie.onlinechess.R;

import java.util.List;

import Chess.com.lukaswillsie.chess.*;

/**
 * This class is responsible for managing the chessboard that we display on the screen.
 */
public class BoardDisplay {
    /**
     * Represents the chessboard being displayed on the screen. Each Square object is a wrapper for
     * a ConstraintLayout representing a square on the screen. This matrix maps onto the screen
     * as follows: board[0][0] is the bottom-left corner of the screen. board[7][0] is the top-left
     * corner of the screen.
     */
    private Square[][] board = new Square[8][8];

    /**
     * The GamePresenter object whose data this object is displaying on the screen.
     */
    private GamePresenter presenter;

    /**
     * The object receiving touch events from all the Squares being managed by this object
     */
    private DisplayListener listener;

    /**
     * The Context of the board that this object is managing
     */
    private Context context;

    /**
     * Takes the given TableLayout and builds a chessboard on it. The given TableLayout should be
     * a TableLayout formatted exactly as in empty_chessboard_layout.xml, with 8 equally-weighted
     * LinearLayouts as children. Does nothing if the given TableLayout does not have the proper
     * format
     *
     * @param layout - the TableLayout that will be turned into a chessboard
     */
    public void build(TableLayout layout) {
        context = layout.getContext();

        for(int i = 0; i < 8; i++) {
            View child = layout.getChildAt(i);
            if(child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                for(int j = 0; j < 8; j++) {
                    ConstraintLayout square_layout = new ConstraintLayout(context);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
                    square_layout.setLayoutParams(params);

                    Square square = new Square(7 - i, j, square_layout);
                    board[7-i][j] = square;
                    row.addView(square_layout);
                }
            }
        }
    }

    /**
     * Binds this BoardDisplay object to the given GamePresenter. The screen will be updated to
     * display the board contained in the given GamePresenter. The given DisplayListener will be
     * notified of all touch events that occur on the board
     */
    public void activate(GamePresenter presenter, DisplayListener listener) {
        this.presenter = presenter;
        this.listener = listener;

        Piece piece;
        for(int row = 0; row < 8; row++) {
            for(int column = 0; column < 8; column++) {
                if(presenter.getUserColour() == Colour.WHITE) {
                    piece = presenter.getPiece(row, column);
                }
                else {
                    piece = presenter.getPiece(7 - row, 7 - column);
                }

                if(piece != null) {
                    board[row][column].setPiece(piece);
                }

                board[row][column].setSquareOnTouchListener(new SquareTouchListener());
                board[row][column].setSquareDragListener(new SquareDragListener());
            }
        }
    }

    /**
     * Highlights the squares specified in the given list. Each Pair in the given list should
     * contain a valid set of coordinates (each coordinate should be between 0 and 7, inclusive on
     * both ends). Also, each Pair should specify a square on the SCREEN to be highlighted. Note
     * that the coordinates then are dependent on what colour the user is playing as.
     *
     * @param squares - a list of squares to be highlighted
     */
    public void highlightSquares(List<Pair> squares) {
        for(Pair pair : squares) {
            if(0 <= pair.first() && pair.first() <= 7 && 0 <= pair.second() && pair.second() <= 7) {
                board[pair.first()][pair.second()].highlight();
            }
        }
    }

    /**
     * Reset all the squares on the board. This simply means un-highlighting any and all squares
     * that have been highlighted.
     */
    public void resetSquares() {
        for(int row = 0; row < 8; row++) {
            for(int column = 0; column < 8; column++) {
                board[row][column].reset();
            }
        }
    }

    /**
     * Start a drag operation at the given square on the board.
     *
     * IMPORTANT NOTE: Row and column should be given as BOARD COORDINATES, independent of what
     * colour the user is playing. Does nothing if row and column are not both between 0 and 7,
     * inclusive.
     *
     * @param row - the row occupied by the square to start the drag at
     * @param column - the column occupied by the square to start the drag at
     */
    public void startDrag(int row, int column) {
        if(0 <= row && row <= 7 && 0 <= column && column <= 7) {
            board[row][column].startDrag();
        }
    }

    /**
     * Reset the square at the given row and column. This means that we set the specified square to
     * display whatever piece occupies that square in the model of our game, or nothing if that
     * square is empty.
     *
     * IMPORTANT NOTE: Row and column should be given as BOARD COORDINATES, independent of what
     * colour the user is playing. Does nothing if row and column are not both between 0 and 7,
     * inclusive.
     *
     * @param row - the row on the board that the square to be reset occupies
     * @param column - the column on the board that the square to be reset occupies
     */
    public void reset(int row, int column) {
        if(0 <= row && row <= 7 && 0 <= column && column <= 7) {
            if(presenter.getUserColour() == Colour.WHITE) {
                board[row][column].setPiece(presenter.getPiece(row, column));
            }
            else {
                board[7 - row][7 - column].setPiece(presenter.getPiece(row, column));
            }
        }
    }

    /**
     * Sets the square specified by row and column to display the given piece. If piece is null, the
     * specified square will be emptied.
     *
     * IMPORTANT NOTE: Row and column should be given as BOARD COORDINATES, independent of what
     * colour the user is playing. Does nothing if row and column are not both between 0 and 7,
     * inclusive.
     *
     * @param row - the row on the board that the square to be set occupies
     * @param column - the column on the board that the square to be set occupies
     * @param piece - the piece to be displayed on the given square
     */
    public void set(int row, int column, Piece piece) {
        if(0 <= row && row <= 7 && 0 <= column && column <= 7) {
            if(presenter.getUserColour() == Colour.WHITE) {
                board[row][column].setPiece(piece);
            }
            else {
                board[7 - row][7 - column].setPiece(piece);
            }
        }
    }

    /**
     * Sets the square specified by row and column to display the given piece. If piece is null, the
     * specified square will be emptied.
     *
     * IMPORTANT NOTE: Row and column should be given as SCREEN COORDINATES, that depend on what
     * colour the user is playing. Does nothing if row and column are not both between 0 and 7,
     * inclusive.
     *
     * @param row - the row on the board that the square to be set occupies
     * @param column - the column on the board that the square to be set occupies
     * @param piece - the piece to be displayed on the given square
     */
    private void setOnScreen(int row, int column, Piece piece) {
        if(0 <= row && row <= 7 && 0 <= column && column <= 7) {
            board[row][column].setPiece(piece);
        }
    }

    /**
     * Execute the given Move. Moves the piece at move.src to the square move.dest. move.src and
     * move.dest should both be given in BOARD COORDINATES, independent of what colour the user is
     * playing. Animates the piece being moved so that it actually slides across the board to the
     * destination square.
     *
     * Does nothing if the square specified by move.src is empty or if either square is
     * invalid (either one has row or column outside of 0,1,...,7).
     *
     * @param move - the Move to be executed
     */
    public void move(Move move) {
        Pair src = convertToScreenCoords(move.src);
        Pair dest = convertToScreenCoords(move.dest);

        // SCREEN COORDINATES for the source and destination square of the move
        final int src_row = src.first();
        final int src_column = src.second();
        final int dest_row = dest.first();
        final int dest_column = dest.second();

        final Piece piece = presenter.getPiece(move.src.first(), move.src.second());
        if(piece != null) {
            // This creates an ImageView, floating on top of the Square at src_row and src_column,
            // that we can animate as part of the move
            final ImageView animate = board[src_row][src_column].getAnimatableView();
            float[] srcCoords = {board[src_row][src_column].getAbsoluteX(), board[src_row][src_column].getAbsoluteY()};
            float[] destCoords = {board[dest_row][dest_column].getAbsoluteX(), board[dest_row][dest_column].getAbsoluteY()};


            // Put an empty square where the piece used to be
            setOnScreen(src_row, src_column, null);

            // Will animate the ImageView we created from the source square to the destination
            // square
            final TranslateAnimation anim = new TranslateAnimation(0, destCoords[0] - srcCoords[0], 0, destCoords[1] - srcCoords[1]);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    // Now that the animation is done, place the piece from the source square on the
                    // destination square and destroy the ImageView we used for the animation.
                    setOnScreen(dest_row, dest_column, piece);
                    ((ViewGroup)animate.getParent()).removeView(animate);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            anim.setDuration(200);
            animate.startAnimation(anim);
        }
    }

    /**
     * Convert the given Pair of board coordinates to screen coordinates.
     *
     * @param pair - the Pair of board coordinates to be converted
     * @return A new Pair, containing the screen coordinates that correspond to the given Pair of
     * board coordinates
     */
    private Pair convertToScreenCoords(Pair pair) {
        if(presenter.getUserColour() == Colour.WHITE) {
            return new Pair(pair.first(), pair.second());
        }
        else {
            return new Pair(7 - pair.first(), 7 - pair.second());
        }
    }

    /**
     * This class is responsible for listening to the Squares being managed by this BoardDisplay and
     * passing touch events on to the object listening to this BoardDisplay.
     */
    private class SquareTouchListener implements Square.SquareOnTouchListener {
        @Override
        public boolean onTouch(int x, int y, MotionEvent event) {
            return listener.onTouch(x, y, event);
        }
    }

    /**
     * This class is responsible for passing drag events on to the object listening to this
     * BoardDisplay
     */
    private class SquareDragListener implements Square.SquareDragListener {
        @Override
        public boolean onDrag(int row, int column, DragEvent event) {
            return listener.onDrag(row, column, event);
        }
    }

    /**
     * Implementing this interface allows objects to receive touch events whenever any square being
     * manager by a BoardDisplay object is touched
     */
    public interface DisplayListener {
        /**
         * Notifies the DisplayListener that the square occupying the specified spot on the screen
         * has received a touch event of the given type.
         *
         * @param row - the square's row. row = 0 is the bottom row of the board on the screen
         * @param column - the square's column. column = 0 is the leftmost column on the screen
         * @param event - contains information about the
         * @return true if the DisplayListener wants to continue to receive touch events relating
         * to the current action, false otherwise. For example, if a click is made (meaning an
         * ACTION_ DOWN event followed by an ACTION_UP event), and the DisplayListener returns false
         * after the ACTION_DOWN event, they won't be notified when the ACTION_UP event occurs.
         * However, they'll be notified the next time a new click or other action starts.
         */
        boolean onTouch(int row, int column, MotionEvent event);

        /**
         * Notifies the DisplayListener that the square occupying the specified spot on the screen
         * has received the given DragEvent.
         *
         * @param row - the row on the screen occupied by the square that received the event
         * @param column - the row on the screen occupied by the square that received the event
         * @param event - the DragEvent that was received
         * @return true if the DisplayListener wants to keep receiving drag events relating to this
         * drag from the specified square, false otherwise
         */
        boolean onDrag(int row, int column, DragEvent event);
    }
}
