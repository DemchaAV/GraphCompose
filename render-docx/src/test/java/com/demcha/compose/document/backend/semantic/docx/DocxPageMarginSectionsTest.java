package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageMarginRule;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.layout.payloads.TableRowFragmentPayload;
import com.demcha.compose.document.node.PageReferenceNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A page the layout sets against other margins than the page before it starts a Word section with
 * them, where it starts between blocks of the body or among a table's rows, and is named where it
 * does not.
 *
 * <p>Measured in Word and LibreOffice on Windows: one set of margins for the whole document ran a
 * page with a larger bottom margin on past its foot, 7 lines of 33 further; a section starting
 * where the page does set every page where the layout does, line for line.</p>
 */
class DocxPageMarginSectionsTest {

    // From page 2, a bottom margin 100pt larger than the first page's.
    private static final DocumentInsets FIRST = DocumentInsets.of(36);
    private static final DocumentInsets LATER = new DocumentInsets(36, 36, 136, 36);

    @Test
    void aPageWithOtherMarginsStartingBetweenBlocksStartsASectionWithThem() throws Exception {
        Exported exported = export(List.of(PageMarginRule.from(2, LATER)), page -> lines(page, "Line", 60));

        List<XWPFParagraph> ending = exported.paragraphsEndingASection();
        assertThat(ending).singleElement().satisfies(paragraph -> {
            assertThat(paragraph.getText()).as("the last line of page 1").isEqualTo(exported.lastLineOf(0));
            assertThat(bottom(paragraph.getCTP().getPPr().getSectPr())).isEqualTo(36 * 20);
        });
        assertThat(bottom(exported.body())).as("page 2 on").isEqualTo(136 * 20);
        assertThat(exported.notes()).singleElement().asString()
                .startsWith("page 2's top and bottom margins differ from page 1's: written as a Word section that starts where page 2 does")
                .as("no header to stand off").doesNotContain("header");
    }

    @Test
    void aPageBreakIntoOtherMarginsBecomesTheSectionsBreak() throws Exception {
        // Kept, the page break would open a blank page before the section's own.
        Exported exported = export(List.of(PageMarginRule.from(2, LATER)), page -> {
            lines(page, "Cover", 5);
            page.addPageBreak(b -> { });
            lines(page, "Line", 30);
        });

        assertThat(exported.document().getDocument().xmlText()).doesNotContain("w:type=\"page\"");
        assertThat(exported.paragraphsEndingASection()).singleElement()
                .satisfies(paragraph -> assertThat(paragraph.getText()).isEmpty());
        assertThat(bottom(exported.body())).isEqualTo(136 * 20);
    }

    @Test
    void aPageStartingAmongATablesRowsSplitsTheTableAtTheRowItStartsWith() throws Exception {
        // The second part opens with the header row, which the layout repeats at the top of page 2.
        Exported exported = export(List.of(PageMarginRule.from(2, LATER)), page -> page
                .addTable(table -> itemsUnderAHeader(table, 40))
                .addParagraph("Total"));

        List<XWPFTable> tables = exported.document().getTables();
        assertThat(tables).hasSize(2);
        XWPFTable first = tables.get(0);
        XWPFTable second = tables.get(1);
        int onPage1 = exported.rowsOn(0);
        assertThat(first.getNumberOfRows()).as("the header and the rows of page 1").isEqualTo(onPage1);
        assertThat(second.getRow(0).getCell(0).getText()).isEqualTo("Item");
        assertThat(second.getRow(0).isRepeatHeader()).isTrue();
        assertThat(second.getRow(1).getCell(0).getText()).as("page 2's first row").isEqualTo("Item " + (onPage1 - 1));
        assertThat(first.getNumberOfRows() + second.getNumberOfRows()).isEqualTo(1 + 40 + 1);
        XWPFParagraph ending = exported.paragraphsEndingASection().get(0);
        assertThat(exported.paragraphsEndingASection()).singleElement();
        List<IBodyElement> body = exported.document().getBodyElements();
        assertThat(body.indexOf(ending)).as("between the two parts").isEqualTo(body.indexOf(first) + 1)
                .isEqualTo(body.indexOf(second) - 1);
        assertThat(((java.math.BigInteger) ending.getCTP().getPPr().getSpacing().getLine()).longValue())
                .as("a hairline, which page 1 has room for").isEqualTo(2);
        assertThat(bottom(ending.getCTP().getPPr().getSectPr())).isEqualTo(36 * 20);
        assertThat(bottom(exported.body())).isEqualTo(136 * 20);
        assertThat(exported.report().notes()).filteredOn(note -> note.subject().equals("page margin"))
                .singleElement().satisfies(note -> {
                    assertThat(note.path()).endsWith("TableNode[0]");
                    assertThat(note.detail()).isEqualTo("page 2's top and bottom margins differ from page 1's: written "
                                                        + "as a Word section that starts at the row page 2 starts with, "
                                                        + "the table split there, the part after it opening with its "
                                                        + "header rows, and its page break fixed there, so rows an edit "
                                                        + "adds above it no longer run onto that page");
                });
    }

    @Test
    void aTableRunningOntoPagesOfTwoOtherSetsOfMarginsIsSplitAtTheRowEachStartsWith() throws Exception {
        DocumentInsets middle = new DocumentInsets(36, 36, 86, 36);
        Exported exported = export(List.of(PageMarginRule.page(2, middle), PageMarginRule.from(3, LATER)),
                page -> page.addTable(table -> itemsUnderAHeader(table, 50)));

        List<XWPFTable> tables = exported.document().getTables();
        assertThat(tables).hasSize(3);
        assertThat(tables.get(0).getNumberOfRows()).isEqualTo(exported.rowsOn(0));
        assertThat(tables.get(1).getNumberOfRows()).as("the header and the rows of page 2").isEqualTo(exported.rowsOn(1));
        assertThat(tables.get(2).getRow(0).getCell(0).getText()).isEqualTo("Item");
        assertThat(tables.get(2).getRow(1).getCell(0).getText())
                .isEqualTo("Item " + (exported.rowsOn(0) - 1 + exported.rowsOn(1) - 1));
        List<XWPFParagraph> ending = exported.paragraphsEndingASection();
        assertThat(ending).hasSize(2);
        List<IBodyElement> body = exported.document().getBodyElements();
        assertThat(body.indexOf(ending.get(0))).isEqualTo(body.indexOf(tables.get(0)) + 1);
        assertThat(body.indexOf(ending.get(1))).isEqualTo(body.indexOf(tables.get(1)) + 1);
        assertThat(bottom(ending.get(0).getCTP().getPPr().getSectPr())).isEqualTo(36 * 20);
        assertThat(bottom(ending.get(1).getCTP().getPPr().getSectPr())).isEqualTo(86 * 20);
        assertThat(bottom(exported.body())).isEqualTo(136 * 20);
    }

    @Test
    void aTableWhoseRowsReachThePagesFootIsNotSplitThere() throws Exception {
        // Page 1's rows end a point above its foot: the hairline between the two parts would not
        // fit, and would open a page of its own before the section's.
        double row = rowHeight();
        Exported exported = export(List.of(PageMarginRule.page(1, new DocumentInsets(36, 36, 400 - 36 - 10 * row - 1, 36)),
                PageMarginRule.from(2, LATER)), page -> page.addTable(table -> rows(table, "Item", 20)));

        assertThat(exported.rowsOn(0)).as("ten rows on page 1").isEqualTo(10);
        assertThat(exported.paragraphsEndingASection()).isEmpty();
        assertThat(exported.document().getTables()).hasSize(1);
        assertThat(exported.report().notes()).filteredOn(note -> note.subject().equals("page margin"))
                .singleElement().satisfies(note -> {
                    assertThat(note.path()).endsWith("TableNode[0]");
                    assertThat(note.detail()).endsWith("page 1's content reaches its foot, where the paragraph that "
                                                       + "would end a Word section before page 2 would open a page of "
                                                       + "its own");
                });
    }

    @Test
    void aTableWithoutHeaderRowsIsSplitIntoPartsWithNone() throws Exception {
        Exported exported = export(List.of(PageMarginRule.from(2, LATER)), page -> page
                .addTable(table -> rows(table, "Item", 30)));

        List<XWPFTable> tables = exported.document().getTables();
        assertThat(tables).hasSize(2);
        assertThat(tables.get(0).getNumberOfRows()).isEqualTo(exported.rowsOn(0));
        assertThat(tables.get(1).getRow(0).getCell(0).getText()).as("page 2's first row")
                .isEqualTo("Item " + exported.rowsOn(0));
        assertThat(tables.get(1).getRow(0).isRepeatHeader()).isFalse();
        assertThat(exported.notes()).singleElement().asString().isEqualTo("page 2's top and bottom margins differ from "
                + "page 1's: written as a Word section that starts at the row page 2 starts with, the table split "
                + "there, and its page break fixed there, so rows an edit adds above it no longer run onto that page");
    }

    @Test
    void aTableSplitBeforeAPageBreakIntoOtherMarginsEndsTwoSections() throws Exception {
        // The split ends page 1's section; the page break, carrying the next, becomes its break.
        DocumentInsets third = new DocumentInsets(36, 36, 86, 36);
        Exported exported = export(List.of(PageMarginRule.page(2, LATER), PageMarginRule.from(3, third)), page -> {
            page.addTable(table -> rows(table, "Item", 20));
            page.addPageBreak(b -> { });
            lines(page, "Line", 10);
        });

        List<XWPFTable> tables = exported.document().getTables();
        assertThat(tables).hasSize(2);
        List<XWPFParagraph> ending = exported.paragraphsEndingASection();
        assertThat(ending).hasSize(2);
        List<IBodyElement> body = exported.document().getBodyElements();
        assertThat(body.indexOf(ending.get(0))).as("between the parts").isEqualTo(body.indexOf(tables.get(0)) + 1);
        assertThat(body.indexOf(ending.get(1))).as("the page break, after the second part")
                .isEqualTo(body.indexOf(tables.get(1)) + 1);
        assertThat(exported.document().getDocument().xmlText()).doesNotContain("w:type=\"page\"");
        assertThat(bottom(ending.get(1).getCTP().getPPr().getSectPr())).isEqualTo(136 * 20);
        assertThat(bottom(exported.body())).isEqualTo(86 * 20);
    }

    @Test
    void aTableOfALaterSectionOfADocumentOfSeveralIsSplitInIt() throws Exception {
        DocumentSession body = GraphCompose.document().pageSize(400, 400).margin(FIRST).create();
        body.pageMargins(List.of(PageMarginRule.from(2, LATER)));
        body.pageFlow(page -> page.addTable(table -> rows(table, "Item", 30)));
        DocumentSession cover = GraphCompose.document().pageSize(400, 400).margin(FIRST).create();
        cover.pageFlow(page -> page.addParagraph("Cover"));
        byte[] bytes;
        try (com.demcha.compose.document.api.MultiSectionDocument document = GraphCompose.documents()
                .section(cover).section(body).create()) {
            bytes = document.toDocxBytes();
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            assertThat(document.getTables()).hasSize(2);
            assertThat(document.getParagraphs())
                    .filteredOn(p -> p.getCTP().isSetPPr() && p.getCTP().getPPr().isSetSectPr())
                    .as("the cover's, then the hairline between the parts").hasSize(2);
            List<IBodyElement> elements = document.getBodyElements();
            assertThat(elements.get(elements.indexOf(document.getTables().get(0)) + 1))
                    .isInstanceOfSatisfying(XWPFParagraph.class,
                            between -> assertThat(between.getCTP().getPPr().isSetSectPr()).isTrue());
            assertThat(bottom(document.getDocument().getBody().getSectPr())).isEqualTo(136 * 20);
        }
    }

    @Test
    void aBookmarkInARowMovedToTheSecondPartIsWrittenOnceThere() throws Exception {
        Exported exported = export(List.of(PageMarginRule.from(2, LATER)), page -> page.addTable(table -> {
            table.columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(100));
            for (int row = 0; row < 30; row++) {
                table.rowCells(row == 25
                                ? DocumentTableCell.node(new ParagraphBuilder().text("Item " + row).anchor("late").build())
                                : DocumentTableCell.text("Item " + row),
                        DocumentTableCell.text("$" + row));
            }
        }));

        List<XWPFTable> tables = exported.document().getTables();
        assertThat(tables).hasSize(2);
        assertThat(bookmarks(tables.get(0))).isEmpty();
        assertThat(bookmarks(tables.get(1))).singleElement().isEqualTo("late");
    }

    @Test
    void aPageReferenceInARowThatWouldMoveKeepsTheTableWhole() throws Exception {
        // The export settles a page reference once the body is written, in the paragraph it wrote
        // it in: moved to the second part, that paragraph is gone.
        Exported exported = export(List.of(PageMarginRule.from(2, LATER)), page -> page
                .addParagraph(p -> p.text("Intro").anchor("intro"))
                .addTable(table -> {
                    table.columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(100));
                    for (int row = 0; row < 30; row++) {
                        table.rowCells(DocumentTableCell.text("Item " + row), row == 25
                                ? DocumentTableCell.node(new PageReferenceNode("intro", null, TextAlign.LEFT))
                                : DocumentTableCell.text("$" + row));
                    }
                }));

        assertThat(exported.document().getTables()).hasSize(1);
        assertThat(exported.document().getDocument().xmlText()).contains("PAGEREF");
        assertThat(exported.report().notes()).filteredOn(note -> note.subject().equals("page margin"))
                .singleElement().satisfies(note -> {
                    assertThat(note.path()).endsWith("TableNode[1]");
                    assertThat(note.detail()).as("each page the table runs onto")
                            .isEqualTo("the top and bottom margins of pages 2 and 3 differ from page 1's, which Word "
                                       + "sets them with: each starts inside this block, where the export starts no "
                                       + "Word section");
                });
    }

    @Test
    void aTableNamedForAPageBeforeASectionItStartsIsNamedForAPageAfterItToo() throws Exception {
        // Page 2 starts inside a cell spanning two rows, page 3 at a row of its own, and page 4
        // inside a span again: named for page 2 and split for page 3, the table is named for page 4.
        List<PageMarginRule> rules = List.of(PageMarginRule.page(2, new DocumentInsets(36, 36, 136, 36)),
                PageMarginRule.page(3, new DocumentInsets(36, 36, 126, 36)),
                PageMarginRule.from(4, new DocumentInsets(36, 36, 116, 36)));
        List<Long> counts = export(rules, page -> page.addTable(table -> rows(table, "Item", 50))).rows();
        int page2 = counts.get(0).intValue();
        int page4 = page2 + counts.get(1).intValue() + counts.get(2).intValue();
        Exported exported = export(rules, page -> page.addTable(table -> {
            table.columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(100));
            for (int row = 0; row < 50; row++) {
                if (row + 1 == page2 || row + 1 == page4) {
                    table.rowCells(DocumentTableCell.text("Span " + row).rowSpan(2), DocumentTableCell.text("$" + row));
                } else if (row == page2 || row == page4) {
                    table.rowCells(DocumentTableCell.text("$" + row));
                } else {
                    table.row("Item " + row, "$" + row);
                }
            }
        }));

        assertThat(exported.notes()).filteredOn(note -> note.contains("starts inside this block"))
                .extracting(note -> note.substring(0, 6)).containsExactly("page 2", "page 4");
        assertThat(exported.notes()).anyMatch(note -> note.startsWith("page 3's top and bottom margins differ from "
                + "page 1's: written"));
    }

    private static List<String> bookmarks(XWPFTable table) {
        List<String> names = new ArrayList<>();
        for (var found : table.getCTTbl().selectPath("declare namespace w='http://schemas.openxmlformats.org/"
                                                     + "wordprocessingml/2006/main' .//w:bookmarkStart")) {
            names.add(((org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark) found).getName());
        }
        return names;
    }

    @Test
    void aBlockSeveralPagesStartInsideNamesEachInItsOneNote() throws Exception {
        // Word sets each with page 1's margins; named for page 2 alone, page 3's went unsaid.
        Exported exported = export(List.of(PageMarginRule.from(2, LATER)), page -> page.addSection(panel -> {
            panel.fillColor(com.demcha.compose.document.style.DocumentColor.rgb(238, 242, 247));
            for (int line = 0; line < 70; line++) {
                panel.addParagraph("Line " + line);
            }
        }));

        assertThat(exported.lastNames()).as("four pages").hasSize(4);
        assertThat(exported.report().notes()).filteredOn(note -> note.subject().equals("page margin"))
                .singleElement().satisfies(note -> {
                    assertThat(note.path()).endsWith("SectionNode[0]");
                    assertThat(note.detail()).isEqualTo("the top and bottom margins of pages 2 to 4 differ from page "
                                                        + "1's, which Word sets them with: each starts inside this "
                                                        + "block, where the export starts no Word section");
                });
    }

    @Test
    void theRunsOfPagesStartingInsideABlockAreNamedAsRuns() throws Exception {
        // Page 4 has page 1's margins again, which Word sets it with: the note leaves it out.
        Exported exported = export(List.of(PageMarginRule.page(2, LATER), PageMarginRule.page(3, LATER),
                PageMarginRule.page(5, LATER)), page -> page.addSection(panel -> {
            panel.fillColor(com.demcha.compose.document.style.DocumentColor.rgb(238, 242, 247));
            for (int line = 0; line < 100; line++) {
                panel.addParagraph("Line " + line);
            }
        }));

        assertThat(exported.lastNames().size()).as("five pages or more").isGreaterThanOrEqualTo(5);
        assertThat(exported.notes()).singleElement().asString().startsWith("the top and bottom margins of pages 2, "
                + "3 and 5 differ from page 1's");
    }

    @Test
    void aNoteAboutAPageNamesThePage() throws Exception {
        Exported written = export(List.of(PageMarginRule.from(2, LATER)), page -> lines(page, "Line", 60));
        Exported sides = export(List.of(PageMarginRule.from(2, DocumentInsets.symmetric(36, 90))),
                page -> lines(page, "Line", 60));

        assertThat(written.report().notes()).filteredOn(note -> note.subject().equals("page margin"))
                .extracting(DocxExportReport.Note::path).containsExactly("page 2");
        assertThat(sides.report().notes()).filteredOn(note -> note.subject().equals("page margin"))
                .extracting(DocxExportReport.Note::path).containsExactly("page 2");
    }

    @Test
    void aNoteAboutAPageOfASectionOfSeveralNamesTheSectionAndThePage() throws Exception {
        DocumentSession body = GraphCompose.document().pageSize(400, 400).margin(FIRST).create();
        body.pageMargins(List.of(PageMarginRule.from(2, LATER)));
        body.pageFlow(page -> lines(page, "Line", 60));
        DocumentSession cover = GraphCompose.document().pageSize(400, 400).margin(FIRST).create();
        cover.pageFlow(page -> page.addParagraph("Cover"));
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (com.demcha.compose.document.api.MultiSectionDocument document = GraphCompose.documents()
                .section(cover).section(body).create()) {
            document.export(new DocxSemanticBackend(report::set));
        }

        assertThat(report.get().notes()).filteredOn(note -> note.subject().equals("page margin"))
                .extracting(DocxExportReport.Note::path).containsExactly("section 2, page 2");
    }

    @Test
    void aPageStartingInsideAPanelIsNamed() throws Exception {
        Exported exported = export(List.of(PageMarginRule.from(2, LATER)), page -> page.addSection(panel -> {
            panel.fillColor(com.demcha.compose.document.style.DocumentColor.rgb(238, 242, 247));
            for (int line = 0; line < 40; line++) {
                panel.addParagraph("Line " + line);
            }
        }));

        assertThat(exported.paragraphsEndingASection()).isEmpty();
        assertThat(exported.report().notes()).filteredOn(note -> note.subject().equals("page margin"))
                .singleElement().satisfies(note -> {
                    assertThat(note.path()).endsWith("SectionNode[0]");
                    assertThat(note.detail()).isEqualTo("page 2's top and bottom margins differ from page 1's, which "
                                                        + "Word sets it with: page 2 starts inside this block, where "
                                                        + "the export starts no Word section");
                });
    }

    @Test
    void aOnePageDocumentIsWrittenWithItsPagesOwnTopAndBottomMargins() throws Exception {
        // Its sides stay the document's, which the body is written against, and are named.
        Exported exported = export(List.of(PageMarginRule.page(1, new DocumentInsets(50, 40, 30, 20))),
                page -> page.addParagraph("Only page"));

        assertThat(exported.paragraphsEndingASection()).isEmpty();
        assertThat(exported.body().getPgMar().getTop()).isEqualTo(java.math.BigInteger.valueOf(50 * 20));
        assertThat(bottom(exported.body())).isEqualTo(30 * 20);
        assertThat(exported.body().getPgMar().getLeft()).isEqualTo(java.math.BigInteger.valueOf(36 * 20));
        assertThat(exported.notes()).singleElement().asString().startsWith("page 1's side margins differ from the "
                + "document's and are not written");
    }

    @Test
    void aLaterSectionOpensWithNoTitlePageOfItsOwn() throws Exception {
        // The first page's zone is the title page's part, written for page 1 alone: a later
        // section with it would set it on its own first page too.
        Exported exported = export(List.of(PageMarginRule.from(2, LATER)), session -> session.chrome().zone(
                DocumentPageZone.header(18, page -> new ParagraphBuilder().text(page.isFirst() ? "Cover" : "Running")
                        .build())), page -> lines(page, "Line", 60));

        assertThat(exported.paragraphsEndingASection()).singleElement().satisfies(paragraph ->
                assertThat(paragraph.getCTP().getPPr().getSectPr().isSetTitlePg()).isTrue());
        assertThat(exported.body().isSetTitlePg()).isFalse();
    }

    @Test
    void aSideHeldAtItsMarginIsHeldInALaterSectionAndSoIsOneNarrowerUnderAHeaderOrFooter() {
        assertThat(DocxPageMarginSections.held(-720, 40, false)).as("held in the first").isEqualTo(-800);
        assertThat(DocxPageMarginSections.held(720, 40, true)).as("wider, nothing reaching over it").isEqualTo(800);
        assertThat(DocxPageMarginSections.held(720, 20, true)).as("narrower, under a header").isEqualTo(-400);
        assertThat(DocxPageMarginSections.held(720, 20, false)).as("narrower, with none to reach over it")
                .isEqualTo(400);
    }

    @Test
    void aLaterSectionsNarrowerTopMarginUnderAHeaderIsHeld() throws Exception {
        // Narrower than page 1's, the header could reach past it; held, the body stays at it.
        Exported exported = export(List.of(PageMarginRule.from(2, new DocumentInsets(20, 36, 36, 36))),
                session -> session.chrome().zone(DocumentPageZone.header(18,
                        page -> new ParagraphBuilder().text("Running").build())),
                page -> lines(page, "Line", 60));

        assertThat(exported.paragraphsEndingASection()).singleElement();
        assertThat(((java.math.BigInteger) exported.body().getPgMar().getTop()).longValue()).isNegative();
    }

    @Test
    void eachChangeOfMarginsStartsASectionOfItsOwn() throws Exception {
        DocumentInsets middle = new DocumentInsets(36, 36, 86, 36);
        Exported exported = export(List.of(PageMarginRule.page(2, middle), PageMarginRule.from(3, LATER)),
                page -> lines(page, "Line", 90));

        List<XWPFParagraph> ending = exported.paragraphsEndingASection();
        assertThat(ending).hasSize(2);
        assertThat(bottom(ending.get(0).getCTP().getPPr().getSectPr())).isEqualTo(36 * 20);
        CTSectPr second = ending.get(1).getCTP().getPPr().getSectPr();
        assertThat(bottom(second)).as("page 2's").isEqualTo(86 * 20);
        assertThat(second.isSetTitlePg()).isFalse();
        assertThat(bottom(exported.body())).as("page 3 on").isEqualTo(136 * 20);
    }

    @Test
    void aPageAfterOneStartingInsideABlockStartsTheSectionItsMarginsNeed() throws Exception {
        // Page 2 starts inside the paragraph, and Word sets it with page 1's margins; page 3, which
        // starts between paragraphs, has margins Word has not been given yet, and starts a section.
        Exported exported = export(List.of(PageMarginRule.from(2, LATER)), page -> {
            lines(page, "Intro", 18);
            page.addParagraph("Long " + "running on over the foot of the first page and onto the next one, ".repeat(6));
            lines(page, "Line", 60);
        });

        assertThat(exported.notes()).hasSize(2);
        assertThat(exported.notes().get(0)).startsWith("page 2's top and bottom margins differ from page 1's, which Word sets it "
                                                       + "with: page 2 starts inside this block");
        assertThat(exported.notes().get(1)).startsWith("page 3's top and bottom margins differ from page 1's: written as a Word "
                                                       + "section that starts where page 3 does");
        assertThat(exported.paragraphsEndingASection()).singleElement().satisfies(paragraph ->
                assertThat(bottom(paragraph.getCTP().getPPr().getSectPr())).isEqualTo(36 * 20));
        assertThat(bottom(exported.body())).isEqualTo(136 * 20);
    }

    @Test
    void aSectionOfADocumentWhoseFooterCountsItsPagesKeepsOneSetOfMargins() throws Exception {
        // Word counts a section's own pages: two Word sections would count this one's apart.
        DocumentSession body = GraphCompose.document().pageSize(400, 400).margin(FIRST).create();
        body.pageMargins(List.of(PageMarginRule.from(2, LATER)));
        body.chrome().zone(DocumentPageZone.footer(30, page -> new com.demcha.compose.document.dsl.RowBuilder()
                .addParagraph(p -> p.text("Body")).flexSpacer().add(page.pageNumber()).add(page.pageTotal())
                .build()));
        body.pageFlow(page -> lines(page, "Line", 60));
        DocumentSession cover = GraphCompose.document().pageSize(400, 400).margin(FIRST).create();
        cover.pageFlow(page -> page.addParagraph("Cover"));
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] bytes;
        try (com.demcha.compose.document.api.MultiSectionDocument document = GraphCompose.documents()
                .section(cover).section(body).create()) {
            bytes = document.export(new DocxSemanticBackend(report::set));
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            assertThat(document.getParagraphs()).as("the cover's section ends; the body's does not split")
                    .filteredOn(p -> p.getCTP().isSetPPr() && p.getCTP().getPPr().isSetSectPr()).hasSize(1);
        }
        assertThat(report.get().notes()).filteredOn(note -> note.subject().equals("page margin")).singleElement()
                .satisfies(note -> assertThat(note.detail()).endsWith("a Word section would count its own pages, "
                                                                      + "where this section's header or footer counts all of them"));
    }

    @Test
    void theRoomForAClosingLineIsReckonedAgainstTheMarginsWordSetsTheLastPageWith() throws Exception {
        // Page 2, a section of its own, a table fills against a bottom margin 64pt over page 1's:
        // reckoned against page 1's, the room under the table took in those 64pt, and a closing line
        // written there would not fit and open a page of its own.
        DocumentInsets later = new DocumentInsets(36, 36, 100, 36);
        int filling = rowsFilling(List.of(PageMarginRule.from(1, later)), 1);
        Exported exported = export(List.of(PageMarginRule.from(2, later)), page -> {
            page.addParagraph("Cover");
            page.addPageBreak(b -> { });
            page.addTable(table -> rows(table, "Item", filling));
        });

        assertThat(exported.paragraphsEndingASection()).as("page 2 its own section").singleElement();
        assertThat(exported.report().notes()).anyMatch(note -> note.subject().equals("closing paragraph"));
    }

    @Test
    void theSeparatorBetweenTwoTablesEndsTheSectionAtTheirPageBreak() throws Exception {
        // Written while the second table is, it stands at the foot of the first's page: the end of
        // a section put before it, it would stand at the top of the next, and hold its space there.
        int filling = rowsFilling(List.of(), 1);
        Exported exported = export(List.of(PageMarginRule.from(2, LATER)), page -> page
                .addTable(table -> rows(table, "First", filling))
                .addTable(table -> rows(table, "Second", 5)));

        List<org.apache.poi.xwpf.usermodel.IBodyElement> body = exported.document().getBodyElements();
        assertThat(body.get(0)).isInstanceOf(org.apache.poi.xwpf.usermodel.XWPFTable.class);
        assertThat(body.get(1)).isInstanceOfSatisfying(XWPFParagraph.class, separator -> {
            assertThat(separator.getCTP().getPPr().isSetSectPr()).isTrue();
            assertThat(separator.getCTP().getPPr().isSetKeepNext()).as("kept with the next, it would follow it")
                    .isFalse();
        });
        assertThat(body.get(2)).isInstanceOf(org.apache.poi.xwpf.usermodel.XWPFTable.class);
    }

    @Test
    void aPageFullToItsFootUnderATableStartsNoSectionAfterIt() throws Exception {
        // The panel, a table in Word, ends at the page's bottom margin: a hairline after it, to end
        // a section in, would not fit, and open a page of its own before the section's own.
        Exported exported = export(List.of(PageMarginRule.from(2, LATER)), page -> {
            page.addSection(section -> section.fillColor(com.demcha.compose.document.style.DocumentColor.rgb(238, 242, 247))
                    .spacer(10, 400 - 36 - 36));
            lines(page, "Line", 10);
        });

        assertThat(exported.paragraphsEndingASection()).isEmpty();
        assertThat(exported.notes()).singleElement().asString().endsWith("page 1's content reaches its foot, where "
                + "the paragraph that would end a Word section before page 2 would open a page of its own");
        assertThat(exported.report().notes()).filteredOn(note -> note.subject().equals("page margin"))
                .extracting(DocxExportReport.Note::path).containsExactly("page 2");
    }

    @Test
    void theSeparatorBetweenTwoTablesNeedsRoomOnItsPageToo() throws Exception {
        // The hairline between the panel, a table in Word, and the table after it ends the section
        // where the panel ends at the page's foot: Word would move it, and the section, a page on.
        Exported exported = export(List.of(PageMarginRule.from(2, LATER)), page -> {
            page.addSection(section -> section.fillColor(com.demcha.compose.document.style.DocumentColor.rgb(238, 242, 247))
                    .spacer(10, 400 - 36 - 36));
            page.addTable(table -> rows(table, "Item", 5));
        });

        assertThat(exported.paragraphsEndingASection()).isEmpty();
        assertThat(exported.notes()).singleElement().asString().contains("page 1's content reaches its foot");
    }

    @Test
    void theLineHoldingTheSpaceAboveACardMovedOntoThePageStaysOnIt() throws Exception {
        // The card the layout moves onto page 2 after a table holds its 12pt margin in a line above
        // it on that page. Ending the section, that line stood at the foot of page 1 instead.
        int filling = rowsFilling(List.of(), 1);
        Exported exported = export(List.of(PageMarginRule.from(2, LATER)), page -> {
            page.addTable(table -> rows(table, "Item", filling - 3));
            page.addSection(card -> card.fillColor(com.demcha.compose.document.style.DocumentColor.rgb(238, 242, 247))
                    .keepTogether().margin(new DocumentInsets(12, 0, 0, 0))
                    .addParagraph("Card one").addParagraph("Card two").addParagraph("Card three")
                    .addParagraph("Card four").addParagraph("Card five").addParagraph("Card six"));
        });

        List<XWPFParagraph> ending = exported.paragraphsEndingASection();
        assertThat(ending).singleElement().satisfies(paragraph -> assertThat(((java.math.BigInteger) paragraph.getCTP()
                .getPPr().getSpacing().getLine()).longValue()).as("a hairline of its own").isEqualTo(2));
        List<org.apache.poi.xwpf.usermodel.IBodyElement> body = exported.document().getBodyElements();
        int at = body.indexOf(ending.get(0));
        assertThat(body.get(at + 1)).as("the held line, on page 2").isInstanceOfSatisfying(XWPFParagraph.class,
                held -> assertThat(((java.math.BigInteger) held.getCTP().getPPr().getSpacing().getLine()).longValue())
                        .isEqualTo(12 * 20));
    }

    @Test
    void otherSideMarginsFromAPageOnAreNamedNotWritten() throws Exception {
        // The body is written against the document's width — a rule's length, a panel's room —
        // and Word sets its text between the section's side margins: written, they would set a
        // full-width rule 108pt short. Named once for the pages that share them.
        Exported exported = export(List.of(PageMarginRule.from(2, DocumentInsets.symmetric(36, 90))),
                page -> lines(page, "Line", 60));

        assertThat(exported.paragraphsEndingASection()).as("its top and bottom are page 1's").isEmpty();
        assertThat(exported.body().getPgMar().getLeft()).isEqualTo(java.math.BigInteger.valueOf(36 * 20));
        assertThat(exported.lastNames()).as("three pages").hasSize(3);
        assertThat(exported.notes()).singleElement().asString().isEqualTo("the side margins of pages 2 and 3 differ "
                + "from the document's and are not written: the body is written against the document's width, and "
                + "Word sets its text between the document's side margins");
    }

    @Test
    void aPageWithOtherTopAndBottomAndSideMarginsWritesTheFirstAndNamesTheSides() throws Exception {
        Exported exported = export(List.of(PageMarginRule.from(2, new DocumentInsets(36, 90, 136, 90))),
                page -> lines(page, "Line", 60));

        assertThat(exported.paragraphsEndingASection()).singleElement();
        assertThat(bottom(exported.body())).isEqualTo(136 * 20);
        assertThat(exported.body().getPgMar().getRight()).isEqualTo(java.math.BigInteger.valueOf(36 * 20));
        assertThat(exported.notes()).hasSize(2);
    }

    @Test
    void aSectionOfADocumentWhoseTextFooterCountsItsPagesKeepsOneSetOfMargins() throws Exception {
        DocumentSession body = GraphCompose.document().pageSize(400, 400).margin(FIRST).create();
        body.pageMargins(List.of(PageMarginRule.from(2, LATER)));
        body.footer(com.demcha.compose.document.output.DocumentHeaderFooter.builder()
                .rightText("{page} of {pages}").build());
        body.pageFlow(page -> lines(page, "Line", 60));
        List<String> notes = sectionedNotes(body);

        assertThat(notes).singleElement().asString().as("each page it would set otherwise")
                .startsWith("the top and bottom margins of pages 2 to ")
                .endsWith("which Word sets them with: a Word section would count its own pages, where this "
                          + "section's header or footer counts all of them");
    }

    @Test
    void aLaterPartOfASectionOfSeveralCountsItsPagesOnFromTheFirst() throws Exception {
        // Each section of a document of several counts its pages from 1: the part a page with other
        // margins starts counts on.
        DocumentSession body = GraphCompose.document().pageSize(400, 400).margin(FIRST).create();
        body.pageMargins(List.of(PageMarginRule.from(2, LATER)));
        body.pageFlow(page -> lines(page, "Line", 60));
        DocumentSession cover = GraphCompose.document().pageSize(400, 400).margin(FIRST).create();
        cover.pageFlow(page -> page.addParagraph("Cover"));
        byte[] bytes;
        try (com.demcha.compose.document.api.MultiSectionDocument document = GraphCompose.documents()
                .section(cover).section(body).create()) {
            bytes = document.toDocxBytes();
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            List<CTSectPr> ending = document.getParagraphs().stream()
                    .filter(p -> p.getCTP().isSetPPr() && p.getCTP().getPPr().isSetSectPr())
                    .map(p -> p.getCTP().getPPr().getSectPr()).toList();
            assertThat(ending).as("the cover's, then the body's first part").hasSize(2);
            assertThat(ending.get(1).getPgNumType().isSetStart()).as("the body counts from 1").isTrue();
            CTSectPr later = document.getDocument().getBody().getSectPr();
            assertThat(later.isSetPgNumType() && later.getPgNumType().isSetStart()).as("and on").isFalse();
        }
    }

    private static List<String> sectionedNotes(DocumentSession body) throws Exception {
        DocumentSession cover = GraphCompose.document().pageSize(400, 400).margin(FIRST).create();
        cover.pageFlow(page -> page.addParagraph("Cover"));
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (com.demcha.compose.document.api.MultiSectionDocument document = GraphCompose.documents()
                .section(cover).section(body).create()) {
            document.export(new DocxSemanticBackend(report::set));
        }
        return report.get().notes().stream().filter(note -> note.subject().equals("page margin"))
                .map(DocxExportReport.Note::detail).toList();
    }

    @Test
    void theSpaceAboveAPictureOpeningAPageIsReckonedFromThatPagesTopMargin() throws Exception {
        // A picture the layout moves onto a page holds the space the page leaves above it there,
        // read from where the layout places it. Page 2's top margin is 26pt under page 1's: read from
        // page 1's, the space above the picture came to less than none, and its 20pt margin was lost.
        Exported exported = export(List.of(PageMarginRule.from(2, new DocumentInsets(10, 36, 36, 36))), page -> {
            lines(page, "Line", 24);
            page.addImage(image -> image.source(png()).size(40, 40).margin(new DocumentInsets(20, 0, 0, 0)));
        });

        List<XWPFParagraph> paragraphs = exported.document().getParagraphs();
        int picture = paragraphs.indexOf(paragraphs.stream()
                .filter(paragraph -> !paragraph.getRuns().isEmpty() && !paragraph.getRuns().get(0)
                        .getEmbeddedPictures().isEmpty()).findFirst().orElseThrow());
        var held = paragraphs.get(picture - 1).getCTP().getPPr().getSpacing();
        assertThat(paragraphs.get(picture - 1).getText()).as("a line held above it").isEmpty();
        assertThat(((java.math.BigInteger) held.getLine()).longValue()).isEqualTo(20 * 20);
    }

    private static byte[] png() {
        try (java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(16, 16,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes);
            return bytes.toByteArray();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static void rows(com.demcha.compose.document.dsl.TableBuilder table, String text, int count) {
        table.columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(100));
        for (int row = 0; row < count; row++) {
            table.row(text + " " + row, "$" + row);
        }
    }

    /** Rows "Item 0" on under a header row the layout repeats on each page. */
    private static void itemsUnderAHeader(com.demcha.compose.document.dsl.TableBuilder table, int count) {
        table.columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(100));
        table.header("Item", "Amount").repeatHeader();
        for (int row = 0; row < count; row++) {
            table.row("Item " + row, "$" + row);
        }
    }

    /** How tall the layout sets a row of {@link #rows}. */
    private static double rowHeight() throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400).margin(FIRST).create()) {
            session.pageFlow(page -> page.addTable(table -> rows(table, "Item", 1)));
            return session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof TableRowFragmentPayload)
                    .findFirst().orElseThrow().height();
        }
    }

    /** The most rows one table holds on a page, laid out alone. */
    private static int rowsFilling(List<PageMarginRule> rules, int pages) throws Exception {
        int rows = 1;
        while (pagesOf(rules, rows + 1) == pages) {
            rows++;
        }
        return rows;
    }

    private static int pagesOf(List<PageMarginRule> rules, int rows) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400).margin(FIRST).create()) {
            session.pageMargins(rules);
            session.pageFlow(page -> page.addTable(table -> {
                table.columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(100));
                for (int row = 0; row < rows; row++) {
                    table.row("Item " + row, "$" + row);
                }
            }));
            return session.layoutGraph().totalPages();
        }
    }

    private static void lines(PageFlowBuilder page, String text, int count) {
        for (int line = 0; line < count; line++) {
            int at = line;
            page.addParagraph(p -> p.name(text + "_" + at).text(text + " " + at));
        }
    }

    private static long bottom(CTSectPr section) {
        return ((java.math.BigInteger) section.getPgMar().getBottom()).longValue();
    }

    private static Exported export(List<PageMarginRule> rules, Consumer<PageFlowBuilder> content) throws Exception {
        return export(rules, session -> { }, content);
    }

    private static Exported export(List<PageMarginRule> rules, Consumer<DocumentSession> chrome,
                                   Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400).margin(FIRST).create()) {
            session.pageMargins(rules);
            chrome.accept(session);
            session.pageFlow(content::accept);
            byte[] bytes = session.export(new DocxSemanticBackend(report::set));
            List<String> lastLines = new ArrayList<>();
            List<Long> rows = new ArrayList<>();
            for (int page = 0; page < session.layoutGraph().totalPages(); page++) {
                int at = page;
                lastLines.add(session.layoutGraph().nodes().stream()
                        .filter(node -> node.nodeKind().equals("ParagraphNode") && node.endPage() == at)
                        .reduce((before, after) -> after)
                        .map(node -> node.semanticName())
                        .orElse(""));
                rows.add(session.layoutGraph().fragments().stream()
                        .filter(fragment -> fragment.pageIndex() == at
                                            && fragment.payload() instanceof TableRowFragmentPayload)
                        .count());
            }
            return new Exported(new XWPFDocument(new ByteArrayInputStream(bytes)), report.get(), lastLines, rows);
        }
    }

    private record Exported(XWPFDocument document, DocxExportReport report, List<String> lastNames, List<Long> rows) {

        /** How many table rows the layout sets on a page, a header it repeats there included. */
        int rowsOn(int page) {
            return rows.get(page).intValue();
        }

        CTSectPr body() {
            return document.getDocument().getBody().getSectPr();
        }

        List<XWPFParagraph> paragraphsEndingASection() {
            return document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetSectPr())
                    .toList();
        }

        /** The text of the last paragraph the layout ends on a page: each is named for its text. */
        String lastLineOf(int page) {
            return lastNames.get(page).replace('_', ' ');
        }

        List<String> notes() {
            return report.notes().stream().filter(note -> note.subject().equals("page margin"))
                    .map(DocxExportReport.Note::detail).toList();
        }
    }
}
