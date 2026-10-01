package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageNumberStyle;
import com.demcha.compose.document.output.DocumentPageNumbering;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A text header or footer — the three slots and their page tokens — is a Word header or footer
 * with live page fields, each line where the page sets it.
 *
 * @author Artem Demchyshyn
 */
class DocxTextBandTest {

    @Test
    void aFooterIsWordsFooterWithItsSlotsAndLivePageNumbers() throws Exception {
        try (XWPFDocument document = export(null, session -> session.footer(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.FOOTER).height(28).fontSize(9)
                .leftText("Thank you").rightText("Page {page} of {pages}")
                .build()))) {
            XWPFFooter footer = only(document.getFooterList());
            String xml = footer._getHdrFtr().xmlText();

            assertThat(footer.getText()).contains("Thank you").contains("Page ").contains(" of ");
            assertThat(xml).containsPattern("<w:instrText[^>]*> PAGE </w:instrText>")
                    .containsPattern("<w:instrText[^>]*> NUMPAGES </w:instrText>")
                    .as("the right slot stands at a right tab against the right margin")
                    .contains("w:val=\"right\"");
            assertThat(xml).as("the total reads the layout's count before Word updates it")
                    .containsPattern("NUMPAGES </w:instrText>.*?<w:t>2</w:t>");
        }
    }

    @Test
    void aHeaderSetsItsCentreSlotAtACentreTabAndItsSeparatorBelowIt() throws Exception {
        try (XWPFDocument document = export(null, session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(30).fontSize(10)
                .leftText("Left").centerText("Middle").rightText("Right")
                .showSeparator(true).separatorColor(DocumentColor.rgb(20, 40, 90)).separatorThickness(1)
                .build()))) {
            XWPFHeader header = only(document.getHeaderList());
            String xml = header._getHdrFtr().xmlText();

            assertThat(header.getText()).contains("Left").contains("Middle").contains("Right");
            assertThat(xml).contains("w:val=\"center\"").contains("w:val=\"right\"");
            assertThat(xml).containsPattern("<w:bottom w:val=\"single\" w:sz=\"8\" w:color=\"14285A\"");
            CTSectPr section = document.getDocument().getBody().getSectPr();
            assertThat(section.getPgMar().getHeader())
                    .as("as far from the top as the page sets the text")
                    .isEqualTo(BigInteger.valueOf(Math.round(DocxTextBands.distanceFromEdge(DocumentHeaderFooter
                            .builder().zone(DocumentHeaderFooterZone.HEADER).height(30).fontSize(10).build()) * 20)));
        }
    }

    @Test
    void aFooterKeptOffTheFirstPageLeavesTheTitlePagesFooterEmpty() throws Exception {
        try (XWPFDocument document = export(null, session -> session.footer(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.FOOTER).leftText("Continued")
                .numbering(DocumentPageNumbering.builder().showOnFirstPage(false).build())
                .build()))) {
            CTSectPr section = document.getDocument().getBody().getSectPr();

            assertThat(section.isSetTitlePg()).isTrue();
            assertThat(document.getFooterList()).hasSize(2)
                    .anyMatch(footer -> footer.getText().contains("Continued"))
                    .anyMatch(footer -> footer.getText().isBlank());
        }
    }

    @Test
    void aBandAndAPageZoneOfOneKindShareWordsOneFooterTheBandFramed() throws Exception {
        try (XWPFDocument document = export(null, session -> {
            session.footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER)
                    .leftText("Band").build());
            session.chrome().zone(DocumentPageZone.footer(20,
                    page -> new RowBuilder().addParagraph(p -> p.text("Zone")).build()));
        })) {
            XWPFFooter footer = only(document.getFooterList());
            List<org.apache.poi.xwpf.usermodel.XWPFParagraph> paragraphs = footer.getParagraphs();

            assertThat(footer.getText()).contains("Band").contains("Zone");
            assertThat(paragraphs).filteredOn(p -> p.getText().contains("Band"))
                    .as("the band stands at its own height, beside the zone's line")
                    .allMatch(p -> p.getCTP().getPPr().isSetFramePr());
            assertThat(paragraphs).filteredOn(p -> p.getText().contains("Zone"))
                    .as("the zone's line flows, placed by the zone's distance")
                    .noneMatch(p -> p.getCTP().isSetPPr() && p.getCTP().getPPr().isSetFramePr());
        }
    }

    @Test
    void twoBandsAtOneHeightAreTwoFramesNotOne() throws Exception {
        // Word takes adjacent paragraphs with the same frame for one frame, a line tall.
        try (XWPFDocument document = export(null, session -> {
            session.footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER)
                    .leftText("Confidential").build());
            session.footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER)
                    .rightText("{page}").build());
        })) {
            List<org.apache.poi.xwpf.usermodel.XWPFParagraph> paragraphs = only(document.getFooterList()).getParagraphs();
            List<Boolean> framed = paragraphs.stream()
                    .map(p -> p.getCTP().isSetPPr() && p.getCTP().getPPr().isSetFramePr())
                    .toList();

            assertThat(framed).as("a hairline between the two, and one closing the footer's flow")
                    .containsExactly(true, false, true, false);
        }
    }

    @Test
    void pageBackgroundsAreCarriedByTheHeadersFlowNotByAFrame() throws Exception {
        try (XWPFDocument document = export(null, session -> {
            session.pageBackground(DocumentColor.rgb(250, 250, 250));
            session.header(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.HEADER).leftText("One").build());
            session.header(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.HEADER).height(18)
                    .rightText("Two").build());
        })) {
            XWPFHeader header = only(document.getHeaderList());

            assertThat(header.getParagraphs()).filteredOn(p -> p.getCTP().xmlText().contains("<wp:anchor"))
                    .singleElement()
                    .matches(p -> !p.getCTP().isSetPPr() || !p.getCTP().getPPr().isSetFramePr());
        }
    }

    @Test
    void aPageNumberKeepsItsBandsColourWhenTheEditorRepaintsIt() throws Exception {
        // A simple field's result is repainted without its run's style: a white number on a dark
        // band came out in the document's ink. Every run of a complex field carries the style.
        try (XWPFDocument document = export(null, session -> session.footer(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.FOOTER).textColor(DocumentColor.WHITE)
                .rightText("Page {page} of {pages}")
                .build()))) {
            String xml = only(document.getFooterList())._getHdrFtr().xmlText();

            assertThat(xml).doesNotContain("fldSimple");
            java.util.regex.Matcher runs = java.util.regex.Pattern.compile("<w:r>(.*?)</w:r>").matcher(xml);
            int fieldRuns = 0;
            while (runs.find()) {
                if (runs.group(1).contains("fldChar") || runs.group(1).contains("instrText")) {
                    fieldRuns++;
                    assertThat(runs.group(1)).as(runs.group(1)).contains("w:color w:val=\"FFFFFF\"");
                }
            }
            assertThat(fieldRuns).as("begin, code, separator and end of two fields").isEqualTo(8);
        }
    }

    @Test
    void aRomanNumberedBandAsksWordForRomanNumbers() throws Exception {
        try (XWPFDocument document = export(null, session -> session.footer(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.FOOTER).rightText("{page}")
                .numbering(DocumentPageNumbering.builder().style(DocumentPageNumberStyle.LOWER_ROMAN).build())
                .build()))) {
            assertThat(only(document.getFooterList())._getHdrFtr().xmlText()).contains("> PAGE \\* roman </w:instrText>");
        }
    }

    @Test
    void numberingWordCannotCountIsReported() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument ignored = export(report, session -> session.footer(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.FOOTER).rightText("{page}")
                .numbering(DocumentPageNumbering.builder().startAt(5).countFrom(3).build())
                .build()))) {
            assertThat(report.get().notes()).filteredOn(note -> note.subject().equals("page footer"))
                    .extracting(DocxExportReport.Note::detail)
                    .anyMatch(detail -> detail.contains("starts on page 3"))
                    .anyMatch(detail -> detail.contains("count from 5"));
        }
    }

    @Test
    void twoFootersEachStandInAFrameAtTheirOwnHeight() throws Exception {
        try (XWPFDocument document = export(null, session -> {
            session.footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER)
                    .height(26).fontSize(7).leftText("Legal line").build());
            session.footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER)
                    .height(14).fontSize(7).rightText("Page {page}").build());
        })) {
            XWPFFooter footer = only(document.getFooterList());
            List<org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFramePr> frames = footer.getParagraphs()
                    .stream().filter(p -> p.getCTP().isSetPPr() && p.getCTP().getPPr().isSetFramePr())
                    .map(p -> p.getCTP().getPPr().getFramePr()).toList();

            assertThat(frames).hasSize(2).allSatisfy(frame -> {
                assertThat(frame).isNotNull();
                assertThat(frame.getVAnchor().toString()).isEqualTo("page");
            });
            long legal = ((Number) frames.get(0).getY()).longValue();
            long page = ((Number) frames.get(1).getY()).longValue();
            assertThat(legal).as("the higher band stands higher on the page, as the PDF draws it")
                    .isLessThan(page);
            // 200pt page: the lower band's line bottom sits at its footer distance above the foot.
            DocumentHeaderFooter lower = DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER)
                    .height(14).fontSize(7).build();
            assertThat(page).isEqualTo(Math.round((200 - DocxTextBands.distanceFromEdge(lower)
                    - DocxTextBands.lineHeight(lower)) * 20));
            assertThat(footer.getParagraphs()).extracting(p -> p.getCTP().isSetPPr() && p.getCTP().getPPr().isSetFramePr())
                    .as("frames at different heights are different frames, and need no hairline between them")
                    .containsExactly(true, true, false);
        }
    }

    @Test
    void aFramedSeparatorFitsInItsFrameAsTheBorderIsWritten() throws Exception {
        try (XWPFDocument document = export(null, session -> {
            session.footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER)
                    .height(26).fontSize(7).leftText("Legal line").showSeparator(true).separatorThickness(0.7f).build());
            session.footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER)
                    .height(14).fontSize(7).rightText("Page {page}").build());
        })) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph legal = only(document.getFooterList()).getParagraphs().get(0);
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder top = legal.getCTP().getPPr().getPBdr().getTop();
            double border = ((Number) top.getSpace()).doubleValue() + ((Number) top.getSz()).doubleValue() / 8.0;
            double line = 7 * DocxTextBands.LINE_FACTOR;

            assertThat(((Number) legal.getCTP().getPPr().getFramePr().getH()).longValue())
                    .as("the line and the border as written, in whole points of space and eighths of width")
                    .isEqualTo(Math.round((line + border) * 20));
        }
    }

    @Test
    void aHeaderAndAFooterEachHaveTheirOwnPartAndAOneBandKindIsNotFramed() throws Exception {
        try (XWPFDocument document = export(null, session -> {
            session.header(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.HEADER).leftText("Top").build());
            session.footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER).leftText("Foot")
                    .showSeparator(true).build());
        })) {
            assertThat(only(document.getHeaderList()).getText()).contains("Top");
            XWPFFooter footer = only(document.getFooterList());
            String xml = footer._getHdrFtr().xmlText();

            assertThat(footer.getText()).contains("Foot");
            assertThat(xml).doesNotContain("framePr").as("a slot alone sets no tab").doesNotContain("w:tabs")
                    .as("a footer's separator is above its text").contains("<w:top ");
        }
    }

    @Test
    void aBandCountedFromPageTwoLeavesTheFirstPageEmptyAndDefaultNumberingSaysNothing() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument document = export(report, session -> {
            session.header(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.HEADER).leftText("Plain").build());
            session.footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER).leftText("Later")
                    .numbering(DocumentPageNumbering.builder().countFrom(2).startAt(2).build()).build());
        })) {
            assertThat(document.getDocument().getBody().getSectPr().isSetTitlePg()).isTrue();
            assertThat(document.getFooterList()).anyMatch(footer -> footer.getText().isBlank());
            assertThat(report.get().notes()).noneMatch(note -> note.subject().startsWith("page "));
        }
    }

    @Test
    void aRomanTotalReadsRomanBeforeWordUpdatesIt() throws Exception {
        try (XWPFDocument document = export(null, session -> session.footer(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.FOOTER).rightText("{page}/{pages}")
                .numbering(DocumentPageNumbering.builder().style(DocumentPageNumberStyle.LOWER_ROMAN).build())
                .build()))) {
            assertThat(only(document.getFooterList()).getText()).contains("i/ii");
        }
    }

    @Test
    void aBandReachingPastTheMarginIsReported() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (XWPFDocument ignored = export(report, session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(60).leftText("Tall").build()))) {
            assertThat(report.get().notes()).anyMatch(note -> note.subject().equals("page header")
                                                              && note.detail().contains("past the page margin"));
        }
    }

    @Test
    void aBandReachingPastTheMarginLeavesTheBodyAtTheMargin() throws Exception {
        // The page lets the band overlap the body; Word moves the body clear of a header taller
        // than its margin unless the margin is written negative. MerchantInvoice's footer row,
        // set down to its 3.4pt margin, went to a second page under a 9.8pt footer.
        try (XWPFDocument document = export(null, session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(60).leftText("Tall").build()))) {
            Object top = document.getDocument().getBody().getSectPr().getPgMar().getTop();
            assertThat(((Number) top).longValue()).as("the 30pt margin, held whatever the band reaches")
                    .isEqualTo(-600);
        }
    }

    @Test
    void aFooterReachingPastTheMarginLeavesTheBodyAtTheMargin() throws Exception {
        try (XWPFDocument document = export(null, session -> session.footer(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.FOOTER).height(60).leftText("Tall").build()))) {
            Object bottom = document.getDocument().getBody().getSectPr().getPgMar().getBottom();
            assertThat(((Number) bottom).longValue()).isEqualTo(-600);
        }
    }

    @Test
    void aFramedBandLeavesTheMarginAsItIs() throws Exception {
        // Two footers stand in frames at their own heights, beside the part's flow.
        try (XWPFDocument document = export(null, session -> {
            session.footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER).height(60)
                    .leftText("Tall").build());
            session.footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER)
                    .rightText("Short").build());
        })) {
            Object bottom = document.getDocument().getBody().getSectPr().getPgMar().getBottom();
            assertThat(((Number) bottom).longValue()).isEqualTo(600);
        }
    }

    @Test
    void aBandPastNoMarginWritesTheLeastNegativeOne() throws Exception {
        // A margin of none has no negative: a twentieth of a point stands for it.
        try (DocumentSession session = GraphCompose.document().pageSize(300, 200)
                .margin(DocumentInsets.of(0)).create()) {
            session.header(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.HEADER)
                    .leftText("Edge").build());
            session.pageFlow(page -> page.addParagraph(p -> p.text("Body")));
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(
                    session.export(new DocxSemanticBackend())))) {
                Object top = document.getDocument().getBody().getSectPr().getPgMar().getTop();
                assertThat(((Number) top).longValue()).isEqualTo(-1);
            }
        }
    }

    @Test
    void aBandWithinTheMarginLeavesItAsItIs() throws Exception {
        try (XWPFDocument document = export(null, session -> session.footer(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.FOOTER).leftText("Short").build()))) {
            Object bottom = document.getDocument().getBody().getSectPr().getPgMar().getBottom();
            assertThat(((Number) bottom).longValue()).isEqualTo(600);
        }
    }

    private static <T> T only(List<T> parts) {
        assertThat(parts).hasSize(1);
        return parts.get(0);
    }

    private static XWPFDocument export(AtomicReference<DocxExportReport> report,
                                       Consumer<DocumentSession> chrome) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(300, 200)
                .margin(DocumentInsets.of(30)).create()) {
            chrome.accept(session);
            session.pageFlow(page -> {
                for (int line = 0; line < 14; line++) {
                    int number = line;
                    page.addParagraph(p -> p.text("Line " + number));
                }
            });
            byte[] docx = session.export(report == null
                    ? new DocxSemanticBackend()
                    : new DocxSemanticBackend(report::set));
            return new XWPFDocument(new ByteArrayInputStream(docx));
        }
    }
}
