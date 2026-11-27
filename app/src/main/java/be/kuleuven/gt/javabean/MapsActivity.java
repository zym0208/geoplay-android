package be.kuleuven.gt.javabean;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
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

import java.util.HashMap;
import java.util.List;

import be.kuleuven.gt.javabean.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private ImageButton imgbtn_back, imgbtn_newGame;
    private TextView tvGameName, tvGameType;
    private Button btnPlay;
    private Marker selectedMarker;
    private View customInfoView;
    public static final int DEFAULT_UPDATE_INTERVAL = 1000;
    public static final int FAST_UPDATE_INTERVAL = 500;
    private static final int PERMISSIONS_FINE_LOCATION = 99;
    Location currentLocation;
    List<Location> savedLocations;

    private RequestQueue requestQueue;

    LocationRequest locationRequest;
    private ActivityMapsBinding binding;
    private final HashMap<String, Marker> gameMarkers = new HashMap<>();

    private Marker userMarker; //Arrow showing user location/bearing

    private FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imgbtn_back = findViewById(R.id.imgbtnBack);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(DEFAULT_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FAST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imgbtn_back = findViewById(R.id.imgbtnBack);
        imgbtn_newGame = findViewById(R.id.imgbtnNewGame);

        imgbtn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MapsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        imgbtn_newGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MapsActivity.this, CreateGameActivity.class);
                intent.putExtra("latitude", currentLocation.getLatitude());
                intent.putExtra("longitude", currentLocation.getLongitude());
                startActivity(intent);
                finish();
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

                        getNearbyGames();

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
                        Log.w("MapsActivity", "Map not ready yet, skipping marker update.");
                    }
                }
            }
        };

        updateGPS();
        startLocationUpdates();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);





        binding = ActivityMapsBinding.inflate(getLayoutInflater());


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

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }
    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates(); // Your method already does permission check and restarts updates
    }

    private void getNearbyGames() { //gets games within 0.1 lon and lat of current location and adds waypoints at their locations

        requestQueue = Volley.newRequestQueue(this);
        String url = "https://studev.groept.be/api/a24pt110/getNearbyGames/"+currentLocation.getLongitude()+"/"+currentLocation.getLongitude()+"/"+currentLocation.getLatitude()+"/"+currentLocation.getLatitude();
        Log.d("DisplayGames", "CurrentLat = "+currentLocation.getLongitude());
        Log.d("DisplayGames", "CurrentLon = "+currentLocation.getLatitude());  //For some reason getlatitude is retrieving longitude and vice versa, so using inverted inputs works right now

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d("DisplayGames", "Received response");
                        Log.d("DisplayGames", "Response length:"+response.length());
                        Bitmap originalBitmapRace = BitmapFactory.decodeResource(getResources(), R.drawable.racingflag);
                        Bitmap scaledBitmapRace = Bitmap.createScaledBitmap(originalBitmapRace, 100, 100, false);
                        Bitmap originalBitmapQuiz = BitmapFactory.decodeResource(getResources(), R.drawable.ideas);
                        Bitmap scaledBitmapQuiz = Bitmap.createScaledBitmap(originalBitmapQuiz, 80, 80, false);

                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject obj = response.getJSONObject(i);
                                String gameType = obj.getString("GameType");
                                String gameName = obj.getString("Name");
                                Double gameLat = obj.getDouble("Latitude");
                                Double gameLon = obj.getDouble("Longitude");
                                int gameID = obj.getInt("GameID");

                                // Create a unique key for this game
                                String gameKey = gameType + "_" + gameName + "_" + gameLat + "_" + gameLon;

                                if (!gameMarkers.containsKey(gameKey)) {
                                    LatLng latLng = new LatLng(gameLon, gameLat);
                                    Bitmap iconBitmap = gameType.equals("Race") ? scaledBitmapRace : scaledBitmapQuiz;

                                    Marker marker = mMap.addMarker(new MarkerOptions()
                                            .position(latLng)
                                            .title(gameName)
                                            .icon(BitmapDescriptorFactory.fromBitmap(iconBitmap)));

                                    // 🔗 Store the game info in the marker's tag
                                    JSONObject gameData = new JSONObject();
                                    gameData.put("type", gameType);
                                    gameData.put("name", gameName);
                                    gameData.put("lat", gameLat);
                                    gameData.put("lon", gameLon);
                                    gameData.put("gameID", gameID);
                                    assert marker != null;
                                    marker.setTag(gameData);  // Info is stored here for marker interface

                                    gameMarkers.put(gameKey, marker);
                                    Log.d("DisplayGames", "Added new marker: " + gameKey);
                                } else {
                                    Log.d("DisplayGames", "Marker already exists: " + gameKey);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MapsActivity.this, "network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(request);
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
        customInfoView = findViewById(R.id.custom_info_window);
        tvGameName = customInfoView.findViewById(R.id.tvGameName);
        tvGameType = customInfoView.findViewById(R.id.tvGameType);
        btnPlay = customInfoView.findViewById(R.id.btnPlay);
        LinearLayout llLeaderboard = customInfoView.findViewById(R.id.llLeaderboard);

        mMap.setOnMarkerClickListener(marker -> { // Logic for Interface when clicking on a marker
            if (marker.equals(userMarker)) {
                return false;
            }

            ImageView imgLike = customInfoView.findViewById(R.id.imgLike);
            TextView tvLikeCount = customInfoView.findViewById(R.id.tvLikeCount);

            selectedMarker = marker;

            // Try to extract game data from the tag
            JSONObject gameData = (JSONObject) marker.getTag();
            if (gameData != null) {
                String gameName = gameData.optString("name", "Unknown");
                String gameType = gameData.optString("type", "Unknown");
                int gameID = gameData.optInt("gameID",-1);
                Log.d("Leaderboard", "Fetching leaderboard for: " + gameName);
                fetchLeaderboard(gameID, gameType, llLeaderboard);

                tvGameName.setText(gameName);
                tvGameType.setText("Type: " + gameType);

                fetchLikesStatus(gameID, imgLike, tvLikeCount);

                imgLike.setOnClickListener(v -> {
                    toggleLike(gameID, imgLike, tvLikeCount);
                });

                // Convert LatLng to screen position
                Point screenPosition = mMap.getProjection().toScreenLocation(marker.getPosition());
                customInfoView.setX(screenPosition.x - customInfoView.getWidth() / 2f);
                customInfoView.setY(screenPosition.y - customInfoView.getHeight());

                customInfoView.setVisibility(View.VISIBLE);

                btnPlay.setOnClickListener(v -> {
                    Toast.makeText(MapsActivity.this, "Joining " + gameType + ": " + gameName, Toast.LENGTH_SHORT).show();
                    if (gameType.equals("Race")) {

                        Intent intent = new Intent(MapsActivity.this, PlayRaceActivity.class);
                        intent.putExtra("latitude", currentLocation.getLatitude());
                        intent.putExtra("longitude", currentLocation.getLongitude());
                        intent.putExtra("gameName", gameName);
                        startActivity(intent);
                        finish();


                    } else if (gameType.equals("Quiz")) {

                        Intent intent = new Intent(MapsActivity.this, PlayQuizActivity.class);
                        intent.putExtra("latitude", currentLocation.getLatitude());
                        intent.putExtra("longitude", currentLocation.getLongitude());
                        intent.putExtra("gameName", gameName);
                        startActivity(intent);
                        finish();
                    }
                });

                return true;
            }

            return false;
        });


        mMap.setOnMapClickListener(latLng -> {
            customInfoView.setVisibility(View.GONE);
        });
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

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);

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
                        Toast.makeText(MapsActivity.this, "Location not available", Toast.LENGTH_SHORT).show();
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

    private void fetchLeaderboard(int gameID, String gameType, LinearLayout llLeaderboard) {

        String url;
        String scoreType;
        if (gameType.equals("Race")) {
             url = "https://studev.groept.be/api/a24pt110/getRaceScores/" + gameID;
             scoreType = "ScoreSeconds";
        }
        else { //If adding more modes, add here <-
            url = "https://studev.groept.be/api/a24pt110/getQuizScores/"+gameID;
            scoreType = "ScorePercentage";
        }
        Log.d("Leaderboard", "Fetching leaderboard for: " + url);

        JsonArrayRequest leaderboardRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {

                    llLeaderboard.removeAllViews(); // clear previous entries
                    for (int i = 0; i < Math.min(3, response.length()); i++) {
                        try {
                            JSONObject scoreEntry = response.getJSONObject(i);
                            String playerName = scoreEntry.getString("Username");
                            float score = scoreEntry.getInt(scoreType);

                            TextView entry = new TextView(this);
                            entry.setText((i + 1) + ". " + playerName + ": " + score);
                            entry.setTextSize(14);
                            entry.setTextColor(Color.BLACK);


                            llLeaderboard.addView(entry);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> Log.e("Leaderboard", "Error loading leaderboard: " + error.getMessage())
        );

        requestQueue.add(leaderboardRequest);
    }
    private void fetchLikesStatus(int gameID, ImageView imgLike, TextView tvLikeCount) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        int userID = prefs.getInt("UserID", 0);

        String url = "https://studev.groept.be/api/a24pt110/getGameLikes/" + userID + "/" + gameID+ "/" + gameID;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.length() > 0) {
                            JSONObject obj = response.getJSONObject(0);
                            boolean userLiked = obj.getInt("UserLiked") == 1;
                            int totalLikes = obj.getInt("TotalLikes");

                            tvLikeCount.setText(String.valueOf(totalLikes));
                            imgLike.setImageResource(userLiked ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
                            imgLike.setTag(userLiked);  // Save status for toggle
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("Likes", "Error fetching likes: " + error.getMessage())
        );

        requestQueue.add(request);
    }

    private void toggleLike(int gameID, ImageView imgLike, TextView tvLikeCount) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        int userID = prefs.getInt("UserID", 0);
        boolean isLiked = (Boolean) imgLike.getTag();
        String url = isLiked ?
                "https://studev.groept.be/api/a24pt110/unlikeGame/" + userID + "/" +  gameID:
                "https://studev.groept.be/api/a24pt110/likeGame/" + userID + "/" +  gameID;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    // Toggle icon
                    imgLike.setImageResource(isLiked ? R.drawable.ic_star_outline : R.drawable.ic_star_filled);
                    imgLike.setTag(!isLiked);
                    // Refresh count
                    fetchLikesStatus(gameID, imgLike, tvLikeCount);
                },
                error -> Log.e("Likes", "Error toggling like: " + error.getMessage())
        );

        requestQueue.add(request);
    }

}