package com.demcha.compose.document.templates.theme;

import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;

import java.awt.Color;

/**
 * Shared visual tokens for weekly schedule templates.
 */
public record WeeklyScheduleTheme(
        Color titleColor,
        Color accentColor,
        Color bodyColor,
        Color mutedTextColor,
        Color gridBorderColor,
        Color bandFillColor,
        Color nameColumnFillColor,
        Color emptyCellFillColor,
        FontName titleFont,
        FontName bodyFont,
        double titleFontSize,
        double weekLabelFontSize,
        double dayLabelFontSize,
        double noteFontSize,
        double metricFontSize,
        double personNameFontSize,
        double cellFontSize,
        double footerFontSize,
        double rootSpacing,
        double sectionSpacing,
        double nameColumnWidth,
        double bandPaddingVertical,
        double bandPaddingHorizontal,
        double bodyPaddingVertical,
        double bodyPaddingHorizontal
) {
    public TextStyle titleStyle() {
        return TextStyle.builder()
                .fontName(titleFont)
                .size(titleFontSize)
                .decoration(TextDecoration.BOLD)
                .color(titleColor)
                .build();
    }

    public TextStyle weekLabelStyle() {
        return TextStyle.builder()
                .fontName(titleFont)
                .size(weekLabelFontSize)
                .decoration(TextDecoration.BOLD)
                .color(accentColor)
                .build();
    }

    public TextStyle dayLabelStyle() {
        return TextStyle.builder()
                .fontName(titleFont)
                .size(dayLabelFontSize)
                .decoration(TextDecoration.BOLD)
                .color(titleColor)
                .build();
    }

    public TextStyle noteStyle() {
        return TextStyle.builder()
                .fontName(bodyFont)
                .size(noteFontSize)
                .decoration(TextDecoration.DEFAULT)
                .color(bodyColor)
                .build();
    }

    public TextStyle metricStyle() {
        return TextStyle.builder()
                .fontName(titleFont)
                .size(metricFontSize)
                .decoration(TextDecoration.BOLD)
                .color(accentColor)
                .build();
    }

    public TextStyle personNameStyle() {
        return TextStyle.builder()
                .fontName(titleFont)
                .size(personNameFontSize)
                .decoration(TextDecoration.BOLD)
                .color(titleColor)
                .build();
    }

    public TextStyle cellTextStyle(Color textColor) {
        return TextStyle.builder()
                .fontName(bodyFont)
                .size(cellFontSize)
                .decoration(TextDecoration.DEFAULT)
                .color(textColor == null ? bodyColor : textColor)
                .build();
    }

    public TextStyle categoryLabelStyle(Color textColor) {
        return TextStyle.builder()
                .fontName(titleFont)
                .size(metricFontSize)
                .decoration(TextDecoration.BOLD)
                .color(textColor == null ? titleColor : textColor)
                .build();
    }

    public TextStyle footerStyle() {
        return TextStyle.builder()
                .fontName(bodyFont)
                .size(footerFontSize)
                .decoration(TextDecoration.DEFAULT)
                .color(mutedTextColor)
                .build();
    }

    public static WeeklyScheduleTheme defaultTheme() {
        return new WeeklyScheduleTheme(
                new Color(21, 46, 86),
                new Color(44, 107, 184),
                new Color(44, 52, 64),
                new Color(108, 121, 142),
                new Color(103, 120, 145),
                new Color(239, 244, 250),
                new Color(233, 240, 249),
                Color.WHITE,
                FontName.POPPINS,
                FontName.HELVETICA,
                26,
                13,
                11.2,
                8.8,
                9.4,
                10.4,
                8.6,
                8.8,
                7,
                5,
                112,
                6,
                8,
                7,
                8);
    }
}
