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

    public static final String TAG = "DriverRequestsActivity";

    private AmberApplication mApplication;
    private RequestsAdapter mRequestsAdapter;

    private LatLng mDriverLocation;

    protected void updateDriverLocation(Location location) {
        if (location == null) {
            location = mApplication.getLastKnownLocation();
            if (location == null) {
                Log.d(TAG, "No location");
                return;
            }
        }
        mDriverLocation = new LatLng(location.getLatitude(), location.getLongitude());
        mApplication.updateDriverLocation(mDriverLocation);
        mRequestsAdapter.updateDriverLocation(mDriverLocation);
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
        mRequestsAdapter = new RequestsAdapter(driver, this);
        if (false) { // BuildConfig.DEBUG
            mRequestsAdapter.cancelAcceptedRequests();
        }
        rvRequests.setAdapter(mRequestsAdapter);


        // Set layout manager to position the items
        rvRequests.setLayoutManager(new LinearLayoutManager(this));

        mApplication = (AmberApplication) getApplication();
        mApplication.requestLocationUpdates(this);
        updateDriverLocation(null);
    }

    // LocationListener methods
    @Override
    public void onLocationChanged(Location location) {

        updateDriverLocation(location);
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
