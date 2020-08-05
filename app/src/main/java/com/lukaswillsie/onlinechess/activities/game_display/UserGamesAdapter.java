package com.lukaswillsie.onlinechess.activities.game_display;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.board.BoardActivity;
import com.lukaswillsie.onlinechess.data.GameData;
import com.lukaswillsie.onlinechess.data.UserGame;

import java.util.List;

/**
 * This class converts Game objects into UI elements, little cards containing important information
 * about the game for the user, at the behest of a RecyclerView. See game_card_layout.xml for the
 * basic layout file that the Adapter inflates before applying styling specific to the game. This
 * class does not do anything with the ImageView in the top-right corner of game_card_layout. It
 * simply fills in and styles the TextViews and background of the card.
 */
public class UserGamesAdapter extends RecyclerView.Adapter<UserGamesAdapter.GameViewHolder> {
    /**
     * The Context that this object will use to access app resources.
     */
    protected Context context;

    /*
     * The list of Game objects this class is adapting for the RecyclerView
     */
    private List<UserGame> games;

    /**
     * Create a new UserGamesAdapter with the information it needs to run
     *
     * @param games   - the list of games this UserGamesAdapter will be responsible for
     * @param context - the Context this object is working for; will be used to access app
     *                resources.
     */
    public UserGamesAdapter(Context context, List<UserGame> games) {
        this.games = games;
        this.context = context;
    }

    /**
     * Create an empty, basic game card as a child of the given parent, and return a GameViewHolder as a
     * wrapper for it.
     *
     * @param parent   the parent ViewGroup that the View we created and wrap wit a GameViewHolder should
     *                 be a child of
     * @param viewType - the view type of the new view (not used in this implementation)
     * @return a GameViewHolder wrapping a newly-created game card View created as a child of the
     * given parent
     */
    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View gameCard = inflater.inflate(R.layout.game_card_layout, parent, false);

        return new GameViewHolder(gameCard);
    }

    /**
     * Called when the RecyclerView wants to bind a new UserGame object to a View for being
     * displayed. Fills in and styles all the fields in the given GameViewHolder according to the
     * state of the game at position.
     * <p>
     * Also applies an OnClickListener to the whole card being wrapped by the given GameViewHolder.
     * The OnClickListener makes it so that when the user clicks the card associated with a
     * particular game, that game is launched in BoardActivity so that the user can view the board.
     * Subclasses that want different onClick behaviour should override setCardListener(), since
     * this method calls that method during execution.
     *
     * @param holder   - the GameViewHolder wrapping the View that we will place the Game's data into
     * @param position - tells us which Game object to fetch from our and bind to the given View
     */
    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        UserGame game = games.get(position);
        Resources resources = context.getResources();

        setCardListener(holder, position);

        // Fetch data about the game
        String gameID = (String) game.getData(GameData.GAMEID);
        String opponent = (String) game.getData(GameData.OPPONENT);
        int open = (Integer) game.getData(GameData.OPEN);

        int userWon = (Integer) game.getData(GameData.USER_WON);
        int userLost = (Integer) game.getData(GameData.USER_LOST);
        int state = (Integer) game.getData(GameData.STATE);
        int turn = (Integer) game.getData(GameData.TURN);
        int drawn = (Integer) game.getData(GameData.DRAWN);
        int drawOffered = (Integer) game.getData(GameData.DRAW_OFFERED);

        holder.turn.setText(holder.turn.getContext().getString(R.string.turn_number_label, turn));

        holder.gameID.setText(gameID.toUpperCase());

        if (opponent.length() > 0) {
            holder.opponent.setText(resources.getString(R.string.opponent_label, opponent));
        } else if (open == 1) {
            holder.opponent.setText(R.string.no_opponent_open);
        } else {
            holder.opponent.setText(R.string.no_opponent_closed);
            holder.status.setText("Waiting for opponent");
            holder.status.setTextColor(resources.getColor(R.color.opponent_turn));

            holder.card.setBackground(resources.getDrawable(R.drawable.opponent_turn_background));
            return;
        }

        if (userWon == 1) {
            holder.status.setText(R.string.user_win);
            holder.status.setTextColor(resources.getColor(R.color.user_win));

            holder.card.setBackground(resources.getDrawable(R.drawable.game_over_background));
        } else if (userLost == 1) {
            holder.status.setText(R.string.user_lose);
            holder.status.setTextColor(resources.getColor(R.color.user_lose));

            holder.card.setBackground(resources.getDrawable(R.drawable.game_over_background));
        } else if (drawn == 1) {
            holder.status.setText(R.string.game_drawn);
            holder.status.setTextColor(resources.getColor(R.color.drawn));

            holder.card.setBackground(resources.getDrawable(R.drawable.game_over_background));
        } else if (state == 0) {
            holder.status.setText(R.string.opponent_turn);
            holder.status.setTextColor(resources.getColor(R.color.opponent_turn));
            holder.status.setAlpha(0.75f);

            holder.card.setBackground(resources.getDrawable(R.drawable.opponent_turn_background));
        } else if (drawOffered == 1) {
            holder.status.setText(R.string.draw_offered_to_user);
            holder.status.setTextColor(resources.getColor(R.color.user_turn));

            holder.card.setBackground(resources.getDrawable(R.drawable.user_turn_background));
        } else {
            holder.status.setText(R.string.user_turn);
            holder.status.setTextColor(resources.getColor(R.color.user_turn));

            holder.card.setBackground(resources.getDrawable(R.drawable.user_turn_background));
        }
    }

    /**
     * Sets the background of the icon in the top-right corner of the game card to the given
     * drawable.
     *
     * @param holder - the GameViewHolder object wrapping up the view that this method should edit
     * @param resID  - the ID of the drawable to place in the background of the icon
     */
    protected void setIconBackground(GameViewHolder holder, @DrawableRes int resID) {
        holder.archive.setBackground(context.getResources().getDrawable(resID));
    }

    /**
     * Apply the given listener to the icon in the top-right corner of the game card.
     *
     * @param holder   - the GameViewHolder object wrapping up the view that this method should edit
     * @param listener - the OnClickListener to apply
     */
    protected void setIconListener(GameViewHolder holder, View.OnClickListener listener) {
        holder.archive.setOnClickListener(listener);
    }

    /**
     * Applies an OnClickListener to the whole card being wrapped by the given GameViewHolder.
     * Subclasses can override this method to apply their own OnClickListeners to game cards. The
     * OnClickListener applied by this method will start BoardActivity and allow the user to view
     * the state of the board in the game at position.
     *
     * @param holder   - the GameViewHolder wrapping the card that we'll apply the listener to
     * @param position - specifies which game's data is bound to the specified holder
     */
    protected void setCardListener(GameViewHolder holder, int position) {
        holder.card.setOnClickListener(new GameCardListener((String) games.get(position).getData(GameData.GAMEID)));
    }

    /**
     * Return the total number of items that the RecyclerView has to be able to display
     *
     * @return the total number of items that the RecyclerView has to be able to display
     */
    @Override
    public int getItemCount() {
        return games.size();
    }

    protected List<UserGame> getGames() {
        return games;
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

    private class GameCardListener implements View.OnClickListener {
        /**
         * The ID of the game that this Listener is listening to
         */
        private final String gameID;

        /**
         * Create a new GameCardListener that, when fired, will launch BoardActivity
         *
         * @param gameID
         */
        private GameCardListener(String gameID) {
            this.gameID = gameID;
        }

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(context, BoardActivity.class);
            intent.putExtra(BoardActivity.GAMEID_TAG, gameID);
            context.startActivity(intent);
        }
    }
}