package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.List;
import java.util.Locale;

import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.BADGE_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.BULLET_LEADING;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.HEADER_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.HEADING_BAND_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.HEADING_SIZE;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.HEADING_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.MAIN_CONTENT_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.NAVY;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.PLAIN_ITEM_GAP;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.RULE_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SECTION_INDENT;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SIDEBAR_BLOCK_GAP;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SIDEBAR_INNER_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SIDEBAR_RULE;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SIDEBAR_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.SIDEBAR_STRONG;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.body;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.compact;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.style;
import static com.demcha.compose.document.templates.cv.presets.NavySidebarStyles.tracked;

/**
 * The two headings this design uses — tracked capitals over a hairline in
 * the navy sidebar, tracked capitals beside a filled badge in the main
 * column — and the indented bullet list the closing sections are set in.
 */
final class NavySidebarWidgets {

    private NavySidebarWidgets() {
    }

    /**
     * A sidebar heading: tracked white capitals over a translucent rule the
     * width of the sidebar's text column.
     *
     * @param section the sidebar column
     * @param title   the heading text, drawn in capitals
     * @param first   true for the block directly under the portrait, which
     *                takes no gap above
     */
    static void sidebarHeading(SectionBuilder section, String title, boolean first) {
        DocumentTextStyle headingStyle =
                style(HEADING_SIZE, SIDEBAR_STRONG, DocumentTextDecoration.BOLD);
        section.addParagraph(p -> {
            p.name("SidebarHeading_" + compact(title));
            p.textStyle(headingStyle);
            tracked(p, title.toUpperCase(Locale.ROOT), headingStyle);
            p.margin(new DocumentInsets(first ? 0 : SIDEBAR_BLOCK_GAP, 0, HEADING_TO_RULE, 0));
        });
        section.addLine(line -> line
                .name("SidebarRule_" + compact(title))
                .horizontal(SIDEBAR_INNER_WIDTH)
                .thickness(SIDEBAR_RULE_THICKNESS)
                .color(SIDEBAR_RULE)
                .margin(new DocumentInsets(0, 0, RULE_TO_BODY, 0)));
    }

    /**
     * A main-column heading: the section's mark inside a navy disc, with
     * tracked capitals on the disc's axis.
     *
     * <p>It is a container rather than a row because the badge and the words
     * are centred against each other; a row of sections would top-align
     * them.</p>
     *
     * @param section   the main column
     * @param iconToken the packaged mark for this section
     * @param title     the heading text, drawn in capitals
     */
    static void sectionHeader(SectionBuilder section, String iconToken, String title) {
        DocumentNode badge = new ShapeContainerBuilder()
                .name("Badge_" + compact(title))
                .circle(BADGE_DIAMETER)
                .clipPolicy(ClipPolicy.CLIP_PATH)
                .fillColor(NAVY)
                .center(new ImageBuilder()
                        .name("BadgeGlyph_" + compact(title))
                        .source(NavySidebarIcons.image(iconToken))
                        .size(NavySidebarIcons.size(iconToken), NavySidebarIcons.size(iconToken))
                        .build())
                .build();

        DocumentTextStyle headingStyle = style(HEADING_SIZE, INK, DocumentTextDecoration.BOLD);
        ParagraphBuilder heading = new ParagraphBuilder()
                .name("SectionTitle_" + compact(title))
                .textStyle(headingStyle);
        tracked(heading, title.toUpperCase(Locale.ROOT), headingStyle);
        DocumentNode headingNode = heading.margin(DocumentInsets.zero()).build();

        section.addContainer(band -> band
                .name("SectionHeader_" + compact(title))
                .rectangle(MAIN_CONTENT_WIDTH, HEADING_BAND_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .centerLeft(badge)
                .position(headingNode, SECTION_INDENT, 0, LayerAlign.CENTER_LEFT));
    }

    /**
     * A bullet list indented to clear the badge, so its text starts on the
     * same axis as the heading beside it.
     *
     * @param section the main column
     * @param name    node name of the list
     * @param items   one bullet per element
     */
    static void indentedList(SectionBuilder section, String name, List<String> items) {
        section.addList(list -> list
                .name(name)
                .bullet()
                .items(items)
                .textStyle(body())
                .lineSpacing(BULLET_LEADING)
                .itemSpacing(PLAIN_ITEM_GAP)
                .margin(new DocumentInsets(HEADER_TO_BODY, 0, 0, SECTION_INDENT)));
    }
}
