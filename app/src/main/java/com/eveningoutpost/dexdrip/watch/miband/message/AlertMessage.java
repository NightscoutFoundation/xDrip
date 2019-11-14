package com.eveningoutpost.dexdrip.watch.miband.message;

import com.eveningoutpost.dexdrip.watch.miband.Const;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import lombok.Getter;

public class AlertMessage extends BaseMessage {

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHAR_NEW_ALERT;
    }

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
        byte[] messageBytes = new byte[1];
        try {
            messageBytes = msg.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            //
        }
        if (category == AlertCategory.CustomHuami) {
            init(3 + messageBytes.length);
        } else {
            init(2 + messageBytes.length);
        }
        putData(category.getValue()); //alertCategory
        putData((byte) 0x01); //number of alert
        if (category == AlertCategory.CustomHuami) {
            putData(fromUint8(icon.value));
        }
        if (msg.length() > 0)
            putData(messageBytes);
        return getBytes();
    }
}
