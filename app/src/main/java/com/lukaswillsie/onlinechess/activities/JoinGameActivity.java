package com.lukaswillsie.onlinechess.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.game_display.OpenGamesActivity;
import com.lukaswillsie.onlinechess.data.Format;
import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.helper.requesters.JoinGameRequester;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;

/**
 * This activity allows the user to join games. They can either enter the ID of a game their friend
 * has created, and join a game that way, or view a list of all games in the system that are open,
 * meaning anyone can join them.
 */
public class JoinGameActivity extends ErrorDialogActivity implements JoinGameRequester, ReconnectListener {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "JoinGameActivity";

    /**
     * We use this enum to keep track of what state the Activity is currently in, with respect to
     * a join game request.
     */
    private enum State {
        WAITING,    // We're waiting for the user to click the "join" button
        JOINING,    // The user has clicked the "join" button and we're waiting for the server
                    // to tell us whether or not the join was successful
        LOADING;    // The join was successful and we're waiting for the server to send us the
                    // game data
    }

    /**
     * Keeps track of the Activity's current state
     */
    private State state;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_game);

        this.state = State.WAITING;

        // Change the colour of the ProgressBar hidden in one of our buttons. We do this
        // programmatically because the only way to do it in XML requires API 21. This code courtesy
        // of Zeyad Assem on Stack Overflow, from the following post:
        // https://stackoverflow.com/questions/2638161/how-to-change-android-indeterminate-progressbar-color
        ProgressBar progressBar = findViewById(R.id.join_game_progress);
        progressBar.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.white),
                android.graphics.PorterDuff.Mode.SRC_IN);

        if(((ChessApplication)getApplicationContext()).getServerHelper() == null) {
            new Reconnector(this, this).reconnect();
        }
    }

    /**
     * We implement this method for ReconnectListener. It's meant to notify us after a Reconnector
     * has finished a successful reconnect request. We don't do anything special when this happens,
     * so we leave this method blank.
     */
    @Override
    public void reconnectionComplete() {}

    /**
     * An onclick, called when the user clicks the "Join" button.
     * @param view - the View that was clicked
     */
    public void joinGame(View view) {
        if(this.state == State.WAITING) {
            handleRequest();
        }
    }

    /**
     * Grab whatever is in the gameID EditText and attempt to send a join game request to the
     * server. If the gameID is invalidly formatted, displays a dialog instead
     */
    private void handleRequest() {
        String gameID = ((EditText)findViewById(R.id.join_game_input)).getText().toString();

        if(Format.validGameID(gameID)) {
            // Hide the ProgressBar in the button and make the text re-appear
            findViewById(R.id.join_game_progress).setVisibility(View.VISIBLE);
            findViewById(R.id.join_button_text).setVisibility(View.INVISIBLE);

            ChessApplication application = ((ChessApplication)getApplicationContext());
            try {
                application.getServerHelper().joinGame(this, gameID, application.getUsername());
            } catch (MultipleRequestException e) {
                Log.e(tag, "MultipleRequestException thrown by JoinGameHelper");
                super.showSystemErrorDialog();
                return;
            }

            this.state = State.JOINING;
            // Close the keyboard and make the cursor in the EditText go away to provide a nice,
            // clean UI while we process the request
            Display.hideKeyboard(this);
            findViewById(R.id.join_game_input).setFocusable(false);
        }
        else {
            Display.showSimpleDialog(R.string.invalid_gameID_format, this);
        }

    }

    /**
     * Reset the UI to its initial state; the one given by the XML layout. Undoes any changes that
     * may have been done because of a join game request being processed.
     */
    private void resetUI() {
        // Re-activate and empty the gameID EditText
        EditText gameID = findViewById(R.id.join_game_input);
        gameID.setText("");
        gameID.setFocusableInTouchMode(true);
        gameID.setFocusable(true);

        // Reset the "join" button's appearance
        findViewById(R.id.join_game_progress).setVisibility(View.GONE);
        findViewById(R.id.join_button_text).setVisibility(View.VISIBLE);
        findViewById(R.id.join_layout).setBackground(getResources().getDrawable(R.drawable.join_game_button_selector));
    }



    /**
     * An onclick method; is called when the user wants to look at a list of open games
     *
     * @param view - the view that was clicked
     */
    public void openGames(View view) {
        if(this.state == State.WAITING) {
            startActivity(new Intent(this, OpenGamesActivity.class));
        }
    }

    /**
     * Called if the server says we successfully joined the specified game
     *
     * NOTE: This does NOT indicate the end of the request; the data associated with the game that
     * was joined still needs to be received from the server. This is just a stop on the way.
     */
    @Override
    public void gameJoined() {
        if(this.state == State.JOINING) {
            findViewById(R.id.join_layout).setBackgroundColor(getResources().getColor(R.color.join_game_successful));
            this.state = State.LOADING;
        }
    }

    /**
     * Called if the gameID we gave isn't associated with a game, according to the server
     */
    @Override
    public void gameDoesNotExist() {
        if(this.state == State.JOINING) {
            resetUI();
            this.state = State.WAITING;

            Display.showSimpleDialog(R.string.game_does_not_exist_dialog_text, this);
        }
    }

    /**
     * Called if the gameID we gave is associated with a game that cannot be joined because it is
     * full
     */
    @Override
    public void gameFull() {
        if(this.state == State.JOINING) {
            resetUI();
            this.state = State.WAITING;

            Display.showSimpleDialog(R.string.game_full_dialog_text, this);
        }
    }

    /**
     * Called if the game we tried to join can't be joined because the user is already in it
     */
    @Override
    public void userAlreadyInGame() {
        if(this.state == State.JOINING) {
            resetUI();
            this.state = State.WAITING;

            Display.showSimpleDialog(R.string.user_already_in_game_dialog_text, this);
        }
    }

    /**
     * Called once a join game request is fully complete. A UserGame object containing information
     * about the game that was joined is passed as an argument
     *
     * @param game - an object containing all the necessary information about the game that was joined
     */
    @Override
    public void joinGameComplete(UserGame game) {
        if(this.state == State.LOADING) {
            this.state = State.WAITING;
            Display.makeToast(this, getString(R.string.game_joined_text, ((EditText)findViewById(R.id.join_game_input)).getText().toString()), Toast.LENGTH_LONG);
            resetUI();

            // Add the received game to our list of the user's games
            ((ChessApplication)getApplicationContext()).getGames().add(game);
        }
    }

    /**
     * Called if our connection to the server is discovered to be lost at any point in an overall
     * join game request
     */
    @Override
    public void connectionLost() {
        if(this.state != State.WAITING) {
            resetUI();
            this.state = State.WAITING;
            super.showConnectionLostDialog();
        }
    }

    /**
     * Called if a server error is encountered at any point in an overall join game request
     */
    @Override
    public void serverError() {
        if(this.state != State.WAITING) {
            super.showServerErrorDialog();
        }
    }

    /**
     * Called if a system error is encountered at any point in an overall join game request
     */
    @Override
    public void systemError() {
        if(this.state != State.WAITING) {
            super.showSystemErrorDialog();
        }
    }

    /* CODE FOR INTERACTING WITH ERROR DIALOGS*/

    /**
     * Provides behaviour in the event that the user presses "Try Again" on a system error dialog
     */
    @Override
    public void retrySystemError() {
        handleRequest();
    }

    /**
     * Provides behaviour in the event that the user presses "Cancel" on a system error dialog
     */
    @Override
    public void cancelSystemError() {
        resetUI();
        this.state = State.WAITING;
    }

    /**
     * Provides behaviour in the event that the user presses "Try Again" on a server error dialog
     */
    @Override
    public void retryServerError() {
        handleRequest();
    }

    /**
     * Provides behaviour in the event that the user presses "Cancel" on a server error dialog
     */
    @Override
    public void cancelServerError() {
        resetUI();
        this.state = State.WAITING;
    }

    /**
     * Provides behaviour in the event that the user presses "Try Again" on a lost connection error
     * dialog.
     */
    @Override
    public void retryConnection() {
        new Reconnector(this, this).reconnect();
    }
}