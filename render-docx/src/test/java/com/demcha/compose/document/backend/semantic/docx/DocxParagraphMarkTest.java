package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A paragraph whose line height the editor sets sizes its mark as its text: the mark is a
 * character on the last line and counts towards its height.
 *
 * @author Artem Demchyshyn
 */
class DocxParagraphMarkTest {

    @Test
    void aHairlineOfTextInACellIsNotAsTallAsBodyText() throws Exception {
        // A heading rule drawn as a filled cell holding half-point text: with the mark at the
        // document's size, the cell came out a line of body text tall.
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(120))
                .rowCells(DocumentTableCell.node(new ParagraphBuilder().name("Rule").text(" ").lineSpacing(0)
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(0.5)).build())
                        .withStyle(DocumentTableStyle.builder().padding(DocumentInsets.zero())
                                .fillColor(DocumentColor.rgb(194, 96, 72)).build()))))) {
            XWPFParagraph rule = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);
            CTPPr properties = rule.getCTP().getPPr();

            assertThat(properties.getRPr().getSzList()).singleElement()
                    .satisfies(size -> assertThat(((Number) size.getVal()).intValue()).isEqualTo(1));
        }
    }

    @Test
    void aLineWrittenAtAnExactHeightLeavesItsMarkAlone() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Body"))
                .addParagraph(p -> p.text("Small print").textStyle(DocumentTextStyle.DEFAULT.withSize(6))))) {
            CTPPr small = document.getParagraphs().get(1).getCTP().getPPr();

            assertThat(small.getSpacing().getLineRule()).hasToString("exact");
            assertThat(small.isSetRPr()).isFalse();
        }
    }

    private static XWPFDocument export(java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content)
            throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            return new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
        }
    }
}
