package com.demcha.examples.features.text;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.emoji.EmojiLibrary;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Side-by-side comparison: each starter emoji drawn as inline <b>SVG (vector)</b>
 * via {@code RichText.svgIcon(...)} and as inline <b>PNG (raster)</b> via
 * {@code RichText.image(...)}. The PNG is produced by rendering the glyph alone
 * through the engine and rasterising that page, so the two columns show the same
 * artwork down two different inline paths — the vector stays crisp at any zoom,
 * the raster is pixel-fixed.
 */
public final class EmojiSvgVsPngExample {

    private static final String[][] EMOJI = {
            {"star", ":star:"},
            {"white_check_mark", ":white_check_mark:"},
            {"warning", ":warning:"},
            {"information_source", ":information_source:"},
            {"red_circle", ":red_circle:"},
            {"large_blue_circle", ":large_blue_circle:"},
            {"green_circle", ":green_circle:"},
            {"purple_circle", ":purple_circle:"},
    };

    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(102, 106, 118);
    private static final DocumentColor RULE = DocumentColor.rgb(180, 188, 200);
    private static final DocumentColor HEADER_FILL = DocumentColor.rgb(20, 60, 75);
    private static final double GLYPH_PT = 22;

    private EmojiSvgVsPngExample() {
    }

    private record Glyph(String code, SvgIcon icon, byte[] png) {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/text", "emoji-svg-vs-png.pdf");
        EmojiLibrary lib = EmojiLibrary.getDefault();

        // Rasterise up front so the table lambda stays free of checked exceptions.
        List<Glyph> glyphs = new ArrayList<>();
        for (String[] e : EMOJI) {
            SvgIcon icon = lib.require(e[0]);
            glyphs.add(new Glyph(e[1], icon, rasterise(icon)));
        }

        DocumentTableStyle bordered = DocumentTableStyle.builder()
                .stroke(DocumentStroke.of(RULE, 0.6))
                .padding(DocumentInsets.of(8))
                .build();
        DocumentTableStyle headerStyle = DocumentTableStyle.builder()
                .fillColor(HEADER_FILL)
                .stroke(DocumentStroke.of(RULE, 0.6))
                .padding(DocumentInsets.of(9))
                .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD)
                        .color(DocumentColor.WHITE).build())
                .build();
        DocumentTableStyle codeStyle = DocumentTableStyle.builder()
                .stroke(DocumentStroke.of(RULE, 0.6))
                .padding(DocumentInsets.of(8))
                .textStyle(DocumentTextStyle.builder().fontName(FontName.COURIER).size(11).color(INK).build())
                .build();

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(32, 32, 32, 32)
                .create()) {
            document.pageFlow()
                    .name("EmojiSvgVsPng")
                    .spacing(12)
                    .addSection("Hero", section -> section
                            .addParagraph(p -> p.text("Emoji rendering — SVG (vector) vs PNG (raster)")
                                    .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD)
                                            .size(16).color(INK).build())
                                    .margin(DocumentInsets.zero()))
                            .addParagraph(p -> p.text("Both columns draw the same glyph at " + (int) GLYPH_PT
                                            + " pt: the SVG column is the engine's native vector render, the PNG column "
                                            + "rasterises that glyph and embeds it as an image.")
                                    .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA)
                                            .size(9).color(MUTED).build())
                                    .margin(DocumentInsets.zero())))
                    .addSection("Table", section -> section.addTable(table -> {
                        TableBuilder t = table
                                .name("SvgVsPng")
                                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto(), DocumentTableColumn.auto())
                                .defaultCellStyle(bordered)
                                .headerRow("Shortcode", "SVG (vector)", "PNG (raster)")
                                .headerStyle(headerStyle);
                        for (Glyph g : glyphs) {
                            t.rowCells(
                                    DocumentTableCell.text(g.code()).withStyle(codeStyle),
                                    DocumentTableCell.node(svgCell(g.icon())),
                                    DocumentTableCell.node(pngCell(g.png())));
                        }
                    }))
                    .build();
            document.buildPdf();
        }
        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static DocumentNode svgCell(SvgIcon icon) {
        return new ParagraphBuilder().rich(r -> r.svgIcon(icon, GLYPH_PT)).build();
    }

    private static DocumentNode pngCell(byte[] png) {
        return new ParagraphBuilder()
                .rich(r -> r.image(DocumentImageData.fromBytes(png), GLYPH_PT, GLYPH_PT))
                .build();
    }

    /** Renders the glyph alone through the engine, then rasterises that page to PNG bytes. */
    private static byte[] rasterise(SvgIcon icon) throws Exception {
        double box = 24.0;
        byte[] glyphPdf;
        try (DocumentSession g = GraphCompose.document().pageSize(box, box).margin(0, 0, 0, 0).create()) {
            g.dsl().pageFlow().name("g")
                    .addParagraph(p -> p.svgIcon(icon, box).margin(DocumentInsets.zero()))
                    .build();
            glyphPdf = g.toPdfBytes();
        }
        try (PDDocument doc = Loader.loadPDF(glyphPdf)) {
            BufferedImage image = new PDFRenderer(doc).renderImageWithDPI(0, 96f, ImageType.ARGB);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ImageIO.write(image, "png", buffer);
            return buffer.toByteArray();
        }
    }
}
