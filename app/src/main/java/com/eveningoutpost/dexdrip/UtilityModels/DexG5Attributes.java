package com.eveningoutpost.dexdrip.UtilityModels;

/**
 * Created by jcostik1 on 3/15/16.
 */
public class DexG5Attributes {
}

//G5 Details thanks to Nathan Racklyeft
/*enum TxRxServiceUUID: String {
        case DeviceInfo = "180A"

        //G5
        case Advertisement = "FEBC"
        case CGMService = "F8083532-849E-531C-C594-30F1F86A4EA5"
        case ServiceB = "F8084532-849E-531C-C594-30F1F86A4EA5"

        //G4
        case G4Service = "F0ACA0B1-EBFA-F96F-28DA-076C35A521DB"

        //WIXEL-HM1x
        case WixelHM1xService = "FFE0"
        }


enum DeviceInfoCharacteristicUUID: String {
        // Read
        // "DexcomUN"
        case ManufacturerNameString = "2A29"
        }


enum CGMServiceCharacteristicUUID: String {

        //G5
        // Read/Notify
        case Communication = "F8083533-849E-531C-C594-30F1F86A4EA5"
        // Write/Indicate
        case Control = "F8083534-849E-531C-C594-30F1F86A4EA5"
        // Read/Write/Indicate
        case Authentication = "F8083535-849E-531C-C594-30F1F86A4EA5"
        // Read/Write/Notify
        case ProbablyBackfill = "F8083536-849E-531C-C594-30F1F86A4EA5"

        //G4
        case G4Authentication = "F0ACACAC-EBFA-F96F-28DA-076C35A521DB"
        case G4Control = "F0ACB20A-EBFA-F96F-28DA-076C35A521DB"
        case G4Communication = "F0ACB20B-EBFA-F96F-28DA-076C35A521DB"
        case G4Pulse = "F0AC2B18-EBFA-F96F-28DA-076C35A521DB"
        case G4SmartPhoneCommand = "F0ACB0CC-EBFA-F96F-28DA-076C35A521DB"
        case G4ReceiverStatus = "F0ACB0CD-EBFA-F96F-28DA-076C35A521DB"

        case WixelSerial = "FFE1"

        }


enum ServiceBCharacteristicUUID: String {
        // Write/Indicate
        case CharacteristicE = "F8084533-849E-531C-C594-30F1F86A4EA5"
        // Read/Write/Notify
        case CharacteristicF = "F8084534-849E-531C-C594-30F1F86A4EA5"
        }*/



//public class DexShareAttributes {
//    //Share Service String
//    public static final UUID CradleService= UUID.fromString("F0ABA0B1-EBFA-F96F-28DA-076C35A521DB");
//
//    //Share Characteristic Strings
//    public static final UUID AuthenticationCode = UUID.fromString("F0ABACAC-EBFA-F96F-28DA-076C35A521DB");
//    public static final UUID ShareMessageReceiver= UUID.fromString("F0ABB20A-EBFA-F96F-28DA-076C35A521DB"); // Max 20 Bytes - Writable
//    public static final UUID ShareMessageResponse= UUID.fromString("F0ABB20B-EBFA-F96F-28DA-076C35A521DB"); // Max 20 Bytes
//    public static final UUID Command= UUID.fromString("F0ABB0CC-EBFA-F96F-28DA-076C35A521DB");
//    public static final UUID Response= UUID.fromString("F0ABB0CD-EBFA-F96F-28DA-076C35A521DB"); // Writable?
//    public static final UUID HeartBeat= UUID.fromString("F0AB2B18-EBFA-F96F-28DA-076C35A521DB");
//
//    //Possible new uuids????  60bfxxxx-60b0-4d4f-0000-000160c48d70
//    public static final UUID CradleService2= UUID.fromString("F0ACA0B1-EBFA-F96F-28DA-076C35A521DB");
//    public static final UUID AuthenticationCode2 = UUID.fromString("F0ACACAC-EBFA-F96F-28DA-076C35A521DB"); // read, write
//    public static final UUID ShareMessageReceiver2= UUID.fromString("F0ACB20A-EBFA-F96F-28DA-076C35A521DB"); // read, write
//    public static final UUID ShareMessageResponse2= UUID.fromString("F0ACB20B-EBFA-F96F-28DA-076C35A521DB"); // indicate, read
//    public static final UUID Command2= UUID.fromString("F0ACB0CC-EBFA-F96F-28DA-076C35A521DB"); // read, write
//    public static final UUID Response2= UUID.fromString("F0ACB0CD-EBFA-F96F-28DA-076C35A521DB"); // indicate, read, write
//    public static final UUID HeartBeat2= UUID.fromString("F0AC2B18-EBFA-F96F-28DA-076C35A521DB"); // notify, read
//
//    //Device Info
//    public static final UUID DeviceService= UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
//    public static final UUID PowerLevel= UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
//
//}
