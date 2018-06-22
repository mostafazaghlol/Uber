package com.mostafa.android.uber.HistoryStaff;

/**
 * Created by mostafa on 6/20/18.
 */

public class History {
    private String rideId,rideTime;

    public String getRideId() {
        return rideId;
    }

    public String getRideTime() {
        return rideTime;
    }

    public History(String mRideId,String mRideTime){
        this.rideId = mRideId;
        this.rideTime = mRideTime;
    }
}
