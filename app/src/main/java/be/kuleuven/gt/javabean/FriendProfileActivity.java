package be.kuleuven.gt.javabean;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FriendProfileActivity extends AppCompatActivity {

    private String currentType = "Quiz";  // 初始显示 Quiz 类型
    private RecyclerView recyclerView;
    private ScoreComparisonAdapter adapter;
    private List<ScoreComparison> comparisonList = new ArrayList<>();

    private String yourEmail, friendEmail, friendName;
    private int pendingRequests = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_friend_profile);

        // UI 绑定
        recyclerView = findViewById(R.id.rvScoreComparison);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScoreComparisonAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        yourEmail = prefs.getString("email", null);

        friendEmail = getIntent().getStringExtra("friend_email");
        friendName = getIntent().getStringExtra("friend_name");

        TextView txtName = findViewById(R.id.txtName);
        txtName.setText(friendName);

        ImageView imageView = findViewById(R.id.imageView);
        // 异步加载好友头像
        ImageUploader.getImageFromServer(this, friendEmail, bitmap -> {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.account_circle_24px); // 默认头像
            }
        });

        setupGameTypeSwitcher();
        TextView txtGameType = findViewById(R.id.txtGameType);
        txtGameType.setText("Game Type: " + currentType);

        loadScoresAndCompare();
        ImageButton btnDelete = findViewById(R.id.btnDeleteFriend);
        btnDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Friend")
                    .setMessage("Are you sure you want to remove " + friendName + " from your friend list?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteFriend())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

    }

    private void setupGameTypeSwitcher() {
        TextView txtGameType = findViewById(R.id.txtGameType);
        txtGameType.setText("Game Type: " + currentType); // 初始化显示

        ImageView btnSwitch = findViewById(R.id.btnSwitchGameType);
        btnSwitch.setOnClickListener(v -> {
            currentType = currentType.equals("Quiz") ? "Race" : "Quiz";
            txtGameType.setText("Game Type: " + currentType);
            filterDisplayedScores();
            Log.d("GameData", "Loaded " + currentType + " scores: " + comparisonList.size());
        });
    }


    private void loadScoresAndCompare() {
        comparisonList.clear();
        pendingRequests = 2;

        loadGameScores("Quiz", "ScorePercentage",
                "https://studev.groept.be/api/a24pt110/getMyQuizgame/" + yourEmail,
                "https://studev.groept.be/api/a24pt110/getMyQuizgame/" + friendEmail);

        loadGameScores("Race", "ScoreSeconds",
                "https://studev.groept.be/api/a24pt110/getMyRacegame/" + yourEmail,
                "https://studev.groept.be/api/a24pt110/getMyRacegame/" + friendEmail);
    }

    private void loadGameScores(String gameType, String scoreField, String urlMy, String urlFriend) {
        RequestQueue queue = Volley.newRequestQueue(this);
        Map<String, Integer> myScores = new HashMap<>();
        Map<String, Integer> friendScores = new HashMap<>();

        JsonArrayRequest reqMe = new JsonArrayRequest(Request.Method.GET, urlMy, null,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            myScores.put(obj.getString("GameName"), obj.getInt(scoreField));
                        } catch (JSONException e) {
                            Log.e("GameData", "Parse my " + gameType + " error: " + e.getMessage());
                        }
                    }

                    JsonArrayRequest reqFriend = new JsonArrayRequest(Request.Method.GET, urlFriend, null,
                            friendResponse -> {
                                for (int i = 0; i < friendResponse.length(); i++) {
                                    try {
                                        JSONObject obj = friendResponse.getJSONObject(i);
                                        friendScores.put(obj.getString("GameName"), obj.getInt(scoreField));
                                    } catch (JSONException e) {
                                        Log.e("GameData", "Parse friend " + gameType + " error: " + e.getMessage());
                                    }
                                }

                                Set<String> allGames = new HashSet<>(myScores.keySet());
                                allGames.addAll(friendScores.keySet());

                                for (String game : allGames) {
                                    int myScore = myScores.getOrDefault(game, 0);
                                    int frScore = friendScores.getOrDefault(game, 0);
                                    comparisonList.add(new ScoreComparison(game, myScore, frScore, gameType));
                                }

                                pendingRequests--;
                                if (pendingRequests == 0) {
                                    filterDisplayedScores();
                                }
                            },
                            error -> {
                                Log.e("GameData", "Volley error friend " + gameType + ": " + error.getMessage());
                                pendingRequests--;
                                if (pendingRequests == 0) {
                                    filterDisplayedScores();
                                }
                            });

                    queue.add(reqFriend);
                },
                error -> {
                    Log.e("GameData", "Volley error my " + gameType + ": " + error.getMessage());
                    pendingRequests--;
                    if (pendingRequests == 0) {
                        filterDisplayedScores();
                    }
                });

        queue.add(reqMe);
    }

    private void filterDisplayedScores() {
        List<ScoreComparison> filtered = new ArrayList<>();
        for (ScoreComparison sc : comparisonList) {
            if (sc.getGameType().equals(currentType)) {
                filtered.add(sc);
            }
        }
        adapter.updateData(filtered);
    }
    private void deleteFriend() {
        String url = "https://studev.groept.be/api/a24pt110/DeleteFriend/"
                + yourEmail + "/" + friendEmail + "/" + yourEmail + "/" + friendEmail;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d("DeleteFriend", "Friend deleted successfully");
                    finish(); // 删除成功后退出页面
                },
                error -> {
                    Log.e("DeleteFriend", "Error deleting friend: " + error.getMessage());
                });

        queue.add(request);
    }

}
