package com.example.cjutzi.myservice;

import android.os.AsyncTask;
import android.util.Log;

public class AsyncActivity extends AsyncTask<Void, Void, Void>
{
    String DEBUG_TAG=this.getClass().getSimpleName();


    AsyncActivityInterface m_callback = null;
    boolean f_active = false;
    
    public AsyncActivity(AsyncActivityInterface callback )
    {
        m_callback = callback ; 
    }

    @Override
    protected Void doInBackground(Void... params)
    {
        Log.i(DEBUG_TAG, "doInBackground.before call");
        m_callback.asyncActivityDo();
        Log.i(DEBUG_TAG, "doInBackground.before after cal");
        m_callback.asyncActivityComplete();
        Log.i(DEBUG_TAG, "doInBackground.before complete");
        return null;
    }


}
