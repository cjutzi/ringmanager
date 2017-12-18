package com.example.cjutzi.myservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by cjutzi on 12/7/17.
 */

public class AlarmBroadcastReceiver extends BroadcastReceiver
{
    long lastCalled = 0;
    /**
     *
     */
    public AlarmBroadcastReceiver()
    {
        Log.i("AlarmBroadcastReceiver"," Constructor ... ");
    }
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (lastCalled == 0)
            lastCalled = System.currentTimeMillis();
        else
            Log.i("AlarmBroadcastReceiver", "last called : "+((System.currentTimeMillis()-lastCalled)/1000)+" seconds ago");

        LocationReceiver.onAlarmReceive(context, intent);
    }
}
