package com.eveningoutpost.dexdrip.cgm.nsfollow.messages;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Profile extends BaseMessage {
    @Expose
    public String _id;
    @Expose
    public String defaultProfile;
    @Expose
    public String startDate; // ISO date time
    @Expose
    public String created_at; // ISO date time
    @Expose
    public HashMap<String, SingleProfile> store;

    public static int compare(Profile a, Profile b) {
        return b.startDate.compareTo(a.startDate);
    }

    public List<Double> getDefaultBasalProfile() {
        SingleProfile singleProfile = store.get(defaultProfile);

        if (singleProfile == null) {
            return new ArrayList<>();
        }

        ArrayList<BasalProfileEntry> profileFromNS = singleProfile.basal;

        int oneHourAsSeconds = 3600;

        for (int i = 0; profileFromNS.size() >= i + 1; i++) {
            int nextIndex = i + 1;

            if (profileFromNS.size() <= nextIndex) {
                break;
            }

            BasalProfileEntry profileEntry = profileFromNS.get(i);
            BasalProfileEntry nextProfileEntry = profileFromNS.get(i + 1);

            int nextTimeAsSeconds = profileEntry.timeAsSeconds + oneHourAsSeconds;

            if (nextProfileEntry.timeAsSeconds > nextTimeAsSeconds) {
                profileFromNS.add(nextIndex, new BasalProfileEntry(nextTimeAsSeconds, profileEntry.value));
            }
        }

        return profileFromNS.stream().map(x -> x.value)
                .collect(Collectors.toList());
    }
}

