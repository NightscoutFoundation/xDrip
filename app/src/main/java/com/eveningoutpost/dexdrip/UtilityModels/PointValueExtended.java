package com.eveningoutpost.dexdrip.UtilityModels;

import lecho.lib.hellocharts.model.PointValue;
import lombok.NoArgsConstructor;

/**
 * Created by Emma Black on 11/15/14.
 */
@NoArgsConstructor
public class PointValueExtended extends PointValue {

    public static final int BloodTest = 1;

    public PointValueExtended(float x, float y, String note_param) {
        super(x, y);
        note = note_param;
    }

    public PointValueExtended(float x, float y, float filtered) {
        super(x, y);
        calculatedFilteredValue = filtered;
    }
    public PointValueExtended(float x, float y) {
        super(x, y);
        calculatedFilteredValue = -1;
    }

    public float calculatedFilteredValue;
    public String note;
    public int type = 0;
    public String uuid;
    public long real_timestamp = 0;
}
