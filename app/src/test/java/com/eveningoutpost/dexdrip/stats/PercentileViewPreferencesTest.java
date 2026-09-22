package com.eveningoutpost.dexdrip.stats;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the preference-driven labels on the percentile chart.
 * <p>
 * {@code drawGrid} picks its level markings from the stored glucose unit, so the labels written on
 * the chart are the read's observable effect. The tests draw the view onto a canvas that records
 * the text it is asked to write, and read the labels back.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class PercentileViewPreferencesTest extends RobolectricTestWithConfig {

    private PercentileView view;

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        prefs().edit().clear().commit();                        // the preference file leaks between test methods
        view = new PercentileView(xdrip.getAppContext());
        view.setCalculatedData(emptyPercentiles());             // otherwise the view only draws "Calculating..."
    }

    // ===== Level markings ========================================================================

    /** In mg/dL the grid is marked every 50 units. */
    @Test
    public void unitsMgdl_marksTheGridInMgdl() {
        // :: Setup
        prefs().edit().putString("units", "mgdl").commit();

        // :: Act
        final List<String> labels = drawnText();

        // :: Verify
        assertThat(labels).containsAtLeast("50", "100", "150", "200", "250", "300", "350");
    }

    /** In mmol/L the same grid carries the mmol level markings instead. */
    @Test
    public void unitsMmol_marksTheGridInMmol() {
        // :: Setup
        prefs().edit().putString("units", "mmol").commit();

        // :: Act
        final List<String> labels = drawnText();

        // :: Verify
        assertThat(labels).containsAtLeast("2.8", "5.5", "8.3", "11", "14", "17", "20");
    }

    /** With no stored unit the grid falls back to mg/dL. */
    @Test
    public void noStoredUnits_marksTheGridInMgdl() {
        // :: Act
        final List<String> labels = drawnText();

        // :: Verify
        assertThat(labels).containsAtLeast("50", "100", "150", "200", "250", "300", "350");
    }

    // ===== Helpers ===============================================================================

    private List<String> drawnText() {
        RecordingCanvas canvas = new RecordingCanvas();
        view.onDraw(canvas);
        return canvas.text;
    }

    /** All percentiles flat at zero: the polygons are degenerate, the grid and labels are not. */
    private PercentileView.CalculatedData emptyPercentiles() {
        PercentileView.CalculatedData data = view.new CalculatedData();
        data.q10 = new double[PercentileView.NO_TIMESLOTS];
        data.q25 = new double[PercentileView.NO_TIMESLOTS];
        data.q50 = new double[PercentileView.NO_TIMESLOTS];
        data.q75 = new double[PercentileView.NO_TIMESLOTS];
        data.q90 = new double[PercentileView.NO_TIMESLOTS];
        return data;
    }

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    /** A canvas that keeps every string the view asks it to draw. */
    private static class RecordingCanvas extends Canvas {
        final List<String> text = new ArrayList<>();

        RecordingCanvas() {
            super(Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888));
        }

        @Override
        public void drawText(String string, float x, float y, Paint paint) {
            text.add(string);
            super.drawText(string, x, y, paint);
        }
    }
}
