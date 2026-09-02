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
 * The marks the Metered invoice packages.
 *
 * <p>Five of them the preset chooses itself — which glyph opens the bill-to
 * block, the ship-to block, the bank panel, the due-by card and the closing
 * note is a property of the design, not of the document. The other five are a
 * vocabulary a document may name on a service line through
 * {@code InvoiceServiceLines.Line.icon()}.</p>
 */
final class MeteredIcons {

    static final String BILL_TO = "bill-to";
    static final String SHIP_TO = "ship-to";
    static final String CARD = "card";
    static final String CALENDAR = "calendar";
    static final String INFO = "info";

    private static final String ICON_ROOT = "/templates/invoice/metered/icons/";

    /** The marks a document may name on a service line. */
    static final Set<String> LINE_TOKENS =
            Set.of("compute", "storage", "database", "transfer", "support");

    private static final Set<String> TOKENS = Set.of(BILL_TO, SHIP_TO, CARD, CALENDAR, INFO,
            "compute", "storage", "database", "transfer", "support");

    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private MeteredIcons() {
    }

    /**
     * The packaged mark a token names.
     *
     * @param token one of this preset's tokens
     * @return the parsed icon
     * @throws IllegalArgumentException when the token names no packaged mark,
     *         listing the ones a document may choose — an icon token is data,
     *         so a wrong one is a data error and not a missing resource
     */
    static SvgIcon icon(String token) {
        if (!TOKENS.contains(token)) {
            throw new IllegalArgumentException(
                    "Unknown metered invoice icon token '" + token
                            + "'. This preset packages " + LINE_TOKENS.stream().sorted().toList()
                            + " for a service line's mark.");
        }
        return CACHE.computeIfAbsent(token, MeteredIcons::read);
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = MeteredIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing metered invoice icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read metered invoice icon: " + resourcePath, e);
        }
    }
}
