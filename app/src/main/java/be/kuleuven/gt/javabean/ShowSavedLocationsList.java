package be.kuleuven.gt.javabean;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public class ShowSavedLocationsList extends AppCompatActivity {
    private Button btn_back;
    private ListView lv_savedLocations;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_saved_locations_list);
        btn_back = findViewById(R.id.btn_back);
        lv_savedLocations = findViewById(R.id.lv_wayPoints);


        GPSLocations gpsLocations = (GPSLocations)getApplicationContext();
        List<Location> savedLocations = gpsLocations.getMyLocations();

        lv_savedLocations.setAdapter(new ArrayAdapter<Location>(ShowSavedLocationsList.this, android.R.layout.simple_list_item_1, savedLocations));

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ShowSavedLocationsList.this, GPSActivity.class));
            }
        });

    }
}