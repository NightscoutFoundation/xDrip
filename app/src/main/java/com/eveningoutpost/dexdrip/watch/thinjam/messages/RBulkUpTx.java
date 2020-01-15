package com.eveningoutpost.dexdrip.watch.thinjam.messages;

import java.util.Arrays;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.ERROR_OK;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.errorText;

// jamorham

public class RBulkUpTx extends BulkUpTx {

    @Getter
    int bytesIncluded;
    @Getter
    boolean quiet = false;

    public RBulkUpTx(final int opcode, final byte[] buffer, final int offset) {

        if (buffer == null || buffer.length < offset || offset > 255)
            return; // fail to initialize if data invalid

        init((byte) opcode, 19);
        data.put((byte) (offset & 0xff));
        bytesIncluded = Math.min(data.remaining(), buffer.length - offset);
        data.put(buffer, offset, bytesIncluded);
    }

    public RBulkUpTx setQuiet() {
        quiet = true;
        return this;
    }


    @Override
    public boolean responseOk(final byte[] response) {
        if (response == null || response.length < 2) return false;
        // check status code and opcode
        if (quiet) {
            return Arrays.equals(response, getBytes());
        } else {
            if (response[0] == 0x06 && response[1] == 0) return true; // end of sequence ok marker
            return (response[0] == getBytes()[0] && response[1] == ERROR_OK) && (!Arrays.equals(response, getBytes()));
        }
    }

    @Override
    public String responseText(final byte[] response) {
        int type = response[0] & 0xFF;
        int code = response[1] & 0xFF;
        if (type == 0x06) {
            return "Retry request from: " + code;
        } else if (Arrays.equals(response, getBytes())) {
            return "Failed to get response from bluetooth layer";
        } else {
            if (code < errorText.length) {
                return errorText[code];
            } else {
                return "Unknown Error Text :: " + type + " " + code;
            }
        }
    }

}
