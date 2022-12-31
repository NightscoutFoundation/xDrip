package com.eveningoutpost.dexdrip.profileeditor;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

import lombok.val;

public class AapsProfile {

    @Expose
    double dia;
    @Expose
    List<AapsElement> carbratio;
    @Expose
    List<AapsElement> sens;
    @Expose
    List<AapsElement> basal;
    @Expose
    String units;
    @Expose
    String timezone;

    private List<Double> basalByMinute;
    private List<ProfileItem> profileByMinute;

    public boolean usingMgdl() {
        return units != null && units.replace("/", "").equalsIgnoreCase("mgdl");
    }

    // explode basal to per minute resolution
    public List<Double> getBasalByMinute() {
        if (basalByMinute == null) {
            if (basal != null) {
                double current = 0d;
                int currentMinute = 0;
                final List<Double> byMinute = new ArrayList<>(1440);
                for (int i = 0; i < 1440; i++) {
                    byMinute.add(current);
                }
                for (val entry : basal) {
                    for (; currentMinute < (entry.timeAsSeconds / 60); currentMinute++) {
                        byMinute.set(currentMinute, current);
                    }
                    current = (float) entry.value;
                }
                // fill tail
                for (; currentMinute < byMinute.size(); currentMinute++) {
                    byMinute.set(currentMinute, current);
                }
                basalByMinute = byMinute;
            }
        }
        return basalByMinute;
    }

    // explode profile to per minute resolution
    public List<ProfileItem> getProfileItemByMinute() {
        if (profileByMinute == null) {
            if (carbratio != null && sens != null) {
                int currentMinute = 0;
                final List<ProfileItem> byMinute = new ArrayList<>(1440);
                for (int i = 0; i < 1440; i++) {
                    byMinute.add(new ProfileItem(i, i + 1, 0d, 0d));
                }
                double lastValue = 0d;
                for (val entry : carbratio) {
                    for (; currentMinute < (entry.timeAsSeconds / 60); currentMinute++) {
                        val current = byMinute.get(currentMinute);
                        current.carb_ratio = lastValue;
                        byMinute.set(currentMinute, current);
                    }
                    lastValue = entry.value;
                }
                // fill tail
                for (; currentMinute < byMinute.size(); currentMinute++) {
                    val current = byMinute.get(currentMinute);
                    current.carb_ratio = lastValue;
                    byMinute.set(currentMinute, current);
                }

                currentMinute = 0;
                lastValue = 0d;
                for (val entry : sens) {
                    for (; currentMinute < (entry.timeAsSeconds / 60); currentMinute++) {
                        val current = byMinute.get(currentMinute);
                        current.sensitivity = lastValue;
                        byMinute.set(currentMinute, current);
                    }
                    lastValue = entry.value;
                }
                // fill tail
                for (; currentMinute < byMinute.size(); currentMinute++) {
                    val current = byMinute.get(currentMinute);
                    current.sensitivity = lastValue;
                    byMinute.set(currentMinute, current);
                }
                profileByMinute = byMinute;
            }
        }
        return profileByMinute;
    }


    // merge exploded back to consolidated ProfileItem list
    public List<ProfileItem> getXdripMergedProfileList() {
        val output = new ArrayList<ProfileItem>();
        ProfileItem current = null;
        for (val item : getProfileItemByMinute()) {
            if (current == null) {
                current = item;
                continue;
            }
            if (current.equals(item)) {
                current.end_min = item.end_min;
            } else {
                if (current.end_min == item.start_min) {
                    current.end_min--;
                }
                output.add(current);
                current = item;
            }
        }
        if (current != null) {
            current.end_min = Math.min(1439, current.end_min); // fix last inclusive minute
            output.add(current);
        }
        return output;
    }

    // barebones sanity check
    public boolean looksReasonable() {
        return basal != null && basal.size() > 0;
    }

}
