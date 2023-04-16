package com.eveningoutpost.dexdrip.sharemodels.models;

import com.google.gson.annotations.Expose;

/**
 * Created by Emma Black on 7/17/15.
 */
public class InvitationPayload {
    public InvitationPayload(String displayName) {
        this.DisplayName = displayName;
    }

    @Expose
    public AlertSettings AlertSettings = new AlertSettings();
    @Expose
    public int Permissions = 1;
    @Expose
    public String DisplayName;

    public class AlertSettings {
        @Expose
        public HighAlert HighAlert = new HighAlert();
        @Expose
        public LowAlert LowAlert = new LowAlert();
        @Expose
        public FixedLowAlert FixedLowAlert = new FixedLowAlert();
        @Expose
        public NoDataAlert NoDataAlert = new NoDataAlert();

        public class HighAlert {
            @Expose
            public int MinValue = 200;
            @Expose
            public String AlarmDelay = "PT1H";
            @Expose
            public int AlertType = 1;
            @Expose
            public boolean IsEnabled = false;
            @Expose
            public String RealarmDelay = "PT2H";
            @Expose
            public String Sound = "High.wav";
            @Expose
            public int MaxValue = 401;
        }
        public class LowAlert {
            @Expose
            public int MinValue = 39;
            @Expose
            public String AlarmDelay = "PT30M";
            @Expose
            public int AlertType = 2;
            @Expose
            public boolean IsEnabled = false;
            @Expose
            public String RealarmDelay = "PT2H";
            @Expose
            public String Sound = "Low.wav";
            @Expose
            public int MaxValue = 70;
        }
        public class FixedLowAlert {
            @Expose
            public int MinValue = 39;
            @Expose
            public String AlarmDelay = "PT0M";
            @Expose
            public int AlertType = 3;
            @Expose
            public boolean IsEnabled = false;
            @Expose
            public String RealarmDelay = "PT30M";
            @Expose
            public String Sound = "UrgentLow.wav";
            @Expose
            public int MaxValue = 55;
        }
        public class NoDataAlert {
            @Expose
            public int MinValue = 39;
            @Expose
            public String AlarmDelay = "PT1H";
            @Expose
            public int AlertType = 4;
            @Expose
            public boolean IsEnabled = false;
            @Expose
            public String RealarmDelay = "PT0M";
            @Expose
            public String Sound = "NoData.wav";
            @Expose
            public int MaxValue = 401;
        }
    }
}
