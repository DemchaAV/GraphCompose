package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxExportReport;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.demcha.compose.document.templates.fidelity.DocxFixtures.assertLaidOut;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * No drawing behind the text may lie in the box of a painted panel standing beside it, bar the
 * ones this test names one by one.
 *
 * <p>Both editors paint a shaded cell's fill as part of the text layer, over whatever is drawn
 * behind it. A drawing that stands beside such a panel — not inside it — and reaches into its box
 * is covered by that fill, however the page stacks the two. Measured in Word 16.0.20527 and
 * LibreOffice 24.2 at 300 dpi on the one corpus document that holds the case: the engine's PDF
 * draws 4308 pixels of the icon's own ink, Word's draws 0 and LibreOffice's draws 0, the icon's
 * whole box coming back the card's colour in both.</p>
 *
 * <p>The judging is geometric, not a reading of {@code behindDoc}. That attribute is set on
 * drawings the reader plainly sees as well: the same fixture with its panel unpainted keeps
 * {@code behindDoc="1"} on its anchor while showing all 3237 pixels of its shape in Word. What
 * decides is whether a painted box lies over the drawing's, so that is what this test computes,
 * from the boxes the layout gave the nodes.</p>
 *
 * <p>It then holds the export's report to the same answer, each drawing checked on its own. The
 * export judges the box of the ink it writes; this test judges the box the layout gave the node
 * that paints it, which holds that ink. So a drawing the export names must come back, in this
 * test's own geometry, lying under some painted panel it does not stand inside — and where this
 * test cannot place it at all, it says so in {@link #NAMED_BUT_UNPLACEABLE} rather than calling it
 * a disagreement. The other way round is not an identity but a difference worth seeing — a node
 * whose box is larger than its ink, a stroke that reaches past it — so what this test finds and
 * the export does not is held in {@link #SEEN_BY_THE_SCAN_ALONE}, empty today.</p>
 *
 * <p>It fails closed, on positive signals rather than on the absence of a complaint. A fixture
 * built for the case must come back named, so a detector that stops seeing its target fails here
 * rather than reporting a clean corpus ({@link #aDrawingAPaintedNeighbourIsLaidOverInTurnIsAlwaysNamed},
 * {@link #aDrawingOverAPaintedNeighbourIsCarriedInFrontOfIt}). The
 * corpus must come back with its documents laid out and with the hundreds of drawings it paints.
 * A note that says a shape was drawn and that this test cannot classify is a failure rather than a
 * silence: the export has four notes whose wording begins that way and only two of them are a
 * drawing's, so a fifth would otherwise narrow the scan without a word. And a painted panel the
 * layout broke across pages is named rather than skipped in silence: its box is the one its first
 * page gave it, so no overlap can be judged against it, and a change that starts breaking panels
 * across pages would otherwise shrink what is judged while this test stayed green.</p>
 *
 * <p>What neither the export nor this test judges, written out so that none of it is a silence,
 * and none of it held by an assertion — these are the edges of what is checked, not invariants.
 * A panel composed in an authored table cell, which the layout gives no box of its own. A table
 * cell's own shading, which belongs to a cell rather than to a node. A cell's paint the table
 * draws itself, which never passes the export's queue. The second panel over a drawing lying over
 * two, the export naming one. And the drawings this test's own scan does not know by their note —
 * a rail drawn as a line, a picture drawn over the badge holding it — which the export judges and
 * this test checks one by one instead ({@link #NAMED_BUT_UNPLACEABLE}).</p>
 *
 * <p>The ledger is empty. The stack's order is carried into the document — a drawing on a layer the
 * page paints after a painted layer of the same stack stands in front of the text — and the one
 * instance the corpus held, the icon over a card's corner, is drawn in front and covered by
 * nothing. An entry coming back is a drawing the export has gone back to losing under a
 * neighbour's fill, and this test then fails until either the export or the ledger is put right.
 * What keeps a drawing behind the text on purpose — a painted layer the page lays over it in turn,
 * a box holding a line of the flow, another drawing of the stack that one of those keeps there —
 * the export names, and the corpus holds no instance of the first two (so the fixtures below carry
 * the positive signal that it still can).</p>
 *
 * <p>What this corpus can and cannot say is written down in numbers rather than left to a reader of
 * empty lists: it lifts one drawing ({@link #LIFTED_IN_THE_CORPUS}) and offers no pair at all to
 * judge the two lists of {@link #PAIRS_THE_CORPUS_OFFERS} over. Those rules are held by the
 * fixtures of {@code DocxDrawingUnderANeighboursFillTest}, and what is held here is that the
 * corpus has not started asking something new.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxNeighbourFillGuardTest {

    /**
     * Empty: no drawing of the corpus is left under a neighbour's fill.
     *
     * <p>It held one entry — the icon laid over a card's top right corner in
     * {@code synthetic-icon_over_card}, whose 18pt box stands wholly inside the card painted on the
     * layer below, and of which neither editor drew one pixel. Carrying the stack's order into the
     * document draws that icon in front of the text, where the card's shading does not reach it,
     * and the export names nothing for it. That was the corpus's only instance: of the seventy-one
     * documents, sixty-four are presets — twenty-six CVs, seventeen invoices, fifteen letters, four
     * proposals, a receipt and a rota — and not one of them lays a drawing over a painted
     * neighbour.</p>
     */
    private static final Map<String, List<String>> UNDER_A_NEIGHBOURS_FILL = Map.of();

    /**
     * Drawings this test's own scan finds under a painted neighbour that the export does not name.
     * A node's box can be larger than the ink drawn in it, so a difference here is expected rather
     * than wrong — but it is the difference between two measurements of the same thing, and it is
     * held rather than waved through. Empty today.
     */
    private static final Map<String, List<String>> SEEN_BY_THE_SCAN_ALONE = Map.of();

    /**
     * Drawings the export names that this test cannot place, and so cannot check. A drawing is
     * drawn under the path of the node that paints it, but a pass of the layout's own — a
     * timeline's rail — is drawn under the pass's path, which no node of the graph carries, so
     * this test has no box for it. Empty today: no rail of the corpus comes to rest in a painted
     * panel's box. One that did would be a real loss, named by the export and unchecked here, and
     * this list is where that would have to be said out loud.
     */
    private static final List<String> NAMED_BUT_UNPLACEABLE = List.of();

    /**
     * Candidate panels — nodes carrying a fill, which the export may or may not have shaded —
     * that the layout broke across pages. A panel's box is the one its first page gave it, so no
     * overlap can be judged against it, and these four are where this test stops looking: named,
     * not counted, so that a change which starts breaking a different one across pages is seen
     * here instead of quietly shrinking what is judged.
     */
    private static final List<String> PANELS_ACROSS_PAGES = List.of(
            "invoice-obsidian_long: ObsidianInvoice[0]/LineItemsCard[3]",
            "synthetic-break_in_panel: ContainerNode[0]/SectionNode[1]",
            "synthetic-break_in_panel: ContainerNode[0]/SectionNode[1]/SectionNode[4]",
            "synthetic-card_over_pages: ContainerNode[0]/SectionNode[1]");

    /**
     * The nodes of the corpus whose notes this test classifies as a drawing's, in front of the
     * text and behind it alike — 503 today, counted once per node however many shapes it is drawn
     * from. A floor rather than the number, and well under it, so that it catches a reader that
     * has stopped seeing drawings rather than a document gained or lost.
     */
    private static final int AT_LEAST_THIS_MANY_DRAWINGS = 450;

    private static final String COVERED = "a panel painted beside it";

    /** The two notes a drawing gets, by which this test knows a note is a drawing's at all. */
    private static final String DRAWN_IN_ITS_BLOCK = "drawn as a shape where the layout puts it, ";
    private static final String DRAWN_IN_ITS_CELL = "drawn as a shape anchored in the table cell it fills";

    /** What a drawing's note says when the export put it in front of the text. */
    private static final String IN_FRONT = "in front of the text";

    /** What it says when the reason is a layer the page paints under the drawing carrying a fill. */
    private static final String LIFTED_OVER_A_NEIGHBOUR =
            "in front of the text, since a layer the page paints under it carries a fill";

    /**
     * Drawings the export lifted in front of the text whose box lies in a painted panel the page
     * paints <em>after</em> them. Empty, and empty for want of a pair rather than for want of a
     * fault: see {@link #PAIRS_THE_CORPUS_OFFERS}.
     */
    private static final List<String> LIFTED_UNDER_A_LATER_PANEL = List.of();

    /**
     * Drawings the export lifted in front, and panels, whose place in the paint order this test
     * cannot read, the page painting no fragment under their own path. Empty today: a pair that
     * cannot be judged would otherwise be a pair quietly not judged.
     */
    private static final List<String> UNPLACEABLE = List.of();

    /**
     * Drawings the export lifted in front of the text that lie over a drawing left behind which
     * the page paints after them — where the file would invert the two, Word drawing every shape
     * in front over every shape behind. Empty, and empty for want of a pair rather than for want
     * of a fault: see {@link #PAIRS_THE_CORPUS_OFFERS}.
     */
    private static final List<String> LIFTED_OVER_A_LATER_DRAWING = List.of();

    /**
     * How many (lifted drawing, later panel or drawing) pairs the two lists above are judged over.
     *
     * <p><strong>Zero.</strong> Measured, and said out loud rather than left for a reader to take
     * the two empty lists for a clean corpus: of the drawings the export lifts here, not one has a
     * painted panel or a drawing left behind that the page paints after it, so neither list can
     * have anything in it whatever the export does. The rules those lists stand for are held by
     * the unit fixtures written for them in {@code DocxDrawingUnderANeighboursFillTest}; the
     * number is asserted exactly so that a corpus which starts offering a pair is seen here
     * instead of quietly making the lists mean something new.</p>
     */
    private static final int PAIRS_THE_CORPUS_OFFERS = 0;

    /**
     * The drawings of the corpus the export lifts in front of the text over a painted layer under
     * them.
     *
     * <p><strong>One:</strong> the icon over the card, the case this was measured on. Asserted
     * exactly rather than as a floor, so that a corpus which starts lifting more — or stops
     * lifting this one — is seen here. The seventy other documents lift nothing: thirty-seven
     * drawings of {@code cv-sidebar_portrait} stand over its painted sidebar, but one of them
     * frames the photo it rings, and the stack's order is taken whole or not at all, so none of
     * them is lifted and that document is byte for byte what the export wrote before.</p>
     */
    private static final int LIFTED_IN_THE_CORPUS = 1;

    @Test
    void everyDrawingUnderANeighboursFillIsLedgered() throws Exception {
        Map<String, List<String>> found = new TreeMap<>();
        Map<String, List<String>> scanOnly = new TreeMap<>();
        List<String> namedButUnplaceable = new ArrayList<>();
        List<String> namedButNotCovered = new ArrayList<>();
        List<String> unclassified = new ArrayList<>();
        List<String> unjudged = new ArrayList<>();
        List<String> acrossPages = new ArrayList<>();
        List<String> liftedUnderALater = new ArrayList<>();
        List<String> liftedOverALater = new ArrayList<>();
        List<String> unplaceable = new ArrayList<>();
        List<String> stems = new ArrayList<>();
        int drawings = 0;
        int lifted = 0;
        int pairs = 0;
        for (DocxCorpusDocument document : DocxFidelityCorpusTest.corpus()) {
            AtomicReference<DocxExportReport> report = new AtomicReference<>();
            Scan scan;
            try (DocumentSession session = DocxFidelityCorpusTest.composed(document)) {
                session.export(DocxSemanticBackend.builder().deterministic(true)
                        .reportSink(report::set).build());
                assertLaidOut(document.stem(), report.get());
                scan = scan(session, report.get());
            }
            stems.add(document.stem());
            drawings += scan.drawings();
            lifted += scan.lifted();
            pairs += scan.pairs();
            scan.liftedUnderALater().forEach(one -> liftedUnderALater.add(document.stem() + ": " + one));
            scan.liftedOverALater().forEach(one -> liftedOverALater.add(document.stem() + ": " + one));
            scan.unplaceable().forEach(one -> unplaceable.add(document.stem() + ": " + one));
            scan.panelsAcrossPages().forEach(panel -> acrossPages.add(document.stem() + ": " + panel));
            scan.unjudged().forEach(path -> unjudged.add(document.stem() + ": " + path));
            scan.unclassified().forEach(note -> unclassified.add(document.stem() + ": " + note));

            List<String> named = report.get().notes().stream()
                    .filter(note -> note.detail() != null && note.detail().contains(COVERED))
                    .map(DocxExportReport.Note::path).sorted().toList();
            List<String> seen = scan.covered().stream().sorted().toList();
            // Every drawing the export names is checked against this test's own geometry, one by
            // one. Not through the scan: the scan knows a drawing by the wording of its note, and
            // the export names drawings whose notes it does not know — a rail is drawn as a line,
            // a picture over a badge says so instead. Those land in namedButUnplaceable rather
            // than being read as a disagreement.
            for (String path : named) {
                if (!scan.placed().containsKey(path)) {
                    namedButUnplaceable.add(document.stem() + ": " + path);
                } else if (!coveredByAPanel(path, scan)) {
                    namedButNotCovered.add(document.stem() + ": " + path);
                }
            }
            List<String> only = seen.stream().filter(path -> !named.contains(path)).toList();
            if (!only.isEmpty()) {
                scanOnly.put(document.stem(), only);
            }
            if (!named.isEmpty()) {
                found.put(document.stem(), named);
            }
        }

        assertThat(drawings).as("the drawings the corpus paints, read")
                .isGreaterThanOrEqualTo(AT_LEAST_THIS_MANY_DRAWINGS);
        assertThat(unclassified).as("every note that says a shape was drawn is one this test knows: "
                                    + "one it does not know is a drawing it would not scan, and so "
                                    + "one it could not find under a panel")
                .isEmpty();
        assertThat(unjudged).as("every drawing of the corpus given a box to judge").isEmpty();
        assertThat(acrossPages.stream().sorted().toList())
                .as("candidate panels broken across pages, which no overlap is judged against: "
                    + "when this list grows, what is judged has shrunk")
                .isEqualTo(PANELS_ACROSS_PAGES);
        assertThat(namedButUnplaceable)
                .as("drawings the export names that this test cannot place, and so cannot check: "
                    + "a drawing drawn under a path of the layout's own rather than a node's")
                .isEqualTo(NAMED_BUT_UNPLACEABLE);
        assertThat(namedButNotCovered)
                .as("the export names a drawing as covered that this test can place and finds "
                    + "under no painted panel: the ink the export judged lies in the box this "
                    + "test judged, so the two cannot disagree while both read one layout")
                .isEmpty();
        assertThat(scanOnly).as("drawings this test finds under a neighbour's fill that the export "
                                + "does not name")
                .isEqualTo(new TreeMap<>(SEEN_BY_THE_SCAN_ALONE));
        assertThat(UNDER_A_NEIGHBOURS_FILL.keySet()).as("the ledger names documents of the corpus")
                .isSubsetOf(stems);
        assertThat(found).as("every drawing under a neighbour's fill is the one the ledger names")
                .isEqualTo(new TreeMap<>(UNDER_A_NEIGHBOURS_FILL));
        assertThat(lifted).as("the drawings the export lifts in front over a painted layer under "
                             + "them, which the two lists below are judged over")
                .isEqualTo(LIFTED_IN_THE_CORPUS);
        assertThat(unplaceable.stream().distinct().sorted().toList())
                .as("every lifted drawing, and everything judged against one, has a place in the "
                    + "page's paint order to judge by")
                .isEqualTo(UNPLACEABLE);
        assertThat(pairs).as("the pairs the two lists below are judged over: nothing found means "
                             + "nothing only where something was looked at, and here nothing is")
                .isEqualTo(PAIRS_THE_CORPUS_OFFERS);
        assertThat(liftedUnderALater.stream().sorted().toList())
                .as("no drawing is lifted in front of a painted panel the page paints after it, "
                    + "where in front is the same loss the other way round")
                .isEqualTo(LIFTED_UNDER_A_LATER_PANEL);
        assertThat(liftedOverALater.stream().sorted().toList())
                .as("no drawing is lifted over a drawing left behind that the page paints after "
                    + "it, which the file would invert")
                .isEqualTo(LIFTED_OVER_A_LATER_DRAWING);
    }

    @Test
    void aDrawingAPaintedNeighbourIsLaidOverInTurnIsAlwaysNamed() throws Exception {
        // The positive signal the corpus pass is held to. With the ledger empty, an export that
        // stopped seeing this case altogether would leave the corpus clean and nothing failing,
        // so a fixture that must come back named carries the fail-closed half. The pin lies
        // between two painted bands: the page paints one under it and one over it, so it belongs
        // behind the text — in front it would cover the upper band's shading — and the lower
        // band's shading covers it there.
        //
        // Deliberately the same document as the unit test of the same case in render-docx, so
        // that the two modules are held to one answer: it is a copy on purpose, not a duplicate
        // to be tidied away.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.add(new LayerStackBuilder().name("Stack")
                    .layer(new SectionBuilder().name("Lower").fillColor(DocumentColor.rgb(90, 120, 200))
                            .fixedWidth(200).addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                            LayerAlign.TOP_LEFT, 0)
                    .position(new ShapeBuilder().name("Pin").size(30, 14)
                            .fillColor(DocumentColor.rgb(26, 86, 148)).build(),
                            150, 2, LayerAlign.TOP_LEFT, 1)
                    .layer(new SectionBuilder().name("Upper").fillColor(DocumentColor.rgb(10, 170, 90))
                            .fixedWidth(200).addParagraph(p -> p.name("Over").text("Over it")).build(),
                            LayerAlign.TOP_LEFT, 2)
                    .build()));
            session.export(new DocxSemanticBackend(report::set));
        }

        assertThat(report.get().notes()).as("the pin under the lower band's fill, named")
                .anyMatch(note -> note.detail() != null && note.detail().contains(COVERED)
                                  && note.path() != null && note.path().contains("Pin"));
    }

    @Test
    void aDrawingOverAPaintedNeighbourIsCarriedInFrontOfIt() throws Exception {
        // The other half of the positive signal: the empty ledger is a measurement, and this is
        // the behaviour that made it empty. The same stack less the band the page paints over the
        // pin — the one difference — and the pin is drawn in front, named as covered by nothing.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.add(new LayerStackBuilder().name("Stack")
                    .layer(new SectionBuilder().name("Band").fillColor(DocumentColor.rgb(90, 120, 200))
                            .fixedWidth(200).addParagraph(p -> p.name("Stage").text("Stage two")).build(),
                            LayerAlign.TOP_LEFT, 0)
                    .position(new ShapeBuilder().name("Pin").size(30, 14)
                            .fillColor(DocumentColor.rgb(26, 86, 148)).build(),
                            150, 2, LayerAlign.TOP_LEFT, 1)
                    .build()));
            session.export(new DocxSemanticBackend(report::set));
        }

        assertThat(report.get().notes())
                .as("nothing covers the pin, so nothing is named for it")
                .noneMatch(note -> note.detail() != null && note.detail().contains(COVERED));
        assertThat(report.get().notes())
                .as("and the export says it stands in front because of the layer under it")
                .anyMatch(note -> note.path() != null && note.path().contains("Pin")
                                  && note.detail() != null
                                  && note.detail().contains(LIFTED_OVER_A_NEIGHBOUR));
    }

    /**
     * What one document's scan found.
     *
     * @param lifted           the drawings the export put in front of the text because a painted
     *                         layer stands under them
     * @param liftedUnderALater the ones of those whose box lies in a painted panel the page paints
     *                         <em>after</em> them, where being in front is the loss the other way
     *                         round
     * @param liftedOverALater  the ones lying over a drawing left behind that the page paints
     *                         after them, where the file inverts the two
     * @param unplaceable      the drawings and panels the page paints no fragment of under their
     *                         own path, so that their place in the paint order cannot be read
     * @param pairs            how many (lifted, later thing) pairs the two lists above were
     *                         judged over: nothing found means nothing only if something was
     *                         looked at
     */
    private record Scan(int drawings, List<String> panelsAcrossPages, List<String> covered,
                        List<String> unjudged, List<String> unclassified,
                        Map<String, PlacedNode> placed, List<String> panels,
                        int lifted, List<String> liftedUnderALater, List<String> liftedOverALater,
                        List<String> unplaceable, int pairs) {
    }

    /** Whether a path's box lies in the box of a painted panel it does not stand inside. */
    private static boolean coveredByAPanel(String path, Scan scan) {
        for (String panel : scan.panels()) {
            if (path.equals(panel) || path.startsWith(panel + "/") || panel.startsWith(path + "/")) {
                continue;
            }
            if (overlaps(scan.placed().get(path), scan.placed().get(panel))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds, from the boxes the layout gave the nodes, every drawing behind the text whose box
     * lies in a painted panel it does not stand inside.
     *
     * <p>Which nodes were drawn as shapes, and which of them stand in front of the text, is read
     * off the export's report: the export is the only one that knows what it drew. Where each
     * stands on the page, and which boxes lie over which, is this test's own.</p>
     */
    private static Scan scan(DocumentSession session, DocxExportReport report) {
        Map<String, PlacedNode> placed = new LinkedHashMap<>();
        for (PlacedNode node : session.layoutGraph().nodes()) {
            placed.putIfAbsent(node.path(), node);
        }
        Map<DocumentNode, String> paths = new IdentityHashMap<>();
        List<DocumentNode> roots = session.roots();
        for (int index = 0; index < roots.size(); index++) {
            indexPaths(roots.get(index), null, index, paths);
        }

        // Where the page paints each path, first fragment first: the engine's own order, not a
        // reading of the tree, so that the order this test judges by is not the order the export
        // judged by.
        Map<String, Integer> paintedAt = new LinkedHashMap<>();
        List<com.demcha.compose.document.layout.PlacedFragment> painted =
                session.layoutGraph().fragments();
        for (int place = 0; place < painted.size(); place++) {
            paintedAt.putIfAbsent(painted.get(place).path(), place);
        }

        List<String> behind = new ArrayList<>();
        List<String> lifted = new ArrayList<>();
        Set<String> asShapes = new HashSet<>();
        List<String> unjudged = new ArrayList<>();
        List<String> unclassified = new ArrayList<>();
        for (DocxExportReport.Note note : report.notes()) {
            String detail = note.detail() == null ? "" : note.detail();
            if (!detail.contains("drawn as a shape")) {
                continue;
            }
            if (!detail.contains(DRAWN_IN_ITS_BLOCK) && !detail.contains(DRAWN_IN_ITS_CELL)) {
                // A row whose paint went unwritten says a shape was drawn too, and is no drawing
                // of the flow. Held rather than passed over: the scan would be narrower than the
                // export by exactly this note, and a drawing it stopped seeing is one it could
                // no longer find under a panel.
                unclassified.add(note.subject() + " " + note.path());
                continue;
            }
            if (note.path() == null) {
                unclassified.add(note.subject() + " with no path");
                continue;
            }
            asShapes.add(note.path());
            if (!placed.containsKey(note.path())) {
                unjudged.add(note.path());
            } else if (!detail.contains(IN_FRONT)) {
                behind.add(note.path());
            } else if (detail.contains(LIFTED_OVER_A_NEIGHBOUR)) {
                lifted.add(note.path());
            }
        }

        List<String> panels = new ArrayList<>();
        List<String> acrossPages = new ArrayList<>();
        for (Map.Entry<DocumentNode, String> entry : paths.entrySet()) {
            String path = entry.getValue();
            PlacedNode box = placed.get(path);
            // A node the report says was drawn as a shape is that drawing and not a panel,
            // whatever its fill: a filled shape container is written as a shaded cell only
            // where it was not drawn.
            if (box == null || asShapes.contains(path) || !mayShadeItsCell(entry.getKey())) {
                continue;
            }
            if (box.startPage() != box.endPage()) {
                acrossPages.add(path);
            } else {
                panels.add(path);
            }
        }

        List<String> covered = new ArrayList<>();
        for (String drawing : behind) {
            for (String panel : panels) {
                if (drawing.equals(panel) || drawing.startsWith(panel + "/")
                    || panel.startsWith(drawing + "/")) {
                    continue;
                }
                if (overlaps(placed.get(drawing), placed.get(panel))) {
                    covered.add(drawing);
                    break;
                }
            }
        }
        // The other way round, twice. A drawing the export lifted in front is painted over
        // everything of the text layer and over every drawing left behind it, so two things must
        // not lie under it: a painted panel the page paints AFTER it, whose shading the drawing
        // would cover instead of being covered by, and a drawing left behind that the page paints
        // after it, whose place over it the file would invert. Judged on the page's own paint
        // order and this test's own geometry, pair by pair, with the pairs counted so that
        // "nothing found" cannot mean "nothing looked at".
        List<String> liftedUnderALater = new ArrayList<>();
        List<String> liftedOverALater = new ArrayList<>();
        List<String> unplaceable = new ArrayList<>();
        int pairs = 0;
        for (String drawing : lifted) {
            Integer drawnAt = paintedAt.get(drawing);
            if (drawnAt == null) {
                unplaceable.add("lifted " + drawing);
                continue;
            }
            for (String panel : panels) {
                Integer panelAt = paintedAt.get(panel);
                if (panelAt == null) {
                    unplaceable.add("panel " + panel);
                    continue;
                }
                if (panelAt <= drawnAt || panel.startsWith(drawing + "/")
                    || drawing.startsWith(panel + "/")) {
                    continue;
                }
                pairs++;
                if (overlaps(placed.get(drawing), placed.get(panel))) {
                    liftedUnderALater.add(drawing + " under " + panel);
                }
            }
            for (String other : behind) {
                Integer otherAt = paintedAt.get(other);
                if (otherAt == null) {
                    unplaceable.add("behind " + other);
                    continue;
                }
                if (otherAt <= drawnAt || other.startsWith(drawing + "/")
                    || drawing.startsWith(other + "/")) {
                    continue;
                }
                pairs++;
                if (overlaps(placed.get(drawing), placed.get(other))) {
                    liftedOverALater.add(drawing + " over " + other);
                }
            }
        }
        return new Scan(asShapes.size(), acrossPages, covered, unjudged, unclassified, placed, panels,
                lifted.size(), liftedUnderALater, liftedOverALater, unplaceable, pairs);
    }

    /** Whether two boxes stand on one page and share any area. */
    private static boolean overlaps(PlacedNode a, PlacedNode b) {
        return a.startPage() == b.startPage()
               && a.placementX() < b.placementX() + b.placementWidth()
               && b.placementX() < a.placementX() + a.placementWidth()
               && a.placementY() < b.placementY() + b.placementHeight()
               && b.placementY() < a.placementY() + a.placementHeight();
    }

    /**
     * Whether the export may write this node as a cell with shading, whose fill is part of the
     * text layer: a section, a container or a row that carries a fill, and a filled shape
     * container too.
     *
     * <p>This is broader than what the export actually shades, on purpose. The export records its
     * panels where it writes their fill, so it knows that a row's paint goes unwritten where its
     * panel cannot hold it, that a container's does where it is a band's layer or a stack's
     * column, and that a filled shape container is sometimes a band instead. From the tree none of
     * that can be told. Reading the tree too narrowly would make this test blind where the export
     * sees; reading it broadly can only add candidates, and a candidate the export does not name
     * lands in {@link #SEEN_BY_THE_SCAN_ALONE} rather than passing unnoticed.</p>
     */
    private static boolean mayShadeItsCell(DocumentNode node) {
        if (node instanceof SectionNode section) {
            return section.fillColor() != null;
        }
        if (node instanceof ContainerNode container) {
            return container.fillColor() != null;
        }
        if (node instanceof RowNode row) {
            return row.fillColor() != null;
        }
        return node instanceof ShapeContainerNode shape && shape.fillColor() != null;
    }

    /** The path the report names a node by: its name or kind, and its place among its siblings. */
    private static void indexPaths(DocumentNode node, String parentPath, int childIndex,
                                   Map<DocumentNode, String> into) {
        if (node == null) {
            return;
        }
        String base = node.name() == null || node.name().isBlank() ? node.nodeKind() : node.name().trim();
        String segment = base.replace('\\', '_').replace('/', '_') + "[" + childIndex + "]";
        String path = parentPath == null ? segment : parentPath + "/" + segment;
        into.put(node, path);
        List<DocumentNode> children = node.children();
        for (int index = 0; index < children.size(); index++) {
            indexPaths(children.get(index), path, index, into);
        }
    }
}
