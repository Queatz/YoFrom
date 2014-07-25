package com.yofrom.yofrom;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by jacob on 7/16/14.
 */
public class FriendsAdapter extends BaseAdapter {
    ArrayList<String> list;
    Context context;
    OnAddFriendCallback callback;

    public abstract static class OnAddFriendCallback {
        public abstract void onAddFriend(String friend);
    }

    public FriendsAdapter(Context c, ArrayList<String> l, OnAddFriendCallback onAdd) {
        context = c;
        list = l;
        callback = onAdd;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int getCount() {
        return list.size() + 1;
    }

    @Override
    public String getItem(int position) {
        if(position < list.size())
            return list.get(position);
        return null;
    }

    public void setList(ArrayList<String> l) {
        list = l;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convert, ViewGroup parent) {
        boolean add = position >= list.size();

        boolean recreate = convert == null ||
                add && convert.findViewById(R.id.add) == null ||
                !add && convert.findViewById(R.id.name) == null;

        if(recreate) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convert = inflater.inflate(add ? R.layout.add_friend : R.layout.nearby_place, null);
        }

        if(add) {
            EditText t = (EditText) convert.findViewById(R.id.add);
            t.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if(i == EditorInfo.IME_ACTION_DONE) {
                        callback.onAddFriend(textView.getText().toString());
                        textView.setText("");
                    }

                    return false;
                }
            });
        }
        else {
            TextView name = (TextView) convert.findViewById(R.id.name);
            name.setText(list.get(position));
        }

        return convert;
    }
}
