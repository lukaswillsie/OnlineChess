package com.lukaswillsie.onlinechess;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.lukaswillsie.onlinechess.activities.GameDisplayActivity;
import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.GameData;

import java.util.ArrayList;
import java.util.List;

public class ArchivedGamesActivity extends GameDisplayActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archived_games);

        int userTurnPos = 0;
        int opponentTurnPos = 0;
        int gameOverPos = 0;
        List<Game> archivedGames = new ArrayList<>();
        List<Game> games = ((ChessApplication)getApplicationContext()).getGames();
        for(Game game : games) {
            if((int)game.getData(GameData.ARCHIVED) == 1) {
                if(isOver(game)) {
                    Log.i(getTag(), "Game " + game.getData(GameData.GAMEID) + " is over");
                    archivedGames.add(gameOverPos, game);
                    gameOverPos++;
                }
                else if(isOpponentTurn(game)) {
                    Log.i(getTag(), game.getData(GameData.GAMEID) + " is opponent turn");
                    archivedGames.add(opponentTurnPos, game);
                    opponentTurnPos++;
                    gameOverPos++;
                }
                else {
                    Log.i(getTag(), game.getData(GameData.GAMEID) + " is user turn");
                    archivedGames.add(userTurnPos, game);
                    userTurnPos++;
                    opponentTurnPos++;
                    gameOverPos++;
                }
            }
        }

        super.processGames(archivedGames);
    }

    @Override
    public String getTag() {
        return "ArchivedGamesActivity";
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
     * Identifies for this class' superclass, GameDisplayActivity, which LinearLayout on the screen
     * it should put packaged Game objects into.
     *
     * @return the id of the LinearLayout that this class' superclass should be putting packaged
     * Game objects into
     */
    @Override
    protected int getLayoutId() {
        return R.id.games_layout;
    }
}