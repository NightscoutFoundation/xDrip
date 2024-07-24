package com.eveningoutpost.dexdrip.importedlibraries.dexcom;

import com.eveningoutpost.dexdrip.models.UserError.Log;

import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.CalRecord;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.EGVRecord;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.GenericXMLRecord;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.MeterRecord;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.PageHeader;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.SensorRecord;
import com.eveningoutpost.dexdrip.services.DexShareCollectionService;
import com.eveningoutpost.dexdrip.ShareTest;

import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;

// This code and this particular library are from the NightScout android uploader
// Check them out here: https://github.com/nightscout/android-uploader
// Some of this code may have been modified for use in this project

public class ReadDataShare {
    byte[] accumulatedResponse;
    private ShareTest mShareTest;
    private DexShareCollectionService mCollectionService;

    public ReadDataShare(ShareTest aShareTest){
        mShareTest = aShareTest;
    }
    public ReadDataShare(DexShareCollectionService collectionService){
        mCollectionService = collectionService;
    }

    public void getRecentEGVs(final Action1<EGVRecord[]> recordListener) {
        final int recordType = Dex_Constants.RECORD_TYPES.EGV_DATA.ordinal();
        final Action1<byte[]> fullPageListener = new Action1<byte[]>() {
            @Override
            public void call(byte[] s) { ParsePage(read(0,s).getData(), recordType, recordListener); }
        };
        Action1<Integer> databasePageRangeCaller = new Action1<Integer>() {
            @Override
            public void call(Integer s) { readDataBasePage(recordType, s, fullPageListener); }
        };
        readDataBasePageRange(recordType, databasePageRangeCaller);
    }

    public void getRecentMeterRecords(final Action1<MeterRecord[]> recordListener) {
        final int recordType = Dex_Constants.RECORD_TYPES.METER_DATA.ordinal();
        final Action1<byte[]> fullPageListener = new Action1<byte[]>() {
            @Override
            public void call(byte[] s) { ParsePage(read(0,s).getData(), recordType, recordListener); }
        };
        Action1<Integer> databasePageRangeCaller = new Action1<Integer>() {
            @Override
            public void call(Integer s) { readDataBasePage(recordType, s, fullPageListener); }
        };
        readDataBasePageRange(recordType, databasePageRangeCaller);
    }

    public void getRecentCalRecords(final Action1<CalRecord[]> recordListener) {
        final int recordType = Dex_Constants.RECORD_TYPES.CAL_SET.ordinal();
        final Action1<byte[]> fullPageListener = new Action1<byte[]>() {
            @Override
            public void call(byte[] s) { ParsePage(read(0,s).getData(), recordType, recordListener); }
        };
        Action1<Integer> databasePageRangeCaller = new Action1<Integer>() {
            @Override
            public void call(Integer s) { readDataBasePage(recordType, s, fullPageListener); }
        };
        readDataBasePageRange(recordType, databasePageRangeCaller);
    }


    public void getRecentSensorRecords(final Action1<SensorRecord[]> recordListener) {
        final int recordType = Dex_Constants.RECORD_TYPES.SENSOR_DATA.ordinal();
        final Action1<byte[]> fullPageListener = new Action1<byte[]>() {
            @Override
            public void call(byte[] s) { ParsePage(read(0,s).getData(), recordType, recordListener); }
        };
        Action1<Integer> databasePageRangeCaller = new Action1<Integer>() {
            @Override
            public void call(Integer s) { readDataBasePage(recordType, s, fullPageListener); }
        };
        readDataBasePageRange(recordType, databasePageRangeCaller);
    }

    public void getTimeSinceEGVRecord(final EGVRecord egvRecord, final Action1<Long> timeSinceEgvRecord) {
        Action1<Long> tempSystemTimeListener = new Action1<Long>() {
            @Override
            public void call(Long s) { Observable.just(s - egvRecord.getSystemTimeSeconds()).subscribe(timeSinceEgvRecord); }
        };
        readSystemTime(tempSystemTimeListener);
    }

    public void ping(final Action1<Boolean> pingListener) {
        Action1<byte[]> pingReader = new Action1<byte[]>() {
            @Override
            public void call(byte[] s) { Observable.just(read(0, s).getCommand() == Dex_Constants.ACK).subscribe(pingListener); }
        };
        writeCommand(Dex_Constants.PING, pingReader);
    }

    public void readBatteryLevel(final Action1<Integer> batteryLevelListener) {
        Action1<byte[]> batteryLevelReader = new Action1<byte[]>() {
            @Override //TODO: find out if this should be wrapped in read(s).getData();
            public void call(byte[] s) { Observable.just(ByteBuffer.wrap(s).order(ByteOrder.LITTLE_ENDIAN).getInt()).subscribe(batteryLevelListener); }
        };
        writeCommand(Dex_Constants.READ_BATTERY_LEVEL, batteryLevelReader);
    }

    public void readSerialNumber(final Action1<String> serialNumberListener) {
        final Action1<byte[]> manufacturingDataListener = new Action1<byte[]>() {
            @Override
            public void call(byte[] s) {
                Element el = ParsePage(s, Dex_Constants.RECORD_TYPES.MANUFACTURING_DATA.ordinal());
                Observable.just(el.getAttribute("SerialNumber")).subscribe(serialNumberListener);
            }
        };
        readDataBasePage(Dex_Constants.RECORD_TYPES.MANUFACTURING_DATA.ordinal(), 0, manufacturingDataListener);
    }

    public void readDisplayTime(final Action1<Date> displayTimeListener) {
        Action1<Long> tempSystemTimeListener = new Action1<Long>() {
            @Override
            public void call(Long s) {
                final long systemTime = s;
                Action1<Long> tempSystemTimeListener = new Action1<Long>() {
                    @Override
                    public void call(Long s) {
                        Date dateDisplayTime = Utils.receiverTimeToDate(systemTime + s);
                        Observable.just(dateDisplayTime).subscribe(displayTimeListener); }
                };
                readDisplayTimeOffset(tempSystemTimeListener);
            }
        };
        readSystemTime(tempSystemTimeListener);
    }

    public void readSystemTime(final Action1<Long> systemTimeListener) {
        Action1<byte[]> systemTimeReader = new Action1<byte[]>() {
            @Override
            public void call(byte[] s) {
                Observable.just(Utils.receiverTimeToDate(ByteBuffer.wrap(read(0,s).getData()).order(ByteOrder.LITTLE_ENDIAN).getInt()).getTime()).subscribe(systemTimeListener);
            }
        };
        writeCommand(Dex_Constants.READ_SYSTEM_TIME, systemTimeReader);
    }

    public void readDisplayTimeOffset(final Action1<Long> displayTimeOffsetListener) {
        Action1<byte[]> displayTimeOffsetReader = new Action1<byte[]>() {
            @Override
            public void call(byte[] s) { Observable.just((long) ByteBuffer.wrap(read(0,s).getData()).order(ByteOrder.LITTLE_ENDIAN).getInt()).subscribe(displayTimeOffsetListener); }
        };
        writeCommand(Dex_Constants.READ_DISPLAY_TIME_OFFSET, displayTimeOffsetReader);
    }

    private void readDataBasePageRange(int recordType, final Action1<Integer> databasePageRangeCaller) {
        ArrayList<Byte> payload = new ArrayList<Byte>();
        payload.add((byte) recordType);
        final Action1<byte[]> databasePageRangeListener = new Action1<byte[]>() {
            @Override
            public void call(byte[] s) {
                Observable.just(ByteBuffer.wrap(new ReadPacket(s).getData()).order(ByteOrder.LITTLE_ENDIAN).getInt(4)).subscribe(databasePageRangeCaller);
            }
        };
        writeCommand(Dex_Constants.READ_DATABASE_PAGE_RANGE, payload, databasePageRangeListener);
    }

    private <T> T readDataBasePage(final int recordType, int page, final Action1<byte[]> fullPageListener) {
        byte numOfPages = 1;
        if (page < 0){ throw new IllegalArgumentException("Invalid page requested:" + page); }
        ArrayList<Byte> payload = new ArrayList<Byte>();
        payload.add((byte) recordType);
        byte[] pageInt = ByteBuffer.allocate(4).putInt(page).array();
        payload.add(pageInt[3]);
        payload.add(pageInt[2]);
        payload.add(pageInt[1]);
        payload.add(pageInt[0]);
        payload.add(numOfPages);
        accumulatedResponse = null;
        final Action1<byte[]> databasePageReader = new Action1<byte[]>() {
            @Override
            public void call(byte[] s) {
                Log.d("ShareTest", "Database Page Reader received SIZE: " + s.length);
                byte[] temp = s;
                if (accumulatedResponse == null) {
                    accumulatedResponse = s;
                } else {
                    try {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        outputStream.write(accumulatedResponse);
                        outputStream.write(temp);
                        accumulatedResponse = outputStream.toByteArray();
                        Log.d("ShareTest", "Combined Response length: " + accumulatedResponse.length);
                    } catch (Exception e) { e.printStackTrace(); }
                }
                if (temp.length < 20) { Observable.just(accumulatedResponse).subscribe(fullPageListener).unsubscribe(); }
            }
        };
        writeCommand(Dex_Constants.READ_DATABASE_PAGES, payload, databasePageReader);
        return null;
    }

    private void writeCommand(int command, ArrayList<Byte> payload, Action1<byte[]> responseListener) {
        List<byte[]> packets = new PacketBuilder(command, payload).composeList();
        if(mShareTest != null) { mShareTest.writeCommand(packets, 0, responseListener); }
        else if (mCollectionService != null) { mCollectionService.writeCommand(packets, 0, responseListener); }
    }

    private void writeCommand(int command, Action1<byte[]> responseListener) {
        List<byte[]> packets = new PacketBuilder(command).composeList();
        if(mShareTest != null) { mShareTest.writeCommand(packets, 0, responseListener); }
        else if (mCollectionService != null) { mCollectionService.writeCommand(packets, 0, responseListener); }
    }

    private ReadPacket read(int numOfBytes, byte[] readPacket) {
        return new ReadPacket(Arrays.copyOfRange(readPacket, 0, readPacket.length));
    }

    private <T> T ParsePage(byte[] data, int recordType) { return ParsePage(data, recordType, null); }
    private <T> T ParsePage(byte[] data, int recordType, Action1<T> parsedPageReceiver) {
        int HEADER_LEN = 28;
        PageHeader pageHeader=new PageHeader(data);
        int NUM_REC_OFFSET = 4;
        int numRec = data[NUM_REC_OFFSET];
        int rec_len;

        switch (Dex_Constants.RECORD_TYPES.values()[recordType]) {
            case MANUFACTURING_DATA:
                GenericXMLRecord xmlRecord = new GenericXMLRecord(Arrays.copyOfRange(data, HEADER_LEN, data.length - 1));
                if(parsedPageReceiver != null) {
                    Observable.just((T) xmlRecord).subscribe(parsedPageReceiver);
                } else {
                    return (T) xmlRecord;
                }
                break;
            case SENSOR_DATA:
                rec_len = 20;
                SensorRecord[] sensorRecords = new SensorRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    sensorRecords[i] = new SensorRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                if(parsedPageReceiver != null) {
                    Observable.just((T) sensorRecords).subscribe(parsedPageReceiver);
                } else {
                    return (T) sensorRecords;
                }
                break;
            case EGV_DATA:
                rec_len = 13;
                EGVRecord[] egvRecords = new EGVRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    egvRecords[i] = new EGVRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                if(parsedPageReceiver != null) {
                    Observable.just((T) egvRecords).subscribe(parsedPageReceiver);
                } else {
                    return (T) egvRecords;
                }
                break;
            case METER_DATA:
                rec_len = 16;
                MeterRecord[] meterRecords = new MeterRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    meterRecords[i] = new MeterRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                if(parsedPageReceiver != null) {
                    Observable.just((T) meterRecords).subscribe(parsedPageReceiver);
                } else {
                    return (T) meterRecords;
                }
                break;
            case CAL_SET:
                rec_len = 249;
                if (pageHeader.getRevision()<=2) { rec_len = 148; }
                CalRecord[] calRecords = new CalRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    calRecords[i] = new CalRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                if(parsedPageReceiver != null) {
                    Observable.just((T) calRecords).subscribe(parsedPageReceiver);
                } else {
                    return (T) calRecords;
                }
                break;
            default:
                break;
        }
        Observable.just((T) null).subscribe(parsedPageReceiver);
        return (T) null;
    }
}
