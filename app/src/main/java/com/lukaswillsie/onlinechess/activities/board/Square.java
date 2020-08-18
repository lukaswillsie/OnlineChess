package com.lukaswillsie.onlinechess.activities.board;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.DragEvent;
import android.view.LayoutInflater;
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
     *
     */
    private final Drawable lightBackground;
    private final Drawable darkBackground;
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
     * A reference to the promotion banner currently attached to this Square, if there is one. null
     * otherwise.
     */
    private ConstraintLayout promotionBanner;

    /**
     * Creates a new Square object. The Square object will assume that it occupies the given row
     * and column on the screen, and will use the given ImageView to display its contents on the
     * screen. The given ImageView should already have been given layout parameters and added to its
     * parent layout. This object will simply manage the colour of its background and the image
     * resource it is displaying.
     * <p>
     * The given ImageView will be given a background according to whether or not this Square is
     * a light or dark square on the board, but won't display any piece until told to do so through
     * the setPiece() method.
     *
     * @param row       - this Square's row, with row=0 representing the bottom of the board
     * @param column    - the square's column, with column=0 representing the left side of the board
     * @param imageView - the ImageView that this object will use to display its piece on the screen
     */
    public Square(int row, int column, ImageView imageView) {
        this.row = row;
        this.column = column;
        this.image = imageView;
        this.context = image.getContext();
        this.lightBackground = new ColorDrawable(context.getResources().getColor(R.color.white));
        this.darkBackground = new ColorDrawable(0xFF4BA2E3);

        if (isLightSquare()) {
            image.setBackground(lightBackground);
        } else {
            image.setBackground(darkBackground);
        }

        int padding = 5;
        image.setPadding(padding, padding, padding, padding);
    }

    /**
     * Starts a drag, originating from this Square. If this square is empty, does nothing.
     * Otherwise, starts a drag event and uses as a drag shadow the image of the piece occupying
     * this Square.
     */
    public void startDrag() {
        if (piece != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                image.startDragAndDrop(null, new PieceDragShadowBuilder(), null, View.DRAG_FLAG_OPAQUE);
            } else {
                image.startDrag(null, new PieceDragShadowBuilder(), null, 0);
            }
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
     * Set this Square to display a picture of the given Piece. If piece is null, this Square will
     * be emptied of whatever it is displaying now and will remain empty.
     *
     * @param piece - the piece to be displayed in this square
     */
    public void setPiece(Piece piece) {
        if (piece == null) {
            image.setImageResource(android.R.color.transparent);
            this.piece = null;
        } else {
            if (piece != this.piece) {
                this.piece = piece;
                this.drawPiece(piece);
            }
            // Do nothing if the piece we're being given is the one we're already displaying
        }
    }

    /**
     * Creates and returns an ImageView, placed directly on top of this Square, but as a child of
     * the root layout of the current activity, not this Square's ConstraintLayout. The ImageView
     * contains an image of the piece being displayed in this Square. The ImageView is placed and
     * sized so perfectly that when it is created the appearance of this Square does not change at
     * all. However, because the created ImageView is a child of the root layout of the activity, it
     * can be animated as part of a move animation.
     * <p>
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
            // Our Square is inside a LinearLayout which is inside a TableLayout
            // which is inside the root ConstraintLayout.
            ConstraintLayout root = (ConstraintLayout) image.getParent().getParent().getParent();

            // Set the ImageView to be precisely as large as our square.
            im.setLayoutParams(new ConstraintLayout.LayoutParams(image.getWidth(), image.getWidth()));

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
        } else {
            return null;
        }
    }

    /**
     * Attach a promotion banner, displaying pieces of the given colour, to this Square. If this
     * Square already has a promotion banner attached to it when this method is called, that
     * promotion banner is removed.
     *
     * @param colour   - the colour of the pieces to be displayed in the promotion banner
     * @param listener - will receive a callback when the user selects a piece
     */
    public void attachPromotionBanner(Colour colour, final BannerListener listener) {
        ConstraintLayout root = (ConstraintLayout) image.getParent().getParent();

        // If this square already has a promotion banner attached to it, we remove it from the
        // screen
        if (promotionBanner != null) {
            ((ViewGroup) promotionBanner.getParent()).removeView(promotionBanner);
        }

        // Inflate our banner from an XML file
        if (colour == Colour.WHITE) {
            promotionBanner = (ConstraintLayout) LayoutInflater.from(context).inflate(R.layout.white_promote_menu_layout, root, false);
        } else {
            promotionBanner = (ConstraintLayout) LayoutInflater.from(context).inflate(R.layout.black_promote_menu_layout, root, false);
        }

        // Wire onClicks from the Views in the banner to the appropriate methods in BannerListener
        promotionBanner.findViewById(R.id.queen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.queenPromotion();
            }
        });

        promotionBanner.findViewById(R.id.rook).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.rookPromotion();
            }
        });

        promotionBanner.findViewById(R.id.bishop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.bishopPromotion();
            }
        });

        promotionBanner.findViewById(R.id.knight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.knightPromotion();
            }
        });

        root.addView(promotionBanner);
        ConstraintSet constraints = new ConstraintSet();
        constraints.clone(root);

        // The banner is already constrained laterally to the sides of the board, in XML. Here we
        // constrain the banner so that it is directly below the row occupied by this Square
        constraints.connect(promotionBanner.getId(), ConstraintSet.TOP, ((View) image.getParent()).getId(), ConstraintSet.BOTTOM);
        constraints.applyTo(root);

        // The banner contains icons for four pieces: a queen, rook, bishop, and knight. Given a
        // height, the banner and its contents automatically size themselves so that the banner
        // contains a vertical column of 4 squares, each containing a different piece. We set the
        // height so that each square in the banner is exactly the size of a Square on the board,
        // and give the banner a horizontal bias that lines it up perfectly directly beneath this
        // square.
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) promotionBanner.getLayoutParams();
        params.height = image.getHeight() * 4;
        params.horizontalBias = column * (1f / 7);
        promotionBanner.setLayoutParams(params);
    }

    /**
     * If there is a promotion banner currently attached to this square, remove it. Does nothing
     * otherwise.
     */
    public void detachPromotionBanner() {
        if (promotionBanner != null) {
            // Remove the promotionBanner form the screen and nullify our reference to it so that it
            // can be garbage-collected
            ((ViewGroup) promotionBanner.getParent()).removeView(promotionBanner);
            promotionBanner = null;
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
        return image.getX() + ((View) image.getParent()).getX() + ((View) image.getParent().getParent()).getX();
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
        return image.getY() + ((View) image.getParent()).getY() + ((View) image.getParent().getParent()).getY();
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
    private @DrawableRes
    int getDrawableID(Piece piece) {
        if (piece.getColour() == Colour.WHITE) {
            if (piece instanceof Pawn) {
                return R.drawable.white_pawn;
            } else if (piece instanceof Rook) {
                return R.drawable.white_rook;
            } else if (piece instanceof Knight) {
                return R.drawable.white_knight;
            } else if (piece instanceof Bishop) {
                return R.drawable.white_bishop;
            } else if (piece instanceof Queen) {
                return R.drawable.white_queen;
            }
            // Otherwise, piece is a King
            else {
                return R.drawable.white_king;
            }
        } else {
            if (piece instanceof Pawn) {
                return R.drawable.black_pawn;
            } else if (piece instanceof Rook) {
                return R.drawable.black_rook;
            } else if (piece instanceof Knight) {
                return R.drawable.black_knight;
            } else if (piece instanceof Bishop) {
                return R.drawable.black_bishop;
            } else if (piece instanceof Queen) {
                return R.drawable.black_queen;
            }
            // Otherwise, piece is a King
            else {
                return R.drawable.black_king;
            }
        }
    }

    /**
     * HIGHLIGHTS this square. This means highlighting it on the screen as one that can be moved to
     * by the user.
     *
     * @param capture - Whether or not to highlight this square as a potential capture, as opposed
     *                to a normal move. Capture squares will be highlighted red, while normal move
     *                squares will be highlighted a turquoise-ish colour
     */
    public void highlight(boolean capture) {
        if (capture) {
            this.image.setBackground(new ColorDrawable(0xFFFA1D1D));
        } else if (isLightSquare()) {
            this.image.setBackground(new ColorDrawable(0xFF62E69E));
        } else {
            this.image.setBackground(new ColorDrawable(0xFF4DB37B));
        }
    }

    /**
     * SELECTS this square. If the user taps a piece and selects it, we change the colour of the
     * square underneath it as a visual reminder of which piece was highlighted.
     */
    public void select() {
        this.image.setBackground(new ColorDrawable(0xFFB5FF54));
    }

    /**
     * Reset the background of this Square. Does not change what piece this Square is showing, but
     * resets the background if this square was previously highlighted or selected.
     */
    public void reset() {
        if (isLightSquare()) {
            image.setBackground(lightBackground);
        } else {
            image.setBackground(darkBackground);
        }
    }

    /**
     * Determine whether or not this square is a light square on the chessboard.
     *
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
        this.image.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // I discovered that if the user clicks a square, holds the click, and then drags
                // their finger all over the board, the touch events for the touch continue to get
                // sent to the original square. So the user could press down on one square, move
                // their finger, and release on another square, and the original square would get
                // all those events. I don't want it to work like that. For example, if the user has
                // a piece selected and clicks down on a square that that piece can move to, I want
                // them to be able to move their finger off that square and let go without moving.
                // They have to click both down and up on the same square to issue a move command.
                // So we add a check to make sure that the touch event hasn't left the bounds of the
                // square before sending off a touch event to the listener.
                float x = event.getX();
                float y = event.getY();
                if (0 <= x && x <= image.getWidth() && 0 <= y && y <= image.getHeight()) {
                    return listener.onTouch(row, column, event);
                } else {
                    return false;
                }
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
        this.image.setOnDragListener(new View.OnDragListener() {
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
         * @param row    - the row that the Square that received the event occupies on the screen
         * @param column - the column that the Square that received the event occupies on the screen
         * @param event  - contains information about the type of touch event that was received
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
         * @param row    - the row that the Square that received the event occupies on the screen
         * @param column - the column that the Square that received the event occupies on the screen
         * @param event  - the DragEvent that was received by this Square
         * @return true if the SquareDragListener wants to continue to receive drag events relating
         * to the current drag from this Square, false otherwise.
         */
        boolean onDrag(int row, int column, DragEvent event);
    }

    /**
     * A BannerListener is an object that can be attached to a promotion banner. When the user
     * selects the piece they want to promote their pawn into, the listener will receive a callback.
     */
    public interface BannerListener {
        /**
         * Called if the user selects the Queen
         */
        void queenPromotion();

        /**
         * Called if the user selects the Rook
         */
        void rookPromotion();

        /**
         * Called if the user selects the Bishop
         */
        void bishopPromotion();

        /**
         * Called if the user selects the Knight
         */
        void knightPromotion();
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
            outShadowTouchPoint.set(width / 2, height / 2);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            shadow.draw(canvas);
        }
    }
}
