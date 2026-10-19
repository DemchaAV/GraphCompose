package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageNumberStyle;
import com.demcha.compose.document.output.DocumentPageNumbering;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a text header or footer reads or where it stands otherwise in Word than on the page is
 * named.
 *
 * <p>Measured in Word and LibreOffice on Windows: 9pt text in a header 11pt tall stood 2.14pt
 * lower in Word (2.10 in LibreOffice) than on the page, and in a footer 10pt tall 1.04pt higher
 * (1.21), its separator 0.92pt higher (1.06); page 28 numbered in letters read {@code bb} in Word
 * and {@code ab} on the page and in LibreOffice; a left slot reaching past the middle drawn under the
 * centre one on the page set the centre and right slots after it in both editors, the right one
 * past the right margin; a slot wider than the line both editors wrapped onto a line below.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxBandTextReportTest {

    @Test
    void aBandTooShallowForItsLineIsNamedWithHowFarItsTextMoves() throws Exception {
        List<DocxExportReport.Note> header = bandNotes(2, session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(11).fontSize(9).leftText("Head").build()));
        List<DocxExportReport.Note> footer = bandNotes(2, session -> session.footer(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.FOOTER).height(10).fontSize(9).leftText("Foot").build()));

        assertThat(header).extracting(DocxExportReport.Note::detail).containsExactly(
                "its text stands about 2.1pt lower than the page sets it: the page sets its baseline 6.5pt from "
                + "the top, too near for its 10.8pt line, which is written from the page's top edge");
        assertThat(header).allSatisfy(note -> assertThat(note.subject()).isEqualTo("page header"));
        assertThat(footer).extracting(DocxExportReport.Note::detail).containsExactly(
                "its text stands about 1.2pt higher than the page sets it: the page sets its baseline 1.0pt from "
                + "the foot, too near for its 10.8pt line, which is written up from the page's foot");
        // 0.14pt, measured 0.14 in Word: past the tenth of a point Word sets lines to.
        assertThat(details(bandNotes(2, session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(13).fontSize(9).leftText("Head").build()))))
                .singleElement().asString().startsWith("its text stands about 0.1pt lower");
    }

    @Test
    void aSeparatorRuledPastALineTallerThanItsBandIsNamedWithIt() throws Exception {
        // A 0.5pt separator over a 10.8pt line in a footer 10pt tall: ruled 10.8pt up and half its
        // width more, where the page strokes it 10pt up. Measured: 0.92pt higher in Word, 1.06 in
        // LibreOffice; in a header 11pt tall, over a line that fits, where the page strokes it.
        List<String> footer = details(bandNotes(2, session -> session.footer(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.FOOTER).height(10).fontSize(9).leftText("Foot").showSeparator(true)
                .build())));
        List<String> header = details(bandNotes(2, session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(11).fontSize(9).leftText("Head").showSeparator(true)
                .build())));

        assertThat(footer).singleElement().asString()
                .endsWith("which is written up from the page's foot; its separator, ruled over that line, stands "
                          + "about 1.0pt higher");
        assertThat(header).singleElement().asString().endsWith("which is written from the page's top edge");
    }

    @Test
    void aBandDeepEnoughForItsLineIsNotNamed() throws Exception {
        assertThat(bandNotes(2, session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(14).fontSize(9).leftText("Head").build()))).isEmpty();
        assertThat(bandNotes(2, session -> session.footer(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.FOOTER).height(12).fontSize(9).leftText("Foot").build()))).isEmpty();
    }

    @Test
    void pageNumbersInLettersAreNamedWhereTheLayoutRunsPastTheTwentySeventhPage() throws Exception {
        assertThat(details(bandNotes(28, lettered("Page {page}", DocumentPageNumberStyle.LOWER_ALPHA))))
                .singleElement().asString()
                .isEqualTo("its page numbers are letters, and Word letters a number past 26 by repeating one "
                           + "letter — 28 reads bb where the page reads ab — so a page number or a page total of "
                           + "28 or more reads otherwise than on the page; LibreOffice letters them as the page does");
        assertThat(details(bandNotes(28, lettered("of {pages}", DocumentPageNumberStyle.UPPER_ALPHA)))).hasSize(1);
        assertThat(bandNotes(27, lettered("Page {page}", DocumentPageNumberStyle.LOWER_ALPHA)))
                .as("aa, the 27th, reads alike").isEmpty();
        assertThat(bandNotes(28, lettered("Page {page}", DocumentPageNumberStyle.LOWER_ROMAN))).isEmpty();
        assertThat(bandNotes(28, lettered("Lettered", DocumentPageNumberStyle.LOWER_ALPHA)))
                .as("no number in it").isEmpty();
    }

    @Test
    void pageNumbersInLettersWithNoLayoutAreNamedWhereTheDocumentRunsThatFar() throws Exception {
        assertThat(details(bandNotesWithoutLayout(lettered("Page {page}", DocumentPageNumberStyle.LOWER_ALPHA))))
                .singleElement().asString()
                .contains("reads otherwise than on the page, where the document runs that far; LibreOffice");
    }

    @Test
    void slotsOverlappingWhereThePageSetsThemAreNamed() throws Exception {
        List<String> notes = details(bandNotes(2, session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(30).fontSize(9)
                .leftText("A rather long left slot that reaches past the middle")
                .centerText("CENTRE").rightText("RIGHT").build())));

        assertThat(notes).singleElement().asString()
                .isEqualTo("its slots' text overlaps where the page sets it, drawn over one another, on every page "
                           + "it is drawn on; Word and LibreOffice set a slot that would overlap the text before it "
                           + "after that text instead — Word on the same line, as far as past the right margin, "
                           + "LibreOffice on it or on a line below");
        assertThat(bandNotes(2, session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(30).fontSize(9)
                .leftText("Left").centerText("CENTRE").rightText("RIGHT").build()))).isEmpty();
        StringBuilder reaching = new StringBuilder("A left slot reaching the right one ");
        while (helvetica9(reaching.toString()) < 240 - helvetica9("RIGHT SLOT") + 4) {
            reaching.append('i');
        }
        String left = reaching.toString();
        assertThat(helvetica9(left)).isLessThan(240);
        assertThat(details(bandNotes(2, session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(30).fontSize(9)
                .leftText(left).rightText("RIGHT SLOT").build()))))
                .as("no centre slot, the left one reaching the right one")
                .singleElement().asString().startsWith("its slots' text overlaps where the page sets it");
    }

    @Test
    void aSlotWiderThanTheLineIsNamed() throws Exception {
        // 240pt between the margins; measured on a wider page, both editors wrapped such a slot.
        List<String> notes = details(bandNotes(2, session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(30).fontSize(9)
                .leftText("A left slot far wider than the line between the margins of this narrow page").build())));

        assertThat(notes).singleElement().asString()
                .isEqualTo("its text runs past the margins where the page sets it, on every page it is drawn on; "
                           + "Word and LibreOffice wrap it onto a line below");
    }

    @Test
    void slotsWithNoLayoutAreReadOnTheFirstPageTheBandIsDrawnOn() throws Exception {
        List<String> notes = details(bandNotesWithoutLayout(session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(30).fontSize(9)
                .leftText("A rather long left slot that reaches past the middle").centerText("CENTRE")
                .numbering(DocumentPageNumbering.builder().showOnFirstPage(false).build()).build())));

        assertThat(notes).singleElement().asString()
                .contains("drawn over one another, with the page numbers of the first page it is drawn on;");
    }

    @Test
    void slotsOverlappingOnSomePagesOnlyAreNamedWithHowMany() throws Exception {
        // The right slot grows a digit on the tenth page: a centre slot that ends between where a
        // one-digit and a two-digit right slot start overlaps it on pages 10 to 12 only.
        double width = 240;
        double nine = helvetica9("Page 9");
        double ten = helvetica9("Page 10");
        StringBuilder centre = new StringBuilder();
        while (helvetica9(centre.toString()) < width - nine - ten) {
            centre.append('i');
        }
        String centred = centre.toString();
        assertThat(helvetica9(centred)).isGreaterThan(width - 2 * ten + 1).isLessThan(width - 2 * nine - 1);

        List<String> notes = details(bandNotes(12, session -> session.footer(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.FOOTER).height(30).fontSize(9)
                .centerText(centred).rightText("Page {page}").build())));
        List<String> offTheFirst = details(bandNotes(12, session -> session.footer(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.FOOTER).height(30).fontSize(9)
                .centerText(centred).rightText("Page {page}")
                .numbering(DocumentPageNumbering.builder().showOnFirstPage(false).build()).build())));

        assertThat(notes).singleElement().asString().contains("drawn over one another, on 3 of the 12 pages it is drawn on;");
        assertThat(offTheFirst).singleElement().asString()
                .as("counted over the pages it is drawn on, the first not among them")
                .contains("on 3 of the 11 pages it is drawn on;");
    }

    private static Consumer<DocumentSession> lettered(String text, DocumentPageNumberStyle style) {
        return session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(30).leftText(text)
                .numbering(DocumentPageNumbering.builder().style(style).build()).build());
    }

    private static double helvetica9(String text) throws Exception {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA).getStringWidth(text) / 1000 * 9;
    }

    private static List<String> details(List<DocxExportReport.Note> notes) {
        return notes.stream().map(DocxExportReport.Note::detail).toList();
    }

    /** The band notes of a document so many pages long, laid out, with the layout behind it. */
    private static List<DocxExportReport.Note> bandNotes(int pages, Consumer<DocumentSession> chrome) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = session(pages, chrome)) {
            session.export(new DocxSemanticBackend(report::set));
        }
        assertThat(report.get().notes()).noneMatch(note -> note.subject().equals("measured geometry"));
        return report.get().notes().stream().filter(note -> note.subject().startsWith("page ")).toList();
    }

    /** The band notes of a one-page document exported with no layout behind it. */
    private static List<DocxExportReport.Note> bandNotesWithoutLayout(Consumer<DocumentSession> chrome) throws Exception {
        AtomicReference<DocumentGraph> graph = new AtomicReference<>();
        AtomicReference<SemanticExportContext> context = new AtomicReference<>();
        try (DocumentSession session = session(1, chrome)) {
            session.export(new SemanticBackend<byte[]>() {
                @Override
                public String name() {
                    return "capture";
                }

                @Override
                public byte[] export(DocumentGraph documentGraph, SemanticExportContext exportContext) {
                    graph.set(documentGraph);
                    context.set(exportContext);
                    return new byte[0];
                }
            });
        }
        SemanticExportContext laidOut = context.get();
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        new DocxSemanticBackend(report::set).export(graph.get(), new SemanticExportContext(laidOut.canvas(),
                laidOut.customFontFamilies(), laidOut.outputFile(), laidOut.outputOptions(), null));
        assertThat(report.get().notes()).anyMatch(note -> note.subject().equals("measured geometry"));
        return report.get().notes().stream().filter(note -> note.subject().startsWith("page ")).toList();
    }

    private static DocumentSession session(int pages, Consumer<DocumentSession> chrome) {
        DocumentSession session = GraphCompose.document().pageSize(300, 200).margin(DocumentInsets.of(30)).create();
        chrome.accept(session);
        session.pageFlow(page -> {
            page.addParagraph(p -> p.text("Page 1"));
            for (int next = 2; next <= pages; next++) {
                int number = next;
                page.addPageBreak(pageBreak -> { });
                page.addParagraph(p -> p.text("Page " + number));
            }
        });
        return session;
    }
}
