package com.eveningoutpost.dexdrip.models;

import android.content.Context;
import android.content.Context;
import android.content.Context;
import android.content.SharedPreferences;

import com.eveningoutpost.dexdrip.xdrip;
import com.eveningoutpost.dexdrip.utilitymodels.Pref; // Corrected
import com.eveningoutpost.dexdrip.models.UserError; // Corrected


public class InsulinStockManager {

    private static final String PREF_NAME = "InsulinStockPrefs";
    private static final String KEY_CURRENT_STOCK = "current_insulin_stock";
    private static final String KEY_INITIAL_STOCK_SET = "initial_stock_set"; // To track if initial stock was set after enabling

    private static final int DEFAULT_INITIAL_STOCK = 300;

    private static SharedPreferences getPrefs() {
        return xdrip.getAppContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isStockTrackingEnabled() {
        return Pref.getBooleanDefaultFalse("insulin_stock_tracking_enabled");
    }

    public static float getCurrentStock() {
        if (!isStockTrackingEnabled()) {
            return -1; // Or throw an exception, depending on desired behavior
        }
        return getPrefs().getFloat(KEY_CURRENT_STOCK, 0);
    }

    public static void setInitialStock(float units) {
        if (!isStockTrackingEnabled()) {
            return;
        }
        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putFloat(KEY_CURRENT_STOCK, units);
        editor.putBoolean(KEY_INITIAL_STOCK_SET, true);
        editor.apply();
        // Consider broadcasting an update or using a listener for UI changes
    }

    public static void decreaseStock(double unitsToDecrease) {
        if (!isStockTrackingEnabled() || unitsToDecrease <= 0) {
            return;
        }
        // Ensure initial stock is set before decreasing
        if (!isInitialStockSet()) {
            // If initial stock was never explicitly set by user after enabling,
            // and we try to decrease, set it to the default initial value first.
            // Or, prompt user / handle differently. For now, using default.
            float defaultInitial = Pref.getStringAsInt("initial_insulin_stock", DEFAULT_INITIAL_STOCK);
            setInitialStock(defaultInitial);
        }

        float currentStock = getCurrentStock();
        float newStock = currentStock - (float) unitsToDecrease;
        if (newStock < 0) {
            newStock = 0; // Stock cannot be negative
            // TODO: Consider alerting the user that stock is depleted or low
            UserError.Log.wtf("InsulinStockManager", "Insulin stock depleted. Attempted to decrease by " + unitsToDecrease + ", current was " + currentStock);
        }

        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putFloat(KEY_CURRENT_STOCK, newStock);
        editor.apply();
        // Consider broadcasting an update or using a listener for UI changes
    }

    public static boolean isInitialStockSet() {
        return getPrefs().getBoolean(KEY_INITIAL_STOCK_SET, false);
    }

    public static void resetInitialStockTracking() {
        // Called when tracking is disabled, to allow fresh setup if re-enabled
        SharedPreferences.Editor editor = getPrefs().edit();
        editor.remove(KEY_INITIAL_STOCK_SET);
        // editor.remove(KEY_CURRENT_STOCK); // Optionally clear current stock value too
        editor.apply();
    }

    public static int getDefaultInitialStock() {
        return DEFAULT_INITIAL_STOCK;
    }
}
