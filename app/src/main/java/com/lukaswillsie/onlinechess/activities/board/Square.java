package com.lukaswillsie.onlinechess.activities.board;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;

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
     * This square's x-coordinate
     */
    private final int x;

    /**
     * This square's y-coordinate
     */
    private final int y;

    /**
     * The ConstraintLayout corresponding to this square on the screen
     */
    private ConstraintLayout layout;

    /**
     * The Context containing the ConstraintLayout that this class is managing
     */
    private Context context;

    /**
     * Contains a reference to the ImageView currently being displayed in this square. null if there
     * is no such ImageView.
     */
    private ImageView image;

    /**
     * Create a new Square with the given position, corresponding to the given layout on the screen
     * @param x - the square's x-coordinate, with x=0 representing the left side of the board
     * @param y - the square's y-coordinate, with y=0 representing the bottom of the board
     * @param layout - the ConstraintLayout corresponding to this square on the screen
     */
    public Square(int x, int y, ConstraintLayout layout) {
        this.x = x;
        this.y = y;
        this.layout = layout;
        this.context = layout.getContext();
    }

    public void setPiece(Piece piece) {
        if(image != null) {
            layout.removeView(image);
        }

        image = createView(piece);
        layout.addView(image);
    }

    /**
     * Create an ImageView to hold a representation of the given piece in the ConstraintLayout
     * being manager by this object.
     *
     * @param piece - the piece that will be displayed in the ImageView
     * @return An ImageView containing a representation of the given piece, ready to be placed
     * inside the ConstraintLayout that this object is wrapping
     */
    private ImageView createView(Piece piece) {
        ImageView imageView = new ImageView(context);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        imageView.setLayoutParams(params);

        if(piece.getColour() == Colour.WHITE) {
            if(piece instanceof Pawn) {
                imageView.setBackground(context.getResources().getDrawable(R.drawable.white_pawn));
            }
            else if(piece instanceof Rook) {
                imageView.setBackground(context.getResources().getDrawable(R.drawable.white_rook));
            }
            else if(piece instanceof Knight) {
                imageView.setBackground(context.getResources().getDrawable(R.drawable.white_knight));
            }
            else if(piece instanceof Bishop) {
                imageView.setBackground(context.getResources().getDrawable(R.drawable.white_bishop));
            }
            else if(piece instanceof Queen) {
                imageView.setBackground(context.getResources().getDrawable(R.drawable.white_queen));
            }
            // Otherwise, piece is a King
            else {
                imageView.setBackground(context.getResources().getDrawable(R.drawable.white_king));
            }
        }
        else {
            if(piece instanceof Pawn) {
                imageView.setBackground(context.getResources().getDrawable(R.drawable.black_pawn));
            }
            else if(piece instanceof Rook) {
                imageView.setBackground(context.getResources().getDrawable(R.drawable.black_rook));
            }
            else if(piece instanceof Knight) {
                imageView.setBackground(context.getResources().getDrawable(R.drawable.black_knight));
            }
            else if(piece instanceof Bishop) {
                imageView.setBackground(context.getResources().getDrawable(R.drawable.black_bishop));
            }
            else if(piece instanceof Queen) {
                imageView.setBackground(context.getResources().getDrawable(R.drawable.black_queen));
            }
            // Otherwise, piece is a King
            else {
                imageView.setBackground(context.getResources().getDrawable(R.drawable.black_king));
            }
        }

        return imageView;
    }
}
