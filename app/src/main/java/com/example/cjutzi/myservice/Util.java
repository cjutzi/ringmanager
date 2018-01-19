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
}
