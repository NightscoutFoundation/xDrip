package com.eveningoutpost.dexdrip.utils;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;

import com.eveningoutpost.dexdrip.NavDrawerBuilder;
import com.eveningoutpost.dexdrip.NavigationDrawerFragment;
import com.eveningoutpost.dexdrip.R;

import java.util.List;

/**
 * Created by stephenblack on 6/8/15.
 */
public abstract class ListActivityWithMenu extends ListActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private NavDrawerBuilder navDrawerBuilder;
    private List<Intent> intent_list;
    private List<String> menu_option_list;
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
        navDrawerBuilder = new NavDrawerBuilder(getApplicationContext());
        menu_option_list = navDrawerBuilder.nav_drawer_options;
        intent_list = navDrawerBuilder.nav_drawer_intents;
        menu_position = menu_option_list.indexOf(menu_name);

        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        if (position != menu_position) {
            startActivity(intent_list.get(position));
            finish();
        }
    }

    public abstract String getMenuName();
}
