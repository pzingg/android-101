package com.gnatware.amber;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by pzingg on 1/10/16.
 */

// Adapter for RecyclerView
public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> {

    public static final String TAG = "RequestsAdapter";

    // Two different tile layouts depending on whether request has been accepted or not.
    public static final int VIEW_TYPE_AVAILABLE_REQUEST = 1;
    public static final int VIEW_TYPE_ACCEPTED_REQUEST = 2;

    // Member variables for requests and Parse query parameters
    private ParseUser mDriver;
    private DriverRequestsActivity mActivity;
    private ParseGeoPoint mDriverLocation;
    private List<RiderRequest> mRequests;

    // Static inner class. ViewHolder insstances are created in outer class's
    // onCreateViewHolder method.
    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public static final String TAG = "RAViewHolder";

        private RequestsAdapter mAdapter;

        // Private member variables accessible from RequestsAdapter
        private int mViewType;
        private TextView mTxtRequesterId;
        private TextView mTxtPickupDistance;

        // Bound data member
        private RiderRequest mRequest;

        public ViewHolder(RequestsAdapter adapter, View itemView, int viewType) {
            super(itemView);

            mAdapter = adapter;
            mViewType = viewType;
            mTxtRequesterId = (TextView) itemView.findViewById(R.id.txtRequesterId);
            if (viewType == VIEW_TYPE_ACCEPTED_REQUEST) {
                mTxtPickupDistance = (TextView) itemView.findViewById(R.id.txtPickupDistance);
            }

            itemView.setOnClickListener(this);
        }

        public Boolean hasAcceptedRequest() {
            return mViewType == VIEW_TYPE_ACCEPTED_REQUEST;
        }

        // Set item view based on the data model
        public void bind(RiderRequest request) {

            // Save information for click handler
            mRequest = request;

            // Set up the sub views (user and distance)
            mTxtRequesterId.setText(request.requesterId);

            if (!request.accepted) {
                if (mTxtPickupDistance == null) {
                    Log.e(TAG, "Wrong view type for available request?");
                } else {
                    double distance = request.getPickupDistance();
                    String strDistance = new DecimalFormat("0.# miles").format(distance);
                    mTxtPickupDistance.setText(strDistance);
                }
            }
        }

        @Override
        public void onClick(View view) {
            Log.d(TAG, "onClick for " + mRequest.objectId);

            if (hasAcceptedRequest()) {
                Log.d(TAG, "Canceling request");
                mAdapter.cancelRequest(mRequest.objectId);
            } else {
                Log.d(TAG, "Starting DriverMapActivity");

                // Do something with the rider request
                Context context = view.getContext();
                Intent driverMapIntent = new Intent(context, DriverMapActivity.class);
                driverMapIntent.putExtra("requestId", mRequest.objectId);
                driverMapIntent.putExtra("requesterId", mRequest.requesterId);
                driverMapIntent.putExtra("pickupLatitude", mRequest.pickupLocation.getLatitude());
                driverMapIntent.putExtra("pickupLongitude", mRequest.pickupLocation.getLongitude());
                driverMapIntent.putExtra("driverLatitude", mRequest.driverLocation.getLatitude());
                driverMapIntent.putExtra("driverLongitude", mRequest.driverLocation.getLongitude());

                context.startActivity(driverMapIntent);
            }
        }
    }

    public RequestsAdapter(DriverRequestsActivity activity, ParseUser driver) {
        mActivity = activity;
        mDriver = driver;
        mRequests = new ArrayList<RiderRequest>();
    }

    @Override
    public int getItemViewType(int position) {
        int viewType = VIEW_TYPE_AVAILABLE_REQUEST;
        if (mRequests.size() > position) {
            RiderRequest request = mRequests.get(position);
            if (request.accepted) {
                viewType = VIEW_TYPE_ACCEPTED_REQUEST;
            }
        }
        return viewType;
    }

    @Override
    public RequestsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Inflate the item custom layout and return ViewHolder instance
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        int itemId = viewType == VIEW_TYPE_ACCEPTED_REQUEST ?
                R.layout.tile_accepted_request : R.layout.tile_available_request;

        View requestItem = inflater.inflate(itemId, parent, false);
        return new ViewHolder(this, requestItem, viewType);
    }

    // Display the data from a specific item within the ViewHolder
    @Override
    public void onBindViewHolder(RequestsAdapter.ViewHolder viewHolder, int position) {

        // Get request data from cached array and bind it to the ViewHolder
        RiderRequest request = mRequests.get(position);
        Log.d(TAG, "onBindViewHolder, request[" + Integer.toString(position) + "]: " +
                request.toString());
        viewHolder.bind(request);
    }

    // Return the total count of items in our cached array
    @Override
    public int getItemCount() {
         return mRequests.size();
    }

    // Public methods
    // Public access for DriverRequestsActivity
    public void updateDriverLocation(LatLng location) {
        mDriverLocation = new ParseGeoPoint(location.latitude, location.longitude);
        updateRequests();
    }

    // Private methods

    // Do an async Parse query and cache the results in the adapter's mRequests array
    private void updateRequests() {
        final ParseQuery<ParseObject> queryAccepted = new ParseQuery<ParseObject>("Request");
        queryAccepted.whereDoesNotExist("canceledAt");
        queryAccepted.whereEqualTo("driver", mDriver);
        queryAccepted.getFirstInBackground(new GetCallback<ParseObject>() {

            @Override
            public void done(ParseObject request, ParseException e1) {
                if (e1 != null) {
                    Log.d(TAG, e1.getMessage());
                } else if (request != null) {
                    Log.d(TAG, "Found accepted request");
                    mRequests.clear();
                    mRequests.add(new RiderRequest(
                            request.getObjectId(),
                            true,
                            request.getParseUser("requester").getObjectId(),
                            request.getParseGeoPoint("pickupLocation"),
                            mDriverLocation));
                    // Update view
                    mActivity.setTitle("Your Active Request");
                    notifyDataSetChanged();
                } else {
                    ParseQuery<ParseObject> queryAvailable = new ParseQuery<ParseObject>("Request");
                    queryAvailable.whereDoesNotExist("canceledAt");
                    queryAvailable.whereDoesNotExist("driver");
                    queryAvailable.whereNotEqualTo("requester", mDriver);
                    queryAvailable.whereNear("pickupLocation", mDriverLocation);
                    queryAvailable.setLimit(100);
                    queryAvailable.findInBackground(new FindCallback<ParseObject>() {

                        @Override
                        public void done(List<ParseObject> requests, ParseException e2) {
                            if (e2 != null) {
                                Log.d(TAG, e2.getMessage());
                            } else {
                                Log.d(TAG, "Found " + requests.size() + " nearby requests");
                                mRequests.clear();
                                for (ParseObject request : requests) {
                                    mRequests.add(new RiderRequest(
                                            request.getObjectId(),
                                            false,
                                            request.getParseUser("requester").getObjectId(),
                                            request.getParseGeoPoint("pickupLocation"),
                                            mDriverLocation));
                                }
                                // Update view
                                mActivity.setTitle("Nearby Requests");
                                notifyDataSetChanged();
                            }
                        }
                    });
                }
            }
        });
    }

    private void cancelRequest(final String requestId) {
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
        query.getInBackground(requestId, new GetCallback<ParseObject>() {

            @Override
            public void done(ParseObject request, ParseException e) {
                if (request == null) {
                    Log.d(TAG, "Could not get request " + requestId);
                } else {
                    Date now = new Date();
                    request.put("canceledAt", now);
                    request.put("cancellationReason", "Canceled by driver...");
                    Log.d(TAG, "Canceling request " + requestId);
                    request.saveInBackground(new SaveCallback() {

                        @Override
                        public void done(ParseException e) {
                            Log.d(TAG, "Request " + requestId + " canceled by driver");

                            // TODO: Notify rider (push notification?)
                            mActivity.showSnack("Request canceled");
                            updateRequests();
                        }
                    });
                }
            }
        });
    }
}
