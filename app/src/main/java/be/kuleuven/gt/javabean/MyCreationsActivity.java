package be.kuleuven.gt.javabean;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MyCreationsActivity extends AppCompatActivity {

    private RecyclerView recyclerQuiz, recyclerRace;
    private MyCreationsAdapter quizAdapter, raceAdapter;
    private ArrayList<GameCreation> quizList, raceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_creations);

        recyclerQuiz = findViewById(R.id.recyclerQuiz);
        recyclerRace = findViewById(R.id.recyclerRace);

        recyclerQuiz.setLayoutManager(new LinearLayoutManager(this));
        recyclerRace.setLayoutManager(new LinearLayoutManager(this));

        quizList = new ArrayList<>();
        raceList = new ArrayList<>();

        quizAdapter = new MyCreationsAdapter(quizList);
        raceAdapter = new MyCreationsAdapter(raceList);

        recyclerQuiz.setAdapter(quizAdapter);
        recyclerRace.setAdapter(raceAdapter);

        fetchMyCreations();
    }

    private void fetchMyCreations() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String email = prefs.getString("email", null);

        if (email == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://studev.groept.be/api/a24pt110/getMyCreation/" + email;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    quizList.clear();
                    raceList.clear();

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            String gameName = obj.getString("GameName");
                            String gameType = obj.getString("GameType");

                            GameCreation creation = new GameCreation(gameName, gameType);
                            if (gameType.equalsIgnoreCase("Quiz")) {
                                quizList.add(creation);
                            } else if (gameType.equalsIgnoreCase("Race")) {
                                raceList.add(creation);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    quizAdapter.notifyDataSetChanged();
                    raceAdapter.notifyDataSetChanged();
                },
                error -> {
                    Toast.makeText(MyCreationsActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    public static class GameCreation {
        public final String gameName;
        public final String gameType;

        public GameCreation(String gameName, String gameType) {
            this.gameName = gameName;
            this.gameType = gameType;
        }
    }
}
