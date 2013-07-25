package com.unitychat;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.unitychat.common.UnityChatHelper;
import com.unitychat.models.Friend;
import com.unitychat.models.Friend.Status;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UnityChatHelper helper = new UnityChatHelper(this, R.id.drawer,
                false, 0, true, "Game");
        helper.addNewFriend(new Friend("Ty", "You suck at this!",
                Status.INACTIVE));
        helper.addNewFriend(new Friend("Garrett", "You suck at this!",
                Status.ACTIVE));
        helper.addNewFriend(new Friend("Dee", "You suck at this!",
                Status.INGAME));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection Add Other Item Later
        return true;
    }
}