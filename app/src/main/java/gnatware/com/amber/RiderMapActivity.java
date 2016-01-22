package gnatware.com.amber;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.ParseACL;
import com.parse.SaveCallback;

import java.util.List;

public class RiderMapActivity extends AppCompatActivity implements
        ViewTreeObserver.OnGlobalLayoutListener, LocationListener, OnMapReadyCallback {

    public static final String TAG = "RiderMapActivity";

    public static final Integer REQUEST_NONE = 0;
    public static final Integer REQUEST_ACTIVE = 1;
    public static final Integer REQUEST_ASSIGNED = 2;
    
    private AmberApplication mApplication;

    // Dynamic UI elements
    private FloatingActionButton mFab;
    private CoordinatorLayout mLayout;

    private Boolean mLayoutComplete;

    // Current rider request status
    private int mRequestState;

    // Location and map updates
    private GoogleMap mMap;
    private LatLng mRiderLocation;
    private LatLng mDriverLocation;
    private String mDriverId;

    protected void updateMap() {
        if (mMap == null) {
            Log.d(TAG, "No map");
        } else {
            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(mRiderLocation)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            if (mDriverLocation != null) {
                if (!mLayoutComplete) {
                    Log.d(TAG, "Layout incomplete");
                    // Fall through to center rider on map
                } else {
                    Log.d(TAG, "Locate driver and rider on map");
                    // Figure out a bounds and zoom level
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    mMap.addMarker(new MarkerOptions()
                            .position(mDriverLocation)
                            .title(mDriverId)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    builder.include(mRiderLocation);
                    builder.include(mDriverLocation);

                    // Error using newLatLngBounds(LatLngBounds, int): Map size can't be 0.
                    // Most likely, layout has not yet occured for the map view.
                    View view = findViewById(R.id.rider_map_view);
                    int width = view.getWidth();
                    int height = view.getHeight();
                    if (height > 400) {
                        height -= 200;
                    } // Try to avoid FAB at bottom right of view?
                    LatLngBounds bounds = builder.build();
                    CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, width, height, 100);
                    mMap.moveCamera(update);
                    return;
                }
            }
            Log.d(TAG, "Center rider on map");
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mRiderLocation, 10));
        }
    }

    protected void updateRiderLocation(Location location) {
        if (location == null) {
            location = mApplication.getLastKnownLocation();
            if (location == null) {
                Log.d(TAG, "No location");
                return;
            }
        }
        mRiderLocation = new LatLng(location.getLatitude(), location.getLongitude());
        updateMap();
    }

    protected void updateDriverLocation() {
        if (mDriverId != null) {
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.getInBackground(mDriverId, new GetCallback<ParseUser>() {

                @Override
                public void done(ParseUser object, ParseException e) {
                    mDriverLocation = null;
                    if (e != null) {
                        Log.d(TAG, "Cannot get driver: " + e.getMessage());
                    } else if (object == null) {
                        Log.d(TAG, "Cannot get driver with id " + mDriverId);
                    } else {
                        ParseGeoPoint location = object.getParseGeoPoint("location");
                        if (location == null) {
                            Log.d(TAG, "Driver " + mDriverId + " has no location");
                        } else {
                            mDriverLocation = new LatLng(
                                    location.getLatitude(), location.getLongitude());
                        }
                    }
                    updateMap();
                }
            });
        } else {
            mDriverLocation = null;
            updateMap();
        }
    }

    protected void submitRequest() {
        Log.d(TAG, "Submit request");
        ParseUser requester = ParseUser.getCurrentUser();

        if (requester != null) {
            String requesterId = requester.getObjectId();
            if (requesterId != null) {

                // Drivers and requesters all need to see requests
                ParseACL acl = new ParseACL();
                acl.setPublicReadAccess(true);
                acl.setPublicWriteAccess(true);

                ParseGeoPoint pickupLocation = new ParseGeoPoint(
                        mRiderLocation.latitude, mRiderLocation.longitude);

                final ParseObject request = new ParseObject("Request");
                request.setACL(acl);
                request.put("requesterId", requesterId);
                request.put("pickupLocation", pickupLocation);

                Log.d(TAG, "Submitting request for user " + requesterId +
                        " at " + pickupLocation.toString());
                request.saveInBackground(new SaveCallback() {

                    @Override
                    public void done(ParseException e) {
                        String status = null;
                        if (e != null) {
                            Log.d(TAG, "Error submitting request: " + e.getMessage());
                            status = "Error sending request!";
                        } else {
                            mRequestState = REQUEST_ACTIVE;
                        }
                        updateRequestPending(status);
                    }
                });
                return;
            }
        }
        Log.d(TAG, "Error submitting request: No anonymous user");
    }

    protected void cancelRequest() {
        Log.d(TAG, "Cancel request");
        ParseUser requester = ParseUser.getCurrentUser();

        if (requester != null) {
            final String requesterId = requester.getObjectId();
            if (requesterId != null) {
                ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
                query.whereEqualTo("requesterId", requesterId);

                Log.d(TAG, "Finding requests for user " + requesterId);
                query.findInBackground(new FindCallback<ParseObject>() {

                    @Override
                    public void done(List<ParseObject> objects, ParseException e1) {
                        if (e1 != null) {
                            Log.d(TAG, "Error canceling request: " + e1.getMessage());
                            updateRequestPending("Error canceling request");
                        } else {
                            if (objects.isEmpty()) {
                                mRequestState = REQUEST_NONE;
                                Log.d(TAG, "Error canceling request: No pending requests");
                                updateRequestPending("No pending requests");
                            } else {
                                Log.d(TAG, "Deleting " + Integer.toString(objects.size()) +
                                        " request(s) for user " + requesterId);
                                ParseObject.deleteAllInBackground(objects, new DeleteCallback() {

                                    @Override
                                    public void done(ParseException e2) {
                                        String status = null;
                                        if (e2 != null) {
                                            Log.d(TAG, "Error deleting requests: " + e2.getMessage());
                                            status = "Error deleting requests";
                                        } else {
                                            mRequestState = REQUEST_NONE;
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
        Log.d(TAG, "Error canceling request: No anonymous user");
    }

    protected void updateRequestPending(String status) {
        if (mRequestState != REQUEST_NONE) {
            Log.d(TAG, "UI set to request pending");
            mFab.setImageResource(R.drawable.ic_clear_black_24dp);
            mFab.setContentDescription("Cancel your request");
            if (status == null) {
                status = "Finding a driver.  Click to cancel.";
            }
        } else {
            Log.d(TAG, "UI reset to NO request pending");
            mFab.setImageResource(R.drawable.ic_add_black_24dp);
            mFab.setContentDescription("Request a driver");
            if (status == null) {
                status = "Click to request a driver...";
            }
        }
        Snackbar snackbar = Snackbar.make(mLayout, status, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    protected void checkPendingRequest(final String status) {
        Log.d(TAG, "Check pending request");
        ParseUser requester = ParseUser.getCurrentUser();

        if (requester != null) {
            final String requesterId = requester.getObjectId();
            if (requesterId != null) {
                ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
                query.whereEqualTo("requesterId", requesterId);

                Log.d(TAG, "Counting requests for user " + requesterId);
                query.getFirstInBackground(new GetCallback<ParseObject>() {

                    @Override
                    public void done(ParseObject object, ParseException e) {
                        if (e != null) {
                            Log.d(TAG, "Cannot check pending request: " + e.getMessage());
                        } else {
                            if (object != null) {
                                mDriverId = object.getString("driverId");
                                if (mDriverId != null) {
                                    mRequestState = REQUEST_ASSIGNED;
                                } else {
                                    mRequestState = REQUEST_ACTIVE;
                                }
                            } else {
                                mDriverId = null;
                                mRequestState = REQUEST_NONE;
                            }
                            updateRequestPending(status);
                            updateDriverLocation();
                        }
                    }
                });
                return;
            }
        }
        Log.d(TAG, "Cannot check pending request: No anonymous user");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLayoutComplete = false;

        mLayout = (CoordinatorLayout) getLayoutInflater().inflate(R.layout.activity_rider_map, null);
        setContentView(mLayout);

        /*
        // Action bar is supplied by theme in AndroidManifest.xml
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
         */
        mFab = (FloatingActionButton) findViewById(R.id.rider_map_fab);
        mFab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, "FAB clicked");
                if (mRequestState != REQUEST_NONE) {
                    cancelRequest();
                } else {
                    submitRequest();
                }
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.rider_map);
        mapFragment.getMapAsync(this);

        mRequestState = REQUEST_NONE;
        checkPendingRequest(null);

        mApplication = (AmberApplication) getApplication();
        mApplication.requestLocationUpdates(this);

        updateRiderLocation(null);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mApplication.removeLocationUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkPendingRequest(null);

        mApplication.requestLocationUpdates(this);

        updateRiderLocation(null);
    }

    // ViewTreeObserver.OnGlobalLayoutListener method
    @Override
    public void onGlobalLayout() {
        Log.d(TAG, "onGlobalLayout");

        // At this point, the UI is fully displayed
        mLayoutComplete = true;
        updateRiderLocation(null);
    }

    // LocationListener methods
    @Override
    public void onLocationChanged(Location location) {
        updateRiderLocation(location);
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

        updateRiderLocation(null);
    }
}

