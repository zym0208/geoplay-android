package be.kuleuven.gt.javabean;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
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
import java.util.List;

public class RaceGame extends Game{
    private List<Location> checkpoints;
    private Context context;

    public RaceGame(Context context, Location startLocation) {
        super(context,startLocation);
        this.type = gameType.RACE;
        checkpoints = new ArrayList<>();
        this.context = context;
        checkpoints.add(startLocation);




    }


    @Override
    public void createGame(String username) {

        //TODO Change from unique game name to combination of game name and location
        requestQueue = Volley.newRequestQueue(context);
        LatLng latLng = new LatLng(startLocation.getLatitude(), startLocation.getLongitude());
        String gameNameEncoded = Uri.encode(gameName.replace(" ", "_"));
        String url = "https://studev.groept.be/api/a24pt110/createGame/Race/" +
                latLng.latitude + "/" + latLng.longitude + "/" + gameNameEncoded+"/"+username;
        Log.d("CreateGame", "Start Lat = "+latLng.latitude);
        Log.d("CreateGame", "Start Lon = "+latLng.longitude);

        Log.d("CreateGame", url);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(context, "Game created！", Toast.LENGTH_SHORT).show();
                        fetchGameIDAndCreateCheckpoints(); // <- Call after confirmation
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

    private void fetchGameIDAndCreateCheckpoints() {
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
                                createRaceCheckpoints(); // Now safe to call
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

    private void createRaceCheckpoints() {
        for (Location cp : checkpoints) {
            String cpUrl = "https://studev.groept.be/api/a24pt110/createRace/" +
                    gameID + "/" + cp.getLatitude() + "/" + cp.getLongitude();

            Log.d("CreateGame", cpUrl);
            Log.d("CreateGame", "Lat = "+cp.getLatitude());
            Log.d("CreateGame", "Lon = "+cp.getLongitude());

            StringRequest cpRequest = new StringRequest(Request.Method.GET, cpUrl,
                    response -> Toast.makeText(context, "Race CP created！", Toast.LENGTH_SHORT).show(),
                    error -> Toast.makeText(context, "Race CP creation failed: " + error.getMessage(), Toast.LENGTH_SHORT).show()
            );

            requestQueue.add(cpRequest);
        }
    }
    public void addCheckpoint(Location checkPoint) {
        checkpoints.add(checkPoint);
    }
    public void removeLastCP() {
        if (checkpoints.size() > 1) {
            checkpoints.remove(checkpoints.size() - 1);
        }
    }
    public void reset() {
        checkpoints.clear();
    }

    public int getLength() {
        return checkpoints.size();
    }


    @Override
    public void setGameName(String newName) {
        gameName = newName;
    }

    @Override
    public int getGameID(String gameName) {
        String url = "https://studev.groept.be/api/a24pt110/getGameID/"+gameName; //你的查询API
        Log.d("Database", url);
        requestQueue = Volley.newRequestQueue(context);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        boolean found = false;
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject obj = response.getJSONObject(i);
                                String gameIDString = obj.getString("GameID");
                                gameID = Integer.parseInt(gameIDString);

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, "network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(request);
        return gameID;
    }
}
