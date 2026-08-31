package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.svg.SvgIcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classpath loader for the Luma Studio invoice marks: the three contact
 * glyphs, the two section discs, the closing heart and the sidebar sprig.
 *
 * <p>They ship inside the templates artifact under
 * {@code templates/invoice/luma-studio/icons/} as SVG, so they scale with the
 * box they are drawn into rather than being resampled. Parsed icons are
 * cached per token, so a repeated mark reads its file once per JVM.</p>
 *
 * <p>Every mark here is chrome rather than data — this sheet draws the same
 * ones whatever the invoice says — so the tokens are constants and no
 * document names one.</p>
 */
final class LumaStudioIcons {

    static final String PHONE = "phone";
    static final String EMAIL = "email";
    static final String WEBSITE = "website";
    static final String NOTES = "notes";
    static final String BANK = "bank";
    static final String HEART = "heart";
    static final String SPRIG = "sprig";

    private static final String ICON_ROOT = "/templates/invoice/luma-studio/icons/";
    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private LumaStudioIcons() {
    }

    /**
     * Builds one mark at a given width; the height follows its ratio.
     *
     * @param token one of the constants on this class
     * @param width the drawn width
     * @return the icon node
     */
    static DocumentNode icon(String token, double width) {
        return CACHE.computeIfAbsent(token, LumaStudioIcons::read).node(width);
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = LumaStudioIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing luma studio invoice icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read luma studio invoice icon: " + resourcePath, e);
        }
    }
}
