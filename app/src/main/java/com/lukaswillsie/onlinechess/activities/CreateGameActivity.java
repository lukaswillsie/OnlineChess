package com.lukaswillsie.onlinechess.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.data.Format;
import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.Server;
import com.lukaswillsie.onlinechess.network.helper.requesters.CreateGameRequester;
import com.lukaswillsie.onlinechess.network.helper.MultipleRequestException;

import static android.widget.Toast.LENGTH_LONG;

/**
 * This Activity is responsible for allowing users to create games
 */
public class CreateGameActivity extends ErrorDialogActivity implements CreateGameRequester, ReconnectListener {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "CreateGameActivity";

    /**
     * Keeps track of the current state of this activity
     */
    private State state;

    /**
     * We implement this method so that we can use Reconnector objects to re-establish a connection
     * to the server if we discover our connection has been lost.
     */
    @Override
    public void reconnectionComplete() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_game);

        this.state = State.WAITING;

        // Change the colour of the ProgressBar hidden in one of our buttons. We do this
        // programmatically because the only way to do it in XML requires API 21. This code courtesy
        // of Zeyad Assem on Stack Overflow, from the following post:
        // https://stackoverflow.com/questions/2638161/how-to-change-android-indeterminate-progressbar-color
        ProgressBar progressBar = findViewById(R.id.create_game_progress);
        progressBar.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.white),
                android.graphics.PorterDuff.Mode.SRC_IN);

        if (Server.getServerHelper() == null) {
            new Reconnector(this, this).reconnect();
        }
    }

    /**
     * Resets the activity by resetting the UI to its natural state (the state given by the XML) and
     * resetting state to WAITING.
     */
    private void resetUI() {
        EditText gameID = findViewById(R.id.create_game_input);
        gameID.setText("");
        gameID.setFocusable(true);
        gameID.setFocusableInTouchMode(true);

        findViewById(R.id.create_game_progress).setVisibility(View.GONE);
        findViewById(R.id.create_game_button_text).setVisibility(View.VISIBLE);
    }

    /**
     * Reset the appearance of the "Create" button
     */
    private void resetButton() {
        findViewById(R.id.create_game_progress).setVisibility(View.GONE);
        findViewById(R.id.create_game_button_text).setVisibility(View.VISIBLE);
    }

    /**
     * Reset this object's state
     */
    private void resetState() {
        this.state = State.WAITING;
    }

    /**
     * Called when the user clicks the "Create" button. We grab what they typed in the EditText and
     * try to create a game with that ID
     *
     * @param v - the View that was clicked
     */
    public void createGame(View v) {
        handleRequest();
    }

    /**
     * Take whatever is in the EditText on the screen and, if it's validly formatted, send a create
     * game request to the server.
     */
    private void handleRequest() {
        if (this.state == State.WAITING) {
            String gameID = ((EditText) findViewById(R.id.create_game_input)).getText().toString();
            boolean open = ((CheckBox) findViewById(R.id.open_checkbox)).isChecked();
            String username = Server.getUsername();

            if (Format.validGameID(gameID)) {
                try {
                    Server.getServerHelper().createGame(this, gameID, open, username);
                } catch (MultipleRequestException e) {
                    Log.e(tag, "Sent multiple requests to ServerHelper");
                    showSystemErrorDialog();
                    return;
                }

                this.state = State.PROCESSING;

                Display.hideKeyboard(this);
                findViewById(R.id.create_game_progress).setVisibility(View.VISIBLE);
                findViewById(R.id.create_game_button_text).setVisibility(View.INVISIBLE);
            } else {
                Display.showSimpleDialog(R.string.gameID_format_invalid_text, this);
            }
        }
    }

    /**
     * Called if our create game request is foiled due a loss of our connection with the server
     */
    @Override
    public void connectionLost() {
        if (this.state == State.PROCESSING) {
            resetButton();
            showConnectionLostDialog();
        }
    }

    /**
     * Called if our create game request is foiled by a server error
     */
    @Override
    public void serverError() {
        if (this.state == State.PROCESSING) {
            resetButton();
            showServerErrorDialog();
        }
    }

    /**
     * Called if our create game request is foiled by a system error
     */
    @Override
    public void systemError() {
        if (this.state == State.PROCESSING) {
            resetButton();
            showSystemErrorDialog();
        }
    }

    /**
     * Called if our create game request terminates successfully
     *
     * @param game - a UserGame object representing the newly-created game
     */
    @Override
    public void gameCreated(UserGame game) {
        if (this.state == State.PROCESSING) {
            Display.makeToast(this, "Game successfully created!", LENGTH_LONG);

            // Add the new game to our list of the user's games
            Server.getGames().add(game);

            resetUI();
            resetState();
        }
    }

    /**
     * Called if our create game request fails because the ID provided by the user is already in
     * use
     */
    @Override
    public void gameIDInUse() {
        if (this.state == State.PROCESSING) {
            Display.showSimpleDialog(R.string.gameID_in_use_text, this);

            resetUI();
            resetState();
        }
    }

    /**
     * Called if our create game request fails because the gameID provided by the user is invalidly
     * formatted. We check format before sending requests to prevent this from happening, but
     * implement this method as a redundancy.
     */
    @Override
    public void invalidFormat() {
        if (this.state == State.PROCESSING) {
            Display.showSimpleDialog(R.string.gameID_format_invalid_text, this);

            resetUI();
            resetState();
        }
    }

    @Override
    public void retrySystemError() {
        handleRequest();
    }

    /* METHODS FOR INTERACTING WITH DIALOGS CREATED BY ErrorDialogActivity */

    @Override
    public void cancelSystemError() {
        resetUI();
        resetState();
    }

    @Override
    public void retryServerError() {
        handleRequest();
    }

    @Override
    public void cancelServerError() {
        resetUI();
        resetState();
    }

    @Override
    public void retryConnection() {
        new Reconnector(this, this).reconnect();
    }

    /**
     * Represents the possible "states" that this Activity can be in
     */
    private enum State {
        WAITING,    // We're waiting for the user to click the "Create" button
        PROCESSING // The user has clicked the "Create" button, we've sent a create request to
        // ServerHelper and are waiting for the response
    }
}