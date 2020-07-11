package com.lukaswillsie.onlinechess.activities.game_display;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.InteriorActivity;
import com.lukaswillsie.onlinechess.activities.Reconnector;
import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.GameData;
import com.lukaswillsie.onlinechess.network.helper.requesters.ArchiveRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.ArchivingRequester;
import com.lukaswillsie.onlinechess.network.helper.requesters.RestoreRequester;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;

import java.util.List;

/**
 * This class converts Game objects into UI elements, little cards containing important information
 * about the game for the user, at the behest of a RecyclerView. See game_card_layout.xml for the
 * basic layout file that the Adapter inflates before applying styling specific to the game. Also
 * see the comments in activity_active_games.xml for a few examples, in XML, of what this class
 * accomplishes with code.
 */
public class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.GameViewHolder> {
    /*
     * The list of Game objects this class is adapting for the RecyclerView
     */
    private List<Game> games;

    /*
     * Whether the list of games is active, meaning that the user can archive them if they want.
     * If the games are active, they're rendered with an icon the user can click to archive them. If
     * they're not active, they're rendered with an icon the user can click to restore (or
     * un-archive) them.
     */
    private boolean active;

    private InteriorActivity activity;

    /**
     * Create a new GamesAdapter with the information it needs to run
     * @param games - the list of games this GamesAdapter will be responsible for
     * @param active - whether or not the given list of games is active. If true, requester must
     *               implement ArchiveRequester for archiving functionality to work. If false,
     *               requester must implement RestoreRequester for restoration functionality to
     *               work.
     * @param activity - the Activity for which this object is doing its work; will be used for UI
     *                 operations, like displaying Toasts. If this object attempts to submit an
     *                 archive/restore request to the server, and discovers the connection to the
     *                 server to have been lost, this activity will be used in conjunction with a
     *                 Reconnector object to re-establish a connection to the server.
     */
    GamesAdapter(List<Game> games, boolean active, InteriorActivity activity) {
        this.games = games;
        this.active = active;
        this.activity = activity;
    }

    /**
     * Create an empty, basic game card as a child of the given parent, and return a GameViewHolder as a
     * wrapper for it.
     * @param parent  the parent ViewGroup that the View we created and wrap wit a GameViewHolder should
     *                be a child of
     * @param viewType - the view type of the new view (not used in this implementation)
     * @return a GameViewHolder wrapping a newly-created game card View created as a child of the
     * given parent
     */
    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        Log.i("ADAPTER", parent.toString());

        View gameCard = inflater.inflate(R.layout.game_card_layout, parent, false);

       return new GameViewHolder(gameCard);
    }

    /**
     * Called when the RecyclerView wants to bind a new Game object to a View for being displayed.
     * @param holder - the GameViewHolder wrapping the View that we will place the Game's data into
     * @param position - tells us which Game object to fetch from our and bind to the given View
     */
    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        Game game = games.get(position);
        Resources resources = holder.gameID.getContext().getResources();

        // Fetch a bunch of data about the game
        String gameID = (String)game.getData(GameData.GAMEID);
        String opponent = (String)game.getData(GameData.OPPONENT);

        int userWon = (Integer)game.getData(GameData.USER_WON);
        int userLost = (Integer)game.getData(GameData.USER_LOST);
        int state = (Integer)game.getData(GameData.STATE);
        int turn = (Integer)game.getData(GameData.TURN);
        int drawn = (Integer)game.getData(GameData.DRAWN);
        int drawOffered = (Integer)game.getData(GameData.DRAW_OFFERED);

        holder.gameID.setText(gameID.toUpperCase());
        holder.opponent.setText(resources.getString(R.string.opponent_label, opponent));
        holder.turn.setText(holder.turn.getContext().getString(R.string.turn_number_label, turn));

        if(userWon == 1) {
            holder.status.setText(R.string.user_win);
            holder.status.setTextColor(resources.getColor(R.color.user_win));

            if(active) {
                holder.archive.setBackground(resources.getDrawable(R.drawable.archive_icon_game_over));
                holder.archive.setOnClickListener(new ArchiveListener(game));
            }
            else {
                holder.archive.setBackground(resources.getDrawable(R.drawable.restore_icon_game_over));
                holder.archive.setOnClickListener(new RestoreListener(game));
            }

            holder.card.setBackground(resources.getDrawable(R.drawable.game_over_background));
        }
        else if (userLost == 1) {
            holder.status.setText(R.string.user_lose);
            holder.status.setTextColor(resources.getColor(R.color.user_lose));

            if(active) {
                holder.archive.setBackground(resources.getDrawable(R.drawable.archive_icon_game_over));
                holder.archive.setOnClickListener(new ArchiveListener(game));
            }
            else {
                holder.archive.setBackground(resources.getDrawable(R.drawable.restore_icon_game_over));
                holder.archive.setOnClickListener(new RestoreListener(game));
            }

            holder.card.setBackground(resources.getDrawable(R.drawable.game_over_background));
        }
        else if (drawn == 1) {
            holder.status.setText(R.string.game_drawn);
            holder.status.setTextColor(resources.getColor(R.color.drawn));

            if(active) {
                holder.archive.setBackground(resources.getDrawable(R.drawable.archive_icon_game_over));
                holder.archive.setOnClickListener(new ArchiveListener(game));
            }
            else {
                holder.archive.setBackground(resources.getDrawable(R.drawable.restore_icon_game_over));
                holder.archive.setOnClickListener(new RestoreListener(game));
            }

            holder.card.setBackground(resources.getDrawable(R.drawable.game_over_background));
        }
        else if (state == 0) {
            holder.status.setText(R.string.opponent_turn);
            holder.status.setTextColor(resources.getColor(R.color.opponent_turn));
            holder.status.setAlpha(0.75f);

            if(active) {
                holder.archive.setBackground(resources.getDrawable(R.drawable.archive_icon_opponent_turn));
                holder.archive.setOnClickListener(new ArchiveListener(game));
            }
            else {
                holder.archive.setBackground(resources.getDrawable(R.drawable.restore_icon_opponent_turn));
                holder.archive.setOnClickListener(new RestoreListener(game));
            }

            holder.card.setBackground(resources.getDrawable(R.drawable.opponent_turn_background));
        }
        else if (drawOffered == 1) {
            holder.status.setText(R.string.draw_offered_to_user);
            holder.status.setTextColor(resources.getColor(R.color.user_turn));

            if(active) {
                holder.archive.setBackground(resources.getDrawable(R.drawable.archive_icon_user_turn));
                holder.archive.setOnClickListener(new ArchiveListener(game));
            }
            else {
                holder.archive.setBackground(resources.getDrawable(R.drawable.restore_icon_user_turn));
                holder.archive.setOnClickListener(new RestoreListener(game));
            }

            holder.card.setBackground(resources.getDrawable(R.drawable.user_turn_background));
        }
        else {
            holder.status.setText(R.string.user_turn);
            holder.status.setTextColor(resources.getColor(R.color.user_turn));

            if(active) {
                holder.archive.setBackground(resources.getDrawable(R.drawable.archive_icon_user_turn));
                holder.archive.setOnClickListener(new ArchiveListener(game));
            }
            else {
                holder.archive.setBackground(resources.getDrawable(R.drawable.restore_icon_user_turn));
                holder.archive.setOnClickListener(new RestoreListener(game));
            }

            holder.card.setBackground(resources.getDrawable(R.drawable.user_turn_background));
        }
    }

    /**
     * Return the total number of items that the RecyclerView has to be able to display
     * @return the total number of items that the RecyclerView has to be able to display
     */
    @Override
    public int getItemCount() {
        return games.size();
    }

    /**
     * Provides a wrapper for easy access to the UI elements contained in a game card, which should
     * have been inflated from game_card_layout.xml and passed to the constructor for this class.
     */
    static class GameViewHolder extends RecyclerView.ViewHolder {
        /*
         * Contains a reference to the View that is the root of the game card being wrapped by this
         * object
         */
        private View card;

        /*
         * A reference to each of the Views that are a part of the game card.
         */
        private TextView gameID;
        private TextView opponent;
        private TextView status;
        private TextView turn;
        private ImageView archive;

        /**
         * Create a new GameViewHolder as a wrapper for the given View. This View should be the
         * result of an inflation of game_card_layout.xml.
         *
         * @param itemView - the View that this GameViewHolder object will act as a wrapper for
         */
        public GameViewHolder(@NonNull View itemView) {
            super(itemView);

            this.gameID = itemView.findViewById(R.id.gameID);
            this.opponent = itemView.findViewById(R.id.opponent);
            this.status = itemView.findViewById(R.id.status);
            this.turn = itemView.findViewById(R.id.turn);
            this.archive = itemView.findViewById(R.id.archive);
            this.card = itemView;
        }
    }

    /**
     * Listens to an archive icon associated with a particular game, and tries to archive that game
     * when the button is clicked.
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

            int pos = games.indexOf(game);
            games.remove(game);
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
            new Reconnector(activity).reconnect();
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

    /**
     * Listens to a restore button associated with a particular game, and attempts to restore that
     * game when the button is clicked.
     */
    private class RestoreListener implements View.OnClickListener, RestoreRequester {
        /*
         * The Game that this listener will restore when it registers a click event
         */
        private Game game;

        /**
         * Create a new ArchiveListener, which will restore the given Game object when a click event
         * is registered.
         * @param game - the Game that this listener will restore when a click event is registered
         */
        private RestoreListener(Game game) {
            this.game = game;
        }

        @Override
        public void onClick(View view) {
            // Send the server a restore request
            ((ChessApplication)view.getContext().getApplicationContext()).getServerHelper().restore((String) game.getData(GameData.GAMEID), this);
        }

        /**
         * Called by ServerHelper after the server has confirmed a restore request
         */
        @Override
        public void restoreSuccessful() {
            Toast.makeText(activity, "Your game was restored successfully", Toast.LENGTH_SHORT).show();

            int pos = games.indexOf(game);
            games.remove(game);
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
            Toast.makeText(activity, "We lost our connection to the server and couldn't restore your game", Toast.LENGTH_LONG).show();
            new Reconnector(activity).reconnect();
        }

        /**
         * Called by ServerHelper if a restore request is met with an error server-side. Because
         * restoring a game isn't a big deal, we choose not to interrupt the user by displaying a
         * dialog, but instead just display a Toast.
         */
        @Override
        public void serverError() {
            Toast.makeText(activity, "The server encountered an unexpected error and your game may not have been restored", Toast.LENGTH_LONG).show();
        }

        /**
         * Called by ServerHelper if a restore request is stymied by a system error of some kind.
         * Because restoring a game isn't a big deal, we choose not to interrupt the user by displaying
         * a dialog, but instead just display a Toast.
         */
        @Override
        public void systemError() {
            Toast.makeText(activity, "We encountered an unexpected error and your game may not have been restored", Toast.LENGTH_LONG).show();
        }
    }
}