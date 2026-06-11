package com.demcha.compose.document.theme;

import com.demcha.compose.document.style.*;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;

import java.awt.*;
import java.util.Objects;

/**
 * Composable design-token bundle for business documents.
 *
 * <p>A {@code BusinessTheme} groups a {@link DocumentPalette}, a
 * {@link SpacingScale}, a {@link TextScale}, and a {@link TablePreset} so that
 * an invoice, a proposal, and a status report rendered through the same theme
 * look like a single product instead of three independently styled documents.</p>
 *
 * <p>Use one of the built-in presets for an immediate style. The
 * original three are tuned for formal documents:
 * {@link #classic()}, {@link #modern()}, {@link #executive()}.
 * Four contemporary additions cover the modern document-design
 * spectrum: {@link #nordic()} (Scandinavian minimal),
 * {@link #editorial()} (warm magazine), {@link #cinematic()}
 * (dark moody surface), and {@link #monochrome()} (brutalist
 * black-and-white with one accent).</p>
 *
 * @param name           human-readable theme identifier (used for diagnostics)
 * @param palette        color tokens
 * @param spacing        spacing scale
 * @param text           text-style scale
 * @param table          table preset
 * @param pageBackground optional page-wide background tint, or {@code null}
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
        DocumentPalette palette = DocumentPalette.builder()
                .primary(new Color(28, 38, 60))      // navy
                .accent(new Color(40, 90, 200))      // bright accent blue
                .surface(Color.WHITE)
                .surfaceMuted(new Color(245, 247, 250))
                .textPrimary(new Color(28, 38, 60))
                .textMuted(new Color(110, 120, 140))
                .rule(new Color(210, 218, 230))
                .build();
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
        DocumentPalette palette = DocumentPalette.builder()
                .primary(new Color(20, 60, 75))
                .accent(new Color(196, 153, 76))
                .surface(new Color(252, 248, 240))   // cream surface
                .surfaceMuted(new Color(244, 238, 228))  // soft tan panel
                .textPrimary(new Color(34, 38, 50))
                .textMuted(new Color(110, 110, 120))
                .rule(new Color(212, 200, 178))
                .build();
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
        DocumentPalette palette = DocumentPalette.builder()
                .primary(new Color(36, 42, 54))      // slate
                .accent(new Color(174, 134, 70))     // muted gold
                .surface(new Color(248, 248, 248))
                .surfaceMuted(new Color(232, 232, 235))
                .textPrimary(new Color(36, 42, 54))
                .textMuted(new Color(110, 116, 124))
                .rule(new Color(214, 216, 220))
                .build();
        SpacingScale spacing = new SpacingScale(4.0, 8.0, 12.0, 24.0, 40.0);
        TextScale text = textScale(palette, FontName.HELVETICA, FontName.TIMES_ROMAN,
                24, 15, 12, 11, 9);
        TablePreset table = tablePreset(palette, spacing);
        return new BusinessTheme("executive", palette, spacing, text, table, null);
    }

    /**
     * "Nordic" theme — cool near-white surface, deep slate-blue
     * primary, dusty slate accent, generous spacing. Tuned for
     * design studios, product reports, and clean startup decks
     * where whitespace is the dominant visual element.
     *
     * @return nordic theme
     * @since 1.6.8
     */
    public static BusinessTheme nordic() {
        DocumentPalette palette = DocumentPalette.builder()
                .primary(new Color(36, 50, 64))      // deep slate-blue
                .accent(new Color(96, 118, 142))     // dusty slate
                .surface(new Color(252, 253, 254))   // cool near-white
                .surfaceMuted(new Color(240, 243, 246))
                .textPrimary(new Color(36, 50, 64))
                .textMuted(new Color(108, 120, 134))
                .rule(new Color(220, 226, 232))      // very subtle cool line
                .build();
        SpacingScale spacing = new SpacingScale(6.0, 12.0, 18.0, 28.0, 44.0);
        TextScale text = textScale(palette, FontName.HELVETICA, FontName.HELVETICA_BOLD,
                26, 16, 12, 10, 9);
        TablePreset table = tablePreset(palette, spacing);
        return new BusinessTheme("nordic", palette, spacing, text, table, null);
    }

    /**
     * "Editorial" theme — warm cream surface, deep ink primary,
     * brick-red accent on a serif body. Tuned for long-form
     * proposals, annual reports, and brand decks that want a
     * magazine feel.
     *
     * @return editorial theme
     * @since 1.6.8
     */
    public static BusinessTheme editorial() {
        DocumentPalette palette = DocumentPalette.builder()
                .primary(new Color(22, 22, 22))      // deep ink
                .accent(new Color(160, 60, 50))      // brick red
                .surface(new Color(250, 245, 235))   // warm cream
                .surfaceMuted(new Color(240, 232, 218))
                .textPrimary(new Color(22, 22, 22))
                .textMuted(new Color(95, 90, 85))    // warm grey
                .rule(new Color(200, 190, 175))
                .build();
        SpacingScale spacing = SpacingScale.defaultScale();
        TextScale text = textScale(palette, FontName.TIMES_ROMAN, FontName.TIMES_ROMAN,
                30, 18, 14, 11, 9);
        TablePreset table = tablePreset(palette, spacing);
        return new BusinessTheme("editorial", palette, spacing, text, table, palette.surface());
    }

    /**
     * "Cinematic" theme — deep navy surface with light text and a
     * bright copper accent. Inverts the usual dark-on-light document
     * convention; tuned for investor pitch decks, product launch
     * one-pagers, and presentations that need a moody premium feel.
     *
     * @return cinematic theme
     * @since 1.6.8
     */
    public static BusinessTheme cinematic() {
        DocumentPalette palette = DocumentPalette.builder()
                .primary(new Color(245, 248, 252))   // near-white (text on dark)
                .accent(new Color(220, 130, 50))     // bright copper
                .surface(new Color(16, 24, 36))      // deep navy SURFACE
                .surfaceMuted(new Color(28, 36, 48)) // slightly lighter navy panel
                .textPrimary(new Color(245, 248, 252))
                .textMuted(new Color(160, 170, 188)) // muted light blue-grey
                .rule(new Color(54, 64, 78))         // subtle on-dark rule
                .build();
        SpacingScale spacing = new SpacingScale(4.0, 8.0, 14.0, 24.0, 40.0);
        TextScale text = textScale(palette, FontName.HELVETICA, FontName.HELVETICA_BOLD,
                30, 18, 14, 11, 9);
        TablePreset table = tablePreset(palette, spacing);
        return new BusinessTheme("cinematic", palette, spacing, text, table, palette.surface());
    }

    /**
     * "Monochrome" theme — pure black on white with a single bold
     * yellow accent. Tuned for design-studio one-pagers, fashion-
     * magazine-style covers, and brutalist editorial layouts where
     * typographic contrast is the entire identity.
     *
     * @return monochrome theme
     * @since 1.6.8
     */
    public static BusinessTheme monochrome() {
        DocumentPalette palette = DocumentPalette.builder()
                .primary(new Color(0, 0, 0))         // pure black
                .accent(new Color(240, 196, 25))     // bold yellow
                .surface(new Color(255, 255, 255))   // pure white
                .surfaceMuted(new Color(244, 244, 244))
                .textPrimary(new Color(0, 0, 0))
                .textMuted(new Color(115, 115, 115)) // medium grey
                .rule(new Color(0, 0, 0))            // bold rules
                .build();
        SpacingScale spacing = new SpacingScale(4.0, 8.0, 12.0, 20.0, 36.0);
        TextScale text = textScale(palette, FontName.HELVETICA, FontName.HELVETICA_BOLD,
                32, 20, 14, 11, 9);
        TablePreset table = tablePreset(palette, spacing);
        return new BusinessTheme("monochrome", palette, spacing, text, table, null);
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
