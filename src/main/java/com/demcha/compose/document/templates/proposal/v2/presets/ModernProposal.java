package com.demcha.compose.document.templates.proposal.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.proposal.ProposalData;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import com.demcha.compose.document.templates.data.proposal.ProposalParty;
import com.demcha.compose.document.templates.data.proposal.ProposalPricingRow;
import com.demcha.compose.document.templates.data.proposal.ProposalSection;
import com.demcha.compose.document.templates.data.proposal.ProposalTimelineItem;

import java.util.List;
import java.util.Objects;

/**
 * Modern Proposal — the first layered proposal preset.
 *
 * <p>It composes a proposal on a
 * {@link com.demcha.compose.document.templates.core.theme.BrandTheme}
 * plus the canonical DSL, mirroring the {@code invoice.v2} preset shape:
 * a {@code create(BrandTheme)} factory returns a thin
 * {@link DocumentTemplate} whose {@code compose} sequences a hero panel,
 * an executive-summary panel, the FROM / TO parties, the body sections,
 * the timeline + pricing tables, the acceptance terms, and a footer. The
 * visual intent is the cinematic "modern business" proposal look, fully
 * theme-driven: the hero, summary, party labels, section headings, table
 * headers, totals, and footer read their colours / fonts / sizes from the
 * {@code BrandTheme}. The table body cells intentionally inherit the DSL
 * default table-cell text — see {@code compose}.</p>
 *
 * <p><strong>Why the parties render inline rather than through
 * {@code core.identity.PartyIdentity}:</strong> a proposal carries two
 * parties (sender + recipient) shown as side-by-side blocks, and a
 * {@link ProposalParty}'s email / phone / website are optional, whereas
 * the shared {@code Contact} / {@code Masthead} widgets model a single
 * mandatory contact triple for a one-person CV masthead. The two-party
 * inline layout is the proposal idiom, so the preset composes the party
 * blocks directly.</p>
 */
public final class ModernProposal {

    /**
     * Stable template identifier.
     */
    public static final String ID = "proposal-modern";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Modern Proposal";

    /**
     * Recommended page margin (in points).
     */
    public static final double RECOMMENDED_MARGIN = 28.0;

    private static final double TABLE_PADDING = 7.0;

    /**
     * Deep teal used for the proposal title, section headings, and table
     * header fills (the modern business primary). Preset-local — no other
     * v2 preset shares it today; promote to a {@link
     * com.demcha.compose.document.templates.core.theme.Palette} slot if a
     * second proposal preset reaches for it.
     */
    private static final DocumentColor PRIMARY = DocumentColor.rgb(20, 60, 75);

    /**
     * Gold accent for the hero accent strip and the acceptance-terms strip
     * (the modern business accent). Preset-local, same rationale as
     * {@link #PRIMARY}.
     */
    private static final DocumentColor ACCENT = DocumentColor.rgb(196, 153, 76);

    private ModernProposal() {
    }

    /**
     * Builds the preset with the Modern Proposal theme
     * ({@link BrandTheme#proposalModern()}).
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<ProposalDocumentSpec> create() {
        return create(BrandTheme.proposalModern());
    }

    /**
     * Builds the preset with a caller-supplied theme, so callers can vary
     * the proposal flavour (typography scale, palette) without forking
     * this class.
     *
     * @param theme active theme
     * @return ready-to-use template
     */
    public static DocumentTemplate<ProposalDocumentSpec> create(BrandTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private record Template(BrandTheme theme) implements DocumentTemplate<ProposalDocumentSpec> {

        @Override
        public String id() {
            return ID;
        }

        @Override
        public String displayName() {
            return DISPLAY_NAME;
        }

        @Override
        public void compose(DocumentSession document, ProposalDocumentSpec spec) {
            Objects.requireNonNull(document, "document");
            ProposalData data = Objects.requireNonNull(spec, "spec").proposal();

            DocumentColor panelFill = theme.palette().banner();   // soft tan
            DocumentColor rule = theme.palette().rule();
            DocumentColor surface = theme.palette().mainFill();    // cream

            // Title / section headings use the deep teal PRIMARY; the project
            // subtitle is the h3 ink heading; labels + body + the footer
            // caption read from the theme. Mirrors the cinematic builtin.
            DocumentTextStyle titleStyle = heading(theme.typography().sizeHeadline(), PRIMARY);
            DocumentTextStyle projectStyle = heading(theme.typography().sizeEntryTitle(),
                    theme.palette().ink());
            DocumentTextStyle sectionTitleStyle = heading(theme.typography().sizeBanner(), PRIMARY);
            DocumentTextStyle labelStyle = theme.bodyBoldStyle();
            DocumentTextStyle bodyStyle = theme.bodyStyle();
            DocumentTextStyle captionStyle = DocumentTextStyle.builder()
                    .fontName(theme.typography().bodyFont())
                    .size(theme.typography().sizeEntrySubtitle())
                    .color(theme.palette().muted())
                    .build();

            DocumentTableStyle bordered = DocumentTableStyle.builder()
                    .stroke(DocumentStroke.of(rule, 0.6))
                    .padding(DocumentInsets.of(TABLE_PADDING))
                    .build();
            DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                    .fillColor(PRIMARY)
                    .stroke(DocumentStroke.of(rule, 0.6))
                    .padding(DocumentInsets.of(TABLE_PADDING + 1))
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(theme.typography().bodyFont())
                            .decoration(DocumentTextDecoration.BOLD)
                            // Header text is the 11pt body size — NOT sizeBanner(),
                            // which in proposalModern() is the 17pt h2 section-heading
                            // slot. (ModernInvoice uses sizeBanner() because its banner
                            // slot is 11pt; do not "align" the two — it breaks parity.)
                            .size(theme.typography().sizeBody())
                            .color(surface)
                            .build())
                    .build();
            DocumentTableStyle emphasizedRowStyle = DocumentTableStyle.builder()
                    .fillColor(panelFill)
                    .stroke(DocumentStroke.of(rule, 0.6))
                    .padding(DocumentInsets.of(TABLE_PADDING + 1))
                    .textStyle(theme.bodyBoldStyle())
                    .build();

            document.dsl().pageFlow()
                    .name("ProposalV2ModernRoot")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .addSection("ProposalHero", section -> section
                            .softPanel(panelFill,
                                    DocumentCornerRadius.right(theme.spacing().bannerCornerRadius()),
                                    theme.spacing().bannerInnerPadding())
                            .accentLeft(ACCENT, theme.spacing().accentRuleWidth())
                            .spacing(6)
                            .addParagraph(p -> p
                                    .text(data.title().isBlank() ? "Proposal" : data.title())
                                    .textStyle(titleStyle)
                                    .margin(DocumentInsets.zero()))
                            .addParagraph(p -> p
                                    .text(data.projectTitle())
                                    .textStyle(projectStyle)
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Proposal ").bold(data.proposalNumber())
                                    .plain("    Prepared ").bold(data.preparedDate())
                                    .plain("    Valid until ").bold(data.validUntil())))
                    .addSection("ProposalExecutiveSummary", section -> section
                            .softPanel(surface, 8, 12)
                            .stroke(DocumentStroke.of(rule, 0.6))
                            .spacing(4)
                            .addParagraph(p -> p
                                    .text("Executive summary")
                                    .textStyle(labelStyle)
                                    .margin(DocumentInsets.zero()))
                            .addParagraph(p -> p
                                    .text(data.executiveSummary())
                                    .textStyle(bodyStyle)
                                    .lineSpacing(1.35)
                                    .margin(DocumentInsets.zero())))
                    .addRow("ProposalParties", row -> row
                            .spacing(18)
                            .weights(1, 1)
                            .addSection("ProposalSender", col -> partyBlock(col, "FROM",
                                    data.sender(), labelStyle, bodyStyle))
                            .addSection("ProposalRecipient", col -> partyBlock(col, "TO",
                                    data.recipient(), labelStyle, bodyStyle)))
                    // Build the root page-flow so the title, executive summary, and
                    // FROM/TO parties are emitted before the section/timeline/pricing
                    // flows below — without this build() the root flow's content drops.
                    .build();

            for (ProposalSection section : data.sections()) {
                document.dsl().pageFlow()
                        .name("ProposalSectionGroup")
                        .spacing(4)
                        .addParagraph(p -> p
                                .text(section.title())
                                .textStyle(sectionTitleStyle)
                                .margin(new DocumentInsets(12, 0, 4, 0)))
                        .addSection("ProposalSectionBody", col -> {
                            for (String paragraph : section.paragraphs()) {
                                col.addParagraph(p -> p
                                        .text(paragraph)
                                        .textStyle(bodyStyle)
                                        .lineSpacing(1.35)
                                        .margin(DocumentInsets.zero()));
                            }
                        })
                        .build();
            }

            if (!data.timeline().isEmpty()) {
                document.dsl().pageFlow()
                        .name("ProposalTimelineGroup")
                        .spacing(4)
                        .addParagraph(p -> p
                                .text("Timeline")
                                .textStyle(sectionTitleStyle)
                                .margin(new DocumentInsets(12, 0, 4, 0)))
                        .addTable(table -> {
                            // The last column is auto-sized to its content (matching the
                            // cinematic builtin). Very long details/descriptions can push
                            // the table past the page's inner width and throw at render
                            // time — keep cell text concise, as the sample data does. The
                            // same applies to the pricing table below.
                            TableBuilder configured = table
                                    .name("ProposalTimeline")
                                    .columns(
                                            DocumentTableColumn.fixed(110),
                                            DocumentTableColumn.fixed(80),
                                            DocumentTableColumn.auto())
                                    .defaultCellStyle(bordered)
                                    .headerRow("Phase", "Duration", "Details")
                                    .headerStyle(headerStyle)
                                    .repeatHeader();
                            for (ProposalTimelineItem item : data.timeline()) {
                                configured.row(item.phase(), item.duration(), item.details());
                            }
                        })
                        .build();
            }

            if (!data.pricingRows().isEmpty()) {
                document.dsl().pageFlow()
                        .name("ProposalPricingGroup")
                        .spacing(4)
                        .addParagraph(p -> p
                                .text("Investment")
                                .textStyle(sectionTitleStyle)
                                .margin(new DocumentInsets(12, 0, 4, 0)))
                        .addTable(table -> {
                            TableBuilder configured = table
                                    .name("ProposalPricing")
                                    .columns(
                                            DocumentTableColumn.fixed(140),
                                            DocumentTableColumn.auto(),
                                            DocumentTableColumn.fixed(110))
                                    .defaultCellStyle(bordered)
                                    .headerRow("Item", "Description", "Amount")
                                    .headerStyle(headerStyle)
                                    .repeatHeader()
                                    .zebra(panelFill, surface);
                            // Convention (shared with the cinematic builtin): an
                            // emphasized LAST pricing row renders via totalRow so it
                            // picks up the emphasized fill + bold; others are plain.
                            List<ProposalPricingRow> rows = data.pricingRows();
                            for (int i = 0; i < rows.size(); i++) {
                                ProposalPricingRow item = rows.get(i);
                                if (item.emphasized() && i == rows.size() - 1) {
                                    configured.totalRow(emphasizedRowStyle,
                                            item.label(), item.description(), item.amount());
                                } else {
                                    configured.row(item.label(), item.description(), item.amount());
                                }
                            }
                        })
                        .build();
            }

            if (!data.acceptanceTerms().isEmpty()) {
                document.dsl().pageFlow()
                        .name("ProposalAcceptanceGroup")
                        .spacing(4)
                        .addParagraph(p -> p
                                .text("Acceptance terms")
                                .textStyle(sectionTitleStyle)
                                .margin(new DocumentInsets(12, 0, 4, 0)))
                        .addSection("ProposalAcceptanceBody", col -> col
                                .accentLeft(ACCENT, 3)
                                .padding(0, 0, 0, 8)
                                .addList(list -> list.items(data.acceptanceTerms())))
                        .build();
            }

            if (!data.footerNote().isBlank()) {
                document.dsl().pageFlow()
                        .name("ProposalV2ModernFooter")
                        .addParagraph(p -> p
                                .text(data.footerNote())
                                .textStyle(captionStyle)
                                .margin(new DocumentInsets(14, 0, 0, 0)))
                        .build();
            }
        }

        private DocumentTextStyle heading(double size, DocumentColor color) {
            return DocumentTextStyle.builder()
                    .fontName(theme.typography().headlineFont())
                    .size(size)
                    .decoration(DocumentTextDecoration.BOLD)
                    .color(color)
                    .build();
        }
    }

    private static void partyBlock(com.demcha.compose.document.dsl.SectionBuilder col,
                                   String label, ProposalParty party,
                                   DocumentTextStyle labelStyle, DocumentTextStyle bodyStyle) {
        col.spacing(2)
                .addParagraph(p -> p.text(label).textStyle(labelStyle).margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text(party.name()).textStyle(labelStyle).margin(DocumentInsets.zero()))
                .addParagraph(p -> p.text(joinAddress(party)).textStyle(bodyStyle)
                        .lineSpacing(1.3).margin(DocumentInsets.zero()));
    }

    private static String joinAddress(ProposalParty party) {
        StringBuilder builder = new StringBuilder();
        for (String line : party.addressLines()) {
            if (line == null || line.isBlank()) {
                continue;
            }
            append(builder, line);
        }
        if (!party.email().isBlank()) {
            append(builder, party.email());
        }
        if (!party.phone().isBlank()) {
            append(builder, party.phone());
        }
        if (!party.website().isBlank()) {
            append(builder, party.website());
        }
        return builder.toString();
    }

    private static void append(StringBuilder builder, String line) {
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(line);
    }
}
