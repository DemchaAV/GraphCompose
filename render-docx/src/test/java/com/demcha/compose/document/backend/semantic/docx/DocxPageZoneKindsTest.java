package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.MultiSectionDocument;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.output.PageContext;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHdrFtrRef;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr;

import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A page zone drawn on some pages only lands on the same pages in Word.
 *
 * <p>Word gives a section a header and footer for the first page, for even pages and for the
 * rest. A zone whose predicate follows those kinds becomes the matching part — with the section
 * stating a title page, or the document stating different even and odd pages — and every other
 * kind of page it is not drawn on gets an empty part, so Word does not show the zone there
 * anyway. The export used to write every such zone on every page.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPageZoneKindsTest {

    @Test
    void aHeaderOnTheFirstPageOnlyIsTheTitlePagesHeader() throws Exception {
        try (XWPFDocument document = export(twoPages(header("Cover", PageContext::isFirst)))) {
            CTSectPr section = bodySection(document);

            assertThat(section.isSetTitlePg()).isTrue();
            assertThat(headerText(document, section, STHdrFtr.FIRST)).isEqualTo("Cover");
            assertThat(headerText(document, section, STHdrFtr.DEFAULT))
                    .as("the other pages carry an empty header rather than the cover's")
                    .isBlank();
        }
    }

    @Test
    void aFooterOnEveryPageButTheFirstLeavesTheFirstEmpty() throws Exception {
        try (XWPFDocument document = export(twoPages(footer("Body", page -> !page.isFirst())))) {
            CTSectPr section = bodySection(document);

            assertThat(section.isSetTitlePg()).isTrue();
            assertThat(footerText(document, section, STHdrFtr.DEFAULT)).isEqualTo("Body");
            assertThat(footerText(document, section, STHdrFtr.FIRST)).isBlank();
            assertThat(evenAndOddPages(document)).isFalse();
        }
    }

    @Test
    void aFooterOnEvenPagesIsTheEvenPagesFooter() throws Exception {
        try (XWPFDocument document = export(twoPages(footer("Verso", page -> page.number() % 2 == 0)))) {
            CTSectPr section = bodySection(document);

            assertThat(evenAndOddPages(document)).isTrue();
            assertThat(footerText(document, section, STHdrFtr.EVEN)).isEqualTo("Verso");
            assertThat(footerText(document, section, STHdrFtr.DEFAULT)).isBlank();
            assertThat(section.isSetTitlePg()).isFalse();
        }
    }

    @Test
    void aZoneWithNoPredicateIsWrittenOnceForEveryPage() throws Exception {
        try (XWPFDocument document = export(twoPages(footer("Always", null)))) {
            CTSectPr section = bodySection(document);

            assertThat(section.isSetTitlePg()).isFalse();
            assertThat(evenAndOddPages(document)).isFalse();
            assertThat(section.getFooterReferenceList()).hasSize(1);
            assertThat(footerText(document, section, STHdrFtr.DEFAULT)).isEqualTo("Always");
        }
    }

    @Test
    void aPredicateWordHasNoPartForIsWrittenOnEveryPageAndReported() throws Exception {
        List<DocxExportReport.Note> notes = new ArrayList<>();
        DocxSemanticBackend backend = DocxSemanticBackend.builder()
                .reportSink(report -> notes.addAll(report.notes()))
                .build();
        byte[] docx;
        try (DocumentSession session = twoPages(footer("Last", PageContext::isLast))) {
            docx = session.export(backend);
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            CTSectPr section = bodySection(document);

            assertThat(section.isSetTitlePg()).isFalse();
            assertThat(footerText(document, section, STHdrFtr.DEFAULT)).isEqualTo("Last");
        }
        assertThat(notes).anySatisfy(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
            assertThat(note.subject()).isEqualTo("page zone");
        });
    }

    @Test
    void aZoneThatSkipsTheFirstPageIsPlacedWhereTheOtherPagesDrawIt() throws Exception {
        long everyPage;
        try (XWPFDocument document = export(twoPages(footer("Body", null)))) {
            everyPage = DocxTwips.of(bodySection(document).getPgMar().getFooter());
        }
        try (XWPFDocument document = export(twoPages(footer("Body", page -> !page.isFirst())))) {
            assertThat(DocxTwips.of(bodySection(document).getPgMar().getFooter()))
                    .as("measured on the pages the zone is drawn on, not guessed from its padding")
                    .isEqualTo(everyPage);
        }
    }

    @Test
    void everySectionStatesItsEvenPagesOnceOneSectionNeedsThem() throws Exception {
        byte[] docx;
        try (MultiSectionDocument document = GraphCompose.documents()
                .section(twoPages(footer("Verso", page -> page.number() % 2 == 0)))
                .section(twoPages(footer("Plain", null)))
                .create()) {
            docx = document.toDocxBytes();
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            CTSectPr second = bodySection(document);

            // Word shows the even-page footer on every even page of the document once even and
            // odd pages differ, so a section whose footer is on every page states it for both.
            assertThat(footerText(document, second, STHdrFtr.EVEN)).isEqualTo("Plain");
            assertThat(footerText(document, second, STHdrFtr.DEFAULT)).isEqualTo("Plain");
        }
    }

    private static DocumentSession twoPages(DocumentPageZone zone) {
        DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(30))
                .create();
        session.chrome().zone(zone);
        session.pageFlow(page -> {
            for (int line = 0; line < 14; line++) {
                int number = line;
                page.addParagraph(p -> p.text("Line " + number));
            }
        });
        return session;
    }

    private static DocumentPageZone header(String text, Predicate<PageContext> appliesTo) {
        DocumentPageZone zone = DocumentPageZone.header(20, page -> new RowBuilder()
                .addParagraph(p -> p.text(text))
                .build());
        return appliesTo == null ? zone : zone.toBuilder().appliesTo(appliesTo).build();
    }

    private static DocumentPageZone footer(String text, Predicate<PageContext> appliesTo) {
        DocumentPageZone zone = DocumentPageZone.footer(20, page -> new RowBuilder()
                .addParagraph(p -> p.text(text))
                .build());
        return appliesTo == null ? zone : zone.toBuilder().appliesTo(appliesTo).build();
    }

    private static XWPFDocument export(DocumentSession session) throws Exception {
        byte[] docx;
        try (session) {
            docx = session.toDocxBytes();
        }
        return new XWPFDocument(new ByteArrayInputStream(docx));
    }

    /**
     * Whether the document states different even and odd pages. Read from the settings part
     * rather than {@code getEvenAndOddHeadings()}, which answers whether the element is there
     * and not whether it says yes.
     */
    private static boolean evenAndOddPages(XWPFDocument document) throws Exception {
        PackagePart settings = document.getPackage()
                .getPart(PackagingURIHelper.createPartName("/word/settings.xml"));
        String xml;
        try (InputStream input = settings.getInputStream()) {
            xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        java.util.regex.Matcher element =
                java.util.regex.Pattern.compile("<w:evenAndOddHeaders([^>]*)/>").matcher(xml);
        if (!element.find()) {
            return false;
        }
        String attributes = element.group(1);
        return !attributes.contains("\"false\"") && !attributes.contains("\"0\"")
               && !attributes.contains("\"off\"");
    }

    private static CTSectPr bodySection(XWPFDocument document) {
        return document.getDocument().getBody().getSectPr();
    }

    private static String headerText(XWPFDocument document, CTSectPr section, STHdrFtr.Enum type) {
        return partText(document, section.getHeaderReferenceList(), type, XWPFHeader.class);
    }

    private static String footerText(XWPFDocument document, CTSectPr section, STHdrFtr.Enum type) {
        return partText(document, section.getFooterReferenceList(), type, XWPFFooter.class);
    }

    private static String partText(XWPFDocument document, List<CTHdrFtrRef> references,
                                   STHdrFtr.Enum type, Class<? extends POIXMLDocumentPart> kind) {
        List<CTHdrFtrRef> ofType = references.stream().filter(ref -> ref.getType() == type).toList();
        assertThat(ofType).as("one %s part", type).hasSize(1);
        POIXMLDocumentPart part = document.getRelationById(ofType.get(0).getId());
        assertThat(part).isInstanceOf(kind);
        StringBuilder text = new StringBuilder();
        List<XWPFParagraph> paragraphs = part instanceof XWPFHeader header
                ? header.getParagraphs()
                : ((XWPFFooter) part).getParagraphs();
        for (XWPFParagraph paragraph : paragraphs) {
            text.append(paragraph.getText());
        }
        return text.toString().strip();
    }
}
