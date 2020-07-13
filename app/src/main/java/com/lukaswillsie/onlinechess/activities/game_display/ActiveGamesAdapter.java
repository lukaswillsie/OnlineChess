package com.lukaswillsie.onlinechess.activities.game_display;

import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.InteriorActivity;
import com.lukaswillsie.onlinechess.activities.Reconnector;
import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.GameData;
import com.lukaswillsie.onlinechess.network.helper.requesters.ArchiveRequester;

import java.util.List;

/**
 * This class adapts a list of active games for a RecyclerView. Because the games are active, we
 * need a way for the user to archive them, if desired. GamesAdapter takes of most of the adapting
 * work, this class just provides the logic necessary for the archiving feature.
 */
public class ActiveGamesAdapter extends GamesAdapter {
    /**
     * Create a new GamesAdapter with the information it needs to run
     *
     * @param games    - the list of games this GamesAdapter will be responsible for
     * @param activity - the Activity for which this object is doing its work; will be used for UI
     *                 operations, like displaying Toasts. We force this activity to be an
     *                 InteriorActivity because archiving games requires a network request, and we
     *                 need the activity to be able to handle a reconnection attempt if the network
     *                 request fails due to a loss of connection.
     */
    public ActiveGamesAdapter(List<Game> games, InteriorActivity activity) {
        super(games, activity);
    }

    /**
     * Called when the RecyclerView wants us to bind a particular game to a View, which is wrapped
     * up by holder
     *
     * @param holder - the GameViewHolder wrapping the View that we will place the Game's data into
     * @param position - tells us which Game object to fetch and bind to the given View
     */
    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        Game game = getGames().get(position);

        int userWon = (Integer)game.getData(GameData.USER_WON);
        int userLost = (Integer)game.getData(GameData.USER_LOST);
        int drawn = (Integer)game.getData(GameData.DRAWN);
        int state = (Integer)game.getData(GameData.STATE);
        int drawOffered = (Integer)game.getData(GameData.DRAW_OFFERED);


        if(userWon == 1) {
            setIconBackground(holder, R.drawable.archive_icon_game_over);
            setIconListener(holder, new ArchiveListener(game));
        }
        else if (userLost == 1) {
            setIconBackground(holder, R.drawable.archive_icon_game_over);
            setIconListener(holder, new ArchiveListener(game));
        }
        else if (drawn == 1) {
            setIconBackground(holder, R.drawable.archive_icon_game_over);
            setIconListener(holder, new ArchiveListener(game));
        }
        else if (state == 0) {
            setIconBackground(holder, R.drawable.archive_icon_opponent_turn);
            setIconListener(holder, new ArchiveListener(game));
        }
        else if (drawOffered == 1) {
            setIconBackground(holder, R.drawable.archive_icon_user_turn);
            setIconListener(holder, new ArchiveListener(game));
        }
        // Otherwise, it's the user's turn and nothing irregular is going on
        else {
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
        private Game game;

        /**
         * Create a new ArchiveListener, which will archive the given Game object when a click event
         * is registered. Assumes, of course, that the view this object listening to is the one
         * corresponding to the given game.
         *
         * @param game - the Game that this listener will archive when a click event is registered
         */
        private ArchiveListener(Game game) {
            this.game = game;
        }

        @Override
        public void onClick(View view) {
            // Send the server an archive request
            ((ChessApplication)view.getContext().getApplicationContext()).getServerHelper().archive((String)game.getData(GameData.GAMEID), this);
        }

        /**
         * Called by ServerHelper after the server has confirmed an archive request.
         */
        @Override
        public void archiveSuccessful() {
            Toast.makeText(activity, "Your game was archived successfully", Toast.LENGTH_SHORT).show();

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
            Toast.makeText(activity, "We lost our connection to the server and couldn't archive your game", Toast.LENGTH_LONG).show();
            // The cast to InteriorActivity below is fine, because we force activity to be an
            // InteriorActivity in our constructor
            new Reconnector((InteriorActivity) activity).reconnect();
        }

        /**
         * Called by ServerHelper if an archive request is met with an error server-side. Because
         * archiving a game isn't a big deal, we choose not to interrupt the user by displaying a
         * dialog, but instead just display a Toast.
         */
        @Override
        public void serverError() {
            Toast.makeText(activity, "The server encountered an unexpected error and your game may not have been archived", Toast.LENGTH_LONG).show();
        }

        /**
         * Called by ServerHelper if an archive request is stymied by a system error of some kind.
         * Because archiving a game isn't a big deal, we choose not to interrupt the user by displaying
         * a dialog, but instead just display a Toast.
         */
        @Override
        public void systemError() {
            Toast.makeText(activity, "We encountered an unexpected error and your game may not have been archived", Toast.LENGTH_LONG).show();
        }
    }
}
