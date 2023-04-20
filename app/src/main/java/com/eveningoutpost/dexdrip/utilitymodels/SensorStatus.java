package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.models.Sensor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.eveningoutpost.dexdrip.models.JoH.tsl;

// jamorham

public class SensorStatus {

    // TODO i18n
    public static String status() {
        final StringBuilder sensor_status = new StringBuilder();
        if (Sensor.isActive()) {
            final Sensor sensor = Sensor.currentSensor();
            final Date date = new Date(sensor.started_at);
            DateFormat df = new SimpleDateFormat();
            sensor_status.append(df.format(date));
            sensor_status.append(" (");
            sensor_status.append((tsl() - sensor.started_at) / Constants.DAY_IN_MS);
            sensor_status.append("d ");
            sensor_status.append(((tsl() - sensor.started_at) % Constants.DAY_IN_MS) / Constants.HOUR_IN_MS);
            sensor_status.append("h)");
        } else {
            sensor_status.append("not available");
        }
        return sensor_status.toString();
    }

}
