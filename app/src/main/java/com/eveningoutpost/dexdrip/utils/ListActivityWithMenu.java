package com.eveningoutpost.dexdrip.utils;

import android.content.Intent;
import android.os.Bundle;


import androidx.drawerlayout.widget.DrawerLayout;

import com.eveningoutpost.dexdrip.BaseListActivity;
import com.eveningoutpost.dexdrip.NavDrawerBuilder;
import com.eveningoutpost.dexdrip.NavigationDrawerFragment;
import com.eveningoutpost.dexdrip.R;

import java.util.List;

/**
 * Created by Emma Black on 6/8/15.
 */
public abstract class ListActivityWithMenu extends BaseListActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private int menu_position;
    private String menu_name;
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume(){
        super.onResume();
        menu_name = getMenuName();
        NavDrawerBuilder  navDrawerBuilder = new NavDrawerBuilder(getApplicationContext());
        List<String> menu_option_list = navDrawerBuilder.nav_drawer_options;
        menu_position = menu_option_list.indexOf(menu_name);

        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        NavDrawerBuilder navDrawerBuilder = new NavDrawerBuilder(getApplicationContext());
        List<String> menu_option_list = navDrawerBuilder.nav_drawer_options;
        List<Intent> intent_list = navDrawerBuilder.nav_drawer_intents;
        if (position != menu_position) {
            startActivity(intent_list.get(position));
            finish();
        }
    }

    public abstract String getMenuName();
}
