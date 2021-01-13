package com.eveningoutpost.dexdrip.GlucoseMeter;


import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.usererror.UserErrorLog;
import com.eveningoutpost.dexdrip.Services.BluetoothGlucoseMeter;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.Services.BluetoothGlucoseMeter.mBluetoothDeviceAddress;
import static com.eveningoutpost.dexdrip.Services.BluetoothGlucoseMeter.statusUpdate;
import static com.eveningoutpost.dexdrip.utils.CRC16ccitt.crc16ccitt;

/**
 * Created by jamorham on 07/06/2017.
 * <p>
 * Verio Flex appears not to be bluetooth standards compliant and so requires additional handling
 */


public class VerioHelper {

    private static final String TAG = "GlucoseVerio";
    private static final byte DATA_DELIMITER = 0x03;
    private static final long TIME_OFFSET = 946684799L;

    public static final UUID VERIO_F7A1_SERVICE = UUID.fromString("af9df7a1-e595-11e3-96b4-0002a5d5c51b");
    public static final UUID VERIO_F7A2_WRITE = UUID.fromString("af9df7a2-e595-11e3-96b4-0002a5d5c51b");
    public static final UUID VERIO_F7A3_NOTIFICATION = UUID.fromString("af9df7a3-e595-11e3-96b4-0002a5d5c51b");

    private static int last_request_record = 0;
    private static int last_received_record = 0;
    private static Long meter_time_offset = null;
    private static int highest_record_number = -1;
    private static int number_of_records = -1;

    private static final boolean d = false;

    public synchronized static GlucoseReadingRx parseMessage(byte[] message) {

        if (message.length == 1) {
            if ((message[0] & (byte) 0x81) == (byte) 0x81) {
                BluetoothGlucoseMeter.awaiting_ack = false;
                UserErrorLog.d(TAG, "ACK received: " + JoH.bytesToHex(message));
                return null;
            }
        } else {
            if (message.length > 5) {
                // Multibyte replies
                if ((message[0] != 0x01) && (message[1] != 0x02)) {
                    UserErrorLog.e(TAG, "Invalid protocol header");
                }

                if (message[2] != (message.length - 1)) {
                    UserErrorLog.e(TAG, "length field problem " + message.length + " vs " + message[2]);
                    return null;
                }

                if (checkCRC(message)) {
                    GlucoseReadingRx gtb = null;
                    // message contains data
                    if (message[5] == 0x06) {
                        BluetoothGlucoseMeter.awaiting_data = false;
                        final byte[] result = new byte[message.length - 9];
                        System.arraycopy(message, 6, result, 0, result.length);
                        if (d)
                            UserErrorLog.d(TAG, "Returning byte array: " + JoH.bytesToHex(result));
                        final ByteBuffer data = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
                        if (d)
                            UserErrorLog.d(TAG, "Wrapped buffer result length: " + result.length);
                        if (result.length == 4) {
                            long val = data.getInt(0);
                            if (val > 100000) {
                                long tval = (val + TIME_OFFSET) * 1000; // warning signed
                                meter_time_offset = JoH.msSince(tval);

                                UserErrorLog.d(TAG, "Time offset: " + meter_time_offset);
                                UserErrorLog.d(TAG, JoH.dateTimeText(tval));
                                statusUpdate("Received Verio Time");
                            } else {
                                UserErrorLog.d(TAG, "Reading counter: " + val);
                                highest_record_number = (int) val;
                                statusUpdate("Verio Record Counter: " + val);
                            }

                        } else if (result.length == 2) {
                            int val = data.getShort(0);
                            UserErrorLog.d(TAG, " Num records: Int16 value: " + val);
                            number_of_records = val;
                            requestRecords(); // ask for record download

                        } else if (result.length == 11) {
                            if (last_request_record > 0) {
                                final long info = data.getInt(6) + data.get(10);
                                //UserErrorLog.d(TAG, "Bgreading sanity check: " + info);
                                if (info == 0) {
                                    if (meter_time_offset != null) {
                                        final long tval = ((data.getInt(0) + TIME_OFFSET) * 1000) + meter_time_offset;
                                        UserErrorLog.d(TAG, "BGreading time: " + JoH.dateTimeText(tval));
                                        final int mgdl = data.getShort(4);
                                        UserErrorLog.d(TAG, "BGreading mgdl: " + mgdl + " mmol:" + JoH.qs(((double) mgdl) * Constants.MGDL_TO_MMOLL, 1));
                                        last_received_record = last_request_record;
                                        statusUpdate("Received Verio BG record: " + last_received_record);
                                        setHighestRecord(last_received_record);
                                        gtb = new GlucoseReadingRx(); // fakeup a record
                                        gtb.mgdl = mgdl;
                                        gtb.time = tval;
                                        gtb.offset = 0;
                                        gtb.device = "OneTouch Verio Flex";
                                        gtb.data = data;
                                        gtb.sequence = last_received_record;
                                    } else {
                                        UserErrorLog.wtf(TAG, "We don't have the meter time so cannot process any records");
                                    }
                                } else {
                                    UserErrorLog.d(TAG, "Bg packet " + last_request_record + " failed sanity test: " + JoH.bytesToHex(message));
                                }
                            } else {
                                UserErrorLog.wtf(TAG, "Received a bg record we did not request");
                            }

                        }
                        BluetoothGlucoseMeter.sendImmediateData(VERIO_F7A1_SERVICE, VERIO_F7A2_WRITE, VerioHelper.getAckCMD(), "data ack");
                        return gtb;

                        // error state
                    } else if (message[5] == 0x07) {
                        if (message[6] == 0x03) {
                            UserErrorLog.e(TAG, "Verio: Command not allowed!");
                            statusUpdate("Verio Command rejected - check pairing");
                        }
                    } else if (message[5] == 0x08) {
                        if (message[6] == 0x03) {
                            UserErrorLog.e(TAG, "Verio: Command not supported!");
                        }
                    } else if (message[5] == 0x09) {
                        if (message[6] == 0x03) {
                            UserErrorLog.e(TAG, "Verio: Command not understood!");
                        }
                    }
                } else {
                    UserErrorLog.wtf(TAG, "Checksum failure on packet: " + JoH.bytesToHex(message));
                }

            } else {
                UserErrorLog.d(TAG, "Unexpected message size");
            }
        }

        UserErrorLog.e(TAG, "Unable to parse!! " + JoH.bytesToHex(message));
        return null; // ERROR
    }

    private static void requestRecords() {
        if ((highest_record_number == -1) || (number_of_records == -1)) {
            UserErrorLog.wtf(TAG, "Cannot request records as counter information is missing");
            return;
        }
        if (number_of_records == 0) {
            statusUpdate("No BG records on meter!");
            return;
        }
        final int reqmin = Math.max(highest_record_number - number_of_records, highest_record_number - 15);
        int i;
        boolean requested_something = false;
        final int t = getHighestRecord();
        for (i = highest_record_number; i > reqmin; i--) {

            if (i > t || d) {
                UserErrorLog.d(TAG, "Requesting record number: " + i);
                BluetoothGlucoseMeter.verioScheduleRequestBg(i);
                requested_something = true;
            } else {
                UserErrorLog.d(TAG, "Not requesting record as it is less than highest: " + i + " vs " + t);
            }
        }
        if (!requested_something)
            statusUpdate("No records on meter newer than we already have synced");
    }

    private static void setHighestRecord(int m) {
        if (mBluetoothDeviceAddress == null) {
            UserErrorLog.wtf(TAG, "Null BT address in setHighestRecord");
            return;
        }
        if (getHighestRecord() >= m) return; // not higher
        PersistentStore.setLong(BluetoothGlucoseMeter.mBluetoothDeviceAddress + "-verio-highest", (long) m);
    }

    private static int getHighestRecord() {
        if (mBluetoothDeviceAddress == null) {
            UserErrorLog.wtf(TAG, "Null BT address in getHighestRecord");
            return 0;
        }
        return (int) PersistentStore.getLong(BluetoothGlucoseMeter.mBluetoothDeviceAddress + "-verio-highest");
    }

    private static byte[] getAckCMD() {
        return new byte[]{(byte) 0x81};
    }

    public static byte[] getTimeCMD() {
        return transmissionTemplate(new byte[]{0x20, 0x02});
    }

    public static byte[] getRcounterCMD() {
        return transmissionTemplate(new byte[]{0x27, 0x00});
    }

    public static byte[] getTcounterCMD() {
        return transmissionTemplate(new byte[]{0x0a, 0x02, 0x06});
    }

    public static byte[] getRecordCMD(int record) {
        return transmissionTemplate(new byte[]{(byte) 0xb3, (byte) (record & 0xff), (byte) ((record >> 8) & 0xff)});
    }

    public static void updateRequestedRecord(int record) {
        last_request_record = record;
    }

    public static byte[] transmissionTemplate(byte[] payload) {
        final byte[] template = templateProducer(payload);
        final byte[] transmission_template = new byte[template.length + 1];
        transmission_template[0] = 0x01;
        System.arraycopy(template, 0, transmission_template, 1, template.length);
        return transmission_template;
    }

    public static byte[] templateProducer(byte[] payload) {

        final int packet_size = payload.length + 7;
        final byte[] template = new byte[packet_size];
        template[0] = (byte) 0x02; // always the same
        template[1] = (byte) packet_size; // size of packet including payload
        template[2] = (byte) 0x00; // always 0
        template[3] = DATA_DELIMITER; // data start marker?

        System.arraycopy(payload, 0, template, 4, payload.length);
        template[4 + payload.length] = DATA_DELIMITER;
        System.arraycopy(crc16ccitt(template, true, false), 0, template, 5 + payload.length, 2);
        if (d) UserErrorLog.d(TAG, "template output: " + JoH.bytesToHex(template));
        return template;
    }

    private static boolean checkCRC(byte[] bytes) {
        final byte[] result = crc16ccitt(bytes, true, true);
        return ((result[0] == bytes[bytes.length - 2]) && (result[1] == bytes[bytes.length - 1]));
    }




}