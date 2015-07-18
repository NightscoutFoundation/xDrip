package com.eveningoutpost.dexdrip.ShareModels;

/**
 * Created by stephenblack on 7/17/15.
 */
public class InvitationPayload {
    public InvitationPayload(String displayName) {
        this.DisplayName = displayName;
    }
    public AlertSettings AlertSettings;
    public int Permissions = 1;
    public String DisplayName;

    public class AlertSettings {
        public HighAlert HighAlert;
        public LowAlert LowAlert;
        public FixedLowAlert FixedLowAlert;
        public NoDataAlert NoDataAlert;

        public class HighAlert {
            public int MinValue = 200;
            public String AlarmDelay = "PT1H";
            public int AlertType = 1;
            public boolean IsEnabled = false;
            public String RealarmDelay = "PT2H";
            public String Sound = "High.wav";
            public int MaxValue = 401;
        }
        public class LowAlert {
            public int MinValue = 39;
            public String AlarmDelay = "PT30M";
            public int AlertType = 2;
            public boolean IsEnabled = false;
            public String RealarmDelay = "PT2H";
            public String Sound = "Low.wav";
            public int MaxValue = 70;
        }
        public class FixedLowAlert {
            public int MinValue = 39;
            public String AlarmDelay = "PT0M";
            public int AlertType = 3;
            public boolean IsEnabled = false;
            public String RealarmDelay = "PT30M";
            public String Sound = "UrgentLow.wav";
            public int MaxValue = 55;
        }
        public class NoDataAlert {
            public int MinValue = 39;
            public String AlarmDelay = "PT1H";
            public int AlertType = 4;
            public boolean IsEnabled = false;
            public String RealarmDelay = "PT0M";
            public String Sound = "NoData.wav";
            public int MaxValue = 401;
        }
    }
}
