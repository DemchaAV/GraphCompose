package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A heading is a statement about structure, and it comes from the document saying so.
 *
 * <p>Word builds its Navigation Pane, its table of contents and its outline view from
 * heading <em>styles</em>. An export that wrote a bookmark and stopped there produced a
 * document that could be jumped to by name and had no structure to move around in at all —
 * a twenty-page report opening as one flat run of paragraphs.</p>
 *
 * <p>The role is read from the outline level the document stated when it declared the
 * bookmark, never from how the paragraph looks. A heading guessed from font size turns a
 * large first line into a chapter and a small real heading into body text, and both are
 * wrong in a document a person then edits.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxHeadingRoleTest {

    @Test
    void aDeclaredOutlineLevelBecomesTheMatchingHeadingStyle() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("Chapter").bookmark(new DocumentBookmarkOptions("Chapter", 0)))
                .addParagraph(p -> p.text("Section").bookmark(new DocumentBookmarkOptions("Section", 1)))
                .addParagraph(p -> p.text("Body")))) {

            assertThat(styleOf(document, "Chapter")).isEqualTo("Heading1");
            assertThat(styleOf(document, "Section")).isEqualTo("Heading2");
            assertThat(styleOf(document, "Body"))
                    .as("a paragraph that claimed nothing stays body text")
                    .isNull();
        }
    }

    @Test
    void wordRecognisesTheStyleAsItsOwnHeading() throws Exception {
        // Word knows its built-in headings by the pair: the id HeadingN and the name
        // "heading N". With only one of them the style is a custom style that happens to be
        // called Heading, and the Navigation Pane stays empty.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("Chapter").bookmark(new DocumentBookmarkOptions("Chapter", 0))))) {

            CTStyle heading = styleById(document, "Heading1");
            assertThat(heading).isNotNull();
            assertThat(heading.getName().getVal()).isEqualTo("heading 1");
            assertThat(heading.getPPr().getOutlineLvl().getVal().intValue())
                    .as("the outline level is what puts it in the Navigation Pane")
                    .isZero();
        }
    }

    @Test
    void theHeadingStyleCarriesNoFormattingOfItsOwn() throws Exception {
        // The paragraph already carries the look its author gave it. A heading style that
        // also set a font and a size would restyle every heading on the way out — the
        // export would be redesigning the page rather than describing it.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("Chapter")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(21))
                        .bookmark(new DocumentBookmarkOptions("Chapter", 0))))) {

            CTStyle heading = styleById(document, "Heading1");
            assertThat(heading.isSetRPr()).as("no run properties").isFalse();
            assertThat(heading.getPPr().isSetSpacing()).as("no spacing").isFalse();
            assertThat(heading.getBasedOn().getVal())
                    .as("everything else is inherited, so Normal still reaches it")
                    .isEqualTo("Normal");
        }
    }

    @Test
    void onlyTheLevelsTheDocumentUsesAreDefined() throws Exception {
        // Nine heading styles in a document with one heading is eight entries in Word's
        // gallery that nothing refers to.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("Chapter").bookmark(new DocumentBookmarkOptions("Chapter", 0))))) {

            assertThat(headingIds(document)).containsExactly("Heading1");
        }
    }

    @Test
    void aLevelPastWordsNineIsClampedRatherThanWrittenAsNothing() throws Exception {
        // ST_DecimalNumber would take it, but Word has nine levels and a tenth reads as a
        // style that does not exist — which is a paragraph with no role at all.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("Deep").bookmark(new DocumentBookmarkOptions("Deep", 12))))) {

            assertThat(styleOf(document, "Deep")).isEqualTo("Heading9");
            assertThat(headingIds(document)).containsExactly("Heading9");
        }
    }

    @Test
    void sizeAloneNeverMakesAHeading() throws Exception {
        // The rule the plan states, pinned: a big paragraph beside a small one claims
        // nothing about structure, and the export must not decide otherwise.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("Large")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(28)))
                .addParagraph(p -> p.text("Small")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(9))))) {

            assertThat(styleOf(document, "Large")).isNull();
            assertThat(headingIds(document)).isEmpty();
        }
    }

    private static String styleOf(XWPFDocument document, String text) {
        XWPFParagraph para = document.getParagraphs().stream()
                .filter(p -> text.equals(p.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no paragraph reading " + text));
        return para.getCTP().getPPr() == null || !para.getCTP().getPPr().isSetPStyle()
                ? null
                : para.getCTP().getPPr().getPStyle().getVal();
    }

    private static CTStyle styleById(XWPFDocument document, String id) {
        return document.getStyles().getCtStyles().getStyleList().stream()
                .filter(style -> id.equals(style.getStyleId()))
                .findFirst()
                .orElse(null);
    }

    private static List<String> headingIds(XWPFDocument document) {
        return document.getStyles().getCtStyles().getStyleList().stream()
                .map(CTStyle::getStyleId)
                .filter(id -> id != null && id.startsWith("Heading"))
                .toList();
    }

    private static XWPFDocument exported(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 600, 20, content);
    }
}
