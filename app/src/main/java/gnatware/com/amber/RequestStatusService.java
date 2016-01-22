package gnatware.com.amber;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.concurrent.locks.ReentrantLock;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class RequestStatusService extends IntentService {

    private static final String TAG = "RequestStatusService";

    public static final String ACTION_GET_REQUEST_STATUS = "gnatware.com.amber.action.GET_REQUEST_STATUS";

    public static final int RESULT_FLAG_ERROR = 1;
    public static final int RESULT_FLAG_DRIVER = 2;
    public static final int RESULT_FLAG_DRIVER_LOCATION = 4;

    private static final String EXTRA_REQUEST_ID = "gnatware.com.amber.extra.REQUEST_ID";
    private static final String EXTRA_RECEIVER = "gnatware.com.amber.extra.RECEIVER";

    public RequestStatusService() {
        super(TAG);
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startGetRequestStatus(Context context, String requestId) {
        Intent intent = new Intent(context, RequestStatusService.class);
        intent.setAction(ACTION_GET_REQUEST_STATUS);
        intent.putExtra(EXTRA_REQUEST_ID, requestId);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_GET_REQUEST_STATUS.equals(action)) {
                final String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
                handleGetRequestStatus(requestId);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleGetRequestStatus(String requestId) {
        // Synchronous requests
        Boolean success = true;
        String error = null;
        ParseObject request = null;
        ParseUser driver = null;
        ParseGeoPoint location = null;
        try {
            ParseQuery<ParseObject> requestQuery = new ParseQuery<ParseObject>("Request");
            request = requestQuery.get(requestId);
            if (request == null) {
                success = false;
                error = "No request object for id " + requestId;
                Log.d(TAG, error);
            }
        } catch (ParseException e) {
            success = false;
            error = "Cannot get request object for id " + requestId + ": " + e.getMessage();
            Log.d(TAG, error);
        }
        if (request != null) {
            String driverId = request.getString("driverId");
            if (driverId != null) {
                try {
                    ParseQuery<ParseUser> driverQuery = ParseUser.getQuery();
                    driver = driverQuery.get(driverId);
                    if (driver == null) {
                        success = false;
                        error = "Cannot get driver with id " + driverId;
                        Log.d(TAG, error);
                    } else {
                        location = driver.getParseGeoPoint("location");
                        if (location == null) {
                            success = false;
                            error = "Cannot get location for driver " + driverId;
                            Log.d(TAG, error);
                        }
                    }
                } catch (ParseException e) {
                    success = false;
                    error = "Cannot get driver object for id " + driverId + ": " + e.getMessage();
                    Log.d(TAG, error);
                }
            }
        }
        int flags = 0;

        // Construct an Intent tying it to the ACTION
        Intent intent = new Intent(ACTION_GET_REQUEST_STATUS);
        intent.putExtra("resultCode", Activity.RESULT_OK);
        intent.putExtra("requestId", requestId);
        if (!success) {
            intent.putExtra("errorMessage", error);
            flags |= RESULT_FLAG_ERROR;
        }
        if (driver != null) {
            intent.putExtra("driverId", driver.getObjectId());
            flags |= RESULT_FLAG_DRIVER;
            if (location != null) {
                intent.putExtra("latitude", location.getLatitude());
                intent.putExtra("longitude", location.getLatitude());
                flags |= RESULT_FLAG_DRIVER_LOCATION;
            }
        }
        intent.putExtra("flags", flags);

        // Fire the broadcast with intent packaged
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
