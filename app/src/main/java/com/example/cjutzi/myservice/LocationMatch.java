package com.example.cjutzi.myservice;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by cjutzi on 11/22/17.
 */

public class LocationMatch
{
    static String DEBUG_TAG = "LocationMatch";
    static int m_lastUniqueUsed = 100;

    static HashMap<String, Object> m_locationList = new HashMap<String, Object>();
    static Context m_context = null;

    static
    {
        m_locationList.put("Living Savior", new LatLng("Living Savior", 45.3749701, -122.7668027, 200, true, LatLng.RING_TYPE.VIBRATE, 1));
        m_locationList.put("Intel JF", new LatLng("Intel JF", 45.543054, -122.960508, 30, true, LatLng.RING_TYPE.FULL,2));
        m_locationList.put("Home", new LatLng("Home", 45.410034, -122.710750, 100, true, LatLng.RING_TYPE.FULL, 3));
        m_locationList.put("Zupan's Market", new LatLng("Zupan's Market", 45.407277, -122.7250246, 50, true, LatLng.RING_TYPE.FULL,4 ));
        m_locationList.put("SJC JetCenter", new LatLng("SJC JetCenter", 37.3591078, -121.932838, 50, true, LatLng.RING_TYPE.VIBRATE, 5));
        m_locationList.put("KHIO", new LatLng("KHIO", 45.5398, -122.9473, 200, true, LatLng.RING_TYPE.FULL, 6));
    }

    /**
     * @param context
     */
    LocationMatch(Context context)
    {
        m_context = context;
        restoreStuff();
    }

    /**
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @param unit 'K' kilo, 'N' Nautical miles, 'M' statute Miles, 'm' meters
     * @return
     */
    public static double distance(double lat1, double lon1, double lat2, double lon2, char unit)
    {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        if (unit == 'K')
        {
            dist = dist * 1.609344;
        }
        else
         if (unit == 'm')
        {
            dist = dist * 1.609344 * 1000.0;
        }
        else
        if (unit == 'N')
        {
            dist = dist * 0.8684;
        }
        return (dist);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts decimal degrees to radians             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts radians to decimal degrees             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }


    /**
     * @return
     */
    public static ArrayList<LatLng> getLocaitons()
    {
        ArrayList retVal = new ArrayList<LatLng>();
        for (String key : m_locationList.keySet())
        {
            retVal.add(m_locationList.get(key));
        }
        return retVal;
    }

    /**
     *
     * @param lat
     * @param lng
     */
    private static void calcListDistance (double lat, double lng, float accuracy)
    {
        for (String key : m_locationList.keySet())
        {
            LatLng latLng = (LatLng) m_locationList.get(key);
            //Log.i(DEBUG_TAG,latLng.name+" lat:"+latLng.lat+" lng:"+latLng.lng+" loc lat:"+lat+" lng:"+lng);

            double dist = distance(lat, lng, latLng.lat, latLng.lng, 'm');

            latLng.lastDistMeter = (int) dist;
            m_locationList.put(latLng.name, latLng); // ??

            Log.i(DEBUG_TAG, "calcListDistance() : Distance from (" + key + ") is (" + dist + ") in meters");
        }
    }

    /**
     *
     * @param name
     * @param activeTime
     */
    public static void addActiveTime (String name, long activeTime)
    {
        LatLng latLng = getLocationBuyKey(name);
        if (latLng != null)
        {
            latLng.activeTime += activeTime;
            saveStuff();
        }
    }

    /**
     * @return
     */
    public static ArrayList<LatLng> getLocationWithDistance(double lat, double lng, float accuracy)
    {
        calcListDistance(lat, lng, accuracy );
        ArrayList retVal = new ArrayList<LatLng>();

        for (String key : m_locationList.keySet())
        {
            retVal.add(m_locationList.get(key));
        }
        return retVal;
    }

    /**
     * @param lat current locaiton
     * @param lng current location
     * @return
     */
    public static ArrayList<LatLng> getActive(double lat, double lng, float accuracy)
    {
        calcListDistance(lat, lng, accuracy);
        ArrayList<LatLng> m_activeKeyList = new ArrayList<LatLng>();

        for (String key : m_locationList.keySet())
        {
            LatLng latLng = (LatLng)m_locationList.get(key);

            if (latLng.lastDistMeter < latLng.triggerDist + accuracy)
            {
                m_activeKeyList.add(latLng);
            }
        }
        return m_activeKeyList;
    }
    /**
     *
     * @param lat
     * @param lng
     * @param accuracy
     * @return
     */
    public static LatLng getClosestActive(double lat, double lng, float accuracy)
    {
        ArrayList<LatLng> activeKeyList = getActive(lat,lng, accuracy);
        if (activeKeyList.size() == 0)
            return null;

        if (activeKeyList.size() == 1)
            return (activeKeyList.get(0));

        LatLng closestLatLng = activeKeyList.get(0);

        for (LatLng latLng : activeKeyList)
        {
            if ((latLng.lastDistMeter ) < closestLatLng.lastDistMeter)
                closestLatLng = latLng;
        }
        return closestLatLng;
    }
    /**
     * @param lat current locaiton
     * @param lng current location
     * @return
     */
    public static LatLng getClosest(double lat, double lng, float accuracy)
    {
        calcListDistance(lat, lng, accuracy);
        LatLng closestLatLng = null;

        for (String key : m_locationList.keySet())
        {
            LatLng latLng = (LatLng) m_locationList.get(key);

            if (closestLatLng == null || latLng.lastDistMeter < closestLatLng.lastDistMeter)
            {
                closestLatLng = latLng;
            }
        }
        if (closestLatLng != null)
            Log.i(DEBUG_TAG, "getClosest() : Distance from (" + closestLatLng.name + ") is (" + closestLatLng.lastDistMeter + ") in meters");

        return closestLatLng;
    }


    /**
     * @param name
     * @param triggerDist
     * @param factive
     * @param ringType
     * @return
     */
    public static boolean addLocation(String name,
                                      int triggerDist,
                                      boolean factive,
                                      float lat, float lng,
                                      LatLng.RING_TYPE ringType)
    {
        if (lat == 0.0 && lng == 0.0)
            return false;
        LatLng   latLng = new LatLng(name, (double)lat, (double)lng, triggerDist, true, ringType, 0);

        addLocation(name, triggerDist, factive, latLng, latLng.ringType);
        return true;
    }
    /**
     *
     * @param name
     * @param triggerDist
     * @param factive
     * @param latLng - will be over written with values in parameters
     * @param ringType
     * @return
     */
    public static LatLng addLocation(String name,
                                     int triggerDist,
                                     boolean factive,
                                     LatLng latLng,
                                     LatLng.RING_TYPE ringType)
    {
        latLng.triggerDist = triggerDist;
        latLng.name = name;
        latLng.ringType = ringType;
        m_locationList.put(name, latLng);
        latLng.uniqueInt = m_lastUniqueUsed++;
        saveStuff();
        return latLng;
    }


    /**
     *
     * @param location
     * @param ringType
     * @return
     */
    public static LatLng updateLocation(String location, LatLng.RING_TYPE ringType)
    {
        for (String key : m_locationList.keySet())
        {
            LatLng latLng = (LatLng) m_locationList.get(key);

            if (location.equals( latLng.name ))
            {
                if (latLng.ringType == ringType)
                {
                    return latLng;
                }
                else
                {
                    latLng.ringType = ringType;
                    saveStuff();
                }
                return latLng;
            }
        }
        return null;
    }
    /**
     *
     * @param location
     * @return
     */
    public static LatLng deleteLocation(String location)
    {
        for (String key : m_locationList.keySet())
        {
            LatLng latLng = (LatLng) m_locationList.get(key);

            if (location.equals( latLng.name ))
            {
                m_locationList.remove(key);
                saveStuff();
                return latLng;
            }
        }
        return null;
    }

    public static LatLng activateLocation(String location, boolean active)
    {
        for (String key : m_locationList.keySet())
        {
            LatLng latLng = (LatLng) m_locationList.get(key);

            if (location.equals( latLng.name ))
            {
                latLng.factive = active;
                saveStuff();
                return latLng;
            }
        }
        return null;
    }
    public static String getLocationKey(String location)
    {
        for (String key : m_locationList.keySet())
        {
            LatLng latLng = (LatLng) m_locationList.get(key);

            if (location.equals( latLng.name ))
            {
                return latLng.name;
            }
        }
        return "";
    }
    public static LatLng getLocationBuyKey(String location)
    {
        for (String key : m_locationList.keySet())
        {
            LatLng latLng = (LatLng) m_locationList.get(key);

            if (location.equals( latLng.name ))
            {
                return latLng;
            }
        }
        return null;
    }
    /**
     *
     */
    private static void saveStuff()
    {
        Log.i(DEBUG_TAG, "saveStuff");

        SaveRestore.saveStuff(m_context.getExternalFilesDir("/").toString(), m_locationList, "locationMatch");
        HashMap<String, Object>retVal = SaveRestore.restoreStuff(m_context.getExternalFilesDir("/").toString(), "locationMatch");
        m_locationList.putAll(retVal);

        if (retVal != null)
        {
            for (String key : retVal.keySet())
            {
                LatLng latLng = (LatLng) retVal.get(key);

                if (m_lastUniqueUsed < latLng.uniqueInt)
                    m_lastUniqueUsed = latLng.uniqueInt;
            }
            m_lastUniqueUsed++;
        }
    }

    /**
     *
     */
    private static void restoreStuff()
    {
        HashMap<String, Object>retVal = SaveRestore.restoreStuff(m_context.getExternalFilesDir("/").toString(), "locationMatch");

        if (retVal != null)
            m_locationList.putAll(retVal);
        Log.i(DEBUG_TAG, "restoreStuff");
    }
}


