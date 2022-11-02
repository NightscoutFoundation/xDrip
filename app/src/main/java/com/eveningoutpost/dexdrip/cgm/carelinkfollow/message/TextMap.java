package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import java.util.HashMap;

public class TextMap {


    public static final String ERROR_TEXT_PREFIX_GUARDIAN = "GM_";
    public static final String ERROR_TEXT_PREFIX_NGP = "N";

    private static HashMap<String, String> errorTextMap;
    private static HashMap<String, String> errorCodeMap;

    static {

        errorTextMap = new HashMap<>();
        errorTextMap.put("", "");
        errorTextMap.put("3", "Battery out limit");
        errorTextMap.put("4", "Delivery stopped. Check BG");
        errorTextMap.put("5", "Pump battery depleted. Insulin delivery stopped");
        errorTextMap.put("6", "Auto Off. Insulin delivery stopped");
        errorTextMap.put("16", "Pump reset. Insulin delivery stopped");
        errorTextMap.put("43", "Pump motor error. Insulin delivery stopped");
        errorTextMap.put("50", "Bolus stopped");
        errorTextMap.put("51", "Delivery limit exceeded. Check BG");
        errorTextMap.put("55", "Pump battery failed. Replace battery");
        errorTextMap.put("59", "Button error");
        errorTextMap.put("61", "Check settings. Insulin delivery stopped");
        errorTextMap.put("62", "Empty reservoir");
        errorTextMap.put("66", "No reservoir");
        errorTextMap.put("74", "Finish loading");
        errorTextMap.put("81", "Replace pump battery now");
        errorTextMap.put("82", "Low Reservoir");
        errorTextMap.put("83", "Check BG");
        errorTextMap.put("84", "Alarm clock");
        errorTextMap.put("85", "Max fill reached");
        errorTextMap.put("86", "Weak battery detected");
        errorTextMap.put("87", "Missed bolus");
        errorTextMap.put("88", "Silenced sensor alert. Check alarm history");
        errorTextMap.put("101", "High SG. CHECK BG");
        errorTextMap.put("102", "Low SG");
        errorTextMap.put("103", "Threshold Suspend");
        errorTextMap.put("104", "Meter BG now");
        errorTextMap.put("105", "Calibration Reminder");
        errorTextMap.put("106", "Calibration error");
        errorTextMap.put("107", "Sensor expired");
        errorTextMap.put("108", "Change sensor");
        errorTextMap.put("109", "Sensor error");
        errorTextMap.put("110", "Recharge transmitter");
        errorTextMap.put("111", "Transmitter battery low");
        errorTextMap.put("112", "Weak signal");
        errorTextMap.put("113", "Lost sensor");
        errorTextMap.put("114", "Sensor glucose approaching high limit");
        errorTextMap.put("115", "Sensor glucose approaching low limit");
        errorTextMap.put("116", "Sensor glucose rising rapidly");
        errorTextMap.put("117", "Sensor glucose falling rapidly");
        errorTextMap.put("Axx", "Pump error Anull");
        errorTextMap.put("Exx", "Pump error Enull");
        errorTextMap.put("N002", "Pump Error. Delivery Stopped");
        errorTextMap.put("N006", "Pump Battery Out Limit");
        errorTextMap.put("N007", "Delivery Stopped. Check BG");
        errorTextMap.put("N011", "Replace Pump Battery Now");
        errorTextMap.put("N012", "Auto Suspend Limit Reached. Delivery Stopped");
        errorTextMap.put("N024", "Critical Pump Error. Stop Pump Use. Use Other Treatment");
        errorTextMap.put("N025", "Pump Power Error. Record Settings");
        errorTextMap.put("N029", "Pump Restarted. Delivery Stopped");
        errorTextMap.put("N037", "Pump Motor Error. Delivery Stopped");
        errorTextMap.put("N051", "Bolus Stopped");
        errorTextMap.put("N052", "Delivery Limit Exceeded. Check BG");
        errorTextMap.put("N057", "Pump Battery Not Compatible");
        errorTextMap.put("N058", "Insert A New AA Battery");
        errorTextMap.put("N061", "Pump Button Error. Delivery Stopped");
        errorTextMap.put("N062", "New Notification Received From Pump");
        errorTextMap.put("N066", "No Reservoir Detected During Infusion Set Change");
        errorTextMap.put("N069", "Loading Incomplete During Infusion Set Change");
        errorTextMap.put("N073", "Replace Pump Battery Now");
        errorTextMap.put("N077", "Pump Settings Error. Delivery Stopped");
        errorTextMap.put("N084", "Pump Battery Removed. Replace Battery");
        errorTextMap.put("N100", "Bolus Entry Timed Out Before Delivery");
        errorTextMap.put("N103", "BG Check Reminder");
        errorTextMap.put("N104", "Replace Pump Battery Soon");
        errorTextMap.put("N105", "Reservoir Low. Change Reservoir Soon");
        errorTextMap.put("N107", "Missed Meal Bolus Reminder");
        errorTextMap.put("N109", "Set Change Reminder");
        errorTextMap.put("N110", "Silenced Sensor Alert. Check Alarm History");
        errorTextMap.put("N113", "Reservoir Empty. Change Reservoir Now");
        errorTextMap.put("N117", "Active Insulin Cleared");
        errorTextMap.put("N130", "Rewind Required. Delivery Stopped");
        errorTextMap.put("N140", "Delivery Suspended. Connect Infusion Set");
        errorTextMap.put("N775", "Calibrate Now");
        errorTextMap.put("N776", "Calibration Error");
        errorTextMap.put("N777", "Change Sensor");
        errorTextMap.put("N779", "Recharge Transmitter Now");
        errorTextMap.put("N780", "Lost Sensor Signal");
        errorTextMap.put("N784", "SG Rising Rapidly");
        errorTextMap.put("N794", "Sensor Expired. Change Sensor");
        errorTextMap.put("N795", "Lost Sensor Signal. Check Transmitter");
        errorTextMap.put("N796", "No Sensor Signal");
        errorTextMap.put("N797", "Sensor Connected");
        errorTextMap.put("N801", "Do Not Calibrate. Wait Up To 3 Hours");
        errorTextMap.put("N802", "Low Sensor Glucose");
        errorTextMap.put("N803", "Low Sensor Glucose. Check BG");
        errorTextMap.put("N805", "Alert Before Low. Check BG");
        errorTextMap.put("N807", "Basal Delivery Resumed. Check BG");
        errorTextMap.put("N809", "Suspend On Low. Delivery Stopped. Check BG");
        errorTextMap.put("N810", "Suspend Before Low. Delivery Stopped. Check BG");
        errorTextMap.put("N812", "Call Emergency Assistance");
        errorTextMap.put("N814", "Basal Resumed. SG Still Under Low Limit. Check BG");
        errorTextMap.put("N815", "Low Limit Changed. Basal Manually Resumed. Check BG");
        errorTextMap.put("N816", "High Sensor Glucose");
        errorTextMap.put("N817", "Alert Before High. Check BG");
        errorTextMap.put("N819", "Auto Mode Exit. Basal Delivery Started. BG Required");
        errorTextMap.put("N821", "Minimum Delivery Timeout. BG Required");
        errorTextMap.put("N822", "Maximum Delivery Timeout. BG Required");
        errorTextMap.put("N823", "High Sensor Glucose For Over 1 Hour");
        errorTextMap.put("N827", "Urgent Low Sensor Glucose. Check BG");
        errorTextMap.put("N829", "BG Required");
        errorTextMap.put("N832", "Calibration Required");
        errorTextMap.put("N833", "Correction Bolus Recommended");
        errorTextMap.put("N869", "Calibration Reminder");
        errorTextMap.put("N870", "Recharge Transmitter Soon");
        errorTextMap.put("Nnodata1", "Reconnecting To Pump");
        errorTextMap.put("Nnodata2", "Lost Signal. Check Mobile Application");
        errorTextMap.put("GM_alert.sg.threshold.low.urgent", "Urgent Low Sensor Glucose");
        errorTextMap.put("GM_alert.sg.threshold.low", "Low Sensor Glucose");
        errorTextMap.put("GM_alert.sg.predictive.low", "Low Predicted");
        errorTextMap.put("GM_alert.sg.rate.falling", "Fall Alert");
        errorTextMap.put("GM_alert.sg.threshold.high", "High Sensor Glucose");
        errorTextMap.put("GM_alert.sg.predictive.high", "High Predicted");
        errorTextMap.put("GM_alert.sg.rate.rising", "Rise Alert");
        errorTextMap.put("GM_alert.transmitter.battery", "Transmitter Battery Empty");
        errorTextMap.put("GM_alert.sensor.replace.calibrationError", "Change Sensor");
        errorTextMap.put("GM_alert.sensor.replace.sensorError", "Change Sensor");
        errorTextMap.put("GM_alert.sensor.replace.lifetime", "Sensor End of Life");
        errorTextMap.put("GM_alert.transmitter.signal", "Lost Sensor Communication");
        errorTextMap.put("GM_alert.sensor.connection", "Sensor Connected");
        errorTextMap.put("GM_alert.sensor.calibration.rejected", "Calibration Not Accepted");
        errorTextMap.put("GM_alert.sensor.calibration.calibrate_now", "Calibrate Now");
        errorTextMap.put("GM_alert.sensor.error", "Sensor Glucose Not Available");
        errorTextMap.put("GM_alert.calibration.reminder", "Calibration Reminder");
        errorTextMap.put("GM_alert.transmitter.error", "Transmitter Error");
        errorTextMap.put("GM_alert.receiver.battery.low", "Mobile Device Battery Low");
        errorTextMap.put("Nalert.sg.threshold.low.urgent", "Urgent Low Sensor Glucose");
        errorTextMap.put("Nalert.sg.threshold.low", "Low Sensor Glucose");
        errorTextMap.put("Nalert.sg.predictive.low", "Low Predicted");
        errorTextMap.put("Nalert.sg.rate.falling", "Fall Alert");
        errorTextMap.put("Nalert.sg.threshold.high", "High Sensor Glucose");
        errorTextMap.put("Nalert.sg.predictive.high", "High Predicted");
        errorTextMap.put("Nalert.sg.rate.rising", "Rise Alert");
        errorTextMap.put("Nalert.transmitter.battery", "Transmitter Battery Empty");
        errorTextMap.put("Nalert.sensor.replace.calibrationError", "Change Sensor");
        errorTextMap.put("Nalert.sensor.replace.sensorError", "Change Sensor");
        errorTextMap.put("Nalert.sensor.replace.lifetime", "Sensor End of Life");
        errorTextMap.put("Nalert.transmitter.signal", "Lost Sensor Communication");
        errorTextMap.put("Nalert.sensor.connection", "Sensor Connected");
        errorTextMap.put("Nalert.sensor.calibration.rejected", "Calibration Not Accepted");
        errorTextMap.put("Nalert.sensor.calibration.calibrate_now", "Calibrate Now");
        errorTextMap.put("Nalert.sensor.error", "Sensor Glucose Not Available");
        errorTextMap.put("Nalert.calibration.reminder", "Calibration Reminder");
        errorTextMap.put("Nalert.transmitter.error", "Transmitter Error");
        errorTextMap.put("Nalert.receiver.battery.low", "Mobile Device Battery Low");

        errorCodeMap = new HashMap<>();
        errorCodeMap.put("002", "002");
        errorCodeMap.put("003", "002");
        errorCodeMap.put("004", "002");
        errorCodeMap.put("013", "002");
        errorCodeMap.put("014", "002");
        errorCodeMap.put("015", "002");
        errorCodeMap.put("016", "002");
        errorCodeMap.put("017", "002");
        errorCodeMap.put("018", "002");
        errorCodeMap.put("019", "002");
        errorCodeMap.put("020", "002");
        errorCodeMap.put("022", "002");
        errorCodeMap.put("023", "002");
        errorCodeMap.put("026", "002");
        errorCodeMap.put("027", "002");
        errorCodeMap.put("028", "002");
        errorCodeMap.put("030", "002");
        errorCodeMap.put("031", "002");
        errorCodeMap.put("033", "002");
        errorCodeMap.put("034", "002");
        errorCodeMap.put("044", "002");
        errorCodeMap.put("045", "002");
        errorCodeMap.put("046", "002");
        errorCodeMap.put("049", "002");
        errorCodeMap.put("053", "002");
        errorCodeMap.put("054", "002");
        errorCodeMap.put("060", "002");
        errorCodeMap.put("063", "002");
        errorCodeMap.put("064", "002");
        errorCodeMap.put("065", "002");
        errorCodeMap.put("067", "002");
        errorCodeMap.put("068", "002");
        errorCodeMap.put("074", "002");
        errorCodeMap.put("075", "002");
        errorCodeMap.put("076", "002");
        errorCodeMap.put("079", "002");
        errorCodeMap.put("080", "002");
        errorCodeMap.put("081", "002");
        errorCodeMap.put("082", "002");
        errorCodeMap.put("117", "117");
        errorCodeMap.put("817", "817");
        errorCodeMap.put("805", "805");
        errorCodeMap.put("819", "819");
        errorCodeMap.put("820", "819");
        errorCodeMap.put("012", "012");
        errorCodeMap.put("807", "807");
        errorCodeMap.put("808", "807");
        errorCodeMap.put("814", "814");
        errorCodeMap.put("103", "103");
        errorCodeMap.put("829", "829");
        errorCodeMap.put("830", "829");
        errorCodeMap.put("831", "829");
        errorCodeMap.put("100", "100");
        errorCodeMap.put("051", "051");
        errorCodeMap.put("775", "775");
        errorCodeMap.put("776", "776");
        errorCodeMap.put("869", "869");
        errorCodeMap.put("832", "832");
        errorCodeMap.put("812", "812");
        errorCodeMap.put("777", "777");
        errorCodeMap.put("778", "777");
        errorCodeMap.put("789", "777");
        errorCodeMap.put("833", "833");
        errorCodeMap.put("024", "024");
        errorCodeMap.put("035", "024");
        errorCodeMap.put("040", "024");
        errorCodeMap.put("047", "024");
        errorCodeMap.put("048", "024");
        errorCodeMap.put("050", "024");
        errorCodeMap.put("055", "024");
        errorCodeMap.put("131", "024");
        errorCodeMap.put("052", "052");
        errorCodeMap.put("007", "007");
        errorCodeMap.put("008", "007");
        errorCodeMap.put("140", "140");
        errorCodeMap.put("801", "801");
        errorCodeMap.put("816", "816");
        errorCodeMap.put("823", "823");
        errorCodeMap.put("824", "823");
        errorCodeMap.put("058", "058");
        errorCodeMap.put("069", "069");
        errorCodeMap.put("780", "780");
        errorCodeMap.put("781", "780");
        errorCodeMap.put("795", "795");
        errorCodeMap.put("815", "815");
        errorCodeMap.put("802", "802");
        errorCodeMap.put("803", "803");
        errorCodeMap.put("822", "822");
        errorCodeMap.put("821", "821");
        errorCodeMap.put("107", "107");
        errorCodeMap.put("066", "066");
        errorCodeMap.put("796", "796");
        errorCodeMap.put("057", "057");
        errorCodeMap.put("006", "006");
        errorCodeMap.put("084", "084");
        errorCodeMap.put("061", "061");
        errorCodeMap.put("037", "037");
        errorCodeMap.put("038", "037");
        errorCodeMap.put("039", "037");
        errorCodeMap.put("041", "037");
        errorCodeMap.put("042", "037");
        errorCodeMap.put("043", "037");
        errorCodeMap.put("025", "025");
        errorCodeMap.put("029", "029");
        errorCodeMap.put("077", "077");
        errorCodeMap.put("779", "779");
        errorCodeMap.put("870", "870");
        errorCodeMap.put("011", "011");
        errorCodeMap.put("073", "011");
        errorCodeMap.put("104", "104");
        errorCodeMap.put("113", "113");
        errorCodeMap.put("105", "105");
        errorCodeMap.put("106", "105");
        errorCodeMap.put("130", "130");
        errorCodeMap.put("797", "797");
        errorCodeMap.put("798", "797");
        errorCodeMap.put("794", "794");
        errorCodeMap.put("109", "109");
        errorCodeMap.put("784", "784");
        errorCodeMap.put("110", "110");
        errorCodeMap.put("810", "810");
        errorCodeMap.put("811", "810");
        errorCodeMap.put("809", "809");
        errorCodeMap.put("062", "062");
        errorCodeMap.put("070", "062");
        errorCodeMap.put("071", "062");
        errorCodeMap.put("072", "062");
        errorCodeMap.put("108", "062");
        errorCodeMap.put("114", "062");
        errorCodeMap.put("786", "062");
        errorCodeMap.put("787", "062");
        errorCodeMap.put("788", "062");
        errorCodeMap.put("799", "062");
        errorCodeMap.put("806", "062");
        errorCodeMap.put("825", "062");
        errorCodeMap.put("828", "062");
        errorCodeMap.put("827", "827");

    }

    public static String getAlarmMessage(String deviceFamily, Alarm alarm) {

        return getErrorMessage(deviceFamily, alarm.kind, alarm.code);

    }

    public static String getNotificationMessage(String deviceFamily, ClearedNotification notification) {
        return getErrorMessage(deviceFamily, notification.messageId, notification.faultId);
    }

    public static String getNotificationMessage(String deviceFamily, String messageId, int faultId) {
        return getErrorMessage(deviceFamily, messageId, faultId);
    }

    public static String getErrorMessage(String deviceFamily, String guardianErrorCode, int ngpErrorCode) {
        String errorTextId;
        String internalEC;

        if (deviceFamily.equals(RecentData.DEVICE_FAMILY_GUARDIAN)) {
            if (guardianErrorCode != null)
                errorTextId = ERROR_TEXT_PREFIX_GUARDIAN + guardianErrorCode;
            else
                errorTextId = null;
        } else if (deviceFamily.equals(RecentData.DEVICE_FAMILY_NGP)) {
            String formattedEC = String.format("%03d", ngpErrorCode);
            if (errorCodeMap.containsKey(formattedEC))
                internalEC = errorCodeMap.get(formattedEC);
            else
                internalEC = formattedEC;
            errorTextId = ERROR_TEXT_PREFIX_NGP + internalEC;
        } else {
            errorTextId = null;
        }

        if (errorTextId != null && errorTextMap.containsKey(errorTextId))
            return errorTextMap.get(errorTextId);
        else
            return null;
    }

}
