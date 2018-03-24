package com.example.cjutzi.myservice;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by cjutzi on 12/19/17.
 */

public class Util
{
    /**
     *
     * @param msecStart
     * @param msecEnd
     * @return
     */
    static
    public String formatTimeDelta (long msecStart, long msecEnd)
    {
        long diffInSeconds = (msecEnd-msecStart)/1000;

        long diff[] = new long[] { 0, 0, 0, 0 };
        /* sec */  diff[3] = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
        /* min */  diff[2] = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
        /* hours */diff[1] = (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;
        /* days */ diff[0] = (diffInSeconds = (diffInSeconds / 24));

        return(String.format(
                "%03d d %02d:%02d:%02d",
                diff[0],
                diff[1],
                diff[2],
                diff[3] ));
    }


    static public Map<String,Long> SortMapStringLong(Map<String,Long> map)
    {
        ValueComparator bvc = new ValueComparator(map);
        TreeMap<String, Long> sorted_map = new TreeMap<String, Long>(bvc);
        sorted_map.putAll(map);
        return sorted_map;
//        HashMap<String, Long> ret_map = new HashMap<String, Long>();
//        ret_map.putAll(sorted_map);
//        return ret_map;
    }

    static
    class ValueComparator implements Comparator<String>
    {
        Map<String, Long> base;

        public ValueComparator(Map<String, Long> base)
        {
            this.base = base;
        }

        // Note: this comparator imposes orderings that are inconsistent with
        // equals.
        public int compare(String a, String b)
        {
//            int compare =
//                    base.get(a).compareTo(base.get(b));
//            if (compare == 0)
//                return 1;
//            else
//                return compare;
//        }
            if (base.get(a).longValue() > base.get(b).longValue())
            {
                return -1;
            }
            else
//            if (base.get(a).longValue() < base.get(b).longValue())
            {
                return 1;
            } // returning 0 would merge keys
//            else
//                return 0;
        }
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


}
