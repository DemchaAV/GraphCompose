package com.demcha.examples.features.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxExportReport;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
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
import com.demcha.compose.document.table.DocumentTableStyle;
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
 * {@link DocxSemanticBackend}. Each section shows what the Word export makes
 * of one kind of content: styled paragraphs and inline runs are Word
 * paragraphs and runs, lists are Word lists with their markers, tables and
 * side-by-side rows are tables, an image keeps its size, a page break stays
 * a page break, a chart is a Word chart with its numbers in an embedded
 * workbook, and a divider and a badge are a paragraph border and a drawn
 * shape holding its letters.
 *
 * <p>{@code document.buildDocx(path)} exports in one call. It builds a
 * default backend, so it has no report and stamps the file with the date of
 * the export. This example configures the backend instead, for the two
 * things that call does not give: {@code reportSink} hands over what Word
 * could not carry as authored, which {@code printReport} prints, and
 * {@code deterministic(true)} pins the package's clocks, so the committed
 * preview comes out as the same bytes on every run.</p>
 *
 * <p>Requires {@code io.github.demchaav:graph-compose-render-docx} on the
 * classpath, which brings Apache POI with it.</p>
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
                .fontName(FontName.HELVETICA)
                .size(20)
                .color(NAVY)
                .decoration(DocumentTextDecoration.BOLD)
                .build();
        DocumentTextStyle heading = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
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
                .fontName(FontName.HELVETICA)
                .decoration(DocumentTextDecoration.ITALIC)
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
                    .subject("What the semantic DOCX backend makes of each node, and what its report names")
                    .keywords("graphcompose, docx, semantic export")
                    .build());

            document.pageFlow().name("Flow").spacing(14)
                    .addParagraph("GraphCompose to Word, from one document", title)
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

                    .addParagraph("Lists stay Word lists, with their markers", heading)
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
                            .markerFor(1, ListMarker.custom("»"))
                            .markerFor(2, ListMarker.custom("·"))
                            .addItem("Nested authoring", l1 -> l1
                                    .addItem("Each depth is a level of one Word list", l2 -> l2
                                            .addItem("Custom markers survive the export"))))

                    .addParagraph("Tables stay tables, and keep their paint", heading)
                    .addTable(t -> t
                            .columns(DocumentTableColumn.auto(),
                                    DocumentTableColumn.auto(),
                                    DocumentTableColumn.auto())
                            // The rule reaches Word as w:tcBorders and the header band as
                            // w:shd, so the exported table reads the way it was designed
                            // rather than on Word's defaults.
                            .defaultCellStyle(DocumentTableStyle.builder()
                                    .stroke(new DocumentStroke(DocumentColor.rgb(205, 212, 219), 0.75))
                                    .build())
                            .rowStyle(0, DocumentTableStyle.builder()
                                    .fillColor(DocumentColor.rgb(232, 238, 243))
                                    .build())
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

                    .addParagraph("Charts become Word charts", heading)
                    .addParagraph("The PDF draws the vector chart; the Word export writes the "
                            + "same node as a Word chart, its numbers in an embedded workbook "
                            + "you edit through Edit Data.", caption)
                    .addSection("Chart", s -> s.chart(ChartSpec.bar().data(quarters).build()))

                    .addParagraph("Drawing becomes Word's own", heading)
                    .addParagraph("In Word the divider below is a paragraph border, and the "
                            + "badge a drawn shape that holds its letters. The export report "
                            + "names both, since neither is the kind of node it was authored as.",
                            caption)
                    .addDivider(d -> d.width(420).thickness(2).color(GOLD))
                    .addCircle(72, TEAL, c -> c
                            .stroke(DocumentStroke.of(GOLD, 1.2))
                            .center(new com.demcha.compose.document.dsl.ParagraphBuilder()
                                    .text("GC")
                                    .textStyle(DocumentTextStyle.builder()
                                            .fontName(FontName.HELVETICA)
                                            .decoration(DocumentTextDecoration.BOLD).size(20)
                                            .color(DocumentColor.WHITE).build())
                                    .align(com.demcha.compose.document.node.TextAlign.CENTER)
                                    .build()))
                    .build();

            document.buildPdf();

            // document.buildDocx(docxFile) is the one-call export. A configured backend adds
            // the report of what Word could not carry as authored, and pinned clocks, so the
            // same document exports to the same bytes on every run.
            document.export(DocxSemanticBackend.builder()
                    .deterministic(true)
                    .reportSink(WordExportExample::printReport)
                    .build(), docxFile);
        }
        System.out.println("Generated: " + docxFile);
        return pdfFile;
    }

    /**
     * Prints what the Word export could not carry as authored. A note that is
     * {@code DROPPED} is not in the file; one that is {@code APPROXIMATED} is
     * there as the nearest thing Word has. Each says where it was: a node's
     * layout path, which traces it back to the code that authored it, a page,
     * or no path at all for a note about the whole document or a page zone.
     */
    private static void printReport(DocxExportReport report) {
        if (report.isEmpty()) {
            System.out.println("Word export: everything written as authored");
            return;
        }
        System.out.printf("Word export: %d dropped, %d approximated%n",
                report.count(DocxExportReport.Severity.DROPPED),
                report.count(DocxExportReport.Severity.APPROXIMATED));
        report.bySubject().forEach((subject, notes) -> {
            System.out.println("  " + subject + " (" + notes.size() + ")");
            notes.forEach(note -> System.out.println("    "
                    + (note.path() == null ? "(no path)" : note.path())));
        });
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
