package com.eveningoutpost.dexdrip.ui;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/**
 * Tests for {@link StringResources}, which decides what {@link TranslationPlaceholderTest} gets to
 * see at all.
 * <p>
 * A sweep can only be as complete as its reader. Every hole opened here - a file not opened, a tag
 * not recognised - leaves the sweep green while the strings behind it go unchecked, and no
 * assertion further down can notice. So the reader is tested against files written here, where what
 * it ought to find is known, rather than against the tree, where it is not.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class StringResourcesTest {

    @Rule
    public final TemporaryFolder directory = new TemporaryFolder();

    /** The ordinary case: names and texts, as declared. */
    @Test
    public void everyStringInAFileIsRead() throws IOException {
        // :: Setup
        final File file = resourceFile("strings.xml",
                "<string name=\"first\">Value %1$d</string>",
                "<string name=\"second\">Plain</string>");

        // :: Act
        final Map<String, String> strings = StringResources.of(file.toPath());

        // :: Verify
        assertWithMessage("strings read from a resource file")
                .that(strings)
                .containsExactly("first", "Value %1$d", "second", "Plain");
    }

    /** A plural is several strings, and the quantity is part of what names each one. */
    @Test
    public void everyItemOfAPluralIsReadUnderItsQuantity() throws IOException {
        // :: Setup
        final File file = resourceFile("strings.xml",
                "<plurals name=\"days\">",
                "  <item quantity=\"one\">%d day</item>",
                "  <item quantity=\"other\">%d days</item>",
                "</plurals>");

        // :: Act
        final Map<String, String> strings = StringResources.of(file.toPath());

        // :: Verify
        assertWithMessage("items read from a plural")
                .that(strings)
                .containsExactly("days" + StringResources.QUANTITY_SEPARATOR + "one", "%d day",
                        "days" + StringResources.QUANTITY_SEPARATOR + "other", "%d days");
    }

    /** Android compiles a string from any resource file, so reading one filename would miss some. */
    @Test
    public void stringsAreReadFromEveryFileInTheDirectory() throws IOException {
        // :: Setup
        resourceFile("strings.xml", "<string name=\"named\">Value %1$d</string>");
        resourceFile("arrays.xml", "<string name=\"beside_an_array\">Other %1$s</string>");

        // :: Act
        final Map<String, String> strings = StringResources.in(directory.getRoot().toPath());

        // :: Verify
        assertWithMessage("strings read from a resource directory")
                .that(strings.keySet())
                .containsExactly("named", "beside_an_array");
    }

    /** Only a file that declares strings contributes any, and a directory of others reads empty. */
    @Test
    public void aDirectoryWithoutStringsReadsEmpty() throws IOException {
        // :: Setup
        resourceFile("dimens.xml", "<dimen name=\"padding\">8dp</dimen>");

        // :: Act
        final Map<String, String> strings = StringResources.in(directory.getRoot().toPath());

        // :: Verify
        assertWithMessage("strings read from a directory that declares none")
                .that(strings)
                .isEmpty();
    }

    /** A file that cannot be parsed is said so, rather than quietly contributing nothing. */
    @Test
    public void anUnreadableFileIsReported() throws IOException {
        // :: Setup
        final File file = directory.newFile("broken.xml");
        Files.write(file.toPath(), "<resources><string name=".getBytes(StandardCharsets.UTF_8));

        // :: Act
        IllegalStateException thrown = null;
        try {
            StringResources.of(file.toPath());
        } catch (IllegalStateException reported) {
            thrown = reported;
        }

        // :: Verify
        assertWithMessage("what reading a malformed resource file reports")
                .that(thrown)
                .isNotNull();
        assertWithMessage("the file named in the report")
                .that(thrown.getMessage())
                .contains("broken.xml");
    }

    private File resourceFile(final String name, final String... lines) throws IOException {
        final StringBuilder content = new StringBuilder("<resources>\n");
        for (final String line : lines) {
            content.append(line).append('\n');
        }
        content.append("</resources>\n");
        final File file = directory.newFile(name);
        Files.write(file.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
