package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The export names the document's own body text as Word's Normal style, and stops every
 * run from restating it.
 *
 * <p>A direct run property beats a style, so an export where every run carries its own
 * size and font accepts "change the Normal style" and then changes nothing — measured in
 * Word before this landed. Writing a styles part is only half of it; the other half is
 * leaving the runs that agree with it silent.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxDocumentStyleTest {

    private static final DocumentTextStyle BODY = DocumentTextStyle.builder()
            .fontName(FontName.HELVETICA).size(10.5).color(DocumentColor.rgb(24, 28, 38)).build();
    private static final DocumentTextStyle HEADING = DocumentTextStyle.builder()
            .fontName(FontName.HELVETICA_BOLD).size(18).color(DocumentColor.rgb(24, 28, 38)).build();

    private static final String LONG_BODY =
            "A body paragraph long enough that its characters outweigh the headings around "
            + "it, which is how the document default is chosen.";

    @Test
    void documentShouldCarryAStylesPartNamingItsOwnBodyText() throws Exception {
        try (XWPFDocument document = exported(page -> {
            page.addParagraph(p -> p.text("Heading").textStyle(HEADING));
            page.addParagraph(p -> p.text(LONG_BODY).textStyle(BODY));
        })) {
            assertThat(document.getStyles())
                    .as("a styles part exists at all")
                    .isNotNull();
            CTRPr defaults = document.getStyles().getCtStyles().getDocDefaults()
                    .getRPrDefault().getRPr();
            assertThat(defaults.getRFontsArray(0).getAscii()).isEqualTo("Helvetica");
            // w:sz counts half-points, so 10.5pt is 21.
            assertThat(defaults.getSzArray(0).getVal().toString()).isEqualTo("21");
        }
    }

    @Test
    void aRunThatOnlyRestatesTheStyleShouldSayNothing() throws Exception {
        try (XWPFDocument document = exported(page -> {
            page.addParagraph(p -> p.text("Heading").textStyle(HEADING));
            page.addParagraph(p -> p.text(LONG_BODY).textStyle(BODY));
        })) {
            CTRPr body = runProperties(document, LONG_BODY);
            assertThat(body == null || body.sizeOfSzArray() == 0)
                    .as("the body run leaves its size to Normal")
                    .isTrue();
            assertThat(body == null || body.sizeOfRFontsArray() == 0)
                    .as("and its font too")
                    .isTrue();
        }
    }

    @Test
    void aRunThatDiffersShouldKeepSayingSo() throws Exception {
        try (XWPFDocument document = exported(page -> {
            page.addParagraph(p -> p.text("Heading").textStyle(HEADING));
            page.addParagraph(p -> p.text(LONG_BODY).textStyle(BODY));
        })) {
            CTRPr heading = runProperties(document, "Heading");
            assertThat(heading).isNotNull();
            // 18pt in half-points. A heading must not be swallowed by the body style.
            assertThat(heading.getSzArray(0).getVal().toString()).isEqualTo("36");
            // The font is where it stops differing. This heading names the face
            // Helvetica-Bold and the body names Helvetica, but a face resolves to its
            // family and takes its weight from the decoration — neither of these carries
            // one, so the page draws both in Helvetica regular at different sizes. Writing
            // the face name made the run look different in the file while being identical
            // on the page, and sent Word looking for a family it does not have.
            assertThat(heading.sizeOfRFontsArray())
                    .as("the same family as the body, so the Normal style already says it")
                    .isZero();
        }
    }

    @Test
    void theDefaultShouldBeChosenByCharactersNotByParagraphCount() throws Exception {
        // Four short headings against one long body paragraph. Counting paragraphs elects
        // the heading style and leaves every body run carrying a direct size; counting
        // characters elects the body, which is what a reader means by "the body style".
        try (XWPFDocument document = exported(page -> {
            for (int i = 0; i < 4; i++) {
                page.addParagraph(p -> p.text("H").textStyle(HEADING));
            }
            page.addParagraph(p -> p.text(LONG_BODY).textStyle(BODY));
        })) {
            CTRPr defaults = document.getStyles().getCtStyles().getDocDefaults()
                    .getRPrDefault().getRPr();
            assertThat(defaults.getSzArray(0).getVal().toString())
                    .as("the long body paragraph outweighs four short headings")
                    .isEqualTo("21");
        }
    }

    @Test
    void stylesThatOnlyDifferByColourIdentityShouldWeighAsOne() throws Exception {
        // DocumentColor has no value equality and DocumentTextStyle is a record, so two
        // styles built the same way with separately-constructed colours are unequal.
        // Building the style inline per paragraph is ordinary authoring, and if each one
        // becomes its own weight the body's characters never add up: here six body
        // paragraphs of ten characters would weigh ten each, losing to three headings
        // sharing one instance, and the document default would come out the heading's.
        try (XWPFDocument document = exported(page -> {
            for (int i = 0; i < 3; i++) {
                page.addParagraph(p -> p.text("Heading of twenty ch").textStyle(HEADING));
            }
            for (int i = 0; i < 6; i++) {
                page.addParagraph(p -> p.text("Body line.").textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .size(10.5)
                        .color(DocumentColor.rgb(24, 28, 38))
                        .build()));
            }
        })) {
            CTRPr defaults = document.getStyles().getCtStyles().getDocDefaults()
                    .getRPrDefault().getRPr();
            assertThat(defaults.getSzArray(0).getVal().toString())
                    .as("sixty characters of body outweigh sixty of heading only if the "
                        + "body's styles weigh as one")
                    .isEqualTo("21");
        }
    }

    @Test
    void theStyleShouldNameTheFamilyForEveryCharacterRangeNotJustAscii() throws Exception {
        // A run suppressed in favour of Normal carries no rFonts at all, and w:ascii only
        // covers ASCII: High-ANSI letters read w:hAnsi, Hebrew and Arabic read w:cs, CJK
        // reads w:eastAsia. Naming one slot would send every accented letter and every
        // complex script to Word's theme font while the rest of the line stayed correct.
        try (XWPFDocument document = exported(page ->
                page.addParagraph(p -> p.text(LONG_BODY).textStyle(BODY)))) {

            var fonts = document.getStyles().getCtStyles().getDocDefaults()
                    .getRPrDefault().getRPr().getRFontsArray(0);
            assertThat(fonts.getAscii()).isEqualTo("Helvetica");
            assertThat(fonts.getHAnsi()).as("High-ANSI, e.g. é").isEqualTo("Helvetica");
            assertThat(fonts.getCs()).as("complex script, e.g. Hebrew").isEqualTo("Helvetica");
            assertThat(fonts.getEastAsia()).as("CJK").isEqualTo("Helvetica");
        }
    }

    @Test
    void aDocumentWithoutTextShouldNotInventAStyle() throws Exception {
        try (XWPFDocument document = exported(page -> page.spacer(10, 10))) {
            assertThat(document.getStyles()).as("the part holds the paragraph defaults").isNotNull();
            var styles = document.getStyles().getCtStyles();
            assertThat(styles.getStyleList())
                    .as("nothing to take a default from, so no Normal is written")
                    .isEmpty();
            assertThat(styles.getDocDefaults().isSetRPrDefault())
                    .as("and no default face")
                    .isFalse();
        }
    }

    @Test
    void aParagraphWithoutSpaceOfItsOwnShouldHaveNoneInWord() throws Exception {
        // Word fills what a document leaves unsaid from its new-document template: 8pt after
        // every paragraph, lines 1.08 tall. The page has neither, so the defaults say so.
        try (XWPFDocument document = exported(page -> page.addParagraph(p -> p.text(LONG_BODY).textStyle(BODY)))) {
            assertNoSpaceAfterAndSingleLines(document);
        }
    }

    @Test
    void aDocumentWithoutTextShouldStillHaveNoSpaceAfterItsParagraphs() throws Exception {
        // Spacers and tables still leave empty paragraphs behind, and Word would give each 8pt.
        try (XWPFDocument document = exported(page -> page.spacer(10, 10))) {
            assertNoSpaceAfterAndSingleLines(document);
        }
    }

    private static void assertNoSpaceAfterAndSingleLines(XWPFDocument document) {
        assertThat(document.getStyles()).as("a styles part exists").isNotNull();
        var defaults = document.getStyles().getCtStyles().getDocDefaults();
        assertThat(defaults != null && defaults.isSetPPrDefault())
                .as("the paragraph defaults are stated, not left to Word's template")
                .isTrue();
        CTSpacing spacing = defaults.getPPrDefault().getPPr().getSpacing();
        assertThat(DocxTwips.of(spacing.getAfter())).as("no space after").isZero();
        assertThat(DocxTwips.of(spacing.getLine())).as("single lines").isEqualTo(240);
        assertThat(spacing.getLineRule()).isEqualTo(STLineSpacingRule.AUTO);
    }

    private static CTRPr runProperties(XWPFDocument document, String text) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            if (paragraph.getText().contains(text)) {
                XWPFRun run = paragraph.getRuns().get(0);
                return run.getCTR().getRPr();
            }
        }
        throw new AssertionError("no paragraph containing: " + text);
    }

    private static XWPFDocument exported(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content) throws Exception {
        byte[] docx;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            docx = session.export(new DocxSemanticBackend());
        }
        return new XWPFDocument(new ByteArrayInputStream(docx));
    }
}
