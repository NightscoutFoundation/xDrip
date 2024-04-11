package com.eveningoutpost.dexdrip.insulin.diaconnp8.penlog;


import android.content.Context;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.Getter;

public class LogInjectionSuccess {
    public static final byte PENLOG_KIND = 0x07;
    @Getter
    private final String data;
    @Getter
    private final String dttm;
    @Getter
    private final byte type;
    @Getter
    private final byte kind;
    @Getter
    private final short setAmt;
    @Getter
    private final short injAmt;
    private final byte insulinKind;
    private final byte cartridgeMaker;
    private final byte batteryRemain;

    private LogInjectionSuccess(String data, String dttm, byte typeAndKind, short setAmt, short injAmt, byte insulinKind, byte cartridgeMaker, byte batteryRemain) {
        this.data = data;
        this.dttm = dttm;
        this.type = DiaconnLogUtil.getType(typeAndKind);
        this.kind = DiaconnLogUtil.getKind(typeAndKind);
        this.setAmt = setAmt;
        this.injAmt = injAmt;
        this.insulinKind = insulinKind;
        this.cartridgeMaker = cartridgeMaker;
        this.batteryRemain = batteryRemain;
    }

    public static LogInjectionSuccess parse(String data) {
        byte[] bytes = DiaconnLogUtil.hexStringToByteArray(data);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return new LogInjectionSuccess(
                data,
                DiaconnLogUtil.getDttm(buffer),
                DiaconnLogUtil.getByte(buffer),
                DiaconnLogUtil.getShort(buffer),
                DiaconnLogUtil.getShort(buffer),
                DiaconnLogUtil.getByte(buffer),
                DiaconnLogUtil.getByte(buffer),
                DiaconnLogUtil.getByte(buffer)
        );
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LogInjectionSuccess{");
        sb.append("LOG_KIND=").append(PENLOG_KIND);
        sb.append(", data='").append(data).append('\'');
        sb.append(", dttm='").append(dttm).append('\'');
        sb.append(", type=").append(type);
        sb.append(", kind=").append(kind);
        sb.append(", setAmt=").append(setAmt);
        sb.append(", injAmt=").append(injAmt);
        sb.append(", insulinKind=").append(insulinKind);
        sb.append(", cartridgeMaker=").append(cartridgeMaker);
        sb.append(", batteryRemain=").append(batteryRemain);
        sb.append('}');
        return sb.toString();
    }

    public String toNote() {
        String insulinKindName = "";
        switch (insulinKind) {
            case 1:
                insulinKindName = xdrip.gs(R.string.title_diaconnp8_insulin_ultra_rapid);
                break;
            case 2:
                insulinKindName = xdrip.gs(R.string.title_diaconnp8_insulin_rapid);
                break;
            case 3:
                insulinKindName = xdrip.gs(R.string.title_diaconnp8_insulin_long);
                break;
            case 4:
                insulinKindName = xdrip.gs(R.string.title_diaconnp8_insulin_NPH);
                break;
            case 5:
                insulinKindName = xdrip.gs(R.string.title_diaconnp8_insulin_Mixed);
                break;
            default:
                break;
        }
        String cartridgeMakerName = "";
        switch (cartridgeMaker) {
            case 1:
                cartridgeMakerName =  xdrip.gs(R.string.title_diaconnp8_cartridge_novo);
                break;
            case 2:
                cartridgeMakerName = xdrip.gs(R.string.title_diaconnp8_cartridge_sanofi);
                break;
            case 3:
                cartridgeMakerName = xdrip.gs(R.string.title_diaconnp8_cartridge_lilly);
                break;
            case 4:
                cartridgeMakerName = xdrip.gs(R.string.title_diaconnp8_cartridge_berlin);
                break;
            case 5:
                cartridgeMakerName = xdrip.gs(R.string.title_diaconnp8_cartridge_diaconn);
                break;
            default:
                break;
        }
        return xdrip.gs(R.string.title_diaconnp8_note_fmt, insulinKindName, cartridgeMakerName);
    }
}
