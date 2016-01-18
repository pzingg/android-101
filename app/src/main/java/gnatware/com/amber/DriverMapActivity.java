package gnatware.com.amber;

import android.content.Intent;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewTreeObserver;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class DriverMapActivity extends AppCompatActivity implements
        ViewTreeObserver.OnGlobalLayoutListener, OnMapReadyCallback {

    private Intent mIntent;
    private Boolean mLayoutComplete;
    private FloatingActionButton mFab;
    private GoogleMap mMap;
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
            Marker pickup = mMap.addMarker(new MarkerOptions().position(mPickupLocation).title(mRequesterId));
            builder.include(mPickupLocation);
            Marker driver = mMap.addMarker(new MarkerOptions().position(mDriverLocation).title("Your Location"));
            builder.include(mDriverLocation);

            // Error using newLatLngBounds(LatLngBounds, int): Map size can't be 0.
            // Most likely, layout has not yet occured for the map view.
            CameraUpdate update = CameraUpdateFactory.newLatLngBounds(builder.build(), 100);
            mMap.moveCamera(update);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("DriverMapActivity", "onCreate");

        mLayoutComplete = false;
        mIntent = getIntent();
        mRequestId = mIntent.getStringExtra("requestId");
        mRequesterId = mIntent.getStringExtra("requesterId");
        double pickupLatitude = mIntent.getDoubleExtra("pickupLatitude", 0.);
        double pickupLongitude = mIntent.getDoubleExtra("pickupLongitude", 0.);
        mPickupLocation = new LatLng(pickupLatitude, pickupLongitude);
        double driverLatitude = mIntent.getDoubleExtra("driverLatitude", 0.);
        double driverLongitude = mIntent.getDoubleExtra("driverLongitude", 0.);
        mDriverLocation = new LatLng(driverLatitude, driverLongitude);

        // Set a global layout listener which will be called when the layout pass is completed and the view is drawn
        CoordinatorLayout mapLayout = (CoordinatorLayout) getLayoutInflater().inflate(R.layout.activity_driver_map, null);
        mapLayout.getViewTreeObserver().addOnGlobalLayoutListener(this);
        setContentView(mapLayout);

        mFab = (FloatingActionButton) findViewById(R.id.driver_map_fab);

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
