package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.ACCENT_RULE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.DISPLAY_FONT;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADING_RULE_OFFSET;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADING_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADING_SIZE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADING_TRACKING;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.HEADING_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.SPACE_RATIO;
import static com.demcha.compose.document.templates.cv.presets.VioletGridStyles.style;

/**
 * The idioms this sheet repeats: the letter-spaced run, the heading followed
 * by its hairline, a disc on its own, and a line of text as a node.
 */
final class VioletGridWidgets {

    private VioletGridWidgets() {
    }

    /**
     * Letter-spaced text, as one inline run per character.
     *
     * <p>A text style carries no tracking, so the gap between two letters is a
     * space run set at whatever size makes that space the wanted number of
     * points wide — which is why the tracking is a number rather than a fixed
     * word space. The gap is set in the BODY face even under display text: the
     * display face's space is narrow enough that the wanted gap would need a
     * run taller than the type it separates, and an inline run sets the line
     * box. A space has no glyph to give the substitution away.</p>
     *
     * @param name     the node name
     * @param text     the text to space out
     * @param font     the face the letters are set in
     * @param size     the size the letters are set at
     * @param color    the colour of the letters
     * @param bold     whether the letters are bold
     * @param tracking the gap in points; zero or less writes the text plainly
     * @return the paragraph, unbuilt, so a caller can still give it a margin
     */
    static ParagraphBuilder tracked(String name, String text, FontName font, double size,
                                    DocumentColor color, boolean bold, double tracking) {
        DocumentTextStyle glyph = style(font, size, color, bold);
        ParagraphBuilder paragraph = new ParagraphBuilder();
        paragraph.name(name);
        paragraph.lineSpacing(0);
        paragraph.textStyle(glyph);
        if (tracking <= 0.0) {
            paragraph.text(text);
            return paragraph;
        }
        DocumentTextStyle gap = style(BODY_FONT, tracking / SPACE_RATIO, color, bold);
        for (int index = 0; index < text.length(); index++) {
            paragraph.inlineText(String.valueOf(text.charAt(index)), glyph);
            if (index < text.length() - 1) {
                paragraph.inlineText(" ", gap);
            }
        }
        return paragraph;
    }

    /**
     * A tracked caps heading followed by a hairline to the right margin.
     *
     * <p>The text column takes what its words need and the rule column takes
     * what is left, which is what makes the rule start where the text ends
     * without anything having to know how wide the text is. Returned as a node
     * rather than added to a builder, so one method serves callers holding
     * different builder types.</p>
     */
    static DocumentNode headingRow(String name, String text, double topMargin) {
        RowBuilder row = new RowBuilder();
        row.name(name + "Heading");
        row.spacing(0);
        row.margin(new DocumentInsets(topMargin, 0, 0, 0));
        row.columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1.0));
        row.addSection(name + "HeadingText", cell -> {
            cell.spacing(0);
            cell.add(tracked(name + "HeadingLabel", text, DISPLAY_FONT, HEADING_SIZE,
                    ACCENT, true, HEADING_TRACKING).build());
        });
        row.addSection(name + "HeadingRule", cell -> {
            cell.spacing(0);
            cell.padding((float) HEADING_RULE_OFFSET, 0f, 0f, (float) HEADING_TO_RULE);
            cell.addLine(line -> line
                    .name(name + "HeadingRuleLine")
                    .fill()
                    .thickness(HEADING_RULE_THICKNESS)
                    .color(ACCENT_RULE));
        });
        return row.build();
    }

    /** A disc on its own, in a paragraph whose line box is set by its type. */
    static DocumentNode markerDot(String name, double diameter, double boxSize,
                                  DocumentColor fill) {
        ParagraphBuilder paragraph = new ParagraphBuilder();
        paragraph.name(name);
        paragraph.lineSpacing(0);
        paragraph.textStyle(style(BODY_FONT, boxSize, fill, false));
        paragraph.dot(diameter, fill);
        return paragraph.build();
    }

    /** One line of text as a node. */
    static DocumentNode text(String name, String value, DocumentTextStyle textStyle) {
        return new ParagraphBuilder()
                .name(name)
                .text(value)
                .lineSpacing(0)
                .textStyle(textStyle)
                .build();
    }

    /** Places a mark inline, on the baseline of the paragraph it opens. */
    static void inlineIcon(ParagraphBuilder paragraph, String token) {
        paragraph.inlineSvgIcon(VioletGridIcons.icon(token), VioletGridIcons.size(token),
                InlineImageAlignment.CENTER);
    }

    /** A body, one entry per line the document wrote. */
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
