package com.lukaswillsie.onlinechess.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.ServerData;

import java.util.List;

public class OpenGamesAdapter extends RecyclerView.Adapter<OpenGamesAdapter.OpenGamesViewHolder> {
    private List<Game> games;

    public OpenGamesAdapter(List<Game> games) {
        this.games = games;
    }

    @NonNull
    @Override
    public OpenGamesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View card = inflater.inflate(R.layout.game_card_layout, parent, false);

        return new OpenGamesViewHolder(card);
    }

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
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    static class OpenGamesViewHolder extends RecyclerView.ViewHolder {
        private View card;
        private TextView gameID;
        private TextView opponent;
        private TextView status;
        private TextView turn;

        public OpenGamesViewHolder(@NonNull View itemView) {
            super(itemView);

            this.gameID = itemView.findViewById(R.id.gameID);
            this.opponent = itemView.findViewById(R.id.opponent);
            this.status = itemView.findViewById(R.id.status);
            this.turn = itemView.findViewById(R.id.turn);
            this.card = itemView;
        }
    }
}
