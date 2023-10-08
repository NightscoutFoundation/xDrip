package com.eveningoutpost.dexdrip.processing;

import static com.eveningoutpost.dexdrip.utilitymodels.Unitized.unit;
import static com.eveningoutpost.dexdrip.utilitymodels.Unitized.usingMgDl;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import java.util.HashMap;

import lombok.val;

/**
 * JamOrHam
 * <p>
 * Factory to get JSmoother instances
 */

public class SmootherFactory {

    private static final String TAG = SmootherFactory.class.getSimpleName();
    private static final HashMap<String, JSmoother> cache = new HashMap<>();

    /**
     * Get JSmoother instance for specification choice
     * Implements cache of instantiated objects
     *
     * @param choice the choice
     * @return the JSmoother concrete object
     */

    public static JSmoother get(final String choice) {
        val collectorPeriod = DexCollectionType.getCurrentSamplePeriod();
        val specification = choice + unit(usingMgDl()) + collectorPeriod;
        JSmoother instance = cache.get(specification);
        if (instance == null) {
            UserError.Log.d(TAG, "New instance for " + specification);
            // new instance required
            switch (choice) {
                // different algorithms can be chosen here
                case "null":
                    instance = new NullSmoother();
                    break;
                case "goto":
                default:
                    instance = new GotoSmoother(collectorPeriod);
                    break;
            }
            cache.put(specification, instance);
        }
        return instance;
    }
}
