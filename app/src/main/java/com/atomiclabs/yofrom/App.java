package com.atomiclabs.yofrom;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SendCallback;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by jacob on 7/16/14.
 */
public class App {
    Activity activity;
    AsyncHttpClient http;
    ArrayList<String> list;
    SharedPreferences preferences;
    FriendsAdapter friendsAdapter;
    User user;

    public App(Activity act) {
        activity = act;
        http = new AsyncHttpClient();
        preferences = activity.getSharedPreferences("com.atomiclabs.yofrom", Context.MODE_PRIVATE);
    }

    public void start() {
        String token = preferences.getString("username", null);

        if(token == null) {
            showSignUp();
        }
        else {
            user = new User();
            user.put("username", token);
            user.saveInBackground();
            subscribeUser();

            refreshNearbyList();
        }
    }

    void subscribeUser() {
        ParseInstallation.getCurrentInstallation().put("username", user.getString("username"));
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }

    void showSignUp() {
        activity.setContentView(R.layout.signup);

        final EditText ed = ((EditText) activity.findViewById(R.id.name));

        ed.requestFocus();
        ed.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    signUp(textView.getText().toString());
                }

                return false;
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Service.INPUT_METHOD_SERVICE);

                imm.showSoftInput(ed, 0);
            }
        }, 100);
    }

    void signUp(String name) {
        activity.setContentView(R.layout.activity_my);

        user = new User();
        user.put("username", name);
        user.saveInBackground();
        subscribeUser();

        preferences.edit().putString("username", name).commit();
        refreshNearbyList();
    }

    void refreshNearbyList() {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        double lat = location == null ? 0 : location.getLatitude(), lon = location == null ? 0 : location.getLongitude();
        String key = "AIzaSyDK_xWSTc3aaESvutnPBN4oggfGCT-0eNM";

        http.get("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + lat + "," + lon + "&radius=250&key=" + key, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                list = new ArrayList<String>();

                try {
                    JSONArray results = new JSONObject(new String(response)).getJSONArray("results");

                    for (int i = 0; i < results.length(); i++) {

                        try {
                            list.add(results.getJSONObject(i).getString("name"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                showNearbyList();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Toast.makeText(activity, "Network failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    void showNearbyList() {
        activity.setContentView(R.layout.nearby_places);

        ListView listView = (ListView) activity.findViewById(R.id.list);
        final NearbyAdapter adapter = new NearbyAdapter(activity, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showFriends(adapter.getItem(i));
            }
        });
    }

    void showFriends(final String locationName) {
        activity.setContentView(R.layout.friends);

        ((TextView) activity.findViewById(R.id.context)).setText(locationName);

        ArrayList<String> friends = getFriends();

        ListView listView = (ListView) activity.findViewById(R.id.list);

        friendsAdapter = new FriendsAdapter(activity, friends, new FriendsAdapter.OnAddFriendCallback() {
            @Override
            public void onAddFriend(String friend) {
                addFriend(friend);
            }
        });

        listView.setAdapter(friendsAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String friend = friendsAdapter.getItem(i);
                sendPush(locationName, friend);
            }
        });
    }

    ArrayList<String> getFriends() {
        ArrayList<String> friends = new ArrayList<String>();

        String s = preferences.getString("friends", null);

        if(s != null) {
            for(String s1 : s.split(";"))
                friends.add(s1);
        }

        return friends;
    }

    ArrayList<String> addFriend(String friend) {
        ArrayList<String> friends = getFriends();

        friends.add(friend);

        String s;

        if(friends.size() > 0) {
            s = friends.get(0);

            for (int i = 1; i < friends.size(); i++)
                s += ";" + friends.get(i);
        }
        else
            s = "";

        preferences.edit().putString("friends", s).commit();

        if(friendsAdapter != null) {
            friendsAdapter.setList(friends);
        }

        return friends;
    }

    void sendPush(String loc, String to) {
        ParseQuery pushQuery = ParseInstallation.getQuery().whereMatches("username", to);

        // Send push notification to query
        ParsePush push = new ParsePush();
        push.setQuery(pushQuery); // Set our Installation query
        push.setMessage(user.get("username") + " @ " + loc);
        push.sendInBackground(new SendCallback() {
            @Override
            public void done(ParseException e) {
                Toast.makeText(activity, "Yo sent!", Toast.LENGTH_SHORT).show();
                Log.i("yofrom", "Yo sent!");
            }
        });
    }
}
