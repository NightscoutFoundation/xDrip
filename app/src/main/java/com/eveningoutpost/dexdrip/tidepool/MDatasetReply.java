package com.eveningoutpost.dexdrip.tidepool;

import java.util.List;

public class MDatasetReply {

    Data data;

    public class Data {
        String createdTime;
        String deviceId;
        String id;
        String time;
        String timezone;
        int timezoneOffset;
        String type;
        String uploadId;
        Client client;
        String computerTime;
        String dataSetType;
        List<String> deviceManufacturers;
        String deviceModel;
        String deviceSerialNumber;
        List<String> deviceTags;
        String timeProcessing;
        String version;
        // meta
    }

    public class Client {
        String name;
        String version;

    }

}
