package com.lukaswillsie.onlinechess.data;

/**
 * Defines all the relevant pieces of data that a Game object should contain. Values of this enum
 * can be passed to a Game object's getData() method to extract the relevant piece of information.
 * <p>
 * This is different from the ServerData enum in that ServerData defines what kind of data
 * the server sends over the connection after a user is logged in. This includes some data that our
 * app doesn't need, like information that pertains to both players in the game.
 * <p>
 * After the information from the server is processed and packaged into Game objects so that all
 * that is left is the information specific to our currently logged-in user, this enum defines the
 * data that is left.
 * <p>
 * accessed through the Game objects.
 */
public enum GameData {
    GAMEID('s'),                  // ID of the game
    OPPONENT('s'),                // Name of the user's opponent in the game. Empty if the
                                        // game is new and there is no opponent yet
    OPEN('i'),                    // 1 if the game is an open game, 0 otherwise
    STATE('i'),                    // State of the game; 1 if it's the user's turn, 0 if it's
                                        // the opponent's. We note that this does not necessarily
                                        // mean the user can actually move a piece. It could be
                                        // their turn because they need to promote a pawn. This may
                                        // also have value 1 even if the game is already over.
    TURN('i'),                    // Current turn number
    ARCHIVED('i'),                // 1 if the user has archived this game, 0 otherwise
    DRAW_OFFERED('i'),            // 1 if a draw has been offered to the player whose turn it
                                        // is, 0 otherwise
    DRAWN('i'),                    // 1 if the players in this game have agreed to a draw, 0 otherwise
    USER_WON('i'),                // 1 if the user won this game, 0 otherwise
    USER_LOST('i'),               // 1 if the user lost this game, 0 otherwise
    FORFEIT('i'),                 // If either player has won the game, indicates whether the
                                        // victory was by forfeit or checkmate. 1 if the victory was
                                        // by forfeit, 0 if by checkmate. If neither player has won,
                                        // means nothing.
    CHECK('i'),                    // 1 if the user is in check, 0 if not
    PROMOTION_NEEDED('i');        // 1 if the user needs to promote a pawn, 0 otherwise (note
                                        // that this is only possible if STATE == 1)

    private char type;

    GameData(char type) {
        this.type = type;
    }

    public char getType() {
        return type;
    }
}