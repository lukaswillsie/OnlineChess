package com.lukaswillsie.onlinechess.activities;

/**
 * Defines what a class that wants to use a Reconnector object needs to implement. A Reconnector
 * object is used to manage reconnecting to the server at runtime. For example, if
 * the user is in the middle of playing a game and we discover we've lost our connection to the
 * server, we need an elegant way of reconnecting and logging in again, and that's what a
 * Reconnector provides. This interface allows us to make reconnect requests of a Reconnector
 * and be notified once they are complete.
 */
public interface ReconnectListener {
    /**
     * Reconnector will call this method once a reconnection attempt has completely finished. That
     * is, once a connection to the server has been established and the user has been successfully
     * re-logged in. This callback is used to notify the Activity that they can resume normal
     * execution.
     */
    void reconnectionComplete();
}
