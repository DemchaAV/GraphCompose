package com.demcha.examples.features.text;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Renders the <b>entire</b> bundled emoji set as a paginated grid — every glyph
 * the {@code graph-compose-emoji} artifact ships, drawn as inline vector icons.
 * Reads the shortcode index from the classpath, loads each glyph and flows them
 * across pages, so the PDF is a full visual catalogue of what resolves.
 */
public final class EmojiGalleryExample {

    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final double ICON_PT = 16;
    private static final int PER_PARAGRAPH = 120;

    private EmojiGalleryExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/text", "emoji-gallery.pdf");
        List<SvgIcon> glyphs = loadAllGlyphs();

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(28, 28, 28, 28)
                .create()) {
            var flow = document.pageFlow().name("EmojiGallery").spacing(6);
            flow.addParagraph(p -> p
                    .text("Noto Emoji — full set (" + glyphs.size() + " glyphs)")
                    .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD)
                            .size(15).color(INK).build())
                    .margin(DocumentInsets.zero()));
            flow.addParagraph(p -> p
                    .text("Every glyph the graph-compose-emoji artifact ships, drawn inline at "
                            + (int) ICON_PT + " pt via RichText.svgIcon.")
                    .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA)
                            .size(9).color(MUTED).build())
                    .margin(DocumentInsets.zero()));

            for (int start = 0; start < glyphs.size(); start += PER_PARAGRAPH) {
                List<SvgIcon> chunk = glyphs.subList(start, Math.min(start + PER_PARAGRAPH, glyphs.size()));
                flow.addParagraph(p -> {
                    for (SvgIcon icon : chunk) {
                        p.inlineSvgIcon(icon, ICON_PT).inlineText("  ");
                    }
                });
            }
            flow.build();
            document.buildPdf();
        }
        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    /** Loads one glyph per indexed codepoint from the classpath, skipping any that fail to parse. */
    private static List<SvgIcon> loadAllGlyphs() throws Exception {
        ClassLoader cl = EmojiGalleryExample.class.getClassLoader();
        Properties index = new Properties();
        try (InputStream in = cl.getResourceAsStream("emoji/emoji-index.properties")) {
            if (in == null) {
                throw new IllegalStateException("graph-compose-emoji not on the classpath");
            }
            index.load(in);
        }
        TreeSet<String> codepoints = new TreeSet<>();
        for (String name : index.stringPropertyNames()) {
            codepoints.add(index.getProperty(name));
        }
        List<SvgIcon> glyphs = new ArrayList<>(codepoints.size());
        for (String cp : codepoints) {
            try (InputStream in = cl.getResourceAsStream("emoji/svg/" + cp + ".svg")) {
                if (in == null) {
                    continue;
                }
                glyphs.add(SvgIcon.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            } catch (RuntimeException ignored) {
                // A glyph that uses an unsupported SVG feature is simply omitted.
            }
        }
        return glyphs;
    }
}
