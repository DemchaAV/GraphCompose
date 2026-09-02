package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.node.DocumentLinkOptions;

import java.util.regex.Pattern;

/**
 * Turns a printed contact detail into something a reader can act on.
 *
 * <p>A proposal prints a number, an address and a site the way a person reads
 * them, and a reader's device needs the form it can dial, mail or open. The
 * conversion lives in one place rather than beside every block that needs it.</p>
 */
final class ProposalUri {

    /**
     * A trunk prefix in a printed number is for a domestic dialler and is not
     * part of the international one — {@code +44 (0)20 7946 0832} dials
     * {@code +442079460832}. Left in, the digits run together into a number that
     * reaches nobody. A parenthesised <em>area code</em> is not a trunk prefix,
     * which is why only an all-zero group goes.
     */
    private static final Pattern TRUNK_PREFIX = Pattern.compile("\\(0+\\)");

    private ProposalUri() {
    }

    /**
     * A printed telephone number as something a reader can dial.
     *
     * @param phone the number as the sheet prints it
     * @return the link options, or {@code null} when the text carries no digits
     */
    static DocumentLinkOptions telLink(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String dialled = TRUNK_PREFIX.matcher(phone).replaceAll("");
        String digits = dialled.replaceAll("[^0-9]", "");
        return digits.isEmpty()
                ? null
                : new DocumentLinkOptions(
                        "tel:" + (phone.trim().startsWith("+") ? "+" : "") + digits);
    }

    /**
     * An address as something a reader can write to.
     *
     * @param email the address as the sheet prints it
     * @return the link options, or {@code null} when there is no address, or
     *         none that could be made from what was printed
     */
    static DocumentLinkOptions mailLink(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        try {
            return new DocumentLinkOptions("mailto:" + email.trim());
        } catch (IllegalArgumentException notAUri) {
            return null;
        }
    }

    /**
     * A written site as something a reader can follow. A sheet prints
     * {@code example.com}; a viewer needs a scheme in front of it.
     *
     * <p>Not everything a document prints where a site would go is one, and a
     * string that cannot be a URI is printed as it stands rather than refused:
     * a link is an affordance, and losing it is not worth failing to render the
     * page over.</p>
     *
     * @param website the site as the sheet prints it
     * @return the link options, or {@code null} when there is no site, or none
     *         that could be made from what was printed
     */
    static DocumentLinkOptions webLink(String website) {
        if (website == null || website.isBlank()) {
            return null;
        }
        String trimmed = website.trim();
        String target = trimmed.startsWith("http://") || trimmed.startsWith("https://")
                ? trimmed
                : "https://" + trimmed;
        try {
            return new DocumentLinkOptions(target);
        } catch (IllegalArgumentException notAUri) {
            return null;
        }
    }

    /**
     * The link a printed contact string wants, guessed from its own shape.
     *
     * <p>A proposal's foot lists its channels as plain strings — a site, an
     * address, a number — without saying which is which, because a reader can
     * see it. This reads the same thing back: an address has an at-sign, a
     * number starts with a plus or is mostly digits, and anything else is a
     * site.</p>
     *
     * @param contact the channel as the sheet prints it
     * @return the link options, or {@code null} when nothing can be made of it
     */
    static DocumentLinkOptions channelLink(String contact) {
        if (contact == null || contact.isBlank()) {
            return null;
        }
        String trimmed = contact.trim();
        if (trimmed.contains("@")) {
            return mailLink(trimmed);
        }
        String digits = trimmed.replaceAll("[^0-9]", "");
        if (trimmed.startsWith("+") || digits.length() >= 9 && digits.length() >= trimmed.length() / 2) {
            return telLink(trimmed);
        }
        return webLink(trimmed);
    }
}
