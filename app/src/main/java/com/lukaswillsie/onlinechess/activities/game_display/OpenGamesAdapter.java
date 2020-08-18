package com.lukaswillsie.onlinechess.activities.game_display;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.Display;
import com.lukaswillsie.onlinechess.activities.ReconnectListener;
import com.lukaswillsie.onlinechess.activities.Reconnector;
import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.GameData;
import com.lukaswillsie.onlinechess.data.ServerData;
import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.Server;
import com.lukaswillsie.onlinechess.network.helper.MultipleRequestException;
import com.lukaswillsie.onlinechess.network.helper.ServerHelper;
import com.lukaswillsie.onlinechess.network.helper.requesters.JoinGameRequester;

import java.util.List;

import static android.widget.Toast.LENGTH_LONG;

/**
 * This class is responsible for adapting a list of open games, i.e. games that any user can join,
 * for display by a RecyclerView. It also provides an OnClickListener that implements the behaviour
 * we want to see for open games: the user need simply click an open game's card to join it.
 */
public class OpenGamesAdapter extends RecyclerView.Adapter<OpenGamesAdapter.OpenGamesViewHolder> {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "OpenGamesAdapter";

    /**
     * The list of Game objects that this Adapter is adapting
     */
    private List<Game> games;

    /**
     * The activity containing the RecyclerView that this Adapter is working for
     */
    private AppCompatActivity activity;

    /**
     * The object that will receive callbacks if this object attempts to submit a join game request
     * to the server, and discovers the connection with the server has been lost.
     */
    private ReconnectListener listener;

    /**
     * Create a new OpenGamesAdapter to adapt the given list of Games for a RecyclerView.
     *
     * @param activity - should be the activity containing the RecyclerView this object will adapt
     *                 for. Is used for UI operations.
     * @param games    - the list of Game objects that this object will be adapting and working with
     * @param listener - if this object attempts to issue a join game request and discovers that the
     *                 connection to the server has been lost, this listener will receive
     *                 callbacks regarding the subsequent reconnection effort
     */
    public OpenGamesAdapter(AppCompatActivity activity, List<Game> games, ReconnectListener listener) {
        this.activity = activity;
        this.games = games;
        this.listener = listener;
    }

    /**
     * Update the adapter to show the given list of games, instead of the list it is showing now
     *
     * @param games - the new List of games to display
     */
    public void setGames(List<Game> games) {
        this.games = games;
        notifyDataSetChanged();
    }


    /**
     * Create a new View, inflated from game_card_layout.xml as a child of the given parent
     * ViewGroup.
     *
     * @param parent   - the ViewGroup which should be a parent of the new OpenGamesViewHolder
     * @param viewType - specifies the type of View to create and place in the ViewHolder (we don't
     *                 make use of this feature)
     * @return An OpenGamesViewHolder containing a View, newly-inflated from game_card_layout.xml,
     * as a child of parent.
     */
    @NonNull
    @Override
    public OpenGamesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View card = inflater.inflate(R.layout.game_card_layout, parent, false);

        return new OpenGamesViewHolder(card);
    }

    /**
     * Binds the Game specified by position to the given OpenGamesViewHolder by putting the
     * information from the specified Game into the View wrapped by the ViewHolder.
     *
     * @param holder   - the ViewHolder wrapping the View that we're binding to
     * @param position - specifies which Game in our collection to bind to the given View
     */
    @Override
    public void onBindViewHolder(@NonNull OpenGamesViewHolder holder, int position) {
        Game game = games.get(position);

        String gameID = (String) game.getData(ServerData.GAMEID);
        String owner;
        String status;

        // Because we assuming we are adapting open games, we can assume that there is only one
        // player in the game
        if (game.getData(ServerData.WHITE).equals("")) {
            owner = (String) game.getData(ServerData.BLACK);
            status = "You play white";
        } else {
            owner = (String) game.getData(ServerData.WHITE);
            status = "You play black";
        }
        int turn = (Integer) game.getData(ServerData.TURN);

        holder.gameID.setText(gameID);
        holder.opponent.setText(owner);
        holder.turn.setText(holder.turn.getContext().getString(R.string.turn_number_label, turn));
        holder.status.setText(status);

        holder.card.setBackground(holder.card.getResources().getDrawable(R.drawable.user_turn_background));
        holder.card.setOnClickListener(new OpenGameListener(game));
    }

    /**
     * Return the size of the collection of objects that this object is adapting for its
     * RecyclerView
     *
     * @return The size of the collection of objects that this object is adapting for its
     * RecyclerView
     */
    @Override
    public int getItemCount() {
        return games.size();
    }

    /**
     * Wraps up a game card to be used for displaying an open game's information
     */
    static class OpenGamesViewHolder extends RecyclerView.ViewHolder {
        /**
         * References to the important Views in the card
         */
        private View card;
        private TextView gameID;
        private TextView opponent;
        private TextView status;
        private TextView turn;

        /**
         * Create a new OpenGamesViewHolder to wrap the given View. The given View must have been
         * inflated from game_card_layout.xml.
         *
         * @param itemView - the game card that this object is going to wrap
         */
        public OpenGamesViewHolder(@NonNull View itemView) {
            super(itemView);

            this.gameID = itemView.findViewById(R.id.gameID);
            this.opponent = itemView.findViewById(R.id.opponent);
            this.status = itemView.findViewById(R.id.status);
            this.turn = itemView.findViewById(R.id.turn);
            this.card = itemView;
        }
    }

    /**
     * A class that will allow us to listen to the game cards we display on the screen. Will submit
     * a join game request to the server when a game card is clicked, and process the server's
     * response.
     */
    private class OpenGameListener implements View.OnClickListener, JoinGameRequester {
        /**
         * The Game object that this OnClickListener is listening to
         */
        private Game game;

        /**
         * Create a new OpenGameListener, to listen to a View object representing the given game.
         * When the View this object is listening to is clicked, this object will attempt to have
         * the current user join the given game
         *
         * @param game - the Game object that this listener will attempt to have the user join when
         *             the View this listener is listening to is clicked
         */
        private OpenGameListener(Game game) {
            this.game = game;
        }

        /**
         * Called when the View this object is listening to is clicked
         *
         * @param v - the View that was clicked
         */
        @Override
        public void onClick(View v) {
            ServerHelper serverHelper = Server.getServerHelper();
            String username = Server.getUsername();
            if (serverHelper == null) {
                new Reconnector(listener, activity).reconnect();
            } else {
                try {
                    serverHelper.joinGame(this, (String) game.getData(ServerData.GAMEID), username);
                } catch (MultipleRequestException e) {
                    Log.e(tag, "Made multiple requests of joinGameHelper");
                    Display.makeToast(activity, "We coudln't join that game. Please try again.", LENGTH_LONG);
                }
            }
        }

        /**
         * Called if the server says we successfully joined the specified game
         * <p>
         * NOTE: This does NOT indicate the end of the request; the data associated with the game that
         * was joined still needs to be received from the server. This is just a stop on the way.
         */
        @Override
        public void gameJoined() {
            // We don't do anything here; we wait until all the game's data has been sent before
            // updating the user
        }

        /**
         * Called if the gameID we gave isn't associated with a game, according to the server
         */
        @Override
        public void gameDoesNotExist() {
            Display.makeToast(activity, "Oops! We encountered an unexpected error and couldn't join that game", LENGTH_LONG);

            // If this method is being called, we know that the Game this object is listening to
            // was at one point sent to us by the server. So, the fact that it all of a sudden
            // doesn't exist is weird, and should be logged. Also, we take the server's word for it
            // and remove the game from the list since the user can't join it.
            Log.e(tag, "Server told us an open game doesn't exist anymore");
            int index = games.indexOf(game);
            if (index != -1) {
                games.remove(game);
                notifyItemRemoved(index);
            }
        }

        /**
         * Called if the gameID we gave is associated with a game that cannot be joined because it is
         * full
         */
        @Override
        public void gameFull() {
            Display.makeToast(activity, "Oops! Someone already joined that game", LENGTH_LONG);

            int index = games.indexOf(game);
            if (index != -1) {
                games.remove(game);
                notifyItemRemoved(index);
            }
        }

        /**
         * Called if the game we tried to join can't be joined because the user is already in it
         */
        @Override
        public void userAlreadyInGame() {
            Display.makeToast(activity, "You're already a player in that game", LENGTH_LONG);

            int index = games.indexOf(game);
            if (index != -1) {
                games.remove(game);
                notifyItemRemoved(index);
            }
        }

        /**
         * Called after a join game request has completed successfully. A UserGame object representing
         * the game that was joined is given as an argument for later usage, if needed.
         *
         * @param game - an object containing all the necessary information about the game that was
         *             joined
         */
        @Override
        public void joinGameComplete(UserGame game) {
            Display.makeToast(activity, "You joined game \"" + game.getData(GameData.GAMEID) + "\"", LENGTH_LONG);
            Server.getGames().add(game);

            int index = games.indexOf(this.game);
            if (index != -1) {
                games.remove(this.game);
                notifyItemRemoved(index);
            }
        }

        /**
         * Called if a join game request fails at any point due to a loss of connection
         */
        @Override
        public void connectionLost() {
            Display.makeToast(activity, "We lost our connection to the server", LENGTH_LONG);
            new Reconnector(listener, activity).reconnect();
        }

        /**
         * Called if a join game request fails at any point due to a server error
         */
        @Override
        public void serverError() {
            Display.makeToast(activity, "We encountered an unexpected server error and couldn't join that game", LENGTH_LONG);
        }

        /**
         * Called if a join game request fails at any point due to a system error
         */
        @Override
        public void systemError() {
            Display.makeToast(activity, "We encountered an unexpected server error and couldn't join that game", LENGTH_LONG);
        }
    }
}
