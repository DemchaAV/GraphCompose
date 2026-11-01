package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageBackgroundFill;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A session's page backgrounds are painted behind every page's text in Word.
 *
 * <p>They were not written at all, so a sidebar CV's white text stood on a white page. Each fill
 * is a rectangle anchored to the page in the section's header, behind the text: Word draws a
 * header's shapes on every page that header is shown on.</p>
 */
class DocxPageBackgroundTest {

    private static final DocumentColor CHARCOAL = DocumentColor.rgb(0x2B, 0x2F, 0x36);

    @Test
    void aColumnFillIsARectangleBehindTheTextWhereThePagePaintsIt() throws Exception {
        try (XWPFDocument document = export(session -> session.pageBackgrounds(List.of(
                PageBackgroundFill.leftColumn(0.3, CHARCOAL))))) {
            String xml = onlyHeader(document)._getHdrFtr().xmlText();

            assertThat(xml.split("<w:drawing", -1).length - 1)
                    .as("one drawing for the one fill, not a drawing inside a drawing")
                    .isEqualTo(1);
            assertThat(xml).contains("behindDoc=\"1\"")
                    .contains("relativeFrom=\"page\"")
                    .contains("<wp:posOffset>0</wp:posOffset>")
                    .contains("cx=\"" + Units.toEMU(0.3 * 400) + "\"")
                    .contains("cy=\"" + Units.toEMU(600) + "\"")
                    .contains("val=\"2B2F36\"");
        }
    }

    @Test
    void aBandIsPlacedFromThePagesTopAndFillsKeepTheirOrder() throws Exception {
        // A tint over the whole page, then a band 60pt tall, 100pt down: the band is drawn over
        // the tint, as the page paints it.
        try (XWPFDocument document = export(session -> session.pageBackgrounds(List.of(
                PageBackgroundFill.fullPage(DocumentColor.rgb(0xF4, 0xF4, 0xF4)),
                PageBackgroundFill.bandPoints(100, 60, 600, CHARCOAL))))) {
            String xml = onlyHeader(document)._getHdrFtr().xmlText();

            assertThat(xml.indexOf("F4F4F4")).isLessThan(xml.indexOf("2B2F36"));
            assertThat(xml).contains("<wp:posOffset>" + Units.toEMU(100) + "</wp:posOffset>")
                    .contains("relativeHeight=\"" + DocxPageBackgrounds.stackHeight(1) + "\"");
        }
    }

    @Test
    void aSectionWithoutBackgroundsDoesNotShowTheOnesOfTheSectionBeforeIt() throws Exception {
        // Word shows a section with no header of its own the previous section's header, shapes
        // and all: the body pages took the cover's navy behind their dark text.
        DocumentSession cover = GraphCompose.document().pageSize(300, 440).margin(DocumentInsets.of(24)).create();
        cover.pageBackground(DocumentColor.rgb(28, 39, 64));
        cover.pageFlow(page -> page.addParagraph("Cover").addPageBreak(b -> { }).addParagraph("Inside cover"));
        DocumentSession body = GraphCompose.document().pageSize(300, 440).margin(DocumentInsets.of(24)).create();
        body.pageFlow(page -> page.addParagraph("Body"));
        byte[] docx;
        try (var sections = GraphCompose.documents().section(cover).section(body).create()) {
            docx = sections.toDocxBytes();
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            var bodySection = document.getDocument().getBody().getSectPr();
            XWPFHeader bodyHeader = new org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy(document, bodySection)
                    .getDefaultHeader();
            assertThat(bodyHeader).as("a header of the body's own, not the cover's").isNotNull();
            assertThat(bodyHeader._getHdrFtr().xmlText()).doesNotContain("<w:drawing");
            assertThat(document.getHeaderList())
                    .as("the cover's header carries its colour")
                    .anyMatch(header -> header._getHdrFtr().xmlText().contains("1C2740"));
        }
    }

    @Test
    void aCoverOfOnePageDoesNotShowItsColourBehindTheSectionAfterIt() throws Exception {
        // A cover of one page used to draw its colour from the body, so there was no header for
        // the section after it to inherit and nothing to shield it from. Now that it writes one,
        // the section after it needs the empty header of its own that a longer cover already
        // gave it.
        DocumentSession cover = GraphCompose.document().pageSize(300, 440).margin(DocumentInsets.of(24)).create();
        cover.pageBackground(DocumentColor.rgb(28, 39, 64));
        cover.pageFlow(page -> page.addParagraph("Cover"));
        DocumentSession body = GraphCompose.document().pageSize(300, 440).margin(DocumentInsets.of(24)).create();
        body.pageFlow(page -> page.addParagraph("Body"));
        byte[] docx;
        try (var sections = GraphCompose.documents().section(cover).section(body).create()) {
            docx = sections.toDocxBytes();
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            XWPFHeader bodyHeader = new org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy(document,
                    document.getDocument().getBody().getSectPr()).getDefaultHeader();
            assertThat(bodyHeader).as("a header of the body's own, not the cover's").isNotNull();
            assertThat(bodyHeader._getHdrFtr().xmlText()).doesNotContain("<w:drawing");
            assertThat(document.getHeaderList())
                    .as("the cover of one page carries its colour in a header")
                    .anyMatch(header -> header._getHdrFtr().xmlText().contains("1C2740"));
        }
    }

    @Test
    void aHeaderZoneKeepsItsTextAndCarriesTheBackgroundToo() throws Exception {
        try (XWPFDocument document = export(session -> {
            session.pageBackgrounds(List.of(PageBackgroundFill.leftColumn(0.3, CHARCOAL)));
            session.chrome().zone(com.demcha.compose.document.output.DocumentPageZone.header(30,
                    page -> new com.demcha.compose.document.dsl.ParagraphBuilder().text("Running head").build()));
        })) {
            String xml = onlyHeader(document)._getHdrFtr().xmlText();

            assertThat(xml).contains("Running head").contains("behindDoc=\"1\"");
        }
    }

    @Test
    void everyHeaderASectionShowsCarriesTheBackgroundUnderIdsOfItsOwn() throws Exception {
        // A zone on the first page only: the first page's header and the others' each carry
        // the fill, so every page is painted, and no two drawings share an id.
        try (XWPFDocument document = export(session -> {
            session.pageBackgrounds(List.of(PageBackgroundFill.leftColumn(0.3,
                    DocumentColor.rgba(0x2B, 0x2F, 0x36, 128))));
            session.chrome().zone(com.demcha.compose.document.output.DocumentPageZone.header(30,
                            page -> new com.demcha.compose.document.dsl.ParagraphBuilder().text("First").build())
                    .toBuilder().appliesTo(com.demcha.compose.document.output.PageContext::isFirst).build());
        })) {
            assertThat(document.getHeaderList()).hasSizeGreaterThanOrEqualTo(2);
            java.util.Set<String> ids = new java.util.HashSet<>();
            for (XWPFHeader header : document.getHeaderList()) {
                String xml = header._getHdrFtr().xmlText();
                assertThat(xml).contains("behindDoc=\"1\"").as("the fill's alpha").contains("<a:alpha");
                var matcher = java.util.regex.Pattern.compile("docPr id=\"(\\d+)\"").matcher(xml);
                while (matcher.find()) {
                    assertThat(ids.add(matcher.group(1))).as("id %s used once", matcher.group(1)).isTrue();
                }
            }
        }
    }

    @Test
    void aHeaderCarryingTheBackgroundsDoesNotMoveABodyThatHasNoMargin() throws Exception {
        // The header is a point tall against the page's top: past a margin of none, Word moved
        // NavySidebar's whole page a point down under it. A negative margin holds the body there.
        try (XWPFDocument document = export(DocumentInsets.zero(), session -> session.pageBackgrounds(List.of(
                PageBackgroundFill.leftColumn(0.3, CHARCOAL))))) {
            var margin = document.getDocument().getBody().getSectPr().getPgMar();

            assertThat(DocxTwips.of(margin.getHeader())).isZero();
            assertThat(DocxTwips.of(margin.getTop())).as("the least margin there is, written negative")
                    .isEqualTo(-1L);
            assertThat(DocxTwips.of(margin.getBottom())).as("no footer reaches the bottom one")
                    .isZero();
        }
    }

    @Test
    void aMarginNarrowerThanTheHeaderIsWrittenNegativeAndOneAsWideIsNot() throws Exception {
        try (XWPFDocument narrower = export(DocumentInsets.of(0.5), session -> session.pageBackgrounds(List.of(
                PageBackgroundFill.leftColumn(0.3, CHARCOAL))));
             XWPFDocument asWide = export(DocumentInsets.of(1), session -> session.pageBackgrounds(List.of(
                PageBackgroundFill.leftColumn(0.3, CHARCOAL))))) {
            assertThat(DocxTwips.of(narrower.getDocument().getBody().getSectPr().getPgMar().getTop()))
                    .isEqualTo(-10);
            assertThat(DocxTwips.of(asWide.getDocument().getBody().getSectPr().getPgMar().getTop()))
                    .as("the header reaches to the margin and no further")
                    .isEqualTo(20);
        }
    }

    @Test
    void aMarginTheHeaderDoesNotReachPastStaysAsThePageSetsIt() throws Exception {
        try (XWPFDocument document = export(session -> session.pageBackgrounds(List.of(
                PageBackgroundFill.leftColumn(0.3, CHARCOAL))))) {
            assertThat(DocxTwips.of(document.getDocument().getBody().getSectPr().getPgMar().getTop()))
                    .isEqualTo(20 * 20);
        }
    }

    @Test
    void aPageOfItsOwnCarriesItsBackgroundsInAHeaderLikeAnyOtherSection() throws Exception {
        // A section of one page drew them from the body instead, to spare the 3pt LibreOffice
        // sets a body down by under a header of any height. A page an editor adds past that one
        // — Word setting the document on a line more, or a user typing — then had no fills, and
        // a sidebar CV's white text stood on a white page. The header paints that page too.
        try (XWPFDocument document = export(DocumentInsets.zero(), session -> session.pageBackgrounds(List.of(
                PageBackgroundFill.leftColumn(0.3, CHARCOAL))), page -> page.addParagraph("Body text."))) {
            String body = document.getDocument().getBody().xmlText();

            assertThat(onlyHeader(document)._getHdrFtr().xmlText())
                    .contains("name=\"Page background 1\"").contains("val=\"2B2F36\"")
                    .contains("behindDoc=\"1\"").contains("relativeFrom=\"page\"");
            assertThat(body).as("nothing of them left in the body").doesNotContain("Page background");
            assertThat(DocxTwips.of(document.getDocument().getBody().getSectPr().getPgMar().getTop()))
                    .as("the header this section newly has holds its body at the margin too")
                    .isEqualTo(-1L);
        }
    }

    @Test
    void aSectionOfOnePageIsGivenTheSameBackgroundAnchorAsOneOfTwo() throws Exception {
        // How many pages the section has decides nothing about the fills: the anchor, the
        // identifier it carries and the header holding it are written the same way for both.
        try (XWPFDocument onePage = export(DocumentInsets.of(20), session -> session.pageBackgrounds(List.of(
                PageBackgroundFill.leftColumn(0.3, CHARCOAL))), page -> page.addParagraph("Body text."));
             XWPFDocument twoPages = export(session -> session.pageBackgrounds(List.of(
                PageBackgroundFill.leftColumn(0.3, CHARCOAL))))) {
            assertThat(onlyHeader(onePage)._getHdrFtr().xmlText())
                    .isEqualTo(onlyHeader(twoPages)._getHdrFtr().xmlText());
        }
    }

    @Test
    void aPageOfOneTableCarriesItsBackgroundsInAHeaderWithNoHairlineOverTheTable() throws Exception {
        // They were anchored in the first cell's paragraph, to keep a hairline paragraph off a
        // row as tall as the page: LibreOffice moved MidnightNavy's row onto a second page under
        // one. The header carries them instead, and the table still opens the page — the
        // hairline is opened for a drawing no paragraph of the page carries, and the fills are
        // no longer such a drawing.
        try (XWPFDocument document = export(DocumentInsets.zero(), session -> session.pageBackgrounds(List.of(
                        PageBackgroundFill.leftColumn(0.3, CHARCOAL))),
                page -> page.addRow("Columns", row -> row
                        .addSection("Side", side -> side.addParagraph("Side"))
                        .addSection("Main", main -> main.addParagraph("Main"))))) {
            var body = document.getBodyElements();
            String cell = document.getTables().get(0).getRow(0).getCell(0).getCTTc().xmlText();

            assertThat(body.get(0)).as("the table opens the page").isInstanceOf(org.apache.poi.xwpf.usermodel.XWPFTable.class);
            assertThat(cell).as("nothing of them in the cell").doesNotContain("Page background");
            assertThat(onlyHeader(document)._getHdrFtr().xmlText())
                    .contains("name=\"Page background 1\"").contains("layoutInCell=\"1\"")
                    .as("the fill is the header's to place, not the body's to move")
                    .contains("locked=\"1\"")
                    .contains("relativeFrom=\"page\"");
        }
    }

    @Test
    void aPageOfOneTableWhoseDrawingsItsCellsCarryIsNotOpenedWithAHairline() throws Exception {
        // The hairline a drawing no paragraph carries would be given is not opened for one a
        // cell's paragraph carries: a row as tall as the page has no room for it, and
        // LibreOffice set such a row on a second page under one (see openBefore).
        try (XWPFDocument document = export(DocumentInsets.zero(), session -> session.pageBackgrounds(List.of(
                        PageBackgroundFill.leftColumn(0.3, CHARCOAL))),
                page -> page.addRow("Columns", row -> row
                        .addSection("Side", side -> side.addParagraph("Side")
                                .addEllipse(dot -> dot.name("Dot").circle(6).fillColor(CHARCOAL)))
                        .addSection("Main", main -> main.addParagraph("Main"))))) {
            assertThat(document.getBodyElements().get(0)).as("the table still opens the page")
                    .isInstanceOf(org.apache.poi.xwpf.usermodel.XWPFTable.class);
            assertThat(document.getTables().get(0).getRow(0).getCell(0).getCTTc().xmlText())
                    .as("the cell's own paragraph carries the drawing").contains("<w:drawing");
        }
    }

    @Test
    void aSectionWithAFooterCarriesItsBackgroundsInItsHeaderAndNotInTheFooter() throws Exception {
        // The fills go into the header of a section that has only a footer, not into the footer
        // it does have: Word paints a page's shapes over a footer's text, and MeteredInvoice's
        // band hid its footer when they were drawn from the body of such a section.
        try (XWPFDocument document = export(DocumentInsets.of(20), session -> {
            session.pageBackgrounds(List.of(PageBackgroundFill.bandPoints(540, 40, 600, CHARCOAL)));
            session.chrome().zone(com.demcha.compose.document.output.DocumentPageZone.footer(30,
                    page -> new com.demcha.compose.document.dsl.ParagraphBuilder().text("Footer").build()));
        }, page -> page.addParagraph("Body text."))) {
            assertThat(onlyHeader(document)._getHdrFtr().xmlText()).contains("2B2F36");
            assertThat(document.getDocument().getBody().xmlText()).doesNotContain("Page background");
            assertThat(document.getFooterList()).as("a footer of its own, carrying no fill").hasSize(1);
            assertThat(document.getFooterList().get(0)._getHdrFtr().xmlText())
                    .as("the fills are not in the footer Word would paint them over")
                    .doesNotContain("Page background").contains("Footer");
        }
    }

    @Test
    void aDocumentWithoutPageBackgroundsWritesNoHeader() throws Exception {
        try (XWPFDocument document = export(session -> { })) {
            assertThat(document.getHeaderList()).isEmpty();
        }
    }

    private static XWPFHeader onlyHeader(XWPFDocument document) {
        assertThat(document.getHeaderList()).hasSize(1);
        return document.getHeaderList().get(0);
    }

    private static XWPFDocument export(Consumer<DocumentSession> setup) throws Exception {
        return export(DocumentInsets.of(20), setup);
    }

    /** A document of two pages, whose backgrounds a header carries to both. */
    private static XWPFDocument export(DocumentInsets margin, Consumer<DocumentSession> setup) throws Exception {
        return export(margin, setup, page -> page.addParagraph("Body text.").addPageBreak(b -> { })
                .addParagraph("Second page."));
    }

    private static XWPFDocument export(DocumentInsets margin, Consumer<DocumentSession> setup,
                                       Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content)
            throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 600)
                .margin(margin)
                .create()) {
            setup.accept(session);
            session.pageFlow(content);
            return new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
        }
    }
}
