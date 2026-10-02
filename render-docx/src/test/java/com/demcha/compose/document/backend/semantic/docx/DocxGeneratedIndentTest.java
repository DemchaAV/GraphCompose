package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A paragraph's blank prefix moves its lines in Word as far as it moves them on the page.
 *
 * <p>{@code EditorialProposal}'s bullets set their wrapped lines after three spaces, under the
 * text rather than under the dot; in Word those lines started under the dot, 11.6pt left.
 * Three Helvetica spaces at 10pt are 3 × 0.278 × 10 = 8.34pt, 167 twips.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxGeneratedIndentTest {

    private static final long THREE_SPACES = 167;
    private static final String TEXT = "A line long enough to wrap onto a second one in the page's narrow measure";

    @Test
    void wrappedLinesStartAfterThePrefixAndTheFirstLineDoesNot() throws Exception {
        CTInd indent = indentOf(p -> p.bulletOffset("   ").indentStrategy(DocumentTextIndent.FROM_SECOND_LINE));

        assertThat(DocxTwips.of(indent.getLeft())).isEqualTo(THREE_SPACES);
        assertThat(DocxTwips.of(indent.getHanging())).as("the first line back at the margin").isEqualTo(THREE_SPACES);
        assertThat(indent.isSetFirstLine()).isFalse();
    }

    @Test
    void onlyTheFirstLineStartsAfterAFirstLinePrefix() throws Exception {
        CTInd indent = indentOf(p -> p.bulletOffset("   ").indentStrategy(DocumentTextIndent.FIRST_LINE));

        assertThat(indent.isSetLeft()).isFalse();
        assertThat(DocxTwips.of(indent.getFirstLine())).isEqualTo(THREE_SPACES);
        assertThat(indent.isSetHanging()).isFalse();
    }

    @Test
    void everyLineStartsAfterAPrefixOnAllOfThem() throws Exception {
        CTInd indent = indentOf(p -> p.bulletOffset("   ").indentStrategy(DocumentTextIndent.ALL_LINES));

        assertThat(DocxTwips.of(indent.getLeft())).isEqualTo(THREE_SPACES);
        assertThat(indent.isSetHanging()).isFalse();
        assertThat(indent.isSetFirstLine()).isFalse();
    }

    @Test
    void thePrefixAddsToTheIndentOfTheContainerAroundIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 400, 20, page -> page
                .addSection(section -> section.padding(new DocumentInsets(0, 0, 0, 20))
                        .addParagraph(p -> styled(p).bulletOffset("   ")
                                .indentStrategy(DocumentTextIndent.FROM_SECOND_LINE))))) {
            CTInd indent = paragraph(document).getCTP().getPPr().getInd();

            assertThat(DocxTwips.of(indent.getLeft())).isEqualTo(20 * 20L + THREE_SPACES);
            assertThat(DocxTwips.of(indent.getHanging())).isEqualTo(THREE_SPACES);
        }
    }

    @Test
    void aRightToLeftParagraphIsIndentedFromItsFlowsStart() throws Exception {
        CTInd indent = indentOf(p -> p.direction(TextDirection.RTL)
                .bulletOffset("   ").indentStrategy(DocumentTextIndent.FROM_SECOND_LINE));

        // In a w:bidi paragraph w:left is the start of the flow — the page's right — and the
        // hanging indent is measured from it.
        assertThat(DocxTwips.of(indent.getLeft())).as("the flow's start").isEqualTo(THREE_SPACES);
        assertThat(indent.isSetRight()).isFalse();
        assertThat(DocxTwips.of(indent.getHanging())).isEqualTo(THREE_SPACES);
    }

    @Test
    void thePrefixIsMeasuredInTheParagraphsStyleNotItsRuns() throws Exception {
        // EditorialProposal's case: the text is an inline run in a smaller face than the
        // paragraph's, and the page sets the prefix in the paragraph's — three spaces at 14pt
        // are 3 × 0.278 × 14 = 11.68pt, 234 twips.
        DocumentTextStyle small = DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(6).build();
        try (XWPFDocument document = DocxExports.withLayout(300, 400, 20, page -> page
                .addParagraph(p -> p.inlineText(TEXT, small)
                        .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(14).build())
                        .bulletOffset("   ").indentStrategy(DocumentTextIndent.FROM_SECOND_LINE)))) {
            CTInd indent = paragraph(document).getCTP().getPPr().getInd();

            assertThat(DocxTwips.of(indent.getLeft())).isEqualTo(234L);
            assertThat(DocxTwips.of(indent.getHanging())).isEqualTo(234L);
        }
    }

    @Test
    void aPrefixWithNoStrategyIsLeftAsItWas() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 400, 20, page -> page
                .addParagraph(p -> styled(p).bulletOffset("   ").indentStrategy(DocumentTextIndent.NONE)))) {
            assertThat(paragraph(document).getCTP().getPPr().isSetInd()).isFalse();
        }
    }

    @Test
    void aPrefixWithLettersInItIsLeftAsItWas() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 400, 20, page -> page
                .addParagraph(p -> styled(p).bulletOffset("-").indentStrategy(DocumentTextIndent.ALL_LINES)))) {
            XWPFParagraph paragraph = paragraph(document);

            assertThat(paragraph.getCTP().getPPr().isSetInd()).isFalse();
        }
    }

    private static CTInd indentOf(Consumer<ParagraphBuilder> indent) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 400, 20, page -> page
                .addParagraph(p -> indent.accept(styled(p))))) {
            return (CTInd) paragraph(document).getCTP().getPPr().getInd().copy();
        }
    }

    private static ParagraphBuilder styled(ParagraphBuilder paragraph) {
        return paragraph.text(TEXT).textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(10).build());
    }

    private static XWPFParagraph paragraph(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().startsWith("A line"))
                .findFirst()
                .orElseThrow();
    }
}
