package com.eveningoutpost.dexdrip.ImportedLibraries.dexcom;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;

import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.CalRecord;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.EGVRecord;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.GenericXMLRecord;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.MeterRecord;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.PageHeader;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.SensorRecord;
import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.driver.UsbSerialDriver;
import com.eveningoutpost.dexdrip.ShareTest;
import com.eveningoutpost.dexdrip.UtilityModels.DexShareAttributes;

import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;

public class ReadDataShare {

    private static final String TAG = ReadDataShare.class.getSimpleName();
    private static final int IO_TIMEOUT = 3000;
    private static final int MIN_LEN = 256;

    //RXJAVA STUFF
    boolean blockingRxJava = false;
    byte[] accumulatedResponse;
    EGVRecord[] accumulatedEgvRecords;

    private ShareTest mShareTest;

    public ReadDataShare(ShareTest aShareTest){
        mShareTest = aShareTest;
    }

    public void getRecentEGVs(final Action1<EGVRecord[]> recordListener) {
        Log.d(TAG, "Reading Evg page...");
        int recordType = Constants.RECORD_TYPES.EGV_DATA.ordinal();
        standardPageReader(recordType, recordListener);
    }

    public void getRecentMeterRecords(final Action1<MeterRecord[]> recordListener) {
        Log.d(TAG, "Reading Meter page...");
        int recordType = Constants.RECORD_TYPES.METER_DATA.ordinal();
        standardPageReader(recordType, recordListener);
    }

    public void getRecentCalRecords(final Action1<CalRecord[]> recordListener) {
        Log.d(TAG, "Reading Cal Records page range...");
        int recordType = Constants.RECORD_TYPES.CAL_SET.ordinal();
        standardPageReader(recordType, recordListener);
    }

    private <T> T standardPageReader(final int recordType, final Action1<T> recordListener){
        final Action1<byte[]> fullPageListener = new Action1<byte[]>() {
            @Override
            public void call(byte[] s) { ParsePage(s, recordType, recordListener); }
        };
        Action1<Integer> databasePageRangeCaller = new Action1<Integer>() {
            @Override
            public void call(Integer s) { readDataBasePage(recordType, s, fullPageListener); }
        };
        readDataBasePageRange(recordType, databasePageRangeCaller);
        return (T) null;
    }

    public void getTimeSinceEGVRecord(final EGVRecord egvRecord,final Action1<Integer> timeSinceEgvRecord) {
        Action1<Integer> tempSystemTimeListener = new Action1<Integer>() {
            @Override
            public void call(Integer s) {
                Observable.just((int) ((long) s - egvRecord.getSystemTimeSeconds())).subscribe(timeSinceEgvRecord);
            }
        };
        readSystemTime(tempSystemTimeListener);
    }

    public void getRecentEGVsPages(final int numOfRecentPages) {
        if (numOfRecentPages < 1) { throw new IllegalArgumentException("Number of pages must be greater than 1."); }
        final int recordType = Constants.RECORD_TYPES.EGV_DATA.ordinal();
        accumulatedEgvRecords = null;
//        accumulatedEgvRecordsPageCounter = 0;
//        accumulatedEgvRecordsNumOfPages = 0;

        final Action1<EGVRecord[]> egvRecordAccumulator = new Action1<EGVRecord[]>() {
            @Override
            public void call(EGVRecord[] records) {
                EGVRecord[] result = Arrays.copyOf(accumulatedEgvRecords, accumulatedEgvRecords.length + records.length);
                System.arraycopy(records, 0, result, accumulatedEgvRecords.length, records.length);
                accumulatedEgvRecords = result;
//                if(accumulatedEgvRecordsPageCounter == accumulatedEgvRecordsNumOfPages){

//                } else {
//                    readDataBasePage(recordType, nextPage, );
//                }


            }
        };
        Action1<Integer> databasePageRangeCaller = new Action1<Integer>() {
            @Override
            public void call(Integer endPage) {
                Log.d(TAG, "Reading " + numOfRecentPages + " EGV page(s)...");
                int recentPages = numOfRecentPages - 1;
                EGVRecord[] allPages = new EGVRecord[0];
                for (int i = Math.min(recentPages,endPage); i >= 0; i--) {
                    int nextPage = endPage - i;
                    Log.d(TAG, "Reading #" + i + " EGV pages (page number " + nextPage + ")");
                    EGVRecord[] ithEGVRecordPage = readDataBasePage(recordType, nextPage, null);
                    EGVRecord[] result = Arrays.copyOf(allPages, allPages.length + ithEGVRecordPage.length);
                    System.arraycopy(ithEGVRecordPage, 0, result, allPages.length, ithEGVRecordPage.length);
                    allPages = result;
                }
                Log.d(TAG, "Read complete of EGV pages.");
            }
        };
        readDataBasePageRange(recordType, databasePageRangeCaller);
//        int endPage = readDataBasePageRange(recordType);


        }




//    public SensorRecord[] getRecentSensorRecords(int numOfRecentPages) {
//        if (numOfRecentPages < 1) {
//            throw new IllegalArgumentException("Number of pages must be greater than 1.");
//        }
//        Log.d(TAG, "Reading Sensor page range...");
//        int recordType = Constants.RECORD_TYPES.SENSOR_DATA.ordinal();
//        int endPage = readDataBasePageRange(recordType);
//        Log.d(TAG, "Reading " + numOfRecentPages + " Sensor page(s)...");
//        numOfRecentPages = numOfRecentPages - 1;
//        SensorRecord[] allPages = new SensorRecord[0];
//        for (int i = Math.min(numOfRecentPages,endPage); i >= 0; i--) {
//            int nextPage = endPage - i;
//            Log.d(TAG, "Reading #" + i + " Sensor pages (page number " + nextPage + ")");
//            SensorRecord[] ithSensorRecordPage = readDataBasePage(recordType, nextPage);
//            SensorRecord[] result = Arrays.copyOf(allPages, allPages.length + ithSensorRecordPage.length);
//            System.arraycopy(ithSensorRecordPage, 0, result, allPages.length, ithSensorRecordPage.length);
//            allPages = result;
//        }
//        Log.d(TAG, "Read complete of Sensor pages.");
//        return allPages;
//    }


    public void ping(final Action1<Boolean> pingListener) {
        Log.d(TAG, "Reading battery level...");
        Action1<byte[]> pingReader = new Action1<byte[]>() {
            @Override
            public void call(byte[] s) {
                Observable.just(read(MIN_LEN, s).getCommand() == Constants.ACK).subscribe(pingListener);
            }
        };
        writeCommand(Constants.PING, pingReader);
    }

    public void readBatteryLevel(final Action1<Integer> batteryLevelListener) {
        Log.d(TAG, "Reading battery level...");
        Action1<byte[]> batteryLevelReader = new Action1<byte[]>() {
            @Override //TODO: find out if this should be wrapped in read(s).getData();
            public void call(byte[] s) {
                Observable.just(ByteBuffer.wrap(s).order(ByteOrder.LITTLE_ENDIAN).getInt()).subscribe(batteryLevelListener);
            }
        };
        writeCommand(Constants.READ_BATTERY_LEVEL, batteryLevelReader);
    }

    public void readSerialNumber(final Action1<String> serialNumberListener) {
        int PAGE_OFFSET = 0;
        final Action1<Element> manufacturingXmlListener = new Action1<Element>() {
            @Override
            public void call(Element s) {
                Observable.just(s.getAttribute("SerialNumber")).subscribe(serialNumberListener);
            }
        };
        final Action1<byte[]> manufacturingDataListener = new Action1<byte[]>() {
            @Override
            public void call(byte[] s) {
                ParsePage(s, Constants.RECORD_TYPES.MANUFACTURING_DATA.ordinal(), manufacturingXmlListener);
            }
        };
        readDataBasePage(Constants.RECORD_TYPES.MANUFACTURING_DATA.ordinal(), PAGE_OFFSET, manufacturingDataListener);
    }

    public void readDisplayTime(final Action1<Date> displayTimeListener) {
        Action1<Integer> tempSystemTimeListener = new Action1<Integer>() {
            @Override //TODO: find out if this should be wrapped in read(s).getData();
            public void call(Integer s) {
                final long systemTime = (long) s;

                Action1<Integer> tempSystemTimeListener = new Action1<Integer>() {
                    @Override //TODO: find out if this should be wrapped in read(s).getData();
                    public void call(Integer s) {
                        final long displayTime = (long) s;
                        Date dateDisplayTime = Utils.receiverTimeToDate(systemTime + displayTime);
                        Observable.just(dateDisplayTime).subscribe(displayTimeListener); }
                };
                readDisplayTimeOffset(tempSystemTimeListener);
            }
        };
        readSystemTime(tempSystemTimeListener);
    }

    public void readSystemTime(final Action1<Integer> systemTimeListener) {
        Log.d(TAG, "Reading system time...");
        Action1<byte[]> systemTimeReader = new Action1<byte[]>() {
            @Override //TODO: find out if this should be wrapped in read(s).getData();
            public void call(byte[] s) { Observable.just(ByteBuffer.wrap(s).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffff).subscribe(systemTimeListener); }
        };
        writeCommand(Constants.READ_SYSTEM_TIME, systemTimeReader);
    }

    public void readDisplayTimeOffset(final Action1<Integer> displayTimeOffsetListener) {
        Log.d(TAG, "Reading display time offset...");
        Action1<byte[]> displayTimeOffsetReader = new Action1<byte[]>() {
            @Override //TODO: find out if this should be wrapped in read(s).getData();
            public void call(byte[] s) { Observable.just(ByteBuffer.wrap(s).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffff).subscribe(displayTimeOffsetListener); }
        };
        writeCommand(Constants.READ_DISPLAY_TIME_OFFSET, displayTimeOffsetReader);
    }

    private void readDataBasePageRange(int recordType, final Action1<Integer> databasePageRangeCaller) {
        ArrayList<Byte> payload = new ArrayList<Byte>();
        payload.add((byte) recordType);
        final Action1<byte[]> databasePageRangeListener = new Action1<byte[]>() {
            @Override
            public void call(byte[] s) {
                Log.d("ShareTest", "Database Page Range received: " + s);
                byte[] readResponse = s;
                int endPage = ByteBuffer.wrap(new ReadPacket(readResponse).getData()).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
                Log.d("ShareTest", "Database Page Range ENDPAGE: " + endPage);
                Observable.just(endPage).subscribe(databasePageRangeCaller);
            }
        };
        writeCommand(Constants.READ_DATABASE_PAGE_RANGE, payload, databasePageRangeListener);
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
                    Log.d("ShareTest", "Database Response accumulator is null, setting new value to: " + s);
                    accumulatedResponse = s;
                } else {
                    try {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        outputStream.write(accumulatedResponse);
                        outputStream.write(temp);
                        accumulatedResponse = outputStream.toByteArray();
                        Log.d("ShareTest", "Combined Response: " + accumulatedResponse);
                        Log.d("ShareTest", "Combined Response length: " + accumulatedResponse.length);
                        String bytes = "";
                        int readAmount = accumulatedResponse.length;
                        for (int i = 0; i < readAmount; i++)
                            bytes += String.format("%02x", accumulatedResponse[i]) + " ";
                        Log.w("ShareTest", "Response hex: " + bytes);
                        if (temp.length < 20) {
                            Observable.just(accumulatedResponse).subscribe(fullPageListener);
                        }
                    } catch (Exception e) {
                        Log.e("ShareTest", e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        };
        writeCommand(Constants.READ_DATABASE_PAGES, payload, databasePageReader);
        return null;
    }

    private void writeCommand(int command, ArrayList<Byte> payload, Action1<byte[]> responseListener) {
        List<byte[]> packets = new PacketBuilder(command, payload).composeList();
        mShareTest.writeCommand(packets, 0, responseListener);
    }

    private void writeCommand(int command, Action1<byte[]> responseListener) {
        List<byte[]> packets = new PacketBuilder(command).composeList();
        mShareTest.writeCommand(packets, 0, responseListener);
    }

    private ReadPacket read(int numOfBytes, byte[] readPacket) {
        byte[] response = readPacket;
        int len = response.length;
        Log.d(TAG, "Read " + len + " byte(s) complete.");
        String bytes = "";
        int readAmount = len;
        for (int i = 0; i < readAmount; i++) bytes += String.format("%02x", response[i]) + " ";
        Log.d(TAG, "Read data: " + bytes);
        byte[] data = Arrays.copyOfRange(response, 0, len);
        return new ReadPacket(data);
    }

    private <T> T ParsePage(byte[] data, int recordType, Action1<T> parsedPageReceiver) {
        Log.d("ShareTest", "Parse Data Length: " + data.length);
        Log.d("ShareTest", "Parse Record Type: " + recordType);
        int HEADER_LEN = 28;
        PageHeader pageHeader=new PageHeader(data);
        int NUM_REC_OFFSET = 4;
        int numRec = data[NUM_REC_OFFSET];
        int rec_len;

        switch (Constants.RECORD_TYPES.values()[recordType]) {
            case MANUFACTURING_DATA:
                GenericXMLRecord xmlRecord = new GenericXMLRecord(Arrays.copyOfRange(data, HEADER_LEN, data.length - 1));
                blockingRxJava = false;
                Observable.just((T) xmlRecord).subscribe(parsedPageReceiver);
            case SENSOR_DATA:
                rec_len = 20;
                SensorRecord[] sensorRecords = new SensorRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    sensorRecords[i] = new SensorRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                Observable.just((T) sensorRecords).subscribe(parsedPageReceiver);
            case EGV_DATA:
                rec_len = 13;
                EGVRecord[] egvRecords = new EGVRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    egvRecords[i] = new EGVRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                Observable.just((T) egvRecords).subscribe(parsedPageReceiver);
            case METER_DATA:
                rec_len = 16;
                MeterRecord[] meterRecords = new MeterRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    meterRecords[i] = new MeterRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                Observable.just((T) meterRecords).subscribe(parsedPageReceiver);
            case CAL_SET:
                rec_len = 249;
                if (pageHeader.getRevision()<=2) {
                    rec_len = 148;
                }
                CalRecord[] calRecords = new CalRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    calRecords[i] = new CalRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                Observable.just((T) calRecords).subscribe(parsedPageReceiver);
            default:
                // Throw error "Database record not supported"
                break;
        }
        blockingRxJava = false;
        Observable.just((T) null).subscribe(parsedPageReceiver);
        return (T) null;
    }

}
