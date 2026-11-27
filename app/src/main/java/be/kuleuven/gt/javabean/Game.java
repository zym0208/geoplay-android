package be.kuleuven.gt.javabean;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public abstract class Game<T> {
    protected Location startLocation;
    protected float score;
    protected gameType type;
    protected int gameID;
    protected RequestQueue requestQueue;
    protected Context context;

    protected String gameName;
    enum gameType {
        RACE,
        QUIZ
    }

    public Game(Context context, Location startLocation) {
        this.startLocation = startLocation;
        if (this.startLocation != null) {
            Log.d("GameCreate", "Start location is:" + startLocation.getLatitude() + startLocation.getLongitude());
        }
        else {
            Log.d("GameCreate", "Start location is null");
        }
        score = 0;
        gameName = "";
        this.context = context;
    }

    public abstract void createGame(String username);

    public String getGameName() {
        return gameName;
    }

    public abstract void setGameName(String newName);
    public abstract int getGameID(String gameName);


}
