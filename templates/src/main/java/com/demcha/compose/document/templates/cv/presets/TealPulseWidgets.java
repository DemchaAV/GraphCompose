package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;

import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.ACCENT_DEEP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BADGE_BAND;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BADGE_MAIN;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_HEADER_GAP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_HEADER_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_HEADING_DROP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_HEADING_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_HEADING_TO_UNDERLINE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_HEADING_TRACKING_EM;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BAND_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.BODY_TEXT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.DOT_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.DOT_GAP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.HEADER_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.HEADING_FONT;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.MAIN_CONTENT_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.MAIN_HEADING_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.MAIN_HEADING_TRACKING_EM;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.PAPER;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.SECTION_GAP;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.compact;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.gapRun;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.spacerRuns;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.spacerStyle;
import static com.demcha.compose.document.templates.cv.presets.TealPulseStyles.style;

/**
 * The idioms this sheet repeats: the tracked run, the badge, the dotted line
 * and the two heading bars.
 */
final class TealPulseWidgets {

    private TealPulseWidgets() {
    }

    // -- tracking ----------------------------------------------------------

    /**
     * A letter-spaced run.
     *
     * <p>A text style carries no letter-spacing, so every character is its own
     * run and the gaps between them are spaces set small enough to advance the
     * wanted fraction of an em. The gap is split across several spacers rather
     * than carried on one, because a run's size sets its line's height as well
     * as its advance.</p>
     *
     * @param paragraph  the paragraph to write into
     * @param text       the text to space out
     * @param textStyle  the style of the letters
     * @param trackingEm the gap as a fraction of an em
     */
    static void tracked(ParagraphBuilder paragraph, String text, DocumentTextStyle textStyle,
                        double trackingEm) {
        int runs = spacerRuns(trackingEm);
        DocumentTextStyle spacer = spacerStyle(textStyle, trackingEm, runs);
        for (int index = 0; index < text.length(); index++) {
            paragraph.inlineText(String.valueOf(text.charAt(index)), textStyle);
            if (index + 1 < text.length()) {
                paragraph.inlineText(" ".repeat(runs), spacer);
            }
        }
    }

    /**
     * The same, with the paper laid behind every run, so a rule the heading is
     * drawn over shows only past the end of the words.
     */
    static void knockedOut(ParagraphBuilder paragraph, String text,
                           DocumentTextStyle textStyle, double trackingEm) {
        int runs = spacerRuns(trackingEm);
        DocumentTextStyle spacer = spacerStyle(textStyle, trackingEm, runs);
        for (int index = 0; index < text.length(); index++) {
            knockedOutRun(paragraph, String.valueOf(text.charAt(index)), textStyle);
            if (index + 1 < text.length()) {
                knockedOutRun(paragraph, " ".repeat(runs), spacer);
            }
        }
    }

    /** One knocked-out run. No horizontal padding, so neighbours abut. */
    private static void knockedOutRun(ParagraphBuilder paragraph, String text,
                                      DocumentTextStyle textStyle) {
        paragraph.inlineHighlight(text, textStyle, PAPER, 0,
                new DocumentInsets(HEADER_RULE_THICKNESS, 0, HEADER_RULE_THICKNESS, 0));
    }

    // -- marks -------------------------------------------------------------

    /** A teal disc owning a white glyph — the pair moves and scales as one. */
    static DocumentNode badge(String name, String token, double diameter) {
        return new ShapeContainerBuilder()
                .name("Badge_" + compact(name))
                .circle(diameter)
                .fillColor(ACCENT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .center(icon(token))
                .build();
    }

    /**
     * One mark as a node. It is wrapped in a section rather than placed
     * directly, so the vector path reaches the page rather than a coloured
     * stand-in shaped like its box.
     */
    static DocumentNode icon(String token) {
        return new SectionBuilder()
                .name("Icon_" + compact(token))
                .spacing(0)
                .padding(DocumentInsets.zero())
                .margin(DocumentInsets.zero())
                .addSvgIcon(TealPulseIcons.icon(token), TealPulseIcons.size(token))
                .build();
    }

    // -- lines -------------------------------------------------------------

    /**
     * A teal dot leading a near-black label.
     *
     * <p>Not a list: a list marker takes its item's own text colour, and here
     * the dot and the label are deliberately different colours.</p>
     */
    static DocumentNode dottedLine(String name, String text, double size, double leading) {
        DocumentTextStyle textStyle =
                style(BODY_FONT, size, BODY_TEXT, DocumentTextDecoration.DEFAULT);
        return new ParagraphBuilder()
                .name(name)
                .textStyle(textStyle)
                .align(TextAlign.LEFT)
                .lineSpacing(leading)
                .dot(DOT_DIAMETER, ACCENT)
                .inlineText(gapRun(DOT_GAP, size) + text, textStyle)
                .margin(DocumentInsets.zero())
                .build();
    }

    // -- heading bars ------------------------------------------------------

    /**
     * The main column's heading: a badge, the words, and a rule running from
     * them to the right margin.
     *
     * <p>The obvious construction is unavailable here. A row of fixed / auto /
     * weighted cells would let the layout derive the rule's length from the
     * heading's width, but a row cannot nest inside a row cell and the main
     * column is a row cell; the alternative, a shape container placing
     * children by anchor, needs every width up front.</p>
     *
     * <p>So the rule is drawn at full length from just past the badge and the
     * heading is laid over it with the paper knocked out behind every run. The
     * rule then shows exactly where the heading is not, which is the picture an
     * auto column would have produced, and its visible length still follows the
     * words rather than a width guessed while authoring.</p>
     */
    static DocumentNode sectionHeader(String title, String token, double gapBelow) {
        double railStart = BADGE_MAIN + SECTION_GAP;
        DocumentNode rail = new ShapeBuilder()
                .name("SectionHeaderRule_" + compact(title))
                .size(MAIN_CONTENT_WIDTH - railStart, HEADER_RULE_THICKNESS)
                .fillColor(ACCENT)
                .build();
        DocumentTextStyle headingStyle = style(
                HEADING_FONT, MAIN_HEADING_SIZE, ACCENT_DEEP, DocumentTextDecoration.BOLD);
        ParagraphBuilder heading = new ParagraphBuilder()
                .name("SectionHeading_" + compact(title))
                .textStyle(headingStyle)
                .align(TextAlign.LEFT)
                .lineSpacing(1.0);
        knockedOut(heading, title, headingStyle, MAIN_HEADING_TRACKING_EM);
        knockedOutRun(heading, gapRun(SECTION_GAP, MAIN_HEADING_SIZE), headingStyle);
        return new ShapeContainerBuilder()
                .name("SectionHeader_" + compact(title))
                .rectangle(MAIN_CONTENT_WIDTH, BADGE_MAIN)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .centerLeft(badge(title, token, BADGE_MAIN))
                .position(rail, railStart, 0, LayerAlign.CENTER_LEFT, 0)
                .position(heading.margin(DocumentInsets.zero()).build(),
                        railStart, 0, LayerAlign.CENTER_LEFT, 1)
                .margin(new DocumentInsets(0, 0, gapBelow, 0))
                .build();
    }

    /**
     * The closing band's heading: the rule sits UNDER the words rather than
     * after them, so its width is derivable — the column less the badge and
     * the gap — and nothing has to be knocked out.
     */
    static DocumentNode columnHeader(String title, String token, double columnWidth) {
        DocumentTextStyle headingStyle = style(
                HEADING_FONT, BAND_HEADING_SIZE, ACCENT_DEEP, DocumentTextDecoration.BOLD);
        ParagraphBuilder heading = new ParagraphBuilder()
                .name("ColumnHeading_" + compact(title))
                .textStyle(headingStyle)
                .align(TextAlign.LEFT)
                .lineSpacing(1.0);
        tracked(heading, title, headingStyle, BAND_HEADING_TRACKING_EM);
        double stackStart = BADGE_BAND + BAND_HEADER_GAP;
        DocumentNode stack = new SectionBuilder()
                .name("ColumnHeadingStack_" + compact(title))
                .spacing(0)
                .add(heading.margin(DocumentInsets.zero()).build())
                .addLine(line -> line
                        .name("ColumnHeadingRule_" + compact(title))
                        .horizontal(columnWidth - stackStart)
                        .thickness(BAND_RULE_THICKNESS)
                        .color(ACCENT)
                        .margin(new DocumentInsets(BAND_HEADING_TO_UNDERLINE, 0, 0, 0)))
                .build();
        return new ShapeContainerBuilder()
                .name("ColumnHeader_" + compact(title))
                .rectangle(columnWidth, BADGE_BAND)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .centerLeft(badge(title, token, BADGE_BAND))
                .position(stack, stackStart, BAND_HEADING_DROP, LayerAlign.CENTER_LEFT)
                .margin(new DocumentInsets(0, 0, BAND_HEADER_TO_BODY, 0))
                .build();
    }
}
