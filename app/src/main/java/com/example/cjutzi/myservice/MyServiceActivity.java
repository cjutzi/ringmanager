package com.example.cjutzi.myservice;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cjutzi.myservice.MyService.MyLocalBinder;
import com.example.cjutzi.myservice.MyService.MyService_NOTIFY_ACTION_COMMAND;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by cjutzi on 11/13/17.
 */

public class MyServiceActivity  extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener, AdapterView.OnItemSelectedListener
{
    String DEBUG_TAG=this.getClass().getSimpleName();

    /*
     * current local state stuff.
    */
    public  static Context                      myContext         = null;
    public  static Activity                     myActivity        = null;
    private ServiceConnection                   m_myServiceConn   = null;
    boolean                                     m_isBound         = false;
    Intent                                      m_myServiceIntent = null;
    static  MyService                           m_myService       = null;

    private Boolean m_sleepUtilProx      = false;
    private Boolean m_trackIdle          = true;
    private Boolean m_trackProxTrigger   = false;
    private Boolean m_trackLocations     = true;

//
//    Boolean              m_checkBox1;
//    Boolean              m_checkBox2;
//    Boolean              m_checkBox3;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.i(DEBUG_TAG, "MyServiceActivity : onCreate)");

        restoreStuff();

        myContext = this;
        myActivity = this;
        m_myServiceIntent = new Intent(myContext, MyService.class);

         if (!m_isBound);
        {
            m_myServiceConn = new SvcConnection();
            startService(m_myServiceIntent);
            bindService(m_myServiceIntent, m_myServiceConn, Context.BIND_ABOVE_CLIENT);
        }
        /*
         * MENU STUFF
         *
         */
        setContentView(R.layout.activity_main);

        /* setup button handling in menu */
        Button      m_button;
        m_button = (Button) findViewById(R.id.getLocations);
        m_button.setOnClickListener(this);
        m_button = (Button) findViewById(R.id.getHistory);
        m_button.setOnClickListener(this);
        m_button = (Button) findViewById(R.id.addcurloc_vibrate);
        m_button.setOnClickListener(this);
        m_button = (Button) findViewById(R.id.addcurloc_full);
        m_button.setOnClickListener(this);
        m_button = (Button) findViewById(R.id.stopService);
        m_button.setOnClickListener(this);
        m_button = (Button) findViewById(R.id.show_cur_map);
        m_button.setOnClickListener(this);


        /*
        m_button = (Button) findViewById(R.id.asynctask);
        m_button.setOnClickListener(this);

        m_button = (Button) findViewById(R.id.startService);
        m_button.setOnClickListener(this);

        */
        m_button = (Button) findViewById(R.id.tracks_clear);
        m_button.setOnClickListener(this);

        m_button = (Button) findViewById(R.id.getTimeSpentWhere);
        m_button.setOnClickListener(this);

        CheckBox rb;
        rb = (CheckBox) findViewById(R.id.sleep_until_prox);
        rb.setOnClickListener(this);
        LocationReceiver.Configure(LocationReceiver.LocationReceiver_CONFIGURATION.SLEEP_UNTIL_PROX,m_sleepUtilProx.toString());
        rb.setChecked(m_sleepUtilProx);

        rb = (CheckBox) findViewById(R.id.track_idle);
        rb.setOnClickListener(this);
        LocationReceiver.Configure(LocationReceiver.LocationReceiver_CONFIGURATION.TRACK_IDLE,m_trackIdle.toString());
        rb.setChecked(m_trackIdle);

        rb = (CheckBox) findViewById(R.id.track_proxy_trigger);
        rb.setOnClickListener(this);
        LocationReceiver.Configure(LocationReceiver.LocationReceiver_CONFIGURATION.TRACK_PROXIMITY_TRIGGER,m_trackProxTrigger.toString());
        rb.setChecked(m_trackProxTrigger);

        rb = (CheckBox) findViewById(R.id.track_locations);
        rb.setOnClickListener(this);
        LocationReceiver.Configure(LocationReceiver.LocationReceiver_CONFIGURATION.TRACK_LOCATIONS,m_trackLocations.toString());
        rb.setChecked(m_trackLocations);




    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Log.i(DEBUG_TAG, "MyServiceActivity : onResme)");
    }


    /**
     *
     */
    private void restoreStuff()
    {
        HashMap<String,Object> hashMap = SaveRestore.restoreStuff(this.getExternalFilesDir("/").toString(), DEBUG_TAG);
        m_sleepUtilProx    = (hashMap==null || hashMap.get("m_sleepUtilProx")   ==null)?false:new Boolean((String)hashMap.get("m_sleepUtilProx"));
        m_trackIdle        = (hashMap==null || hashMap.get("m_trackIdle")       ==null)?true: new Boolean((String)hashMap.get("m_trackIdle"));
        m_trackProxTrigger = (hashMap==null || hashMap.get("m_trackProxTrigger")==null)?false:new Boolean((String)hashMap.get("m_trackProxTrigger"));
        m_trackLocations   = (hashMap==null || hashMap.get("m_trackLocations")  ==null)?true :new Boolean((String)hashMap.get("m_trackLocations"));

    }

    /**
     *
     */
    @Override
    protected void onDestroy()
    {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.i(DEBUG_TAG, "MyServiceActivity : onDestroy()");
        if (this.m_isBound)
        {
            unbindService(m_myServiceConn);
            m_isBound=false;
        }
        finish();
    }

    /**
     *
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        Log.i(DEBUG_TAG, "MyServiceActivity : onPause()");
        if (this.m_isBound)
        {
            unbindService(m_myServiceConn);
            m_isBound=false;
        }
        HashMap<String,Object> hashMap = new HashMap<String,Object>();

        hashMap.put("m_sleepUtilProx",m_sleepUtilProx.toString());
        hashMap.put("m_trackIdle",m_trackIdle.toString());
        hashMap.put("m_trackProxTrigger",m_trackProxTrigger.toString());
        hashMap.put("m_trackLocations",m_trackLocations.toString());
        SaveRestore.saveStuff(this.getExternalFilesDir("/").toString(), hashMap, DEBUG_TAG);
        finish();
    }

    /**
     *
     */
    private void setupHistoryMenu()
    {
        ArrayList<String> history = LocationReceiver.getLocationActivityHistory();

        if (history == null || history.size() == 0)
            return;

        setContentView(R.layout.simple_listview);
        ListView lv=(ListView)findViewById(R.id.simplelistview);
        lv.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1 , history)
            {
                @Override
                public View getView(int position, View convertView, ViewGroup parent){
                    // Get the current item from ListView
                    View view = super.getView(position,convertView,parent);
                    view.setPadding(0,0,0,0);
//                  // Get the Layout Parameters for ListView Current Item View
//                  ViewGroup.LayoutParams params = view.getLayoutParams();
//
//                  // Set the height of the Item View
//                  params.height = 50;
//                  view.setLayoutParams(params);

                    return view;
                }
            });

    }


    /**
     *
     */
    private void setupTimeSpentWhere()
    {
        Map<String, Long> myMap = LocationMatch.getTimeSpentWhereMap();
        if (myMap == null || myMap.size() == 0)
            return;

        setContentView (R.layout.simple_listview);
        ListView lv=(ListView)findViewById(R.id.simplelistview);
        UICustomMapAdapter adapter = new UICustomMapAdapter(this, myMap);
        lv.setAdapter(adapter);

//        ArrayList<String> timeSpentWhere = LocationMatch.getTimeSpentWhere();
//
//        if (timeSpentWhere == null || timeSpentWhere.size() == 0)
//            return;
//
//        setContentView(R.layout.simple_listview);
//        ListView lv=(ListView)findViewById(R.id.simplelistview);
//        lv.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1 , timeSpentWhere)
//        {
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent){
//                // Get the current item from ListView
//                View view = super.getView(position,convertView,parent);
//                view.setPadding(0,0,0,0);
////                    // Get the Layout Parameters for ListView Current Item View
////                    ViewGroup.LayoutParams params = view.getLayoutParams();
////
////                    // Set the height of the Item View
////                    params.height = 50;
////                    view.setLayoutParams(params);
//
//                return view;
//            }
//        });

    }
    /**
     * this sets up the list current locaitons menu
     */
    private void setupLocationMenu()
    {

        /* add stuff */
        ListView lv;
        UIModel[] modelItems;
        setContentView(R.layout.locationmatch_main);
        lv = (ListView) findViewById(R.id.listView1);

        // place lat long in first row
        TextView tv    = (TextView) findViewById(R.id.locLatlng);
        tv.setOnClickListener(this);

        LatLng latLngCur = LocationReceiver.getCurrentLoc();
        ArrayList<LatLng> locations;

        if (latLngCur != null)
        {
            String latLngTxt = String.format("%8.4f,%8.4f : current location press or long press to launch", latLngCur.lat, latLngCur.lng);
            tv.setText(latLngTxt);
            locations = LocationMatch.getLocationWithDistance(latLngCur.lat, latLngCur.lng, latLngCur.lastAccuracy);
        }
        else
        {
           locations = LocationMatch.getLocaitons();
        }

        // build UI
        modelItems = new UIModel[locations.size()];
        int i=0;

        for (LatLng entry : locations)
        {
            String a = String.format("%-20s\tdist(%8d)\ttype(%10s)",entry.name,entry.lastDistMeter,entry.ringType);
            modelItems[i] = new UIModel(entry.name, entry.ringType.toString(), NumberFormat.getNumberInstance(Locale.US).format(entry.lastDistMeter), entry.factive?1:0);
            i++;
        }

        UICustomAdapter adapter = new UICustomAdapter( this,
                                                       this,
                                                       this,
                                                        this,
                                                        modelItems);

        lv.setAdapter(adapter);
        Log.i(DEBUG_TAG, "onClick.getLocations()");
    }

    /**
     *
     * @param view
     */
    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            /* menu for active click on notification */

            /* target current lat/lng */
            case R.id.locLatlng:
                TextView tv = (TextView)view.findViewById(R.id.locLatlng);
                String text = (String)tv.getText();
                String[] textblock = text.split(",");
                float latitude = new Float(textblock[0]);
                textblock = textblock[1].split(":");
                float longitude = new Float(textblock[0]);
                String uri = String.format( "geo:%f,%f", latitude, longitude);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                myContext.startActivity(intent);
                break;

            /* pull up location menu button */
            case R.id.getHistory:
                setupHistoryMenu();
                break;

            case R.id.getTimeSpentWhere:
                setupTimeSpentWhere();
                break;

            case R.id.getLocations:
                setupLocationMenu();
                break;

            case R.id.row_name:
                TextView tv1 = view.findViewById(R.id.row_name);
                Log.i(DEBUG_TAG,"Hello:"+tv1.getText());
                break;

//            case R.id.row_type:
//                Spinner spinner = (Spinner)view.findViewById(R.id.row_type);
//                Log.i(DEBUG_TAG,"row_type:"+spinner.getSelectedItem());
//                break;

            case R.id.row_checkbox:
                CheckBox cb = (CheckBox) view.findViewById(R.id.row_checkbox);
                String name = (String)cb.getTag();
                LocationMatch.activateLocation(name,cb.isChecked());
                Log.i(DEBUG_TAG,"Hello:"+cb.isChecked());
                break;


            /* menu add current locations */
            case R.id.addcurloc_vibrate:
            case R.id.addcurloc_full:
                {
                    Log.i(DEBUG_TAG, "onClick.addcurloc_silent/addcurloc_vib()");
                    setContentView(R.layout.addloc);

                    Button cancel = (Button) findViewById(R.id.addloc_cancel);
                    cancel.setOnClickListener(this);

                    Button btnok = (Button) findViewById(R.id.addloc_ok);
                    btnok.setOnClickListener(this);
                    CheckBox chkBoxVib = (CheckBox) findViewById(R.id.vibrate);
                    chkBoxVib.setOnClickListener(this);
                    CheckBox chkBoxRng = (CheckBox) findViewById(R.id.full_ring);
                    chkBoxRng.setOnClickListener(this);
                    EditText distText = (EditText) findViewById(R.id.boundary);
                    distText.setText("100"); // set to 100 meters by default
                    CheckBox chkActive = (CheckBox) findViewById(R.id.active);
                    chkActive.setChecked(true);
                    Button m_button = (Button) findViewById(R.id.check_current_loc);
                    m_button.setOnClickListener(this);
                    EditText gpsText = (EditText) findViewById(R.id.gps);
                    LatLng latLng = LocationReceiver.getCurrentLoc();

                    if (latLng != null)
                        gpsText.setText(String.format("%8.4f,%8.4f", latLng.lat, latLng.lng));
                    else
                        gpsText.setText(String.format("%8.4f,%8.4f", -1.0, -1.0));

                    switch (view.getId())
                    {
                        case R.id.addcurloc_vibrate:
                            chkBoxVib.setChecked(true);
                            chkBoxRng.setChecked(false);
                            break;
                        case R.id.addcurloc_full:
                            chkBoxVib.setChecked(false);
                            chkBoxRng.setChecked(true);
                            break;
                    }
                }
                break;

            case R.id.addloc_cancel:
                this.onBackPressed();
                break;

            case R.id.check_current_loc:
                {
                    RadioButton rb = (RadioButton) findViewById(R.id.check_current_loc);
                    EditText gpsText = (EditText) findViewById(R.id.gps);
                    String   gpsTextStr = gpsText.getText().toString();
                    float lat;
                    float lng;
                    try {
                        String str[] = gpsTextStr.split(",");
                        lat = new Float(str[0]);
                        lng = new Float(str[1]);
                    }
                    catch (Exception e)
                    {
                        Toast.makeText(myContext, "Bad Format on GPS location", Toast.LENGTH_LONG).show();
                        break;
                    }
                    uri = String.format( "geo:%f,%f", lat, lng);
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    myContext.startActivity(intent);

//                    if (rb.isChecked())
//                    {
//                        LatLng latLng = LocationReceiver.getCurrentLoc();
//                        gpsText.setText(String.format("%8.4f,%8.4f", latLng.lat, latLng.lng));
//                    }
//                    else
//                    {
//                        gpsText.setText("");
//                    }
                }
                break;
            case R.id.vibrate:
                {
                    CheckBox chkBoxVibr = (CheckBox) findViewById(R.id.vibrate);
                    CheckBox chkBoxFullr = (CheckBox) findViewById(R.id.full_ring);
                    if (chkBoxVibr.isChecked())
                        chkBoxFullr.setChecked(false);
                    else
                        chkBoxFullr.setChecked(true);
                }
                break;

            case R.id.full_ring:
                {
                    CheckBox chkBoxVibr    = (CheckBox) findViewById(R.id.vibrate);
                    CheckBox chkBoxFullr   = (CheckBox) findViewById(R.id.full_ring);
                    if (chkBoxFullr.isChecked())
                        chkBoxVibr.setChecked(false);
                    else
                        chkBoxVibr.setChecked(true);
                }
                break;

            case R.id.addloc_ok:
                {
                    CheckBox chkBoxVibr    = (CheckBox) findViewById(R.id.vibrate);
                    CheckBox chkBoxFullr   = (CheckBox) findViewById(R.id.full_ring);
                    CheckBox chkBoxActiver = (CheckBox) findViewById(R.id.active);
                    EditText editText      = (EditText) findViewById(R.id.locName);
                    String locName         = editText.getText().toString();
                    EditText distTextr     = (EditText) findViewById(R.id.boundary);
                    String boundary        = distTextr.getText().toString();
                    Boolean factive        = chkBoxActiver.isChecked();
                    EditText gpsText       = (EditText) findViewById(R.id.gps);
                    String   gpsTextStr    = gpsText.getText().toString();
                    Float    lat;
                    Float    lng;

                    Integer triggerDis = null;
                    try
                    {
                        triggerDis = new Integer(boundary);
                    }
                    catch (Exception e)
                    {
                        distTextr.setTextColor(Color.rgb(200,0,0));
                        distTextr.setText("!!! Needs to be an Integer !!!");
                        break;
                    }
                    if (gpsTextStr.isEmpty())
                    {
                        gpsText.setTextColor(Color.rgb(200,0,0));
                        gpsText.setText("!!! Add GPS location !!!");
                        break;
                    }
                    try {
                        String str[] = gpsTextStr.split(",");
                        lat = new Float(str[0]);
                        lng = new Float(str[1]);
                    }
                    catch (Exception e)
                    {
                        Toast.makeText(myContext, "Bad Format on GPS location", Toast.LENGTH_LONG).show();
                        break;
                    }

                    if (locName == null || locName.isEmpty() || locName.equals("!!! ADD NAME PLEASE !!!"))
                    {
                        editText.setTextColor(Color.rgb(200,0,0));
                        editText.setText("!!! ADD NAME PLEASE !!!");
                        break;
                    }

                    if (chkBoxVibr.isChecked())
                    {
                        if (!LocationMatch.addLocation(locName, triggerDis, factive, lat, lng, LatLng.RING_TYPE.VIBRATE))
                        {
                            Toast.makeText(myContext, "Current Location can not be established", Toast.LENGTH_LONG).show();
                        }
                    }
                    else
                    if (chkBoxFullr.isChecked())
                    {
                        if (!LocationMatch.addLocation(locName, triggerDis, factive, lat, lng, LatLng.RING_TYPE.FULL))
                        {
                            Toast.makeText(myContext, "Current Location can not be established", Toast.LENGTH_LONG).show();
                        }
                    }
                    this.onBackPressed();
                    break;
                }

            case R.id.show_cur_map:
                LatLng latLng = LocationReceiver.getCurrentLoc();
                if (latLng != null)
                {
                    uri = String.format("geo:%f,%f", latLng.lat, latLng.lng);
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    myContext.startActivity(intent);
                }
                Log.i(DEBUG_TAG, "onClick.show_cur_map()");
                break;

            case R.id.stopService:
                Log.i(DEBUG_TAG, "onClick.stopService()");
                m_myService.notificationReceiver(MyService_NOTIFY_ACTION_COMMAND.ACTIVITY_STOPSVC, null);
                break;


            /* stuff going to the Locaiton Receiver directly */
            case R.id.sleep_until_prox:
                Log.i(DEBUG_TAG, "onClick.sleep_until_prox()");
                {
                    CheckBox radioButton    = (CheckBox) findViewById(R.id.sleep_until_prox);
                    boolean isChecked = radioButton.isChecked();
                    m_sleepUtilProx  = isChecked;

                    LocationReceiver.Configure(LocationReceiver.LocationReceiver_CONFIGURATION.SLEEP_UNTIL_PROX,m_sleepUtilProx.toString());
                }
                break;

            case R.id.track_idle:
                Log.i(DEBUG_TAG, "onClick.track_idle()");
                    {
                        CheckBox radioButton    = (CheckBox) findViewById(R.id.track_idle);
                        boolean isChecked = radioButton.isChecked();
                        m_trackIdle  = isChecked;

                        LocationReceiver.Configure(LocationReceiver.LocationReceiver_CONFIGURATION.TRACK_IDLE,m_trackIdle.toString());
                    }
                    break;

            case R.id.track_locations:
                    Log.i(DEBUG_TAG, "onClick.m_track_locations()");
                    {
                        CheckBox checkbox    = (CheckBox) findViewById(R.id.track_locations);
                        boolean isChecked = checkbox.isChecked();
                        m_trackLocations = isChecked;
                        LocationReceiver.Configure(LocationReceiver.LocationReceiver_CONFIGURATION.TRACK_LOCATIONS,m_trackLocations.toString());
                    }
                    break;

            case R.id.track_proxy_trigger:
                    Log.i(DEBUG_TAG, "onClick.m_track_proxy_trigger()");
                    {
                        CheckBox checkbox    = (CheckBox) findViewById(R.id.track_proxy_trigger);
                        boolean isChecked = checkbox.isChecked();
                        m_trackProxTrigger = isChecked;
                        LocationReceiver.Configure(LocationReceiver.LocationReceiver_CONFIGURATION.TRACK_PROXIMITY_TRIGGER,m_trackProxTrigger.toString());
                    }
                    break;

            case R.id.tracks_clear:
                    Log.i(DEBUG_TAG, "onClick.tracks_clear()");

                    AlertDialog.Builder builder;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    {
                        builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                    }
                    else
                    {
                        builder = new AlertDialog.Builder(this);
                    }

                    builder.setTitle("Delete Tracker Log?")
                            .setMessage("Delete Tracker Log?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.i(DEBUG_TAG, "TRACK_CLEAR -- deleted"+ dialog.toString());
                                    LocationReceiver.Configure(LocationReceiver.LocationReceiver_CONFIGURATION.TRACK_CLEAR,null);
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    break;

            default:
                Log.i(DEBUG_TAG, "onClick.default()");
            break;
        }
    }


    String m_f_todelete = "";

    /**
     * LongClick on the list menu from the locations (for delete / activate etc.. )
     * @param view
     * @return
     */
    @Override
    public boolean onLongClick(View view)
    {
        switch (view.getId())
        {
            case R.id.locLatlng:
                TextView tv = (TextView)view.findViewById(R.id.locLatlng);
                String text = (String)tv.getText();
                String[] textblock = text.split(",");
                float latitude = new Float(textblock[0]);
                textblock = textblock[1].split(":");
                float longitude = new Float(textblock[0]);
                String uri = String.format( "geo:%f,%f", latitude, longitude);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                myContext.startActivity(intent);
                break;

            case R.id.row_name:
                TextView tv1 = view.findViewById(R.id.row_name);
                String name = LocationMatch.getLocationKey(tv1.getText().toString());
                m_f_todelete = name;


                AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                {
                    builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                }
                else
                {
                    builder = new AlertDialog.Builder(this);
                }
                builder.setTitle("Delete entry: "+name)
                        .setMessage("Delete \""+name+"\"")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i(DEBUG_TAG, "-->"+ dialog.toString());
                                LatLng latLng = LocationMatch.deleteLocation(m_f_todelete);
                                setupLocationMenu();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                setupLocationMenu();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                break;

            case R.id.row_checkbox:
                CheckBox cb = (CheckBox) view.findViewById(R.id.row_checkbox);
                Log.i(DEBUG_TAG,"Hello:"+cb.isChecked());
                break;

        }
        return false;
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        String selected = parent.getItemAtPosition(position).toString();
        String name = (String)parent.getTag();
        Log.i(DEBUG_TAG, "onItemSelected ("+name+")");

        LocationMatch.updateLocation(name, LatLng.RING_TYPE.valueOf(selected));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {
        Log.i(DEBUG_TAG, "onNothingSelected");
    }

    /*
     * SERVICE CONNECT
     */

    public class SvcConnection implements ServiceConnection
    {
        @Override
        public void onBindingDied(ComponentName className)
        {
            Log.e(DEBUG_TAG, "Service binding Died: " + className);
            m_isBound = false;
        }
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            Log.d(DEBUG_TAG, "Service connected: " + className);

            if (!m_isBound)
            {
                MyLocalBinder binder = (MyLocalBinder) service;
                m_myService = binder.getService();
                m_myService.notificationReceiver(MyService_NOTIFY_ACTION_COMMAND.ACTIVITY_CONNECTED, null);

                /* configure what we have.. */

                m_isBound = true;
            }
            else
            {
                Log.e(DEBUG_TAG, "onServiceConnected was connected isBound is true -- connected: " + className);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className)
        {
            Log.d(DEBUG_TAG, "Service disconnected: " + className);
            if (!m_isBound)
            {
                Log.e(DEBUG_TAG, "onServiceDisconnected was called isBound is flase -- Disconnected: " + className);

            }
            m_isBound = false;
        }
    }
}