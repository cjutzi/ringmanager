package com.example.cjutzi.myservice;

import static com.example.cjutzi.myservice.LatLng.RING_TYPE.FULL;

/**
 * Created by cjutzi on 11/22/17.
 */

class LatLng implements java.io.Serializable
{

    static final long serialVersionUID = 1;


    static public enum  RING_TYPE
    {
        FULL,
        VIBRATE,
    }


    String      name;
    double      lat;
    double      lng;
    int         triggerDist;
    boolean     factive;
    RING_TYPE   ringType;
    Integer     lastDistMeter;
    float       lastAccuracy;
    int         uniqueInt;
    long        activeTime;
    long        idleTime;

    /**
     *
     * @param name
     * @param lat
     * @param lng
     * @param triggerDist
     */
    LatLng (String name, double lat, double lng, int triggerDist, int uniqueInt)
    {
        this.name = name;
        this.lat  = lat;
        this.lng  = lng;
        this.triggerDist = triggerDist;
        this.factive = true;
        this.lastDistMeter = -1;
        ringType = FULL;
        this.uniqueInt = uniqueInt;
        long activeTime = 0;
    }
    /**
     *
     * @param name
     * @param lat
     * @param lng
     * @param triggerDist
     */
    LatLng (String name, double lat, double lng, int triggerDist, boolean active, int uniqueInt)
    {
        this.name = name;
        this.lat  = lat;
        this.lng  = lng;
        this.triggerDist = triggerDist;
        this.factive = active;
        this.lastDistMeter = -1;
        this.uniqueInt = uniqueInt;
        long activeTime = 0;
    }

    /**
     *
     * @param name
     * @param lat
     * @param lng
     * @param triggerDist
     * @param active
     * @param uniqueInt
     * @param accuracy
     */
    LatLng (String name, double lat, double lng, int triggerDist, boolean active, int uniqueInt, float accuracy)
    {
        this.name = name;
        this.lat  = lat;
        this.lng  = lng;
        this.triggerDist = triggerDist;
        this.factive = active;
        this.lastDistMeter = -1;
        this.uniqueInt = uniqueInt;
        this.lastAccuracy = accuracy;
        long activeTime = 0;
    }

    /**
     *
     * @param name
     * @param lat
     * @param lng
     * @param triggerDist
     * @param ringType
     */
    LatLng (String name, double lat, double lng, int triggerDist, boolean active, RING_TYPE ringType, int uniqueInt)
    {
        this.name = name;
        this.lat  = lat;
        this.lng  = lng;
        this.triggerDist = triggerDist;
        this.factive = active;
        this.ringType = ringType;
        this.lastDistMeter = -1;
        this.uniqueInt = uniqueInt;
        long activeTime = 0;
    }

}