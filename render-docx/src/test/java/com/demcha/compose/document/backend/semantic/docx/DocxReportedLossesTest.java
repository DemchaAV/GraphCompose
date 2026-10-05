package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.layout.BoxConstraints;
import com.demcha.compose.document.layout.FragmentContext;
import com.demcha.compose.document.layout.FragmentPlacement;
import com.demcha.compose.document.layout.LayoutFragment;
import com.demcha.compose.document.layout.MeasureResult;
import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.layout.PaginationPolicy;
import com.demcha.compose.document.layout.PrepareContext;
import com.demcha.compose.document.layout.PreparedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.output.DocumentProtection;
import com.demcha.compose.document.output.DocumentViewerPreferences;
import com.demcha.compose.document.output.DocumentWatermark;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
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
 * What a document asks for that the Word file is not given is in the report, not only in the
 * file's absence.
 *
 * <p>A row's fill, a logo in a page header, a watermark, a protection and a node kind the
 * export does not know were each left out of the Word file with no more than a log line, or
 * nothing: a caller reading the report was told the document lost nothing. Each is now a note
 * naming what the file does not carry.</p>
 */
class DocxReportedLossesTest {

    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentStroke RULE = DocumentStroke.of(DocumentColor.rgb(26, 86, 148), 1);

    @Test
    void aRowsFillOutlineAndBordersAreReportedAsNotWritten() throws Exception {
        DocxExportReport report = reportOf(session -> session.pageFlow(page -> page.addRow(row -> row
                .name("Totals").fillColor(SURFACE).stroke(RULE).borders(DocumentBorders.bottom(RULE))
                .addParagraph("Subtotal").addParagraph("120.00"))));

        List<DocxExportReport.Note> notes = report.bySubject().get("row paint");
        assertThat(notes).as("one note for the row").hasSize(1);
        assertThat(notes.get(0).severity()).isEqualTo(DocxExportReport.Severity.DROPPED);
        assertThat(notes.get(0).path()).contains("Totals");
        assertThat(notes.get(0).detail()).contains("the row's fill, outline and borders are not written");
    }

    @Test
    void aRowWithAFillAloneSaysSo() throws Exception {
        DocxExportReport report = reportOf(session -> session.pageFlow(page -> page.addRow(row -> row
                .fillColor(SURFACE).addParagraph("Left").addParagraph("Right"))));

        assertThat(report.bySubject().get("row paint")).singleElement()
                .extracting(DocxExportReport.Note::detail).asString().contains("the row's fill is not written");
    }

    @Test
    void aRowThatPaintsNothingReportsNothing() throws Exception {
        // A corner radius rounds a paint; with none to round, the page draws nothing either.
        DocxExportReport report = reportOf(session -> session.pageFlow(page -> page.addRow(row -> row
                .cornerRadius(6).stroke(DocumentStroke.of(SURFACE, 0)).addParagraph("Left").addParagraph("Right"))));

        assertThat(report.bySubject()).doesNotContainKey("row paint");
    }

    @Test
    void aPictureInAPageZoneIsReportedOnceThoughWrittenIntoTwoHeaders() throws Exception {
        // A second zone on the first page alone gives the section a first-page header, so the
        // logo's zone is written into it and into the ordinary header: told once all the same.
        DocxExportReport report = reportOf(session -> {
            session.chrome().zone(DocumentPageZone.header(30, page -> new RowBuilder()
                    .addImage(image -> image.name("Logo").source(DocumentImageData.fromBytes(png())).size(24, 24))
                    .addParagraph("Quarterly report")
                    .build()));
            session.chrome().zone(onTheFirstPage(DocumentPageZone.footer(20,
                    page -> new ParagraphBuilder().text("Cover").build())));
            longBody(session);
        });

        List<DocxExportReport.Note> notes = report.bySubject().get("page zone content");
        assertThat(notes).as("the logo, once").hasSize(1);
        assertThat(notes.get(0).severity()).isEqualTo(DocxExportReport.Severity.DROPPED);
        assertThat(notes.get(0).detail()).contains("ImageNode 'Logo' is not written");
    }

    @Test
    void twoPicturesOfNoNameInAPageZoneAreTwoNotes() throws Exception {
        DocxExportReport report = reportOf(session -> {
            session.chrome().zone(DocumentPageZone.header(30, page -> new RowBuilder()
                    .addImage(image -> image.source(DocumentImageData.fromBytes(png())).size(24, 24))
                    .addImage(image -> image.source(DocumentImageData.fromBytes(png())).size(24, 24))
                    .build()));
            session.pageFlow(page -> page.addParagraph("Body"));
        });

        assertThat(report.bySubject().get("page zone content")).as("each picture is a loss of its own").hasSize(2);
    }

    @Test
    void aZoneOfTextAndPageNumbersReportsNothing() throws Exception {
        DocxExportReport report = reportOf(session -> {
            session.chrome().zone(DocumentPageZone.footer(20, page -> new RowBuilder()
                    .addParagraph("Quarterly report").add(page.pageNumber()).build()));
            session.pageFlow(page -> page.addParagraph("Body"));
        });

        assertThat(report.bySubject()).doesNotContainKey("page zone content");
    }

    @Test
    void aPaintedRowInAPageZoneIsReportedOnceThoughWrittenIntoTwoFooters() throws Exception {
        // A zone's row is written as one line of its children, into each kind of footer it is given.
        DocxExportReport report = reportOf(session -> {
            session.chrome().zone(DocumentPageZone.footer(20, page -> new RowBuilder().name("Band")
                    .fillColor(SURFACE).addParagraph("Quarterly report").add(page.pageNumber()).build()));
            session.chrome().zone(onTheFirstPage(DocumentPageZone.header(20,
                    page -> new ParagraphBuilder().text("Cover").build())));
            longBody(session);
        });

        assertThat(report.bySubject().get("row paint")).as("the band's fill, once").singleElement()
                .extracting(DocxExportReport.Note::detail).asString()
                .contains("the row's fill is not written: a page zone's row is written as one line");
    }

    @Test
    void aRowOfNoColumnsThePagePaintsIsReported() throws Exception {
        DocxExportReport report = reportOf(session -> session.pageFlow(page -> page.addRow(row -> row
                .fillColor(SURFACE).padding(DocumentInsets.of(8)))));

        assertThat(report.bySubject()).containsKey("row paint");
    }

    @Test
    void aRowOfNoColumnsAndNoHeightIsNotReported() throws Exception {
        // The page paints a row's box only where the layout gives it some height.
        DocxExportReport report = reportOf(session -> session.pageFlow(page -> page
                .addParagraph("Before").addRow(row -> row.fillColor(SURFACE)).addParagraph("After")));

        assertThat(report.bySubject()).doesNotContainKey("row paint");
    }

    @Test
    void aRoundedRowSaysItsPaintWasRounded() throws Exception {
        DocxExportReport report = reportOf(session -> session.pageFlow(page -> page.addRow(row -> row
                .fillColor(SURFACE).cornerRadius(6).addParagraph("Left").addParagraph("Right"))));

        assertThat(report.bySubject().get("row paint")).singleElement()
                .extracting(DocxExportReport.Note::detail).asString().contains("the row's rounded fill is not written");
    }

    @Test
    void aFilledRowRoundTextInATableCellIsDroppedNotApproximated() throws Exception {
        // A cell's paint is drawn as a shape only where it frames no text; round a label and a
        // value it is not drawn at all.
        DocxExportReport report = reportOf(session -> session.pageFlow(page -> page.addTable(table -> table
                .columns(DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.node(new RowBuilder().fillColor(SURFACE)
                        .addParagraph("Due").addParagraph("120.00").build())))));

        assertThat(report.bySubject().get("row paint")).singleElement().satisfies(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.DROPPED);
            assertThat(note.detail()).contains("drawn only where it frames no text");
        });
    }

    @Test
    void aFilledRowRoundNoTextInATableCellIsApproximated() throws Exception {
        // Round no text, the cell's drawing draws the row's box as a shape in the cell.
        DocxExportReport report = reportOf(session -> session.pageFlow(page -> page.addTable(table -> table
                .columns(DocumentTableColumn.fixed(300))
                .rowCells(DocumentTableCell.node(new RowBuilder().fillColor(SURFACE).padding(DocumentInsets.of(6))
                        .addSpacer(40).addSpacer(40).build())))));

        assertThat(report.bySubject().get("row paint")).singleElement()
                .extracting(DocxExportReport.Note::severity).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
    }

    @Test
    void aZonesLossesInASectionedFileNameTheirSection() throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (com.demcha.compose.document.api.MultiSectionDocument document = GraphCompose.documents()
                .section(withALogoHeader("Cover"))
                .section(withALogoHeader("Body"))
                .create()) {
            document.export(new DocxSemanticBackend(captured::set));
        }

        assertThat(captured.get().bySubject().get("page zone content")).extracting(DocxExportReport.Note::path)
                .containsExactly("section 1", "section 2");
    }

    @Test
    void aWatermarkAProtectionAndViewerPreferencesAreReportedAsNotWritten() throws Exception {
        DocxExportReport report = reportOf(session -> {
            session.chrome()
                    .watermark(DocumentWatermark.builder().text("DRAFT").build())
                    .protect(DocumentProtection.builder().userPassword("u").build())
                    .viewerPreferences(DocumentViewerPreferences.openOutline());
            session.pageFlow(page -> page.addParagraph("Body"));
        });

        assertThat(report.bySubject()).containsKeys("watermark", "protection", "viewer preferences");
        assertThat(report.bySubject().get("protection")).singleElement()
                .extracting(DocxExportReport.Note::detail).asString().contains("opens and edits without a password");
    }

    @Test
    void aWatermarkIsReportedPerSectionAndAProtectionOnceForTheFile() throws Exception {
        // A watermark is on its section's pages; a protection is the whole file's, however many
        // sections ask for it.
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (com.demcha.compose.document.api.MultiSectionDocument document = GraphCompose.documents()
                .section(protectedWatermarked("Cover"))
                .section(protectedWatermarked("Body"))
                .create()) {
            document.export(new DocxSemanticBackend(captured::set));
        }

        assertThat(captured.get().bySubject().get("watermark")).extracting(DocxExportReport.Note::path)
                .containsExactly("section 1", "section 2");
        assertThat(captured.get().bySubject().get("protection")).hasSize(1);
    }

    @Test
    void aPageFieldInTheBodyIsDroppedForWhatItIs() throws Exception {
        DocxExportReport report = reportOf(session -> session.pageFlow(page -> page
                .add(com.demcha.compose.document.output.PageContext.unpaginated().pageNumber())));

        assertThat(report.notes()).filteredOn(note -> note.subject().contains("PageField")).singleElement()
                .extracting(DocxExportReport.Note::detail).asString()
                .contains("a page field is written in a page zone").doesNotContain("geometry");
    }

    @Test
    void anUnknownNodeKindIsReportedWithWhatIsLostWithIt() throws Exception {
        DocxExportReport report = reportOf(session -> {
            session.registerNodeDefinition(new StampDefinition());
            session.pageFlow(page -> page.add(new Stamp("Approval",
                    List.of(new ParagraphBuilder().text("Approved by finance").build()))));
        });

        List<DocxExportReport.Note> notes = report.bySubject().get("Stamp");
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).severity()).isEqualTo(DocxExportReport.Severity.DROPPED);
        assertThat(notes.get(0).detail())
                .as("not the geometry reason a built-in drawing gets")
                .contains("a node kind the DOCX export does not know")
                .contains("nor the 1 node it holds")
                .doesNotContain("geometry");
    }

    @Test
    void anUnknownNodeKindThatPaintsAShapeIsDrawnAndSaysWhatElseIsLost() throws Exception {
        DocxExportReport report = reportOf(session -> {
            session.registerNodeDefinition(new SealDefinition());
            session.pageFlow(page -> page.addParagraph("Signed").add(new Seal("Seal")));
        });

        assertThat(report.bySubject().get("Seal")).singleElement().satisfies(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
            assertThat(note.detail()).contains("drawn as a shape")
                    .contains("only the shapes it paints itself are written");
        });
    }

    @Test
    void aBuiltInDrawingNothingShowsIsStillDroppedForItsGeometry() throws Exception {
        // A path painted only with a gradient is a kind the export knows and cannot draw.
        DocxExportReport report = reportOf(session -> session.pageFlow(page -> page
                .addPath(path -> path.name("Rail").size(20, 20).moveTo(0, 0).lineTo(1, 0).lineTo(0.5, 1)
                        .closePath().fill(com.demcha.compose.document.style.DocumentPaint.linear(SURFACE,
                                DocumentColor.rgb(26, 86, 148))))));

        assertThat(report.notes()).filteredOn(note -> note.severity() == DocxExportReport.Severity.DROPPED)
                .singleElement().extracting(DocxExportReport.Note::detail).asString()
                .contains("geometry has no semantic Word analogue").doesNotContain("does not know");
    }

    /** A node kind of the caller's own that paints a circle the export can draw. */
    private record Seal(String name) implements DocumentNode {
    }

    private static final class SealDefinition implements NodeDefinition<Seal> {
        @Override
        public Class<Seal> nodeType() {
            return Seal.class;
        }

        @Override
        public PreparedNode<Seal> prepare(Seal node, PrepareContext ctx, BoxConstraints constraints) {
            return PreparedNode.leaf(node, new MeasureResult(30, 30));
        }

        @Override
        public PaginationPolicy paginationPolicy(Seal node) {
            return PaginationPolicy.ATOMIC;
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<Seal> prepared, FragmentContext ctx,
                                                  FragmentPlacement placement) {
            return List.of(new LayoutFragment(placement.path(), 0, 0, 0, placement.width(), placement.height(),
                    new com.demcha.compose.document.layout.payloads.EllipseFragmentPayload(
                            java.awt.Color.RED, null, null, null)));
        }
    }

    /** A node kind of the caller's own, holding a paragraph the export never sees. */
    private record Stamp(String name, List<DocumentNode> children) implements DocumentNode {
    }

    private static final class StampDefinition implements NodeDefinition<Stamp> {
        @Override
        public Class<Stamp> nodeType() {
            return Stamp.class;
        }

        @Override
        public PreparedNode<Stamp> prepare(Stamp node, PrepareContext ctx, BoxConstraints constraints) {
            return PreparedNode.leaf(node, new MeasureResult(120, 30));
        }

        @Override
        public List<DocumentNode> children(Stamp node) {
            // A leaf to the layout: the stamp lays out and paints what it holds itself.
            return List.of();
        }

        @Override
        public PaginationPolicy paginationPolicy(Stamp node) {
            return PaginationPolicy.ATOMIC;
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<Stamp> prepared, FragmentContext ctx,
                                                  FragmentPlacement placement) {
            return List.of(new LayoutFragment(placement.path(), 0, 0, 0,
                    placement.width(), placement.height(), "stamp"));
        }
    }

    private static DocxExportReport reportOf(Consumer<DocumentSession> compose) throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 600)
                .margin(DocumentInsets.of(20))
                .create()) {
            compose.accept(session);
            session.export(new DocxSemanticBackend(captured::set));
        }
        assertThat(captured.get()).as("the sink is called once the bytes exist").isNotNull();
        return captured.get();
    }

    /** A zone drawn on the first page alone, which gives its section a first-page header. */
    private static DocumentPageZone onTheFirstPage(DocumentPageZone zone) {
        return zone.toBuilder().appliesTo(com.demcha.compose.document.output.PageContext::isFirst).build();
    }

    private static void longBody(DocumentSession session) {
        session.pageFlow(page -> {
            for (int i = 0; i < 60; i++) {
                page.addParagraph("A paragraph long enough to run the document onto further pages: " + i);
            }
        });
    }

    private static DocumentSession withALogoHeader(String text) {
        DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create();
        session.chrome().zone(DocumentPageZone.header(30, page -> new RowBuilder()
                .addImage(image -> image.source(DocumentImageData.fromBytes(png())).size(24, 24))
                .addParagraph(text).build()));
        session.pageFlow(page -> page.addParagraph(text));
        return session;
    }

    private static DocumentSession protectedWatermarked(String text) {
        DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create();
        session.chrome()
                .watermark(DocumentWatermark.builder().text("DRAFT").build())
                .protect(DocumentProtection.builder().userPassword("u").build());
        session.pageFlow(page -> page.addParagraph(text));
        return session;
    }

    private static byte[] png() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(24, 24, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
