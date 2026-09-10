package com.demcha.compose.document.backend;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.layout.LayoutAnchorId;
import com.demcha.compose.document.layout.LayoutAnchorNode;
import com.demcha.compose.document.node.AlignNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A wrapper that says something about geometry must not take its content with it.
 *
 * <p>{@code DocxSemanticBackend} walks the semantic tree and writes what it recognises. A
 * node it does not recognise is not written — and if that node is a wrapper, everything
 * below it goes too. That is what happened when timelines started anchoring their entries:
 * the export succeeded, produced a well-formed document, and lost every word in it.</p>
 *
 * <p>A layout anchor reports where its child landed; an alignment says where in the
 * available width to put it. Word lays text out itself, so neither survives as geometry —
 * but both have exactly one child, and the child is the document. These cases are written
 * against the wrappers rather than against a timeline, because the contract is the
 * backend's: anything transparent stays transparent, whoever put it there.</p>
 */
class DocxTransparentWrapperTest {

    private enum Kind { PROBE }

    @Test
    void anAnchorAtDocumentLevelKeepsItsParagraph() {
        assertThat(exported(flow -> flow.add(anchor(paragraph("anchored at the top level")))))
                .contains("anchored at the top level");
    }

    @Test
    void anAlignAtDocumentLevelKeepsItsParagraph() {
        assertThat(exported(flow -> flow.add(
                new AlignNode(paragraph("aligned at the top level"), HorizontalAlign.CENTER))))
                .contains("aligned at the top level");
    }

    @Test
    void bothNestedTogetherKeepTheParagraph() {
        assertThat(exported(flow -> flow.add(new AlignNode(
                anchor(paragraph("aligned and anchored")), HorizontalAlign.CENTER))))
                .contains("aligned and anchored");
    }

    @Test
    void aRowCellKeepsWhatTheWrappersInsideItHold() {
        // The second walker. A row exports as a one-row table, and its cells have their own
        // traversal — a wrapper handled at document level and missed here loses the subtree
        // just as completely, which is exactly the shape a timeline's marker column has.
        assertThat(exported(flow -> flow.addRow(row -> {
            row.addSection(cell -> cell.add(
                    new AlignNode(anchor(paragraph("inside a cell")), HorizontalAlign.CENTER)));
            row.addParagraph("beside it");
        })))
                .contains("inside a cell")
                .contains("beside it");
    }

    private static DocumentNode anchor(DocumentNode child) {
        return new LayoutAnchorNode("", new LayoutAnchorId(new Object(), Kind.PROBE, 0), child);
    }

    private static DocumentNode paragraph(String text) {
        return new ParagraphBuilder().text(text).build();
    }

    private static String exported(Consumer<PageFlowBuilder> content) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 260).margin(DocumentInsets.of(20)).create()) {
            PageFlowBuilder flow = session.pageFlow();
            content.accept(flow);
            flow.build();
            byte[] docx = session.export(new DocxSemanticBackend());
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                return extractor.getText();
            }
        } catch (Exception failure) {
            throw new IllegalStateException("export failed", failure);
        }
    }
}
