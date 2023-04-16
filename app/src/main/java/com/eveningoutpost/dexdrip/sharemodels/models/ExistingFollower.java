package com.eveningoutpost.dexdrip.sharemodels.models;

import com.google.gson.annotations.Expose;

/**
 * Created by Emma Black on 8/10/15.
 */
public class ExistingFollower {

    @Expose
    public String ContactId;
    @Expose
    public String ContactName;
    @Expose
    public String DisplayName;

    @Expose
    public boolean IsEnabled;
    @Expose
    public boolean IsMonitoringSessionActive;
    @Expose
    public int Permissions;
    @Expose
    public int State;

    @Expose
    public String SubscriberId;
    @Expose
    public String SubscriptionId;

    @Expose
    public DateTime DateTimeCreated;
    @Expose
    public DateTime DateTimeModified;
    @Expose
    public DateTime InviteExpires;

    public class DateTime {
        @Expose
        public String DateTime;
        @Expose
        public int OffsetMinutes;
    }
}
