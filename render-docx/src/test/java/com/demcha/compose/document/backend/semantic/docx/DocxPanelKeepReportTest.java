package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A block the layout moves to a new page inside a panel that runs on there is named when Word
 * may break it there: Word breaks the panel's row where its own lines run out.
 *
 * <p>Measured in Word 16 and LibreOffice on Windows, inside a filled section the layout breaks
 * over a page:</p>
 * <ul>
 *   <li>a stack of three lines, or a section of three kept together, moved whole to the next page
 *       stood with two of its lines at the foot of the page above in both, its paragraphs told to
 *       keep with the next or not;</li>
 *   <li>a heading kept with the line under it, moved down with it, stood at the foot of the page
 *       above, the line on the next page;</li>
 *   <li>a filled section kept together, and a row holding a card, moved there whole, as the page
 *       sets them.</li>
 * </ul>
 *
 * @author Artem Demchyshyn
 */
class DocxPanelKeepReportTest {

    private static final String WHOLE = "the layout moves it to the next page whole, inside a panel that runs on there";
    private static final String WITH_NEXT = "the layout moves it to the next page with the block after it";

    @Test
    void aStackMovedToTheNextPageInsideAPanelIsNamed() throws Exception {
        assertThat(notes(inAPanel(10, panel -> panel.addLayerStack(stack -> stack.name("Entry").topLeft(threeLines())))))
                .singleElement().satisfies(note -> {
                    assertThat(note.path()).contains("Entry");
                    assertThat(note.detail()).startsWith(WHOLE);
                });
    }

    @Test
    void aBlockKeptTogetherMovedThereIsNamedToo() throws Exception {
        assertThat(notes(inAPanel(10, panel -> panel.addSection("Kept", kept -> kept.keepTogether()
                .addParagraph("Opening").addParagraph("Body").addParagraph("Tail")))))
                .singleElement().satisfies(note -> assertThat(note.path()).contains("Kept"));
    }

    @Test
    void aBandMovedThereIsNamedToo() throws Exception {
        assertThat(notes(inAPanel(10, panel -> panel.addLayerStack(stack -> stack.name("Band").topLeft(threeLines())
                .bottomRight(new ParagraphBuilder().text("Caption").build())))))
                .singleElement().satisfies(note -> assertThat(note.path()).contains("Band"));
    }

    @Test
    void aBlockKeptWithTheNextMovedThereIsNamed() throws Exception {
        // Ten lines leave 24pt: the heading and the line under it, 26pt, move down together.
        assertThat(notes(inAPanel(10, panel -> panel.addSection("Heading", heading -> heading.keepWithNext()
                .addParagraph("Opening")).addParagraph("Body"))))
                .singleElement().satisfies(note -> {
                    assertThat(note.path()).contains("Heading");
                    assertThat(note.detail()).startsWith(WITH_NEXT);
                });
    }

    @Test
    void aBlockOpeningAPanelInsideThePanelIsNamedOnce() throws Exception {
        // The inner panel is in the outer one's row, which breaks before the stack too; the stack
        // holds a section kept together, named with it, not again.
        assertThat(notes(inAPanel(10, panel -> panel.addSection(inner -> inner
                .fillColor(DocumentColor.rgb(250, 240, 230))
                .addLayerStack(stack -> stack.name("Entry").topLeft(new SectionBuilder().keepTogether()
                        .addParagraph("Opening").addParagraph("Body").addParagraph("Tail").build()))))))
                .singleElement().satisfies(note -> assertThat(note.path()).contains("Entry"));
    }

    @Test
    void aPanelKeptTogetherInsideThePanelIsNot() throws Exception {
        // Its own row may not split: Word moves it whole, a stack in it with it.
        assertThat(notes(inAPanel(10, panel -> panel.addSection(kept -> kept.keepTogether()
                .fillColor(DocumentColor.rgb(250, 240, 230))
                .addLayerStack(stack -> stack.topLeft(threeLines())))))).isEmpty();
    }

    @Test
    void aCardInARowInsideThePanelIsNot() throws Exception {
        // The row may not split, and moves with the card in it.
        assertThat(notes(inAPanel(10, panel -> panel.addRow(row -> row
                .add(new SectionBuilder().fillColor(DocumentColor.rgb(250, 240, 230))
                        .addLayerStack(stack -> stack.topLeft(threeLines())).build())
                .addParagraph("Beside"))))).isEmpty();
    }

    @Test
    void aStackInARowsCellInsideThePanelIsNot() throws Exception {
        assertThat(notes(inAPanel(10, panel -> panel.addRow(row -> row
                .add(new LayerStackBuilder().topLeft(threeLines()).build())
                .addParagraph("Beside"))))).isEmpty();
    }

    @Test
    void oneOnThePanelsFirstPageIsNot() throws Exception {
        assertThat(notes(inAPanel(2, panel -> panel.addLayerStack(stack -> stack.topLeft(threeLines()))))).isEmpty();
    }

    @Test
    void oneOpeningThePanelsPieceAfterABreakIsNot() throws Exception {
        // The break closes the panel there and opens it again on the next page.
        assertThat(notes(inAPanel(2, panel -> panel.addPageBreak(pageBreak -> { })
                .addLayerStack(stack -> stack.topLeft(threeLines()))))).isEmpty();
    }

    @Test
    void oneOpeningTheNextPanelIsNot() throws Exception {
        // The panel before it ends on the first page; this one opens on the second with the stack.
        Consumer<PageFlowBuilder> content = page -> {
            inAPanel(9, panel -> { }).accept(page);
            page.addSection(next -> next.fillColor(DocumentColor.rgb(230, 236, 245)).padding(DocumentInsets.of(6))
                    .addLayerStack(stack -> stack.topLeft(threeLines())));
        };
        assertThat(notes(content)).isEmpty();
    }

    @Test
    void oneInTheFlowIsNot() throws Exception {
        // Kept on its page there, as Word keeps it.
        Consumer<PageFlowBuilder> content = page -> {
            for (int i = 0; i < 10; i++) {
                page.addParagraph("Filler line " + i);
            }
            page.addLayerStack(stack -> stack.topLeft(threeLines()));
        };
        assertThat(notes(content)).isEmpty();
    }

    /**
     * A filled section on a 300 x 200 page with a 20pt margin, {@code lines} lines of filler, then
     * the block; ten lines leave no room for three more under them.
     */
    private static Consumer<PageFlowBuilder> inAPanel(int lines, Consumer<SectionBuilder> block) {
        return page -> page.addSection(panel -> {
            panel.fillColor(DocumentColor.rgb(230, 236, 245)).padding(DocumentInsets.of(6));
            for (int i = 0; i < lines; i++) {
                panel.addParagraph("Filler line " + i);
            }
            block.accept(panel);
            panel.addParagraph("Closing");
        });
    }

    private static com.demcha.compose.document.node.DocumentNode threeLines() {
        return new SectionBuilder().addParagraph("Opening").addParagraph("Body").addParagraph("Tail").build();
    }

    private static List<DocxExportReport.Note> notes(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.report(300, 200, 20, content).notes().stream()
                .filter(note -> note.detail().startsWith(WHOLE) || note.detail().startsWith(WITH_NEXT))
                .toList();
    }
}
