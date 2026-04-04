package com.eveningoutpost.dexdrip.stats;

import android.graphics.Paint;

import androidx.core.content.ContextCompat;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Test;

import java.lang.reflect.Field;

import static com.google.common.truth.Truth.assertThat;

/**
 * Characterization tests for {@link PercentileView}.
 *
 * @author Asbjørn Aarrestad
 */
public class PercentileViewTest extends RobolectricTestWithConfig {

    // --- Constructor ---

    /** Characterization: outerPaint color is initialized from percentile_outer resource */
    @Test
    public void constructor_setsOuterPaintColor() throws Exception {
        // :: Act
        PercentileView view = new PercentileView(xdrip.getAppContext());

        // :: Verify
        int expected = ContextCompat.getColor(xdrip.getAppContext(), R.color.percentile_outer);
        assertThat(paintColor(view, "outerPaint")).isEqualTo(expected);
    }

    /** Characterization: outerPaintLabel color is initialized from percentile_outer resource */
    @Test
    public void constructor_setsOuterPaintLabelColor() throws Exception {
        // :: Act
        PercentileView view = new PercentileView(xdrip.getAppContext());

        // :: Verify
        int expected = ContextCompat.getColor(xdrip.getAppContext(), R.color.percentile_outer);
        assertThat(paintColor(view, "outerPaintLabel")).isEqualTo(expected);
    }

    /** Characterization: innerPaint color is initialized from percentile_inner resource */
    @Test
    public void constructor_setsInnerPaintColor() throws Exception {
        // :: Act
        PercentileView view = new PercentileView(xdrip.getAppContext());

        // :: Verify
        int expected = ContextCompat.getColor(xdrip.getAppContext(), R.color.percentile_inner);
        assertThat(paintColor(view, "innerPaint")).isEqualTo(expected);
    }

    /** Characterization: innerPaintLabel color is initialized from percentile_inner resource */
    @Test
    public void constructor_setsInnerPaintLabelColor() throws Exception {
        // :: Act
        PercentileView view = new PercentileView(xdrip.getAppContext());

        // :: Verify
        int expected = ContextCompat.getColor(xdrip.getAppContext(), R.color.percentile_inner);
        assertThat(paintColor(view, "innerPaintLabel")).isEqualTo(expected);
    }

    /** Characterization: medianPaint color is initialized from percentile_median resource */
    @Test
    public void constructor_setsMedianPaintColor() throws Exception {
        // :: Act
        PercentileView view = new PercentileView(xdrip.getAppContext());

        // :: Verify
        int expected = ContextCompat.getColor(xdrip.getAppContext(), R.color.percentile_median);
        assertThat(paintColor(view, "medianPaint")).isEqualTo(expected);
    }

    /** Characterization: medianPaintLabel color is initialized from percentile_median resource */
    @Test
    public void constructor_setsMedianPaintLabelColor() throws Exception {
        // :: Act
        PercentileView view = new PercentileView(xdrip.getAppContext());

        // :: Verify
        int expected = ContextCompat.getColor(xdrip.getAppContext(), R.color.percentile_median);
        assertThat(paintColor(view, "medianPaintLabel")).isEqualTo(expected);
    }

    // --- Helpers ---

    private int paintColor(PercentileView view, String fieldName) throws Exception {
        Field field = PercentileView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return ((Paint) field.get(view)).getColor();
    }
}
