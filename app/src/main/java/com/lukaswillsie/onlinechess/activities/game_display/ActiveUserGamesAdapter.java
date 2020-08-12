package com.lukaswillsie.onlinechess.activities.game_display;

import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.Display;
import com.lukaswillsie.onlinechess.activities.ReconnectListener;
import com.lukaswillsie.onlinechess.activities.Reconnector;
import com.lukaswillsie.onlinechess.data.GameData;
import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.Server;
import com.lukaswillsie.onlinechess.network.helper.requesters.ArchiveRequester;

import java.util.List;

/**
 * This class adapts a list of active games for a RecyclerView. Because the games are active, we
 * need a way for the user to archive them, if desired. UserGamesAdapter takes care of most of the
 * adapting work, this class just provides the logic necessary for the archiving feature.
 */
public class ActiveUserGamesAdapter extends UserGamesAdapter {
    /**
     * This class provides ArchiveListener objects that will attach to each game and attempt to
     * issue archive requests when the user clicks the archive button. If the archive request fails
     * because the connection with the server has been lost, the ArchiveListener will initiate a
     * reconnect request using a Reconnector and this object. It will receive a callback when a
     * reconnection attempt completes successfully.
     */
    private ReconnectListener listener;

    /**
     * Create a new ActiveUserGamesAdapter. The given activity will be used for UI operations, in the
     * event that an archive request fails and error dialogs need to be shown. If an archive request
     * initiated by this object fails due to a loss of connection, it will initiate a reconnection
     * attempt. The given listener will receive a callback when this attempt completes, so that the
     * UI can update in case any game data has changed since the reconnection.
     *
     * @param activity - should be the activity containing the RecyclerView this Adapter is working
     *                 for
     * @param games    - the list of UserGames this object will adapt
     * @param listener - will receive callbacks regarding any reconnection attempts initiated by
     *                 this object
     */
    public ActiveUserGamesAdapter(AppCompatActivity activity, List<UserGame> games, ReconnectListener listener) {
        super(activity, games);
        this.listener = listener;
    }

    /**
     * Called when the RecyclerView wants us to bind a particular game to a View, which is wrapped
     * up by holder
     *
     * @param holder   - the GameViewHolder wrapping the View that we will place the Game's data into
     * @param position - tells us which Game object to fetch and bind to the given View
     */
    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        UserGame game = getGames().get(position);

        int userWon = (Integer) game.getData(GameData.USER_WON);
        int userLost = (Integer) game.getData(GameData.USER_LOST);
        int drawn = (Integer) game.getData(GameData.DRAWN);
        int state = (Integer) game.getData(GameData.STATE);


        if (userWon == 1) {
            setIconBackground(holder, R.drawable.archive_icon_game_over);
            setIconListener(holder, new ArchiveListener(game));
        } else if (userLost == 1) {
            setIconBackground(holder, R.drawable.archive_icon_game_over);
            setIconListener(holder, new ArchiveListener(game));
        } else if (drawn == 1) {
            setIconBackground(holder, R.drawable.archive_icon_game_over);
            setIconListener(holder, new ArchiveListener(game));
        } else if (state == 0) {
            setIconBackground(holder, R.drawable.archive_icon_opponent_turn);
            setIconListener(holder, new ArchiveListener(game));
        } else {
            setIconBackground(holder, R.drawable.archive_icon_user_turn);
            setIconListener(holder, new ArchiveListener(game));
        }
    }

    /**
     * Listens to an archive icon associated with a particular game, and tries to archive that game
     * via a network request when the button is clicked.
     */
    private class ArchiveListener implements View.OnClickListener, ArchiveRequester {
        /*
         * The Game that this listener will archive when the View it is listening to is pressed
         */
        private UserGame game;

        /**
         * Create a new ArchiveListener, which will archive the given Game object when a click event
         * is registered. Assumes, of course, that the view this object listening to is the one
         * corresponding to the given game.
         *
         * @param game - the Game that this listener will archive when a click event is registered
         */
        private ArchiveListener(UserGame game) {
            this.game = game;
        }

        @Override
        public void onClick(View view) {
            // Send the server an archive request
            Server.getServerHelper().archive((String) game.getData(GameData.GAMEID), this);
        }

        /**
         * Called by ServerHelper after the server has confirmed an archive request.
         */
        @Override
        public void archiveSuccessful() {
            Display.makeToast(context, "Your archive was successful", Toast.LENGTH_LONG);

            int pos = getGames().indexOf(game);
            getGames().remove(game);
            game.setArchived(true);
            // Removes the card associated with the game from the RecyclerView on the screen
            notifyItemRemoved(pos);
        }

        /**
         * Called by ServerHelper if after an archive request is issued it realizes that the
         * connection with the server has been lost. Because archiving a game isn't a big deal, we
         * choose not to interrupt the user by displaying a dialog, but instead just display a
         * Toast, before starting a reconnection attempt.
         */
        @Override
        public void connectionLost() {
            Display.makeToast(context, "We lost our connection to the server and couldn't archive your game", Toast.LENGTH_LONG);
            // The cast to AppCompatActivity below is fine, because we force our context to be an
            // AppCompatActivity in our constructor
            new Reconnector(listener, (AppCompatActivity) context).reconnect();
        }

        /**
         * Called by ServerHelper if an archive request is met with an error server-side. Because
         * archiving a game isn't a big deal, we choose not to interrupt the user by displaying a
         * dialog, but instead just display a Toast.
         */
        @Override
        public void serverError() {
            Display.makeToast(context, "The server encountered an unexpected error and your game may not have been archived", Toast.LENGTH_LONG);
        }

        /**
         * Called by ServerHelper if an archive request is stymied by a system error of some kind.
         * Because archiving a game isn't a big deal, we choose not to interrupt the user by displaying
         * a dialog, but instead just display a Toast.
         */
        @Override
        public void systemError() {
            Display.makeToast(context, "We encountered an unexpected error and your game may not have been archived", Toast.LENGTH_LONG);
        }
    }
}
