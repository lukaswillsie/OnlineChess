package com.lukaswillsie.onlinechess.data;

/**
 * This enum codifies the data values that make up a single game. These are also the values that are
 * sent by the server over the connection after a user is logged in, so that the app has a list
 * of all games the user is involved in.
 *
 * There's also an array that defines what order the data is sent in by the server.
 *
 * This enum is a copy of the GameData enum in the server's source code, with some of the unnecessary
 * fields and methods removed.
 *
 * @author Lukas Willsie
 *
 */
public enum ServerData {
    GAMEID('s', 0),				// ID of the game
    WHITE('s', 1),				// Name of the user playing white
    BLACK('s', 2),				// Name of the user playing black
    OPEN('i', 3),				// 0 if the game is "open", meaning that all players
                                            // can view and join it if they want; 1 otherwise
    STATE('i', 4),				// State of the game; 0 if white's turn, 1 if black's turn
    TURN('i', 5),				// Current turn number
    WHITE_ARCHIVED('i', 6),	    // 1 if the white user has archived this game, 0 otherwise
    BLACK_ARCHIVED('i', 7),	    // 1 if the black user has archived this game, 0 otherwise
    DRAW_OFFERED('i', 8),		// 1 if a draw has been offered, and the player whose turn it is
                                    // needs to respond
    DRAWN('i', 9),				// 1 if the players in this game have agreed to a draw, 0 otherwise
    WINNER('s', 10),				// Contains the name of the winner, or the empty String if there isn't a winner
    WHITE_CHK('i', 11),		    // 1 if white is in check, 0 if not
    BLACK_CHK('i', 12),		    // 1 if black is in check, 0 if not
    PROMOTION_NEEDED('i', 13);	// 1 if the player whose turn it is needs to promote a piece before their turn can end

    // This array contains all the ServerData values, in the order in which the server sends them.
    // This is the exact same as the order in which they are declared above, but we choose to make
    // it explicit here.
    public static final ServerData[] order = {GAMEID, WHITE, BLACK, OPEN, STATE, TURN, WHITE_ARCHIVED, BLACK_ARCHIVED, DRAW_OFFERED, DRAWN, WINNER, WHITE_CHK, BLACK_CHK, PROMOTION_NEEDED};

    // One of 'i' or 's'. Represents whether the type of this data is int or String, respectively.
    public final char type;

    // The index of this piece of data in the above array. Equivalently, this piece of data's
    // position in the batches of data sent by the server. That is, if index=0, this piece of data
    // is always the first sent by the server.
    public final int index;

    /**
     * Create a GameData instance according to the given information
     *
     * @param type - The type of data this GameData instance is. In particular, either 'i' - integer, or 's' - String. This field
     * is used when processing data.
     */
    ServerData(char type, int index) {
        this.type = type;
        this.index = index;
    }
}