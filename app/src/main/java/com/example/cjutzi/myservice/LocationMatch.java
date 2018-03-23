package com.example.cjutzi.myservice;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cjutzi on 11/22/17.
 */

public class LocationMatch
{
    String DEBUG_TAG=this.getClass().getSimpleName();

    static private  HashMap<String, Object> m_locationList = new HashMap<String, Object>();

    static
    {
        m_locationList.put("Living Savior", new LatLng("Living Savior", 45.3749701, -122.7668027, 200, true, LatLng.RING_TYPE.VIBRATE));
        m_locationList.put("Intel JF", new LatLng("Intel JF", 45.543054, -122.960508, 100, true, LatLng.RING_TYPE.FULL));
        m_locationList.put("Home", new LatLng("Home", 45.410034, -122.710750, 100, true, LatLng.RING_TYPE.FULL));
        m_locationList.put("Zupan's Market", new LatLng("Zupan's Market", 45.407277, -122.7250246, 50, true, LatLng.RING_TYPE.FULL));
        m_locationList.put("SJC JetCenter", new LatLng("SJC JetCenter", 37.3591078, -121.932838, 50, true, LatLng.RING_TYPE.VIBRATE));
        m_locationList.put("KHIO", new LatLng("KHIO", 45.5398, -122.9473, 200, true, LatLng.RING_TYPE.FULL));
    }

    Context m_context = null;


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
     * calculates the distance sets accuracy for each element in the list..
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
            latLng.lastAccuracy = accuracy;
            m_locationList.put(latLng.name, latLng); // ??
        }
    }

    /**
     *
     * @param name
     * @param activeTimeMsec
     */
    public void addActiveTime (String name, long activeTimeMsec)
    {
        LatLng latLng = getLocationBuyKey(name);
        if (latLng != null)
        {
            latLng.activeTimeSec += (activeTimeMsec/1000);
            saveLocaitons();
        }
    }

    /**
     * @return
     */
    public ArrayList<LatLng> getLocationWithDistance(double lat, double lng, float accuracy)
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
     * get the "factive=true" list that are closeest..
     *
     * @param lat current locaiton
     * @param lng current location
     * @return
     */
    public ArrayList<LatLng> getActive(double lat, double lng, float accuracy)
    {
        calcListDistance(lat, lng, accuracy);
        ArrayList<LatLng> m_activeKeyList = new ArrayList<LatLng>();

        for (String key : m_locationList.keySet())
        {
            LatLng latLng = (LatLng)m_locationList.get(key);

            if (!latLng.factive)
                continue;

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
    public LatLng getClosestActive(double lat, double lng, float accuracy)
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
    public LatLng getClosest(double lat, double lng, float accuracy)
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
//    public static boolean addLocation(String name,
//                                      int triggerDist,
//                                      boolean factive,
//                                      float lat, float lng,
//                                      LatLng.RING_TYPE ringType)
//    {
//        if (lat == 0.0 && lng == 0.0)
//            return false;
//        LatLng   latLng = new LatLng(name, (double)lat, (double)lng, triggerDist, true, ringType, 0);
//
//        addLocation(name, triggerDist, factive, latLng, latLng.ringType);
//        /* cjutzi - where does proximity trigger live? */
//        return true;
//    }
    /**
     *
     * @return
     */
    public LatLng addLocation(LatLng latLng)
    {
        if (latLng == null)
            return null;

        if ((latLng.name == null || latLng.name.length() ==0) ||
            (latLng.lastDistMeter == 0) ||
            (latLng.ringType == null))
            throw new RuntimeException("Invalid LatLng object");

        /* if this is an update first delete it then add it bcak :-)*/
        if (deleteLocation(latLng.name) != null)
            Log.i(DEBUG_TAG,"addLocation: WARNING.. ("+latLng.name+") was not removed before adding, Caution regarding Proximity registration");

        m_locationList.put(latLng.name, latLng);
        saveLocaitons();
        return latLng;
    }


//    /**
//     *
//     * @param location
//     * @param ringType
//     * @return
//     */
//    public LatLng updateLocation(String location, LatLng.RING_TYPE ringType)
//    {
//        for (String key : m_locationList.keySet())
//        {
//            LatLng latLng = (LatLng) m_locationList.get(key);
//
//            if (location.equals( latLng.name ))
//            {
//                if (latLng.ringType == ringType)
//                {
//                    return latLng;
//                }
//                else
//                {
//                    LocationReceiver.activateLocation(latLng.name ,true);
//                    latLng.ringType = ringType;
//                    saveLocaitons();
//                }
//                return latLng;
//            }
//        }
//        return null;
//    }
    /**
     * removes from list and saves list after removal
     *
     * @param location
     * @return
     */
    public LatLng deleteLocation(String location)
    {
        for (String key : m_locationList.keySet())
        {
            LatLng latLng = (LatLng) m_locationList.get(key);

            if (location.equals( latLng.name ))
            {
                m_locationList.remove(key);
                saveLocaitons();
                return latLng;
            }
        }
        return null;
    }

//    /**
//     *
//     * @param name
//     * @param updatedLatLng
//     * @return
//     */
//    public static boolean updateLatLng(String name, LatLng updatedLatLng)
//    {
//        saveLocaitons();
//        return true;
//    }

    /**
     *
     */
    public  void saveLocaitons()
    {
        saveStuff();
    }
//    /**
//     *
//     * @param location
//     * @param activeKeyList
//     * @return
//     */
//    public LatLng activateLocation(String location, boolean active)
//    {
//        for (String key : m_locationList.keySet())
//        {
//            LatLng latLng = (LatLng) m_locationList.get(key);
//
//            if (location.equals( latLng.name ))
//            {
//                latLng.factive = active;
//                saveLocaitons();
//                return latLng;
//            }
//        }
//        return null;
//    }

    /**
     *
     * @param location
     * @return
     */
    public String getLocationKey(String location)
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

    /**
     *
     * @param location
     * @return
     */
    public LatLng getLocationBuyKey(String location)
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
     *  Will be dead code if I can get the Customer Adaptor to work.
     *
     * @return
     */
    public  ArrayList<String> getTimeSpentWhere()
    {
        ArrayList<String>  retArray = new ArrayList<String>();

        for (String key :  m_locationList.keySet())
        {
            LatLng latLng = (LatLng) m_locationList.get(key);
            retArray.add(String.format(" %-20s  %20s",latLng.name, Util.formatTimeDelta(0,latLng.activeTimeSec *1000)));
        }
        return retArray;
    }

    /**
     *
     * @return
     */
    public  Map<String, Long> getTimeSpentWhereMap()
    {
        HashMap<String, Long> retMap = new HashMap<String, Long>();

        for (String key :  m_locationList.keySet())
        {
            LatLng latLng = (LatLng) m_locationList.get(key);
            retMap.put(latLng.name, latLng.activeTimeSec);
           //retMap.put(latLng.name, Util.formatTimeDelta(0,latLng.activeTimeSec*1000));
        }
        return retMap;
    }
    /**
     *
     */
    private void saveStuff()
    {
        Log.i(DEBUG_TAG, "saveStuff");
        SaveRestore.saveStuff(m_context.getExternalFilesDir("/").toString(), m_locationList, "locationMatch");
        restoreStuff();
    }

    /**
     * restores all and resets lastUniqueUsed to be used in LatLng creates
     */
    private void restoreStuff()
    {
        HashMap<String, Object>retVal = SaveRestore.restoreStuff(m_context.getExternalFilesDir("/").toString(), "locationMatch");

        if (retVal != null)
            m_locationList.putAll(retVal);

        Log.i(DEBUG_TAG, "restoreStuff");
    }









}


