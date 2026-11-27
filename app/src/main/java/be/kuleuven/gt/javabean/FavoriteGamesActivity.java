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

public class FavoriteGamesActivity extends AppCompatActivity {

    private RecyclerView recyclerGames;
    private FavoriteGamesAdapter adapter;
    private ArrayList<String> gameList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_games);

        recyclerGames = findViewById(R.id.recyclerGames);
        recyclerGames.setLayoutManager(new LinearLayoutManager(this));

        gameList = new ArrayList<>();
        adapter = new FavoriteGamesAdapter(gameList);
        recyclerGames.setAdapter(adapter);

        fetchFavoriteGames();
    }

    private void fetchFavoriteGames() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String email = prefs.getString("email", null);

        if (email == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://studev.groept.be/api/a24pt110/getLike/" + email;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    gameList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            String gameName = obj.getString("GameName");
                            gameList.add(gameName);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    adapter.notifyDataSetChanged();
                },
                error -> {
                    Toast.makeText(FavoriteGamesActivity.this, "Failed to load favorite games", Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }
}
