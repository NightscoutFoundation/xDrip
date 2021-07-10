package com.eveningoutpost.dexdrip.cgm.medtrum;

/**
 * jamorham
 *
 * Medtrum domain interface class + persistent storage
 */

import android.util.Pair;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.AdvertRx;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.AnnexARx;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.bt.ScanRecordFromLegacy;
import com.polidea.rxandroidble2.scan.ScanRecord;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.Models.JoH.msSince;
import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.MANUFACTURER_ID;
import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.SUPPORTED_DEVICES;

public class Medtrum {

    private static final String TAG = "Medtrum";
    private static final String PREF_CURRENT_SERIAL = "medtrum-current-tx-serial";
    private static final String PENDING_CALIBRATION_TIMESTAMP = "medtrum-pending-calibration-timestamp";
    private static final String PENDING_CALIBRATION_GLUCOSE = "medtrum-pending-calibration-glucose";
    private static final String VERSION_STORE = "medtrum-tx-version-";
    public static final String PREF_AHEX = "medtrum_a_hex";


    @Getter
    private static AnnexARx lastAdvertAnnex = null;

    public static byte[] getManufactuerSpecificDataFromScanRecord(final ScanRecord scanRecord) {
        return scanRecord.getManufacturerSpecificData(MANUFACTURER_ID);
    }

    public static void processDataFromScanRecord(final ScanRecord scanRecord) {
        final byte[] dataBlob = getManufactuerSpecificDataFromScanRecord(scanRecord);
        if (dataBlob != null) {
            final AdvertRx advert = new AdvertRx(dataBlob);
            if (advert.isValid()) {
                lastAdvertAnnex = advert.getAnnex();
                UserError.Log.d(TAG, "Advert: " + advert.toS());
                //advert.getAnnex().processForTimeKeeper(advert.serial); // TODO is timekeeper valid here?
                setVersion(advert.serial, advert.version);
            }
        } else {
            UserError.Log.d(TAG, "Could not extract needed data from scan record");
        }
    }

    public static boolean isRecognisedDevice(byte[] dataBlob) {
        final AdvertRx advert = new AdvertRx(dataBlob);
        return advert.isValid();
    }

    public static String getDeviceInfoStringFromLegacy(byte[] scanRecord) {
        final ScanRecord sr = ScanRecordFromLegacy.parseFromBytes(scanRecord);
        if (sr != null) {
            return getDeviceInfoString(sr);
        }
        return null;
    }

    // TODO create processing method to avoid code duplication for legacy scan records
    public static boolean saveSerialFromLegacy(byte[] scanRecord) {
        final ScanRecord sr = ScanRecordFromLegacy.parseFromBytes(scanRecord);
        if (sr != null) {
            final byte[] dataBlob = getManufactuerSpecificDataFromScanRecord(sr);
            if (dataBlob == null) return false;
            final AdvertRx advert = new AdvertRx(dataBlob);
            if (advert.isValid()) {
                saveSerial(advert.serial);
                return true;
            }
        }
        return false;
    }

    public static String getDeviceInfoString(ScanRecord scanRecord) {
        final byte[] dataBlob = getManufactuerSpecificDataFromScanRecord(scanRecord);
        if (dataBlob == null) return null;
        final AdvertRx advert = new AdvertRx(dataBlob);
        if (advert.isValid()) {
            // TODO format string
            return "Medtrum " + advert.getDeviceName() + " SN:" + advert.serial;
        }
        return null;
    }


    public static boolean isDeviceTypeSupported(int device_type) {
        return SUPPORTED_DEVICES.contains(device_type);
    }

    public static void saveSerial(long serial) {
        Pref.setLong(PREF_CURRENT_SERIAL, serial);
    }

    public static long getSerial() {
        return Pref.getLong(PREF_CURRENT_SERIAL, 0); // 0 is undefined
    }

    private static boolean acceptCommands() {
        return DexCollectionType.getDexCollectionType() == DexCollectionType.Medtrum;
    }


    public static synchronized void addCalibration(final int glucose, long timestamp) {
        if (acceptCommands()) {
            // avoid double calibrations
            if (glucose == 0 || JoH.ratelimit("medtrum-calibration-cooldown", 5)) {
                PersistentStore.setLong(PENDING_CALIBRATION_TIMESTAMP, timestamp);
                PersistentStore.setLong(PENDING_CALIBRATION_GLUCOSE, glucose);
                if (glucose != 0) {
                    Inevitable.task("medtrum-ping", 1000, MedtrumCollectionService::calibratePing);
                }
            }
        }
    }

    public static synchronized void clearCalibration() {
        addCalibration(0, 0);
    }

    public static synchronized Pair<Long, Integer> getCalibration() {
        final long timestamp = PersistentStore.getLong(PENDING_CALIBRATION_TIMESTAMP);
        final long glucose = PersistentStore.getLong(PENDING_CALIBRATION_GLUCOSE);
        if (glucose == 0 || timestamp == 0 || msSince(timestamp) > Constants.HOUR_IN_MS * 8) {
            return null;
        } else {
            return new Pair<>(timestamp, (int) glucose);
        }

    }

    public static int getVersion(long serial) {
        return (int) PersistentStore.getLong(VERSION_STORE + serial);
    }

    public static void setVersion(long serial, int version) {
        if (serial > 0 && version > 0) {
            if (getVersion(serial) != version) {
                PersistentStore.setLong(VERSION_STORE + serial, version);
            }
        }
    }

}
