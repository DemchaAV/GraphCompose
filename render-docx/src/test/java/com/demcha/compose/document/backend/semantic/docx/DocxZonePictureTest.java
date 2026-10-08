package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ImageFragmentPayload;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.DocumentTransform;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A picture of a page zone — a logo — is written in the zone's line, where the page draws it.
 *
 * <p>Word stands an inline picture on its line's baseline, four fifths down an exact line
 * ({@link DocxTextBands#BASELINE_SHARE}). Where the picture is the line's tallest part, the line
 * is placed by its foot, which then stands where the page draws it, and is tall enough to hold
 * it. A part the page sets on a baseline of its own — the text beside a logo, set from the
 * logo's top — is raised to it by {@code w:position}.</p>
 *
 * <p>The page's place for a picture is read from the layout; for a text, from the PDF the engine
 * draws. Word's from the file: the distance from the edge, the exact line and the positions.</p>
 */
class DocxZonePictureTest {

    private static final double PAGE_HEIGHT = 600;
    private static final DocumentTextStyle CHROME = DocumentTextStyle.DEFAULT.withSize(8);
    private static final byte[] LOGO = png(48, 24);

    @Test
    void aHeadersLogoStandsWhereThePageDrawsIt() throws Exception {
        Exported exported = export(zone(DocumentHeaderFooterZone.HEADER, page -> logo().build()));
        XWPFParagraph line = exported.headerLine();
        PlacedFragment drawn = exported.picture();

        assertThat(sizeOf(line.getRuns().get(0))).containsExactly(48.0, 24.0);
        assertThat(fromTheTop(exported.margin().getHeader()) + share(line)).as("its foot, from the page's top")
                .isCloseTo(PAGE_HEIGHT - drawn.y(), within(0.1));
        assertThat(share(line)).as("the line holds it above its baseline").isGreaterThanOrEqualTo(24 - 0.05);
        assertThat(line.getRuns().get(0).getCTR().getDrawingArray(0).getInlineArray(0).getDocPr().getDescr())
                .as("no file name for a screen reader to read out").isEmpty();
        assertThat(exported.report().isEmpty()).as(String.valueOf(exported.report().notes())).isTrue();
    }

    @Test
    void aContainedLogoStandsWhereThePageDrawsItInItsBox() throws Exception {
        // A 2:1 picture contained in a 48 by 40 box is drawn 48 by 24 in its middle, 8pt above the
        // box's foot: that is the foot Word stands on the baseline.
        Exported exported = export(zone(DocumentHeaderFooterZone.HEADER, 60,
                page -> logo().size(48, 40).fitMode(DocumentImageFitMode.CONTAIN).build()));
        XWPFParagraph line = exported.headerLine();

        assertThat(sizeOf(line.getRuns().get(0))).containsExactly(48.0, 24.0);
        assertThat(fromTheTop(exported.margin().getHeader()) + share(line)).as("its drawn foot, from the page's top")
                .isCloseTo(PAGE_HEIGHT - (exported.picture().y() + 8), within(0.1));
    }

    @Test
    void textSetAboveItsLogoGrowsTheLineToHoldIt() throws Exception {
        // The logo, 6pt down the row, is the tallest part; the text, at the row's top, reaches 6pt
        // above it, past the four fifths of the logo's 30pt line above its baseline.
        Exported exported = export(zone(DocumentHeaderFooterZone.HEADER, page -> new RowBuilder().name("Line")
                .addImage(image -> image.name("Logo").source(LOGO).size(48, 24)
                        .margin(new DocumentInsets(6, 0, 0, 0)))
                .flexSpacer()
                .addParagraph(p -> p.text("Quarterly").textStyle(CHROME))
                .build()));
        XWPFParagraph line = exported.headerLine();
        double baseline = fromTheTop(exported.margin().getHeader()) + share(line);
        XWPFRun text = line.getRuns().get(line.getRuns().size() - 1);

        assertThat(lineOf(line)).as("30pt above the logo's foot, four fifths of it").isCloseTo(37.5, within(0.1));
        assertThat(baseline).as("the logo's foot").isCloseTo(PAGE_HEIGHT - exported.picture().y(), within(0.1));
        assertThat(baseline - positionOf(text)).isCloseTo(exported.baseline("Quarterly"), within(0.3));
        assertThat(exported.report().isEmpty()).as(String.valueOf(exported.report().notes())).isTrue();
    }

    @Test
    void aFootersLogoStandsWhereThePageDrawsIt() throws Exception {
        Exported exported = export(zone(DocumentHeaderFooterZone.FOOTER, page -> logo().build()));
        XWPFParagraph line = exported.footerLine();

        assertThat(fromTheTop(exported.margin().getFooter()) + (lineOf(line) - share(line)))
                .as("its foot, from the page's foot").isCloseTo(exported.picture().y(), within(0.1));
        assertThat(exported.report().isEmpty()).as(String.valueOf(exported.report().notes())).isTrue();
    }

    @Test
    void textBesideALogoIsRaisedToTheBaselineThePageSetsItOn() throws Exception {
        for (RowVerticalAlign align : List.of(RowVerticalAlign.TOP, RowVerticalAlign.CENTER)) {
            Exported exported = export(zone(DocumentHeaderFooterZone.HEADER, page -> new RowBuilder().name("Line")
                    .verticalAlign(align)
                    .addImage(image -> image.name("Logo").source(LOGO).size(48, 24))
                    .flexSpacer()
                    .addParagraph(p -> p.text("Quarterly").textStyle(CHROME))
                    .build()));
            XWPFParagraph line = exported.headerLine();
            double baseline = fromTheTop(exported.margin().getHeader()) + share(line);

            assertThat(baseline).as("the logo's foot, " + align).isCloseTo(PAGE_HEIGHT - exported.picture().y(),
                    within(0.1));
            XWPFRun text = line.getRuns().get(line.getRuns().size() - 1);
            assertThat(text.text()).isEqualTo("Quarterly");
            assertThat(baseline - positionOf(text)).as("raised onto its own baseline, to the half point, " + align)
                    .isCloseTo(exported.baseline("Quarterly"), within(0.3));
            assertThat(exported.report().isEmpty()).as(String.valueOf(exported.report().notes())).isTrue();
        }
    }

    @Test
    void textRightAfterALogoStartsWhereWordSetsIt() throws Exception {
        // Word sets the text after the picture's width, as the page sets it in the column after the
        // logo's, as wide as the logo; an even split would set it at the row's middle, named.
        Exported exported = export(zone(DocumentHeaderFooterZone.HEADER, page -> new RowBuilder().name("Line")
                .columns(com.demcha.compose.document.style.DocumentRowColumn.fixed(48),
                        com.demcha.compose.document.style.DocumentRowColumn.weight(1))
                .addImage(image -> image.name("Logo").source(LOGO).size(48, 24))
                .addParagraph(p -> p.text("Quarterly").textStyle(CHROME))
                .build()));
        Exported split = export(zone(DocumentHeaderFooterZone.HEADER, page -> new RowBuilder().name("Line")
                .addImage(image -> image.name("Logo").source(LOGO).size(48, 24))
                .addParagraph(p -> p.text("Quarterly").textStyle(CHROME))
                .build()));

        assertThat(exported.report().isEmpty()).as(String.valueOf(exported.report().notes())).isTrue();
        assertThat(split.report().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .containsExactly("a header written as one line of Word's header; 1 of its 2 parts stands off where "
                                 + "the page sets them");
    }

    @Test
    void aPageNumberBesideAFootersLogoIsRaisedEveryRunOfItsField() throws Exception {
        Exported exported = export(zone(DocumentHeaderFooterZone.FOOTER, page -> new RowBuilder().name("Line")
                .addImage(image -> image.name("Logo").source(LOGO).size(48, 24))
                .flexSpacer()
                .add(page.pageNumber(CHROME))
                .build()));
        XWPFParagraph line = exported.footerLine();
        List<XWPFRun> field = line.getRuns().subList(2, line.getRuns().size());
        double raise = positionOf(field.get(0));

        assertThat(field).as("begin, instruction, separator, result, end").hasSize(5)
                .allSatisfy(run -> assertThat(positionOf(run)).isEqualTo(raise));
        double baseline = PAGE_HEIGHT - fromTheTop(exported.margin().getFooter()) - (lineOf(line) - share(line));
        assertThat(baseline - raise).isCloseTo(exported.baseline("1"), within(0.3));
        assertThat(exported.report().isEmpty()).as(String.valueOf(exported.report().notes())).isTrue();
    }

    @Test
    void aLogoShorterThanTheTextBesideItIsRaisedToWhereThePageDrawsIt() throws Exception {
        // The tallest part, the text places the line; the logo, drawn from the row's top, is
        // raised off its baseline as an author's picture is.
        Exported exported = export(zone(DocumentHeaderFooterZone.HEADER, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Acme").textStyle(DocumentTextStyle.DEFAULT.withSize(30)))
                .flexSpacer()
                .addImage(image -> image.name("Logo").source(LOGO).size(16, 8))
                .build()));
        XWPFParagraph line = exported.headerLine();
        XWPFRun picture = line.getRuns().get(line.getRuns().size() - 1);
        double baseline = fromTheTop(exported.margin().getHeader()) + share(line);

        assertThat(sizeOf(picture)).containsExactly(16.0, 8.0);
        assertThat(baseline).as("the text's").isCloseTo(exported.baseline("Acme"), within(0.1));
        assertThat(baseline - positionOf(picture)).as("its foot, raised").isCloseTo(PAGE_HEIGHT - exported.picture().y(),
                within(0.3));
    }

    @Test
    void aLogoIsWrittenAtTheSizeThePageDrawsIt() throws Exception {
        // A 2:1 picture contained in a 60 by 24 box is drawn 48 by 24, in its middle; one that
        // covers it is drawn the box's size, cropped.
        Exported contained = export(zone(DocumentHeaderFooterZone.HEADER,
                page -> logo().size(60, 24).fitMode(DocumentImageFitMode.CONTAIN).build()));
        Exported covered = export(zone(DocumentHeaderFooterZone.HEADER,
                page -> logo().size(60, 24).fitMode(DocumentImageFitMode.COVER).build()));

        assertThat(sizeOf(contained.headerLine().getRuns().get(0))).containsExactly(48.0, 24.0);
        XWPFRun cover = covered.headerLine().getRuns().get(0);
        assertThat(sizeOf(cover)).containsExactly(60.0, 24.0);
        assertThat(cover.getEmbeddedPictures().get(0).getCTPicture().getBlipFill().isSetSrcRect())
                .as("cropped to its box").isTrue();
    }

    @Test
    void aLinkedLogoIsWrittenInItsLink() throws Exception {
        Exported exported = export(zone(DocumentHeaderFooterZone.HEADER,
                page -> logo().link(new DocumentLinkOptions("https://example.com")).build()));
        var link = exported.headerLine().getCTP().getHyperlinkArray(0);

        assertThat(link.getRArray(0).sizeOfDrawingArray()).as("the picture in the link").isEqualTo(1);
        XWPFHeaderFooter header = exported.document().getHeaderList().get(0);
        assertThat(header.getPackagePart().getRelationship(link.getId()).getTargetURI())
                .hasToString("https://example.com");
        assertThat(exported.report().isEmpty()).as(String.valueOf(exported.report().notes())).isTrue();
    }

    @Test
    void whatALogoLosesOnTheLineIsNamed() throws Exception {
        Exported exported = export(zone(DocumentHeaderFooterZone.HEADER, page -> logo()
                .transform(DocumentTransform.rotate(15)).anchor("logo")
                .bookmark(new DocumentBookmarkOptions("Logo")).build()));

        assertThat(exported.headerLine().getRuns().get(0).getEmbeddedPictures()).as("written all the same").hasSize(1);
        assertThat(exported.report().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .singleElement().asString()
                .contains("a picture's transform is not carried, so it is drawn upright at its size")
                .contains("a picture's outline entry is not written")
                .contains("a picture's anchor has no bookmark in the Word file: a link to it points at none");
        assertThat(exported.report().bySubject()).doesNotContainKey("page zone content");
    }

    @Test
    void aLogoInTwoKindsOfHeaderIsWrittenIntoEach() throws Exception {
        // A second zone on the first page alone gives the section a first-page header: the logo's
        // zone is written into it and into the ordinary one.
        Exported exported = export(session -> {
            session.chrome().zone(zone(DocumentHeaderFooterZone.HEADER, page -> logo().build()));
            session.chrome().zone(DocumentPageZone.builder().zone(DocumentHeaderFooterZone.FOOTER).height(20)
                    .appliesTo(page -> page.isFirst())
                    .content(page -> new ParagraphBuilder().text("Cover").build()).build());
        }, true);

        assertThat(exported.document().getHeaderList()).hasSize(2)
                .allSatisfy(header -> assertThat(header.getAllPictures()).as("the logo").hasSize(1));
        assertThat(exported.report().bySubject()).doesNotContainKey("page zone content");
    }

    @Test
    void aLogoThePageDrawsOtherwiseOnItsFirstPageIsWrittenAndNotMeasured() throws Exception {
        // Written 48 wide for no page in particular, it is drawn 24 wide on page 1: a line measured
        // by that is not the one written.
        Exported exported = export(session -> session.chrome().zone(zone(DocumentHeaderFooterZone.HEADER,
                page -> logo().size(page.isLast() ? 48 : 24, 24).build())), true);

        assertThat(sizeOf(exported.headerLine().getRuns().get(0))).containsExactly(48.0, 24.0);
        assertThat(exported.headerLine().getCTP().getPPr().getSpacing().isSetLineRule())
                .as("Word's own line").isFalse();
        assertThat(exported.report().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .containsExactly("a header written as one line of Word's header; whether its picture stands where "
                                 + "the page sets it is not measured");
    }

    @Test
    void aLogoOfAnotherPictureOnItsFirstPageIsNotMeasuredWhereItsHeightIsThePicturesOwn() throws Exception {
        // 48 wide, as tall as its picture makes it: a square on page 1, two to one as written.
        Exported exported = export(session -> session.chrome().zone(zone(DocumentHeaderFooterZone.HEADER, 60,
                page -> new ImageBuilder().name("Logo")
                        .source(DocumentImageData.fromBytes(page.isLast() ? LOGO : png(48, 48))).width(48).build())),
                true);

        assertThat(sizeOf(exported.headerLine().getRuns().get(0))).containsExactly(48.0, 24.0);
        assertThat(exported.report().bySubject().get("page zone")).extracting(DocxExportReport.Note::detail)
                .containsExactly("a header written as one line of Word's header; whether its picture stands where "
                                 + "the page sets it is not measured");
    }

    @Test
    void withoutALayoutALogoIsWrittenAtTheSizeItStates() throws Exception {
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, PAGE_HEIGHT)
                .margin(DocumentInsets.of(72)).create()) {
            session.chrome().zone(zone(DocumentHeaderFooterZone.HEADER, page -> logo().build()));
            session.pageFlow(page -> page.addParagraph("Body"));
            CapturingBackend captured = new CapturingBackend();
            session.export(captured);
            docx = new DocxSemanticBackend().export(captured.graph,
                    new com.demcha.compose.document.backend.semantic.SemanticExportContext(captured.canvas,
                            List.of(), null, captured.options));
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            XWPFParagraph line = document.getHeaderList().get(0).getParagraphs().get(0);
            assertThat(sizeOf(line.getRuns().get(0))).containsExactly(48.0, 24.0);
        }
    }

    private static ImageBuilder logo() {
        return new ImageBuilder().name("Logo").source(DocumentImageData.fromBytes(LOGO)).size(48, 24);
    }

    private static DocumentPageZone zone(DocumentHeaderFooterZone kind,
                                         Function<com.demcha.compose.document.output.PageContext,
                                                 com.demcha.compose.document.node.DocumentNode> content) {
        return zone(kind, 48, content);
    }

    /** A zone of a height, its content 10pt in from its top. */
    private static DocumentPageZone zone(DocumentHeaderFooterZone kind, double height,
                                         Function<com.demcha.compose.document.output.PageContext,
                                                 com.demcha.compose.document.node.DocumentNode> content) {
        return DocumentPageZone.builder().zone(kind).height(height).padding(new DocumentInsets(10, 0, 0, 0))
                .content(content::apply).build();
    }

    /** The width and the height a picture's run writes it at, in points. */
    private static List<Double> sizeOf(XWPFRun run) {
        CTInline inline = run.getCTR().getDrawingArray(0).getInlineArray(0);
        return List.of(inline.getExtent().getCx() / 12700.0, inline.getExtent().getCy() / 12700.0);
    }

    /** How far a run is raised off its line's baseline, in points: its {@code w:position}. */
    private static double positionOf(XWPFRun run) {
        var properties = run.getCTR().getRPr();
        assertThat(properties).as("a raised run's properties").isNotNull();
        assertThat(properties.sizeOfPositionArray()).as("a raised run's position").isEqualTo(1);
        return ((Number) properties.getPositionArray(0).getVal()).doubleValue() / 2;
    }

    private static double share(XWPFParagraph paragraph) {
        return 0.8 * lineOf(paragraph);
    }

    private static double lineOf(XWPFParagraph paragraph) {
        CTSpacing spacing = paragraph.getCTP().getPPr().getSpacing();
        return DocxTwips.of(spacing.getLine()) / 20.0;
    }

    private static double fromTheTop(Object twips) {
        return DocxTwips.of(twips) / 20.0;
    }

    private static byte[] png(int width, int height) {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(width, height,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
    }

    /** Takes the graph, the canvas and the options a session hands any backend. */
    private static final class CapturingBackend
            implements com.demcha.compose.document.backend.semantic.SemanticBackend<byte[]> {

        private com.demcha.compose.document.layout.DocumentGraph graph;
        private com.demcha.compose.document.layout.LayoutCanvas canvas;
        private com.demcha.compose.document.output.DocumentOutputOptions options;

        @Override
        public String name() {
            return "capture";
        }

        @Override
        public byte[] export(com.demcha.compose.document.layout.DocumentGraph documentGraph,
                             com.demcha.compose.document.backend.semantic.SemanticExportContext context) {
            this.graph = documentGraph;
            this.canvas = context.canvas();
            this.options = context.outputOptions();
            return new byte[0];
        }
    }

    private record Exported(XWPFDocument document, DocxExportReport report, List<TextPosition> text,
                            List<PlacedFragment> pictures) {

        CTPageMar margin() {
            return document.getDocument().getBody().getSectPr().getPgMar();
        }

        XWPFParagraph headerLine() {
            return document.getHeaderList().get(0).getParagraphs().get(0);
        }

        XWPFParagraph footerLine() {
            return document.getFooterList().get(0).getParagraphs().get(0);
        }

        /** The box the page draws the zone's one picture in, on the first page. */
        PlacedFragment picture() {
            return pictures.get(0);
        }

        /** Where the page sets a word's baseline, from its top, on the first page it draws it. */
        double baseline(String word) {
            StringBuilder letters = new StringBuilder();
            for (int start = 0; start < text.size(); start++) {
                letters.setLength(0);
                for (int index = start; index < text.size() && letters.length() < word.length(); index++) {
                    letters.append(text.get(index).getUnicode());
                }
                if (letters.toString().equals(word)) {
                    return text.get(start).getYDirAdj();
                }
            }
            throw new AssertionError("the page draws no " + word);
        }
    }

    private static Exported export(DocumentPageZone zone) throws Exception {
        return export(session -> session.chrome().zone(zone), false);
    }

    private static Exported export(Consumer<DocumentSession> chrome, boolean twoPages) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, PAGE_HEIGHT)
                .margin(DocumentInsets.of(72))
                .create()) {
            chrome.accept(session);
            session.pageFlow(page -> {
                page.addParagraph(p -> p.text("Body"));
                if (twoPages) {
                    page.addPageBreak(pageBreak -> { });
                    page.addParagraph(p -> p.text("More"));
                }
            });
            List<PlacedFragment> pictures = session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.path().startsWith("@page-zone") && fragment.pageIndex() == 0
                                        && fragment.payload() instanceof ImageFragmentPayload)
                    .toList();
            List<TextPosition> text = textOf(session.toPdfBytes());
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get(), text, pictures);
        }
    }

    /** Every letter the page draws, in the order it draws them, page by page. */
    private static List<TextPosition> textOf(byte[] pdf) throws IOException {
        List<TextPosition> letters = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void processTextPosition(TextPosition text) {
                    letters.add(text);
                }
            };
            stripper.getText(document);
        }
        return letters;
    }
}
