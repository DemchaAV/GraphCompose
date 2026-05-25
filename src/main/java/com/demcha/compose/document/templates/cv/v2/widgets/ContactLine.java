package com.demcha.compose.document.templates.cv.v2.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.data.CvContact;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.CvLink;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Contact-line widget — the pipe-separated phone / email / address /
 * optional links row that sits under the {@link Headline}.
 *
 * <h2>Variants</h2>
 *
 * <ul>
 *   <li>{@link #centered} — centred line, phone → email → address →
 *       links. Used by classic / editorial presets that center the
 *       whole header block.</li>
 *   <li>{@link #rightAligned} — right-aligned line, address → phone
 *       → email → links. Used by modern presets where the header
 *       sits flush right next to the name.</li>
 *   <li>{@link #rightAlignedStacked} — right-aligned vertical stack,
 *       one contact item per line. Used by clean / sidebar presets
 *       where contact metadata forms a quiet top-right rail.</li>
 *   <li>{@link #leftAligned} — left-aligned slash/pipe-separated
 *       command-bar row. Used by compact presets with dark headers.</li>
 * </ul>
 *
 * <p>Email is always rendered as a clickable {@code mailto:} link;
 * each optional {@link CvLink} becomes a clickable hyperlink with
 * the {@code label} as the visible text. The separator glyph comes
 * from {@code theme.decoration().contactSeparator()}.</p>
 */
public final class ContactLine {

    private ContactLine() {
    }

    /**
     * Centred pipe-separated contact row. Order: phone → email →
     * address → links. Visual signature of {@code BoxedSections},
     * {@code MinimalUnderlined}.
     */
    public static void centered(SectionBuilder host, CvIdentity identity, CvTheme theme) {
        render(host, identity, theme, TextAlign.CENTER, Order.PHONE_FIRST);
    }

    /**
     * Centred contact row with explicit text-style overrides for
     * non-link text, clickable links, and separators. Same ordering as
     * {@link #centered(SectionBuilder, CvIdentity, CvTheme)}, but lets
     * editorial presets tint / underline the links without forking the
     * contact assembly logic.
     *
     * @param bodyStyleOverride      style for phone and address;
     *                               {@code null} →
     *                               {@code theme.contactStyle()}
     * @param linkStyleOverride      style for email + every link;
     *                               {@code null} →
     *                               {@code theme.contactStyle()}
     * @param separatorStyleOverride style for the separator glyph;
     *                               {@code null} →
     *                               {@code theme.contactSeparatorStyle()}
     */
    public static void centered(SectionBuilder host, CvIdentity identity,
                                CvTheme theme,
                                DocumentTextStyle bodyStyleOverride,
                                DocumentTextStyle linkStyleOverride,
                                DocumentTextStyle separatorStyleOverride) {
        renderStyled(host, identity, theme, TextAlign.CENTER, Order.PHONE_FIRST,
                bodyStyleOverride, linkStyleOverride, separatorStyleOverride);
    }

    /**
     * Right-aligned pipe-separated contact row. Order: address →
     * phone → email → links — address-first reads as the location
     * label authors usually put first in this style.
     */
    public static void rightAligned(SectionBuilder host, CvIdentity identity, CvTheme theme) {
        render(host, identity, theme, TextAlign.RIGHT, Order.ADDRESS_FIRST);
    }

    /**
     * Left-aligned contact row. Order: address -> phone -> email ->
     * links. Useful for command-bar headers where the name and
     * contact metadata both anchor to the left edge.
     */
    public static void leftAligned(SectionBuilder host, CvIdentity identity,
                                   CvTheme theme) {
        render(host, identity, theme, TextAlign.LEFT, Order.ADDRESS_FIRST);
    }

    /**
     * Left-aligned contact row with explicit text-style overrides for
     * non-link text, clickable links, and separators.
     *
     * @param bodyStyleOverride      style for address and phone;
     *                               {@code null} ->
     *                               {@code theme.contactStyle()}
     * @param linkStyleOverride      style for email + every link;
     *                               {@code null} -> resolved body
     *                               style
     * @param separatorStyleOverride style for the separator glyph;
     *                               {@code null} ->
     *                               {@code theme.contactSeparatorStyle()}
     */
    public static void leftAligned(SectionBuilder host, CvIdentity identity,
                                   CvTheme theme,
                                   DocumentTextStyle bodyStyleOverride,
                                   DocumentTextStyle linkStyleOverride,
                                   DocumentTextStyle separatorStyleOverride) {
        renderStyled(host, identity, theme, TextAlign.LEFT, Order.ADDRESS_FIRST,
                bodyStyleOverride, linkStyleOverride, separatorStyleOverride);
    }

    /**
     * Right-aligned contact split across <strong>two stacked
     * lines</strong> with explicit text-style overrides. Used by
     * {@code ModernProfessional} for its 2-row contact header where
     * email + links sit on a dedicated second row and use a custom
     * link colour the theme doesn't carry.
     *
     * <p>Row layout:</p>
     * <ul>
     *   <li><strong>Row 1</strong> — address {@code |} phone</li>
     *   <li><strong>Row 2</strong> — email {@code |} link₁ {@code |} link₂ … (all clickable)</li>
     * </ul>
     *
     * <p>Email and every {@link CvLink} are rendered as proper PDF
     * hyperlinks (mailto: for the email, the link's URL for each
     * label) — not just styled text.</p>
     *
     * @param bodyStyleOverride      style for the non-link items
     *                               (address, phone); {@code null} →
     *                               {@code theme.contactStyle()}
     * @param linkStyleOverride      style for email + every link;
     *                               {@code null} →
     *                               {@code theme.contactStyle()}
     * @param separatorStyleOverride style for the {@code " | "} pipe
     *                               separator; {@code null} →
     *                               {@code theme.contactSeparatorStyle()}
     */
    public static void twoRowRightAligned(SectionBuilder host, CvIdentity identity,
                                          CvTheme theme,
                                          DocumentTextStyle bodyStyleOverride,
                                          DocumentTextStyle linkStyleOverride,
                                          DocumentTextStyle separatorStyleOverride) {
        DocumentTextStyle bodyStyle = bodyStyleOverride != null
                ? bodyStyleOverride : theme.contactStyle();
        DocumentTextStyle linkStyle = linkStyleOverride != null
                ? linkStyleOverride : theme.contactStyle();
        DocumentTextStyle separatorStyle = separatorStyleOverride != null
                ? separatorStyleOverride : theme.contactSeparatorStyle();

        CvContact c = identity.contact();
        host.spacing(0).padding(theme.spacing().contactPadding())
                // Row 1 — address + phone.
                .addParagraph(p -> p
                        .textStyle(bodyStyle)
                        .align(TextAlign.RIGHT)
                        .margin(DocumentInsets.zero())
                        .rich(rich -> {
                            rich.style(c.address(), bodyStyle);
                            rich.style(" | ", separatorStyle);
                            rich.style(c.phone(), bodyStyle);
                        }))
                // Row 2 — email + every link, all clickable.
                .addParagraph(p -> p
                        .textStyle(bodyStyle)
                        .align(TextAlign.RIGHT)
                        .margin(DocumentInsets.zero())
                        .rich(rich -> {
                            rich.with(c.email(), linkStyle,
                                    new DocumentLinkOptions("mailto:" + c.email()));
                            for (CvLink l : identity.links()) {
                                rich.style(" | ", separatorStyle);
                                rich.with(l.label(), linkStyle,
                                        new DocumentLinkOptions(l.url()));
                            }
                        }));
    }

    /**
     * Right-aligned contact stack, one item per line. Order:
     * address → phone → email → links. This is useful for two-column
     * headers where a pipe-separated row would become too wide.
     */
    public static void rightAlignedStacked(SectionBuilder host,
                                           CvIdentity identity,
                                           CvTheme theme) {
        rightAlignedStacked(host, identity, theme, null, null);
    }

    /**
     * Right-aligned contact stack with explicit text-style overrides
     * for non-link text and clickable links.
     *
     * @param bodyStyleOverride style for address / phone; {@code null}
     *                          → {@code theme.contactStyle()}
     * @param linkStyleOverride style for email + links; {@code null}
     *                          → resolved body style
     */
    public static void rightAlignedStacked(SectionBuilder host,
                                           CvIdentity identity,
                                           CvTheme theme,
                                           DocumentTextStyle bodyStyleOverride,
                                           DocumentTextStyle linkStyleOverride) {
        DocumentTextStyle bodyStyle = bodyStyleOverride != null
                ? bodyStyleOverride : theme.contactStyle();
        DocumentTextStyle linkStyle = linkStyleOverride != null
                ? linkStyleOverride : bodyStyle;

        host.spacing(1.5).padding(theme.spacing().contactPadding());
        for (Part part : parts(identity, Order.ADDRESS_FIRST)) {
            if (part.text().isBlank()) {
                continue;
            }
            host.addParagraph(p -> p
                    .textStyle(part.link() == null ? bodyStyle : linkStyle)
                    .align(TextAlign.RIGHT)
                    .margin(DocumentInsets.zero())
                    .rich(rich -> {
                        if (part.link() == null) {
                            rich.style(part.text(), bodyStyle);
                        } else {
                            rich.with(part.text(), linkStyle, part.link());
                        }
                    }));
        }
    }

    /**
     * Lower-level entry. Pick the alignment and the field order
     * explicitly.
     */
    public static void render(SectionBuilder host, CvIdentity identity, CvTheme theme,
                              TextAlign alignment, Order order) {
        List<Part> parts = parts(identity, order);
        DocumentTextStyle textStyle = theme.contactStyle();
        DocumentTextStyle separatorStyle = theme.contactSeparatorStyle();
        String separator = theme.decoration().contactSeparator();

        host.spacing(0)
                .padding(theme.spacing().contactPadding())
                .addParagraph(p -> p
                        .textStyle(textStyle)
                        .align(alignment)
                        .margin(DocumentInsets.zero())
                        .rich(rich -> {
                            for (int i = 0; i < parts.size(); i++) {
                                Part part = parts.get(i);
                                if (part.link() != null) {
                                    rich.link(part.text(), part.link());
                                } else {
                                    rich.style(part.text(), textStyle);
                                }
                                if (i < parts.size() - 1) {
                                    rich.style(separator, separatorStyle);
                                }
                            }
                        }));
    }

    private static void renderStyled(SectionBuilder host, CvIdentity identity,
                                     CvTheme theme, TextAlign alignment,
                                     Order order,
                                     DocumentTextStyle bodyStyleOverride,
                                     DocumentTextStyle linkStyleOverride,
                                     DocumentTextStyle separatorStyleOverride) {
        List<Part> parts = parts(identity, order);
        DocumentTextStyle bodyStyle = bodyStyleOverride != null
                ? bodyStyleOverride : theme.contactStyle();
        DocumentTextStyle linkStyle = linkStyleOverride != null
                ? linkStyleOverride : bodyStyle;
        DocumentTextStyle separatorStyle = separatorStyleOverride != null
                ? separatorStyleOverride : theme.contactSeparatorStyle();
        String separator = theme.decoration().contactSeparator();

        host.spacing(0)
                .padding(theme.spacing().contactPadding())
                .addParagraph(p -> p
                        .textStyle(bodyStyle)
                        .align(alignment)
                        .margin(DocumentInsets.zero())
                        .rich(rich -> {
                            for (int i = 0; i < parts.size(); i++) {
                                Part part = parts.get(i);
                                if (part.link() != null) {
                                    rich.with(part.text(), linkStyle, part.link());
                                } else {
                                    rich.style(part.text(), bodyStyle);
                                }
                                if (i < parts.size() - 1) {
                                    rich.style(separator, separatorStyle);
                                }
                            }
                        }));
    }

    /** Field order in the rendered line. */
    public enum Order {
        /** phone → email → address → links. */
        PHONE_FIRST,
        /** address → phone → email → links. */
        ADDRESS_FIRST
    }

    private static List<Part> parts(CvIdentity identity, Order order) {
        CvContact c = identity.contact();
        List<Part> parts = new ArrayList<>(4 + identity.links().size());
        DocumentLinkOptions email = new DocumentLinkOptions("mailto:" + c.email());
        switch (order) {
            case PHONE_FIRST -> {
                parts.add(new Part(c.phone(), null));
                parts.add(new Part(c.email(), email));
                parts.add(new Part(c.address(), null));
            }
            case ADDRESS_FIRST -> {
                parts.add(new Part(c.address(), null));
                parts.add(new Part(c.phone(), null));
                parts.add(new Part(c.email(), email));
            }
        }
        for (CvLink link : identity.links()) {
            parts.add(new Part(link.label(), new DocumentLinkOptions(link.url())));
        }
        return parts;
    }

    private record Part(String text, DocumentLinkOptions link) {
    }
}
