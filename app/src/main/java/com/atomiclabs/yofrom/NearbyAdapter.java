package com.atomiclabs.yofrom;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by jacob on 7/16/14.
 */
public class NearbyAdapter extends BaseAdapter {
    ArrayList<String> list;
    Context context;

    public NearbyAdapter(Context c, ArrayList<String> l) {
        context = c;
        list = l;
    }

    @Override
    public long getItemId(int position) {
        return list.get(position).hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public String getItem(int position) {
        return list.get(position);
    }

    @Override
    public View getView(int position, View convert, ViewGroup parent) {
        if(convert == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convert = inflater.inflate(R.layout.nearby_place, null);
        }

        TextView name = (TextView) convert.findViewById(R.id.name);

        name.setText(list.get(position));

        return convert;
    }
}
