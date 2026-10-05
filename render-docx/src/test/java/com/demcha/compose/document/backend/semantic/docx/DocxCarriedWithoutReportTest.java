package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentDashPattern;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTransform;
import com.demcha.compose.document.table.DocumentTableCell;
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
 * What a node the export writes or draws goes without is in its report note: a link, an
 * outline entry, an anchor a drawing cannot hold, a gradient, unequal corners, a dash, caps, an
 * image's transform.
 *
 * <p>The export wrote each of these nodes and said nothing of what it left behind: a linked
 * logo became an unlinked picture, a rotated photo stood upright, a dashed rail was drawn
 * solid, an anchor on a drawn shape left a page reference to it as plain text — and the report
 * listed no loss.</p>
 */
class DocxCarriedWithoutReportTest {

    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentBookmarkOptions OUTLINE = new DocumentBookmarkOptions("Entry");

    @Test
    void aDrawnShapeNamesItsGradientCornersLinkOutlineEntryAndAnchor() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph(p -> p.text("Target").anchor("target"))
                .addShape(shape -> shape.name("Box").size(120, 30)
                        .fill(DocumentPaint.linear(SURFACE, INK)).stroke(DocumentStroke.of(INK, 1))
                        .cornerRadius(DocumentCornerRadius.of(2, 8, 2, 8))
                        .linkTo("target").bookmark(OUTLINE).anchor("box")));

        assertThat(detailOf(report, "ShapeNode")).contains("drawn as a shape")
                .contains("its gradient fill is not carried")
                .contains("its corners are drawn at the largest of their radii")
                .contains("its link is not carried")
                .contains("its outline entry is not written")
                .contains("its anchor has no bookmark in the Word file");
    }

    @Test
    void aDrawnEllipseNamesItsLinkOutlineEntryAndAnchor() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph(p -> p.text("Target").anchor("target"))
                .addEllipse(ellipse -> ellipse.size(30, 30).fillColor(INK)
                        .linkTo("target").bookmark(OUTLINE).anchor("dot")));

        assertThat(detailOf(report, "EllipseNode")).contains("its link is not carried")
                .contains("its outline entry is not written").contains("its anchor has no bookmark");
    }

    @Test
    void aDrawnLineNamesItsDashCapsAndLink() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph(p -> p.text("Target").anchor("target"))
                .addLine(line -> line.vertical(60).thickness(2).color(INK)
                        .dashed(DocumentDashPattern.of(4, 2)).lineCap(DocumentLineCap.ROUND).linkTo("target")
                        .bookmark(OUTLINE).anchor("rail")));

        assertThat(detailOf(report, "LineNode")).contains("drawn as a shape")
                .contains("its dash pattern is not carried, so it is drawn solid")
                .contains("its caps are not written, so the editor ends it its own way")
                .contains("its link is not carried").contains("its outline entry is not written")
                .contains("its anchor has no bookmark in the Word file");
    }

    @Test
    void aFilledBarWrittenAsARuleNamesItsLinkAndOutlineEntry() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph(p -> p.text("Target").anchor("target"))
                .addShape(shape -> shape.size(200, 2).fillColor(INK).linkTo("target").bookmark(OUTLINE)
                        .anchor("bar")));

        assertThat(detailOf(report, "ShapeNode")).startsWith("written as a paragraph border")
                .contains("its link is not carried").contains("its outline entry is not written")
                .as("a rule in the flow holds its own anchor's bookmark").doesNotContain("anchor");
    }

    @Test
    void aRuleWithRoundCapsSaysTheyAreLost() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addLine(line -> line.horizontal(200).thickness(2).color(INK).lineCap(DocumentLineCap.ROUND)));

        assertThat(detailOf(report, "LineNode")).startsWith("written as a paragraph border")
                .contains("its caps are not carried: a border ends flat");
    }

    @Test
    void anImageInTheFlowNamesItsTransformLinkAndOutlineEntry() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph(p -> p.text("Target").anchor("target"))
                .addImage(image -> image.source(DocumentImageData.fromBytes(png())).size(80, 40)
                        .transform(DocumentTransform.rotate(15)).linkTo("target").bookmark(OUTLINE)));

        assertThat(detailOf(report, "ImageNode")).startsWith("written as a picture in the flow")
                .contains("its transform is not carried, so it is drawn upright at its size")
                .contains("its link is not carried").contains("its outline entry is not written");
    }

    @Test
    void aTableNamesItsLinkAndOutlineEntry() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph(p -> p.text("Target").anchor("target"))
                .addTable(table -> table.columns(DocumentTableColumn.fixed(200)).row("Cell")
                        .linkTo("target").bookmark(OUTLINE)));

        assertThat(detailOf(report, "TableNode")).startsWith("written as a Word table")
                .contains("its link is not carried").contains("its outline entry is not written");
    }

    @Test
    void aSectionAndABarcodeNameTheirOutlineEntries() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addSection(section -> section.bookmark(OUTLINE).addParagraph("Inside"))
                .addBarcode(barcode -> barcode.qrCode().data("GC-1").size(60, 60).bookmark(OUTLINE)));

        assertThat(detailOf(report, "SectionNode")).isEqualTo("written as its contents; its outline entry is not written");
        assertThat(detailOf(report, "barcode")).contains("its outline entry is not written");
    }

    @Test
    void aDrawingComposedInATableCellNamesWhatItGoesWithout() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph(p -> p.text("Target").anchor("target"))
                .addTable(table -> table.columns(DocumentTableColumn.fixed(200))
                        .rowCells(DocumentTableCell.node(new ShapeBuilder().size(20, 20)
                                .fill(DocumentPaint.linear(SURFACE, INK)).stroke(DocumentStroke.of(INK, 1))
                                .linkTo("target").build()))));

        assertThat(detailOf(report, "ShapeNode")).startsWith("composed in a table cell, whose drawing is its table's")
                .contains("its link is not carried")
                .as("its paint is the table's note's to name").doesNotContain("gradient");
    }

    @Test
    void aSectionLaidOutAsALayerColumnNamesItsOutlineEntry() throws Exception {
        // A layer stack of two columns side by side writes each column as what it holds,
        // without dispatching the column's own node.
        DocxExportReport report = reportOf(page -> page.addLayerStack(stack -> stack
                .layer(new com.demcha.compose.document.dsl.SectionBuilder().bookmark(OUTLINE)
                        .margin(new DocumentInsets(0, 220, 0, 0)).addParagraph("Left column").build(),
                        com.demcha.compose.document.node.LayerAlign.TOP_LEFT)
                .layer(new com.demcha.compose.document.dsl.SectionBuilder()
                        .margin(new DocumentInsets(0, 0, 0, 200)).addParagraph("Right column").build(),
                        com.demcha.compose.document.node.LayerAlign.TOP_LEFT)));

        assertThat(detailOf(report, "SectionNode")).isEqualTo("written as a column of its layer stack; its outline entry is not written");
    }

    @Test
    void thePageFlowAndAPanelNameTheirOutlineEntries() throws Exception {
        DocxExportReport report = reportOf(page -> page.bookmark(OUTLINE)
                .addSection(panel -> panel.fillColor(SURFACE).bookmark(OUTLINE).addParagraph("Inside")));

        assertThat(detailOf(report, "ContainerNode")).isEqualTo("written as its contents; its outline entry is not written");
        assertThat(detailOf(report, "SectionNode")).isEqualTo("written as a panel; its outline entry is not written");
    }

    @Test
    void aPictureBesideItsLabelNamesItsLink() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph(p -> p.text("Target").anchor("target"))
                .addLayerStack(stack -> stack
                        .layer(new com.demcha.compose.document.dsl.ImageBuilder()
                                .source(DocumentImageData.fromBytes(png())).size(12, 12).linkTo("target").build(),
                                com.demcha.compose.document.node.LayerAlign.CENTER_LEFT)
                        .layer(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Label")
                                .margin(new DocumentInsets(0, 0, 0, 18)).build(),
                                com.demcha.compose.document.node.LayerAlign.CENTER_LEFT)));

        assertThat(detailOf(report, "ImageNode")).startsWith("drawn beside the text it labels")
                .contains("its link is not carried");
    }

    @Test
    void aShapePaintedOnlyWithAGradientIsDroppedForThat() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addShape(shape -> shape.size(80, 30).fill(DocumentPaint.linear(SURFACE, INK))));

        assertThat(detailOf(report, "ShapeNode")).contains("painted only with a gradient")
                .doesNotContain("geometry");
    }

    @Test
    void nodesThatLoseNoneOfThemAddNoNote() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph("Plain")
                .addImage(image -> image.source(DocumentImageData.fromBytes(png())).size(80, 40))
                .addTable(table -> table.columns(DocumentTableColumn.fixed(200)).row("Cell"))
                .addSection(section -> section.addParagraph("Inside"))
                .addShape(shape -> shape.size(200, 2).fillColor(INK)));

        assertThat(report.bySubject()).doesNotContainKeys("ImageNode", "TableNode", "SectionNode", "ShapeNode");
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
