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
 * The marks the Merchant invoice packages.
 *
 * <p>Seven the preset chooses itself — the two party discs, the bank panel, its
 * note, the due-by card, the closing note, and the three contact channels. The
 * other four are a vocabulary a document may name on a service line through
 * {@code InvoiceServiceLines.Line.icon()}.</p>
 */
final class MerchantIcons {

    static final String BILL_TO = "bill-to";
    static final String SHIP_TO = "ship-to";
    static final String BANK = "bank";
    static final String CALENDAR = "calendar";
    static final String INFO = "info";
    static final String WEBSITE = "website";
    static final String EMAIL = "email";
    static final String PHONE = "phone";

    private static final String ICON_ROOT = "/templates/invoice/merchant/icons/";

    /** The marks a document may name on a service line. */
    static final Set<String> LINE_TOKENS = Set.of("bag", "tag", "globe", "support");

    private static final Set<String> TOKENS = Set.of(
            BILL_TO, SHIP_TO, BANK, CALENDAR, INFO, WEBSITE, EMAIL, PHONE,
            "bag", "tag", "globe", "support");

    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private MerchantIcons() {
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
                    "Unknown merchant invoice icon token '" + token
                            + "'. This preset packages " + LINE_TOKENS.stream().sorted().toList()
                            + " for a service line's mark.");
        }
        return CACHE.computeIfAbsent(token, MerchantIcons::read);
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = MerchantIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing merchant invoice icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read merchant invoice icon: " + resourcePath, e);
        }
    }
}
