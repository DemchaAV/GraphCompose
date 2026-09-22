package com.demcha.compose.document.backend.semantic.docx;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The name Word will accept for one of the document's own anchors.
 *
 * <p>A document names its anchors to suit itself — {@code "Terms & conditions"},
 * {@code "section-2"}, a heading's whole sentence. Word accepts far less: a bookmark name
 * starts with a letter, carries only letters, digits and underscores, and stops at 40
 * characters. Written through unchanged, a name Word dislikes is not a broken link but a
 * file Word refuses to open, and a name truncated twice in two places is a link that points
 * at nothing.</p>
 *
 * <p>So the mapping is made once and remembered: the same anchor asked for twice gets the
 * same name, and two different anchors that clean to the same text get different ones. That
 * is the whole reason this is an object rather than a static method — a link and the
 * bookmark it points at are written at different times, by different parts of the export,
 * and they have to agree.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxBookmarkNames {

    /** {@code ST_String} caps a bookmark name here; Word enforces it on open. */
    private static final int MAX_LENGTH = 40;

    private final Map<String, String> byAnchor = new HashMap<>();
    private final Map<String, Integer> collisions = new HashMap<>();
    private int nextId;

    /**
     * The Word name for an anchor, the same one every time it is asked for.
     *
     * @param anchor the document's own anchor name
     * @return a name Word accepts, or null when the anchor is empty
     */
    String nameFor(String anchor) {
        if (anchor == null || anchor.isBlank()) {
            return null;
        }
        return byAnchor.computeIfAbsent(anchor, this::mint);
    }

    /** @return the next bookmark id; Word wants them distinct within the document */
    int nextId() {
        return nextId++;
    }

    private String mint(String anchor) {
        StringBuilder cleaned = new StringBuilder(anchor.length());
        for (int index = 0; index < anchor.length() && cleaned.length() < MAX_LENGTH; index++) {
            char character = anchor.charAt(index);
            if (Character.isLetterOrDigit(character) || character == '_') {
                cleaned.append(character);
            } else if (cleaned.length() > 0 && cleaned.charAt(cleaned.length() - 1) != '_') {
                // One separator for a run of them, so "Terms & conditions" does not become
                // "Terms___conditions" and spend its 40 characters on punctuation.
                cleaned.append('_');
            }
        }
        while (cleaned.length() > 0 && cleaned.charAt(cleaned.length() - 1) == '_') {
            cleaned.setLength(cleaned.length() - 1);
        }
        if (cleaned.length() == 0 || !Character.isLetter(cleaned.charAt(0))) {
            // A name starting with a digit or emptied by cleaning is not a name Word takes.
            cleaned.insert(0, "anchor_");
        }
        String base = cleaned.length() > MAX_LENGTH ? cleaned.substring(0, MAX_LENGTH) : cleaned.toString();

        // Two anchors that clean to the same text are still two anchors. Without this the
        // second one's bookmark would sit on the first one's name and every link to either
        // would land in the same place.
        String key = base.toLowerCase(Locale.ROOT);
        int seen = collisions.merge(key, 1, Integer::sum);
        if (seen == 1) {
            return base;
        }
        String suffix = "_" + seen;
        String trimmed = base.length() + suffix.length() > MAX_LENGTH
                ? base.substring(0, MAX_LENGTH - suffix.length())
                : base;
        return trimmed + suffix;
    }
}
