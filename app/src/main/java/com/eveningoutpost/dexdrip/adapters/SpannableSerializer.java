package com.eveningoutpost.dexdrip.adapters;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.val;

// jamorham

// gson doesn't appear to work for span colours so we use our own bandwidth efficient serializer

public class SpannableSerializer {


    public static String serializeSpannableString(SpannableString ss) {
        if (ss == null) ss = new SpannableString(""); // use blank if input is null
        val json = new JSONObject();
        try {
            json.put("mText", ss.toString());
            json.put("bgc", extractClass(ss, BackgroundColorSpan.class));
            json.put("fgc", extractClass(ss, ForegroundColorSpan.class));

        } catch (JSONException e) {
            // we're done for if we hit here
        }
        return json.toString();
    }

    public static SpannableString unserializeSpannableString(final String str) {
        try {
            val json = new JSONObject(str);
            val ss = new SpannableString(json.getString("mText"));
            pushSpanColor(json.getJSONArray("fgc"), ss, 1);
            pushSpanColor(json.getJSONArray("bgc"), ss, 2);
            return ss;

        } catch (JSONException e) {
            return new SpannableString("serialize error");
        }
    }

    private static JSONArray extractClass(final SpannableString ss, final Class<? extends CharacterStyle> clz) {
        val array = new JSONArray();
        val spansBg = ss.getSpans(0, ss.length(), clz);
        for (val span : spansBg) {
            int col;
            switch (clz.getSimpleName()) {
                case "BackgroundColorSpan":
                    col = ((BackgroundColorSpan) span).getBackgroundColor();
                    break;
                case "ForegroundColorSpan":
                    col = ((ForegroundColorSpan) span).getForegroundColor();
                    break;
                default:
                    throw new RuntimeException("Cant match extract class type: " + clz.getSimpleName());
            }
            pullSpanColor(ss, span, col, array);
        }
        return array;
    }

    private static void pushSpanColor(final JSONArray array, final SpannableString ss, final int type) {
        for (int i = 0; i < array.length(); i++) {
            try {
                val jsonObject = array.getJSONObject(i);
                int col = jsonObject.getInt("c");
                int start = jsonObject.getInt("s");
                int end = jsonObject.getInt("e");
                CharacterStyle style;
                switch (type) {
                    case 1:
                        style = new ForegroundColorSpan(col);
                        break;
                    case 2:
                        style = new BackgroundColorSpan(col);
                        break;
                    default:
                        throw new RuntimeException("unknown pushSpanColor type: " + type);
                }
                ss.setSpan(style, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } catch (JSONException e) {
                //
            }
        }
    }

    private static void pullSpanColor(final SpannableString ss, final CharacterStyle spans, final int colour, final JSONArray array) {
        val jsonObject = new JSONObject();
        try {
            jsonObject.put("c", colour);
            jsonObject.put("s", ss.getSpanStart(spans));
            jsonObject.put("e", ss.getSpanEnd(spans));
        } catch (JSONException e) {
            //
        }
        array.put(jsonObject);
    }

}
