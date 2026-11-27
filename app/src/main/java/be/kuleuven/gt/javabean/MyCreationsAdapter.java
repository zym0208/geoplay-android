package be.kuleuven.gt.javabean;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MyCreationsAdapter extends RecyclerView.Adapter<MyCreationsAdapter.ViewHolder> {

    private List<MyCreationsActivity.GameCreation> gameList;

    public MyCreationsAdapter(List<MyCreationsActivity.GameCreation> gameList) {
        this.gameList = gameList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textGameName;

        public ViewHolder(View itemView) {
            super(itemView);
            textGameName = itemView.findViewById(R.id.textGameName);
        }
    }

    @Override
    public MyCreationsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_game_creation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyCreationsAdapter.ViewHolder holder, int position) {
        MyCreationsActivity.GameCreation creation = gameList.get(position);
        holder.textGameName.setText(creation.gameName);
    }

    @Override
    public int getItemCount() {
        return gameList.size();
    }
}

