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
 * <p>It then holds the export's report to the same answer, in the one direction where the two are
 * bound to agree. The export judges the box of the ink it writes; this test judges the box the
 * layout gave the node that paints it, which holds that ink. So everything the export names this
 * test must find, and {@link #everyDrawingUnderANeighboursFillIsLedgered} asserts exactly that. The
 * other way round is not an identity but a difference worth seeing — a node whose box is larger
 * than its ink, a stroke that reaches past it — so what this test finds and the export does not is
 * held in a list of its own ({@link #SEEN_BY_THE_SCAN_ALONE}), empty today.</p>
 *
 * <p>It fails closed, on positive signals rather than on the absence of a complaint. A fixture
 * built for the case must come back named, so a detector that stops seeing its target fails here
 * rather than reporting a clean corpus ({@link #aDrawingOverAPaintedNeighbourIsAlwaysNamed}). The
 * corpus must come back with its documents laid out and with the hundreds of drawings it paints.
 * A note that says a shape was drawn and that this test cannot classify is a failure rather than a
 * silence: the export has four notes whose wording begins that way and only two of them are a
 * drawing's, so a fifth would otherwise be read as a drawing behind the text and judged as one.
 * And a painted panel the layout broke across pages is named rather than skipped in silence: its
 * box is the one its first page gave it, so no overlap can be judged against it, and a change that
 * starts breaking panels across pages would otherwise shrink what is judged while this test stayed
 * green.</p>
 *
 * <p>Two things neither the export nor this test judges, both named here so that neither is a
 * silence: a table cell's own shading, which belongs to a cell rather than to a node with a box of
 * its own, and the second panel over a drawing that lies over two, the export naming one.</p>
 *
 * <p>The list is a ledger of work still to do, not a licence: the export has no drawing it is
 * content to leave under a neighbour's fill. The entry goes when the stack's order is carried into
 * the document, and this test then fails until the ledger is brought down to match.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxNeighbourFillGuardTest {

    /**
     * The icon laid over a card's top right corner through a layer stack: the card is a painted
     * panel on the layer below, and the icon's 18pt box stands wholly inside it. Both editors
     * paint the card's fill over it, and neither draws one pixel of its ink.
     *
     * <p>It is the corpus's only instance, and it is a synthetic document rather than a shipped
     * preset: of the seventy-one, sixty-four are presets — twenty-six CVs, seventeen invoices,
     * fifteen letters, four proposals, a receipt and a rota — and not one of them lays a drawing
     * over a painted neighbour. The synthetic document was not written for this case either. It
     * joined the corpus to hold a band's space above and below the panel that opens it, and the
     * commit that added it says in as many words that the fidelity gate reads lines of text, so
     * where the icon stands it does not see. The case is therefore measured and real, and no
     * preset is known to reach it.</p>
     */
    private static final Map<String, List<String>> UNDER_A_NEIGHBOURS_FILL = Map.of(
            "synthetic-icon_over_card",
            List.of("ContainerNode[0]/LayerStackNode[1]/SvgIcon[1]/SvgLayer0[0]"));

    /**
     * Drawings this test's own scan finds under a painted neighbour that the export does not name.
     * A node's box can be larger than the ink drawn in it, so a difference here is expected rather
     * than wrong — but it is the difference between two measurements of the same thing, and it is
     * held rather than waved through. Empty today.
     */
    private static final Map<String, List<String>> SEEN_BY_THE_SCAN_ALONE = Map.of();

    /**
     * Painted panels the layout broke across pages. A panel's box is the one its first page gave
     * it, so no overlap can be judged against it, and these four are the corpus's blind spot:
     * named, not counted, so that a change which starts breaking a different panel across pages
     * is seen here instead of quietly shrinking what is judged.
     */
    private static final List<String> PANELS_ACROSS_PAGES = List.of(
            "invoice-obsidian_long: ObsidianInvoice[0]/LineItemsCard[3]",
            "synthetic-break_in_panel: ContainerNode[0]/SectionNode[1]",
            "synthetic-break_in_panel: ContainerNode[0]/SectionNode[1]/SectionNode[4]",
            "synthetic-card_over_pages: ContainerNode[0]/SectionNode[1]");

    /** What the corpus paints today, so a reader that stops seeing drawings fails here. */
    private static final int AT_LEAST_THIS_MANY_DRAWINGS = 450;

    private static final String COVERED = "a panel painted beside it";

    /** The two notes a drawing gets, by which this test knows a note is a drawing's at all. */
    private static final String DRAWN_IN_ITS_BLOCK = "drawn as a shape where the layout puts it, ";
    private static final String DRAWN_IN_ITS_CELL = "drawn as a shape anchored in the table cell it fills";

    /** What a drawing's note says when the export put it in front of the text. */
    private static final String IN_FRONT = "in front of the text";

    @Test
    void everyDrawingUnderANeighboursFillIsLedgered() throws Exception {
        Map<String, List<String>> found = new TreeMap<>();
        Map<String, List<String>> scanOnly = new TreeMap<>();
        List<String> missedByTheScan = new ArrayList<>();
        List<String> unclassified = new ArrayList<>();
        List<String> unjudged = new ArrayList<>();
        List<String> acrossPages = new ArrayList<>();
        List<String> stems = new ArrayList<>();
        int drawings = 0;
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
            scan.panelsAcrossPages().forEach(panel -> acrossPages.add(document.stem() + ": " + panel));
            scan.unjudged().forEach(path -> unjudged.add(document.stem() + ": " + path));
            scan.unclassified().forEach(note -> unclassified.add(document.stem() + ": " + note));

            List<String> named = report.get().notes().stream()
                    .filter(note -> note.detail() != null && note.detail().contains(COVERED))
                    .map(DocxExportReport.Note::path).sorted().toList();
            List<String> seen = scan.covered().stream().sorted().toList();
            named.stream().filter(path -> !seen.contains(path))
                    .forEach(path -> missedByTheScan.add(document.stem() + ": " + path));
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
                                    + "an unknown one would be read as a drawing behind the text")
                .isEmpty();
        assertThat(unjudged).as("every drawing of the corpus given a box to judge").isEmpty();
        assertThat(acrossPages.stream().sorted().toList())
                .as("painted panels broken across pages, which no overlap is judged against: when "
                    + "this list grows, what is judged has shrunk")
                .isEqualTo(PANELS_ACROSS_PAGES);
        assertThat(missedByTheScan)
                .as("the export names a drawing under a neighbour's fill that this test's own "
                    + "geometry does not find: the ink it judged lies in the box this test judged, "
                    + "so this cannot happen while both read the same layout")
                .isEmpty();
        assertThat(scanOnly).as("drawings this test finds under a neighbour's fill that the export "
                                + "does not name")
                .isEqualTo(new TreeMap<>(SEEN_BY_THE_SCAN_ALONE));
        assertThat(UNDER_A_NEIGHBOURS_FILL.keySet()).as("the ledger names documents of the corpus")
                .isSubsetOf(stems);
        assertThat(found).as("every drawing under a neighbour's fill is the one the ledger names")
                .isEqualTo(new TreeMap<>(UNDER_A_NEIGHBOURS_FILL));
    }

    @Test
    void aDrawingOverAPaintedNeighbourIsAlwaysNamed() throws Exception {
        // The positive signal the corpus pass is held to: were the export to stop seeing this
        // case, the corpus would come back clean and the ledger would be the only thing failing.
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

        assertThat(report.get().notes()).as("the pin under the band's fill, named")
                .anyMatch(note -> note.detail() != null && note.detail().contains(COVERED)
                                  && note.path() != null && note.path().contains("Pin"));
    }

    /** What one document's scan found. */
    private record Scan(int drawings, List<String> panelsAcrossPages, List<String> covered,
                        List<String> unjudged, List<String> unclassified) {
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

        List<String> behind = new ArrayList<>();
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
                // of the flow. Named rather than taken for one.
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
        return new Scan(asShapes.size(), acrossPages, covered, unjudged, unclassified);
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
