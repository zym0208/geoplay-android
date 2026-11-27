package be.kuleuven.gt.javabean;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

import be.kuleuven.gt.javabean.databinding.ActivityMapsBinding;
import be.kuleuven.gt.javabean.databinding.ActivityRaceCreationBinding;

public class RaceCreationActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private ImageButton imgbtn_backRace;
    private Button btn_create_checkpoint, btn_undo_checkpoint, btn_create_game;
    private EditText edit_game_name;
    public static final int DEFAULT_UPDATE_INTERVAL = 1000;
    public static final int FAST_UPDATE_INTERVAL = 500;
    private static final int PERMISSIONS_FINE_LOCATION = 99;

    private ArrayList<Marker> checkpointMarkers;


    private RaceGame createRace;
    Location currentLocation;
    List<Location> savedLocations;

    LocationRequest locationRequest;
    private ActivityRaceCreationBinding binding;


    private Marker userMarker; //Arrow showing user location/bearing

    private FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRaceCreationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        double lat = getIntent().getDoubleExtra("latitude", 0);
        double lon = getIntent().getDoubleExtra("longitude", 0);
        currentLocation = new Location("fromIntent");
        currentLocation.setLatitude(lat);
        currentLocation.setLongitude(lon);
        Log.d("GameCreate", "Lat start from Activity:"+currentLocation.getLatitude());
        Log.d("GameCreate", "Lon start from Activity:"+currentLocation.getLongitude());

        locationRequest = new LocationRequest();
        locationRequest.setInterval(DEFAULT_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FAST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        imgbtn_backRace = findViewById(R.id.imgbtnBackRace);
        btn_create_game = findViewById(R.id.btn_create_game);
        btn_create_checkpoint = findViewById(R.id.btn_create_checkpoint);
        btn_undo_checkpoint = findViewById(R.id.btn_undo_cp);
        edit_game_name = findViewById(R.id.edit_game_name);

        createRace = new RaceGame(RaceCreationActivity.this, currentLocation);
        checkpointMarkers = new ArrayList<>();


        imgbtn_backRace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(RaceCreationActivity.this, MapsActivity.class);
                startActivity(intent);
                finish();
            }
        });
        btn_create_checkpoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createRace.addCheckpoint(currentLocation);
                LatLng cpLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                Marker marker = mMap.addMarker(new MarkerOptions().position(cpLatLng).title("CheckPoint" + createRace.getLength()));
                checkpointMarkers.add(marker);

            }
        });
        btn_create_game.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = edit_game_name.getText().toString().trim();
                createRace.setGameName(name);
                if (createRace.getGameName().isEmpty() || createRace.getLength() < 2) {
                    Toast.makeText(RaceCreationActivity.this, "Please ensure name is set and you have checkpoints", Toast.LENGTH_LONG).show();
                    return;
                }
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                String username = prefs.getString("Username", "");
                createRace.createGame(username);
                Intent intent = new Intent(RaceCreationActivity.this, MapsActivity.class);
                startActivity(intent);
                finish();
            }
        });
        btn_undo_checkpoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("GameCreate", "Current checkpoint number:"+checkpointMarkers.size());
                createRace.removeLastCP();
                if (checkpointMarkers.size() < 1) {

                    Toast.makeText(RaceCreationActivity.this, "No Checkpoints to remove", Toast.LENGTH_SHORT).show();
                    return;
                }
                checkpointMarkers.get(checkpointMarkers.size()-1).remove();  // Removes it from the map
                checkpointMarkers.remove(checkpointMarkers.size()-1);        // Removes it from your list

            }
        });

        //triggered when update interval is met
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();

                if (location != null) {
                    currentLocation = location;
                    GPSLocations gpsLocations = (GPSLocations) getApplicationContext();
                    gpsLocations.setCurrLocation(location);

                    if (mMap != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        float bearing = location.getBearing();

                        if (userMarker == null) {
                            // First time: create the marker
                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(latLng)
                                    .anchor(0.5f, 0.5f)
                                    .rotation(bearing)
                                    .flat(true)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow));
                            userMarker = mMap.addMarker(markerOptions);
                        } else {
                            // Update marker position and rotation
                            userMarker.setPosition(latLng);
                            userMarker.setRotation(bearing);
                        }

                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(latLng)
                                .zoom(19)
                                .tilt(30)
                                .bearing(bearing)
                                .build();
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    } else {
                        Log.w("RaceCreationActivity", "Map not ready yet, skipping marker update.");
                    }
                }
            }
        };

        updateGPS();
        startLocationUpdates();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(RaceCreationActivity.this);





        binding = ActivityRaceCreationBinding.inflate(getLayoutInflater());



        // Create GoogleMapOptions
        GoogleMapOptions options = new GoogleMapOptions()
                .mapType(GoogleMap.MAP_TYPE_HYBRID) // for example
                .compassEnabled(true)
                .zoomControlsEnabled(false)
                .scrollGesturesEnabled(false);


        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.map_fragment, mapFragment)
                .commit();


        mapFragment.getMapAsync(this);

//        GPSLocations gpsLocations = (GPSLocations) getApplicationContext();
//        savedLocations = gpsLocations.getMyLocations();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        try {
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.no_poi_style));

            if (!success) {
                Log.e("MAP_STYLE", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("MAP_STYLE", "Can't find style. Error: ", e);
        }
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);


        //Example code for adding marker
        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //  mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        // mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));



        GPSLocations gpsLocations = (GPSLocations)getApplicationContext();
        savedLocations = gpsLocations.getMyLocations();

        for (Location location: savedLocations) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Lat:"+location.getLatitude()+" Lon:"+location.getLongitude());
            mMap.addMarker(markerOptions);
        }

        gpsLocations.setCurrLocation(currentLocation);
        Location curLocation = gpsLocations.getCurrLocation();
        if (currentLocation !=null) {
            LatLng currLatLng = new LatLng(curLocation.getLatitude(), curLocation.getLongitude());


            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(currLatLng)
                    .zoom(19)
                    .tilt(30) // 30-degree tilt
                    .build();


            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }   else {
            Toast.makeText(this, "Current location not available yet", Toast.LENGTH_SHORT).show();
            Log.w("MAP", "Current location is null.");
        }

    }
    private void updateGPS() {
        //get permissions to track GPS
        //get current location from fused client
        //update UI

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(RaceCreationActivity.this);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //permissions granted
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    //we have permissions. put the values of location into ui components
                    if (location != null) {
                        //updateSavedLocations(location);
                        currentLocation = location;
                        GPSLocations gpsLocations = (GPSLocations)getApplicationContext();
                        gpsLocations.setCurrLocation(location);
                    } else {
                        Toast.makeText(RaceCreationActivity.this, "Location not available", Toast.LENGTH_SHORT).show();
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


    private void updateSavedLocations(Location location) {

        if (location != null) {

            GPSLocations gpsLocations = (GPSLocations)getApplicationContext();
            savedLocations = gpsLocations.getMyLocations();


        }
    }
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        updateGPS();

    }
}