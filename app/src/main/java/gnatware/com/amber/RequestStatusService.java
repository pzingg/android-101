package gnatware.com.amber;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

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
    public static final String ACTION_GET_RIDER_REQUEST_STATUS = "gnatware.com.amber.action.GET_RIDER_REQUEST_STATUS";

    public static final int RESULT_FLAG_ERROR = 1;
    public static final int RESULT_FLAG_REQUEST = 2;
    public static final int RESULT_FLAG_REQUESTER = 4;
    public static final int RESULT_FLAG_DRIVER = 8;
    public static final int RESULT_FLAG_DRIVER_LOCATION = 16;

    private static final String EXTRA_REQUEST_ID = "gnatware.com.amber.extra.REQUEST_ID";
    private static final String EXTRA_REQUESTER_ID = "gnatware.com.amber.extra.REQUESTER_ID";
    private static final String EXTRA_RECEIVER = "gnatware.com.amber.extra.RECEIVER";

    public RequestStatusService() {
        super(TAG);
    }

    /**
     * Starts this service to perform an action with the given parameters. If
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

    public static void startGetRiderRequestStatus(Context context, String requesterId) {
        Intent intent = new Intent(context, RequestStatusService.class);
        intent.setAction(ACTION_GET_RIDER_REQUEST_STATUS);
        intent.putExtra(EXTRA_REQUESTER_ID, requesterId);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            Log.d(TAG, "onHandleIntent: null intent");
        } else {
            final String action = intent.getAction();
            Log.d(TAG, "onHandleIntent " + action);
            if (ACTION_GET_REQUEST_STATUS.equals(action)) {
                final String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
                getRequestStatus(requestId);
            } else if (ACTION_GET_RIDER_REQUEST_STATUS.equals(action)) {
                final String requesterId = intent.getStringExtra(EXTRA_REQUESTER_ID);
                getRiderRequestStatus(requesterId);
            }
        }
    }

    /**
     * Handle an action  in the provided background thread with the provided parameters.
     */
    private void getRequestStatus(String requestId) {
        Log.d(TAG, "getRequestStatus for request " + requestId);

        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
        query.whereEqualTo("objectId", requestId);
        query.include("requester");
        query.include("driver");
        performQueryAndSendResult(query, ACTION_GET_REQUEST_STATUS);
    }

    private void getRiderRequestStatus(String requesterId) {
        Log.d(TAG, "getRiderRequestStatus for requester " + requesterId);

        // Nested query - synchronous request
        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
        userQuery.whereEqualTo("objectId", requesterId);

        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
        query.whereMatchesQuery("requester", userQuery);
        query.include("requester");
        query.include("driver");
        performQueryAndSendResult(query, ACTION_GET_RIDER_REQUEST_STATUS);
    }

    private void performQueryAndSendResult(ParseQuery<ParseObject> query, String action) {
        String error = null;
        String requestId = null;
        String requesterId = null;
        String driverId = null;
        ParseGeoPoint location = null;

        ParseObject request = null;
        try {
            request = query.getFirst();
        } catch (ParseException e) {
            error = "Cannot get request object for query " + query.toString() + ": " + e.getMessage();
        }

        if (request == null) {
            if (error == null) {
                error = "No request object for query " + query.toString();
            }
            Log.d(TAG, error);
        } else {
            requestId = request.getObjectId();

            ParseUser requester = request.getParseUser("requester");
            if (requester != null) {
                requesterId = requester.getObjectId();
            }

            ParseUser driver = request.getParseUser("driver");
            if (driver != null) {
                driverId = driver.getObjectId();
                location = driver.getParseGeoPoint("lastLocation");
                if (location == null) {
                    // Not flagged as an error ?
                    Log.d(TAG, "Cannot get location for driver " + driver.getObjectId());
                }
            }
        }

        Log.d(TAG, "Query result: request=" + requestId + ", requester=" + requesterId + ", driver=" + driverId);
        sendResultForAction(action,
                error, requestId, requesterId, driverId, location);
    }

    private void sendResultForAction(String action,
                            String error, String requestId, String requesterId,
                            String driverId, ParseGeoPoint location) {
        int flags = 0;

        // Construct an Intent tying it to the ACTION
        Intent intent = new Intent(action);
        intent.putExtra("resultCode", Activity.RESULT_OK);
        if (error != null) {
            intent.putExtra("errorMessage", error);
            flags |= RESULT_FLAG_ERROR;
        }
        if (requestId != null) {
            intent.putExtra("requestId", requestId);
            flags |= RESULT_FLAG_REQUEST;
            if (requesterId != null) {
                intent.putExtra("requesterId", requesterId);
                flags |= RESULT_FLAG_REQUESTER;
            }
            if (driverId != null) {
                intent.putExtra("driverId", driverId);
                flags |= RESULT_FLAG_DRIVER;
                if (location != null) {
                    intent.putExtra("latitude", location.getLatitude());
                    intent.putExtra("longitude", location.getLongitude());
                    flags |= RESULT_FLAG_DRIVER_LOCATION;
                }
            }
        }
        intent.putExtra("flags", flags);
        Log.d(TAG, "sendResultforAction: broadcasting intent");

        // Fire the broadcast with intent packaged
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
