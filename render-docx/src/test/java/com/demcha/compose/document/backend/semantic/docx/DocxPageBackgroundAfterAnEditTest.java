package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageBackgroundFill;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The pages an editor adds past a one-page section are painted by the header it already has.
 *
 * <p>A section of one page drew its page backgrounds from the body, and a DOCX is a document
 * people edit: Word sets it on a line more than the export did, or a reader types. The page that
 * appeared then had no sidebar, and a CV whose column carries white text showed a white page with
 * invisible writing on it. The fills go into the section's header instead, so every page the
 * section has takes them, the editor's included.</p>
 *
 * <p><strong>The limit of what these tests check.</strong> Page two is the editor's: it exists
 * only once Word or LibreOffice lays the grown document out, and CI runs neither. So these tests
 * assert the record a page is painted from — the fills in the header of the one section the
 * document has, nothing of them in the body, and no page of that section taking a header without
 * them — and not a picture of page two. The picture is not in the tree at all: it was taken in
 * Word 16.0.20527 on a throwaway probe of a one-page sidebar export grown this way, in the change
 * that moved the fills into the header (#987), where page two held the appended text and was white
 * at every point sampled down its left column before, and carries the column on pages two and
 * three after. That reading is checkable only in that change's own record, so a reader asking
 * whether Word paints the page has to go there; what is checkable here is the writing Word paints
 * it from.</p>
 *
 * <p>{@link DocxPageBackgroundTest} holds what a fresh export writes. This holds what survives an
 * edit, which is where the defect showed, so it reads the document back after an editor has
 * written to it.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPageBackgroundAfterAnEditTest {

    private static final DocumentColor CHARCOAL = DocumentColor.rgb(0x2B, 0x2F, 0x36);

    private static final DocumentTextStyle WHITE = DocumentTextStyle.builder()
            .color(DocumentColor.rgb(0xFF, 0xFF, 0xFF))
            .build();

    private static final double PAGE_WIDTH = 400;

    private static final double PAGE_HEIGHT = 600;

    /**
     * How many paragraphs the editor appends: enough that the grown document is longer than the
     * section's one page, which
     * {@link #theSameParagraphsAppendedPastThePageTakeMoreThanOnePageToSet} measures with the
     * engine's own lines. The editor sets its lines itself, so that measurement does not settle
     * what Word does with them; the reading that does is #987's, where Word put the appended text
     * on pages two and three.
     */
    private static final int APPENDED = 80;

    @Test
    void theFixtureIsASectionOfOnePageWithNoChromeOfItsOwn() throws Exception {
        // The configuration the defect needs, and the one that used to take the body route: one
        // page of content, and no zone of the document's own in a header or a footer. Read from
        // the written file, so a zone added to the fixture shows here as the footer part, the
        // paragraph text or the table it writes.
        try (DocumentSession session = sidebarSession()) {
            assertThat(session.pageIndex().totalPages()).as("a section of one page").isEqualTo(1);

            try (XWPFDocument document = new XWPFDocument(
                    new ByteArrayInputStream(session.export(new DocxSemanticBackend())))) {
                assertThat(document.getFooterList()).as("no footer of the document's own").isEmpty();
                XWPFHeader header = onlyHeaderOf(document);
                assertThat(header.getText().trim())
                        .as("the one header is the export's own, holding no text of the document")
                        .isEmpty();
                assertThat(header.getTables())
                        .as("nor a table of it, which its text would not show")
                        .isEmpty();
            }
        }
    }

    @Test
    void thePagesAnEditorAddsTakeTheHeaderThatCarriesTheFills() throws Exception {
        try (XWPFDocument grown = grownByAnEditor(sidebar())) {
            CTSectPr section = grown.getDocument().getBody().getSectPr();
            String body = grown.getDocument().getBody().xmlText();

            assertThat(occurrencesOf(body, "<w:sectPr"))
                    .as("one section, so every page of the grown document is its page")
                    .isEqualTo(1);
            assertThat(body).as("nothing of the fills in the body an added page does not hold")
                    .doesNotContain("Page background");
            assertThat(appendedParagraphsOf(grown))
                    .as("the editor's paragraphs are in the document that was read back")
                    .isEqualTo(APPENDED);

            // Which header a page of the section takes: the first page's where the section states
            // one, the even pages' where the document states different even and odd pages, the
            // default otherwise. This section states neither, so every one of its pages takes the
            // default header, and the fills are in it. DocxPageBackgroundTest holds the sections
            // that do state them, each of their headers carrying the fills too.
            assertThat(section.isSetTitlePg())
                    .as("no first page of its own for page one to take a header of")
                    .isFalse();
            assertThat(DocxSettings.evenAndOddPages(grown))
                    .as("no even pages of their own")
                    .isFalse();
            String header = onlyHeaderOf(grown)._getHdrFtr().xmlText();

            assertThat(header)
                    .as("behind the text, placed from the page, and not the body's to move")
                    .contains("name=\"Page background 1\"")
                    .contains("val=\"2B2F36\"")
                    .contains("behindDoc=\"1\"")
                    .contains("relativeFrom=\"page\"")
                    .contains("locked=\"1\"")
                    .as("the column the page paints, not a fill of no extent")
                    .contains("cx=\"" + Units.toEMU(0.35 * PAGE_WIDTH) + "\"")
                    .contains("cy=\"" + Units.toEMU(PAGE_HEIGHT) + "\"");
            assertThat(occurrencesOf(header, "<w:drawing"))
                    .as("one drawing for the one fill")
                    .isEqualTo(1);
        }
    }

    @Test
    void theSameParagraphsAppendedPastThePageTakeMoreThanOnePageToSet() throws Exception {
        // Why eighty: the paragraphs the editor appends are more than the section's page holds.
        // Measured with the engine's own lines, which is what this repository can measure; the
        // editor sets its lines itself, and taller here, so it needs at least as many pages.
        try (DocumentSession grown = session(page -> {
            sidebarContent(page);
            for (int line = 1; line <= APPENDED; line++) {
                page.addParagraph(appendedText(line));
            }
        })) {
            assertThat(grown.pageIndex().totalPages())
                    .as("the section's one page does not hold them")
                    .isGreaterThan(1);
        }
    }

    /** A page of its own: a dark left column from the backgrounds, white text standing on it. */
    private static byte[] sidebar() throws Exception {
        try (DocumentSession session = sidebarSession()) {
            return session.export(new DocxSemanticBackend());
        }
    }

    private static DocumentSession sidebarSession() {
        return session(DocxPageBackgroundAfterAnEditTest::sidebarContent);
    }

    private static DocumentSession session(Consumer<PageFlowBuilder> content) {
        DocumentSession session = GraphCompose.document()
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .margin(DocumentInsets.of(24))
                .create();
        try {
            session.pageBackgrounds(List.of(PageBackgroundFill.leftColumn(0.35, CHARCOAL)));
            session.pageFlow(content);
        } catch (RuntimeException | Error failure) {
            session.close();
            throw failure;
        }
        return session;
    }

    private static void sidebarContent(PageFlowBuilder page) {
        page.addParagraph("SIDEBAR", WHITE);
        page.addParagraph("white on the column", WHITE);
        page.addParagraph("Body line.");
    }

    /** What an editor does: paragraphs appended past the section's one page, and written back. */
    private static XWPFDocument grownByAnEditor(byte[] docx) throws Exception {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            for (int line = 1; line <= APPENDED; line++) {
                document.createParagraph().createRun().setText(appendedText(line));
            }
            document.write(sink);
        }
        return new XWPFDocument(new ByteArrayInputStream(sink.toByteArray()));
    }

    private static String appendedText(int line) {
        return "Appended line " + line;
    }

    private static long appendedParagraphsOf(XWPFDocument document) {
        return document.getParagraphs().stream()
                .map(XWPFParagraph::getText)
                .filter(text -> text != null && text.startsWith("Appended line "))
                .count();
    }

    /** The one header the document writes, which is then the one its pages take. */
    private static XWPFHeader onlyHeaderOf(XWPFDocument document) {
        assertThat(document.getHeaderList()).hasSize(1);
        return document.getHeaderList().get(0);
    }

    private static int occurrencesOf(String xml, String part) {
        return xml.split(Pattern.quote(part), -1).length - 1;
    }
}
