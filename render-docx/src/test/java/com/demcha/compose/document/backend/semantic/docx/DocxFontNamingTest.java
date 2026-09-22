package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A run names a family Word has, not a face it does not.
 *
 * <p>{@code Helvetica-Bold} is a face, and the export wrote it where Word expects a family.
 * Word resolves a family and takes the weight from {@code w:b}; asked for a family by that
 * name it finds none and substitutes, which is how a document that named its headings by
 * face came out set in something else.</p>
 *
 * <p>The weight is deliberately not read from the face name. The engine does not read it
 * either — {@code FontLibrary.resolveFamily} rewrites the face to its family and the face
 * is chosen from the style's decoration — so a style naming {@code Helvetica-Bold} and
 * setting no decoration lays the page out in Helvetica regular. Writing {@code w:b} here
 * would make Word bolder than the page it is meant to match.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxFontNamingTest {

    /** Long enough to be the body the Normal style is chosen from, by character weight. */
    private static final String LONG_BODY =
            "A body paragraph long enough that the style it is set in is the one the "
            + "document is mostly written in, so a heading beside it has to say what "
            + "differs about itself.";

    @Test
    void aFaceNameIsWrittenAsItsFamily() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text(LONG_BODY).textStyle(style(FontName.LATO)))
                .addParagraph(p -> p.text("Heading").textStyle(style(FontName.HELVETICA_BOLD))))) {

            XWPFRun heading = runOf(document, "Heading");
            assertThat(heading.getFontFamily())
                    .as("the family, not the face")
                    .isEqualTo("Helvetica");
        }
    }

    @Test
    void theFaceNameDoesNotMakeTheRunBold() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text(LONG_BODY).textStyle(style(FontName.LATO)))
                .addParagraph(p -> p.text("Heading").textStyle(style(FontName.HELVETICA_BOLD))))) {

            assertThat(runOf(document, "Heading").isBold())
                    .as("the page draws this regular, so the file must not say bold")
                    .isFalse();
        }
    }

    @Test
    void theDecorationStillDecidesTheWeight() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text(LONG_BODY).textStyle(style(FontName.LATO)))
                .addParagraph(p -> p.text("Strong").textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .decoration(DocumentTextDecoration.BOLD)
                        .size(13)
                        .build())))) {

            assertThat(runOf(document, "Strong").isBold())
                    .as("this one asked for bold, and the page draws it bold")
                    .isTrue();
        }
    }

    @Test
    void twoNamesForOneFamilyCountAsOneStyle() throws Exception {
        // Helvetica and Helvetica-Bold are written identically, so weighing them apart
        // would split one body style in two and could elect the lighter half as Normal.
        try (XWPFDocument document = exported(page -> {
            page.addParagraph(p -> p.text("A long stretch of body text set one way")
                    .textStyle(style(FontName.HELVETICA)));
            page.addParagraph(p -> p.text("A long stretch of body text set the other")
                    .textStyle(style(FontName.HELVETICA_BOLD)));
        })) {
            assertThat(document.getStyles().getCtStyles().getDocDefaults()
                    .getRPrDefault().getRPr().getRFontsArray(0).getAscii())
                    .isEqualTo("Helvetica");
            for (String text : new String[] {"A long stretch of body text set one way",
                                             "A long stretch of body text set the other"}) {
                assertThat(runOf(document, text).getCTR().getRPr() == null
                           || runOf(document, text).getCTR().getRPr().sizeOfRFontsArray() == 0)
                        .as("neither run repeats a family the Normal style already carries: %s", text)
                        .isTrue();
            }
        }
    }

    private static DocumentTextStyle style(FontName fontName) {
        return DocumentTextStyle.builder().fontName(fontName).size(13).build();
    }

    private static XWPFRun runOf(XWPFDocument document, String text) {
        return document.getParagraphs().stream()
                .filter(paragraph -> text.equals(paragraph.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no paragraph reading " + text))
                .getRuns().get(0);
    }

    private static XWPFDocument exported(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 600, 20, content);
    }
}
