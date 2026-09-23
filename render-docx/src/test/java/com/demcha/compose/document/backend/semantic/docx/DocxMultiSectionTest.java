package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.MultiSectionDocument;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHdrFtrRef;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Several sessions export as one Word document, a Word section per session.
 *
 * <p>A multi-section document exists to give each part its own page — a cover in one size, a
 * body in another, a footer on one and not the other. Word holds all of that per section, so
 * each section of the input becomes a section of the file, and the things Word would do
 * differently left alone — carry the page count on, repeat the previous section's footer —
 * are told to do what the PDF does.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxMultiSectionTest {

    @Test
    void eachSectionKeepsItsOwnPage() throws Exception {
        try (XWPFDocument document = export(cover(), landscapeBody())) {
            List<CTSectPr> sections = sectionsOf(document);

            assertThat(sections).hasSize(2);
            assertThat(DocxTwips.of(sections.get(0).getPgSz().getW())).isEqualTo(300 * 20L);
            assertThat(DocxTwips.of(sections.get(0).getPgSz().getH())).isEqualTo(400 * 20L);
            assertThat(sections.get(0).getPgSz().getOrient()).isEqualTo(STPageOrientation.PORTRAIT);
            assertThat(DocxTwips.of(sections.get(0).getPgMar().getLeft())).isEqualTo(24 * 20L);

            assertThat(DocxTwips.of(sections.get(1).getPgSz().getW())).isEqualTo(500 * 20L);
            assertThat(DocxTwips.of(sections.get(1).getPgSz().getH())).isEqualTo(300 * 20L);
            assertThat(sections.get(1).getPgSz().getOrient()).isEqualTo(STPageOrientation.LANDSCAPE);
            assertThat(DocxTwips.of(sections.get(1).getPgMar().getLeft())).isEqualTo(40 * 20L);
        }
    }

    @Test
    void eachSectionCountsItsPagesFromOne() throws Exception {
        try (XWPFDocument document = export(cover(), landscapeBody())) {
            for (CTSectPr section : sectionsOf(document)) {
                assertThat(section.isSetPgNumType()).isTrue();
                assertThat(DocxTwips.of(section.getPgNumType().getStart())).isEqualTo(1L);
            }
            XWPFFooter footer = footerOf(document, sectionsOf(document).get(1));
            assertThat(fieldInstructions(footer))
                    .as("the total is the section's, as the section's own footer counts it on the page")
                    .contains("SECTIONPAGES")
                    .doesNotContain("NUMPAGES");
        }
    }

    @Test
    void aPageTotalReadsTheSectionsLaidOutCountBeforeAnEditorUpdatesIt() throws Exception {
        DocumentSession longBody = landscapeBody();
        longBody.pageFlow(page -> {
            for (int line = 0; line < 30; line++) {
                int number = line;
                page.addParagraph(p -> p.text("Body line " + number));
            }
        });
        try (XWPFDocument document = export(cover(), longBody)) {
            XWPFFooter footer = footerOf(document, sectionsOf(document).get(1));

            // LibreOffice does not update SECTIONPAGES, so what is written is what it shows:
            // with a placeholder of 1, the body's second page read "page 2 of 1".
            assertThat(footer.getText().strip()).isEqualTo("Body\t12");
        }
    }

    @Test
    void aSectionWithoutAFooterDoesNotRepeatThePreviousOne() throws Exception {
        try (XWPFDocument document = export(landscapeBody(), cover())) {
            List<CTSectPr> sections = sectionsOf(document);
            XWPFFooter first = footerOf(document, sections.get(0));
            XWPFFooter second = footerOf(document, sections.get(1));

            assertThat(first.getText()).contains("Body");
            assertThat(second)
                    .as("Word would otherwise show the body's footer on the cover")
                    .isNotSameAs(first);
            assertThat(second.getText()).isBlank();
        }
    }

    @Test
    void aSectionBeforeAnyFooterNeedsNoneOfItsOwn() throws Exception {
        try (XWPFDocument document = export(cover(), landscapeBody())) {
            assertThat(sectionsOf(document).get(0).getFooterReferenceList())
                    .as("nothing before it to inherit")
                    .isEmpty();
        }
    }

    @Test
    void aLinkReachesAnAnchorInAnotherSection() throws Exception {
        try (XWPFDocument document = export(cover(), landscapeBody())) {
            List<String> anchors = new ArrayList<>();
            List<String> bookmarks = new ArrayList<>();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                for (CTHyperlink link : paragraph.getCTP().getHyperlinkList()) {
                    if (link.getAnchor() != null) {
                        anchors.add(link.getAnchor());
                    }
                }
                for (CTBookmark bookmark : paragraph.getCTP().getBookmarkStartList()) {
                    bookmarks.add(bookmark.getName());
                }
            }

            assertThat(anchors).hasSize(1);
            assertThat(bookmarks).contains(anchors.get(0));
        }
    }

    @Test
    void aSectionEndingInATableIsClosedByAParagraphOfItsOwn() throws Exception {
        DocumentSession tableOnly = session(300, 400, 24);
        tableOnly.pageFlow(page -> page.addTable(t -> t.autoColumns(2).row("Net", "100")));
        try (XWPFDocument document = export(tableOnly, landscapeBody())) {
            assertThat(sectionsOf(document)).hasSize(2);
            XWPFParagraph carrier = document.getParagraphArray(0);
            assertThat(document.getBodyElements().get(0))
                    .as("the table comes first, and the section ends after it")
                    .isSameAs(document.getTables().get(0));
            assertThat(carrier.getCTP().getPPr().isSetSectPr()).isTrue();
        }
    }

    @Test
    void oneSectionExportsExactlyAsItsSessionDoes() throws Exception {
        DocxSemanticBackend backend = DocxSemanticBackend.builder().deterministic(true).build();
        byte[] alone;
        try (DocumentSession session = landscapeBody()) {
            alone = session.export(backend);
        }
        byte[] asASection;
        try (MultiSectionDocument document = GraphCompose.documents().section(landscapeBody()).create()) {
            asASection = document.export(backend);
        }

        assertThat(asASection).isEqualTo(alone);
    }

    private static XWPFDocument export(DocumentSession... sessions) throws Exception {
        var builder = GraphCompose.documents();
        for (DocumentSession session : sessions) {
            builder.section(session);
        }
        byte[] docx;
        try (MultiSectionDocument document = builder.create()) {
            docx = document.toDocxBytes();
        }
        return new XWPFDocument(new ByteArrayInputStream(docx));
    }

    private static DocumentSession cover() {
        DocumentSession cover = session(300, 400, 24);
        cover.pageFlow(page -> page.addParagraph(p -> p.text("Go to the introduction").linkTo("intro")));
        return cover;
    }

    private static DocumentSession landscapeBody() {
        DocumentSession body = session(500, 300, 40);
        body.chrome().zone(DocumentPageZone.footer(30, page -> new RowBuilder()
                .addParagraph(p -> p.text("Body"))
                .flexSpacer()
                .add(page.pageNumber())
                .add(page.pageTotal())
                .build()));
        body.pageFlow(page -> page.addParagraph(p -> p.text("Introduction").anchor("intro")));
        return body;
    }

    private static DocumentSession session(double width, double height, double margin) {
        return GraphCompose.document()
                .pageSize(width, height)
                .margin(DocumentInsets.of(margin))
                .create();
    }

    /** Every section's properties in document order: those ending a section, then the body's. */
    private static List<CTSectPr> sectionsOf(XWPFDocument document) {
        List<CTSectPr> sections = new ArrayList<>();
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            if (paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetSectPr()) {
                sections.add(paragraph.getCTP().getPPr().getSectPr());
            }
        }
        sections.add(document.getDocument().getBody().getSectPr());
        return sections;
    }

    private static XWPFFooter footerOf(XWPFDocument document, CTSectPr section) {
        assertThat(section.getFooterReferenceList()).hasSize(1);
        CTHdrFtrRef reference = section.getFooterReferenceList().get(0);
        POIXMLDocumentPart part = document.getRelationById(reference.getId());
        assertThat(part).isInstanceOf(XWPFFooter.class);
        return (XWPFFooter) part;
    }

    private static List<String> fieldInstructions(XWPFFooter footer) {
        List<String> instructions = new ArrayList<>();
        for (XWPFParagraph paragraph : footer.getParagraphs()) {
            for (CTSimpleField field : paragraph.getCTP().getFldSimpleList()) {
                instructions.add(field.getInstr().trim());
            }
        }
        return instructions;
    }
}
