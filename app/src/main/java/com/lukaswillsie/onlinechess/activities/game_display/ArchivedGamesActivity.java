package com.lukaswillsie.onlinechess.activities.game_display;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.ReconnectListener;
import com.lukaswillsie.onlinechess.activities.Reconnector;
import com.lukaswillsie.onlinechess.data.GameData;
import com.lukaswillsie.onlinechess.data.UserGame;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of the user's archived games on the screen
 */
public class ArchivedGamesActivity extends AppCompatActivity implements ReconnectListener {
    /*
     * Used for logging things to the console
     */
    private static final String tag = "ArchivedGamesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archived_games);

        // If our app was terminated by the operating system and is now being resumed, we'll have to
        // re-establish a connection with the server
        if (((ChessApplication) getApplicationContext()).getServerHelper() == null) {
            new Reconnector(this, this).reconnect();
        } else {
            // Set up our RecyclerView to display a list of the user's archived games
            RecyclerView recyclerView = findViewById(R.id.games_recycler);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new ArchivedUserGamesAdapter(this, getGames(), this));
        }
    }

    /**
     * We ensure that if any of the user's games have changed in any way since this activity was
     * paused, this activity displays the update. For example, suppose the user clicks on one of
     * their games, goes into BoardActivity, and makes a move. Without this method, when they get
     * back to this activity the UI state will be unchanged. So we'll still be telling them that
     * it's their turn in the game they just made a move in, even though it's not. Thus, we need to
     * refresh the UI to reflect the reality of the model.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Set up our RecyclerView to display a list of the user's archived games
        RecyclerView recyclerView = findViewById(R.id.games_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ActiveUserGamesAdapter(this, getGames(), this));
    }

    /**
     * Checks if the given Game is over
     *
     * @param game - the Game to analyze
     * @return true if the given Game is over (somebody has won or a draw has been agreed to),
     * false otherwise
     */
    private boolean isOver(UserGame game) {
        return (int) game.getData(GameData.USER_WON) == 1
                || (int) game.getData(GameData.USER_LOST) == 1
                || (int) game.getData(GameData.DRAWN) == 1;
    }

    /**
     * Checks if it is the user's opponent's turn in the given Game
     *
     * @param game - the Game to analyze
     * @return true of it's the user's opponent's turn in the given Game, false otherwise
     */
    private boolean isOpponentTurn(UserGame game) {
        return (int) game.getData(GameData.STATE) == 0;
    }

    /**
     * Process the list of the user's games stored in ChessApplication to extract only those we
     * want displayed; the archived ones. Also sorts them in the following order:
     * 1. Games in which it is the user's turn
     * 2. Games in which it is the opponent's turn
     * 3. Games that are over, either by draw or victory
     *
     * @return A sorted list of the user's archived games, for display on the screen.
     */
    private List<UserGame> getGames() {
        int userTurnPos = 0;
        int opponentTurnPos = 0;
        int gameOverPos = 0;
        List<UserGame> archivedGames = new ArrayList<>();
        List<UserGame> games = ((ChessApplication) getApplicationContext()).getGames();
        for (UserGame game : games) {
            if ((int) game.getData(GameData.ARCHIVED) == 1) {
                if (isOver(game)) {
                    Log.i(tag, "Game " + game.getData(GameData.GAMEID) + " is over");
                    archivedGames.add(gameOverPos, game);
                    gameOverPos++;
                } else if (isOpponentTurn(game)) {
                    Log.i(tag, game.getData(GameData.GAMEID) + " is opponent turn");
                    archivedGames.add(opponentTurnPos, game);
                    opponentTurnPos++;
                    gameOverPos++;
                } else {
                    Log.i(tag, game.getData(GameData.GAMEID) + " is user turn");
                    archivedGames.add(userTurnPos, game);
                    userTurnPos++;
                    opponentTurnPos++;
                    gameOverPos++;
                }
            }
        }

        return archivedGames;
    }

    /**
     * Reconnector will call this method once a reconnection attempt has completely finished. That
     * is, once a connection to the server has been established and the user has been successfully
     * re-logged in. This callback notifies us that we can proceed with normal execution, and assume
     * a working connection to the server.
     */
    @Override
    public void reconnectionComplete() {
        // Set up our RecyclerView to display a list of the user's archived games
        RecyclerView recyclerView = findViewById(R.id.games_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ArchivedUserGamesAdapter(this, getGames(), this));
    }
}