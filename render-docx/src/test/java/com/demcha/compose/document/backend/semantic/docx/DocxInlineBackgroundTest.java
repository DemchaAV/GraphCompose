package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An inline chip is a fill behind a phrase, and Word has one.
 *
 * <p>An inline {@code code} span and a status badge both exported as bare text: the
 * reduction to text runs keeps the glyphs and drops the chip, so a red badge reading
 * "overdue" came out the same colour as the sentence around it — a document losing the
 * part of itself that was doing the talking.</p>
 *
 * <p>Word shades a run with {@code w:shd}, which takes any RGB. What it cannot express is
 * the chip's shape: shading covers the glyph box, so the rounded corners and the padding go
 * and the export says so. A {@code w:shd} fill is also opaque, so a translucent chip is
 * flattened against what Word paints beneath it first.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxInlineBackgroundTest {

    private static final DocumentColor BADGE = DocumentColor.rgb(214, 56, 56);
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);

    @Test
    void aChipsFillBecomesTheRunsShading() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p
                        .inlineText("Status: ")
                        .inlineHighlight("overdue", DocumentTextStyle.DEFAULT, BADGE,
                                0, DocumentInsets.zero())))) {

            assertThat(fillOf(runReading(document, "overdue")))
                    .as("an opaque fill is written exactly as the document gave it")
                    .isEqualTo("D63838");
            assertThat(fillOf(runReading(document, "Status: ")))
                    .as("the sentence around it is not shaded")
                    .isNull();
        }
    }

    @Test
    void aTranslucentChipIsFlattenedAgainstThePage() throws Exception {
        // The chip this sugar reaches for most is a fifth-opacity grey, and w:shd has no
        // alpha. Written at full strength it is a solid slab where the page has a tint.
        // 175/184/193 at 20% over white is 239/241/243 — the same composite the PDF makes.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.inlineText("Call ").inlineCode("render()")))) {

            assertThat(fillOf(runReading(document, "render()"))).isEqualTo("EFF1F3");
        }
    }

    @Test
    void aChipInsideAShadedCardIsFlattenedAgainstTheCard() throws Exception {
        // Flattening against the page would be flattening against something that is not
        // under it: this export writes the card's fill as the paragraph's own shading, and
        // Word paints the run's over that.
        try (XWPFDocument document = exported(page -> page
                .addSection("Card", card -> card
                        .fillColor(SURFACE)
                        .addParagraph(p -> p.inlineText("Call ").inlineCode("render()"))))) {

            // 175/184/193 at 20% over 238/243/249 is 225/231/238.
            assertThat(fillOf(runReading(document, "render()"))).isEqualTo("E1E7EE");
        }
    }

    @Test
    void aChipKeepsItsTextAndItsStyle() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p
                        .inlineText("Run ")
                        .inlineHighlight("build", DocumentTextStyle.builder()
                                        .color(DocumentColor.WHITE).size(9).build(),
                                BADGE, 0, DocumentInsets.zero())
                        .inlineText(" first")))) {

            XWPFRun chip = runReading(document, "build");
            assertThat(chip.getColor()).as("the chip's own ink survives beside its fill").isEqualTo("FFFFFF");
            assertThat(document.getParagraphs().get(0).getText())
                    .as("the sentence still reads as one")
                    .isEqualTo("Run build first");
        }
    }

    @Test
    void aLinkedChipIsStillALink() throws Exception {
        // The chip and the link are written by different parts of the run path, and the
        // one that knows about the fill must not be the one that forgets the address.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.inlineHighlight("docs", DocumentTextStyle.DEFAULT,
                        BADGE, 0, DocumentInsets.zero(),
                        new DocumentLinkOptions("https://graphcompose.dev"))))) {

            assertThat(fillOf(runReading(document, "docs"))).isEqualTo("D63838");
            assertThat(document.getParagraphs().get(0).getCTP().getHyperlinkList())
                    .as("a chip carrying a link is exported as a hyperlink")
                    .hasSize(1);
        }
    }

    @Test
    void aChipInsideAListItemIsAChip() throws Exception {
        // A badge in a bulleted list is a badge for the same reason it is one in a
        // paragraph: the list path writes its own runs and used to write them plain.
        try (XWPFDocument document = exported(page -> page
                .addList(list -> list
                        .bullet()
                        .addItem(rich -> rich.plain("Invoice ").highlight("overdue",
                                DocumentTextStyle.DEFAULT, BADGE, 0, DocumentInsets.zero()))))) {

            assertThat(fillOf(runReading(document, "overdue"))).isEqualTo("D63838");
        }
    }

    @Test
    void theShapeAChipLosesIsRecorded() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph(p -> p.inlineText("Call ").inlineCode("render()")));

        assertThat(report.count(DocxExportReport.Severity.DROPPED)).isZero();
        assertThat(report.bySubject()).containsKey("inline chip");
        DocxExportReport.Note note = report.bySubject().get("inline chip").get(0);
        assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
        assertThat(note.detail())
                .as("the fill is written; what goes is the shape around it")
                .contains("rounded corners")
                .contains("padding");
    }

    @Test
    void aSquareChipWithNoPaddingLosesNothingAndSaysNothing() throws Exception {
        // The report is a record of loss. A chip Word can hold exactly must not appear in
        // it, or a caller reading the report cannot tell the two cases apart.
        DocxExportReport report = reportOf(page -> page
                .addParagraph(p -> p.inlineHighlight("overdue", DocumentTextStyle.DEFAULT,
                        BADGE, 0, DocumentInsets.zero())));

        assertThat(report.isEmpty()).isTrue();
    }

    @Test
    void aParagraphWhoseRunsCarryNoTextStillReadsAsItsText() throws Exception {
        // The runs are walked in their authored form now, and an image run carries no text
        // at all. The paragraph's own text is what it reads, and it must still be written.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("The whole line")))) {

            assertThat(document.getParagraphs().get(0).getText()).isEqualTo("The whole line");
        }
    }

    private static String fillOf(XWPFRun run) {
        if (!run.getCTR().isSetRPr() || run.getCTR().getRPr().sizeOfShdArray() == 0) {
            return null;
        }
        CTShd shading = run.getCTR().getRPr().getShdArray(0);
        // XmlBeans hands a written hex colour back as its three bytes, not as the string
        // it was set from.
        Object fill = shading.getFill();
        return fill instanceof byte[] bytes
                ? java.util.HexFormat.of().withUpperCase().formatHex(bytes)
                : null;
    }

    private static XWPFRun runReading(XWPFDocument document, String text) {
        for (XWPFParagraph para : document.getParagraphs()) {
            for (XWPFRun run : para.getRuns()) {
                if (text.equals(run.text())) {
                    return run;
                }
            }
        }
        throw new AssertionError("no run reading '" + text + "' among "
                + document.getParagraphs().stream().flatMap(p -> p.getRuns().stream())
                        .map(r -> "'" + r.text() + "'").toList());
    }

    private static XWPFDocument exported(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 600, 20, content);
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
        return captured.get();
    }
}
