package be.kuleuven.gt.javabean;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

public class GPSActivity extends AppCompatActivity {
    public static final int DEFAULT_UPDATE_INTERVAL = 3000;
    public static final int FAST_UPDATE_INTERVAL = 1000;
    private static final int PERMISSIONS_FINE_LOCATION = 99;
    private TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, tv_wayPointCount;

    private Button btnBack, btn_newWayPoint, btn_showWayPointList, btn_showMap;
    private Switch sw_locationsupdates, sw_gps;

    Location currentLocation;

    List<Location> savedLocations;

    // Google's API for location services
    FusedLocationProviderClient fusedLocationProviderClient;

    //Location request is a config file for all settings related to FusedLocationProviderClient
    LocationRequest locationRequest;

    LocationCallback locationCallBack;

    @Override
    protected void onCreate(Bundle savedInstancState) {
        super.onCreate(savedInstancState);
        setContentView(R.layout.activity_gps);

        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);
        tv_wayPointCount = findViewById(R.id.tv_countOfBreadCrumbs);
        sw_locationsupdates = findViewById(R.id.sw_locationsupdates);
        sw_gps = findViewById(R.id.sw_gps);
        btnBack = findViewById(R.id.btnBack);
        btn_newWayPoint = findViewById(R.id.btn_newWayPoint);
        btn_showWayPointList = findViewById(R.id.btn_showWayPointList);
        btn_showMap = findViewById(R.id.btn_showMaps);

        // set all properties of LocationRequest
        locationRequest = new LocationRequest();
        locationRequest.setInterval(DEFAULT_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FAST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        //triggered whenever update interval is met.
        locationCallBack = new LocationCallback() {

            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //save location
                updateUIValues(locationResult.getLastLocation());
            }
        };

        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_gps.isChecked()) {
                    //most accurate - use gps
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Using GPS sensors");
                }
                else {
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Using Towers+Wifi");
                }

            }

        });
        sw_locationsupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_locationsupdates.isChecked()) {
                    //turn on location
                    startLocationUpdates();
                }
                else {
                    //turn off 
                    stopLocationUpdates();
                }
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(GPSActivity.this, MainActivity.class));
            }
        });
        btn_newWayPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            //get gps location
                // add to list
                GPSLocations gpsLocations = (GPSLocations)getApplicationContext();
                savedLocations = gpsLocations.getMyLocations();
                savedLocations.add(currentLocation);
                gpsLocations.setMyLocations(savedLocations);
            }
        });
        btn_showWayPointList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(GPSActivity.this, ShowSavedLocationsList.class));
            }
        });

        btn_showMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(GPSActivity.this, MapsActivity.class));
            }
        });

        updateGPS();


    } // end on create

    private void stopLocationUpdates() {
        tv_updates.setText("Location is no longer being tracked");
        tv_lat.setText("Not tracking location");
        tv_lon.setText("Not tracking location");
        tv_speed.setText("Not tracking location");
        tv_address.setText("Not tracking location");
        tv_accuracy.setText("Not tracking location");
        tv_altitude.setText("Not tracking location");
        tv_sensor.setText("Not tracking location");

        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
    }

    private void startLocationUpdates() {
        tv_updates.setText("Location is being tracked");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        updateGPS();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateGPS();
                }
                else {
                    Toast.makeText(this, "This app requires permission to be granted to work properly", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }


    private void updateGPS() {
        //get permissions to track GPS
        //get current location from fused client
        //update UI

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(GPSActivity.this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        //permissions granted
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    //we have permissions. put the values of location into ui components
                    if (location != null) {
                        updateUIValues(location);
                        currentLocation = location;
                        GPSLocations gpsLocations = (GPSLocations)getApplicationContext();
                        gpsLocations.setCurrLocation(location);
                    } else {
                        Toast.makeText(GPSActivity.this, "Location not available", Toast.LENGTH_SHORT).show();
                        Log.w("GPS", "Location object is null.");
                    }



                }
            });
        }
        else {
            //no permissions yet
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }
    }

    private void updateUIValues(Location location) {

        if (location != null) {
            tv_lat.setText(String.valueOf(location.getLatitude()));
            tv_lon.setText(String.valueOf(location.getLongitude()));
            tv_accuracy.setText(String.valueOf(location.getAccuracy()));

            if (location.hasAltitude()) {
                tv_altitude.setText(String.valueOf(location.getAltitude()));
            }
            else {
                tv_altitude.setText("Not available");
            }

            if (location.hasSpeed()) {
                tv_speed.setText(String.valueOf(location.getSpeed()));
            }
            else {
                tv_speed.setText("Not available");
            }

            Geocoder geocoder = new Geocoder(GPSActivity.this);
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                tv_address.setText(addresses.get(0).getAddressLine(0));
            }
            catch(Exception e){
                tv_address.setText("Unable to get street address");

            }

            GPSLocations gpsLocations = (GPSLocations)getApplicationContext();
            savedLocations = gpsLocations.getMyLocations();

            //show no. of waypoints.
            tv_wayPointCount.setText(Integer.toString(savedLocations.size()));

        }
    }

}
