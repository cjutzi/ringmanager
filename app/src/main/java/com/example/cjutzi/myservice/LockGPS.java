package com.example.cjutzi.myservice;

/**
 * Created by cjutzi on 3/23/18.
 */



public interface LockGPS
{
    public enum MESSAGES {
         GPSLOCKING,
         GPSFIXED,
         NETWORK,
         GPSACCURACY,
         DONE
    }

    public void GPSLocked(LockGPS.MESSAGES zeroIsDoneGpsIsPosNetworkIsNeg);
}
