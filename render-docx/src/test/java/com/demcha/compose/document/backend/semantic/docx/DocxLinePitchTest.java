package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * A paragraph whose lines the page sets at different heights keeps the page's distance between
 * them in Word.
 *
 * <p>Word sets every line of a paragraph one height apart. {@code EditorialProposal}'s terms are
 * bullets whose wrapped lines start with a prefix of spaces set in the paragraph's style, taller
 * than the runs: on the page the first line is 12pt and the wrapped one 12.95, their baselines
 * 13.05pt apart. Written at the tallest line, Word set them 13.8pt apart, and every item stood
 * lower than the one above it.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxLinePitchTest {

    private static final DocumentTextStyle BODY = DocumentTextStyle.builder().fontName(FontName.LATO).size(10).build();
    private static final String TEXT = "   50% deposit is required to secure the project and schedule the kick-off.";

    @Test
    void linesOfDifferentHeightsAreWrittenTheirDistanceApartOnThePage() throws Exception {
        Exported exported = export(p -> p.inlineText(TEXT, BODY)
                .bulletOffset("   ").indentStrategy(DocumentTextIndent.FROM_SECOND_LINE).lineSpacing(0.87));
        List<ParagraphLine> lines = exported.lines();
        assertThat(lines).hasSize(2);
        assertThat(lines.get(1).lineHeight()).as("the prefix makes the wrapped line taller")
                .isGreaterThan(lines.get(0).lineHeight() + 0.5);

        double pitch = lines.get(0).baselineOffsetFromBottom() + exported.gap()
                       + lines.get(1).lineHeight() - lines.get(1).baselineOffsetFromBottom();
        assertThat(exported.lineTwips())
                .as("the page's distance between the baselines, not the tallest line and the gap;"
                    + " the line and the gap are rounded apart")
                .isCloseTo(Math.round(pitch * 20), org.assertj.core.api.Assertions.within(1L));
    }

    @Test
    void whatTheLinesFallShortOfThePageIsOwedBelowThem() throws Exception {
        Exported exported = export(p -> p.inlineText(TEXT, BODY)
                .bulletOffset("   ").indentStrategy(DocumentTextIndent.FROM_SECOND_LINE).lineSpacing(0.87));
        List<ParagraphLine> lines = exported.lines();
        double page = lines.get(0).lineHeight() + exported.gap() + lines.get(1).lineHeight();

        // What is owed below is written as the space above the next paragraph, which has none.
        double word = 2 * exported.lineTwips() / 20.0 - exported.gapTakenFromAbove()
                      + exported.beforeNextTwips() / 20.0;
        assertThat(word).as("the paragraph and what it owes as tall in Word as on the page")
                .isCloseTo(page, offset(0.06));
        assertThat(exported.beforeNextTwips()).isPositive();
    }

    @Test
    void linesOfOneHeightAreWrittenAsBefore() throws Exception {
        // The tallest line and the gap, the gap taken back out above: nothing owed.
        Exported exported = export(p -> p
                .text("A line long enough to wrap onto a second one and then onto a third in this measure")
                .textStyle(BODY).lineSpacing(2));
        List<ParagraphLine> lines = exported.lines();
        assertThat(lines).hasSizeGreaterThan(1);
        int n = lines.size();

        assertThat(exported.lineTwips())
                .isEqualTo(Math.round(lines.get(0).textLineHeight() * 20) + Math.round(((n - 1) * 40.0 + 40) / n));
        assertThat(exported.beforeNextTwips()).isZero();
    }

    @Test
    void aParagraphOfOneLineKeepsItsLaidOutHeight() throws Exception {
        // Nothing makes a shorter line up where a single line is set by its rise or its pictures,
        // so its prefix still counts.
        try (DocumentSession session = GraphCompose.document().pageSize(260, 400).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addParagraph(p -> p.inlineText("Short", BODY)
                    .bulletOffset("   ").indentStrategy(DocumentTextIndent.FIRST_LINE)));
            ParagraphLine line = session.layoutGraph().fragments().stream()
                    .map(fragment -> fragment.payload())
                    .filter(ParagraphFragmentPayload.class::isInstance)
                    .map(payload -> ((ParagraphFragmentPayload) payload).lines().get(0))
                    .findFirst()
                    .orElseThrow();
            XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
            XWPFParagraph paragraph = document.getParagraphs().stream()
                    .filter(p -> p.getText().contains("Short"))
                    .findFirst()
                    .orElseThrow();

            assertThat(DocxTwips.of(paragraph.getCTP().getPPr().getSpacing().getLine()))
                    .isEqualTo(Math.round(line.textLineHeight() * 20));
        }
    }

    private record Exported(XWPFParagraph paragraph, XWPFParagraph next, List<ParagraphLine> lines, double gap) {

        CTSpacing spacing() {
            return paragraph.getCTP().getPPr().getSpacing();
        }

        long lineTwips() {
            return DocxTwips.of(spacing().getLine());
        }

        /** The 12pt above the paragraph, less what is written there. */
        double gapTakenFromAbove() {
            long before = spacing().isSetBefore() ? DocxTwips.of(spacing().getBefore()) : 0;
            return (240 - before) / 20.0;
        }

        long beforeNextTwips() {
            CTSpacing spacing = next.getCTP().getPPr() == null ? null : next.getCTP().getPPr().getSpacing();
            return spacing == null || !spacing.isSetBefore() ? 0 : DocxTwips.of(spacing.getBefore());
        }
    }

    private static Exported export(Consumer<ParagraphBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(260, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> page
                    .addParagraph(p -> p.text("Above").textStyle(BODY))
                    .addParagraph(p -> content.accept(p.margin(DocumentInsets.top(12))))
                    .addParagraph(p -> p.text("Below").textStyle(BODY)));
            ParagraphFragmentPayload payload = session.layoutGraph().fragments().stream()
                    .map(fragment -> fragment.payload())
                    .filter(ParagraphFragmentPayload.class::isInstance)
                    .map(ParagraphFragmentPayload.class::cast)
                    .filter(paragraph -> paragraph.lines().size() > 1)
                    .findFirst()
                    .orElseThrow();
            byte[] docx = session.export(new DocxSemanticBackend());
            XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));
            XWPFParagraph paragraph = document.getParagraphs().stream()
                    .filter(p -> p.getText().contains("deposit") || p.getText().startsWith("A line"))
                    .findFirst()
                    .orElseThrow();
            XWPFParagraph next = document.getParagraphs().stream()
                    .filter(p -> p.getText().equals("Below"))
                    .findFirst()
                    .orElseThrow();
            return new Exported(paragraph, next, payload.lines(), Math.max(0, payload.lineGap()));
        }
    }
}
