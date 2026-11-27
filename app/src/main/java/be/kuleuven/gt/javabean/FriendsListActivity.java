package be.kuleuven.gt.javabean;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class FriendsListActivity extends AppCompatActivity {
    private List<Friend> friends = new ArrayList<>();
    private FriendAdapter adapter;
    private ListView listView; // 类成员变量声明
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        listView = findViewById(R.id.listViewFriends);
        adapter = new FriendAdapter(this, friends);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Friend selectedFriend = friends.get(position);
            Intent intent = new Intent(FriendsListActivity.this, FriendProfileActivity.class);
            intent.putExtra("friend_email", selectedFriend.getEmail());
            intent.putExtra("friend_name", selectedFriend.getUsername());

            startActivity(intent);
        });

        loadFriends();
    }

    private void loadFriends() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userEmail = prefs.getString("email", null);
        String url = "https://studev.groept.be/api/a24pt110/getFriends/" + userEmail + "/" + userEmail + "/" + userEmail;

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray arr = new JSONArray(response);
                        friends.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            String username = obj.getString("Username");
                            String email = obj.getString("Email");
                            friends.add(new Friend(username, email));
                        }
                        adapter.notifyDataSetChanged(); // 初步刷新，头像异步加载
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Failed to load friends", Toast.LENGTH_SHORT).show()
        );
        queue.add(request);
    }


}
