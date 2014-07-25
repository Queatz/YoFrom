package com.yofrom.yofrom;

import android.app.Application;
import android.os.AsyncTask;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.PushService;

/**
 * Created by jacob on 7/16/14.
 */
public class MyApplication extends Application{
    @Override
    public void onCreate() {
        ParseObject.registerSubclass(User.class);
        Parse.initialize(this, "39R5Z3p67UaBZPFBbaolrE232Qvurmp0bUTTq4uY", "7baBEqEVXikWSKfzRf05RoRF2q9vfBSusWjt5vM5");
        PushService.setDefaultPushCallback(getApplicationContext(), MyActivity.class);
    }
}
