package com.eveningoutpost.dexdrip.Models;

import android.content.SharedPreferences;
import android.util.Log;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.profileeditor.ProfileEditor;
import com.eveningoutpost.dexdrip.profileeditor.ProfileItem;

import java.util.List;

/**
 * Created by jamorham on 04/01/16.
 */

// user profile for insulin related parameters which can be configured and set at times of day
// currently a placeholder with hardcoded values

// TODO Proper support for MG/DL


public class Profile {

    private final static String TAG = "jamorham pred";
    public static double minimum_shown_iob = 0.005;
    public static double minimum_shown_cob = 0.01;
    public static double minimum_insulin_recommendation = 0.1;
    public static double minimum_carb_recommendation = 1;
    public static double scale_factor = 18;
    private static double the_carb_ratio = 10; // now defunct
    private static double stored_default_sensitivity = 54; // now defunct
    private static double stored_default_absorption_rate = 35;
    private static double stored_default_insulin_action_time = 3.0;
    private static double stored_default_carb_delay_minutes = 15;
    private static List<ProfileItem> profileItemList;

    public static double getSensitivity(double when) {
       final double sensitivity = findItemListElementForTime(when).sensitivity;
       // Log.d(TAG,"sensitivity: "+sensitivity);
        return sensitivity;
       // expressed in native units lowering effect of 1U
    }

    public static void setSensitivityDefault(double value) {
        // sanity check goes here
        stored_default_sensitivity = value;
    }

    public static void setInsulinActionTimeDefault(double value) {
        // sanity check goes here
        if (value < 0.1) return;
        if (value > 24) return;
        stored_default_insulin_action_time = value;
    }

    static double getCarbAbsorptionRate(double when) {
        return stored_default_absorption_rate; // carbs per hour
    }

    public static void setCarbAbsorptionDefault(double value) {
        // sanity check goes here
        if (value < 0.01) return;
        stored_default_absorption_rate = value;
    }

    static double insulinActionTime(double when) {
        return stored_default_insulin_action_time;
    }

    static double carbDelayMinutes(double when) {
        return stored_default_carb_delay_minutes;
    }

    static double maxLiverImpactRatio(double when) {
        return 0.3; // how much can the liver block carbs going in to blood stream?
    }

    static double getCarbRatio(double when) {
        return findItemListElementForTime(when).carb_ratio;
        //return the_carb_ratio; // g per unit
    }


    private static void populateProfile()
    {
        if (profileItemList==null)
        {
            profileItemList = ProfileEditor.loadData(false);
            Log.d(TAG,"Loaded profile data, blocks: "+profileItemList.size());
        }
    }

    public static void invalidateProfile()
    {
        profileItemList = null;
    }

    private static ProfileItem findItemListElementForTime(double when)
    {
        populateProfile();
        // TODO does this want/need a hash table lookup cache?
        if (profileItemList.size()==1) profileItemList.get(0); // always will be first/only element.
        // get time of day
        final int min = ProfileItem.timeStampToMin(when);
        // find element
        for (ProfileItem item : profileItemList)
        {
            if (item.start_min < item.end_min) {
                // regular
                if ((item.start_min <= min) && (item.end_min >= min)) {
                   // Log.d(TAG, "Match on item " + item.getTimePeriod() + " " + profileItemList.indexOf(item));
                    return item;
                }
            } else {
                // item spans midnight
                if ((min >= item.start_min) || (min <= item.end_min))
                {
                  //  Log.d(TAG, "midnight span Match on item " + item.getTimePeriod() + " " + profileItemList.indexOf(item));
                    return item;
                }
            }
        }
        return null; // should be impossible
    }


    static public void setDefaultCarbRatio(Double value) {
        if (value <= 0) {
            Log.e(TAG, "Invalid default carb ratio: " + value);
            return;
        }
        the_carb_ratio = value; // g per unit
    }

    static double getLiverSensRatio(double when) {
        return 2.0;
    }

    static double getTargetRangeInMmol(double when) {
        return 5.5;
    }

    public static double getTargetRangeInUnits(double when) {
        return getTargetRangeInMmol(when) * scale_factor;
    }

    static double getCarbSensitivity(double when) {
        return getCarbRatio(when) / getSensitivity(when);
    }

    static double getCarbsToRaiseByMmol(double mmol, double when) {

        double result = getCarbSensitivity(when) * mmol;
        return result;
    }

    static double getInsulinToLowerByMmol(double mmol, double when) {
        return mmol / getSensitivity(when);
    }

    // take an average of carb suggestions when our scope is between two times
    static double getCarbsToRaiseByMmolBetweenTwoTimes(double mmol, double whennow, double whenthen) {
        double result = (getCarbsToRaiseByMmol(mmol, whennow) + getCarbsToRaiseByMmol(mmol, whenthen)) / 2;
        UserError.Log.d(TAG, "GetCarbsToRaiseByMmolBetweenTwoTimes: " + JoH.qs(mmol) + " result: " + JoH.qs(result));
        return result;
    }

    static double getInsulinToLowerByMmolBetweenTwoTimes(double mmol, double whennow, double whenthen) {
        return (getInsulinToLowerByMmol(mmol, whennow) + getInsulinToLowerByMmol(mmol, whenthen)) / 2;
    }

    public static double[] evaluateEndGameMmol(double mmol, double endGameTime, double timeNow) {
        double addcarbs = 0;
        double addinsulin = 0;
        double target_mmol = getTargetRangeInMmol(endGameTime) * scale_factor;
        double diff_mmol = target_mmol - mmol;
        if (diff_mmol > 0) {
            addcarbs = getCarbsToRaiseByMmolBetweenTwoTimes(diff_mmol, timeNow, endGameTime);
        }

        if (diff_mmol < 0) {
            addinsulin = getInsulinToLowerByMmolBetweenTwoTimes(diff_mmol * -1, timeNow, endGameTime);
        }
        return new double[]{addcarbs, addinsulin};
    }

    public static void reloadPreferences(SharedPreferences prefs) {
        // TODO HANDLE EURO NUMBER FORMAT
        try {
            Profile.setSensitivityDefault(Double.parseDouble(prefs.getString("profile_insulin_sensitivity_default", "0")));
        } catch (Exception e) {
            Home.toaststatic("Invalid insulin sensitivity");
        }
        try {
            Profile.setDefaultCarbRatio(Double.parseDouble(prefs.getString("profile_carb_ratio_default", "0")));
        } catch (Exception e) {
            Home.toaststatic("Invalid default carb ratio!");
        }
        try {
            Profile.setCarbAbsorptionDefault(Double.parseDouble(prefs.getString("profile_carb_absorption_default", "0")));
        } catch (Exception e) {
            Home.toaststatic("Invalid carb absorption rate");
        }
        try {
            Profile.setInsulinActionTimeDefault(tolerantParseDouble(prefs.getString("xplus_insulin_dia", "3.0")));
        } catch (Exception e) {
            Home.toaststatic("Invalid insulin action time");
        }
        profileItemList=null;
        populateProfile();
    }

    private static double tolerantParseDouble(String str) throws NumberFormatException {
        return Double.parseDouble(str.replace(",", "."));

    }
}