package com.gnatware.amber;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.design.widget.Snackbar;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.ui.ParseLoginConfig;

import junit.framework.Assert;

import java.util.Date;
import java.util.List;

/**
 * Created by pzingg on 1/9/16.
 */

// A MultiDexApplication.
// Over 64k methods, thanks to Google Maps and Facebook
public class AmberApplication extends MultiDexApplication {

    public static final String LOG_TAG = "AmberApplication";

    public static final int RC_LOG_IN_BUTTON_CLICKED = 1001;
    public static final int RC_USER_REQUIRES_AUTHENTICATION = 1002;
    public static final int RC_ACTION_REQUIRES_AUTHENTICATION = 1003;

    private LocationManager mLocationManager;
    private String mProvider;
    private LatLng mDefaultLocation;

    // TODO: Warning in logcat - GoogleService failed to initialize
    // Missing an expected resource: 'R.string.google_app_id' for initializing Google services.
    // Possible causes are missing google-services.json
    // or com.google.gms.google-services gradle plugin.

    // TODO: Use ParseLoginUI for sign up process
    // http://blog.parse.com/learn/engineering/login-love-for-your-android-app/

    // TODO: Convert Parse anonymous user to signed up user
    // http://stackoverflow.com/questions/27595057/converting-an-anonymous-user-to-a-regular-user-and-saving

    // TODO: Enable push notifications when requests are canceled
    // https://www.parse.com/tutorials/android-push-notifications

    static public void startSignInActivityForResult(Activity from, int requestCode) {
        Log.d(LOG_TAG, "startSignInActivity");

        // Reuse Parse config for our sign-in fragments
        ParseLoginConfig config = new ParseLoginConfig();
        config.setParseLoginEnabled(true);
        config.setParseLoginEmailAsUsername(true);
        config.setParseSignupMinPasswordLength(6);
        config.setFacebookLoginEnabled(true);
        config.setTwitterLoginEnabled(true);

        Intent signInIntent = new Intent(from, SignInActivity.class);
        signInIntent.putExtras(config.toBundle());

        Log.d(LOG_TAG, "starting SignInActivity with requestCode " + String.valueOf(requestCode));
        from.startActivityForResult(signInIntent, requestCode);
    }

    static public void askUserToSignIn(
            final Activity from, final int requestCode,
            final int dialogMessageId, final View snackContainer, final int cancelMessageId) {

        AlertDialog.Builder builder = new AlertDialog.Builder(from);
        builder.setMessage(dialogMessageId)
            .setTitle(R.string.login_required_title)
            .setPositiveButton("Sign in", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    startSignInActivityForResult(from, requestCode);
                }
            });

        if (snackContainer != null && cancelMessageId != 0) {
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    // User cancelled the dialog - show cancel message
                    Snackbar snackbar = Snackbar.make(snackContainer, cancelMessageId,
                            Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            });
        }
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    static public boolean validUserOrRequestSignIn(
            ParseUser user, Activity from, int dialogMessageId,
            final View snackContainer, int cancelMessageId) {
        if (user == null) {
            Log.d(LOG_TAG, "No user");
            return false;
        }

        int requestCode = 0;
        boolean valid = false;
        if (ParseAnonymousUtils.isLinked(user)) {
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
            Log.d(LOG_TAG, "Anonymous user - require sign up");
            requestCode = RC_ACTION_REQUIRES_AUTHENTICATION;
        } else if (!user.isAuthenticated()) {
            Log.d(LOG_TAG, "Unauthenticated user - require log in");
            requestCode = RC_USER_REQUIRES_AUTHENTICATION;
        } else {
            valid = true;
        }
        if (!valid) {
            askUserToSignIn(from, requestCode, dialogMessageId, snackContainer, cancelMessageId);
        }
        return valid;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Check our keys
        assertGoogleMapsKey();

        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);

        // Add your initialization code here
        Parse.initialize(this);

        // Allow anonymous users and save one if it's created
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
        try {
            mLocationManager.removeUpdates(listener);
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "removeLocationUpdates: Location permission not granted");
        }
    }

    public void requestLocationUpdates(LocationListener listener) {

        // Updates every 400 ms, or 1 degree change
        try {
            mLocationManager.requestLocationUpdates(mProvider, 400, 1, listener);
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "requestLocationUpdates: Location permission not granted");
        }
    }

    public Location getLastKnownLocation() {
        Location location = null;
        try {
            location = mLocationManager.getLastKnownLocation(mProvider);
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "getLastKnownLocation: Location permission not granted");
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
            driver.put("lastLocation", new ParseGeoPoint(location.latitude, location.longitude));
            driver.put("lastLocationAt", new Date());
            driver.saveEventually();
        } else {
            Log.e(LOG_TAG, "updateDriverLocation: Current user is not a driver");
        }
    }

    // For debugging purposes
    public void cancelAllActiveRequests() {
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
        query.whereDoesNotExist("canceledAt");
        query.whereExists("driver");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> requests, ParseException e) {
                Date now = new Date();
                for (ParseObject request : requests) {
                    final String requestId = request.getObjectId();
                    request.put("canceledAt", now);
                    request.put("cancellationReason", "Debugging");
                    request.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            Log.d(LOG_TAG, "Request " + requestId + " canceled for debugging");
                            // TODO: Notify rider and driver (push notification?)
                        }
                    });
                }
            }
        });
    }

    private void assertGoogleMapsKey() {

        // All Google Maps keys are supposed to start with "AIza"
        // Make sure we can read this resource and it has the right prefix
        Log.d(LOG_TAG, "Checking Google Maps key");
        String key = getString(R.string.google_maps_key);
        Assert.assertEquals("AIza", key.substring(0, 4));
    }
}
