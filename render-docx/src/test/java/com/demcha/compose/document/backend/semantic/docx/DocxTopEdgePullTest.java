package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A paragraph's, a page reference's or a rule's top edge below zero pulls it up into what is
 * above it. Word has no space above below zero, so the export takes the pull out of the space
 * owed above, and a paragraph's also off the foot of the line above and the top of its own line;
 * what none of them gives is not written, and the report names it, though not yet on a resumed
 * layer's first block, on a block opening a page in the flow, or on a side of an overlay's line
 * pair, where the space Word writes is reckoned elsewhere.
 *
 * <p>The paragraphs here run to more than one line on a 160pt column, so neither line can give
 * anything: only a one-line paragraph rises inside its line, and only one above gives its foot.</p>
 */
class DocxTopEdgePullTest {

    private static final String SUBJECT = "space above";
    private static final String TWO_LINES = "Alpha bravo charlie delta echo foxtrot golf hotel india juliet kilo";
    private static final Pattern IN_THE_FLOW = Pattern.compile(
            "^it stands (\\d+(?:\\.\\d+)?)pt lower than the page sets it, and what follows with it: the page sets "
            + "it (\\d+(?:\\.\\d+)?)pt over the block above it, and Word sets no block over another$");
    private static final Pattern IN_A_CELL = Pattern.compile(
            "^its top edge below zero pulls it (\\d+(?:\\.\\d+)?)pt further up than anything above it in its "
            + "table cell gives back, which Word does not write: it stands up to that much lower than the page "
            + "sets it, and where its cell sets its row's height, the row is that much taller$");

    @Test
    void aParagraphPulledPastTheSpaceAboveItIsNamedWithWhatFollows() throws Exception {
        // 6pt up out of a 2pt gap: 4pt the page sets it over the paragraph above.
        List<DocxExportReport.Note> notes = notes(page -> page
                .addParagraph(p -> p.name("Above").text(TWO_LINES).margin(new DocumentInsets(0, 0, 2, 0)))
                .addParagraph(p -> p.name("Pulled").text(TWO_LINES).margin(new DocumentInsets(-6, 0, 0, 0))));

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
        assertThat(notes.get(0).path()).contains("Pulled");
        Matcher flow = IN_THE_FLOW.matcher(notes.get(0).detail());
        assertThat(flow.matches()).as("the note: %s", notes.get(0).detail()).isTrue();
        assertThat(Double.parseDouble(flow.group(1))).isCloseTo(4, within(0.01));
    }

    @Test
    void aPullTheSpaceAboveGivesBackIsNotNamed() throws Exception {
        assertThat(notes(page -> page
                .addParagraph(p -> p.name("Above").text(TWO_LINES).margin(new DocumentInsets(0, 0, 10, 0)))
                .addParagraph(p -> p.name("Pulled").text(TWO_LINES).margin(new DocumentInsets(-6, 0, 0, 0)))))
                .isEmpty();
    }

    @Test
    void aPullTheLineAboveGivesBackIsNotNamed() throws Exception {
        // The one line above holds more than half a point below its letters, and gives that.
        assertThat(notes(page -> page
                .addParagraph(p -> p.name("Above").text("Above"))
                .addParagraph(p -> p.name("Pulled").text(TWO_LINES).margin(new DocumentInsets(-0.5, 0, 0, 0)))))
                .isEmpty();
    }

    @Test
    void aOneLineParagraphThatRisesInsideItsLineIsNotNamed() throws Exception {
        // Letters with nothing over their x-height leave room at the top of their line, in a face
        // whose letters the export reads, and give it.
        DocumentTextStyle lato = DocumentTextStyle.builder().fontName(FontName.LATO).size(10).build();
        assertThat(notes(page -> page
                .addParagraph(p -> p.name("Above").text(TWO_LINES))
                .addParagraph(p -> p.name("Pulled").text("ace ace").textStyle(lato)
                        .margin(new DocumentInsets(-1, 0, 0, 0)))))
                .isEmpty();
    }

    @Test
    void aRulePulledPastTheSpaceAboveItIsNamed() throws Exception {
        List<DocxExportReport.Note> notes = notes(page -> page
                .addParagraph(p -> p.name("Above").text(TWO_LINES))
                .addLine(line -> line.name("Rule").horizontal(100).thickness(1)
                        .color(DocumentColor.rgb(0, 0, 0)).margin(new DocumentInsets(-3, 0, 0, 0))));

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).path()).contains("Rule");
        Matcher flow = IN_THE_FLOW.matcher(notes.get(0).detail());
        assertThat(flow.matches()).as("the note: %s", notes.get(0).detail()).isTrue();
        assertThat(Double.parseDouble(flow.group(1))).isCloseTo(3, within(0.01));
    }

    @Test
    void aParagraphOpeningItsCellIsNamedAsItsCellSetsIt() throws Exception {
        // A row centring a paragraph pulled 4pt up beside a taller one: nothing above it in its cell.
        List<DocxExportReport.Note> notes = notes(page -> page.addRow("Strip", row -> row
                .verticalAlign(RowVerticalAlign.CENTER)
                .addParagraph(p -> p.name("Tall").text(TWO_LINES + " " + TWO_LINES))
                .addParagraph(p -> p.name("Pair").text(TWO_LINES).margin(new DocumentInsets(-4, 0, 0, 0)))));

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).path()).contains("Pair");
        Matcher cell = IN_A_CELL.matcher(notes.get(0).detail());
        assertThat(cell.matches()).as("the note: %s", notes.get(0).detail()).isTrue();
        assertThat(Double.parseDouble(cell.group(1))).isCloseTo(4, within(0.01));
    }

    @Test
    void onlyTheBlocksOwnPullIsNamedOnIt() throws Exception {
        // A section pulled 3pt up opens with a paragraph pulled 2pt more: the paragraph's note
        // names its own 2pt, the section's edge being the section's.
        List<DocxExportReport.Note> notes = notes(page -> page
                .addParagraph(p -> p.name("Above").text(TWO_LINES))
                .addSection("Pulling", section -> section.margin(new DocumentInsets(-3, 0, 0, 0))
                        .addParagraph(p -> p.name("Pulled").text(TWO_LINES).margin(new DocumentInsets(-2, 0, 0, 0)))));

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).path()).contains("Pulled");
        Matcher flow = IN_THE_FLOW.matcher(notes.get(0).detail());
        assertThat(flow.matches()).as("the note: %s", notes.get(0).detail()).isTrue();
        assertThat(Double.parseDouble(flow.group(1))).isCloseTo(2, within(0.01));
    }

    @Test
    void aBorderBelowThePanelAboveIsNotCountedInThePull() throws Exception {
        // 10pt up out of a 6pt gap under a stroked card: 4pt of pull, the card's border below
        // being the card's, named on its own.
        List<DocxExportReport.Note> notes = notes(page -> page.spacing(6)
                .addSection("Card", card -> card.stroke(com.demcha.compose.document.style.DocumentStroke.of(
                                DocumentColor.rgb(0, 0, 0), 2))
                        .addParagraph(p -> p.text(TWO_LINES)))
                .addParagraph(p -> p.name("Pulled").text(TWO_LINES).margin(new DocumentInsets(-10, 0, 0, 0))));

        // The figures as the report writes them, whole points without a fraction.
        List<String> pulls = notes.stream()
                .map(note -> IN_THE_FLOW.matcher(note.detail()))
                .filter(Matcher::matches)
                .map(flow -> flow.group(2))
                .toList();
        assertThat(pulls).as("the pull over the block above, and no more").containsExactly("4");
    }

    @Test
    void aPullOnABandsLaterLayerIsNotReckonedHere() throws Exception {
        // A later layer resumes at the page's distance from the layers above it, which is not the
        // space owed here.
        assertThat(notes(page -> page.addParagraph("Above").addLayerStack(stack -> stack
                .topLeft(new com.demcha.compose.document.dsl.ParagraphBuilder().text("Opening").build())
                .bottomLeft(new com.demcha.compose.document.dsl.ParagraphBuilder().name("Caption").text("Caption")
                        .margin(new DocumentInsets(-4, 0, 0, 0)).build())))).isEmpty();
    }

    @Test
    void aParagraphOpeningACellOnALaterPageIsNamed() throws Exception {
        // In a table cell the space owed is the cell's own, though the row opens the page.
        List<DocxExportReport.Note> notes = notes(page -> {
            page.addParagraph(TWO_LINES).addPageBreak(pageBreak -> { });
            page.addRow("Strip", row -> row
                    .addParagraph(p -> p.name("Tall").text(TWO_LINES + " " + TWO_LINES))
                    .addParagraph(p -> p.name("Pair").text(TWO_LINES).margin(new DocumentInsets(-4, 0, 0, 0))));
        });

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).path()).contains("Pair");
        Matcher cell = IN_A_CELL.matcher(notes.get(0).detail());
        assertThat(cell.matches()).as("the note: %s", notes.get(0).detail()).isTrue();
        assertThat(Double.parseDouble(cell.group(1))).isCloseTo(4, within(0.01));
    }

    @Test
    void aParagraphInAPanelMovedToTheNextPageIsNamed() throws Exception {
        // A panel kept whole that does not fit under the paragraphs above moves to the next page;
        // in its cell the space owed is the cell's own.
        List<DocxExportReport.Note> notes = notes(page -> {
            for (int index = 0; index < 8; index++) {
                page.addParagraph(TWO_LINES);
            }
            page.addSection("Card", card -> card.fillColor(DocumentColor.rgb(238, 243, 249)).keepTogether()
                    .addParagraph(p -> p.name("Title").text(TWO_LINES))
                    .addParagraph(p -> p.name("Pulled").text(TWO_LINES).margin(new DocumentInsets(-4, 0, 0, 0)))
                    .addParagraph(p -> p.text(TWO_LINES)));
        });

        List<DocxExportReport.Note> pulled = notes.stream().filter(note -> note.path().contains("Pulled")).toList();
        assertThat(pulled).hasSize(1);
        Matcher cell = IN_A_CELL.matcher(pulled.get(0).detail());
        assertThat(cell.matches()).as("the note: %s", pulled.get(0).detail()).isTrue();
        assertThat(cell.group(1)).as("all of it, nothing in the card giving it back").isEqualTo("4");
    }

    @Test
    void aPullOnABlockAfterAPageBreakIsNotReckonedHere() throws Exception {
        // Nothing stands above it on its page, and Word drops the space above at the page's top.
        assertThat(notes(page -> page
                .addParagraph(p -> p.name("Before").text(TWO_LINES))
                .addPageBreak(pageBreak -> { })
                .addParagraph(p -> p.name("After").text(TWO_LINES).margin(new DocumentInsets(-4, 0, 0, 0)))))
                .isEmpty();
    }

    @Test
    void aPullOnTheDocumentsFirstBlockIsNotReckonedHere() throws Exception {
        assertThat(notes(page -> page
                .addParagraph(p -> p.name("First").text(TWO_LINES).margin(new DocumentInsets(-4, 0, 0, 0)))))
                .isEmpty();
    }

    @Test
    void anEdgeAtZeroOrAboveIsNotNamed() throws Exception {
        assertThat(notes(page -> page
                .addParagraph(p -> p.name("Above").text(TWO_LINES))
                .addParagraph(p -> p.name("Level").text(TWO_LINES).margin(new DocumentInsets(0, 0, 0, 0)))
                .addParagraph(p -> p.name("Below").text(TWO_LINES).margin(new DocumentInsets(3, 0, 0, 0)))))
                .isEmpty();
    }

    /** The space-above notes of an export the layout stands behind. */
    private static List<DocxExportReport.Note> notes(Consumer<PageFlowBuilder> content) throws Exception {
        DocxExportReport report = DocxExports.report(200, 400, 20, content);
        assertThat(report.bySubject()).as("laid out").doesNotContainKey("measured geometry");
        return report.bySubject().getOrDefault(SUBJECT, List.of());
    }
}
