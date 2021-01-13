package com.eveningoutpost.dexdrip.Models.usererror;


import android.util.Log;

import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import java.util.Hashtable;

public class ExtraLogTags {
    private static final String TAG = "UserError";
    static Hashtable<String, Integer> extraTags;
    ExtraLogTags () {
        extraTags = new Hashtable <String, Integer>();
        String extraLogs = Pref.getStringDefaultBlank("extra_tags_for_logging");
        readPreference(extraLogs);
    }

    /*
     * This function reads a string representing tags that the user wants to log
     * Format of string is tag1:level1,tag2,level2
     * Example of string is Alerts:i,BG:W
     *
     */
    public static void readPreference(String extraLogs) {
        extraLogs = extraLogs.trim();
        if (extraLogs.length() > 0) {
            UserErrorStore.get().createLow(TAG, "called with string " + extraLogs);
        }
        extraTags.clear();

        // allow splitting to work with a single entry and no delimiter zzz
        if ((extraLogs.length() > 1) && (!extraLogs.contains(","))) {
            extraLogs += ",";
        }
        String[] tags = extraLogs.split(",");
        if (tags.length == 0) {
            return;
        }

        // go over all tags and parse them
        for(String tag : tags) {
            if (tag.length() > 0) parseTag(tag);
        }
    }

    static void parseTag(String tag) {
        // Format is tag:level for example  Alerts:i
        String[] tagAndLevel = tag.trim().split(":");
        if(tagAndLevel.length != 2) {
            UserErrorLog.e(TAG, "Failed to parse " + tag);
            return;
        }
        String level =  tagAndLevel[1];
        String tagName = tagAndLevel[0].toLowerCase();
        if (level.compareTo("d") == 0) {
            extraTags.put(tagName, Log.DEBUG);
            UserErrorStore.get().createLow(TAG, "Adding tag with DEBUG " + tagAndLevel[0] );
            return;
        }
        if (level.compareTo("v") == 0) {
            extraTags.put(tagName, Log.VERBOSE);
            UserErrorStore.get().createLow(TAG,"Adding tag with VERBOSE " + tagAndLevel[0] );
            return;
        }
        if (level.compareTo("i") == 0) {
            extraTags.put(tagName, android.util.Log.INFO);
            UserErrorStore.get().createLow(TAG, "Adding tag with info " + tagAndLevel[0] );
            return;
        }
        UserErrorLog.e(TAG, "Unknown level for tag " + tag + " please use d v or i");
    }

    static boolean shouldLogTag(final String tag, final int level) {
        final Integer levelForTag = extraTags.get(tag != null ? tag.toLowerCase() : "");
        return levelForTag != null && level >= levelForTag;
    }
}