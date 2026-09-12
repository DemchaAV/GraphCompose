package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;

import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.CREDENTIALS_TO_TOOLS;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.CREDENTIAL_ENTRY_GAP;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.CREDENTIAL_GUTTER_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.CREDENTIAL_HALF_GUTTER;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.CREDENTIAL_HEADING_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.CREDENTIAL_ICON_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.CREDENTIAL_LEFT_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.CREDENTIAL_LINE_GAP;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.CREDENTIAL_RIGHT_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.DETAIL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.ENTRIES_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.HEADING_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.RULE_TO_CREDENTIALS;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SMALL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.TOOL_SEPARATOR;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.textStyle;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldWidgets.layeredRow;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldWidgets.mainHeading;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldWidgets.plainHeading;

/**
 * The two closing blocks: the credentials in a pair of columns divided by a
 * hairline, and the tools strip beneath them.
 */
final class CharcoalGoldCredentials {

    private CharcoalGoldCredentials() {
    }

    /**
     * The credential pair.
     *
     * <p>Two cells, not three: the gutter is split down the middle and half
     * given to each column, so the divider can be the right-hand column's own
     * left border and take its height from that column's content. A third
     * cell holding a line would need a height, and a height here could only
     * be computed against today's text.</p>
     */
    static void renderCredentials(SectionBuilder main,
                                  EntriesSection certifications,
                                  EntriesSection achievements) {
        boolean left = SectionLookup.hasContent(certifications);
        boolean right = SectionLookup.hasContent(achievements);
        main.addSection("Credentials", block -> {
            block.spacing(0);
            block.margin((float) ENTRIES_TO_RULE, 0f, 0f, 0f);
            block.addLine(line -> line
                    .name("CredentialsRule")
                    .fill()
                    .thickness(RULE_THICKNESS)
                    .color(RULE));
            layeredRow(block, "CredentialColumns", RULE_TO_CREDENTIALS, 0.0, row -> {
                row.spacing(0);
                row.weights(
                        CREDENTIAL_LEFT_WEIGHT + CREDENTIAL_GUTTER_WEIGHT / 2.0,
                        CREDENTIAL_RIGHT_WEIGHT + CREDENTIAL_GUTTER_WEIGHT / 2.0);
                row.addSection("Certifications", column -> {
                    column.padding(0f, (float) CREDENTIAL_HALF_GUTTER, 0f, 0f);
                    if (left) {
                        renderColumn(column, "Certification", certifications);
                    }
                });
                row.addSection("Achievements", column -> {
                    // The divider is this column's left border, so it stops
                    // where the column's own content does.
                    column.accentLeft(RULE, RULE_THICKNESS);
                    column.padding(0f, 0f, 0f, (float) CREDENTIAL_HALF_GUTTER);
                    if (right) {
                        renderColumn(column, "Achievement", achievements);
                    }
                });
            });
        });
    }

    /** One credential column: a plain heading, then a marked entry per row. */
    private static void renderColumn(SectionBuilder column, String prefix,
                                     EntriesSection credentials) {
        column.spacing(0);
        column.keepTogether();
        plainHeading(column, credentials.title());
        List<CvEntry> items = credentials.entries();
        for (int i = 0; i < items.size(); i++) {
            CvEntry item = items.get(i);
            boolean first = i == 0;
            boolean last = i == items.size() - 1;
            int index = i;
            layeredRow(column, prefix + "_" + index,
                    first ? CREDENTIAL_HEADING_TO_BODY : CREDENTIAL_ENTRY_GAP, 0.0,
                    row -> {
                        row.spacing(0);
                        row.weights(CREDENTIAL_ICON_WEIGHT, 1.0 - CREDENTIAL_ICON_WEIGHT);
                        row.addSection(prefix + "Icon_" + index, cell -> {
                            cell.spacing(0);
                            if (!item.icon().isBlank()) {
                                cell.addSvgIcon(CharcoalGoldIcons.icon(item.icon()),
                                        CharcoalGoldIcons.CREDENTIAL_SIZE);
                            }
                        });
                        row.addSection(prefix + "Text_" + index, text -> {
                            text.spacing(0);
                            text.addParagraph(p -> {
                                p.name(prefix + "Name_" + index)
                                        .textStyle(textStyle(DETAIL_SIZE, INK, true));
                                CharcoalGoldText.title(p, item,
                                        textStyle(DETAIL_SIZE, INK, true));
                                p.margin(0f, 0f, (float) CREDENTIAL_LINE_GAP, 0f);
                            });
                            text.addParagraph(p -> p
                                    .name(prefix + "Issuer_" + index)
                                    .text(item.subtitle())
                                    .textStyle(textStyle(SMALL_SIZE, INK, false))
                                    .margin(0f, 0f, (float) CREDENTIAL_LINE_GAP, 0f));
                            text.addParagraph(p -> p
                                    .name(prefix + "Year_" + index)
                                    .text(item.date())
                                    .textStyle(textStyle(SMALL_SIZE, INK, false)));
                        });
                    });
            if (!last) {
                column.addLine(line -> line
                        .name(prefix + "Rule_" + index)
                        .fill()
                        .thickness(RULE_THICKNESS)
                        .color(RULE)
                        .margin(new DocumentInsets(CREDENTIAL_ENTRY_GAP, 0, 0, 0)));
            }
        }
    }

    /**
     * The closing strip: one line of tools, centred, with a pale pipe
     * between neighbours.
     */
    static void renderTools(SectionBuilder main, ParagraphSection tools) {
        List<String> items = CharcoalGoldText.lines(tools.body());
        main.addSection("TechnicalTools", block -> {
            block.spacing(0);
            block.margin((float) CREDENTIALS_TO_TOOLS, 0f, 0f, 0f);
            block.keepWithNext();
            mainHeading(block, tools.title());
            block.addParagraph(p -> {
                p.name("ToolsStrip");
                p.align(TextAlign.CENTER);
                p.margin((float) HEADING_TO_BODY, 0f, 0f, 0f);
                for (int i = 0; i < items.size(); i++) {
                    if (i > 0) {
                        p.inlineText(TOOL_SEPARATOR, textStyle(DETAIL_SIZE, RULE, false));
                    }
                    p.inlineText(items.get(i), textStyle(DETAIL_SIZE, INK, false));
                }
            });
        });
    }
}
