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
import com.lukaswillsie.onlinechess.network.helper.requesters.RestoreRequester;

import java.util.List;

/**
 * This class adapts a list of archived Game objects to be displayed in a RecyclerView. We allow the
 * user to restore, or "un-archive", games that they have archived, so this class provides code for
 * doing that. UserGamesAdapter, the superclass, provides all the code for inflating each Game's layout,
 * binding each Game to a view, and styling the view appropriately.
 */
public class ArchivedUserGamesAdapter extends UserGamesAdapter {
    /**
     * Will receive callbacks relating to a reconnection attempt initiated by this object
     */
    private ReconnectListener listener;

    /**
     * Create a new UserGamesAdapter with the information it needs to run
     *
     * @param games    - the list of games this UserGamesAdapter will be responsible for
     * @param activity - the Activity for which this object is doing its work; will be used for UI
     *                 operations, like displaying Toasts. We force this activity to be an
     *                 InteriorActivity because restoring games requires a network request, and we
     *                 need the activity to be able to handle a reconnection attempt if the network
     *                 request fails due to a loss of connection.
     */
    public ArchivedUserGamesAdapter(AppCompatActivity activity, List<UserGame> games, ReconnectListener listener) {
        super(activity, games);
        this.listener = listener;
    }

    /**
     * Called when our RecyclerView wants to bind a Game object to a View, wrapped up in holder
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
        int drawOffered = (Integer) game.getData(GameData.DRAW_OFFERED);


        if (userWon == 1) {
            setIconBackground(holder, R.drawable.restore_icon_game_over);
            setIconListener(holder, new RestoreListener(game));
        } else if (userLost == 1) {
            setIconBackground(holder, R.drawable.restore_icon_game_over);
            setIconListener(holder, new RestoreListener(game));
        } else if (drawn == 1) {
            setIconBackground(holder, R.drawable.restore_icon_game_over);
            setIconListener(holder, new RestoreListener(game));
        } else if (state == 0) {
            setIconBackground(holder, R.drawable.restore_icon_opponent_turn);
            setIconListener(holder, new RestoreListener(game));
        } else if (drawOffered == 1) {
            setIconBackground(holder, R.drawable.restore_icon_user_turn);
            setIconListener(holder, new RestoreListener(game));
        }
        // Otherwise, it's the user's turn and nothing irregular is going on
        else {
            setIconBackground(holder, R.drawable.restore_icon_user_turn);
            setIconListener(holder, new RestoreListener(game));
        }
    }

    /**
     * Listens to a restore button associated with a particular game, and attempts to restore that
     * game via a network request when the button is clicked.
     */
    private class RestoreListener implements View.OnClickListener, RestoreRequester {
        /*
         * The Game that this listener will restore when it registers a click event
         */
        private UserGame game;

        /**
         * Create a new ArchiveListener, which will restore the given Game object when a click event
         * is registered.
         *
         * @param game - the Game that this listener will restore when a click event is registered
         */
        private RestoreListener(UserGame game) {
            this.game = game;
        }

        @Override
        public void onClick(View view) {
            // Send the server a restore request
            Server.getServerHelper().restore((String) game.getData(GameData.GAMEID), this);
        }

        /**
         * Called by ServerHelper after the server has confirmed a restore request
         */
        @Override
        public void restoreSuccessful() {
            Display.makeToast(context, "Your game was restored successfully", Toast.LENGTH_SHORT);

            int pos = getGames().indexOf(game);
            getGames().remove(game);
            game.setArchived(false);
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
            Display.makeToast(context, "We lost our connection to the server and couldn't restore your game", Toast.LENGTH_LONG);
            // The cast to InteriorActivity below is fine, because we force activity to be an
            // InteriorActivity in our constructor
            new Reconnector(listener, (AppCompatActivity) context).reconnect();
        }

        /**
         * Called by ServerHelper if a restore request is met with an error server-side. Because
         * restoring a game isn't a big deal, we choose not to interrupt the user by displaying a
         * dialog, but instead just display a Toast.
         */
        @Override
        public void serverError() {
            Display.makeToast(context, "The server encountered an unexpected error and your game may not have been restored", Toast.LENGTH_LONG);
        }

        /**
         * Called by ServerHelper if a restore request is stymied by a system error of some kind.
         * Because restoring a game isn't a big deal, we choose not to interrupt the user by displaying
         * a dialog, but instead just display a Toast.
         */
        @Override
        public void systemError() {
            Display.makeToast(context, "We encountered an unexpected error and your game may not have been restored", Toast.LENGTH_LONG);
        }
    }
}
