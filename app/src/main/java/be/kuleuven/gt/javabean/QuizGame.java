package be.kuleuven.gt.javabean;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class QuizGame extends Game {
    private ArrayList<QuizQuestion> questionArrayList;
    public QuizGame(Context context, Location startLocation) {
        super(context, startLocation);
        this.context = context;
        questionArrayList = new ArrayList<>();
    }

    @Override
    public void createGame(String username) {

        requestQueue = Volley.newRequestQueue(context);
        LatLng latLng = new LatLng(startLocation.getLatitude(), startLocation.getLongitude());
        String gameNameEncoded = Uri.encode(gameName.replace(" ", "_"));
        String url = "https://studev.groept.be/api/a24pt110/createGame/Quiz/" +
                latLng.latitude + "/" + latLng.longitude + "/" + gameNameEncoded+"/"+username;
        Log.d("CreateGame", "Start Lat = "+latLng.latitude);
        Log.d("CreateGame", "Start Lon = "+latLng.longitude);

        Log.d("CreateGame", url);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(context, "Game created", Toast.LENGTH_SHORT).show();
                        fetchGameIDAndCreateQuestions(); // <- Call after confirmation
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, "Game creation failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        requestQueue.add(request);

        gameID = getGameID(gameName);
    }


    private void fetchGameIDAndCreateQuestions() {
        String gameNameEncoded = Uri.encode(gameName.replace(" ", "_"));
        String url = "https://studev.groept.be/api/a24pt110/getGameID/" + gameNameEncoded;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            if (response.length() > 0) {
                                JSONObject obj = response.getJSONObject(0);
                                gameID = obj.getInt("GameID");
                                Log.d("CreateGame", "Game ID: " + gameID);
                                createQuizQuestions();
                            }
                        } catch (JSONException e) {
                            Log.e("CreateGame", "JSON parse error", e);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, "Failed to get Game ID: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        requestQueue.add(request);
    }

    private void createQuizQuestions() {
        for (QuizQuestion question : questionArrayList) {
            String questionEncoded = Uri.encode(question.getQuestion().replace(" ", "_"));
            String questionUrl = "https://studev.groept.be/api/a24pt110/createQuizQuestion/" +
                    gameID + "/" + questionEncoded;

            StringRequest questionRequest = new StringRequest(Request.Method.GET, questionUrl,
                    response -> {
                        Toast.makeText(context, "Quiz question created!", Toast.LENGTH_SHORT).show();
                        // After question created, get its ID and create answers
                        fetchQuestionIDAndCreateAnswers(question);
                    },
                    error -> Toast.makeText(context, "Quiz question creation failed: " + error.getMessage(), Toast.LENGTH_SHORT).show()
            );

            requestQueue.add(questionRequest);
        }
    }

    private void fetchQuestionIDAndCreateAnswers(QuizQuestion question) {
        String questionEncoded = Uri.encode(question.getQuestion().replace(" ", "_"));
        String url = "https://studev.groept.be/api/a24pt110/getQuizQuestionID/" + gameID + "/" + questionEncoded;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.length() > 0) {
                            int questionID = response.getJSONObject(0).getInt("QuestionID");
                            createAnswersForQuestion(question, questionID);
                        }
                    } catch (JSONException e) {
                        Log.e("CreateAnswers", "JSON parse error", e);
                    }
                },
                error -> Toast.makeText(context, "Failed to get Question ID: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }


    private void createAnswersForQuestion(QuizQuestion question, int questionID) {
        // First add correct answer
        String correct = Uri.encode(question.getCorrectAnswer().replace(" ", "_"));
        String correctUrl = "https://studev.groept.be/api/a24pt110/createQuizAnswer/" +
                questionID + "/" + correct + "/1"; // 1 = correct

        requestQueue.add(new StringRequest(Request.Method.GET, correctUrl,
                response -> {},
                error -> Toast.makeText(context, "Failed to add correct answer", Toast.LENGTH_SHORT).show()
        ));

        // Then add incorrect answers
        for (String wrong : question.getIncorrectAnswers()) {
            String wrongEncoded = Uri.encode(wrong.replace(" ", "_"));
            String wrongUrl = "https://studev.groept.be/api/a24pt110/createQuizAnswer/" +
                    questionID + "/" + wrongEncoded + "/0"; // 0 = incorrect

            requestQueue.add(new StringRequest(Request.Method.GET, wrongUrl,
                    response -> {},
                    error -> Toast.makeText(context, "Failed to add wrong answer", Toast.LENGTH_SHORT).show()
            ));
        }
    }


    public boolean undoQuestion() {
        if (!questionArrayList.isEmpty()) {
            questionArrayList.remove(questionArrayList.size()-1);
            return true;
        }
        else {
            return false;
        }
    }

    public void addQuestion(QuizQuestion newQuestion) {
        questionArrayList.add(newQuestion);
    }


    public int getQuizLength() {
        return questionArrayList.size();
    }

    @Override
    public void setGameName(String newName) {
        gameName = newName;
    }

    @Override
    public int getGameID(String gameName) {
        return 0;
    }
}
