package com.eveningoutpost.dexdrip.importedlibraries.dexcom;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;

import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.CalRecord;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.EGVRecord;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.GenericXMLRecord;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.MeterRecord;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.PageHeader;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.SensorRecord;
import com.eveningoutpost.dexdrip.importedlibraries.usbserial.driver.UsbSerialDriver;
import com.eveningoutpost.dexdrip.models.UserError.Log;

import org.w3c.dom.Element;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class ReadData {

// This code and this particular library are from the NightScout android uploader
// Check them out here: https://github.com/nightscout/android-uploader
// Some of this code may have been modified for use in this project

    private static final String TAG = ReadData.class.getSimpleName();
    private static final int IO_TIMEOUT = 3000;
    private static final int MIN_LEN = 256;
    private UsbSerialDriver mSerialDevice;
    protected final Object mReadBufferLock = new Object();
    private UsbDeviceConnection mConnection;
    private UsbDevice mDevice;

    public ReadData(){}
    public ReadData(UsbSerialDriver device) {
        mSerialDevice = device;
    }
    public ReadData(UsbSerialDriver device, UsbDeviceConnection connection, UsbDevice usbDevice) {
        mSerialDevice = device;
        mConnection = connection;
        mDevice = usbDevice;
        try {
      mSerialDevice.getPorts().get(0).open(connection);
        } catch(IOException e) {
            Log.d("FAILED WHILE", "trying to open");
        }
//        }
    }

    public EGVRecord[] getRecentEGVs() {
        int recordType = Dex_Constants.RECORD_TYPES.EGV_DATA.ordinal();
        int endPage = readDataBasePageRange(recordType);
        return readDataBasePage(recordType, endPage);
    }

    public EGVRecord[] getRecentEGVsPages(int numOfRecentPages) {
        if (numOfRecentPages < 1) {
            throw new IllegalArgumentException("Number of pages must be greater than 1.");
        }
        Log.d(TAG, "Reading EGV page range...");
        int recordType = Dex_Constants.RECORD_TYPES.EGV_DATA.ordinal();
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
        int recordType = Dex_Constants.RECORD_TYPES.METER_DATA.ordinal();
        int endPage = readDataBasePageRange(recordType);
        return readDataBasePage(recordType, endPage);
    }

    public SensorRecord[] getRecentSensorRecords(int numOfRecentPages) {
        if (numOfRecentPages < 1) {
            throw new IllegalArgumentException("Number of pages must be greater than 1.");
        }
        Log.d(TAG, "Reading Sensor page range...");
        int recordType = Dex_Constants.RECORD_TYPES.SENSOR_DATA.ordinal();
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
        int recordType = Dex_Constants.RECORD_TYPES.CAL_SET.ordinal();
        int endPage = readDataBasePageRange(recordType);
        Log.d(TAG, "Reading Cal Records page...");
        return readDataBasePage(recordType, endPage);
    }
    public byte[] getRecentCalRecordsTest() {
        Log.d(TAG, "Reading Cal Records page range...");
        int recordType = Dex_Constants.RECORD_TYPES.CAL_SET.ordinal();
        int endPage = readDataBasePageRange(recordType);
        Log.d(TAG, "Reading Cal Records page...");
        return readDataBasePageTest(recordType, endPage);
    }

    public boolean ping() {
        writeCommand(Dex_Constants.PING);
        return read(MIN_LEN).getCommand() == Dex_Constants.ACK;
    }

    public int readBatteryLevel() {
        Log.d(TAG, "Reading battery level...");
        writeCommand(Dex_Constants.READ_BATTERY_LEVEL);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public String readSerialNumber() {
        int PAGE_OFFSET = 0;
        byte[] readData = readDataBasePage(Dex_Constants.RECORD_TYPES.MANUFACTURING_DATA.ordinal(), PAGE_OFFSET);
        Element md = ParsePage(readData, Dex_Constants.RECORD_TYPES.MANUFACTURING_DATA.ordinal());
        return md.getAttribute("SerialNumber");
    }

    public Date readDisplayTime() {
        return Utils.receiverTimeToDate(readSystemTime() + readDisplayTimeOffset());
    }

    public long readSystemTime() {
        Log.d(TAG, "Reading system time...");
        writeCommand(Dex_Constants.READ_SYSTEM_TIME);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffff;
    }

    public int readDisplayTimeOffset() {
        Log.d(TAG, "Reading display time offset...");
        writeCommand(Dex_Constants.READ_DISPLAY_TIME_OFFSET);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffff;
    }

    private int readDataBasePageRange(int recordType) {
        ArrayList<Byte> payload = new ArrayList<Byte>();
        Log.d(TAG, "adding Payload");
        payload.add((byte) recordType);
        Log.d(TAG, "Sending write command");
        writeCommand(Dex_Constants.READ_DATABASE_PAGE_RANGE, payload);
        Log.d(TAG, "About to call getdata");
        byte[] readData = read(MIN_LEN).getData();
        Log.d(TAG, "Going to return");
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
    }

    private <T> T readDataBasePage(int recordType, int page) {
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
        writeCommand(Dex_Constants.READ_DATABASE_PAGES, payload);
        byte[] readData = read(2122).getData();
        return ParsePage(readData, recordType);
    }
    private byte[] readDataBasePageTest(int recordType, int page) {
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
        return writeCommandTest(Dex_Constants.READ_DATABASE_PAGES, payload);
    }

    private void writeCommand(int command, ArrayList<Byte> payload) {
        byte[] packet = new PacketBuilder(command, payload).compose();
        if (mSerialDevice != null) {
            try {
//                UsbInterface mDataInterface = mDevice.getInterface(1);
//                UsbEndpoint mWriteEndpoint = mDataInterface.getEndpoint(0);
//                mConnection.bulkTransfer(mWriteEndpoint, packet, packet.length, IO_TIMEOUT);
                  mSerialDevice.getPorts().get(0).write(packet, IO_TIMEOUT);
            } catch (Exception e) {
                Log.e(TAG, "Unable to write to serial device.", e);
            }
        }
    }
    private byte[] writeCommandTest(int command, ArrayList<Byte> payload) {
        byte[] packet = new PacketBuilder(command, payload).compose();
        return packet;
    }
    private void writeCommand(int command) {
        byte[] packet = new PacketBuilder(command).compose();
        if (mSerialDevice != null) {
            try {
//                UsbInterface mDataInterface = mDevice.getInterface(1);
//                UsbEndpoint mWriteEndpoint = mDataInterface.getEndpoint(0);
//                mConnection.bulkTransfer(mWriteEndpoint, packet, packet.length, IO_TIMEOUT);
                mSerialDevice.getPorts().get(0).write(packet, IO_TIMEOUT);
            } catch (Exception e) {
                Log.e(TAG, "Unable to write to serial device.", e);
            }
        }
    }

    private ReadPacket read(int numOfBytes) {
        byte[] readData = new byte[numOfBytes];
        int len = 0;
        try {
//            UsbInterface mDataInterface = mDevice.getInterface(1);
//            UsbEndpoint mReadEndpoint = mDataInterface.getEndpoint(1);
//            byte[] mReadBuffer;
//            mReadBuffer = new byte[16 * 1024];
//
//            int readAmt = Math.min(readData.length, mReadBuffer.length);
//            synchronized (mReadBufferLock) {
//
//
//                Log.d(TAG, "Read about to call bulk transfer.");
//                if (len < 0) {
//                    // This sucks: we get -1 on timeout, not 0 as preferred.
//                    // We *should* use UsbRequest, except it has a bug/api oversight
//                    // where there is no way to determine the number of bytes read
//                    // in response :\ -- http://b.android.com/28023
//                    if (IO_TIMEOUT == Integer.MAX_VALUE) {
//                        // Hack: Special case "~infinite timeout" as an error.
//                        len = -1;
//                    }
//                    len = 0;
//                }
//
////              System.arraycopy(mReadBuffer, 0, readData, 0, readAmt);
//            }
//            len = mConnection.bulkTransfer(mReadEndpoint, readData, readAmt, IO_TIMEOUT);

            len = mSerialDevice.getPorts().get(0).read(readData, IO_TIMEOUT);

            Log.d(TAG, "Read " + len + " byte(s) complete.");

            // Add a 100ms delay for when multiple write/reads are occurring in series
            Thread.sleep(100);

            // TODO: this debug code to print data of the read, should be removed after
            // finding the source of the reading issue
            String bytes = "";
            int readAmount = len;
            for (int i = 0; i < readAmount; i++) bytes += String.format("%02x", readData[i]) + " ";
            Log.d(TAG, "Read data: " + bytes);
            ////////////////////////////////////////////////////////////////////////////////////////

        } catch (Exception e) {
            Log.e(TAG, "Unable to read from serial device.", e);
        }
        byte[] data = Arrays.copyOfRange(readData, 0, len);
        return new ReadPacket(data);
    }

    private <T> T ParsePage(byte[] data, int recordType) {
        int HEADER_LEN = 28;
        PageHeader pageHeader=new PageHeader(data);
        int NUM_REC_OFFSET = 4;
        int numRec = data[NUM_REC_OFFSET];
        int rec_len;

        switch (Dex_Constants.RECORD_TYPES.values()[recordType]) {
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
