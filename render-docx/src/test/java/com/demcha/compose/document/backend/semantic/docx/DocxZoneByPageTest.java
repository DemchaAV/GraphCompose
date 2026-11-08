package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.MultiSectionDocument;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.output.PageContext;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHdrFtrRef;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A page zone whose content the page builds otherwise for its first or last page is written as
 * Word can say it.
 *
 * <p>Word has a header and footer for a section's first page and one for the pages after it.
 * A zone built otherwise for the first page — a cover's line, the running one — is written into
 * both, with the section stating a title page; one built otherwise for the last page has no part
 * of Word's to go in, and is named. The export used to build every zone once, for a page that was
 * both the first and the last, and wrote that on every page.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxZoneByPageTest {

    private static final DocumentTextStyle SMALL = DocumentTextStyle.DEFAULT.withSize(8);

    @Test
    void aFooterBuiltOtherwiseForTheFirstPageIsTheTitlePagesAndTheRestTheOrdinaryOnes() throws Exception {
        Exported exported = export(3, footer(page -> text(page.isFirst() ? "Original keep" : "Continuation sheet")));

        assertThat(exported.section().isSetTitlePg()).isTrue();
        assertThat(exported.footerText(STHdrFtr.FIRST)).isEqualTo("Original keep");
        assertThat(exported.footerText(STHdrFtr.DEFAULT)).isEqualTo("Continuation sheet");
        for (STHdrFtr.Enum type : List.of(STHdrFtr.FIRST, STHdrFtr.DEFAULT)) {
            XWPFParagraph line = exported.footerLine(type);
            assertThat(line.getCTP().getPPr().getSpacing().isSetLineRule())
                    .as("%s: measured on a page drawing it", type).isTrue();
            assertThat(line.getCTP().getPPr().isSetFramePr())
                    .as("%s: at the one distance both stand at", type).isFalse();
        }
        assertThat(exported.notes()).noneMatch(note -> note.subject().equals("page zone"));
    }

    @Test
    void aZoneDrawnAfterTheFirstPageOnlyIsBuiltForThosePages() throws Exception {
        Exported exported = export(3, footer(page -> text(page.isFirst() ? "Cover" : "Running"))
                .toBuilder().appliesTo(page -> !page.isFirst()).build());

        assertThat(exported.footerText(STHdrFtr.DEFAULT)).isEqualTo("Running");
        assertThat(exported.footerText(STHdrFtr.FIRST)).isBlank();
    }

    @Test
    void aFooterBuiltOtherwiseForTheLastPageIsWrittenThereAsOnThePagesBeforeItAndNamed() throws Exception {
        Exported exported = export(3, footer(page -> text(page.isLast() ? "End of record" : "Continued overleaf")));

        assertThat(exported.section().isSetTitlePg()).isFalse();
        assertThat(exported.footerText(STHdrFtr.DEFAULT)).isEqualTo("Continued overleaf");
        assertThat(exported.zoneNotes()).containsExactly("a footer the page builds otherwise for the last page is "
                + "written there as it is built for the pages between the first and the last: Word has a footer for "
                + "the first page and one for the rest, none for the last");
    }

    @Test
    void theFirstPageOfADocumentOfOnePageIsBuiltAsTheLast() throws Exception {
        Exported exported = export(1, footer(page -> text(page.isLast() ? "End" : "Continued")));

        assertThat(exported.footerText(STHdrFtr.FIRST)).isEqualTo("End");
        assertThat(exported.footerText(STHdrFtr.DEFAULT)).as("for a page Word's own pagination adds")
                .isEqualTo("Continued");
        assertThat(exported.zoneNotes()).containsExactly("a footer on the pages after the first written as one line "
                + "of Word's footer; whether its text stands where the page sets it is not measured");
        assertThat(DocxTwips.of(exported.section().getPgMar().getFooter()))
                .as("drawn on no page after the first, it stands where its content landed on the first")
                .isPositive();
    }

    @Test
    void aLineAfterTheFirstPageTheLayoutDoesNotMeasureStandsWhereThosePagesSetIt() throws Exception {
        // Built for the pages after the first, "Running" is drawn there as "End": its line is not
        // measured, and its distance is read from where the content landed on those pages, not on
        // the first, where an 18pt cover's line reaches lower.
        Exported split = export(2, DocumentPageZone.footer(30, page -> page.isFirst()
                ? new ParagraphBuilder().text("Cover").textStyle(DocumentTextStyle.DEFAULT.withSize(18)).build()
                : text(page.isLast() ? "End" : "Running")));
        Exported eightPoint = export(2, DocumentPageZone.footer(30, page -> text(page.isPaginated() ? "End" : "Running")));

        assertThat(DocxTwips.of(split.section().getPgMar().getFooter()))
                .isEqualTo(DocxTwips.of(eightPoint.section().getPgMar().getFooter()));
    }

    @Test
    void aZoneBuildingNothingAfterTheFirstPageButReadingTheNumberOnTheLastIsNamed() throws Exception {
        Exported exported = export(3, footer(page -> page.isFirst() ? text("Cover")
                : page.isLast() ? text("Page " + page.number()) : null));

        assertThat(exported.footerText(STHdrFtr.FIRST)).isEqualTo("Cover");
        assertThat(exported.footerText(STHdrFtr.DEFAULT)).isBlank();
        assertThat(exported.zoneNotes()).anySatisfy(note -> assertThat(note)
                .startsWith("a footer the page builds otherwise for the last page"));
        assertThat(exported.zoneNotes()).as("the page between draws none either: nothing is lost there")
                .noneMatch(note -> note.startsWith("a footer on the pages after the first is not written"));
    }

    @Test
    void theFirstPagesLineStandingAtAnotherDistanceStandsInAFrameAsTheRestsDoes() throws Exception {
        Exported exported = export(3, footer(page -> new ParagraphBuilder().text("Acme")
                .textStyle(page.isFirst() ? DocumentTextStyle.DEFAULT.withSize(18) : SMALL).build()));

        assertThat(exported.footerLine(STHdrFtr.FIRST).getCTP().getPPr().isSetFramePr()).isTrue();
        assertThat(exported.footerLine(STHdrFtr.DEFAULT).getCTP().getPPr().isSetFramePr()).isTrue();
    }

    @Test
    void aColourTheFirstPageAloneIsSetInIsWrittenThere() throws Exception {
        DocumentColor red = DocumentColor.rgb(200, 0, 0);
        Exported exported = export(3, footer(page -> new ParagraphBuilder().text("Acme")
                .textStyle(page.isFirst() ? SMALL.withColor(red) : SMALL).build()));

        assertThat(exported.footerLine(STHdrFtr.FIRST).getRuns().get(0).getColor()).isEqualToIgnoringCase("C80000");
        assertThat(java.util.Objects.requireNonNullElse(exported.footerLine(STHdrFtr.DEFAULT).getRuns().get(0).getColor(),
                "000000")).as("black, Word's own").isEqualToIgnoringCase("000000");
    }

    @Test
    void aZoneReadingThePageNumberAfterTheFirstPageIsRefusedThere() {
        assertThatThrownBy(() -> export(3, footer(page -> text(page.isFirst() ? "Cover" : "Page " + page.number()))))
                .as("built for the pages after the first, it reads the number Word sets")
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("pageNumber()");
    }

    @Test
    void aZoneReadingThePageNumberOnItsLastPageOnlyIsNamed() throws Exception {
        Exported exported = export(3, footer(page -> text(page.isLast() && !page.isFirst()
                ? "Page " + page.number() : "Running")));

        assertThat(exported.footerText(STHdrFtr.DEFAULT)).isEqualTo("Running");
        assertThat(exported.zoneNotes()).singleElement().asString()
                .startsWith("a footer the page builds otherwise for the last page");
    }

    @Test
    void aHeaderBuiltOtherwiseForTheFirstPageIsTheTitlePagesHeader() throws Exception {
        Exported exported = export(3, DocumentPageZone.header(20, page -> text(page.isFirst() ? "Cover" : "Running")));

        assertThat(exported.section().isSetTitlePg()).isTrue();
        assertThat(exported.text(exported.header(STHdrFtr.FIRST))).isEqualTo("Cover");
        assertThat(exported.text(exported.header(STHdrFtr.DEFAULT))).isEqualTo("Running");
    }

    @Test
    void aPictureTheFirstPageAloneIsDrawnWithIsWrittenThere() throws Exception {
        // In a box of its own size, the picture's data is all that differs.
        Exported exported = export(3, DocumentPageZone.header(40, page -> new ImageBuilder()
                .source(png(page.isFirst() ? 4 : 8)).size(48, 24).build()));

        assertThat(exported.section().isSetTitlePg()).isTrue();
        assertThat(exported.header(STHdrFtr.FIRST).getAllPictures()).singleElement()
                .satisfies(first -> assertThat(first.getData()).isEqualTo(png(4)));
        assertThat(exported.header(STHdrFtr.DEFAULT).getAllPictures()).singleElement()
                .satisfies(rest -> assertThat(rest.getData()).isEqualTo(png(8)));
    }

    @Test
    void theRestsLineGoesOnEvenPagesTooWhereEvenPagesDiffer() throws Exception {
        Exported exported = export(3,
                DocumentPageZone.footer(20, page -> text("Verso")).toBuilder()
                        .appliesTo(page -> page.number() % 2 == 0).build(),
                DocumentPageZone.header(20, page -> text(page.isFirst() ? "Cover" : "Running")));

        assertThat(exported.text(exported.header(STHdrFtr.FIRST))).isEqualTo("Cover");
        assertThat(exported.text(exported.header(STHdrFtr.DEFAULT))).isEqualTo("Running");
        assertThat(exported.text(exported.header(STHdrFtr.EVEN))).isEqualTo("Running");
    }

    @Test
    void eachSectionStatesItsOwnTitlePage() throws Exception {
        byte[] docx;
        try (MultiSectionDocument document = GraphCompose.documents()
                .section(session(3, footer(page -> text(page.isFirst() ? "Original keep" : "Continuation sheet"))))
                .section(session(2, footer(page -> text("Plain"))))
                .create()) {
            docx = document.toDocxBytes();
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            Exported exported = new Exported(document, List.of());
            CTSectPr first = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetSectPr())
                    .findFirst().orElseThrow().getCTP().getPPr().getSectPr();

            assertThat(first.isSetTitlePg()).isTrue();
            assertThat(exported.text(exported.footer(first, STHdrFtr.FIRST))).isEqualTo("Original keep");
            assertThat(exported.text(exported.footer(first, STHdrFtr.DEFAULT))).isEqualTo("Continuation sheet");
            assertThat(exported.section().isSetTitlePg()).as("the second section's first page is its own").isFalse();
            assertThat(exported.footerText(STHdrFtr.DEFAULT)).isEqualTo("Plain");
        }
    }

    @Test
    void whatBothLinesLoseIsSaidOnce() throws Exception {
        Exported exported = export(3, footer(page -> new RowBuilder().fillColor(DocumentColor.LIGHT_GRAY)
                .addParagraph(p -> p.text(page.isFirst() ? "Cover" : "Running").textStyle(SMALL)).build()));

        assertThat(exported.notes()).filteredOn(note -> note.subject().equals("row paint")).hasSize(1);
        assertThat(exported.notes()).doesNotHaveDuplicates();
    }

    @Test
    void twoLossesOfTheSecondLineAloneStayTwo() throws Exception {
        // The first page's line loses nothing; the rest's loses two shapes of no name.
        Exported exported = export(3, footer(page -> page.isFirst() ? text("Cover") : new RowBuilder()
                .addShape(shape -> shape.size(8, 8).fillColor(DocumentColor.LIGHT_GRAY))
                .addParagraph(p -> p.text("Running").textStyle(SMALL))
                .addShape(shape -> shape.size(8, 8).fillColor(DocumentColor.LIGHT_GRAY))
                .build()));

        assertThat(exported.notes()).filteredOn(note -> note.subject().equals("page zone content")).hasSize(2);
    }

    @Test
    void theLinesOfAZoneOfValuesBuiltAfreshAreWrittenOnce() throws Exception {
        // A barcode's options are no record; built alike for every page, the zone is one footer.
        Exported exported = export(3, footer(page -> new RowBuilder()
                .addParagraph(p -> p.text("Scan to pay").textStyle(SMALL))
                .add(new com.demcha.compose.document.dsl.BarcodeBuilder().qrCode().data("INV-1")
                        .foreground(DocumentColor.rgb(10, 10, 10)).size(16, 16).build())
                .build()));

        assertThat(exported.section().isSetTitlePg()).isFalse();
        assertThat(exported.section().getFooterReferenceList()).hasSize(1);
    }

    @Test
    void aZoneBuildingNothingForThePagesBetweenThatThePageDrawsIsNamed() throws Exception {
        Exported exported = export(3, footer(page -> page.isFirst() ? text("Cover")
                : page.isPaginated() ? text("Running") : null));

        assertThat(exported.footerText(STHdrFtr.DEFAULT)).isBlank();
        assertThat(exported.zoneNotes()).contains("a footer on the pages after the first is not written: built for "
                + "them, its content is none; a zone absent from some pages says so through appliesTo");
    }

    @Test
    void aZoneBuildingNothingForTheFirstPageThatThePageDrawsIsNamed() throws Exception {
        // The page draws a footer on page 1, asked with its number; Word's first-page footer is
        // built without one, and this content builds none for that.
        Exported exported = export(3, footer(page -> page.isFirst()
                ? (page.isPaginated() ? text("Cover") : null)
                : text("Running")));

        assertThat(exported.footerText(STHdrFtr.DEFAULT)).isEqualTo("Running");
        assertThat(exported.zoneNotes()).contains("a footer on the first page is not written: built for it, its "
                + "content is none; a zone absent from some pages says so through appliesTo");
    }

    @Test
    void theSameWordsSetInBoldOnTheFirstPageAreTheTitlePagesFooter() throws Exception {
        // The two builds differ only in one constant of the decoration's enum.
        Exported exported = export(3, footer(page -> new ParagraphBuilder().text("Cover")
                .textStyle(new DocumentTextStyle(SMALL.fontName(), SMALL.size(), page.isFirst()
                        ? com.demcha.compose.document.style.DocumentTextDecoration.BOLD
                        : com.demcha.compose.document.style.DocumentTextDecoration.DEFAULT, SMALL.color())).build()));

        assertThat(exported.section().isSetTitlePg()).isTrue();
        assertThat(exported.footerLine(STHdrFtr.FIRST).getRuns()).isNotEmpty().allMatch(run -> run.isBold());
        assertThat(exported.footerLine(STHdrFtr.DEFAULT).getRuns()).isNotEmpty().noneMatch(run -> run.isBold());
    }

    @Test
    void theSameWordsInAnotherKindOfNodeOnTheFirstPageAreTheTitlePagesFooter() throws Exception {
        Exported exported = export(3, footer(page -> page.isFirst() ? text("Cover")
                : new RowBuilder().addParagraph(p -> p.text("Cover").textStyle(SMALL)).build()));

        assertThat(exported.section().isSetTitlePg()).isTrue();
        assertThat(exported.section().getFooterReferenceList()).hasSize(2);
    }

    @Test
    void aRowHoldingOneLineFewerOnTheFirstPageIsTheTitlePagesFooter() throws Exception {
        // Three lines against four: both lists of one class, so their sizes are what tells them
        // apart — a list of one or two is another class than a list of three or more. The first
        // page's is the shorter, so read line by line it would match the rest's first three.
        Exported exported = export(3, footer(page -> {
            RowBuilder row = new RowBuilder().addParagraph(p -> p.text("Acme").textStyle(SMALL))
                    .addParagraph(p -> p.text("Invoice").textStyle(SMALL))
                    .addParagraph(p -> p.text("2026").textStyle(SMALL));
            return (page.isFirst() ? row : row.addParagraph(p -> p.text("Copy").textStyle(SMALL))).build();
        }));

        assertThat(exported.section().isSetTitlePg()).isTrue();
        assertThat(exported.text(exported.footer(STHdrFtr.FIRST))).contains("2026").doesNotContain("Copy");
        assertThat(exported.text(exported.footer(STHdrFtr.DEFAULT))).contains("Copy");
    }

    @Test
    void columnsStyledOtherwiseOnTheFirstPageAreTheTitlePagesFooter() throws Exception {
        // Two styled columns against three, maps of one class — a map of one entry is another
        // class than a map of two or more: the first page's every key is among the rest's, so
        // only their sizes tell them apart.
        DocumentTableStyle red = DocumentTableStyle.builder().fillColor(DocumentColor.rgb(200, 40, 40)).build();
        DocumentTableStyle blue = DocumentTableStyle.builder().fillColor(DocumentColor.rgb(40, 40, 200)).build();

        assertThat(export(3, DocumentPageZone.footer(30, page -> table(red, red, red))).section().getFooterReferenceList())
                .as("the same column styles on every page: one footer").hasSize(1);
        assertThat(export(3, DocumentPageZone.footer(30, page -> page.isFirst() ? table(red, red, null) : table(red, red, red)))
                .section().isSetTitlePg()).as("a column styled after the first page alone").isTrue();
        assertThat(export(3, DocumentPageZone.footer(30, page -> table(red, red, page.isFirst() ? red : blue))).section().isSetTitlePg())
                .as("a column styled otherwise on the first page").isTrue();
    }

    /** A table of one row, a cell a column, each column styled as given or, for null, not at all. */
    private static DocumentNode table(DocumentTableStyle... columns) {
        String[] cells = new String[columns.length];
        java.util.Arrays.fill(cells, "X");
        com.demcha.compose.document.dsl.TableBuilder table = new com.demcha.compose.document.dsl.TableBuilder()
                .autoColumns(columns.length)
                .row(cells);
        for (int column = 0; column < columns.length; column++) {
            if (columns[column] != null) {
                table.columnStyle(column, columns[column]);
            }
        }
        return table.build();
    }

    private static DocumentPageZone footer(Function<PageContext, DocumentNode> content) {
        return DocumentPageZone.footer(20, content);
    }

    private static byte[] png(int side) {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(side, side,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
    }

    private static DocumentNode text(String text) {
        return new ParagraphBuilder().text(text).textStyle(SMALL).build();
    }

    /** Exports zones over a body of as many pages. */
    private static Exported export(int pages, DocumentPageZone... zones) throws Exception {
        List<DocxExportReport.Note> notes = new ArrayList<>();
        DocxSemanticBackend backend = DocxSemanticBackend.builder()
                .reportSink(report -> notes.addAll(report.notes()))
                .build();
        byte[] docx;
        try (DocumentSession session = session(pages, zones)) {
            assertThat(session.layoutGraph().totalPages()).as("the body's pages").isEqualTo(pages);
            docx = session.export(backend);
        }
        return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), notes);
    }

    private static DocumentSession session(int pages, DocumentPageZone... zones) {
        DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(30))
                .create();
        for (DocumentPageZone zone : zones) {
            session.chrome().zone(zone);
        }
        session.pageFlow(page -> {
            for (int number = 1; number <= pages; number++) {
                page.addParagraph("Page " + number);
                if (number < pages) {
                    page.addPageBreak(pageBreak -> { });
                }
            }
        });
        return session;
    }

    private record Exported(XWPFDocument document, List<DocxExportReport.Note> notes) {

        CTSectPr section() {
            return document.getDocument().getBody().getSectPr();
        }

        List<String> zoneNotes() {
            return notes.stream().filter(note -> note.subject().equals("page zone"))
                    .map(DocxExportReport.Note::detail).toList();
        }

        XWPFFooter footer(STHdrFtr.Enum type) {
            return footer(section(), type);
        }

        XWPFFooter footer(CTSectPr section, STHdrFtr.Enum type) {
            return (XWPFFooter) part(section.getFooterReferenceList(), type);
        }

        XWPFHeader header(STHdrFtr.Enum type) {
            return (XWPFHeader) part(section().getHeaderReferenceList(), type);
        }

        private org.apache.poi.ooxml.POIXMLDocumentPart part(List<CTHdrFtrRef> references, STHdrFtr.Enum type) {
            List<CTHdrFtrRef> ofType = references.stream().filter(ref -> ref.getType() == type).toList();
            assertThat(ofType).as("one %s part", type).hasSize(1);
            return document.getRelationById(ofType.get(0).getId());
        }

        String footerText(STHdrFtr.Enum type) {
            return text(footer(type));
        }

        String text(XWPFHeaderFooter part) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : part.getParagraphs()) {
                text.append(paragraph.getText());
            }
            return text.toString().strip();
        }

        /** The footer's line: its first paragraph holding text. */
        XWPFParagraph footerLine(STHdrFtr.Enum type) {
            return footer(type).getParagraphs().stream().filter(paragraph -> !paragraph.getText().isBlank())
                    .findFirst().orElseThrow();
        }
    }
}
