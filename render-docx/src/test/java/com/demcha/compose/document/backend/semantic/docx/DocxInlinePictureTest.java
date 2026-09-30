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
            assertThat(picture.getEmbeddedPictures().get(0).getCTPicture().getNvPicPr().getCNvPr().getDescr())
                    .as("the picture's own name for it, which some readers take instead")
                    .isEqualTo("🚀");
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
    void aLineHoldingAPictureTallerThanItsTextIsThePagesLineGrownToIt() throws Exception {
        // The page makes the line as tall as the picture: one line holding it is written exactly
        // that tall, and Word sets the picture in it where the page does.
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .inlineText("Tall ").inlineImage(DocumentImageData.fromBytes(png(30, 30)), 30, 30)))) {
            var spacing = document.getParagraphs().get(0).getCTP().getPPr().getSpacing();

            assertThat(spacing.getLineRule())
                    .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
            assertThat(DocxTwips.of(spacing.getLine())).isGreaterThanOrEqualTo(30 * 20L);
        }
    }

    @Test
    void anIconLineIsWrittenAtThePagesHeightNotGrownToWordsOwn() throws Exception {
        // A 12pt icon the page centres on a 14pt line stands inside it where Word puts it. Grown
        // "at least" instead, Word made TimelineMinimal's contact lines 0.9pt taller each.
        try (XWPFDocument plain = export(page -> page.addParagraph(p -> p.inlineText("+44 20 7946 0000")));
             XWPFDocument iconed = export(page -> page.addParagraph(p -> p
                     .inlineSvgIcon(ICON, 12).inlineText(" +44 20 7946 0000")))) {
            var spacing = iconed.getParagraphs().get(0).getCTP().getPPr().getSpacing();

            assertThat(spacing.getLineRule())
                    .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
            assertThat(DocxTwips.of(spacing.getLine())).as("the page's line, which the icon fits")
                    .isEqualTo(DocxTwips.of(plain.getParagraphs().get(0).getCTP().getPPr().getSpacing().getLine()));
        }
    }

    @Test
    void contactLinesStepAsThePageStepsThemInWord() throws Exception {
        // TimelineMinimal's contact stack: a 10.5pt icon lowered beside 7pt text, 3pt apart.
        // Grown "at least" to the icon, each line came out 0.9pt taller in Word.
        com.demcha.compose.document.style.DocumentTextStyle small =
                com.demcha.compose.document.style.DocumentTextStyle.DEFAULT.withSize(7);
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addSection("Contact", contact -> {
                contact.spacing(3);
                for (String line : List.of("London, UK", "+44 20 5555 1000", "GitHub")) {
                    contact.addParagraph(p -> p.name("Line").textStyle(small).inlineText(line + "  ", small)
                            .inlineSvgIcon(ICON, 10.5, InlineImageAlignment.CENTER, -1.35, null));
                }
            }));
            List<Double> tops = session.layoutGraph().nodes().stream()
                    .filter(node -> node.nodeKind().equals("ParagraphNode"))
                    .map(node -> node.placementY() + node.placementHeight())
                    .toList();
            XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
            List<XWPFParagraph> lines = document.getParagraphs();

            for (int i = 1; i < 3; i++) {
                var above = lines.get(i - 1).getCTP().getPPr().getSpacing();
                var spacing = lines.get(i).getCTP().getPPr().getSpacing();
                assertThat(spacing.getLineRule()).as("exact, at the page's height")
                        .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
                double pageStep = tops.get(i - 1) - tops.get(i);
                // Each line starts as far above the page's as the next, the same icon in each.
                double wordStep = (DocxTwips.of(above.getLine())
                                   + (spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0)) / 20.0;
                assertThat(wordStep).as("line %d steps as the page's", i).isCloseTo(pageStep, within(0.1));
            }
        }
    }

    @Test
    void anIconsInkAboveThePagesLineComesOutOfTheSpaceAboveIt() throws Exception {
        // Lowered 1.35pt, the icon's ink stands above the page's line by as much: the Word line
        // starts that much higher, and the 3pt above it is written that much shorter.
        Laid laid = contactLines(InlineImageAlignment.CENTER, -1.35);
        var spacing = laid.lines().get(1).getCTP().getPPr().getSpacing();
        double up = 3 - DocxTwips.of(spacing.getBefore()) / 20.0;

        assertThat(up).as("the space above gives up the ink's reach").isGreaterThan(0.5);
        assertThat(DocxTwips.of(spacing.getLine()) / 20.0).as("the page's line and that reach")
                .isCloseTo(laid.heights().get(1) + up, within(0.1));
    }

    @Test
    void anIconsInkBelowThePagesLineComesOutOfTheSpaceBelowIt() throws Exception {
        // Set from the text's top, the icon hangs below the page's line: the Word line ends that
        // much lower, and the space above the next line is written that much shorter.
        Laid laid = contactLines(InlineImageAlignment.TEXT_TOP, 1.5);
        var first = laid.lines().get(0).getCTP().getPPr().getSpacing();
        var next = laid.lines().get(1).getCTP().getPPr().getSpacing();
        double down = DocxTwips.of(first.getLine()) / 20.0 - laid.heights().get(0);

        assertThat(first.getLineRule())
                .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
        assertThat(down).as("the line reaches the ink below it").isGreaterThan(0.5);
        assertThat(DocxTwips.of(next.getBefore()) / 20.0).as("the 3pt gap, less the hang")
                .isCloseTo(3 - down, within(0.1));
    }

    @Test
    void aLineWithNoSpaceAboveToTakeLeavesItAsItWas() throws Exception {
        // The first line opens the page: nothing above it gives up the icon's reach, so the room
        // for it is made in the line itself, the page's line and the reach together, as it was.
        Laid laid = contactLines(InlineImageAlignment.CENTER, -1.35);
        var first = laid.lines().get(0).getCTP().getPPr().getSpacing();
        var second = laid.lines().get(1).getCTP().getPPr().getSpacing();

        assertThat(first.isSetBefore() ? DocxTwips.of(first.getBefore()) : 0L).as("nothing taken above").isZero();
        assertThat(DocxTwips.of(first.getLine())).as("as tall as a line that took its reach from above")
                .isEqualTo(DocxTwips.of(second.getLine()));
    }

    @Test
    void aLineHoldingOnlyAPictureIsGrownAsBefore() throws Exception {
        // With no text on it the line is not seated on the page's baseline, and held exact the
        // picture would stand where Word's baseline puts it, its top cut by the line's.
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .inlineImage(DocumentImageData.fromBytes(png(30, 30)), 30, 30)))) {
            assertThat(document.getParagraphs().get(0).getCTP().getPPr().getSpacing().getLineRule())
                    .isNotEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
        }
    }

    /** The Word paragraphs of three contact lines, and the page's height of each. */
    private record Laid(List<XWPFParagraph> lines, List<Double> heights) {
    }

    private static Laid contactLines(InlineImageAlignment alignment, double offset) throws Exception {
        com.demcha.compose.document.style.DocumentTextStyle small =
                com.demcha.compose.document.style.DocumentTextStyle.DEFAULT.withSize(7);
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addSection("Contact", contact -> {
                contact.spacing(3);
                for (String line : List.of("London, UK", "+44 20 5555 1000", "GitHub")) {
                    contact.addParagraph(p -> p.textStyle(small).inlineText(line + "  ", small)
                            .inlineSvgIcon(ICON, 10.5, alignment, offset, null));
                }
            }));
            List<Double> heights = session.layoutGraph().nodes().stream()
                    .filter(node -> node.nodeKind().equals("ParagraphNode"))
                    .map(node -> node.placementHeight())
                    .toList();
            XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
            return new Laid(document.getParagraphs(), heights);
        }
    }

    @Test
    void anIconInAParagraphOfSeveralLinesGrowsTheLineForTheEditorThatStandsItOnTheBaseline() throws Exception {
        // Word has one line height for a paragraph, and the page makes only the icon's line
        // taller: such a paragraph is left "at least", for an editor to grow the line it needs.
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .inlineSvgIcon(ICON, 12).inlineText(" +44 20 7946 0000".repeat(8))))) {
            assertThat(document.getParagraphs().get(0).getCTP().getPPr().getSpacing().getLineRule())
                    .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.AT_LEAST);
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
    void anIconRisingAboveAListsTextIsCentredOnALineThatGrowsToIt() throws Exception {
        // 9pt text, a 14pt icon: at an exact height the editor clipped the icon's top, so the
        // line is at least the height the icon reaches, and the icon is lowered to the centre.
        try (XWPFDocument document = export(page -> page.addList(l -> l.hangingIndent(true)
                .textStyle(com.demcha.compose.document.style.DocumentTextStyle.builder().size(9).build())
                .addItem(item -> item.plain("Call ").svgIcon(ICON, 14))))) {
            XWPFParagraph item = document.getParagraphs().get(0);
            XWPFRun picture = item.getRuns().stream().filter(r -> !r.getEmbeddedPictures().isEmpty()).findFirst().orElseThrow();
            var spacing = item.getCTP().getPPr().getSpacing();

            assertThat(((Number) picture.getCTR().getRPr().getPositionArray(0).getVal()).intValue()).isNegative();
            assertThat(spacing.getLineRule()).isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.AT_LEAST);
            assertThat(DocxTwips.of(spacing.getLine())).isGreaterThanOrEqualTo(14 * 20L);
        }
    }

    @Test
    void eachListItemPlacesItsIconByItsOwnLineNotTheFirstItems() throws Exception {
        // The first item's 30pt picture makes its line 30pt; the second item's line is its
        // own 12pt icon's, and the icon is centred on that — as it is in a list of it alone.
        try (XWPFDocument both = export(page -> page.addList(l -> l.hangingIndent(true)
                .textStyle(com.demcha.compose.document.style.DocumentTextStyle.builder().size(9).build())
                .addItem(item -> item.plain("Logo ").svgIcon(ICON, 30))
                .addItem(item -> item.plain("Call ").svgIcon(ICON, 12))));
             XWPFDocument alone = export(page -> page.addList(l -> l.hangingIndent(true)
                     .textStyle(com.demcha.compose.document.style.DocumentTextStyle.builder().size(9).build())
                     .addItem(item -> item.plain("Call ").svgIcon(ICON, 12))))) {
            List<Integer> positions = picturePositions(both);

            assertThat(positions).hasSize(2);
            assertThat(positions.get(1)).isEqualTo(picturePositions(alone).get(0)).isNegative();
        }
    }

    @Test
    void anItemsLineIsAsTallAsItsTextOrItsTallestGraphic() {
        ParagraphLine listLine = new ParagraphLine("x", 10, 30, 10.4, 7, 2, List.of(), List.of());

        assertThat(DocxSemanticBackend.itemLine(listLine, List.of()).lineHeight()).isEqualTo(10.4);
        ParagraphLine iconed = DocxSemanticBackend.itemLine(listLine, List.of(
                new com.demcha.compose.document.node.InlineSvgRun(ICON, 12, 12, InlineImageAlignment.CENTER, 0,
                        (com.demcha.compose.document.node.DocumentLinkTarget) null)));
        assertThat(iconed.lineHeight()).isEqualTo(12);
        assertThat(iconed.textAscent()).isEqualTo(7);
        assertThat(iconed.baselineOffsetFromBottom()).isEqualTo(2);
    }

    @Test
    void aParagraphLeavesItsTextWhenAnyOfItsPicturesDoes() {
        var inside = new DocxSemanticBackend.PictureReach(10, false, 12, 0, 0);
        var leaving = new DocxSemanticBackend.PictureReach(8, true, 12, 0, 1);

        assertThat(inside.max(leaving)).isEqualTo(new DocxSemanticBackend.PictureReach(10, true, 12, 0, 1));
        assertThat(leaving.max(inside)).isEqualTo(new DocxSemanticBackend.PictureReach(10, true, 12, 0, 1));
    }

    @Test
    void aLineWhosePicturesStandUnknownHasNoPagesLine() {
        var placed = new DocxSemanticBackend.PictureReach(10, true, 12, 1, 0);
        var unplaced = DocxSemanticBackend.PictureReach.unplaced(8);

        assertThat(placed.max(unplaced).pageLine()).as("one picture stands where it is not known").isZero();
        assertThat(unplaced.max(placed).pageLine()).isZero();
        assertThat(DocxSemanticBackend.PictureReach.NONE.max(placed)).as("no picture is no answer").isEqualTo(placed);
        assertThat(placed.max(DocxSemanticBackend.PictureReach.NONE)).isEqualTo(placed);
    }

    private static List<Integer> picturePositions(XWPFDocument document) {
        return document.getParagraphs().stream()
                .flatMap(paragraph -> paragraph.getRuns().stream())
                .filter(run -> !run.getEmbeddedPictures().isEmpty())
                .map(run -> run.getCTR().isSetRPr() && run.getCTR().getRPr().sizeOfPositionArray() > 0
                        ? ((Number) run.getCTR().getRPr().getPositionArray(0).getVal()).intValue() : 0)
                .toList();
    }

    @Test
    void anIconWithinTheTextsHeightLeavesTheLineAlone() throws Exception {
        try (XWPFDocument plain = export(page -> page.addParagraph(p -> p.inlineText("Call +44 20 7946 0000")));
             XWPFDocument iconed = export(page -> page.addParagraph(p -> p
                     .inlineSvgIcon(ICON, 8).inlineText(" +44 20 7946 0000")))) {
            var spacing = iconed.getParagraphs().get(0).getCTP().getPPr().getSpacing();
            assertThat(DocxTwips.of(spacing.getLine()))
                    .isEqualTo(DocxTwips.of(plain.getParagraphs().get(0).getCTP().getPPr().getSpacing().getLine()));
            assertThat(spacing.getLineRule())
                    .as("still the page's exact height")
                    .isEqualTo(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
        }
    }

    @Test
    void aPictureHasNoDescriptionButTheTextItStandsFor() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .inlineText("A ").inlineImage(DocumentImageData.fromBytes(png(10, 10)), 10, 10)))) {
            XWPFRun run = document.getParagraphs().get(0).getRuns().get(1);

            assertThat(run.getCTR().getDrawingArray(0).getInlineArray(0).getDocPr().getDescr()).isEmpty();
            assertThat(run.getEmbeddedPictures().get(0).getCTPicture().getNvPicPr().getCNvPr().getDescr())
                    .as("not the file name POI hands it, which a screen reader would read")
                    .isEmpty();
        }
    }

    @Test
    void aPictureLeavesTheTextWhenEitherEditorPutsItPastTheAscentOrTheDescent() {
        // A line with a 9pt ascent and a 2pt descent.
        ParagraphLine line = new ParagraphLine("x", 10, 14, 12, 9, 2, List.of(), List.of());

        // Lowered 2pt, an 8pt picture tops out at 6pt in Word and at 8pt on LibreOffice's
        // baseline: within the ascent either way. The line asks for Word's reach, 2 + 6.
        // Both stand inside the page's 14pt line where Word puts them.
        assertThat(DocxSemanticBackend.PictureReach.of(-2, 8, line))
                .isEqualTo(new DocxSemanticBackend.PictureReach(8, false, 14, 0, 0));
        // A 10pt one tops out at 8pt in Word, inside, but at 10pt on the baseline, outside: the
        // line is "at least" Word's 2 + 8, and LibreOffice grows it to its own placement.
        assertThat(DocxSemanticBackend.PictureReach.of(-2, 10, line))
                .isEqualTo(new DocxSemanticBackend.PictureReach(10, true, 14, 0, 0));
        // A 14pt one is outside in both, and meets the top of the page's 14pt line; a 16pt one
        // passes it by 2pt.
        assertThat(DocxSemanticBackend.PictureReach.of(-2, 14, line).overText()).isTrue();
        assertThat(DocxSemanticBackend.PictureReach.of(-2, 14, line).above()).isZero();
        assertThat(DocxSemanticBackend.PictureReach.of(-2, 16, line).above()).isEqualTo(2);
        // One lowered 4pt hangs past the 2pt descent in Word, 2pt below the line's foot.
        assertThat(DocxSemanticBackend.PictureReach.of(-4, 5, line).overText()).isTrue();
        assertThat(DocxSemanticBackend.PictureReach.of(-4, 5, line).below()).isEqualTo(2);
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
