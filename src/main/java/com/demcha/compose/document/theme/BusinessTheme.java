package com.demcha.compose.document.theme;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;

import java.awt.Color;
import java.util.Objects;

/**
 * Composable design-token bundle for business documents.
 *
 * <p>A {@code BusinessTheme} groups a {@link DocumentPalette}, a
 * {@link SpacingScale}, a {@link TextScale}, and a {@link TablePreset} so that
 * an invoice, a proposal, and a status report rendered through the same theme
 * look like a single product instead of three independently styled documents.</p>
 *
 * <p>Use one of the built-in presets for an immediate style:
 * {@link #classic()}, {@link #modern()}, {@link #executive()}.</p>
 *
 * @param name human-readable theme identifier (used for diagnostics)
 * @param palette color tokens
 * @param spacing spacing scale
 * @param text text-style scale
 * @param table table preset
 * @param pageBackground optional page-wide background tint, or {@code null}
 *
 * @author Artem Demchyshyn
 */
public record BusinessTheme(
        String name,
        DocumentPalette palette,
        SpacingScale spacing,
        TextScale text,
        TablePreset table,
        DocumentColor pageBackground
) {
    /**
     * Validates required token bundles and normalises an empty {@code name}.
     */
    public BusinessTheme {
        name = name == null ? "" : name;
        Objects.requireNonNull(palette, "palette");
        Objects.requireNonNull(spacing, "spacing");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(table, "table");
    }

    /**
     * "Classic" theme — clean white surfaces, navy primary, bright accent.
     * Good baseline for invoices and one-page proposals.
     *
     * @return classic theme
     */
    public static BusinessTheme classic() {
        DocumentPalette palette = DocumentPalette.of(
                new Color(28, 38, 60),     // primary navy
                new Color(40, 90, 200),    // bright accent blue
                Color.WHITE,               // surface
                new Color(245, 247, 250),  // surface muted
                new Color(28, 38, 60),     // text primary
                new Color(110, 120, 140),  // text muted
                new Color(210, 218, 230)); // rule
        SpacingScale spacing = SpacingScale.defaultScale();
        TextScale text = textScale(palette, FontName.HELVETICA, FontName.HELVETICA_BOLD,
                26, 16, 13, 11, 9);
        TablePreset table = tablePreset(palette, spacing);
        return new BusinessTheme("classic", palette, spacing, text, table, null);
    }

    /**
     * "Modern" theme — warm cream paper, deep teal primary, gold accent. Ideal
     * for cinematic proposals and pitch decks rendered to PDF.
     *
     * @return modern theme
     */
    public static BusinessTheme modern() {
        DocumentPalette palette = DocumentPalette.of(
                new Color(20, 60, 75),
                new Color(196, 153, 76),
                new Color(252, 248, 240),  // cream surface
                new Color(244, 238, 228),  // soft tan panel
                new Color(34, 38, 50),
                new Color(110, 110, 120),
                new Color(212, 200, 178));
        SpacingScale spacing = new SpacingScale(4.0, 8.0, 14.0, 22.0, 36.0);
        TextScale text = textScale(palette, FontName.HELVETICA, FontName.HELVETICA_BOLD,
                28, 17, 13, 11, 10);
        TablePreset table = tablePreset(palette, spacing);
        return new BusinessTheme("modern", palette, spacing, text, table, palette.surface());
    }

    /**
     * "Executive" theme — slate panels with a gold accent on a near-white
     * surface. Tuned for board reports and quarterly summaries.
     *
     * @return executive theme
     */
    public static BusinessTheme executive() {
        DocumentPalette palette = DocumentPalette.of(
                new Color(36, 42, 54),     // slate primary
                new Color(174, 134, 70),   // muted gold accent
                new Color(248, 248, 248),
                new Color(232, 232, 235),
                new Color(36, 42, 54),
                new Color(110, 116, 124),
                new Color(214, 216, 220));
        SpacingScale spacing = new SpacingScale(4.0, 8.0, 12.0, 24.0, 40.0);
        TextScale text = textScale(palette, FontName.HELVETICA, FontName.TIMES_ROMAN,
                24, 15, 12, 11, 9);
        TablePreset table = tablePreset(palette, spacing);
        return new BusinessTheme("executive", palette, spacing, text, table, null);
    }

    /**
     * Returns a copy of this theme with the page background overridden.
     *
     * @param color new background color, or {@code null} to clear
     * @return updated theme
     */
    public BusinessTheme withPageBackground(DocumentColor color) {
        return new BusinessTheme(name, palette, spacing, text, table, color);
    }

    /**
     * Returns a copy of this theme with a different name (useful for forks).
     *
     * @param name new theme name
     * @return renamed theme
     */
    public BusinessTheme withName(String name) {
        return new BusinessTheme(name, palette, spacing, text, table, pageBackground);
    }

    private static TextScale textScale(DocumentPalette palette,
                                       FontName bodyFont,
                                       FontName headingFont,
                                       double h1,
                                       double h2,
                                       double h3,
                                       double body,
                                       double caption) {
        return new TextScale(
                style(headingFont, h1, DocumentTextDecoration.BOLD, palette.primary()),
                style(headingFont, h2, DocumentTextDecoration.BOLD, palette.primary()),
                style(headingFont, h3, DocumentTextDecoration.BOLD, palette.textPrimary()),
                style(bodyFont, body, DocumentTextDecoration.DEFAULT, palette.textPrimary()),
                style(bodyFont, caption, DocumentTextDecoration.DEFAULT, palette.textMuted()),
                style(bodyFont, body, DocumentTextDecoration.BOLD, palette.textPrimary()),
                style(bodyFont, body, DocumentTextDecoration.BOLD, palette.accent()));
    }

    private static DocumentTextStyle style(FontName font,
                                           double size,
                                           DocumentTextDecoration decoration,
                                           DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .decoration(decoration)
                .color(color)
                .build();
    }

    private static TablePreset tablePreset(DocumentPalette palette, SpacingScale spacing) {
        DocumentInsets cellPadding = DocumentInsets.of(spacing.sm());
        DocumentTableStyle base = DocumentTableStyle.builder()
                .padding(cellPadding)
                .fillColor(palette.surface())
                .stroke(DocumentStroke.of(palette.rule(), 0.5))
                .build();
        DocumentTableStyle header = DocumentTableStyle.builder()
                .padding(cellPadding)
                .fillColor(palette.surfaceMuted())
                .stroke(DocumentStroke.of(palette.rule(), 0.5))
                .build();
        DocumentTableStyle totalRow = DocumentTableStyle.builder()
                .padding(cellPadding)
                .fillColor(palette.surfaceMuted())
                .stroke(DocumentStroke.of(palette.rule(), 0.5))
                .build();
        DocumentTableStyle zebra = DocumentTableStyle.builder()
                .padding(cellPadding)
                .fillColor(palette.surfaceMuted())
                .stroke(DocumentStroke.of(palette.rule(), 0.5))
                .build();
        return new TablePreset(base, header, totalRow, zebra);
    }
}
