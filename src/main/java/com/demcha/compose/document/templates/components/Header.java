package com.demcha.compose.document.templates.components;

import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.themes.Spacing;
import com.demcha.compose.document.theme.BusinessTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Templates v2 document header — name, contact info, and links rendered
 * at the top of a document.
 *
 * <p>Header is a stateless composer: configure it once with theme and
 * spacing, then call {@link #compose(Input)} for each spec. The
 * structure is fixed (name → contact → links), but the visual
 * placement is selected up-front via the style factory:</p>
 *
 * <ul>
 *   <li>{@link #rightAligned(BusinessTheme, Spacing)} — content
 *       right-justified at the top right of the page (Modern
 *       Professional pattern).</li>
 * </ul>
 *
 * <p>Additional placement variants (centered, left-banner, monogram)
 * will be added when their preset migrations begin in Phase E. The
 * overall design (a short list of named factories) keeps the API
 * predictable: a preset writer scans the factory list, picks the one
 * matching their layout, and is done.</p>
 */
public final class Header {

    private final TextAlign alignment;
    private final BusinessTheme theme;
    private final Spacing spacing;
    private final String namePrefix;

    private Header(TextAlign alignment, BusinessTheme theme, Spacing spacing) {
        this.alignment = alignment;
        this.theme = theme;
        this.spacing = spacing;
        this.namePrefix = "Header";
    }

    /**
     * Returns a header configured for right-aligned placement.
     *
     * <p>The name appears in the {@code h1} style of the active theme
     * with the contact line and links line stacked below it, all
     * flush with the right edge of the content area.</p>
     *
     * @param theme   active business theme
     * @param spacing active spacing tokens
     * @return new right-aligned header composer
     * @throws NullPointerException if either argument is null
     */
    public static Header rightAligned(BusinessTheme theme, Spacing spacing) {
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(spacing, "spacing");
        return new Header(TextAlign.RIGHT, theme, spacing);
    }

    /**
     * Composes the header into a single {@link DocumentNode}.
     *
     * @param input header input data
     * @return container node holding the rendered name, contact, and
     *         links rows
     * @throws NullPointerException if {@code input} is null
     */
    public DocumentNode compose(Input input) {
        Objects.requireNonNull(input, "input");

        List<DocumentNode> rows = new ArrayList<>(3);
        rows.add(nameRow(input.name()));

        String contact = joinPipe(input.contactItems());
        if (!contact.isEmpty()) {
            rows.add(contactRow(contact));
        }

        String links = joinPipe(input.linkLabels());
        if (!links.isEmpty()) {
            rows.add(linksRow(links));
        }

        return new ContainerNode(
                namePrefix,
                rows,
                /* spacing      */ 0.0,
                DocumentInsets.zero(),
                DocumentInsets.zero(),
                /* fillColor    */ null,
                /* stroke       */ null,
                /* cornerRadius */ null,
                /* borders      */ null);
    }

    private ParagraphNode nameRow(String name) {
        return new ParagraphNode(
                namePrefix + ".name",
                name,
                null,
                theme.text().h1(),
                alignment,
                /* lineSpacing    */ 0.0,
                /* bulletOffset   */ "",
                /* indentStrategy */ null,
                /* link           */ null,
                /* bookmark       */ null,
                /* padding        */ DocumentInsets.zero(),
                /* margin         */ new DocumentInsets(
                        0.0, 0.0, spacing.headerLineSpacing(), 0.0),
                /* autoSize       */ null);
    }

    private ParagraphNode contactRow(String contact) {
        return new ParagraphNode(
                namePrefix + ".contact",
                contact,
                null,
                theme.text().caption(),
                alignment,
                0.0,
                "",
                null,
                null,
                null,
                DocumentInsets.zero(),
                new DocumentInsets(0.0, 0.0, spacing.headerLineSpacing(), 0.0),
                null);
    }

    private ParagraphNode linksRow(String links) {
        return new ParagraphNode(
                namePrefix + ".links",
                links,
                null,
                theme.text().caption(),
                alignment,
                0.0,
                "",
                null,
                null,
                null,
                DocumentInsets.zero(),
                DocumentInsets.zero(),
                null);
    }

    private static String joinPipe(List<String> parts) {
        if (parts == null || parts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(part.trim());
        }
        return sb.toString();
    }

    /**
     * Header input data — the user-facing identity block carried by a
     * {@code CvSpec} or similar parent spec.
     *
     * @param name         document subject's name (required, non-blank)
     * @param contactItems contact-row items joined with " | " separators
     *                     (typically address, phone); null or blank
     *                     items are skipped
     * @param linkLabels   link-row labels joined with " | " separators
     *                     (typically email, LinkedIn, GitHub); null or
     *                     blank items are skipped
     */
    public record Input(String name, List<String> contactItems, List<String> linkLabels) {

        /**
         * Compact constructor that defensively copies the supplied
         * lists and rejects a null name.
         *
         * @throws NullPointerException if {@code name} is null
         */
        public Input {
            Objects.requireNonNull(name, "name");
            contactItems = contactItems == null ? List.of() : List.copyOf(contactItems);
            linkLabels = linkLabels == null ? List.of() : List.copyOf(linkLabels);
        }
    }
}
