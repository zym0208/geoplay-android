package be.kuleuven.gt.javabean;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ScoreComparisonAdapter extends RecyclerView.Adapter<ScoreComparisonAdapter.ScoreViewHolder> {
    private final List<ScoreComparison> scoreList;

    public ScoreComparisonAdapter(List<ScoreComparison> scoreList) {
        this.scoreList = scoreList;
    }

    @NonNull
    @Override
    public ScoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_score_comparison, parent, false);
        return new ScoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScoreViewHolder holder, int position) {
        ScoreComparison item = scoreList.get(position);
        holder.tvGameName.setText(item.getGameName());
        holder.tvYourScore.setText("You: " + (int)item.getYourScore());
        holder.tvFriendScore.setText("Friend: " + (int)item.getFriendScore());
    }

    @Override
    public int getItemCount() {
        return scoreList.size();
    }
    public void updateData(List<ScoreComparison> newData) {
        scoreList.clear();
        scoreList.addAll(newData);
        notifyDataSetChanged();
    }


    public static class ScoreViewHolder extends RecyclerView.ViewHolder {
        TextView tvGameName, tvYourScore, tvFriendScore;

        public ScoreViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGameName = itemView.findViewById(R.id.tvGameName);
            tvYourScore = itemView.findViewById(R.id.tvYourScore);
            tvFriendScore = itemView.findViewById(R.id.tvFriendScore);
        }
    }
}