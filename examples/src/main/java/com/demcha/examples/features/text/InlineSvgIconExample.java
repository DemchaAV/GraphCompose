package com.demcha.examples.features.text;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.examples.support.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Runnable showcase for inline SVG-icon runs ({@code @since 1.9.0}).
 *
 * <p>Parsed {@link SvgIcon}s are placed on the text baseline with
 * {@code RichText.svgIcon(icon, size)} / {@code ParagraphBuilder.inlineSvgIcon(...)},
 * so multi-colour vector glyphs flow inside a line of text — crisp at any zoom,
 * carrying their own colours, with no dependence on the active font's glyph
 * coverage. This is the engine path for vector colour emoji: a {@code :rocket:}
 * shortcode becomes a Twemoji SVG dropped inline. The glyphs below are
 * hand-authored stand-ins (gold star, green check badge, gradient orb, info and
 * warning marks) until the {@code graph-compose-emoji} Twemoji pack ships.</p>
 */
public final class InlineSvgIconExample {
    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final DocumentColor BRAND = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor PANEL = DocumentColor.rgb(248, 244, 234);

    /** Gold five-point star. */
    private static final SvgIcon STAR = SvgIcon.parse("""
            <svg viewBox="0 0 24 24">
              <polygon points="12,2 15,9 22,9 16,14 18,21 12,17 6,21 8,14 2,9 9,9" fill="#F5A623"/>
            </svg>
            """);

    /** Green disc with a white tick — a two-layer "done" badge. */
    private static final SvgIcon CHECK = SvgIcon.parse("""
            <svg viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="11" fill="#22C55E"/>
              <path d="M7 12 L11 16 L17 8" fill="none" stroke="#FFFFFF" stroke-width="2.6"/>
            </svg>
            """);

    /** Violet gradient orb — exercises the inline gradient paint path. */
    private static final SvgIcon ORB = SvgIcon.parse("""
            <svg viewBox="0 0 24 24">
              <defs>
                <linearGradient id="g" gradientUnits="userSpaceOnUse" x1="2" y1="2" x2="22" y2="22">
                  <stop offset="0" stop-color="#A78BFA"/>
                  <stop offset="1" stop-color="#6128D9"/>
                </linearGradient>
              </defs>
              <circle cx="12" cy="12" r="11" fill="url(#g)"/>
            </svg>
            """);

    /** Blue info badge — disc, dot and stem. */
    private static final SvgIcon INFO = SvgIcon.parse("""
            <svg viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="11" fill="#3B82F6"/>
              <circle cx="12" cy="7" r="1.6" fill="#FFFFFF"/>
              <rect x="10.6" y="10" width="2.8" height="8" fill="#FFFFFF"/>
            </svg>
            """);

    /** Amber warning triangle. */
    private static final SvgIcon WARN = SvgIcon.parse("""
            <svg viewBox="0 0 24 24">
              <polygon points="12,2 23,21 1,21" fill="#F59E0B"/>
              <rect x="10.8" y="8" width="2.4" height="7" fill="#FFFFFF"/>
              <rect x="10.8" y="16.4" width="2.4" height="2.4" fill="#FFFFFF"/>
            </svg>
            """);

    private InlineSvgIconExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/text", "inline-svg-icons.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(34, 34, 34, 34)
                .create()) {

            document.pageFlow()
                    .name("InlineSvgIconShowcase")
                    .spacing(14)
                    .addSection("Hero", section -> section
                            .softPanel(THEME.palette().surfaceMuted(), 10, 16)
                            .accentLeft(DocumentColor.rgb(97, 40, 217), 4)
                            .spacing(6)
                            .addParagraph(p -> p
                                    .text("Inline SVG icons")
                                    .textStyle(THEME.text().h1())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Multi-colour vector glyphs drawn on the text baseline ")
                                    .accent("from SVG, not font glyphs", BRAND)
                                    .plain(" — the engine path for vector colour emoji. ")
                                    .svgIcon(STAR, 11).plain(" ").svgIcon(CHECK, 11)
                                    .plain(" ").svgIcon(ORB, 11)))
                    .addSection("Status", section -> labelledRow(section,
                            "svgIcon(icon, size) — a coloured glyph between words",
                            rich -> rich
                                    .svgIcon(CHECK, 10).plain(" Deploy succeeded     ")
                                    .svgIcon(WARN, 10).plain(" Disk almost full     ")
                                    .svgIcon(INFO, 10).plain(" 3 updates available")))
                    .addSection("Bullets", section -> labelledRow(section,
                            "any SvgIcon as a list bullet",
                            rich -> rich
                                    .svgIcon(STAR, 10).plain(" Crisp at any zoom — true vector     ")
                                    .svgIcon(ORB, 10).plain(" Gradients render inline     ")
                                    .svgIcon(CHECK, 10).plain(" No font glyph required")))
                    .addSection("Sizing", section -> section
                            .softPanel(PANEL, 6, 12)
                            .spacing(5)
                            .addParagraph(p -> p
                                    .text("size is the glyph's height in points; width follows the icon's aspect ratio")
                                    .textStyle(caption())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Scales with the text  ")
                                    .svgIcon(ORB, 8).plain("  ")
                                    .svgIcon(ORB, 12).plain("  ")
                                    .svgIcon(ORB, 16).plain("  ")
                                    .svgIcon(ORB, 22)))
                    .addSection("Footer", section -> section
                            .accentTop(THEME.palette().rule(), 0.6)
                            .padding(new DocumentInsets(8, 0, 0, 0))
                            .addRich(rich -> rich
                                    .plain("Source: ")
                                    .style("examples/.../InlineSvgIconExample.java",
                                            DocumentTextStyle.builder()
                                                    .fontName(FontName.COURIER)
                                                    .size(8)
                                                    .color(MUTED)
                                                    .build())))
                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static void labelledRow(SectionBuilder section, String label, Consumer<RichText> body) {
        section
                .softPanel(PANEL, 6, 12)
                .spacing(4)
                .addParagraph(p -> p
                        .text(label)
                        .textStyle(caption())
                        .margin(DocumentInsets.zero()))
                .addRich(body::accept);
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .decoration(DocumentTextDecoration.BOLD)
                .size(8.5)
                .color(MUTED)
                .build();
    }
}
