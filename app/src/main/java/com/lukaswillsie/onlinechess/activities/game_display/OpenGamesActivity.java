package com.lukaswillsie.onlinechess.activities.game_display;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.ErrorDialogActivity;
import com.lukaswillsie.onlinechess.activities.JoinGameActivity;
import com.lukaswillsie.onlinechess.activities.OpenGamesAdapter;
import com.lukaswillsie.onlinechess.activities.ReconnectListener;
import com.lukaswillsie.onlinechess.activities.Reconnector;
import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.ServerData;
import com.lukaswillsie.onlinechess.network.helper.requesters.OpenGamesRequester;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;

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

        // If the OS shut our app down while we were in the background and is now restarting us,
        // we'll need to reconnect to the server. We detect that this has happened if our static
        // field serverHelper in ChessApplication has been nullified. If it hasn't we proceed as
        // usual and grab a list of open games from the server. If it IS null, we try to reconnect
        if (((ChessApplication) getApplicationContext()).getServerHelper() != null) {
            try {
                ((ChessApplication) getApplicationContext()).getServerHelper().getOpenGames(this);
            }
            // This shouldn't happen. We won't be submitting requests to ServerHelper until the last
            // request has terminated. However, if it does, we display a dialog to the user and
            // present the option to try again.
            catch (MultipleRequestException e) {
                Log.e(tag, "MultipleRequestException thrown in response to getOpenGames() request");
                createSystemErrorDialog();
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
        ChessApplication application = (ChessApplication) getApplicationContext();

        // Remove all games that the current user is in from the list (we don't want to show them
        // games that they can't join)
        int i = 0;
        while(i < games.size()) {
            if(((String)games.get(i).getData(ServerData.WHITE)).equals(application.getUsername())
            || ((String)games.get(i).getData(ServerData.BLACK)).equals(application.getUsername())) {
                games.remove(i);
            }
            else {
                i++;
            }
        }

        RecyclerView recyclerView = findViewById(R.id.games_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new OpenGamesAdapter(this, games, this));
    }

    /**
     * Called if an open games request is stymied by the discovery that our connection to the server
     * has been lost
     */
    @Override
    public void connectionLost() {
        super.createConnectionLostDialog();
    }

    /**
     * Called if an open games request fails due to a server error
     */
    @Override
    public void serverError() {
        super.createServerErrorDialog();
    }

    /**
     * Called if an open games request fails due to a system error
     */
    @Override
    public void systemError() {
        super.createSystemErrorDialog();
    }

    /**
     * Called after we show the user a system error dialog, due to a failure of a request, and the
     * user clicks "Try Again"
     */
    @Override
    public void retrySystemError() {
        // To retry, we attempt to load the list of openGames again, just like in onCreate. We still
        // have to check that our app hasn;t been terminated while in the background.
        if (((ChessApplication) getApplicationContext()).getServerHelper() != null) {
            try {
                ((ChessApplication) getApplicationContext()).getServerHelper().getOpenGames(this);
            }
            // This shouldn't happen. We won't be submitting requests to ServerHelper until the last
            // request has terminated. However, if it does, we display a dialog to the user and
            // present the option to try again.
            catch (MultipleRequestException e) {
                Log.e(tag, "MultipleRequestException thrown in response to getOpenGames() request");
                createSystemErrorDialog();
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
        // have to check that our app hasn;t been terminated while in the background.
        if (((ChessApplication) getApplicationContext()).getServerHelper() != null) {
            try {
                ((ChessApplication) getApplicationContext()).getServerHelper().getOpenGames(this);
            }
            // This shouldn't happen. We won't be submitting requests to ServerHelper until the last
            // request has terminated. However, if it does, we display a dialog to the user and
            // present the option to try again.
            catch (MultipleRequestException e) {
                Log.e(tag, "MultipleRequestException thrown in response to getOpenGames() request");
                createSystemErrorDialog();
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
            ((ChessApplication) getApplicationContext()).getServerHelper().getOpenGames(this);
        }
        // This shouldn't happen. We won't be submitting requests to ServerHelper until the last
        // request has terminated. However, if it does, we display a dialog to the user and
        // present the option to try again.
        catch (MultipleRequestException e) {
            Log.e(tag, "MultipleRequestException thrown in response to getOpenGames() request");
            createSystemErrorDialog();
        }
    }
}