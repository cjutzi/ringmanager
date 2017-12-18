package com.example.cjutzi.myservice;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

/**
 * Created by cjutzi on 11/13/17.
 */

public class MyService extends Service implements AsyncActivityInterface
{
    String DEBUG_TAG = this.getClass().getSimpleName();
    static long startTimeMsec = 0;

    static final int BASE_ALARM_SEC = 120;
    static final int BASE_LOC_WAKE = 120;


    /* alarm stuff */
    private AlarmManager        m_alarmManager       = null;
    private PendingIntent       m_alarmPendingIntent = null;    /* used for alarmManager and to cancel it */

    private LocationMatch       m_locMatch      = null;
    private LocationReceiver    m_locRcv        = null; // new LocationReceiver(this); // can not init here since there are no location services here.. yet (until onCreate)

    public enum MyService_NOTIFY_ACTION_COMMAND
    {
        ACTIVITY_CONNECTED,
        ACTIVITY_ASYNCTASK,
        ACTIVITY_STOPSVC,

    };

    /**
     * @author cjutzi
     */
    public class MyLocalBinder extends Binder
    {
        public MyService getService() {
            return MyService.this;
        }
    }

    private final IBinder myBinder = new MyLocalBinder();
    Boolean m_f_isBound = false;

    /*
     * (non-Javadoc)
     * @see android.app.Service#onUnbind(android.content.Intent)
     */
    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.d(DEBUG_TAG, "onUnBind()");
        m_f_isBound = false;
        return super.onUnbind(intent);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent arg0)
    {
        Log.d(DEBUG_TAG, "onBind()");
        m_f_isBound = true;
        return myBinder;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        Log.i(DEBUG_TAG, "onStartCommand()");
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.i(DEBUG_TAG, "onDestroy()");
        saveStuff();
        m_locRcv.cleanup();
        m_alarmManager.cancel(m_alarmPendingIntent);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.i(DEBUG_TAG, "onCreate()");
        startTimeMsec = System.currentTimeMillis();
        restoreStuff();
        String t2[] = new String[]
                {
                  "Service Intializing"
                };
        iconNotificationCreateAndShow("No Location Active", "No Location Active", t2);

        /* setup Alarm Receiver to go off every 5 min */

        /* setup alarms for localation receiver */
        m_locRcv = new LocationReceiver(this);
        /*
           have to create location Match after Location Receiver, since they register for Callbacks to Location Recvier
           based on proximity..
         */
        m_locMatch = new LocationMatch(this); // for this context.. create an object to save/restore

        /*
           activate locations after you have created hte location object.
         */
        m_locRcv.activateLocations(m_locMatch);
        m_locRcv.callback(0);   /* init the core callback stuff */


//        m_locAr  = new AlarmReceiver(LocationReceiver.class.getSimpleName(),m_locRcv, 0); //BASE_LOC_WAKE);
//        m_thisAr = new AlarmReceiver(this.getClass().getSimpleName(), this, BASE_ALARM_SEC);


//        m_locMatch = new LocationMatch(this);
//        m_locRcv.callback(0);   /* init the core callback stuff */

    }

    /*
     *  ICON IN TRAY
     */
    private static final int STATUSBAR_ICON_ID = 1;
    String lastTextContent = "";
    String lastTextTicker = "";
    static NotificationManager m_NotificationManager = null;

    /**
     * @param textContent
     * @param textTicker
     */
    public void iconNotificationCreateAndShow(String textContent, String textTicker, String[] lines)
    {
        if (textContent == null)
            textContent = lastTextContent;
        if (textTicker == null)
            textTicker = lastTextTicker;

        lastTextTicker = textTicker;
        lastTextContent = textContent;

        Resources res = getResources();
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_kilroy)
                        .setContentTitle(res.getString(R.string.app_name))
                        .setTicker(textTicker)
                        .setContentText(textContent);


        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(textContent);
        if (lines != null)
            for (int i = 0; i < lines.length; i++)
                inboxStyle.addLine(lines[i]);

        mBuilder.setStyle(inboxStyle);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MyServiceActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MyServiceActivity.class);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        m_NotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = mBuilder.build();
        m_NotificationManager.notify(STATUSBAR_ICON_ID, notification);
    }

    /*
     * way to have the Activity talk to the service in an eventful way below.. notifcaitonRecevier
     */
    /*
     * REPLACING BROADCAST RECEIVER
     * @param command
     */
    /*
     * way to have the Activity talk to the service in an eventful way below.. notifcaitonRecevier
     */

    /**
     * REPLACING BROADCAST RECEIVER
     *
     * @param command
     */
    public void notificationReceiver(MyService_NOTIFY_ACTION_COMMAND command, String voidStarDataString)
    {
        switch (command) {
            case ACTIVITY_CONNECTED:
                Log.i(DEBUG_TAG, "MyService.notificationReceiver(ACTIVITY_CONNECTED)");
                break;

            case ACTIVITY_ASYNCTASK:
                Log.i(DEBUG_TAG, "MyService.notificationReceiver(ACTIVITY_ASYNCTASK)");
                AsyncActivity aful = new AsyncActivity(this);
                aful.execute();
                break;

            case ACTIVITY_STOPSVC:
                Log.i(DEBUG_TAG, "MyService.notificationReceiver(ACTIVITY_STOPSVC)");
                m_NotificationManager.cancel(STATUSBAR_ICON_ID);
//                m_alarmManager.cancel(m_alarmPendingIntent);
                getApplication().onTerminate();
//                m_thisAr.unsubscribe();
//                m_locAr.unsubscribe();
                stopSelf();
                break;



            default:
                Log.i(DEBUG_TAG, "MyService.notificationReceiver(???)");
        }
    }


    /**
     *
     */
    private void saveStuff()
    {
//        HashMap<String,Object> hashMap = new HashMap<String,Object>();
//        hashMap.put("m_state1",m_state1.toString());
//        SaveRestore.saveStuff(this.getExternalFilesDir("/").toString(), hashMap, DEBUG_TAG);
    }

    /**
     *
     */
    private void restoreStuff()
    {
//        HashMap<String,Object> hashMap = SaveRestore.restoreStuff(this.getExternalFilesDir("/").toString(), DEBUG_TAG);
//        m_state1 = (hashMap==null || hashMap.get("m_state1")==null)?1:new Integer((String)hashMap.get("m_state1"));
    }


    /* ASYNCACTIVITY INTERFACE */
    private Object lock = new Object();

    @Override
    /* AsyncActivityInterface */
    public void asyncActivityDo()
    {

        Log.i(DEBUG_TAG, "asyncAtivityDo()");
        try {
            synchronized (lock) {
                Log.i(DEBUG_TAG, "asyncActivityDo.start");
                lock.wait(10000);
                Log.i(DEBUG_TAG, "asyncActivityDo.done");
            }
        }
        catch (Exception e)
        {
            Log.e(DEBUG_TAG, "asyncActivityDo.exception: " + e.getMessage());

        }
    }

    @Override
    public void asyncActivityComplete() {
        Log.i(DEBUG_TAG, "asyncAtivityComplete()");
    }

    /* setup and change icons */


}
