package com.lukaswillsie.onlinechess.activities.game_display;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.InteriorActivity;
import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.GameData;
import com.lukaswillsie.onlinechess.network.helper.requesters.RestoreRequester;

import java.util.ArrayList;
import java.util.List;

public class ArchivedGamesActivity extends InteriorActivity implements RestoreRequester {
    /*
     * Used for logging things to the console
     */
    private static final String tag = "ArchivedGamesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archived_games);

        // Set up our RecyclerView to display a list of the user's archived games
        RecyclerView recyclerView = findViewById(R.id.games_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new GamesAdapter(getGames(), false, this));
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
     * Process the list of the user's games stored in ChessApplication to extract only those we
     * want displayed; the archived ones. Also sorts them in the following order:
     * 1. Games in which it is the user's turn
     * 2. Games in which it is the opponent's turn
     * 3. Games that are over, either by draw or victory
     *
     * @return A sorted list of the user's archived games, for display on the screen.
     */
    private List<Game> getGames() {
        int userTurnPos = 0;
        int opponentTurnPos = 0;
        int gameOverPos = 0;
        List<Game> archivedGames = new ArrayList<>();
        List<Game> games = ((ChessApplication)getApplicationContext()).getGames();
        for(Game game : games) {
            if((int)game.getData(GameData.ARCHIVED) == 1) {
                if(isOver(game)) {
                    Log.i(tag, "Game " + game.getData(GameData.GAMEID) + " is over");
                    archivedGames.add(gameOverPos, game);
                    gameOverPos++;
                }
                else if(isOpponentTurn(game)) {
                    Log.i(tag, game.getData(GameData.GAMEID) + " is opponent turn");
                    archivedGames.add(opponentTurnPos, game);
                    opponentTurnPos++;
                    gameOverPos++;
                }
                else {
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

    @Override
    public void restoreSuccessful() {
        Toast.makeText(this, "RESTORATION SUCCESSFUL", Toast.LENGTH_LONG);
    }

    @Override
    public void loginComplete() {

    }

    @Override
    public void connectionLost() {

    }

    @Override
    public void serverError() {

    }

    @Override
    public void systemError() {

    }
}