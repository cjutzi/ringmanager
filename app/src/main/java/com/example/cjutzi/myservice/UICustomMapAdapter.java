package com.example.cjutzi.myservice;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Collection;
import java.util.Map;

/**
 * Created by cjutzi on 12/22/17.
 */

public class UICustomMapAdapter extends BaseAdapter
{

    Context  m_context = null;
    int      m_size    = 0;
    String[] m_value   = null;
    String[] m_key     = null;

    /**
     *
     * @param context
     * @param baseMap
     */
    public UICustomMapAdapter(Context context, Map<String, Long> baseMap)
    {
        Map<String,Long> sortedBaseMap = Util.SortMapStringLong(baseMap);
        m_size = sortedBaseMap.size();

        m_context = context;
        m_value   = new String[m_size];
        m_key     = new String[m_size];

        Collection<Long> longCol = sortedBaseMap.values();
        Collection<String> keyCol =  sortedBaseMap.keySet();
        String keys[] = keyCol.toArray(new String[0]);
        Long   vals[] = longCol.toArray(new Long[0]);

        for (int i = 0 ; i< m_size; i++)
        {
            m_value[i] = Util.formatTimeDelta(0,vals[i]);
            m_key[i]   = keys[i];
        }
    }
    @Override
    public int getCount()
    {
        return m_size;
    }

    @Override
    public Object getItem(int position)
    {
        return m_value[position];
    }

    @Override
    public long getItemId(int arg0)
    {
        return arg0;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent)
    {


        LayoutInflater inflater = ((Activity)m_context).getLayoutInflater();
        convertView = inflater.inflate(R.layout.row_two_col, parent, false);

        String key = m_key[pos];
        String value = getItem(pos).toString();

        // TODO replace findViewById by ViewHolder
        ((TextView) convertView.findViewById(R.id.text_left)).setText(key);
        ((TextView) convertView.findViewById(R.id.text_right)).setText(value);

        return convertView;
    }
}
