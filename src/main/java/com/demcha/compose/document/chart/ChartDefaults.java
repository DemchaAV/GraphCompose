package com.demcha.compose.document.chart;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import java.util.List;

/**
 * Built-in, theme-independent defaults for the chart subsystem.
 *
 * <p>The active {@code BusinessTheme} is baked into nodes at authoring time and
 * is not reachable during the layout pass, so a chart must be able to render
 * fully styled from these constants alone. A future enhancement can let the DSL
 * builder resolve a document-matched {@link ChartTheme} at authoring time; until
 * then {@link #DEFAULT_THEME} supplies a professional, deterministic palette and
 * typography.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public final class ChartDefaults {

    /**
     * Fraction of a category slot a bar group fills when no style overrides it.
     */
    public static final double BAR_WIDTH_RATIO = 0.72;

    /**
     * Default number of axis tick intervals requested from {@link NiceScale}.
     */
    public static final int TARGET_TICKS = 5;

    /**
     * Default professional series palette (Tableau-10 inspired, deterministic).
     */
    public static final List<DocumentPaint> DEFAULT_PALETTE = List.of(
            DocumentPaint.solid(DocumentColor.rgb(78, 121, 167)),
            DocumentPaint.solid(DocumentColor.rgb(242, 142, 43)),
            DocumentPaint.solid(DocumentColor.rgb(89, 161, 79)),
            DocumentPaint.solid(DocumentColor.rgb(225, 87, 89)),
            DocumentPaint.solid(DocumentColor.rgb(118, 183, 178)),
            DocumentPaint.solid(DocumentColor.rgb(237, 201, 72)),
            DocumentPaint.solid(DocumentColor.rgb(176, 122, 161)),
            DocumentPaint.solid(DocumentColor.rgb(156, 117, 95)));

    /**
     * Default grid line stroke — a thin light-grey rule.
     */
    public static final DocumentStroke DEFAULT_GRID_STROKE =
            DocumentStroke.of(DocumentColor.rgb(224, 224, 224), 0.5);

    /**
     * Default tick / category label style.
     */
    public static final DocumentTextStyle AXIS_TEXT_STYLE = DocumentTextStyle.builder()
            .fontName(FontName.HELVETICA)
            .size(8)
            .color(DocumentColor.rgb(90, 90, 90))
            .build();

    /**
     * Default legend label style.
     */
    public static final DocumentTextStyle LEGEND_TEXT_STYLE = DocumentTextStyle.builder()
            .fontName(FontName.HELVETICA)
            .size(9)
            .color(DocumentColor.rgb(60, 60, 60))
            .build();

    /**
     * Default value-label style.
     */
    public static final DocumentTextStyle VALUE_LABEL_TEXT_STYLE = DocumentTextStyle.builder()
            .fontName(FontName.HELVETICA)
            .size(8)
            .color(DocumentColor.rgb(60, 60, 60))
            .build();

    /**
     * Default value-label halo — a white chip behind line-chart value labels so
     * digits stay legible where line strokes cross them. Charts rendered on a
     * non-white surface should override it with the surface colour via
     * {@code ChartStyle.valueLabelHalo(...)}.
     */
    public static final DocumentPaint VALUE_LABEL_HALO =
            DocumentPaint.solid(DocumentColor.WHITE);

    /**
     * Default pie/donut slice separator — a white 1pt stroke between slices.
     */
    public static final DocumentStroke SLICE_STROKE =
            DocumentStroke.of(DocumentColor.WHITE, 1.0);

    /**
     * Default donut-centre KPI text style.
     */
    public static final DocumentTextStyle DONUT_CENTER_TEXT_STYLE = DocumentTextStyle.builder()
            .fontName(FontName.HELVETICA_BOLD)
            .size(13)
            .color(DocumentColor.rgb(45, 45, 45))
            .build();

    /**
     * Arc tessellation step for pie/donut sector polygons, in degrees.
     */
    public static final double SECTOR_TESSELLATION_STEP_DEGREES = 3.0;

    /**
     * Built-in default chart theme used when no document theme is supplied.
     */
    public static final ChartTheme DEFAULT_THEME = new ChartTheme(
            DEFAULT_PALETTE,
            DEFAULT_GRID_STROKE,
            AXIS_TEXT_STYLE,
            LEGEND_TEXT_STYLE,
            VALUE_LABEL_TEXT_STYLE,
            VALUE_LABEL_HALO);

    private ChartDefaults() {
    }
}
