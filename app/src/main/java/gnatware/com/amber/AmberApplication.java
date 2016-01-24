package gnatware.com.amber;

import android.app.Application;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;

import java.util.Date;

/**
 * Created by pzingg on 1/9/16.
 */
public class AmberApplication extends Application {

    public static final String TAG = "AmberApplication";

    private LocationManager mLocationManager;
    private String mProvider;
    private LatLng mDefaultLocation;


    // TODO: GoogleService failed to initialize
    // Missing an expected resource: 'R.string.google_app_id' for initializing Google services.
    // Possible causes are missing google-services.json
    // or com.google.gms.google-services gradle plugin.

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);

        // Add your initialization code here
        Parse.initialize(this);

        ParseUser.enableAutomaticUser();
        ParseACL defaultACL = new ParseACL();

        // Optionally enable public read access.
        // defaultACL.setPublicReadAccess(true);

        ParseACL.setDefaultACL(defaultACL, true);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mProvider = mLocationManager.getBestProvider(new Criteria(), false);
        mDefaultLocation = new LatLng(-34, 151);
    }

    public void removeLocationUpdates(LocationListener listener) {
        // Updates every 400 ms, or 1 degree change
        Log.d(TAG, "Removing location updates for listener " + listener);
        try {
            mLocationManager.removeUpdates(listener);
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted");
        }
    }

    public void requestLocationUpdates(LocationListener listener) {
        // Updates every 400 ms, or 1 degree change
        Log.d(TAG, "Requesting location updates for listener " + listener + " with provider " + mProvider);
        try {
            mLocationManager.requestLocationUpdates(mProvider, 400, 1, listener);
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted");
        }
    }

    public Location getLastKnownLocation() {
        Location location = null;
        try {
            location = mLocationManager.getLastKnownLocation(mProvider);
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted");
        }
        return location;
    }

    public LatLng getDefaultLocation() {
        return mDefaultLocation;
    }

    public LatLng getLastOrDefaultLocation() {
        Location location = getLastKnownLocation();
        return (location != null) ?
                new LatLng(location.getLatitude(), location.getLongitude()) :
                mDefaultLocation;
    }

    public void updateDriverLocation(LatLng location) {
        ParseUser driver = ParseUser.getCurrentUser();
        if (driver != null && driver.getString("role") == "driver") {
            Log.d(TAG, "updateDriverLocation: Saving driver location");
            driver.put("lastLocation", new ParseGeoPoint(location.latitude, location.longitude));
            driver.put("lastLocationAt", new Date());
            driver.saveEventually();
        } else {
            Log.d(TAG, "updateDriverLocation: Current user is not a driver");
        }
    }
}
