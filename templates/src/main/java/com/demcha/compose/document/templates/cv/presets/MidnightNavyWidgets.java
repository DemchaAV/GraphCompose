package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ASIDE_DIVIDER;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ASIDE_HEAD_SIZE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ASIDE_RULE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.ASIDE_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.MAIN_HEAD_SIZE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.MAIN_INNER_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.WHITE;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.px;
import static com.demcha.compose.document.templates.cv.presets.MidnightNavyStyles.style;

/**
 * The pieces more than one part of the Midnight Navy sheet draws: the two kinds
 * of heading, the wrapper every horizontal pair goes through, and the two text
 * helpers the design's tracking needs.
 */
final class MidnightNavyWidgets {

    private MidnightNavyWidgets() {
    }

    /**
     * A row that survives inside a row cell.
     *
     * <p>A row nested directly in a row cell is refused by the layout compiler,
     * and both columns of this sheet are row cells — so every horizontal pair in
     * the preset goes through here, wrapped in a single layer of a stack.</p>
     *
     * @param parent the section to add the row to
     * @param name   the node name, for review and rollback
     * @param spec   describes the row
     */
    static void layeredRow(SectionBuilder parent, String name, Consumer<RowBuilder> spec) {
        SectionBuilder holder = new SectionBuilder();
        holder.name(name + "Holder");
        holder.addRow(name, spec);
        DocumentNode node = holder.build();
        parent.addLayerStack(stack -> stack
                .name(name + "Layer")
                .layer(node, LayerAlign.TOP_LEFT, 0));
    }

    /**
     * An aside heading over its rule.
     *
     * @param block      the block the heading opens
     * @param label      the heading
     * @param ruleWidth  the rule's length — the contact block's is shorter
     * @param gapBelow   the gap from the rule to the first line under it
     */
    static void asideHeading(SectionBuilder block, String label, double ruleWidth,
                             double gapBelow) {
        block.addParagraph(p -> p
                .name("AsideHeading" + compact(label))
                .text(label)
                .textStyle(style(ASIDE_HEAD_SIZE, WHITE, DocumentTextDecoration.BOLD)));
        block.addLine(line -> line
                .name("AsideRule" + compact(label))
                .horizontal(ruleWidth)
                .thickness(ASIDE_RULE_THICKNESS)
                .color(ASIDE_RULE)
                .margin(new DocumentInsets(px(2), 0, gapBelow - px(2), 0)));
    }

    /**
     * A main-column heading over its full-width rule.
     *
     * @param block          the block the heading opens
     * @param label          the heading
     * @param ruleThickness  the rule's weight — the achievements rule is finer
     * @param gapBelow       the gap from the rule to the first line under it
     */
    static void mainHeading(SectionBuilder block, String label, double ruleThickness,
                            double gapBelow) {
        block.addParagraph(p -> p
                .name("MainHeading" + compact(label))
                .text(label)
                .textStyle(style(MAIN_HEAD_SIZE, INK, DocumentTextDecoration.BOLD)));
        block.addLine(line -> line
                .name("MainRule" + compact(label))
                .horizontal(MAIN_INNER_WIDTH)
                .thickness(ruleThickness)
                .color(RULE)
                .margin(new DocumentInsets(px(7.3), 0, gapBelow, 0)));
    }

    /** The divider under the name plate, in the aside's own dim rule colour. */
    static void nameDivider(SectionBuilder column, double width, double leftInset, double gapAbove) {
        column.addLine(line -> line
                .name("NameDivider")
                .horizontal(width)
                .thickness(px(1))
                .color(ASIDE_DIVIDER)
                .margin(new DocumentInsets(gapAbove, 0, 0, leftInset)));
    }

    /**
     * The role line's wide tracking, as spaces between characters.
     *
     * <p>There is no letter-spacing API, so the tracking is real spaces and its
     * width depends on the face's space advance: change the family and the
     * tracking has to be re-checked.</p>
     *
     * @param text the line to track
     * @return the same line with a space between every pair of characters and
     *         three between words
     */
    static String tracked(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() * 2);
        boolean startOfWord = true;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ' ') {
                out.append("   ");
                startOfWord = true;
                continue;
            }
            if (!startOfWord) {
                out.append(' ');
            }
            out.append(ch);
            startOfWord = false;
        }
        return out.toString();
    }

    /** A label reduced to what can go in a node name. */
    static String compact(String text) {
        return text == null ? "" : text.replaceAll("[^A-Za-z0-9]", "");
    }

    /** A body split into the lines it was written as, blanks dropped. */
    static List<String> lines(String body) {
        List<String> out = new ArrayList<>();
        for (String line : body.split(String.valueOf((char) 10))) {
            if (!line.isBlank()) {
                out.add(line.strip());
            }
        }
        return out;
    }
}
