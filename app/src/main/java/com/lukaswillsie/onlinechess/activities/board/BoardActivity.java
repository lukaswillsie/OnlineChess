package com.lukaswillsie.onlinechess.activities.board;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.Display;
import com.lukaswillsie.onlinechess.activities.ErrorDialogActivity;
import com.lukaswillsie.onlinechess.activities.ErrorDialogFragment;
import com.lukaswillsie.onlinechess.activities.ReconnectListener;
import com.lukaswillsie.onlinechess.activities.Reconnector;
import com.lukaswillsie.onlinechess.data.GameData;
import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.Server;
import com.lukaswillsie.onlinechess.network.helper.ServerHelper;
import com.lukaswillsie.onlinechess.network.helper.requesters.LoadGameRequester;
import com.lukaswillsie.onlinechess.network.helper.MultipleRequestException;

import java.util.ArrayList;
import java.util.List;

import Chess.com.lukaswillsie.chess.Board;

/**
 * BoardActivity is the most important Activity in the app; it allows users to actually view
 * their game boards and make moves.
 */
public class BoardActivity extends ErrorDialogActivity implements ReconnectListener, LoadGameRequester, GameDialogCreator {
    /**
     * Activities that start this Activity MUST use this tag to pass, as an extra in the intent,
     * the ID of the game that this Activity is supposed to load.
     */
    public static final String GAMEID_TAG = "gameID";
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "BoardActivity";
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
        display.build((ConstraintLayout) findViewById(R.id.board_layout));

        // Reconnect if necessary. Otherwise, try and fetch the data for the game we're supposed to
        // be loading
        ServerHelper serverHelper = Server.getServerHelper();
        if (serverHelper == null) {
            new Reconnector(this, this).reconnect();
        } else {
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
        ServerHelper serverHelper = Server.getServerHelper();
        try {
            serverHelper.loadGame(this, gameID);
        } catch (MultipleRequestException e) {
            Log.e(tag, "Tried to make multiple load game requests of serverHelper");
            this.showSystemErrorDialog();
        }
    }

    public void bannerOnClickTest(View v) {
        System.out.println("CLICKED");
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
        List<UserGame> userGames = Server.getGames();
        UserGame game = null;
        for (UserGame userGame : userGames) {
            if (userGame.getData(GameData.GAMEID).equals(gameID)) {
                game = userGame;
            }
        }

        // If we didn't find a UserGame object, something has gone wrong that we can't remedy, so we
        // display a dialog for the user that will finish the Activity and send them back to the
        // previous screen
        if (game == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Oops! Something went wrong and we couldn't show you your game.");
            builder.setPositiveButton(R.string.ok_dialog_button, new CriticalErrorListener());
            builder.setOnCancelListener(new CriticalErrorListener());
            return;
        }

        // Populate the screen with data about the game
        String gameID = (String) game.getData(GameData.GAMEID);
        int turn = (Integer) game.getData(GameData.TURN);
        String opponent = (String) game.getData(GameData.OPPONENT);
        if(opponent.length() == 0) {
            ((TextView) findViewById(R.id.opponent)).setText(R.string.game_no_opponent_text);
        }
        else {
            ((TextView) findViewById(R.id.opponent)).setText(getString(R.string.game_opponent_label, opponent));
        }
        ((TextView) findViewById(R.id.title)).setText(gameID);
        ((TextView) findViewById(R.id.turn_counter)).setText(getString(R.string.game_turn_number_label, Integer.toString(turn)));

        // Create a GamePresenter and GameManager for this game, now that we have all the data we
        // need
        GamePresenter presenter = new GamePresenter(game, board);
        if(manager == null) {
            manager = new ChessManager(gameID, presenter, display, this, this);
        }
        else {
            manager.setGame(gameID, presenter);
        }
    }

    /**
     * Called when the user presses the "Next" button. The idea behind the "Next" and "Previous"
     * buttons is that we want to allow the user to quickly cycle through all the games in which
     * it's their turn to make a move. So here we fetch a list of all such games, and load a new
     * game for the user depending on what game they're viewing now.
     *
     * @param v - the view that was clicked
     */
    public void nextGame(View v) {
        // Get a list of all active games in which it is the user's turn to make a move
        List<UserGame> activeGames = new ArrayList<>();
        List<UserGame> games = Server.getGames();
        for (UserGame game : games) {
            if (!((int) game.getData(GameData.ARCHIVED) == 1)) {
                if (!game.isOver() && !game.isOpponentTurn()) {
                    Log.i(tag, "Game " + game.getData(GameData.GAMEID) + " is over");
                    activeGames.add(game);
                }
            }
        }

        // Get the index of the current game in the list of games in which it is the user's turn
        int index = -1;
        for(int i = 0; i < activeGames.size(); i++) {
            if(activeGames.get(i).getData(GameData.GAMEID).equals(gameID)) {
                index = i;
                break;
            }
        }

        // If the user is not viewing a game in which it is their turn, we jump to a game in which
        // do have to make a move, namely the first in our list. If there aren't ANY games they have
        // to make a move in, we notify them of this fact.
        if(index == -1) {
            if(activeGames.size() == 0) {
                Display.makeToast(this, "There are no games in which it is your turn", Toast.LENGTH_LONG);
            }
            else {
                this.gameID = (String) activeGames.get(0).getData(GameData.GAMEID);
                start();
            }
        }
        else {
            // Increment index, or set index to 0 if it's already at the end of activeGames
            index = (index == activeGames.size()-1) ? 0 : index + 1;

            this.gameID = (String) activeGames.get(index).getData(GameData.GAMEID);
            start();
        }
    }

    /**
     * Called when the user presses the "Previous" button. The idea behind the "Next" and "Previous"
     * buttons is that we want to allow the user to quickly cycle through all the games in which
     * it's their turn to make a move. So here we fetch a list of all such games, and load a new
     * game for the user depending on what game they're viewing now.
     *
     * @param v - the view that was clicked
     */
    public void previousGame(View v) {
        // Get a list of all active games in which it is the user's turn to make a move
        List<UserGame> activeGames = new ArrayList<>();
        List<UserGame> games = Server.getGames();
        for (UserGame game : games) {
            if (!((int) game.getData(GameData.ARCHIVED) == 1)) {
                if (!game.isOver() && !game.isOpponentTurn()) {
                    Log.i(tag, "Game " + game.getData(GameData.GAMEID) + " is over");
                    activeGames.add(game);
                }
            }
        }

        // Get the index of the current game in the list of games in which it is the user's turn
        int index = -1;
        for(int i = 0; i < activeGames.size(); i++) {
            if(activeGames.get(i).getData(GameData.GAMEID).equals(gameID)) {
                index = i;
                break;
            }
        }

        // If the user is not viewing a game in which it is their turn, we show them the game which
        // is first in the list. Or notify them if there are no games in which it is their turn.
        if(index == -1) {
            if(activeGames.size() == 0) {
                Display.makeToast(this, "There are no games in which it is your turn", Toast.LENGTH_LONG);
            }
            else {
                this.gameID = (String) activeGames.get(0).getData(GameData.GAMEID);
                start();
            }
        }
        else {
            // Decrement index, or set it to activeGames.size() - 1 if it's already 0
            index = (index == 0) ? activeGames.size() - 1 : index - 1;

            this.gameID = (String) activeGames.get(index).getData(GameData.GAMEID);
            start();
        }
    }

    /**
     * Displays an error dialog for the user. The error dialog will contain the specified String
     * resource in its message. The dialog will have "Try Again" and "Cancel" buttons. These buttons
     * will be wired to the retry() and cancel() method of the given listener, respectively. The
     * dialog will be cancellable (the user can click outside of the dialog to make it go away), and
     * the given listener will receive a callback to its cancel() method if the dialog is cancelled.
     *
     * @param messageID - the ID of the String resource that will be the dialog's message
     * @param listener  - the object that will receive the callbacks from the created dialog
     */
    @Override
    public void showErrorDialog(int messageID, ErrorDialogFragment.CancellableErrorDialogListener listener) {
        showCustomDialog(messageID, listener);
    }

    /**
     * Displays an error dialog for the user, notifying them of a loss of connection. The dialog
     * will contain the given String resource as its message and will have one button, which will
     * say "Try Again", and will be connected to the retry() method of the listener.
     *
     * @param messageID - the ID of the String resource that will be the dialog's message
     * @param listener  - the object that will receive the callbacks from the created dialog
     */
    @Override
    public void showConnectionLostDialog(int messageID, ErrorDialogFragment.ErrorDialogListener listener) {
        showCustomDialog(messageID, listener);
    }

    /**
     * Notifies the user that they won the game. The dialog can be dismissed if the player wants to
     * inspect the board.
     */
    @Override
    public void showUserWinDialog(boolean resigned) {
        if(resigned) {
            showOutcomeDialog(Outcome.WIN_RESIGN);
        }
        else {
            showOutcomeDialog(Outcome.WIN);
        }
    }

    /**
     * Notifies the user that they lost the game. The dialog can be dismissed if the player wants to
     * inspect the board.
     */
    @Override
    public void showUserLoseDialog() {
        showOutcomeDialog(Outcome.LOSE);
    }

    /**
     * Notifies the user that they drew the game. The dialog can be dismissed if the player wants to
     * inspect the board.
     */
    @Override
    public void showUserDrawDialog() {
        showOutcomeDialog(Outcome.DRAW);
    }

    private void showOutcomeDialog(Outcome outcome) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);



        switch (outcome) {
            case WIN:
                builder.setView(getLayoutInflater().inflate(R.layout.win_game_dialog, null));
                break;
            case WIN_RESIGN:
                builder.setView(getLayoutInflater().inflate(R.layout.win_game_resign_dialog, null));
                break;
            case LOSE:
                builder.setView(getLayoutInflater().inflate(R.layout.lose_game_dialog, null));
                break;
            case DRAW:
                builder.setView(getLayoutInflater().inflate(R.layout.draw_game_dialog, null));
                break;
        }

        builder.setPositiveButton(R.string.ok_dialog_button, null);
        builder.show();
    }

    private enum Outcome {
        WIN,
        WIN_RESIGN,
        LOSE,
        DRAW;
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
        this.showServerErrorDialog();
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
        this.showServerErrorDialog();
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
        this.showServerErrorDialog();
    }

    /**
     * Called by worker Threads that try and submit a request to the server but are met with a
     * system error
     */
    @Override
    public void systemError() {
        this.showSystemErrorDialog();
    }

    /**
     * Provides behaviour in the event that the user presses "Try Again" on a system error dialog
     */
    @Override
    public void retrySystemError() {
        // Attempt to load the data we need again
        start();
    }



    /* METHODS FOR INTERACTING WITH DIALOGS CREATED BY ErrorDialogActivity */

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
}