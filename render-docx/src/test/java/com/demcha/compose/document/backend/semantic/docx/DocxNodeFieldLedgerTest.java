package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.node.AlignNode;
import com.demcha.compose.document.node.BarcodeNode;
import com.demcha.compose.document.node.CanvasLayerNode;
import com.demcha.compose.document.node.ChartNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.node.PageFieldNode;
import com.demcha.compose.document.node.PageReferenceNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.PathNode;
import com.demcha.compose.document.node.PolygonNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.output.DocumentOutputOptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the DOCX export does with every field of every node, and with every output option:
 * writes it, reports it when the author set it, has nothing for Word to carry, or leaves a
 * known gap that names what is lost.
 *
 * <p>The export's report promises a caller that what the file does not carry is said, not
 * approximated in silence. That promise is only as good as the fields someone thought about:
 * a field added to a node later is written by the PDF backend at once and, until the DOCX export
 * decides, lost from the Word file without a word. This ledger fails on any node kind or field
 * it has no entry for, so the decision is made when the field is added, not when a reader finds
 * the loss.</p>
 *
 * <p>A node's entries are about the body. A page zone is written as one line of its
 * paragraphs' runs, page fields and tabs, which loses more of a paragraph and a row than the
 * body does; that is recorded once, as the {@code zones} output option's gap.</p>
 *
 * <p>The entries are claims about the export, not proofs of it: a {@code WRITTEN} field is
 * proved by the export's own tests, a {@code REPORTED} one by a report test, and a {@code GAP}
 * is a loss the report does not yet name, each with what is lost. Closing a gap moves its entry,
 * in the same change that makes the export write or report it.</p>
 */
class DocxNodeFieldLedgerTest {

    private enum Fate {
        /** Reaches the Word file, directly or through the geometry the layout resolved. */
        WRITTEN,
        /** Not in the Word file, and named in the export's report when the author set it. */
        REPORTED,
        /**
         * Nothing for a Word file to carry: a name the report addresses the node by, an
         * identity the layout resolves a wrapper's place by, or a field the page itself does
         * not apply.
         */
        INERT,
        /** Lost, in some or all cases, without a report note yet; the entry says what. */
        GAP
    }

    /** A field's fate, and for a gap what is lost, for a report what names it. */
    private record Entry(Fate fate, String note) {
    }

    private static final Map<Class<?>, Map<String, Entry>> NODES = new LinkedHashMap<>();
    private static final Map<String, Entry> OUTPUT_OPTIONS = fields(
            "metadata:WRITTEN",
            "watermark:REPORTED",
            "protection:REPORTED",
            "viewerPreferences:REPORTED",
            "headersAndFooters:WRITTEN",
            // The node entries below are the body's. A zone is written as one line of its
            // paragraphs' runs, page fields and tabs, which is a gap of its own.
            "zones:GAP:in a page zone, a paragraph's alignment, spacing, direction, prefix, fitted size and "
            + "outline entry and a row's columns, gap and padding; whatever else a zone holds is reported");

    static {
        node(AlignNode.class, "name:INERT", "child:WRITTEN", "align:WRITTEN", "margin:WRITTEN");
        node(BarcodeNode.class, "name:INERT", "barcodeOptions:WRITTEN", "width:WRITTEN", "height:WRITTEN",
                "linkTarget:REPORTED", "bookmarkOptions:REPORTED",
                "padding:REPORTED:its left side; its right moves nothing in a paragraph set from the left",
                "margin:REPORTED:its left side; its right moves nothing in a paragraph set from the left",
                "transform:REPORTED", "anchor:WRITTEN");
        node(CanvasLayerNode.class, "name:INERT",
                "width:REPORTED:the width its text wraps at",
                "height:GAP:the room it holds as its row's tallest cell, or ending a band or a layer "
                + "stack's column; in a flow something follows it in, it is reported",
                "placements:REPORTED:where what it writes stands; its drawings stand where it places them",
                "clipPolicy:INERT:the page clips no canvas", "padding:WRITTEN", "margin:WRITTEN");
        node(ChartNode.class, "name:INERT", "spec:REPORTED", "style:REPORTED:in the chart's note",
                "margin:REPORTED:in the chart's note; above and below; below a block a band or a column measures from, written",
                "padding:REPORTED:in the chart's note; above and below; below a block a band or a column measures from, written");
        node(ContainerNode.class, "name:INERT", "children:WRITTEN", "spacing:WRITTEN", "padding:WRITTEN",
                "margin:WRITTEN", "fillColor:WRITTEN", "stroke:WRITTEN", "cornerRadius:REPORTED", "borders:WRITTEN",
                "anchor:REPORTED:as a layer stack's column, which has no bookmark; in the flow it is bookmarked",
                "bookmarkOptions:REPORTED", "flowWidth:REPORTED:of an unpainted one, a panel in a table cell and a layer stack's column");
        node(EllipseNode.class, "name:INERT", "width:WRITTEN", "height:WRITTEN", "fillColor:WRITTEN",
                "stroke:WRITTEN", "linkTarget:REPORTED", "bookmarkOptions:REPORTED",
                "padding:WRITTEN", "margin:WRITTEN", "transform:REPORTED",
                "anchor:REPORTED");
        node(ImageNode.class, "name:INERT", "imageData:WRITTEN", "width:WRITTEN", "height:WRITTEN",
                "scale:WRITTEN", "fitMode:WRITTEN", "linkTarget:REPORTED",
                "bookmarkOptions:REPORTED", "padding:REPORTED:its left side; its right moves nothing in a paragraph set from the left; drawn beside its text or over its badge, all of it",
                "margin:REPORTED:its left side; its right moves nothing in a paragraph set from the left", "transform:REPORTED",
                "anchor:WRITTEN");
        node(LayerStackNode.class, "name:INERT", "layers:WRITTEN", "padding:WRITTEN", "margin:WRITTEN",
                "clipToBounds:REPORTED:where it cuts what its layers paint; composed in a table cell, on its table");
        node(LineNode.class, "name:INERT", "width:WRITTEN", "height:WRITTEN", "startX:WRITTEN", "startY:WRITTEN",
                "endX:WRITTEN", "endY:WRITTEN", "stroke:WRITTEN",
                "linkTarget:REPORTED",
                "bookmarkOptions:REPORTED", "padding:WRITTEN", "margin:WRITTEN", "transform:REPORTED",
                "dashPattern:REPORTED",
                "anchor:REPORTED:drawn; a rule in the flow is bookmarked", "lineCap:REPORTED:a cap other than butt; whether an editor ends a drawn butt line flat is not measured",
                "fillWidth:WRITTEN", "keepWithNext:REPORTED:of a line drawn in the flow the page keeps blocks together in");
        node(ListNode.class, "name:INERT",
                "items:REPORTED:a blank one a hangingIndent list draws as its marker alone; any other is written",
                "nestedItems:WRITTEN", "marker:WRITTEN",
                "textStyle:WRITTEN", "align:REPORTED",
                "lineSpacing:REPORTED:where the layout's items are not its own and one wraps, an item run "
                + "onto the next page among them; composed in a table cell, its wrapping not measured; "
                + "with no layout, in the section's note",
                "itemSpacing:WRITTEN",
                "continuationIndent:REPORTED:where an item wraps, or its lines are not read, in a markerless "
                + "list or a tree of items without hangingIndent, the only lists the page sets it in",
                "normalizeMarkers:WRITTEN", "padding:WRITTEN", "margin:WRITTEN",
                "hangingIndent:REPORTED:where an item stands at a stated column, a space past its marker or "
                + "two spaces a level in",
                "markerGap:REPORTED:where an item stands at a stated column, a space past its marker or "
                + "two spaces a level in");
        node(PageBreakNode.class, "name:INERT", "margin:INERT");
        node(PageFieldNode.class, "name:INERT", "kind:WRITTEN", "textStyle:WRITTEN",
                "align:GAP:its alignment in a page zone", "padding:GAP:its sides in a page zone",
                "margin:GAP:its sides in a page zone");
        node(PageReferenceNode.class, "name:INERT",
                "anchor:REPORTED:where the anchor has no bookmark, its number is written as text",
                "textStyle:WRITTEN", "align:WRITTEN", "placeholderText:WRITTEN",
                "padding:REPORTED:the sides its alignment sets it from",
                "margin:REPORTED:the sides its alignment sets it from");
        node(ParagraphNode.class, "name:INERT", "text:WRITTEN", "inlineRuns:WRITTEN", "textStyle:WRITTEN",
                "align:WRITTEN", "lineSpacing:WRITTEN",
                "bulletOffset:REPORTED:its letters before the first line; over the flow, as a side of an "
                + "overlay's pair or as a badge's text, the room it sets lines in by where that moves one",
                "indentStrategy:WRITTEN", "linkTarget:WRITTEN",
                "bookmarkOptions:REPORTED:a title that is not the text Word lists it by, a pair's whole line for "
                + "a side of one; a level past Word's ninth; the right side's of a pair whose left holds the level",
                "padding:WRITTEN", "margin:WRITTEN",
                "autoSize:REPORTED:the size the page fits the text in the paragraph's style to; with no layout, "
                + "not measured",
                "verticalAlign:WRITTEN", "anchor:WRITTEN", "direction:WRITTEN");
        node(PathNode.class, "name:INERT", "width:WRITTEN", "height:WRITTEN", "segments:WRITTEN",
                "fillColor:WRITTEN", "fillPaint:REPORTED", "stroke:WRITTEN", "strokePaint:REPORTED",
                "padding:WRITTEN", "margin:WRITTEN", "dashPattern:REPORTED", "lineCap:REPORTED", "lineJoin:REPORTED");
        node(PolygonNode.class, "name:INERT", "width:WRITTEN", "height:WRITTEN", "points:WRITTEN",
                "fillColor:WRITTEN", "stroke:WRITTEN", "padding:WRITTEN", "margin:WRITTEN");
        node(RowNode.class, "name:INERT", "children:WRITTEN", "weights:WRITTEN", "gap:WRITTEN", "padding:WRITTEN",
                "margin:WRITTEN", "fillColor:REPORTED", "stroke:REPORTED",
                "cornerRadius:REPORTED:with the paint it rounds; with none it paints nothing",
                "borders:REPORTED", "columns:WRITTEN", "verticalAlign:WRITTEN", "arrangement:WRITTEN");
        // The wrappers the layout adds round a timeline's parts, public records the export writes
        // through to their child.
        node(com.demcha.compose.document.layout.LayoutAnchorNode.class, "name:INERT", "id:INERT", "child:WRITTEN");
        node(com.demcha.compose.document.layout.HorizontalBandsNode.class, "name:INERT", "key:INERT",
                "child:WRITTEN");
        node(com.demcha.compose.document.layout.HorizontalBandContentNode.class, "name:INERT", "key:INERT",
                "slot:WRITTEN", "child:WRITTEN");
        node(SectionNode.class, "name:INERT", "children:WRITTEN", "spacing:WRITTEN", "padding:WRITTEN",
                "margin:WRITTEN", "fillColor:WRITTEN", "stroke:WRITTEN", "cornerRadius:REPORTED", "borders:WRITTEN",
                "keepTogether:WRITTEN",
                "anchor:REPORTED:as a layer stack's column, which has no bookmark; in the flow it is bookmarked",
                "bleed:REPORTED:of a panel the page bleeds, in the flow it pages",
                "bookmarkOptions:REPORTED", "keepWithNext:WRITTEN",
                "flowWidth:REPORTED:of an unpainted one, a panel in a table cell and a layer stack's column");
        node(ShapeContainerNode.class, "name:INERT", "outline:WRITTEN", "layers:WRITTEN",
                "clipPolicy:REPORTED:where it cuts what its layers paint; composed in a table cell, on its table",
                "fillColor:WRITTEN", "stroke:WRITTEN", "padding:WRITTEN", "margin:WRITTEN",
                "transform:REPORTED");
        node(ShapeNode.class, "name:INERT", "width:WRITTEN", "height:WRITTEN", "fillColor:WRITTEN",
                "stroke:WRITTEN", "cornerRadius:REPORTED:unequal corners, drawn at the largest radius",
                "linkTarget:REPORTED", "bookmarkOptions:REPORTED", "padding:WRITTEN",
                "margin:WRITTEN", "transform:REPORTED", "fillPaint:REPORTED",
                "anchor:REPORTED:drawn; a rule in the flow is bookmarked");
        node(SpacerNode.class, "name:INERT", "width:WRITTEN", "height:WRITTEN",
                "padding:REPORTED:above and below; below a block a band or a column measures from, written",
                "margin:REPORTED:above and below; below a block a band or a column measures from, written",
                "grow:WRITTEN");
        node(TableNode.class, "name:INERT", "columns:WRITTEN", "rows:WRITTEN", "defaultCellStyle:WRITTEN",
                "rowStyles:WRITTEN", "columnStyles:WRITTEN", "width:WRITTEN", "linkTarget:REPORTED",
                "bookmarkOptions:REPORTED",
                "padding:WRITTEN",
                "margin:WRITTEN", "repeatedHeaderRowCount:WRITTEN", "anchor:WRITTEN");
    }

    @Test
    void everyNodeKindGraphComposeShipsHasAnEntry() throws Exception {
        // Every concrete one, nested or not, public or not: the export counts all of them as its
        // own (DocxSemanticBackend.isBuiltIn), so none may go undecided.
        assertThat(names(nodeClasses()))
                .as("the node classes of %s: a kind with no entry has fields nobody decided "
                    + "the DOCX export's answer for", PACKAGES)
                .containsExactlyInAnyOrderElementsOf(names(NODES.keySet()));
    }

    @Test
    void everyNodeKindGraphComposeShipsIsARecord() throws Exception {
        // The ledger reads a node's fields as its record components; a node class that is not a
        // record would have fields this test cannot see, and so no decision for any of them.
        assertThat(nodeClasses()).as("node classes that are not records")
                .filteredOn(kind -> !kind.isRecord()).isEmpty();
    }

    @Test
    void everyFieldOfEveryNodeHasAnEntry() {
        Map<String, Set<String>> undecided = new LinkedHashMap<>();
        Map<String, Set<String>> stale = new LinkedHashMap<>();
        NODES.forEach((kind, entries) -> {
            Set<String> fields = components(kind);
            Set<String> missing = new TreeSet<>(fields);
            missing.removeAll(entries.keySet());
            Set<String> extra = new TreeSet<>(entries.keySet());
            extra.removeAll(fields);
            if (!missing.isEmpty()) {
                undecided.put(kind.getSimpleName(), missing);
            }
            if (!extra.isEmpty()) {
                stale.put(kind.getSimpleName(), extra);
            }
        });
        assertThat(undecided).as("fields with no entry: decide whether the DOCX export writes them, "
                                 + "reports them, or leaves a gap, and say which here").isEmpty();
        assertThat(stale).as("entries for fields the node no longer has").isEmpty();
    }

    @Test
    void everyOutputOptionHasAnEntry() {
        assertThat(components(DocumentOutputOptions.class))
                .as("the output options a session hands every export")
                .containsExactlyInAnyOrderElementsOf(OUTPUT_OPTIONS.keySet());
    }

    @Test
    void everyGapSaysWhatIsLost() {
        List<String> unexplained = new ArrayList<>();
        NODES.forEach((kind, entries) -> entries.forEach((field, entry) -> {
            if (entry.fate() == Fate.GAP && entry.note().isBlank()) {
                unexplained.add(kind.getSimpleName() + "." + field);
            }
        }));
        OUTPUT_OPTIONS.forEach((option, entry) -> {
            if (entry.fate() == Fate.GAP && entry.note().isBlank()) {
                unexplained.add("DocumentOutputOptions." + option);
            }
        });
        assertThat(unexplained).as("a gap names what the Word file loses").isEmpty();
    }

    private static void node(Class<? extends DocumentNode> kind, String... specs) {
        NODES.put(kind, fields(specs));
    }

    /** {@code field:FATE}, or {@code field:GAP:what is lost}. */
    private static Map<String, Entry> fields(String... specs) {
        Map<String, Entry> entries = new LinkedHashMap<>();
        for (String spec : specs) {
            String[] parts = spec.split(":", 3);
            Entry entry = new Entry(Fate.valueOf(parts[1]), parts.length > 2 ? parts[2] : "");
            if (entries.put(parts[0], entry) != null) {
                throw new IllegalStateException("two entries for " + parts[0]);
            }
        }
        return entries;
    }

    private static Set<String> components(Class<?> record) {
        return Arrays.stream(record.getRecordComponents()).map(RecordComponent::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> names(java.util.Collection<Class<?>> kinds) {
        return kinds.stream().map(Class::getName).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * The packages whose node kinds the export counts as its own: the public node records, and
     * the wrappers the layout adds (see {@code DocxSemanticBackend.isBuiltIn}).
     */
    private static final List<String> PACKAGES = List.of(DocumentNode.class.getPackageName(),
            com.demcha.compose.document.layout.LayoutAnchorNode.class.getPackageName());

    /**
     * The concrete node classes the core module ships in {@link #PACKAGES} — nested ones, and
     * ones not public, included.
     */
    private static List<Class<?>> nodeClasses() throws IOException, URISyntaxException, ClassNotFoundException {
        Path source = Path.of(DocumentNode.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<String> classFiles = new ArrayList<>();
        for (String pkg : PACKAGES) {
            String folder = pkg.replace('.', '/') + "/";
            if (Files.isDirectory(source)) {
                try (Stream<Path> files = Files.list(source.resolve(folder))) {
                    files.map(file -> folder + file.getFileName()).forEach(classFiles::add);
                }
            } else {
                try (JarFile jar = new JarFile(source.toFile())) {
                    jar.stream().map(java.util.zip.ZipEntry::getName)
                            .filter(name -> name.startsWith(folder) && name.indexOf('/', folder.length()) < 0)
                            .forEach(classFiles::add);
                }
            }
        }
        List<Class<?>> kinds = new ArrayList<>();
        for (String file : classFiles) {
            if (!file.endsWith(".class") || file.endsWith("package-info.class")) {
                continue;
            }
            // Loaded without initialising: a nested class's binary name keeps its '$'.
            Class<?> type = Class.forName(file.substring(0, file.length() - ".class".length()).replace('/', '.'),
                    false, DocumentNode.class.getClassLoader());
            if (DocumentNode.class.isAssignableFrom(type)
                && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
                kinds.add(type);
            }
        }
        return kinds;
    }
}
