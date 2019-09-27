package com.eveningoutpost.dexdrip.watch.thinjam.messages;

import java.util.Arrays;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.ERROR_OK;

// jamorham

public class BulkUpTx extends BaseTx {

    @Getter
    int bytesIncluded;
    @Getter
    boolean quiet = false;

    public BulkUpTx(final int opcode, final byte[] buffer, final int offset) {

        if (buffer == null || buffer.length < offset) return; // fail to initialize if data invalid

        init((byte) opcode, 19);
        bytesIncluded = Math.min(data.remaining(), buffer.length - offset);
        data.put(buffer, offset, bytesIncluded);
    }

    public BulkUpTx setQuiet() {
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
            return (response[0] == getBytes()[0] && response[1] == ERROR_OK) && (!Arrays.equals(response, getBytes()));
        }
    }

}
