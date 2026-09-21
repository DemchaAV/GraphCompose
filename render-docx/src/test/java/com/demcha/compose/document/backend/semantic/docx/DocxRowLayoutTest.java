package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The one-row table a {@code row(...)} is carried as.
 *
 * <p>A row is a layout device rather than something the author asked to see ruled, so the
 * carrier's own grid is turned off. What this pins is that it is turned off <em>once</em>:
 * POI's {@code createTable} already writes a full set of single-line borders, and adding a
 * second element per edge on top of them leaves a {@code w:tblBorders} that
 * {@code CT_TblBorders} does not allow. Word reads the last one and draws nothing, so the
 * render looked correct while the part was invalid.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxRowLayoutTest {

    @Test
    void theCarriersGridIsTurnedOffWithOneElementPerEdge() throws Exception {
        XWPFTable table = onlyTable(page -> page.addRow(r -> r
                .addParagraph(p -> p.text("Left"))
                .addParagraph(p -> p.text("Right"))));

        String borders = table.getCTTbl().getTblPr().getTblBorders().xmlText();
        for (String edge : new String[] {"top", "bottom", "left", "right", "insideH", "insideV"}) {
            assertThat(occurrences(borders, "<w:" + edge + " "))
                    .as("one w:%s, not POI's default plus ours", edge)
                    .isEqualTo(1);
        }
        assertThat(borders)
                .as("and the one that survives is the one that hides the grid")
                .doesNotContain("single");
    }

    private static int occurrences(String haystack, String needle) {
        int count = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + 1)) {
            count++;
        }
        return count;
    }

    private static XWPFTable onlyTable(Consumer<PageFlowBuilder> content) throws Exception {
        byte[] docx;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 600)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            docx = session.export(new DocxSemanticBackend());
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getTables()).hasSize(1);
            return document.getTables().get(0);
        }
    }
}
