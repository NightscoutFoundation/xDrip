package com.eveningoutpost.dexdrip.watch;

// jamorham

import android.util.Pair;

import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

interface PrefBindingInit{
    void initialize();
}

public abstract class PrefBinding implements PrefBindingInit {
     @Getter
    private final List<Pair<String, Integer>> items = new ArrayList<>();

    protected PrefBinding(){
        initialize();
    }

    public void add(final String pref, final int value) {
        items.add(new Pair<>(pref, value));
    }

    public List<Integer> getEnabled(final String prefix) {
        final List<Integer> results = new ArrayList<>();
        for (Pair<String, Integer> pair : items) {
            if (pair.first.startsWith(prefix)) {
                if (Pref.getBooleanDefaultFalse(pair.first)) {
                    results.add(pair.second);
                }
            }
        }
        return results;
    }

    public List<Pair<Integer, Boolean>> getStates(final String prefix) {
        final List<Pair<Integer, Boolean>> results = new ArrayList<>();
        for (Pair<String, Integer> pair : items) {
            if (pair.first.startsWith(prefix)) {
                results.add(new Pair<>(pair.second, Pref.getBooleanDefaultFalse(pair.first)));
            }
        }
        return results;
    }
}
