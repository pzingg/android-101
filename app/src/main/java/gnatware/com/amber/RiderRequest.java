package gnatware.com.amber;

import com.parse.ParseGeoPoint;

/**
 * Created by pzingg on 1/10/16.
 */
public class RiderRequest {

    public String requesterId;
    public ParseGeoPoint pickupLocation;
    public ParseGeoPoint driverLocation;

    public RiderRequest(String requesterId,
                        ParseGeoPoint pickupLocation, ParseGeoPoint driverLocation) {
        this.requesterId = requesterId;
        this.pickupLocation = pickupLocation;
        this.driverLocation = driverLocation;
    }

    public double getPickupDistance() {
        return driverLocation.distanceInMilesTo(pickupLocation);
    }
}
