package gnatware.com.amber;

import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
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
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.text.DecimalFormat;
import java.util.Date;

public class DriverMapActivity extends AppCompatActivity implements
        ViewTreeObserver.OnGlobalLayoutListener, OnMapReadyCallback {

    private FloatingActionButton mFab;
    private CoordinatorLayout mLayout;

    private GoogleMap mMap;

    private Boolean mLayoutComplete;
    private String mRequestId;
    private String mRequesterId;
    private LatLng mPickupLocation;
    private LatLng mDriverLocation;

    protected void updateMarkersAndCamera() {
        if (mMap == null) {
            Log.d("DriverMapActivity", "no map");
        } else if (mRequesterId == null) {
            Log.d("DriverMapActivity", "no requester");
        } else if (!mLayoutComplete) {
            Log.d("DriverMapActivity", "layout incomplete");
        } else {
            Log.d("DriverMapActivity", "locate map");

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
            CameraUpdate update = CameraUpdateFactory.newLatLngBounds(builder.build(), 100);
            mMap.moveCamera(update);
        }
    }

    protected void acceptRequest() {
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
                } else if (object.getString("driverId") != null) {
                    message = "Request " + mRequestId + " has been accepted by another driver";
                }
                if (message != null) {

                    // Something failed, show message
                    // TODO: Go back to DriverRequestsActivity?
                    Log.d("DriverMapActivity", message);
                    Snackbar snackbar = Snackbar.make(mLayout, message, Snackbar.LENGTH_LONG);
                    snackbar.show();
                } else {

                    // Request still available, book it and show directions
                    String driverId = ParseUser.getCurrentUser().getObjectId();
                    object.put("driverId", driverId);
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
                            Log.d("DriverMapActivity", message);
                            Snackbar snackbar = Snackbar.make(mLayout, message, Snackbar.LENGTH_LONG);
                            snackbar.show();
                            if (accepted) {
                                DecimalFormat format = new DecimalFormat("0.######");
                                String mapsUri = "https://www.google.com/maps/dir/" +
                                        format.format(mDriverLocation.latitude) + "," +
                                        format.format(mDriverLocation.longitude) + "/" +
                                        format.format(mPickupLocation.latitude) + "," +
                                        format.format(mPickupLocation.longitude) + "/";

                                // Start Google Maps activity
                                Log.d("DriverMapActivity", "Starting maps intent with URI " + mapsUri);
                                Intent mapsIntent = new Intent(android.content.Intent.ACTION_VIEW,
                                        Uri.parse(mapsUri));
                                mapsIntent.setPackage("com.google.android.apps.maps");
                                startActivity(mapsIntent);
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("DriverMapActivity", "onCreate");
        mLayoutComplete = false;

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
                Log.d("DriverMapActivity", "FAB clicked");
                acceptRequest();
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.driver_map);
        mapFragment.getMapAsync(this);

        updateMarkersAndCamera();
    }

    @Override
    public void onGlobalLayout() {
        Log.d("DriverMapActivity", "onGlobalLayout");

        // At this point, the UI is fully displayed
        mLayoutComplete = true;
        updateMarkersAndCamera();
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

        Log.d("DriverMapActivity", "onMapReady");
        mMap = googleMap;

        updateMarkersAndCamera();
    }
}
