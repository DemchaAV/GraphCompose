package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;

import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.ACCENT_PRIMARY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.BODY_LEADING;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.ENTRY_HEAD_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.ENTRY_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.HEADING_TRACKING_EM;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.MAIN_CONTENT_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.MAIN_HEADING_SIZE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.MAIN_HEADING_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.META_SIZE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.RULE_MUTED;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SECTION_ACCENT_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SECTION_ACCENT_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SIDEBAR_HEADING_SIZE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SIDEBAR_HEADING_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.SIDEBAR_INNER_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.TEXT_MUTED;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.TEXT_PRIMARY;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.compact;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.style;
import static com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarStyles.tracked;

/**
 * The parts both columns of the Professional Sidebar CV are built from: the
 * heading over its short accent rule, the hairline that separates entries,
 * and the band that sets an entry title against its dates.
 */
final class ProfessionalSidebarWidgets {

    private ProfessionalSidebarWidgets() {
    }

    /**
     * A sidebar heading: tracked capitals over a short accent rule.
     *
     * <p>The heading is drawn as authored — the design sets these in
     * capitals, and the preset does not upper-case for the author, so a
     * document that titles a section in sentence case gets sentence case.</p>
     *
     * @param section the sidebar column
     * @param title   the heading text
     * @param bodyGap the gap between the rule and the section body
     */
    static void sidebarHeading(SectionBuilder section, String title, double bodyGap) {
        heading(section, "Sidebar", title, SIDEBAR_HEADING_SIZE,
                SIDEBAR_HEADING_TO_RULE, bodyGap);
    }

    /**
     * A main-column heading, a fraction larger than its sidebar counterpart
     * and otherwise identical.
     *
     * @param section the main column
     * @param title   the heading text
     * @param bodyGap the gap between the rule and the section body
     */
    static void mainHeading(SectionBuilder section, String title, double bodyGap) {
        heading(section, "Main", title, MAIN_HEADING_SIZE,
                MAIN_HEADING_TO_RULE, bodyGap);
    }

    private static void heading(SectionBuilder section, String column, String title,
                                double size, double ruleGap, double bodyGap) {
        DocumentTextStyle headingStyle =
                style(BODY_FONT, size, TEXT_PRIMARY, DocumentTextDecoration.BOLD);
        ParagraphBuilder heading = new ParagraphBuilder()
                .name(column + "Heading_" + compact(title))
                .textStyle(headingStyle);
        tracked(heading, title, headingStyle, HEADING_TRACKING_EM);
        section.add(heading.margin(new DocumentInsets(0, 0, ruleGap, 0)).build());
        section.addLine(line -> line
                .name(column + "HeadingRule_" + compact(title))
                .horizontal(SECTION_ACCENT_WIDTH)
                .thickness(SECTION_ACCENT_HEIGHT)
                .color(ACCENT_PRIMARY)
                .margin(new DocumentInsets(0, 0, bodyGap, 0)));
    }

    /** The hairline between two sidebar sections. */
    static void sidebarDivider(SectionBuilder section, double gapAbove, double gapBelow) {
        divider(section, "SidebarDivider", SIDEBAR_INNER_WIDTH, gapAbove, gapBelow);
    }

    /** The hairline between two main-column entries. */
    static void mainDivider(SectionBuilder section, double gapAbove, double gapBelow) {
        divider(section, "MainDivider", MAIN_CONTENT_WIDTH, gapAbove, gapBelow);
    }

    private static void divider(SectionBuilder section, String name, double width,
                                double gapAbove, double gapBelow) {
        section.addLine(line -> line
                .name(name)
                .horizontal(width)
                .thickness(RULE_THICKNESS)
                .color(RULE_MUTED)
                .margin(new DocumentInsets(gapAbove, 0, gapBelow, 0)));
    }

    /**
     * An entry head: the title at the left of a fixed-height band with its
     * dates set flush right on the same optical line.
     *
     * <p>It is a container rather than a two-column row because the two ends
     * are centred against each other vertically, which a row of sections
     * cannot do — a section is as tall as its content and starts at the top
     * of its column.</p>
     *
     * @param section the host column
     * @param name    the node name of the band
     * @param title   the entry title
     * @param dates   the entry dates, drawn flush right
     */
    static void titleDateBand(SectionBuilder section, String name,
                              String title, String dates) {
        DocumentNode titleNode = paragraph(name + "Title", title,
                style(BODY_FONT, ENTRY_TITLE_SIZE, TEXT_PRIMARY,
                        DocumentTextDecoration.BOLD),
                TextAlign.LEFT);
        DocumentNode dateNode = paragraph(name + "Dates", dates,
                style(BODY_FONT, META_SIZE, TEXT_MUTED,
                        DocumentTextDecoration.DEFAULT),
                TextAlign.RIGHT);
        section.addContainer(band -> band
                .name(name)
                .rectangle(MAIN_CONTENT_WIDTH, ENTRY_HEAD_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .centerLeft(titleNode)
                .centerRight(dateNode));
    }

    /** A standalone paragraph node, for anchoring inside a container. */
    static DocumentNode paragraph(String name, String text,
                                  DocumentTextStyle style, TextAlign align) {
        return new ParagraphBuilder()
                .name(name)
                .text(text)
                .textStyle(style)
                .align(align)
                .lineSpacing(BODY_LEADING)
                .margin(DocumentInsets.zero())
                .build();
    }

    static void spacer(SectionBuilder section, double height) {
        section.addSpacer(spacer -> spacer.height(height));
    }
}
