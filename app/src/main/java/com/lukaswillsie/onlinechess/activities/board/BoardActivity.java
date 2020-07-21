package com.lukaswillsie.onlinechess.activities.board;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TableLayout;

import com.lukaswillsie.onlinechess.R;

public class BoardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        BoardDisplay display = new BoardDisplay();
        display.build((TableLayout) findViewById(R.id.board_layout));
    }
}