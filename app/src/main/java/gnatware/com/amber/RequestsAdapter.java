package gnatware.com.amber;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pzingg on 1/10/16.
 */
public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {

        // Private member variables accessible from RequestsAdapter
        private TextView mTxtRequesterId;
        private TextView mTxtPickupDistance;

        public ViewHolder(View itemView) {
            super(itemView);

            mTxtRequesterId = (TextView) itemView.findViewById(R.id.txtRequesterId);
            mTxtPickupDistance = (TextView) itemView.findViewById(R.id.txtPickupDistance);
        }

        // Set item view based on the data model
        public void bind(RiderRequest request) {
            mTxtRequesterId.setText(request.requesterId);
            double distance = request.getPickupDistance();
            String strDistance = new DecimalFormat("0.# miles").format(distance);
            mTxtPickupDistance.setText(strDistance);
        }
    }

    // Member variables for requests and Parse query parameters
    private ParseUser mDriver;
    private ParseGeoPoint mDriverLocation;
    private ParseQuery<ParseObject> mQuery;
    private List<RiderRequest> mRequests;

    public RequestsAdapter(ParseUser driver) {
        mDriver = driver;
        mRequests = new ArrayList<RiderRequest>();
        mQuery = new ParseQuery<ParseObject>("Request");
    }

    public void setDriverLocation(double latitude, double longitude) {
        mDriverLocation = new ParseGeoPoint(latitude, longitude);
        updateRequests();
    }

    // Do an async Parse query and cache the results in the adapter's mRequests array
    public void updateRequests() {
        mQuery.whereNotEqualTo("requesterId", mDriver.getObjectId());
        mQuery.whereNear("pickupLocation", mDriverLocation);
        mQuery.findInBackground(new FindCallback<ParseObject>() {

            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e != null) {
                    Log.d("RequestsAdapter", e.getMessage());
                } else {
                    Log.d("RequestsAdapter", "Found " + objects.size() + " nearby requests");
                    mRequests.clear();
                    for (ParseObject request : objects) {
                        mRequests.add(new RiderRequest(
                                request.getString("requesterId"),
                                request.getParseGeoPoint("pickupLocation"),
                                mDriverLocation));
                    }
                    // Update view
                    notifyDataSetChanged();
                }
            }
        });
    }

    // Create a ViewHolder that will display our items
    @Override
    public RequestsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Inflate the item custom layout and return ViewHolder instance
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View requestItem = inflater.inflate(R.layout.item_request, parent, false);
        return new ViewHolder(requestItem);
    }

    // Display the data from a specific item within the ViewHolder
    @Override
    public void onBindViewHolder(RequestsAdapter.ViewHolder viewHolder, int position) {

        // Get request data from cached array and bind it to the ViewHolder
        RiderRequest request = mRequests.get(position);
        Log.d("RequestsAdapter", "onBindViewHolder, request[" + Integer.toString(position) + "]: " +
                request.toString());
        viewHolder.bind(request);
    }

    // Return the total count of items in our cached array
    @Override
    public int getItemCount() {
        int count = mRequests.size();
        Log.d("RequestsAdapter", "getItemCount returning " + Integer.toString(count));
        return count;
    }
}
