package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Element
public class SimpleText {
    @Element(required = false, name = "TextSettings")
    public TextSettings textSettingsDateSettings = new TextSettings();

    @Element(required = false, name = "Position")
    public Position position = new Position();

    @Attribute(required = false, name = "text_pattern")
    public String textPattern = "$value $unit at $time";
}
