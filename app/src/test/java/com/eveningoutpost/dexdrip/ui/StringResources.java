package com.eveningoutpost.dexdrip.ui;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * The strings a resource directory declares, read from the files rather than from the framework.
 * <p>
 * Every {@code *.xml} in the directory is read, not only {@code strings.xml}. Android compiles a
 * {@code <string>} from whatever file it sits in, and this tree uses that: English is spread over
 * {@code internal.xml} and {@code strings_activity_preferences.xml} among others, and two languages
 * keep an {@code arrays-xx.xml} beside their strings. A reader that went by filename would leave
 * those unread while reporting nothing missing, which is the one failure a check like this must not
 * have.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public final class StringResources {

    /** Separates a plural's name from the quantity of one of its items. */
    public static final String QUANTITY_SEPARATOR = "/";

    private static final String STRING = "string";
    private static final String PLURALS = "plurals";
    private static final String ITEM = "item";
    private static final String NAME = "name";
    private static final String QUANTITY = "quantity";

    private StringResources() {
    }

    /** Every string in every resource file of one directory, keyed by name. */
    public static Map<String, String> in(final Path directory) {
        final Map<String, String> entries = new HashMap<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.xml")) {
            for (final Path file : files) {
                entries.putAll(of(file));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not read the resources in " + directory, e);
        }
        return entries;
    }

    /**
     * The name to text of every {@code <string>} in one resource file, and of every item of every
     * {@code <plurals>} keyed as {@code name/quantity}.
     */
    public static Map<String, String> of(final Path file) {
        final Map<String, String> entries = new HashMap<>();
        final Element resources = documentElementOf(file);
        final NodeList strings = resources.getElementsByTagName(STRING);
        for (int i = 0; i < strings.getLength(); i++) {
            final Element string = (Element) strings.item(i);
            if (!string.getAttribute(NAME).isEmpty()) {
                entries.put(string.getAttribute(NAME), string.getTextContent());
            }
        }
        final NodeList plurals = resources.getElementsByTagName(PLURALS);
        for (int i = 0; i < plurals.getLength(); i++) {
            final Element plural = (Element) plurals.item(i);
            final NodeList items = plural.getElementsByTagName(ITEM);
            for (int j = 0; j < items.getLength(); j++) {
                final Element item = (Element) items.item(j);
                if (!plural.getAttribute(NAME).isEmpty() && !item.getAttribute(QUANTITY).isEmpty()) {
                    entries.put(plural.getAttribute(NAME) + QUANTITY_SEPARATOR
                            + item.getAttribute(QUANTITY), item.getTextContent());
                }
            }
        }
        return entries;
    }

    private static Element documentElementOf(final Path file) {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            return factory.newDocumentBuilder().parse(file.toFile()).getDocumentElement();
        } catch (Exception e) {
            throw new IllegalStateException("Could not read " + file, e);
        }
    }
}
