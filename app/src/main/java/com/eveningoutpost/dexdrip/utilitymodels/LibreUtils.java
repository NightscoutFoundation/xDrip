package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.LibreOOPAlgorithm;
import com.eveningoutpost.dexdrip.models.SensorSanity;

import com.eveningoutpost.dexdrip.models.UserError.Log;

public class LibreUtils {

    private static final String TAG = LibreUtils.class.getSimpleName();

    private final static long[] crc16table = {
            0, 4489, 8978, 12955, 17956, 22445, 25910, 29887, 35912,
            40385, 44890, 48851, 51820, 56293, 59774, 63735, 4225, 264,
            13203, 8730, 22181, 18220, 30135, 25662, 40137, 36160, 49115,
            44626, 56045, 52068, 63999, 59510, 8450, 12427, 528, 5017,
            26406, 30383, 17460, 21949, 44362, 48323, 36440, 40913, 60270,
            64231, 51324, 55797, 12675, 8202, 4753, 792, 30631, 26158,
            21685, 17724, 48587, 44098, 40665, 36688, 64495, 60006, 55549,
            51572, 16900, 21389, 24854, 28831, 1056, 5545, 10034, 14011,
            52812, 57285, 60766, 64727, 34920, 39393, 43898, 47859, 21125,
            17164, 29079, 24606, 5281, 1320, 14259, 9786, 57037, 53060,
            64991, 60502, 39145, 35168, 48123, 43634, 25350, 29327, 16404,
            20893, 9506, 13483, 1584, 6073, 61262, 65223, 52316, 56789,
            43370, 47331, 35448, 39921, 29575, 25102, 20629, 16668, 13731,
            9258, 5809, 1848, 65487, 60998, 56541, 52564, 47595, 43106,
            39673, 35696, 33800, 38273, 42778, 46739, 49708, 54181, 57662,
            61623, 2112, 6601, 11090, 15067, 20068, 24557, 28022, 31999,
            38025, 34048, 47003, 42514, 53933, 49956, 61887, 57398, 6337,
            2376, 15315, 10842, 24293, 20332, 32247, 27774, 42250, 46211,
            34328, 38801, 58158, 62119, 49212, 53685, 10562, 14539, 2640,
            7129, 28518, 32495, 19572, 24061, 46475, 41986, 38553, 34576,
            62383, 57894, 53437, 49460, 14787, 10314, 6865, 2904, 32743,
            28270, 23797, 19836, 50700, 55173, 58654, 62615, 32808, 37281,
            41786, 45747, 19012, 23501, 26966, 30943, 3168, 7657, 12146,
            16123, 54925, 50948, 62879, 58390, 37033, 33056, 46011, 41522,
            23237, 19276, 31191, 26718, 7393, 3432, 16371, 11898, 59150,
            63111, 50204, 54677, 41258, 45219, 33336, 37809, 27462, 31439,
            18516, 23005, 11618, 15595, 3696, 8185, 63375, 58886, 54429,
            50452, 45483, 40994, 37561, 33584, 31687, 27214, 22741, 18780,
            15843, 11370, 7921, 3960 };

    // first two bytes = crc16 included in data
    static long computeCRC16(byte[] data, int start, int size){
        long crc = 0xffff;
        for (int i = start + 2; i < start + size; i++) {
            crc = ((crc >> 8) ^ crc16table[(int)(crc ^   (data[i] & 0xFF) ) & 0xff]);
        }

        long reverseCrc = 0;
        for (int i=0; i <16; i++) {
            reverseCrc = (reverseCrc << 1) | (crc & 1);
            crc >>= 1;
        }
        return reverseCrc;
    }

    static boolean CheckCRC16(byte[] data, int start, int size) {
        long crc = computeCRC16(data, start, size);
        return crc == ((data[start+1]& 0xFF) * 256 + (data[start] & 0xff));
    }
    private static boolean verifyLibrePro(byte[] data) {
        if(data.length < Constants.LIBREPRO_HEADER1_SIZE + Constants.LIBREPRO_HEADER2_SIZE) {
            Log.e(TAG, "Must have at least 80 bytes for librepro data");
            return false;
        }

        boolean checksum_ok = CheckCRC16(data, 0 ,Constants.LIBREPRO_HEADER1_SIZE);
        checksum_ok &= CheckCRC16(data, Constants.LIBREPRO_HEADER1_SIZE ,Constants.LIBREPRO_HEADER2_SIZE);
        checksum_ok &= CheckCRC16(data, Constants.LIBREPRO_HEADER1_SIZE + Constants.LIBREPRO_HEADER2_SIZE, Constants.LIBREPRO_HEADER3_SIZE);
        return checksum_ok;
    }

    // Check the CRC of a libre sensor.
    public static boolean verify(byte[] data, byte[] patchInfo) {
        LibreOOPAlgorithm.SensorType sensorType = LibreOOPAlgorithm.getSensorType(patchInfo);
        if(sensorType == LibreOOPAlgorithm.SensorType.LibreProH) {
            return verifyLibrePro(data);
        }
        // Continue for libre1,2 checks
        if(data.length < Constants.LIBRE_1_2_FRAM_SIZE) {
            Log.e(TAG, "Must have at least 344 bytes for libre data");
            return false;
        }
        boolean checksum_ok = CheckCRC16(data, 0 ,24);
        checksum_ok &= CheckCRC16(data, 24 ,296);
        checksum_ok &= CheckCRC16(data, 320 ,24);
        return checksum_ok;

    }

    public static boolean isSensorReady(byte sensorStatusByte) {
    
        String sensorStatusString = "";
        boolean ret = false;
    
        switch (sensorStatusByte) {
            case 0x01:
                sensorStatusString = "not yet started";
                break;
            case 0x02:
                sensorStatusString = "starting";
                ret = true;
                break;
            case 0x03:          // status for 14 days and 12 h of normal operation, libre reader quits after 14 days
                sensorStatusString = "ready";
                ret = true;
                break;
            case 0x04:          // status of the following 12 h, sensor delivers last BG reading constantly
                sensorStatusString = "expired";
                // @keencave: to use dead sensor for test
    //            ret = true;
                break;
            case 0x05:          // sensor stops operation after 15d after start
                sensorStatusString = "shutdown";
                // @keencave: to use dead sensors for test
    //            ret = true;
                break;
            case 0x06:
                sensorStatusString = "in failure";
                break;
            default:
                sensorStatusString = "in an unknown state";
                break;
        }
    
        Log.i(TAG, "Sensor status is: " + sensorStatusString);
    
        
        
        
        if (SensorSanity.allowTestingWithDeadSensor()) {
            Log.e(TAG, "Warning allow to use a dead sensor");
            return true;
        }
    
        if (!ret) {
            Home.toaststaticnext("Can't use this sensor as it is " + sensorStatusString);
        }
    
        return ret;
    }

    // check manufacturer bytes look valid
    public static boolean validatePatchInfo(final byte[] buffer) {
        return buffer.length >= 11 && buffer[9] == (byte) 0x07 && buffer[10] == (byte) 0xE0;
    }

    
    // This is the function that all should read (only the correct 8 bytes)
    // Since I don't have a blukon to test, not changing decodeSerialNumber
    public static String decodeSerialNumberKey(byte[] input) {
        byte[] serialBuffer = new byte[3+8];
        System.arraycopy(input,0, serialBuffer, 3, 8);
        return decodeSerialNumber( serialBuffer);
    }


    // This function assumes that the UID is starting at place 3, and is 8 bytes long
    public static String decodeSerialNumber(byte[] input) {

        String lookupTable[] =
                {
                        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                        "A", "C", "D", "E", "F", "G", "H", "J", "K", "L",
                        "M", "N", "P", "Q", "R", "T", "U", "V", "W", "X",
                        "Y", "Z"
                };
        byte[] uuidShort = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
        int i;

        for (i = 2; i < 8; i++) uuidShort[i - 2] = input[(2 + 8) - i];
        uuidShort[6] = 0x00;
        uuidShort[7] = 0x00;

        String binary = "";
        String binS = "";
        for (i = 0; i < 8; i++) {
            binS = String.format("%8s", Integer.toBinaryString(uuidShort[i] & 0xFF)).replace(' ', '0');
            binary += binS;
        }
        
        String v = "0";
        char[] pozS = {0, 0, 0, 0, 0};
        for (i = 0; i < 10; i++) {
            for (int k = 0; k < 5; k++) pozS[k] = binary.charAt((5 * i) + k);
            int value = (pozS[0] - '0') * 16 + (pozS[1] - '0') * 8 + (pozS[2] - '0') * 4 + (pozS[3] - '0') * 2 + (pozS[4] - '0') * 1;
            v += lookupTable[value];
        }
        Log.d(TAG, "decodeSerialNumber=" + v);

        return v;
    }
}
