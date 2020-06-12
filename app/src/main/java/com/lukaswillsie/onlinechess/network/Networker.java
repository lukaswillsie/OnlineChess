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
    /**
     * This method will be called if a system error occurs during the processing of a network
     * request. For example, if the internet has gone down, or an output/input stream cannot be
     * opened, or is causing some other problem. Simply allows the calling activity to differentiate
     * how it communicates errors to the user.
     */
    void systemError();
}
