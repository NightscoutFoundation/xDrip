package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "Watchface")
public class WatchfaceConfig {
    @Attribute(name = "watch_type")
    public String watchType;

    @Attribute(required = false, name = "canvas_image_offset")
    public int canvasImageOffset = 0;

    @Element(required = false, name = "Graph")
    public GraphSettings graph = new GraphSettings();

    @Element(required = false, name = "ArrowPosition")
    public Position arrowPosition = new Position();

    @Element(required = false, name = "IobText")
    public SimpleText iob = new SimpleText();

    @Element(required = false, name = "DeltaText")
    public ValueTimeText deltaText = new ValueTimeText();

    @Element(required = false, name = "DeltaTimeText")
    public ValueTimeText deltaTimeText;

    @Element(required = false, name = "TreatmentText")
    public ValueTimeText treatmentText = new ValueTimeText();

    @Element(required = false, name = "TreatmentTimeText")
    public ValueTimeText treatmentTimeText;

}
