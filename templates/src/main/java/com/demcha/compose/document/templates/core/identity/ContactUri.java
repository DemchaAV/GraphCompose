package com.demcha.compose.document.templates.core.identity;

import com.demcha.compose.document.node.DocumentLinkOptions;

import java.util.regex.Pattern;

/**
 * Turns a printed contact detail into something a reader can act on.
 *
 * <p>A document prints a number, an address and a site the way a person reads
 * them; a reader's device needs the form it can dial, mail or open. Every
 * template family needs that conversion and they all need the same one, so it
 * lives beside the rest of the neutral identity layer rather than three times
 * over in three families' presets.</p>
 *
 * <h2>A missing link is never a failed render</h2>
 *
 * <p>Every method here answers {@code null} rather than throwing: not
 * everything a document prints where a contact would go is one, and losing the
 * affordance is not worth failing to compose the page over. A caller passes the
 * result straight to a component that takes a link or nothing.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public final class ContactUri {

    /**
     * A trunk prefix in a printed number is for a domestic dialler and is not
     * part of the international one — {@code +44 (0)20 7946 0832} dials
     * {@code +442079460832}. Left in, the digits run together into a number that
     * reaches nobody. A parenthesised <em>area code</em> is not a trunk prefix,
     * which is why only an all-zero group goes.
     */
    private static final Pattern TRUNK_PREFIX = Pattern.compile("\\(0+\\)");

    private ContactUri() {
    }

    /**
     * A printed telephone number as a {@code tel:} target.
     *
     * @param phone the number as the sheet prints it
     * @return the target, or {@code null} when the text carries no digits to
     *         dial
     */
    public static String tel(String phone) {
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
    public static DocumentLinkOptions telLink(String phone) {
        return options(tel(phone));
    }

    /**
     * An address as something a reader can write to.
     *
     * @param email the address as the sheet prints it
     * @return the link options, or {@code null} when there is no address, or
     *         none that could be made from what was printed
     */
    public static DocumentLinkOptions mailLink(String email) {
        return email == null || email.isBlank()
                ? null
                : options("mailto:" + email.trim());
    }

    /**
     * A written site as something a reader can follow. A sheet prints
     * {@code example.com}; a viewer needs a scheme in front of it.
     *
     * @param website the site as the sheet prints it
     * @return the link options, or {@code null} when there is no site, or none
     *         that could be made from what was printed
     */
    public static DocumentLinkOptions webLink(String website) {
        if (website == null || website.isBlank()) {
            return null;
        }
        String trimmed = website.trim();
        return options(trimmed.startsWith("http://") || trimmed.startsWith("https://")
                ? trimmed
                : "https://" + trimmed);
    }

    /**
     * The link a printed contact string wants, guessed from its own shape.
     *
     * <p>A document's foot often lists its channels as plain strings — a site,
     * an address, a number — without saying which is which, because a reader can
     * see it. This reads the same thing back: an address has an at-sign, a
     * number starts with a plus or is mostly digits, and anything else is a
     * site.</p>
     *
     * <p>Reach for this only where the document really does not say. Where a
     * field is known to hold a number or an address, name it: a heuristic that
     * is right almost always is worse than a fact.</p>
     *
     * @param contact the channel as the sheet prints it
     * @return the link options, or {@code null} when nothing can be made of it
     */
    public static DocumentLinkOptions channelLink(String contact) {
        if (contact == null || contact.isBlank()) {
            return null;
        }
        String trimmed = contact.trim();
        if (trimmed.contains("@")) {
            return mailLink(trimmed);
        }
        String digits = trimmed.replaceAll("[^0-9]", "");
        boolean dialable = trimmed.startsWith("+")
                || digits.length() >= 9 && digits.length() >= trimmed.length() / 2;
        return dialable ? telLink(trimmed) : webLink(trimmed);
    }

    /**
     * The link a target makes, or nothing when it makes none.
     *
     * <p>{@link DocumentLinkOptions} refuses a string that is not a URI, and a
     * contact line is not the place to find that out: a notice, a name, a line
     * of prose can all reach a channel field, and none of them should stop the
     * page composing.</p>
     */
    private static DocumentLinkOptions options(String target) {
        try {
            return new DocumentLinkOptions(target);
        } catch (IllegalArgumentException notAUri) {
            return null;
        }
    }
}
