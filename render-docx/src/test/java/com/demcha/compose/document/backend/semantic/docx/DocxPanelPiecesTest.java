package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.style.DocumentColor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A panel a page break runs through is cut into tables at the break, wherever among the sections
 * and panels its cell writes the break is.
 *
 * @author Artem Demchyshyn
 */
class DocxPanelPiecesTest {

    private static final Predicate<DocumentNode> CONTAINERS =
            node -> node instanceof SectionNode || node instanceof ContainerNode;
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 242, 247);

    @Test
    void aBreakAmongAPanelsChildrenCutsItInTwo() throws Exception {
        DocumentNode card = card(page -> page.addSection(section -> section.fillColor(SURFACE)
                .addParagraph("One").addPageBreak(b -> { }).addParagraph("Two")));

        List<List<DocxPanelPieces.Part>> pieces = DocxPanelPieces.of(card, CONTAINERS);

        assertThat(pieces).hasSize(2);
        assertThat(texts(pieces.get(0))).containsExactly("One");
        assertThat(texts(pieces.get(1))).containsExactly("Two");
        assertThat(pieces.get(1).get(0).isWhole()).isTrue();
        assertThat(DocxPanelPieces.followsABreak(pieces.get(1), false)).as("right after the break").isTrue();
        assertThat(DocxPanelPieces.followsABreak(pieces.get(0), true)).isFalse();
    }

    @Test
    void aBreakInsideASectionOrAPanelInsideCutsThemWithIt() throws Exception {
        DocumentNode card = card(page -> page.addSection(section -> section.fillColor(SURFACE)
                .addParagraph("Head")
                .addSection(mid -> mid.addSection(in -> in.fillColor(SURFACE)
                        .addParagraph("One").addPageBreak(b -> { }).addParagraph("Two")))
                .addParagraph("Tail")));

        List<List<DocxPanelPieces.Part>> pieces = DocxPanelPieces.of(card, CONTAINERS);

        assertThat(pieces).hasSize(2);
        DocxPanelPieces.Part mid = pieces.get(0).get(1);
        assertThat(mid.isWhole()).isFalse();
        assertThat(mid.opens()).isTrue();
        assertThat(mid.closes()).isFalse();
        DocxPanelPieces.Part in = mid.parts().get(0);
        assertThat(texts(in.parts())).containsExactly("One");
        DocxPanelPieces.Part midAfter = pieces.get(1).get(0);
        assertThat(midAfter.node()).isSameAs(mid.node());
        assertThat(midAfter.opens()).isFalse();
        assertThat(midAfter.closes()).isTrue();
        assertThat(texts(midAfter.parts().get(0).parts())).containsExactly("Two");
        assertThat(pieces.get(1).get(1).node()).isInstanceOf(ParagraphNode.class);
        assertThat(DocxPanelPieces.followsABreak(pieces.get(1), false))
                .as("going on from a break inside its first child, not among its own").isFalse();
        assertThat(DocxPanelPieces.followsABreak(midAfter.parts().get(0).parts(), false)).isTrue();
    }

    @Test
    void aBreakInANodeItsCellDoesNotWriteAsAContainerIsNotCut() throws Exception {
        DocumentNode card = card(page -> page.addSection(section -> section.fillColor(SURFACE)
                .addSection(in -> in.addParagraph("One").addPageBreak(b -> { }).addParagraph("Two"))));

        List<List<DocxPanelPieces.Part>> pieces = DocxPanelPieces.of(card, node -> node == card);

        assertThat(pieces).as("its section written some other way, whole").hasSize(1);
        assertThat(pieces.get(0).get(0).isWhole()).isTrue();
        assertThat(DocxPanelPieces.of(card, CONTAINERS)).as("cut through its section").hasSize(2);
    }

    @Test
    void aBreakEndingAPanelLeavesAnEmptyPieceAfterIt() throws Exception {
        DocumentNode card = card(page -> page.addSection(section -> section.fillColor(SURFACE)
                .addParagraph("One").addPageBreak(b -> { })));

        List<List<DocxPanelPieces.Part>> pieces = DocxPanelPieces.of(card, CONTAINERS);

        assertThat(pieces).hasSize(2);
        assertThat(pieces.get(1)).isEmpty();
        assertThat(DocxPanelPieces.followsABreak(pieces.get(1), false)).isFalse();
    }

    /** The first block of a page flow built by {@code content}. */
    private static DocumentNode card(java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content)
            throws Exception {
        return DocxExports.graphOf(content).roots().get(0).children().get(0);
    }

    private static List<String> texts(List<DocxPanelPieces.Part> parts) {
        return parts.stream().map(DocxPanelPieces.Part::node)
                .map(node -> node instanceof ParagraphNode paragraph ? paragraph.text() : node.nodeKind())
                .toList();
    }
}
