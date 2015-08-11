package com.eveningoutpost.dexdrip.ShareModels.Models;

/**
 * Created by stephenblack on 8/10/15.
 */
public class ExistingFollower {
    public String ContactId;
    public String ContactName;
    public String DisplayName;

    public boolean IsEnabled;
    public boolean IsMonitoringSessionActive;
    public int Permissions;
    public int State;

    public String SubscriberId;
    public String SubscriptionId;

    public DateTime DateTimeCreated;
    public DateTime DateTimeModified;
    public DateTime InviteExpires;

    public class DateTime {
        public String DateTime;
        public int OffsetMinutes;
    }
}
