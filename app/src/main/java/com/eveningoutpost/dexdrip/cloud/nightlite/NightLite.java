package com.eveningoutpost.dexdrip.cloud.nightlite;

import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.utilitymodels.Unitized.usingMgDl;

import com.eveningoutpost.dexdrip.cloud.nightlite.NightLiteProto.GlucoseEntry;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

// JamOrHam

import lombok.val;

public class NightLite {

    private final static long NEW_EPOCH = 1767225600; // TODO adjustable

    public static NightLiteProto.Block getForHours(int hours) {
        val b = NightLiteProto.Block.newBuilder();
        val readings = BgReading.latestForGraph(500, tsl() - Constants.HOUR_IN_MS * hours, tsl());
        for (val reading : readings) {
            b.addEntries(fromBgReading(reading));
        }
        val treatments = Treatments.latestForGraph(500, tsl() - Constants.HOUR_IN_MS * hours, tsl());
        for (val treatment : treatments) {
            b.addTreatments(fromTreatment(treatment));
        }
        addConfig(b);
        return b.build();
    }

    private static void addConfig(NightLiteProto.Block.Builder b) {
        b.setUnits(usingMgDl() ? NightLiteProto.Units.MGDL : NightLiteProto.Units.MMOL);
    }

    private static GlucoseEntry fromBgReading(final BgReading bgReading) {
        val builder = GlucoseEntry.newBuilder()
                .setGlucose((int) Math.round(bgReading.calculated_value))
                .setTimestamp(timeStampFromUtc(bgReading.timestamp))
                .setTrend(trendFromOrdinal(bgReading.getSlopeOrdinal()));
        if (!bgReading.hide_slope) {
            val deltaByMinute = bgReading.calculated_value_slope * 60_000;
            builder.setDelta((int) Math.round(deltaByMinute * 10));
        }
        return builder.build();
    }

    private static NightLiteProto.Treatment fromTreatment(final Treatments treatment) {
        val builder = NightLiteProto.Treatment.newBuilder()
                .setTimestamp(timeStampFromUtc(treatment.timestamp))
                .setUnits((int) Math.round(treatment.insulin * 100))
                .setCarbs((int) Math.round(treatment.carbs));
        return builder.build();
    }

    private static NightLiteProto.Trend trendFromOrdinal(int ordinal) {
        switch (ordinal) {
            case 1:
                return NightLiteProto.Trend.DOUBLE_UP;
            case 2:
                return NightLiteProto.Trend.SINGLE_UP;
            case 3:
                return NightLiteProto.Trend.UP_45;
            case 4:
                return NightLiteProto.Trend.FLAT;
            case 5:
                return NightLiteProto.Trend.DOWN_45;
            case 6:
                return NightLiteProto.Trend.SINGLE_DOWN;
            case 7:
                return NightLiteProto.Trend.DOUBLE_DOWN;
            case 8:
                return NightLiteProto.Trend.NOT_COMPUTABLE;
            case 9:
                return NightLiteProto.Trend.OUT_OF_RANGE;
            default:
                return NightLiteProto.Trend.NONE;
        }
    }

    private static int timeStampFromUtc(final long timestamp) {
        return (int) ((timestamp / 1000) - NEW_EPOCH);
    }
}
