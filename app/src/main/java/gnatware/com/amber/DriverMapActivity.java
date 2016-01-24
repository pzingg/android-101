package gnatware.com.amber;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.text.DecimalFormat;
import java.util.Date;

public class DriverMapActivity extends AppCompatActivity implements
        ViewTreeObserver.OnGlobalLayoutListener, LocationListener, OnMapReadyCallback {

    public static final String TAG = "DriverMapActivity";

    private AmberApplication mApplication;
    private FloatingActionButton mFab;
    private CoordinatorLayout mLayout;

    private GoogleMap mMap;

    private Boolean mLayoutComplete;
    private String mRequestId;
    private String mRequesterId;
    private LatLng mPickupLocation;
    private LatLng mDriverLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        initializeState();
        updateDriverLocation(null);
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

    // ViewTreeObserver.OnGlobalLayoutListener method
    @Override
    public void onGlobalLayout() {
        Log.d(TAG, "onGlobalLayout");

        // At this point, the UI is fully displayed
        mLayoutComplete = true;
        updateDriverLocation(null);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");
        mMap = googleMap;

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

    private void initializeState() {
        mLayoutComplete = false;
        mApplication = (AmberApplication) getApplication();

        Intent intent = getIntent();
        mRequestId = intent.getStringExtra("requestId");
        mRequesterId = intent.getStringExtra("requesterId");
        double pickupLatitude = intent.getDoubleExtra("pickupLatitude", 0.);
        double pickupLongitude = intent.getDoubleExtra("pickupLongitude", 0.);
        mPickupLocation = new LatLng(pickupLatitude, pickupLongitude);
        double driverLatitude = intent.getDoubleExtra("driverLatitude", 0.);
        double driverLongitude = intent.getDoubleExtra("driverLongitude", 0.);
        mDriverLocation = new LatLng(driverLatitude, driverLongitude);

        // Set a global layout listener which will be called when the layout pass is completed and the view is drawn
        mLayout = (CoordinatorLayout) getLayoutInflater().inflate(R.layout.activity_driver_map, null);
        mLayout.getViewTreeObserver().addOnGlobalLayoutListener(this);
        setContentView(mLayout);

        mFab = (FloatingActionButton) findViewById(R.id.driver_map_fab);
        mFab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, "FAB clicked");
                acceptRequest();
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.driver_map);
        mapFragment.getMapAsync(this);

        mApplication.requestLocationUpdates(this);
    }

    private void updateMap() {
        Log.d(TAG, "updateMap");
        if (mMap == null) {
            Log.d(TAG, "No map");
        } else if (mRequesterId == null) {
            Log.d(TAG, "No requester");
        } else if (!mLayoutComplete) {
            Log.d(TAG, "Layout incomplete");
        } else {
            Log.d(TAG, "Center driver and rider on map");
            // Figure out a bounds and zoom level
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            Marker pickup = mMap.addMarker(new MarkerOptions()
                    .position(mPickupLocation)
                    .title(mRequesterId)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            builder.include(mPickupLocation);
            Marker driver = mMap.addMarker(new MarkerOptions()
                    .position(mDriverLocation)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            builder.include(mDriverLocation);

            // Error using newLatLngBounds(LatLngBounds, int): Map size can't be 0.
            // Most likely, layout has not yet occured for the map view.
            View view = findViewById(R.id.driver_map_view);
            int width = view.getWidth();
            int height = view.getHeight();
            if (height > 400) { height -= 200; } // Try to avoid FAB at bottom right of view?
            LatLngBounds bounds = builder.build();
            CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, width, height, 100);
            mMap.moveCamera(update);
        }
    }

    private void updateDriverLocation(Location location) {
        Log.d(TAG, "updateDriverLocation");
        if (location == null) {
            location = mApplication.getLastKnownLocation();
            if (location == null) {
                Log.d(TAG, "No location");
                return;
            }
        }
        mDriverLocation = new LatLng(location.getLatitude(), location.getLongitude());
        mApplication.updateDriverLocation(mDriverLocation);
        updateMap();
    }

    private void acceptRequest() {
        Log.d(TAG, "acceptRequest");
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
        query.getInBackground(mRequestId, new GetCallback<ParseObject>() {

            @Override
            public void done(ParseObject object, ParseException e1) {
                String message = null;
                if (e1 != null) {
                    message = "An error occured fetching request " +
                            mRequestId + ": " + e1.getMessage();
                } else if (object == null) {
                    message = "Request " + mRequestId + " no longer exists";
                } else if (object.getParseUser("driver") != null) {
                    message = "Request " + mRequestId + " has been accepted by another driver";
                }
                if (message != null) {

                    // Something failed, show message
                    // TODO: Go back to DriverRequestsActivity?
                    Log.d(TAG, message);
                    Snackbar snackbar = Snackbar.make(mLayout, message, Snackbar.LENGTH_LONG);
                    snackbar.show();
                } else {

                    // Request still available, book it and show directions
                    ParseUser driver = ParseUser.getCurrentUser();
                    object.put("driver", driver);
                    object.put("driverLat", mDriverLocation.latitude);
                    object.put("driverLng", mDriverLocation.longitude);
                    object.put("acceptedAt", new Date());
                    object.saveInBackground(new SaveCallback() {

                        @Override
                        public void done(ParseException e2) {
                            String message = null;
                            Boolean accepted = false;
                            if (e2 != null) {
                                message = "Unable to update request " +
                                        mRequestId + ": " + e2.getMessage();
                            } else {
                                message = "Request " + mRequestId + " accepted";
                                accepted = true;
                            }
                            Log.d(TAG, message);
                            Snackbar snackbar = Snackbar.make(mLayout, message, Snackbar.LENGTH_LONG);
                            snackbar.show();
                            if (accepted) {
                                DecimalFormat format = new DecimalFormat("0.######");
                                String mapsUri = "https://maps.google.com/maps?saddr=" +
                                        format.format(mDriverLocation.latitude) + "," +
                                        format.format(mDriverLocation.longitude) + "&daddr=" +
                                        format.format(mPickupLocation.latitude) + "," +
                                        format.format(mPickupLocation.longitude);

                                // Start Google Maps activity
                                // TODO: Fix GmsClient clearcut.service.START error on emulator
                                // E/GmsClient: unable to connect to service: com.google.android.gms.clearcut.service.START
                                Log.d(TAG, "Starting maps intent with URI " + mapsUri);
                                if (!BuildConfig.DEBUG) {
                                    Intent mapsIntent = new Intent(android.content.Intent.ACTION_VIEW,
                                            Uri.parse(mapsUri));
                                    mapsIntent.setPackage("com.google.android.apps.maps");
                                    startActivity(mapsIntent);
                                }
                            }
                        }
                    });
                }
            }
        });
    }
}
