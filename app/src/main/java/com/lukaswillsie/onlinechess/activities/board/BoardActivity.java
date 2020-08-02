package com.lukaswillsie.onlinechess.activities.board;

import androidx.appcompat.app.AlertDialog;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.ErrorDialogActivity;
import com.lukaswillsie.onlinechess.activities.ReconnectListener;
import com.lukaswillsie.onlinechess.activities.Reconnector;
import com.lukaswillsie.onlinechess.data.GameData;
import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.helper.ServerHelper;
import com.lukaswillsie.onlinechess.network.helper.requesters.LoadGameRequester;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;

import java.util.List;

import Chess.com.lukaswillsie.chess.Board;

/**
 * BoardActivity is the most important Activity in the app; it allows users to actually view
 * their game boards and make moves.
 */
public class BoardActivity extends ErrorDialogActivity implements ReconnectListener, LoadGameRequester {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "BoardActivity";

    /**
     * Activities that start this Activity MUST use this tag to pass, as an extra in the intent,
     * the ID of the game that this Activity is supposed to load.
     */
    public static final String GAMEID_TAG = "gameID";

    /**
     * The ID of the game being displayed by this Activity
     */
    private String gameID;

    /**
     * The BoardDisplay object managing the UI
     */
    private BoardDisplay display;

    /**
     * The object managing the game being displayed by this Activity
     */
    private ChessManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        gameID = getIntent().getStringExtra(GAMEID_TAG);

        // Will create an empty chessboard on the screen
        display = new BoardDisplay();
        display.build((TableLayout) findViewById(R.id.board_layout));

        // Reconnect if necessary. Otherwise, try and fetch the data for the game we're supposed to
        // be loading
        ServerHelper serverHelper = ((ChessApplication)getApplicationContext()).getServerHelper();
        if(serverHelper == null) {
            new Reconnector(this, this).reconnect();
        }
        else {
            start();
        }
    }

    /**
     * Reconnector will call this method once a reconnection attempt has completely finished.
     */
    @Override
    public void reconnectionComplete() {
        start();
    }

    /**
     * Send a request to the server, trying to load the game this Activity has been told to load
     */
    private void start() {
        ServerHelper serverHelper = ((ChessApplication)getApplicationContext()).getServerHelper();
        try {
            serverHelper.loadGame(this, gameID);
        } catch (MultipleRequestException e) {
            Log.e(tag, "Tried to make multiple load game requests of serverHelper");
            this.createSystemErrorDialog();
        }
    }

    /**
     * Called after a load game request finishes successfully. The given Board object will represent
     * the requested game, and will have been initialized successfully from the data sent over by
     * the server.
     *
     * @param board - a Board object successfully initialized to hold all data associated with the
     *              requested game
     */
    @Override
    public void success(Board board) {
        // Find the UserGame object corresponding to the game we're supposed to be displaying
        List<UserGame> userGames = ((ChessApplication)getApplicationContext()).getGames();
        UserGame game = null;
        for(UserGame userGame : userGames) {
            if(userGame.getData(GameData.GAMEID).equals(gameID)) {
                game = userGame;
            }
        }

        // If we didn't find a UserGame object, something has gone wrong that we can't remedy, so we
        // display a dialog for the user that will finish the Activity and send them back to the
        // previous screen
        if(game == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Oops! Something went wrong and we couldn't show you your game.");
            builder.setPositiveButton(R.string.ok_dialog_button, new CriticalErrorListener());
            builder.setOnCancelListener(new CriticalErrorListener());
            return;
        }

        // Create a GamePresenter and GameManager for this game, now that we have all the data we
        // need
        GamePresenter presenter = new GamePresenter(game, board);
        manager = new ChessManager(presenter, display);
    }

    /**
     * A very simple listener class that listens to a dialog that we put up to notify the user of
     * a critical error. In the context of this Activity, this means an error we can't remedy. So
     * the listener just finishes this activity after the user cancels the dialog or clicks any
     * button.
     */
    private class CriticalErrorListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            BoardActivity.this.finish();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            BoardActivity.this.finish();
        }
    }

    /**
     * Called if the server responds to the request by saying the supplied gameID is not associated
     * with a game in its system
     */
    @Override
    public void gameDoesNotExist() {
        // Our app only allows users to view games that the server has told us they are a player
        // in. So if the server is now all of a sudden telling us those games don't exist, something
        // has gone wrong server-side.
        this.createServerErrorDialog();
    }

    /**
     * Called if the server responds to the request by saying that the user the app currently has
     * logged in is not a player in the game whose data was requested
     */
    @Override
    public void userNotInGame() {
        // Our app only allows users to view games that the server has told us they are a player
        // in. So if the server is now all of a sudden telling us the user isn't a player in a game,
        // something  has gone wrong server-side.
        this.createServerErrorDialog();
    }

    /**
     * Called by worker Threads that try and submit a request to the server but discover that the
     * connection to the server has been lost.
     */
    @Override
    public void connectionLost() {
        new Reconnector(this, this).reconnect();
    }
    /**
     * Called by worker Threads that try and submit a request to the server and are met with an
     * error on the server's part
     */
    @Override
    public void serverError() {
        this.createServerErrorDialog();
    }

    /**
     * Called by worker Threads that try and submit a request to the server but are met with a
     * system error
     */
    @Override
    public void systemError() {
        this.createSystemErrorDialog();
    }



    /* METHODS FOR INTERACTING WITH DIALOGS CREATED BY ErrorDialogActivity */


    /**
     * Provides behaviour in the event that the user presses "Try Again" on a system error dialog
     */
    @Override
    public void retrySystemError() {
        // Attempt to load the data we need again
        start();
    }

    /**
     * Provides behaviour in the event that the user presses "Cancel" on a system error dialog
     */
    @Override
    public void cancelSystemError() {
        finish();
    }

    /**
     * Provides behaviour in the event that the user presses "Try Again" on a server error dialog
     */
    @Override
    public void retryServerError() {
        // Attempt to load the data we need again
        start();
    }

    /**
     * Provides behaviour in the event that the user presses "Cancel" on a server error dialog
     */
    @Override
    public void cancelServerError() {
        finish();
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