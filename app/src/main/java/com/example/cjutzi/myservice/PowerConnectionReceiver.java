package com.example.cjutzi.myservice;

import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Created by cjutzi on 11/15/17.
 */

public class PowerConnectionReceiver extends BroadcastReceiver implements AsyncActivityInterface
{
    String DEBUG_TAG=this.getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction() == Intent.ACTION_POWER_CONNECTED)
        {
            //Handle power connected
            Log.e(DEBUG_TAG,"power connected");
        }
        else
        if(intent.getAction() == Intent.ACTION_POWER_DISCONNECTED)
        {
            //Handle power disconnected
            Log.e(DEBUG_TAG,"power disconnected");
            try
            {
                Process proc = Runtime.getRuntime()
                        .exec(new String[]{ "su", "-c", "reboot -p" });
                proc.waitFor();
                return;
            }
            catch (Exception ex)
            {
                Log.e(DEBUG_TAG, "Shutdown Failed: " + ex.getMessage());
                Intent i = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
                intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try
                {
                    context.startActivity(i);
                    return;
                }
                catch (Exception ee)
                {
                    Log.e(DEBUG_TAG, "Shutdown REQUEST_SHUTDOWN Failed: " + ee.getMessage());

                    // AsyncActivity aa = new AsyncActivity(this);
                    // aa.doInBackground();

                    asyncActivityDo();
                }
            }
        }
        else
        {
        }

    }

    /**
     *
     */
    public void asyncActivityDo()
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                try
                {
                  Instrumentation inst = new Instrumentation();
                  inst.sendKeyDownUpSync(KeyEvent.KEYCODE_POWER);
                  Log.i(DEBUG_TAG,"KEYCODE_POWER pressed");
                  return;
                }
                catch (Exception e)
                {
                    Log.e(DEBUG_TAG,"KEYCODE_POWER Failed: "+e.getMessage());
                }
            }
        };

        new Thread(r).start();
        return;
    }
    ;
    public void asyncActivityComplete(){};
}
