package com.lukaswillsie.onlinechess.activities.game_display;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.Display;
import com.lukaswillsie.onlinechess.activities.ReconnectListener;
import com.lukaswillsie.onlinechess.activities.Reconnector;
import com.lukaswillsie.onlinechess.data.GameData;
import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.Server;
import com.lukaswillsie.onlinechess.network.helper.MultipleRequestException;
import com.lukaswillsie.onlinechess.network.helper.requesters.LoadGamesRequester;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays for the user a list of all their "active" games. That is, all games that the user
 * hasn't yet marked as archived.
 */
public class ActiveGamesActivity extends AppCompatActivity implements ReconnectListener, LoadGamesRequester {
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

            // Set up our SwipeRefreshLayout so that it submits a load games request on refresh
            final SwipeRefreshLayout refreshLayout = findViewById(R.id.games_refresh);
            refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    try {
                        Server.getServerHelper().loadGames(Server.getUsername(), ActiveGamesActivity.this);
                    } catch (MultipleRequestException e) {
                        // This should never happen, but if it does we stop the refresh animation
                        // and show an error dialog. The user can try again if they want to, at
                        // which point, hopefully, the request will have been handled.
                        Log.e(tag, "Submitted multiple load games requests to ServerHelper");
                        Display.showSimpleDialog(R.string.games_refresh_failed, ActiveGamesActivity.this);
                        refreshLayout.setRefreshing(false);
                    }
                }
            });
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
        RecyclerView recyclerView = findViewById(R.id.games_recycler);
        ((UserGamesAdapter)recyclerView.getAdapter()).setGames(getGames());
    }

    /**
     * Called by Reconnector once a reconnection process is over, notifying us that we can proceed
     * with normal execution, and that we can count on a working connection with the server. So we
     * proceed with populating the onscreen RecyclerView with game cards.
     */
    @Override
    public void reconnectionComplete() {
        RecyclerView recyclerView = findViewById(R.id.games_recycler);
        ((UserGamesAdapter)recyclerView.getAdapter()).setGames(getGames());;
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

    /**
     * Called by ServerHelper if a load games request, submitted as part of a refresh, succeeds.
     *
     * @param games - the list of the user's games that was sent over by the server
     */
    @Override
    public void success(List<UserGame> games) {
        Server.setGames(games);
        RecyclerView recyclerView = findViewById(R.id.games_recycler);
        ((UserGamesAdapter)recyclerView.getAdapter()).setGames(getGames());

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.games_refresh);
        swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * Called by ServerHelper if a load games request, submitted as part of a refresh, fails due to
     * a loss of connection with the server.
     */
    @Override
    public void connectionLost() {
        SwipeRefreshLayout refreshLayout = findViewById(R.id.games_refresh);
        refreshLayout.setRefreshing(false);
        new Reconnector(this, this).reconnect();
    }

    /**
     * Called by ServerHelper if a load games request, submitted as part of a refresh, fails due to
     * an error server-side.
     */
    @Override
    public void serverError() {
        SwipeRefreshLayout refreshLayout = findViewById(R.id.games_refresh);
        refreshLayout.setRefreshing(false);

        Display.showSimpleDialog(R.string.games_refresh_failed, this);
    }

    /**
     * Called by ServerHelper if a load games request, submitted as part of a refresh, fails due to
     * a system error.
     */
    @Override
    public void systemError() {
        SwipeRefreshLayout refreshLayout = findViewById(R.id.games_refresh);
        refreshLayout.setRefreshing(false);

        Display.showSimpleDialog(R.string.games_refresh_failed, this);
    }
}