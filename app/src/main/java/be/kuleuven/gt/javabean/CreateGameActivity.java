package be.kuleuven.gt.javabean;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Arrays;
import java.util.List;

public class CreateGameActivity extends AppCompatActivity {
    private Button btnBack, btnConfirmGame;
    private Spinner spnChooseGame;

    private String selectedGame;
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_game);
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

        btnBack = findViewById(R.id.btnBackCreateGame);
        btnConfirmGame = findViewById(R.id.btnChooseGame);

        spnChooseGame = findViewById(R.id.spnSelectGame);
        List<String> items = Arrays.asList("Race", "Quiz");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnChooseGame.setAdapter(adapter);

        spnChooseGame.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGame = parent.getItemAtPosition(position).toString();
                Toast.makeText(getApplicationContext(), "Selected: " + selectedGame, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nothing here?
            }


        });


        btnConfirmGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedGame.equals("Race")) {
                    Intent intent = new Intent(CreateGameActivity.this, RaceCreationActivity.class);
                    intent.putExtra("latitude", currentLocation.getLatitude());
                    intent.putExtra("longitude", currentLocation.getLongitude());
                    startActivity(intent);
                    finish();
                }
                else if (selectedGame.equals("Quiz")) {
                    Intent intent = new Intent(CreateGameActivity.this, QuizCreationActivity.class);
                    intent.putExtra("latitude", currentLocation.getLatitude());
                    intent.putExtra("longitude", currentLocation.getLongitude());
                    startActivity(intent);
                    finish();
                }


            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CreateGameActivity.this, MapsActivity.class);
                startActivity(intent);
                finish();

            }
        });




    }
}