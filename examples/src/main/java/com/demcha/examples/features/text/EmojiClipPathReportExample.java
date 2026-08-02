package com.demcha.examples.features.text;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Diagnostic table for the emoji that use SVG {@code clip-path} (the Adobe
 * Illustrator {@code <use>} + {@code clipPath} idiom), which the importer cannot
 * honour yet. Each row shows the codepoint, the glyph <b>as actually rendered</b>
 * (clip ignored), and its shortcode(s) — so the few that overflow into garbage
 * are easy to spot against the many that read fine.
 */
public final class EmojiClipPathReportExample {

    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final DocumentColor RULE = DocumentColor.rgb(180, 188, 200);
    private static final DocumentColor HEADER_FILL = DocumentColor.rgb(120, 40, 40);
    private static final double GLYPH_PT = 22;

    private EmojiClipPathReportExample() {
    }

    private record Entry(String codepoint, String shortcodes, SvgIcon icon) {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/text", "emoji-clip-path.pdf");
        ClassLoader cl = EmojiClipPathReportExample.class.getClassLoader();

        Properties index = new Properties();
        try (InputStream in = cl.getResourceAsStream("emoji/emoji-index.properties")) {
            if (in == null) {
                throw new IllegalStateException("graph-compose-emoji not on the classpath");
            }
            index.load(in);
        }
        TreeMap<String, TreeSet<String>> byCodepoint = new TreeMap<>();
        for (String name : index.stringPropertyNames()) {
            byCodepoint.computeIfAbsent(index.getProperty(name), k -> new TreeSet<>()).add(name);
        }

        List<Entry> entries = new ArrayList<>();
        for (var e : byCodepoint.entrySet()) {
            String cp = e.getKey();
            try (InputStream in = cl.getResourceAsStream("emoji/svg/" + cp + ".svg")) {
                if (in == null) {
                    continue;
                }
                String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                if (!xml.contains("clip-path")) {
                    continue;
                }
                entries.add(new Entry(cp, ":" + String.join(": :", e.getValue()) + ":", SvgIcon.parse(xml)));
            } catch (RuntimeException ignored) {
                // unparseable for another reason — out of scope for this report
            }
        }

        DocumentTableStyle cell = DocumentTableStyle.builder()
                .stroke(DocumentStroke.of(RULE, 0.5))
                .padding(DocumentInsets.of(5))
                .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(8.5).color(INK).build())
                .build();
        DocumentTableStyle header = DocumentTableStyle.builder()
                .fillColor(HEADER_FILL)
                .stroke(DocumentStroke.of(RULE, 0.5))
                .padding(DocumentInsets.of(6))
                .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA)
                        .decoration(DocumentTextDecoration.BOLD).color(DocumentColor.WHITE).build())
                .build();

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(30, 30, 30, 30)
                .create()) {
            document.pageFlow()
                    .name("EmojiClipPath")
                    .spacing(8)
                    .addParagraph(p -> p
                            .text("Emoji using SVG clip-path (" + entries.size() + " glyphs)")
                            .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA).decoration(DocumentTextDecoration.BOLD)
                                    .size(14).color(INK).build())
                            .margin(DocumentInsets.zero()))
                    .addParagraph(p -> p
                            .text("clip-path (the Illustrator <use>+clipPath idiom) is not honoured: the clip is "
                                    + "ignored and content renders unclipped. Most read fine; the few whose clip is "
                                    + "structural overflow — the Glyph column shows the actual render so they stand out.")
                            .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(9).color(MUTED).build())
                            .margin(DocumentInsets.zero()))
                    .addTable(table -> {
                        TableBuilder t = table
                                .name("ClipPath")
                                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto(), DocumentTableColumn.auto())
                                .defaultCellStyle(cell)
                                .headerRow("Codepoint", "Glyph", "Shortcode(s)")
                                .headerStyle(header)
                                .repeatHeader();
                        for (Entry entry : entries) {
                            t.rowCells(
                                    DocumentTableCell.text(entry.codepoint()),
                                    DocumentTableCell.node(glyphCell(entry.icon())),
                                    DocumentTableCell.text(entry.shortcodes()));
                        }
                    })
                    .build();
            document.buildPdf();
        }
        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static DocumentNode glyphCell(SvgIcon icon) {
        return new ParagraphBuilder().rich(r -> r.svgIcon(icon, GLYPH_PT)).build();
    }
}
