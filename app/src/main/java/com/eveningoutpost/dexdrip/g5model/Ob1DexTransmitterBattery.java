package com.eveningoutpost.dexdrip.g5model;


import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.services.G5BaseService;
import com.eveningoutpost.dexdrip.services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;


/**
 * External public-facing accessor for getting transmitter battery and related status information
 * for Ob1G5/G6, which abstracts away the G5/G6 internals.
 *
 * Used in the Ob1G5/G6 status page as well as for Nightscout transmitter battery upload.
 *
 * @author James Woglom (j@wogloms.net)
 */
public class Ob1DexTransmitterBattery {

    private final String tx_id;
    private final BatteryInfoRxMessage battery;
    private final VersionRequestRxMessage firmware;

    public Ob1DexTransmitterBattery(String tx_id, BatteryInfoRxMessage battery, VersionRequestRxMessage firmware) {
        this.tx_id = tx_id;
        this.battery = battery;
        this.firmware = firmware;
    }

    private Ob1DexTransmitterBattery(String tx_id) {
        this.tx_id = tx_id;
        this.battery = Ob1G5StateMachine.getBatteryDetails(tx_id);
        this.firmware = (VersionRequestRxMessage) Ob1G5StateMachine.getFirmwareXDetails(tx_id, 0);
    }

    public Ob1DexTransmitterBattery() {
        this(Pref.getStringDefaultBlank("dex_txid"));
    }

    public boolean isPresent() {
        return battery != null && firmware != null;
    }

    public TransmitterStatus status() {
        return TransmitterStatus.getBatteryLevel(firmware.status);
    }

    public int days() {
        if (battery.runtime > -1) {
            return battery.runtime;
        }

        return DexTimeKeeper.getTransmitterAgeInDays(tx_id);
    }

    public String daysEstimate() {
        final int timekeeperDays = DexTimeKeeper.getTransmitterAgeInDays(tx_id);
        long last_transmitter_timestamp = Ob1G5CollectionService.getLast_transmitter_timestamp();

        StringBuilder b = new StringBuilder();

        if (battery.runtime > -1) {
            b.append(battery.runtime);
        }

        if (timekeeperDays > -1) {
            if (b.length() > 0) {
                b.append(FirmwareCapability.isTransmitterG6Rev2(tx_id) ? " " : " / ");
            }
            b.append(timekeeperDays);
        }

        if (last_transmitter_timestamp > 0) {
            b.append(" / ");
            b.append(JoH.qs((double) last_transmitter_timestamp / 86400, 1));
        }

        return b.toString();
    }

    public int voltageA() {
        return battery.voltagea;
    };

    public int voltageB() {
        return battery.voltageb;
    }

    public boolean voltageAWarning() {
        return voltageA() < G5BaseService.LOW_BATTERY_WARNING_LEVEL;
    }

    public boolean voltageBWarning() {
        return voltageB() < (G5BaseService.LOW_BATTERY_WARNING_LEVEL - 10);
    };

    public int resistance() {
        return battery.resist;
    }

    public enum ResistanceStatus {
        UNKNOWN(null),
        GOOD(StatusItem.Highlight.GOOD),
        NORMAL(StatusItem.Highlight.NORMAL),
        WARNING(StatusItem.Highlight.NOTICE),
        BAD(StatusItem.Highlight.BAD)
        ;

        // Passes-through the highlight used on the status page
        public final StatusItem.Highlight highlight;
        ResistanceStatus(StatusItem.Highlight highlight) {
            this.highlight = highlight;
        }
    }
    public ResistanceStatus resistanceStatus() {
        if (!FirmwareCapability.isFirmwareTemperatureCapable(firmware.firmware_version_string)) {
            return ResistanceStatus.UNKNOWN;
        }

        if (resistance() > 1400) {
            return ResistanceStatus.BAD;
        } else if (resistance() > 1000) {
            return ResistanceStatus.WARNING;
        } else if (resistance() > 750) {
            return ResistanceStatus.NORMAL;
        } else {
            return ResistanceStatus.GOOD;
        }
    };

    public long lastQueried() {
        return PersistentStore.getLong(G5BaseService.G5_BATTERY_FROM_MARKER + tx_id);
    }

    public int temperature() {
        return battery.temperature;
    }
}
