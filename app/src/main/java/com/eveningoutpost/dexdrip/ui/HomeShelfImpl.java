package com.eveningoutpost.dexdrip.ui;

/**
 * Created by jamorham on 20/10/2017.
 *
 * Initial shelf implementation for Home
 *
 */

public class HomeShelfImpl extends BaseShelf {

    public HomeShelfImpl() {
        this.PREFS_PREFIX = "home-shelf-";
        map.put("time_buttons", "Time Buttons");
        map.put("chart_preview", "Chart Preview");
        map.put("source_wizard", "Source Wizard");

        defaults.put("chart_preview", true);
        populate();
    }

}
