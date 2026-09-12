package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classpath loader for the Consulting Invoice icon set — the channel marks
 * beside the contact lines, the bank badge, and the calendar of the
 * due-by notice.
 *
 * <p>These are template chrome, not content: they ship inside the templates
 * artifact under {@code templates/invoice/consulting/icons/} and are named
 * by the preset, never by the data. Loaded bytes are cached per name, so a
 * render reads each icon file once per JVM.</p>
 */
final class ConsultingIcons {

    static final String LOCATION = "location";
    static final String PHONE = "phone";
    static final String EMAIL = "email";
    static final String WEBSITE = "website";
    static final String BANK = "bank";
    static final String CALENDAR = "payment-calendar";

    private static final String ICON_ROOT = "/templates/invoice/consulting/icons/";
    private static final Map<String, DocumentImageData> CACHE = new ConcurrentHashMap<>();

    private ConsultingIcons() {
    }

    /**
     * Builds a sized icon node.
     *
     * @param name the icon name
     * @param size the box the icon is fitted into
     * @return the icon node
     */
    static DocumentNode icon(String name, double size) {
        return new ImageBuilder()
                .name("Icon-" + name)
                .source(image(name))
                .fitToBounds(size, size)
                .build();
    }

    private static DocumentImageData image(String name) {
        return CACHE.computeIfAbsent(name, ConsultingIcons::read);
    }

    private static DocumentImageData read(String name) {
        String resourcePath = ICON_ROOT + name + ".png";
        try (InputStream input = ConsultingIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing consulting invoice icon: " + resourcePath);
            }
            return DocumentImageData.fromBytes(input.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read consulting invoice icon: " + resourcePath, e);
        }
    }
}
