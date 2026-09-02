package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.templates.data.proposal.ProposalAttention;
import com.demcha.compose.document.templates.data.proposal.ProposalBrand;
import com.demcha.compose.document.templates.data.proposal.ProposalMetaLine;
import com.demcha.compose.document.templates.data.proposal.ProposalRecipient;
import com.demcha.compose.document.templates.data.proposal.ProposalTitleLines;

import java.util.List;

import static com.demcha.compose.document.templates.proposal.presets.IndigoFlow.columnDivider;
import static com.demcha.compose.document.templates.proposal.presets.IndigoFlow.layeredRow;
import static com.demcha.compose.document.templates.proposal.presets.IndigoFlow.rule;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ACCENT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ACCENT_RULE_AT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ACCENT_RULE_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ADDRESS_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ADDRESS_EXTRA_PITCH_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ATTN_EMAIL_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ATTN_LABEL_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ATTN_NAME_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ATTN_NAME_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ATTN_PHONE_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ATTN_ROLE_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ATTN_ROLE_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.BODY;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.COMPANY_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.COMPANY_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.CONTACT_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.CONTENT_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.DIVIDER_UPPER_H_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.DIVIDER_UPPER_TOP_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.HALF;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.HEADLINE_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.HEADLINE_EXTRA_PITCH_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.HEADLINE_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.HERO_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.INK;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.INTRO_GAP_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.INTRO_LEFT_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.KICKER_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.KICKER_RULE_AT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.KICKER_RULE_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.KICKER_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.LABEL_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.LEFT_DIVIDER_AT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.LEFT_DIVIDER_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.MASTHEAD_RULE_AT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.META_CIRCLE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.META_CIRCLE_AT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.META_CIRCLE_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.META_GLYPH;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.META_LABEL_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.META_LABEL_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.META_PITCH_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.META_VALUE_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.META_VALUE_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.PREPARED_LABEL_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.RULE_ACCENT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.RULE_SOFT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.RULE_STRONG;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.SUBTITLE_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.SUBTITLE_MEASURE_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.SUBTITLE_PITCH_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.TINT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.WORDMARK_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.WORDMARK_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.bold;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.boxBottomPx;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.plain;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.px;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.toPx;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.topBearing;

/**
 * The head of the sheet: the wordmark against the document's label, a rule under
 * both, then the addressed party at the left and the headline at the right.
 */
final class IndigoMasthead {

    private IndigoMasthead() {
    }

    /** The wordmark on the left, the document's label and its accent rule on the right. */
    static void renderMasthead(PageFlowBuilder page, ProposalBrand brand, IndigoFlow flow) {
        double top = flow.boxAt(WORDMARK_CAP - toPx(topBearing(WORDMARK_SIZE, true)),
                toPx(WORDMARK_SIZE));
        page.addRow("Masthead", row -> {
            row.spacing(0)
                    .margin(new DocumentInsets(top, 0, 0, 0))
                    .columns(DocumentRowColumn.weight(1), DocumentRowColumn.weight(1));
            row.addParagraph(p -> p
                    .name("Wordmark")
                    .text(brand.nameLine1())
                    .textStyle(bold(WORDMARK_SIZE, INK)));
            row.addSection("DocumentLabel", cell -> {
                cell.spacing(0);
                IndigoFlow inner = new IndigoFlow(
                        KICKER_CAP - toPx(topBearing(KICKER_SIZE, true)));
                cell.addParagraph(p -> p
                        .name("DocumentLabelText")
                        .text(brand.documentLabel())
                        .textStyle(bold(KICKER_SIZE, INK))
                        .align(TextAlign.RIGHT)
                        .margin(new DocumentInsets(
                                inner.capAt(KICKER_CAP, KICKER_SIZE, true), 0, 0, 0)));
                // A line carries no alignment, so the rule is pushed right by the
                // width its cell does not use — derived, not tuned.
                double indent = (CONTENT_W * HALF) - KICKER_RULE_W;
                cell.addLine(line -> rule(line, "DocumentLabelRule", KICKER_RULE_W, ACCENT)
                        .thickness(RULE_ACCENT)
                        .margin(new DocumentInsets(
                                inner.boxAt(KICKER_RULE_AT, 3), 0, 0, indent)));
            });
        });
    }

    /** The full-width rule under the masthead. */
    static void renderMastheadRule(PageFlowBuilder page, IndigoFlow flow) {
        double top = flow.boxAt(MASTHEAD_RULE_AT, 1.5);
        page.addLine(line -> rule(line, "MastheadRule", CONTENT_W, RULE_STRONG)
                .margin(new DocumentInsets(top, 0, 0, 0)));
    }

    /**
     * The upper half: the addressee and the person at it on the left, the
     * headline and the header tiles on the right.
     *
     * <p>Three columns, not two: the middle one is the design's own hairline
     * between the two blocks.</p>
     */
    static void renderIntroRow(PageFlowBuilder page, ProposalRecipient recipient,
                               ProposalAttention attention, ProposalTitleLines title,
                               ProposalMetaLine meta, IndigoFlow flow) {
        double rowTopPx = PREPARED_LABEL_CAP - toPx(topBearing(LABEL_SIZE, true));
        double top = flow.boxAt(rowTopPx,
                boxBottomPx(META_VALUE_CAP, META_VALUE_SIZE, false) - rowTopPx);
        page.addRow("IntroRow", row -> {
            row.spacing(0)
                    .margin(new DocumentInsets(top, 0, 0, 0))
                    .columns(DocumentRowColumn.fixed(INTRO_LEFT_W),
                            DocumentRowColumn.fixed(INTRO_GAP_W),
                            DocumentRowColumn.weight(1));
            row.addSection("IntroLeft", left -> {
                left.spacing(0);
                IndigoFlow cell = new IndigoFlow(rowTopPx);
                renderRecipient(left, recipient, cell);
                renderAttention(left, attention, cell);
            });
            columnDivider(row, "ColumnDividerUpper",
                    DIVIDER_UPPER_TOP_PX - rowTopPx, DIVIDER_UPPER_H_PX);
            row.addSection("IntroRight", right -> {
                right.spacing(0);
                IndigoFlow cell = new IndigoFlow(rowTopPx);
                renderHeadline(right, title, cell);
                renderMetaTiles(right, meta, cell);
            });
        });
    }

    private static void renderRecipient(SectionBuilder left, ProposalRecipient recipient,
                                        IndigoFlow cell) {
        left.addParagraph(p -> p
                .name("RecipientLabel")
                .text(recipient.label())
                .textStyle(bold(LABEL_SIZE, ACCENT))
                .margin(new DocumentInsets(
                        cell.capAt(PREPARED_LABEL_CAP, LABEL_SIZE, true), 0, 0, 0)));
        left.addParagraph(p -> p
                .name("RecipientName")
                .text(recipient.name())
                .textStyle(bold(COMPANY_SIZE, INK))
                .margin(new DocumentInsets(
                        cell.capAt(COMPANY_CAP, COMPANY_SIZE, true), 0, 0, 0)));

        List<String> lines = recipient.addressLines();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int index = i;
            // The design maps three address lines; a fourth and beyond continue
            // on the pitch the mapped ones establish.
            double capPx = index < ADDRESS_CAP.length
                    ? ADDRESS_CAP[index]
                    : ADDRESS_CAP[ADDRESS_CAP.length - 1]
                            + (index - ADDRESS_CAP.length + 1) * ADDRESS_EXTRA_PITCH_PX;
            double gap = cell.capAt(capPx, CONTACT_SIZE, false);
            left.addParagraph(p -> p
                    .name("RecipientAddress_" + index)
                    .text(line)
                    .textStyle(plain(CONTACT_SIZE, BODY))
                    .margin(new DocumentInsets(gap, 0, 0, 0)));
        }

        left.addLine(line -> rule(line, "RecipientDivider", LEFT_DIVIDER_W, RULE_SOFT)
                .margin(new DocumentInsets(cell.boxAt(LEFT_DIVIDER_AT, 1.5), 0, 0, 0)));
    }

    private static void renderAttention(SectionBuilder left, ProposalAttention attention,
                                        IndigoFlow cell) {
        left.addParagraph(p -> p
                .name("AttentionLabel")
                .text(attention.label())
                .textStyle(bold(LABEL_SIZE, ACCENT))
                .margin(new DocumentInsets(
                        cell.capAt(ATTN_LABEL_CAP, LABEL_SIZE, true), 0, 0, 0)));
        left.addParagraph(p -> p
                .name("AttentionName")
                .text(attention.name())
                .textStyle(bold(ATTN_NAME_SIZE, INK))
                .margin(new DocumentInsets(
                        cell.capAt(ATTN_NAME_CAP, ATTN_NAME_SIZE, true), 0, 0, 0)));
        left.addParagraph(p -> p
                .name("AttentionRole")
                .text(attention.role())
                .textStyle(plain(ATTN_ROLE_SIZE, BODY))
                .margin(new DocumentInsets(
                        cell.capAt(ATTN_ROLE_CAP, ATTN_ROLE_SIZE, false), 0, 0, 0)));
        reachable(left, "AttentionEmail", attention.email(),
                ProposalUri.mailLink(attention.email()), cell, ATTN_EMAIL_CAP);
        reachable(left, "AttentionPhone", attention.phone(),
                ProposalUri.telLink(attention.phone()), cell, ATTN_PHONE_CAP);
    }

    /**
     * A contact line, made followable, at the design y the map gives it.
     *
     * <p>The cursor is asked for its margin inside the guard rather than outside
     * it. A line the document does not carry was never laid out, and a cursor
     * advanced past it would place the next line from where the missing one
     * would have ended instead of where the map puts it.</p>
     */
    private static void reachable(SectionBuilder parent, String name, String value,
                                  DocumentLinkOptions link, IndigoFlow cell, double capPx) {
        if (value.isBlank()) {
            return;
        }
        double gap = cell.capAt(capPx, CONTACT_SIZE, false);
        parent.addParagraph(p -> {
            p.name(name).margin(new DocumentInsets(gap, 0, 0, 0));
            if (link == null) {
                p.text(value).textStyle(plain(CONTACT_SIZE, BODY));
            } else {
                p.inlineText(value, plain(CONTACT_SIZE, BODY), link);
            }
        });
    }

    /**
     * The headline, its accent rule and the paragraph under it.
     *
     * <p>The rag is a composition decision, so each designed line is its own
     * paragraph. Wrapping one string would re-break it at the engine's measure
     * rather than where the design breaks it.</p>
     */
    private static void renderHeadline(SectionBuilder right, ProposalTitleLines title,
                                       IndigoFlow cell) {
        List<String> lines = title.lines();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int index = i;
            double capPx = index < HEADLINE_CAP.length
                    ? HEADLINE_CAP[index]
                    : HEADLINE_CAP[HEADLINE_CAP.length - 1]
                            + (index - HEADLINE_CAP.length + 1) * HEADLINE_EXTRA_PITCH_PX;
            double gap = cell.capAt(capPx, HEADLINE_SIZE, true);
            right.addParagraph(p -> p
                    .name("HeadlineLine_" + index)
                    .text(line)
                    .textStyle(bold(HEADLINE_SIZE, INK))
                    .margin(new DocumentInsets(gap, 0, 0, 0)));
        }

        right.addLine(line -> rule(line, "HeadlineRule", ACCENT_RULE_W, ACCENT)
                .thickness(RULE_ACCENT)
                .margin(new DocumentInsets(cell.boxAt(ACCENT_RULE_AT, 3), 0, 0, 0)));

        List<String> standfirst = title.standfirst();
        if (standfirst.isEmpty()) {
            return;
        }
        double measureInset = Math.max(0, HERO_W - px(SUBTITLE_MEASURE_PX));
        double gap = cell.capAt(SUBTITLE_CAP, BODY_SIZE, false);
        right.addParagraph(p -> p
                .name("HeadlineStandfirst")
                .text(String.join(" ", standfirst))
                .textStyle(plain(BODY_SIZE, BODY))
                // Line spacing is additive points between lines, not a multiple.
                .lineSpacing(px(SUBTITLE_PITCH_PX) - BODY_SIZE)
                .margin(new DocumentInsets(gap, measureInset, 0, 0)));
        // The design sets it in three lines at this measure; the map states that
        // rather than asking the engine afterwards.
        cell.advanceTo(SUBTITLE_CAP + 2 * SUBTITLE_PITCH_PX + toPx(BODY_SIZE));
    }

    /** The row of marked tiles under the headline. */
    private static void renderMetaTiles(SectionBuilder right, ProposalMetaLine meta,
                                        IndigoFlow cell) {
        List<ProposalMetaLine.Entry> entries = meta.entries();
        if (entries.isEmpty()) {
            return;
        }
        double top = cell.boxAt(META_CIRCLE_AT,
                boxBottomPx(META_VALUE_CAP, META_VALUE_SIZE, false) - META_CIRCLE_AT);
        layeredRow(right, "MetaTiles", row -> {
            row.spacing(0).margin(new DocumentInsets(top, 0, 0, 0));
            DocumentRowColumn[] columns = new DocumentRowColumn[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                columns[i] = i < META_PITCH_PX.length
                        ? DocumentRowColumn.fixed(px(META_PITCH_PX[i]))
                        : DocumentRowColumn.weight(1);
            }
            row.columns(columns);
            for (int i = 0; i < entries.size(); i++) {
                ProposalMetaLine.Entry entry = entries.get(i);
                int index = i;
                row.addSection("MetaTile_" + index, group -> {
                    group.spacing(0);
                    IndigoFlow stack = new IndigoFlow(META_CIRCLE_AT);
                    group.add(IndigoWidgets.disc("MetaCircle_" + index, TINT,
                            entry.icon(), META_CIRCLE, META_GLYPH));
                    stack.boxAt(META_CIRCLE_AT, META_CIRCLE_PX);
                    group.addParagraph(p -> p
                            .name("MetaLabel_" + index)
                            .text(entry.label())
                            .textStyle(plain(META_LABEL_SIZE, BODY))
                            .margin(new DocumentInsets(
                                    stack.capAt(META_LABEL_CAP, META_LABEL_SIZE, false), 0, 0, 0)));
                    group.addParagraph(p -> p
                            .name("MetaValue_" + index)
                            .text(entry.value())
                            .textStyle(plain(META_VALUE_SIZE, INK))
                            .margin(new DocumentInsets(
                                    stack.capAt(META_VALUE_CAP, META_VALUE_SIZE, false), 0, 0, 0)));
                });
            }
        });
    }
}
