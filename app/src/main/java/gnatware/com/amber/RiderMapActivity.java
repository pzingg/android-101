package gnatware.com.amber;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
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
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.ParseACL;
import com.parse.SaveCallback;
import com.parse.ui.ParseLoginBuilder;

import junit.framework.Assert;

import java.util.Date;
import java.util.List;

public class RiderMapActivity extends AppCompatActivity implements
        ViewTreeObserver.OnGlobalLayoutListener, LocationListener, OnMapReadyCallback {

    public static final String TAG = "RiderMapActivity";
    public static final String ACTION_REQUEST_STATUS_ALARM = "gnatware.com.amber.action.REQUEST_STATUS_ALARM";

    public static final Integer REQUEST_NONE = 0;
    public static final Integer REQUEST_ACTIVE = 1;
    public static final Integer REQUEST_ASSIGNED = 2;
    private static final String[] REQUEST_STATES = {
            "NONE", "ACTIVE", "ASSIGNED"
    };

    private AmberApplication mApplication;
    private RequestStatusReceiver mStatusReceiver;
    private AlarmManager mAlarmManager;
    private PendingIntent mStatusAlarmIntent;

    // Dynamic UI elements
    private FloatingActionButton mFab;
    private CoordinatorLayout mLayout;

    private Boolean mLayoutComplete;

    // Current rider request status
    private String mRequestId;
    private String mDriverId;
    private LatLng mDriverLocation;

    // Location and map updates
    private GoogleMap mMap;
    private LatLng mRiderLocation;

    // Static method
    public static void checkPendingRequest(Context context, String logMessage) {
        Log.d(TAG, "checkPendingRequest (" + logMessage + ")");

        ParseUser requester = ParseUser.getCurrentUser();
        if (requester != null) {
            Log.d(TAG, "Starting service for requester " + requester.getObjectId());
            RequestStatusService.startGetRiderRequestStatus(context, requester.getObjectId());
        } else {
            Log.d(TAG, "No current user!");
        }
    }

    // To be an inner class, must be declared static, with a zero-argment constructor.
    // Therefore this class cannot access any non-static members of the containing class.
    // The receiver class name is registered in AndroidManifest.xml and the receiver is
    // constructed by the pending intent ActivityThread.handleReceiver method, or else you get:
    // "java.lang.InstantiationException: class has no zero argument constructor".
    public static class RequestStatusAlarmReceiver extends BroadcastReceiver {

        private static final String TAG = "RSAlarmReceiver";

        public RequestStatusAlarmReceiver() {
        }

        // Handle the repeating alarm
        @Override
        public void onReceive(Context context, Intent intent) {
            long now = SystemClock.elapsedRealtime();
            Log.d(TAG, "onReceive, time=" + String.valueOf(now));
            checkPendingRequest(context, "onReceive");
        }
    }

    // Handle the result of a getRequestStatus service
    public class RequestStatusReceiver extends BroadcastReceiver {

        private static final String TAG = "RequestStatusReceiver";

        public RequestStatusReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // This method is called when the BroadcastReceiver is receiving
            // an Intent broadcast.
            int flags = intent.getIntExtra("flags", 0);
            if (0 != (flags & RequestStatusService.RESULT_FLAG_ERROR)) {
                Log.d(TAG, "onReceive: Error result: " + intent.getStringExtra("errorMessage"));
            } else {
                mRequestId = null;
                mDriverId = null;
                mDriverLocation = null;
                if (0 != (flags & RequestStatusService.RESULT_FLAG_REQUEST)) {
                    mRequestId = intent.getStringExtra("requestId");
                    if (0 != (flags & RequestStatusService.RESULT_FLAG_DRIVER)) {
                        mDriverId = intent.getStringExtra("driverId");
                        if (0 != (flags & RequestStatusService.RESULT_FLAG_DRIVER_LOCATION)) {
                            double latitude = intent.getDoubleExtra("latitude", 1000.);
                            double longitude = intent.getDoubleExtra("longitude", 1000.);
                            if (latitude < 200. && longitude < 200.) {
                                mDriverLocation = new LatLng(latitude, longitude);
                            }
                        }
                    }
                }
                Log.d(TAG, "onReceive: Parsed result: request=" +
                        (mRequestId == null ? "null" : mRequestId) + ", driver=" +
                        (mDriverId == null ? "null" : mDriverId) + ", location=" +
                        (mDriverLocation == null ? "null" : mDriverLocation.toString()));
                updateRequestStateUI(null);
                updateMap();
            }
        }
    }

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

        cancelRequestStatusAlarm();
        mApplication.removeLocationUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        mApplication.requestLocationUpdates(this);
        scheduleRequestStatusAlarm("onResume");
        checkPendingRequest(this, "onResume");
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

    private void initializeState() {
        mLayoutComplete = false;

        mApplication = (AmberApplication) getApplication();
        mStatusReceiver = new RequestStatusReceiver();
        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Create the pending intent from our receiver
        // requestCode = 0
        // flags = 0
        Intent intent = new Intent(this, RequestStatusAlarmReceiver.class);
        mStatusAlarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        // Set a global layout listener which will be called when the layout pass is completed and the view is drawn
        mLayout = (CoordinatorLayout) getLayoutInflater().inflate(R.layout.activity_rider_map, null);
        mLayout.getViewTreeObserver().addOnGlobalLayoutListener(this);
        setContentView(mLayout);

        mFab = (FloatingActionButton) findViewById(R.id.rider_map_fab);
        mFab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, "FAB clicked");
                if (mRequestId != null) {
                    cancelRequest();
                } else {
                    postRequest();
                }
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.rider_map);
        mapFragment.getMapAsync(this);
    }

    public void showSnack(String message) {
        Snackbar snackbar = Snackbar.make(mLayout, message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    private void updateMap() {
        if (mMap == null) {
            Log.d(TAG, "No map");
        } else if (mRiderLocation != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(mRiderLocation)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            if (mDriverId != null && mDriverLocation != null) {
                if (!mLayoutComplete) {
                    Log.d(TAG, "Layout incomplete");
                    // Fall through to center rider on map
                } else {
                    Log.d(TAG, "Locate driver and rider on map");
                    // Log.d(TAG, "   riderLocation=" + mRiderLocation.toString());
                    // Log.d(TAG, "  driverLocation=" + mDriverLocation.toString());

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

    private void updateRiderLocation(Location location) {
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

    private void postRequest() {
        Log.d(TAG, "postRequest");
        ParseUser requester = ParseUser.getCurrentUser();
        if (requester == null) {
            Log.d(TAG, "No user");
        } else if (ParseAnonymousUtils.isLinked(requester)) {

            // You can convert an anonymous user into a regular user by setting
            // the username and password, then calling signUp(), or by logging in or
            // linking with a service like Facebook or Twitter. The converted user will
            // retain all of its data. To determine whether the current user is an
            // anonymous user, you can check ParseAnonymousUtils.isLinked()

            // App Settings note: If we currently have a saved anonymous user, and the
            // user creates a new account through the Parse sign up UI, it's important that
            // we DISABLE "Revoke session on password change" in the Users > User Sessions
            // area of App Settings for the application on parse.com.
            // The sign up process changes two fields on the user:
            //     The authData field, previously '{"anonymous":...}', is now null, and
            //     The password field is changed from a random one to the new (encrypted) password.
            // Unless the "Revoke session" setting is disabled, the password change
            // will remove the session, and the next Parse query we run will fail with an
            // "invalid session token" exception, requiring a log out and log in.
            Log.d(TAG, "Anonymous user - require sign up");
            signUpUser();
        } else if (!requester.isAuthenticated()) {
            Log.d(TAG, "Unauthenticated user - require log in");
            logInUser();
        } else {
            // Drivers and requesters all need to see requests
            ParseACL acl = new ParseACL();
            acl.setPublicReadAccess(true);
            acl.setPublicWriteAccess(true);

            ParseGeoPoint pickupLocation = new ParseGeoPoint(
                    mRiderLocation.latitude, mRiderLocation.longitude);

            final ParseObject request = new ParseObject("Request");
            request.setACL(acl);
            request.put("requester", requester);
            request.put("pickupLocation", pickupLocation);
            request.put("requestedAt", new Date());

            Log.d(TAG, "Posting request for user " + requester.getObjectId() +
                    " at " + pickupLocation.toString());
            request.saveInBackground(new SaveCallback() {

                @Override
                public void done(ParseException e) {
                    String status = null;
                    if (e != null) {
                        Log.d(TAG, "Error posting request: " + e.getMessage());
                        status = "Error posting request!";
                    }
                    updateRequestStateUI(status);
                }
            });
        }
    }

    private void cancelRequest() {
        Log.d(TAG, "Cancel request");
        final ParseUser requester = ParseUser.getCurrentUser();

        if (requester != null) {
                ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
                query.whereEqualTo("requester", requester);
                query.whereDoesNotExist("canceledAt");

                Log.d(TAG, "Finding requests for user " + requester.getObjectId());
                query.findInBackground(new FindCallback<ParseObject>() {

                    @Override
                    public void done(List<ParseObject> requests, ParseException e1) {
                        if (e1 != null) {
                            Log.d(TAG, "Error canceling request: " + e1.getMessage());
                            updateRequestStateUI("Error canceling request");
                        } else {
                            if (requests.isEmpty()) {
                                Log.d(TAG, "Error canceling request: No pending requests");
                                resetRequestState();
                                updateRequestStateUI("No pending requests");
                            } else {
                                Log.d(TAG, "Canceling " + Integer.toString(requests.size()) +
                                        " request(s) for user " + requester.getObjectId());
                                resetRequestState();
                                updateRequestStateUI("Request canceled");
                                Date now = new Date();
                                for (final ParseObject request : requests) {
                                    final String requestId = request.getObjectId();
                                    request.put("canceledAt", now);
                                    request.put("cancellationReason", "Canceled by rider...");
                                    request.saveInBackground(new SaveCallback() {

                                        @Override
                                        public void done(ParseException e) {
                                            Log.d(TAG, "Request " + requestId + " canceled by rider");
                                            // TODO: Notify driver (push notification?)
                                        }
                                    });
                                }
                            }
                        }
                    }
                });
                return;
        }
        Log.d(TAG, "Error canceling request: No anonymous user");
    }

    private int getRequestState() {
        return (mRequestId == null) ?
            REQUEST_NONE : ((mDriverId == null) ? REQUEST_ACTIVE : REQUEST_ASSIGNED);
    }

    private void resetRequestState() {
        mRequestId = null;
        mDriverId = null;
        mDriverLocation = null;
    }

    private void updateRequestStateUI(String status) {
        int requestState = getRequestState();
        Log.d(TAG, "updateRequestStateUI " + REQUEST_STATES[requestState]);
        if (requestState != REQUEST_NONE) {
            mFab.setImageResource(R.drawable.ic_clear_black_24dp);
            mFab.setContentDescription("Cancel your request");
            if (status == null) {
                status = "Finding a driver.  Click to cancel.";
            }
        } else {
            mFab.setImageResource(R.drawable.ic_add_black_24dp);
            mFab.setContentDescription("Request a driver");
            if (status == null) {
                status = "Click to request a driver...";
            }
        }
        showSnack(status);
    }

    // Setup a recurring alarm every 5 seconds
    private void scheduleRequestStatusAlarm(String logMessage) {
        Assert.assertNotNull(mStatusReceiver);
        Assert.assertNotNull(mAlarmManager);
        Assert.assertNotNull(mStatusAlarmIntent);
        Log.d(TAG, "scheduleRequestStatusAlarm (" + logMessage + ")");

        // Set the result receiver
        IntentFilter filter = new IntentFilter(RequestStatusService.ACTION_GET_RIDER_REQUEST_STATUS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mStatusReceiver, filter);

        // Set the repeating alarm
        long interval = 5000;
        long now = SystemClock.elapsedRealtime();
        Log.d(TAG, "Setting alarm, time=" + String.valueOf(now));
        mAlarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME, interval, interval, mStatusAlarmIntent);
    }

    // Cancel the pending intent
    private void cancelRequestStatusAlarm() {
        Assert.assertNotNull(mStatusReceiver);
        Assert.assertNotNull(mAlarmManager);
        Assert.assertNotNull(mStatusAlarmIntent);

        Log.d(TAG, "Canceling 5 second repeating alarm");
        mAlarmManager.cancel(mStatusAlarmIntent);

        Log.d(TAG, "Unregistering getRequestStatus receiver");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mStatusReceiver);
    }

    private void signUpUser() {
        ParseLoginBuilder builder = new ParseLoginBuilder(this);
        builder.setParseLoginButtonText("Log in as another user");
        startActivityForResult(builder.build(), 0);
    }

    private void logInUser() {
        ParseLoginBuilder builder = new ParseLoginBuilder(this);
        startActivityForResult(builder.build(), 0);
    }
}

