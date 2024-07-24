package com.eveningoutpost.dexdrip.importedlibraries.dexcom.records;

import com.eveningoutpost.dexdrip.models.UserError.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.StringReader;
import java.util.Arrays;

// This code and this particular library are from the NightScout android uploader
// Check them out here: https://github.com/nightscout/android-uploader
// Some of this code may have been modified for use in this project

public class GenericXMLRecord extends GenericTimestampRecord {
    int XML_START = 8;
    int XML_END = 241;

    private final String TAG = GenericXMLRecord.class.getSimpleName();

    private Element xmlElement;

    public GenericXMLRecord(byte[] packet) {
        super(packet);
        Document document;
        // TODO: it would be best if we could just remove /x00 characters and read till end
        String xml = new String(Arrays.copyOfRange(packet, XML_START, XML_END));
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try
        {
            builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(xml)));
            xmlElement = document.getDocumentElement();
        } catch (Exception e) {
            Log.e(TAG, "Unable to build xml element", e);
        }
    }

    // example: String sn = getXmlElement().getAttribute("SerialNumber");
    public Element getXmlElement() {
        return xmlElement;
    }
}
