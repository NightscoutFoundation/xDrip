package com.eveningoutpost.dexdrip.cgm.webfollow;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

import lombok.val;

/**
 * JamOrHam
 * Easy access generic json mapifier
 */

public class Mapifier {

    private final Type tt = new TypeToken<Map<String, Object>>() {
    }.getType();
    private final Gson gson = new Gson();
    private final Map<String, Object> m;

    public Mapifier(final String json) {
        this.m = gson.fromJson(json, tt);
    }

    public Mapifier(final Map<String, Object> m) {
        this.m = m;
    }

    @SuppressWarnings("unchecked")
    public Object pluck(final String reference, final boolean singular) {
        Map<String, Object> item = m;
        val names = reference.split("\\.");
        val last = names[names.length - 1];
        for (val name : names) {
            val o = item.get(name);
            if (o == null) {
                break;
            }
            if (o instanceof LinkedTreeMap) {
                item = (Map<String, Object>) o;
            } else if (o instanceof ArrayList) {
                val a = (ArrayList<?>) o;
                if (singular) {
                    if (a.size() != 1) {
                        throw new RuntimeException("Invalid singular");
                    }
                } else if (name.equals(last)) {
                    return a;
                }
                item = (Map<String, Object>) a.get(0);
            } else {
                return o.toString();
            }
        }
        return null;
    }

    public Object pluckAny(final String reference) {
        return pluck(reference, false);
    }

    public String pluckString(final String reference) {
        return (String) pluck(reference, true);
    }

    public Double pluckDouble(final String reference) {
        try {
            return Double.parseDouble(pluckString(reference).replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }

    public String toString() {
        val sb = new StringBuilder();
        if (m == null) {
            sb.append("Null map");
        } else {
            for (val entry : m.entrySet()) {
                sb.append(entry.getValue().getClass().getSimpleName());
                sb.append(" :: ");
                sb.append(entry);
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
