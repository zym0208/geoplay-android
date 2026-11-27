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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import be.kuleuven.gt.javabean.databinding.ActivityMapsBinding;
import be.kuleuven.gt.javabean.databinding.ActivityPlayRaceBinding;
import be.kuleuven.gt.javabean.databinding.ActivityRaceCreationBinding;

public class PlayRaceActivity extends FragmentActivity implements OnMapReadyCallback {
//TODO Timer. progress bar. back button. race end screen?
    public static final float DistanceThreshold = 5.0f;
    private GoogleMap mMap;
    private TextView lblTimer;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private int currentCheckpointIndex = 0;
    private long raceStartTimeMillis,raceEndTimeMillis;
    private boolean raceStarted = false;
    public static final int DEFAULT_UPDATE_INTERVAL = 1000;
    public static final int FAST_UPDATE_INTERVAL = 500;
    private static final int PERMISSIONS_FINE_LOCATION = 99;

    private RequestQueue requestQueue;

    private ArrayList<Marker> checkpointMarkers;
    private ArrayList<Location> checkpointLocations;
    Location currentLocation;
    List<Location> savedLocations;

    LocationRequest locationRequest;
    private ActivityPlayRaceBinding binding;
    private String gameName;
    private int gameID;


    private Marker userMarker; //Arrow showing user location/bearing

    private FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayRaceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestQueue = Volley.newRequestQueue(PlayRaceActivity.this);

        //Receive parameters via intent from previous activity
        double lat = getIntent().getDoubleExtra("latitude", 0);
        double lon = getIntent().getDoubleExtra("longitude", 0);
        gameName = getIntent().getStringExtra("gameName");
        currentLocation = new Location("fromIntent");
        currentLocation.setLatitude(lat);
        currentLocation.setLongitude(lon);

        lblTimer = findViewById(R.id.lblTimer);

        Log.d("GameCreate", "Lat start from Activity:"+currentLocation.getLatitude());
        Log.d("GameCreate", "Lon start from Activity:"+currentLocation.getLongitude());

        locationRequest = new LocationRequest();
        locationRequest.setInterval(DEFAULT_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FAST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);




        checkpointMarkers = new ArrayList<>();
        checkpointLocations = new ArrayList<>();









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

                        //Checkpoint logic
                        for (int i = 0; i < checkpointLocations.size(); i++) {
                            Location checkpoint = checkpointLocations.get(i);
                            float distance = location.distanceTo(checkpoint);

                            if (distance <= DistanceThreshold) {
                                currentCheckpointIndex += 1;
                                Toast.makeText(getApplicationContext(), "You reached checkpoint " + (currentCheckpointIndex), Toast.LENGTH_SHORT).show();

                                // Remove checkpoint and marker
                                checkpointLocations.remove(i);
                                checkpointMarkers.get(i).remove();
                                checkpointMarkers.remove(i);

                                // Check if race is finished
                                if (checkpointLocations.isEmpty()) {
                                    raceEndTimeMillis = System.currentTimeMillis();
                                    long elapsedMillis = raceEndTimeMillis - raceStartTimeMillis;
                                    long seconds = elapsedMillis / 1000;
                                    timerHandler.removeCallbacks(timerRunnable);

                                    Toast.makeText(PlayRaceActivity.this, "Race complete! Time: " + seconds + "s", Toast.LENGTH_LONG).show();

                                    // You can also log it or pass it to a score screen
                                    Log.d("PlayGame", "Race completed in " + seconds + " seconds");
                                    uploadScore(seconds);


                                }
                                break; // Prevents loop from continuing on modified list
                            }
                        }


                        //update camera to current location
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

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(PlayRaceActivity.this);









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


    }

    private void uploadScore(long seconds) {
        try {
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String username = prefs.getString("Username", ""); // -1 = default if not found
            String url = "https://studev.groept.be/api/a24pt110/AddRaceScore/" + username + "/" + gameID + "/" + seconds;
            Log.d("UploadScore", "Uploading race score");
            Log.d("UploadScore", url);

            StringRequest request = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Toast.makeText(PlayRaceActivity.this, "Score upload success！", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(PlayRaceActivity.this, MapsActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(PlayRaceActivity.this, "Score upload faled: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            requestQueue.add(request);


        } catch (Exception e) {
            Log.e("UploadScore",e.toString());
            Intent intent = new Intent(PlayRaceActivity.this, MapsActivity.class);
            startActivity(intent);
            finish();

        }
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
        setupGame();

    }
    private void updateGPS() {
        //get permissions to track GPS
        //get current location from fused client
        //update UI

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(PlayRaceActivity.this);

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
                        Toast.makeText(PlayRaceActivity.this, "Location not available", Toast.LENGTH_SHORT).show();
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


    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        updateGPS();

    }



    private void setupGame() {
        //First get the game ID
        String gameNameEncoded = Uri.encode(gameName.replace(" ", "_"));
        //String url = "https://studev.groept.be/api/a24pt110/getLogin/"+gameNameEncoded+"/"+currentLocation.getLatitude()+"/"+currentLocation.getLatitude()+"/"+currentLocation.getLongitude()+"/"+currentLocation.getLongitude();
        String url = "https://studev.groept.be/api/a24pt110/FindNearbyRace/"+gameNameEncoded+"/"+currentLocation.getLongitude()+"/"+currentLocation.getLongitude()+"/"+currentLocation.getLatitude()+"/"+currentLocation.getLatitude();
        Log.d("PlayGame", "Get Race ID");
        Log.d("PlayGame", url);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (response.length() == 0) {
                            Toast.makeText(PlayRaceActivity.this, "Error starting game.", Toast.LENGTH_SHORT).show();
                            Log.e("PlayGame", "Couldnt find gameID");
                            Intent intent = new Intent(PlayRaceActivity.this, MapsActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else {
                            try {
                                for (int i = 0; i < response.length(); i++) {
                                    JSONObject obj = response.getJSONObject(i);
                                    gameID = obj.getInt("GameID");
                                    Log.d("PlayGame", "Found game ID as: "+gameID);
                                    getCheckpoints();


                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(PlayRaceActivity.this, "network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(request);
    }

    private void getCheckpoints() {
        String url = "https://studev.groept.be/api/a24pt110/GetRaceCheckpoints/"+gameID;
        Log.d("PlayGame", "Get Checkpoints");
        Log.d("PlayGame", url);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (response.length() == 0) {
                            Toast.makeText(PlayRaceActivity.this, "Error starting game.", Toast.LENGTH_SHORT).show();
                            Log.e("PlayGame", "Couldnt find checkpoints");
                            Intent intent = new Intent(PlayRaceActivity.this, MapsActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else {
                            try {
                                for (int i = 0; i < response.length(); i++) {
                                    JSONObject obj = response.getJSONObject(i);
                                    LatLng latLng = new LatLng(obj.getDouble("Latitude"), obj.getDouble("Longitude"));
                                    Log.d("PlayGame", "Creating marker at: "+obj.getDouble("Longitude")+ obj.getDouble("Latitude"));

                                    MarkerOptions markerOptions = new MarkerOptions()
                                            .position(latLng)
                                            .title("Checkpoint " + (i + 1));

                                    Marker marker = mMap.addMarker(markerOptions);
                                    Location checkpointLocation = new Location("Checkpoint");
                                    checkpointLocation.setLatitude(latLng.latitude);
                                    checkpointLocation.setLongitude(latLng.longitude);
                                    checkpointLocations.add(checkpointLocation);

                                    checkpointMarkers.add(marker);

                                }
                                raceStartTimeMillis = System.currentTimeMillis();
                                raceStarted = true;

                                timerRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        long elapsedMillis = System.currentTimeMillis() - raceStartTimeMillis;
                                        int seconds = (int) (elapsedMillis / 1000);
                                        int minutes = seconds / 60;
                                        seconds = seconds % 60;

                                        lblTimer.setText(String.format("%02d:%02d", minutes, seconds));
                                        timerHandler.postDelayed(this, 1000);
                                    }
                                };
                                timerHandler.post(timerRunnable);
                                Log.d("PlayGame", "Race started at " + raceStartTimeMillis);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(PlayRaceActivity.this, "network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(request);


    }

}