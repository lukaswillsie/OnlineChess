package com.lukaswillsie.onlinechess.activities.board;

import Chess.com.lukaswillsie.chess.Pair;

/**
 * Represents, very simply, a move on a chessboard. The move is characterized simply by its
 * beginning and end points, represented here as Pairs.
 */
public class Move {
    public final Pair src;
    public final Pair dest;

    public Move(Pair src, Pair dest) {
        this.src = src;
        this.dest = dest;
    }
}
