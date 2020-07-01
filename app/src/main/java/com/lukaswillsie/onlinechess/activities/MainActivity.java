package com.lukaswillsie.onlinechess.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.activities.game_display.ActiveGamesActivity;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.game_display.ArchivedGamesActivity;

/**
 * MainActivity is the main screen of our app; the one with our title and a list of buttons allowing
 * the user to navigate our app.
 */
public class MainActivity extends InteriorActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // This is how we detect that the system destroyed our app while it was running in the
        // background and is now restarting it.
        if(((ChessApplication) getApplicationContext()).getGames() == null) {
            // Re-establish a connection with the server and re-login the user
            new Reconnector(this).reconnect();
        }
    }

    /**
     * Onclick event for the "Active Games" button
     * @param view - the view that was clicked
     */
    public void activeGames(View view) {
        startActivity(new Intent(this, ActiveGamesActivity.class));
    }

    /**
     * Onclick event for the "Archived Games" button
     * @param view - the view that was clicked
     */
    public void archivedGames(View view) {
        startActivity(new Intent(this, ArchivedGamesActivity.class));
    }

    /**
     * Called by Reconnector when a reconnection process is complete. We don't do anything special
     * when this happens.
     */
    @Override
    public void loginComplete() {}
}
