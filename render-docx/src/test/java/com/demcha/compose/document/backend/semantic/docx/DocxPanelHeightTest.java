package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A panel keeps the height the page gives it, and a document ending in a table does not run on
 * to a page of its own.
 *
 * @author Artem Demchyshyn
 */
class DocxPanelHeightTest {

    @Test
    void aPanelTallerThanItsTextKeepsThePagesHeight() throws Exception {
        // A card whose inner row's padding, not its text, sets its height: Word closed it round
        // the text, as MerchantInvoice's due-date card.
        try (XWPFDocument document = export(page -> page.addSection("DueCard", card -> card
                .fillColor(DocumentColor.rgb(247, 249, 246))
                .addRow("DueRow", row -> row.padding(new DocumentInsets(20, 0, 20, 0))
                        .columns(com.demcha.compose.document.style.DocumentRowColumn.weight(1))
                        .addParagraph(p -> p.text("26 June 2026")))))) {
            XWPFTable panel = document.getTables().get(0);
            String row = panel.getRow(0).getCtRow().xmlText();

            java.util.regex.Matcher height = java.util.regex.Pattern
                    .compile("<w:trHeight w:val=\"(\\d+)\" w:hRule=\"atLeast\"/>").matcher(row);
            assertThat(height.find()).as(row).isTrue();
            assertThat(Integer.parseInt(height.group(1))).as("40pt of padding and a line of text")
                    .isGreaterThan(48 * 20);
        }
    }

    @Test
    void theEmptyParagraphClosingADocumentThatEndsInATableIsNotLaidOut() throws Exception {
        try (XWPFDocument document = export(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(120)).row("Last")))) {
            List<IBodyElement> body = document.getBodyElements();
            XWPFParagraph closing = (XWPFParagraph) body.get(body.size() - 1);

            assertThat(body.get(body.size() - 2)).isInstanceOf(XWPFTable.class);
            assertThat(closing.getCTP().getPPr().getRPr().sizeOfVanishArray()).isEqualTo(1);
        }
    }

    @Test
    void aDocumentEndingInTextKeepsItsLastParagraphVisible() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p.text("The end")))) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            XWPFParagraph last = paragraphs.get(paragraphs.size() - 1);

            assertThat(last.getCTP().isSetPPr() && last.getCTP().getPPr().isSetRPr()
                       && last.getCTP().getPPr().getRPr().sizeOfVanishArray() > 0).isFalse();
        }
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            return new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
        }
    }
}
