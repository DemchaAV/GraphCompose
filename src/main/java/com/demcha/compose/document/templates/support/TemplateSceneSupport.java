package com.demcha.compose.document.templates.support;

import com.demcha.compose.document.model.node.ListMarker;
import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

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
     */
    public static TemplateParagraphSpec paragraph(String name,
                                                  String text,
                                                  TextStyle style,
                                                  TextAlign align,
                                                  double lineSpacing,
                                                  Padding padding,
                                                  Margin margin) {
        return new TemplateParagraphSpec(name, text, style, align, lineSpacing, "", BlockIndentStrategy.NONE, padding, margin);
    }

    /**
     * Creates a normalized paragraph instruction with legacy block-indent semantics.
     */
    public static TemplateParagraphSpec blockParagraph(String name,
                                                       String text,
                                                       TextStyle style,
                                                       TextAlign align,
                                                       double lineSpacing,
                                                       String bulletOffset,
                                                       BlockIndentStrategy indentStrategy,
                                                       Padding padding,
                                                       Margin margin) {
        return new TemplateParagraphSpec(name, text, style, align, lineSpacing, bulletOffset, indentStrategy, padding, margin);
    }

    /**
     * Creates a normalized list instruction.
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
        return new TemplateListSpec(name, items, marker, style, align, lineSpacing, itemSpacing, true, padding, margin);
    }

    /**
     * Creates a normalized divider instruction.
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
     */
    public static String bulletText(List<String> items) {
        return sanitizeLines(items).stream()
                .map(item -> "• " + item)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    /**
     * Removes blanks and trims each line.
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
