package be.kuleuven.gt.javabean;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class UITranslationHelper {

    // 遍历所有控件并翻译文本，包括 TextView、Button、EditText
    public static void translateUI(View rootView, String targetLanguage) {
        traverseViews(rootView, targetLanguage);
    }

    private static void traverseViews(View view, String targetLanguage) {
        if (view == null) return;

        if (view instanceof TextView && !(view instanceof Button) && !(view instanceof EditText)) {
            TextView textView = (TextView) view;
            translateAndSetText(textView.getContext(), textView.getText().toString(), translated -> textView.setText(translated), targetLanguage);
        } else if (view instanceof Button) {
            Button button = (Button) view;
            translateAndSetText(button.getContext(), button.getText().toString(), translated -> button.setText(translated), targetLanguage);
        } else if (view instanceof EditText) {
            EditText editText = (EditText) view;
            translateAndSetText(editText.getContext(), editText.getHint().toString(), translated -> editText.setHint(translated), targetLanguage);
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                traverseViews(group.getChildAt(i), targetLanguage);
            }
        }
    }

    // 调用 Microsoft Translator API 并执行回调
    private static void translateAndSetText(Context context, String originalText, TextCallback callback, String targetLanguage) {
        final String API_KEY = "REPLACE_WITH_LOCAL_SECRET";
        final String REGION = "West Europe";
        final String ENDPOINT = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&to=" + targetLanguage;

        JSONArray bodyArray = new JSONArray();
        JSONObject textObj = new JSONObject();
        try {
            textObj.put("Text", originalText);
            bodyArray.put(textObj);
        } catch (JSONException e) {
            callback.onTranslated(originalText);
            return;
        }

        JsonRequest<JSONArray> request = new JsonRequest<JSONArray>(
                Request.Method.POST,
                ENDPOINT,
                bodyArray.toString(),
                response -> {
                    try {
                        // 微软翻译返回是JSONArray
                        JSONArray resultArray = response;
                        JSONObject firstObj = resultArray.getJSONObject(0);
                        JSONArray translations = firstObj.getJSONArray("translations");
                        String translated = translations.getJSONObject(0).getString("text");
                        callback.onTranslated(translated);
                    } catch (JSONException e) {
                        callback.onTranslated(originalText);
                    }
                },
                error -> callback.onTranslated(originalText)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Ocp-Apim-Subscription-Key", API_KEY);
                headers.put("Ocp-Apim-Subscription-Region", REGION);
                headers.put("Content-Type", "application/json");
                return headers;
            }

            @Override
            protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
                try {
                    String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                    return Response.success(new JSONArray(jsonString), HttpHeaderParser.parseCacheHeaders(response));
                } catch (UnsupportedEncodingException | JSONException e) {
                    return Response.error(new ParseError(e));
                }
            }
        };

        Volley.newRequestQueue(context).add(request);
    }

    private interface TextCallback {
        void onTranslated(String translatedText);
    }
}
