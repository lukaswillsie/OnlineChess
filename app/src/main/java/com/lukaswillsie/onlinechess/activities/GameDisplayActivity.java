package com.lukaswillsie.onlinechess.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;

import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.GameData;

import java.util.List;

/**
 * Our app distinguishes between two types of games: active games and archived games. Active games
 * are simply all games that haven't yet been archived by the user. We want to be able to display
 * to the user a list of their active games and a list of their archived games, but in different
 * activities, and we want the way we display and format game information to be consistent across
 * both.
 *
 * So this class centralizes logic for taking a list of Game objects and displaying a nice, clean,
 * card layout on the screen for each.
 *
 * Subclass Activities should contain a vertically-oriented LinearLayout to which Game cards can be
 * added. This class has an abstract method getLayoutId() that subclasses should use to identify
 * which view in their layout is the one that should have Game cards deposited in it.
 *
 * There's a commented-out section of activity_active_games.xml that has a few examples in proper
 * XML of what this class accomplishes programmatically at runtime.
 */
public abstract class GameDisplayActivity extends InteriorActivity {
    /*
     * Used for converting dp to px.
     */
    private float logicalDensity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        logicalDensity = metrics.density;
    }

    /**
     * Takes the given list of Games and displays each on the screen as a nicely formatted card.
     * @param games - a list of Games to display
     */
    protected void processGames(List<Game> games) {
        LinearLayout layout = findViewById(getLayoutId());
        for(Game game : games) {
            ConstraintLayout gameLayout = packageGame(game);
            layout.addView(gameLayout);
        }
    }

    /**
     * Takes a Game object and builds a ConstraintLayout for displaying the data it contains to
     * the user. It constructs a card (it will look much like a CardView) and then arranges the ID
     * of the game, the user's opponent in the game, the turn number of the game, and the state (who
     * won, or whose turn it is, etc.) within the card, styling accordingly.
     *
     * This ConstraintLayout should be placed into a LinearLayout upon return.
     *
     * @param game - the game to package into a ConstraintLayout
     * @return the given game, package into a ConstraintLayout that is ready for displaying on the
     * screen
     */
    private ConstraintLayout packageGame(Game game) {
        // Create an empty ConstraintLayout to hold all the game's data
        ConstraintLayout layout = new ConstraintLayout(this);
        int layoutID = ViewCompat.generateViewId();
        layout.setId(layoutID);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(90));
        layout.setLayoutParams(cardParams);

        // Create TextView to hold gameID
        String gameID = (String)game.getData(GameData.GAMEID);
        TextView gameIDView = new TextView(this);
        int gameIDViewID = ViewCompat.generateViewId();
        gameIDView.setId(gameIDViewID);
        styleGameID(gameIDView);
        gameIDView.setText(gameID.toUpperCase());
        layout.addView(gameIDView);

        // Create TextView to hold opponent's name (or "No opponent" if the player is currently the
        // only player in the game)
        String opponent = (String)game.getData(GameData.OPPONENT);
        TextView opponentView = new TextView(this);
        int opponentViewID = ViewCompat.generateViewId();
        opponentView.setId(opponentViewID);
        if(opponent.equals("")) {
            // The TextView will now hold: "No opponent"
            opponentView.setText(R.string.no_opponent);
        }
        else {
            // The TextView will now hold: "Opponent: <opponent>"
            opponentView.setText(getString(R.string.opponent_label, opponent));
        }
        styleOpponentLabel(opponentView);
        layout.addView(opponentView);

        // Create a TextView to hold the "state" of the game.
        TextView stateView = new TextView(this);
        int stateViewID = ViewCompat.generateViewId();
        stateView.setId(stateViewID);
        styleStatus(stateView);

        // Fetch a bunch of data about the game
        int userWon = (Integer)game.getData(GameData.USER_WON);
        int userLost = (Integer)game.getData(GameData.USER_LOST);
        int state = (Integer)game.getData(GameData.STATE);
        int turn = (Integer)game.getData(GameData.TURN);
        int drawn = (Integer)game.getData(GameData.DRAWN);
        int drawOffered = (Integer)game.getData(GameData.DRAW_OFFERED);

        // The text in the stateView TextView holds the "state" of the game. It can contain a
        // variety of different messages. If the game has already been won, it tells the user
        // whether they won or lost. Otherwise, if the game has been drawn, it tells the user this.
        // Otherwise, if the game is ongoing, it tells the user whose turn it is. If it's the user's
        // turn and they have to respond to a draw offer from their opponent, it notifies them of
        // this

        // The user won
        if(userWon == 1) {
            stateView.setText(R.string.user_win);
            // Set the colour of the text, and since the game is over we make the whole card slightly transparent
            stateView.setTextColor(getResources().getColor(R.color.user_win));
            layout.setBackground(getResources().getDrawable(R.drawable.game_over_background));
        }
        // The user lost
        else if (userLost == 1) {
            stateView.setText(R.string.user_lose);
            // Set the colour of the text, and since the game is over we make the whole card slightly transparent
            stateView.setTextColor(getResources().getColor(R.color.user_lose));
            layout.setBackground(getResources().getDrawable(R.drawable.game_over_background));
        }
        // The game is over by draw
        else if (drawn == 1) {
            stateView.setText(R.string.game_drawn);
            // Set the colour of the text, and since the game is over we make the whole card slightly transparent
            stateView.setTextColor(getResources().getColor(R.color.drawn));
            layout.setBackground(getResources().getDrawable(R.drawable.game_over_background));
        }
        // It's the opponent's turn
        else if (state == 0) {
            stateView.setText(R.string.opponent_turn);
            // Set the colour of the text
            stateView.setTextColor(getResources().getColor(R.color.opponent_turn));
            stateView.setAlpha(0.75f);
            layout.setBackground(getResources().getDrawable(R.drawable.opponent_turn_background));
        }
        // It's the user's turn and they've been offered a draw
        else if (drawOffered == 1){
            stateView.setText(R.string.draw_offered_to_user);
            // Set the colour of the text
            stateView.setTextColor(getResources().getColor(R.color.user_turn));
            layout.setBackground(getResources().getDrawable(R.drawable.user_turn_background));
        }
        // It's the user's turn
        else {
            stateView.setText(R.string.user_turn);
            // Set the colour of the text
            stateView.setTextColor(getResources().getColor(R.color.user_turn));
            layout.setBackground(getResources().getDrawable(R.drawable.user_turn_background));
        }
        layout.addView(stateView);

        // A TextView that displays the current turn number
        TextView turnView = new TextView(this);
        int turnViewId = ViewCompat.generateViewId();
        turnView.setId(turnViewId);
        turnView.setText(getString(R.string.turn_number_label, turn));
        layout.addView(turnView);
        styleTurn(turnView);


        // Now that all our TextViews have been styled and given text, we need to properly
        // constrain them
        ConstraintSet set = new ConstraintSet();
        set.clone(layout);

        // Create two guidelines that we will use to properly align our views
        int rightGuidelineID = ViewCompat.generateViewId();
        set.create(rightGuidelineID, ConstraintSet.VERTICAL);
        set.setGuidelinePercent(rightGuidelineID, 0.95f);

        int leftGuidelineID = ViewCompat.generateViewId();
        set.create(leftGuidelineID, ConstraintSet.VERTICAL);
        set.setGuidelinePercent(leftGuidelineID, 0.05f);

        // Constrain the gameID label
        set.constrainWidth(gameIDViewID, ConstraintSet.MATCH_CONSTRAINT);
        set.constrainHeight(gameIDViewID, ConstraintSet.WRAP_CONTENT);
        set.connect(gameIDViewID, ConstraintSet.START, leftGuidelineID, ConstraintSet.START);
        set.connect(gameIDViewID, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        set.connect(gameIDViewID, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        set.constrainPercentWidth(gameIDViewID, 0.5f);
        set.setVerticalBias(gameIDViewID, 0.4f);

        // Constrain the opponent label
        set.connect(opponentViewID, ConstraintSet.TOP, gameIDViewID, ConstraintSet.BOTTOM);
        set.connect(opponentViewID, ConstraintSet.START, gameIDViewID, ConstraintSet.START);

        // Constrain the state label
        set.connect(stateViewID, ConstraintSet.TOP, gameIDViewID, ConstraintSet.TOP);
        set.connect(stateViewID, ConstraintSet.BOTTOM, gameIDViewID, ConstraintSet.BOTTOM);
        set.connect(stateViewID, ConstraintSet.END, rightGuidelineID, ConstraintSet.END);

        // Constrain the turn counter
        set.connect(turnViewId, ConstraintSet.TOP, stateViewID, ConstraintSet.BOTTOM);
        set.connect(turnViewId, ConstraintSet.END, stateViewID, ConstraintSet.END);

        // Apply our constraints to the layout
        set.applyTo(layout);

        return layout;
    }

    /**
     * Styles the given TextView as a TextView holding a game's ID
     * @param gameIDView - the TextView to style
     */
    private void styleGameID(TextView gameIDView) {
        gameIDView.setTextColor(getResources().getColor(android.R.color.black));
        gameIDView.setTypeface(Typeface.DEFAULT_BOLD);
        gameIDView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
    }

    /**
     * Styles the given TextView as a TextView holding the opponent's name.
     * @param opponentView - the TextView to be styled
     */
    private void styleOpponentLabel(TextView opponentView) {
        opponentView.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
        opponentView.setTextColor(getResources().getColor(R.color.opponentLabel));
    }

    /**
     * Applies base style to the given TextView as a TextView holding the status of a game. Does not set the color,
     * since this changes based on what the status of the game is.
     *
     * @param statusView - the TextView to be styled
     */
    private void styleStatus(TextView statusView) {
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        statusView.setTypeface(Typeface.DEFAULT_BOLD);
    }

    /**
     * Style the given TextView as a TextView holding the turn number of a game
     */
    private void styleTurn(TextView turnView) {
        turnView.setTextColor(getResources().getColor(R.color.turnCounter));
    }

    /**
     * Converts density-independent pixels into pixels
     * @param dp - the quantity of dp to convert
     * @return the given quantity of dp converted into pixels
     */
    private int dpToPx(int dp) {
        // This code comes from mportuesisf's answer to the following StackOverflow question:
        // https://stackoverflow.com/questions/6656540/android-convert-px-to-dp-video-aspect-ratio
        return (int) (Math.ceil(dp * logicalDensity) + 0.5);
    }

    /**
     * Subclasses should use this method to identify which LinearLayout on the screen this Activity
     * should put packaged Game objects into.
     *
     * @return the id of the LinearLayout that this Activity should be putting packaged Game objects
     * into
     */
    public abstract @IdRes int getLayoutId();

    /**
     * Return the tag that this object uses for logging to the console. Ensures that when code in
     * its superclass (InteriorActivity) logs to the console, this subclass' tag is actually used,
     * to ensure accurate tagging.
     */
    @Override
    public String getTag() {
        return "GameDisplayActivity";
    }
}
