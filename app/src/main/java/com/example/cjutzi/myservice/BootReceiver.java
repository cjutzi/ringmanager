package com.example.cjutzi.myservice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


/**
 * Created by cjutzi on 11/15/17.
 */


public class BootReceiver extends BroadcastReceiver
{
    String DEBUG_TAG=this.getClass().getSimpleName();


    public void onReceive(Context context, Intent intent)
    {
        Log.i(DEBUG_TAG,"BootReceiver : onReceive() -- Starting MyService..." );
        context.startService(new Intent(context, MyService.class));
    }
}