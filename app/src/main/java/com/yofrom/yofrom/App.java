package com.yofrom.yofrom;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
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
    LocationManagerHelper locationReporting;
    User user;
    int page;

    public class LocationManagerHelper implements LocationListener {
        App app;
        int count;

        public LocationManagerHelper(App a) {
            app = a;
        }

        @Override
        public void onLocationChanged(Location loc) {
            if(app.page != R.layout.nearby_places)
                return;

            if(count < 2)
                app.refreshNearbyList();
            else
                app.stopLocation();

            count++;
        }

        @Override
        public void onProviderDisabled(String provider) { }

        @Override
        public void onProviderEnabled(String provider) { }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }

    }

    public App(Activity act) {
        activity = act;
        http = new AsyncHttpClient();
        preferences = activity.getSharedPreferences("com.yofrom.yofrom", Context.MODE_PRIVATE);
        locationReporting = new LocationManagerHelper(this);
        page = 0;
    }

    public void start() {
        String token = preferences.getString("username", null);

        if(token == null) {
            showSignUp();
        }
        else {
            subscribeUser(token);

            refreshNearbyList();
        }
    }

    public String getUsername() {
        if(user == null)
            return null;

        return user.getString("username");
    }

    void subscribeUser(String name) {
        user = new User();
        user.put("username", name);

        ParseInstallation.getCurrentInstallation().put("username", name);
        ((MyActivity) activity).updateName();
        ParseInstallation.getCurrentInstallation().saveEventually();
    }

    void showSignUp() {
        page = R.layout.signup;
        activity.setContentView(page);

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
        page = R.layout.activity_my;
        activity.setContentView(page);

        preferences.edit().putString("username", name).commit();

        subscribeUser(name);

        refreshNearbyList();
    }

    void refreshNearbyList() {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        Location location;

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        else {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Toast.makeText(this.activity, "GPS is disabled", Toast.LENGTH_SHORT).show();
        }

        startLocation();

        double lat = location == null ? 0 : location.getLatitude(), lon = location == null ? 0 : location.getLongitude();
        String key = "AIzaSyDK_xWSTc3aaESvutnPBN4oggfGCT-0eNM";

        http.get("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + lat + "," + lon + "&radius=500&key=" + key, new AsyncHttpResponseHandler() {
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
        page = R.layout.nearby_places;
        activity.setContentView(page);

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
        page = R.layout.friends;
        activity.setContentView(page);

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

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
                new AlertDialog.Builder(activity)
                        .setTitle(activity.getString(R.string.delete))
                        .setPositiveButton(activity.getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i2) {
                                delFriend(i);
                            }
                        })
                        .setNegativeButton(activity.getString(R.string.no), null)
                        .show();
                return true;
            }
        });
    }

    ArrayList<String> getFriends() {
        ArrayList<String> friends = new ArrayList<String>();

        String s = preferences.getString("friends", null);

        if(s != null && s.length() > 0) {
            for(String s1 : s.split(";"))
                friends.add(s1);
        }

        return friends;
    }

    void saveFriends(ArrayList<String> friends) {
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
    }

    void delFriend(int i) {
        ArrayList<String> friends = getFriends();

        friends.remove(i);

        saveFriends(friends);
    }

    ArrayList<String> addFriend(String friend) {
        ArrayList<String> friends = getFriends();

        friends.add(friend);

        saveFriends(friends);

        return friends;
    }

    void sendPush(final String loc, final String to) {
        ParseQuery pushQuery = ParseInstallation.getQuery().whereEqualTo("username", to);

        // Send push notification to query
        ParsePush push = new ParsePush();
        push.setQuery(pushQuery); // Set our Installation query
        push.setMessage(user.get("username") + " at " + loc);
        push.sendInBackground(new SendCallback() {
            @Override
            public void done(ParseException e) {
                Toast.makeText(activity, "Yo sent!", Toast.LENGTH_SHORT).show();
                Log.i("yofrom", "Yo sent! (to: " + to + ")");
            }
        });
    }

    void stopLocation() {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        locationManager.removeUpdates(locationReporting);
    }

    void startLocation() {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationReporting);
    }
}
