package be.kuleuven.gt.javabean;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ImageUploader {

    // 上传图片接口（使用 POST）
    private static final String UPLOAD_IMAGE_URL = "https://studev.groept.be/api/a24pt110/insertImage";

    // 获取图片接口（使用 GET）
    private static final String GET_IMAGE_URL = "https://studev.groept.be/api/a24pt110/getLastImage";

    // 上传回调接口


    // 上传图片到服务器
    // 上传图片到服务器（使用 Email 替代 UserID）
    public static void uploadImageToServer(Context context, Uri imageUri, String imageName, String userEmail, UploadCallback callback) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            byte[] imageBytes = readBytes(inputStream);
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            RequestQueue queue = Volley.newRequestQueue(context);

            StringRequest request = new StringRequest(Request.Method.POST, UPLOAD_IMAGE_URL,
                    response -> callback.onComplete(true),
                    error -> {
                        error.printStackTrace();
                        callback.onComplete(false);
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("name", imageName);
                    params.put("imagedata", base64Image);
                    params.put("email", userEmail);
                    return params;
                }
            };

            queue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
            callback.onComplete(false);
        }
    }


    // 从服务器获取最新图片
// 从服务器获取指定邮箱用户的头像
    public static void getImageFromServer(Context context, String email, DownloadCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);

        // 拼接 URL + Email 参数（因为 JsonArrayRequest 不支持 POST 带 body）
        String urlWithParam = GET_IMAGE_URL + "/" + email;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, urlWithParam, null,
                response -> {
                    try {
                        if (response.length() > 0) {
                            JSONObject imageObj = response.getJSONObject(0);
                            String base64Image = imageObj.getString("imageString");
                            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            callback.onImageDownloaded(bitmap);
                        } else {
                            // 没有数据
                            callback.onImageDownloaded(null);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onImageDownloaded(null);
                    }
                },
                error -> {
                    error.printStackTrace();
                    callback.onImageDownloaded(null);
                });

        queue.add(request);
    }


    // 工具：读取 InputStream 为 byte[]
    private static byte[] readBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        return byteBuffer.toByteArray();
    }}