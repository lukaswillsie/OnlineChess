package com.lukaswillsie.onlinechess.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.lukaswillsie.onlinechess.activities.active_games.ActiveGamesActivity;
import com.lukaswillsie.onlinechess.R;

public class MainActivity extends InteriorActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void activeGames(View view) {
        startActivity(new Intent(this, ActiveGamesActivity.class));
    }

    @Override
    public String getTag() {
        return "MainActivity";
    }
}