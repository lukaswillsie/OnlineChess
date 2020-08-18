package com.lukaswillsie.onlinechess.activities.game_display;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.Display;
import com.lukaswillsie.onlinechess.activities.ErrorDialogActivity;
import com.lukaswillsie.onlinechess.activities.JoinGameActivity;
import com.lukaswillsie.onlinechess.activities.ReconnectListener;
import com.lukaswillsie.onlinechess.activities.Reconnector;
import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.ServerData;
import com.lukaswillsie.onlinechess.network.Server;
import com.lukaswillsie.onlinechess.network.helper.MultipleRequestException;
import com.lukaswillsie.onlinechess.network.helper.requesters.OpenGamesRequester;

import java.util.ArrayList;
import java.util.List;

/**
 * This activity simply displays a list of all currently open games (games that anyone can join) for
 * the user.
 */
public class OpenGamesActivity extends ErrorDialogActivity implements OpenGamesRequester, ReconnectListener {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "OpenGamesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_games);

        // Initialize our RecyclerView, giving it, for the time being, an empty OpenGamesAdapter.
        // This Adapter will be given a list of games to display once we have received them from the
        // server
        RecyclerView recycler = findViewById(R.id.games_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(new OpenGamesAdapter(this, new ArrayList<Game>(), this));

        // Set up the SwipeRefreshLayout containing our RecyclerView so that we submit an open games
        // request to the server on attempted refresh
        final SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.games_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                try {
                    Server.getServerHelper().getOpenGames(OpenGamesActivity.this);
                }
                // This shouldn't happen. We won't be submitting requests to ServerHelper until the
                // last request has terminated. However, if it does, we simply notify the user of the
                // problem and end the refresh animation.
                catch (MultipleRequestException e) {
                    Log.e(tag, "MultipleRequestException thrown in response to getOpenGames() request");
                    Display.showSimpleDialog(R.string.open_games_refresh_failed, OpenGamesActivity.this);
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });

        // If the OS shut our app down while we were in the background and is now restarting us,
        // we'll need to reconnect to the server. We detect that this has happened if our static
        // field serverHelper in Server has been nullified. If it hasn't we proceed as
        // usual and grab a list of open games from the server. If it IS null, we try to reconnect.
        if (Server.getServerHelper() != null) {
            try {
                Server.getServerHelper().getOpenGames(this);
            }
            // This shouldn't happen. We won't be submitting requests to ServerHelper until the last
            // request has terminated. However, if it does, we display a dialog to the user and
            // present the option to try again.
            catch (MultipleRequestException e) {
                Log.e(tag, "MultipleRequestException thrown in response to getOpenGames() request");
                showSystemErrorDialog();
            }
        } else {
            new Reconnector(this, this).reconnect();
        }
    }

    /**
     * Called upon successful completion of an open games request
     *
     * @param games - the list of all open games in the system sent over by the server
     */
    @Override
    public void openGames(List<Game> games) {
        // Remove all games that the current user is in from the list (we don't want to show them
        // games that they can't join)
        int i = 0;
        while (i < games.size()) {
            if (games.get(i).getData(ServerData.WHITE).equals(Server.getUsername())
                    || games.get(i).getData(ServerData.BLACK).equals(Server.getUsername())) {
                games.remove(i);
            } else {
                i++;
            }
        }

        RecyclerView recyclerView = findViewById(R.id.games_recycler);
        ((OpenGamesAdapter) recyclerView.getAdapter()).setGames(games);

        // If the open games request we submitted was a result of a refresh, we need to end the
        // refresh animation.
        SwipeRefreshLayout refreshLayout = findViewById(R.id.games_refresh);
        if (refreshLayout.isRefreshing()) {
            refreshLayout.setRefreshing(false);
        }
    }

    /**
     * Called if an open games request is stymied by the discovery that our connection to the server
     * has been lost
     */
    @Override
    public void connectionLost() {
        super.showConnectionLostDialog();

        // If the open games request we submitted was a result of a refresh, we need to end the
        // refresh animation.
        SwipeRefreshLayout refreshLayout = findViewById(R.id.games_refresh);
        if (refreshLayout.isRefreshing()) {
            refreshLayout.setRefreshing(false);
        }
    }

    /**
     * Called if an open games request fails due to a server error
     */
    @Override
    public void serverError() {
        super.showServerErrorDialog();

        // If the open games request we submitted was a result of a refresh, we need to end the
        // refresh animation.
        SwipeRefreshLayout refreshLayout = findViewById(R.id.games_refresh);
        if (refreshLayout.isRefreshing()) {
            refreshLayout.setRefreshing(false);
        }
    }

    /**
     * Called if an open games request fails due to a system error
     */
    @Override
    public void systemError() {
        super.showSystemErrorDialog();

        // If the open games request we submitted was a result of a refresh, we need to end the
        // refresh animation.
        SwipeRefreshLayout refreshLayout = findViewById(R.id.games_refresh);
        if (refreshLayout.isRefreshing()) {
            refreshLayout.setRefreshing(false);
        }
    }

    /**
     * Called after we show the user a system error dialog, due to a failure of a request, and the
     * user clicks "Try Again"
     */
    @Override
    public void retrySystemError() {
        // To retry, we attempt to load the list of openGames again, just like in onCreate. We still
        // have to check that our app hasn;t been terminated while in the background.
        if (Server.getServerHelper() != null) {
            try {
                Server.getServerHelper().getOpenGames(this);
            }
            // This shouldn't happen. We won't be submitting requests to ServerHelper until the last
            // request has terminated. However, if it does, we display a dialog to the user and
            // present the option to try again.
            catch (MultipleRequestException e) {
                Log.e(tag, "MultipleRequestException thrown in response to getOpenGames() request");
                showSystemErrorDialog();
            }
        } else {
            new Reconnector(this, this).reconnect();
        }
    }

    /**
     * Called after we show the user a system error dialog, due to a failure of a request, and the
     * user clicks "Cancel"
     */
    @Override
    public void cancelSystemError() {
        startActivity(new Intent(this, JoinGameActivity.class));
    }

    /**
     * Called after we show the user a server error dialog, due to a failure of a request, and the
     * user clicks "Try Again"
     */
    @Override
    public void retryServerError() {
        // To retry, we attempt to load the list of openGames again, just like in onCreate. We still
        // have to check that our app hasn't been terminated while in the background.
        if (Server.getServerHelper() != null) {
            try {
                Server.getServerHelper().getOpenGames(this);
            }
            // This shouldn't happen. We won't be submitting requests to ServerHelper until the last
            // request has terminated. However, if it does, we display a dialog to the user and
            // present the option to try again.
            catch (MultipleRequestException e) {
                Log.e(tag, "MultipleRequestException thrown in response to getOpenGames() request");
                showSystemErrorDialog();
            }
        } else {
            new Reconnector(this, this).reconnect();
        }
    }

    /**
     * Called after we show the user a server error dialog, due to a failure of a request, and the
     * user clicks "Cancel"
     */
    @Override
    public void cancelServerError() {
        startActivity(new Intent(this, JoinGameActivity.class));
    }

    /**
     * Called after we show the user a connection lost dialog, due to a failure of a request, and the
     * user clicks "Try Again"
     */
    @Override
    public void retryConnection() {
        new Reconnector(this, this).reconnect();
    }

    /**
     * Called after we initiate a reconnect process using a Reconnector object, and the Reconnector
     * object wants to notify us that the reconnection process is complete
     */
    @Override
    public void reconnectionComplete() {
        // Attempt to get a list of all the open games in the system so that we can display them
        try {
            Server.getServerHelper().getOpenGames(this);
        }
        // This shouldn't happen. We won't be submitting requests to ServerHelper until the last
        // request has terminated. However, if it does, we display a dialog to the user and
        // present the option to try again.
        catch (MultipleRequestException e) {
            Log.e(tag, "MultipleRequestException thrown in response to getOpenGames() request");
            showSystemErrorDialog();
        }
    }
}