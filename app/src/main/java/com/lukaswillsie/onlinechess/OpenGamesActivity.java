package com.lukaswillsie.onlinechess;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

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