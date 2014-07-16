package com.atomiclabs.yofrom;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.PushService;

/**
 * Created by jacob on 7/16/14.
 */
public class MyApplication extends Application{
    @Override
    public void onCreate() {
        Parse.initialize(this, "39R5Z3p67UaBZPFBbaolrE232Qvurmp0bUTTq4uY", "7baBEqEVXikWSKfzRf05RoRF2q9vfBSusWjt5vM5");
        ParseObject.registerSubclass(User.class);
        PushService.setDefaultPushCallback(getApplicationContext(), MyActivity.class);
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }
}
