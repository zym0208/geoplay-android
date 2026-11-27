package be.kuleuven.gt.javabean;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
//TODO DECODE STRINGS
public class PlayQuizActivity extends AppCompatActivity {
    private Button btnSubmitAnswer;
    private TextView lblQuizName, lblQuestion;
    private RadioGroup rgpAnswers;
    private String gameName;
    private int gameID, currentQuestion, totalQuestionsExpected, questionsLoaded, correctQuestions;
    private ArrayList<QuizQuestion> quizQuestionArrayList;
    private Location currentLocation;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_play_quiz);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        currentQuestion = 0;
        totalQuestionsExpected = 0;
        questionsLoaded = 0;
        correctQuestions = 0;
        quizQuestionArrayList = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(PlayQuizActivity.this);

        //Receive parameters via intent from previous activity
        double lat = getIntent().getDoubleExtra("latitude", 0);
        double lon = getIntent().getDoubleExtra("longitude", 0);
        gameName = getIntent().getStringExtra("gameName");
        currentLocation = new Location("fromIntent");
        currentLocation.setLatitude(lat);
        currentLocation.setLongitude(lon);


        btnSubmitAnswer = findViewById(R.id.btnSubmitAnswer);
        lblQuizName = findViewById(R.id.lblQuizName);
        lblQuestion = findViewById(R.id.lblQuestionAsked);
        rgpAnswers = findViewById(R.id.rgpAnswers);

        getQuiz();

        btnSubmitAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selectedId = rgpAnswers.getCheckedRadioButtonId();
                if (selectedId == -1) {
                    Toast.makeText(PlayQuizActivity.this, "Please select an answer", Toast.LENGTH_SHORT).show();
                    return;
                }

                RadioButton selectedRadioButton = findViewById(selectedId);
                String selectedAnswer = selectedRadioButton.getText().toString();

                QuizQuestion question = quizQuestionArrayList.get(currentQuestion);
                if (question.isCorrect(selectedAnswer)) {
                    Toast.makeText(PlayQuizActivity.this, "Correct!", Toast.LENGTH_SHORT).show();
                    correctQuestions++;
                } else {
                    Toast.makeText(PlayQuizActivity.this, "Wrong!", Toast.LENGTH_SHORT).show();
                }

                currentQuestion++;
                displayQuestion(currentQuestion);
            }
        });

    }

    private void getQuiz() {

        //get game ID first
        String gameNameEncoded = Uri.encode(gameName.replace(" ", "_"));
        String url = "https://studev.groept.be/api/a24pt110/FindNearbyQuiz/"+gameNameEncoded+"/"+currentLocation.getLongitude()+"/"+currentLocation.getLongitude()+"/"+currentLocation.getLatitude()+"/"+currentLocation.getLatitude();
        Log.d("PlayGame", "Get Quiz ID");
        Log.d("PlayGame", url);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (response.length() == 0) {
                            Toast.makeText(PlayQuizActivity.this, "Error starting game.", Toast.LENGTH_SHORT).show();
                            Log.e("PlayGame", "Couldn't find gameID");
                            Intent intent = new Intent(PlayQuizActivity.this, MapsActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else {
                            try {
                                for (int i = 0; i < response.length(); i++) {
                                    JSONObject obj = response.getJSONObject(i);
                                    gameID = obj.getInt("GameID");
                                    Log.d("PlayGame", "Found game ID as: "+gameID);

                                    //Use game id to get question
                                    getQuestionAndAnswers();


                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(PlayQuizActivity.this, "network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(request);
    }

    private void getQuestionAndAnswers() {
        String url = "https://studev.groept.be/api/a24pt110/GetQuestion/"+gameID;
        Log.d("PlayGame", "Get Question");
        Log.d("PlayGame", url);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (response.length() == 0) {
                            Toast.makeText(PlayQuizActivity.this, "Error starting game.", Toast.LENGTH_SHORT).show();
                            Log.e("PlayGame", "Couldnt find Questions");
                            Intent intent = new Intent(PlayQuizActivity.this, MapsActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else {
                            totalQuestionsExpected = response.length();
                            try {
                                for (int i = 0; i < response.length(); i++) {
                                    JSONObject obj = response.getJSONObject(i);
                                    int questionID = obj.getInt("QuestionID");
                                    String question = obj.getString("Question");
                                    //use question id to get answer list
                                    getAnswers(questionID, question);

                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(PlayQuizActivity.this, "network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(request);
    }

    //Use question ID to get answers from DB, including randomizing order.
    private void getAnswers(int questionID, String question) {
        String url = "https://studev.groept.be/api/a24pt110/GetQuizAnswers/"+questionID;
        Log.d("PlayGame", "Get Answers");
        Log.d("PlayGame", url);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (response.length() == 0) {
                            Toast.makeText(PlayQuizActivity.this, "Error starting game.", Toast.LENGTH_SHORT).show();
                            Log.e("PlayGame", "Couldnt find Answers");
                            Intent intent = new Intent(PlayQuizActivity.this, MapsActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else {
                            try {
                                Log.d("PlayGame", "Adding questions");
                                String correctAnswer = "";
                                ArrayList<String> incorrectAnswers = new ArrayList<>();
                                for (int i = 0; i < response.length(); i++) {
                                    JSONObject obj = response.getJSONObject(i);
                                    String answer = obj.getString("Answer");
                                    int isCorrectInt = obj.getInt("IsCorrect");
                                    boolean isCorrect = (isCorrectInt == 1);

                                    if (isCorrect) {
                                        correctAnswer = answer;
                                    }
                                    else {
                                        incorrectAnswers.add(answer);
                                    }

                                }

                                //Add all questions to array to iterate over for display. Each question knows the correct, incorrect answers.
                                QuizQuestion quizQuestion = new QuizQuestion(question, correctAnswer, incorrectAnswers);
                                quizQuestionArrayList.add(quizQuestion);
                                questionsLoaded++;
                                if (questionsLoaded == totalQuestionsExpected) {
                                    Log.d("PlayGame", "All questions loaded. Displaying first question.");
                                    displayQuestion(currentQuestion);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(PlayQuizActivity.this, "network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(request);
    }

    //Check if quiz is done, otherwise display the next question in the array.
    private void displayQuestion(int index) {
        Log.d("PlayGame", "Question size: "+quizQuestionArrayList.size());
        if (index >= quizQuestionArrayList.size()) {
            Toast.makeText(this, "Quiz completed! Your score is "+correctQuestions+"/"+questionsLoaded, Toast.LENGTH_LONG).show();
            float score = (float) correctQuestions /questionsLoaded;
            uploadScore(score);
            return;
        }

        QuizQuestion question = quizQuestionArrayList.get(index);
        lblQuizName.setText(question.getQuestion());
        lblQuestion.setText(question.getQuestion());

        rgpAnswers.removeAllViews(); // clear previous answers

        ArrayList<String> answers = question.getRandomizedAnswers();
        for (int i = 0; i < answers.size(); i++) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(answers.get(i));
            radioButton.setId(View.generateViewId());
            rgpAnswers.addView(radioButton);
        }
    }
    private void uploadScore(float score) {
        try {
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String username = prefs.getString("Username", "");
            String url = "https://studev.groept.be/api/a24pt110/AddQuizScore/" + username + "/" + gameID + "/" + score*100;
            Log.d("UploadScore", "Uploading race score");
            Log.d("UploadScore", url);

            StringRequest request = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Toast.makeText(PlayQuizActivity.this, "Score upload success！", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(PlayQuizActivity.this, MapsActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(PlayQuizActivity.this, "Score upload faled: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(PlayQuizActivity.this, MapsActivity.class);
                    startActivity(intent);
                    finish();
                }
            });

            requestQueue.add(request);


        } catch (Exception e) {
            Log.e("UploadScore",e.toString());
            Intent intent = new Intent(PlayQuizActivity.this, MapsActivity.class);
            startActivity(intent);
            finish();

        }
    }


}