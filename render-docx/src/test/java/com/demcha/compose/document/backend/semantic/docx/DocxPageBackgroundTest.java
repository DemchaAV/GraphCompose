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
                    .contains("relativeHeight=\"2\"");
        }
    }

    @Test
    void aSectionWithoutBackgroundsDoesNotShowTheOnesOfTheSectionBeforeIt() throws Exception {
        // Word shows a section with no header of its own the previous section's header, shapes
        // and all: the body pages took the cover's navy behind their dark text.
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
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 600)
                .margin(DocumentInsets.of(20))
                .create()) {
            setup.accept(session);
            session.pageFlow(page -> page.addParagraph("Body text."));
            return new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
        }
    }
}
