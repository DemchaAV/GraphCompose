package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;

import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.ACCENT_DEEP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_COL1_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_COL2_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_COL3_TEXT_INSET;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_COL3_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_DEGREE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_FACT_GAP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_LEADING;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_LINE_GAP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_TO_TAGLINE_RULE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_WEIGHT_1;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_WEIGHT_2;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_WEIGHT_3;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BODY_TEXT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.CERT_ITEM_GAP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.CLOSING_RULE_TO_BAND;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.CONTENT_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.DIVIDER_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.FULL_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.HEADING_FONT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.RULE_SOFT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.RULE_STRONG;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.TAGLINE_RULE_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.TAGLINE_RULE_TO_TEXT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.TAGLINE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.TAGLINE_TRACKING_EM;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.compact;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.style;
import static com.demcha.compose.document.templates.cv.presets.TealPulseWidgets.columnHeader;
import static com.demcha.compose.document.templates.cv.presets.TealPulseWidgets.dottedLine;
import static com.demcha.compose.document.templates.cv.presets.TealPulseWidgets.icon;
import static com.demcha.compose.document.templates.cv.presets.TealPulseWidgets.tracked;

/**
 * What closes the sheet: the three-column band and the tagline under its
 * rule.
 */
final class TealPulseClosing {

    private TealPulseClosing() {
    }

    static void render(PageFlowBuilder page, EntriesSection education,
                       EntriesSection certifications, EntriesSection facts,
                       ParagraphSection tagline) {
        page.addLine(line -> line
                .name("ClosingBandRule")
                .horizontal(CONTENT_WIDTH)
                .thickness(FULL_RULE_THICKNESS)
                .color(RULE_STRONG)
                .margin(new DocumentInsets(0, 0, CLOSING_RULE_TO_BAND, 0)));
        renderBand(page, education, certifications, facts);
        renderTagline(page, tagline);
    }

    // -- the band ----------------------------------------------------------

    /**
     * The three closing columns.
     *
     * <p>The two rules between them are left accents on the second and third
     * columns, so they sit on the band's own weight boundaries and cannot
     * drift off the grid.</p>
     */
    private static void renderBand(PageFlowBuilder page, EntriesSection education,
                                   EntriesSection certifications, EntriesSection facts) {
        page.addRow("ClosingBand", row -> {
            row.weights(BAND_WEIGHT_1, BAND_WEIGHT_2, BAND_WEIGHT_3);
            row.verticalAlign(RowVerticalAlign.TOP);
            row.spacing(0);
            row.addSection("EducationColumn", column -> {
                column.spacing(0);
                renderEducation(column, education);
            });
            row.addSection("CertificationsColumn", column -> {
                column.spacing(0);
                column.accentLeft(RULE_SOFT, DIVIDER_WIDTH);
                column.padding(new DocumentInsets(0, 0, 0, BAND_PAD_LEFT));
                renderCertifications(column, certifications);
            });
            row.addSection("AdditionalColumn", column -> {
                column.spacing(0);
                column.accentLeft(RULE_SOFT, DIVIDER_WIDTH);
                column.padding(new DocumentInsets(0, 0, 0, BAND_PAD_LEFT));
                renderFacts(column, facts);
            });
            row.margin(new DocumentInsets(0, 0, BAND_TO_TAGLINE_RULE, 0));
        });
    }

    /** Each degree as three stacked lines: the award, the campus, the years. */
    private static void renderEducation(SectionBuilder column, EntriesSection education) {
        if (education == null || education.entries().isEmpty()) {
            return;
        }
        column.add(columnHeader(education.title(), TealPulseIcons.EDUCATION, BAND_COL1_WIDTH));
        List<CvEntry> entries = education.entries();
        for (int index = 0; index < entries.size(); index++) {
            CvEntry entry = entries.get(index);
            boolean last = index == entries.size() - 1;
            column.add(bandLine("EducationDegree_" + index, entry.title(), BAND_DEGREE_SIZE,
                    DocumentTextDecoration.BOLD, BAND_LINE_GAP, entry.link()));
            column.add(bandLine("EducationInstitution_" + index, entry.subtitle(),
                    BAND_BODY_SIZE, DocumentTextDecoration.DEFAULT, BAND_LINE_GAP, ""));
            column.add(bandLine("EducationDates_" + index, entry.date(), BAND_BODY_SIZE,
                    DocumentTextDecoration.DEFAULT, last ? 0 : BAND_LINE_GAP, ""));
        }
    }

    /** One line of closing-band body text, carrying a link when it has one. */
    private static DocumentNode bandLine(String name, String text, double size,
                                         DocumentTextDecoration decoration, double gapBelow,
                                         String link) {
        DocumentTextStyle textStyle = style(BODY_FONT, size, BODY_TEXT, decoration);
        ParagraphBuilder paragraph = new ParagraphBuilder()
                .name(name)
                .text(text)
                .textStyle(textStyle)
                .align(TextAlign.LEFT)
                .lineSpacing(1.0)
                .margin(new DocumentInsets(0, 0, gapBelow, 0));
        if (link != null && !link.isBlank()) {
            paragraph.link(new DocumentLinkOptions(link));
        }
        return paragraph.build();
    }

    private static void renderCertifications(SectionBuilder column, EntriesSection section) {
        if (section == null || section.entries().isEmpty()) {
            return;
        }
        column.add(columnHeader(section.title(), TealPulseIcons.CERTIFICATIONS,
                BAND_COL2_WIDTH));
        column.addSection("CertificationItems", items -> {
            items.spacing(CERT_ITEM_GAP);
            for (CvEntry entry : section.entries()) {
                items.add(dottedLine("Certification_" + compact(entry.title()), entry.title(),
                        BAND_BODY_SIZE, 1.0));
            }
        });
    }

    /**
     * The closing facts: a bold label and its value as two runs of one
     * wrapping paragraph, which is why a fact that runs to a second line
     * starts it at the column edge rather than under its value.
     */
    private static void renderFacts(SectionBuilder column, EntriesSection section) {
        if (section == null || section.entries().isEmpty()) {
            return;
        }
        column.add(columnHeader(section.title(), TealPulseIcons.ADDITIONAL, BAND_COL3_WIDTH));
        DocumentTextStyle labelStyle =
                style(BODY_FONT, BAND_BODY_SIZE, BODY_TEXT, DocumentTextDecoration.BOLD);
        DocumentTextStyle valueStyle =
                style(BODY_FONT, BAND_BODY_SIZE, BODY_TEXT, DocumentTextDecoration.DEFAULT);
        column.addSection("AdditionalFacts", facts -> {
            facts.spacing(BAND_FACT_GAP);
            facts.padding(new DocumentInsets(0, BAND_COL3_TEXT_INSET, 0, 0));
            for (CvEntry entry : section.entries()) {
                facts.addParagraph(paragraph -> paragraph
                        .name("Fact_" + compact(entry.title()))
                        .textStyle(valueStyle)
                        .align(TextAlign.LEFT)
                        .lineSpacing(BAND_LEADING)
                        .inlineText(entry.title(), labelStyle)
                        .inlineText(" " + entry.body().replace(String.valueOf((char) 10), " "),
                                valueStyle)
                        .margin(DocumentInsets.zero()));
            }
        });
    }

    // -- the tagline -------------------------------------------------------

    /**
     * The rule and the heart are one terminal — the rule anchored to its left,
     * the heart to its right — so the mark follows the rule's end whatever the
     * content width becomes. The tracked line sits under it.
     */
    private static void renderTagline(PageFlowBuilder page, ParagraphSection tagline) {
        if (tagline == null || tagline.body().isBlank()) {
            return;
        }
        double heartSize = TealPulseIcons.size(TealPulseIcons.HEART);
        double heartLift = -(heartSize - TAGLINE_RULE_HEIGHT) / 2.0;
        DocumentNode rule = new ShapeBuilder()
                .name("TaglineRuleLine")
                .size(CONTENT_WIDTH, FULL_RULE_THICKNESS)
                .fillColor(RULE_STRONG)
                .build();
        page.addContainer(container -> container
                .name("TaglineRule")
                .rectangle(CONTENT_WIDTH, TAGLINE_RULE_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .centerLeft(rule)
                .position(icon(TealPulseIcons.HEART), 0, heartLift, LayerAlign.CENTER_RIGHT)
                .margin(new DocumentInsets(0, 0, TAGLINE_RULE_TO_TEXT, 0)));

        DocumentTextStyle textStyle =
                style(HEADING_FONT, TAGLINE_SIZE, ACCENT_DEEP, DocumentTextDecoration.BOLD);
        ParagraphBuilder line = new ParagraphBuilder()
                .name("Tagline")
                .textStyle(textStyle)
                .align(TextAlign.CENTER)
                .lineSpacing(1.0);
        tracked(line, tagline.body().replace(String.valueOf((char) 10), " "), textStyle,
                TAGLINE_TRACKING_EM);
        page.add(line.margin(DocumentInsets.zero()).build());
    }
}
