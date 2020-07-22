package com.lukaswillsie.onlinechess.activities.board;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.lukaswillsie.onlinechess.R;

import Chess.com.lukaswillsie.chess.*;

/**
 * This class is responsible for managing the chessboard that we display on the screen.
 */
public class BoardDisplay {
    /**
     * Represents the chessboard being displayed on the screen. Each Square object is a wrapper for
     * a ConstraintLayout representing a square on the screen. This matrix maps onto the screen
     * as follows: board[0][0] is the bottom-left corner of the screen. board[1][0]
     */
    private Square[][] board = new Square[8][8];

    /**
     * The GamePresenter object whose data this object is displaying on the screen.
     */
    private GamePresenter presenter;

    /**
     * The Context for which this object is managing the UI
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
        this.context = layout.getContext();
        Resources resources = context.getResources();

        for(int i = 0; i < 8; i++) {
            View child = layout.getChildAt(i);
            if(child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                for(int j = 0; j < 8; j++) {
                    ConstraintLayout square_layout = new ConstraintLayout(context);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
                    square_layout.setLayoutParams(params);

                    // Here we give the TableLayout the look of a chessboard by giving it a checker
                    // pattern.
                    //
                    // Since we're iterating through the TableLayout and down,(i,j) = (0,0) is the
                    // top corner. Looking at the top row of a chessboard (i=0), even j-values
                    // indicate a light square. This is also the case for every other row going down
                    // the board; i.e. all the ones with even i-values. If we look at the second row
                    // from the top, where i = 1, we see that all the light squares have odd
                    // j-values. This pattern also repeats for every other row going down the board;
                    // i.e. all the rows with odd i-values. We use these patterns to identify which
                    // squares are white and which black.
                    if(i % 2 == 0 && j % 2 == 0 || i % 2 == 1 && j % 2 == 1) {
                        square_layout.setBackground(new ColorDrawable(resources.getColor(R.color.white)));
                    }
                    else {
                        square_layout.setBackground(new ColorDrawable(0xFF4BA2E3));
                    }

                    Square square = new Square(7 - i, 7 -j, square_layout);
                    board[7-i][7-j] = square;

                    row.addView(square_layout);
                }
            }
        }
    }

    /**
     * Binds this BoardDisplay object to the given GamePresenter. The screen will be updated to
     * display the board contained in the given GamePresenter.
     */
    public void setPresenter(GamePresenter presenter) {
        this.presenter = presenter;

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
            }
        }
    }
}
