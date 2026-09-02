package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.node.DocumentLinkOptions;

import java.util.regex.Pattern;

/**
 * Turns a printed contact detail into something a reader can act on.
 *
 * <p>A sheet prints a number, an address and a site the way a person reads
 * them, and a reader's device needs the form it can dial, mail or open. The two
 * are not the same string, so the conversion lives in one place rather than
 * beside every block on every invoice preset that needs it.</p>
 */
final class InvoiceUri {

    /**
     * A trunk prefix in a printed number is for a domestic dialler and is not
     * part of the international one — {@code +44 (0)20 7946 0832} dials
     * {@code +442079460832}. Left in, the digits run together into a number that
     * reaches nobody. A parenthesised <em>area code</em> is not a trunk prefix,
     * which is why only an all-zero group goes.
     */
    private static final Pattern TRUNK_PREFIX = Pattern.compile("\\(0+\\)");

    private InvoiceUri() {
    }

    /**
     * A printed telephone number as something a reader can dial.
     *
     * @param phone the number as the sheet prints it
     * @return a {@code tel:} target, or {@code null} when the text carries no
     *         digits to dial
     */
    private static String tel(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String dialled = TRUNK_PREFIX.matcher(phone).replaceAll("");
        String digits = dialled.replaceAll("[^0-9]", "");
        return digits.isEmpty()
                ? null
                : "tel:" + (phone.trim().startsWith("+") ? "+" : "") + digits;
    }

    /**
     * The same, ready to hand to a component that takes a link or nothing.
     *
     * @param phone the number as the sheet prints it
     * @return the link options, or {@code null} when there is nothing to dial
     */
    static DocumentLinkOptions telLink(String phone) {
        String uri = tel(phone);
        return uri == null ? null : new DocumentLinkOptions(uri);
    }

    /**
     * An address as something a reader can write to.
     *
     * @param email the address as the sheet prints it
     * @return the link options, or {@code null} when there is no address
     */
    static DocumentLinkOptions mailLink(String email) {
        return email == null || email.isBlank()
                ? null
                : new DocumentLinkOptions("mailto:" + email.trim());
    }

    /**
     * A written site as something a reader can follow. A sheet prints
     * {@code example.com}; a viewer needs a scheme in front of it.
     *
     * @param website the site as the sheet prints it
     * @return the link options, or {@code null} when there is no site
     */
    static DocumentLinkOptions webLink(String website) {
        if (website == null || website.isBlank()) {
            return null;
        }
        String trimmed = website.trim();
        return new DocumentLinkOptions(
                trimmed.startsWith("http://") || trimmed.startsWith("https://")
                        ? trimmed
                        : "https://" + trimmed);
    }
}
