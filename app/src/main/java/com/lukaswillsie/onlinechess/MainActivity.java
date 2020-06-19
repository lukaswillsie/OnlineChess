package com.lukaswillsie.onlinechess;

import android.os.Bundle;
import android.util.Log;

import com.lukaswillsie.onlinechess.activities.InteriorActivity;

public class MainActivity extends InteriorActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public String getTag() {
        return "MainActivity";
    }
}
