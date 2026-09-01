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
 * Classpath loader for the Payments invoice marks: the two party glyphs, the
 * six a service line may take, and the four the preset places itself — the
 * bank on the payment card, the document on the notes, the calendar on the due
 * block and the headset on the support block.
 *
 * <p>They ship inside the templates artifact under
 * {@code templates/invoice/payments/icons/} as SVG, so they scale with the box
 * they are drawn into rather than being resampled. Parsed icons are cached per
 * token, so a sheet that repeats a mark reads each file once per JVM.</p>
 *
 * <h2>The set is coloured, not tinted</h2>
 *
 * <p>Each glyph carries its colour in its own markup, in one of the design's
 * two tints: ink for the marks that sit inside a lavender disc, accent for the
 * ones that stand on the page. A token therefore names a glyph <em>and</em> the
 * tint it is drawn in, which is why {@link #HEADSET} and {@link #SUPPORT} are
 * the same drawing under two names — one opens a service line inside a disc,
 * the other closes the sheet on the page.</p>
 *
 * <p>{@link #LINE_TOKENS} is the vocabulary a document names through
 * {@code InvoiceServiceLines.Line.icon()}. It is scoped to this preset —
 * another preset packages its own set.</p>
 */
final class PaymentsIcons {

    static final String BILL_TO = "bill-to";
    static final String SHIP_TO = "ship-to";
    static final String BANK = "bank";
    static final String DOCUMENT = "document";
    static final String CALENDAR = "calendar";
    static final String SUPPORT = "support";
    static final String HEADSET = "headset";

    private static final String ICON_ROOT = "/templates/invoice/payments/icons/";

    /** Every mark this preset packages. */
    private static final Set<String> TOKENS = Set.of(
            BILL_TO, SHIP_TO, BANK, DOCUMENT, CALENDAR, SUPPORT,
            "card", "card-settings", "shield", "globe", "mobile", HEADSET);

    /** The marks a document may name on a service line. */
    static final Set<String> LINE_TOKENS = Set.of(
            "card", "card-settings", "shield", "globe", "mobile", HEADSET);

    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private PaymentsIcons() {
    }

    /**
     * Reads one mark.
     *
     * @param token one of the packaged tokens
     * @return the parsed icon
     * @throws IllegalArgumentException when the token names no packaged mark,
     *         listing the ones a document may choose — an icon token is data,
     *         so a wrong one is a data error
     */
    static SvgIcon icon(String token) {
        if (!TOKENS.contains(token)) {
            throw new IllegalArgumentException(
                    "Unknown payments invoice icon token '" + token
                            + "'. This preset packages " + LINE_TOKENS.stream().sorted().toList()
                            + " for a service line's mark.");
        }
        return CACHE.computeIfAbsent(token, PaymentsIcons::read);
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = PaymentsIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing payments invoice icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read payments invoice icon: " + resourcePath, e);
        }
    }
}
