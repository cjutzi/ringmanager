package com.example.cjutzi.myservice;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cjutzi on 11/22/17.
 */

/*
 * Extendable methods
 *
 *    public void               addActiveTime (String name, long activeTimeMsec)
 *    public ArrayList<LatLng>  getLocationWithDistance(double lat, double lng, float accuracy)
 *    public ArrayList<LatLng>  getActive(double lat, double lng, float accuracy)
 *    public LatLng             getClosestActive(double lat, double lng, float accuracy)
 *    public LatLng             getClosest(double lat, double lng, float accuracy)
 *    public LatLng             getLocationBuyKey(String location)
 *    public LatLng             addLocation(LatLng latLng)
 *    public LatLng             deleteLocation(String location)
 *    public  ArrayList<String> getTimeSpentWhere()
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
     *
     */
    LocationMatch()
    {
       throw new RuntimeException("must have context passed to construct");
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

            double dist = Util.distance(lat, lng, latLng.lat, latLng.lng, 'm');

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



    /**
     *
     */
    public  void saveLocaitons()
    {
        saveStuff();
    }

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


