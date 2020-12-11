package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

@Element
public class TextSettings {
    @Attribute(required = false)
    public int color = -1;
    @Attribute(required = false, name = "font_size")
    public int fontSize = 10;
}
