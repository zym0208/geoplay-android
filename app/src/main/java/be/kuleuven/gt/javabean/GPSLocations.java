package be.kuleuven.gt.javabean;

import android.app.Application;
import android.location.Location;

import java.util.ArrayList;
import java.util.List;

public class GPSLocations extends Application {

    private static GPSLocations singleton;

    private List<Location> myLocations;

    public Location getCurrLocation() {
        return currLocation;
    }

    public void setCurrLocation(Location currLocation) {
        this.currLocation = currLocation;
    }

    private Location currLocation;

    public List<Location> getMyLocations() {
        return myLocations;
    }

    public void setMyLocations(List<Location> myLocations) {
        this.myLocations = myLocations;
    }

    public GPSLocations getInstance() {
        return singleton;
    }

    public void onCreate() {
        super.onCreate();
        singleton = this;
        myLocations = new ArrayList<>();
    }
}
