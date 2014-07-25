package com.yofrom.yofrom;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class MyActivity extends Activity {
    Menu mMenu;
    App app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my);

        app = new App(this);

        app.start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);

        mMenu = menu;

        updateName();

        return true;
    }

    public void updateName() {
        if(mMenu != null) {
            mMenu.getItem(0).setTitle("You are " + app.getUsername());
            invalidateOptionsMenu();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        app.stopLocation();
    }

    @Override
    public void onResume() {
        super.onResume();
        app.startLocation();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            app.showSignUp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
