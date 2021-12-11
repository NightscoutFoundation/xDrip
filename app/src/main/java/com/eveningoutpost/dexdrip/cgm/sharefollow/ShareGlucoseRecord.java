package com.eveningoutpost.dexdrip.cgm.sharefollow;

import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.Dex_Constants;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Unitized;
import com.google.gson.annotations.Expose;

/**
 * jamorham
 *
 * Model of json data object we receive from Share Servers
 */

public class ShareGlucoseRecord {

    @Expose
    public String DT;

    @Expose
    public String ST;

    @Expose
    public String WT;

    @Expose
    public Dex_Constants.TREND_ARROW_VALUES Trend;

    @Expose
    public double Value;

    public long timestamp = -1;

    public Double slopePerMsFromDirection() {
        final String slope_name = Trend.trendName();
        return slope_name == null ? null : BgReading.slopefromName(slope_name);
    }

    public Long getTimestamp() {
        if (timestamp == -1) {
            try {
                timestamp = Long.parseLong(WT.replaceAll("[^0-9]", ""));
            } catch (Exception e) {
                timestamp = 0L;
            }
        }
        return timestamp;
    }

    public String toS() {
        return DT + " " + ST + " " + WT + " " + Value + " " + Trend + " " + JoH.dateTimeText(getTimestamp()) + " " + Unitized.unitized_string_static(Value) + " " + Trend.trendName();
    }

}
