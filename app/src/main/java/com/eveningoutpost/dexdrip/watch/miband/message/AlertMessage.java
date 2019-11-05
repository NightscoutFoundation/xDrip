package com.eveningoutpost.dexdrip.watch.miband.message;

import com.eveningoutpost.dexdrip.watch.miband.Const;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import lombok.Getter;

public class AlertMessage extends BaseMessage{

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHAR_NEW_ALERT;
    }

    public enum AlertType {

        Email(1),
        Call(2),
        MissedCall(3),
        SMS_MMS(5);

        @Getter
        private final byte value;

        AlertType(final int value) {
            this.value = (byte) value;
        }
    }

    public byte[] getAlertMessage(final String msg) {
        return getAlertMessage(msg, AlertType.SMS_MMS);
    }

    public byte[] getAlertMessage(final String msg, final AlertType icon) {
        byte[] messageBytes = new byte[1];
        try {
            messageBytes = msg.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            //
        }
        init(2 + messageBytes.length);
        putData(icon.getValue()); //icon
        putData((byte) 0x01); //number of alert
        putData(messageBytes);
        return getBytes();
    }
}
