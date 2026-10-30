package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxExportReport;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.templates.fidelity.FlowOwnerDrawingDocuments.FlowOwnerDocument;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.demcha.compose.document.templates.fidelity.DocxFixtures.assertLaidOut;
import static com.demcha.compose.document.templates.fidelity.DocxFixtures.body;
import static com.demcha.compose.document.templates.fidelity.DocxFixtures.docx;
import static com.demcha.compose.document.templates.fidelity.DocxFixtures.footer;
import static com.demcha.compose.document.templates.fidelity.DocxFixtures.header;
import static com.demcha.compose.document.templates.fidelity.DocxFixtures.paragraph;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A drawing painted for a block that writes text in the flow is carried by a paragraph, not nailed
 * to the sheet: {@code wp:positionV relativeFrom="paragraph"} and never {@code "page"}. Word and
 * LibreOffice move a drawing carried by a paragraph with that paragraph when the text above it is
 * edited; one placed from the page stays where it was written while the text it belongs to walks
 * away from under it.
 *
 * <p>Held two ways. Over the template corpus, which fixes what the export does today: every
 * floating drawing a node paints is carried by a paragraph but for the ones this test names one by
 * one, each with the reason it is still on the sheet ({@link #STILL_ON_THE_PAGE}). And over
 * documents of its own, one for each writer of a paragraph that may carry a drawing
 * ({@link FlowOwnerDrawingDocuments}), which hold the rule rather than today's state: every
 * drawing in them must be placed down from a paragraph, and none may be on the sheet.</p>
 *
 * <p>The list is a ledger of work still to do, not a set of cases where the sheet is right: the
 * export has no drawing it is content to leave there. It names each drawing, not just how many, so
 * that a change fixing one and breaking another in the same document cannot pass on a tally that
 * happens to come out the same. An entry shrinks or goes when the export learns to carry those
 * drawings, and this test then fails until the ledger is brought down to match — which is what
 * makes it a ledger and not a licence.</p>
 *
 * <p>It fails closed. The reader is held to finding the drawings it is to judge: every page fill
 * must come back placed from the page, which no reader that lost the ability to read
 * {@code relativeFrom} can show, every floating drawing must come back saying the editors read
 * where it is placed from at all ({@code simplePos="0"}), the corpus must come back with the
 * hundreds of carried drawings it holds, and an anchor the reader cannot classify is a failure
 * rather than a silence ({@link DocxDrawingPlacements#problems()}).</p>
 */
class DocxDrawingAnchorGuardTest {

    /**
     * Drawings of a table's composed cell: the walk from the cell whose box holds the drawing to a
     * paragraph of that cell finds none it can be placed from, and the drawing stays on the sheet.
     *
     * <p>Which step of the walk gives up has not been pinned down. A cell set to the middle of a
     * nested row stands in the way in these three invoices, and was taken for the cause, but a
     * composed cell holding text and a drawing leaves the drawing on the sheet with no nested row
     * anywhere near it, so that is at most one way in. The reason here is therefore the outcome,
     * not a mechanism.</p>
     */
    private static final String COMPOSED_CELL_WALK_STOPS =
            "a composed cell's drawing, which the walk from its holding cell found no paragraph for,"
            + " and which the fall back to the text nearest it did not place either";

    /**
     * Drawings painted for a block whose own text is written as the drawing itself — a lockup or a
     * banner laid over the flow, set in a text box where the page sets it — so no paragraph of the
     * blocks holding it is written on its page at all, and none of the page's other paragraphs
     * could be placed from either.
     */
    private static final String OWNER_WRITES_NO_PARAGRAPH =
            "its owner's own text is written as the drawing, so no paragraph of its blocks stands on its page";

    /**
     * A drawing whose holding block does write paragraphs on its page — four of them stand within
     * its path — and which none of them was placed from even so.
     *
     * <p>How far it rises is not what refuses it, and not because the rise is within what is
     * allowed: the rise is not on this path at all. {@code DocxDrawingAnchors.Seat.risesNoFurther}
     * is consulted only from {@code Seat.place}, which a composed cell's drawing reaches by way of
     * {@code placeBeside}. A drawing a node paints is placed by {@code Seat.placeHeld}, which never
     * asks how far it rises.</p>
     *
     * <p>What {@code placeHeld} does refuse on is a short list, and one of them is measured out: a
     * paragraph whose top is not known, which this one's is (52.80pt, not NaN). The rest stand:
     * a repeated header row, a paragraph not set from its cell's left, a cell whose width or left
     * edge is not known in twips, and a cell not set from its top whose row hands the drawing to
     * no neighbour. Which of those it is has not been measured.</p>
     */
    private static final String HOLDER_PARAGRAPH_REFUSED =
            "paragraphs of its block stand on its page and none of them placed it, nor did any other"
            + " paragraph of that page once the nearest were tried in turn";

    /**
     * The drawings of the corpus still placed from the page: which they are, and why each is there.
     * Measured over the corpus of 71 documents, which holds 760 floating drawings painted by a
     * node, of which 728 are carried by a paragraph.
     *
     * <p>Twenty-five are a composed cell's ({@link #COMPOSED_CELL_WALK_STOPS}): thirteen in
     * {@code invoice-payments} under {@code LineItemsBox/LineItems}, eight in
     * {@code invoice-workspace} and four in {@code invoice-merchant}, each with 92 or more
     * paragraphs on its page to choose from, so it is not for want of one. Seven are painted for a
     * block of a sidebar or a closing banner: six in {@code invoice-luma_studio_long} — the brand
     * lockup's four text badges on page 1 and the closing banner's two on page 3
     * ({@link #OWNER_WRITES_NO_PARAGRAPH}) — and one in {@code invoice-obsidian_long}, the closing
     * band's disc on page 5 ({@link #HOLDER_PARAGRAPH_REFUSED}).</p>
     */
    private static final Map<String, OnThePage> STILL_ON_THE_PAGE = Map.of(
            "invoice-payments", new OnThePage(COMPOSED_CELL_WALK_STOPS,
                    "Drawing 8", "Drawing 9", "Drawing 10", "Drawing 11", "Drawing 12", "Drawing 13", "Drawing 14",
                    "Drawing 15", "Drawing 16", "Drawing 17", "Drawing 18", "Drawing 19", "Drawing 20"),
            "invoice-workspace", new OnThePage(COMPOSED_CELL_WALK_STOPS,
                    "Drawing 5", "Drawing 6", "Drawing 7", "Drawing 8", "Drawing 9", "Drawing 10", "Drawing 11",
                    "Drawing 12"),
            "invoice-merchant", new OnThePage(COMPOSED_CELL_WALK_STOPS,
                    "Drawing 7", "Drawing 8", "Drawing 9", "Drawing 10"),
            "invoice-luma_studio_long", new OnThePage(OWNER_WRITES_NO_PARAGRAPH,
                    "Drawing 5", "Drawing 6", "Drawing 8", "Drawing 9", "Drawing 27", "Drawing 28"),
            "invoice-obsidian_long", new OnThePage(HOLDER_PARAGRAPH_REFUSED, "Drawing 68"));

    /**
     * The drawings of a document still placed from the page, and why they are.
     *
     * @param reason   why they are there, which is a reason they are not carried yet and never a
     *                 reason the sheet is right for them
     * @param drawings their names in the file, as the export writes them
     */
    private record OnThePage(String reason, List<String> drawings) {

        OnThePage(String reason, String... drawings) {
            this(reason, List.of(drawings));
        }

        List<String> named() {
            return drawings;
        }
    }

    /**
     * The ledger as a failure reads it: each document, how many drawings it is allowed to leave on
     * the page, and the reason recorded for them. A change that moves a drawing onto the sheet, or
     * carries one at last, fails against this.
     */
    private static String ledgerAsText() {
        StringBuilder text = new StringBuilder();
        new TreeMap<>(STILL_ON_THE_PAGE).forEach((stem, still) -> text.append("  ").append(stem).append(": ")
                .append(still.named().size()).append(" — ").append(still.reason()).append(System.lineSeparator()));
        return text.toString();
    }

    /**
     * How many floating drawings the corpus has a node paint, as a floor: a reader that stopped
     * finding them would otherwise report every document clean. Measured at 760 over 71 documents,
     * beside 381 inline ones, which are placed from nothing and so are not this guard's business.
     *
     * <p>Held close to the measurement rather than loosely, since the slack is what a drawing could
     * go missing in: a document that stops painting a drawing altogether is not this guard's
     * subject, but a reader that stops seeing them is exactly what the floor is here to catch.</p>
     */
    private static final int AT_LEAST_THIS_MANY_DRAWINGS = 750;

    /**
     * How many page fills the corpus holds, as a floor, each of which must come back placed from
     * the page: this is the reader's own proof that it can read a drawing placed from the page at
     * all. Measured at 32.
     */
    private static final int AT_LEAST_THIS_MANY_BACKGROUNDS = 30;

    @Test
    void everyDrawingOfTheCorpusIsCarriedByAParagraphBarTheOnesLedgered() throws Exception {
        Map<String, List<String>> onThePage = new TreeMap<>();
        List<String> problems = new ArrayList<>();
        List<String> stems = new ArrayList<>();
        int floating = 0;
        int backgrounds = 0;
        for (DocxCorpusDocument document : DocxFidelityCorpusTest.corpus()) {
            AtomicReference<DocxExportReport> report = new AtomicReference<>();
            byte[] docx;
            try (DocumentSession session = DocxFidelityCorpusTest.composed(document)) {
                docx = session.export(DocxSemanticBackend.builder().deterministic(true).reportSink(report::set).build());
            }
            assertLaidOut(document.stem(), report.get());
            DocxDrawingPlacements placements = DocxDrawingPlacements.read(docx);
            assertThat(placements.parts()).as("%s: its body read", document.stem()).contains("word/document.xml");
            placements.problems().forEach(problem -> problems.add(document.stem() + ": " + problem));
            // Every page fill belongs to the sheet: this is the reader showing it reads what a
            // drawing is placed from, and not a constant.
            assertThat(placements.backgrounds()).as("%s: its page fills placed from the page", document.stem())
                    .allMatch(DocxDrawingPlacements.Placement::anchoredToThePage);
            // And every floating drawing says the editors read where it is placed from at all.
            assertThat(placements.floatingPainted()).as("%s: its drawings placed from what they name",
                            document.stem())
                    .allMatch(drawing -> drawing.simplePos().equals("0"));
            stems.add(document.stem());
            floating += placements.floatingPainted().size();
            backgrounds += placements.backgrounds().size();
            List<String> left = placements.paintedOnThePage().stream()
                    .map(DocxDrawingPlacements.Placement::name).sorted().toList();
            if (!left.isEmpty()) {
                onThePage.put(document.stem(), left);
            }
        }

        assertThat(problems).as("every drawing of the corpus classified").isEmpty();
        assertThat(floating).as("the floating drawings the corpus paints, read")
                .isGreaterThanOrEqualTo(AT_LEAST_THIS_MANY_DRAWINGS);
        assertThat(backgrounds).as("the page fills the corpus holds, read")
                .isGreaterThanOrEqualTo(AT_LEAST_THIS_MANY_BACKGROUNDS);
        assertThat(STILL_ON_THE_PAGE.keySet()).as("the ledger names documents of the corpus").isSubsetOf(stems);
        Map<String, List<String>> ledgered = new TreeMap<>();
        STILL_ON_THE_PAGE.forEach((stem, still) ->
                ledgered.put(stem, still.named().stream().sorted().toList()));
        // The reasons go into the message, so that a failure says what the ledger claims about each
        // document and the reader can see at once which claim the change has broken.
        assertThat(onThePage).as("the drawings still placed from the page, against the ledger:%n%s", ledgerAsText())
                .isEqualTo(ledgered);
    }

    @Test
    void aDrawingWhoseOwnerWritesTextIsCarriedByAParagraph() throws Exception {
        // Every document is read before anything fails: the point of a document per writer of text
        // is to name which writer stopped offering its paragraph, and the first failure would
        // otherwise hide the other five.
        List<String> wrong = new ArrayList<>();
        for (FlowOwnerDocument document : FlowOwnerDrawingDocuments.documents()) {
            AtomicReference<DocxExportReport> report = new AtomicReference<>();
            byte[] docx;
            try (DocumentSession session = GraphCompose.document().create()) {
                document.compose().accept(session);
                docx = session.export(DocxSemanticBackend.builder().deterministic(true).reportSink(report::set).build());
            }
            assertLaidOut(document.name(), report.get());
            DocxDrawingPlacements placements = DocxDrawingPlacements.read(docx);

            placements.problems().forEach(problem -> wrong.add(document.name() + ": " + problem));
            if (placements.floatingPainted().size() != document.drawings()) {
                wrong.add(document.name() + ": floats " + placements.floatingPainted().size()
                          + " drawings, not the " + document.drawings() + " it paints");
            }
            // Not merely "not on the sheet": written as what moves with the text. An export that
            // put these in the text instead would leave nothing on the sheet either.
            for (DocxDrawingPlacements.Placement drawing : placements.floatingPainted()) {
                if (!drawing.placedFrom().equals("paragraph")) {
                    wrong.add(document.name() + ": " + drawing.name() + " is placed from \""
                              + drawing.placedFrom() + "\" and not down from a paragraph of "
                              + document.carrier());
                }
            }
        }

        assertThat(wrong).as("every drawing carried by the writer of text its owner holds").isEmpty();
    }

    // The reader, on files written by hand: it must find what it is to judge, wherever it stands,
    // and say so when it cannot.

    @Test
    void aDrawingPlacedFromItsParagraphIsCarriedAndOneFromThePageIsNot() throws Exception {
        DocxDrawingPlacements placements = DocxDrawingPlacements.read(docx(Map.of("word/document.xml",
                body(paragraph(anchor("Drawing 1", "paragraph", 1270))
                     + paragraph(anchor("Drawing 2", "page", 254000))))));

        assertThat(placements.problems()).isEmpty();
        assertThat(placements.floatingPainted()).hasSize(2);
        assertThat(placements.paintedOnThePage()).singleElement()
                .satisfies(drawing -> assertThat(drawing.name()).isEqualTo("Drawing 2"));
        assertThat(placements.placements().get(0).carriedByItsParagraph()).isTrue();
        assertThat(placements.placements().get(0).down()).isEqualTo(1270);
    }

    @Test
    void aPageFillIsToldFromADrawingANodePaints() throws Exception {
        DocxDrawingPlacements placements = DocxDrawingPlacements.read(docx(Map.of("word/header1.xml",
                header(paragraph(anchor("Page background 1", "page", 0))))));

        assertThat(placements.problems()).isEmpty();
        assertThat(placements.backgrounds()).singleElement()
                .satisfies(fill -> assertThat(fill.anchoredToThePage()).isTrue());
        assertThat(placements.painted()).isEmpty();
        assertThat(placements.paintedOnThePage()).isEmpty();
    }

    @Test
    void aDrawingIsFoundInAHeaderANestedTableAndATextBox() throws Exception {
        String nested = "<w:tbl><w:tr><w:tc><w:tbl><w:tr><w:tc>"
                        + paragraph(anchor("Drawing 1", "page", 0)) + "</w:tc></w:tr></w:tbl></w:tc></w:tr></w:tbl>";
        String textBox = paragraph("<w:r><w:drawing><wp:anchor simplePos=\"0\">"
                                   + "<wp:positionH relativeFrom=\"column\">"
                                   + "<wp:posOffset>0</wp:posOffset></wp:positionH>"
                                   + "<wp:positionV relativeFrom=\"paragraph\"><wp:posOffset>0</wp:posOffset>"
                                   + "</wp:positionV><wp:docPr id=\"2\" name=\"Drawing 2\"/>"
                                   + "<a:graphic><a:graphicData><wps:wsp><wps:txbx><w:txbxContent>"
                                   + paragraph(anchor("Drawing 3", "page", 0))
                                   + "</w:txbxContent></wps:txbx></wps:wsp></a:graphicData></a:graphic>"
                                   + "</wp:anchor></w:drawing></w:r>");
        DocxDrawingPlacements placements = DocxDrawingPlacements.read(docx(Map.of(
                "word/document.xml", body(nested + textBox),
                "word/footer1.xml", footer(paragraph(anchor("Drawing 4", "page", 0))))));

        assertThat(placements.problems()).isEmpty();
        assertThat(placements.painted()).extracting(DocxDrawingPlacements.Placement::name)
                .containsExactlyInAnyOrder("Drawing 1", "Drawing 2", "Drawing 3", "Drawing 4");
        assertThat(placements.paintedOnThePage()).extracting(DocxDrawingPlacements.Placement::name)
                .containsExactlyInAnyOrder("Drawing 1", "Drawing 3", "Drawing 4");
    }

    @Test
    void anInlineDrawingStandsInTheTextAndIsNotFloating() throws Exception {
        DocxDrawingPlacements placements = DocxDrawingPlacements.read(docx(Map.of("word/document.xml",
                body(paragraph("<w:r><w:drawing><wp:inline><wp:extent cx=\"100\" cy=\"100\"/>"
                               + "<wp:docPr id=\"1\" name=\"Drawing 1\"/></wp:inline></w:drawing></w:r>")))));

        assertThat(placements.problems()).isEmpty();
        // Counted as a drawing a node paints, but not as a floating one: it is placed from nothing.
        assertThat(placements.painted()).hasSize(1);
        assertThat(placements.floatingPainted()).isEmpty();
        assertThat(placements.placements()).singleElement().satisfies(drawing -> {
            assertThat(drawing.floating()).isFalse();
            assertThat(drawing.carriedByItsParagraph()).isTrue();
            assertThat(drawing.anchoredToThePage()).isFalse();
        });
    }

    @Test
    void aDrawingSentToAnAbsolutePointIsAProblemWhateverItIsPlacedFrom() throws Exception {
        // simplePos="1" takes the editors to wp:simplePos and leaves positionV unread, so a
        // drawing nailed to the sheet this way still says relativeFrom="paragraph".
        DocxDrawingPlacements placements = DocxDrawingPlacements.read(docx(Map.of("word/document.xml",
                body(paragraph("<w:r><w:drawing><wp:anchor simplePos=\"1\">"
                               + "<wp:simplePos x=\"914400\" y=\"914400\"/>" + ACROSS
                               + "<wp:positionV relativeFrom=\"paragraph\"><wp:posOffset>0</wp:posOffset>"
                               + "</wp:positionV>"
                               + "<wp:docPr id=\"1\" name=\"Drawing 1\"/></wp:anchor></w:drawing></w:r>")))));

        assertThat(placements.problems()).singleElement()
                .satisfies(problem -> assertThat(problem).contains("\"Drawing 1\"")
                        .contains("simplePos=\"1\"").contains("neither positionH nor positionV"));
        assertThat(placements.placements()).singleElement().satisfies(drawing -> {
            assertThat(drawing.placedFrom()).as("what it says it is placed from").isEqualTo("paragraph");
            assertThat(drawing.carriedByItsParagraph()).as("but no paragraph carries it").isFalse();
        });
    }

    @Test
    void aDrawingWithNoSimplePosIsPlacedFromWhatItNames() throws Exception {
        // The attribute defaults to false, so one left out is not a drawing sent to a point.
        DocxDrawingPlacements placements = DocxDrawingPlacements.read(docx(Map.of("word/document.xml",
                body(paragraph("<w:r><w:drawing><wp:anchor>" + ACROSS
                               + "<wp:positionV relativeFrom=\"paragraph\"><wp:posOffset>0</wp:posOffset>"
                               + "</wp:positionV>"
                               + "<wp:docPr id=\"1\" name=\"Drawing 1\"/></wp:anchor></w:drawing></w:r>")))));

        assertThat(placements.problems()).isEmpty();
        assertThat(placements.placements()).singleElement()
                .satisfies(drawing -> assertThat(drawing.carriedByItsParagraph()).isTrue());
    }

    @Test
    void anAnchorWithNothingToBePlacedFromIsAProblem() throws Exception {
        DocxDrawingPlacements placements = DocxDrawingPlacements.read(docx(Map.of("word/document.xml",
                body(paragraph("<w:r><w:drawing><wp:anchor simplePos=\"0\">" + ACROSS
                               + "<wp:docPr id=\"1\" name=\"Drawing 1\"/></wp:anchor></w:drawing></w:r>")))));

        assertThat(placements.problems()).singleElement()
                .satisfies(problem -> assertThat(problem).contains("\"Drawing 1\"")
                        .contains("names nothing to be placed down from"));
    }

    @Test
    void anAnchorPlacedDownTwiceIsAProblem() throws Exception {
        // What it is placed from would come from one of them and how far down from the other.
        DocxDrawingPlacements placements = DocxDrawingPlacements.read(docx(Map.of("word/document.xml",
                body(paragraph("<w:r><w:drawing><wp:anchor simplePos=\"0\">" + ACROSS
                               + "<wp:positionV relativeFrom=\"paragraph\"><wp:posOffset>1270</wp:posOffset>"
                               + "</wp:positionV>"
                               + "<wp:positionV relativeFrom=\"page\"><wp:align>top</wp:align></wp:positionV>"
                               + "<wp:docPr id=\"1\" name=\"Drawing 1\"/></wp:anchor></w:drawing></w:r>")))));

        assertThat(placements.problems()).anySatisfy(problem -> assertThat(problem)
                .contains("\"Drawing 1\"").contains("carries more than one wp:positionV"));
    }

    @Test
    void anAnchorPlacedFromSomethingElseIsAProblem() throws Exception {
        DocxDrawingPlacements placements = DocxDrawingPlacements.read(docx(Map.of("word/document.xml",
                body(paragraph(anchor("Drawing 1", "margin", 0))))));

        assertThat(placements.problems()).singleElement()
                .satisfies(problem -> assertThat(problem).contains("placed down from \"margin\""));
        assertThat(placements.paintedOnThePage()).as("not read as the sheet, and not passed over either").isEmpty();
    }

    @Test
    void anAnchorPlacedAcrossFromSomethingElseIsAProblem() throws Exception {
        DocxDrawingPlacements placements = DocxDrawingPlacements.read(docx(Map.of("word/document.xml",
                body(paragraph("<w:r><w:drawing><wp:anchor simplePos=\"0\">"
                               + "<wp:positionH relativeFrom=\"margin\"><wp:posOffset>0</wp:posOffset>"
                               + "</wp:positionH>"
                               + "<wp:positionV relativeFrom=\"paragraph\"><wp:posOffset>0</wp:posOffset>"
                               + "</wp:positionV>"
                               + "<wp:docPr id=\"1\" name=\"Drawing 1\"/></wp:anchor></w:drawing></w:r>")))));

        assertThat(placements.problems()).singleElement()
                .satisfies(problem -> assertThat(problem).contains("placed across from \"margin\""));
    }

    @Test
    void anAnchorWithNoNameOrANameOfItsOwnIsAProblem() throws Exception {
        DocxDrawingPlacements placements = DocxDrawingPlacements.read(docx(Map.of("word/document.xml",
                body(paragraph("<w:r><w:drawing><wp:anchor simplePos=\"0\">" + ACROSS
                               + "<wp:positionV relativeFrom=\"page\">"
                               + "<wp:posOffset>0</wp:posOffset></wp:positionV></wp:anchor></w:drawing></w:r>")
                     + paragraph(anchor("Chart 7", "page", 0))))));

        assertThat(placements.problems()).satisfiesExactly(
                problem -> assertThat(problem).contains("an unnamed drawing").contains("carries no wp:docPr name"),
                problem -> assertThat(problem).contains("\"Chart 7\"")
                        .contains("named neither a drawing nor a page background"));
    }

    @Test
    void aKeywordWhereTheOffsetShouldBeIsAProblemAndNotNought() throws Exception {
        // wp:align in place of wp:posOffset is schema-legal and says a keyword — "bottom" — where
        // the export says a number. Read as a missing offset it would come out as nought, which is
        // a drawing sitting exactly on its paragraph's top: a reading, and a wrong one.
        DocxDrawingPlacements placements = DocxDrawingPlacements.read(docx(Map.of("word/document.xml",
                body(paragraph("<w:r><w:drawing><wp:anchor simplePos=\"0\">" + ACROSS
                               + "<wp:positionV relativeFrom=\"paragraph\"><wp:align>bottom</wp:align>"
                               + "</wp:positionV>"
                               + "<wp:docPr id=\"1\" name=\"Drawing 1\"/></wp:anchor></w:drawing></w:r>")))));

        assertThat(placements.problems()).singleElement()
                .satisfies(problem -> assertThat(problem).contains("\"Drawing 1\"")
                        .contains("says how far down in nothing this reader can read"));
        assertThat(placements.placements()).singleElement()
                .satisfies(drawing -> assertThat(drawing.down()).as("not read as nought").isEqualTo(Long.MIN_VALUE));
    }

    @Test
    void anOffsetThatIsNoNumberIsAProblem() throws Exception {
        DocxDrawingPlacements placements = DocxDrawingPlacements.read(docx(Map.of("word/document.xml",
                body(paragraph("<w:r><w:drawing><wp:anchor simplePos=\"0\">" + ACROSS
                               + "<wp:positionV relativeFrom=\"paragraph\">"
                               + "<wp:posOffset>somewhere</wp:posOffset></wp:positionV>"
                               + "<wp:docPr id=\"1\" name=\"Drawing 1\"/></wp:anchor></w:drawing></w:r>")))));

        assertThat(placements.problems()).singleElement()
                .satisfies(problem -> assertThat(problem).contains("says how far down in nothing this reader can read"));
    }

    /** Placed across from the page's edge, so a fixture may be about what it is placed down from. */
    private static final String ACROSS =
            "<wp:positionH relativeFrom=\"page\"><wp:posOffset>0</wp:posOffset></wp:positionH>";

    /** A floating drawing, placed across from the page and down from what it is given. */
    private static String anchor(String name, String placedFrom, long down) {
        return "<w:r><w:drawing><wp:anchor simplePos=\"0\">"
               + "<wp:positionH relativeFrom=\"page\"><wp:posOffset>127000</wp:posOffset></wp:positionH>"
               + "<wp:positionV relativeFrom=\"" + placedFrom + "\"><wp:posOffset>" + down
               + "</wp:posOffset></wp:positionV>"
               + "<wp:extent cx=\"100\" cy=\"100\"/>"
               + "<wp:docPr id=\"1\" name=\"" + name + "\"/>"
               + "</wp:anchor></w:drawing></w:r>";
    }
}
