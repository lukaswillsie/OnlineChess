package com.lukaswillsie.onlinechess.activities;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Defines what an Activity that wants to use a Reconnector object needs to implement. A Reconnector
 * object is used to manage reconnecting to the server in the middle of execution. For example, if
 * the user is in the middle of playing a game and we discover we've lost our connection to the
 * server, we need an elegant way of reconnecting and logging in again, and that's what a
 * Reconnector provides.
 *
 * This class is called "Interior"Activity because Activities that require this service can be seen
 * as residing in the interior of the app, as the entry point of our app is a loading screen.
 */
public abstract class InteriorActivity extends AppCompatActivity {
    /**
     * Reconnector will call this method once a reconnection attempt has completely finished. That
     * is, once a connection to the server has been established and the user has been successfully
     * re-logged in. This callback is used to notify the Activity that they can resume normal
     * execution.
     */
    public abstract void reconnectionComplete();
}
