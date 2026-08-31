package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;

import java.util.function.Consumer;

import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.ACCENT_BAR_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.HEADING_INDENT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.MAIN_HEADING_SIZE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.MARKER_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.PAPER;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SIDEBAR_HEADING_SIZE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SIDEBAR_INK;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.compact;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.textStyle;

/**
 * The pieces this sheet repeats: the heading with its gold bar, the ringed
 * marker that caps a role, the two-space inline gap after a mark, and the
 * row wrapper every measured row is built on.
 */
final class CharcoalGoldWidgets {

    private CharcoalGoldWidgets() {
    }

    /** A sidebar heading: pale text behind the gold bar. */
    static void sidebarHeading(SectionBuilder block, String text) {
        block.addSection("Heading_" + compact(text), heading -> heading
                .spacing(0)
                .accentLeft(ACCENT, ACCENT_BAR_WIDTH)
                .padding(0f, 0f, 0f, (float) HEADING_INDENT)
                .addParagraph(p -> p
                        .name("HeadingText_" + compact(text))
                        .text(text)
                        .textStyle(textStyle(SIDEBAR_HEADING_SIZE, SIDEBAR_INK, false))));
    }

    /**
     * A main-column heading: the same gold bar, with a hairline running from
     * the words to the right edge.
     *
     * <p>The rule is a weighted cell beside an {@code auto} one, so it takes
     * whatever the words leave — no measurement needed, unlike a rule that
     * has to be given a width.</p>
     */
    static void mainHeading(SectionBuilder block, String text) {
        block.addSection("Heading_" + compact(text), heading -> {
            heading.spacing(0);
            heading.accentLeft(ACCENT, ACCENT_BAR_WIDTH);
            heading.padding(0f, 0f, 0f, (float) HEADING_INDENT);
            layeredRow(heading, "HeadingRow_" + compact(text), 0.0, 0.0, row -> {
                row.verticalAlign(RowVerticalAlign.CENTER);
                row.columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1.0));
                row.addParagraph(p -> p
                        .name("HeadingText_" + compact(text))
                        .text(text)
                        .textStyle(textStyle(MAIN_HEADING_SIZE, INK, false)));
                row.addLine(line -> line
                        .name("HeadingRule_" + compact(text))
                        .fill()
                        .thickness(RULE_THICKNESS)
                        .color(RULE)
                        .margin(new DocumentInsets(0, 0, 0, HEADING_INDENT)));
            });
        });
    }

    /** A main-column heading without the trailing rule, for the closing columns. */
    static void plainHeading(SectionBuilder block, String text) {
        block.addSection("Heading_" + compact(text), heading -> heading
                .spacing(0)
                .accentLeft(ACCENT, ACCENT_BAR_WIDTH)
                .padding(0f, 0f, 0f, (float) HEADING_INDENT)
                .addParagraph(p -> p
                        .name("HeadingText_" + compact(text))
                        .text(text)
                        .textStyle(textStyle(MAIN_HEADING_SIZE, INK, false))));
    }

    /**
     * A row wrapped in a layer stack, which is where its vertical margins
     * live.
     *
     * <p>A row carries no margin of its own, so every measured row on this
     * sheet is placed by the stack around it. That also gives the marker
     * somewhere to be positioned from.</p>
     *
     * @param parent       the host section
     * @param name         node-name stem for the stack, its layer and the row
     * @param marginTop    the gap above
     * @param marginBottom the gap below
     * @param spec         builds the row
     */
    static void layeredRow(SectionBuilder parent, String name, double marginTop,
                           double marginBottom, Consumer<RowBuilder> spec) {
        SectionBuilder layer = new SectionBuilder();
        layer.name(name + "Layer");
        layer.spacing(0);
        layer.addRow(name, spec);
        parent.addLayerStack(stack -> stack
                .name(name + "Stack")
                .margin(new DocumentInsets(marginTop, 0, marginBottom, 0))
                .layer(layer.build(), LayerAlign.TOP_LEFT, 0));
    }

    /** The hollow ring that caps a role on the experience rail. */
    static DocumentNode marker(int index) {
        return new EllipseBuilder()
                .name("MarkerRing_" + index)
                .circle(MARKER_DIAMETER)
                .fillColor(PAPER)
                .stroke(DocumentStroke.of(INK, 0.8))
                .build();
    }

    /** The two spaces this design leaves between a mark and the text after it. */
    static void inlineGap(ParagraphBuilder paragraph, DocumentTextStyle style) {
        paragraph.inlineText("  ", style);
    }
}
