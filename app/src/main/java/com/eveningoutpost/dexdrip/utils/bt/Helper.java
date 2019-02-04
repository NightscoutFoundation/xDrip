package com.eveningoutpost.dexdrip.utils.bt;

import android.bluetooth.BluetoothDevice;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.UUID;

// jamorham

public class Helper {

    private static final SparseArray<String> statusToName = new SparseArray<>();
    private static final HashMap<String, String> characteristicToName = new HashMap<>();

    /* Reproduced from framework BluetoothGatt.java instead of referencing directly for lower api compat */
    /**
     * A GATT operation completed successfully
     */
    private static final int GATT_SUCCESS = 0;
    /**
     * GATT read operation is not permitted
     */
    private static final int GATT_READ_NOT_PERMITTED = 0x2;
    /**
     * GATT write operation is not permitted
     */
    private static final int GATT_WRITE_NOT_PERMITTED = 0x3;
    /**
     * Insufficient authentication for a given operation
     */
    private static final int GATT_INSUFFICIENT_AUTHENTICATION = 0x5;
    /**
     * The given request is not supported
     */
    private static final int GATT_REQUEST_NOT_SUPPORTED = 0x6;
    /**
     * Insufficient encryption for a given operation
     */
    private static final int GATT_INSUFFICIENT_ENCRYPTION = 0xf;
    /**
     * A read or write operation was requested with an invalid offset
     */
    private static final int GATT_INVALID_OFFSET = 0x7;
    /**
     * A write operation exceeds the maximum length of the attribute
     */
    private static final int GATT_INVALID_ATTRIBUTE_LENGTH = 0xd;
    /**
     * A remote device connection is congested.
     */
    private static final int GATT_CONNECTION_CONGESTED = 0x8f;
    /**
     * A GATT operation failed, errors other than the above
     */
    private static final int GATT_FAILURE = 0x101;


    private static final int GATT_INVALID_HANDLE = 0x01;
    //private static final int GATT_READ_NOT_PERMIT = 0x02;
    //private static final int GATT_WRITE_NOT_PERMIT = 0x03;
    private static final int GATT_INVALID_PDU = 0x04;
    //private static final int GATT_INSUF_AUTHENTICATION = 0x05;
    private static final int GATT_REQ_NOT_SUPPORTED = 0x06;
    //private static final int GATT_INVALID_OFFSET = 0x07;
    private static final int GATT_INSUF_AUTHORIZATION = 0x08;
    private static final int GATT_PREPARE_Q_FULL = 0x09;
    private static final int GATT_NOT_FOUND = 0x0a;
    private static final int GATT_NOT_LONG = 0x0b;
    private static final int GATT_INSUF_KEY_SIZE = 0x0c;
    private static final int GATT_INVALID_ATTR_LEN = 0x0d;
    private static final int GATT_ERR_UNLIKELY = 0x0e;
    private static final int GATT_INSUF_ENCRYPTION = 0x0f;
    private static final int GATT_UNSUPPORT_GRP_TYPE = 0x10;
    private static final int GATT_INSUF_RESOURCE = 0x11;
    private static final int GATT_ILLEGAL_PARAMETER = 0x87;
    private static final int GATT_NO_RESOURCES = 0x80;
    private static final int GATT_INTERNAL_ERROR = 0x81;
    private static final int GATT_WRONG_STATE = 0x82;
    private static final int GATT_DB_FULL = 0x83;
    private static final int GATT_BUSY = 0x84;
    private static final int GATT_ERROR = 0x85; // 133 error
    private static final int GATT_CMD_STARTED = 0x86;
    private static final int GATT_PENDING = 0x88;
    private static final int GATT_AUTH_FAIL = 0x89;
    private static final int GATT_MORE = 0x8a;
    private static final int GATT_INVALID_CFG = 0x8b;
    private static final int GATT_SERVICE_STARTED = 0x8c;
    private static final int GATT_ENCRYPED_MITM = GATT_SUCCESS;
    private static final int GATT_ENCRYPED_NO_MITM = 0x8d;
    private static final int GATT_NOT_ENCRYPTED = 0x8e;
    private static final int GATT_CONGESTED = 0x8f;

    private static final int GATT_CCC_CFG_ERR = 0xFD; /* Client Characteristic Configuration Descriptor Improperly Configured */
    private static final int GATT_PRC_IN_PROGRESS = 0xFE; /* Procedure Already in progress */
    private static final int GATT_OUT_OF_RANGE = 0xFF; /* Attribute value out of range */

    //private static final int GATT_CONN_UNKNOWN                   0
    //private static final int GATT_CONN_L2C_FAILURE               1                               /* general L2cap failure  */
    //private static final int GATT_CONN_TIMEOUT                   HCI_ERR_CONNECTION_TOUT         /* 0x08 connection timeout  */
    public static final int GATT_CONN_TERMINATE_PEER_USER = 0x13;   /*  HCI_ERR_PEER_USER 0x13 connection terminate by peer user  */
    private static final int GATT_CONN_TERMINATE_LOCAL_HOST = 0x16; /*     HCI_ERR_CONN_CAUSE_LOCAL_HOST   /* 0x16 connectionterminated by local host  */
    // private static final int GATT_CONN_FAIL_ESTABLISH            HCI_ERR_CONN_FAILED_ESTABLISHMENT/* 0x03E connection fail to establish  */
    private static final int GATT_CONN_LMP_TIMEOUT = 0x22;       /* HCI_ERR_LMP_RESPONSE_TIMEOUT     /* 0x22 connection fail for LMP response tout */
    // private static final int GATT_CONN_CANCEL                    L2CAP_CONN_CANCEL                /* 0x0100 L2CAP connection cancelled  */

    private static final int HCI_ERR_UNDEFINED_0x31 = 0x31;
    private static final int HCI_ERR_ROLE_SWITCH_PENDING = 0x32;
    private static final int HCI_ERR_UNDEFINED_0x33 = 0x33;
    private static final int HCI_ERR_RESERVED_SLOT_VIOLATION = 0x34;
    private static final int HCI_ERR_ROLE_SWITCH_FAILED = 0x35;
    private static final int HCI_ERR_INQ_RSP_DATA_TOO_LARGE = 0x36;
    private static final int HCI_ERR_SIMPLE_PAIRING_NOT_SUPPORTED = 0x37;
    private static final int HCI_ERR_HOST_BUSY_PAIRING = 0x38;
    private static final int HCI_ERR_REJ_NO_SUITABLE_CHANNEL = 0x39;
    private static final int HCI_ERR_CONTROLLER_BUSY = 0x3A;
    private static final int HCI_ERR_UNACCEPT_CONN_INTERVAL = 0x3B;
    private static final int HCI_ERR_DIRECTED_ADVERTISING_TIMEOUT = 0x3C;
    private static final int HCI_ERR_CONN_TOUT_DUE_TO_MIC_FAILURE = 0x3D;
    private static final int HCI_ERR_CONN_FAILED_ESTABLISHMENT = 0x3E;


    // common characteristics

    public static final UUID INFO_SYSTEM_ID = UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb");
    public static final UUID INFO_MODEL_NUMBER = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    public static final UUID INFO_SERIAL_NUMBER = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
    public static final UUID INFO_FIRMWARE_VERSION = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID INFO_HARDWARE_VERSION = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");
    public static final UUID INFO_SOFTWARE_VERSION = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb");
    public static final UUID INFO_MANUFACTURER = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    public static final UUID INFO_REGULATORY_ID = UUID.fromString("00002a2a-0000-1000-8000-00805f9b34fb");
    public static final UUID INFO_PNP_ID = UUID.fromString("00002a50-0000-1000-8000-00805f9b34fb");


    static {
        statusToName.put(GATT_SUCCESS, "Gatt Success");
        statusToName.put(GATT_READ_NOT_PERMITTED, "Read not permitted");
        statusToName.put(GATT_WRITE_NOT_PERMITTED, "Write not permitted");
        statusToName.put(GATT_INSUFFICIENT_AUTHENTICATION, "Insufficient Authentication");
        statusToName.put(GATT_REQUEST_NOT_SUPPORTED, "Request not supported");
        statusToName.put(GATT_INSUFFICIENT_ENCRYPTION, "Insufficient encryption");
        statusToName.put(GATT_INVALID_OFFSET, "Invalid Offset");
        statusToName.put(GATT_INSUF_AUTHORIZATION, "Insufficient Authorization or Connection Timeout");
        statusToName.put(GATT_INVALID_ATTRIBUTE_LENGTH, "Invalid attribute length");
        statusToName.put(GATT_CONNECTION_CONGESTED, "Connection congested");
        statusToName.put(GATT_FAILURE, "General GATT failure 133 code");

        statusToName.put(GATT_CONN_TERMINATE_PEER_USER, "Connection terminated by peer");
        statusToName.put(GATT_CONN_TERMINATE_LOCAL_HOST, "Connection terminated by local host");
        statusToName.put(HCI_ERR_UNACCEPT_CONN_INTERVAL, "HCI Unacceptable connection interval??");
        statusToName.put(GATT_CONN_LMP_TIMEOUT, "Link manager protocol timeout");


        characteristicToName.put(INFO_SYSTEM_ID.toString(), "System ID");
        characteristicToName.put(INFO_MODEL_NUMBER.toString(), "Model");
        characteristicToName.put(INFO_SERIAL_NUMBER.toString(), "Serial");
        characteristicToName.put(INFO_FIRMWARE_VERSION.toString(), "Firmware");
        characteristicToName.put(INFO_HARDWARE_VERSION.toString(), "Hardware");
        characteristicToName.put(INFO_SOFTWARE_VERSION.toString(), "Software");
        characteristicToName.put(INFO_MANUFACTURER.toString(), "Manufacturer");
        characteristicToName.put(INFO_REGULATORY_ID.toString(), "Regulatory ID");
        characteristicToName.put(INFO_PNP_ID.toString(), "PNP ID");

    }

    // feels like the framework should be providing this for us already!
    public static String getStatusName(final int status) {
        if (statusToName.indexOfKey(status) > -1) {
            return statusToName.get(status);
        } else {
            return "Unknown status: " + status;
        }
    }

    public static String getCharactersticName(final String hunt) {
        final String result = characteristicToName.get(hunt);
        return result != null ? result : "Unknown";
    }

    // TODO i18n
    public static String bondStateToString(final int bs) {
        String bondState;
        if (bs == BluetoothDevice.BOND_NONE) {
            bondState = "Unpaired";
        } else if (bs == BluetoothDevice.BOND_BONDING) {
            bondState = "Pairing";
        } else if (bs == BluetoothDevice.BOND_BONDED) {
            bondState = "Paired";
        } else if (bs == 0) {
            bondState = "Startup";
        } else {
            bondState = "Unknown bond state: " + bs;
        }
        return bondState;
    }

}
