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
import java.util.Collections;
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

    String                       DEBUG_TAG                  = this.getClass().getSimpleName();

    static final int             ACCURACY_LIMIT_METERS      = 30;
    static final int             MAX_ACCURACY_FAILURE_TRYS  = 15;
    static final int             MAX_FIX_WAIT_SECONDS       = 10;
    static final int             ALARM_INRANGE_PROX_MIN     = 5;
    static final int             ALARM_STARTRANGE_PROX_MIN  = 1;

    /** CURRENT LOCATION STATE - OR LAST **/

    static private double         m_currentLocationLat      = 0.0d;
    static private double         m_currentLocationLng      = 0.0d;
    static private float          m_currentLocaitonAcc      = 0.0f;
    static private float          m_currentLocationSpeedmps = 0.0f;
    static private float          m_currentLocationBearning = 0.0f;    // never really used.


    static MyService             m_context                  = null;
    Boolean                      m_f_WeTurnedOnVibrate      = false;
    LocationManager              m_locationManager          = null;
    int                          m_currentAlarmPeriodMin    = ALARM_INRANGE_PROX_MIN;
    int                          m_onLocationChangeCnt      = 0;
    int                          m_callbackAlarmCnt         = 0;
    int                          m_onReceiveProximityCnt    = 0;
    long                         m_svcStartTimeMsec         = 0;
    boolean                      m_inProximity              = true;
    boolean                      m_alarmActive              = true;

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
    long    m_activeTimeStart                            = 0;
    long    m_lastSavedActiveTimeStart                   = 0;  // used to save tenative state of total time at one location - allows accumiation of time in saved database as
                                                               // as opposed to only saving it when a transition occurs.. what was occuring was the service was getting killed
                                                               // and if it died while at a location, non of the locaiton time was accumulated. so now we save a snap shot
                                                               // of the delta time.  THis variable tracks time since last saved..


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
        m_svcStartTimeMsec  =
            m_activeTimeStart  =
                m_lastSavedActiveTimeStart = System.currentTimeMillis();
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

    /**
     *
     * @return
     */

    static
    public LatLng  getCurrentLoc()
    {
        return new LatLng("", m_currentLocationLat, m_currentLocationLng, -1, false, 0);
    }
    /**
     *
     * @return
     */
    private  String[] getNoficationArray()
    {

        double locLat = m_currentLocationLat;
        double locLng = m_currentLocationLng;
        float  locAcc = m_currentLocaitonAcc;

        LatLng closestLatLng = LocationMatch.getClosest(m_currentLocationLat, m_currentLocationLng, m_currentLocaitonAcc);

        /* if you are active.. use the active location */

        if (m_activeName != null)
        {
            LatLng currentLoc =  LocationMatch.getLocationBuyKey(m_activeName);
            locLat = currentLoc.lat;
            locLng = currentLoc.lng;
            locAcc = m_currentLocaitonAcc;

        }
        else
            return new String [] { "" };

        ArrayList<String> returnlist = new ArrayList<String>();

        returnlist.add("standby: "+ (!m_alarmActive?("sleeping"):(" "+ m_currentAlarmPeriodMin +" min"))+" alarm: "+(m_alarmActive?"on":"off")+" FixFail : "+m_abortedGPSFixes);
        returnlist.add("Proximity  : "+ m_inProximity);
        returnlist.add("Counters   : onLoc ("+m_onLocationChangeCnt+") onProx ("+m_onReceiveProximityCnt+") onAlarm ("+m_callbackAlarmCnt+")");
        returnlist.add("Nearest    : "+(closestLatLng == null?"N/A":String.format("%s @ %d",closestLatLng.name, closestLatLng.lastDistMeter)));
        returnlist.add("UpTime     : "+Util.formatTimeDelta(m_svcStartTimeMsec,System.currentTimeMillis()));
        returnlist.add("Current Loc: "+String.format("%8.4f,%8.4f", locLat, locLng));
        returnlist.add("ActiveTime : "+(m_activeName ==null?"N/A":Util.formatTimeDelta(m_activeTimeStart,System.currentTimeMillis())));

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
                if (msecFixWait > MAX_FIX_WAIT_SECONDS*1000)
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

        if (location.hasSpeed())
        {
            m_currentLocationSpeedmps    = location.getSpeed(); // meters per second (as documented.. sure wish I could know by the variable name :-)
        }
        else
            m_currentLocationSpeedmps    = 0.0f;

        m_currentLocationBearning = location.getBearing();
        m_currentLocationLng      = location.getLongitude();
        m_currentLocationLat      = location.getLatitude();

        /*
           if the provider is Network.. flush it.
           if not.. accuracy < 30 meters is great.. try MAX_ACCURACY_FAILURE_TRYS times.. if not .. fuck it.
           typically it's a 5-10 try with the GPS before you have the accuracy..
           'if you don't succeed, try (*20) again..
         */
        synchronized (m_accuracyTry)
        {
            m_accuracyTry++;

            if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER) ||
                (location.getAccuracy() < ACCURACY_LIMIT_METERS ) ||
                (m_accuracyTry > MAX_ACCURACY_FAILURE_TRYS))
            {
                m_locationManager.removeUpdates(this);
                Log.i(DEBUG_TAG, "onLocationChange : provider = " + location.getProvider() + "  Accuracy = " + location.getAccuracy() + " try # " + m_accuracyTry);
                m_accuracyTry = 0;
            }
        }
        processCheckIfInGeoFence();
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
        addLocation(name, m_currentLocationLat, m_currentLocationLng, triggerDistance, factive, mode);
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
                                                                                                   (name==null ? "N/A" : Util.formatTimeDelta(m_activeTimeStart,System.currentTimeMillis()))));
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
                                                   String.format(getDateString(true) + " between : %s", (Util.formatTimeDelta(m_activeTimeStart, System.currentTimeMillis()))));
                saveStuff();
            }
        }
    }


    /**
     * Treat this as if it's just a trigger to look.
     * The accuracy of the Proximity thing is shit..
     * Just run through the list and choose a location if active.. be done.. make it simple..
     *
     * If you are moving at say 5mph or say 3meters/sec abort.. we're moving and you're not
     * residing anywhere (unless you're running to your desk) - I've put this check in
     * where this is called, not in this proceedure. (personal choice)
     *
     * this proceedure used to be called in a couple of places, but now only one..
     * timer --> Geo Sync -->  LocationReceiver.Update so this now will be called at each
     *
     */
    private void processCheckIfInGeoFence()
    {
        LatLng latLngClosestActive;
        latLngClosestActive = LocationMatch.getClosestActive(m_currentLocationLat, m_currentLocationLng, m_currentLocaitonAcc);

        synchronized (lock)
        {
            // if you have a close location
            // that you're within the GeoFence..

            if (latLngClosestActive != null)
            {
                /*
                   Could be swapping, but might not be..
                   you are now moving to an activation of a location
                   if there is an active.. you need to de-activate.. if they are the same, do nothing.
                 */
                //
                // if you were active..
                //
                if (m_activeName != null)
                {
                    // do nothing.. we've entered again.. this can happen given I over ride the in/out on the Prox alarm.
                    // this also occurs on the timeout. If you're stil where you were.. save the current time you've been there and
                    // reset the time.. Also.. Save it off incase the service dies.. :-)..
                    // this keeps you from loosing alot of recorded time if the service dies after N hours and you have not updated the
                    // location's active time..
                    //
                    if (m_activeName.equals(latLngClosestActive.name))
                    {
                        String t1 = latLngClosestActive.name + "  " + latLngClosestActive.ringType + " - " + (latLngClosestActive.factive ? "active" : "in-active");
                        m_context.iconNotificationCreateAndShow(t1, t1, getNoficationArray());
                        //LocationMatch.addActiveTime (m_activeName, System.currentTimeMillis()-m_lastSavedActiveTimeStart);
                        LocationMatch.addActiveTime (m_activeName, System.currentTimeMillis()-m_lastSavedActiveTimeStart);
                        m_lastSavedActiveTimeStart = System.currentTimeMillis();
                        return;
                    }
                    //
                    // if it does not.. remove the current and move to the new
                    //
                    LatLng latLngActive = LocationMatch.getLocationBuyKey(m_activeName);
                    manageAudioBasedOnEnterLeave(latLngActive, false);
                    addLocationActivityHistoryBlock(m_activeName, latLngActive.triggerDist, latLngActive.lastDistMeter,latLngActive.lastAccuracy, false);
                    LocationMatch.addActiveTime (m_activeName, System.currentTimeMillis()-m_lastSavedActiveTimeStart);
                    m_activeTimeStart = System.currentTimeMillis();  // really this is a start-idle time, but I'm using it
                    m_activeName = null;
                }

                /* else you were in no-mans land */

                /*
                 * if you are moving fast.. just exit and wait until you stop moving..
                 */
                if (m_currentLocationSpeedmps > 3.0)
                {
                    addLocationActivityHistoryBlock(latLngClosestActive.name + "_TRANS_SPEED", latLngClosestActive.triggerDist, latLngClosestActive.lastDistMeter-1, latLngClosestActive.lastAccuracy, false);
                    return;
                }

                /*
                 * transition to new location..
                 */
                manageAudioBasedOnEnterLeave(latLngClosestActive, true);
                addLocationActivityHistoryBlock(null, -1, -1, -1.0f, true);
                m_activeName = latLngClosestActive.name;
                m_lastSavedActiveTimeStart =
                        m_activeTimeStart = System.currentTimeMillis();
                String t1 = latLngClosestActive.name + "  " + latLngClosestActive.ringType + " - " + (latLngClosestActive.factive ? "active" : "in-active");
                m_context.iconNotificationCreateAndShow(t1, t1, getNoficationArray());
                return;
            }
            /*
             * you are in no-mans land
             */
            else
            {
                /*
                 * and you have an active location (you might have transitioned out .. no?)
                 */
                if (m_activeName != null)
                {
                    LatLng latLngActive  = LocationMatch.getLocationBuyKey(m_activeName);

                    /*
                     * Added if statement to try and mitigate against false triggers when accuracy is shit.
                     * Feb-28-2018 - cjutzi
                     */
                    if (latLngActive.lastDistMeter >= latLngActive.triggerDist+latLngActive.lastAccuracy)
                    {
                        manageAudioBasedOnEnterLeave(latLngActive, false);
                        addLocationActivityHistoryBlock(m_activeName, latLngActive.triggerDist, latLngActive.lastDistMeter, latLngActive.lastAccuracy, false);
                        /*
                         * if total time was < ALARM_STARTRANGE_PROX_MIN min, you were driving and you happend to come close enough to trigger
                         * the geo-fence..
                         * If so.. don't save it.. it was a transient thing.. :-).
                         * or so I say. - cjutzi 3/16/18
                         */
                        long totalLastSavedTimeMsec      = (System.currentTimeMillis() - m_lastSavedActiveTimeStart);
                        Long totalLastSavedTimeMinutes   = totalLastSavedTimeMsec/60/1000;
                        Long totalActiveSavedTimeMinutes = (System.currentTimeMillis() - m_activeTimeStart)/60/1000;

                        /* if  I'm on the first tick of the timer for a new location and you find your self in Libo.. (no mans land)..
                         * don't count it.. just transition.  You might have been driving down the road.
                         */
                        if ( totalLastSavedTimeMinutes == totalActiveSavedTimeMinutes &&
                             totalLastSavedTimeMinutes <= ALARM_STARTRANGE_PROX_MIN)

                        {
                            addLocationActivityHistoryBlock(m_activeName + "_TRANS_TIME ("+totalLastSavedTimeMsec/1000+" sec)", latLngActive.triggerDist, latLngActive.lastDistMeter, latLngActive.lastAccuracy, false);
                        }
                        else
                        {
                            addLocationActivityHistoryBlock(m_activeName + "_OUT ("+totalLastSavedTimeMsec/1000+" sec)", latLngActive.triggerDist, latLngActive.lastDistMeter, latLngActive.lastAccuracy, false);
                            LocationMatch.addActiveTime(m_activeName, totalLastSavedTimeMsec);
                        }
                        m_activeName = null;
                        m_lastSavedActiveTimeStart =
                                m_activeTimeStart = System.currentTimeMillis();
                    }
                    else // accuracy not good enogh to justify exit.
                    {
                        addLocationActivityHistoryBlock(m_activeName + "_FALSE_TRIG", latLngActive.triggerDist, latLngActive.lastDistMeter, latLngActive.lastAccuracy, false);
                        return;
                    }
                }
            }
        }
        m_context.iconNotificationCreateAndShow("No Location Active", "No Location Active", getNoficationArray());

        LatLng latLng = LocationMatch.getClosest(m_currentLocationLat, m_currentLocationLng, m_currentLocaitonAcc);

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
            /* first one is to test if we're still there.. if not.. we were driving by no?.. don't count it */
            setAlarmIntent(ALARM_STARTRANGE_PROX_MIN*60,ALARM_INRANGE_PROX_MIN*60);
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

        ArrayList<String> reverseList = new ArrayList(m_arrayLocationActivityHistory.values());
        Collections.reverse(reverseList);
        return reverseList;
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
