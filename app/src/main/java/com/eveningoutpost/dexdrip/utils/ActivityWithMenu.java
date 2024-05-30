package com.eveningoutpost.dexdrip.utils;

import android.content.Intent;
import android.os.Bundle;


import androidx.drawerlayout.widget.DrawerLayout;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.NavDrawerBuilder;
import com.eveningoutpost.dexdrip.NavigationDrawerFragment;
import com.eveningoutpost.dexdrip.R;

import java.util.List;

/**
 * Created by Emma Black on 6/8/15.
 */
public abstract class ActivityWithMenu extends BaseAppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private int menu_position;
    private String menu_name;
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume(){
        try {
            super.onResume();
        } catch (IllegalArgumentException e) {
            UserError.Log.wtf("ActivityWithMenu", "Nasty error trying to call onResume() " + e);
        }
        menu_name = getMenuName();
        NavDrawerBuilder navDrawerBuilder = new NavDrawerBuilder(getApplicationContext());
        List<String> menu_option_list = navDrawerBuilder.nav_drawer_options;
        menu_position = menu_option_list.indexOf(menu_name);

      mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
      mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        NavDrawerBuilder navDrawerBuilder = new NavDrawerBuilder(getApplicationContext());
        //List<String> menu_option_list = navDrawerBuilder.nav_drawer_options;
        final List<Intent> intent_list = navDrawerBuilder.nav_drawer_intents;
        if (position != menu_position) {
            if (position >= intent_list.size()) {
                JoH.static_toast_long("Menu got confused! try again or report this error");
            } else {
                startActivity(intent_list.get(position));
            }
            //do not close activity if it is the Launcher or "Home".
            if (!getMenuName().equalsIgnoreCase(getString(R.string.home_screen))) {
                finish();
            }
        }
    }

    public abstract String getMenuName();
}
