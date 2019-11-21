package com.eveningoutpost.dexdrip.watch.miband.message;

import com.eveningoutpost.dexdrip.watch.miband.Const;

import java.nio.ByteBuffer;
import java.util.UUID;

import lombok.Getter;

//UUID_CHAR_ALERT_LEVEL
public class AlertLevelMessage extends BaseMessage{
    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHAR_ALERT_LEVEL;
    }

    public enum AlertLevelType {

        NoAlert(0),
        MidAlert(1),
        HightAlert(2),
        VibrateAlert(3),
        Custom((byte)0xfa);// followed by another uin8
        @Getter
        private final byte value;

        AlertLevelType (final int value) {
            this.value = (byte) value;
        }
    }

    public byte[] getAlertLevelMessage(AlertLevelType level) {
        init(1);
        putData(level.getValue()); //icon
        return getBytes();
    }

    public byte[] getPeriodicVibrationMessage(byte count, short activeVibrationTime, short  pauseVibrationTime ) {
        byte[] arr;
        ByteBuffer b = ByteBuffer.allocate(2);
        init(6);
        //custom vibration type
        putData((byte)0xff);
        b.putShort(activeVibrationTime);
        arr = b.array();
        //put activeVibrationTime
        putData(arr[1]);
        putData(arr[0]);
        b.clear();
        b.putShort(pauseVibrationTime);
        arr = b.array();
        //put pauseVibrationTime
        putData(arr[1]);
        putData(arr[0]);
        putData(count);
        return getBytes();
    }

}
