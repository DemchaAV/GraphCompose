package com.demcha.compose.document.backend;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A page zone exported to Word becomes a real footer part, not a picture of one.
 *
 * <p>The semantic lane cannot take the fixed-layout answer: Word paginates the
 * document, so a page number written into the footer as text would be right on
 * one page and wrong on the rest. The zone's content therefore places
 * {@code pageNumber()}, which is resolved text on a PDF and a live {@code PAGE}
 * field here — the same lambda, exported honestly to two formats that learn the
 * number at different moments.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPageZoneTest {

    @Test
    void aZoneBecomesAWordFooterWithLiveFields() throws Exception {
        byte[] docx = exportWithFooter(page -> new RowBuilder()
                .name("FooterLine")
                .addParagraph(paragraph -> paragraph.name("Note").text("Confidential"))
                .flexSpacer()
                .addParagraph(paragraph -> paragraph.name("Label").text("Page "))
                .add(page.pageNumber())
                .addParagraph(paragraph -> paragraph.name("Of").text(" of "))
                .add(page.pageTotal())
                .build());

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getFooterList())
                    .as("the zone lands in a footer part Word owns, not in the body")
                    .isNotEmpty();

            XWPFFooter footer = document.getFooterList().get(0);
            assertThat(footer.getText()).contains("Confidential");

            assertThat(footer._getHdrFtr().xmlText())
                    .as("the page number is Word's own field, so it stays correct when the"
                            + " reader edits the document")
                    .contains("PAGE")
                    .contains("NUMPAGES");
        }
    }

    @Test
    void readingAPageNumberAsAnIntIsRefusedOnASemanticExport() {
        assertThatThrownBy(() -> exportWithFooter(page -> new RowBuilder()
                .name("FooterLine")
                .addParagraph(paragraph -> paragraph
                        .name("Counter")
                        .text("Page " + page.number() + " of " + page.total()))
                .build()))
                .as("baking the number into text would be wrong on every page but one,"
                        + " so the context refuses instead of returning a plausible lie")
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("pageNumber()");
    }

    /**
     * The headline claim, asserted rather than described: one zone definition,
     * two formats, each answering the page-number question the way it can.
     */
    @Test
    void oneZoneDefinitionServesBothLanes() throws Exception {
        java.util.function.Function<com.demcha.compose.document.output.PageContext,
                com.demcha.compose.document.node.DocumentNode> footer = page -> new RowBuilder()
                .name("Shared")
                .addParagraph(paragraph -> paragraph.name("Note").text("Confidential"))
                .flexSpacer()
                .add(page.pageNumber())
                .build();

        byte[] pdf;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(420, 300)
                .margin(DocumentInsets.of(28))
                .create()) {
            document.chrome().zone(DocumentPageZone.footer(32, footer));
            document.dsl().pageFlow()
                    .name("SharedFixture")
                    .addParagraph(paragraph -> paragraph.name("Body").text("Body copy."))
                    .build();
            pdf = document.toPdfBytes();
        }

        try (org.apache.pdfbox.pdmodel.PDDocument document =
                     org.apache.pdfbox.Loader.loadPDF(pdf)) {
            assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(document))
                    .as("the fixed-layout lane knows the page, so it draws the number")
                    .contains("Confidential")
                    .contains("1");
        }

        byte[] docx = exportWithFooter(footer);
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getFooterList()).isNotEmpty();
            assertThat(document.getFooterList().get(0)._getHdrFtr().xmlText())
                    .as("the semantic lane does not, so it asks Word for it")
                    .contains("PAGE");
        }
    }

    /**
     * {@code page.pageNumber(textStyle)} takes a style on purpose, and the
     * fixed-layout lane honors it — so the Word lane has to as well, or "one
     * zone definition serves both lanes" is only mostly true. The placeholder
     * run carries the style, and Word keeps a field result's formatting when it
     * repaints the field.
     */
    @Test
    void aPageFieldCarriesItsTextStyleIntoTheWordRun() throws Exception {
        com.demcha.compose.document.style.DocumentTextStyle style =
                com.demcha.compose.document.style.DocumentTextStyle.builder()
                        .size(9)
                        .decoration(com.demcha.compose.document.style.DocumentTextDecoration.BOLD)
                        .build();
        byte[] docx = exportWithFooter(page -> new RowBuilder()
                .name("StyledField")
                .add(page.pageNumber(style))
                .build());

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            XWPFRun fieldRun = document.getFooterList().get(0).getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .filter(run -> "1".equals(run.text()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no placeholder run found in the footer"));
            assertThat(fieldRun.getFontSizeAsDouble())
                    .as("the 9pt the API took reaches the Word run")
                    .isEqualTo(9.0);
            assertThat(fieldRun.isBold())
                    .as("and so does the weight")
                    .isTrue();
        }
    }

    /**
     * A page predicate is a fixed-layout capability: Word paginates the
     * document, so there is no page to test it against when the zone is
     * written. The worst answer would be dropping the predicate silently; the
     * export keeps the zone on every page — content beats absence — and says
     * what it could not honor.
     */
    @Test
    void aPagePredicateCannotBeEvaluatedSoTheZoneLandsEverywhereAndSaysSo() throws Exception {
        ch.qos.logback.classic.Logger backendLog = (ch.qos.logback.classic.Logger)
                org.slf4j.LoggerFactory.getLogger(DocxSemanticBackend.class);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> seen =
                new ch.qos.logback.core.read.ListAppender<>();
        seen.start();
        backendLog.addAppender(seen);
        try {
            byte[] docx = export(DocumentPageZone.builder()
                    .height(32)
                    .appliesTo(page -> !page.isFirst())
                    .content(page -> new RowBuilder()
                            .name("Conditional")
                            .addParagraph(paragraph -> paragraph.name("Note").text("Confidential"))
                            .build())
                    .build());

            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
                assertThat(document.getFooterList())
                        .as("the zone is still exported — omitting it would lose content")
                        .isNotEmpty();
                assertThat(document.getFooterList().get(0).getText()).contains("Confidential");
            }
            assertThat(seen.list)
                    .as("and the export names the predicate it could not evaluate")
                    .anySatisfy(event -> {
                        assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.WARN);
                        assertThat(event.getFormattedMessage()).contains("appliesTo");
                    });
        } finally {
            backendLog.detachAppender(seen);
        }
    }

    private static byte[] exportWithFooter(
            java.util.function.Function<com.demcha.compose.document.output.PageContext,
                    com.demcha.compose.document.node.DocumentNode> content) throws Exception {
        return export(DocumentPageZone.footer(32, content));
    }

    private static byte[] export(DocumentPageZone zone) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(420, 300)
                .margin(DocumentInsets.of(28))
                .create()) {

            document.chrome().zone(zone);
            document.dsl().pageFlow()
                    .name("DocxZoneFixture")
                    .addParagraph(paragraph -> paragraph.name("Body").text("Body copy."))
                    .build();

            return document.export(new DocxSemanticBackend());
        }
    }
}
