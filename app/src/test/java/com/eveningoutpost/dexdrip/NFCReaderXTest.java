package com.eveningoutpost.dexdrip;


import com.eveningoutpost.dexdrip.Models.ReadingData;
import com.eveningoutpost.dexdrip.UtilityModels.LibreUtils;
import com.google.gson.GsonBuilder;

import org.junit.Test;


import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;

public class NFCReaderXTest extends RobolectricTestWithConfig {

    static byte[] data_libre1 = new byte[]{(byte) 0x45, (byte) 0x99, (byte) 0x08, (byte) 0x1b, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x46, (byte) 0x24, (byte) 0x00, (byte) 0x0c, (byte) 0xd3, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x9d, (byte) 0x80, (byte) 0xb8, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x5d, (byte) 0x80, (byte) 0xc1, (byte) 0x04, (byte) 0x00, (byte) 0x10, (byte) 0x9d, (byte) 0x80, (byte) 0xce, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x9d, (byte) 0x80, (byte) 0xd2, (byte) 0x04, (byte) 0x00, (byte) 0xe8, (byte) 0x9c, (byte) 0x80, (byte) 0xc9, (byte) 0x04, (byte) 0x00, (byte) 0xdc, (byte) 0x5c, (byte) 0x80, (byte) 0xc3, (byte) 0x04, (byte) 0x00, (byte) 0xc4, (byte) 0x9c, (byte) 0x80, (byte) 0xc8, (byte) 0x04, (byte) 0x00, (byte) 0x8c, (byte) 0x5c, (byte) 0x80, (byte) 0xcb, (byte) 0x04, (byte) 0x00, (byte) 0x64, (byte) 0x5c, (byte) 0x80, (byte) 0xcf, (byte) 0x04, (byte) 0x00, (byte) 0x40, (byte) 0x5c, (byte) 0x80, (byte) 0xea, (byte) 0x04, (byte) 0x00, (byte) 0xf8, (byte) 0x5b, (byte) 0x80, (byte) 0x28, (byte) 0x05, (byte) 0x00, (byte) 0xa8, (byte) 0x5b, (byte) 0x80, (byte) 0x3c, (byte) 0x05, (byte) 0x00, (byte) 0x80, (byte) 0x5b, (byte) 0x80, (byte) 0x48, (byte) 0x05, (byte) 0x00, (byte) 0x9c, (byte) 0x5b, (byte) 0x80, (byte) 0x32, (byte) 0x05, (byte) 0x00, (byte) 0xec, (byte) 0x9b, (byte) 0x80, (byte) 0x2b, (byte) 0x05, (byte) 0x00, (byte) 0xe0, (byte) 0x5b, (byte) 0x80, (byte) 0x68, (byte) 0x04, (byte) 0x00, (byte) 0xb0, (byte) 0x57, (byte) 0x80, (byte) 0x4a, (byte) 0x03, (byte) 0x00, (byte) 0xf4, (byte) 0x9b, (byte) 0x80, (byte) 0xfd, (byte) 0x02, (byte) 0x00, (byte) 0xd0, (byte) 0x9b, (byte) 0x80, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x14, (byte) 0x5c, (byte) 0x80, (byte) 0x42, (byte) 0x03, (byte) 0x00, (byte) 0x58, (byte) 0x5b, (byte) 0x80, (byte) 0xa0, (byte) 0x03, (byte) 0x00, (byte) 0xfc, (byte) 0x9c, (byte) 0x80, (byte) 0x23, (byte) 0x04, (byte) 0x00, (byte) 0x78, (byte) 0x5d, (byte) 0x80, (byte) 0xca, (byte) 0x04, (byte) 0x00, (byte) 0x94, (byte) 0x9d, (byte) 0x80, (byte) 0xf8, (byte) 0x04, (byte) 0x00, (byte) 0x6c, (byte) 0x9d, (byte) 0x80, (byte) 0x78, (byte) 0x04, (byte) 0x00, (byte) 0xd4, (byte) 0x9d, (byte) 0x80, (byte) 0x72, (byte) 0x04, (byte) 0x00, (byte) 0x5c, (byte) 0x5c, (byte) 0x80, (byte) 0xc6, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x9d, (byte) 0x80, (byte) 0x86, (byte) 0x04, (byte) 0x00, (byte) 0x08, (byte) 0x9d, (byte) 0x80, (byte) 0x58, (byte) 0x04, (byte) 0x00, (byte) 0xe0, (byte) 0x9d, (byte) 0x80, (byte) 0x31, (byte) 0x04, (byte) 0x00, (byte) 0x38, (byte) 0x9e, (byte) 0x80, (byte) 0x33, (byte) 0x04, (byte) 0x00, (byte) 0xe8, (byte) 0x5b, (byte) 0x80, (byte) 0xb7, (byte) 0x04, (byte) 0x00, (byte) 0xcc, (byte) 0x5a, (byte) 0x80, (byte) 0xa4, (byte) 0x04, (byte) 0x00, (byte) 0x18, (byte) 0x5a, (byte) 0x80, (byte) 0xdc, (byte) 0x04, (byte) 0x00, (byte) 0xfc, (byte) 0x5a, (byte) 0x80, (byte) 0xe1, (byte) 0x04, (byte) 0x00, (byte) 0x90, (byte) 0x5a, (byte) 0x80, (byte) 0x8b, (byte) 0x04, (byte) 0x00, (byte) 0xd0, (byte) 0x59, (byte) 0x80, (byte) 0x18, (byte) 0x04, (byte) 0x00, (byte) 0x84, (byte) 0x59, (byte) 0x80, (byte) 0xc2, (byte) 0x03, (byte) 0x00, (byte) 0x24, (byte) 0x59, (byte) 0x80, (byte) 0x5b, (byte) 0x03, (byte) 0x00, (byte) 0x9c, (byte) 0x58, (byte) 0x80, (byte) 0x5b, (byte) 0x03, (byte) 0x00, (byte) 0x70, (byte) 0x58, (byte) 0x80, (byte) 0x7f, (byte) 0x03, (byte) 0x00, (byte) 0x40, (byte) 0x58, (byte) 0x80, (byte) 0xe3, (byte) 0x03, (byte) 0x00, (byte) 0x9c, (byte) 0x58, (byte) 0x80, (byte) 0xae, (byte) 0x04, (byte) 0x00, (byte) 0xbc, (byte) 0x58, (byte) 0x80, (byte) 0x5d, (byte) 0x05, (byte) 0x00, (byte) 0xc4, (byte) 0x18, (byte) 0x80, (byte) 0xa5, (byte) 0x05, (byte) 0x00, (byte) 0x20, (byte) 0x58, (byte) 0x80, (byte) 0x14, (byte) 0x05, (byte) 0x00, (byte) 0xbc, (byte) 0x17, (byte) 0x80, (byte) 0x88, (byte) 0x04, (byte) 0x00, (byte) 0xfc, (byte) 0x57, (byte) 0x80, (byte) 0x81, (byte) 0x13, (byte) 0x01, (byte) 0x00, (byte) 0x4f, (byte) 0x6a, (byte) 0x30, (byte) 0x08, (byte) 0x4b, (byte) 0x0b, (byte) 0x58, (byte) 0x51, (byte) 0x14, (byte) 0x03, (byte) 0x96, (byte) 0x80, (byte) 0x5a, (byte) 0x00, (byte) 0xcd, (byte) 0xa6, (byte) 0x14, (byte) 0x7e, (byte) 0x1a, (byte) 0x00, (byte) 0x00, (byte) 0xf3, (byte) 0xc8, (byte) 0x6b};
    static byte[] patchInfo_libre1 = new byte[]{(byte) 0x9d, (byte) 0x08, (byte) 0x30, (byte) 0x08, (byte) 0x1a, (byte) 0x24};

    static byte[] data_librepro1 = new byte[]{(byte)0x85, (byte)0xe4, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x4a, (byte)0x46, (byte)0x47, (byte)0x56, (byte)0x31, (byte)0x35, (byte)0x31, (byte)0x2d, (byte)0x54, (byte)0x30, (byte)0x36, (byte)0x38, (byte)0x32, (byte)0x4a, (byte)0x49, (byte)0xc3, (byte)0x2c, (byte)0x2d, (byte)0x10, (byte)0x00, (byte)0x72, (byte)0x0c, (byte)0xc0, (byte)0x4e, (byte)0x14, (byte)0x03, (byte)0x96, (byte)0x80, (byte)0x5a, (byte)0x00, (byte)0xed, (byte)0xa6, (byte)0x06, (byte)0x71, (byte)0x1a, (byte)0xc8, (byte)0x04, (byte)0x7e, (byte)0x49, (byte)0x6d, (byte)0x62, (byte)0x03, (byte)0xcb, (byte)0x1b, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xfe, (byte)0x66, (byte)0xfd, (byte)0x39, (byte)0x0d, (byte)0x00, (byte)0xdd, (byte)0x03, (byte)0x22, (byte)0x04, (byte)0xc8, (byte)0xe8, (byte)0xd9, (byte)0x01, (byte)0x10, (byte)0x04, (byte)0xc8, (byte)0xe4, (byte)0x19, (byte)0x02, (byte)0x00, (byte)0x04, (byte)0xc8, (byte)0xdc, (byte)0xd9, (byte)0x01, (byte)0xf3, (byte)0x03, (byte)0xc8, (byte)0xd4, (byte)0xd9, (byte)0x01, (byte)0xe7, (byte)0x03, (byte)0xc8, (byte)0xcc, (byte)0xd9, (byte)0x01, (byte)0xde, (byte)0x03, (byte)0xc8, (byte)0xc4, (byte)0xd9, (byte)0x01, (byte)0xdf, (byte)0x03, (byte)0xc8, (byte)0xb8, (byte)0x19, (byte)0x02, (byte)0xeb, (byte)0x03, (byte)0xc8, (byte)0xac, (byte)0x19, (byte)0x02, (byte)0xe1, (byte)0x03, (byte)0xc8, (byte)0xa0, (byte)0xd9, (byte)0x01, (byte)0xd6, (byte)0x03, (byte)0xc8, (byte)0x90, (byte)0x19, (byte)0x02, (byte)0xc8, (byte)0x03, (byte)0xc8, (byte)0x40, (byte)0xda, (byte)0x01, (byte)0xc7, (byte)0x03, (byte)0xc8, (byte)0xa8, (byte)0xda, (byte)0x01, (byte)0xcd, (byte)0x03, (byte)0xc8, (byte)0xe8, (byte)0xda, (byte)0x01, (byte)0xfb, (byte)0x03, (byte)0xc8, (byte)0x20, (byte)0xda, (byte)0x01, (byte)0xfe, (byte)0x03, (byte)0xc8, (byte)0x18, (byte)0xda, (byte)0x01, (byte)0x08, (byte)0x04, (byte)0xc8, (byte)0xec, (byte)0x19, (byte)0x02, (byte)0x3a, (byte)0x04, (byte)0xc8, (byte)0xac, (byte)0xd9, (byte)0x01, (byte)0xec, (byte)0x02, (byte)0xc8, (byte)0x80, (byte)0xd9, (byte)0x01, (byte)0x45, (byte)0x01, (byte)0xc8, (byte)0x98, (byte)0x9a, (byte)0x01, (byte)0xb7, (byte)0x00, (byte)0xc8, (byte)0x7c, (byte)0xda, (byte)0x01, (byte)0x97, (byte)0x00, (byte)0xc8, (byte)0x64, (byte)0x19, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0xc0, (byte)0x02, (byte)0x19, (byte)0x02, (byte)0x98, (byte)0x00, (byte)0xc0, (byte)0x06, (byte)0xda, (byte)0x01, (byte)0xa4, (byte)0x00, (byte)0xc8, (byte)0x2c, (byte)0x9b, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0xc0, (byte)0x12, (byte)0xdb, (byte)0x01, (byte)0xaa, (byte)0x00, (byte)0xc8, (byte)0xd4, (byte)0xda, (byte)0x01, (byte)0xaf, (byte)0x00, (byte)0xc8, (byte)0x10, (byte)0x9b, (byte)0x01, (byte)0x24, (byte)0x01, (byte)0xc8, (byte)0x98, (byte)0x9b, (byte)0x01, (byte)0x06, (byte)0x02, (byte)0xc8, (byte)0x84, (byte)0xda, (byte)0x01, (byte)0x6d, (byte)0x01, (byte)0xc8, (byte)0xac, (byte)0x19, (byte)0x02, (byte)0xb8, (byte)0x01, (byte)0xc8, (byte)0xfc, (byte)0x18, (byte)0x02, (byte)0xed, (byte)0x02, (byte)0xc8, (byte)0xa0, (byte)0x18, (byte)0x02, (byte)0xda, (byte)0x03, (byte)0xc8, (byte)0x48, (byte)0x18, (byte)0x02, (byte)0xce, (byte)0x03, (byte)0xc8, (byte)0x40, (byte)0x18, (byte)0x02, (byte)0x3a, (byte)0x04, (byte)0xc8, (byte)0xd8, (byte)0x18, (byte)0x02, (byte)0x63, (byte)0x04, (byte)0xc8, (byte)0xe0, (byte)0x18, (byte)0x02, (byte)0xcb, (byte)0x02, (byte)0xc8, (byte)0xc4, (byte)0xda, (byte)0x01, (byte)0x43, (byte)0x02, (byte)0xc8, (byte)0x70, (byte)0x9b, (byte)0x01, (byte)0x06, (byte)0x02, (byte)0xc8, (byte)0xd8, (byte)0x9a, (byte)0x01, (byte)0x08, (byte)0x02, (byte)0xc8, (byte)0x34, (byte)0x9a, (byte)0x01, (byte)0x3c, (byte)0x03, (byte)0xc8, (byte)0x58, (byte)0x19, (byte)0x02, (byte)0x26, (byte)0x03, (byte)0xc8, (byte)0xbc, (byte)0x18, (byte)0x02, (byte)0xc6, (byte)0x02, (byte)0xc8, (byte)0xa0, (byte)0x18, (byte)0x02, (byte)0x79, (byte)0x02, (byte)0xc8, (byte)0xc0, (byte)0x18, (byte)0x02};
    static byte[] patchInfo_librepro = new byte[]{(byte) 0x70, (byte) 0x00, (byte) 0x10, (byte) 0x08, (byte) 0x1a, (byte) 0x24};

    static byte[] data_librepro2 = new byte[]{(byte)0x85, (byte)0xe4, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x4a, (byte)0x46, (byte)0x47, (byte)0x56, (byte)0x31, (byte)0x35, (byte)0x31, (byte)0x2d, (byte)0x54, (byte)0x30, (byte)0x36, (byte)0x38, (byte)0x32, (byte)0x4a, (byte)0x49, (byte)0xc3, (byte)0x2c, (byte)0x2d, (byte)0x10, (byte)0x00, (byte)0x72, (byte)0x0c, (byte)0xc0, (byte)0x4e, (byte)0x14, (byte)0x03, (byte)0x96, (byte)0x80, (byte)0x5a, (byte)0x00, (byte)0xed, (byte)0xa6, (byte)0x06, (byte)0x71, (byte)0x1a, (byte)0xc8, (byte)0x04, (byte)0x7e, (byte)0x49, (byte)0x6d, (byte)0x62, (byte)0x03, (byte)0xcb, (byte)0x1b, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x99, (byte)0x0d, (byte)0xf1, (byte)0x3a, (byte)0x00, (byte)0x00, (byte)0xed, (byte)0x03, (byte)0x4c, (byte)0x05, (byte)0xc8, (byte)0x3c, (byte)0xda, (byte)0x01, (byte)0x4a, (byte)0x05, (byte)0xc8, (byte)0x44, (byte)0xda, (byte)0x01, (byte)0x48, (byte)0x05, (byte)0xc8, (byte)0x34, (byte)0xda, (byte)0x01, (byte)0x46, (byte)0x05, (byte)0xc8, (byte)0x2c, (byte)0xda, (byte)0x01, (byte)0x49, (byte)0x05, (byte)0xc8, (byte)0x28, (byte)0xda, (byte)0x01, (byte)0x52, (byte)0x05, (byte)0xc8, (byte)0x34, (byte)0xda, (byte)0x01, (byte)0x4c, (byte)0x05, (byte)0xc8, (byte)0x24, (byte)0x9a, (byte)0x01, (byte)0x54, (byte)0x05, (byte)0xc8, (byte)0x20, (byte)0xda, (byte)0x01, (byte)0x53, (byte)0x05, (byte)0xc8, (byte)0x18, (byte)0xda, (byte)0x01, (byte)0x4e, (byte)0x05, (byte)0xc8, (byte)0x18, (byte)0x9a, (byte)0x01, (byte)0x51, (byte)0x05, (byte)0xc8, (byte)0x14, (byte)0xda, (byte)0x01, (byte)0x54, (byte)0x05, (byte)0xc8, (byte)0x04, (byte)0xda, (byte)0x01, (byte)0x59, (byte)0x05, (byte)0xc8, (byte)0x00, (byte)0x1a, (byte)0x02, (byte)0x54, (byte)0x05, (byte)0xc8, (byte)0xf0, (byte)0x19, (byte)0x02, (byte)0x61, (byte)0x05, (byte)0xc8, (byte)0xf4, (byte)0xd9, (byte)0x01, (byte)0x68, (byte)0x05, (byte)0xc8, (byte)0x0c, (byte)0xda, (byte)0x01, (byte)0x3a, (byte)0x04, (byte)0xc8, (byte)0xac, (byte)0xd9, (byte)0x01, (byte)0xec, (byte)0x02, (byte)0xc8, (byte)0x80, (byte)0xd9, (byte)0x01, (byte)0x45, (byte)0x01, (byte)0xc8, (byte)0x98, (byte)0x9a, (byte)0x01, (byte)0xb7, (byte)0x00, (byte)0xc8, (byte)0x7c, (byte)0xda, (byte)0x01, (byte)0x97, (byte)0x00, (byte)0xc8, (byte)0x64, (byte)0x19, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0xc0, (byte)0x02, (byte)0x19, (byte)0x02, (byte)0x98, (byte)0x00, (byte)0xc0, (byte)0x06, (byte)0xda, (byte)0x01, (byte)0xa4, (byte)0x00, (byte)0xc8, (byte)0x2c, (byte)0x9b, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0xc0, (byte)0x12, (byte)0xdb, (byte)0x01, (byte)0xaa, (byte)0x00, (byte)0xc8, (byte)0xd4, (byte)0xda, (byte)0x01, (byte)0xaf, (byte)0x00, (byte)0xc8, (byte)0x10, (byte)0x9b, (byte)0x01, (byte)0x24, (byte)0x01, (byte)0xc8, (byte)0x98, (byte)0x9b, (byte)0x01, (byte)0x06, (byte)0x02, (byte)0xc8, (byte)0x84, (byte)0xda, (byte)0x01, (byte)0x6d, (byte)0x01, (byte)0xc8, (byte)0xac, (byte)0x19, (byte)0x02, (byte)0xb8, (byte)0x01, (byte)0xc8, (byte)0xfc, (byte)0x18, (byte)0x02, (byte)0xed, (byte)0x02, (byte)0xc8, (byte)0xa0, (byte)0x18, (byte)0x02, (byte)0xda, (byte)0x03, (byte)0xc8, (byte)0x48, (byte)0x18, (byte)0x02, (byte)0xce, (byte)0x03, (byte)0xc8, (byte)0x40, (byte)0x18, (byte)0x02, (byte)0x3a, (byte)0x04, (byte)0xc8, (byte)0xd8, (byte)0x18, (byte)0x02, (byte)0x63, (byte)0x04, (byte)0xc8, (byte)0xe0, (byte)0x18, (byte)0x02, (byte)0xcb, (byte)0x02, (byte)0xc8, (byte)0xc4, (byte)0xda, (byte)0x01, (byte)0x43, (byte)0x02, (byte)0xc8, (byte)0x70, (byte)0x9b, (byte)0x01, (byte)0x06, (byte)0x02, (byte)0xc8, (byte)0xd8, (byte)0x9a, (byte)0x01, (byte)0x08, (byte)0x02, (byte)0xc8, (byte)0x34, (byte)0x9a, (byte)0x01, (byte)0x3c, (byte)0x03, (byte)0xc8, (byte)0x58, (byte)0x19, (byte)0x02, (byte)0x26, (byte)0x03, (byte)0xc8, (byte)0xbc, (byte)0x18, (byte)0x02, (byte)0xc6, (byte)0x02, (byte)0xc8, (byte)0xa0, (byte)0x18, (byte)0x02, (byte)0x79, (byte)0x02, (byte)0xc8, (byte)0xc0, (byte)0x18, (byte)0x02};
    static byte[] data_librepro3 = new byte[]{(byte)0x85, (byte)0xe4, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x4a, (byte)0x46, (byte)0x47, (byte)0x56, (byte)0x31, (byte)0x35, (byte)0x31, (byte)0x2d, (byte)0x54, (byte)0x30, (byte)0x36, (byte)0x38, (byte)0x32, (byte)0x4a, (byte)0x49, (byte)0xc3, (byte)0x2c, (byte)0x2d, (byte)0x10, (byte)0x00, (byte)0x72, (byte)0x0c, (byte)0xc0, (byte)0x4e, (byte)0x14, (byte)0x03, (byte)0x96, (byte)0x80, (byte)0x5a, (byte)0x00, (byte)0xed, (byte)0xa6, (byte)0x06, (byte)0x71, (byte)0x1a, (byte)0xc8, (byte)0x04, (byte)0x7e, (byte)0x49, (byte)0x6d, (byte)0x62, (byte)0x03, (byte)0xcb, (byte)0x1b, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x29, (byte)0x6a, (byte)0x29, (byte)0x3b, (byte)0x08, (byte)0x00, (byte)0xf1, (byte)0x03, (byte)0xf5, (byte)0x05, (byte)0xc8, (byte)0x80, (byte)0x18, (byte)0x02, (byte)0xbd, (byte)0x05, (byte)0xc8, (byte)0x2c, (byte)0xd9, (byte)0x01, (byte)0x9b, (byte)0x05, (byte)0xc8, (byte)0xcc, (byte)0x99, (byte)0x01, (byte)0x7c, (byte)0x05, (byte)0xc8, (byte)0x0c, (byte)0x9a, (byte)0x01, (byte)0x74, (byte)0x05, (byte)0xc8, (byte)0x10, (byte)0xda, (byte)0x01, (byte)0x5d, (byte)0x05, (byte)0xc8, (byte)0x74, (byte)0x9a, (byte)0x01, (byte)0x4e, (byte)0x05, (byte)0xc8, (byte)0xac, (byte)0x9a, (byte)0x01, (byte)0x44, (byte)0x05, (byte)0xc8, (byte)0xc4, (byte)0x9a, (byte)0x01, (byte)0x40, (byte)0x05, (byte)0xc8, (byte)0x18, (byte)0x97, (byte)0x02, (byte)0x62, (byte)0x05, (byte)0xc8, (byte)0x2c, (byte)0xd5, (byte)0x02, (byte)0x90, (byte)0x05, (byte)0xc8, (byte)0x98, (byte)0xd5, (byte)0x02, (byte)0xba, (byte)0x05, (byte)0xc8, (byte)0x48, (byte)0xd4, (byte)0x02, (byte)0xd2, (byte)0x05, (byte)0xc8, (byte)0x00, (byte)0x14, (byte)0x03, (byte)0xfd, (byte)0x05, (byte)0xc8, (byte)0x68, (byte)0x14, (byte)0x03, (byte)0x14, (byte)0x06, (byte)0xc8, (byte)0x68, (byte)0x56, (byte)0x02, (byte)0x06, (byte)0x06, (byte)0xc8, (byte)0xe0, (byte)0x17, (byte)0x02, (byte)0x3a, (byte)0x04, (byte)0xc8, (byte)0xac, (byte)0xd9, (byte)0x01, (byte)0xec, (byte)0x02, (byte)0xc8, (byte)0x80, (byte)0xd9, (byte)0x01, (byte)0x45, (byte)0x01, (byte)0xc8, (byte)0x98, (byte)0x9a, (byte)0x01, (byte)0xb7, (byte)0x00, (byte)0xc8, (byte)0x7c, (byte)0xda, (byte)0x01, (byte)0x97, (byte)0x00, (byte)0xc8, (byte)0x64, (byte)0x19, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0xc0, (byte)0x02, (byte)0x19, (byte)0x02, (byte)0x98, (byte)0x00, (byte)0xc0, (byte)0x06, (byte)0xda, (byte)0x01, (byte)0xa4, (byte)0x00, (byte)0xc8, (byte)0x2c, (byte)0x9b, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0xc0, (byte)0x12, (byte)0xdb, (byte)0x01, (byte)0xaa, (byte)0x00, (byte)0xc8, (byte)0xd4, (byte)0xda, (byte)0x01, (byte)0xaf, (byte)0x00, (byte)0xc8, (byte)0x10, (byte)0x9b, (byte)0x01, (byte)0x24, (byte)0x01, (byte)0xc8, (byte)0x98, (byte)0x9b, (byte)0x01, (byte)0x06, (byte)0x02, (byte)0xc8, (byte)0x84, (byte)0xda, (byte)0x01, (byte)0x6d, (byte)0x01, (byte)0xc8, (byte)0xac, (byte)0x19, (byte)0x02, (byte)0xb8, (byte)0x01, (byte)0xc8, (byte)0xfc, (byte)0x18, (byte)0x02, (byte)0xed, (byte)0x02, (byte)0xc8, (byte)0xa0, (byte)0x18, (byte)0x02, (byte)0xda, (byte)0x03, (byte)0xc8, (byte)0x48, (byte)0x18, (byte)0x02, (byte)0xce, (byte)0x03, (byte)0xc8, (byte)0x40, (byte)0x18, (byte)0x02, (byte)0x3a, (byte)0x04, (byte)0xc8, (byte)0xd8, (byte)0x18, (byte)0x02, (byte)0x63, (byte)0x04, (byte)0xc8, (byte)0xe0, (byte)0x18, (byte)0x02, (byte)0xcb, (byte)0x02, (byte)0xc8, (byte)0xc4, (byte)0xda, (byte)0x01, (byte)0x43, (byte)0x02, (byte)0xc8, (byte)0x70, (byte)0x9b, (byte)0x01, (byte)0x06, (byte)0x02, (byte)0xc8, (byte)0xd8, (byte)0x9a, (byte)0x01, (byte)0x08, (byte)0x02, (byte)0xc8, (byte)0x34, (byte)0x9a, (byte)0x01, (byte)0x3c, (byte)0x03, (byte)0xc8, (byte)0x58, (byte)0x19, (byte)0x02, (byte)0x26, (byte)0x03, (byte)0xc8, (byte)0xbc, (byte)0x18, (byte)0x02, (byte)0xc6, (byte)0x02, (byte)0xc8, (byte)0xa0, (byte)0x18, (byte)0x02, (byte)0x79, (byte)0x02, (byte)0xc8, (byte)0xc0, (byte)0x18, (byte)0x02};

    @Test
    public void testChecksumLibre() {
        // Test libre1

        // Test the good casses
        assertThat(true).isEqualTo(LibreUtils.verify(data_libre1, patchInfo_libre1));
        assertThat(true).isEqualTo(LibreUtils.verify(data_librepro1, patchInfo_librepro));
        assertThat(true).isEqualTo(LibreUtils.verify(data_librepro2, patchInfo_librepro));
        assertThat(true).isEqualTo(LibreUtils.verify(data_librepro3, patchInfo_librepro));

        // Testing bad cases of libre1
        byte[] temp_data = Arrays.copyOf(data_libre1, data_libre1.length);
        temp_data[5] = 55;
        assertThat(false).isEqualTo(LibreUtils.verify(temp_data, patchInfo_libre1));

        temp_data = Arrays.copyOf(data_libre1, data_libre1.length);
        temp_data[100] = 100;
        assertThat(false).isEqualTo(LibreUtils.verify(temp_data, patchInfo_libre1));

        temp_data = Arrays.copyOf(data_libre1, data_libre1.length);
        temp_data[330] = 55;
        assertThat(false).isEqualTo(LibreUtils.verify(temp_data, patchInfo_libre1));

        // Testing bad cases of libre pro
        temp_data = Arrays.copyOf(data_librepro1, data_librepro1.length);
        temp_data[5] = 55;
        assertThat(false).isEqualTo(LibreUtils.verify(temp_data, patchInfo_librepro));

        temp_data = Arrays.copyOf(data_librepro1, data_librepro1.length);
        temp_data[50] = 100;
        assertThat(false).isEqualTo(LibreUtils.verify(temp_data, patchInfo_librepro));

        temp_data = Arrays.copyOf(data_librepro1, data_librepro1.length);
        temp_data[100] = 55;
        assertThat(false).isEqualTo(LibreUtils.verify(temp_data, patchInfo_librepro));
    }

    @Test
    public void testParseDataLibre1() {

        int[] trend_bg_vals = new int[16];
        for (int i = 0; i < 16; i++) {
            trend_bg_vals[i] = i + 100;
        }

        int[] history_bg_vals = new int[32];
        for (int i = 0; i < 16; i++) {
            history_bg_vals[i] = i + 100;
        }

        ReadingData readingData = NFCReaderX.parseData(data_libre1, patchInfo_libre1, 5000000l, trend_bg_vals, history_bg_vals);
        String json = new GsonBuilder().create().toJson(readingData);
        System.out.println(json);

        String expected = "{\n" +
                "  \"trend\": [\n" +
                "    {\n" +
                "      \"realDate\": 4100000,\n" +
                "      \"sensorTime\": 4978,\n" +
                "      \"glucoseLevel\": 100,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1235,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1856,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 4160000,\n" +
                "      \"sensorTime\": 4979,\n" +
                "      \"glucoseLevel\": 101,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1208,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1856,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 4220000,\n" +
                "      \"sensorTime\": 4980,\n" +
                "      \"glucoseLevel\": 102,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1217,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1860,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 4280000,\n" +
                "      \"sensorTime\": 4981,\n" +
                "      \"glucoseLevel\": 103,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1230,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1856,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 4340000,\n" +
                "      \"sensorTime\": 4982,\n" +
                "      \"glucoseLevel\": 104,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1234,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1850,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 4400000,\n" +
                "      \"sensorTime\": 4983,\n" +
                "      \"glucoseLevel\": 105,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1225,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1847,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 4460000,\n" +
                "      \"sensorTime\": 4984,\n" +
                "      \"glucoseLevel\": 106,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1219,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1841,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 4520000,\n" +
                "      \"sensorTime\": 4985,\n" +
                "      \"glucoseLevel\": 107,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1224,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1827,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 4580000,\n" +
                "      \"sensorTime\": 4986,\n" +
                "      \"glucoseLevel\": 108,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1227,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1817,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 4640000,\n" +
                "      \"sensorTime\": 4987,\n" +
                "      \"glucoseLevel\": 109,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1231,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1808,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 4700000,\n" +
                "      \"sensorTime\": 4988,\n" +
                "      \"glucoseLevel\": 110,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1258,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1790,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 4760000,\n" +
                "      \"sensorTime\": 4989,\n" +
                "      \"glucoseLevel\": 111,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1320,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1770,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 4820000,\n" +
                "      \"sensorTime\": 4990,\n" +
                "      \"glucoseLevel\": 112,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1340,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1760,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 4880000,\n" +
                "      \"sensorTime\": 4991,\n" +
                "      \"glucoseLevel\": 113,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1352,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1767,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 4940000,\n" +
                "      \"sensorTime\": 4992,\n" +
                "      \"glucoseLevel\": 114,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1330,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1787,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 5000000,\n" +
                "      \"sensorTime\": 4993,\n" +
                "      \"glucoseLevel\": 115,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1323,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1784,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"history\": [\n" +
                "    {\n" +
                "      \"realDate\": -23680000,\n" +
                "      \"sensorTime\": 4515,\n" +
                "      \"glucoseLevel\": 100,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1158,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1858,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -22780000,\n" +
                "      \"sensorTime\": 4530,\n" +
                "      \"glucoseLevel\": 101,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1112,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1912,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -21880000,\n" +
                "      \"sensorTime\": 4545,\n" +
                "      \"glucoseLevel\": 102,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1073,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1934,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -20980000,\n" +
                "      \"sensorTime\": 4560,\n" +
                "      \"glucoseLevel\": 103,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1075,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1786,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -20080000,\n" +
                "      \"sensorTime\": 4575,\n" +
                "      \"glucoseLevel\": 104,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1207,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1715,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -19180000,\n" +
                "      \"sensorTime\": 4590,\n" +
                "      \"glucoseLevel\": 105,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1188,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1670,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -18280000,\n" +
                "      \"sensorTime\": 4605,\n" +
                "      \"glucoseLevel\": 106,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1244,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1727,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -17380000,\n" +
                "      \"sensorTime\": 4620,\n" +
                "      \"glucoseLevel\": 107,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1249,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1700,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -16480000,\n" +
                "      \"sensorTime\": 4635,\n" +
                "      \"glucoseLevel\": 108,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1163,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1652,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -15580000,\n" +
                "      \"sensorTime\": 4650,\n" +
                "      \"glucoseLevel\": 109,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1048,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1633,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -14680000,\n" +
                "      \"sensorTime\": 4665,\n" +
                "      \"glucoseLevel\": 110,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 962,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1609,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -13780000,\n" +
                "      \"sensorTime\": 4680,\n" +
                "      \"glucoseLevel\": 111,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 859,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1575,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -12880000,\n" +
                "      \"sensorTime\": 4695,\n" +
                "      \"glucoseLevel\": 112,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 859,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1564,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -11980000,\n" +
                "      \"sensorTime\": 4710,\n" +
                "      \"glucoseLevel\": 113,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 895,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1552,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -11080000,\n" +
                "      \"sensorTime\": 4725,\n" +
                "      \"glucoseLevel\": 114,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 995,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1575,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -10180000,\n" +
                "      \"sensorTime\": 4740,\n" +
                "      \"glucoseLevel\": 115,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1198,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1583,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -9280000,\n" +
                "      \"sensorTime\": 4755,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1373,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1585,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -8380000,\n" +
                "      \"sensorTime\": 4770,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1445,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1544,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -7480000,\n" +
                "      \"sensorTime\": 4785,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1300,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1519,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -6580000,\n" +
                "      \"sensorTime\": 4800,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1160,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1535,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -5680000,\n" +
                "      \"sensorTime\": 4815,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1128,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1516,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -4780000,\n" +
                "      \"sensorTime\": 4830,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 842,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1789,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -3880000,\n" +
                "      \"sensorTime\": 4845,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 765,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1780,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -2980000,\n" +
                "      \"sensorTime\": 4860,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 768,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1797,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -2080000,\n" +
                "      \"sensorTime\": 4875,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 834,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1750,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -1180000,\n" +
                "      \"sensorTime\": 4890,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 928,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1855,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": -280000,\n" +
                "      \"sensorTime\": 4905,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1059,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1886,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 620000,\n" +
                "      \"sensorTime\": 4920,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1226,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1893,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 1520000,\n" +
                "      \"sensorTime\": 4935,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1272,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1883,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 2420000,\n" +
                "      \"sensorTime\": 4950,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1144,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1909,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 3320000,\n" +
                "      \"sensorTime\": 4965,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1138,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1815,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"realDate\": 4220000,\n" +
                "      \"sensorTime\": 4980,\n" +
                "      \"glucoseLevel\": 0,\n" +
                "      \"glucoseLevelSmoothed\": -1,\n" +
                "      \"glucoseLevelRaw\": 1222,\n" +
                "      \"glucoseLevelRawSmoothed\": 0,\n" +
                "      \"flags\": 0,\n" +
                "      \"temp\": 1856,\n" +
                "      \"source\": \"FRAM\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"raw_data\": [\n" +
                "    69,\n" +
                "    -103,\n" +
                "    8,\n" +
                "    27,\n" +
                "    3,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    0,\n" +
                "    70,\n" +
                "    36,\n" +
                "    0,\n" +
                "    12,\n" +
                "    -45,\n" +
                "    4,\n" +
                "    0,\n" +
                "    0,\n" +
                "    -99,\n" +
                "    -128,\n" +
                "    -72,\n" +
                "    4,\n" +
                "    0,\n" +
                "    0,\n" +
                "    93,\n" +
                "    -128,\n" +
                "    -63,\n" +
                "    4,\n" +
                "    0,\n" +
                "    16,\n" +
                "    -99,\n" +
                "    -128,\n" +
                "    -50,\n" +
                "    4,\n" +
                "    0,\n" +
                "    0,\n" +
                "    -99,\n" +
                "    -128,\n" +
                "    -46,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -24,\n" +
                "    -100,\n" +
                "    -128,\n" +
                "    -55,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -36,\n" +
                "    92,\n" +
                "    -128,\n" +
                "    -61,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -60,\n" +
                "    -100,\n" +
                "    -128,\n" +
                "    -56,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -116,\n" +
                "    92,\n" +
                "    -128,\n" +
                "    -53,\n" +
                "    4,\n" +
                "    0,\n" +
                "    100,\n" +
                "    92,\n" +
                "    -128,\n" +
                "    -49,\n" +
                "    4,\n" +
                "    0,\n" +
                "    64,\n" +
                "    92,\n" +
                "    -128,\n" +
                "    -22,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -8,\n" +
                "    91,\n" +
                "    -128,\n" +
                "    40,\n" +
                "    5,\n" +
                "    0,\n" +
                "    -88,\n" +
                "    91,\n" +
                "    -128,\n" +
                "    60,\n" +
                "    5,\n" +
                "    0,\n" +
                "    -128,\n" +
                "    91,\n" +
                "    -128,\n" +
                "    72,\n" +
                "    5,\n" +
                "    0,\n" +
                "    -100,\n" +
                "    91,\n" +
                "    -128,\n" +
                "    50,\n" +
                "    5,\n" +
                "    0,\n" +
                "    -20,\n" +
                "    -101,\n" +
                "    -128,\n" +
                "    43,\n" +
                "    5,\n" +
                "    0,\n" +
                "    -32,\n" +
                "    91,\n" +
                "    -128,\n" +
                "    104,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -80,\n" +
                "    87,\n" +
                "    -128,\n" +
                "    74,\n" +
                "    3,\n" +
                "    0,\n" +
                "    -12,\n" +
                "    -101,\n" +
                "    -128,\n" +
                "    -3,\n" +
                "    2,\n" +
                "    0,\n" +
                "    -48,\n" +
                "    -101,\n" +
                "    -128,\n" +
                "    0,\n" +
                "    3,\n" +
                "    0,\n" +
                "    20,\n" +
                "    92,\n" +
                "    -128,\n" +
                "    66,\n" +
                "    3,\n" +
                "    0,\n" +
                "    88,\n" +
                "    91,\n" +
                "    -128,\n" +
                "    -96,\n" +
                "    3,\n" +
                "    0,\n" +
                "    -4,\n" +
                "    -100,\n" +
                "    -128,\n" +
                "    35,\n" +
                "    4,\n" +
                "    0,\n" +
                "    120,\n" +
                "    93,\n" +
                "    -128,\n" +
                "    -54,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -108,\n" +
                "    -99,\n" +
                "    -128,\n" +
                "    -8,\n" +
                "    4,\n" +
                "    0,\n" +
                "    108,\n" +
                "    -99,\n" +
                "    -128,\n" +
                "    120,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -44,\n" +
                "    -99,\n" +
                "    -128,\n" +
                "    114,\n" +
                "    4,\n" +
                "    0,\n" +
                "    92,\n" +
                "    92,\n" +
                "    -128,\n" +
                "    -58,\n" +
                "    4,\n" +
                "    0,\n" +
                "    0,\n" +
                "    -99,\n" +
                "    -128,\n" +
                "    -122,\n" +
                "    4,\n" +
                "    0,\n" +
                "    8,\n" +
                "    -99,\n" +
                "    -128,\n" +
                "    88,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -32,\n" +
                "    -99,\n" +
                "    -128,\n" +
                "    49,\n" +
                "    4,\n" +
                "    0,\n" +
                "    56,\n" +
                "    -98,\n" +
                "    -128,\n" +
                "    51,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -24,\n" +
                "    91,\n" +
                "    -128,\n" +
                "    -73,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -52,\n" +
                "    90,\n" +
                "    -128,\n" +
                "    -92,\n" +
                "    4,\n" +
                "    0,\n" +
                "    24,\n" +
                "    90,\n" +
                "    -128,\n" +
                "    -36,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -4,\n" +
                "    90,\n" +
                "    -128,\n" +
                "    -31,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -112,\n" +
                "    90,\n" +
                "    -128,\n" +
                "    -117,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -48,\n" +
                "    89,\n" +
                "    -128,\n" +
                "    24,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -124,\n" +
                "    89,\n" +
                "    -128,\n" +
                "    -62,\n" +
                "    3,\n" +
                "    0,\n" +
                "    36,\n" +
                "    89,\n" +
                "    -128,\n" +
                "    91,\n" +
                "    3,\n" +
                "    0,\n" +
                "    -100,\n" +
                "    88,\n" +
                "    -128,\n" +
                "    91,\n" +
                "    3,\n" +
                "    0,\n" +
                "    112,\n" +
                "    88,\n" +
                "    -128,\n" +
                "    127,\n" +
                "    3,\n" +
                "    0,\n" +
                "    64,\n" +
                "    88,\n" +
                "    -128,\n" +
                "    -29,\n" +
                "    3,\n" +
                "    0,\n" +
                "    -100,\n" +
                "    88,\n" +
                "    -128,\n" +
                "    -82,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -68,\n" +
                "    88,\n" +
                "    -128,\n" +
                "    93,\n" +
                "    5,\n" +
                "    0,\n" +
                "    -60,\n" +
                "    24,\n" +
                "    -128,\n" +
                "    -91,\n" +
                "    5,\n" +
                "    0,\n" +
                "    32,\n" +
                "    88,\n" +
                "    -128,\n" +
                "    20,\n" +
                "    5,\n" +
                "    0,\n" +
                "    -68,\n" +
                "    23,\n" +
                "    -128,\n" +
                "    -120,\n" +
                "    4,\n" +
                "    0,\n" +
                "    -4,\n" +
                "    87,\n" +
                "    -128,\n" +
                "    -127,\n" +
                "    19,\n" +
                "    1,\n" +
                "    0,\n" +
                "    79,\n" +
                "    106,\n" +
                "    48,\n" +
                "    8,\n" +
                "    75,\n" +
                "    11,\n" +
                "    88,\n" +
                "    81,\n" +
                "    20,\n" +
                "    3,\n" +
                "    -106,\n" +
                "    -128,\n" +
                "    90,\n" +
                "    0,\n" +
                "    -51,\n" +
                "    -90,\n" +
                "    20,\n" +
                "    126,\n" +
                "    26,\n" +
                "    0,\n" +
                "    0,\n" +
                "    -13,\n" +
                "    -56,\n" +
                "    107\n" +
                "  ]\n" +
                "}";

        assertThat(json).isEqualTo(expected.replaceAll("\\s+", ""));
    }



    @Test
    public void testParseDataLibrePro() {

        int[] trend_bg_vals = new int[16];
        for (int i = 0; i < 16; i++) {
            trend_bg_vals[i] = i + 100;
        }

        int[] history_bg_vals = new int[32];
        for (int i = 0; i < 16; i++) {
            history_bg_vals[i] = i + 100;
        }

        ReadingData readingData = NFCReaderX.parseData(data_librepro1, patchInfo_librepro, 5000000l, trend_bg_vals, history_bg_vals);
        String json = new GsonBuilder().create().toJson(readingData);
        System.out.println(json);

        String expected = "{\n" +
                "    \"trend\": [\n" +
                "        {\n" +
                "            \"realDate\": 5000000,\n" +
                "            \"sensorTime\": 14845,\n" +
                "            \"glucoseLevel\": -1,\n" +
                "            \"glucoseLevelSmoothed\": -1,\n" +
                "            \"glucoseLevelRaw\": 973,\n" +
                "            \"glucoseLevelRawSmoothed\": 0,\n" +
                "            \"flags\": 0,\n" +
                "            \"temp\": 0,\n" +
                "            \"source\": \"FRAM\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"history\": [],\n" +
                "    \"raw_data\": [\n" +
                "        -123,\n" +
                "        -28,\n" +
                "        0,\n" +
                "        0,\n" +
                "        3,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        74,\n" +
                "        70,\n" +
                "        71,\n" +
                "        86,\n" +
                "        49,\n" +
                "        53,\n" +
                "        49,\n" +
                "        45,\n" +
                "        84,\n" +
                "        48,\n" +
                "        54,\n" +
                "        56,\n" +
                "        50,\n" +
                "        74,\n" +
                "        73,\n" +
                "        -61,\n" +
                "        44,\n" +
                "        45,\n" +
                "        16,\n" +
                "        0,\n" +
                "        114,\n" +
                "        12,\n" +
                "        -64,\n" +
                "        78,\n" +
                "        20,\n" +
                "        3,\n" +
                "        -106,\n" +
                "        -128,\n" +
                "        90,\n" +
                "        0,\n" +
                "        -19,\n" +
                "        -90,\n" +
                "        6,\n" +
                "        113,\n" +
                "        26,\n" +
                "        -56,\n" +
                "        4,\n" +
                "        126,\n" +
                "        73,\n" +
                "        109,\n" +
                "        98,\n" +
                "        3,\n" +
                "        -53,\n" +
                "        27,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        -2,\n" +
                "        102,\n" +
                "        -3,\n" +
                "        57,\n" +
                "        13,\n" +
                "        0,\n" +
                "        -35,\n" +
                "        3,\n" +
                "        34,\n" +
                "        4,\n" +
                "        -56,\n" +
                "        -24,\n" +
                "        -39,\n" +
                "        1,\n" +
                "        16,\n" +
                "        4,\n" +
                "        -56,\n" +
                "        -28,\n" +
                "        25,\n" +
                "        2,\n" +
                "        0,\n" +
                "        4,\n" +
                "        -56,\n" +
                "        -36,\n" +
                "        -39,\n" +
                "        1,\n" +
                "        -13,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        -44,\n" +
                "        -39,\n" +
                "        1,\n" +
                "        -25,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        -52,\n" +
                "        -39,\n" +
                "        1,\n" +
                "        -34,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        -60,\n" +
                "        -39,\n" +
                "        1,\n" +
                "        -33,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        -72,\n" +
                "        25,\n" +
                "        2,\n" +
                "        -21,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        -84,\n" +
                "        25,\n" +
                "        2,\n" +
                "        -31,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        -96,\n" +
                "        -39,\n" +
                "        1,\n" +
                "        -42,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        -112,\n" +
                "        25,\n" +
                "        2,\n" +
                "        -56,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        64,\n" +
                "        -38,\n" +
                "        1,\n" +
                "        -57,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        -88,\n" +
                "        -38,\n" +
                "        1,\n" +
                "        -51,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        -24,\n" +
                "        -38,\n" +
                "        1,\n" +
                "        -5,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        32,\n" +
                "        -38,\n" +
                "        1,\n" +
                "        -2,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        24,\n" +
                "        -38,\n" +
                "        1,\n" +
                "        8,\n" +
                "        4,\n" +
                "        -56,\n" +
                "        -20,\n" +
                "        25,\n" +
                "        2,\n" +
                "        58,\n" +
                "        4,\n" +
                "        -56,\n" +
                "        -84,\n" +
                "        -39,\n" +
                "        1,\n" +
                "        -20,\n" +
                "        2,\n" +
                "        -56,\n" +
                "        -128,\n" +
                "        -39,\n" +
                "        1,\n" +
                "        69,\n" +
                "        1,\n" +
                "        -56,\n" +
                "        -104,\n" +
                "        -102,\n" +
                "        1,\n" +
                "        -73,\n" +
                "        0,\n" +
                "        -56,\n" +
                "        124,\n" +
                "        -38,\n" +
                "        1,\n" +
                "        -105,\n" +
                "        0,\n" +
                "        -56,\n" +
                "        100,\n" +
                "        25,\n" +
                "        2,\n" +
                "        0,\n" +
                "        0,\n" +
                "        -64,\n" +
                "        2,\n" +
                "        25,\n" +
                "        2,\n" +
                "        -104,\n" +
                "        0,\n" +
                "        -64,\n" +
                "        6,\n" +
                "        -38,\n" +
                "        1,\n" +
                "        -92,\n" +
                "        0,\n" +
                "        -56,\n" +
                "        44,\n" +
                "        -101,\n" +
                "        1,\n" +
                "        0,\n" +
                "        0,\n" +
                "        -64,\n" +
                "        18,\n" +
                "        -37,\n" +
                "        1,\n" +
                "        -86,\n" +
                "        0,\n" +
                "        -56,\n" +
                "        -44,\n" +
                "        -38,\n" +
                "        1,\n" +
                "        -81,\n" +
                "        0,\n" +
                "        -56,\n" +
                "        16,\n" +
                "        -101,\n" +
                "        1,\n" +
                "        36,\n" +
                "        1,\n" +
                "        -56,\n" +
                "        -104,\n" +
                "        -101,\n" +
                "        1,\n" +
                "        6,\n" +
                "        2,\n" +
                "        -56,\n" +
                "        -124,\n" +
                "        -38,\n" +
                "        1,\n" +
                "        109,\n" +
                "        1,\n" +
                "        -56,\n" +
                "        -84,\n" +
                "        25,\n" +
                "        2,\n" +
                "        -72,\n" +
                "        1,\n" +
                "        -56,\n" +
                "        -4,\n" +
                "        24,\n" +
                "        2,\n" +
                "        -19,\n" +
                "        2,\n" +
                "        -56,\n" +
                "        -96,\n" +
                "        24,\n" +
                "        2,\n" +
                "        -38,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        72,\n" +
                "        24,\n" +
                "        2,\n" +
                "        -50,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        64,\n" +
                "        24,\n" +
                "        2,\n" +
                "        58,\n" +
                "        4,\n" +
                "        -56,\n" +
                "        -40,\n" +
                "        24,\n" +
                "        2,\n" +
                "        99,\n" +
                "        4,\n" +
                "        -56,\n" +
                "        -32,\n" +
                "        24,\n" +
                "        2,\n" +
                "        -53,\n" +
                "        2,\n" +
                "        -56,\n" +
                "        -60,\n" +
                "        -38,\n" +
                "        1,\n" +
                "        67,\n" +
                "        2,\n" +
                "        -56,\n" +
                "        112,\n" +
                "        -101,\n" +
                "        1,\n" +
                "        6,\n" +
                "        2,\n" +
                "        -56,\n" +
                "        -40,\n" +
                "        -102,\n" +
                "        1,\n" +
                "        8,\n" +
                "        2,\n" +
                "        -56,\n" +
                "        52,\n" +
                "        -102,\n" +
                "        1,\n" +
                "        60,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        88,\n" +
                "        25,\n" +
                "        2,\n" +
                "        38,\n" +
                "        3,\n" +
                "        -56,\n" +
                "        -68,\n" +
                "        24,\n" +
                "        2,\n" +
                "        -58,\n" +
                "        2,\n" +
                "        -56,\n" +
                "        -96,\n" +
                "        24,\n" +
                "        2,\n" +
                "        121,\n" +
                "        2,\n" +
                "        -56,\n" +
                "        -64,\n" +
                "        24,\n" +
                "        2\n" +
                "    ]\n" +
                "}";

        assertThat(json).isEqualTo(expected.replaceAll("\\s+", ""));
    }
}