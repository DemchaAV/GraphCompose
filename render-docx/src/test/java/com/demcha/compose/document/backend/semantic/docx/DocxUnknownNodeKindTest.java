package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.layout.BoxConstraints;
import com.demcha.compose.document.layout.CompositeLayoutSpec;
import com.demcha.compose.document.layout.FragmentContext;
import com.demcha.compose.document.layout.FragmentPlacement;
import com.demcha.compose.document.layout.LayoutFragment;
import com.demcha.compose.document.layout.MeasureResult;
import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.layout.NodeDefinitionSupport;
import com.demcha.compose.document.layout.PaginationPolicy;
import com.demcha.compose.document.layout.PrepareContext;
import com.demcha.compose.document.layout.PreparedNode;
import com.demcha.compose.document.layout.payloads.EllipseFragmentPayload;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentFlowWidth;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A node kind of the caller's own, laid out by its own {@link NodeDefinition}: what the Word export
 * makes of the nodes it holds.
 *
 * <p>Where the definition lays its children out as a composite, the layout places them and the page
 * prints them, so they are written in the node's place and the report says so. Where it places none —
 * it paints what it holds itself — or no layout stands behind the export, the node is dropped and
 * named, as before.</p>
 */
class DocxUnknownNodeKindTest {

    private static final double SPACING = 9;

    @Test
    void theParagraphsAKindOfTheCallersOwnHoldsAreWrittenInItsPlace() throws Exception {
        Export export = exportOf(session -> {
            session.registerNodeDefinition(new CalloutDefinition(CompositeLayoutSpec.Axis.VERTICAL, false));
            session.add(paragraph("Before the callout"));
            session.add(callout());
            session.add(paragraph("After the callout"));
        });

        assertThat(export.text()).containsSubsequence(
                "Before the callout", "Callout line one", "Callout line two", "After the callout");
        assertThat(export.report().count(DocxExportReport.Severity.DROPPED)).isZero();
        assertThat(export.report().bySubject().get("Callout")).singleElement().satisfies(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
            assertThat(note.path()).isEqualTo("Callout[1]");
            assertThat(note.detail()).isEqualTo(HOLDS_TWO);
        });
    }

    private static final String HOLDS_TWO = "a node kind the DOCX export does not know: the 2 nodes it holds are "
                                            + "written in its place, and nothing of its own but the shapes it paints";

    @Test
    void theyAreWrittenInASectionAndInARowCellToo() throws Exception {
        Export export = exportOf(session -> {
            session.registerNodeDefinition(new CalloutDefinition(CompositeLayoutSpec.Axis.VERTICAL, false));
            session.pageFlow(page -> page
                    .addSection("Holder", section -> section.addParagraph("Section heading").add(callout())
                            .addParagraph("Section close"))
                    .addRow(row -> row.weights(1, 1)
                            .addSection("Left", cell -> cell.addParagraph("Left cell"))
                            .addSection("Right", cell -> cell.addParagraph("Cell heading")
                                    .add(callout("In a cell one", "In a cell two")).addParagraph("Cell close"))));
        });

        assertThat(export.text()).containsSubsequence("Section heading", "Callout line one", "Callout line two",
                "Section close", "Left cell", "Cell heading", "In a cell one", "In a cell two", "Cell close");
        assertThat(export.document().getTables()).as("the row is a table, its cell holding the callout's text")
                .singleElement().satisfies(table -> assertThat(table.getRow(0).getCell(1).getText())
                        .containsSubsequence("Cell heading", "In a cell one", "In a cell two", "Cell close"));
        assertThat(export.report().count(DocxExportReport.Severity.DROPPED)).isZero();
        assertThat(export.report().bySubject().get("Callout")).hasSize(2).allSatisfy(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
            assertThat(note.detail()).isEqualTo(HOLDS_TWO);
        });
    }

    @Test
    void aFirstChildThePageBreaksLeavesTheSpacingAndTheOrderAsTheyAre() throws Exception {
        // The first child runs onto the next page; the box the layout gives it is its first piece's.
        String longText = "Callout line one " + "and a paragraph long enough to cross the page. ".repeat(160);
        Export export = exportOf(session -> {
            session.registerNodeDefinition(new CalloutDefinition(CompositeLayoutSpec.Axis.VERTICAL, false));
            session.add(new Callout("Callout", List.of(spaced(longText), spaced("Callout line two"),
                    spaced("Callout line three"))));
            List<com.demcha.compose.document.layout.PlacedNode> held = session.layoutGraph().nodes().stream()
                    .filter(node -> "Callout[0]".equals(node.parentPath())).toList();
            assertThat(held).as("the premise: three children, the first crossing a page and the next two "
                                + "standing whole on the page it ends on").hasSize(3);
            int ends = held.get(0).endPage();
            assertThat(held.get(0).startPage()).isLessThan(ends);
            assertThat(held.subList(1, 3)).allSatisfy(node -> {
                assertThat(node.startPage()).isEqualTo(ends);
                assertThat(node.endPage()).isEqualTo(ends);
            });
        });
        Export section = exportOf(session -> session.pageFlow(page -> page.addSection("Callout", holder -> holder
                .spacing(SPACING).add(spaced(longText)).add(spaced("Callout line two"))
                .add(spaced("Callout line three")))));

        assertThat(export.report().bySubject().get("Callout")).singleElement()
                .extracting(DocxExportReport.Note::detail).asString()
                .as("children one under another, across a page or not")
                .doesNotContain("side by side");
        assertThat(between(export.document(), "Callout line two", "Callout line three"))
                .isEqualTo(between(section.document(), "Callout line two", "Callout line three"))
                .isGreaterThan((int) Math.round((SPACING + MARGIN_BELOW + MARGIN_ABOVE) * 20) - 20);
    }

    @Test
    void aKindWithOneChildIsNamedInTheSingular() throws Exception {
        Export export = exportOf(session -> {
            session.registerNodeDefinition(new CalloutDefinition(CompositeLayoutSpec.Axis.VERTICAL, false));
            session.add(new Callout("Callout", List.of(paragraph("Callout line one"))));
        });

        assertThat(export.text()).contains("Callout line one");
        assertThat(export.report().bySubject().get("Callout")).singleElement()
                .extracting(DocxExportReport.Note::detail).asString()
                .startsWith("a node kind the DOCX export does not know: the 1 node it holds is written in its place");
    }

    @Test
    void theSpaceBetweenThemIsTheSpaceTheLayoutSets() throws Exception {
        Export export = exportOf(session -> {
            session.registerNodeDefinition(new CalloutDefinition(CompositeLayoutSpec.Axis.VERTICAL, false));
            session.add(new Callout("Callout", List.of(spaced("Callout line one"), spaced("Callout line two"))));
        });

        // A section of the same spacing holding the same paragraphs is written by the same writer.
        Export section = exportOf(session -> session.pageFlow(page -> page.addSection("Callout", holder -> holder
                .spacing(SPACING).add(spaced("Callout line one")).add(spaced("Callout line two")))));

        assertThat(between(export.document()))
                .as("the definition's %spt between its children, written as a section's, their margins "
                    + "written as theirs", SPACING)
                .isEqualTo(between(section.document()))
                .isGreaterThan((int) Math.round((SPACING + MARGIN_BELOW + MARGIN_ABOVE) * 20) - 20);
    }

    private static final double MARGIN_ABOVE = 5;
    private static final double MARGIN_BELOW = 3;

    /** A paragraph with margins of its own above and below, which the space between two of them counts. */
    private static DocumentNode spaced(String text) {
        return new ParagraphBuilder().text(text)
                .margin(new DocumentInsets(MARGIN_ABOVE, 0, MARGIN_BELOW, 0)).build();
    }

    /** The twips Word sets between the callout's two lines, after the first and before the second. */
    private static int between(XWPFDocument document) {
        return between(document, "Callout line one", "Callout line two");
    }

    /** The twips Word sets between two paragraphs, after the first and before the second. */
    private static int between(XWPFDocument document, String above, String below) {
        XWPFParagraph first = paragraphHolding(document, above);
        XWPFParagraph second = paragraphHolding(document, below);
        return Math.max(0, first.getSpacingAfter()) + Math.max(0, second.getSpacingBefore());
    }

    @Test
    void childrenThePageSetsSideBySideAreNamedAsWrittenOneUnderAnother() throws Exception {
        for (CompositeLayoutSpec.Axis axis : List.of(CompositeLayoutSpec.Axis.HORIZONTAL,
                CompositeLayoutSpec.Axis.STACK)) {
            Export export = exportOf(session -> {
                session.registerNodeDefinition(new CalloutDefinition(axis, false));
                session.add(callout());
            });

            assertThat(export.text()).as(axis.name()).containsSubsequence("Callout line one", "Callout line two");
            assertThat(export.report().bySubject().get("Callout")).as(axis.name()).singleElement()
                    .extracting(DocxExportReport.Note::detail).asString()
                    .endsWith("; the page sets them side by side or over one another, and they are written one "
                              + "under another");
        }
    }

    @Test
    void childrenThePageSetsSideBySideAreKeptOnOnePageAsTheLayoutMovesThemWhole() throws Exception {
        for (CompositeLayoutSpec.Axis axis : List.of(CompositeLayoutSpec.Axis.HORIZONTAL,
                CompositeLayoutSpec.Axis.STACK, CompositeLayoutSpec.Axis.VERTICAL)) {
            Export export = exportOf(session -> {
                session.registerNodeDefinition(new CalloutDefinition(axis, false));
                session.add(callout());
            });

            XWPFParagraph first = paragraphHolding(export.document(), "Callout line one");
            boolean kept = first.getCTP().getPPr() != null && first.getCTP().getPPr().isSetKeepNext();
            assertThat(kept).as("%s: the first line kept with the next", axis)
                    .isEqualTo(axis != CompositeLayoutSpec.Axis.VERTICAL);
        }
    }

    @Test
    void aKindThatPaintsAShapeAndHoldsParagraphsIsDrawnAndItsParagraphsWritten() throws Exception {
        Export export = exportOf(session -> {
            session.registerNodeDefinition(new CalloutDefinition(CompositeLayoutSpec.Axis.VERTICAL, true));
            session.add(callout());
        });

        assertThat(export.text()).containsSubsequence("Callout line one", "Callout line two");
        assertThat(export.report().bySubject().get("Callout")).singleElement().satisfies(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
            assertThat(note.detail()).startsWith("drawn as a shape")
                    .contains("; a node kind the DOCX export does not know: the 2 nodes it holds are written in "
                              + "its place")
                    .doesNotContain("only the shapes it paints itself are written");
        });
    }

    @Test
    void aKindWhoseDefinitionLaysOutNoneOfItsChildrenIsStillDropped() throws Exception {
        // It paints what it holds itself: the layout places none of its children, so nothing says
        // they are what the page shows.
        Export export = exportOf(session -> {
            session.registerNodeDefinition(new StampDefinition());
            session.add(new Stamp("Stamp", List.of(paragraph("Approved by finance"))));
        });

        assertThat(export.text()).doesNotContain("Approved by finance");
        assertThat(export.report().bySubject().get("Stamp")).singleElement().satisfies(note -> {
            assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.DROPPED);
            assertThat(note.detail()).contains("nor the 1 node it holds");
        });
    }

    @Test
    void aKindWithNoLayoutBehindTheExportIsStillDropped() throws Exception {
        // No definition registered: the document cannot be laid out, and the export goes on without it.
        Export export = exportOf(session -> {
            session.add(paragraph("Before the callout"));
            session.add(callout());
        });

        assertThat(export.text()).contains("Before the callout").doesNotContain("Callout line one");
        assertThat(export.report().bySubject().get("Callout")).singleElement()
                .extracting(DocxExportReport.Note::severity).isEqualTo(DocxExportReport.Severity.DROPPED);
        assertThat(export.report().bySubject().get("measured geometry")).singleElement()
                .extracting(DocxExportReport.Note::detail).asString()
                .contains("No node definition registered for " + Callout.class.getName());
    }

    @Test
    void aKindComposedInATableCellIsStillDropped() throws Exception {
        // The page lays it out in the cell, but the layout gives what a cell holds no place of its own.
        Export export = exportOf(session -> {
            session.registerNodeDefinition(new CalloutDefinition(CompositeLayoutSpec.Axis.VERTICAL, false));
            session.pageFlow(page -> page.addTable(table -> table.autoColumns(1)
                    .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(callout()))));
        });

        assertThat(export.text()).doesNotContain("Callout line one");
        assertThat(export.report().bySubject().get("Callout")).singleElement()
                .extracting(DocxExportReport.Note::severity).isEqualTo(DocxExportReport.Severity.DROPPED);
    }

    @Test
    void aKindWhoseDefinitionLaysOutAChildItDoesNotListIsStillDropped() throws Exception {
        // The export writes the children the node lists; here the page prints one more.
        Export export = exportOf(session -> {
            session.registerNodeDefinition(
                    new CalloutDefinition(CompositeLayoutSpec.Axis.VERTICAL, false, LaidOut.ONE_MORE));
            session.add(callout());
        });

        assertThat(export.text()).doesNotContain("Callout line one").doesNotContain("Laid out by the definition");
        assertThat(export.report().bySubject().get("Callout")).singleElement()
                .extracting(DocxExportReport.Note::severity).isEqualTo(DocxExportReport.Severity.DROPPED);
    }

    @Test
    void aKindWhoseDefinitionLaysOutNodesAtOtherPlacesThanItListsIsStillDropped() throws Exception {
        // As many as it lists, named otherwise, so at other places of the layout: the page prints others.
        Export export = exportOf(session -> {
            session.registerNodeDefinition(
                    new CalloutDefinition(CompositeLayoutSpec.Axis.VERTICAL, false, LaidOut.OTHERS));
            session.add(callout());
        });

        assertThat(export.text()).doesNotContain("Callout line one").doesNotContain("Laid out by the definition");
        assertThat(export.report().bySubject().get("Callout")).singleElement()
                .extracting(DocxExportReport.Note::severity).isEqualTo(DocxExportReport.Severity.DROPPED);
    }

    @Test
    void otherNodesOfTheSameKindAtTheListedPlacesHaveTheListedOnesWritten() throws Exception {
        // The layout knows a node by its place alone — its name or kind and its index — so copies laid
        // out at the listed places stand for the listed nodes, whose text is what is written.
        Export export = exportOf(session -> {
            session.registerNodeDefinition(
                    new CalloutDefinition(CompositeLayoutSpec.Axis.VERTICAL, false, LaidOut.COPIES));
            session.add(callout());
        });

        assertThat(export.text()).containsSubsequence("Callout line one", "Callout line two")
                .doesNotContain("Laid out by the definition");
        assertThat(export.report().bySubject().get("Callout")).singleElement()
                .extracting(DocxExportReport.Note::detail).isEqualTo(HOLDS_TWO);
    }

    /** Which children a callout's definition lays out. */
    private enum LaidOut {
        /** The ones the node lists. */
        LISTED,
        /** One of its own before the ones the node lists. */
        ONE_MORE,
        /** As many as the node lists, but named otherwise, so at other places of the layout. */
        OTHERS,
        /** As many as the node lists, unnamed paragraphs as they are, so at their places. */
        COPIES
    }

    private static Callout callout() {
        return callout("Callout line one", "Callout line two");
    }

    private static Callout callout(String one, String two) {
        return new Callout("Callout", List.of(paragraph(one), paragraph(two)));
    }

    private static DocumentNode paragraph(String text) {
        return new ParagraphBuilder().text(text).build();
    }

    private static XWPFParagraph paragraphHolding(XWPFDocument document, String text) {
        return document.getParagraphs().stream().filter(paragraph -> paragraph.getText().contains(text))
                .findFirst().orElseThrow();
    }

    private record Export(XWPFDocument document, String text, DocxExportReport report) {
    }

    private static Export exportOf(Consumer<DocumentSession> compose) throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 600)
                .margin(DocumentInsets.of(20))
                .create()) {
            compose.accept(session);
            docx = session.export(DocxSemanticBackend.builder().reportSink(captured::set).build());
        }
        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));
        try (XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return new Export(document, extractor.getText(), captured.get());
        }
    }

    /** A node kind of the caller's own that holds paragraphs, laid out as a composite. */
    private record Callout(String name, List<DocumentNode> children) implements DocumentNode {
    }

    private static final class CalloutDefinition implements NodeDefinition<Callout> {
        private final CompositeLayoutSpec.Axis axis;
        private final boolean paints;
        private final LaidOut laidOut;

        CalloutDefinition(CompositeLayoutSpec.Axis axis, boolean paints) {
            this(axis, paints, LaidOut.LISTED);
        }

        CalloutDefinition(CompositeLayoutSpec.Axis axis, boolean paints, LaidOut laidOut) {
            this.axis = axis;
            this.paints = paints;
            this.laidOut = laidOut;
        }

        @Override
        public Class<Callout> nodeType() {
            return Callout.class;
        }

        @Override
        public List<DocumentNode> children(Callout node) {
            return switch (laidOut) {
                case LISTED -> node.children();
                case ONE_MORE -> {
                    List<DocumentNode> children = new java.util.ArrayList<>();
                    children.add(paragraph("Laid out by the definition"));
                    children.addAll(node.children());
                    yield children;
                }
                case OTHERS -> node.children().stream()
                        .map(child -> (DocumentNode) new ParagraphBuilder().name("Other")
                                .text("Laid out by the definition").build())
                        .toList();
                case COPIES -> node.children().stream()
                        .map(child -> paragraph("Laid out by the definition"))
                        .toList();
            };
        }

        @Override
        public PreparedNode<Callout> prepare(Callout node, PrepareContext ctx, BoxConstraints constraints) {
            MeasureResult measured = axis == CompositeLayoutSpec.Axis.VERTICAL
                    ? NodeDefinitionSupport.measureComposite(children(node), SPACING,
                            NodeDefinitionSupport.toPadding(node.padding()), ctx, constraints,
                            DocumentFlowWidth.natural())
                    : new MeasureResult(constraints.availableWidth(), 30);
            if (axis == CompositeLayoutSpec.Axis.STACK) {
                // Layers over one another, each at the stack's top left.
                int layers = children(node).size();
                return PreparedNode.composite(node, measured,
                        new com.demcha.compose.document.layout.payloads.PreparedStackLayout(
                                java.util.Collections.nCopies(layers,
                                        com.demcha.compose.document.node.LayerAlign.TOP_LEFT),
                                java.util.Collections.nCopies(layers, 0.0),
                                java.util.Collections.nCopies(layers, 0.0)),
                        new CompositeLayoutSpec(0, axis));
            }
            return PreparedNode.composite(node, measured, new CompositeLayoutSpec(SPACING, axis));
        }

        @Override
        public PaginationPolicy paginationPolicy(Callout node) {
            // The compiler pages a composite child by child, whatever this says.
            return PaginationPolicy.SPLITTABLE;
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<Callout> prepared, FragmentContext ctx,
                                                  FragmentPlacement placement) {
            return paints
                    ? List.of(new LayoutFragment(placement.path(), 0, 0, 0, placement.width(), placement.height(),
                            new EllipseFragmentPayload(java.awt.Color.RED, null, null, null)))
                    : List.of();
        }
    }

    /** A node kind of the caller's own that lists a paragraph and paints what it holds itself. */
    private record Stamp(String name, List<DocumentNode> children) implements DocumentNode {
    }

    private static final class StampDefinition implements NodeDefinition<Stamp> {
        @Override
        public Class<Stamp> nodeType() {
            return Stamp.class;
        }

        @Override
        public PreparedNode<Stamp> prepare(Stamp node, PrepareContext ctx, BoxConstraints constraints) {
            return PreparedNode.leaf(node, new MeasureResult(120, 30));
        }

        @Override
        public List<DocumentNode> children(Stamp node) {
            return List.of();
        }

        @Override
        public PaginationPolicy paginationPolicy(Stamp node) {
            return PaginationPolicy.ATOMIC;
        }

        @Override
        public List<LayoutFragment> emitFragments(PreparedNode<Stamp> prepared, FragmentContext ctx,
                                                  FragmentPlacement placement) {
            return List.of();
        }
    }
}
