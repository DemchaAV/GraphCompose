package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;

import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ACHIEVEMENT_DISC_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ACHIEVEMENT_DISC_GAP;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ACHIEVEMENT_ICON_SIZE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ACHIEVEMENT_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.BODY;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.CERT_DIVIDER_GAP;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.CERT_DIVIDER_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.CERT_MARKER_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ENTRY_GAP;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.HAIRLINE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.JOB_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.MAIN_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.MAIN_PAD_RIGHT;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.MAIN_PAD_TOP;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.MAIN_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.MAIN_SECTION_GAP;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.MARKER_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.MUTED;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.NAVY;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.RAIL_GUTTER;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.RAIL_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.leading;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.px;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.style;

/**
 * The paper column: the summary, the roles held on a rail, the achievement
 * discs and the certifications.
 *
 * <h2>Why the rail is an accent and not a timeline</h2>
 *
 * <p>A timeline builder has no slot for a date on the far side of its rail and
 * discards a negative gutter, so its marker cannot centre on the rail. Here the
 * rail is instead the left accent of the entry section, so its height derives
 * from the entry it belongs to, and the gap between entries is bottom padding
 * <em>inside</em> the border — which is what makes consecutive rails meet
 * rather than leaving a break between roles.</p>
 *
 * <p>An accent is drawn centred on the edge it belongs to, so the section's
 * left edge already <em>is</em> the rail's axis: a marker reaches it by walking
 * back across the gutter and half its own width, and nothing else. Correcting
 * by half the rail's thickness on top of that lands every marker a rail width
 * to the right of the line it is meant to sit on.</p>
 */
final class MidnightNavyMain {

    private MidnightNavyMain() {
    }

    /**
     * Draws the column.
     *
     * @param column         the main cell
     * @param summary        the opening prose, or {@code null}
     * @param experience     the roles held, or {@code null}
     * @param achievements   the discs, or {@code null}
     * @param certifications the closing columns, or {@code null}
     */
    static void compose(SectionBuilder column, ParagraphSection summary, EntriesSection experience,
                        EntriesSection achievements, EntriesSection certifications) {
        column.spacing(MAIN_SECTION_GAP)
                .padding(new DocumentInsets(MAIN_PAD_TOP, MAIN_PAD_RIGHT, 0, MAIN_PAD_LEFT));
        if (SectionLookup.hasContent(summary)) {
            renderSummary(column, summary);
        }
        if (SectionLookup.hasContent(experience)) {
            renderExperience(column, experience);
        }
        if (SectionLookup.hasContent(achievements)) {
            renderAchievements(column, achievements);
        }
        if (SectionLookup.hasContent(certifications)) {
            renderCertifications(column, certifications);
        }
    }

    private static void renderSummary(SectionBuilder column, ParagraphSection summary) {
        column.addSection("Summary", block -> {
            block.spacing(0);
            MidnightNavyWidgets.mainHeading(block, summary.title(), MAIN_RULE_THICKNESS, px(22.4));
            block.addParagraph(p -> p
                    .name("SummaryText")
                    .text(summary.body())
                    .textStyle(style(BODY_SIZE, BODY, DocumentTextDecoration.DEFAULT))
                    .lineSpacing(leading(22.7, BODY_SIZE)));
        });
    }

    private static void renderExperience(SectionBuilder column, EntriesSection experience) {
        column.addSection("Experience", block -> {
            block.spacing(0);
            MidnightNavyWidgets.mainHeading(block, experience.title(), MAIN_RULE_THICKNESS, px(23));
            List<CvEntry> entries = experience.entries();
            for (int i = 0; i < entries.size(); i++) {
                renderExperienceEntry(block, entries.get(i), i == entries.size() - 1);
            }
        });
    }

    private static void renderExperienceEntry(SectionBuilder block, CvEntry entry, boolean last) {
        block.addSection("ExperienceEntry", section -> {
            section.spacing(0)
                    .keepTogether()
                    .accentLeft(HAIRLINE, RAIL_WIDTH)
                    .padding(new DocumentInsets(0, 0, last ? 0 : ENTRY_GAP, RAIL_GUTTER));

            // The header row and the marker share one layer stack: the row needs
            // the wrapper anyway, and the marker then rides the row it is level
            // with rather than being placed against the section.
            SectionBuilder header = new SectionBuilder();
            header.name("EntryHeaderHolder");
            header.addRow("EntryHeader", row -> {
                row.spacing(0).weights(0.62, 0.38);
                row.addParagraph(p -> {
                    p.name("EntryTitle");
                    p.textStyle(style(JOB_TITLE_SIZE, INK, DocumentTextDecoration.BOLD));
                    if (entry.link().isBlank()) {
                        p.inlineText(entry.title(),
                                style(JOB_TITLE_SIZE, INK, DocumentTextDecoration.BOLD));
                    } else {
                        p.inlineText(entry.title(),
                                style(JOB_TITLE_SIZE, INK, DocumentTextDecoration.BOLD),
                                new DocumentLinkOptions(entry.link()));
                    }
                });
                row.addParagraph(p -> p
                        .name("EntryPeriod")
                        .text(entry.date())
                        .textStyle(style(BODY_SIZE, MUTED, DocumentTextDecoration.DEFAULT))
                        .align(TextAlign.RIGHT));
            });
            section.addLayerStack(stack -> {
                stack.name("EntryHeaderLayer");
                stack.layer(header.build(), LayerAlign.TOP_LEFT, 0);
                // The accent is drawn centred on the section's left edge, so
                // that edge already IS the rail's axis and the marker only has
                // to walk back across the gutter and half its own width.
                // Adding half the rail's thickness on top corrects in the
                // direction the marker is already offset and lands it a rail
                // width to the right of the line it is meant to sit on.
                stack.position(new EllipseBuilder()
                                .name("EntryMarker")
                                .circle(MARKER_DIAMETER)
                                .fillColor(NAVY)
                                .build(),
                        -RAIL_GUTTER - MARKER_DIAMETER / 2.0,
                        px(3.1), LayerAlign.TOP_LEFT, 1);
            });

            MidnightNavyWidgets.layeredRow(section, "EntrySubheader", row -> {
                row.spacing(0).weights(0.62, 0.38);
                row.addParagraph(p -> p
                        .name("EntryCompany")
                        .text(entry.subtitle())
                        .textStyle(style(BODY_SIZE, MUTED, DocumentTextDecoration.ITALIC))
                        .margin(new DocumentInsets(px(5.4), 0, 0, 0)));
                row.addParagraph(p -> p
                        .name("EntryLocation")
                        .text(entry.place())
                        .textStyle(style(BODY_SIZE, MUTED, DocumentTextDecoration.DEFAULT))
                        .align(TextAlign.RIGHT)
                        .margin(new DocumentInsets(px(5.4), 0, 0, 0)));
            });

            section.addList(list -> list
                    .name("EntryBullets")
                    .marker("·")
                    .items(MidnightNavyWidgets.lines(entry.body()))
                    .textStyle(style(BODY_SIZE, BODY, DocumentTextDecoration.DEFAULT))
                    .lineSpacing(leading(23.7, BODY_SIZE))
                    .itemSpacing(leading(23.7, BODY_SIZE))
                    .margin(new DocumentInsets(px(24.8), 0, 0, 0)));
        });
    }

    /**
     * The achievements — one disc and its line per card, side by side.
     *
     * <p>A card shows the entry's title beside its mark: the design gives a card
     * one line of text and no heading over it, so the title is that line.</p>
     */
    private static void renderAchievements(SectionBuilder column, EntriesSection achievements) {
        column.addSection("Achievements", block -> {
            block.spacing(0).keepTogether();
            MidnightNavyWidgets.mainHeading(block, achievements.title(),
                    ACHIEVEMENT_RULE_THICKNESS, px(32.4));
            MidnightNavyWidgets.layeredRow(block, "AchievementCards", row -> {
                row.spacing(0).evenWeights().verticalAlign(RowVerticalAlign.TOP);
                for (CvEntry card : achievements.entries()) {
                    row.addSection("AchievementCard", cell -> renderAchievementCard(cell, card));
                }
            });
        });
    }

    private static void renderAchievementCard(SectionBuilder cell, CvEntry card) {
        MidnightNavyWidgets.layeredRow(cell, "AchievementCardRow", row -> {
            row.spacing(ACHIEVEMENT_DISC_GAP)
                    .verticalAlign(RowVerticalAlign.CENTER)
                    .columns(DocumentRowColumn.fixed(ACHIEVEMENT_DISC_DIAMETER),
                            DocumentRowColumn.weight(1));
            row.addSection("AchievementDiscCell", disc -> disc
                    .addContainer(container -> container
                            .name("AchievementDisc")
                            .circle(ACHIEVEMENT_DISC_DIAMETER)
                            .fillColor(NAVY)
                            .center(MidnightNavyIcons.icon(card.icon())
                                    .node(ACHIEVEMENT_ICON_SIZE))));
            row.addParagraph(p -> p
                    .name("AchievementText")
                    .text(card.title())
                    .textStyle(style(BODY_SIZE, BODY, DocumentTextDecoration.DEFAULT))
                    .lineSpacing(leading(22, BODY_SIZE)));
        });
    }

    /**
     * The certifications — even columns divided by a hairline.
     *
     * <p>The divider is an accent on the column, so its height derives from the
     * entry beside it rather than from a line whose length would have to be
     * computed.</p>
     */
    private static void renderCertifications(SectionBuilder column, EntriesSection certifications) {
        column.addSection("Certifications", block -> {
            block.spacing(0).margin(new DocumentInsets(px(-13), 0, 0, 0));
            MidnightNavyWidgets.mainHeading(block, certifications.title(),
                    MAIN_RULE_THICKNESS, px(28.4));
            List<CvEntry> entries = certifications.entries();
            MidnightNavyWidgets.layeredRow(block, "CertificationColumns", row -> {
                row.spacing(0).evenWeights().verticalAlign(RowVerticalAlign.TOP);
                for (int i = 0; i < entries.size(); i++) {
                    CvEntry entry = entries.get(i);
                    boolean first = i == 0;
                    row.addSection("CertificationColumn", cell -> {
                        if (!first) {
                            cell.accentLeft(HAIRLINE, CERT_DIVIDER_WIDTH)
                                    .padding(new DocumentInsets(0, 0, 0, CERT_DIVIDER_GAP));
                        }
                        renderCertificationEntry(cell, entry);
                    });
                }
            });
        });
    }

    private static void renderCertificationEntry(SectionBuilder cell, CvEntry entry) {
        cell.spacing(px(5.7));
        cell.addParagraph(p -> {
            p.name("CertificationTitle");
            p.dot(CERT_MARKER_DIAMETER, INK);
            if (entry.link().isBlank()) {
                p.inlineText("   " + entry.title(),
                        style(BODY_SIZE, INK, DocumentTextDecoration.BOLD));
            } else {
                p.inlineText("   " + entry.title(),
                        style(BODY_SIZE, INK, DocumentTextDecoration.BOLD),
                        new DocumentLinkOptions(entry.link()));
            }
        });
        if (!entry.subtitle().isBlank()) {
            cell.addParagraph(p -> p
                    .name("CertificationIssuer")
                    .text(entry.subtitle())
                    .textStyle(style(BODY_SIZE, MUTED, DocumentTextDecoration.ITALIC))
                    .margin(new DocumentInsets(0, 0, 0, px(22))));
        }
        if (!entry.date().isBlank()) {
            cell.addParagraph(p -> p
                    .name("CertificationYear")
                    .text(entry.date())
                    .textStyle(style(BODY_SIZE, MUTED, DocumentTextDecoration.DEFAULT))
                    .margin(new DocumentInsets(0, 0, 0, px(22))));
        }
    }
}
