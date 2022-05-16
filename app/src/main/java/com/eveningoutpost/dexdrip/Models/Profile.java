package com.eveningoutpost.dexdrip.Models;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.profileeditor.BasalRepository;
import com.eveningoutpost.dexdrip.profileeditor.ProfileEditor;
import com.eveningoutpost.dexdrip.profileeditor.ProfileItem;
import com.eveningoutpost.dexdrip.utils.FoodType;
import com.eveningoutpost.dexdrip.xdrip;

import org.apache.commons.lang3.StringUtils;

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
    public static double minimum_fats_recommendation = 1;
    public static double minimum_proteins_recommendation = 1;
    public static double scale_factor = 18;
    private static double the_carb_ratio = 10; // now defunct
    private static double the_fats_ratio = 10; // now defunct
    private static double the_proteins_ratio = 10; // now defunct
    private static double stored_default_sensitivity = 54; // now defunct
    private static double stored_default_absorption_rate = 35;
    private static double stored_default_carbs_absorption_rate = 35;
    private static double stored_default_fats_absorption_rate = 35;
    private static double stored_default_proteins_absorption_rate = 35;
    private static double stored_default_insulin_action_time = 3.0;
    private static double stored_default_carb_delay_minutes = 15;
    private static double stored_default_fats_delay_minutes = 15;
    private static double stored_default_proteins_delay_minutes = 15;
    private static double stored_default_delay_minutes = 15;
    private static boolean preferences_loaded = false;
    private static List<ProfileItem> profileItemList;

    public static double getSensitivity(long when) {
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

    /**
     * @deprecated
     * This method doesn't support other food types. i.e: fats and proteins.
     * <p> Use {@link Profile#getFoodAbsorptionRate(double, FoodType)} instead.
     * @param when time.
     * @return carbs per hour.
     */
    @Deprecated
    static double getCarbAbsorptionRate(double when) {
        return stored_default_absorption_rate;
    }

    static double getFoodAbsorptionRate(double when, FoodType foodType) {
        switch (foodType) {
            case CARBS:
                return stored_default_carbs_absorption_rate;

            case FATS:
                return stored_default_fats_absorption_rate;

            case PROTEINS:
                return stored_default_proteins_absorption_rate;

            default:
                return stored_default_absorption_rate;
        }
    }

    /**
     * @deprecated
     * This method doesn't support other food types. i.e: fats and proteins.
     * <p> Use {@link Profile#setFoodAbsorptionDefault(double, FoodType)} instead.
     * @param value stored_default_absorption_rate.
     */
    @Deprecated
    public static void setCarbAbsorptionDefault(double value) {
        // sanity check goes here
        if (value < 0.01) return;
        stored_default_absorption_rate = value;
    }

    public static void setFoodAbsorptionDefault(double value, FoodType foodType) {
        // sanity check goes here
        if (value < 0.01) return;

        switch (foodType) {
            case CARBS:
                stored_default_carbs_absorption_rate = value;
                break;

            case FATS:
                stored_default_fats_absorption_rate = value;
                break;

            case PROTEINS:
                stored_default_proteins_absorption_rate = value;
                break;
        }
    }

    static double insulinActionTime(long when) {
        return stored_default_insulin_action_time;
    }

    /**
     * @deprecated
     * This method doesn't support other food types. i.e: fats and proteins.
     * <p> Use {@link Profile#foodDelayMinutes(long, FoodType)} instead.
     * @param when stored_default_carb_delay_minutes.
     * @return stored_default_carb_delay_minutes.
     */
    @Deprecated
    static double carbDelayMinutes(long when) {
        return stored_default_carb_delay_minutes;
    }

    static double foodDelayMinutes(long when, FoodType foodType) {
        switch (foodType) {
            case CARBS:
                return stored_default_carb_delay_minutes;

            case FATS:
                return stored_default_fats_delay_minutes;

            case PROTEINS:
                return stored_default_proteins_delay_minutes;

            default:
                return stored_default_delay_minutes;
        }
    }

    static double maxLiverImpactRatio(long when) {
        return 0.3; // how much can the liver block carbs going in to blood stream?
    }

    /**
     * @deprecated
     * This method doesn't support other food types. i.e: fats and proteins.
     * <p> Use {@link Profile#getFoodRatio(long, FoodType)} instead.
     * @param when time.
     * @return g per unit.
     */
    @Deprecated
    public static double getCarbRatio(long when) {
        return findItemListElementForTime(when).carb_ratio;
        //return the_carb_ratio; // g per unit
    }

    public static double getFoodRatio(long when, FoodType foodType) {

        switch (foodType) {
            case CARBS:
                return findItemListElementForTime(when).carb_ratio;

            case FATS:
                return findItemListElementForTime(when).fats_ratio;

            case PROTEINS:
                return findItemListElementForTime(when).proteins_ratio;

            default:
                return 0.0;
        }
    }


    private static void populateProfile() {
        if (profileItemList == null) {
            profileItemList = ProfileEditor.loadData(false);
            Log.d(TAG, "Loaded profile data, blocks: " + profileItemList.size());
        }
    }

    public static void invalidateProfile() {
        profileItemList = null;
    }

    private static ProfileItem findItemListElementForTime(long when) {
        populateProfile();
        // TODO does this want/need a hash table lookup cache?
        if (profileItemList.size() == 1) {
            return profileItemList.get(0); // always will be first/only element.
        }
        // get time of day
        final int min = ProfileItem.timeStampToMin(when);
        // find element
        for (ProfileItem item : profileItemList) {
            if (item.start_min < item.end_min) {
                // regular
                if ((item.start_min <= min) && (item.end_min >= min)) {
                    // Log.d(TAG, "Match on item " + item.getTimePeriod() + " " + profileItemList.indexOf(item));
                    return item;
                }
            } else {
                // item spans midnight
                if ((min >= item.start_min) || (min <= item.end_min)) {
                    //  Log.d(TAG, "midnight span Match on item " + item.getTimePeriod() + " " + profileItemList.indexOf(item));
                    return item;
                }
            }
        }
        Log.wtf(TAG, "Null return from findItemListElementForTime");
        return null; // should be impossible
    }


    /**
     * @deprecated
     * This method doesn't support other food types. i.e: fats and proteins.
     * <p> Use {@link Profile#setDefaultFoodRatio(Double, FoodType)} instead.
     * @param value the_carb_ratio.
     * @return g per unit.
     */
    @Deprecated
    static public void setDefaultCarbRatio(Double value) {
        if (value <= 0) {
            Log.e(TAG, "Invalid default carb ratio: " + value);
            return;
        }
        the_carb_ratio = value;
    }

    static public void setDefaultFoodRatio(Double value, FoodType foodType) {
        if (value <= 0) {
            Log.e(TAG, "Invalid default " + foodType.value +" ratio: " + value);
            return;
        }

        switch (foodType) {
            case CARBS:
                the_carb_ratio = value;
                break;

            case FATS:
                the_fats_ratio = value;
                break;

            case PROTEINS:
                the_proteins_ratio = value;
                break;
        }
    }

    static double getLiverSensRatio(long when) {
        return 2.0;
    }

    public static void validateTargetRange() {
        final double default_target_glucose = tolerantParseDouble(Pref.getString("plus_target_range", Double.toString(5.5 / scale_factor)));
        if (default_target_glucose > tolerantParseDouble(Pref.getString("highValue", Double.toString(5.5 / scale_factor)))) {
            Pref.setString("plus_target_range", JoH.qs(default_target_glucose * Constants.MGDL_TO_MMOLL, 1));
            UserError.Log.i(TAG, "Converted initial value of target glucose to mmol");
        }
    }

    static double getTargetRangeInMmol(long when) {
        // return tolerantParseDouble(Home.getString("plus_target_range",Double.toString(5.5 / scale_factor)));
        return getTargetRangeInUnits(when) / scale_factor;
    }

    public static double getTargetRangeInUnits(double when) {
        return tolerantParseDouble(Pref.getString("plus_target_range", Double.toString(5.5 / scale_factor)));
        //return getTargetRangeInMmol(when) * scale_factor; // TODO deal with rounding errors here? (3 decimal places?)
    }

    /**
     * @deprecated
     * This method doesn't support other food types. i.e: fats and proteins.
     * <p> Use {@link Profile#getFoodSensitivity(long, FoodType)} instead.
     * @param when time.
     * @return sensitivity.
     */
    @Deprecated
    static double getCarbSensitivity(long when) {
        return getCarbRatio(when) / getSensitivity(when);
    }

    static double getFoodSensitivity(long when, FoodType foodType) {
        return getFoodRatio(when, foodType) / getSensitivity(when);
    }

    /**
     * @deprecated
     * This method doesn't support other food types. i.e: fats and proteins.
     * <p> Use {@link Profile#getFoodToRaiseByMmol(double, long, FoodType)} instead.
     * @param mmol mmol.
     * @param when time.
     * @return CarbsToRaiseByMmol.
     */
    @Deprecated

    static double getCarbsToRaiseByMmol(double mmol, long when) {
        return getCarbSensitivity(when) * mmol;
    }

    static double getFoodToRaiseByMmol(double mmol, long when, FoodType foodType) {

        return getFoodSensitivity(when, foodType) * mmol;
    }

    static double getInsulinToLowerByMmol(double mmol, long when) {
        return mmol / getSensitivity(when);
    }

    /**
     * take an average of carb suggestions when our scope is between two times
     * @deprecated
     * This method doesn't support other food types. i.e: fats and proteins.
     * <p> Use {@link Profile#getFoodToRaiseByMmolBetweenTwoTimes(double, long, long, FoodType)} instead.
     * @param mmol mmol.
     * @param whennow whennow.
     * @param whenthen whenthen.
     * @return CarbsToRaiseByMmolBetweenTwoTimes.
     */
    @Deprecated
    static double getCarbsToRaiseByMmolBetweenTwoTimes(double mmol, long whennow, long whenthen) {
        double result = (getCarbsToRaiseByMmol(mmol, whennow) + getCarbsToRaiseByMmol(mmol, whenthen)) / 2;
        UserError.Log.d(TAG, "GetCarbsToRaiseByMmolBetweenTwoTimes: " + JoH.qs(mmol) + " result: " + JoH.qs(result));
        return result;
    }

    static double getFoodToRaiseByMmolBetweenTwoTimes(double mmol, long whennow, long whenthen, FoodType foodType) {
        double result = (getFoodToRaiseByMmol(mmol, whennow, foodType) + getFoodToRaiseByMmol(mmol, whenthen, foodType)) / 2;

        UserError.Log.d(TAG, "Get"+ StringUtils.capitalize(foodType.value) + "ToRaiseByMmolBetweenTwoTimes: " + JoH.qs(mmol) + " result: " + JoH.qs(result));
        return result;
    }

    static double getInsulinToLowerByMmolBetweenTwoTimes(double mmol, long whennow, long whenthen) {
        return (getInsulinToLowerByMmol(mmol, whennow) + getInsulinToLowerByMmol(mmol, whenthen)) / 2;
    }

    public static double[] evaluateEndGameMmol(double mmol, long endGameTime, long timeNow) {
        double addcarbs = 0;
        double addFats = 0;
        double addProteins = 0;
        double addinsulin = 0;
        final double target_mmol = getTargetRangeInMmol(endGameTime) * scale_factor;
        double diff_mmol = target_mmol - mmol;
        if (diff_mmol > 0) {
            addcarbs = getFoodToRaiseByMmolBetweenTwoTimes(diff_mmol, timeNow, endGameTime, FoodType.CARBS);
            addFats = getFoodToRaiseByMmolBetweenTwoTimes(diff_mmol, timeNow, endGameTime, FoodType.FATS);
            addProteins = getFoodToRaiseByMmolBetweenTwoTimes(diff_mmol, timeNow, endGameTime, FoodType.PROTEINS);
        } else if (diff_mmol < 0) {
            addinsulin = getInsulinToLowerByMmolBetweenTwoTimes(diff_mmol * -1, timeNow, endGameTime);
        }
        return new double[]{addcarbs, addFats, addProteins, addinsulin};
    }

    public static void reloadPreferencesIfNeeded(SharedPreferences prefs) {
        if (!preferences_loaded) reloadPreferences(prefs);
    }

    public static void reloadPreferences() {
        Log.d(TAG, "Reloaded profile preferences");
        reloadPreferences(PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()));
    }

    public static synchronized void reloadPreferences(SharedPreferences prefs) {
        validateTargetRange();
        try {
            Profile.setSensitivityDefault(tolerantParseDouble(prefs.getString("profile_insulin_sensitivity_default", "0")));
        } catch (Exception e) {
            if (JoH.ratelimit("invalid-insulin-profile", 60)) {
                Home.toaststatic("Invalid insulin sensitivity");
            }
        }
        try {
            Profile.setFoodAbsorptionDefault(tolerantParseDouble(prefs.getString("profile_carb_absorption_default", "0")), FoodType.CARBS);
        } catch (Exception e) {
            if (JoH.ratelimit("invalid-insulin-profile", 60)) {
                Home.toaststatic("Invalid carb absorption rate");
            }
        }
        try {
            Profile.setFoodAbsorptionDefault(tolerantParseDouble(prefs.getString("profile_fats_absorption_default", "0")), FoodType.FATS);
        } catch (Exception e) {
            if (JoH.ratelimit("invalid-insulin-profile", 60)) {
                Home.toaststatic("Invalid fats absorption rate");
            }
        }
        try {
            Profile.setFoodAbsorptionDefault(tolerantParseDouble(prefs.getString("profile_proteins_absorption_default", "0")), FoodType.PROTEINS);
        } catch (Exception e) {
            if (JoH.ratelimit("invalid-insulin-profile", 60)) {
                Home.toaststatic("Invalid proteins absorption rate");
            }
        }
        try {
            Profile.setInsulinActionTimeDefault(tolerantParseDouble(prefs.getString("xplus_insulin_dia", "3.0")));
        } catch (Exception e) {
            if (JoH.ratelimit("invalid-insulin-profile", 60)) {
                Home.toaststatic("Invalid insulin action time");
            }
        }
        profileItemList = null;
        populateProfile();
        preferences_loaded = true;
    }

    private static double tolerantParseDouble(String str) throws NumberFormatException {
        return Double.parseDouble(str.replace(",", "."));
    }

    public static double getBasalRate(final long when) {
        return BasalRepository.getActiveRate(when);
    }

    public static double getBasalRateAbsoluteFromPercent(final long when, int basal_percent) {
        return getBasalRate(when) * basal_percent / 100d;
    }
    public static int getBasalRatePercentFromAbsolute(final long when, double basal_absolute) {
        return (int) Math.round((basal_absolute / getBasalRate(when)) * 100d);
    }

}