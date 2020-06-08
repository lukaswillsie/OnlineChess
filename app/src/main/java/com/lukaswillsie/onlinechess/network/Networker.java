package com.lukaswillsie.onlinechess.network;

/**
 * This interface is used to identify all activities making any request whatsoever of a ServerHelper
 * object. It also defines the one necessary behaviour common to all activities making requests of
 * a ServerHelper: communicating the eventuality of a system error to the user.
 *
 * No activity should implement this interface directly, but rather implement a more
 * specific sub-interface, whichever one(s) corresponds to the exact request(s) that the activity
 * may have to make.
 */
public interface Networker {
    void systemError();
}
