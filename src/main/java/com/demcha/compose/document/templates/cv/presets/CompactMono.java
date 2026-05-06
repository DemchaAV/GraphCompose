package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.components.Header;
import com.demcha.compose.document.templates.components.Module;
import com.demcha.compose.document.templates.cv.builder.CvBuilder;
import com.demcha.compose.document.templates.cv.layouts.SingleColumn;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.templates.themes.Spacing;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;

/**
 * Templates v2 "Compact Mono" CV preset.
 *
 * <p>Dense, code-flavoured CV using monospace typography. Ideal for
 * engineering / tooling roles where the document itself signals
 * "I write code". Compact spacing rhythm keeps the page tight; the
 * teal-cyan accent gives section headings a discrete pop without
 * shouting.</p>
 *
 * <p>Visual signature ported from the legacy
 * {@code CompactMonoCvTemplateComposer}:</p>
 *
 * <ul>
 *   <li>INK    = #1C222A (near-black ink for body text)</li>
 *   <li>HEADER = #121820 (very dark for headings — almost ink)</li>
 *   <li>ACCENT = #007E97 (teal-cyan for highlights)</li>
 *   <li>Courier family — monospace throughout</li>
 * </ul>
 *
 * <p>To customise: copy the body of {@link #create(BusinessTheme)} into
 * your own class and tweak any of the styles or spacing tokens.</p>
 */
public final class CompactMono {

    /** Stable template identifier. */
    public static final String ID = "compact-mono";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Compact Mono";

    private CompactMono() {
        // utility class — not instantiable
    }

    /**
     * Builds a fresh {@code Compact Mono} template configured for the
     * given business theme.
     *
     * @param theme active business theme (palette + typography are
     *              overridden by the preset's monospace-specific tokens)
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<CvSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.compact();

        DocumentTextStyle headingStyle = DocumentTextStyle.builder()
                .fontName(FontName.COURIER_BOLD)
                .size(13.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(18, 24, 32))       // HEADER almost-black
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.COURIER)
                .size(9.5)
                .color(DocumentColor.rgb(28, 34, 42))       // INK
                .build();

        Module.Style moduleStyle = new Module.Style(
                headingStyle,
                bodyStyle,
                spacing.sectionTitleAbove(),
                spacing.sectionTitleBelow());

        return CvBuilder.builder()
                .id(ID)
                .displayName(DISPLAY_NAME)
                .theme(theme)
                .spacing(spacing)
                .layout(SingleColumn.layout()
                        .moduleGap(spacing.moduleGap()))
                .header(Header.rightAligned(theme, spacing))
                .moduleStyle(moduleStyle)
                .place(SingleColumn.MAIN,
                        "Professional Summary",
                        "Technical Skills",
                        "Education & Certifications",
                        "Projects",
                        "Professional Experience",
                        "Additional Information")
                .build();
    }
}
