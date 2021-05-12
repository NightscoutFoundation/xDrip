package com.eveningoutpost.dexdrip.watch.lefun.messages;

// jamorham

import lombok.Getter;

public class RxPong extends BaseRx {

    public static byte opcode = 0x00;

    {
        length = 20;
    }

    boolean oldType;

    private byte ff1;
    private byte othree;
    private byte zero2;
    private byte zero3;

    @Getter
    private String model;
    @Getter
    private String manufacturer;
    private byte vers1;
    @Getter
    private String hwVersion;
    @Getter
    private String swVersion;


    @Override
    public BaseRx fromBytes(final byte[] bytes) {

        this.bytes = bytes;

        if (!validate(opcode)) {
            length = 16;
            buffer = null; // reset
            if (!validate(opcode)) {
                return null;
            } else {
                oldType = true;
            }
        }

        ff1 = buffer.get();
        othree = buffer.get();
        zero2 = buffer.get();
        zero3 = buffer.get();

        model = getStringBytes(3);
        vers1 = buffer.get();
        hwVersion = getCanonicalVersion(2);
        swVersion = getCanonicalVersion(2);
        if (!oldType) {
            manufacturer = getStringBytes(4);
        }

        return this;
    }


    @Override
    public String toString() {
        return String.format("Pong: manu: %s : model: %s : hw: %s sw: %s", manufacturer, model, hwVersion, swVersion);
    }

}
