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


    // openDataSet and others return this in the root of the json reply it seems
    String id;
    String uploadId;

    public String getUploadId() {
        return (data != null && data.uploadId != null) ? data.uploadId : uploadId;
    }



}
