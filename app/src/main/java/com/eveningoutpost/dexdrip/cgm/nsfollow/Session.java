package com.eveningoutpost.dexdrip.cgm.nsfollow;

import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.Entry;
import com.eveningoutpost.dexdrip.cgm.nsfollow.utils.NightscoutUrl;

import java.util.List;

/**
 *  jamorham
 *
 *  Session object manages a session with Nightscout
 */


@SuppressWarnings("unchecked")
public class Session {

    public NightscoutUrl url;
    public BaseCallback<List<Entry>> callback;


    // most recent set of entries
    public List<Entry> entries;


    // populate session data from a response object which could be any supported type
    public void populate(final Object object) {
        if (object instanceof List) {
            final List<Object> someList = (List<Object>)object;

            if (!someList.isEmpty() && someList.get(0) instanceof Entry) {
                entries = (List<Entry>)object;
            }

        }
    }

}
