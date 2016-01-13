package gnatware.com.amber;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.CountCallback;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.ParseACL;
import com.parse.SaveCallback;

import java.util.List;

public class RiderMapActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback {

    // Dynamic UI elements
    private FloatingActionButton mFab;

    // Current rider request status
    private Boolean mRequestPending;

    // Location and map updates
    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private String mProvider;
    private double mCurrentLatitude;
    private double mCurrentLongitude;

    protected void setMarkerAndZoomToLocation(LatLng latLng, String label) {
        mCurrentLatitude = latLng.latitude;
        mCurrentLongitude = latLng.longitude;
        if (mMap == null) {
            Log.d("RiderMapActivity", "Map not ready in " + label + ", location now " + latLng.toString());
        } else {
            Log.d("RiderMapActivity", label + ", location now " + latLng.toString());
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title("Your Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
        }
    }
    protected void submitRequest() {
        Log.d("RiderMapActivity", "Submit request");
        ParseUser requester = ParseUser.getCurrentUser();

        if (requester != null) {
            String requesterId = requester.getObjectId();
            if (requesterId != null) {

                // Drivers and requesters all need to see requests
                ParseACL acl = new ParseACL();
                acl.setPublicReadAccess(true);
                acl.setPublicWriteAccess(true);

                ParseGeoPoint pickupLocation = new ParseGeoPoint(mCurrentLatitude, mCurrentLongitude);

                final ParseObject request = new ParseObject("Request");
                request.setACL(acl);
                request.put("requesterId", requesterId);
                request.put("pickupLocation", pickupLocation);

                Log.d("RiderMapActivity", "Submitting request for user " + requesterId +
                        " at " + pickupLocation.toString());
                request.saveInBackground(new SaveCallback() {

                    @Override
                    public void done(ParseException e) {
                        String status = null;
                        if (e != null) {
                            Log.d("RiderMapActivity", "Error submitting request: " + e.getMessage());
                            status = "Error sending request!";
                        } else {
                            mRequestPending = true;
                        }
                        updateRequestPending(status);
                    }
                });
                return;
            }
        }
        Log.d("RiderMapActivity", "Error submitting request: No anonymous user");
    }

    protected void cancelRequest() {
        Log.d("RiderMapActivity", "Cancel request");
        ParseUser requester = ParseUser.getCurrentUser();

        if (requester != null) {
            final String requesterId = requester.getObjectId();
            if (requesterId != null) {
                ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
                query.whereEqualTo("requesterId", requesterId);

                Log.d("RiderMapActivity", "Finding requests for user " + requesterId);
                query.findInBackground(new FindCallback<ParseObject>() {

                    @Override
                    public void done(List<ParseObject> objects, ParseException e1) {
                        if (e1 != null) {
                            Log.d("RiderMapActivity", "Error canceling request: " + e1.getMessage());
                            updateRequestPending("Error canceling request");
                        } else {
                            if (objects.isEmpty()) {
                                mRequestPending = false;
                                Log.d("RiderMapActivity", "Error canceling request: No pending requests");
                                updateRequestPending("No pending requests");
                            } else {
                                Log.d("RiderMapActivity", "Deleting " + Integer.toString(objects.size()) +
                                        " request(s) for user " + requesterId);
                                ParseObject.deleteAllInBackground(objects, new DeleteCallback() {

                                    @Override
                                    public void done(ParseException e2) {
                                        String status = null;
                                        if (e2 != null) {
                                            Log.d("RiderMapActivity", "Error deleting requests: " + e2.getMessage());
                                            status = "Error deleting requests";
                                        } else {
                                            mRequestPending = false;
                                        }
                                        updateRequestPending(status);
                                    }
                                });
                            }
                        }
                    }
                });
                return;
            }
        }
        Log.d("RiderMapActivity", "Error canceling request: No anonymous user");
    }

    protected void updateRequestPending(String status) {
        if (mRequestPending) {
            Log.d("RiderMapActivity", "UI set to request pending");
            mFab.setImageResource(R.mipmap.cancel);
            mFab.setContentDescription("Cancel your request");
            if (status == null) {
                status = "Finding a driver.  Click to cancel.";
            }
        } else {
            Log.d("RiderMapActivity", "UI reset to NO request pending");
            mFab.setImageResource(R.mipmap.car);
            mFab.setContentDescription("Request a driver");
            if (status == null) {
                status = "Click to request a driver...";
            }
        }
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.map_layout);
        Snackbar snackbar = Snackbar.make(coordinatorLayout, status, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    protected void checkPendingRequest(final String status) {
        Log.d("RiderMapActivity", "Check pending request");
        ParseUser requester = ParseUser.getCurrentUser();

        if (requester != null) {
            final String requesterId = requester.getObjectId();
            if (requesterId != null) {
                ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
                query.whereEqualTo("requesterId", requesterId);

                Log.d("RiderMapActivity", "Counting requests for user " + requesterId);
                query.countInBackground(new CountCallback() {

                    @Override
                    public void done(int count, ParseException e) {
                        if (e != null) {
                            Log.d("RiderMapActivity", "Cannot check pending request: " + e.getMessage());
                        } else {
                            Log.d("RiderMapActivity", "User " + requesterId + "  has " +
                                    Integer.toString(count) + " pending request(s)");
                            if (count > 0) {
                                mRequestPending = true;
                            } else {
                                mRequestPending = false;
                            }
                        }
                        updateRequestPending(status);
                    }
                });
                return;
            }
        }
        Log.d("RiderMapActivity", "Cannot check pending request: No anonymous user");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_map);

        /*
        // Action bar is supplied by theme in AndroidManifest.xml
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
         */

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mRequestPending = false;
        mFab = (FloatingActionButton)findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d("RiderMapActivity", "FAB clicked");
                if (mRequestPending) {
                    cancelRequest();
                } else {
                    submitRequest();
                }
            }
        });

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mProvider = mLocationManager.getBestProvider(new Criteria(), false);

        // Updates every 400 ms, or 1 degree change
        Log.d("RiderMapActivity", "onCreate: Requesting location updates with mProvider " + mProvider);
        mLocationManager.requestLocationUpdates(mProvider, 400, 1, this);

        checkPendingRequest(null);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mLocationManager.removeUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Updates every 400 ms, or 1 degree change
        Log.d("RiderMapActivity", "onResume: Requesting location updates with mProvider " + mProvider);
        mLocationManager.requestLocationUpdates(mProvider, 400, 1, this);

        checkPendingRequest(null);
    }

    // GoogleMap OnMapReadyCallback method
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

        Location location = mLocationManager.getLastKnownLocation(mProvider);
        LatLng latLng = (location != null) ?
                new LatLng(location.getLatitude(), location.getLongitude()) :
                new LatLng(-34, 151);

        setMarkerAndZoomToLocation(latLng, "onMapReady");
    }

    // LocationListener methods
    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        setMarkerAndZoomToLocation(latLng, "onLocationChanged");
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

