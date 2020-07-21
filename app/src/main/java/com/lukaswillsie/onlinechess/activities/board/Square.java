package com.lukaswillsie.onlinechess.activities.board;

import androidx.constraintlayout.widget.ConstraintLayout;

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
     * Create a new Square with the given position, corresponding to the given layout on the screen
     * @param x - the square's x-coordinate, with x=0 representing the left side of the board
     * @param y - the square's y-coordinate, with y=0 representing the bottom of the board
     * @param layout - the ConstraintLayout corresponding to this square on the screen
     */
    public Square(int x, int y, ConstraintLayout layout) {
        this.x = x;
        this.y = y;
        this.layout = layout;
    }
}
