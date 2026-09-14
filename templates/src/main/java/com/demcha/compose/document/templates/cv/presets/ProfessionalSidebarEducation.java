package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.dsl.TimelineRailEnd;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.EntriesSection;

import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.ACCENT_PRIMARY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.BODY_LEADING;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_DEGREE_AIR;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_DEGREE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_ENTRY_GAP;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_HEADING_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_LINE_GAP;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_MARKER_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_RAIL_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_RAIL_X;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.EDUCATION_TEXT_X;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.ENTRY_HEAD_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.RULE_MUTED;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.TEXT_MUTED;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.TEXT_PRIMARY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.body;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.style;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarWidgets.sidebarHeading;

/**
 * The sidebar's education block: the degrees on a hairline rail, a dot on
 * each entry's first line.
 */
final class ProfessionalSidebarEducation {

    private ProfessionalSidebarEducation() {
    }

    /**
     * The education rail: a hairline down the left of the block with a dot
     * on each entry's first line.
     *
     * <p>The rail runs from the first dot to the foot of the last entry, which is what the
     * block is drawn as and what it now asks for. It used to be the section's left accent,
     * and the first entry filled its own head band with the sidebar colour, masked the rail's
     * protruding edge above its dot and redrew the rail below it, meaning to start the line
     * at the first marker. Measured against the render, that never happened: an accent draws
     * above the fill and above the mask, so the rail stayed visible for the 2.5pt above the
     * first dot and the only thing the construction achieved was drawing that stretch below
     * the dot twice. The mask and the redraw are gone; the rail is the timeline's and says
     * what it does.</p>
     */
    static void renderEducation(SectionBuilder section, EntriesSection education) {
        sidebarHeading(section, education.title(), EDUCATION_HEADING_TO_BODY);
        SectionBuilder holder = new SectionBuilder();
        holder.name("EducationRailHolder");
        holder.spacing(0);
        // The axis column centres the rail on itself, so the timeline starts half a dot left
        // of the column the accent drew on and the line lands back on it.
        holder.margin(new DocumentInsets(0, 0, 0,
                EDUCATION_RAIL_X - EDUCATION_MARKER_DIAMETER / 2.0));
        holder.addTimeline(timeline -> {
            timeline.markerOnRail()
                    // Begin at the first dot, run on to the foot of the entries. That is
                    // what this block is drawn as, and what the mask-and-redraw it replaced
                    // was trying to fake — it could not be asked for while the rail's two
                    // ends were one value.
                    .rail(rail -> rail
                            .stroke(DocumentStroke.of(RULE_MUTED, EDUCATION_RAIL_WIDTH))
                            .from(TimelineRailEnd.MARKER).to(TimelineRailEnd.ENTRY_BOUND))
                    .axisWidth(EDUCATION_MARKER_DIAMETER)
                    .markerGap(EDUCATION_TEXT_X - EDUCATION_MARKER_DIAMETER / 2.0)
                    .gutter(0)
                    .spacing(EDUCATION_ENTRY_GAP);
            List<CvEntry> entries = education.entries();
            for (int i = 0; i < entries.size(); i++) {
                int index = i;
                CvEntry entry = entries.get(i);
                timeline.entry(educationMarker(index), e -> e.content(body -> {
                    body.spacing(0);
                    renderEducationHead(body, entry, index);
                    body.addParagraph(p -> p
                            .name("EducationInstitution_" + index)
                            .text(entry.subtitle())
                            .textStyle(style(BODY_FONT, BODY_SIZE, TEXT_MUTED,
                                    DocumentTextDecoration.ITALIC))
                            .margin(new DocumentInsets(
                                    EDUCATION_LINE_GAP, 0, EDUCATION_LINE_GAP, 0)));
                    body.addParagraph(p -> p
                            .name("EducationDates_" + index)
                            .text(entry.date())
                            .textStyle(body())
                            .margin(DocumentInsets.zero()));
                }));
            }
        });
        DocumentNode educationRail = holder.build();
        // The timeline goes in through a layer stack rather than straight into the
        // section. A timeline publishes a row, a row cannot nest inside a row cell,
        // and this sidebar is a row cell — the stack is what insulates it. Unwinding
        // the stacks that turn out not to need it buys no capability, so they stay
        // until that is done as its own deliberate cleanup.
        section.addLayerStack(stack -> stack
                .name("EducationRail")
                .layer(educationRail, LayerAlign.TOP_LEFT, 0));
    }

    /**
     * The accent dot that marks a degree, in a box as tall as the head band it rides.
     *
     * <p>The dot used to be centred in that band by the container positioning it; a timeline
     * top-aligns its marker, so the band height is declared here and the dot carries the half
     * band above it. Same box, same centre, and the rail still runs through it.</p>
     */
    private static TimelineMarker educationMarker(int index) {
        double halfBand = (ENTRY_HEAD_HEIGHT - EDUCATION_MARKER_DIAMETER) / 2.0;
        return TimelineMarker.custom(EDUCATION_MARKER_DIAMETER, ENTRY_HEAD_HEIGHT,
                column -> column.addEllipse(ellipse -> ellipse
                        .name("EducationMarker_" + index)
                        .circle(EDUCATION_MARKER_DIAMETER)
                        .fillColor(ACCENT_PRIMARY)
                        .margin(new DocumentInsets(halfBand, 0, 0, 0))));
    }

    /**
     * The degree title — the entry's own first line, not a band of declared height.
     *
     * <p>It was a container of exactly {@code ENTRY_HEAD_HEIGHT} with the title centred in
     * it, which put the title's centre on the dot's, the dot being centred in a box of the
     * same height. That works for a title of one line and for no other. "MSc Advanced
     * Computer Science and Software Engineering" needs three lines in a sidebar this narrow,
     * and a box told to be 9.3pt tall stayed 9.3pt tall; under
     * {@link ClipPolicy#OVERFLOW_VISIBLE} the extra lines drew rather than vanished — 16.840pt
     * of them, over the institution and the dates below.</p>
     *
     * <p>Nothing about a one-line title moves. The band's surplus over one line is now
     * {@link ProfessionalSidebarStyles#EDUCATION_DEGREE_AIR} above and below the title, so it
     * occupies the same 9.3pt and its centre stays on the dot's; and the width is the same
     * number too, since the container declared
     * {@code SIDEBAR_INNER_WIDTH - EDUCATION_RAIL_X - EDUCATION_TEXT_X} and the timeline's
     * content column resolves to exactly that out of {@code axisWidth} and {@code markerGap}.
     * What changes is only that a title needing a second line now gets one.</p>
     */
    private static void renderEducationHead(SectionBuilder rail, CvEntry entry, int index) {
        rail.addParagraph(p -> p
                .name("EducationDegree_" + index)
                .text(entry.title())
                .textStyle(style(BODY_FONT, EDUCATION_DEGREE_SIZE, TEXT_PRIMARY,
                        DocumentTextDecoration.BOLD))
                .align(TextAlign.LEFT)
                .lineSpacing(BODY_LEADING)
                .margin(new DocumentInsets(
                        EDUCATION_DEGREE_AIR, 0, EDUCATION_DEGREE_AIR, 0)));
    }
}
