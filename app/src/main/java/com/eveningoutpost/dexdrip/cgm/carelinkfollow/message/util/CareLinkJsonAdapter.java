package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CareLinkJsonAdapter extends TypeAdapter<Date> {

    protected static final SimpleDateFormat[] ZONED_DATE_FORMATS = {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
    };

    @Override
    public void write(JsonWriter out, Date value) throws IOException {

    }

    @Override
    public Date read(JsonReader reader) throws IOException {

        Date parsedDate = null;

        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }

        String dateAsString = reader.nextString();

        parsedDate = this.parseDateString(dateAsString);

        if(parsedDate == null){
            dateAsString = dateAsString + "Z";
            parsedDate = this.parseDateString(dateAsString);
        }

        return  parsedDate;

    }

    protected Date parseDateString(String dateString) {
        for (SimpleDateFormat zonedFormat : ZONED_DATE_FORMATS) {
            try {
                return zonedFormat.parse(dateString);
            } catch (Exception ex) {
            }
        }
        return null;
    }

}
