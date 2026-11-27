package be.kuleuven.gt.javabean;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;

public class FriendManager {
    public static void sendFriendRequest(Context context, String fromEmail, String toUsername, RequestQueue queue) {
        String url = "https://studev.groept.be/api/a24pt110/sendFriendRequest/" + fromEmail + "/" + toUsername;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> Toast.makeText(context, R.string.request_sent, Toast.LENGTH_SHORT).show(),
                error -> Toast.makeText(context, R.string.request_failed, Toast.LENGTH_SHORT).show()
        );
        queue.add(request);
    }

    public static void respondToRequest(Context context, int requestId, boolean accept, RequestQueue queue, Runnable callback) {
        String url;
        if (accept) {
            // 接受请求，调用 respondFriendRequest API
            url = "https://studev.groept.be/api/a24pt110/respondFriendRequest/" + requestId + "/" + requestId;
        } else {
            // 拒绝请求，调用 rejectFriendRequest API
            url = "https://studev.groept.be/api/a24pt110/rejectFriendRequest/" + requestId;
        }

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Toast.makeText(context, R.string.request_handled, Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.run();
                },
                error -> Toast.makeText(context, R.string.request_failed, Toast.LENGTH_SHORT).show()
        );
        queue.add(request);
    }

}
