package com.lukaswillsie.onlinechess.activities.game_display;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.ReconnectListener;
import com.lukaswillsie.onlinechess.activities.Reconnector;
import com.lukaswillsie.onlinechess.data.GameData;
import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.Server;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays for the user a list of all their "active" games. That is, all games that the user
 * hasn't yet marked as archived.
 */
public class ActiveGamesActivity extends AppCompatActivity implements ReconnectListener {
    /*
     * Tag used for logging to the console
     */
    private static final String tag = "ActiveGamesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_active_games);

        if (Server.getServerHelper() == null) {
            new Reconnector(this, this).reconnect();
        } else {
            // Set up our RecyclerView to display a list of the user's archived games
            RecyclerView recyclerView = findViewById(R.id.games_recycler);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new ActiveUserGamesAdapter(this, getGames(), this));
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
     * Called by Reconnector once a reconnection process is over, notifying us that we can proceed
     * with normal execution, and that we can count on a working connection with the server. So we
     * proceed with populating the onscreen RecyclerView with game cards.
     */
    @Override
    public void reconnectionComplete() {
        RecyclerView recyclerView = findViewById(R.id.games_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ActiveUserGamesAdapter(this, getGames(), this));
    }

    /**
     * Returns a list of all of the user's active games, sorted in the following order:
     * 1. Ongoing games in which it is the user's turn
     * 2. Ongoing games in which it is the opponent's turn
     * 3. Games that have ended, either through victory or draw
     *
     * @return a sorted list of all the user's active games, for displaying on the screen.
     */
    private List<UserGame> getGames() {
        int userTurnPos = 0;
        int opponentTurnPos = 0;
        int gameOverPos = 0;
        List<UserGame> activeGames = new ArrayList<>();
        List<UserGame> games = Server.getGames();
        for (UserGame game : games) {
            if (!((int) game.getData(GameData.ARCHIVED) == 1)) {
                if (game.isOver()) {
                    activeGames.add(gameOverPos, game);
                    gameOverPos++;
                } else if (game.isOpponentTurn()) {
                    activeGames.add(opponentTurnPos, game);
                    opponentTurnPos++;
                    gameOverPos++;
                } else {
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