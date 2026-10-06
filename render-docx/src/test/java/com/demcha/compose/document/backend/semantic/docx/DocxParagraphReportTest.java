package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a paragraph's own fields lose in the Word file is in the report: the size an auto-sized
 * paragraph's text is fitted to, the prefix its {@code bulletOffset} sets before its lines, and its
 * outline entry's title and level.
 *
 * <p>Each was drawn by the page and left out of the file without a note. A paragraph that keeps
 * them — its text at the size the page fits it to, a blank prefix written as its indent, an outline
 * entry titled by its text — is not named.</p>
 */
class DocxParagraphReportTest {

    private static final String HEADLINE = "A headline far too long for one line at its size";
    private static final DocumentTextStyle TEN = DocumentTextStyle.DEFAULT.withSize(10);
    private static final double CONTENT = 180;

    @Test
    void anAutoSizedParagraphsTextIsNamedWhereThePageFitsItToAnotherSize() throws Exception {
        Consumer<PageFlowBuilder> shrunk = page -> page.addParagraph(p -> p.name("Headline").text(HEADLINE)
                .textStyle(DocumentTextStyle.DEFAULT.withSize(24)).autoSize(24, 6));
        double fitted = laidOutSize(shrunk);
        assertThat(fitted).as("the page fits it smaller").isLessThan(24);
        assertThat(paragraphNote(shrunk))
                .isEqualTo("written as a paragraph; its text is written at 24pt, where the page fits it to "
                           + points(fitted) + "pt");
        // Up to a size above its style's, a line that fits at it is set at it.
        assertThat(paragraphNote(page -> page.addParagraph(p -> p.text("Hi").textStyle(TEN).autoSize(24))))
                .isEqualTo("written as a paragraph; its text is written at 10pt, where the page fits it to 24pt");
        // Fitted to its own size, it loses nothing.
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.text("Hi")
                .textStyle(DocumentTextStyle.DEFAULT.withSize(24)).autoSize(24, 6)))).isEmpty();
        // Not auto-sized, it is not named.
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.text(HEADLINE)
                .textStyle(DocumentTextStyle.DEFAULT.withSize(24))))).isEmpty();
        // Word holds a size to the half point: 10.3 and 10.5 are one size in the file.
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.text("Hi")
                .textStyle(DocumentTextStyle.DEFAULT.withSize(10.3)).autoSize(10.5)))).isEmpty();
    }

    @Test
    void aPrefixThePageSetsInTheFittedSizeIsNamedWhereEveryRunKeepsItsOwn() throws Exception {
        // The page sets the prefix in the paragraph's style at the fitted size; the indent it is
        // written as is measured at the style's.
        Consumer<PageFlowBuilder> prefixed = page -> page.addParagraph(p -> p
                .textStyle(DocumentTextStyle.DEFAULT.withSize(24)).inlineText(HEADLINE, TEN)
                .bulletOffset("    ").indentStrategy(DocumentTextIndent.ALL_LINES).autoSize(24, 6));
        double fitted = laidOutSize(prefixed);
        assertThat(fitted).as("the prefix, laid out first, at the fitted size").isLessThan(24);
        assertThat(paragraphNote(prefixed))
                .isEqualTo("written as a paragraph; its text is written at 24pt, where the page fits it to "
                           + points(fitted) + "pt");
    }

    @Test
    void onlyTheTextThatTakesTheParagraphsStyleIsFitted() throws Exception {
        // A run with a style of its own is laid out at its own size, as it is written.
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.textStyle(TEN)
                .inlineText("Hi ", TEN).inlineText("there", DocumentTextStyle.DEFAULT.withSize(12))
                .autoSize(24)))).isEmpty();
        // Beside one, a run with none takes the size the page fits the paragraph to.
        assertThat(paragraphNote(page -> page.addParagraph(p -> p.textStyle(TEN)
                .inlineText("Hi ", DocumentTextStyle.DEFAULT.withSize(12)).inlineText("there")
                .autoSize(24))))
                .isEqualTo("written as a paragraph; its text is written at 10pt, where the page fits it to 24pt");
        // Fitted to the size a run has of its own, it is that size.
        assertThat(paragraphNote(page -> page.addParagraph(p -> p.textStyle(TEN)
                .inlineText("Hi ", DocumentTextStyle.DEFAULT.withSize(24)).inlineText("there")
                .autoSize(24))))
                .isEqualTo("written as a paragraph; its text is written at 10pt, where the page fits it to 24pt");
    }

    @Test
    void withNoLayoutTheSizeThePageFitsTheTextToIsNotMeasured() throws Exception {
        DocxExportReport report = DocxExports.reportWithoutLayout(240, 600, 30, page -> page
                .addParagraph(p -> p.text("Hi").textStyle(DocumentTextStyle.DEFAULT.withSize(24)).autoSize(24, 6)));

        assertThat(report.bySubject().get("ParagraphNode")).extracting(DocxExportReport.Note::detail)
                .containsExactly("written as a paragraph; its text is written at 24pt — the size the page fits it "
                                 + "to is not measured");
        // Not auto-sized, a paragraph is written at the size the page sets it in, laid out or not.
        assertThat(DocxExports.reportWithoutLayout(240, 600, 30, page -> page.addParagraph(p -> p.text("Hi")
                .textStyle(DocumentTextStyle.DEFAULT.withSize(24)))).bySubject()).doesNotContainKey("ParagraphNode");
    }

    @Test
    void aPrefixsLettersAreNamedWhereThePageSetsThemBeforeTheFirstLine() throws Exception {
        String letters = "written as a paragraph; its bulletOffset's letters, \"•\", are not written before its first line";
        assertThat(paragraphNote(prefixed("• ", DocumentTextIndent.FIRST_LINE, "Ship it"))).isEqualTo(letters);
        assertThat(paragraphNote(prefixed("• ", DocumentTextIndent.ALL_LINES, "Ship it"))).isEqualTo(letters);
        // Before the wrapped lines the page sets the spaces that cover it, which are written.
        assertThat(paragraphNotes(prefixed("• ", DocumentTextIndent.FROM_SECOND_LINE, HEADLINE))).isEmpty();
        // A blank prefix is written as the paragraph's indent.
        assertThat(paragraphNotes(prefixed("   ", DocumentTextIndent.ALL_LINES, HEADLINE))).isEmpty();
        // A first line ended at once holds nothing to set a prefix before.
        assertThat(paragraphNotes(prefixed("• ", DocumentTextIndent.FIRST_LINE, "\nShip it"))).isEmpty();
        assertThat(paragraphNotes(prefixed("• ", DocumentTextIndent.NONE, "Ship it"))).isEmpty();
        // In a paragraph of runs, the first line is the runs' up to their first line break.
        assertThat(paragraphNote(page -> page.addParagraph(p -> p.inlineText("Ship ").inlineText("it", TEN)
                .bulletOffset("• ").indentStrategy(DocumentTextIndent.FIRST_LINE)))).isEqualTo(letters);
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.inlineText("\nShip ").inlineText("it", TEN)
                .bulletOffset("• ").indentStrategy(DocumentTextIndent.FIRST_LINE)))).isEmpty();
    }

    @Test
    void aPrefixIsNamedWhereThePathThatWritesTheParagraphWritesNone() throws Exception {
        String room = "the room its bulletOffset sets its lines in by is not written";
        // One side of an overlay's left-and-right pair.
        assertThat(paragraphNote(page -> page.add(pair(new ParagraphBuilder().name("Title").text("ENGINEER")
                .bulletOffset("   ").indentStrategy(DocumentTextIndent.FIRST_LINE).build(), "2022"))))
                .isEqualTo("written as one side of a line it shares; " + room);
        // A prefix for the wrapped lines of a side of one line moves nothing.
        assertThat(paragraphNotes(page -> page.add(pair(new ParagraphBuilder().name("Title").text("ENGINEER")
                .bulletOffset("   ").indentStrategy(DocumentTextIndent.FROM_SECOND_LINE).build(), "2022"))))
                .isEmpty();
        // Nor does one before a line set from its other end: the right side's dates end where they end.
        assertThat(paragraphNotes(page -> page.add(pair(new ParagraphBuilder().name("Title").text("ENGINEER").build(),
                new ParagraphBuilder().name("Dates").text("2022").align(TextAlign.RIGHT).bulletOffset("   ")
                        .indentStrategy(DocumentTextIndent.FIRST_LINE).build())))).isEmpty();
        // A prefix's letters are named on any path, beside the room.
        assertThat(paragraphNote(page -> page.add(pair(new ParagraphBuilder().name("Title").text("ENGINEER")
                .bulletOffset("• ").indentStrategy(DocumentTextIndent.FIRST_LINE).build(), "2022"))))
                .isEqualTo("written as one side of a line it shares; its bulletOffset's letters, \"•\", are not "
                           + "written before its first line; " + room);
        // A text box over the flow.
        String boxed = "laid over the flow, which gives it no room: set in a text box where the page sets it";
        assertThat(paragraphNotes(page -> page.add(sidebar(new ParagraphBuilder().name("Monogram").text("L")
                .bulletOffset("• ").indentStrategy(DocumentTextIndent.FIRST_LINE).build())).addParagraph("Masthead")))
                .contains(boxed + "; its bulletOffset's letters, \"•\", are not written before its first line; " + room);
        // Right to left, the prefix stands at the right: a line set from the left keeps its text where it is.
        assertThat(paragraphNotes(page -> page.add(sidebar(new ParagraphBuilder().name("Monogram").text("L")
                .direction(TextDirection.RTL).align(TextAlign.LEFT).bulletOffset("   ")
                .indentStrategy(DocumentTextIndent.FIRST_LINE).build())).addParagraph("Masthead")))
                .contains(boxed);
        assertThat(paragraphNotes(page -> page.add(sidebar(new ParagraphBuilder().name("Monogram").text("L")
                .direction(TextDirection.RTL).align(TextAlign.RIGHT).bulletOffset("   ")
                .indentStrategy(DocumentTextIndent.FIRST_LINE).build())).addParagraph("Masthead")))
                .contains(boxed + "; " + room);
        // A badge's initials, which a blank prefix moves off its centre.
        assertThat(paragraphNote(page -> page.add(new ShapeContainerBuilder().name("Badge").circle(40)
                .fillColor(DocumentColor.rgb(30, 50, 90))
                .center(new ParagraphBuilder().text("JR").bulletOffset(" ")
                        .indentStrategy(DocumentTextIndent.FIRST_LINE).build())
                .build())))
                .isEqualTo("written as its badge's text; " + room);
    }

    @Test
    void anOutlineEntryIsNamedWhereItsTitleIsNotItsText() throws Exception {
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.text("Results")
                .bookmark(new DocumentBookmarkOptions("Results", 1))))).isEmpty();
        // Word lists a heading by its text, a line break or a run of spaces in it one space.
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.text("Results  of\nthe year")
                .bookmark(new DocumentBookmarkOptions("Results of the year", 1))))).isEmpty();
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.text("Results of the year")
                .bookmark(new DocumentBookmarkOptions("Results  of the year", 1))))).isEmpty();
        assertThat(paragraphNote(page -> page.addParagraph(p -> p.text("RESULTS")
                .bookmark(new DocumentBookmarkOptions("Results", 1)))))
                .isEqualTo("written as a paragraph; its outline entry shows \"RESULTS\", not its title \"Results\"");
        assertThat(paragraphNote(page -> page.addParagraph(p -> p.inlineText("Results ").inlineText("2026", TEN)
                .bookmark(new DocumentBookmarkOptions("Results", 1)))))
                .isEqualTo("written as a paragraph; its outline entry shows \"Results 2026\", not its title \"Results\"");
        // A paragraph of runs is listed by all of their text.
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.inlineText("Results ").inlineText("2026", TEN)
                .bookmark(new DocumentBookmarkOptions("Results 2026", 1))))).isEmpty();
    }

    @Test
    void anOutlineLevelPastWordsNinthIsNamed() throws Exception {
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.text("Deep")
                .bookmark(new DocumentBookmarkOptions("Deep", 8))))).as("Word's ninth").isEmpty();
        assertThat(paragraphNote(page -> page.addParagraph(p -> p.text("Deeper")
                .bookmark(new DocumentBookmarkOptions("Deeper", 9)))))
                .isEqualTo("written as a paragraph; its outline entry is written at Word's ninth level, where the "
                           + "page nests it deeper");
    }

    @Test
    void aPairsLineIsOneHeadingListedByAllOfItsText() throws Exception {
        String shared = "written as one side of a line it shares; ";
        // The line is the left side's heading, listed by both sides' text; the right side's entry is lost.
        assertThat(paragraphNotes(page -> page.add(pair(heading("ENGINEER", "ENGINEER 2022", 1),
                heading("2022", "2022", 2)))))
                .containsExactly(shared + "its outline entry is not written");
        assertThat(paragraphNotes(page -> page.add(pair(heading("ENGINEER", "ENGINEER", 1), "2022"))))
                .containsExactly(shared + "its outline entry shows \"ENGINEER 2022\", not its title \"ENGINEER\"");
        // With none on its left, the line is the right side's heading.
        assertThat(paragraphNotes(page -> page.add(pair(new ParagraphBuilder().name("Title").text("ENGINEER")
                .build(), heading("2022", "ENGINEER 2022", 2))))).isEmpty();
        assertThat(paragraphNotes(page -> page.add(pair(new ParagraphBuilder().name("Title").text("ENGINEER")
                .build(), heading("2022", "2022", 2)))))
                .containsExactly(shared + "its outline entry shows \"ENGINEER 2022\", not its title \"2022\"");
    }

    /** A paragraph of a pair that declares an outline entry, the right one aligned right. */
    private static DocumentNode heading(String text, String title, int level) {
        return new ParagraphBuilder().name(level == 1 ? "Title" : "Dates").text(text)
                .align(level == 1 ? TextAlign.LEFT : TextAlign.RIGHT)
                .bookmark(new DocumentBookmarkOptions(title, level)).build();
    }

    private static Consumer<PageFlowBuilder> prefixed(String prefix, DocumentTextIndent strategy, String text) {
        return page -> page.addParagraph(p -> p.name("Item").text(text).bulletOffset(prefix).indentStrategy(strategy));
    }

    /** An overlay of two texts on one line, the left one set from the left, the right one from the right. */
    private static DocumentNode pair(DocumentNode left, String right) {
        return pair(left, new ParagraphBuilder().name("Dates").text(right).align(TextAlign.RIGHT).build());
    }

    private static DocumentNode pair(DocumentNode left, DocumentNode right) {
        return new ShapeContainerBuilder().name("EntryHead").rectangle(CONTENT, 20)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(left, 0, 0, LayerAlign.CENTER_LEFT)
                .position(right, 0, 0, LayerAlign.CENTER_RIGHT)
                .build();
    }

    /** A sidebar hung into the page's margin, its text laid over the flow on a filled block. */
    private static DocumentNode sidebar(DocumentNode text) {
        return new ShapeContainerBuilder().name("Sidebar")
                .rectangle(100, 120).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(new DocumentInsets(-30, 0, -90, -30))
                .position(new com.demcha.compose.document.dsl.ShapeBuilder().name("Block").size(100, 120)
                        .fillColor(DocumentColor.rgb(160, 80, 50)).build(), 0, 0, LayerAlign.TOP_LEFT, 0)
                .position(text, 10, 10, LayerAlign.TOP_LEFT, 1)
                .build();
    }

    private static String points(double size) {
        return BigDecimal.valueOf(Math.round(size * 100) / 100.0).stripTrailingZeros().toPlainString();
    }

    /** The size of the first letters the page lays out. */
    private static double laidOutSize(Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = session(content)) {
            return session.layoutGraph().fragments().stream()
                    .map(fragment -> fragment.payload())
                    .filter(ParagraphFragmentPayload.class::isInstance)
                    .map(ParagraphFragmentPayload.class::cast)
                    .flatMap(paragraph -> paragraph.lines().stream())
                    .flatMap(line -> line.spans().stream())
                    .filter(ParagraphTextSpan.class::isInstance)
                    .map(ParagraphTextSpan.class::cast)
                    .findFirst().orElseThrow().textStyle().size();
        }
    }

    private static String paragraphNote(Consumer<PageFlowBuilder> content) throws Exception {
        List<String> notes = paragraphNotes(content);
        assertThat(notes).as("the paragraph's note").hasSize(1);
        return notes.get(0);
    }

    private static List<String> paragraphNotes(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (DocumentSession session = session(content)) {
            session.export(new DocxSemanticBackend(captured::set));
        }
        assertThat(captured.get()).as("the sink is called once the bytes exist").isNotNull();
        return captured.get().bySubject().getOrDefault("ParagraphNode", List.of()).stream()
                .map(DocxExportReport.Note::detail).toList();
    }

    private static DocumentSession session(Consumer<PageFlowBuilder> content) {
        DocumentSession session = GraphCompose.document()
                .pageSize(240, 600)
                .margin(DocumentInsets.of(30))
                .create();
        session.pageFlow(content::accept);
        return session;
    }
}
