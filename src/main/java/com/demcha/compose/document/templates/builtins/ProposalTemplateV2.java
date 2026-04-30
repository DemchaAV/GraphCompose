package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.templates.api.ProposalTemplate;
import com.demcha.compose.document.templates.data.proposal.ProposalData;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import com.demcha.compose.document.templates.data.proposal.ProposalParty;
import com.demcha.compose.document.templates.data.proposal.ProposalPricingRow;
import com.demcha.compose.document.templates.data.proposal.ProposalSection;
import com.demcha.compose.document.templates.data.proposal.ProposalTimelineItem;
import com.demcha.compose.document.templates.support.common.TemplateLifecycleLog;
import com.demcha.compose.document.theme.BusinessTheme;

import java.util.List;
import java.util.Objects;

/**
 * Phase E.2 — cinematic proposal template that composes against the
 * canonical DSL using a {@link BusinessTheme} for every visual choice.
 *
 * <p>Stacks the v1.4 / v1.5 cinematic primitives:</p>
 *
 * <ul>
 *   <li>Hero soft panel rounded only on the right so the left accent
 *       stripe meets a square edge — uses
 *       {@link DocumentCornerRadius#right(double)} (Phase E.1.1).</li>
 *   <li>Executive-summary soft panel as a separate themed block.</li>
 *   <li>Sender / recipient parties row with line-spaced address blocks.</li>
 *   <li>Scope / deliverables sections rendered as titled paragraph
 *       groups using the theme's {@code text().h2()} headings and
 *       {@code text().body()} paragraph style.</li>
 *   <li>Timeline and pricing tables rendered through the canonical
 *       {@code TableBuilder} with header / zebra / repeating-header /
 *       totals-row styling.</li>
 *   <li>Acceptance-term bullet list and a footer note paragraph.</li>
 * </ul>
 *
 * <p>Constructed from any {@link BusinessTheme} so the same proposal
 * data can ship through {@code BusinessTheme.classic()},
 * {@code BusinessTheme.modern()}, or {@code BusinessTheme.executive()}
 * without touching the composition code.</p>
 *
 * @author Artem Demchyshyn
 */
public final class ProposalTemplateV2 implements ProposalTemplate {
    private static final double TABLE_PADDING = 7.0;

    private final BusinessTheme theme;

    /**
     * Creates the template with {@link BusinessTheme#modern()}.
     */
    public ProposalTemplateV2() {
        this(BusinessTheme.modern());
    }

    /**
     * Creates the template with an explicit theme.
     *
     * @param theme the visual theme used for every section, table, and
     *              accent
     */
    public ProposalTemplateV2(BusinessTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    @Override
    public String getTemplateId() {
        return "proposal-v2";
    }

    @Override
    public String getTemplateName() {
        return "Proposal V2 (cinematic)";
    }

    @Override
    public String getDescription() {
        return "Theme-driven proposal template using soft panels, accent strips, "
                + "themed timeline / pricing tables, and a repeating pricing header.";
    }

    @Override
    public void compose(DocumentSession document, ProposalDocumentSpec spec) {
        long startNanos = TemplateLifecycleLog.start(getTemplateId(), spec);
        try {
            ProposalData data = Objects.requireNonNull(spec, "spec").proposal();
            DocumentColor surface = theme.palette().surface();
            DocumentColor surfaceMuted = theme.palette().surfaceMuted();
            DocumentColor accent = theme.palette().accent();
            DocumentColor rule = theme.palette().rule();

            DocumentTableStyle bordered = DocumentTableStyle.builder()
                    .stroke(DocumentStroke.of(rule, 0.6))
                    .padding(DocumentInsets.of(TABLE_PADDING))
                    .build();
            DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                    .fillColor(theme.palette().primary())
                    .stroke(DocumentStroke.of(rule, 0.6))
                    .padding(DocumentInsets.of(TABLE_PADDING + 1))
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(theme.text().label().fontName())
                            .decoration(theme.text().label().decoration())
                            .size(theme.text().label().size())
                            .color(surface)
                            .build())
                    .build();
            DocumentTableStyle emphasizedRowStyle = DocumentTableStyle.builder()
                    .fillColor(surfaceMuted)
                    .stroke(DocumentStroke.of(rule, 0.6))
                    .padding(DocumentInsets.of(TABLE_PADDING + 1))
                    .textStyle(theme.text().label())
                    .build();

            document.dsl().pageFlow()
                    .name("ProposalCinematicRoot")
                    .spacing(14)
                    .addSection("ProposalHero", section -> section
                            // Round only the right corners — flush left
                            // edge meets the accent stripe cleanly.
                            .softPanel(surfaceMuted, DocumentCornerRadius.right(10), 14)
                            .accentLeft(accent, 4)
                            .spacing(6)
                            .addParagraph(p -> p
                                    .text(data.title().isBlank() ? "Proposal" : data.title())
                                    .textStyle(theme.text().h1())
                                    .margin(DocumentInsets.zero()))
                            .addParagraph(p -> p
                                    .text(data.projectTitle())
                                    .textStyle(theme.text().h3())
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
                                    .textStyle(theme.text().label())
                                    .margin(DocumentInsets.zero()))
                            .addParagraph(p -> p
                                    .text(data.executiveSummary())
                                    .textStyle(theme.text().body())
                                    .lineSpacing(1.35)
                                    .margin(DocumentInsets.zero())))
                    .addRow("ProposalParties", row -> row
                            .spacing(18)
                            .weights(1, 1)
                            .addSection("ProposalSender", col -> col
                                    .spacing(2)
                                    .addParagraph(p -> p
                                            .text("FROM")
                                            .textStyle(theme.text().label())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text(data.sender().name())
                                            .textStyle(theme.text().label())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text(joinAddress(data.sender()))
                                            .textStyle(theme.text().body())
                                            .lineSpacing(1.3)
                                            .margin(DocumentInsets.zero())))
                            .addSection("ProposalRecipient", col -> col
                                    .spacing(2)
                                    .addParagraph(p -> p
                                            .text("TO")
                                            .textStyle(theme.text().label())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text(data.recipient().name())
                                            .textStyle(theme.text().label())
                                            .margin(DocumentInsets.zero()))
                                    .addParagraph(p -> p
                                            .text(joinAddress(data.recipient()))
                                            .textStyle(theme.text().body())
                                            .lineSpacing(1.3)
                                            .margin(DocumentInsets.zero()))));

            for (ProposalSection section : data.sections()) {
                document.dsl().pageFlow()
                        .name("ProposalSectionGroup")
                        .spacing(4)
                        .addParagraph(p -> p
                                .text(section.title())
                                .textStyle(theme.text().h2())
                                .margin(new DocumentInsets(12, 0, 4, 0)))
                        .addSection("ProposalSectionBody", col -> {
                            for (String paragraph : section.paragraphs()) {
                                col.addParagraph(p -> p
                                        .text(paragraph)
                                        .textStyle(theme.text().body())
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
                                .textStyle(theme.text().h2())
                                .margin(new DocumentInsets(12, 0, 4, 0)))
                        .addTable(table -> {
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
                                .textStyle(theme.text().h2())
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
                                    .zebra(surfaceMuted, surface);
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
                                .textStyle(theme.text().h2())
                                .margin(new DocumentInsets(12, 0, 4, 0)))
                        .addSection("ProposalAcceptanceBody", col -> col
                                .accentLeft(accent, 3)
                                .padding(0, 0, 0, 8)
                                .addList(list -> list.items(data.acceptanceTerms())))
                        .build();
            }

            if (!data.footerNote().isBlank()) {
                document.dsl().pageFlow()
                        .name("ProposalFooter")
                        .addParagraph(p -> p
                                .text(data.footerNote())
                                .textStyle(theme.text().caption())
                                .margin(new DocumentInsets(14, 0, 0, 0)))
                        .build();
            }

            TemplateLifecycleLog.success(getTemplateId(), spec, startNanos);
        } catch (RuntimeException | Error ex) {
            TemplateLifecycleLog.failure(getTemplateId(), spec, startNanos, ex);
            throw ex;
        }
    }

    private static String joinAddress(ProposalParty party) {
        StringBuilder builder = new StringBuilder();
        for (String line : party.addressLines()) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
        if (!party.email().isBlank()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(party.email());
        }
        if (!party.phone().isBlank()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(party.phone());
        }
        if (!party.website().isBlank()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(party.website());
        }
        return builder.toString();
    }
}
