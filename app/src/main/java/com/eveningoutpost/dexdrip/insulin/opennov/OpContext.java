package com.eveningoutpost.dexdrip.insulin.opennov;

import com.eveningoutpost.dexdrip.insulin.opennov.mt.ARequest;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.Apdu;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.Configuration;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.EventReport;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.EventRequest;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.IdModel;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.RelativeTime;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.SegmentInfoList;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.Specification;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.TrigSegmDataXfer;

/**
 * JamOrHam
 * OpenNov context holder
 */

public class OpContext {

    public Specification specification;
    public RelativeTime relativeTime;
    public EventRequest eventRequest;
    public EventReport eventReport;
    public Configuration configuration;
    public TrigSegmDataXfer trigSegmDataXfer;
    public SegmentInfoList segmentInfoList;
    public IdModel model;
    public ARequest aRequest;
    public Apdu apdu;
    public int invokeId = -1;

    public Configuration getConfiguration() {
        if (configuration != null) {
            return configuration;
        } else {
            if (eventReport != null && eventReport.configuration != null) {
                configuration = eventReport.configuration; // cache
                return configuration;
            }
        }
        return null;
    }

    public boolean hasConfiguration() {
        return configuration != null || getConfiguration() != null;
    }

    public boolean isError() {
        return apdu == null || apdu.isError();
    }

    public boolean wantsRelease() {
        return apdu != null && apdu.wantsRelease();
    }

}
