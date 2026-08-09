package com.demcha.compose.document.templates.receipt.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.identity.SvgGlyph;
import com.demcha.compose.document.templates.core.text.TextOrnaments;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.receipt.ReceiptData;
import com.demcha.compose.document.templates.data.receipt.ReceiptDocumentSpec;
import com.demcha.compose.document.templates.data.receipt.ReceiptFieldGroup;
import com.demcha.compose.document.templates.receipt.components.ReceiptStyles;
import com.demcha.compose.document.templates.receipt.widgets.AmountHero;
import com.demcha.compose.document.templates.receipt.widgets.DetailGroup;
import com.demcha.compose.document.templates.receipt.widgets.PartyPair;
import com.demcha.compose.document.templates.receipt.widgets.ReceiptFooter;
import com.demcha.compose.document.templates.receipt.widgets.ReceiptMasthead;
import com.demcha.compose.document.templates.receipt.widgets.StatusTrail;

import java.util.Objects;

/**
 * Modern Receipt — the reference preset of the layered {@code receipt}
 * family: a bank-grade transfer confirmation.
 *
 * <p>The page reads top to bottom in the order a reader asks the questions:
 * who issued this and what is it (masthead), how much and did it go through
 * (hero), between whom (party pair), on what terms (detail groups), how it
 * got there (status trail), and how to check it (footer QR + small
 * print).</p>
 *
 * <h2>What makes it a receipt rather than an invoice</h2>
 *
 * <p>An invoice asks for money and is built around a table of line items. A
 * receipt reports money that already moved and is built around one number
 * and its provenance — so the amount is set larger than the title, the
 * status carries a coloured chip, and the detail blocks are hairline
 * label/value rows rather than a bordered grid. Nothing on the page is
 * computed: every value arrives formatted from the issuing system.</p>
 *
 * <h2>Branding</h2>
 *
 * <p>The theme carries no brand colour. An issuer's mark and accent arrive
 * per document through {@link Options}, so one preset and one theme serve
 * every bank — swap the glyph and the accent and the same code renders a
 * different institution's confirmation.</p>
 *
 * <h2>Page chrome</h2>
 *
 * <p>{@code compose} installs a footer carrying {@code Page n of m}. A
 * receipt is a document people print, file, and photograph, and a page that
 * cannot say whether it is the whole story is a support call — so the
 * pagination marker is part of this preset rather than something each caller
 * remembers to add. It goes through the session's chrome API, which means a
 * caller who wants a different footer sets one after composing.</p>
 */
public final class ModernReceipt {

    /**
     * Stable template identifier.
     */
    public static final String ID = "receipt-modern";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Modern Receipt";

    /**
     * Recommended page margin (in points). Wider than the invoice preset's:
     * the layout leans on white space rather than on rules, and a tight
     * margin would undo that.
     */
    public static final double RECOMMENDED_MARGIN = 34.0;

    /** Height reserved for the page-number footer, in points. */
    private static final float FOOTER_HEIGHT = 24f;

    /**
     * Slack left unspent when the footer is pinned, in points. The measurement
     * is exact to the layout's own arithmetic; this keeps a rounding difference
     * from spilling the footer onto a second page.
     */
    private static final double PIN_EPSILON = 0.5;

    /**
     * Below this much free space, pinning is not worth a second composition
     * pass — the footer is already close enough to the bottom margin to read as
     * one.
     */
    private static final double MIN_PIN_HEIGHT = 8.0;

    private ModernReceipt() {
    }

    /**
     * Per-document branding and wording knobs.
     *
     * <p>These are structural rather than cosmetic in the theme's sense: two
     * receipts from two banks share every token in the
     * {@link BrandTheme#receiptModern()} theme and differ only in their mark
     * and their accent. Carrying them here keeps a per-issuer theme from
     * being the way to render a second bank.</p>
     *
     * @param logo          the issuer's mark, or {@code null} to fall back to
     *                      the issuer's name set as a spaced-caps wordmark
     * @param logoWidth     width of the mark in points; the height follows the
     *                      glyph's aspect ratio. Non-positive values fall back
     *                      to {@link #DEFAULT_LOGO_WIDTH}
     * @param logoColor     colour the mark is filled with, or {@code null} for
     *                      the theme's ink
     * @param accent        the issuer's brand accent — the hero strip, the
     *                      direction arrow, the reached timeline step. {@code
     *                      null} falls back to the theme's ink, which renders
     *                      an unbranded but complete receipt
     * @param timelineTitle heading over the status trail; blank falls back to
     *                      {@link #DEFAULT_TIMELINE_TITLE}
     */
    public record Options(SvgGlyph logo,
                          double logoWidth,
                          DocumentColor logoColor,
                          DocumentColor accent,
                          String timelineTitle) {

        /** Width the issuer's mark renders at when the caller gives none. */
        public static final double DEFAULT_LOGO_WIDTH = 92.0;

        /** Heading over the status trail when the caller gives none. */
        public static final String DEFAULT_TIMELINE_TITLE = "Payment progress";

        /**
         * Normalizes the width and the heading.
         */
        public Options {
            logoWidth = logoWidth > 0 ? logoWidth : DEFAULT_LOGO_WIDTH;
            timelineTitle = timelineTitle == null || timelineTitle.isBlank()
                    ? DEFAULT_TIMELINE_TITLE
                    : timelineTitle;
        }

        /**
         * Unbranded defaults — no mark, theme ink for the accent.
         *
         * @return default options
         */
        public static Options defaults() {
            return new Options(null, DEFAULT_LOGO_WIDTH, null, null, null);
        }

        /**
         * Defaults plus an issuer's mark and accent.
         *
         * @param logo   the issuer's mark
         * @param accent the issuer's brand accent
         * @return branded options
         */
        public static Options branded(SvgGlyph logo, DocumentColor accent) {
            return new Options(logo, DEFAULT_LOGO_WIDTH, null, accent, null);
        }

        /**
         * Returns a copy carrying a different mark width.
         *
         * @param width width in points
         * @return updated options
         */
        public Options withLogoWidth(double width) {
            return new Options(logo, width, logoColor, accent, timelineTitle);
        }

        /**
         * Returns a copy carrying a different mark colour.
         *
         * @param color fill colour for the mark
         * @return updated options
         */
        public Options withLogoColor(DocumentColor color) {
            return new Options(logo, logoWidth, color, accent, timelineTitle);
        }

        /**
         * Returns a copy carrying a different status-trail heading.
         *
         * @param title heading text
         * @return updated options
         */
        public Options withTimelineTitle(String title) {
            return new Options(logo, logoWidth, logoColor, accent, title);
        }
    }

    /**
     * Builds the preset with the Modern Receipt theme
     * ({@link BrandTheme#receiptModern()}) and unbranded options.
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<ReceiptDocumentSpec> create() {
        return create(BrandTheme.receiptModern(), Options.defaults());
    }

    /**
     * Builds the preset with a caller-supplied theme and unbranded options.
     *
     * @param theme active theme
     * @return ready-to-use template
     */
    public static DocumentTemplate<ReceiptDocumentSpec> create(BrandTheme theme) {
        return create(theme, Options.defaults());
    }

    /**
     * Builds the preset with a caller-supplied theme and branding.
     *
     * @param theme   active theme
     * @param options issuer mark, accent, and wording
     * @return ready-to-use template
     */
    public static DocumentTemplate<ReceiptDocumentSpec> create(BrandTheme theme, Options options) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme, options == null ? Options.defaults() : options);
    }

    private record Template(BrandTheme theme, Options options)
            implements DocumentTemplate<ReceiptDocumentSpec> {

        @Override
        public String id() {
            return ID;
        }

        @Override
        public String displayName() {
            return DISPLAY_NAME;
        }

        @Override
        public void compose(DocumentSession document, ReceiptDocumentSpec spec) {
            Objects.requireNonNull(document, "document");
            ReceiptData data = Objects.requireNonNull(spec, "spec").receipt();

            DocumentColor accent = options.accent() == null
                    ? theme.palette().ink()
                    : options.accent();
            DocumentColor markColor = options.logoColor() == null
                    ? theme.palette().ink()
                    : options.logoColor();

            // Whether this preset may pin its footer. The pin re-composes, which
            // means clearing the session — safe only when the receipt is the whole
            // document. A caller who composed a cover page first keeps it, and the
            // footer flows after the body instead. There is also nothing to pin
            // when the receipt carries no footer: the spacer would be a page-tall
            // hole holding nothing down.
            boolean ownsDocument = document.roots().isEmpty() && ReceiptFooter.hasContent(data);

            composeBody(document, data, accent, markColor, 0);

            if (ownsDocument) {
                double pin = bottomSlack(document) - theme.spacing().pageFlowSpacing() - PIN_EPSILON;
                if (pin > MIN_PIN_HEIGHT) {
                    document.clear();
                    composeBody(document, data, accent, markColor, pin);
                }
            }

            document.chrome().footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    .height(FOOTER_HEIGHT)
                    .rightText("Page {page} of {pages}")
                    .fontSize((float) theme.typography().sizeContact())
                    .textColor(theme.palette().muted())
                    .build());
        }

        /**
         * Points of unused page left under the composed content on the last page.
         *
         * <p>The engine has no vertical flex — a spacer grows inside a row, not
         * down a page — so the only way to seat a block on the bottom margin is
         * to measure what the body left and spend it. Layout runs on demand, so
         * reading the snapshot here costs one pass over content that is about to
         * be laid out anyway.</p>
         *
         * <p>Only nodes that both start and end on the last page are measured. A
         * node's {@code placementY} is resolved on the page it starts on, so a
         * container spanning a page break — the root flow of any receipt longer
         * than a page always does — would report a coordinate from a page whose
         * free space says nothing about the last one.</p>
         */
        private static double bottomSlack(DocumentSession document) {
            LayoutSnapshot snapshot = document.layoutSnapshot();
            int lastPage = snapshot.totalPages() - 1;
            double lowest = Double.MAX_VALUE;
            for (LayoutNodeSnapshot node : snapshot.nodes()) {
                if (node.startPage() == lastPage && node.endPage() == lastPage) {
                    lowest = Math.min(lowest, node.placementY());
                }
            }
            if (lowest == Double.MAX_VALUE) {
                return 0.0;
            }
            // Placement is measured from the page bottom, so the lowest edge minus
            // the bottom margin is exactly the gap the footer can fall through.
            return lowest - snapshot.canvas().margin().bottom();
        }

        /**
         * Sequences the blocks. Each {@code addSection} is guarded by whether
         * its block has anything to draw: a widget that renders nothing still
         * leaves a section behind, and an empty section still takes its turn in
         * the flow's spacing — which is how a receipt with no timeline ends up
         * with a timeline-sized hole in it.
         */
        private void composeBody(DocumentSession document, ReceiptData data,
                                 DocumentColor accent, DocumentColor markColor,
                                 double footerPin) {
            PageFlowBuilder flow = document.dsl().pageFlow()
                    .name("ReceiptModernRoot")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .addSection("ReceiptMasthead", section -> ReceiptMasthead.render(
                            section, data, options.logo(), options.logoWidth(), markColor, theme))
                    .addSection("ReceiptHero", section ->
                            AmountHero.render(section, data, accent, theme));

            if (data.hasPayer() || data.hasBeneficiary()) {
                flow.addSection("ReceiptParties", section ->
                        PartyPair.render(section, data, accent, theme));
            }

            for (ReceiptFieldGroup group : data.detailGroups()) {
                if (group.fields().isEmpty()) {
                    continue;
                }
                flow.addSection("ReceiptDetails", section ->
                        DetailGroup.render(section, group, theme));
            }

            if (!data.timeline().isEmpty()) {
                flow.addSection("ReceiptTrail", section -> StatusTrail.render(
                        section, options.timelineTitle(), data.timeline(), accent, theme));
            }
            if (!data.notes().isEmpty()) {
                flow.addSection("ReceiptNotes", section -> renderNotes(section, data));
            }

            if (footerPin > 0) {
                flow.addSpacer(spacer -> spacer.name("ReceiptFooterPin").height(footerPin));
            }

            if (ReceiptFooter.hasContent(data)) {
                flow.addSection("ReceiptFooter", section ->
                        ReceiptFooter.render(section, data, theme));
            }
            flow.build();
        }

        /**
         * Notes render inline rather than through {@code DetailGroup}: they are
         * prose, not label/value rows, so they take the body style and no
         * hairlines.
         */
        private void renderNotes(SectionBuilder host, ReceiptData data) {
            host.keepTogether()
                    .spacing(theme.spacing().sectionBodySpacing())
                    .addParagraph(p -> p
                            .text(TextOrnaments.spacedUpper("Notes"))
                            .textStyle(ReceiptStyles.groupTitle(theme))
                            .margin(new DocumentInsets(0, 0, 6, 0)))
                    .addParagraph(p -> p
                            .text(String.join("\n", data.notes()))
                            .textStyle(ReceiptStyles.body(theme))
                            .lineSpacing(theme.typography().bodyLineSpacing())
                            .margin(DocumentInsets.zero()));
        }
    }
}
