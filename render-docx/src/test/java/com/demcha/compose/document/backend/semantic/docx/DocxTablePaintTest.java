package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cell fill and borders in the DOCX semantic backend.
 *
 * <p>A {@code DocumentTableStyle} carries a {@code fillColor} and a {@code stroke}, and neither
 * reached the file: a zebra body, a header band and a ruled grid all exported on Word's
 * defaults, which is to say with no fill and no borders. Word owns both — {@code w:shd} and
 * {@code w:tcBorders} — so what these pin is a mapping, not an approximation.</p>
 */
class DocxTablePaintTest {

    private static final DocumentColor BAND = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor RULE = DocumentColor.rgb(180, 40, 40);

    @Test
    void aCellFillReachesWordAsShading() throws Exception {
        XWPFTable table = firstTable(new TableBuilder()
                .name("Filled")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("banded")
                                .withStyle(DocumentTableStyle.builder().fillColor(BAND).build()),
                        DocumentTableCell.text("plain"))
                .build());

        assertThat(shadingFill(table.getRow(0).getCell(0))).isEqualToIgnoringCase("14505F");
        // A cell nothing painted keeps Word's default rather than being filled with black.
        assertThat(table.getRow(0).getCell(1).getCTTc().getTcPr()).isNull();
    }

    @Test
    void aStrokeBecomesFourBordersInEighthsOfAPoint() throws Exception {
        XWPFTable table = firstTable(new TableBuilder()
                .name("Ruled")
                .columns(DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("ruled").withStyle(DocumentTableStyle.builder()
                        .stroke(new DocumentStroke(RULE, 1.0)).build()))
                .build());

        CTTcBorders borders = table.getRow(0).getCell(0).getCTTc().getTcPr().getTcBorders();
        assertThat(borders).isNotNull();
        for (var edge : List.of(borders.getTop(), borders.getBottom(),
                borders.getLeft(), borders.getRight())) {
            assertThat(edge.getVal()).isEqualTo(STBorder.SINGLE);
            assertThat(edge.getSz()).isEqualTo(BigInteger.valueOf(8));
            assertThat(hex(edge.getColor())).isEqualToIgnoringCase("B42828");
        }
    }

    @Test
    void theCascadeResolvesPerFieldRatherThanPerStyle() throws Exception {
        // The table rules every cell; the row bands one. A per-object cascade would let the
        // row's style replace the table's outright and the border would vanish with it.
        XWPFTable table = firstTable(new TableBuilder()
                .name("Cascade")
                .columns(DocumentTableColumn.auto())
                .defaultCellStyle(DocumentTableStyle.builder()
                        .stroke(new DocumentStroke(RULE, 1.0)).build())
                .rowStyle(0, DocumentTableStyle.builder().fillColor(BAND).build())
                .rowCells(DocumentTableCell.text("both"))
                .build());

        XWPFTableCell cell = table.getRow(0).getCell(0);
        assertThat(shadingFill(cell)).isEqualToIgnoringCase("14505F");
        assertThat(cell.getCTTc().getTcPr().getTcBorders()).isNotNull();
    }

    @Test
    void aMergedCellIsPaintedOnEveryPositionItCovers() throws Exception {
        // A vMerge continuation cell draws its own shading, so leaving it unpainted would
        // stripe the merged region.
        XWPFTable table = firstTable(new TableBuilder()
                .name("MergedPaint")
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("tall")
                                .rowSpan(2)
                                .withStyle(DocumentTableStyle.builder().fillColor(BAND).build()),
                        DocumentTableCell.text("top"))
                .rowCells(DocumentTableCell.text("bottom"))
                .build());

        assertThat(shadingFill(table.getRow(0).getCell(0))).isEqualToIgnoringCase("14505F");
        assertThat(shadingFill(table.getRow(1).getCell(0))).isEqualToIgnoringCase("14505F");
    }

    @Test
    void aHairlineStaysALineRatherThanRoundingAway() throws Exception {
        XWPFTable table = firstTable(new TableBuilder()
                .name("Hairline")
                .columns(DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("thin").withStyle(DocumentTableStyle.builder()
                        .stroke(new DocumentStroke(RULE, 0.05)).build()))
                .build());

        // 0.05 pt is under an eighth; rounding it to zero would ask Word for a border of no
        // width, which is a border nobody sees.
        assertThat(table.getRow(0).getCell(0).getCTTc().getTcPr().getTcBorders().getTop().getSz())
                .isEqualTo(BigInteger.ONE);
    }

    @Test
    void aStrokeOfNoWidthSaysTheCellHasNoBorder() throws Exception {
        // DocumentStroke.of(colour, 0) is how this codebase says "no border", and a shipped
        // CV preset uses it. Writing nothing would leave the cell on the table grid POI
        // creates, so a deliberately borderless design would export ruled — the opposite of
        // what the fixed-layout backend draws from the same input.
        XWPFTable table = firstTable(new TableBuilder()
                .name("Borderless")
                .columns(DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("clean").withStyle(DocumentTableStyle.builder()
                        .stroke(new DocumentStroke(DocumentColor.WHITE, 0)).build()))
                .build());

        CTTcBorders borders = table.getRow(0).getCell(0).getCTTc().getTcPr().getTcBorders();
        assertThat(borders).isNotNull();
        for (var edge : List.of(borders.getTop(), borders.getBottom(),
                borders.getLeft(), borders.getRight())) {
            assertThat(edge.getVal()).isEqualTo(STBorder.NIL);
        }
    }

    private static String shadingFill(XWPFTableCell cell) {
        return hex(cell.getCTTc().getTcPr().getShd().getFill());
    }

    /** XmlBeans hands an ST_HexColor back as bytes, so read it as the colour it encodes. */
    private static String hex(Object value) {
        if (value instanceof byte[] bytes) {
            StringBuilder text = new StringBuilder();
            for (byte part : bytes) {
                text.append(String.format("%02X", part));
            }
            return text.toString();
        }
        return String.valueOf(value);
    }

    private static XWPFTable firstTable(TableNode node) throws Exception {
        byte[] docx;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            session.add(node);
            docx = session.export(new DocxSemanticBackend());
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            List<XWPFTable> tables = document.getTables();
            assertThat(tables).hasSize(1);
            return tables.get(0);
        }
    }
}
