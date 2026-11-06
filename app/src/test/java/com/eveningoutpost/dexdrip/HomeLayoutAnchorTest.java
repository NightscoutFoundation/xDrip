package com.eveningoutpost.dexdrip;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import lombok.val;

/**
 * Guards the RelativeLayout anchor chain in the home screen layout. A view that anchors to an id no
 * view carries is not a build error and no inflation test can catch it here, so the layout file is
 * parsed directly.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class HomeLayoutAnchorTest {

    private static final String LAYOUT = "src/main/res/layout/activity_home.xml";

    // ===== activity_home anchors ========================================================================================

    /** Every positional anchor in the layout points at an id that some view in the same file defines. */
    @Test
    public void everyLayoutAnchorResolvesToADefinedView() throws Exception {
        // :: Setup
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(locateLayout());
        val definedIds = collectDefinedIds(document);

        // :: Act & Verify
        val elements = document.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            val attributes = elements.item(i).getAttributes();
            for (int j = 0; j < attributes.getLength(); j++) {
                val name = attributes.item(j).getNodeName();
                val value = attributes.item(j).getNodeValue();
                if (!isPositionalAnchor(name) || !isLocalIdReference(value)) continue;
                assertWithMessage(name + "=\"" + value + "\" points at a view defined in " + LAYOUT)
                        .that(definedIds).contains(stripIdPrefix(value));
            }
        }
    }

    /** The sensor age view still anchors to something, so the removal cannot quietly unanchor it. */
    @Test
    public void sensorAgeViewStillHasAnEndAnchor() throws Exception {
        // :: Setup
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(locateLayout());

        // :: Act
        String anchor = null;
        val elements = document.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            val element = (Element) elements.item(i);
            if ("@+id/libstatus".equals(element.getAttribute("android:id"))) {
                anchor = element.getAttribute("android:layout_toEndOf");
            }
        }

        // :: Verify
        assertWithMessage("the sensor age view exists and carries an end anchor").that(anchor).isNotEmpty();
    }

    private static Set<String> collectDefinedIds(Document document) {
        val ids = new HashSet<String>();
        val elements = document.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            val id = ((Element) elements.item(i)).getAttribute("android:id");
            if (isLocalIdReference(id)) ids.add(stripIdPrefix(id));
        }
        return ids;
    }

    private static boolean isPositionalAnchor(String attributeName) {
        return attributeName.startsWith("android:layout_to")
                || attributeName.equals("android:layout_above")
                || attributeName.equals("android:layout_below")
                || attributeName.startsWith("android:layout_align");
    }

    private static boolean isLocalIdReference(String value) {
        return value != null && (value.startsWith("@+id/") || value.startsWith("@id/"));
    }

    private static String stripIdPrefix(String value) {
        return value.substring(value.indexOf('/') + 1);
    }

    private static File locateLayout() {
        final File fromModule = new File(LAYOUT);
        if (fromModule.isFile()) return fromModule;
        final File fromRoot = new File("app/" + LAYOUT);
        assertWithMessage("the layout file was found; working directory is " + new File(".").getAbsolutePath())
                .that(fromRoot.isFile()).isTrue();
        return fromRoot;
    }
}
