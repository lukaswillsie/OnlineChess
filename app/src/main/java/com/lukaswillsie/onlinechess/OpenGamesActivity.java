package com.lukaswillsie.onlinechess;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

/**
 * This activity simply displays a list of all currently open games (games that anyone can join) for
 * the user.
 */
public class OpenGamesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_games);
    }
}