package com.gnatware.amber;

import com.parse.ParseGeoPoint;

/**
 * Created by pzingg on 1/10/16.
 */
public class RiderRequest {

    public String objectId;
    public Boolean accepted;
    public String requesterId;
    public ParseGeoPoint pickupLocation;
    public ParseGeoPoint driverLocation;

    public RiderRequest(String objectId, Boolean accepted, String requesterId,
                        ParseGeoPoint pickupLocation, ParseGeoPoint driverLocation) {
        this.objectId = objectId;
        this.accepted = accepted;
        this.requesterId = requesterId;
        this.pickupLocation = pickupLocation;
        this.driverLocation = driverLocation;
    }

    public double getPickupDistance() {
        return driverLocation.distanceInMilesTo(pickupLocation);
    }
}
