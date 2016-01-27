package com.gnatware.amber;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseUser;

public class DriverRequestsActivity extends AppCompatActivity implements LocationListener {

    public static final String TAG = "DriverRequestsActivity";

    private AmberApplication mApplication;
    private CoordinatorLayout mLayout;
    private RequestsAdapter mRequestsAdapter;

    private LatLng mDriverLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        initializeState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

        mApplication.removeLocationUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

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

    // Public methods

    // Public access for RequestsAdapter access
    public void showSnack(String message) {
        Snackbar snackbar = Snackbar.make(mLayout, message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    // Private methods
    private void initializeState() {
        mApplication = (AmberApplication) getApplication();

        mLayout = (CoordinatorLayout) getLayoutInflater().inflate(R.layout.activity_driver_requests, null);
        setContentView(mLayout);

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
        mRequestsAdapter = new RequestsAdapter(this, driver);
        if (false) { // BuildConfig.DEBUG
            mApplication.cancelAllActiveRequests();
        }
        rvRequests.setAdapter(mRequestsAdapter);

        // Set layout manager to position the items
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
    }

    private void updateDriverLocation(Location location) {
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
}
