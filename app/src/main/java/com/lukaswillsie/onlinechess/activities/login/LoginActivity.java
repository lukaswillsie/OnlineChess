package com.lukaswillsie.onlinechess.activities.login;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.MainActivity;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.EditTextActivity;
import com.lukaswillsie.onlinechess.activities.ErrorDialogFragment;
import com.lukaswillsie.onlinechess.activities.load.LoadActivity;
import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.network.helper.LoginRequester;
import com.lukaswillsie.onlinechess.network.helper.ServerHelper;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;

import java.util.List;

public class LoginActivity extends EditTextActivity implements LoginRequester {
    private static final String tag = "LoginActivity";
    private ServerHelper serverHelper;
    private State state;

    /**
     * Represents the possible states that this activity can be in
     */
    private enum State {
        WAITING_FOR_USER_INPUT,         // The user is entering their data and hasn't pressed "LOGIN" yet
        WAITING_FOR_SERVER_RESPONSE,    // The user has pressed "LOGIN" but the server hasn't validated their
                                        // credentials yet
        LOADING;                        // The user's credentials have been validated, and now the app is processing the
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

        this.state = State.WAITING_FOR_USER_INPUT;

        // Put onFocusChangeListeners on EditTexts so they become darker when focused
        this.styleEditText((EditText) findViewById(R.id.username));
        this.styleEditText((EditText) findViewById(R.id.password));

        serverHelper = ((ChessApplication)getApplicationContext()).getServerHelper();
        Log.i(tag, "serverHelper is " + serverHelper);
    }

    /**
     * Called when the user presses the login button.
     *
     * @param view - the login button that was pressed
     */
    public void login(View view) {
        this.processLogin();
    }

    /**
     * Called when the user wants to create a new account
     * @param view - the View that the user clicked to communicate that they want to create a new
     *             account
     */
    public void createNewAccount(View view) {
        Intent intent = new Intent(this, CreateAccountActivity.class);
        this.startActivity(intent);
    }

    /**
     * Handles a login request by the user. Changes the display to indicate a loading proccess by
     * closing the keyboard, disabling the username and password EditTexts, displaying a circular
     * ProgressBar inside of the login button, and changing the login button text to
     * "Processing credentials..."
     */
    private void processLogin() {
        // We only want to perform an action if we're at the first stage, and we don't have an
        // ongoing login request being handled
        if(this.state == State.WAITING_FOR_USER_INPUT) {
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

            if(Format.validUsername(username) && Format.validPassword(password)) {
                try {
                    serverHelper.login(this, username, password);
                    this.state = State.WAITING_FOR_SERVER_RESPONSE;
                } catch (MultipleRequestException e) {
                    // TODO: Decide what to do here. This should never happen.
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

    /**
     * This callback is for when the server has responded that the user's credentials are valid.
     * This occurs BEFORE the server sends over the user's game data, so it doesn't necessarily
     * mean the login process is complete and that this Activity should move to the next. It simply
     * allows this activity to notify the user of the progress of the login request. In this case,
     * we change the colour of the login button and change the text being displayed to the user from
     * "Processing credentials..." to "Loading".
     */
    @Override
    public void loginSuccess() {
        if(this.state == State.WAITING_FOR_SERVER_RESPONSE) {
            // Change login button colour to indicate successful login
            CardView loginCard = findViewById(R.id.login);
            loginCard.setCardBackgroundColor(getResources().getColor(R.color.loginSuccessful));

            // Change login button text to indicate change in login request status to user
            ((TextView)findViewById(R.id.login_button_text)).setText(R.string.loading_text);

            this.state = State.LOADING;
        }
    }

    /**
     * This callback is for when the server has responded that the username entered by the user
     * doesn't exist in its records. Displays an error message for the user and wakes up the
     * EditTexts to allow a new login request.
     */
    @Override
    public void usernameInvalid() {
        // This method should only be called while the activity is in the below state
        if(this.state == State.WAITING_FOR_SERVER_RESPONSE) {
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

            this.state = State.WAITING_FOR_USER_INPUT;
        }
        else {
            Log.e(tag, "usernameInvalid() called while not in " + State.WAITING_FOR_SERVER_RESPONSE + " state.");
        }
    }

    /**
     * Called when the server has responded that the password entered by the user doesn't match the
     * username entered by the user. Displays an error message for the user and wakes up the
     * EditTexts to allow a new login request.
     */
    @Override
    public void passwordInvalid() {
        // This method should only be called while the activity is in the below state
        if(this.state == State.WAITING_FOR_SERVER_RESPONSE) {
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

            this.state = State.WAITING_FOR_USER_INPUT;
        }
        else {
            Log.e(tag, "usernameInvalid() called while not in " + State.WAITING_FOR_SERVER_RESPONSE + " state.");
        }
    }

    /**
     * Is called after the ServerHelper has fully processed data sent over by the server, to notify
     * this Activity that the application can now proceed. Takes a list of Game objects, each of
     * which is a wrapper for data representing a game the newly logged-in user is playing, so that
     * this data can be saved for later global access by the application.
     *
     * @param games - A list of objects representing every game that the logged-in user is a
     *              participant in
     */
    @Override
    public void loginComplete(List<Game> games) {
        // This callback should only be used when the activity is in the below state
        if(this.state == State.LOADING) {
            // Save the list of games globally
            ((ChessApplication) getApplicationContext()).setGames(games);

            // Move to the next Activity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

    /**
     * Is called if a request is made but the server is found to be unresponsive during the course
     * of handling the request. Shows a dialog box notifying the user of what happened, and offers
     * to let them try again.
     */
    @Override
    public void connectionLost() {
        DialogFragment dialog = new ErrorDialogFragment(new ConnectionLostDialogListener(this), getResources().getString(R.string.connection_lost_alert));
        dialog.show(getSupportFragmentManager(), "connection_lost_dialog");
    }

    /**
     * Is called if the server ever responds to a request with ReturnCodes.SERVER_ERROR, or if
     * the data received from the server is wrong, and doesn't correspond to its established
     * protocols. Shows a dialog box notifying the user of what happened, and offers to let them
     * try again.
     */
    @Override
    public void serverError() {
        DialogFragment dialog = new ErrorDialogFragment(new ServerErrorDialogListener(), getResources().getString(R.string.server_error_alert));
        dialog.show(getSupportFragmentManager(), "server_error_dialog");
    }

    /**
     * This method will be called if a system error occurs during the processing of a network
     * request. For example, if the internet has gone down, or an output/input stream cannot be
     * opened, or is causing some other problem. Shows a dialog box notifying the user of what
     * happened, and offers to let them try again.
     */
    @Override
    public void systemError() {
        DialogFragment dialog = new ErrorDialogFragment(new SystemErrorDialogListener(), getResources().getString(R.string.system_error_alert));
        dialog.show(getSupportFragmentManager(), "system_error_dialog");
    }
}
