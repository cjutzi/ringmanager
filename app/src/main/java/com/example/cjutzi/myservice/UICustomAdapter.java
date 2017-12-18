package com.example.cjutzi.myservice;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;


public class UICustomAdapter extends ArrayAdapter<UIModel>
{
    UIModel[]                           modelItems = null;
    Context                             context;
    View.OnClickListener                cListener;
    View.OnLongClickListener            lcListener;
    AdapterView.OnItemSelectedListener  isListener;

    public UICustomAdapter(Context context,
                           View.OnClickListener clistener,
                           View.OnLongClickListener lcListener,
                           AdapterView.OnItemSelectedListener isListener,
                           UIModel[] resource)
    {
        super(context,R.layout.row,resource);
        // TODO Auto-generated constructor stub
        this.context = context;
        this.modelItems = resource;
        this.cListener = clistener;
        this.lcListener  = lcListener;
        this.isListener = isListener;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // TODO Auto-generated method stub
        LayoutInflater inflater = ((Activity)context).getLayoutInflater();
        convertView = inflater.inflate(R.layout.row, parent, false);

        TextView tv = (TextView) convertView.findViewById(R.id.row_name);
        tv.setText(modelItems[position].getName());
        tv.setOnClickListener(cListener);
        tv.setOnLongClickListener(lcListener);

        CheckBox cb = (CheckBox) convertView.findViewById(R.id.row_checkbox);
        cb.setOnClickListener(cListener);
        cb.setOnLongClickListener(lcListener);
        cb.setTag(tv.getText());

        tv = (TextView) convertView.findViewById(R.id.row_distance);
        tv.setText(modelItems[position].getDistance());


        /* drop down .. FULL and Vibrate..                  */
        /* manage this and put current value in selection   */

        Spinner spinner = (Spinner) convertView.findViewById(R.id.row_type);
        String[] spinnerList = { LatLng.RING_TYPE.FULL.toString(), LatLng.RING_TYPE.VIBRATE.toString() };
//        ArrayAdapter<String> spinnerAdaptor = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, spinnerList);
//        spinnerAdaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<String> spinnerAdaptor = new ArrayAdapter<String>(context, R.layout.myservice_spinner_item, spinnerList);
        spinnerAdaptor.setDropDownViewResource(R.layout.myservice_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdaptor);
        spinner.setTag(modelItems[position].getName());

        int spinnerPosition = spinnerAdaptor.getPosition(modelItems[position].getType());
        spinner.setSelection(spinnerPosition);
        spinner.setOnItemSelectedListener(isListener);


        if(modelItems[position].getValue() == 1)
            cb.setChecked(true);
        else
            cb.setChecked(false);
        return convertView;
    }


}