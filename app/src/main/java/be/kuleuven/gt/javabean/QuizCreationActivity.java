package be.kuleuven.gt.javabean;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.toolbox.Volley;

import java.util.ArrayList;

public class QuizCreationActivity extends AppCompatActivity {
    private Button btnBack, btnAddQuestion, btnCreateQuiz, btnUndo;
    private EditText edtQuestion, edtCorrectAnswer, edtIncorrectAnswer1, edtIncorrectAnswer2, edtIncorrectAnswer3, edtName;
    private QuizGame quizGame;
    private TextView lblQuestionCounter;
    private int questionTotal;
    private Location currentLocation;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_creation);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        double lat = getIntent().getDoubleExtra("latitude", 0);
        double lon = getIntent().getDoubleExtra("longitude", 0);
        currentLocation = new Location("fromIntent");
        currentLocation.setLatitude(lat);
        currentLocation.setLongitude(lon);


        edtQuestion = findViewById(R.id.edtQuestion);
        edtCorrectAnswer = findViewById(R.id.edtCorrectAnswer);
        edtIncorrectAnswer1 = findViewById(R.id.edtAnswer1);
        edtIncorrectAnswer2 = findViewById(R.id.edtAnswer2);
        edtIncorrectAnswer3 = findViewById(R.id.edtAnswer3);
        edtName = findViewById(R.id.edtQuizName);

        lblQuestionCounter = findViewById(R.id.lblQuestionCounter);

        btnAddQuestion = findViewById(R.id.btnAddQuestion);
        btnBack = findViewById(R.id.btnBackQuestion);
        btnUndo = findViewById(R.id.btnUndoQuestion);
        btnCreateQuiz = findViewById(R.id.btnCreateQuiz);

        quizGame = new QuizGame(QuizCreationActivity.this, currentLocation);
        questionTotal = 0;


        btnAddQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String question = edtQuestion.getText().toString().trim();
                String answer = edtCorrectAnswer.getText().toString().trim();
                String wrong1 = edtIncorrectAnswer1.getText().toString().trim();
                String wrong2 = edtIncorrectAnswer2.getText().toString().trim();
                String wrong3 = edtIncorrectAnswer3.getText().toString().trim();
                if (question.isEmpty() || answer.isEmpty() || wrong1.isEmpty() || wrong2.isEmpty() || wrong3.isEmpty()) {
                    Toast.makeText(QuizCreationActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                }
                else {
                    ArrayList<String> incorrectAnswers = new ArrayList<>();
                    incorrectAnswers.add(wrong1);
                    incorrectAnswers.add(wrong2);
                    incorrectAnswers.add(wrong3);
                    QuizQuestion quizQuestion = new QuizQuestion(question, answer, incorrectAnswers);
                    quizGame.addQuestion(quizQuestion);
                    edtQuestion.setText("");
                    edtCorrectAnswer.setText("");
                    edtIncorrectAnswer1.setText("");
                    edtIncorrectAnswer2.setText("");
                    edtIncorrectAnswer3.setText("");
                    Log.d("QuizCreate", "Question added");
                    Toast.makeText(QuizCreationActivity.this, "Question Added", Toast.LENGTH_SHORT).show();
                    questionTotal +=1;
                    String label = "Total Questions: "+questionTotal;
                    lblQuestionCounter.setText(label);
                }

            }
        });

        btnCreateQuiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String gameName = edtName.getText().toString().trim();
                if (gameName.isEmpty()) {
                    Toast.makeText(QuizCreationActivity.this, "Please set a name", Toast.LENGTH_SHORT).show();
                    return;
                }
                quizGame.setGameName(gameName);
                if (quizGame.getQuizLength() < 1) {
                    Toast.makeText(QuizCreationActivity.this, "Please add questions first", Toast.LENGTH_SHORT).show();
                    return;
                }
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                String username = prefs.getString("Username", "");
                quizGame.createGame(username);
                Intent intent = new Intent(QuizCreationActivity.this, MapsActivity.class);
                startActivity(intent);
                finish();

            }
        });

        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!quizGame.undoQuestion()) {
                    Toast.makeText(QuizCreationActivity.this, "No questions to undo", Toast.LENGTH_SHORT).show();
                }
                else {
                    questionTotal -=1;
                    String label = "Total Questions: "+questionTotal;
                    lblQuestionCounter.setText(label);
                }

            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(QuizCreationActivity.this, CreateGameActivity.class);
                intent.putExtra("latitude", currentLocation.getLatitude());
                intent.putExtra("longitude", currentLocation.getLongitude());
                startActivity(intent);
                finish();

            }
        });


    }
}