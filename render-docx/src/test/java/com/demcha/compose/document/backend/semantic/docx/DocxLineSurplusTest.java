package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A paragraph Word sets taller than the page takes the difference out of the space below it, and
 * names what that space cannot take.
 *
 * <p>Word sets every line of a paragraph at one height, which the export writes as its tallest
 * line's; the page sets each line at its own. A paragraph opening on a large run sets its wrapped
 * line closer on the page than in Word, and Word's paragraph ends that much lower. What follows
 * takes it out of the space between them, as it takes a line hanging below the block above; what
 * the space cannot take is named.</p>
 */
class DocxLineSurplusTest {

    private static final DocumentTextStyle LARGE = DocumentTextStyle.builder().size(20).build();
    private static final DocumentTextStyle SMALL = DocumentTextStyle.builder().size(9).build();

    /** The page's content: 400 x 600 with a 20pt margin. */
    private static final double CONTENT = 560;
    /** What the default text's line after the paragraph is raised into the space above it, in points. */
    private static final double RAISE = DocxExports.DEFAULT_LINE_RAISE / 20.0;

    @Test
    void theSpaceBelowAParagraphWordSetsTallerTakesTheDifference() throws Exception {
        Exported exported = export(page -> page.spacing(20).addParagraph(DocxLineSurplusTest::mixed).addParagraph("After"));
        double surplus = surplus(exported);

        assertThat(surplus).as("the premise: Word's lines at the tallest pass the page's").isGreaterThan(1);
        assertThat(before(paragraph(exported.document(), "After")) / 20.0).as("the gap less what Word's lines pass the page by")
                .isCloseTo(20 - surplus - RAISE, within(0.05));
        assertThat(exported.report().bySubject().get("space above")).isNull();
    }

    @Test
    void whatTheSpaceBelowCannotTakeIsNamed() throws Exception {
        Exported exported = export(page -> page.addParagraph(DocxLineSurplusTest::mixed)
                .addParagraph(p -> p.name("After").text("After")));
        double surplus = surplus(exported);

        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("After");
            assertThat(note.detail()).isEqualTo("it stands " + points(surplus) + "pt lower than the page "
                    + "sets it, and what follows with it: the space the page leaves above it does not hold the line "
                    + "hanging below the block above it (" + points(surplus) + "pt)");
        });
    }

    @Test
    void aParagraphEndingAPageLeavesWhatItPassesThePageByThere() throws Exception {
        // The block the layout opens the next page with stands at that page's top, as in Word:
        // the hang stays on the page above, and nothing is named.
        double mixed = export(page -> page.addParagraph(DocxLineSurplusTest::mixed)).heights().get("Mixed");
        Exported exported = export(page -> page.spacer(0, CONTENT - mixed - 2)
                .addParagraph(DocxLineSurplusTest::mixed)
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(surplus(exported)).as("the premise: Word's lines pass the page's").isGreaterThan(1);
        assertThat(exported.pages().get("After")).as("the premise: the layout opens a page with it")
                .isGreaterThan(exported.pages().get("Mixed"));
        assertThat(exported.report().bySubject().get("space above")).isNull();
    }

    @Test
    void whatABlockEndingAPageNeverTookIsNamedThere() throws Exception {
        // A list item takes nothing out of a top edge of its own: the paragraph's hang over it
        // stands it lower on its page, which the paragraph opening the next page leaves named.
        double mixed = export(page -> page.addParagraph(DocxLineSurplusTest::mixed)).heights().get("Mixed");
        double points = export(page -> page.addList(list -> list.name("Points").bullet().items("Point")))
                .heights().get("Points");
        Exported exported = export(page -> page.spacer(0, CONTENT - mixed - points - 2)
                .addParagraph(DocxLineSurplusTest::mixed)
                .addList(list -> list.name("Points").bullet().items("Point"))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(exported.pages().get("After")).as("the premise: the layout opens a page with it")
                .isGreaterThan(exported.pages().get("Points"));
        assertThat(exported.report().bySubject().get("space above")).singleElement()
                .satisfies(note -> assertThat(note.path()).contains("Points"));
    }

    @Test
    void theSpaceBelowAParagraphOfOneSizeTakesWhatItsRoundingAdds() throws Exception {
        // Rounded up to the twip over enough lines to pass the page by a tenth of a point and
        // more: Word sets the lines as written, OrangeOps' achievement lines among them.
        Exported exported = rounded(surplus -> surplus > 0.15);
        double line = line(paragraph(exported.document(), "rounded"));
        double page = exported.heights().get("Rounded");
        double surplus = Math.round(page / line) * line - page;

        assertThat(before(paragraph(exported.document(), "After")) / 20.0).as("the gap less what the lines pass the page by")
                .isCloseTo(20 - surplus - RAISE, within(0.05));
        assertThat(exported.report().bySubject().get("space above")).isNull();
    }

    @Test
    void aParagraphWrittenAtItsTallestLineOwesNothingForItsRounding() throws Exception {
        // Rounded down to the twip, over enough lines to fall a tenth of a point and more short of
        // the page: owed, that rounding stood ModernInvoice's lines a step of Word's grid lower.
        Exported exported = rounded(surplus -> surplus < -0.15);

        assertThat(before(paragraph(exported.document(), "After"))).as("the gap alone")
                .isEqualTo(400 - DocxExports.DEFAULT_LINE_RAISE);
    }

    @Test
    void aSpacerOfNoHeightWithNoSpaceBelowItIsNamed() throws Exception {
        Exported exported = export(page -> page.addParagraph("Above")
                .addSpacer(spacer -> spacer.name("Anchor").width(100).height(0))
                .addParagraph(p -> p.name("After").text("After")));

        assertThat(exported.report().bySubject().get("space above")).singleElement().satisfies(note -> {
            assertThat(note.path()).contains("After");
            assertThat(note.detail()).isEqualTo("it stands 0.1pt lower than the page sets it, and what follows with "
                    + "it: the space the page leaves above it does not hold the line hanging below the block above "
                    + "it (0.1pt)");
        });
    }

    /**
     * A one-size paragraph of many lines, followed 20pt below by another, at the first type size
     * from 8 to 12pt whose lines, written at the twip, pass the page's by what {@code passing}
     * accepts.
     */
    private static Exported rounded(java.util.function.DoublePredicate passing) throws Exception {
        String text = "rounded text that wraps onto many lines ".repeat(20);
        for (double size = 8; size < 12; size += 0.05) {
            DocumentTextStyle style = DocumentTextStyle.builder().size(size).build();
            Exported exported = export(page -> page.spacing(20)
                    .addParagraph(p -> p.name("Rounded").inlineText(text, style))
                    .addParagraph("After"));
            double line = line(paragraph(exported.document(), "rounded"));
            double page = exported.heights().get("Rounded");
            if (passing.test(Math.round(page / line) * line - page)) {
                return exported;
            }
        }
        throw new AssertionError("the premise: no size from 8 to 12pt rounds its lines that far off the page");
    }

    private static void mixed(ParagraphBuilder paragraph) {
        paragraph.name("Mixed").inlineText("Lead ", LARGE)
                .inlineText("small text that wraps onto a second line ".repeat(3), SMALL);
    }

    /** How far Word's two lines, both at the paragraph's one height, pass the page's. */
    private static double surplus(Exported exported) {
        double line = line(paragraph(exported.document(), "Lead"));
        double page = exported.heights().get("Mixed");
        assertThat(page).as("the premise: the paragraph is two lines, the second shorter").isBetween(line, 2 * line);
        return 2 * line - page;
    }

    private static double line(XWPFParagraph paragraph) {
        return DocxTwips.of(paragraph.getCTP().getPPr().getSpacing().getLine()) / 20.0;
    }

    private static String points(double points) {
        return java.math.BigDecimal.valueOf(Math.round(points * 100) / 100.0).stripTrailingZeros().toPlainString();
    }

    private static XWPFParagraph paragraph(XWPFDocument document, String start) {
        return document.getParagraphs().stream().filter(p -> p.getText().startsWith(start)).findFirst().orElseThrow();
    }

    private static long before(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()) {
            return 0;
        }
        return DocxTwips.of(properties.getSpacing().getBefore());
    }

    private record Exported(XWPFDocument document, DocxExportReport report, Map<String, Double> heights,
                            Map<String, Integer> pages) {
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        Map<String, Double> heights = new HashMap<>();
        Map<String, Integer> pages = new HashMap<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            session.layoutGraph().nodes().forEach(node -> {
                if (node.semanticName() != null) {
                    heights.putIfAbsent(node.semanticName(), node.placementHeight());
                    pages.putIfAbsent(node.semanticName(), node.startPage());
                }
            });
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get(), heights, pages);
    }
}
