package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.svg.SvgIcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The marks the Platform invoice packages.
 *
 * <p>Nine the preset chooses itself — which glyph opens the bill-to block, the
 * ship-to block, the bank panel, the panel's note, the due-date card and each of
 * the three contact channels. The other five are a vocabulary a document may
 * name on a service line through {@code InvoiceServiceLines.Line.icon()}.</p>
 *
 * <p>Each mark carries the accent colour in the asset itself rather than taking
 * it at draw time, which is how the design draws them: they are filled shapes in
 * one colour, not line work tinted per use.</p>
 */
final class PlatformIcons {

    static final String BILL_TO = "bill-to";
    static final String SHIP_TO = "ship-to";
    static final String BANK = "bank";
    static final String CALENDAR = "calendar";
    static final String INFO = "info";
    static final String INFO_OUTLINE = "info-outline";
    static final String WEBSITE = "website";
    static final String EMAIL = "email";
    static final String PHONE = "phone";

    private static final String ICON_ROOT = "/templates/invoice/platform/icons/";

    /** The marks a document may name on a service line. */
    static final Set<String> LINE_TOKENS =
            Set.of("compute", "storage", "database", "network", "support");

    private static final Set<String> TOKENS = Set.of(
            BILL_TO, SHIP_TO, BANK, CALENDAR, INFO, INFO_OUTLINE, WEBSITE, EMAIL, PHONE,
            "compute", "storage", "database", "network", "support");

    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private PlatformIcons() {
    }

    /**
     * The packaged mark a token names.
     *
     * @param token one of this preset's tokens
     * @return the parsed icon
     * @throws IllegalArgumentException when the token names no packaged mark,
     *         listing the ones a document may choose — an icon token is data, so
     *         a wrong one is a data error and not a missing resource
     */
    static SvgIcon icon(String token) {
        if (!TOKENS.contains(token)) {
            throw new IllegalArgumentException(
                    "Unknown platform invoice icon token '" + token
                            + "'. This preset packages " + LINE_TOKENS.stream().sorted().toList()
                            + " for a service line's mark.");
        }
        return CACHE.computeIfAbsent(token, PlatformIcons::read);
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = PlatformIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing platform invoice icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read platform invoice icon: " + resourcePath, e);
        }
    }
}
