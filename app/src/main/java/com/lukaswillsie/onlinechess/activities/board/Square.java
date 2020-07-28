package com.lukaswillsie.onlinechess.activities.board;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;

import com.lukaswillsie.onlinechess.R;

import Chess.com.lukaswillsie.chess.Bishop;
import Chess.com.lukaswillsie.chess.Colour;
import Chess.com.lukaswillsie.chess.Knight;
import Chess.com.lukaswillsie.chess.Pawn;
import Chess.com.lukaswillsie.chess.Piece;
import Chess.com.lukaswillsie.chess.Queen;
import Chess.com.lukaswillsie.chess.Rook;

/**
 * Represents a square on the chessboard. A square has a set of (x,y) coordinates corresponding to
 * its place on the screen. (0,0) is the bottom left corner of the screen.
 */
public class Square {
    /**
     * The row this square occupies on the board. row 0 is the bottom row of the board.
     */
    private final int row;

    /**
     * This column this square occupies on the board. column 0 is the leftmost column on the board
     */
    private final int column;

    /**
     * The ConstraintLayout corresponding to this square on the screen
     */
    private ConstraintLayout layout;

    /**
     * The Context containing the ConstraintLayout that this object is managing
     */
    private Context context;

    /**
     * Contains a reference to the ImageView currently being displayed in this Square.
     */
    private ImageView image;

    /**
     * The Piece that this Square is currently displaying. null if this Square is empty.
     */
    private Piece piece;

    /**
     * This object will be notified when this square is clicked.
     */
    private SquareOnTouchListener listener;

    /**
     *
     */
    private final Drawable lightBackground;
    private final Drawable darkBackground;

    /**
     * Create a new Square with the given position, corresponding to the given layout on the screen.
     * The given layout should not have any children, and should at this point have no attributes
     * applied to it other than its layout parameters
     *
     * @param row - this Square's row, with row=0 representing the bottom of the board
     * @param column - the square's column, with column=0 representing the left side of the board
     * @param layout - the ConstraintLayout corresponding to this square on the screen
     */
    public Square(int row, int column, ConstraintLayout layout) {
        this.row = row;
        this.column = column;
        this.layout = layout;
        this.context = layout.getContext();
        this.lightBackground = new ColorDrawable(context.getResources().getColor(R.color.white));
        this.darkBackground = new ColorDrawable(0xFF4BA2E3);

        if(isLightSquare()) {
            layout.setBackground(lightBackground);
        }
        else {
            layout.setBackground(darkBackground);
        }

        image = new ImageView(context);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int margin = 5;
        params.setMargins(margin, margin, margin, margin);
        image.setLayoutParams(params);

        layout.addView(image);
    }

    /**
     * Starts a drag, originating from this Square. If this square is empty, does nothing.
     * Otherwise, starts a drag event and uses as a drag shadow the image of the piece occupying
     * this Square.
     */
    public void startDrag() {
        if(piece != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                layout.startDragAndDrop(null, new PieceDragShadowBuilder(), null, View.DRAG_FLAG_OPAQUE);
            }
            else {
                layout.startDrag(null, new PieceDragShadowBuilder(), null, 0);
            }
        }
    }

    /**
     * This class is responsible for building drag shadows originating from this square
     */
    private class PieceDragShadowBuilder extends View.DragShadowBuilder {
        Drawable shadow;

        private PieceDragShadowBuilder() {
            this.shadow = context.getResources().getDrawable(getDrawableID(piece));
        }

        @Override
        public void onProvideShadowMetrics(Point outShadowSize, Point outShadowTouchPoint) {
            int width = image.getWidth();
            int height = image.getHeight();

            shadow.setBounds(0, 0, width, height);

            outShadowSize.set(width, height);
            outShadowTouchPoint.set(width/2, height/2);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            shadow.draw(canvas);
        }
    }

    /**
     * Set this Square to display a picture of the given Piece. If piece is null, this Square will
     * be emptied of whatever it is displaying now and will remain empty.
     *
     * @param piece - the piece to be displayed in this square
     */
    public void setPiece(Piece piece) {
        if(piece == null) {
            image.setImageResource(android.R.color.transparent);
            this.piece = null;
        }
        else {
            this.piece = piece;
            this.drawPiece(piece);
        }
    }

    /**
     * Return the Piece that this Square is displaying. Is null if this Square is empty.
     *
     * @return The Piece object that this Square is displaying.
     */
    public Piece getPiece() {
        return piece;
    }

    /**
     * Creates and returns an ImageView, placed directly on top of this Square, but as a child of
     * the root layout of the current activity, not this Square's ConstraintLayout. The ImageView
     * contains an image of the piece being displayed in this Square. The ImageView is placed and
     * sized so perfectly that when it is created the appearance of this Square does not change at
     * all. However, because the created ImageView is a child of the root layout of the activity, it
     * can be animated as part of a move animation.
     *
     * Does nothing and returns null if this Square is empty when this method is called.
     *
     * @return A newly-created ImageView containing an image of the Piece on this Square. On the
     * screen, the ImageView will be placed perfectly on top of this Square, so that no visual
     * change is discernible. The ImageView will be a child of the ConstraintLayout containing
     * the TableLayout of which this Square is a cell.
     */
    public ImageView getAnimatableView() {
        if (piece != null) {
            ImageView im = new ImageView(context);
            // Our Square's ConstraintLayout is inside a LinearLayout which is inside a TableLayout
            // which is inside the root ConstraintLayout.
            ConstraintLayout root = (ConstraintLayout)layout.getParent().getParent().getParent();

            // Set the ImageView to be precisely as large as our square.
            im.setLayoutParams(new ConstraintLayout.LayoutParams(layout.getWidth(), layout.getWidth()));

            // Constrain the ImageView within the root layout
            int id = ViewCompat.generateViewId();
            im.setId(id);
            ConstraintSet constraints = new ConstraintSet();
            constraints.clone(root);
            constraints.connect(id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
            constraints.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            constraints.connect(id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            constraints.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            constraints.applyTo(root);

            // Make the ImageView hold the same image as this Square
            im.setBackgroundResource(getDrawableID(piece));

            im.setX(getAbsoluteX());
            im.setY(getAbsoluteY());

            root.addView(im);
            return im;
        }
        else {
            return null;
        }
    }

    /**
     * Get this Square's absolute x coordinate in pixels. This means the square's horizontal
     * distance from the top left corner of the root layout.
     *
     * @return - this Square's absolute x coordinate
     */
    public float getAbsoluteX() {
        // Our layout is nested as follows: we have a ConstraintLayout as our root. Then we have
        // a TableLayout. The TableLayout has 8 rows, each of which is a LinearLayout. Then each
        // LinearLayout contains 8 ConstraintLayouts, which act as the squares in that row. So
        // to get the x-coordinate of this Square with respect to the top left of the root layout,
        // we need to get the x-coordinate of our ConstraintLayout within the LinearLayout. Then
        // we need to add the x-coordinate of that LinearLayout within the TableLayout. Then we need
        // to add the x-coordinate of the TableLayout within the root ConstraintLayout.
        return layout.getX() + ((View)layout.getParent()).getX() + ((View)layout.getParent().getParent()).getX();
    }

    /**
     * Get this Square's absolute y coordinate in pixels. This means the square's horizontal
     * distance from the top left corner of the root layout.
     *
     * @return - this Square's absolute y coordinate
     */
    public float getAbsoluteY() {
        // Our layout is nested as follows: we have a ConstraintLayout as our root. Then we have
        // a TableLayout. The TableLayout has 8 rows, each of which is a LinearLayout. Then each
        // LinearLayout contains 8 ConstraintLayouts, which act as the squares in that row. So
        // to get the y-coordinate of this Square with respect to the top left of the root layout,
        // we need to get the y-coordinate of our ConstraintLayout within the LinearLayout. Then
        // we need to add the y-coordinate of that LinearLayout within the TableLayout. Then we need
        // to add the y-coordinate of the TableLayout within the root ConstraintLayout.
        return layout.getY() + ((View)layout.getParent()).getY() + ((View)layout.getParent().getParent()).getY();
    }

    /**
     * Draw a representation of the given Piece in the ImageView being managed by this Square.
     *
     * @param piece - the piece that will be displayed in the ImageView
     */
    private void drawPiece(Piece piece) {
        image.setImageResource(getDrawableID(piece));
    }

    /**
     * Takes the given piece and returns a drawable resource ID that can be used to represent that
     * piece.
     *
     * @param piece - the Piece to convert into a drawable
     * @return A drawable resource ID for a drawable that can represent the given piece
     */
    private @DrawableRes int getDrawableID(Piece piece) {
        if(piece.getColour() == Colour.WHITE) {
            if(piece instanceof Pawn) {
                return R.drawable.white_pawn;
            }
            else if(piece instanceof Rook) {
                return R.drawable.white_rook;
            }
            else if(piece instanceof Knight) {
                return R.drawable.white_knight;
            }
            else if(piece instanceof Bishop) {
                return R.drawable.white_bishop;
            }
            else if(piece instanceof Queen) {
                return R.drawable.white_queen;
            }
            // Otherwise, piece is a King
            else {
                return R.drawable.white_king;
            }
        }
        else {
            if(piece instanceof Pawn) {
                return R.drawable.black_pawn;
            }
            else if(piece instanceof Rook) {
                return R.drawable.black_rook;
            }
            else if(piece instanceof Knight) {
                return R.drawable.black_knight;
            }
            else if(piece instanceof Bishop) {
                return R.drawable.black_bishop;
            }
            else if(piece instanceof Queen) {
                return R.drawable.black_queen;
            }
            // Otherwise, piece is a King
            else {
                return R.drawable.black_king;
            }
        }
    }

    /**
     * HIGHLIGHTS this square. This means highlighting it as one that can be moved to by some piece
     * selected by the user.
     */
    public void highlight() {
        this.layout.setBackground(new ColorDrawable(0xFF62E69E));
    }

    /**
     * Reset the background of this Square. Does not change what piece this Square is showing, but
     * resets the background if this square was previously highlighted.
     */
    public void reset() {
        if(isLightSquare()) {
            layout.setBackground(lightBackground);
        }
        else {
            layout.setBackground(darkBackground);
        }
    }

    /**
     * Determine whether or not this square is a light square on the chessboard.
     * @return true if this square is a light square, false if it is a dark square
     */
    private boolean isLightSquare() {
        // If you look at a chess board using the row,column system that we're using, you'll
        // notice the following pattern determines which squares are light and which dark
        return row % 2 == 0 && column % 2 == 1 || row % 2 == 1 && column % 2 == 0;
    }

    /**
     * Applies the given listener to this Square. The given listener will receive all touch events
     * received by this Square.
     *
     * @param listener - the object that will receive touch events from this Square
     */
    @SuppressLint("ClickableViewAccessibility")
    public void setSquareOnTouchListener(final SquareOnTouchListener listener) {
        this.layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return listener.onTouch(row, column, event);
            }
        });
    }

    /**
     * Set the given SquareDragListener to receive drag events from this Square
     *
     * @param listener - the object that will receive any and all subsequent drag events from this
     *                 square
     */
    public void setSquareDragListener(final SquareDragListener listener) {
        this.layout.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                return listener.onDrag(row, column, event);
            }
        });
    }

    /**
     * Objects wishing to be notified of touch events relevant to this Square and the
     * ConstraintLayout it is managing must implement this interface.
     */
    public interface SquareOnTouchListener {
        /**
         * This method will be called whenever a touch event is received by a Square, passing on the
         * relevant information to the SquareOnTouchListener.
         *
         * @param row - the row that the Square that received the event occupies on the screen
         * @param column - the column that the Square that received the event occupies on the screen
         * @param event - contains information about the type of touch event that was received
         * @return true if the SquareOnTouchListener wants to receive any subsequent touch events
         * relating to the current action, false otherwise. For example, if a click is made (meaning
         * an ACTION_ DOWN event followed by an ACTION_UP event), and the SquareOnTouchListener
         * returns false after the ACTION_DOWN event, they won't be notified when the ACTION_UP
         * event occurs. However, they'll be notified the next time a new click or other action
         * starts.
         */
        boolean onTouch(int row, int column, MotionEvent event);
    }

    /**
     * Objects wishing to be notified of drag events relating to this Square must implement this
     * interface.
     */
    public interface SquareDragListener {
        /**
         * Whenever a DragEvent is received by this Square, the following method will be called,
         * notifying the Listener of the even, as well as this Square's position on the screen.
         *
         * @param row - the row that the Square that received the event occupies on the screen
         * @param column - the column that the Square that received the event occupies on the screen
         * @param event - the DragEvent that was received by this Square
         * @return true if the SquareDragListener wants to continue to receive drag events relating
         * to the current drag from this Square, false otherwise.
         */
        boolean onDrag(int row, int column, DragEvent event);
    }
}
