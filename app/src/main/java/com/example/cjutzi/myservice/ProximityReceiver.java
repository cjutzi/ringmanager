package com.example.cjutzi.myservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by cjutzi on 12/4/17.
 */

public class ProximityReceiver extends BroadcastReceiver {
    /**
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        LocationReceiver.onProximityReceive(context, intent);
    }
}