package com.lukaswillsie.onlinechess.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.board.BoardActivity;
import com.lukaswillsie.onlinechess.activities.game_display.ActiveGamesActivity;
import com.lukaswillsie.onlinechess.activities.game_display.ArchivedGamesActivity;
import com.lukaswillsie.onlinechess.activities.login.LoginActivity;
import com.lukaswillsie.onlinechess.data.RememberMeHelper;
import com.lukaswillsie.onlinechess.network.Server;

import java.io.IOException;

/**
 * MainActivity is the main screen of our app; the one with our title and a list of buttons allowing
 * the user to navigate our app.
 */
public class MainActivity extends AppCompatActivity implements ReconnectListener {
    /**
     * Tag used for logging to the console
     */
    private static String tag = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // This is how we detect that the system destroyed our app while it was running in the
        // background and is now restarting it.
        if (Server.getGames() == null) {
            // Re-establish a connection with the server and re-login the user
            new Reconnector(this, this).reconnect();
        }
    }

    /**
     * Onclick event for the "Create Game" button
     *
     * @param view - the View that was clicked
     */
    public void createGame(View view) {
        startActivity(new Intent(this, CreateGameActivity.class));
    }

    /**
     * Onclick event for the "Join Game" button
     *
     * @param view - the View that was clicked
     */
    public void joinGame(View view) {
        startActivity(new Intent(this, JoinGameActivity.class));
    }

    /**
     * Onclick event for the "Active Games" button
     *
     * @param view - the View that was clicked
     */
    public void activeGames(View view) {
        startActivity(new Intent(this, ActiveGamesActivity.class));
    }

    /**
     * Onclick event for the "Archived Games" button
     *
     * @param view - the View that was clicked
     */
    public void archivedGames(View view) {
        startActivity(new Intent(this, ArchivedGamesActivity.class));
    }

    /**
     * Onclick for "Test" button. We use it to test BoardActivity
     *
     * @param v - the view that was clicked
     */
    public void test(View v) {
        Intent intent = new Intent(this, BoardActivity.class);

        // This is the ID of a game I've created on the server and know exists, so I can use it for
        // testing purposes
        intent.putExtra(BoardActivity.GAMEID_TAG, "third");
        startActivity(intent);
    }

    public void logout(View v) {
        // Ensures that the user who's currently logged in won't automatically be logged in next
        // time, if they clicked "Remember Me" when logging in.
        try {
            new RememberMeHelper(this).logout();
        } catch (IOException e) {
            Log.e(tag, "Couldn't create RememberMeHelper to log out the user");
        }

        Server.logout();
        startActivity(new Intent(this, LoginActivity.class));
    }

    /**
     * Called by Reconnector when a reconnection process is complete. We don't do anything special
     * when this happens.
     */
    @Override
    public void reconnectionComplete() {
    }
}
