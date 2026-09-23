package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.svg.SvgIcon;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A picture or an icon in a line of text reaches Word where it sits in the line.
 *
 * <p>Both were dropped, so a contact line lost its phone and mail icons and a sentence its
 * emoji. A picture is written from its bytes; an SVG icon is drawn into a transparent picture
 * by the same raster the PPTX backend uses, and raised or lowered to where the page's
 * alignment puts it.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxInlinePictureTest {

    private static final SvgIcon ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'>"
            + "<circle cx='12' cy='12' r='10' fill='#1A5694'/></svg>");

    @Test
    void aPictureInALineIsARunBetweenItsWords() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .inlineText("Before ")
                .inlineImage(DocumentImageData.fromBytes(png(20, 20)), 20, 20)
                .inlineText(" after")))) {
            List<XWPFRun> runs = document.getParagraphs().get(0).getRuns();

            assertThat(runs).hasSize(3);
            assertThat(runs.get(0).text()).isEqualTo("Before ");
            XWPFPicture picture = runs.get(1).getEmbeddedPictures().get(0);
            assertThat(picture.getCTPicture().getSpPr().getXfrm().getExt().getCx()).isEqualTo(Units.toEMU(20));
            assertThat(runs.get(2).text()).isEqualTo(" after");
        }
    }

    @Test
    void anSvgIconIsATransparentPictureAtItsSize() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .inlineSvgIcon(ICON, 12).inlineText(" +44 20 7946 0000")))) {
            XWPFPicture picture = document.getParagraphs().get(0).getRuns().get(0).getEmbeddedPictures().get(0);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(picture.getPictureData().getData()));

            assertThat(picture.getCTPicture().getSpPr().getXfrm().getExt().getCy()).isEqualTo(Units.toEMU(12));
            assertThat(image.getRGB(0, 0) >>> 24).as("the corner outside the circle shows the page").isZero();
            assertThat(image.getRGB(image.getWidth() / 2, image.getHeight() / 2) & 0xFFFFFF).isEqualTo(0x1A5694);
        }
    }

    @Test
    void anEmojiIsAPictureWhoseDescriptionIsTheEmojiAndTheReportSaysSo() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = withReport(report, page -> page.addParagraph(p -> p
                .inlineText("Launch ").inlineEmoji(":rocket:", 14)))) {
            XWPFRun picture = document.getParagraphs().get(0).getRuns().get(1);
            String description = picture.getCTR().getDrawingArray(0).getInlineArray(0).getDocPr().getDescr();

            assertThat(picture.getEmbeddedPictures()).hasSize(1);
            assertThat(description).isEqualTo("🚀");
            assertThat(report.get().bySubject()).containsKey("inline icon");
            assertThat(report.get().count(DocxExportReport.Severity.DROPPED)).isZero();
        }
    }

    @Test
    void aParagraphOfOnlyAnIconDoesNotAlsoWriteItsText() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p.inlineEmoji(":rocket:", 14)))) {
            List<XWPFRun> runs = document.getParagraphs().get(0).getRuns();

            assertThat(runs).hasSize(1);
            assertThat(runs.get(0).getEmbeddedPictures()).hasSize(1);
        }
    }

    @Test
    void aPictureOnTheBaselineIsRaisedByItsOffset() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .inlineText("On ")
                .inlineImage(DocumentImageData.fromBytes(png(10, 10)), 10, 10, InlineImageAlignment.BASELINE, 2, null)))) {
            XWPFRun picture = document.getParagraphs().get(0).getRuns().get(1);

            // w:position counts half-points.
            assertThat(picture.getCTR().getRPr().getPositionArray(0).getVal()).isEqualTo(java.math.BigInteger.valueOf(4));
        }
    }

    @Test
    void aLineHoldingAPictureTallerThanItsTextIsAsTallAsTheLayoutMadeIt() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .inlineText("Tall ").inlineImage(DocumentImageData.fromBytes(png(30, 30)), 30, 30)))) {
            long line = DocxTwips.of(document.getParagraphs().get(0).getCTP().getPPr().getSpacing().getLine());

            assertThat(line).as("not the text's 12pt or so, which Word would clip the picture to")
                    .isGreaterThanOrEqualTo(30 * 20L);
        }
    }

    @Test
    void aLinkedPictureIsInsideTheLink() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Target").anchor("target"))
                .addParagraph(p -> p.inlineImageLinkTo(DocumentImageData.fromBytes(png(10, 10)), 10, 10, "target")))) {
            XWPFParagraph linked = document.getParagraphs().get(1);

            assertThat(linked.getCTP().getHyperlinkList()).hasSize(1);
            assertThat(linked.getCTP().getHyperlinkArray(0).xmlText()).contains("pic:pic");
        }
    }

    @Test
    void anIconInARichListItemIsAPictureToo() throws Exception {
        try (XWPFDocument document = export(page -> page.addList(l -> l.addItem(item -> item
                .plain("Call ").svgIcon(ICON, 10))))) {
            boolean pictured = document.getParagraphs().stream()
                    .flatMap(p -> p.getRuns().stream())
                    .anyMatch(r -> !r.getEmbeddedPictures().isEmpty());

            assertThat(pictured).isTrue();
        }
    }

    @Test
    void thePageAlignmentsSetThePicturesBottomFromTheBaseline() {
        // A line 12pt tall, its baseline 3pt above its bottom, text 9pt above it; a 6pt icon.
        ParagraphLine line = new ParagraphLine("x", 10, 12, 12, 9, 3, List.of(), List.of());

        assertThat(DocxSemanticBackend.inlineBottomFromBaseline(InlineImageAlignment.BASELINE, 0, 6, line)).isZero();
        assertThat(DocxSemanticBackend.inlineBottomFromBaseline(InlineImageAlignment.CENTER, 0, 6, line))
                .isCloseTo(0.0, within(1e-9));
        assertThat(DocxSemanticBackend.inlineBottomFromBaseline(InlineImageAlignment.TEXT_TOP, 0, 6, line)).isEqualTo(3.0);
        assertThat(DocxSemanticBackend.inlineBottomFromBaseline(InlineImageAlignment.TEXT_BOTTOM, 1, 6, line)).isEqualTo(-2.0);
    }

    private static byte[] png(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 400, 20, content);
    }

    private static XWPFDocument withReport(AtomicReference<DocxExportReport> report,
                                           Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            return new XWPFDocument(new ByteArrayInputStream(docx));
        }
    }
}
