package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.DASH_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.DASH_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.HAIRLINE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.HEADING_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.PLATE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.TAIL_GAP;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.TIGHT_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.columnInsets;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.compact;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.dashOffset;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.style;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.titleOffset;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.trackedWidth;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.tracked;

/**
 * The three things this sheet is built from: the section heading — a short
 * dash, tracked capitals, and on the full-width bands a rule trailing to the
 * margin — the round plate a mark sits in, and the N-column band the
 * projects, certifications and achievements are laid out as.
 */
final class SerifHeadlineWidgets {

    private SerifHeadlineWidgets() {
    }

    /**
     * A section heading.
     *
     * <p>The dash and the words are anchored in a band rather than stacked,
     * because the design measures both from the page edge and not from the
     * column they sit in: a heading inside the grid and one across the page
     * put their letters on the same vertical, which is what
     * {@code leftSurfaceDistance} corrects for.</p>
     *
     * @param section             the host column
     * @param title               the heading text, drawn in capitals
     * @param width               the band's width
     * @param tail                true to trail a rule from the words to the
     *                            right edge
     * @param gapAbove            the gap above the band
     * @param leftSurfaceDistance how far this band's left edge already is
     *                            from the page edge
     */
    static void sectionHeading(SectionBuilder section, String title, double width,
                               boolean tail, double gapAbove, double leftSurfaceDistance) {
        String caps = title.toUpperCase(Locale.ROOT);
        double dashAt = dashOffset(leftSurfaceDistance);
        double titleAt = titleOffset(leftSurfaceDistance);

        DocumentTextStyle headingStyle = style(HEADING_SIZE, INK, DocumentTextDecoration.DEFAULT);
        DocumentNode dash = new LineBuilder()
                .name("HeadingDash_" + compact(title))
                .horizontal(DASH_WIDTH)
                .thickness(DASH_THICKNESS)
                .color(INK)
                .margin(DocumentInsets.zero())
                .build();
        ParagraphBuilder heading = new ParagraphBuilder()
                .name("SectionTitle_" + compact(title))
                .textStyle(headingStyle)
                .lineSpacing(TIGHT_LEADING);
        tracked(heading, caps, headingStyle);
        DocumentNode headingNode = heading.margin(DocumentInsets.zero()).build();

        // The rule is authored, so its width comes from an estimate of what
        // the letters will measure — see trackedWidth.
        double tailWidth = width - titleAt - trackedWidth(caps, HEADING_SIZE) - TAIL_GAP;
        DocumentNode tailNode = tail && tailWidth > 0
                ? new LineBuilder()
                        .name("HeadingTail_" + compact(title))
                        .horizontal(tailWidth)
                        .thickness(HAIRLINE_THICKNESS)
                        .color(RULE)
                        .margin(DocumentInsets.zero())
                        .build()
                : null;

        section.addContainer(band -> {
            band.name("SectionHeading_" + compact(title))
                    .rectangle(width, HEADING_SIZE * TIGHT_LEADING)
                    .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                    .padding(DocumentInsets.zero())
                    .margin(new DocumentInsets(gapAbove, 0, 0, 0))
                    .position(dash, dashAt, 0, LayerAlign.CENTER_LEFT)
                    .position(headingNode, titleAt, 0, LayerAlign.CENTER_LEFT);
            if (tailNode != null) {
                band.position(tailNode, 0, 0, LayerAlign.CENTER_RIGHT);
            }
        });
    }

    /**
     * A mark inside a pale round plate, clipped to the circle.
     *
     * @param plateName     node name of the plate
     * @param iconToken     the packaged mark
     * @param plateDiameter the plate's diameter
     * @return the plate node
     */
    static DocumentNode plate(String plateName, String iconToken, double plateDiameter) {
        return new ShapeContainerBuilder()
                .name(plateName)
                .circle(plateDiameter)
                .clipPolicy(ClipPolicy.CLIP_PATH)
                .fillColor(PLATE)
                .padding(DocumentInsets.zero())
                .center(new ImageBuilder()
                        .name(plateName + "Glyph")
                        .source(SerifHeadlineIcons.image(iconToken))
                        .fitToBounds(SerifHeadlineIcons.size(iconToken),
                                SerifHeadlineIcons.size(iconToken))
                        .margin(DocumentInsets.zero())
                        .build())
                .build();
    }

    /** One column of a band: its text, the mark beside it, and the mark's offset. */
    record BandColumn(SectionBuilder text, DocumentNode glyph, double glyphDx) {
    }

    /** Builds the column at a given index. */
    @FunctionalInterface
    interface ColumnFactory {

        /**
         * @param index which column, from zero
         * @return that column
         */
        BandColumn column(int index);
    }

    /**
     * A band of equal columns, each with a mark and a hairline between
     * neighbours.
     *
     * <p>It is a layer stack rather than a row because the marks hang outside
     * their columns and the separators sit exactly on the column edges —
     * both of which a row's slots would clip or push. Every child is placed
     * by its own left and right insets instead, computed by
     * {@link SerifHeadlineStyles#columnInsets}.</p>
     *
     * <p>The three passes are the drawing order: text first, then the marks
     * over it, then the separators.</p>
     *
     * @param section         the host column
     * @param name            node-name stem for the band and its parts
     * @param bandWidth       the band's width
     * @param columns         how many columns
     * @param gapAbove        the gap above the band
     * @param separatorHeight how tall the hairlines between columns are
     * @param factory         builds each column
     */
    static void columnBand(SectionBuilder section, String name, double bandWidth,
                           int columns, double gapAbove, double separatorHeight,
                           ColumnFactory factory) {
        double columnWidth = bandWidth / columns;
        List<BandColumn> parts = new ArrayList<>(columns);
        for (int i = 0; i < columns; i++) {
            parts.add(factory.column(i));
        }
        section.addLayerStack(stack -> {
            stack.name(name + "Band")
                    .padding(DocumentInsets.zero())
                    .margin(new DocumentInsets(gapAbove, 0, 0, 0));
            for (int i = 0; i < columns; i++) {
                stack.layer(parts.get(i).text()
                        .margin(columnInsets(bandWidth, columnWidth, i, 0.0, 0.0))
                        .build(), LayerAlign.TOP_LEFT);
            }
            for (int i = 0; i < columns; i++) {
                BandColumn part = parts.get(i);
                if (part.glyph() == null) {
                    continue;
                }
                stack.layer(sleeve(name + "Glyph_" + i, part.glyph(),
                                columnInsets(bandWidth, columnWidth, i, part.glyphDx(), 0.0)),
                        LayerAlign.TOP_LEFT);
            }
            for (int i = 1; i < columns; i++) {
                stack.layer(sleeve(name + "Separator_" + i,
                                separatorLine(name, i, separatorHeight),
                                columnInsets(bandWidth, columnWidth, i, 0.0,
                                        HAIRLINE_THICKNESS)),
                        LayerAlign.TOP_LEFT);
            }
        });
    }

    /**
     * A layer carries its position as margins, and a node built elsewhere
     * already has its own; wrapping it in a section gives the insets
     * somewhere to live without touching the node.
     */
    private static DocumentNode sleeve(String name, DocumentNode child, DocumentInsets margin) {
        SectionBuilder sleeve = new SectionBuilder();
        sleeve.name(name).spacing(0).padding(DocumentInsets.zero()).margin(margin);
        sleeve.add(child);
        return sleeve.build();
    }

    private static DocumentNode separatorLine(String name, int index, double height) {
        return new LineBuilder()
                .name(name + "SeparatorLine_" + index)
                .vertical(height)
                .thickness(HAIRLINE_THICKNESS)
                .color(RULE)
                .margin(DocumentInsets.zero())
                .build();
    }
}
