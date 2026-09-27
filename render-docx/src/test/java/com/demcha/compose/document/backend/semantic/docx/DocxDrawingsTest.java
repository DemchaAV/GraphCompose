package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drawing the export has no Word element for — a dot, a ring round a portrait, a timeline's
 * rail — is a shape anchored to the page where the page draws it, behind the text.
 *
 * @author Artem Demchyshyn
 */
class DocxDrawingsTest {

    private static final DocumentColor ACCENT = DocumentColor.rgb(26, 86, 148);

    @Test
    void anEllipseIsDrawnWhereThePagePutsIt() throws Exception {
        AtomicReference<PlacedFragment> dot = new AtomicReference<>();
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(report, session -> {
            session.pageFlow(page -> page
                    .addParagraph(p -> p.text("Above"))
                    .addEllipse(e -> e.name("Dot").circle(30).fillColor(ACCENT))
                    .addParagraph(p -> p.text("Below")));
            dot.set(fragmentNamed(session, "Dot"));
        })) {
            String anchor = anchors(document.getDocument().xmlText()).get(0);
            // A fragment's y is its bottom edge, measured up from the page's foot.
            double top = 400 - dot.get().y() - dot.get().height();

            assertThat(anchor).contains("prst=\"ellipse\"")
                    .contains("behindDoc=\"1\"")
                    .contains("<wp:positionH relativeFrom=\"page\"><wp:posOffset>"
                              + Units.toEMU(dot.get().x()) + "</wp:posOffset>")
                    .contains("<wp:positionV relativeFrom=\"page\"><wp:posOffset>"
                              + Units.toEMU(top) + "</wp:posOffset>")
                    .contains("srgbClr val=\"1A5694\"");
            assertThat(document.getParagraphs()).extracting(p -> p.getText())
                    .as("the text around it is written as before")
                    .contains("Above", "Below");
            assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
        }
    }

    @Test
    void aPictureClippedToACircleTakesTheCircle() throws Exception {
        try (XWPFDocument document = export(null, session -> session.add(new ShapeContainerBuilder()
                .name("Portrait")
                .circle(60)
                .stroke(DocumentStroke.of(ACCENT, 1))
                .center(new ImageBuilder().name("Photo").source(pngBytes()).size(60, 60).build())
                .build()))) {
            String body = document.getDocument().xmlText();

            assertThat(document.getAllPictures()).hasSize(1);
            assertThat(pictureGeometry(body)).as("the photo, cropped to the circle").containsExactly("ellipse");
            assertThat(anchors(body)).as("the ring drawn round it")
                    .singleElement().asString().contains("prst=\"ellipse\"");
        }
    }

    @Test
    void aLogoSmallerThanItsRoundBadgeStaysWhole() throws Exception {
        // The page clips to the badge's circle, which a 24pt logo in a 60pt badge stands inside.
        try (XWPFDocument document = export(null, session -> session.add(new ShapeContainerBuilder()
                .name("Badge")
                .circle(60)
                .center(new ImageBuilder().name("Logo").source(pngBytes()).size(24, 24).build())
                .build()))) {
            assertThat(pictureGeometry(document.getDocument().xmlText())).containsExactly("rect");
        }
    }

    @Test
    void aPictureInARectangleStaysARectangle() throws Exception {
        try (XWPFDocument document = export(null, session -> session.add(new ShapeContainerBuilder()
                .name("Frame")
                .rectangle(60, 60)
                .center(new ImageBuilder().name("Photo").source(pngBytes()).size(60, 60).build())
                .build()))) {
            assertThat(pictureGeometry(document.getDocument().xmlText())).containsExactly("rect");
        }
    }

    @Test
    void theBodysDrawingsStandAboveThePageBackgrounds() throws Exception {
        // LibreOffice stacks a header's shapes and the body's together: a column's fill in the
        // header painted over the timeline rail drawn in the body.
        try (XWPFDocument document = export(null, session -> {
            session.pageBackground(DocumentColor.rgb(250, 250, 250));
            session.pageFlow(page -> page
                    .addEllipse(e -> e.name("First").circle(10).fillColor(ACCENT))
                    .addEllipse(e -> e.name("Second").circle(10).fillColor(ACCENT)));
        })) {
            List<Long> body = heights(document.getParagraphs());
            List<Long> header = new ArrayList<>();
            for (XWPFHeader part : document.getHeaderList()) {
                header.addAll(heights(part.getParagraphs()));
            }

            assertThat(header).isNotEmpty();
            assertThat(body).hasSize(2).isSorted();
            assertThat(body.get(0)).isGreaterThan(header.stream().mapToLong(Long::longValue).max().orElseThrow());
        }
    }

    @Test
    void aDrawingWithNoParagraphAfterItOnItsPageIsStillDrawn() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(report, session -> session.pageFlow(page -> page
                .addParagraph(p -> p.text("Only line"))
                .addEllipse(e -> e.name("Last").circle(20).fillColor(ACCENT))))) {
            assertThat(anchors(document.getDocument().xmlText())).singleElement()
                    .asString().contains("prst=\"ellipse\"");
            assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
        }
    }

    @Test
    void aShapeBelowTheLastTextOfItsPageIsAnchoredOnThatPage() throws Exception {
        // The dot is the last block of page 1: the next node starts on page 2, and a shape left
        // waiting for a paragraph on its page would find none and be dropped.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(report, session -> session.pageFlow(page -> page
                .addParagraph(p -> p.text("Page one"))
                .addEllipse(e -> e.name("Dot").circle(20).fillColor(ACCENT))
                .addPageBreak(b -> { })
                .addParagraph(p -> p.text("Page two"))))) {
            assertThat(paragraphCarrying(document, "prst=\"ellipse\"")).isEqualTo("Page one");
            assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
        }
    }

    @Test
    void aShapeAtTheTopOfAPageWaitsForThatPagesFirstText() throws Exception {
        try (XWPFDocument document = export(null, session -> session.pageFlow(page -> page
                .addParagraph(p -> p.text("Page one"))
                .addPageBreak(b -> { })
                .addEllipse(e -> e.name("Dot").circle(20).fillColor(ACCENT))
                .addParagraph(p -> p.text("Page two"))))) {
            assertThat(paragraphCarrying(document, "prst=\"ellipse\"")).isEqualTo("Page two");
        }
    }

    @Test
    void aPageZonesDrawingIsNotDrawnAgainInTheBody() throws Exception {
        // A header's band is the header's: drawn in the body too, it stood over the header's text.
        try (XWPFDocument document = export(null, session -> {
            session.chrome().zone(com.demcha.compose.document.output.DocumentPageZone.header(30,
                    page -> new com.demcha.compose.document.dsl.ShapeBuilder()
                            .name("Band").size(200, 12).fillColor(ACCENT).build()));
            session.pageFlow(page -> page.addParagraph(p -> p.text("Body")));
        })) {
            assertThat(anchors(document.getDocument().xmlText())).isEmpty();
        }
    }

    @Test
    void aPageALongTableFillsCarriesItsShapesInARow() throws Exception {
        // Page 2 holds nothing but the table's later rows, then the dot: a row there is the
        // paragraph on that page, where the dot has to be anchored.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(report, session -> session.pageFlow(page -> page
                .addTable(t -> {
                    t.columns(com.demcha.compose.document.table.DocumentTableColumn.auto());
                    for (int row = 0; row < 40; row++) {
                        t.row("Row " + row);
                    }
                })
                .addEllipse(e -> e.name("Dot").circle(20).fillColor(ACCENT))))) {
            assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
            String carrier = document.getTables().get(0).getRows().stream()
                    .flatMap(row -> row.getTableCells().stream())
                    .flatMap(cell -> cell.getParagraphs().stream())
                    .filter(paragraph -> paragraph.getCTP().xmlText().contains("prst=\"ellipse\""))
                    .map(org.apache.poi.xwpf.usermodel.XWPFParagraph::getText)
                    .findFirst()
                    .orElseThrow();
            assertThat(carrier).as("the first row the layout put on page 2")
                    .isNotEqualTo("Row 0").startsWith("Row ");
        }
    }

    @Test
    void eachSectionAnchorsItsShapesOnItsOwnPages() throws Exception {
        // Pages are counted within a section: the second section's page 1 is not the first's.
        DocumentSession first = GraphCompose.document().pageSize(400, 400).margin(DocumentInsets.of(20)).create();
        first.pageFlow(page -> page.addParagraph(p -> p.text("First section")));
        DocumentSession second = GraphCompose.document().pageSize(400, 400).margin(DocumentInsets.of(20)).create();
        second.pageFlow(page -> page
                .addEllipse(e -> e.name("Dot").circle(20).fillColor(ACCENT))
                .addParagraph(p -> p.text("Second section")));
        byte[] docx;
        try (com.demcha.compose.document.api.MultiSectionDocument document = GraphCompose.documents()
                .section(first).section(second).create()) {
            docx = document.toDocxBytes();
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(paragraphCarrying(document, "prst=\"ellipse\"")).isEqualTo("Second section");
        }
    }

    @Test
    void theReportSaysWhatAShapeLosesInAPanelOrUnderATransform() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument ignored = export(report, session -> session.pageFlow(page -> page
                .addSection("Panel", s -> s.fillColor(DocumentColor.rgb(220, 230, 240))
                        .addParagraph(p -> p.text("In the panel"))
                        .addEllipse(e -> e.name("Hidden").circle(20).fillColor(ACCENT)))
                .addEllipse(e -> e.name("Turned").size(30, 10).fillColor(ACCENT)
                        .transform(com.demcha.compose.document.style.DocumentTransform.rotate(45)))))) {
            List<String> messages = report.get().notes().stream()
                    .filter(note -> note.severity() == DocxExportReport.Severity.APPROXIMATED)
                    .map(DocxExportReport.Note::detail)
                    .toList();
            assertThat(messages).anyMatch(message -> message.contains("shading"))
                    .anyMatch(message -> message.contains("transform is not carried"));
        }
    }

    @Test
    void anOutlineNoShapeDrawsIsReportedAsDropped() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument ignored = export(report, session -> session.add(new ShapeContainerBuilder()
                .name("Star")
                .star(60, 60)
                .fillColor(ACCENT)
                .center(new com.demcha.compose.document.dsl.ParagraphBuilder().text("New").build())
                .build()))) {
            assertThat(report.get().bySubject().get("shape container outline")).hasSize(1);
        }
    }

    /** The text of the body paragraph whose XML holds a marker. */
    private static String paragraphCarrying(XWPFDocument document, String marker) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getCTP().xmlText().contains(marker))
                .map(org.apache.poi.xwpf.usermodel.XWPFParagraph::getText)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void aShapeAloneOnTheLastPageGetsAParagraphOfItsOwn() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(report, session -> session.pageFlow(page -> page
                .addParagraph(p -> p.text("Page one"))
                .addPageBreak(b -> { })
                .addEllipse(e -> e.name("Dot").circle(20).fillColor(ACCENT))))) {
            assertThat(paragraphCarrying(document, "prst=\"ellipse\"")).as("an empty carrier").isEmpty();
            assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
        }
    }

    private static PlacedFragment fragmentNamed(DocumentSession session, String name) {
        return session.layoutGraph().fragments().stream()
                .filter(fragment -> fragment.path() != null && fragment.path().contains(name))
                .findFirst()
                .orElseThrow();
    }

    /** Every anchored drawing in a part's XML, in document order. */
    private static List<String> anchors(String xml) {
        List<String> anchors = new ArrayList<>();
        Matcher matcher = Pattern.compile("<wp:anchor .*?</wp:anchor>", Pattern.DOTALL).matcher(xml);
        while (matcher.find()) {
            anchors.add(matcher.group());
        }
        return anchors;
    }

    /** The preset geometry of every picture in a part's XML. */
    private static List<String> pictureGeometry(String xml) {
        List<String> shapes = new ArrayList<>();
        Matcher matcher = Pattern.compile("<pic:spPr>.*?prst=\"(\\w+)\"", Pattern.DOTALL).matcher(xml);
        while (matcher.find()) {
            shapes.add(matcher.group(1));
        }
        return shapes;
    }

    /** The stacking height of every anchored drawing in some paragraphs, as written. */
    private static List<Long> heights(List<org.apache.poi.xwpf.usermodel.XWPFParagraph> paragraphs) {
        List<Long> heights = new ArrayList<>();
        for (org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph : paragraphs) {
            for (org.apache.poi.xwpf.usermodel.XWPFRun run : paragraph.getRuns()) {
                for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing drawing
                        : run.getCTR().getDrawingList()) {
                    for (org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor anchor
                            : drawing.getAnchorList()) {
                        heights.add(anchor.getRelativeHeight());
                    }
                }
            }
        }
        return heights;
    }

    private static XWPFDocument export(AtomicReference<DocxExportReport> report,
                                       Consumer<DocumentSession> content) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            content.accept(session);
            byte[] docx = session.export(report == null
                    ? new DocxSemanticBackend()
                    : new DocxSemanticBackend(report::set));
            return new XWPFDocument(new ByteArrayInputStream(docx));
        }
    }

    private static byte[] pngBytes() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
