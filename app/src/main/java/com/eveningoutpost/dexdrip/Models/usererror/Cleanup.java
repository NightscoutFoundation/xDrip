package com.eveningoutpost.dexdrip.Models.usererror;


import android.os.AsyncTask;

import com.activeandroid.Model;

import java.util.List;

public class Cleanup<T extends Model> extends AsyncTask<List<T>, Integer, Boolean> {

    @Override
    protected Boolean doInBackground(List<T>... models) {
        try {
            for(T modelInstance : models[0]) {
                modelInstance.delete();
            }
            return true;
        } catch(Exception e) {
            return false;
        }
    }
}