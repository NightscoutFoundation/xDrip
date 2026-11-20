package com.eveningoutpost.dexdrip;

import android.os.Looper;
import android.support.wearable.view.WatchViewStub;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Tests for the green save tick in {@link KeypadInputActivity}.
 * <p>
 * The tick used to be the background of the entry field, pinned to its right edge. On round watches that edge is
 * outside the visible circle, so the tick could not be seen. It now follows the entered text, and on round screens a
 * long value is shrunk so the value and its tick fit inside the circle.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
@Config(sdk = 33, qualifiers = "w192dp-h192dp-round-xhdpi") // text is only measured for real from SDK 26
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class KeypadInputActivityTest extends RobolectricTestWithConfig {

    private KeypadInputActivity activity;

    @Before
    public void startKeypad() {
        KeypadInputActivity.resetValues();
        // the round watches we tested get the square layout, because the stub is never told the screen is round
        activity = startKeypad(R.layout.rect_activity_main);
    }

    @After
    public void clearValues() {
        KeypadInputActivity.resetValues();
    }

    // =============================================================================================================
    @Test
    public void noTickBeforeAValueIsEntered() {
        // :: Act
        activity.findViewById(R.id.insulintabbutton).performClick();

        // :: Verify
        assertThat(entry().toString()).isEqualTo(" units");
        assertThat(ticks()).isEmpty();
    }

    // =============================================================================================================
    @Test
    public void tickFollowsTheEnteredValue() {
        // :: Setup
        activity.findViewById(R.id.insulintabbutton).performClick();

        // :: Act
        activity.findViewById(R.id.five_button).performClick();

        // :: Verify
        assertThat(entry().toString()).startsWith("5 units");
        ImageSpan[] ticks = ticks();
        assertThat(ticks).hasLength(1);
        assertThat(((Spanned) entry()).getSpanEnd(ticks[0])).isEqualTo(entry().length());
    }

    // =============================================================================================================
    @Test
    public void tickGoesAwayWhenTheValueIsDeleted() {
        // :: Setup
        activity.findViewById(R.id.insulintabbutton).performClick();
        activity.findViewById(R.id.five_button).performClick();

        // :: Act
        activity.findViewById(R.id.backspace_button).performClick();

        // :: Verify
        assertThat(entry().toString()).isEqualTo(" units");
        assertThat(ticks()).isEmpty();
    }

    // =============================================================================================================
    @Test
    public void tickFollowsTheValueOnEveryTab() {
        // :: Act
        activity.findViewById(R.id.carbstabbutton).performClick();
        activity.findViewById(R.id.one_button).performClick();
        activity.findViewById(R.id.two_button).performClick();

        // :: Verify
        assertThat(entry().toString()).startsWith("12g carbs");
        assertThat(ticks()).hasLength(1);
    }

    // =============================================================================================================
    @Test
    public void shortValueKeepsItsTextSize() {
        // :: Setup
        final float layoutSize = entryView().getTextSize();

        // :: Act
        activity.findViewById(R.id.insulintabbutton).performClick();
        activity.findViewById(R.id.five_button).performClick();

        // :: Verify
        assertThat(entryView().getTextSize()).isEqualTo(layoutSize);
    }

    // =============================================================================================================
    @Test
    public void longValueShrinksToFitTheRoundScreen() {
        // :: Setup
        final float layoutSize = entryView().getTextSize();
        activity.findViewById(R.id.carbstabbutton).performClick();

        // :: Act
        activity.findViewById(R.id.one_button).performClick();
        activity.findViewById(R.id.two_button).performClick();
        activity.findViewById(R.id.zero_button).performClick();

        // :: Verify
        assertThat(entryView().getTextSize()).isLessThan(layoutSize);
        final float width = Layout.getDesiredWidth(entry(), entryView().getPaint());
        assertThat(width).isAtMost(activity.getResources().getDisplayMetrics().widthPixels / 2f + 1);
    }

    // =============================================================================================================
    @Test
    public void textSizeComesBackWhenTheValueIsShortAgain() {
        // :: Setup
        activity.findViewById(R.id.carbstabbutton).performClick();
        activity.findViewById(R.id.one_button).performClick();
        final float shortSize = entryView().getTextSize();
        activity.findViewById(R.id.two_button).performClick();
        activity.findViewById(R.id.zero_button).performClick();

        // :: Act
        activity.findViewById(R.id.backspace_button).performClick();
        activity.findViewById(R.id.backspace_button).performClick();

        // :: Verify
        assertThat(entry().toString()).startsWith("1g carbs");
        assertThat(entryView().getTextSize()).isEqualTo(shortSize);
    }

    // =============================================================================================================
    @Test
    public void shrunkValueDoesNotMoveTheKeypad() {
        // :: Setup
        activity.findViewById(R.id.carbstabbutton).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        final int keypadTop = topOnScreen(R.id.one_button);

        // :: Act
        activity.findViewById(R.id.one_button).performClick();
        activity.findViewById(R.id.two_button).performClick();
        activity.findViewById(R.id.zero_button).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(keypadTop).isGreaterThan(0);
        assertThat(topOnScreen(R.id.one_button)).isEqualTo(keypadTop);
    }

    // =============================================================================================================
    @Test
    public void tickFollowsTheValueInTheRoundLayout() {
        // :: Setup
        activity = startKeypad(R.layout.round_activity_main);

        // :: Act
        activity.findViewById(R.id.insulintabbutton).performClick();
        activity.findViewById(R.id.five_button).performClick();

        // :: Verify
        assertThat(entry().toString()).startsWith("5 units");
        assertThat(ticks()).hasLength(1);
    }

    // =============================================================================================================
    @Test
    public void longValueStaysOnOneLineInTheRoundLayout() {
        // :: Setup
        activity = startKeypad(R.layout.round_activity_main);
        activity.findViewById(R.id.carbstabbutton).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Act
        activity.findViewById(R.id.one_button).performClick();
        activity.findViewById(R.id.two_button).performClick();
        activity.findViewById(R.id.zero_button).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(entry().toString()).startsWith("120g carbs");
        assertThat(entryView().getLineCount()).isEqualTo(1);
        final int fieldWidth = entryView().getWidth() - entryView().getTotalPaddingLeft()
                - entryView().getTotalPaddingRight();
        assertThat(Layout.getDesiredWidth(entry(), entryView().getPaint())).isAtMost(fieldWidth + 1f);
    }

    // =============================================================================================================
    @Test
    @Config(qualifiers = "notround")
    public void longValueKeepsItsTextSizeOnASquareScreen() {
        // :: Setup
        final float layoutSize = entryView().getTextSize();
        activity.findViewById(R.id.carbstabbutton).performClick();

        // :: Act
        activity.findViewById(R.id.one_button).performClick();
        activity.findViewById(R.id.two_button).performClick();
        activity.findViewById(R.id.zero_button).performClick();

        // :: Verify
        assertThat(entry().toString()).startsWith("120g carbs");
        assertThat(entryView().getTextSize()).isEqualTo(layoutSize);
    }

    // =============================================================================================================

    private KeypadInputActivity startKeypad(int layout) {
        final ActivityController<KeypadInputActivity> controller =
                Robolectric.buildActivity(KeypadInputActivity.class).create();
        ((WatchViewStub) controller.get().findViewById(R.id.watch_view_stub)).setRoundLayout(layout);
        return controller.start().resume().visible().get();
    }

    private TextView entryView() {
        return activity.findViewById(R.id.dialed_no_textview);
    }

    private int topOnScreen(int id) {
        final int[] location = new int[2];
        activity.findViewById(id).getLocationOnScreen(location);
        return location[1];
    }

    private CharSequence entry() {
        return entryView().getText();
    }

    private ImageSpan[] ticks() {
        if (!(entry() instanceof Spanned)) {
            return new ImageSpan[0];
        }
        return ((Spanned) entry()).getSpans(0, entry().length(), ImageSpan.class);
    }
}
