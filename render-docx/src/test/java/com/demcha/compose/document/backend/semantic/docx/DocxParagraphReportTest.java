package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a paragraph's own fields lose in the Word file is in the report: the size an auto-sized
 * paragraph's text is fitted to, where its lines do not tell it, the prefix its {@code bulletOffset}
 * sets before its lines, and its outline entry's title and level.
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
    void anAutoSizedParagraphsTextIsNamedOnlyWhereItsLinesDoNotTellTheSizeThePageFitsItTo() throws Exception {
        // Written at the size the page fits it to, smaller or larger than its style's, it loses nothing.
        Consumer<PageFlowBuilder> shrunk = page -> page.addParagraph(p -> p.name("Headline").text(HEADLINE)
                .textStyle(DocumentTextStyle.DEFAULT.withSize(24)).autoSize(24, 6));
        assertThat(firstSpan(shrunk).textStyle().size()).as("the page fits it smaller").isLessThan(24);
        assertThat(paragraphNotes(shrunk)).isEmpty();
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.text("Hi").textStyle(TEN).autoSize(24)))).isEmpty();
        // So is one fitted to its own style's size.
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.text("Hi")
                .textStyle(DocumentTextStyle.DEFAULT.withSize(24)).autoSize(24, 6)))).isEmpty();
        // So is a prefix the page sets in the paragraph's style, where every run keeps its own.
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p
                .textStyle(DocumentTextStyle.DEFAULT.withSize(24)).inlineText(HEADLINE, TEN)
                .bulletOffset("    ").indentStrategy(DocumentTextIndent.ALL_LINES).autoSize(24, 6)))).isEmpty();
        // Fitted to the size a run has of its own, the lines hold no other: it is that size.
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.textStyle(TEN)
                .inlineText("Hi ", DocumentTextStyle.DEFAULT.withSize(24)).inlineText("there")
                .autoSize(24)))).isEmpty();
        // Fitted to 12pt beside runs of 12pt and 10pt of their own, the lines do not tell which is its.
        String unmeasured = "its text is written at 10pt — the size the page fits it to is not measured";
        assertThat(paragraphNote(page -> page.addParagraph(p -> p.textStyle(TEN)
                .inlineText("A ", DocumentTextStyle.DEFAULT.withSize(12))
                .inlineText("B ", DocumentTextStyle.DEFAULT.withSize(10)).inlineText("C").autoSize(12))))
                .isEqualTo("written as a paragraph; " + unmeasured);
        // On a side of a pair as in the body.
        assertThat(paragraphNote(page -> page.add(pair(new ParagraphBuilder().name("Title").textStyle(TEN)
                .inlineText("A ", DocumentTextStyle.DEFAULT.withSize(12))
                .inlineText("B ", DocumentTextStyle.DEFAULT.withSize(10)).inlineText("C").autoSize(12).build(),
                "2022"))))
                .isEqualTo("written as one side of a line it shares; " + unmeasured);
        // Not auto-sized, it is not named.
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.text(HEADLINE)
                .textStyle(DocumentTextStyle.DEFAULT.withSize(24))))).isEmpty();
    }

    @Test
    void aParagraphOfRunsInStylesOfTheirOwnHasNoFittedSizeToLose() throws Exception {
        // A run with a style of its own is laid out at its own size, as it is written: with no
        // text in the paragraph's style, nothing is fitted to lose.
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.textStyle(TEN)
                .inlineText("A ", DocumentTextStyle.DEFAULT.withSize(12))
                .inlineText("B ", DocumentTextStyle.DEFAULT.withSize(10)).autoSize(12)))).isEmpty();
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
        // Nor one of characters the page drops.
        assertThat(paragraphNotes(prefixed("• ", DocumentTextIndent.FIRST_LINE, "⁠\nShip it"))).isEmpty();
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
        // Nor does one before a right side a right tab holds by its end, whichever way it is aligned.
        assertThat(paragraphNotes(page -> page.add(pair(new ParagraphBuilder().name("Title").text("ENGINEER").build(),
                new ParagraphBuilder().name("Dates").text("2022").align(TextAlign.RIGHT).bulletOffset("   ")
                        .indentStrategy(DocumentTextIndent.FIRST_LINE).build())))).isEmpty();
        assertThat(paragraphNotes(page -> page.add(pair(new ParagraphBuilder().name("Title").text("ENGINEER").build(),
                new ParagraphBuilder().name("Dates").text("2022").align(TextAlign.CENTER).bulletOffset("   ")
                        .indentStrategy(DocumentTextIndent.FIRST_LINE).build())))).isEmpty();
        // A right side a left tab holds by its start starts where its prefix does.
        assertThat(paragraphNote(page -> page.add(new ShapeContainerBuilder().name("BankRow").rectangle(CONTENT, 14)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().name("Label").text("IBAN").build(), 0, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder().name("Value").text("GB36 SRLG").bulletOffset("   ")
                        .indentStrategy(DocumentTextIndent.FIRST_LINE).build(), 80, 0, LayerAlign.CENTER_LEFT)
                .build())))
                .isEqualTo("written as one side of a line it shares; " + room);
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
        // An empty line after the first takes no prefix.
        assertThat(paragraphNotes(page -> page.add(sidebar(new ParagraphBuilder().name("Monogram").text("L\n")
                .bulletOffset("   ").indentStrategy(DocumentTextIndent.FROM_SECOND_LINE).build())).addParagraph("Masthead")))
                .contains(boxed);
        // Over more lines than one, Word breaks the lines without the prefix's room, however aligned.
        assertThat(paragraphNotes(page -> page.add(sidebar(new ParagraphBuilder().name("Tagline")
                .text("Design studio of record since the spring").align(TextAlign.RIGHT).bulletOffset("   ")
                .indentStrategy(DocumentTextIndent.FROM_SECOND_LINE).build())).addParagraph("Masthead")))
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
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.text("Results of the year")
                .bookmark(new DocumentBookmarkOptions("Results of the year", 1))))).as("a no-break space").isEmpty();
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
    void anOutlineLevelPastWordsNinthIsNamedWhereItSharesTheNinthWithAnother() throws Exception {
        // Word's ninth level holds a deeper one as the page's outline does, one step below the last.
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.text("Top")
                        .bookmark(new DocumentBookmarkOptions("Top", 0)))
                .addParagraph(p -> p.text("Deeper").bookmark(new DocumentBookmarkOptions("Deeper", 9)))))
                .isEmpty();
        // Beside the ninth itself, it is one level with it where the page nests it below.
        assertThat(paragraphNotes(page -> page.addParagraph(p -> p.text("Deep")
                        .bookmark(new DocumentBookmarkOptions("Deep", 8)))
                .addParagraph(p -> p.text("Deeper").bookmark(new DocumentBookmarkOptions("Deeper", 9)))))
                .containsExactly("written as a paragraph; its outline entry is written at Word's ninth level, which "
                                 + "it shares with a level the page nests apart from it");
    }

    @Test
    void aPairsLineIsOneHeadingListedByAllOfItsText() throws Exception {
        String shared = "written as one side of a line it shares; ";
        // The line is the left side's heading, listed by both sides' text; the right side's entry is lost.
        assertThat(paragraphNotes(page -> page.add(pair(title("ENGINEER 2022"), dates("2022")))))
                .containsExactly(shared + "its outline entry is not written");
        assertThat(paragraphNotes(page -> page.add(pair(title("ENGINEER"), "2022"))))
                .containsExactly(shared + "its outline entry shows \"ENGINEER 2022\", not its title \"ENGINEER\"");
        // With none on its left, the line is the right side's heading.
        assertThat(paragraphNotes(page -> page.add(pair(new ParagraphBuilder().name("Title").text("ENGINEER")
                .build(), dates("ENGINEER 2022"))))).isEmpty();
        assertThat(paragraphNotes(page -> page.add(pair(new ParagraphBuilder().name("Title").text("ENGINEER")
                .build(), dates("2022")))))
                .containsExactly(shared + "its outline entry shows \"ENGINEER 2022\", not its title \"2022\"");
    }

    /** A pair's left side, "ENGINEER", declaring an outline entry at the first level under a title. */
    private static DocumentNode title(String outlineTitle) {
        return new ParagraphBuilder().name("Title").text("ENGINEER")
                .bookmark(new DocumentBookmarkOptions(outlineTitle, 1)).build();
    }

    /** A pair's right side, "2022" aligned right, declaring an outline entry at the second level under a title. */
    private static DocumentNode dates(String outlineTitle) {
        return new ParagraphBuilder().name("Dates").text("2022").align(TextAlign.RIGHT)
                .bookmark(new DocumentBookmarkOptions(outlineTitle, 2)).build();
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
                .position(new ShapeBuilder().name("Block").size(100, 120)
                        .fillColor(DocumentColor.rgb(160, 80, 50)).build(), 0, 0, LayerAlign.TOP_LEFT, 0)
                .position(text, 10, 10, LayerAlign.TOP_LEFT, 1)
                .build();
    }

    /** The first text the page lays out. */
    private static ParagraphTextSpan firstSpan(Consumer<PageFlowBuilder> content) throws Exception {
        try (DocumentSession session = session(content)) {
            return session.layoutGraph().fragments().stream()
                    .map(fragment -> fragment.payload())
                    .filter(ParagraphFragmentPayload.class::isInstance)
                    .map(ParagraphFragmentPayload.class::cast)
                    .flatMap(paragraph -> paragraph.lines().stream())
                    .flatMap(line -> line.spans().stream())
                    .filter(ParagraphTextSpan.class::isInstance)
                    .map(ParagraphTextSpan.class::cast)
                    .findFirst().orElseThrow();
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
