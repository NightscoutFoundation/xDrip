package com.eveningoutpost.dexdrip.services.broadcastservice;

public class Const {
    public static final String BG_ALERT_TYPE = "BG_ALERT_TYPE";

    public static final String INTENT_FUNCTION_KEY = "FUNCTION";
    public static final String INTENT_PACKAGE_KEY = "PACKAGE";
    public static final String INTENT_REPLY_MSG = "REPLY_MSG";
    public static final String INTENT_REPLY_CODE = "REPLY_CODE";
    public static final String INTENT_SETTINGS = "SETTINGS";
    public static final String INTENT_ALERT_TYPE = "ALERT_TYPE";
    public static final String INTENT_STAT_HOURS = "STAT_HOURS";

    public static final String INTENT_REPLY_CODE_OK = "OK";
    public static final String INTENT_REPLY_CODE_ERROR = "ERROR";
    public static final String INTENT_REPLY_CODE_PACKAGE_ERROR = "ERROR_NO_PACKAGE";
    public static final String INTENT_REPLY_CODE_NOT_REGISTERED = "NOT_REGISTERED";

    public static final String CMD_SET_SETTINGS = "set_settings";
    public static final String CMD_UPDATE_BG_FORCE = "update_bg_force";
    public static final String CMD_ALERT = "alarm";
    public static final String CMD_CANCEL_ALERT = "cancel_alarm";
    public static final String CMD_SNOOZE_ALERT = "snooze_alarm";
    public static final String CMD_ADD_STEPS = "add_steps";
    public static final String CMD_ADD_HR = "add_hrs";
    public static final String CMD_ADD_TREATMENT = "add_treatment";
    /**
     * The command which  {@link BroadcastService} send when it is starter, this wil allow to notify other applications that service is ready to communicate
     */
    public static final String CMD_START = "start";
    public static final String CMD_UPDATE_BG = "update_bg";
    public static final String CMD_REPLY_MSG = "reply_msg";
    public static final String CMD_STAT_INFO = "stat_info";
}
