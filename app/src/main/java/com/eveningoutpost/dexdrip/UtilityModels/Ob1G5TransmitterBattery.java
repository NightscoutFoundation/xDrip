package com.eveningoutpost.dexdrip.UtilityModels;

import com.eveningoutpost.dexdrip.G5Model.BatteryInfoRxMessage;
import com.eveningoutpost.dexdrip.G5Model.Ob1G5StateMachine;
import com.eveningoutpost.dexdrip.G5Model.TransmitterStatus;
import com.eveningoutpost.dexdrip.G5Model.VersionRequestRxMessage;
import com.eveningoutpost.dexdrip.Services.G5BaseService;

/**
 * Holds information about a G5/G6 transmitter's battery status,
 * used when sent to Nightscout and obtained via Ob1G5StateMachine.
 *
 * @author James Woglom (j@wogloms.net)
 */
public class Ob1G5TransmitterBattery implements TransmitterBattery {
   public static Ob1G5TransmitterBattery getTransmitterBattery() {
       return new Ob1G5TransmitterBattery(Pref.getStringDefaultBlank("dex_txid"));
   }

   private BatteryInfoRxMessage batteryRx;
   private VersionRequestRxMessage versionRx;
   private static final int batteryWarningLevel = G5BaseService.getLowBatteryWarningLevel();

   public Ob1G5TransmitterBattery(String tx_id) {
       batteryRx = Ob1G5StateMachine.getBatteryDetails(tx_id);
       versionRx = Ob1G5StateMachine.getFirmwareDetails(tx_id);
   }

   public TransmitterStatus status() {
       return TransmitterStatus.getBatteryLevel(versionRx.status);
   }

   public int days() {
       return batteryRx.runtime;
   }

   public int voltageA() {
       return batteryRx.voltagea;
   }

   public VoltageStatus voltageAStatus() {
       if (batteryRx.voltagea < batteryWarningLevel) return VoltageStatus.WARNING;
       return VoltageStatus.GOOD;
   }

   public int voltageB() {
       return batteryRx.voltageb;
   }

   public VoltageStatus voltageBStatus() {
       if (batteryRx.voltageb < batteryWarningLevel) return VoltageStatus.WARNING;
       return VoltageStatus.GOOD;
   }

   public int resistance() {
       return batteryRx.resist;
   }

   public ResistanceStatus resistanceStatus() {
       int r = batteryRx.resist;

       if (r > 1400) {
           return ResistanceStatus.BAD;
       } else if (r > 1000) {
           return ResistanceStatus.NOTICE;
       } else if (r > 750) {
           return ResistanceStatus.NORMAL;
       } else {
           return ResistanceStatus.GOOD;
       }
   }

   public int temperature() {
       return batteryRx.temperature;
   }

   // This is what nightscout shows in the UI
   public String battery() {
       return voltageA()+"/"+voltageB()+"/"+resistance();
   }
}
