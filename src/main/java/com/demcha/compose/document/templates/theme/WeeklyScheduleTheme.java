package com.demcha.compose.document.templates.theme;

import com.demcha.compose.font.FontName;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;

import java.awt.Color;

/**
 * Shared visual tokens for weekly schedule templates.
 *
 * @param titleColor title text color
 * @param accentColor accent color
 * @param bodyColor body text color
 * @param mutedTextColor muted text color
 * @param gridBorderColor grid border color
 * @param bandFillColor header band fill color
 * @param nameColumnFillColor name column fill color
 * @param emptyCellFillColor empty cell fill color
 * @param titleFont title font family
 * @param bodyFont body font family
 * @param titleFontSize title font size
 * @param weekLabelFontSize week label font size
 * @param dayLabelFontSize day label font size
 * @param noteFontSize note font size
 * @param metricFontSize metric font size
 * @param personNameFontSize person name font size
 * @param cellFontSize schedule cell font size
 * @param footerFontSize footer font size
 * @param rootSpacing spacing between root blocks
 * @param sectionSpacing spacing between sections
 * @param nameColumnWidth width of the fixed name column
 * @param bandPaddingVertical vertical padding for header bands
 * @param bandPaddingHorizontal horizontal padding for header bands
 * @param bodyPaddingVertical vertical body padding
 * @param bodyPaddingHorizontal horizontal body padding
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
    /**
     * Builds the title text style.
     *
     * @return title text style
     */
    public TextStyle titleStyle() {
        return TextStyle.builder()
                .fontName(titleFont)
                .size(titleFontSize)
                .decoration(TextDecoration.BOLD)
                .color(titleColor)
                .build();
    }

    /**
     * Builds the week-label text style.
     *
     * @return week-label text style
     */
    public TextStyle weekLabelStyle() {
        return TextStyle.builder()
                .fontName(titleFont)
                .size(weekLabelFontSize)
                .decoration(TextDecoration.BOLD)
                .color(accentColor)
                .build();
    }

    /**
     * Builds the day-label text style.
     *
     * @return day-label text style
     */
    public TextStyle dayLabelStyle() {
        return TextStyle.builder()
                .fontName(titleFont)
                .size(dayLabelFontSize)
                .decoration(TextDecoration.BOLD)
                .color(titleColor)
                .build();
    }

    /**
     * Builds the day note text style.
     *
     * @return note text style
     */
    public TextStyle noteStyle() {
        return TextStyle.builder()
                .fontName(bodyFont)
                .size(noteFontSize)
                .decoration(TextDecoration.DEFAULT)
                .color(bodyColor)
                .build();
    }

    /**
     * Builds the metric-row text style.
     *
     * @return metric text style
     */
    public TextStyle metricStyle() {
        return TextStyle.builder()
                .fontName(titleFont)
                .size(metricFontSize)
                .decoration(TextDecoration.BOLD)
                .color(accentColor)
                .build();
    }

    /**
     * Builds the person-name text style.
     *
     * @return person-name text style
     */
    public TextStyle personNameStyle() {
        return TextStyle.builder()
                .fontName(titleFont)
                .size(personNameFontSize)
                .decoration(TextDecoration.BOLD)
                .color(titleColor)
                .build();
    }

    /**
     * Builds the schedule cell text style.
     *
     * @param textColor optional override text color
     * @return cell text style
     */
    public TextStyle cellTextStyle(Color textColor) {
        return TextStyle.builder()
                .fontName(bodyFont)
                .size(cellFontSize)
                .decoration(TextDecoration.DEFAULT)
                .color(textColor == null ? bodyColor : textColor)
                .build();
    }

    /**
     * Builds the category label text style.
     *
     * @param textColor optional override text color
     * @return category label text style
     */
    public TextStyle categoryLabelStyle(Color textColor) {
        return TextStyle.builder()
                .fontName(titleFont)
                .size(metricFontSize)
                .decoration(TextDecoration.BOLD)
                .color(textColor == null ? titleColor : textColor)
                .build();
    }

    /**
     * Builds the footer text style.
     *
     * @return footer text style
     */
    public TextStyle footerStyle() {
        return TextStyle.builder()
                .fontName(bodyFont)
                .size(footerFontSize)
                .decoration(TextDecoration.DEFAULT)
                .color(mutedTextColor)
                .build();
    }

    /**
     * Returns the default weekly schedule theme.
     *
     * @return default theme
     */
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
