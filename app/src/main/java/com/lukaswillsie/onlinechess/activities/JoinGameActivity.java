package com.lukaswillsie.onlinechess.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lukaswillsie.onlinechess.R;

/**
 * This activity allows the user to join games. They can either enter the ID of a game their friend
 * has created, and join a game that way, or view a list of all games in the system that are open,
 * meaning anyone can join them.
 */
public class JoinGameActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_game);
    }

    public void joinGame(View view) {
        findViewById(R.id.join_game_progress).setVisibility(View.VISIBLE);
        findViewById(R.id.join_button_text).setVisibility(View.INVISIBLE);
    }

    /**
     * An onclick method; is called when the user wants to look at a list of open games
     *
     * @param view - the view that was clicked
     */
    public void openGames(View view) {
        startActivity(new Intent(this, OpenGamesActivity.class));
    }
}