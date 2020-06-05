package com.eveningoutpost.dexdrip.adapters;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.ui.helpers.Span;

import org.junit.Test;

import lombok.val;

import static com.eveningoutpost.dexdrip.adapters.SpannableSerializer.serializeSpannableString;
import static com.eveningoutpost.dexdrip.adapters.SpannableSerializer.unserializeSpannableString;
import static com.google.common.truth.Truth.assertWithMessage;

// jamorham

public class SpannableSerializerTest extends RobolectricTestWithConfig {

    @Test
    public void serializeSpannableStringTest() {

        // TODO this is also testing that Span.colorSpan() output is as originally defined - really this test should not be dependent on that
        val span = Span.colorSpan("abc", Color.RED);
        span.setSpan(new ForegroundColorSpan(Color.GREEN), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        val result = serializeSpannableString(span);
        assertWithMessage("span serialize template 1").that(result).isEqualTo("{\"mText\":\" abc \",\"bgc\":[{\"c\":-65536,\"s\":0,\"e\":5}],\"fgc\":[{\"c\":-16711936,\"s\":1,\"e\":2}]}");
        val reformedResult = serializeSpannableString(unserializeSpannableString(serializeSpannableString(span)));
        assertWithMessage("span serialize reformed matches").that(reformedResult).isEqualTo(result);
        //System.out.println(reformedResult);

    }
}