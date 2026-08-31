package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.ShapeOutline;

import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAPER;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.SECTION_DISC_ICON_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.SECTION_DISC_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.SECTION_HEADING;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.SECTION_HEADING_OFFSET;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TRACK_SECTION_HEADING;

/**
 * The idioms this sheet repeats: the tracked run, the mark on its accent
 * disc, the plain node-shaped paragraph, and the heading that pairs the two.
 */
final class LumaStudioWidgets {

    private LumaStudioWidgets() {
    }

    /**
     * A run of letter-spaced text.
     *
     * <p>A text style carries no tracking, so the gap between letters is an
     * inline rectangle of the ground colour — invisible, and exactly as wide
     * as the spacing asks for. The rectangle is 0.05pt tall so it takes no
     * part in the line box.</p>
     *
     * @param name     the node name
     * @param text     the text to space out
     * @param style    the style of the letters
     * @param tracking the gap in points; zero or less writes the text plainly
     * @param ground   the colour behind the run, which the gaps are painted in
     * @return the paragraph node
     */
    static DocumentNode tracked(String name, String text, DocumentTextStyle style,
                                double tracking, DocumentColor ground) {
        return tracked(name, text, style, tracking, ground, TextVerticalAlign.DEFAULT);
    }

    /**
     * The same, seated to a given vertical alignment.
     *
     * @param name          the node name
     * @param text          the text to space out
     * @param style         the style of the letters
     * @param tracking      the gap in points
     * @param ground        the colour the gaps are painted in
     * @param verticalAlign where the text sits in its box
     * @return the paragraph node
     */
    static DocumentNode tracked(String name, String text, DocumentTextStyle style,
                                double tracking, DocumentColor ground,
                                TextVerticalAlign verticalAlign) {
        return tracked(name, text, style, tracking, ground, TextAlign.LEFT, verticalAlign);
    }

    /**
     * The same, aligned across the box it is drawn in — which the table
     * headers need and the rest of the sheet does not.
     *
     * @param name          the node name
     * @param text          the text to space out
     * @param style         the style of the letters
     * @param tracking      the gap in points
     * @param ground        the colour the gaps are painted in
     * @param align         where the run sits across its box
     * @param verticalAlign where the text sits in its box
     * @return the paragraph node
     */
    static DocumentNode tracked(String name, String text, DocumentTextStyle style,
                                double tracking, DocumentColor ground, TextAlign align,
                                TextVerticalAlign verticalAlign) {
        ParagraphBuilder paragraph = new ParagraphBuilder()
                .name(name)
                .textStyle(style)
                .align(align)
                .verticalAlign(verticalAlign)
                .lineSpacing(0)
                .margin(DocumentInsets.zero());
        if (tracking <= 0) {
            return paragraph.text(text).build();
        }
        return paragraph.rich(rich -> {
            for (int index = 0; index < text.length(); index++) {
                if (index > 0) {
                    rich.shape(new ShapeOutline.Rectangle(tracking, 0.05), ground);
                }
                rich.style(String.valueOf(text.charAt(index)), style);
            }
        }).build();
    }

    /** A plain paragraph as a node, for anchoring inside a container. */
    static DocumentNode paragraph(String text, DocumentTextStyle style, TextAlign align) {
        return new ParagraphBuilder()
                .text(text)
                .textStyle(style)
                .align(align)
                .lineSpacing(0)
                .margin(DocumentInsets.zero())
                .build();
    }

    /** The same, carrying a link when one is given. */
    static DocumentNode linkedParagraph(String text, String href, DocumentTextStyle style) {
        ParagraphBuilder paragraph = new ParagraphBuilder()
                .text(text)
                .textStyle(style)
                .align(TextAlign.LEFT)
                .lineSpacing(0)
                .margin(DocumentInsets.zero());
        if (href != null && !href.isBlank()) {
            paragraph.link(new DocumentLinkOptions(href));
        }
        return paragraph.build();
    }

    /**
     * A mark centred on an accent disc.
     *
     * <p>A layer stack over an ellipse rather than a filled shape container:
     * a container's own fill is dropped when it sits inside a parent carrying
     * a large negative margin, which is where the closing banner puts it.</p>
     */
    static DocumentNode disc(double diameter, String iconToken, double iconSize) {
        return new LayerStackBuilder()
                .name("Disc-" + iconToken)
                .back(new EllipseBuilder()
                        .name("DiscFill")
                        .circle(diameter)
                        .fillColor(ACCENT)
                        .margin(DocumentInsets.zero())
                        .build())
                .layer(LumaStudioIcons.icon(iconToken, iconSize), LayerAlign.CENTER)
                .build();
    }

    /**
     * A closing-block heading: the disc, and the tracked words on its axis.
     *
     * @param section   the host block
     * @param iconToken the mark on the disc
     * @param heading   the heading text
     * @param width     the band's width
     */
    static void sectionHead(SectionBuilder section, String iconToken, String heading,
                            double width) {
        section.addContainer(container -> container
                .name("SectionHead")
                .rectangle(width, SECTION_DISC_SIZE)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .centerLeft(disc(SECTION_DISC_SIZE, iconToken, SECTION_DISC_ICON_SIZE))
                .position(tracked("SectionHeading", heading, SECTION_HEADING,
                                TRACK_SECTION_HEADING, PAPER),
                        SECTION_HEADING_OFFSET, 0, LayerAlign.CENTER_LEFT));
    }
}
