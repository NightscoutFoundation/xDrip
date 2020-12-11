package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Element
public class GraphSettings {
    @Element(required = false)
    public Position position = new Position();

    @Attribute(required=false)
    public int width = -1;

    @Attribute(required=false)
    public int height = 0;

    @Attribute(required = false, name = "bg_color")
    public int bgColor = -1;

    @Attribute(required=false, name = "display_graph")
    public boolean displayGraph = false;
}
