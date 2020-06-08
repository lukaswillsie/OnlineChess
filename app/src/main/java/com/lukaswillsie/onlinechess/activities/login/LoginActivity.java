package com.lukaswillsie.onlinechess.activities.login;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.MainActivity;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.ErrorDialogFragment;
import com.lukaswillsie.onlinechess.activities.load.LoadActivity;
import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.Keys;
import com.lukaswillsie.onlinechess.network.LoginRequester;
import com.lukaswillsie.onlinechess.network.ServerHelper;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;

import java.util.List;

public class LoginActivity extends AppCompatActivity implements LoginRequester {
    private static final String tag = "LoginActivity";
    private ServerHelper serverHelper;
    private State state;

    /**
     * Represents the possible states that this activity can be in
     */
    private enum State {
        WAITING,    // The user is entering their data and hasn't pressed "LOGIN" yet
        LOGGING_IN, // The user has pressed "LOGIN" and the server hasn't validated their
                    // credentials yet
        LOADING;    // The user's credentials have been validated, and the app is processing the
                    // game data sent over by the server
    }

    private class SystemErrorDialogListener implements ErrorDialogFragment.ErrorDialogListener {
        @Override
        public void retry() {
           processLogin();
        }
    }

    private class ServerErrorDialogListener implements ErrorDialogFragment.ErrorDialogListener {
        @Override
        public void retry() {
            processLogin();
        }
    }

    private class ConnectionLostDialogListener implements ErrorDialogFragment.ErrorDialogListener {
        private LoginActivity creator;

        public ConnectionLostDialogListener(LoginActivity creator) {
            this.creator = creator;
        }
        @Override
        public void retry() {
            Intent intent = new Intent(this.creator, LoadActivity.class);
            creator.startActivity(intent);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.state = State.WAITING;

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

    public void login(View view) {
        this.processLogin();
    }

    private void processLogin() {
        // We only want to perform an action if we're at the first stage, and we don't have an
        // ongoing login request being handled
        if(this.state == State.WAITING) {
            // Hide any error text that may be showing from past login attempts
            findViewById(R.id.login_input_error).setVisibility(View.INVISIBLE);

            // Change login button colour to indicate processing start
            CardView loginCard = findViewById(R.id.login);
            loginCard.setCardBackgroundColor(getResources().getColor(R.color.loginLoading));

            // Replace "LOGIN" text with a progress bar and text saying "Processing credentials..."
            // until we get a response from the server
            ((TextView) findViewById(R.id.login_button_text)).setText(R.string.login_processing_text);
            findViewById(R.id.login_progress).setVisibility(View.VISIBLE);

            // Prevent the user from interacting with the EditTexts until the login request is complete
            hideKeyboard();
            findViewById(R.id.username).setFocusable(false);
            findViewById(R.id.password).setFocusable(false);

            String username = ((EditText) findViewById(R.id.username)).getText().toString();
            String password = ((EditText) findViewById(R.id.password)).getText().toString();

            this.state = State.LOGGING_IN;

            if(Format.validUsername(username) && Format.validPassword(password)) {
                try {
                    serverHelper.login(this, username, password);
                } catch (MultipleRequestException e) {
                    Log.e(tag, "Submitted multiple requests to ServerHelper");
                }
            }
            else {
                this.invalidInfo();
            }
        }
    }

    private void invalidInfo() {
        // TODO: Decide what to do with invalidly formatted input
    }

    /**
     * Hide the keyboard, so that we present the user with a nice clean loading interface after they
     * press the LOGIN button.
     *
     * This code was found on StackOverflow at the following address:
     *
     * https://stackoverflow.com/questions/1109022/close-hide-android-soft-keyboard?answertab=votes#tab-top
     */
    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();

        if(view == null) {
            view = new View(this);
        }

        manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void loginSuccess() {
        // Change login button colour to indicate successful login
        CardView loginCard = findViewById(R.id.login);
        loginCard.setCardBackgroundColor(getResources().getColor(R.color.loginSuccessful));

        // Change login button text to indicate change in login request status to user
        ((TextView)findViewById(R.id.login_button_text)).setText(R.string.loading_text);
    }

    @Override
    public void usernameInvalid() {
        // This method should only be called while the activity is in the below state
        if(this.state == State.LOGGING_IN) {
            // Display appropriate error text
            TextView errorText = findViewById(R.id.login_input_error);
            errorText.setText(R.string.invalid_username_error);
            errorText.setVisibility(View.VISIBLE);

            // Change login button colour to indicate that the user can try to login again
            CardView loginCard = findViewById(R.id.login);
            loginCard.setCardBackgroundColor(Color.parseColor("#000000"));

            // Reactivate the username and password EditTexts
            EditText username = findViewById(R.id.username);
            username.setText("");
            username.setFocusableInTouchMode(true);
            username.setFocusable(true);

            EditText password = findViewById(R.id.password);
            password.setText("");
            password.setFocusableInTouchMode(true);
            password.setFocusable(true);

            // Reset the login button text to "LOGIN" and hide progress bar
            ((TextView)findViewById(R.id.login_button_text)).setText(R.string.login_button);
            findViewById(R.id.login_progress).setVisibility(View.INVISIBLE);

            this.state = State.WAITING;
        }
        else {
            Log.e(tag, "usernameInvalid() called while not in " + State.LOGGING_IN + " state.");
        }
    }

    @Override
    public void passwordInvalid() {
        // This method should only be called while the activity is in the below state
        if(this.state == State.LOGGING_IN) {
            // Display appropriate error text
            TextView errorText = findViewById(R.id.login_input_error);
            errorText.setText(R.string.invalid_password_error);
            errorText.setVisibility(View.VISIBLE);

            // Change login button colour to indicate that the user can try to login again
            CardView loginCard = findViewById(R.id.login);
            loginCard.setCardBackgroundColor(Color.parseColor("#000000"));

            // Reactivate the username EditText
            EditText username = findViewById(R.id.username);
            username.setText("");
            username.setFocusableInTouchMode(true);
            username.setFocusable(true);

            // Reactivate the password EditText
            EditText password = findViewById(R.id.password);
            password.setText("");
            password.setFocusableInTouchMode(true);
            password.setFocusable(true);

            // Reset the login button text to "LOGIN" and hide progress bar
            ((TextView)findViewById(R.id.login_button_text)).setText(R.string.login_button);
            findViewById(R.id.login_progress).setVisibility(View.INVISIBLE);

            this.state = State.WAITING;
        }
        else {
            Log.e(tag, "usernameInvalid() called while not in " + State.LOGGING_IN + " state.");
        }
    }

    @Override
    public void loginComplete(List<Game> games) {
        ((ChessApplication)getApplicationContext()).setGames(games);

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void connectionLost() {
        DialogFragment dialog = new ErrorDialogFragment(new ConnectionLostDialogListener(this), getResources().getString(R.string.connection_lost_alert));
        dialog.show(getSupportFragmentManager(), "connection_lost_dialog");
    }

    @Override
    public void serverError() {
        DialogFragment dialog = new ErrorDialogFragment(new ServerErrorDialogListener(), getResources().getString(R.string.connection_lost_alert));
        dialog.show(getSupportFragmentManager(), "server_error_dialog");
    }

    @Override
    public void systemError() {
        DialogFragment dialog = new ErrorDialogFragment(new SystemErrorDialogListener(), getResources().getString(R.string.connection_lost_alert));
        dialog.show(getSupportFragmentManager(), "system_error_dialog");
    }
}
