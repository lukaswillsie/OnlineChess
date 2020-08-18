package com.lukaswillsie.onlinechess.activities.game_display;

import android.os.Bundle;
import android.util.Log;

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
 * Displays a list of the user's archived games on the screen
 */
public class ArchivedGamesActivity extends AppCompatActivity implements ReconnectListener, LoadGamesRequester {
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
        if (Server.getServerHelper() == null) {
            new Reconnector(this, this).reconnect();
        } else {
            // Set up our RecyclerView to display a list of the user's archived games
            RecyclerView recyclerView = findViewById(R.id.games_recycler);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new ArchivedUserGamesAdapter(this, getGames(), this));

            // Set up our SwipeRefreshLayout to submit a load games request to the server when the
            // RecyclerView is refreshed by the user
            final SwipeRefreshLayout refreshLayout = findViewById(R.id.games_refresh);
            refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    try {
                        Server.getServerHelper().loadGames(Server.getUsername(), ArchivedGamesActivity.this);
                    } catch (MultipleRequestException e) {
                        // This shouldn't happen (we only submit load games requests to ServerHelper
                        // one at a time, and never submit another before receiving a callback about
                        // the first). If it does, we display an error dialog and end the refresh
                        // animation
                        Log.e(tag, "Submitted multiple load games requests to ServerHelper");
                        Display.showSimpleDialog(R.string.games_refresh_failed, ArchivedGamesActivity.this);
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
        // Set up our RecyclerView to display a list of the user's archived games
        RecyclerView recyclerView = findViewById(R.id.games_recycler);
        ((UserGamesAdapter) recyclerView.getAdapter()).setGames(getGames());
    }

    /**
     * Process the list of the user's games stored in Server to extract only those we
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
        List<UserGame> games = Server.getGames();
        for (UserGame game : games) {
            if ((int) game.getData(GameData.ARCHIVED) == 1) {
                if (game.isOver()) {
                    Log.i(tag, "Game " + game.getData(GameData.GAMEID) + " is over");
                    archivedGames.add(gameOverPos, game);
                    gameOverPos++;
                } else if (game.isOpponentTurn()) {
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
        ((UserGamesAdapter) recyclerView.getAdapter()).setGames(getGames());
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
        SwipeRefreshLayout refreshLayout = findViewById(R.id.games_refresh);

        refreshLayout.setRefreshing(false);
        ((UserGamesAdapter) recyclerView.getAdapter()).setGames(getGames());
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
        Display.showSimpleDialog(R.string.games_refresh_failed, this);

        SwipeRefreshLayout refreshLayout = findViewById(R.id.games_refresh);
        refreshLayout.setRefreshing(false);
    }

    /**
     * Called by ServerHelper if a load games request, submitted as part of a refresh, fails due to
     * a system error.
     */
    @Override
    public void systemError() {
        Display.showSimpleDialog(R.string.games_refresh_failed, this);

        SwipeRefreshLayout refreshLayout = findViewById(R.id.games_refresh);
        refreshLayout.setRefreshing(false);
    }
}