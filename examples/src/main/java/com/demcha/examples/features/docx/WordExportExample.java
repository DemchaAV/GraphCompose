package com.demcha.examples.features.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.DocxSemanticBackend;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

/**
 * One document, two outputs: the same {@link DocumentSession} renders a
 * fixed-layout PDF <em>and</em> exports an editable Word file through
 * {@link DocxSemanticBackend}. Each section demonstrates one row of the
 * DOCX capability table from {@code docs/recipes/docx-export.md}:
 * styled paragraphs and inline runs map 1:1, lists keep markers and
 * per-depth indent, tables and side-by-side rows stay tables, images
 * embed at their declared size, page breaks survive, charts fall back to
 * their data table, and pure geometry (dividers, shape containers) stays
 * PDF-only by design.
 *
 * <p>Requires {@code org.apache.poi:poi-ooxml} on the classpath — the
 * dependency is optional in the GraphCompose POM, so this module adds it
 * explicitly, exactly like a consuming project would.</p>
 *
 * @author Artem Demchyshyn
 */
public final class WordExportExample {

    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(102, 106, 118);
    private static final DocumentColor NAVY = DocumentColor.rgb(20, 45, 80);
    private static final DocumentColor GOLD = DocumentColor.rgb(196, 153, 76);
    private static final DocumentColor TEAL = DocumentColor.rgb(20, 80, 95);

    private WordExportExample() {
    }

    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/docx", "word-export-companion.pdf");
        Path docxFile = ExampleOutputPaths.prepare("features/docx", "word-export-companion.docx");

        DocumentTextStyle title = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(20)
                .color(NAVY)
                .decoration(DocumentTextDecoration.BOLD)
                .build();
        DocumentTextStyle heading = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(13)
                .color(INK)
                .decoration(DocumentTextDecoration.BOLD)
                .build();
        DocumentTextStyle body = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(11)
                .color(INK)
                .build();
        DocumentTextStyle caption = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_OBLIQUE)
                .size(10)
                .color(MUTED)
                .build();

        ChartData quarters = ChartData.builder()
                .categories("Q1", "Q2", "Q3", "Q4")
                .series("Revenue", 42.0, 55.0, 61.0, 70.0)
                .series("Profit", 12.0, 17.0, 21.0, 26.0)
                .build();
        DocumentImageData photo = demoImage();

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(595, 842)
                .margin(DocumentInsets.of(48))
                .create()) {

            // Session metadata lands in the PDF info dictionary AND the
            // Word core properties (File > Info in Word).
            document.metadata(DocumentMetadata.builder()
                    .title("GraphCompose Word export companion")
                    .author("GraphCompose")
                    .subject("Every node the semantic DOCX backend maps, falls back on, or skips")
                    .keywords("graphcompose, docx, semantic export")
                    .build());

            document.pageFlow().name("Flow").spacing(14)
                    .addParagraph("GraphCompose → Word, from one document", title)
                    .addRich(rich -> rich
                            .plain("This file exists twice: a fixed-layout ")
                            .bold("PDF")
                            .plain(" and an editable ")
                            .bold("DOCX")
                            .plain(" exported from the same session. Inline runs — ")
                            .italic("italic, ")
                            .color("colored, ", GOLD)
                            .accent("bold accents", TEAL)
                            .plain(" — survive in both."))

                    .addParagraph("Lists keep markers and indent", heading)
                    .addList(list -> list
                            .name("FlatList")
                            .textStyle(body)
                            .dash()
                            .items("Flat items use the list marker",
                                    "Word gets one paragraph per item"))
                    .addList(list -> list
                            .name("NestedList")
                            .textStyle(body)
                            .itemSpacing(2)
                            .markerFor(1, ListMarker.custom("◦"))
                            .markerFor(2, ListMarker.custom("▪"))
                            .addItem("Nested authoring", l1 -> l1
                                    .addItem("Two spaces of indent per depth in Word", l2 -> l2
                                            .addItem("Custom markers survive the export"))))

                    .addParagraph("Tables stay tables", heading)
                    .addTable(t -> t
                            .columns(DocumentTableColumn.auto(),
                                    DocumentTableColumn.auto(),
                                    DocumentTableColumn.auto())
                            .headerRow("Quarter", "Revenue", "Profit")
                            .row("Q1", "42", "12")
                            .row("Q2", "55", "17")
                            .row("Q3", "61", "21")
                            .row("Q4", "70", "26"))

                    .addParagraph("Side-by-side rows become one-row tables", heading)
                    .addRow(r -> r.spacing(14).weights(1, 1)
                            .addSection("Left", a -> a
                                    .addParagraph("Left cell — editors keep the columns.", body))
                            .addSection("Right", b -> b
                                    .addParagraph("Right cell — content stays editable.", body)))

                    .addParagraph("Images embed at their declared size", heading)
                    .addImage(i -> i.source(photo).size(180, 100))

                    .addPageBreak(pb -> pb.name("ToPage2"))

                    .addParagraph("Charts fall back to their data", heading)
                    .addParagraph("The PDF renders the vector chart; Word has no layout pass, "
                            + "so the same node exports as a categories-by-series table.", caption)
                    .addSection("Chart", s -> s.chart(ChartSpec.bar().data(quarters).build()))

                    .addParagraph("Geometry stays PDF-only by design", heading)
                    .addParagraph("The divider and the badge below render in the PDF; the "
                            + "semantic export skips pure geometry and writes the badge's "
                            + "text inline instead.", caption)
                    .addDivider(d -> d.width(420).thickness(2).color(GOLD))
                    .addCircle(72, TEAL, c -> c
                            .stroke(DocumentStroke.of(GOLD, 1.2))
                            .center(new com.demcha.compose.document.dsl.ParagraphBuilder()
                                    .text("GC")
                                    .textStyle(DocumentTextStyle.builder()
                                            .fontName(FontName.HELVETICA_BOLD).size(20)
                                            .color(DocumentColor.WHITE).build())
                                    .align(com.demcha.compose.document.node.TextAlign.CENTER)
                                    .build()))
                    .build();

            document.buildPdf();
            document.export(new DocxSemanticBackend(), docxFile);
        }
        System.out.println("Generated: " + docxFile);
        return pdfFile;
    }

    /**
     * Small deterministic gradient placeholder (a few KB) — keeps the
     * committed PDF/DOCX previews light, unlike the 1.8 MB catalog photo.
     */
    private static DocumentImageData demoImage() throws Exception {
        BufferedImage image = new BufferedImage(360, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setPaint(new GradientPaint(0, 0, new Color(20, 45, 80),
                    360, 200, new Color(20, 80, 95)));
            g.fillRect(0, 0, 360, 200);
            g.setPaint(new Color(196, 153, 76));
            g.setStroke(new BasicStroke(6f));
            g.drawLine(0, 170, 360, 110);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        return DocumentImageData.fromBytes(png.toByteArray());
    }

    public static void main(String[] args) throws Exception {
        Path output = generate();
        System.out.println("Generated: " + output);
    }
}
