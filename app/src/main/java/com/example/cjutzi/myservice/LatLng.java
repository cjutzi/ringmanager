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

    String          name;
    double          lat;
    double          lng;
    int             triggerDist;
    boolean         factive;
    RING_TYPE       ringType;
    Integer         lastDistMeter;
    float           lastAccuracy;
    int             uniqueInt;              // managed in LocationReciever only (i.e. assigned )
    long            activeTimeSec;


    /**
     *
     * @param name
     * @param lat
     * @param lng
     * @param triggerDist
     * @param factive
     * @param ringType
     */
    public void initLatLng(String name, double lat, double lng, int triggerDist, boolean factive, RING_TYPE ringType )
    {
        this.name           = name;
        this.lat            = lat;
        this.lng            = lng;
        this.triggerDist    = triggerDist;
        this.factive        = factive;
        this.ringType       = ringType;
        this.uniqueInt      = -1;
        this.lastDistMeter  = -1;
        this.activeTimeSec  = 0;
    }
    /**
     *
     * @param name
     * @param lat
     * @param lng
     * @param triggerDist
     */
    LatLng (String name, double lat, double lng, int triggerDist)
    {
        initLatLng(name, lat, lng, triggerDist, true,  FULL);
    }

    /**
     *
     * @param name
     * @param lat
     * @param lng
     * @param triggerDist
     * @param active
     */
    LatLng (String name, double lat, double lng, int triggerDist, boolean active)
    {
        initLatLng(name, lat, lng, triggerDist, active, FULL);
    }
    /**
     *
     * @param name
     * @param lat
     * @param lng
     * @param triggerDist
     */
    LatLng (String name, double lat, double lng, int triggerDist, boolean active, RING_TYPE ringType)
    {
        initLatLng(name, lat, lng, triggerDist, active, ringType);
    }




}