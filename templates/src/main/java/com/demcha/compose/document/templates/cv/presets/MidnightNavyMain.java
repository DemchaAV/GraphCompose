package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
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
 * <h2>The rail</h2>
 *
 * <p>The roles are a timeline: one rail, one marker per role, sitting on it. It was built
 * from a per-entry left accent with the marker positioned into a layer stack, because a
 * timeline could not then put a marker on its rail. It can — {@code markerOnRail()} anchors
 * the rail through the marker's centre — so the rail is the timeline's, and the layer stack
 * and its offsets are gone.</p>
 *
 * <p>Two things keep the drawing where it was. The rail's ends come from
 * {@code ENTRY_BOUNDS}, which spans the entries' own boxes: the gap between roles is padding
 * inside an entry's box and the last entry carries none, so consecutive segments meet and
 * nothing trails past the final marker, exactly as the accents did. And the axis column
 * centres the rail on itself, so the timeline starts half an axis to the left and the rail
 * lands back on the column edge the heading rule shares.</p>
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
            renderExperienceTimeline(block, experience.entries());
        });
    }

    /**
     * The roles, on one rail.
     *
     * <p>The axis column centres the rail on itself, so the timeline starts half an axis
     * left of the column the heading rule sits on and the rail lands back on that edge —
     * where the per-entry accent drew it before. The rail's ends come from
     * {@code ENTRY_BOUNDS}: the gap between roles is padding inside an entry's own box and
     * the last entry carries none, which is what made consecutive accents meet.</p>
     */
    private static void renderExperienceTimeline(SectionBuilder block, List<CvEntry> entries) {
        SectionBuilder holder = new SectionBuilder();
        holder.name("ExperienceRailHolder");
        {
            SectionBuilder host = holder;
            host.spacing(0)
                    .margin(new DocumentInsets(0, 0, 0, -MARKER_DIAMETER / 2.0));
            host.addTimeline(timeline -> {
                timeline.markerOnRail()
                        .connector(HAIRLINE, RAIL_WIDTH)
                        .axisWidth(MARKER_DIAMETER)
                        .markerGap(RAIL_GUTTER - MARKER_DIAMETER / 2.0)
                        .gutter(0)
                        .spacing(ENTRY_GAP)
                        .keepEntriesTogether();
                for (CvEntry entry : entries) {
                    timeline.entry(entryMarker(),
                            e -> e.content(section -> renderExperienceEntry(section, entry)));
                }
            });
        }
        DocumentNode node = holder.build();
        block.addLayerStack(stack -> stack
                .name("ExperienceRail")
                .layer(node, LayerAlign.TOP_LEFT, 0));
    }

    /**
     * The navy disc, in a box tall enough to carry the drop that sets it on the title's cap
     * height. A marker's declared box is what the timeline reserves and derives the rail
     * from, so the drop rides inside the marker rather than being positioned against the
     * header row.
     */
    private static TimelineMarker entryMarker() {
        return TimelineMarker.custom(MARKER_DIAMETER, MARKER_DIAMETER + px(3.1),
                column -> column.addEllipse(ellipse -> ellipse
                        .name("EntryMarker")
                        .circle(MARKER_DIAMETER)
                        .fillColor(NAVY)
                        .margin(new DocumentInsets(px(3.1), 0, 0, 0))));
    }

    private static void renderExperienceEntry(SectionBuilder section, CvEntry entry) {
        section.spacing(0);
        // A row nested directly in a row cell is refused, and a timeline entry lays its
        // content out in one — so the header goes through the same wrapper as every other
        // horizontal pair in this sheet.
        MidnightNavyWidgets.layeredRow(section, "EntryHeader", row -> {
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
