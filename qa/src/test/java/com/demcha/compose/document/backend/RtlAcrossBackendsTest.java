package com.demcha.compose.document.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pins what each backend actually emits for the right-to-left cases that broke.
 *
 * <p>Every one of these was found by looking at a render, not by a test — a chip drawn
 * {@code )a > b(}, an Arabic bracket Word faced the wrong way, a chip whose words landed
 * in a different order on a slide than on the page. What they have in common is that the
 * mechanism tests all passed while the document was wrong, because each asserted what the
 * engine computed rather than what the file says. So this asserts the files.</p>
 *
 * <p>Two backends, asked at the level their defects lived at. The PDF is absent on
 * purpose: it is painted, so the only honest question is what the page looks like, and
 * text extraction cannot answer it — PDFBox reports each glyph's {@code ToUnicode}
 * meaning, which is the character the author typed rather than the shape that was drawn,
 * so a reversed line extracts as though it were never reversed. The page's appearance is
 * pinned by the pixel baselines in {@code RtlScenariosVisualTest} and its glyph placement
 * by {@code RtlGlyphOrderTest}; what those two cannot see is the inside of a {@code .pptx}
 * or a {@code .docx}, which is this file's job.</p>
 *
 * <ul>
 *   <li><b>PowerPoint</b> — every text frame, its declared direction, and the order the
 *       frames sit in. Mirroring, splitting and placement all show up here.</li>
 *   <li><b>Word</b> — each paragraph's text, its {@code w:bidi}, and whether its runs
 *       carry {@code w:rtl}. Word lays the text out itself, so what it is told is the
 *       whole of what this backend controls.</li>
 * </ul>
 *
 * <p>The expected values were read off the applications, not off this engine: each
 * scenario was rendered, opened in PowerPoint and in Word, and confirmed to say what its
 * author typed before it was written down here. That is what makes them worth failing on
 * — a change that moves any of them changes a document someone has already read.</p>
 */
class RtlAcrossBackendsTest {

    private static final DocumentColor CHIP_BG = DocumentColor.rgb(0xE6, 0xE9, 0xEF);
    private static final String HEBREW_HELLO = "שלום";
    private static final String HEBREW_WORLD = "עולם";

    // ---------------------------------------------------------------- chips

    @Test
    void aChipWhosePairEnclosesLatinKeepsItsComparisonFacingTheRightWay() throws Exception {
        Scenario chip = chipLine("(a > b)");

        assertThat(chip.chipFrames())
                .describedAs("three frames: the brackets take the line's level, the "
                        + "comparison between them keeps its own, and each is placed on "
                        + "its own so PowerPoint has nothing left to re-resolve")
                .containsExactly("(", "a > b", ")");
    }

    @Test
    void aChipWhosePairEnclosesHebrewIsMirroredForTheViewerThatReversesIt() throws Exception {
        Scenario chip = chipLine("(" + "שנה" + ")");

        assertThat(chip.chipFrames())
                .describedAs("one uniformly right-to-left run, so one frame — and the pair "
                        + "is swapped, because this is the run PowerPoint reverses itself")
                .containsExactly(")" + "שנה" + "(");
    }

    @Test
    void aChipMixingScriptsIsSplitSoBothFilesAgreeAboutTheOrder() throws Exception {
        Scenario chip = chipLine("a בית (ספר)");

        assertThat(chip.chipFrames())
                .describedAs("Latin first, then the Hebrew run with its pair mirrored — "
                        + "handed over whole, PowerPoint re-resolved the fragment without "
                        + "the line around it and put the words in another order than the "
                        + "PDF did")
                .containsExactly("a ", "בית )ספר(");
    }

    @Test
    void anArabicChipsRunsStayInsideItsOwnBackground() throws Exception {
        // Splitting a chip means placing each run by a width this backend measures itself,
        // and the fill was sized by the layout — so the two have to measure the same text.
        // The runs go over unshaped, for PowerPoint to join; measuring them that way is
        // measuring a wider string than the page reserved, and an Arabic chip's last run
        // ended six millimetres past its own rounded rect.
        Scenario chip = page(flow -> flow.addParagraph(p -> p
                .rich(rich -> rich.plain("مرحبا ")
                        .highlight("النسخة 2.2.0", style(FontName.AMIRI), CHIP_BG,
                                3.0, DocumentInsets.of(2))
                        .plain(" بالعالم"))
                .direction(TextDirection.RTL)
                .textStyle(style(FontName.AMIRI))));

        assertThat(chip.chipRunBounds())
                .describedAs("every run of the chip is drawn within the fill behind it")
                .isNotEmpty()
                .allSatisfy(run -> assertThat(run).isLessThanOrEqualTo(chip.chipFillRight()));
    }

    // ------------------------------------------------------------ paragraphs

    @Test
    void aLoneNeutralBetweenHebrewWordsIsMirroredInEveryFile() throws Exception {
        Scenario line = paragraph(HEBREW_HELLO + " > " + HEBREW_WORLD, FontName.DAVID_LIBRE);

        assertThat(line.pptxTexts())
                .describedAs("PowerPoint reverses this run but does not mirror it, so the "
                        + "swap is made before the hand-off")
                .anyMatch(text -> text.contains("<"));
        assertThat(line.docxParagraphs())
                .describedAs("Word does the whole algorithm itself, so it gets the text as "
                        + "typed and is told the direction twice — on the paragraph and on "
                        + "the run")
                .anySatisfy(entry -> {
                    assertThat(entry.text()).contains(">");
                    assertThat(entry.bidi()).isTrue();
                    assertThat(entry.runRightToLeft()).isTrue();
                });
    }

    @Test
    void anArabicBracketSurvivesEveryBackend() throws Exception {
        Scenario line = paragraph("النسخة 2.2.0 (صدرت في 2026)", FontName.AMIRI);

        assertThat(line.docxParagraphs())
                .describedAs("Word draws this pair the wrong way round without w:rtl on the "
                        + "run — the defect needed Arabic, where digits after a letter "
                        + "resolve as an Arabic number rather than a European one")
                .anySatisfy(entry -> assertThat(entry.runRightToLeft()).isTrue());
    }

    // ---------------------------------------------------------------- tables

    @Test
    void aTableCellCarriesItsDirectionIntoEveryBackend() throws Exception {
        Scenario table = page(flow -> flow.addTable(t -> t
                .columns(DocumentTableColumn.fixed(240))
                .defaultCellStyle(cells(FontName.DAVID_LIBRE, TextDirection.RTL))
                .row(HEBREW_HELLO + " " + HEBREW_WORLD)));

        assertThat(table.pptxTexts())
                .describedAs("the slide gets the text as typed; PowerPoint orders it")
                .anyMatch(text -> text.contains(HEBREW_HELLO));
        assertThat(table.docxParagraphs())
                .describedAs("the cell's paragraph and its run both say which way it reads")
                .anySatisfy(entry -> {
                    assertThat(entry.bidi()).isTrue();
                    assertThat(entry.runRightToLeft()).isTrue();
                });
    }

    @Test
    void autoIsAnsweredPerCellInEveryBackend() throws Exception {
        Scenario table = page(flow -> flow.addTable(t -> t
                .columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(200))
                .defaultCellStyle(cells(FontName.DAVID_LIBRE, TextDirection.AUTO))
                .row(HEBREW_HELLO, "Latin stays put")));

        assertThat(table.docxParagraphs())
                .describedAs("one declared direction, two cells, two different answers")
                .anySatisfy(entry -> {
                    assertThat(entry.text()).isEqualTo(HEBREW_HELLO);
                    assertThat(entry.bidi()).isTrue();
                })
                .anySatisfy(entry -> {
                    assertThat(entry.text()).isEqualTo("Latin stays put");
                    assertThat(entry.bidi()).isFalse();
                });
    }

    // ----------------------------------------------------------- the plumbing

    /** One document, rendered once per backend and read back three ways. */
    private record Scenario(byte[] pptx, byte[] docx) {

        /** Every text frame of the slide, left to right. */
        List<String> pptxTexts() throws Exception {
            return frames(null);
        }

        /** The right edge of each chip text frame, left to right. */
        List<Double> chipRunBounds() throws Exception {
            return chipShapes("GraphCompose Inline Chip Text").stream()
                    .map(shape -> shape.getAnchor().getMaxX())
                    .toList();
        }

        /**
         * The right edge of the rounded rect the chip's runs must stay inside.
         *
         * <p>Found by name rather than by type: POI models a text box as an auto shape
         * too, so "the widest auto shape on the slide" is a neighbouring span, and an
         * assertion written that way passes whatever the chip does.</p>
         */
        double chipFillRight() throws Exception {
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                return show.getSlides().get(0).getShapes().stream()
                        .filter(shape -> "GraphCompose Inline Chip Fill".equals(shape.getShapeName()))
                        .map(shape -> shape.getAnchor().getMaxX())
                        .max(Double::compare)
                        .orElseThrow();
            }
        }

        private List<XSLFTextShape> chipShapes(String shapeName) throws Exception {
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                List<XSLFTextShape> boxes = new ArrayList<>();
                for (XSLFShape shape : show.getSlides().get(0).getShapes()) {
                    if (shape instanceof XSLFTextShape box && !box.getText().isBlank()
                            && shapeName.equals(box.getShapeName())) {
                        boxes.add(box);
                    }
                }
                boxes.sort(Comparator.comparingDouble(box -> box.getAnchor().getX()));
                return boxes;
            }
        }

        /** Only the chip's frames, left to right. */
        List<String> chipFrames() throws Exception {
            return frames("GraphCompose Inline Chip Text");
        }

        private List<String> frames(String shapeName) throws Exception {
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                List<XSLFTextShape> boxes = new ArrayList<>();
                for (XSLFShape shape : show.getSlides().get(0).getShapes()) {
                    if (shape instanceof XSLFTextShape box && !box.getText().isBlank()
                            && (shapeName == null || shapeName.equals(box.getShapeName()))) {
                        boxes.add(box);
                    }
                }
                boxes.sort(Comparator.comparingDouble(box -> box.getAnchor().getX()));
                return boxes.stream().map(XSLFTextShape::getText).toList();
            }
        }

        /** Each Word paragraph with the two properties that decide how it is laid out. */
        List<WordParagraph> docxParagraphs() throws Exception {
            try (XWPFDocument word = new XWPFDocument(new ByteArrayInputStream(docx))) {
                List<WordParagraph> all = new ArrayList<>();
                collect(word.getParagraphs(), all);
                word.getTables().forEach(table -> table.getRows().forEach(row ->
                        row.getTableCells().forEach(cell -> collect(cell.getParagraphs(), all))));
                return all;
            }
        }

        private static void collect(List<XWPFParagraph> source, List<WordParagraph> target) {
            for (XWPFParagraph paragraph : source) {
                if (paragraph.getText().isBlank()) {
                    continue;
                }
                boolean bidi = paragraph.getCTP().isSetPPr()
                        && paragraph.getCTP().getPPr().isSetBidi();
                boolean runRtl = !paragraph.getRuns().isEmpty()
                        && paragraph.getRuns().stream().allMatch(run ->
                        run.getCTR().isSetRPr() && run.getCTR().getRPr().sizeOfRtlArray() > 0);
                target.add(new WordParagraph(paragraph.getText(), bidi, runRtl));
            }
        }
    }

    private record WordParagraph(String text, boolean bidi, boolean runRightToLeft) {
    }

    private static DocumentTextStyle style(FontName font) {
        return DocumentTextStyle.builder().fontName(font).size(15).build();
    }

    private static DocumentTableStyle cells(FontName font, TextDirection direction) {
        return DocumentTableStyle.builder().textStyle(style(font)).direction(direction).build();
    }

    private static Scenario chipLine(String chip) {
        return page(flow -> flow.addParagraph(p -> p
                .rich(rich -> rich.plain(HEBREW_HELLO + " ")
                        .highlight(chip, style(FontName.DAVID_LIBRE), CHIP_BG,
                                3.0, DocumentInsets.of(2))
                        .plain(" " + HEBREW_WORLD))
                .direction(TextDirection.RTL)
                .textStyle(style(FontName.DAVID_LIBRE))));
    }

    private static Scenario paragraph(String text, FontName font) {
        return page(flow -> flow.addParagraph(p -> p.text(text)
                .direction(TextDirection.RTL).textStyle(style(font))));
    }

    /** Lays the content out once and exports the same session to all three backends. */
    private static Scenario page(Consumer<PageFlowBuilder> content) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(560, 200)
                .margin(DocumentInsets.of(24))
                .create()) {

            document.pageFlow(content::accept);
            // The PDF is rendered too, because exporting the other two from a session that
            // never laid out its pages would be measuring a different document.
            document.toPdfBytes();
            return new Scenario(document.toPptxBytes(),
                    document.export(new DocxSemanticBackend()));
        } catch (Exception failure) {
            throw new IllegalStateException("scenario could not be rendered", failure);
        }
    }
}
