package com.eveningoutpost.dexdrip.processing;

import static com.eveningoutpost.dexdrip.models.JoH.tolerantParseDouble;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.Unitized;

import java.util.List;

/**
 * JamOrHam
 * <p>
 * BaseSmoother class, simply extend this class and override methods you want to support
 */
public abstract class BaseSmoother implements JSmoother {

    public List<BgReading> smoothBgReadings(final List<BgReading> readings) {
        return readings; // Default implementation does nothing
    }

    double getLowMarkInMgDl() {
        double lowMark = tolerantParseDouble(Pref.getString("lowValue", "70"), 70);
        if (!Unitized.usingMgDl()) {
            lowMark = Unitized.mgdlConvert(lowMark);
        }
        return lowMark;
    }

}
