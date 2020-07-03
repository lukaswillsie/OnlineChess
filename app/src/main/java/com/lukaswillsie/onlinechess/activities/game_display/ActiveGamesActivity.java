package com.lukaswillsie.onlinechess.activities.game_display;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.InteriorActivity;
import com.lukaswillsie.onlinechess.activities.Reconnector;
import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.GameData;
import com.lukaswillsie.onlinechess.network.helper.requesters.ArchiveRequester;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays for the user a list of all their "active" games. That is, all games that the user
 * hasn't yet marked as archived.
 */
public class ActiveGamesActivity extends InteriorActivity {
    /*
     * Tag used for logging to the console
     */
    private static final String tag = "ActiveGamesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_games);

        if(((ChessApplication)getApplicationContext()).getServerHelper() == null) {
            new Reconnector(this).reconnect();
        }
        else {
            // Set up our RecyclerView to display a list of the user's archived games
            RecyclerView recyclerView = findViewById(R.id.games_recycler);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new GamesAdapter(getGames(), true, this));
        }
    }

    /**
     * Called by Reconnector once a reconnection process is over, notifying us that we can proceed
     * with normal execution, and that we can count on a working connection with the server. So we
     * proceed with populating the onscreen RecyclerView with game cards.
     */
    @Override
    public void reconnectionComplete() {
        RecyclerView recyclerView = findViewById(R.id.games_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new GamesAdapter(getGames(), true, this));
    }

    /**
     * Checks if the given Game is over
     * @param game - the Game to analyze
     * @return true if the given Game is over (somebody has won or a draw has been agreed to),
     * false otherwise
     */
    private boolean isOver(Game game) {
        return      (int)game.getData(GameData.USER_WON) == 1
                ||  (int)game.getData(GameData.USER_LOST) == 1
                ||  (int)game.getData(GameData.DRAWN) == 1;
    }

    /**
     * Checks if it is the user's opponent's turn in the given Game
     * @param game - the Game to analyze
     * @return true of it's the user's opponent's turn in the given Game, false otherwise
     */
    private boolean isOpponentTurn(Game game) {
        return (int)game.getData(GameData.STATE) == 0;
    }

    /**
     * Returns a list of all of the user's active games, sorted in the following order:
     * 1. Ongoing games in which it is the user's turn
     * 2. Ongoing games in which it is the opponent's turn
     * 3. Games that have ended, either through victory or draw
     *
     * @return a sorted list of all the user's active games, for displaying on the screen.
     */
    private List<Game> getGames() {
        int userTurnPos = 0;
        int opponentTurnPos = 0;
        int gameOverPos = 0;
        List<Game> activeGames = new ArrayList<>();
        List<Game> games = ((ChessApplication)getApplicationContext()).getGames();
        for(Game game : games) {
            if(!((int)game.getData(GameData.ARCHIVED) == 1)) {
                if(isOver(game)) {
                    Log.i(tag, "Game " + game.getData(GameData.GAMEID) + " is over");
                    activeGames.add(gameOverPos, game);
                    gameOverPos++;
                }
                else if(isOpponentTurn(game)) {
                    Log.i(tag, game.getData(GameData.GAMEID) + " is opponent turn");
                    activeGames.add(opponentTurnPos, game);
                    opponentTurnPos++;
                    gameOverPos++;
                }
                else {
                    Log.i(tag, game.getData(GameData.GAMEID) + " is user turn");
                    activeGames.add(userTurnPos, game);
                    userTurnPos++;
                    opponentTurnPos++;
                    gameOverPos++;
                }
            }
        }

        return activeGames;
    }
}