package com.lukaswillsie.onlinechess.activities.board;

/**
 * An enum that simply and cleanly represents the possible pieces that can go on a chessboard. The
 * sub-enum PromotePiece defines the set of pieces that a pawn can be promoted into. It also
 * associates with each piece a character to represent it. This is the character that we will send to
 * the server as part of a promotion request, so it should conform to protocol. This enum allows us
 * to write cleaner, more readable code when handling promotions.
 */
public enum PieceType {
    PAWN,
    ROOK,
    KNIGHT,
    BISHOP,
    QUEEN,
    KING;

    public enum PromotePiece {
        ROOK('r'),
        KNIGHT('n'),
        BISHOP('b'),
        QUEEN('q');

        public final char charRep;

        PromotePiece(char charRep) {
            this.charRep = charRep;
        }
    }
}
