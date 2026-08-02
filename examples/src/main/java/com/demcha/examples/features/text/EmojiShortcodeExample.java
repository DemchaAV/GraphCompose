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
import com.demcha.examples.support.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Runnable showcase for colour emoji by shortcode ({@code @since 1.9.0}).
 *
 * <p>{@code RichText.emoji(":star:", size)} / {@code ParagraphBuilder.inlineEmoji(...)}
 * resolve a GitHub-style shortcode to an inline vector colour glyph, drawn on the
 * text baseline — crisp at any zoom, no emoji font needed. Glyphs come from the
 * {@code graph-compose-emoji} companion artifact on the classpath (here, the
 * small original starter set). Resolution is lenient: an unknown shortcode falls
 * back to its literal text, exactly the way GitHub renders an unrecognised
 * {@code :code:}.</p>
 */
public final class EmojiShortcodeExample {
    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final DocumentColor BRAND = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor PANEL = DocumentColor.rgb(248, 244, 234);

    private EmojiShortcodeExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/text", "emoji-shortcodes.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(34, 34, 34, 34)
                .create()) {

            document.pageFlow()
                    .name("EmojiShortcodeShowcase")
                    .spacing(14)
                    .addSection("Hero", section -> section
                            .softPanel(THEME.palette().surfaceMuted(), 10, 16)
                            .accentLeft(DocumentColor.rgb(97, 40, 217), 4)
                            .spacing(6)
                            .addParagraph(p -> p
                                    .text("Colour emoji by shortcode")
                                    .textStyle(THEME.text().h1())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Write ")
                                    .accent(":rocket:", BRAND)
                                    .plain(", get ")
                                    .emoji(":rocket:", 11)
                                    .plain(" — an inline vector glyph on the baseline. Ship it ")
                                    .emoji(":white_check_mark:", 11).plain(" ")
                                    .emoji(":tada:", 11)))
                    .addSection("Status", section -> labelledRow(section,
                            "emoji(\":code:\", size) — a coloured glyph between words",
                            rich -> rich
                                    .emoji(":white_check_mark:", 10).plain(" Deploy succeeded     ")
                                    .emoji(":warning:", 10).plain(" Disk almost full     ")
                                    .emoji(":information_source:", 10).plain(" 3 updates available")))
                    .addSection("Reactions", section -> labelledRow(section,
                            "the full GitHub shortcode set resolves",
                            rich -> rich
                                    .emoji(":+1:", 11).plain("  ")
                                    .emoji(":heart:", 11).plain("  ")
                                    .emoji(":fire:", 11).plain("  ")
                                    .emoji(":100:", 11).plain("  ")
                                    .emoji(":smile:", 11).plain("  ")
                                    .emoji(":eyes:", 11).plain("  ")
                                    .emoji(":sparkles:", 11).plain("  ")
                                    .emoji(":bug:", 11).plain("  ")
                                    .emoji(":bulb:", 11)))
                    .addSection("Fallback", section -> section
                            .softPanel(PANEL, 6, 12)
                            .spacing(5)
                            .addParagraph(p -> p
                                    .text("an unknown shortcode renders as its literal text, GitHub-style")
                                    .textStyle(caption())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Known ").emoji(":tada:", 10)
                                    .plain("   ·   unknown ").emoji(":does_not_exist:", 10)
                                    .plain(" ").emoji(":not_a_real_code:", 10)
                                    .plain("  (rendered as text)")))
                    .addSection("Sizing", section -> section
                            .softPanel(PANEL, 6, 12)
                            .spacing(5)
                            .addParagraph(p -> p
                                    .text("size is the glyph's height in points; the glyph keeps its aspect ratio")
                                    .textStyle(caption())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Scales with the text  ")
                                    .emoji(":rocket:", 8).plain("  ")
                                    .emoji(":rocket:", 12).plain("  ")
                                    .emoji(":rocket:", 16).plain("  ")
                                    .emoji(":rocket:", 22)))
                    .addSection("Footer", section -> section
                            .accentTop(THEME.palette().rule(), 0.6)
                            .padding(new DocumentInsets(8, 0, 0, 0))
                            .addRich(rich -> rich
                                    .plain("Source: ")
                                    .style("examples/.../EmojiShortcodeExample.java",
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
