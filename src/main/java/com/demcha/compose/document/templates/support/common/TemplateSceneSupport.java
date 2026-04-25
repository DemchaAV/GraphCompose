package com.demcha.compose.document.templates.support.common;

import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.engine.components.content.text.TextIndentStrategy;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Shared helper methods used by canonical scene composers.
 *
 * @author Artem Demchyshyn
 */
public final class TemplateSceneSupport {
    private TemplateSceneSupport() {
    }

    /**
     * Creates a normalized paragraph instruction.
     *
     * @param name semantic paragraph name
     * @param text paragraph text
     * @param style text style
     * @param align horizontal alignment
     * @param lineSpacing extra line spacing
     * @param padding inner padding
     * @param margin outer margin
     * @return paragraph instruction
     */
    public static TemplateParagraphSpec paragraph(String name,
                                                  String text,
                                                  TextStyle style,
                                                  TextAlign align,
                                                  double lineSpacing,
                                                  Padding padding,
                                                  Margin margin) {
        return paragraph(name, text, style, align, lineSpacing, null, padding, margin);
    }

    /**
     * Creates a normalized paragraph instruction with optional hyperlink metadata.
     *
     * @param name semantic paragraph name
     * @param text paragraph text
     * @param style text style
     * @param align horizontal alignment
     * @param lineSpacing extra line spacing
     * @param linkOptions optional link metadata
     * @param padding inner padding
     * @param margin outer margin
     * @return paragraph instruction
     */
    public static TemplateParagraphSpec paragraph(String name,
                                                  String text,
                                                  TextStyle style,
                                                  TextAlign align,
                                                  double lineSpacing,
                                                  DocumentLinkOptions linkOptions,
                                                  Padding padding,
                                                  Margin margin) {
        return new TemplateParagraphSpec(name, text, List.of(), style, align, lineSpacing, "", TextIndentStrategy.NONE, linkOptions, padding, margin);
    }

    /**
     * Creates a normalized paragraph instruction from inline text runs.
     *
     * @param name semantic paragraph name
     * @param inlineRuns inline text runs
     * @param style fallback text style
     * @param align horizontal alignment
     * @param lineSpacing extra line spacing
     * @param padding inner padding
     * @param margin outer margin
     * @return paragraph instruction
     */
    public static TemplateParagraphSpec inlineParagraph(String name,
                                                        List<InlineTextRun> inlineRuns,
                                                        TextStyle style,
                                                        TextAlign align,
                                                        double lineSpacing,
                                                        Padding padding,
                                                        Margin margin) {
        return new TemplateParagraphSpec(name, "", inlineRuns, style, align, lineSpacing, "", TextIndentStrategy.NONE, null, padding, margin);
    }

    /**
     * Creates a normalized paragraph instruction with legacy block-indent semantics.
     *
     * @param name semantic paragraph name
     * @param text paragraph text
     * @param style text style
     * @param align horizontal alignment
     * @param lineSpacing extra line spacing
     * @param bulletOffset first-line marker or prefix
     * @param indentStrategy text indent strategy
     * @param padding inner padding
     * @param margin outer margin
     * @return paragraph instruction
     */
    public static TemplateParagraphSpec blockParagraph(String name,
                                                       String text,
                                                       TextStyle style,
                                                       TextAlign align,
                                                       double lineSpacing,
                                                       String bulletOffset,
                                                       TextIndentStrategy indentStrategy,
                                                       Padding padding,
                                                       Margin margin) {
        return blockParagraph(name, text, style, align, lineSpacing, bulletOffset, indentStrategy, null, padding, margin);
    }

    /**
     * Creates a normalized paragraph instruction with legacy block-indent semantics
     * and optional hyperlink metadata.
     *
     * @param name semantic paragraph name
     * @param text paragraph text
     * @param style text style
     * @param align horizontal alignment
     * @param lineSpacing extra line spacing
     * @param bulletOffset first-line marker or prefix
     * @param indentStrategy text indent strategy
     * @param linkOptions optional link metadata
     * @param padding inner padding
     * @param margin outer margin
     * @return paragraph instruction
     */
    public static TemplateParagraphSpec blockParagraph(String name,
                                                       String text,
                                                       TextStyle style,
                                                       TextAlign align,
                                                       double lineSpacing,
                                                       String bulletOffset,
                                                       TextIndentStrategy indentStrategy,
                                                       DocumentLinkOptions linkOptions,
                                                       Padding padding,
                                                       Margin margin) {
        return new TemplateParagraphSpec(name, text, List.of(), style, align, lineSpacing, bulletOffset, indentStrategy, linkOptions, padding, margin);
    }

    /**
     * Creates a normalized list instruction.
     *
     * @param name semantic list name
     * @param items list items
     * @param marker list marker
     * @param style item text style
     * @param align horizontal alignment
     * @param lineSpacing extra wrapped-line spacing
     * @param itemSpacing extra space between items
     * @param padding inner padding
     * @param margin outer margin
     * @return list instruction
     */
    public static TemplateListSpec list(String name,
                                        List<String> items,
                                        ListMarker marker,
                                        TextStyle style,
                                        TextAlign align,
                                        double lineSpacing,
                                        double itemSpacing,
                                        Padding padding,
                                        Margin margin) {
        return list(name, items, marker, style, align, lineSpacing, itemSpacing, "", padding, margin);
    }

    /**
     * Creates a normalized list instruction with markerless continuation indentation.
     *
     * @param name semantic list name
     * @param items list items
     * @param marker list marker
     * @param style item text style
     * @param align horizontal alignment
     * @param lineSpacing extra wrapped-line spacing
     * @param itemSpacing extra space between items
     * @param continuationIndent markerless continuation indent text
     * @param padding inner padding
     * @param margin outer margin
     * @return list instruction
     */
    public static TemplateListSpec list(String name,
                                        List<String> items,
                                        ListMarker marker,
                                        TextStyle style,
                                        TextAlign align,
                                        double lineSpacing,
                                        double itemSpacing,
                                        String continuationIndent,
                                        Padding padding,
                                        Margin margin) {
        return new TemplateListSpec(
                name,
                items,
                marker,
                style,
                align,
                lineSpacing,
                itemSpacing,
                continuationIndent,
                true,
                padding,
                margin);
    }

    /**
     * Creates a normalized divider instruction.
     *
     * @param name semantic divider name
     * @param width divider width
     * @param thickness divider thickness
     * @param color divider color
     * @param margin outer margin
     * @return divider instruction
     */
    public static TemplateDividerSpec divider(String name,
                                              double width,
                                              double thickness,
                                              Color color,
                                              Margin margin) {
        return new TemplateDividerSpec(name, width, thickness, color, margin);
    }

    /**
     * Joins non-blank values with the supplied delimiter.
     *
     * @param delimiter delimiter between values
     * @param values values to join
     * @return joined text
     */
    public static String joinNonBlank(String delimiter, String... values) {
        List<String> nonBlank = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                nonBlank.add(value.trim());
            }
        }
        return String.join(delimiter, nonBlank);
    }

    /**
     * Converts a list of bullet items into a multi-line text block.
     *
     * @param items bullet item text
     * @return multi-line bullet text
     */
    public static String bulletText(List<String> items) {
        return sanitizeLines(items).stream()
                .map(item -> "• " + item)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    /**
     * Removes blanks and trims each line.
     *
     * @param values raw values
     * @return sanitized non-blank values
     */
    public static List<String> sanitizeLines(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>();
        for (String value : values) {
            String normalized = Objects.requireNonNullElse(value, "").trim();
            if (!normalized.isBlank()) {
                sanitized.add(normalized);
            }
        }
        return List.copyOf(sanitized);
    }

    /**
     * Removes the small markdown subset used across built-in template fixtures.
     *
     * @param value raw text
     * @return text without the small supported markdown subset
     */
    public static String stripBasicMarkdown(String value) {
        return Objects.requireNonNullElse(value, "")
                .replace("**", "")
                .replace("__", "")
                .replace("##", "")
                .replace("`", "")
                .replace("_", "")
                .replace("*", "");
    }

    /**
     * Appends a simple section header composed of a heading paragraph and a rule.
     *
     * @param target active template compose target
     * @param prefix semantic node name prefix
     * @param title section title
     * @param titleStyle title text style
     * @param ruleWidth divider width
     * @param ruleColor divider color
     * @param ruleThickness divider thickness
     * @param margin header margin
     */
    public static void addSectionHeader(TemplateComposeTarget target,
                                        String prefix,
                                        String title,
                                        TextStyle titleStyle,
                                        double ruleWidth,
                                        Color ruleColor,
                                        double ruleThickness,
                                        Margin margin) {
        target.addParagraph(paragraph(
                prefix + "Heading",
                Objects.requireNonNullElse(title, ""),
                titleStyle,
                TextAlign.LEFT,
                1.0,
                Padding.zero(),
                margin));
        target.addDivider(divider(
                prefix + "Rule",
                ruleWidth,
                ruleThickness,
                ruleColor,
                Margin.zero()));
    }
}
