package com.lukaswillsie.onlinechess.data;

import java.util.HashMap;
import java.util.List;

/**
 * This class is essentially a wrapper for a HashMap, containing all the relevant information about
 * a game of chess, like the names of the player(s), whose turn it is, etc. Note that for games that
 * the user of the app is in, we have the class UserGame, so that we can more easily access
 * information specific to the user, like what their opponent's name is, whether or not the user
 * won, etc. This class is used for other games, ones that the user isn't in. It's simply a
 * collection of raw data.
 */
public class Game {
    private HashMap<ServerData, Object> data = new HashMap<>();

    /**
     * Initialize this Game object using the given stream of data. The given List should be a list
     * of data sent over by the server. It should have as many elements as there are elements in
     * ServerData.order, and each Object in the list should have type (int or String) equal to its
     * corresponding element in ServerData.order.
     *
     * @param serverData - the list of data from which to initialize this Game object.
     * @return 0 if this object is successfully initialized using the given data, 1 otherwise
     */
    public int initialize(List<Object> serverData) {
        if (serverData.size() != ServerData.order.length) {
            return 1;
        }

        for (int i = 0; i < serverData.size(); i++) {
            ServerData dataType = ServerData.order[i];

            if (dataType.type == 'i') {
                if (serverData.get(i) instanceof Integer) {
                    data.put(dataType, serverData.get(i));
                } else {
                    return 1;
                }
            } else if (dataType.type == 's') {
                if (serverData.get(i) instanceof String) {
                    data.put(dataType, serverData.get(i));
                } else {
                    return 1;
                }
            }
        }

        return 0;
    }

    /**
     * NOTE: This method shouldn't be used until initialize has been called and returned 0.
     * <p>
     * Get the specified piece of information about this game. This class ensures that if initialize
     * has been called and returned 0, each piece of data contained in this object is of the proper
     * type. That is, the piece of data associated with each ServerData key is guaranteed to have
     * type Integer if key.type == 'i', and type String if key.type == 's'. So the Object returned
     * by this method can be safely cast to Integer or String according to dataType.
     *
     * @param dataType - specifies which piece of data about this game to grab
     * @return the data in this Game object associated with the given enum
     */
    public Object getData(ServerData dataType) {
        return data.get(dataType);
    }
}
