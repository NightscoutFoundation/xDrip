package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CareLink SensorGlucose message with helper methods for processing
 */
public class SensorGlucose {

    public long sg;
    public String datetime;
    public boolean timeChange;
    public String kind;
    public long version;

    public long timestamp = -1;
    public Date date = null;

    public Long getTimestamp() {
        getDate();
        return timestamp;
    }

    public Date getDate(){
        //Set date
        if (timestamp == -1) {
            try {
                Matcher matcher = null;
                //timezone offset hours one digit to two digits
                matcher = Pattern.compile("(.*)([\\+|-])(\\d:\\d\\d)").matcher(this.datetime);
                if(matcher.matches()) {
                    this.datetime = matcher.group(1) + matcher.group(2) + "0" + matcher.group(3);
                }
                //remove millis
                matcher = Pattern.compile("(.*:\\d{2})(\\.\\d*)(.*)").matcher(this.datetime);
                if(matcher.matches()) {
                    this.datetime = matcher.group(1) + matcher.group(3);
                }
                date = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")).parse(this.datetime);
            } catch (Exception e1) {
                try {
                    date = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")).parse(this.datetime);
                } catch (Exception e2) {
                    timestamp = 0L;
                }
            }
        }
        //Set timezone
        if(date != null) timestamp = date.getTime();
        return  date;
    }

    public String toS() {
        String dt;
        if (datetime == null) { dt = ""; } else{ dt = datetime; }
        return dt + " "  + String.valueOf(sg);
    }

}