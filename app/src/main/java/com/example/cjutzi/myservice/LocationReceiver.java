package com.example.cjutzi.myservice;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Created by cjutzi on 11/21/17.
 */

public class LocationReceiver implements  LocationListener, GpsStatus.Listener
{

    public enum LocationReceiver_CONFIGURATION
    {
        SLEEP_UNTIL_PROX,
        TRACK_IDLE,
        TRACK_PROXIMITY_TRIGGER,
        TRACK_LOCATIONS,
        TRACK_CLEAR,
    };

    String                       DEBUG_TAG               = this.getClass().getSimpleName();

    static  final int            ALARM_INRANGE_PROX_MIN  = 5;
    static LatLng                m_currentLocationLatLng = null;

    static MyService             m_context               = null;
    Boolean                      m_f_WeTurnedOnVibrate   = false;
    LocationManager              m_locationManager       = null;
    int                          m_currentAlarmPeriodMin = ALARM_INRANGE_PROX_MIN;
    int                          m_onLocationChangeCnt   = 0;
    int                          m_callbackAlarmCnt      = 0;
    int                          m_onReceiveProximityCnt = 0;
    long                         m_svcStartTimeMsec      = 0;
    boolean                      m_inProximity           = true;
    boolean                      m_alarmActive           = true;

    /* configureation stuff */
    static private Boolean m_sleepUtilProx      = false;
    static private Boolean m_trackIdle          = true;
    static private Boolean m_trackProxTrigger   = false;
    static private Boolean m_trackLocations     = true;


    static TreeMap<String, String> m_arrayLocationActivityHistory = new TreeMap<String, String>();

    /*
      locks and stats for onReceive to process in/out
      activity start time stuff
    */
    static  final       Integer lock                     = new Integer(1);
    static              String  m_activeName             = null;
                        long    m_activeTimeStart        = 0;


    static LocationReceiver      m_locationReceiver      = null;  // TODO - fix this but look at ProximityReceiver since it calls this statically.. need to figure this out.


    public static void Configure(LocationReceiver_CONFIGURATION config, String parameter)
    {
        switch (config)
        {
            case SLEEP_UNTIL_PROX:
                    {
                        m_sleepUtilProx     = new Boolean(parameter);
                    }
                    break;
            case TRACK_IDLE:
                    {
                        m_trackIdle         = new Boolean(parameter);
                    }
                    break;
            case TRACK_PROXIMITY_TRIGGER:
                    {
                        m_trackProxTrigger  = new Boolean(parameter);
                    }
                    break;
            case TRACK_LOCATIONS:
                    {
                        m_trackLocations    = new Boolean(parameter);
                    }
                    break;
            case TRACK_CLEAR:
                    {
                        m_arrayLocationActivityHistory.clear();
                        m_arrayLocationActivityHistory.put(getCurrentTimeMills(),String.format(getDateString(true) + " log cleared "));
                        saveStuff();
                    }
                    break;
        }
    }
    /**
     *
     */
    LocationReceiver()
    {
        if (m_context == null) throw new RuntimeException("must have context passed to construct");
    }
    /**
     *
     * @param context
     */
    LocationReceiver (MyService context)
    {
        Log.i(DEBUG_TAG,"***** LocationReceiver -- constructor called");
        if (context == null)
            throw new RuntimeException();

        m_locationReceiver  = this;
        m_context           = context;

        /* has to happen before Location Manager is activates */
        /*
            setup the callback alarm..
        */

        setAlarmIntent(ALARM_INRANGE_PROX_MIN*60, ALARM_INRANGE_PROX_MIN*60);

        m_locationManager   = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        m_svcStartTimeMsec  = System.currentTimeMillis();
        m_activeTimeStart   = System.currentTimeMillis();
        restoreStuff();
        m_arrayLocationActivityHistory.put(getCurrentTimeMills(),String.format(getDateString(true) + " svc started "));
        saveStuff();
    }


    /**
     *
     * @return
     */
    static private String getCurrentTimeMills()
    {
        Long retVal = System.currentTimeMillis();
        return retVal.toString();
    }

    private AlarmManager  alarmManager  = null;
    private PendingIntent alarmPendingIntent = null;

    public void cancelAlarmIntent()
    {
        Log.i(DEBUG_TAG,"cancelAlarmIntent ");
        if (alarmManager == null)
            return;
        alarmManager.cancel(alarmPendingIntent);
        m_alarmActive = false;

    }
    /**
     *
     * @param initialSec
     * @param intervalSec
     */
    private void setAlarmIntent(int initialSec, int intervalSec)
    {
        Log.i(DEBUG_TAG,"setAlarmIntent - "+initialSec+" "+intervalSec);
        m_currentAlarmPeriodMin = intervalSec/60;

        if (alarmManager == null)
             alarmManager = (AlarmManager) m_context.getSystemService(Context.ALARM_SERVICE);

        if (alarmPendingIntent == null)
        {
            Intent alarmIntent = new Intent(m_context, AlarmBroadcastReceiver.class);
            alarmPendingIntent = PendingIntent.getBroadcast(m_context, 1, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        cancelAlarmIntent();
        m_alarmActive = true;
        alarmManager.setRepeating (AlarmManager.RTC_WAKEUP, initialSec*1000, intervalSec*1000,  alarmPendingIntent);
    }
    /**
     * since Locations will activate and re-register for Proximity.. we need to do this after the
     * constructor..
     * Call this after construction of both this guy and the LM.. mas me the locaitonMatch object.
     * @return
     */

    public void activateLocations(LocationMatch lm)
    {
        //LocationMatch m_locationMatch = lm;
         /* on create, active all active locations */
        for (LatLng latLng : lm.getLocaitons())
        {
            if (latLng.factive)
                activateLocation(latLng.name, latLng.factive);
        }
    }
    static
    private String formatTimeDelta (long msecStart, long msecEnd)
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

    /**
     *
     * @return
     */

    static
    public LatLng  getCurrentLoc()
    {
        return m_currentLocationLatLng;  // updated by timer call and locationReqeust with callback
    }
    /**
     *
     * @return
     */
    private  String[] getNoficationArray()
    {
        LatLng currentLoc = getCurrentLoc();
        if (currentLoc == null && m_activeName != null)
            currentLoc = LocationMatch.getLocationBuyKey(m_activeName);
        else
        if (currentLoc == null)
            return new String [] { "" };

        LatLng latLng = LocationMatch.getClosest(currentLoc.lat, currentLoc.lng, currentLoc.lastAccuracy);
        ArrayList<String> returnlist = new ArrayList<String>();

        returnlist.add("standby: "+ (!m_alarmActive?("sleeping"):(" "+ m_currentAlarmPeriodMin +" min"))+" alarm: "+(m_alarmActive?"on":"off")+" FixFail : "+m_abortedGPSFixes);
        returnlist.add("Proximity  : "+ m_inProximity);
        returnlist.add("Counters   : onLoc ("+m_onLocationChangeCnt+") onProx ("+m_onReceiveProximityCnt+") onAlarm ("+m_callbackAlarmCnt+")");
        returnlist.add("Nearest    : "+(latLng == null?"N/A":String.format("%s @ %d",latLng.name, latLng.lastDistMeter)));
        returnlist.add("UpTime     : "+formatTimeDelta(m_svcStartTimeMsec,System.currentTimeMillis()));
        returnlist.add("Current Loc: "+String.format("%8.4f,%8.4f", currentLoc.lat, currentLoc.lng));
        returnlist.add("ActiveTime : "+(m_activeName ==null?"N/A":formatTimeDelta(m_activeTimeStart,System.currentTimeMillis())));

        String[] stringArray = returnlist.toArray(new String[0]);

        return stringArray;
    }

    long m_msecSinceStartFix = 0;
    long m_abortedGPSFixes = 0;

    @Override
    public void onGpsStatusChanged(int event)
    {

        switch(event) {
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                Log.i(DEBUG_TAG, "GPS status change: GPS_EVENT_FIRST_FIX " + event);
                m_locationManager.removeGpsStatusListener(this);
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                Log.i(DEBUG_TAG, "GPS status change: GPS_EVENT_SATELLITE_STATUS " + event);

                /*
                    failing GPS Fix..
                 */
                long msecFixWait = System.currentTimeMillis() - m_msecSinceStartFix;
                if (msecFixWait > 10*1000)
                {
                    Log.i(DEBUG_TAG, "GPS status change: ****** ABORTING GPS FIX.. MOVING TO NETWORK PROVIDER -- waited "+msecFixWait+" msec");
                    m_abortedGPSFixes++;
                    m_locationManager.removeGpsStatusListener(this);
                    reqeustGPSLocation(LocationManager.NETWORK_PROVIDER);
                }
                break;
            case GpsStatus.GPS_EVENT_STARTED:
                m_msecSinceStartFix = System.currentTimeMillis();
                Log.i(DEBUG_TAG, "GPS status change: GPS_EVENT_STARTED " + event);
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                Log.i(DEBUG_TAG, "GPS status change: GPS_EVENT_STOPPED " + event);
                break;
            default:
                Log.i(DEBUG_TAG, "GPS status change: " + event);
                break;
        }
    }

    static Integer m_accuracyTry  = 0;
    /**
     *
     * @param location
     */
    @Override
    public void onLocationChanged(Location location)
    {
        Log.i(DEBUG_TAG, "onLocationChange : provider = "+location.getProvider()+"  Accuracy = "+location.getAccuracy() + " try # "+m_accuracyTry);

        m_onLocationChangeCnt++;

        double lat      = 0.0,
               lng      = 0.0,
               mtrAlt   = 0.0;
        float mpsSpeed  = 0.0f;
        double bearing  = 0.0f;

        mtrAlt          = location.getAltitude();

        if (location.hasSpeed())
        {
            mpsSpeed = location.getSpeed(); // meters per second (as documented.. sure wish I could know by the variable name :-)
        }
        bearing         = location.getBearing();

        lng             = location.getLongitude();
        lat             = location.getLatitude();


        /* update current location */
        if (m_currentLocationLatLng != null)
        {
            m_currentLocationLatLng.lat = location.getLatitude();
            m_currentLocationLatLng.lng = location.getLongitude();
            m_currentLocationLatLng.lastAccuracy = location.getAccuracy();
        }
        else
        {
            m_currentLocationLatLng = new LatLng("CurrentLocation", location.getLatitude(), location.getLongitude(), -1, false, 0, location.getAccuracy());
        }

        /*
           if the provider is Network.. flush it.
           if not.. accuracy < 30 meters is great.. try 15 times.. if not .. fuck it.
           typically it's a 5-10 try with the GPS before you have the accuracy..
           'if you don't succeed, try (*20) again..
         */
        synchronized (m_accuracyTry)
        {
            m_accuracyTry++;

            if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER) ||
                (location.getAccuracy() < 30) ||
                (m_accuracyTry > 15))
            {
                m_locationManager.removeUpdates(this);
                Log.i(DEBUG_TAG, "onLocationChange : provider = " + location.getProvider() + "  Accuracy = " + location.getAccuracy() + " try # " + m_accuracyTry);
                m_accuracyTry = 0;
            }
        }
        processCheckIfInGeoFence(null, location.getAccuracy());
    }

    /**
     *
     * @param provider
     * @param status
     * @param extras
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        Log.i(DEBUG_TAG, "onStatusChanged : provider = "+provider);
    }

    /**
     *
     * @param provider
     */
    @Override
    public void onProviderEnabled(String provider)
    {
        Log.i(DEBUG_TAG, "onProviderEnabled : provider = "+provider);

    }

    /**
     *
     * @param provider
     */
    @Override
    public void onProviderDisabled(String provider)
    {
        Log.i(DEBUG_TAG, "onProviderDisabled : provider = "+provider);
    }

    /**
     *
     */
    public void cleanup()
    {
        m_locationManager.removeUpdates(this);
        if (m_activeName == null)
        {
            addLocationActivityHistoryBlock(null, -1, -1, -1.0f, true);
        }
        else
        {
            LatLng latLngActive = LocationMatch.getLocationBuyKey(m_activeName);
            addLocationActivityHistoryBlock(m_activeName, latLngActive.triggerDist, latLngActive.lastDistMeter, latLngActive.lastAccuracy,false);
        };
        m_arrayLocationActivityHistory.put(getCurrentTimeMills(),String.format(getDateString(true) + " svc cleanup "));
        saveStuff();
    }

    /**
     *
     * @param latLng
     */
    private void manageProximityPendingIntent(LatLng latLng)
    {
        if (latLng != null && latLng.factive)
        {
            Intent intent = new Intent(m_context, ProximityReceiver.class);
            intent.putExtra("name", latLng.name);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(m_context, latLng.uniqueInt , intent, 0);
            m_locationManager.addProximityAlert(latLng.lat, latLng.lng, latLng.triggerDist, -1, pendingIntent);
            Log.i(DEBUG_TAG,"activating "+latLng.name+" proximity = "+latLng.triggerDist);
        }
        else
        if (latLng != null)
        {
            Intent intent = new Intent(m_context, ProximityReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(m_context, latLng.uniqueInt , intent, 0);
            m_locationManager.removeProximityAlert(pendingIntent);
        }
    }

    /**
     *
     * @param name
     * @param lat
     * @param lng
     * @param triggerDistance
     * @param factive
     * @param mode
     */
    public void addLocation(String name, double lat, double lng, int triggerDistance, boolean factive, LatLng.RING_TYPE mode)
    {
        LatLng latLngDoesExist = LocationMatch.getLocationBuyKey(name);
        if (latLngDoesExist != null)
        {
            // removes and removes proximity detect also.
            removeLocation(name);
        }
        LatLng latLng = new LatLng(name, lat, lng, triggerDistance, factive, mode, 0);
        latLng = LocationMatch.addLocation(name, triggerDistance, factive, latLng, mode); // will assign unique ID
        manageProximityPendingIntent(latLng);
    }
    /**
     *
     * @param name
     * @param triggerDistance
     * @param mode
     */
    public void addLocation(String name, int triggerDistance, boolean factive, LatLng.RING_TYPE mode)
    {

        LatLng latLng = getCurrentLoc();
        addLocation(name, latLng.lat, latLng.lng, triggerDistance, factive, mode);
    }

    /**
     *
     * @param name
     */
    public void removeLocation(String name)
    {
        LatLng latLng = LocationMatch.deleteLocation(name);
       manageProximityPendingIntent(latLng);
    }

    /**
     *
     * @param locName
     * @param factive
     * @return
     */
    public void activateLocation(String locName, boolean factive)
    {
        LatLng latLng = LocationMatch.activateLocation(locName, factive);
        manageProximityPendingIntent(latLng);
    }


    /**
     *
     * @return
     */
    static private String getDateString(boolean f_trimFirstTwoDigOfYear)
    {
        Date date = new Date();
        String modifiedDate= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        if (f_trimFirstTwoDigOfYear)
            return modifiedDate.substring(2);

        return modifiedDate;
    }


    /**
     *
     */
    private void addLocationActivityHistoryBlock(String name, int triggerDistance, int trueDistance, float accuracy, boolean f_activating)
    {
        // first click.. don't do it..
        if (m_activeTimeStart == 0)
            return;
        /*
           transition out of active.. save record..
         */
        if (!f_activating)
        {
            if (m_trackLocations)
            {
                m_arrayLocationActivityHistory.put(getCurrentTimeMills(),
                                                   String.format(getDateString(true) + " %-10s : %s ",
                                                                                                   (name==null?"":name),
                                                                                                   (name==null ? "N/A" : formatTimeDelta(m_activeTimeStart,System.currentTimeMillis()))));
                int dist = trueDistance-triggerDistance;
                if (dist < 0)
                {
                    m_arrayLocationActivityHistory.put(getCurrentTimeMills(),
                            String.format("%dm dist %dm trig %s accuracy (Inside Fence)",
                                    (name == null ? -1 : trueDistance - triggerDistance), (name == null ? -1 : triggerDistance), (name == null || accuracy == 0.0) ? "N/A" : String.format("%3.0f", accuracy)));
                }
                else
                {
                    m_arrayLocationActivityHistory.put(getCurrentTimeMills(),
                            String.format("%dm dist %dm trig %s accuracy",
                                    (name == null ? -1 : trueDistance - triggerDistance), (name == null ? -1 : triggerDistance), (name == null || accuracy == 0.0) ? "N/A" : String.format("%3.0f", accuracy)));
                }
                saveStuff();
            }
        }
        else
        {
            /* assumes m_activeName == null                                     */
            /* assumes m_activeStartTime was set when m_activeName was null'ed  */
            if (m_trackIdle)
            {
                m_arrayLocationActivityHistory.put(getCurrentTimeMills(),
                                                   String.format(getDateString(true) + " between : %s", (formatTimeDelta(m_activeTimeStart, System.currentTimeMillis()))));
                saveStuff();
            }
        }
    }


    /**
     * Treat this as if it's just a trigger to look.
     * The accuracy of the Proximity thing is shit..
     * Just run through the list and choose a location if active.. be done.. make it simple..
     *
     * @param name
     * @param accuracy
     *
     */
    private void processCheckIfInGeoFence(String name, float accuracy)
    {
        LatLng latLngClosestActive;
        LatLng latLngCurrent             = getCurrentLoc();

        if (latLngCurrent == null && name != null)
        {
            latLngClosestActive = LocationMatch.getLocationBuyKey(name);
        }
        else
        {
            latLngClosestActive = LocationMatch.getClosestActive(latLngCurrent.lat, latLngCurrent.lng, accuracy);
        }

        synchronized (lock)
        {
            if (latLngClosestActive != null)
            {
                /*
                   Could be swapping, but might not be..
                   you are now moving to an activation of a location
                   if there is an active.. you need to de-activate.. if they are the same, do nothing.
                 */
                if (m_activeName != null)
                {
                    // do nothing.. we've entered again.. this can happen given I over ride the in/out on the Prox alarm.
                    //
                    if (m_activeName.equals(latLngClosestActive.name))
                    {
                        String t1 = latLngClosestActive.name + "  " + latLngClosestActive.ringType + " - " + (latLngClosestActive.factive ? "active" : "in-active");
                        m_context.iconNotificationCreateAndShow(t1, t1, getNoficationArray());
                        return;
                    }
                    //
                    // if it does not.. remove the current and move to the new
                    //
                    LatLng latLngActive = LocationMatch.getLocationBuyKey(m_activeName);
                    manageAudioBasedOnEnterLeave(latLngActive, false);
                    addLocationActivityHistoryBlock(m_activeName, latLngActive.triggerDist, latLngActive.lastDistMeter,latLngActive.lastAccuracy, false);
                    LocationMatch.addActiveTime (m_activeName, System.currentTimeMillis()-m_activeTimeStart);
                    m_activeTimeStart = System.currentTimeMillis();  // really this is a start-idle time, but I'm using it
                    m_activeName = null;
                }

                /*
                    transition to new location..
                 */
                manageAudioBasedOnEnterLeave(latLngClosestActive, true);
                addLocationActivityHistoryBlock(null, -1, -1, -1.0f, true);
                m_activeName = latLngClosestActive.name;
                m_activeTimeStart = System.currentTimeMillis();
                String t1 = latLngClosestActive.name + "  " + latLngClosestActive.ringType + " - " + (latLngClosestActive.factive ? "active" : "in-active");
                m_context.iconNotificationCreateAndShow(t1, t1, getNoficationArray());
                return;
            }
            else
            {
                if (m_activeName != null)
                {
                    LatLng latLngActive  = LocationMatch.getLocationBuyKey(m_activeName);
                    manageAudioBasedOnEnterLeave(latLngActive, false);
                    addLocationActivityHistoryBlock(m_activeName, latLngActive.triggerDist, latLngActive.lastDistMeter, latLngActive.lastAccuracy, false);
                    LocationMatch.addActiveTime (m_activeName, System.currentTimeMillis()-m_activeTimeStart);
                    m_activeName = null;
                    m_activeTimeStart = System.currentTimeMillis();
                }
            }
        }
        m_context.iconNotificationCreateAndShow("No Location Active", "No Location Active", getNoficationArray());

        LatLng latLng = LocationMatch.getClosest(latLngCurrent.lat, latLngCurrent.lng, latLngCurrent.lastAccuracy);

        if ( (latLng != null) && latLng.lastDistMeter > 2000)
        {
            m_inProximity = false;
        }
    }

    /**
     * Proximity Receive..
     *
     * @param name
     * @param entering
     */
    public void processOnReceive (String name, boolean entering)
    {
        Log.i("LocationReceiver","processOnReceive (name="+name+", fenter="+entering+")");
        m_inProximity = true;
        m_onReceiveProximityCnt++;


        if (m_trackProxTrigger)
        {
            m_arrayLocationActivityHistory.put(getCurrentTimeMills(), String.format(getDateString(true) + " Prox (" + name + ") " + (entering ? "enter" : "exit")));
            saveStuff();
        }


//        PowerManager powerManager = (PowerManager) m_context.getSystemService(Context.POWER_SERVICE);
//        if (powerManager.isPowerSaveMode())
//        {
//            setAlarmIntent(ALARM_INRANGE_PROX_MIN*60,ALARM_INRANGE_PROX_MIN*60);
//
//            if (m_locationReceiver != null)
//                m_locationReceiver.processCheckIfInGeoFence(name,0);
//        }
//        else
        {
            setAlarmIntent(1,ALARM_INRANGE_PROX_MIN*60);
        }

//        setAlarmIntent(ALARM_INRANGE_PROX_MIN*60,ALARM_INRANGE_PROX_MIN*60);
//        m_locationReceiver
    }
    /**
     *
     * proximity trigger based on location - from Android
     *
     * @param context
     * @param intent
     */

    static
    public void onProximityReceive(Context context, Intent intent)
    {
        final String key = LocationManager.KEY_PROXIMITY_ENTERING;
        final Boolean entering = intent.getBooleanExtra(key, false);
        String name = intent.getExtras().getString("name");
        Log.i("LocationReceiver","onReceive (name="+name+", fenter="+entering+")");

        if (m_locationReceiver != null)
            m_locationReceiver.processOnReceive(name, entering);
    }

    /**
     *
     * @param context
     * @param intent
     */
    static
    public void onAlarmReceive(Context context, Intent intent)
    {
        if (m_locationReceiver != null)
            m_locationReceiver.callback(0);
    }
    /**
     * assumes: called within locked context
     *
     * @param latLngTarget
     * @param fentered
     */

    private void manageAudioBasedOnEnterLeave(LatLng latLngTarget, boolean fentered)
    {
        AudioManager am;
        if (m_context == null)
            throw new RuntimeException ("Should never be Null - manageAudioBaseOnenterLeave -- context = null");

        am = (AudioManager) m_context.getSystemService(Context.AUDIO_SERVICE);
        int mode = am.getRingerMode();

        if (fentered)
        {
            if (latLngTarget.ringType == LatLng.RING_TYPE.FULL)
            {
                /* don't turn on normal if the user has set to silent already */
                if (mode != AudioManager.RINGER_MODE_VIBRATE &&
                   mode != AudioManager.RINGER_MODE_SILENT)
                {
                    am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                }
            }
            else
            {
                if (mode != AudioManager.RINGER_MODE_VIBRATE &&
                    mode != AudioManager.RINGER_MODE_SILENT)
                {
                    am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    m_f_WeTurnedOnVibrate = true;
                }
            }
        }
        else
        {
            if (m_f_WeTurnedOnVibrate)
            {
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                m_f_WeTurnedOnVibrate = false;
            }
        }

    }


    /**
     *
     * @param provider
     */
    private void reqeustGPSLocation(String provider)
    {
        Log.i(DEBUG_TAG,"reqeustGPSLocation - "+provider);

        if (provider.equals(LocationManager.GPS_PROVIDER) )
            m_locationManager.addGpsStatusListener(this);

        m_locationManager.requestLocationUpdates(provider, 0,0,this);
    }
    /**
     *
     * @param secSinceLast
     */
    public void callback(Integer secSinceLast)
    {
        Log.i(DEBUG_TAG,"callback from Alarm - setup Location track....");
        m_callbackAlarmCnt++;

        String  provider,
                providerBackup;

        if (m_inProximity)
        {
            provider       = LocationManager.GPS_PROVIDER;
            providerBackup = LocationManager.NETWORK_PROVIDER;
        }
        else
        {
            provider       = LocationManager.NETWORK_PROVIDER;
            providerBackup = LocationManager.GPS_PROVIDER;
        }

        if (m_locationManager.isProviderEnabled(provider))
            reqeustGPSLocation(provider);
        else
            reqeustGPSLocation(providerBackup);


        //  occurs in callback on provider lock ---      processCheckIfInGeoFence();
    }

    /**
     *
     * @return
     */
    static

    public ArrayList<String> getLocationActivityHistory()

    {
        return new ArrayList<String>(m_arrayLocationActivityHistory.values());
    }

    /**
     *
     */
    static
    private void saveStuff()
    {
        HashMap<String, Object>putList = new HashMap<String, Object>();
        putList.putAll(m_arrayLocationActivityHistory);

        SaveRestore.saveStuff(m_context.getExternalFilesDir("/").toString(), putList, "activityHistory");
    }

    /**
     *
     */
    private void restoreStuff()
    {
        HashMap<String,Object> hashMap = SaveRestore.restoreStuff(m_context.getExternalFilesDir("/").toString(), "activityHistory");
        m_arrayLocationActivityHistory.clear();
        for (String key : hashMap.keySet())
        {
            m_arrayLocationActivityHistory.put(key, (String) hashMap.get(key));
        }

    }


}
