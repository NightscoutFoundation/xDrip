package com.eveningoutpost.dexdrip.glucosemeter.glucomen.blocks;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.buffer.MyByteBuffer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;

/**
 * JamOrHam
 * SerialBlock handler
 */

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SerialBlock extends MyByteBuffer {

    public String serial;

    public static SerialBlock parse(final byte[] bytes) {
        if (bytes != null && bytes.length > 4) {
            val serial = new String(JoH.reverseBytes(bytes))
                    .replace("\0", "");
            if (serial.length() > 4) {
                return new SerialBlock(serial);
            }
        }
        return null;
    }
}
