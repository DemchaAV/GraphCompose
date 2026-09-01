package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.svg.SvgIcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The marks the Subscription invoice packages.
 *
 * <p>All of them are the preset's, not the document's. Five open the payment
 * band's cells and one opens the closing band, and which glyph opens which is a
 * property of the design rather than of the invoice — a bank payment block's
 * cells are the same five roles wherever it is printed.</p>
 *
 * <p>The band's marks are drawn <em>in order</em>: the design is five marked
 * cells, so a payment block with more fields than that draws the extra ones
 * unmarked rather than repeating a glyph that would then mean nothing.</p>
 */
final class SubscriptionIcons {

    /** The payment band's marks, in the order the design's cells take them. */
    static final List<String> BAND =
            List.of("beneficiary", "bank", "transfer", "card", "reference");

    static final String SHIELD = "shield";

    private static final String ICON_ROOT = "/templates/invoice/subscription/icons/";

    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private SubscriptionIcons() {
    }

    /**
     * The mark the payment band's cell at {@code index} opens with, or
     * {@code null} when the design has no cell there.
     *
     * @param index the cell's position in the band
     * @return the icon, or {@code null} past the band's own five cells
     */
    static SvgIcon band(int index) {
        return index < 0 || index >= BAND.size() ? null : icon(BAND.get(index));
    }

    /**
     * A packaged mark by name.
     *
     * @param token the mark's name
     * @return the parsed icon
     */
    static SvgIcon icon(String token) {
        return CACHE.computeIfAbsent(token, SubscriptionIcons::read);
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = SubscriptionIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing subscription invoice icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read subscription invoice icon: " + resourcePath, e);
        }
    }
}
