package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.ChartNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.PageReferenceNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What of a block's own geometry the export does not write is in its report note: the sides of
 * a picture, a barcode or a page reference, a table's padding, the room a spacer holds past its
 * height, the space round a chart's table, the fixed width of an unpainted box.
 *
 * <p>Each was left out in silence: a logo given a left margin stood at the column's edge, a
 * spacer with a margin held only its height and everything under it rose, and a section fixed
 * to half the column ran its text the column's width — and the report listed no loss.</p>
 */
class DocxBlockGeometryReportTest {

    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);

    @Test
    void anInlinePictureNamesTheLeftSideItIsWrittenWithout() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addImage(image -> image.source(DocumentImageData.fromBytes(png())).size(80, 40)
                        .margin(new DocumentInsets(0, 0, 0, 30)).padding(new DocumentInsets(0, 0, 0, 10))));

        assertThat(detailOf(report, "ImageNode")).isEqualTo("written as an inline picture; "
                + "its left margin is not in the file; its left padding is not in the file");
        assertThat(bodyOf(page -> page.addImage(image -> image.source(DocumentImageData.fromBytes(png()))
                .size(80, 40).margin(new DocumentInsets(0, 0, 0, 30)).padding(new DocumentInsets(0, 0, 0, 10)))))
                .as("the file carries neither").isEqualTo(bodyOf(page -> page
                        .addImage(image -> image.source(DocumentImageData.fromBytes(png())).size(80, 40))));
    }

    @Test
    void aPictureWithInsetsAboveBelowAndRightLosesNothing() throws Exception {
        // Its paragraph sets it from the left, where its right side moves nothing.
        DocxExportReport report = reportOf(page -> page
                .addImage(image -> image.source(DocumentImageData.fromBytes(png())).size(80, 40)
                        .margin(new DocumentInsets(12, 20, 12, 0)).padding(new DocumentInsets(4, 6, 4, 0))));

        assertThat(report.bySubject()).doesNotContainKey("ImageNode");
    }

    @Test
    void aPictureDrawnBesideItsLabelNamesThePaddingItFills() throws Exception {
        DocxExportReport report = reportOf(page -> page.addLayerStack(stack -> stack
                .layer(new com.demcha.compose.document.dsl.ImageBuilder()
                        .source(DocumentImageData.fromBytes(png())).size(12, 12)
                        .padding(DocumentInsets.of(2)).build(), LayerAlign.CENTER_LEFT)
                .layer(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Label")
                        .margin(new DocumentInsets(0, 0, 0, 22)).build(), LayerAlign.CENTER_LEFT)));

        assertThat(detailOf(report, "ImageNode")).startsWith("drawn beside the text it labels")
                .endsWith("its padding is not in the file: the picture is fitted to its box with the "
                          + "padding in it");
    }

    @Test
    void aBarcodeNamesItsSides() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addBarcode(barcode -> barcode.qrCode().data("GC-1").size(60, 60)
                        .margin(new DocumentInsets(0, 0, 0, 30))));

        assertThat(detailOf(report, "barcode")).startsWith("written as a picture of the symbol")
                .endsWith("its left margin is not in the file");
        assertThat(bodyOf(page -> page.addBarcode(barcode -> barcode.qrCode().data("GC-1").size(60, 60)
                .margin(new DocumentInsets(0, 0, 0, 30)))))
                .as("the file does not carry it").isEqualTo(bodyOf(page -> page
                        .addBarcode(barcode -> barcode.qrCode().data("GC-1").size(60, 60))));
    }

    @Test
    void aTableNamesItsSidePaddingAndHoldsItsMargin() throws Exception {
        DocxExportReport padded = reportOf(page -> page
                .addTable(table -> table.columns(DocumentTableColumn.fixed(200)).row("Cell")
                        .margin(new DocumentInsets(0, 0, 0, 10)).padding(new DocumentInsets(0, 0, 0, 30))));
        DocxExportReport indented = reportOf(page -> page
                .addTable(table -> table.columns(DocumentTableColumn.fixed(200)).row("Cell")
                        .margin(new DocumentInsets(0, 0, 0, 30))));

        assertThat(detailOf(padded, "TableNode"))
                .isEqualTo("written as a Word table; its left padding is not in the file");
        assertThat(indented.bySubject()).as("a table's margin is its indent").doesNotContainKey("TableNode");
        String marginOnly = indentOf(bodyOf(page -> page.addTable(table -> table
                .columns(DocumentTableColumn.fixed(200)).row("Cell").margin(new DocumentInsets(0, 0, 0, 10)))));
        assertThat(marginOnly).as("its margin is its indent").isNotEmpty();
        assertThat(indentOf(bodyOf(page -> page.addTable(table -> table.columns(DocumentTableColumn.fixed(200))
                .row("Cell").margin(new DocumentInsets(0, 0, 0, 10)).padding(new DocumentInsets(0, 0, 0, 30))))))
                .as("its indent does not carry its padding").isEqualTo(marginOnly);
    }

    @Test
    void aPageReferenceNamesItsSides() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph(p -> p.text("Target").anchor("target"))
                .add(new PageReferenceNode("", "target", DocumentTextStyle.DEFAULT, TextAlign.LEFT, "",
                        DocumentInsets.zero(), new DocumentInsets(0, 0, 0, 30))));

        assertThat(detailOf(report, "PageReferenceNode"))
                .isEqualTo("written as a paragraph; its side margins are not in the file");
        assertThat(bodyOf(page -> page.addParagraph(p -> p.text("Target").anchor("target"))
                .add(new PageReferenceNode("", "target", DocumentTextStyle.DEFAULT, TextAlign.LEFT, "",
                        DocumentInsets.zero(), new DocumentInsets(0, 0, 0, 30)))))
                .as("the file does not carry it").isEqualTo(bodyOf(page -> page
                        .addParagraph(p -> p.text("Target").anchor("target")).addPageReference("target")));
    }

    @Test
    void aPageReferenceToAnAnchorNoBookmarkMarksNamesItsNumberAsText() throws Exception {
        // A drawn shape's anchor has no bookmark, so the reference to it is its number alone.
        DocxExportReport report = reportOf(page -> page
                .addShape(shape -> shape.size(40, 30).fillColor(INK).anchor("box"))
                .addPageReference("box"));

        assertThat(detailOf(report, "PageReferenceNode")).isEqualTo("written as a paragraph; its anchor has "
                + "no bookmark in the Word file, so its page number is written as text that an edit does "
                + "not update");
    }

    @Test
    void aSpacerNamesTheRoomItHoldsPastItsHeight() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph("Above")
                .addSpacer(spacer -> spacer.height(10).margin(new DocumentInsets(20, 0, 0, 0)))
                .addParagraph("Below"));

        assertThat(detailOf(report, "SpacerNode")).isEqualTo("written as its height; its margin and padding "
                + "above and below are not written with it");
        assertThat(bodyOf(page -> page.addParagraph("Above")
                .addSpacer(spacer -> spacer.height(10).margin(new DocumentInsets(20, 0, 0, 0)))
                .addParagraph("Below")))
                .as("the file does not carry it").isEqualTo(bodyOf(page -> page.addParagraph("Above")
                        .addSpacer(spacer -> spacer.height(10)).addParagraph("Below")));
    }

    @Test
    void aChartNamesTheSpaceRoundItsTable() throws Exception {
        ChartData data = ChartData.builder().categories("Q1", "Q2").series("2025", 12.4, 15.1).build();
        DocxExportReport report = reportOf(page -> page
                .add(new ChartNode("", ChartSpec.bar().data(data).build(), null,
                        new DocumentInsets(20, 0, 20, 30), DocumentInsets.zero())));

        assertThat(detailOf(report, "chart")).startsWith("exported as its data table")
                .endsWith("; its margin and padding are not written round the table");
        assertThat(bodyOf(page -> page.add(new ChartNode("", ChartSpec.bar().data(data).build(), null,
                new DocumentInsets(20, 0, 20, 30), DocumentInsets.zero()))))
                .as("the file does not carry them").isEqualTo(bodyOf(page -> page
                        .add(new ChartNode(ChartSpec.bar().data(data).build()))));
    }

    @Test
    void anUnpaintedSectionNamesTheFixedWidthItsTextRunsPast() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addSection(section -> section.fixedWidth(150).addParagraph("Text the page wraps at 150pt.")));

        assertThat(detailOf(report, "SectionNode")).isEqualTo("written as its contents; its fixed width is "
                + "not in the file, so its paragraphs and lists run the width of the column it stands in");
        assertThat(bodyOf(page -> page.addSection(section -> section.fixedWidth(150)
                .addParagraph("Text the page wraps at 150pt."))))
                .as("the file does not carry it").isEqualTo(bodyOf(page -> page
                        .addSection(section -> section.addParagraph("Text the page wraps at 150pt."))));
    }

    @Test
    void aFixedWidthThePanelOrTheColumnHoldsLosesNothing() throws Exception {
        // A panel is a table the width the layout placed it at; a width the column cannot give
        // is the column's anyway.
        DocxExportReport report = reportOf(page -> page
                .addSection(panel -> panel.fixedWidth(150).fillColor(SURFACE).addParagraph("In a panel"))
                .addSection(section -> section.fixedWidth(900).addParagraph("Wider than the column")));

        assertThat(report.bySubject()).doesNotContainKey("SectionNode");
    }

    @Test
    void aPictureInARowColumnTheLayoutPlacedIsPastItsLeftMarginThere() throws Exception {
        // The column's cell starts where the layout placed the picture, past its margin.
        DocxExportReport report = reportOf(page -> page.addRow(row -> row.weights(1, 1)
                .addImage(image -> image.source(DocumentImageData.fromBytes(png())).size(60, 30)
                        .margin(new DocumentInsets(0, 0, 0, 20)))
                .addParagraph("Beside it")));

        assertThat(report.bySubject()).doesNotContainKey("ImageNode");
    }

    @Test
    void aFixedWidthSectionInAnAutoRowColumnLosesNothing() throws Exception {
        // An auto column is the section's own width, and a point of editor slack.
        DocxExportReport report = reportOf(page -> page.addRow(row -> row
                .columns(com.demcha.compose.document.style.DocumentRowColumn.auto(),
                        com.demcha.compose.document.style.DocumentRowColumn.weight(1))
                .addSection(section -> section.fixedWidth(120).addParagraph("Fixed"))
                .addParagraph("The rest of the row")));

        assertThat(report.bySubject()).doesNotContainKey("SectionNode");
    }

    @Test
    void aSpacerClosingABandHasItsInsetsWrittenAsTheSpaceBelow() throws Exception {
        // A title and its date at either end of a band: the space below the band is measured
        // from its lowest block, the spacer's margin included.
        DocxExportReport report = reportOf(page -> page
                .addLayerStack(stack -> stack
                        .layer(new com.demcha.compose.document.dsl.SectionBuilder().addParagraph("Title")
                                .addSpacer(spacer -> spacer.height(4).margin(new DocumentInsets(0, 0, 10, 0)))
                                .build(), LayerAlign.TOP_LEFT)
                        .layer(new com.demcha.compose.document.dsl.ParagraphBuilder().text("May 2026")
                                .align(TextAlign.RIGHT).build(), LayerAlign.TOP_RIGHT))
                .addParagraph("Below"));

        assertThat(report.bySubject()).doesNotContainKey("SpacerNode");
    }

    /** The exported body's XML, to compare a document with and without what is not written. */
    private static String bodyOf(Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 600)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            try (org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument(
                    new java.io.ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                return document.getDocument().getBody().xmlText();
            }
        }
    }

    /** A table's {@code w:tblInd} in the body's XML, or none. */
    private static String indentOf(String body) {
        java.util.regex.Matcher indent = java.util.regex.Pattern.compile("<w:tblInd [^>]*/>").matcher(body);
        return indent.find() ? indent.group() : "";
    }

    private static String detailOf(DocxExportReport report, String subject) {
        List<DocxExportReport.Note> notes = report.bySubject().get(subject);
        assertThat(notes).as("the note on the %s", subject).isNotNull().hasSize(1);
        return notes.get(0).detail();
    }

    private static DocxExportReport reportOf(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 600)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            session.export(new DocxSemanticBackend(captured::set));
        }
        assertThat(captured.get()).as("the sink is called once the bytes exist").isNotNull();
        return captured.get();
    }

    private static byte[] png() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
