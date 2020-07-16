package com.eveningoutpost.dexdrip.watch.miband.message;

import com.eveningoutpost.dexdrip.watch.miband.Const;

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.watch.miband.Firmware.FirmwareOperations.fromUint8;

public class AlertMessage extends BaseMessage {

    public enum AlertCategory {
        Simple(0),
        Email(1),
        Call(3),
        MissedCall(4),
        SMS_MMS(5),
        SMS(5),
        VoiceMail(6),
        Schedule(7),
        HighPriorityAlert(8),
        InstantMessage(9),
        // 10-250 reserved for future use
        // 251-255 defined by service specification
        Any(255),
        Custom(-1),
        CustomHuami(-6);

        @Getter
        private final byte value;

        AlertCategory(final int value) {
            this.value = (byte) value;
        }
    }

    public enum CustomIcon {
        // icons which are unsure which app they are for are suffixed with _NN
        WECHAT(0),
        PENGUIN_1(1),
        MI_CHAT_2(2),
        FACEBOOK(3),
        TWITTER(4),
        MI_APP_5(5),
        SNAPCHAT(6),
        WHATSAPP(7),
        RED_WHITE_FIRE_8(8),
        CHINESE_9(9),
        ALARM_CLOCK(10),
        APP_11(11),
        INSTAGRAM(12),
        CHAT_BLUE_13(13),
        COW_14(14),
        CHINESE_15(15),
        CHINESE_16(16),
        STAR_17(17),
        APP_18(18),
        CHINESE_19(19),
        CHINESE_20(20),
        CALENDAR(21),
        FACEBOOK_MESSENGER(22),
        VIBER(23),
        LINE(24),
        TELEGRAM(25),
        KAKAOTALK(26),
        SKYPE(27),
        VKONTAKTE(28),
        POKEMONGO(29),
        HANGOUTS(30),
        MI_31(31),
        CHINESE_32(32),
        CHINESE_33(33),
        EMAIL(34),
        WEATHER(35),
        HR_WARNING_36(36);

        @Getter
        private final byte value;

        CustomIcon(final int value) {
            this.value = (byte) value;
        }
    }

    public byte[] getAlertMessage(final String msg, final AlertCategory category) {
        return getAlertMessage(msg, category, CustomIcon.WECHAT);
    }

    public byte[] getAlertMessage(final String msg, final AlertCategory category, final CustomIcon icon) {
        return getAlertMessage(msg, category, icon, "");
    }

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHAR_NEW_ALERT;
    }

    /*
    This works on all Huami devices except Mi Band 2
    */
    public byte[] getAlertMessage(String msg, final AlertMessage.AlertCategory category, final AlertMessage.CustomIcon icon, final String title) {
        byte[] messageBytes = new byte[1];
        byte[] titleBytes = new byte[1];
        if (msg.isEmpty())
            msg = title;
        String message = "\0" + StringUtils.truncate(msg, 128) + "\0";
        String titleString = StringUtils.truncate(title, 18) + "\0";
        try {
            messageBytes = message.getBytes("UTF-8");
            titleBytes = titleString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            //
        }
        int len = 0;
        if (msg.length() > 0)
            len += messageBytes.length;
        if (title.length() > 0)
            len += titleBytes.length;

        if (category == AlertMessage.AlertCategory.CustomHuami) {
            init(3 + len);
        } else {
            init(2 + len);
        }
        putData(category.getValue()); //alertCategory
        putData((byte) 0x01); //number of alerts
        if (category == AlertMessage.AlertCategory.CustomHuami) {
            putData(fromUint8(icon.getValue()));
        }
        if (msg.length() > 0)
            putData(messageBytes);

        if (title.length() > 0)
            putData(titleBytes);
        return getBytes();
    }

    public byte[] getAlertMessageOld(String message, final AlertMessage.AlertCategory category) {
        return getAlertMessageOld(message, category, null);
    }

    // suitable for miband 2 and call with title
    public byte[] getAlertMessageOld(String message, final AlertMessage.AlertCategory category, final AlertMessage.CustomIcon icon) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(100);
        stream.write(fromUint8(category.getValue()));
        stream.write(fromUint8(0x01));
        if (category == AlertMessage.AlertCategory.CustomHuami) {
            stream.write(fromUint8(icon.getValue()));
        }
        if (message.length() > 0) {
            try {
                stream.write(message.getBytes("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            ;
        } else {
            // some write a null byte instead of leaving out this optional value
            // stream.write(new byte[] {0});
        }
        return stream.toByteArray();
    }
}