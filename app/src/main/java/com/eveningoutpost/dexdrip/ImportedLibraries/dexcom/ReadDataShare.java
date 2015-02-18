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
import com.eveningoutpost.dexdrip.UtilityModels.DexShareAttributes;

import org.w3c.dom.Element;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ReadDataShare {

    private static final String TAG = ReadDataShare.class.getSimpleName();
    private static final int IO_TIMEOUT = 3000;
    private static final int MIN_LEN = 256;
//    private UsbSerialDriver mSerialDevice;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mShareService;
    private BluetoothGattCharacteristic mAuthenticationCharacteristic;
    private BluetoothGattCharacteristic mSendDataCharacteristic;
    private BluetoothGattCharacteristic mReceiveDataCharacteristic;
    private BluetoothGattCharacteristic mHeartBeatCharacteristic;
    private BluetoothGattCharacteristic mCommandCharacteristic;
    private BluetoothGattCharacteristic mResponseCharacteristic;
    protected final Object mReadBufferLock = new Object();

    public ReadDataShare(BluetoothGatt bluetoothGatt, BluetoothGattService gattService,
                         BluetoothGattCharacteristic receiveDataCharacteristic,
                         BluetoothGattCharacteristic heartBeatCharacteristic,
                         BluetoothGattCharacteristic commandCharacteristic,
                         BluetoothGattCharacteristic responseCharacteristic){
        mBluetoothGatt = bluetoothGatt;
        mShareService = gattService;
        mSendDataCharacteristic = gattService.getCharacteristic(DexShareAttributes.ShareMessageReceiver);
        mReceiveDataCharacteristic = receiveDataCharacteristic;
        mHeartBeatCharacteristic = heartBeatCharacteristic;
        mCommandCharacteristic = commandCharacteristic;
//        mResponseCharacteristic = commandCharacteristic;
        mResponseCharacteristic = responseCharacteristic;
//        mCommandCharacteristic = responseCharacteristic;
    }

    public boolean getResponse(){
        delay();
        mCommandCharacteristic.setValue("1");
        Log.w("ShareTest", "success AcK");
        mBluetoothGatt.writeCharacteristic(mCommandCharacteristic);
        delay();
        delay();
        mBluetoothGatt.readCharacteristic(mResponseCharacteristic);
        if(mResponseCharacteristic != null) {
            Log.w("ShareTest", "Response characteristic is not null");
            Log.w("ShareTest", "Response:" + mResponseCharacteristic.getValue());
            mBluetoothGatt.readCharacteristic(mCommandCharacteristic);
            Log.w("ShareTest", "Command:" + mCommandCharacteristic.getValue());

        }
        if(mResponseCharacteristic.getStringValue(0).compareTo("1") == 0){
            mCommandCharacteristic.setValue("1");
            Log.w("ShareTest", "success AcK");
            mBluetoothGatt.writeCharacteristic(mCommandCharacteristic);
            delay();
            return true;
        } else {
            Log.w("ShareTest", "failure NaK");
            return false;
        }

    }

    public EGVRecord[] getRecentEGVs() {
        int recordType = Constants.RECORD_TYPES.EGV_DATA.ordinal();
        int endPage = readDataBasePageRange(recordType);
        return readDataBasePage(recordType, endPage);
    }

    public EGVRecord[] getRecentEGVsPages(int numOfRecentPages) {
        if (numOfRecentPages < 1) {
            throw new IllegalArgumentException("Number of pages must be greater than 1.");
        }
        Log.d(TAG, "Reading EGV page range...");
        int recordType = Constants.RECORD_TYPES.EGV_DATA.ordinal();
        int endPage = readDataBasePageRange(recordType);
        Log.d(TAG, "Reading " + numOfRecentPages + " EGV page(s)...");
        numOfRecentPages = numOfRecentPages - 1;
        EGVRecord[] allPages = new EGVRecord[0];
        for (int i = Math.min(numOfRecentPages,endPage); i >= 0; i--) {
            int nextPage = endPage - i;
            Log.d(TAG, "Reading #" + i + " EGV pages (page number " + nextPage + ")");
            EGVRecord[] ithEGVRecordPage = readDataBasePage(recordType, nextPage);
            EGVRecord[] result = Arrays.copyOf(allPages, allPages.length + ithEGVRecordPage.length);
            System.arraycopy(ithEGVRecordPage, 0, result, allPages.length, ithEGVRecordPage.length);
            allPages = result;
        }
        Log.d(TAG, "Read complete of EGV pages.");
        return allPages;
    }

    public long getTimeSinceEGVRecord(EGVRecord egvRecord) {
        return readSystemTime() - egvRecord.getSystemTimeSeconds();
    }

    public MeterRecord[] getRecentMeterRecords() {
        Log.d(TAG, "Reading Meter page...");
        int recordType = Constants.RECORD_TYPES.METER_DATA.ordinal();
        int endPage = readDataBasePageRange(recordType);
        return readDataBasePage(recordType, endPage);
    }

    public SensorRecord[] getRecentSensorRecords(int numOfRecentPages) {
        if (numOfRecentPages < 1) {
            throw new IllegalArgumentException("Number of pages must be greater than 1.");
        }
        Log.d(TAG, "Reading Sensor page range...");
        int recordType = Constants.RECORD_TYPES.SENSOR_DATA.ordinal();
        int endPage = readDataBasePageRange(recordType);
        Log.d(TAG, "Reading " + numOfRecentPages + " Sensor page(s)...");
        numOfRecentPages = numOfRecentPages - 1;
        SensorRecord[] allPages = new SensorRecord[0];
        for (int i = Math.min(numOfRecentPages,endPage); i >= 0; i--) {
            int nextPage = endPage - i;
            Log.d(TAG, "Reading #" + i + " Sensor pages (page number " + nextPage + ")");
            SensorRecord[] ithSensorRecordPage = readDataBasePage(recordType, nextPage);
            SensorRecord[] result = Arrays.copyOf(allPages, allPages.length + ithSensorRecordPage.length);
            System.arraycopy(ithSensorRecordPage, 0, result, allPages.length, ithSensorRecordPage.length);
            allPages = result;
        }
        Log.d(TAG, "Read complete of Sensor pages.");
        return allPages;
    }

    public CalRecord[] getRecentCalRecords() {
        Log.d(TAG, "Reading Cal Records page range...");
        int recordType = Constants.RECORD_TYPES.CAL_SET.ordinal();
        int endPage = readDataBasePageRange(recordType);
        Log.d(TAG, "Reading Cal Records page...");
        return readDataBasePage(recordType, endPage);
    }

    public boolean ping() {
        writeCommand(Constants.PING);
        return read(MIN_LEN).getCommand() == Constants.ACK;
    }

    public int readBatteryLevel() {
        Log.d(TAG, "Reading battery level...");
        writeCommand(Constants.READ_BATTERY_LEVEL);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public String readSerialNumber() {
        int PAGE_OFFSET = 0;
        byte[] readData = readDataBasePage(Constants.RECORD_TYPES.MANUFACTURING_DATA.ordinal(), PAGE_OFFSET);
        Element md = ParsePage(readData, Constants.RECORD_TYPES.MANUFACTURING_DATA.ordinal());
        return md.getAttribute("SerialNumber");
    }

    public Date readDisplayTime() {
        return Utils.receiverTimeToDate(readSystemTime() + readDisplayTimeOffset());
    }

    public long readSystemTime() {
        Log.d(TAG, "Reading system time...");
        writeCommand(Constants.READ_SYSTEM_TIME);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffff;
    }

    public int readDisplayTimeOffset() {
        Log.d(TAG, "Reading display time offset...");
        writeCommand(Constants.READ_DISPLAY_TIME_OFFSET);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffff;
    }

    private int readDataBasePageRange(int recordType) {

        ArrayList<Byte> payload = new ArrayList<Byte>();
        Log.d("ShareTest", "adding Payload");
        payload.add((byte) recordType);
        Log.d("ShareTest", "Sending write command");
        writeCommand(Constants.READ_DATABASE_PAGE_RANGE, payload);
        boolean good = getResponse();

        Log.d(TAG, "About to call getdata");
        byte[] readData = read(MIN_LEN).getData();
        Log.d(TAG, "Going to return");
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt(4);


//        byte[] payload = new byte[1];
//        payload[0] = (byte) recordType;
//        mCommandCharacteristic.setValue(payload);
//        mBluetoothGatt.writeCharacteristic(mCommandCharacteristic);
//        delay();
//        mBluetoothGatt.readCharacteristic(mResponseCharacteristic);
//        delay();
//        Log.w("ShareTest", "PageRange: " + mResponseCharacteristic.getStringValue(0));
//        Log.w("ShareTest", mResponseCharacteristic.getValue().toString());

//        return Integer.valueOf(mResponseCharacteristic.getStringValue(0));
//        ArrayList<Byte> payload = new ArrayList<Byte>();
//        Log.d(TAG, "adding Payload");
//        payload.add((byte) recordType);
//        Log.d(TAG, "Sending write command");
//        writeCommand(Constants.READ_DATABASE_PAGE_RANGE, payload);
//        Log.d(TAG, "About to call getdata");
//        byte[] readData = read(MIN_LEN).getData();
//        Log.d(TAG, "Going to return");
//        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
    }

    private <T> T readDataBasePage(int recordType, int page) {
        Log.w("ShareTest", "Record Type: "+recordType);
        Log.w("ShareTest", "Page: "+page);
        byte numOfPages = 1;
        if (page < 0){
            throw new IllegalArgumentException("Invalid page requested:" + page);
        }
        ArrayList<Byte> payload = new ArrayList<Byte>();
        payload.add((byte) recordType);
        byte[] pageInt = ByteBuffer.allocate(4).putInt(page).array();
        payload.add(pageInt[3]);
        payload.add(pageInt[2]);
        payload.add(pageInt[1]);
        payload.add(pageInt[0]);
        payload.add(numOfPages);

        Log.w("ShareTest", "payload: "+payload.toString());
        writeCommand(Constants.READ_DATABASE_PAGES, payload);
        byte[] readData = read(2122).getData();
        return ParsePage(readData, recordType);
    }

    private void writeCommand(int command, ArrayList<Byte> payload) {
        List<byte[]> packets = new PacketBuilder(command, payload).composeList();
        Log.d("ShareTest", "In Write Command");
        for(byte[] packet : packets) {
            Log.d("ShareTest", "Write Command 1");
            mSendDataCharacteristic.setValue(packet);
            mBluetoothGatt.writeCharacteristic(mSendDataCharacteristic);
            Log.d("ShareTest", "Wrote a byte message to the characteristic");
            delay();
            getResponse();
        }
    }

    private void writeCommand(int command) {
        List<byte[]> packets = new PacketBuilder(command).composeList();
        for(byte[] packet : packets) {
            mSendDataCharacteristic.setValue(packet);
            mBluetoothGatt.writeCharacteristic(mSendDataCharacteristic);
            Log.d("ShareTest", "Wrote a byte message to the characteristic");
            delay();
            getResponse();
        }
    }
    public static void delay(){
        int sleep = 10000;
        try {
            Log.d("ShareTest", "Sleeping for " + sleep + "ms");
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            Log.e("ShareTest", "INTERUPTED");
        }
    }
    private ReadPacket read(int numOfBytes) {
        mBluetoothGatt.readCharacteristic(mResponseCharacteristic);
        Log.w("ShareTest", "Response Characteristic: "+ mResponseCharacteristic.getStringValue(0));
        delay();
        mBluetoothGatt.readCharacteristic(mHeartBeatCharacteristic);
        Log.w("ShareTest", "Response Characteristic: "+ mHeartBeatCharacteristic.getStringValue(0));
        delay();
        mCommandCharacteristic.setValue("1");
        mBluetoothGatt.writeCharacteristic(mCommandCharacteristic);
        delay();
        Log.w("ShareTest", "Response Characteristic: "+ mCommandCharacteristic.getStringValue(0));
        delay();

        mBluetoothGatt.readCharacteristic(mReceiveDataCharacteristic);
        delay();
        byte[] one = mReceiveDataCharacteristic.getValue();

        Log.w("ShareTest", "One: "+ one.toString());
        Log.w("ShareTest", "One String: "+ mReceiveDataCharacteristic.getStringValue(0));
        byte[] temp;
        byte[] combined;
        mBluetoothGatt.readCharacteristic(mReceiveDataCharacteristic);
        delay();
        temp = mReceiveDataCharacteristic.getValue();

        Log.w("ShareTest", "Temp: "+ temp.toString());
        Log.w("ShareTest", "Temp String: "+ mReceiveDataCharacteristic.getStringValue(0));

        while(one.length <= numOfBytes && temp.length > 0) {
            combined = new byte[one.length + temp.length];
            System.arraycopy(one, 0, combined, 0, one.length);
            System.arraycopy(temp,0,combined,one.length,temp.length);
            one = combined;

            Log.w("ShareTest", "Partial packet: "+ one.toString());
            Log.w("ShareTest", "Partial packet: "+ one.length);
            mBluetoothGatt.readCharacteristic(mReceiveDataCharacteristic);
            delay();
            numOfBytes -= one.length;
            temp = mReceiveDataCharacteristic.getValue();
        }
        Log.w("ShareTest", "FULL PACKET: "+ one.toString());
        Log.w("ShareTest", "FULL PACKET: "+ one.length);
        return new ReadPacket(one);
    }

    private <T> T ParsePage(byte[] data, int recordType) {
        int HEADER_LEN = 28;
        PageHeader pageHeader=new PageHeader(data);
        int NUM_REC_OFFSET = 4;
        int numRec = data[NUM_REC_OFFSET];
        int rec_len;

        switch (Constants.RECORD_TYPES.values()[recordType]) {
            case MANUFACTURING_DATA:
                GenericXMLRecord xmlRecord = new GenericXMLRecord(Arrays.copyOfRange(data, HEADER_LEN, data.length - 1));
                return (T) xmlRecord;
            case SENSOR_DATA:
                rec_len = 20;
                SensorRecord[] sensorRecords = new SensorRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    sensorRecords[i] = new SensorRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                return (T) sensorRecords;
            case EGV_DATA:
                rec_len = 13;
                EGVRecord[] egvRecords = new EGVRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    egvRecords[i] = new EGVRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                return (T) egvRecords;
            case METER_DATA:
                rec_len = 16;
                MeterRecord[] meterRecords = new MeterRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    meterRecords[i] = new MeterRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                return (T) meterRecords;
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
                return (T) calRecords;
            default:
                // Throw error "Database record not supported"
                break;
        }

        return (T) null;
    }
}
