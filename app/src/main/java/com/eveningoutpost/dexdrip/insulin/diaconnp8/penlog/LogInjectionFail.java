package com.eveningoutpost.dexdrip.insulin.diaconnp8.penlog;

import androidx.annotation.NonNull;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.Getter;

public class LogInjectionFail {
    public static final byte PENLOG_KIND = 0x08;
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
    private final byte injFailReason;

    private LogInjectionFail(String data, String dttm, byte typeAndKind, short setAmt, short injAmt, byte insulinKind, byte cartridgeMaker, byte injFailReason) {
        this.data = data;
        this.dttm = dttm;
        this.type = DiaconnLogUtil.getType(typeAndKind);
        this.kind = DiaconnLogUtil.getKind(typeAndKind);
        this.setAmt = setAmt;
        this.injAmt = injAmt;
        this.insulinKind = insulinKind;
        this.cartridgeMaker = cartridgeMaker;
        this.injFailReason = injFailReason;
    }

    public static LogInjectionFail parse(String data) {
        byte[] bytes = DiaconnLogUtil.hexStringToByteArray(data);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return new LogInjectionFail(
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
        return "LOG_INJ_FAIL{" + "LOG_KIND=" + PENLOG_KIND +
                ", data='" + data + '\'' +
                ", dttm='" + dttm + '\'' +
                ", type=" + type +
                ", kind=" + kind +
                ", setAmt=" + setAmt +
                ", injAmt=" + injAmt +
                ", insulinKind=" + insulinKind +
                ", cartridgeMaker=" + cartridgeMaker +
                ", injFailReason=" + injFailReason +
                '}';
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
