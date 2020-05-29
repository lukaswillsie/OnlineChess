package com.lukaswillsie.onlinechess.activities.login;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.data.Keys;
import com.lukaswillsie.onlinechess.network.ServerHelper;

public class LoginActivity extends AppCompatActivity {
    private static final String tag = "LoginActivity";
    private ServerHelper serverHelper;

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

        serverHelper = ((ChessApplication)getApplicationContext()).getServerHelper();
    }

    public void login(View button) {

    }
}
