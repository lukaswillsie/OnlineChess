package com.lukaswillsie.onlinechess.data;

import android.util.Log;

import java.util.HashMap;
import java.util.List;

/**
 * A class that represents a chess game by encapsulating the information associated with it,
 * like the name of the game, the name of the user's opponent, whose turn it is, etc.
 *
 * Objects of this kind should only be created AFTER A USER HAS BEEN LOGGED IN.
 */
public class Game {
    private static final String tag = "Game";

    private HashMap<GameData, Object> data;

    /**
     * The name of the user currently logged into the app
     */
    private String username;

    /**
     * Create an empty new Game object. This object should not be used until initialize() is called.
     *
     * @param username - The name of the user currently logged into the app
     */
    public Game(String username) {
        this.username = username;
    }

    /**
     * Initialize this Game object from the given source data. data is assumed to be a list of
     * Objects, all either Integers or Strings.
     *
     * See the Protocols.pdf document in the ChessServer repo for more details about how the server
     * sends game data to the app, but below is a summary of the details relevant to this class.
     *
     * After the app logs a user in, the server sends a sequence of batches of transmissions. Each
     * batch corresponds to a game the user is involved in, and consists of the same data points,
     * defined in the ServerData enum, always in the exact same order. The order is also defined in
     * the ServerData enum. Each data point should be converted to a String or Integer according
     * to the "type" field in the ServerData enum, and then each batch of data points should be
     * converted into a list, keeping the same order as they were sent in by the server, and then
     * each list should be given to a different Game object via their initialize() method.
     *
     * This method will unpack the data from the server and extract the information relevant to
     * the app.
     *
     * @param serverData - a list of Strings and Integers corresponding to important data about a game,
     *             sent by the server to the app
     * @return  0 if this object is correctly initialized
     *          1 if the data received by this object in the form of serverData is incorrect in
     *          some way. In this event, the object logs the error before returning.
     */
    public int initialize(List<Object> serverData) {
        this.data = new HashMap<>();
        try {
            // Assign the gameID
            data.put(GameData.GAMEID, this.getServerData(serverData, ServerData.GAMEID));

            // The data that we need to extract from serverData and store in this Game object is all
            // the information specific to the user we have logged in. So first we figure out if the
            // user is black or white
            String white = (String) this.getServerData(serverData, ServerData.WHITE);
            String black = (String) this.getServerData(serverData, ServerData.BLACK);

            int state;
            if(white.equals(username)) {
                data.put(GameData.OPPONENT, black);

                // The server sends the state of the game as a bit: 0 or 1, where 0 means that it's
                // white's turn and 1 that it's black's turn. The state that we store in Game objects
                // is 1 if it's the USER's turn, 0 otherwise.
                state = (((Integer)this.getServerData(serverData, ServerData.STATE)).equals(0)) ? 1 : 0;
                data.put(GameData.STATE, state);

                data.put(GameData.ARCHIVED, this.getServerData(serverData, ServerData.WHITE_ARCHIVED));

                data.put(GameData.CHECK, this.getServerData(serverData, ServerData.WHITE_CHK));
            }
            else if(black.equals(username)) {
                data.put(GameData.OPPONENT, white);

                // The server sends the state of the game as a bit: 0 or 1, where 0 means that it's
                // white's turn and 1 that it's black's turn. The state that we store in Game objects
                // is 1 if it's the USER's turn, 0 otherwise.
                state = (((Integer)this.getServerData(serverData, ServerData.STATE)).equals(1)) ? 1 : 0;
                data.put(GameData.STATE, state);

                data.put(GameData.ARCHIVED, this.getServerData(serverData, ServerData.BLACK_ARCHIVED));

                data.put(GameData.CHECK, this.getServerData(serverData, ServerData.BLACK_CHK));
            }
            else {
                Log.e(tag, "Tried to create game " + this.data.get(GameData.GAMEID) + " but user is not a player in it");
                return 1;
            }

            data.put(GameData.OPEN, this.getServerData(serverData, ServerData.OPEN));

            data.put(GameData.TURN, this.getServerData(serverData, ServerData.TURN));

            // The user has only been offered a draw if it's their turn AND a draw has been offered
            data.put(GameData.DRAW_OFFERED, (state == 1 && ((Integer)this.getServerData(serverData, ServerData.DRAW_OFFERED)).equals(1)) ? 1 : 0);

            data.put(GameData.DRAWN, this.getServerData(serverData, ServerData.DRAWN));

            // If the user won, set USER_WON and USER_LOST accordingly
            if(this.getServerData(serverData, ServerData.WINNER).equals(username)) {
                data.put(GameData.USER_WON, 1);
                data.put(GameData.USER_LOST, 0);
            }
            // If the user didn't win, and the WINNER field from ServerData is non-empty, then the
            // opponent won
            else if(!this.getServerData(serverData, ServerData.WINNER).equals("")) {
                data.put(GameData.USER_WON, 0);
                data.put(GameData.USER_LOST, 1);
            }
            // Otherwise, WINNER is empty and so nobody won
            else {
                data.put(GameData.USER_WON, 0);
                data.put(GameData.USER_LOST, 0);
            }

            return 0;
        }
        catch(ClassCastException e) {
            Log.e(tag, "Some data couldn't be correctly cast from list: " + serverData);
            return 1;
        }
        catch(NumberFormatException e) {
            Log.e(tag, "Some data couldn't be parsed into int from list: " + serverData);
            return 1;
        }
        catch(IndexOutOfBoundsException e) {
            Log.e(tag, "There weren't enough data points in list: " + serverData);
            return 1;
        }
    }

    public Object getData(GameData data) {
        return this.data.get(data);
    }

    /**
     * Extract the piece of data corresponding to the given dataType from the given List, which is
     * assumed to be a list of data received from the server. See the javadoc for the above
     * constructor for  details.
     *
     * @param serverData - the List to extract data from
     * @param dataType - identifies which piece of data to extract
     * @return the given piece of data, returned as an Object, uncasted
     */
    private Object getServerData(List<Object> serverData, ServerData dataType) {
        return serverData.get(dataType.index);
    }

    /**
     * Mark this game as archived by the user.
     */
    public void setArchived(boolean archived) {
        if(archived) {
            this.data.put(GameData.ARCHIVED, 1);
        }
        else {
            this.data.put(GameData.ARCHIVED, 0);
        }
    }

    public String toString() {
        if(this.data == null) {
            return "No game data";
        }
        else {
            StringBuilder builder = new StringBuilder();
            for(GameData data : GameData.values()) {
                builder.append(data.toString() + ": " + this.data.get(data).toString() + "\n");
            }

            return builder.toString();
        }
    }
}
