package be.kuleuven.gt.javabean;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import be.kuleuven.gt.javabean.FriendManager;
import be.kuleuven.gt.javabean.R;

public class PendingFriendRequestsActivity extends AppCompatActivity {
    private LinearLayout requestList;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_requests); // 关联 activity 布局
        requestList = findViewById(R.id.requestList); // 获取容器
        queue = Volley.newRequestQueue(this); // 初始化请求队列

        // 从 SharedPreferences 获取用户邮箱
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String email = prefs.getString("email", null);
        if (email != null) {
            fetchRequests(email); // 获取请求列表
        }
    }

    private void fetchRequests(String email) {
        String url = "https://studev.groept.be/api/a24pt110/getFriendRequests/" + email;
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    requestList.removeAllViews(); // 清空现有的请求项
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            String fromUser = obj.getString("FromUsername");
                            int requestId = obj.getInt("RequestID");

                            // 加载每个请求项的视图
                            View item = LayoutInflater.from(this).inflate(R.layout.item_request, null);
                            TextView txtUser = item.findViewById(R.id.txtFromUser);
                            Button btnAccept = item.findViewById(R.id.btnAccept);
                            Button btnReject = item.findViewById(R.id.btnReject);

                            // 设置请求项的内容
                            txtUser.setText(getString(R.string.friend_request_from, fromUser));
                            btnAccept.setOnClickListener(v ->
                                    FriendManager.respondToRequest(this, requestId, true, queue, () -> fetchRequests(email)));
                            btnReject.setOnClickListener(v ->
                                    FriendManager.respondToRequest(this, requestId, false, queue, () -> fetchRequests(email)));

                            requestList.addView(item); // 将请求项添加到容器中






                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> Toast.makeText(this, R.string.request_failed, Toast.LENGTH_SHORT).show()
        );
        queue.add(request); // 添加请求到队列
    }
}
