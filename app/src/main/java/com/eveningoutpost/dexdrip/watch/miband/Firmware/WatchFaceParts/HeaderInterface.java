package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts;

import java.io.IOException;
import java.io.InputStream;

public interface HeaderInterface {
    public Header readFrom(InputStream stream) throws IOException;
}
