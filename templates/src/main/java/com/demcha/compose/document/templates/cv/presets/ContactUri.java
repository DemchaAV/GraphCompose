package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.node.DocumentLinkOptions;

import java.util.regex.Pattern;

/**
 * Turns a printed contact detail into something a reader can act on.
 *
 * <p>A sheet prints a number the way a person reads it and a reader's device
 * dials the digits, and the two are not the same string — so the conversion
 * belongs in one place rather than beside every contact block that needs it.</p>
 */
final class ContactUri {

    /**
     * A trunk prefix in a printed number is for a domestic dialler and is not
     * part of the international one — {@code +44 (0)20 7946 0832} dials
     * {@code +442079460832}. Left in, the digits run together into a number
     * that reaches nobody. A parenthesised <em>area code</em> is not a trunk
     * prefix, which is why only an all-zero group goes.
     */
    private static final Pattern TRUNK_PREFIX = Pattern.compile("\\(0+\\)");

    private ContactUri() {
    }

    /**
     * A printed telephone number as something a reader can dial.
     *
     * @param phone the number as the sheet prints it
     * @return a {@code tel:} target, or {@code null} when the text carries no
     *         digits to dial
     */
    static String tel(String phone) {
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
}
