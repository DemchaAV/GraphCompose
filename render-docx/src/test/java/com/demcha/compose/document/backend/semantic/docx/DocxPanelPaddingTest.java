package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A shape written as a panel in a table cell keeps its padding round its outline.
 *
 * <p>The page sets a shape's padding round its outline and its layers in the outline itself.
 * Measured in Word 16 and LibreOffice on Windows, with the padding written as the panel cell's
 * margins: a 10pt padding stood the fill 10pt left of the page's and a label at its top-left 9pt
 * low, and a 20pt left padding stood a label at its bottom-right 20pt left. Written round the
 * panel's table — its indent and the space above it — fill and text stood where the page sets
 * them, in both, where its cell sets it at the top or at the middle. Both editors set a cell's
 * content at its middle or foot without the space below a table that ends it, which is named.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPanelPaddingTest {

    private static final DocumentColor LIGHT = DocumentColor.rgb(220, 230, 245);

    @Test
    void thePaddingStandsRoundTheOutlineNotInsideIt() throws Exception {
        Exported exported = export(panel(new DocumentInsets(0, 0, 0, 20)), null);

        assertThat(exported.panelIndent()).as("the table 20pt in").isEqualTo(400L);
        assertThat(exported.panelMargins()).as("its cell's text at the outline").containsOnly(0L);
        assertThat(exported.panelNotes()).isEmpty();
    }

    @Test
    void aCentredCellsPanelGivesUpTheSpaceBelowFromTheSpaceAbove() throws Exception {
        Exported exported = export(panel(DocumentInsets.of(10)), null);

        assertThat(exported.spaceAbove()).as("10pt above, less the 10pt below Word does not count").isZero();
        assertThat(exported.spaceBelow()).as("still written below the table").isEqualTo(200L);
        assertThat(exported.panelNotes()).isEmpty();
    }

    @Test
    void aTopCellsPanelKeepsTheSpaceAboveItsMarginAndPaddingTogether() throws Exception {
        DocumentNode panel = new ShapeContainerBuilder().rectangle(100, 60).fillColor(LIGHT)
                .padding(DocumentInsets.of(6)).margin(DocumentInsets.top(4))
                .layer(new ParagraphBuilder().text("Label").build(), LayerAlign.TOP_LEFT).build();
        Exported exported = export(panel, DocumentTableTextAnchor.TOP_LEFT);

        assertThat(exported.spaceAbove()).isEqualTo(200L);
        assertThat(exported.panelNotes()).isEmpty();
    }

    @Test
    void aCentredCellsPanelWithMoreSpaceBelowIsNamed() throws Exception {
        Exported exported = export(panel(new DocumentInsets(4, 0, 16, 0)), null);

        assertThat(exported.spaceAbove()).isZero();
        assertThat(exported.panelNotes()).singleElement().extracting(DocxExportReport.Note::detail)
                .isEqualTo("its table cell sets what it holds at its middle, and Word does not count the space below "
                           + "its table there: it stands 6pt lower than the page sets it");
    }

    @Test
    void aPanelEndingWhatItsCellHoldsIsSetDownToo() throws Exception {
        // A section that writes a heading and then the panel ends with the panel's table.
        Exported exported = export(new SectionBuilder().addParagraph("Head").add(panel(DocumentInsets.of(10))).build(), null);
        Exported unpadded = export(new SectionBuilder().addParagraph("Head").add(panel(DocumentInsets.zero())).build(), null);

        assertThat(exported.spaceAbove()).as("given up to the space below: the heading's own, as with no padding")
                .isEqualTo(unpadded.spaceAbove());
        assertThat(exported.panelNotes()).isEmpty();
    }

    @Test
    void aPanelSomethingFollowsKeepsTheSpaceAbove() throws Exception {
        // The space below the table is the next paragraph's, which the cell counts.
        DocumentNode section = new SectionBuilder().add(panel(DocumentInsets.of(10))).addParagraph("Tail").build();
        Exported exported = export(section, null);

        assertThat(exported.spaceAbove()).isEqualTo(200L);
        assertThat(exported.panelNotes()).isEmpty();
    }

    @Test
    void aPanelInARowTheLayoutDoesNotHoldKeepsTheSpaceAbove() throws Exception {
        // A table composed in a cell has no rows of its own in the layout: Word sets them as tall
        // as what they hold.
        DocumentNode inner = new TableBuilder().columns(DocumentTableColumn.fixed(120))
                .rowCells(DocumentTableCell.node(panel(DocumentInsets.of(10)))).build();
        Exported exported = export(inner, null);
        XWPFTableCell innerCell = exported.outerCell().getTables().get(0).getRow(0).getCell(0);

        XWPFTable panel = innerCell.getTables().get(0);
        assertThat(spaceAbove(innerCell, panel)).isEqualTo(200L);
        assertThat(exported.panelNotes()).isEmpty();
    }

    @Test
    void aShapeWiderThanItsCellLessItsPaddingIsHeldInTheCell() throws Exception {
        // A 110pt column leaves 102pt of text; the 10pt padding stands left of the outline, which
        // is held to the 92pt left beside it rather than run past the cell, which Word widens for.
        DocumentNode panel = new ShapeContainerBuilder().rectangle(100, 60).fillColor(LIGHT)
                .padding(new DocumentInsets(0, 0, 0, 10))
                .layer(new ParagraphBuilder().text("Label").build(), LayerAlign.TOP_LEFT).build();
        Exported exported = export(panel, null, 110);

        assertThat(exported.panelIndent()).isEqualTo(200L);
        assertThat(DocxTwips.of(exported.panel().getCTTbl().getTblPr().getTblW().getW()))
                .as("no wider than the cell's text less the padding").isLessThanOrEqualTo(Math.round((102 - 10) * 20.0));
    }

    @Test
    void aFootCellsPanelIsNamed() throws Exception {
        Exported exported = export(panel(DocumentInsets.of(10)), DocumentTableTextAnchor.BOTTOM_LEFT);

        assertThat(exported.panelNotes()).singleElement().extracting(DocxExportReport.Note::detail)
                .isEqualTo("its table cell sets what it holds at its foot, and Word does not count the space below "
                           + "its table there: it stands 10pt lower than the page sets it");
    }

    @Test
    void theDefaultAnchorIsTheFoot() throws Exception {
        Exported exported = export(panel(DocumentInsets.of(10)), DocumentTableTextAnchor.DEFAULT);

        assertThat(exported.panelNotes()).singleElement().extracting(DocxExportReport.Note::detail).asString()
                .startsWith("its table cell sets what it holds at its foot");
    }

    private static DocumentNode panel(DocumentInsets padding) {
        return new ShapeContainerBuilder().rectangle(100, 60).fillColor(LIGHT).padding(padding)
                .layer(new ParagraphBuilder().text("Label").build(), LayerAlign.TOP_LEFT).build();
    }

    /** The space the paragraph before a cell's table holds above it, in twips; 0 with none before it. */
    private static long spaceAbove(XWPFTableCell cell, XWPFTable table) {
        List<IBodyElement> elements = cell.getBodyElements();
        int index = elements.indexOf(table);
        if (index < 1 || !(elements.get(index - 1) instanceof XWPFParagraph before)) {
            return 0;
        }
        return Math.max(0, before.getSpacingBefore()) + Math.max(0, before.getSpacingAfter());
    }

    private record Exported(XWPFDocument document, List<DocxExportReport.Note> notes) {

        XWPFTableCell outerCell() {
            return document.getTables().get(0).getRow(0).getCell(0);
        }

        XWPFTable panel() {
            return outerCell().getTables().get(0);
        }

        long panelIndent() {
            CTTblPr properties = panel().getCTTbl().getTblPr();
            return properties.isSetTblInd() ? DocxTwips.of(properties.getTblInd().getW()) : 0L;
        }

        List<Long> panelMargins() {
            CTTcMar margins = panel().getRow(0).getCell(0).getCTTc().getTcPr().getTcMar();
            return List.of(DocxTwips.of(margins.getTop().getW()), DocxTwips.of(margins.getLeft().getW()),
                    DocxTwips.of(margins.getBottom().getW()), DocxTwips.of(margins.getRight().getW()));
        }

        long spaceAbove() {
            return DocxPanelPaddingTest.spaceAbove(outerCell(), panel());
        }

        /** The space the paragraph closing the cell after the panel's table holds below it, in twips. */
        long spaceBelow() {
            List<IBodyElement> elements = outerCell().getBodyElements();
            int index = elements.indexOf(panel());
            assertThat(elements.get(index + 1)).as("the paragraph after the table").isInstanceOf(XWPFParagraph.class);
            return Math.max(0, ((XWPFParagraph) elements.get(index + 1)).getSpacingAfter());
        }

        List<DocxExportReport.Note> panelNotes() {
            return notes.stream().filter(note -> note.subject().equals("ShapeContainerNode")).toList();
        }
    }

    private static Exported export(DocumentNode content, DocumentTableTextAnchor anchor) throws Exception {
        return export(content, anchor, 140);
    }

    private static Exported export(DocumentNode content, DocumentTableTextAnchor anchor, double column)
            throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(72)).create()) {
            session.pageFlow(page -> page
                    .addParagraph("Before")
                    .addTable(table -> {
                        table.columns(DocumentTableColumn.fixed(column), DocumentTableColumn.fixed(100));
                        if (anchor != null) {
                            table.defaultCellStyle(DocumentTableStyle.builder().textAnchor(anchor).build());
                        }
                        table.rowCells(DocumentTableCell.node(content),
                                DocumentTableCell.node(new ParagraphBuilder().text("Beside").build()));
                    })
                    .addParagraph("After"));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        assertThat(report.get().notes()).noneMatch(note -> note.subject().equals("measured geometry"));
        return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get().notes());
    }
}
