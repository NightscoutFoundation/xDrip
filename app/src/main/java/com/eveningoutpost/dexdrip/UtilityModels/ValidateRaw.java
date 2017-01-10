package com.eveningoutpost.dexdrip.UtilityModels;

import com.eveningoutpost.dexdrip.utils.DexCollectionType;

/**
 * Created by jamorham on 10/01/2017.
 * <p>
 * Checks if a raw value is out of bounds
 */

public class ValidateRaw {

    public static boolean isRawAcceptable(double raw_value) {
        return isRawAcceptable(DexCollectionType.getDexCollectionType(), raw_value);
    }


    public static boolean isRawAcceptable(DexCollectionType collectionType, double raw_value) {

        if (DexCollectionType.hasDexcomRaw(collectionType)) {
            if (raw_value > Constants.DEXCOM_MAX_RAW) {
                return false;
            }
        }
        return true; // always okay for types we don't know or manage or if we fall through all tests
    }
}
