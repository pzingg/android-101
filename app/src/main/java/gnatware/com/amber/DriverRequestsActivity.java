package gnatware.com.amber;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseUser;

public class DriverRequestsActivity extends AppCompatActivity implements LocationListener {

    private LocationManager mLocationManager;
    private String mProvider;
    private double mCurrentLatitude;
    private double mCurrentLongitude;
    private RequestsAdapter mRequestsAdapter;

    public void updateLocationResults(Location location) {
        LatLng latLng = (location != null) ?
                new LatLng(location.getLatitude(), location.getLongitude()) :
                new LatLng(-34, 151);

        Log.d("DriverActivity", "Location is now " + latLng.toString());
        mCurrentLatitude = latLng.latitude;
        mCurrentLongitude = latLng.longitude;
        mRequestsAdapter.setDriverLocation(mCurrentLatitude, mCurrentLongitude);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_requests);

        /*
        // Action bar is supplied by theme in AndroidManifest.xml
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */

        // Set up RecyclerView and its adapter
        RecyclerView rvRequests = (RecyclerView) findViewById(R.id.rvNearbyRequests);

        // Create the adapter that interfaces with Parse API to bind data to the view
        ParseUser driver = ParseUser.getCurrentUser();
        mRequestsAdapter = new RequestsAdapter(driver);
        if (BuildConfig.DEBUG) {
            mRequestsAdapter.cancelAcceptedRequests();
        }
        rvRequests.setAdapter(mRequestsAdapter);

        // Set layout manager to position the items
        rvRequests.setLayoutManager(new LinearLayoutManager(this));

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mProvider = mLocationManager.getBestProvider(new Criteria(), false);

        // Updates every 400 ms, or 1 degree change
        Log.d("DriverActivity", "onCreate: Requesting location updates with mProvider " + mProvider);
        mLocationManager.requestLocationUpdates(mProvider, 400, 1, this);

        Location location = mLocationManager.getLastKnownLocation(mProvider);
        updateLocationResults(location);
    }

    // LocationListener methods
    @Override
    public void onLocationChanged(Location location) {
        updateLocationResults(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
