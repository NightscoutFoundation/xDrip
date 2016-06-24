package com.eveningoutpost.dexdrip.ProfileEditor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * Created by jamorham on 21/06/2016.
 */
public class ProfileItem {
    private String title;
    @Expose
    protected int day_of_week, start_min, end_min;
    @Expose
    protected double carb_ratio, sensitivity, absorption_rate;


    public ProfileItem(int start_min, int end_min,double carb_ratio,double sensitivity)
    {
        this.start_min = start_min;
        this.end_min = end_min;
        this.carb_ratio = carb_ratio;
        this.sensitivity = sensitivity;
    }

    public String getTimePeriod()
    {
        return String.format("%02d:%02d",start_min / 60,start_min %60)+" -> "+String.format("%02d:%02d",end_min/60, end_min %60);
    }

    public String getTimeStart()
    {
        return String.format("%02d:%02d",start_min / 60,start_min %60);
    }
    public String getTimeEnd()
    {
        return String.format("%02d:%02d",end_min / 60,end_min %60);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String name) {
        this.title = name;
    }

    public String toJson() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                //.registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();
        return gson.toJson(this);
    }

}
