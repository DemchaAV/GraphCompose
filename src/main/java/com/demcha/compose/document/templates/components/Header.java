package com.demcha.compose.document.templates.components;

import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
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
    private final DocumentTextStyle nameStyleOverride;
    private final DocumentTextStyle contactStyleOverride;
    private final DocumentTextStyle linkStyleOverride;

    private Header(TextAlign alignment,
                   BusinessTheme theme,
                   Spacing spacing,
                   DocumentTextStyle nameStyleOverride,
                   DocumentTextStyle contactStyleOverride,
                   DocumentTextStyle linkStyleOverride) {
        this.alignment = alignment;
        this.theme = theme;
        this.spacing = spacing;
        this.namePrefix = "Header";
        this.nameStyleOverride = nameStyleOverride;
        this.contactStyleOverride = contactStyleOverride;
        this.linkStyleOverride = linkStyleOverride;
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
        return new Header(TextAlign.RIGHT, theme, spacing, null, null, null);
    }

    /**
     * Returns a copy of this header that uses {@code style} for the
     * subject's name, overriding the theme's {@code h1} default.
     *
     * @param style replacement name text style; pass {@code null} to
     *              clear an existing override
     * @return updated header
     */
    public Header withNameStyle(DocumentTextStyle style) {
        return new Header(alignment, theme, spacing, style, contactStyleOverride, linkStyleOverride);
    }

    /**
     * Returns a copy of this header that uses {@code style} for the
     * contact line (address / phone), overriding the theme's
     * {@code caption} default.
     *
     * @param style replacement contact text style; pass {@code null} to
     *              clear an existing override
     * @return updated header
     */
    public Header withContactStyle(DocumentTextStyle style) {
        return new Header(alignment, theme, spacing, nameStyleOverride, style, linkStyleOverride);
    }

    /**
     * Returns a copy of this header that uses {@code style} for each
     * link run (email / LinkedIn / GitHub / etc.), overriding the
     * paragraph-level fallback. Typical usage is an underlined accent
     * style so the link labels render visibly clickable.
     *
     * @param style replacement link text style; pass {@code null} to
     *              clear an existing override
     * @return updated header
     */
    public Header withLinkStyle(DocumentTextStyle style) {
        return new Header(alignment, theme, spacing, nameStyleOverride, contactStyleOverride, style);
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

        if (!input.links().isEmpty()) {
            rows.add(linksRow(input.links()));
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
        DocumentTextStyle style = nameStyleOverride != null
                ? nameStyleOverride
                : theme.text().h1();
        return new ParagraphNode(
                namePrefix + ".name",
                name,
                null,
                style,
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
        DocumentTextStyle style = contactStyleOverride != null
                ? contactStyleOverride
                : theme.text().caption();
        return new ParagraphNode(
                namePrefix + ".contact",
                contact,
                null,
                style,
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

    private ParagraphNode linksRow(List<Link> links) {
        // Build inline runs: for each non-blank link, emit one InlineTextRun
        // for the label (carrying its own DocumentLinkOptions when the URL is
        // non-blank, making the run a clickable hyperlink in PDF backends
        // that honour link metadata) and one plain run for the " | "
        // separator between links. When a link-style override is set, each
        // link label run receives that explicit style so the link reads
        // visibly clickable (typical preset choice: accent colour with
        // underline decoration); separators stay on the paragraph fallback.
        List<InlineRun> runs = new ArrayList<>(links.size() * 2);
        boolean separatorPending = false;
        for (Link link : links) {
            if (link == null || link.label() == null || link.label().isBlank()) {
                continue;
            }
            if (separatorPending) {
                runs.add(new InlineTextRun(" | ", null, null));
            }
            DocumentLinkOptions linkOptions = link.url() == null || link.url().isBlank()
                    ? null
                    : new DocumentLinkOptions(link.url().trim());
            runs.add(new InlineTextRun(link.label().trim(), linkStyleOverride, linkOptions));
            separatorPending = true;
        }
        DocumentTextStyle paragraphStyle = contactStyleOverride != null
                ? contactStyleOverride
                : theme.text().caption();
        return new ParagraphNode(
                namePrefix + ".links",
                /* text       */ "",
                /* inlineRuns */ runs,
                paragraphStyle,
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
     * One labelled link in the header links row. Empty {@code url}
     * means the label is rendered as plain text rather than a
     * clickable hyperlink — useful for visible-but-non-clickable
     * email addresses or social-handle labels.
     *
     * @param label visible label text (e.g. "alex@example.dev",
     *              "LinkedIn", "GitHub"); must not be null or blank
     * @param url   target URI with scheme (e.g. {@code https://...},
     *              {@code mailto:...}); empty / blank means no link
     */
    public record Link(String label, String url) {

        /**
         * Compact constructor that rejects null or blank labels and
         * normalises {@code null} url to empty.
         *
         * @throws NullPointerException     if {@code label} is null
         * @throws IllegalArgumentException if {@code label} is blank
         */
        public Link {
            Objects.requireNonNull(label, "label");
            if (label.isBlank()) {
                throw new IllegalArgumentException("label must not be blank");
            }
            url = url == null ? "" : url;
        }

        /**
         * Convenience factory for an inactive (non-clickable) link.
         *
         * @param label visible label text
         * @return link with empty URL
         */
        public static Link plain(String label) {
            return new Link(label, "");
        }

        /**
         * Convenience factory for an active hyperlink.
         *
         * @param label visible label text
         * @param url   target URI with scheme
         * @return active link
         */
        public static Link active(String label, String url) {
            return new Link(label, url);
        }
    }

    /**
     * Header input data — the user-facing identity block carried by a
     * {@code CvSpec} or similar parent spec.
     *
     * @param name         document subject's name (required, non-blank)
     * @param contactItems contact-row items joined with " | " separators
     *                     (typically address, phone); null or blank
     *                     items are skipped
     * @param links        link-row entries joined with " | " separators
     *                     (typically email, LinkedIn, GitHub); each
     *                     {@link Link} carries its label plus an
     *                     optional URL that becomes a clickable
     *                     hyperlink in PDF backends honouring link
     *                     metadata
     */
    public record Input(String name, List<String> contactItems, List<Link> links) {

        /**
         * Compact constructor that defensively copies the supplied
         * lists and rejects a null name.
         *
         * @throws NullPointerException if {@code name} is null
         */
        public Input {
            Objects.requireNonNull(name, "name");
            contactItems = contactItems == null ? List.of() : List.copyOf(contactItems);
            links = links == null ? List.of() : List.copyOf(links);
        }
    }
}
