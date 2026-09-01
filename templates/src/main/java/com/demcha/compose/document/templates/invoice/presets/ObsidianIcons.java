package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.svg.SvgIcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The two marks the Obsidian invoice packages, one for each of its information
 * cards. Both are the preset's: which glyph opens the notes and which opens the
 * payment details is a property of the design, not of the invoice.
 *
 * <p>The party discs carry no packaged mark. The supplier's shows the caller's
 * logo when there is one and the brand's initials otherwise; the billed party's
 * shows initials taken from its name.</p>
 */
final class ObsidianIcons {

    static final String NOTES = "notes";
    static final String PAYMENT = "payment";

    private static final String ICON_ROOT = "/templates/invoice/obsidian/icons/";

    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private ObsidianIcons() {
    }

    /**
     * A packaged mark by name.
     *
     * @param token the mark's name
     * @return the parsed icon
     */
    static SvgIcon icon(String token) {
        return CACHE.computeIfAbsent(token, ObsidianIcons::read);
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = ObsidianIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing obsidian invoice icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read obsidian invoice icon: " + resourcePath, e);
        }
    }
}
