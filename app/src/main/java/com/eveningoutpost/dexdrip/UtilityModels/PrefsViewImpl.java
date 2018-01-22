package com.eveningoutpost.dexdrip.UtilityModels;

import android.databinding.BaseObservable;

/**
 * Created by jamorham on 05/10/2017.
 * <p>
 * Implementation of PrefsView
 */

public class PrefsViewImpl extends BaseObservable implements PrefsView {

    public boolean getbool(String name) {
        return Pref.getBooleanDefaultFalse(name);
    }

    public void setbool(String name, boolean value) {
        Pref.setBoolean(name, value);
        notifyChange();
    }

    public void togglebool(String name) {
        setbool(name, !getbool(name));
    }
}
