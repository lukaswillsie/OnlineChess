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
import com.lukaswillsie.onlinechess.network.helper.requesters.DrawRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.ForfeitRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.LoadGameRequester;
import com.lukaswillsie.onlinechess.network.helper.MultipleRequestException;
import com.lukaswillsie.onlinechess.network.helper.requesters.RejectRequester;

import java.util.ArrayList;
import java.util.List;

import Chess.com.lukaswillsie.chess.Board;

/**
 * BoardActivity is the most important Activity in the app; it allows users to actually view
 * their game boards and make moves.
 */
public class BoardActivity extends ErrorDialogActivity implements ReconnectListener, LoadGameRequester, GameDialogCreator,  GameListener {
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
    /*
     * The UserGame object containing information about the game that we are currently displaying
     */
    private UserGame game;
    /**
     * The BoardDisplay object managing the UI
     */
    private BoardDisplay display;

    /**
     * The object managing the game being displayed by this Activity
     */
    private ChessManager manager;
    /**
     * If true, means we are currently submitting a draw/resign request to the server, and shouldn't
     * submit any more requests. Otherwise, we are free to submit a request after the user clicks
     * one of the "Draw" or "Resign" buttons.
     */
    private boolean activeRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        String gameID = getIntent().getStringExtra(GAMEID_TAG);

        // Will create an empty chessboard on the screen
        display = new BoardDisplay();
        display.build((ConstraintLayout) findViewById(R.id.board_layout));

        // Reconnect if necessary. Otherwise, try and fetch the data for the game we're supposed to
        // be loading
        ServerHelper serverHelper = Server.getServerHelper();
        if (serverHelper == null) {
            new Reconnector(this, this).reconnect();
        } else {
            start(gameID);
        }
    }

    /**
     * Called if the user clicks the "Draw" button
     *
     * @param v - the View that was clicked
     */
    public void draw(View v) {
        if(manager != null && !activeRequest) {
            try {
                Server.getServerHelper().draw(new OfferDrawRequestListener(), gameID);
                activeRequest = true;
            } catch (MultipleRequestException e) {
                Log.e(tag, "ServerHelper says we submitted multiple draw offer requests, even though this should never happen");
            }
        }
    }

    /**
     * Called when the user clicks the "Refresh" button
     *
     * @param v - the View that was clicked
     */
    public void refresh(View v) {
        if(manager != null) {
            start(gameID);
        }
    }

    /**
     * Called if the user clicks the "Resign" button
     *
     * @param v - the View that was clicked
     */
    public void resign(View v) {
        if(manager != null && !activeRequest) {
            try {
                Server.getServerHelper().forfeit(new ForfeitRequestListener(), gameID);
                activeRequest = true;
            } catch (MultipleRequestException e) {
                Log.e(tag, "ServerHelper says we submitted multiple draw offer requests, even though this should never happen");
            }
        }
    }

    /**
     * Reconnector will call this method once a reconnection attempt has completely finished.
     */
    @Override
    public void reconnectionComplete() {
        start(gameID);
    }

    /**
     * Called by ChessManager after the user makes a move. Here, we call setUI() to make sure that
     * all of our satellite data TextViews are updated, for example the one that says "Your turn" or
     * "{opponent}'s turn".
     */
    @Override
    public void userMoved() {
        setUI();
    }

    /**
     * Sends a request to the server to load the game with the given gameID. Once the server has
     * responded, that game will be displayed on the screen. If the given gameID does not correspond
     * to a game in the user's list of games, will display a dialog for the user that finishes
     * this activity when closed.
     *
     * @param gameID - the ID of the game that we want to switch to displaying
     */
    private void start(String gameID) {
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
        this.gameID = gameID;

        ServerHelper serverHelper = Server.getServerHelper();
        try {
            serverHelper.loadGame(this, gameID, Server.getUsername());
        } catch (MultipleRequestException e) {
            Log.e(tag, "Tried to make multiple load game requests of serverHelper");
            this.showSystemErrorDialog();
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
    public void success(Board board, UserGame game) {
        // Create a GamePresenter and GameManager for this game, now that we have all the data we
        // need
        GamePresenter presenter = new GamePresenter(game, board);
        this.game = game;
        if(manager == null) {
            manager = new ChessManager(gameID, presenter, display, this, this, this);
        }
        else {
            manager.setGame(gameID, presenter);
        }

        // We search our list of the user's games, trying to find the one that we just received from
        // the server. When we find it, we replace it in the list with the new version just sent
        // over by the server. This ensures our model is always up to date.
        // Note that we can be sure we are going to find it, because we would have already searched
        // for it in start()
        for(int i = 0; i < Server.getGames().size(); i++) {
            if(Server.getGames().get(i).getData(GameData.GAMEID).equals(game.getData(GameData.GAMEID))) {
                Server.getGames().set(i, game);
                break;
            }
        }

        setUI();
    }

    /**
     * This method sets the state of the UI so that it perfectly reflects the state of the game
     * being displayed. It ensures that the various TextViews containing information about the
     * game contain the correct information. It also ensures that the visibility of the "Draw" and
     * "Resign" buttons is set properly, according to whether or not it is the user's turn
     */
    private void setUI() {
        if (game != null) {
            // Populate the screen with data about the game
            String gameID = (String) game.getData(GameData.GAMEID);
            int turn = (Integer) game.getData(GameData.TURN);
            int drawOffered = (Integer) game.getData(GameData.DRAW_OFFERED);
            int state = (Integer) game.getData(GameData.STATE);
            String opponent = (String) game.getData(GameData.OPPONENT);

            if(opponent.length() == 0) {
                ((TextView) findViewById(R.id.opponent)).setText(R.string.game_no_opponent_text);
            }
            else {
                ((TextView) findViewById(R.id.opponent)).setText(getString(R.string.game_opponent_label, opponent));
            }
            ((TextView) findViewById(R.id.title)).setText(gameID);
            ((TextView) findViewById(R.id.turn_counter)).setText(getString(R.string.game_turn_number_label, Integer.toString(turn)));

            TextView stateLabel = findViewById(R.id.state);
            if(game.isOver()) {
                if((Integer) game.getData(GameData.USER_WON) == 1) {
                    stateLabel.setTextColor(getResources().getColor(R.color.user_win));
                    stateLabel.setText(R.string.user_won_state_label);
                }
                else if((Integer) game.getData(GameData.USER_LOST) == 1) {
                    stateLabel.setTextColor(getResources().getColor(R.color.user_lose));
                    stateLabel.setText(R.string.user_lost_state_label);
                }
                // The game is a draw
                else {
                    stateLabel.setTextColor(getResources().getColor(R.color.light_gray));
                    stateLabel.setText(R.string.draw_state_label);
                }
            }
            else if(state == 1) {
                stateLabel.setTextColor(getResources().getColor(R.color.user_turn));
                stateLabel.setText(R.string.user_turn_state_label);
            }
            else {
                stateLabel.setTextColor(getResources().getColor(R.color.light_gray));

                if (drawOffered == 1) {
                    stateLabel.setText(R.string.opponent_turn_draw_offer_state_label);
                }
                else {
                    stateLabel.setText(getString(R.string.opponent_move_state_label, opponent));
                }
            }

            // We assume that the state label should be visible, and the draw offer layout
            // invisible. If the user has been offered a draw, we're going to reverse this, but it's
            // simpler to assume otherwise.
            stateLabel.setVisibility(View.VISIBLE);
            findViewById(R.id.draw_offer_layout).setVisibility(View.GONE);

            // Figure out what to do with the "Draw" and "Resign" buttons, as well as the draw offer
            // layout
            if(!game.isOver() && state == 1) {
                // If the user has been offered a draw, show the draw offer layout and hide the
                // "Draw" and "Resign" buttons
                if(drawOffered == 1) {
                    showDrawOfferLayout();
                    stateLabel.setVisibility(View.GONE);
                    findViewById(R.id.draw_placeholder).setVisibility(View.VISIBLE);
                    findViewById(R.id.resign_placeholder).setVisibility(View.VISIBLE);
                    findViewById(R.id.offer_draw_button).setVisibility(View.GONE);
                    findViewById(R.id.resign_button).setVisibility(View.GONE);
                }
                // Otherwise, it's the user's turn to move/promote, so we can show the "Draw" and
                // "Resign" buttons
                else {
                    findViewById(R.id.draw_placeholder).setVisibility(View.GONE);
                    findViewById(R.id.resign_placeholder).setVisibility(View.GONE);
                    findViewById(R.id.offer_draw_button).setVisibility(View.VISIBLE);
                    findViewById(R.id.resign_button).setVisibility(View.VISIBLE);
                }
            }
            // Otherwise, hide the "Draw" and "Resign" buttons because the user can't offer a draw or
            // resign if it's not their turn (or if the game is over)
            else {
                findViewById(R.id.draw_placeholder).setVisibility(View.VISIBLE);
                findViewById(R.id.resign_placeholder).setVisibility(View.VISIBLE);
                findViewById(R.id.offer_draw_button).setVisibility(View.GONE);
                findViewById(R.id.resign_button).setVisibility(View.GONE);
            }
        }
    }

    /**
     * Reveals the layout on the screen that allows users to respond to a draw offer by their
     * opponent.
     */
    private void showDrawOfferLayout() {
        ConstraintLayout drawOfferLayout = findViewById(R.id.draw_offer_layout);
        drawOfferLayout.setVisibility(View.VISIBLE);
        drawOfferLayout.findViewById(R.id.reject_offer_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Server.getServerHelper().reject(new AcceptRejectListener(false), gameID);
                } catch (MultipleRequestException e) {
                    // This might happen if the user rapidly clicks the accept button multiple
                    // times. We simply do nothing here, and wait for the request that the first
                    // click submitted to be handled.
                    Log.e(tag, "Submitted multiple draw offer accept requests to ServerHelper");
                }
            }
        });
        drawOfferLayout.findViewById(R.id.accept_offer_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Server.getServerHelper().draw(new AcceptRejectListener(true), gameID);
                } catch (MultipleRequestException e) {
                    // This might happen if the user rapidly clicks the reject button multiple
                    // times. We simply do nothing here, and wait for the request that the first
                    // click submitted to be handled.
                    Log.e(tag, "Submitted multiple draw offer reject requests to ServerHelper");
                }
            }
        });
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
                start((String) activeGames.get(0).getData(GameData.GAMEID));
            }
        }
        // Otherwise, the game we are currently displaying is an active game
        else {
            // If there is only one active game, the one we are displaying, we don't have to do
            // anything. So we only load a new game if activesGames.size() is not equal to 1.
            if(activeGames.size() != 1) {
                // Increment index, or set index to 0 if it's already at the end of activeGames
                index = (index == activeGames.size()-1) ? 0 : index + 1;

                start((String) activeGames.get(index).getData(GameData.GAMEID));
            }
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
                start((String) activeGames.get(0).getData(GameData.GAMEID));
            }
        }
        else {
            // Decrement index, or set it to activeGames.size() - 1 if it's already 0
            index = (index == 0) ? activeGames.size() - 1 : index - 1;

            start((String) activeGames.get(index).getData(GameData.GAMEID));
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

    /**
     * Represents the different possible outcomes that a game of chess can have.
     */
    private enum Outcome {
        WIN,        // The user won through checkmate
        WIN_RESIGN, // The user won because their opponent resigned
        LOSE,       // The user lost
        DRAW;       // The game ended in a draw
    }

    /**
     * Show a dialog to the user communicating the specified outcome in the game we are displaying.
     *
     * @param outcome - represents how the game we are displaying ended, and thus what event the
     *                user should be notified of
     */
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
        start(gameID);
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
        start(gameID);
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
     * We give an instance of this class to ServerHelper when we send a draw offer on behalf of the
     * user. It will receive the callback once the request completes. We use an inner class here for
     * readability and organization. We also use one because the DrawRequester and ForfeitRequester
     * (see ForfeitRequestListener below) interfaces share quite a few method signatures. So if
     * BoardActivity implemented both interfaces, to those methods we'd have to add a way to
     * differentiate between draw request callbacks and forfeit request callbacks. This is cleaner.
     */
    private class OfferDrawRequestListener implements DrawRequester {
        /**
         * Called if our draw request succeeds
         */
        @Override
        public void drawSuccess() {
            // Now that we've offered a draw, it's the opponent's turn
            game.setData(GameData.STATE, 0);
            game.setData(GameData.DRAW_OFFERED, 1);
            manager.resume();

            setUI();
            activeRequest = false;
        }

        /**
         * Called if our draw request fails because the connection to the server has been lost
         */
        @Override
        public void connectionLost() {
            activeRequest = false;
            setUI();
            manager.resume();
            new Reconnector(BoardActivity.this, BoardActivity.this).reconnect();
        }

        /*
         * Various methods that get called if our draw offer request fails. We lump these together
         * because we only submit resignation requests if we know they're valid, according to our
         * data when we send them. So if the request fails, we just tell the user that it failed,
         * and don't do anything further.
         */
        @Override
        public void gameDoesNotExist() {
            showErrorDialog();
            activeRequest = false;
            setUI();
            manager.resume();
        }

        @Override
        public void userNotInGame() {
            showErrorDialog();
            activeRequest = false;
            setUI();
            manager.resume();
        }

        @Override
        public void noOpponent() {
            showErrorDialog();
            activeRequest = false;
            setUI();
            manager.resume();
        }

        @Override
        public void gameIsOver() {
            showErrorDialog();
            activeRequest = false;
            setUI();
            manager.resume();
        }

        @Override
        public void notUserTurn() {
            showErrorDialog();
            activeRequest = false;
            setUI();
            manager.resume();
        }

        @Override
        public void serverError() {
            showErrorDialog();
            activeRequest = false;
            setUI();
            manager.resume();
        }

        @Override
        public void systemError() {
            showErrorDialog();
            activeRequest = false;
            setUI();
            manager.resume();
        }

        private void showErrorDialog() {
            Display.showSimpleDialog(R.string.draw_offer_failed, BoardActivity.this);
        }
    }

    /**
     * We use this class to receive callbacks whenever we send a request to either accept or reject
     * a draw offer.
     */
    private class AcceptRejectListener implements DrawRequester, RejectRequester {
        /**
         * Whether the request that this object is listening to is a request to ACCEPT a draw offer,
         * or REJECT one.
         */
        private boolean isAccept;

        /**
         * Create a new AcceptRejectListener. It will be able to handle callbacks related to either a
         * draw offer accept request, or a draw offer reject request.
         *
         * @param isAccept - whether the request this object is managing is to accept a draw offer
         *                 or reject one
         */
        private AcceptRejectListener(boolean isAccept) {
            this.isAccept = isAccept;
        }
        
        @Override
        public void drawSuccess() {
            // Update the model
            game.setData(GameData.DRAWN, 1);
            game.setData(GameData.DRAW_OFFERED, 0);

            showUserDrawDialog();
            setUI();
        }

        @Override
        public void rejectSuccess() {
            // Update the model
            game.setData(GameData.STATE, 0);
            game.setData(GameData.DRAW_OFFERED, 0);

            setUI();
        }

        /**
         * Called when we want to notify the user that a draw accept request failed
         */
        private void showAcceptanceError() {
            Display.makeToast(BoardActivity.this, R.string.draw_acceptance_failed, Toast.LENGTH_LONG);
        }

        /**
         * Called when we want to notify the user that a draw rejection failed
         */
        private void showRejectionError() {
            Display.makeToast(BoardActivity.this, R.string.draw_rejection_failed, Toast.LENGTH_LONG);
        }

        /**
         * Called if a accept or reject request fails because of a loss of connection with the
         * server
         */
        @Override
        public void connectionLost() {
            new Reconnector(BoardActivity.this, BoardActivity.this).reconnect();
        }

        /*
         * The below methods are called if the server thinks there is some sort of problem with a
         * request that we sent. We only send requests if we think they're valid, and should be
         * legal according to the data that the server has sent us. So we treat all of these the
         * same way: we notify the user that there was a problem and allow them to try again if they
         * want.
         */

        @Override
        public void gameDoesNotExist() {
            if (isAccept) {
                showAcceptanceError();
            }
            else {
                showRejectionError();
            }

            setUI();
        }

        @Override
        public void userNotInGame() {
            if (isAccept) {
                showAcceptanceError();
            }
            else {
                showRejectionError();
            }

            setUI();
        }

        @Override
        public void noOpponent() {
            if (isAccept) {
                showAcceptanceError();
            }
            else {
                showRejectionError();
            }

            setUI();
        }

        @Override
        public void gameIsOver() {
            if (isAccept) {
                showAcceptanceError();
            }
            else {
                showRejectionError();
            }

            setUI();
        }

        @Override
        public void notUserTurn() {
            if (isAccept) {
                showAcceptanceError();
            }
            else {
                showRejectionError();
            }

            setUI();
        }

        @Override
        public void noDrawOffer() {
            if (isAccept) {
                showAcceptanceError();
            }
            else {
                showRejectionError();
            }

            setUI();
        }

        @Override
        public void serverError() {
            if (isAccept) {
                showAcceptanceError();
            }
            else {
                showRejectionError();
            }

            setUI();
        }

        @Override
        public void systemError() {
            if (isAccept) {
                showAcceptanceError();
            }
            else {
                showRejectionError();
            }

            setUI();
        }
    }

    /**
     * We give an instance of this class to ServerHelper when we send a resignation on behalf of the
     * user. It will receive the callback once the request completes. We use an inner class here for
     * readability and organization. We also use one because the DrawRequester and ForfeitRequester
     * (see OfferDrawRequestListener above) interfaces share quite a few method signatures. So if
     * BoardActivity implemented both interfaces, to those methods we'd have to add a way to
     * differentiate between draw request callbacks and forfeit request callbacks. This is cleaner.
     */
    private class ForfeitRequestListener implements ForfeitRequester {
        /**
         * Called if our forfeit request succeeds
         */
        @Override
        public void forfeitSuccess() {
            game.setData(GameData.USER_LOST, 1);
            game.setData(GameData.FORFEIT, 1);

            manager.resume();
            setUI();
            activeRequest = false;
        }

        /**
         * Called if our forfeit request fails because of a loss of connection to the server
         */
        @Override
        public void connectionLost() {
            new Reconnector(BoardActivity.this, BoardActivity.this).reconnect();
            activeRequest = false;
            setUI();
            manager.resume();
        }

        /*
         * Various methods that get called if our resignation request fails. We lump these together
         * because we only submit resignation requests if we know they're valid, according to our
         * data when we send them. So if the request fails, we just tell the user that it failed,
         * and don't do anything further.
         */
        @Override
        public void gameDoesNotExist() {
            showErrorDialog();
            activeRequest = false;
            setUI();
            manager.resume();
        }

        @Override
        public void userNotInGame() {
            showErrorDialog();
            activeRequest = false;
            setUI();
            manager.resume();
        }

        @Override
        public void noOpponent() {
            showErrorDialog();
            activeRequest = false;
            setUI();
            manager.resume();
        }

        @Override
        public void gameIsOver() {
            showErrorDialog();
            activeRequest = false;
            setUI();
            manager.resume();
        }

        @Override
        public void notUserTurn() {
            showErrorDialog();
            activeRequest = false;
            setUI();
            manager.resume();
        }

        @Override
        public void serverError() {
            showErrorDialog();
            activeRequest = false;
            setUI();
            manager.resume();
        }

        @Override
        public void systemError() {
            showErrorDialog();
            activeRequest = false;
            setUI();
            manager.resume();
        }

        private void showErrorDialog() {
            Display.showSimpleDialog(R.string.resignation_failed, BoardActivity.this);
        }
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