package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Element
public class Position {
    @Attribute(required=false)
    public int x = 0;

    @Attribute(required=false)
    public int y = 0;
}
