package com.lukaswillsie.onlinechess;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {
    private boolean progressOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText username = findViewById(R.id.username);
        username.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    v.setAlpha(1);
                }
                else {
                    v.setAlpha(0.5f);
                }
            }
        });

        EditText password = findViewById(R.id.password);
        password.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    v.setAlpha(1);
                }
                else {
                    v.setAlpha(0.5f);
                }
            }
        });
    }

    public void stopBar(View button) {
        if(progressOn) {
            findViewById(R.id.progress_bar).setVisibility(View.GONE);
        }
        else {
            findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
        }
        progressOn = !progressOn;
    }
}
