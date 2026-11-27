package be.kuleuven.gt.javabean;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FavoriteGamesAdapter extends RecyclerView.Adapter<FavoriteGamesAdapter.GameViewHolder> {

    private final List<String> games;

    public FavoriteGamesAdapter(List<String> games) {
        this.games = games;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        holder.txtGame.setText(games.get(position));
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {
        TextView txtGame;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            txtGame = itemView.findViewById(android.R.id.text1);
        }
    }
}
