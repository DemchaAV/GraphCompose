package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.backend.semantic.SemanticSection;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.NumberFormatSpec;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.document.backend.fixed.pdf.handlers.InlineSvgRasters;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.InlineSvgLayers;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.ParagraphDirection;
import com.demcha.compose.document.layout.NodeDefinitionSupport;
import com.demcha.compose.document.layout.TableGrid;
import com.demcha.compose.document.node.ChartNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.output.DocumentOutputOptions;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.DocumentLinkTarget;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ExternalLinkTarget;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.node.InlineHighlightRun;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.InlineImageRun;
import com.demcha.compose.document.node.InlineShapeRun;
import com.demcha.compose.document.node.InlineSvgRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.InternalLinkTarget;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.node.RowArrangement;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.document.style.InlineBackground;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.font.FontName;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.IRunBody;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import com.demcha.compose.document.node.PageFieldKind;
import com.demcha.compose.document.node.PageFieldNode;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.output.PageContext;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.xmlbeans.impl.xb.xmlschema.SpaceAttribute;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabStop;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabJc;
import org.apache.poi.common.usermodel.PictureType;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRelativeRect;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTString;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyles;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Functional canonical DOCX semantic backend backed by Apache POI.
 *
 * <p>The backend walks the semantic document graph and writes a DOCX file with:
 * paragraphs (single-style or inline-run), per-row tables, embedded images,
 * spacer paragraphs for vertical gaps, page-break markers, and section
 * containers. It deliberately ignores fixed-layout concerns (no per-page
 * pagination, no PDF chrome) since semantic exports target editing tools.</p>
 *
 * <p><b>Dependencies:</b> this backend ships in
 * {@code io.github.demchaav:graph-compose-render-docx}, which brings
 * {@code org.apache.poi:poi-ooxml} transitively, and
 * {@code graph-compose-render-pdf} — opening a session resolves the font-metrics
 * provider only that module publishes, and a barcode is drawn with its matrix encoder,
 * so it is carried at compile scope, as the PPTX module carries it. Adding that one artifact
 * to {@code graph-compose-core} is all a DOCX consumer needs.</p>
 *
 * <p><b>Threads:</b> an instance holds the state of the export it is running — the spacing
 * still owed, the bookmark names handed out, the list definitions written — and starts every
 * export from nothing, including one that follows an export that threw. So one instance can
 * be used for any number of exports one after another, but not by two threads at once.
 * Create one per thread; the session's {@code buildDocx} / {@code writeDocx} /
 * {@code toDocxBytes} already do, taking a new backend for every export.</p>
 *
 * @author Artem Demchyshyn
 */
public final class DocxSemanticBackend implements SemanticBackend<byte[]> {

    /** Word''s built-in default paragraph style; the name is fixed by the format. */
    private static final String NORMAL_STYLE_ID = "Normal";
    /** Word measures tab stops in twentieths of a point. */
    private static final double TWIPS_PER_POINT = 20.0;
    /** {@code w:sz} and {@code w:szCs} count half-points. */
    private static final double HALF_POINTS_PER_POINT = 2.0;
    private static final double POINT_TO_TWIP = 20.0;
    private static final Logger LOG = LoggerFactory.getLogger(DocxSemanticBackend.class);
    // The page's content width, so an image is held to the same bound layout holds it to.
    // Set per export; Double.MAX_VALUE means "no canvas, so nothing to clamp against".
    private double contentWidth = Double.MAX_VALUE;
    // One capability warning per export pass keeps the log readable when a
    // template uses many shape containers. Reset on every export() call so
    // each session sees the warning at least once.
    private final AtomicBoolean shapeContainerWarned = new AtomicBoolean(false);
    private final AtomicBoolean chartWarned = new AtomicBoolean(false);
    // Geometry-only node kinds already warned about this export pass.
    private final java.util.Set<String> warnedNodeKinds =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicBoolean containerRadiusWarned = new AtomicBoolean(false);
    // The fill of the panel being written into, or null: a table cell with no fill of its own
    // is drawn white by the engine, and inside a filled panel has to say so rather than let the
    // panel's colour through.
    private DocumentColor surfaceBehind;
    // How many overlays — layer stacks, canvases, shape containers — the node being written sits in.
    private int overlayDepth;
    // How far the containers being written hold their content in from each side, in points:
    // every enclosing margin and padding, counted from the page margin or the cell's edge.
    private double insetLeft;
    private double insetRight;
    // Identifiers for the shapes this export draws itself, kept clear of the ones POI numbers
    // its pictures with: a drawing's id has to be unique in the document.
    private long nextDrawingId;
    // The text style the document is mostly written in, promoted to Word's Normal style.
    // Null until an export computes it, and when the graph carries no text at all.
    private DocumentTextStyle documentDefaultStyle;
    // One Word list definition per ListNode that can be one, keyed by identity because
    // two lists reading the same are still two lists.
    private final java.util.Map<com.demcha.compose.document.node.ListNode, BigInteger>
            listNumbering = new java.util.IdentityHashMap<>();
    // What the engine already measured for the nodes being written: line heights and
    // resolved column widths. Empty when the export was handed no layout.
    private DocxLayoutMetrics layout = DocxLayoutMetrics.EMPTY;
    // The vertical edge of a container whose children have not been written yet, waiting
    // for the first paragraph inside it — a container is not a Word object, so the space it
    // holds above itself has to be carried by something that is.
    private double carriedSpacingBefore;

    /** Space the last body paragraph holds below itself, not yet written — see {@link #owePendingSpacingAfter}. */
    private double pendingSpacingAfter;

    /** The page's height in points, or {@code NaN} when the export has no canvas. */
    private double canvasHeight = Double.NaN;

    /** Whether this export writes more than one section — see {@link #exportSections}. */
    private boolean sectioned;

    /** The gap the list being written puts between its items. */
    private double pendingItemSpacing;

    /** Whether the list being written has an item above the one about to be written. */
    private boolean anItemWasWritten;
    // The last paragraph written into the body, so a container can hand it the space it
    // holds below itself once its children are done.
    private XWPFParagraph lastBodyParagraph;
    // The empty paragraph closing the last table written into the cell being filled, while
    // nothing has been written after it; see newTable.
    private XWPFParagraph tableCloser;
    // The cell being filled, when one is. A composed cell is written by the ordinary
    // writers pointed at it rather than by a second set that knows about cells: the first
    // arrangement only ever learned about paragraphs, so a cell built from an image or a
    // list came out empty.
    private XWPFTableCell currentCell;
    // How wide content may be inside that cell, so a table nested in it gets a width
    // instead of being squeezed by Word to a character a line.
    private double currentCellWidth = Double.NaN;
    // Every family this export can name, by the logical name a style asks for. The
    // session's own registrations win over the bundled ones, the way they do everywhere.
    private java.util.Map<FontName, FontFamilyDefinition> wordFamilies = java.util.Map.of();
    // What this export could not carry as authored. Collected whether or not anyone asked
    // for it: building it costs a list, and deciding later that nobody wanted it is not
    // something the writers can do halfway through.
    private DocxExportReport.Builder report = new DocxExportReport.Builder();
    // The Word names this export gave the document's anchors, so a link and the bookmark
    // it points at agree.
    private DocxBookmarkNames bookmarkNames = new DocxBookmarkNames();
    // The outline levels this document asks for, so the styles part defines those and no
    // others. Filled before the styles part is written, which comes before the body.
    private java.util.Set<Integer> headingLevels = java.util.Set.of();
    // Anchors this export writes a bookmark for, so a page reference knows it has a target.
    private java.util.Set<String> bookmarkedAnchors = java.util.Set.of();
    // Where the finished report goes, when the caller configured somewhere for it to go.
    private final java.util.function.Consumer<DocxExportReport> reportSink;
    // The instant every clock in the package is pinned to, or null for live timestamps.
    private final Instant deterministicTimestamp;

    /** The instant deterministic output pins to by default — the PDF and PPTX backends' own. */
    private static final Instant DEFAULT_DETERMINISTIC_INSTANT = Instant.parse("2000-01-01T00:00:00Z");

    /**
     * A container's paint: what makes it a panel, written as a one-cell table.
     *
     * @param fill    background, written as the cell's {@code w:shd}
     * @param borders per-side strokes, written as the cell's {@code w:tcBorders}
     */
    private record ContainerPaint(DocumentColor fill, DocumentBorders borders) {

        /** @return true when there is nothing to paint, so the container is written as its content */
        boolean isEmpty() {
            return fill == null && borders == null;
        }
    }

    /**
     * Creates a DOCX semantic backend.
     */
    public DocxSemanticBackend() {
        this((java.util.function.Consumer<DocxExportReport>) null);
    }

    /**
     * Creates a backend that hands its report to {@code reportSink} when an export ends.
     *
     * <p>A Word document cannot hold everything a page can draw, and this export says so
     * rather than approximating in silence — but it said so to the log, which a service
     * generating documents for other people has no way to read. Configure a sink and the
     * same information arrives as a {@link DocxExportReport}: what was dropped, what was
     * approximated, and the path of the node each came from.</p>
     *
     * <p>The sink is called once per export, after the bytes are complete, and only for an
     * export that finished — an export that fails throws, and a report is not a way to
     * discover that it did.</p>
     *
     * @param reportSink where the report goes, or null to keep the log as the only channel
     * @since 2.5.0
     */
    public DocxSemanticBackend(java.util.function.Consumer<DocxExportReport> reportSink) {
        this.reportSink = reportSink;
        this.deterministicTimestamp = null;
    }

    private DocxSemanticBackend(Builder builder) {
        this.reportSink = builder.reportSink;
        this.deterministicTimestamp = builder.deterministicTimestamp;
    }

    /**
     * Starts a backend configured beyond what the constructors say.
     *
     * @return a new builder
     * @since 2.5.0
     */
    @com.demcha.compose.document.api.Beta
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Configures a {@link DocxSemanticBackend}. Each call replaces what it sets, and
     * {@link #build()} can be called more than once.
     *
     * @since 2.5.0
     */
    @com.demcha.compose.document.api.Beta
    public static final class Builder {

        private java.util.function.Consumer<DocxExportReport> reportSink;
        private Instant deterministicTimestamp;

        private Builder() {
        }

        /**
         * Where the export's {@link DocxExportReport} goes when an export ends. See
         * {@link DocxSemanticBackend#DocxSemanticBackend(java.util.function.Consumer)}.
         *
         * @param reportSink the sink, or null to keep the log as the only channel
         * @return this builder
         */
        public Builder reportSink(java.util.function.Consumer<DocxExportReport> reportSink) {
            this.reportSink = reportSink;
            return this;
        }

        /**
         * Enables (or disables) deterministic output. When enabled, the package's OPC
         * created / modified core properties are pinned to a fixed default timestamp and
         * every zip entry's modification time is normalized, so the same document exports to
         * byte-identical output across runs — for reproducible builds and byte-level output
         * tests. Disabled by default, the same contract the PDF and PPTX backends keep.
         *
         * <p>Embedded fonts are deterministic either way: each font's obfuscation key is
         * derived from the font rather than drawn at random.</p>
         *
         * @param enabled {@code true} to pin output at the default timestamp,
         *                {@code false} to keep POI's live timestamps
         * @return this builder
         */
        public Builder deterministic(boolean enabled) {
            this.deterministicTimestamp = enabled ? DEFAULT_DETERMINISTIC_INSTANT : null;
            return this;
        }

        /**
         * Enables deterministic output with an explicit timestamp. See
         * {@link #deterministic(boolean)}.
         *
         * <p>The instant is truncated to whole seconds up front: zip DOS times carry
         * two-second resolution and OPC dates whole seconds, so truncating keeps every
         * serialized clock in agreement for sub-second inputs.</p>
         *
         * @param timestamp the instant to pin the package's clocks to
         * @return this builder
         * @throws NullPointerException if {@code timestamp} is null
         */
        public Builder deterministic(Instant timestamp) {
            this.deterministicTimestamp = java.util.Objects.requireNonNull(timestamp, "timestamp")
                    .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
            return this;
        }

        /**
         * @return a backend with this configuration
         */
        public DocxSemanticBackend build() {
            return new DocxSemanticBackend(this);
        }
    }

    @Override
    public String name() {
        return "docx-semantic";
    }

    /**
     * {@inheritDoc}
     *
     * <p>This backend asks for the layout, and pays the measurement and pagination pass for
     * it. Two of the things that decide how the file looks are measurements over the font —
     * how tall a line of text is, and how wide an {@code auto} column came out — and this
     * backend has no font runtime of its own. The engine made both already; without them
     * the export hands the questions to Word, whose answers are its own: measured against
     * the reference render, Word set each body line at 13.9pt where the document says 9.7,
     * and sized a table to its text rather than to the width the layout gave it.</p>
     *
     * <p>Nothing here depends on the layout being present. An export handed none still
     * writes a complete document: every place that reads a measured number falls back to
     * what the document itself states, and states what that costs.</p>
     */
    @Override
    public boolean requiresResolvedLayout() {
        return true;
    }

    @Override
    public byte[] export(DocumentGraph graph, SemanticExportContext context) throws Exception {
        return write(List.of(new SemanticSection(graph, context)), context.outputFile());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Each section becomes a Word section: its page size, orientation and margins, and its
     * own header and footer. Word ends a section at the paragraph that carries its section
     * properties, so every section but the last hands its properties to its last paragraph —
     * or to an empty one when it ends in a table — and the last keeps the document's.</p>
     *
     * <p>Three things follow what a multi-section PDF does, where Word would do otherwise
     * left alone:</p>
     * <ul>
     *   <li>page numbers start again at 1 in every section, and a zone's page total is the
     *       section's ({@code SECTIONPAGES}) rather than the document's;</li>
     *   <li>a section with no header or footer of its own gets an empty one, because Word
     *       would otherwise repeat the previous section's;</li>
     *   <li>metadata is taken from the first section that declares it.</li>
     * </ul>
     *
     * <p>Everything that belongs to the document rather than to a page is shared: one
     * styles part, whose Normal is the text style the whole document is mostly written in,
     * one font table holding every section's families (the first definition of a family
     * wins, as it does in the PDF), and one set of bookmark names, so a link in one section
     * reaches an anchor in another.</p>
     */
    @Override
    public byte[] exportSections(List<SemanticSection> sections) throws Exception {
        java.util.Objects.requireNonNull(sections, "sections");
        if (sections.isEmpty()) {
            throw new IllegalArgumentException("A document needs at least one section to export.");
        }
        return write(sections, null);
    }

    private byte[] write(List<SemanticSection> sections, Path outputFile) throws Exception {
        sectioned = sections.size() > 1;
        DocumentGraph whole = sectioned ? wholeDocument(sections) : sections.get(0).graph();
        java.util.Collection<FontFamilyDefinition> fonts = sectioned
                ? fontsOf(sections)
                : sections.get(0).context().customFontFamilies();
        shapeContainerWarned.set(false);
        chartWarned.set(false);
        containerRadiusWarned.set(false);
        warnedNodeKinds.clear();
        surfaceBehind = null;
        overlayDepth = 0;
        insetLeft = 0;
        insetRight = 0;
        nextDrawingId = 100_000;
        listNumbering.clear();
        report = new DocxExportReport.Builder();
        bookmarkNames = new DocxBookmarkNames();
        headingLevels = headingLevelsIn(whole);
        bookmarkedAnchors = bookmarkedAnchorsIn(whole);
        wordFamilies = DocxFontTable.familiesByName(fonts);
        documentDefaultStyle = dominantTextStyle(whole);
        currentCell = null;
        currentCellWidth = Double.NaN;
        // Kinds of zone an earlier section wrote: Word repeats a section's header and footer
        // in the sections after it that have none of their own.
        java.util.Set<DocumentHeaderFooterZone> earlierZones =
                java.util.EnumSet.noneOf(DocumentHeaderFooterZone.class);
        boolean evenAndOdd = distinguishesEvenPages(sections);
        try (XWPFDocument document = new XWPFDocument()) {
            if (evenAndOdd) {
                // A document-wide setting in Word, so every section states its even pages.
                document.setEvenAndOddHeadings(true);
            }
            for (int index = 0; index < sections.size(); index++) {
                SemanticSection section = sections.get(index);
                SemanticExportContext context = section.context();
                if (index > 0) {
                    endSection(document);
                }
                beginSection(section, index);
                applyPageGeometry(document, context.canvas());
                if (index == 0) {
                    writeStylesPart(document);
                    DocxFontTable.write(document, whole, fonts, report);
                    applyMetadata(document, metadataOf(sections));
                }
                earlierZones.addAll(applyPageZones(document, context.outputOptions().zones(),
                        evenAndOdd, earlierZones));
                if (applyPageBackgrounds(document, context.layoutGraph(), evenAndOdd)) {
                    earlierZones.add(DocumentHeaderFooterZone.HEADER);
                }
                for (DocumentNode root : section.graph().roots()) {
                    writeNode(document, root);
                }
                // Nothing follows the last root to carry what it holds below itself.
                flushSpacingAfter();
            }
            if (deterministicTimestamp != null) {
                DocxDeterminism.pinCoreProperties(document, deterministicTimestamp);
            }
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                document.write(output);
                byte[] bytes = deterministicTimestamp == null
                        ? output.toByteArray()
                        : DocxDeterminism.normalizeZipEntries(output.toByteArray(), deterministicTimestamp);
                if (outputFile != null) {
                    Files.write(outputFile, bytes);
                }
                // Handed over once the bytes exist, so a caller is never told what an
                // export lost by an export that did not finish.
                if (reportSink != null) {
                    reportSink.accept(report.build());
                }
                return bytes;
            }
        }
    }

    /**
     * Resets what a section starts from: its own measurements, page width and height, and no
     * spacing carried in from the section before it.
     */
    private void beginSection(SemanticSection section, int index) {
        SemanticExportContext context = section.context();
        layout = DocxLayoutMetrics.of(section.graph(), context.layoutGraph());
        if (layout.isEmpty()) {
            // Said once per section, naming it when there are several: without measurements
            // the line height is Word's and so is every auto column, and a caller comparing
            // this file against the rendered page deserves to know that before they look.
            report.add(DocxExportReport.Severity.APPROXIMATED, "measured geometry",
                    sectioned ? "section " + (index + 1) : null,
                    (sectioned ? "this section" : "this document")
                    + " could not be laid out, so line heights and auto column "
                    + "widths are the editor's rather than the engine's");
        }
        carriedSpacingBefore = 0;
        pendingSpacingAfter = 0;
        pendingItemSpacing = 0;
        anItemWasWritten = false;
        lastBodyParagraph = null;
        contentWidth = context.canvas() == null ? Double.MAX_VALUE : context.canvas().innerWidth();
        canvasHeight = context.canvas() == null ? Double.NaN : context.canvas().height();
    }

    /**
     * Ends the section written so far, so the next one starts with a page of its own.
     *
     * <p>Word keeps a section's properties on the last paragraph of that section, and the
     * body's own properties belong to the last section alone. So the properties written for
     * this section move onto its last paragraph and the body starts again empty.</p>
     *
     * <p>Two endings have no paragraph of their own to carry them, and get an added one:
     * a section that ends in a table, since Word does not end a section on a table, and a
     * section that wrote nothing into the body at all — an empty session, or one of shapes
     * this export drops — whose last paragraph is still the one closing the section before
     * it. Handing that paragraph these properties would overwrite the earlier section's,
     * folding two sections into one. The added paragraph is one invisible point tall, so it
     * cannot push a full page onto a page of its own.</p>
     */
    private void endSection(XWPFDocument document) {
        CTBody body = document.getDocument().getBody();
        CTSectPr finished = (CTSectPr) bodySectPr(document).copy();
        List<IBodyElement> elements = document.getBodyElements();
        XWPFParagraph carrier = !elements.isEmpty()
                                && elements.get(elements.size() - 1) instanceof XWPFParagraph last
                                && !(last.getCTP().isSetPPr() && last.getCTP().getPPr().isSetSectPr())
                ? last
                : collapsed(document.createParagraph());
        CTPPr properties = carrier.getCTP().isSetPPr()
                ? carrier.getCTP().getPPr()
                : carrier.getCTP().addNewPPr();
        properties.setSectPr(finished);
        body.unsetSectPr();
    }

    /**
     * An empty header or footer for a kind of page the section draws none on, so Word does not
     * put another one there — the previous section's, or the section's own for other pages.
     *
     * <p>Its one paragraph is a point tall. When the section has no zone of this kind at all,
     * it also sits against the page edge: left at Word's default distance it would reach past
     * a narrow margin, and Word would push the body down to make room for a header the page
     * does not draw. A section that does draw one keeps that one's distance.</p>
     */
    private static void blankZone(XWPFHeaderFooterPolicy policy, CTSectPr sectPr, boolean header,
                                  org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr.Enum type,
                                  boolean againstTheEdge) {
        XWPFHeaderFooter blank = header ? policy.createHeader(type) : policy.createFooter(type);
        collapsed(blank.createParagraph());
        if (againstTheEdge && sectPr.isSetPgMar()) {
            if (header) {
                sectPr.getPgMar().setHeader(BigInteger.ZERO);
            } else {
                sectPr.getPgMar().setFooter(BigInteger.ZERO);
            }
        }
    }

    /**
     * Whether any section has a zone Word can only place with different even and odd pages.
     *
     * <p>Word turns that on for the whole document, not per section, so it is decided before
     * any section is written.</p>
     */
    private static boolean distinguishesEvenPages(List<SemanticSection> sections) {
        for (SemanticSection section : sections) {
            List<DocumentPageZone> zones = section.context().outputOptions().zones();
            if (zones == null) {
                continue;
            }
            int pages = section.context().layoutGraph() == null
                    ? 0
                    : section.context().layoutGraph().totalPages();
            for (DocumentPageZone zone : zones) {
                java.util.Set<DocxPageClasses.PageClass> drawnOn = DocxPageClasses.of(zone, pages);
                if (drawnOn != null
                    && drawnOn.contains(DocxPageClasses.PageClass.EVEN)
                       != drawnOn.contains(DocxPageClasses.PageClass.LATER_ODD)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Makes a paragraph that exists only for Word's structure take a single point. */
    private static XWPFParagraph collapsed(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().isSetPPr()
                ? paragraph.getCTP().getPPr()
                : paragraph.getCTP().addNewPPr();
        CTSpacing spacing = properties.isSetSpacing() ? properties.getSpacing() : properties.addNewSpacing();
        spacing.setBefore(BigInteger.ZERO);
        spacing.setAfter(BigInteger.ZERO);
        spacing.setLineRule(STLineSpacingRule.EXACT);
        spacing.setLine(BigInteger.valueOf(Math.round(POINT_TO_TWIP)));
        return paragraph;
    }

    private static CTSectPr bodySectPr(XWPFDocument document) {
        CTBody body = document.getDocument().getBody();
        return body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
    }

    /** Every section's roots, in order: what the document as a whole is written in. */
    private static DocumentGraph wholeDocument(List<SemanticSection> sections) {
        List<DocumentNode> roots = new ArrayList<>();
        for (SemanticSection section : sections) {
            roots.addAll(section.graph().roots());
        }
        return new DocumentGraph(roots);
    }

    /** Every section's font families, the first definition of a family winning. */
    private static List<FontFamilyDefinition> fontsOf(List<SemanticSection> sections) {
        java.util.Map<FontName, FontFamilyDefinition> byName = new java.util.LinkedHashMap<>();
        for (SemanticSection section : sections) {
            for (FontFamilyDefinition family : section.context().customFontFamilies()) {
                byName.putIfAbsent(family.name(), family);
            }
        }
        return new ArrayList<>(byName.values());
    }

    /** The first section's metadata that states any, as a multi-section PDF takes it. */
    private static DocumentMetadata metadataOf(List<SemanticSection> sections) {
        for (SemanticSection section : sections) {
            DocumentMetadata metadata = section.context().outputOptions().metadata();
            if (metadata != null) {
                return metadata;
            }
        }
        return null;
    }

    private void applyMetadata(XWPFDocument document, DocumentMetadata metadata) {
        if (metadata != null) {
            org.apache.poi.ooxml.POIXMLProperties props = document.getProperties();
            if (metadata.getTitle() != null) {
                props.getCoreProperties().setTitle(metadata.getTitle());
            }
            if (metadata.getAuthor() != null) {
                props.getCoreProperties().setCreator(metadata.getAuthor());
            }
            if (metadata.getSubject() != null) {
                props.getCoreProperties().setSubjectProperty(metadata.getSubject());
            }
            if (metadata.getKeywords() != null) {
                props.getCoreProperties().setKeywords(metadata.getKeywords());
            }
        }
        // The text header/footer, watermark and protection are still ignored: the
        // three text slots and their placeholder tokens describe a painted band
        // rather than content Word can own. A page zone does describe content, so
        // that is the one that maps — see applyPageZones.
    }

    /**
     * Writes each page zone into a real Word header or footer part.
     *
     * <p>A fixed-layout backend receives a paginated document and draws the zone
     * per page; Word paginates for itself, so the zone is written once as a
     * definition and Word repeats it. That is why the content function is called
     * with an unpaginated {@link PageContext}: a page number baked into text here
     * would be right on one page and wrong on the others, so a zone that needs one
     * places {@code pageNumber()} and gets a live {@code PAGE} field instead.</p>
     *
     * <p>A page predicate is the other fixed-layout piece: {@code appliesTo} tests a page,
     * and Word owns pagination, so there is no page here to test. What Word does have is a
     * header and footer per kind of page — the first, even ones, the rest — and the
     * predicate is asked which of those it is drawn on ({@link DocxPageClasses}). A zone
     * on the first page only becomes the section's first-page header, with the section
     * stating a title page; one on even pages becomes the even-page header, with the
     * document stating different even and odd pages. A predicate that does not follow those
     * kinds is written on every page, content beating absence, and the export reports what
     * it could not honour.</p>
     *
     * <p>A kind of page the section draws no zone of that kind on gets an empty part when
     * Word would otherwise put something there: the section's own zone for other pages, or
     * an earlier section's zone, which Word repeats in a section without one.</p>
     *
     * @param evenAndOdd   whether the document states different even and odd pages
     * @param earlierZones the kinds of zone an earlier section wrote
     * @return the kinds of zone this section wrote
     */
    private java.util.Set<DocumentHeaderFooterZone> applyPageZones(
            XWPFDocument document,
            List<DocumentPageZone> zones,
            boolean evenAndOdd,
            java.util.Set<DocumentHeaderFooterZone> earlierZones) {
        // The page height the zones are measured against is the canvas's, which is what the
        // page geometry was written from — not a value parsed back out of the XML.
        java.util.Set<DocumentHeaderFooterZone> written =
                java.util.EnumSet.noneOf(DocumentHeaderFooterZone.class);
        List<DocumentPageZone> sectionZones = zones == null ? List.of() : zones;
        if (sectionZones.isEmpty() && earlierZones.isEmpty()) {
            return written;
        }
        List<DocumentNode> contents = new ArrayList<>();
        List<java.util.Set<DocxPageClasses.PageClass>> drawnOn = new ArrayList<>();
        for (DocumentPageZone zone : sectionZones) {
            DocumentNode content = zone.getContent() == null
                    ? null
                    : zone.getContent().apply(PageContext.unpaginated());
            contents.add(content);
            drawnOn.add(content == null
                    ? java.util.EnumSet.noneOf(DocxPageClasses.PageClass.class)
                    : pageClassesOf(zone));
        }
        boolean titlePage = false;
        for (int index = 0; index < sectionZones.size(); index++) {
            java.util.Set<DocxPageClasses.PageClass> classes = drawnOn.get(index);
            if (contents.get(index) != null
                && classes.contains(DocxPageClasses.PageClass.FIRST)
                   != classes.contains(DocxPageClasses.PageClass.LATER_ODD)) {
                titlePage = true;
            }
        }
        // Bound to the section being written, whose properties are the body's until it ends.
        CTSectPr sectPr = bodySectPr(document);
        if (titlePage && !sectPr.isSetTitlePg()) {
            sectPr.addNewTitlePg();
        }
        XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(document, sectPr);
        java.util.Set<String> parts = new java.util.HashSet<>();
        for (int index = 0; index < sectionZones.size(); index++) {
            DocumentPageZone zone = sectionZones.get(index);
            DocumentNode content = contents.get(index);
            java.util.Set<DocxPageClasses.PageClass> classes = drawnOn.get(index);
            if (content == null || classes.isEmpty()) {
                continue;
            }
            boolean header = zone.getZone() == DocumentHeaderFooterZone.HEADER;
            for (org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr.Enum type
                    : partTypes(titlePage, evenAndOdd)) {
                if (classes.contains(pageClassOf(type))) {
                    writeZoneLine(header ? policy.createHeader(type) : policy.createFooter(type), content);
                    parts.add(zone.getZone() + "/" + type);
                }
            }
            placeZone(document, zone, index, header);
            written.add(zone.getZone());
        }
        for (DocumentHeaderFooterZone kind : DocumentHeaderFooterZone.values()) {
            if (!written.contains(kind) && !earlierZones.contains(kind)) {
                continue;
            }
            for (org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr.Enum type
                    : partTypes(titlePage, evenAndOdd)) {
                if (!parts.contains(kind + "/" + type)) {
                    blankZone(policy, sectPr, kind == DocumentHeaderFooterZone.HEADER, type,
                            !written.contains(kind));
                }
            }
        }
        return written;
    }

    /**
     * The kinds of page a zone is drawn on — every kind when its predicate does not follow
     * them, which is written down as what the export could not honour.
     */
    private java.util.Set<DocxPageClasses.PageClass> pageClassesOf(DocumentPageZone zone) {
        java.util.Set<DocxPageClasses.PageClass> classes = DocxPageClasses.of(zone, layout.pageCount());
        if (classes != null) {
            return classes;
        }
        LOG.warn("docx.zone.pagePredicate zone={} — its appliesTo predicate does not follow Word's"
                + " first, even and odd pages, so the zone is written on every page; per-page"
                + " chrome of that kind needs a fixed-layout backend.", zone.getZone());
        report.add(DocxExportReport.Severity.APPROXIMATED, "page zone", null,
                "its page predicate picks pages Word has no header or footer for — only the first,"
                + " even and odd pages can differ — so it is written on every page");
        return java.util.EnumSet.allOf(DocxPageClasses.PageClass.class);
    }

    /**
     * Paints the section's page backgrounds behind the text of every page it has.
     *
     * <p>Each fill is a shape anchored to the page in the section's headers — every kind of
     * header it shows, first page and even pages included — so it is drawn on each page
     * whichever header that page takes (see {@link DocxPageBackgrounds}). A section without a
     * header gets an empty one against the page edge to carry them, the way a section with
     * no zone of a kind gets one that shows nothing.</p>
     *
     * <p>Word shows a section with no header of its own the header of the section before it,
     * shapes and all, so a header written here counts as one the section wrote: a later
     * section without backgrounds then gets an empty header of its own, rather than the
     * cover's colour behind its text.</p>
     *
     * @return whether the section now has headers carrying its backgrounds
     */
    private boolean applyPageBackgrounds(XWPFDocument document,
                                         com.demcha.compose.document.layout.LayoutGraph graph,
                                         boolean evenAndOdd) {
        List<DocxPageBackgrounds.Fill> fills = DocxPageBackgrounds.of(graph);
        if (fills.isEmpty()) {
            return false;
        }
        CTSectPr sectPr = bodySectPr(document);
        XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(document, sectPr);
        boolean hasHeader = policy.getDefaultHeader() != null || policy.getFirstPageHeader() != null
                            || policy.getEvenPageHeader() != null;
        for (org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr.Enum type
                : partTypes(sectPr.isSetTitlePg(), evenAndOdd)) {
            if (headerOf(policy, type) == null) {
                blankZone(policy, sectPr, true, type, !hasHeader);
            }
            org.apache.poi.xwpf.usermodel.XWPFHeader header = headerOf(policy, type);
            XWPFParagraph carrier = header.getParagraphs().isEmpty()
                    ? collapsed(header.createParagraph())
                    : header.getParagraphs().get(0);
            XWPFRun run = carrier.createRun();
            for (int order = 0; order < fills.size(); order++) {
                run.getCTR().addNewDrawing().set(
                        DocxPageBackgrounds.drawing(fills.get(order), nextDrawingId++, order));
            }
        }
        return true;
    }

    private static org.apache.poi.xwpf.usermodel.XWPFHeader headerOf(
            XWPFHeaderFooterPolicy policy,
            org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr.Enum type) {
        if (type == XWPFHeaderFooterPolicy.FIRST) {
            return policy.getFirstPageHeader();
        }
        return type == XWPFHeaderFooterPolicy.EVEN ? policy.getEvenPageHeader() : policy.getDefaultHeader();
    }

    /** The header and footer kinds the section uses: the default, and the ones it states. */
    private static List<org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr.Enum> partTypes(
            boolean titlePage, boolean evenAndOdd) {
        List<org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr.Enum> types = new ArrayList<>(3);
        types.add(XWPFHeaderFooterPolicy.DEFAULT);
        if (titlePage) {
            types.add(XWPFHeaderFooterPolicy.FIRST);
        }
        if (evenAndOdd) {
            types.add(XWPFHeaderFooterPolicy.EVEN);
        }
        return types;
    }

    /** The kind of page a Word header or footer type is shown on. */
    private static DocxPageClasses.PageClass pageClassOf(
            org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr.Enum type) {
        if (type == XWPFHeaderFooterPolicy.FIRST) {
            return DocxPageClasses.PageClass.FIRST;
        }
        return type == XWPFHeaderFooterPolicy.EVEN
                ? DocxPageClasses.PageClass.EVEN
                : DocxPageClasses.PageClass.LATER_ODD;
    }

    /**
     * Puts a header or footer as far from its page edge as the page puts it.
     *
     * <p>Nothing was written, so Word used its own distance — 36pt — and the probe's footer
     * sat 14.5pt higher than the page draws it, on every page. Word holds the distance as
     * {@code w:pgMar/@w:header} and {@code @w:footer}, so this is a mapping.</p>
     *
     * <p>The distance is where the zone's content landed in the resolved layout, which is
     * the number the page was drawn with. Without a layout it falls back to the zone's own
     * padding on that edge — a band's content is laid from its top, so for a footer that is
     * the nearer estimate rather than the exact one, and it is only reached when the
     * document could not be laid out at all.</p>
     */
    private void placeZone(XWPFDocument document, DocumentPageZone zone, int index, boolean header) {
        CTSectPr sectPr = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();
        CTPageMar margin = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        double pageHeight = canvasHeight;
        OptionalDouble measured = Double.isNaN(pageHeight)
                ? OptionalDouble.empty()
                : layout.zoneDistanceFromEdge(index, header, pageHeight);
        DocumentInsets padding = zone.getPadding() == null ? DocumentInsets.zero() : zone.getPadding();
        double distance = measured.orElse(header ? padding.top() : padding.bottom());
        if (header) {
            margin.setHeader(BigInteger.valueOf(toTwips(distance)));
        } else {
            margin.setFooter(BigInteger.valueOf(toTwips(distance)));
        }
    }

    /**
     * Writes one zone's subtree as a single line in the header/footer part.
     *
     * <p>A zone is one band deep, so its children belong on one Word line rather
     * than stacked paragraphs: a row's children become runs in document order, and
     * a flex spacer becomes the tab that carries the rest to the right margin —
     * which is how a Word footer is built by hand anyway.</p>
     */
    private void writeZoneLine(XWPFHeaderFooter target, DocumentNode content) {
        XWPFParagraph para = target.createParagraph();
        para.setSpacingBefore(0);
        para.setSpacingAfter(0);
        // Reuse the properties the spacing calls above already created. addNewPPr() would
        // append a second w:pPr, and Word reads the first — the tab stop would be in the
        // file and ignored, so the page number fell back to Word's default half-inch grid
        // instead of sitting at the right margin.
        CTPPr properties = para.getCTP().isSetPPr()
                ? para.getCTP().getPPr()
                : para.getCTP().addNewPPr();
        CTTabStop tab = properties.addNewTabs().addNewTab();
        tab.setVal(STTabJc.RIGHT);
        tab.setPos(java.math.BigInteger.valueOf(Math.round(contentWidth * TWIPS_PER_POINT)));

        List<DocumentNode> parts = content instanceof RowNode row ? row.children() : List.of(content);
        for (DocumentNode part : parts) {
            appendZonePart(para, part);
        }
    }

    private void appendZonePart(XWPFParagraph para, DocumentNode part) {
        if (part instanceof ParagraphNode paragraph) {
            writeParagraphRuns(para, paragraph, false);
        } else if (part instanceof PageFieldNode field) {
            appendPageField(para, field);
        } else if (part instanceof SpacerNode) {
            para.createRun().addTab();
        } else {
            warnUnsupportedZoneNode(part);
        }
    }

    /**
     * Emits Word's own field rather than a number, so it stays correct when the
     * reader adds a page or edits the document.
     */
    private void appendPageField(XWPFParagraph para, PageFieldNode field) {
        CTSimpleField simple = para.getCTP().addNewFldSimple();
        // A multi-section document numbers each section from 1, so the total a zone states is
        // its section's; in a document of one section the two are the same count.
        String total = sectioned ? " SECTIONPAGES " : " NUMPAGES ";
        simple.setInstr(field.kind() == PageFieldKind.TOTAL ? total : " PAGE ");
        // Word repaints the field on open; the placeholder run is what a reader
        // sees before that happens, and what a text extractor finds. It carries
        // the node's text style like any other run — Word keeps a field result's
        // formatting when it repaints it, so an unstyled placeholder would snap
        // a styled page number back to the document default.
        XWPFRun run = new XWPFRun(simple.addNewR(), (IRunBody) para);
        applyStyle(run, field.textStyle());
        run.setText(fieldPlaceholder(field.kind()));
    }

    /**
     * What a page field reads before an editor updates it.
     *
     * <p>A page number is written as 1: a header or footer is one definition for every page,
     * so no single number is right. A total is one number, and the layout already counted
     * it — the section's pages, which is what {@code SECTIONPAGES} and, in a document of one
     * section, {@code NUMPAGES} will come to. Not every editor updates the field: measured in
     * LibreOffice, a {@code SECTIONPAGES} total stays at the text written here, so a
     * placeholder of 1 showed "page 2 of 1".</p>
     */
    private String fieldPlaceholder(PageFieldKind kind) {
        if (kind == PageFieldKind.TOTAL && layout.pageCount() > 0) {
            return Integer.toString(layout.pageCount());
        }
        return "1";
    }

    private void warnUnsupportedZoneNode(DocumentNode node) {
        if (warnedNodeKinds.add("zone:" + node.nodeKind())) {
            LOG.warn("docx.zone.unsupportedNode kind={} — a page zone maps paragraphs, page fields"
                    + " and spacers onto a Word header/footer; other nodes are skipped",
                    node.nodeKind());
        }
    }

    private void writeNode(XWPFDocument document, DocumentNode node) throws Exception {
        boolean keepTogether = node.keepTogether() && layout.onOnePage(node);
        boolean keepWithNext = node.keepWithNext() && layout.onOnePage(node);
        String anchor = blockAnchorOf(node, overlayDepth == 0);
        if (!keepTogether && !keepWithNext && anchor == null) {
            writeNodeContent(document, node);
            return;
        }
        // What the block writes lands where writing is going on — the body, or the cell being
        // filled. Read off the body alone, a block inside a card wrote nothing, and its
        // anchor and its keeps were silently dropped.
        XWPFTableCell destination = currentCell;
        java.util.function.Supplier<List<IBodyElement>> elements = destination == null
                ? document::getBodyElements
                : destination::getBodyElements;
        int first = elements.get().size();
        // The paragraph closing a table above, if the block takes it over, is the block's.
        XWPFParagraph closer = cellEndsWithItsTableCloser() ? tableCloser : null;
        writeNodeContent(document, node);
        if (closer != null && tableCloser != closer) {
            first--;
        }
        List<IBodyElement> written = elements.get().subList(first, elements.get().size());
        if (keepTogether || keepWithNext) {
            keepOnOnePage(written, keepWithNext);
        }
        if (anchor != null) {
            bookmarkAround(written, anchor);
        }
    }

    /**
     * The anchor of a block that writes more than one paragraph, or none.
     *
     * <p>A paragraph wraps its own text in its bookmark as it writes it. Every other node
     * that can carry an anchor and reaches Word — a section, a container, a table, an
     * image — had none, so an internal link to it went nowhere and a page reference to it
     * had nothing to count.</p>
     */
    private static String blockAnchorOf(DocumentNode node, boolean inFlow) {
        if (node instanceof SectionNode section) {
            return section.anchor();
        }
        if (node instanceof ContainerNode container) {
            return container.anchor();
        }
        if (node instanceof TableNode table) {
            return table.anchor();
        }
        if (node instanceof ImageNode image) {
            return image.anchor();
        }
        if (node instanceof com.demcha.compose.document.node.BarcodeNode barcode) {
            return barcode.anchor();
        }
        // Only a drawing that reaches Word as a rule has a paragraph to hold its bookmark.
        if (inFlow && node instanceof com.demcha.compose.document.node.LineNode line && DocxRules.of(line) != null) {
            return line.anchor();
        }
        if (inFlow && node instanceof com.demcha.compose.document.node.ShapeNode shape && DocxRules.of(shape) != null) {
            return shape.anchor();
        }
        return null;
    }

    /**
     * Wraps what a block wrote in the bookmark its anchor names.
     *
     * <p>The bookmark opens at the start of the first paragraph the block wrote and closes
     * at the end of the last — the first and last cell's, for a block that begins or ends
     * with a table — so a link lands on the block's first line and a page reference counts
     * the page it starts on. A block that wrote nothing has nothing to mark.</p>
     */
    private void bookmarkAround(List<IBodyElement> written, String anchor) {
        XWPFParagraph first = null;
        XWPFParagraph last = null;
        for (IBodyElement element : written) {
            XWPFParagraph opening = element instanceof XWPFTable table
                    ? edgeParagraph(table, true)
                    : element instanceof XWPFParagraph paragraph ? paragraph : null;
            XWPFParagraph closing = element instanceof XWPFTable table
                    ? edgeParagraph(table, false)
                    : opening;
            if (first == null) {
                first = opening;
            }
            if (closing != null) {
                last = closing;
            }
        }
        String name = first == null || last == null ? null : bookmarkNames.nameFor(anchor);
        if (name == null) {
            return;
        }
        int id = bookmarkNames.nextId();
        CTBookmark start = first.getCTP().addNewBookmarkStart();
        start.setId(BigInteger.valueOf(id));
        start.setName(name);
        moveToParagraphStart(first.getCTP(), start);
        last.getCTP().addNewBookmarkEnd().setId(BigInteger.valueOf(id));
    }

    /**
     * The first paragraph of a table's first cell, or the last of its last cell — inside the
     * table that cell opens with, for a card whose first block is a table.
     */
    private static XWPFParagraph edgeParagraph(XWPFTable table, boolean opening) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) {
            return null;
        }
        XWPFTableRow row = rows.get(opening ? 0 : rows.size() - 1);
        List<XWPFTableCell> cells = row.getTableCells();
        if (cells.isEmpty()) {
            return null;
        }
        List<IBodyElement> inside = cells.get(opening ? 0 : cells.size() - 1).getBodyElements();
        if (inside.isEmpty()) {
            return null;
        }
        IBodyElement edge = inside.get(opening ? 0 : inside.size() - 1);
        if (edge instanceof XWPFTable nested) {
            return edgeParagraph(nested, opening);
        }
        return edge instanceof XWPFParagraph paragraph ? paragraph : null;
    }

    /**
     * Moves an element added at the end of a paragraph to the start of its content, just after
     * its properties — where a bookmark has to open for a link to land on the first word.
     */
    private static void moveToParagraphStart(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP paragraph,
                                             org.apache.xmlbeans.XmlObject element) {
        try (org.apache.xmlbeans.XmlCursor source = element.newCursor();
             org.apache.xmlbeans.XmlCursor target = paragraph.newCursor()) {
            if (!target.toFirstChild()) {
                return;
            }
            if (paragraph.isSetPPr() && !target.toNextSibling()) {
                return;
            }
            if (!target.isAtSamePositionAs(source)) {
                source.moveXml(target);
            }
        }
    }

    /**
     * Tells Word to keep together what the layout kept together.
     *
     * <p>{@code keepTogether()} moves a block to the next page whole rather than letting it
     * run over the break, and {@code keepWithNext()} does the same for a block and the first
     * line of the one after it. Word re-paginates on its own and was told neither, so a card
     * the page held together split across Word's break, and a heading the page moved down
     * with its body was left at the foot of the page above it.</p>
     *
     * <p>Word says both with two paragraph properties: {@code w:keepLines} keeps a paragraph's
     * own lines on one page, and {@code w:keepNext} keeps it on the page of whatever follows.
     * Every paragraph the block wrote gets the first and every one but the last the second,
     * which chains the block into one unit; a block kept with the next gives its last
     * paragraph {@code w:keepNext} as well. A table inside the block takes part row by row,
     * because Word reads keep-with-next on a row's paragraphs as keeping the row with the
     * next one.</p>
     *
     * <p>Only a block the layout placed on one page is kept. The layout keeps a block together
     * only when a page can hold it and lets a taller one flow, and a block that ran over a
     * page break is exactly that; without a layout there is nothing to say either way.</p>
     */
    private static void keepOnOnePage(List<IBodyElement> written, boolean withNext) {
        List<List<XWPFParagraph>> units = new ArrayList<>();
        for (IBodyElement element : written) {
            if (element instanceof XWPFParagraph paragraph) {
                units.add(List.of(paragraph));
            } else if (element instanceof XWPFTable table) {
                for (XWPFTableRow row : table.getRows()) {
                    List<XWPFParagraph> paragraphs = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        paragraphs.addAll(cell.getParagraphs());
                    }
                    units.add(paragraphs);
                }
            }
        }
        for (int index = 0; index < units.size(); index++) {
            boolean last = index == units.size() - 1;
            for (XWPFParagraph paragraph : units.get(index)) {
                CTPPr properties = paragraph.getCTP().isSetPPr()
                        ? paragraph.getCTP().getPPr()
                        : paragraph.getCTP().addNewPPr();
                if (!properties.isSetKeepLines()) {
                    properties.addNewKeepLines();
                }
                if ((!last || withNext) && !properties.isSetKeepNext()) {
                    properties.addNewKeepNext();
                }
            }
        }
    }

    private void writeNodeContent(XWPFDocument document, DocumentNode node) throws Exception {
        boolean overlay = isOverlay(node);
        if (overlay) {
            overlayDepth++;
        }
        try {
            dispatchNode(document, node);
        } finally {
            if (overlay) {
                overlayDepth--;
            }
        }
    }

    /**
     * Whether a node lays its children over one another rather than one after another.
     *
     * <p>Its children are still written, in order, for the text in them; but a drawn rule
     * among them is part of a picture — a skill meter's track and the fill laid over it — and
     * written as rules in the flow they came out as two bars one under the other.</p>
     */
    private static boolean isOverlay(DocumentNode node) {
        return node instanceof com.demcha.compose.document.node.LayerStackNode
               || node instanceof com.demcha.compose.document.node.CanvasLayerNode
               || node instanceof ShapeContainerNode;
    }

    /** The rule a node is in the flow, or {@code null} — over something else it is not one. */
    private DocxRules.Rule ruleOf(DocumentNode node) {
        return overlayDepth == 0 ? DocxRules.of(node) : null;
    }

    private void dispatchNode(XWPFDocument document, DocumentNode node) throws Exception {
        DocxRules.Rule rule = ruleOf(node);
        if (node instanceof ParagraphNode paragraph) {
            writeParagraph(document, paragraph);
        } else if (node instanceof com.demcha.compose.document.node.PageReferenceNode reference) {
            writePageReference(document, reference);
        } else if (node instanceof ImageNode image) {
            writeImage(document, image);
        } else if (node instanceof com.demcha.compose.document.node.BarcodeNode barcode) {
            writeBarcode(document, barcode);
        } else if (rule != null) {
            writeRule(document, node, rule);
        } else if (node instanceof TableNode table) {
            writeTableWithItsOwnSpacing(document, table);
        } else if (node instanceof SpacerNode spacer) {
            writeSpacer(document, spacer);
        } else if (node instanceof PageBreakNode) {
            writePageBreak(document);
        } else if (node instanceof RowNode row) {
            writeTableWithItsOwnSpacing(document, row);
        } else if (node instanceof ShapeContainerNode shapeContainer) {
            writeShapeContainer(document, shapeContainer);
        } else if (node instanceof ChartNode chart) {
            writeChartFallback(document, chart);
        } else if (node instanceof com.demcha.compose.document.node.ListNode list) {
            writeList(document, list);
        } else if (node instanceof com.demcha.compose.document.layout.HorizontalBandContentNode band) {
            writeInBand(document, band);
        } else if (node instanceof ContainerNode || node instanceof SectionNode
                   || node instanceof com.demcha.compose.document.node.LayerStackNode
                   || node instanceof com.demcha.compose.document.node.CanvasLayerNode
                   || isSemanticallyTransparent(node)) {
            // Overlay/positioned wrappers have no DOCX analogue for their
            // geometry, but their children can be semantic (text, images) —
            // render them sequentially rather than dropping the subtree.
            // A fill or a border is the exception: the container is then a panel, written
            // as a one-cell table carrying both, instead of disappearing.
            writeContainerChildren(document, node);
        } else {
            // Geometry-only node kinds (line, ellipse, shape, path, polygon)
            // have no semantic Word analogue. Warn once per kind so a
            // dropped chart-line or icon is visible in the log instead of
            // silently missing; authors needing pixel-perfect output use the
            // PDF fixed-layout backend.
            warnUnsupported(node);
        }
    }

    /**
     * One warning per dropped node kind, deduplicated across the export.
     *
     * <p>The report is told about every one of them, not one per kind: a caller asking
     * what the document lost wants the three charts it lost, and which three. The log is
     * the summary and the report is the record.</p>
     */
    private void warnUnsupported(DocumentNode node) {
        if (warnedNodeKinds.add(node.nodeKind())) {
            LOG.warn("DocxSemanticBackend: dropping '{}' node(s) — geometry has no semantic "
                     + "Word analogue; use the PDF backend for pixel-perfect output", node.nodeKind());
        }
        report.add(DocxExportReport.Severity.DROPPED, node.nodeKind(), layout.pathOf(node),
                "geometry has no semantic Word analogue, so it is not in the document at all");
    }

    /**
     * One warning per dropped inline-run kind, deduplicated across the
     * export — the inline mirror of {@link #warnUnsupported(DocumentNode)}.
     * Every kind the model has is written today — text and chips as runs,
     * pictures, icons, emoji and shapes as pictures — so this speaks only for a
     * kind added later and not yet taught to this export, which would otherwise
     * vanish from the paragraph with no signal at all.
     */
    private void warnDroppedInlineRuns(ParagraphNode node) {
        warnDroppedInlineRuns(node.inlineRuns(), layout.pathOf(node));
    }

    private void warnDroppedInlineRuns(List<InlineRun> runs, String path) {
        for (InlineRun run : runs) {
            if (run instanceof InlineTextRun || run instanceof InlineHighlightRun
                || run instanceof InlineImageRun || run instanceof InlineSvgRun
                || run instanceof InlineShapeRun) {
                continue;
            }
            String kind = run.getClass().getSimpleName();
            if (warnedNodeKinds.add("inline:" + kind)) {
                LOG.warn("DocxSemanticBackend: dropping inline '{}' run(s) — no semantic Word "
                         + "analogue; the paragraph text renders without them, use the PDF "
                         + "backend for full fidelity", kind);
            }
            report.add(DocxExportReport.Severity.DROPPED, "inline " + kind, path,
                    "no semantic Word analogue; the paragraph's text is written without it");
        }
    }

    /**
     * Word''s marker column, in twips. Chosen to sit close to the single space the text
     * path used rather than to Word''s much wider default, and stated as the convention it
     * is: measuring the marker would need a font runtime, which is the same thing
     * {@code hangingIndent} is missing and the reason its gap is unrepresentable here.
     */
    private static final int LIST_HANGING_TWIPS = 180;

    /** Added per nesting level, approximating the two spaces the text path indented by. */
    private static final int LIST_NESTING_STEP_TWIPS = 120;

    /**
     * Levels one Word list definition may hold.
     *
     * <p>{@code CT_AbstractNum/lvl} is {@code maxOccurs="9"} — Word has nine list levels
     * and {@code w:ilvl} runs 0..8. Writing a tenth produces a part that POI saves without
     * complaint and Word refuses to open, so a list nested deeper keeps its markers as
     * text rather than shipping a document that cannot be opened at all.</p>
     */
    private static final int MAX_LIST_LEVELS = 9;

    /**
     * Gives a list a real Word list definition, when it is one Word can express.
     *
     * <p>A marker written into the run text looks like a list and is not one: pressing
     * Enter yields a plain paragraph rather than the next item, which is the contract
     * failure this repairs. Attaching {@code w:numPr} makes Word own the marker, so the
     * list continues, renumbers and demotes the way a reader expects.</p>
     *
     * <p>This does not fix {@code markerGap}, and does not claim to. Word places content
     * at an absolute indent and cannot be told "one marker width plus a gap from here";
     * real numbering was measured against that requirement and rejected for it, and it is
     * still rejected. What it buys is behaviour, and it costs geometry: the marker column
     * is a stated constant rather than the measured gap.</p>
     *
     * @return the list definition to attach, or {@code null} when the list has to stay
     *         marker-prefixed text
     */
    private BigInteger numberingFor(XWPFDocument document,
                                    com.demcha.compose.document.node.ListNode list) {
        BigInteger existing = listNumbering.get(list);
        if (existing != null) {
            return existing;
        }
        List<String> levels = markerPerDepth(list);
        if (levels == null) {
            return null;
        }
        CTAbstractNum abstractNum = CTAbstractNum.Factory.newInstance();
        abstractNum.setAbstractNumId(BigInteger.valueOf(listNumbering.size()));
        for (int depth = 0; depth < levels.size(); depth++) {
            CTLvl level = abstractNum.addNewLvl();
            level.setIlvl(BigInteger.valueOf(depth));
            level.addNewStart().setVal(BigInteger.ONE);
            // Every marker this export can carry is a literal, so the format is BULLET
            // even when the literal is a digit: Word must draw the marker the author
            // wrote, not one it derives from the item''s position.
            level.addNewNumFmt().setVal(STNumberFormat.BULLET);
            level.addNewLvlText().setVal(levels.get(depth));
            level.addNewLvlJc().setVal(STJc.LEFT);
            CTInd indent = level.addNewPPr().addNewInd();
            indent.setLeft(BigInteger.valueOf(
                    (long) LIST_HANGING_TWIPS + (long) LIST_NESTING_STEP_TWIPS * depth));
            indent.setHanging(BigInteger.valueOf(LIST_HANGING_TWIPS));
        }
        BigInteger abstractId = document.createNumbering()
                .addAbstractNum(new org.apache.poi.xwpf.usermodel.XWPFAbstractNum(abstractNum));
        BigInteger numId = document.getNumbering().addNum(abstractId);
        listNumbering.put(list, numId);
        return numId;
    }

    /**
     * The one marker each nesting depth uses, or {@code null} when the list cannot be a
     * Word list.
     *
     * <p>A Word list definition names one marker per level, so a list whose items at the
     * same depth carry different markers has no definition to be given and keeps writing
     * its markers as text. So does a list with a drawn marker, which has no Word analogue
     * at all, one with no marker, where numbering would add an indent the author did not
     * ask for, and one with rich items, whose runs the numbered path does not write.</p>
     */
    private static List<String> markerPerDepth(com.demcha.compose.document.node.ListNode list) {
        java.util.Map<Integer, String> perDepth = new java.util.TreeMap<>();
        // Seeded from the flat items only when one of them survives normalization. A list
        // whose flat items are all blank writes no paragraph for them, so claiming depth
        // zero for their marker would either reject a uniform nested list whose own depth
        // zero differs, or mint a definition nothing references.
        if (list.items().stream().anyMatch(item -> !com.demcha.compose.document.node.ListMarker
                .normalizeItemText(item, list.normalizeMarkers()).isBlank())) {
            if (!isPlainVisible(list.marker())) {
                return null;
            }
            perDepth.put(0, levelText(list.marker()));
        }
        for (com.demcha.compose.document.node.ListItem item : list.nestedItems()) {
            if (!collectMarkers(item, 0, perDepth)) {
                return null;
            }
        }
        if (perDepth.isEmpty() || perDepth.size() > MAX_LIST_LEVELS) {
            return null;
        }
        // Depths must be contiguous from zero; a definition cannot skip a level.
        for (int depth = 0; depth < perDepth.size(); depth++) {
            if (!perDepth.containsKey(depth)) {
                return null;
            }
        }
        return List.copyOf(perDepth.values());
    }

    private static boolean collectMarkers(com.demcha.compose.document.node.ListItem item,
                                          int depth,
                                          java.util.Map<Integer, String> perDepth) {
        if (item.isRich()) {
            return false;
        }
        com.demcha.compose.document.node.ListMarker marker =
                item.marker() != null
                        ? item.marker()
                        : com.demcha.compose.document.node.ListMarker.defaultForDepth(depth);
        if (!isPlainVisible(marker)) {
            return false;
        }
        String existing = perDepth.putIfAbsent(depth, levelText(marker));
        if (existing != null && !existing.equals(levelText(marker))) {
            return false;
        }
        for (com.demcha.compose.document.node.ListItem child : item.children()) {
            if (!collectMarkers(child, depth + 1, perDepth)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPlainVisible(com.demcha.compose.document.node.ListMarker marker) {
        return !marker.isRich() && marker.isVisible() && !marker.value().isBlank();
    }

    /**
     * The marker as Word's {@code w:lvlText} wants it: the glyph alone.
     *
     * <p>A marker's own value carries the separating space the text path needed, because
     * there it was concatenated straight onto the item. Word puts the gap there itself
     * from the level's indent, so the space would be drawn twice.</p>
     */
    private static String levelText(com.demcha.compose.document.node.ListMarker marker) {
        return marker.value().strip();
    }

    /**
     * Semantic list mapping: each item becomes a marker-prefixed paragraph in
     * the list's text style. Flat items run through the same
     * {@code ListMarker.normalizeItemText} step as fixed-layout rendering
     * (author-typed markers stripped, blank items skipped); nested items
     * indent two spaces per depth and use their own marker when one is set,
     * falling back to {@code ListMarker.defaultForDepth} otherwise.
     */
    private void writeList(XWPFDocument document,
                           com.demcha.compose.document.node.ListNode list) {
        BigInteger numId = numberingFor(document, list);
        // The list's own box, and the space it puts between its items. A list is not a Word
        // object either — its items are paragraphs written where it stood — so its edges go
        // where every other block's go, and itemSpacing becomes the gap above each item
        // after the first.
        carriedSpacingBefore += list.margin().top() + list.padding().top();
        double previousItemSpacing = pendingItemSpacing;
        pendingItemSpacing = list.itemSpacing();
        boolean previousItemWritten = anItemWasWritten;
        anItemWasWritten = false;
        try {
            writeListItems(document, list, numId);
        } finally {
            pendingItemSpacing = previousItemSpacing;
            anItemWasWritten = previousItemWritten;
        }
        owePendingSpacingAfter(list.margin().bottom() + list.padding().bottom());
    }

    /** The gap above the next item, which is nothing at all above the first. */
    private void spaceBeforeTheNextItem() {
        if (anItemWasWritten) {
            owePendingSpacingAfter(pendingItemSpacing);
        }
        anItemWasWritten = true;
    }

    private void writeListItems(XWPFDocument document,
                                com.demcha.compose.document.node.ListNode list,
                                BigInteger numId) {
        for (String item : list.items()) {
            // Same normalization as the fixed-layout pipeline: strip an
            // author-typed leading marker and skip items with no content.
            String normalized = com.demcha.compose.document.node.ListMarker
                    .normalizeItemText(item, list.normalizeMarkers());
            if (normalized.isBlank()) {
                continue;
            }
            java.util.OptionalDouble lineHeight = layout.lineHeight(list);
            if (list.marker().isRich()) {
                // A drawn marker's pieces are runs, so the row is written the way
                // any row with runs in it is; its item is still just a label.
                writeRichListLine(document, list.textStyle(), list.marker(),
                        com.demcha.compose.document.node.ListItem.of(normalized), 0, lineHeight, layout.firstLine(list),
                        layout.pathOf(list));
            } else if (numId != null) {
                // Word draws the marker, so the text is the item and nothing else.
                writeListLine(document, list.textStyle(), normalized, 0, numId, lineHeight);
            } else {
                writeListLine(document, list.textStyle(),
                        list.marker().prefix() + normalized, 0, null, lineHeight);
            }
        }
        for (com.demcha.compose.document.node.ListItem item : list.nestedItems()) {
            writeNestedItem(document, list, item, 0, numId);
        }
    }

    private void writeNestedItem(XWPFDocument document,
                                 com.demcha.compose.document.node.ListNode list,
                                 com.demcha.compose.document.node.ListItem item,
                                 int depth,
                                 BigInteger numId) {
        // prefix() carries its own trailing space (and is empty for
        // markerless lists). Items without an explicit (or markerFor-baked)
        // marker fall back to the same depth cascade the fixed-layout
        // pipeline uses — never to the flat-list marker.
        com.demcha.compose.document.node.ListMarker marker =
                item.marker() != null
                        ? item.marker()
                        : com.demcha.compose.document.node.ListMarker.defaultForDepth(depth);
        java.util.OptionalDouble lineHeight = layout.lineHeight(list);
        if (item.isRich() || marker.isRich()) {
            writeRichListLine(document, list.textStyle(), marker, item, depth, lineHeight, layout.firstLine(list),
                    layout.pathOf(list));
        } else if (numId != null) {
            writeListLine(document, list.textStyle(), item.label(), depth, numId, lineHeight);
        } else {
            writeListLine(document, list.textStyle(), marker.prefix() + item.label(), depth, null,
                    lineHeight);
        }
        for (com.demcha.compose.document.node.ListItem child : item.children()) {
            writeNestedItem(document, list, child, depth + 1, numId);
        }
    }

    /**
     * Writes one item, either as a real Word list paragraph or as the marker-prefixed
     * text the export used before Word numbering existed here.
     *
     * @param numId the list definition to attach, or {@code null} to write the marker
     *              and the nesting indent as characters
     */
    private void writeListLine(XWPFDocument document, DocumentTextStyle style,
                               String text, int depth, BigInteger numId,
                               java.util.OptionalDouble lineHeight) {
        spaceBeforeTheNextItem();
        XWPFParagraph para = newBodyParagraph(document);
        applyLineHeight(para, lineHeight);
        if (numId != null) {
            para.setNumID(numId);
            para.setNumILvl(BigInteger.valueOf(depth));
            indentListItemInside(para, depth);
        }
        XWPFRun run = para.createRun();
        applyStyle(run, style);
        run.setText(numId != null ? text : "  ".repeat(depth) + text);
    }

    /**
     * Keeps a numbered item's own indent when it sits inside a padded container.
     *
     * <p>A paragraph's own {@code w:ind} replaces its numbering level's, so the container's
     * inset written on it would drop the level's hanging indent and the marker would run into
     * the text. The level's indent is added back on top of the inset.</p>
     */
    private void indentListItemInside(XWPFParagraph para, int depth) {
        CTPPr properties = para.getCTP().getPPr();
        if (properties == null) {
            return;
        }
        if (properties.isSetInd()) {
            long levelLeft = (long) LIST_HANGING_TWIPS + (long) LIST_NESTING_STEP_TWIPS * depth;
            CTInd indent = properties.getInd();
            indent.setLeft(BigInteger.valueOf(toTwips(insetLeft) + levelLeft));
            indent.setHanging(BigInteger.valueOf(LIST_HANGING_TWIPS));
        }
    }

    /**
     * Writes an item whose marker or whose content is made of inline runs as one
     * Word run per inline run, the way {@link #writeParagraphRuns} writes a rich
     * paragraph.
     *
     * <p>This is the part of the opt-in marker/content list the semantic export
     * can reproduce, and it reproduces it exactly. The geometry — the measured
     * marker column, the gap, the shared content origin — is unavailable here for
     * the reason {@code hangingIndent} documents. Which piece of an item is bold
     * is not geometry: Word carries a style per run inside a paragraph, so
     * writing the item's plain reading in one face would be dropping something
     * Word can hold.</p>
     *
     * <p>The same is true of a marker. A marker written as text keeps the colour
     * and face it was given, because those are run properties Word has. A marker
     * that draws a disc or an icon is written as the picture it draws, the way a
     * shape or an icon in a line is, rather than as a glyph the author did not ask
     * for — and is followed by the same space as a text marker.</p>
     */
    private void writeRichListLine(XWPFDocument document, DocumentTextStyle style,
                                   com.demcha.compose.document.node.ListMarker marker,
                                   com.demcha.compose.document.node.ListItem item,
                                   int depth,
                                   java.util.OptionalDouble lineHeight,
                                   java.util.Optional<com.demcha.compose.document.layout.payloads.ParagraphLine> line,
                                   String path) {
        warnDroppedInlineRuns(marker.runs(), path);
        warnDroppedInlineRuns(item.runs(), path);
        line = line.map(listLine -> itemLine(listLine, item.runs()));
        spaceBeforeTheNextItem();
        XWPFParagraph para = newBodyParagraph(document);
        applyLineHeight(para, lineHeight);
        XWPFRun leading = para.createRun();
        applyStyle(leading, style);
        leading.setText("  ".repeat(depth) + (marker.isRich() ? "" : marker.prefix()));
        PictureReach pictures = PictureReach.NONE;
        if (marker.isRich()) {
            pictures = writeInlineTextRuns(para, style, marker.runs(), path, line);
            // The gap after a marker is markerGap, which is geometry and so not
            // available here; a space is what separates a marker from its item on
            // the text path, and it separates them here for the same reason.
            // A marker that drew a picture separates the same way; one whose picture had no
            // data drew nothing, and gets no space either.
            if (!com.demcha.compose.document.node.InlineRun.plainText(marker.runs()).isBlank()
                || pictures.reach() > 0) {
                XWPFRun gap = para.createRun();
                applyStyle(gap, style);
                gap.setText(" ");
            }
        }
        if (item.isRich()) {
            pictures = pictures.max(writeInlineTextRuns(para, style, item.runs(), path, line));
        } else {
            XWPFRun label = para.createRun();
            applyStyle(label, style);
            label.setText(item.label());
        }
        makeRoomForPictures(para, pictures);
    }

    /**
     * Appends one Word run per text-carrying inline run, each in its own style
     * and falling back to {@code style} when it has none — and, for a chip, on the
     * fill it was given: a badge inside a list item is a badge for the same reason
     * it is one inside a paragraph. A picture or an icon among them is placed as it is in a
     * paragraph, from the list's measure of its line.
     *
     * @return how far the pictures written reach, or {@link PictureReach#NONE}
     */
    private PictureReach writeInlineTextRuns(XWPFParagraph para, DocumentTextStyle style,
                                       List<InlineRun> runs, String path,
                                       java.util.Optional<com.demcha.compose.document.layout.payloads.ParagraphLine> line) {
        PictureReach pictures = PictureReach.NONE;
        for (InlineRun run : runs) {
            InlineTextRun text = textOf(run);
            if (text == null) {
                pictures = pictures.max(writeInlinePicture(para, run, null, path, line));
                continue;
            }
            // A list item's run can carry a link, and it used to be written as plain text:
            // the paragraph path learned links and this one did not, so the same phrase was
            // a link in a sentence and dead text in a bullet.
            XWPFRun docRun = newRun(para, text.linkTarget());
            applyStyle(docRun, text.textStyle() == null ? style : text.textStyle());
            applyInlineBackground(docRun, backgroundOf(run), path);
            docRun.setText(text.text() == null ? "" : text.text());
        }
        return pictures;
    }

    /**
     * Semantic chart fallback: the semantic export has no layout pass, so the
     * chart's compiled vector geometry is unavailable here. The chart's
     * <em>semantic</em> content is its data, so the fallback writes a
     * categories-by-series table (values formatted with the chart's own axis
     * format). Authors who need the rendered chart must use the PDF
     * fixed-layout backend, where charts compile into ordinary primitives.
     */
    private void writeChartFallback(XWPFDocument document, ChartNode node) throws Exception {
        report.add(DocxExportReport.Severity.APPROXIMATED, "chart", layout.pathOf(node),
                "exported as its data table — a categories-by-series table in the chart's own "
                + "value format — because the drawn chart is layout geometry");
        if (chartWarned.compareAndSet(false, true)) {
            LOG.warn("docx.export.chart-fallback kind={} — the semantic DOCX export has no "
                    + "layout pass, so charts are exported as their data table. "
                    + "(One warning per export; use the PDF backend for the rendered chart.)",
                    node.spec().getClass().getSimpleName());
        }
        ChartData data = node.spec().data();
        NumberFormatSpec format = node.spec().valueFormat();

        TableBuilder table = new TableBuilder()
                .name(node.name().isEmpty() ? "ChartData" : node.name() + "Data")
                .autoColumns(data.seriesCount() + 1);
        String[] header = new String[data.seriesCount() + 1];
        header[0] = "";
        for (int s = 0; s < data.seriesCount(); s++) {
            header[s + 1] = data.series().get(s).name();
        }
        table.headerRow(header);
        for (int c = 0; c < data.categoryCount(); c++) {
            String[] row = new String[data.seriesCount() + 1];
            row[0] = data.categories().get(c);
            for (int s = 0; s < data.seriesCount(); s++) {
                Double v = data.series().get(s).values().get(c);
                row[s + 1] = v == null ? "" : format.format(v);
            }
            table.row(row);
        }
        writeTable(document, table.build());
    }

    /**
     * Writes a wrapper's children — inside a one-cell table when it paints a panel.
     *
     * <p>A container with no fill and no border is not a Word object: its children are
     * written where it stood, held in by its sides (see {@link #writeContainerBody}).</p>
     *
     * <p>A container that paints — a card, a callout, an outlined box — is written as a
     * table of one cell, which is how a panel is built in Word by hand. Word has no element
     * that wraps a run of paragraphs, and painting each paragraph instead left the panel in
     * pieces: measured in LibreOffice, the accent bar broke beside every row and table inside
     * the card, the band had white gaps where the space between blocks sat and none under a
     * table, and the padding above and below lay outside it. A cell holds all of it: its
     * shading is the fill behind everything inside, its borders are the card's edges at the
     * card's full height, and its margins are the padding on all four sides. What is inside
     * is written by the same writers that write a composed table cell, so it stays paragraphs,
     * lists, rows and tables a reader edits as usual.</p>
     *
     * <p>What does not survive is the corner radius, since a cell is rectangular; it is
     * warned about once per export rather than pretended away.</p>
     */
    private void writeContainerChildren(XWPFDocument document, DocumentNode node) throws Exception {
        ContainerPaint paint = paintOf(node);
        if (paint.isEmpty()) {
            writeContainerBody(document, node);
            return;
        }
        warnContainerRadiusDropped(node);
        writePanel(document, node, paint);
    }

    /**
     * Writes a painting container as a one-cell table; see {@link #writeContainerChildren}.
     *
     * <p>The geometry is the page's, translated. The page centres a panel's border on its
     * edge and measures the padding from the edge; the editor keeps a cell's border inside
     * the cell and starts the cell's margin after it. So each margin is the padding less half
     * that side's border, and the table is wider than the panel by half of each side border:
     * the text then lands the padding in from the panel's edge, and the border straddles the
     * edge, as on the page. Measured in LibreOffice against the engine's render, the band, the
     * accent bar and the text of a card with a 3pt accent land on the page's pixels.</p>
     *
     * <p>The table sits where the container's margin box starts, the enclosing insets and its
     * own left margin in. A table in the body is placed by its first cell's text, so its
     * {@code w:tblInd} is where the text starts; a table nested in a cell is placed by its
     * outer edge, so there it is where the border's outer edge is. Measured, a nested panel
     * indented like a body one sat its whole padding right of the page. Its top and bottom
     * margins are the space around the table, owed like any block's. A container kept
     * together that the layout held on one page is a row that may not break.</p>
     *
     * <p>Word breaks no page inside a table cell, so a page break among the panel's children
     * closes the panel there and opens it again after the break, on the next page — the way
     * the page draws a card a break runs through. Inside a cell there is no page to break,
     * and the panel is written whole.</p>
     */
    private void writePanel(XWPFDocument document, DocumentNode node, ContainerPaint paint) throws Exception {
        List<List<DocumentNode>> pieces = new ArrayList<>();
        pieces.add(new ArrayList<>());
        for (DocumentNode child : node.children()) {
            if (child instanceof PageBreakNode && currentCell == null) {
                pieces.add(new ArrayList<>());
            } else {
                pieces.get(pieces.size() - 1).add(child);
            }
        }
        for (int index = 0; index < pieces.size(); index++) {
            if (index > 0) {
                writePageBreak(document);
            }
            writePanelPiece(document, node, paint, pieces.get(index), index == 0, index == pieces.size() - 1);
        }
    }

    /** Writes one table of a panel: the whole panel, or the part of it between page breaks. */
    private void writePanelPiece(XWPFDocument document, DocumentNode node, ContainerPaint paint,
                                 List<DocumentNode> children, boolean first, boolean last) throws Exception {
        DocumentInsets margin = node.margin();
        DocumentInsets padding = node.padding();
        DocumentBorders borders = paint.borders() == null ? DocumentBorders.NONE : paint.borders();
        double halfLeft = strokeWidth(borders.left()) / 2;
        double halfRight = strokeWidth(borders.right()) / 2;
        // Whatever edge an enclosing container is still holding above its first paragraph is
        // space above this table too, and a table carries no space above itself.
        owePendingSpacingAfter(carriedSpacingBefore + (first ? margin.top() : 0));
        carriedSpacingBefore = 0;
        holdTheSpaceAboveATable(document);
        double width = panelWidth(node);

        XWPFTable table = newTable(document, 1, 1);
        hideTableGrid(table);
        XWPFTableCell cell = table.getRow(0).getCell(0);
        if (Double.isFinite(width) && width > 0) {
            double outer = width + halfLeft + halfRight;
            setTableWidth(table, outer);
            writeGrid(table, new double[]{outer});
            CTTcPr properties = cellProperties(cell);
            CTTblWidth cellWidth = properties.isSetTcW() ? properties.getTcW() : properties.addNewTcW();
            cellWidth.setType(STTblWidth.DXA);
            cellWidth.setW(BigInteger.valueOf(toTwips(outer)));
        }
        applyCellPaint(cell, paint.fill(), null);
        paintCellSides(cell, paint.borders());
        applyCellPadding(cell, insideTheBorders(padding, borders));
        if (node.keepTogether() && layout.onOnePage(node)) {
            table.getRow(0).setCantSplitRow(true);
        }

        cell.removeParagraph(0);
        DocumentColor outerSurface = surfaceBehind;
        double outerCellWidth = currentCellWidth;
        if (paint.fill() != null) {
            surfaceBehind = paint.fill();
        }
        currentCellWidth = Double.isFinite(width) ? width - padding.left() - padding.right() : Double.NaN;
        try {
            writeCellNodes(cell, children);
        } finally {
            surfaceBehind = outerSurface;
            currentCellWidth = outerCellWidth;
        }
        if (cell.getParagraphs().isEmpty()) {
            // A panel with nothing Word can hold inside is its padding tall on the page, not a
            // line of text taller.
            holdToHairline(cell.addParagraph());
        }

        double edge = insetLeft + margin.left();
        double indent = currentCell == null ? edge + padding.left() - halfLeft : edge - halfLeft;
        if (indent != 0) {
            CTTblPr tableProperties = table.getCTTbl().getTblPr();
            CTTblWidth tableIndent = tableProperties.isSetTblInd()
                    ? tableProperties.getTblInd()
                    : tableProperties.addNewTblInd();
            tableIndent.setType(STTblWidth.DXA);
            // Signed: a nested panel with no margin starts half its border left of the cell.
            tableIndent.setW(BigInteger.valueOf(Math.round(indent * POINT_TO_TWIP)));
        }
        if (last) {
            owePendingSpacingAfter(margin.bottom());
        }
    }

    /**
     * Gives the space owed above a table somewhere to go when nothing above it can hold it.
     *
     * <p>Word has no space above a table, so the paragraph before it carries it — and at the
     * top of the document or of a cell there is none, and the space was lost: a card's top
     * margin, or the padding of a section it opens. A paragraph a tenth of a point tall holds
     * it instead. After a table the separator between the two already does.</p>
     */
    private void holdTheSpaceAboveATable(XWPFDocument document) {
        boolean tableAbove = currentCell == null
                ? endsWithATable(document.getBodyElements())
                : cellEndsWithItsTableCloser();
        if (pendingSpacingAfter > 0 && lastBodyParagraph == null && !tableAbove) {
            holdToHairline(newBodyParagraph(document));
        }
    }

    /**
     * How wide a panel is: as the layout placed it, or else what is left where it is written,
     * less its own side margins — or unknown, left to the editor, where neither is known: no
     * canvas, or a cell whose width this export did not write.
     */
    private double panelWidth(DocumentNode node) {
        boolean known = currentCell != null ? Double.isFinite(currentCellWidth) : contentWidth < Double.MAX_VALUE;
        double available = known
                ? availableWidth() - node.margin().left() - node.margin().right()
                : Double.NaN;
        java.util.OptionalDouble placed = layout.placedWidth(node);
        if (placed.isEmpty()) {
            return available;
        }
        // A panel sized round its content is exactly as wide as its longest line, which an
        // editor setting the text in its own face can push onto a second line; it gets the
        // slack an auto column gets, where there is room for it.
        double room = Double.isFinite(available) ? available - placed.getAsDouble() : EDITOR_COLUMN_SLACK_POINTS;
        return placed.getAsDouble() + Math.max(0, Math.min(EDITOR_COLUMN_SLACK_POINTS, room));
    }

    /**
     * A panel's padding as a cell's margins: less half the border on each side, the half that
     * lies inside the panel on the page and inside the cell's margin in the editor (see
     * {@link #writePanel}).
     */
    private static DocumentInsets insideTheBorders(DocumentInsets padding, DocumentBorders borders) {
        return new DocumentInsets(
                Math.max(0, padding.top() - strokeWidth(borders.top()) / 2),
                Math.max(0, padding.right() - strokeWidth(borders.right()) / 2),
                Math.max(0, padding.bottom() - strokeWidth(borders.bottom()) / 2),
                Math.max(0, padding.left() - strokeWidth(borders.left()) / 2));
    }

    private static double strokeWidth(DocumentStroke stroke) {
        return stroke == null ? 0 : Math.max(0, stroke.width());
    }

    /**
     * Writes a panel's borders on its cell, side by side; a side the panel does not draw is
     * stated as none, so the cell carries no border the page does not show.
     */
    private static void paintCellSides(XWPFTableCell cell, DocumentBorders borders) {
        CTTcPr properties = cellProperties(cell);
        CTTcBorders edges = properties.isSetTcBorders() ? properties.getTcBorders() : properties.addNewTcBorders();
        DocumentBorders sides = borders == null ? DocumentBorders.NONE : borders;
        paintCellSide(edges.isSetTop() ? edges.getTop() : edges.addNewTop(), sides.top());
        paintCellSide(edges.isSetBottom() ? edges.getBottom() : edges.addNewBottom(), sides.bottom());
        paintCellSide(edges.isSetLeft() ? edges.getLeft() : edges.addNewLeft(), sides.left());
        paintCellSide(edges.isSetRight() ? edges.getRight() : edges.addNewRight(), sides.right());
    }

    private static void paintCellSide(CTBorder edge, DocumentStroke stroke) {
        if (stroke == null || stroke.width() <= 0) {
            paintEdge(edge, STBorder.NIL, null, null);
            return;
        }
        // w:sz counts eighths of a point, rounded to at least one so a hairline the author
        // asked for stays a line rather than vanishing.
        paintEdge(edge, STBorder.SINGLE, BigInteger.valueOf(Math.max(1, Math.round(stroke.width() * 8.0))),
                toHexColor(stroke.color().color()));
    }

    /**
     * Writes a container's children, carrying the container's own vertical box with them.
     *
     * <p>A container is not a Word object — its children are written where it stood — so
     * the space it holds above and below itself had nowhere to go and was dropped. Word has
     * that space on a paragraph and only on a paragraph, so the top goes to the first
     * paragraph written inside and the bottom joins the space owed below the container,
     * which the next paragraph writes above itself (see {@link #owePendingSpacingAfter}).</p>
     *
     * <p>Both are added to whatever that paragraph asks for itself, and both survive
     * nesting: a card inside a section hands its top to the same first paragraph, which
     * ends up carrying the sum — the same sum the page shows.</p>
     *
     * <p>A container that begins with a table keeps that edge unwritten. Word has no
     * space-before on a table, and the alternatives — an empty paragraph, a floating
     * table's {@code w:tblpPr} — either add a line the document never asked for or move the
     * table out of the flow it is in. The edge below it is not lost the same way: the gap
     * under a table is the space above whatever follows, and that is a paragraph.</p>
     */
    private void writeContainerBody(XWPFDocument document, DocumentNode node) throws Exception {
        carriedSpacingBefore += node.margin().top() + node.padding().top();
        // The sides are carried the same way, as the indent of every paragraph inside: a
        // container's content starts inside its margin and its padding on the page, and was
        // written flush with the page margin, the card's text touching the card's edge.
        double outerLeft = insetLeft;
        double outerRight = insetRight;
        insetLeft += node.margin().left() + node.padding().left();
        insetRight += node.margin().right() + node.padding().right();
        try {
            for (DocumentNode child : node.children()) {
                writeNode(document, child);
            }
        } finally {
            insetLeft = outerLeft;
            insetRight = outerRight;
        }
        owePendingSpacingAfter(node.margin().bottom() + node.padding().bottom());
        // Nothing inside took the top edge — a container of tables, or an empty one — so it
        // is not left waiting to land on whatever paragraph comes next.
        carriedSpacingBefore = 0;
    }

    private static boolean hasRadius(com.demcha.compose.document.style.DocumentCornerRadius radius) {
        return radius != null && !radius.isZero();
    }

    /**
     * Gives the package a styles part naming the document''s own body text as Normal.
     *
     * <p>Without one Word invents a latent Normal that no run refers to, so a reader who
     * restyles the document changes nothing: every run carries its own size and font, and a
     * direct property beats a style. Writing the part and leaving those runs silent is what
     * makes "change the Normal style" behave the way a Word user expects.</p>
     *
     * <p>Nothing is written when the graph carries no text to take a default from.</p>
     */
    private void writeStylesPart(XWPFDocument document) {
        DocumentTextStyle defaults = documentDefaultStyle;
        if (defaults == null) {
            return;
        }
        CTStyles styles = CTStyles.Factory.newInstance();
        applyDefaultRunProperties(styles.addNewDocDefaults().addNewRPrDefault().addNewRPr(), defaults);

        CTStyle normal = styles.addNewStyle();
        normal.setType(STStyleType.PARAGRAPH);
        normal.setStyleId(NORMAL_STYLE_ID);
        normal.setDefault(true);
        normal.addNewName().setVal(NORMAL_STYLE_ID);
        applyDefaultRunProperties(normal.addNewRPr(), defaults);

        headingLevels.stream().sorted().forEach(level -> writeHeadingStyle(styles, level));

        document.createStyles().setStyles(styles);
    }

    /**
     * Defines one of Word's heading styles, as a role and nothing else.
     *
     * <p>The style carries an outline level and no formatting at all. That is the point: a
     * heading in this export is a <em>statement about structure</em>, made by the document
     * when it asked for an outline entry, and the paragraph already carries the look its
     * author gave it. A heading style that also set a font and a size would restyle every
     * heading in the document on the way out — the export would be redesigning the page
     * rather than describing it.</p>
     *
     * <p>Word recognises its built-in headings by the pair: the id {@code HeadingN} and the
     * name {@code heading N}. Written with only one of them, the style is a custom style
     * that happens to be called Heading, the Navigation Pane stays empty, and "promote to
     * heading 2" in Word does something else.</p>
     *
     * @param styles the styles part being built
     * @param level  the zero-based outline level, as the document states it
     */
    private static void writeHeadingStyle(CTStyles styles, int level) {
        int ordinal = level + 1;
        CTStyle heading = styles.addNewStyle();
        heading.setType(STStyleType.PARAGRAPH);
        heading.setStyleId("Heading" + ordinal);
        heading.addNewName().setVal("heading " + ordinal);
        heading.addNewBasedOn().setVal(NORMAL_STYLE_ID);
        heading.addNewQFormat();
        heading.addNewPPr().addNewOutlineLvl().setVal(BigInteger.valueOf(level));
    }

    private void applyDefaultRunProperties(CTRPr properties, DocumentTextStyle defaults) {
        if (defaults.fontName() != null) {
            // All four slots, exactly as XWPFRun.setFontFamily writes them on a run.
            // w:ascii alone covers only ASCII: High-ANSI characters read w:hAnsi, Hebrew
            // and Arabic read w:cs, CJK reads w:eastAsia. Naming one and suppressing the
            // run's own rFonts would send every accented letter and every complex script
            // to Word's theme font while the rest of the line kept the asked-for family.
            String family = wordFamilyOf(defaults.fontName());
            CTFonts fonts = properties.addNewRFonts();
            fonts.setAscii(family);
            fonts.setHAnsi(family);
            fonts.setCs(family);
            fonts.setEastAsia(family);
        }
        if (defaults.size() > 0) {
            // w:sz counts half-points, and w:szCs carries the same for complex scripts.
            BigInteger halfPoints = BigInteger.valueOf(Math.round(defaults.size() * 2));
            properties.addNewSz().setVal(halfPoints);
            properties.addNewSzCs().setVal(halfPoints);
        }
        if (defaults.color() != null) {
            properties.addNewColor().setVal(toHexColor(defaults.color().color()));
        }
    }

    /**
     * The family name to write for a style's font, as Word understands families.
     *
     * <p>A {@link FontName} can name a face rather than a family — {@code Helvetica-Bold}
     * is one — and the two are not interchangeable here. Word resolves a family and takes
     * the weight from {@code w:b}; asked for a family called "Helvetica-Bold" it finds
     * none and substitutes, which is how a document that named its headings by face came
     * out set in something else entirely.</p>
     *
     * <p>The face is resolved to its family exactly as the layout resolves it, through
     * {@link FontLibrary#resolveFamily(FontName)}, so both renders are set in the same
     * family. The weight is deliberately <em>not</em> taken from the face name: the engine
     * does not take it either — a style naming {@code Helvetica-Bold} with no decoration
     * lays out regular — and writing {@code w:b} here would make Word bolder than the page
     * it is meant to match.</p>
     *
     * <p>The name itself comes from the family's own {@code wordFamily()}, which is what
     * that field is for, so a registered family can carry a Word name that differs from
     * its logical one.</p>
     *
     * @param fontName the style's font, possibly null or a face alias
     * @return the family name to write, or null when the style named no font
     */
    private String wordFamilyOf(FontName fontName) {
        if (fontName == null) {
            return null;
        }
        FontName family = FontLibrary.resolveFamily(fontName);
        FontFamilyDefinition definition = wordFamilies.get(family);
        return definition == null ? family.name() : definition.wordFamily();
    }

    /** Word has nine heading levels; a document asking for a tenth is clamped to the ninth. */
    private static final int MAX_HEADING_LEVEL = 8;

    /**
     * Gives a paragraph the heading role the document asked for, and only the role.
     *
     * <p>Word builds its Navigation Pane, its table of contents and its outline view from
     * heading <em>styles</em>, not from bookmarks — so an export that wrote a bookmark and
     * stopped there produced a document that could be jumped to by name and had no
     * structure at all to move around in.</p>
     *
     * <p>The role comes from the outline level the document stated when it declared the
     * bookmark. It is never inferred from how the paragraph looks: a heading guessed from
     * font size turns a large first line into a chapter and a small real heading into body
     * text, and both are wrong in a document a person then edits.</p>
     *
     * <p>The paragraph keeps its own formatting. The style it points at carries an outline
     * level and nothing else, so what changes is what Word knows about the paragraph, not
     * how it is drawn.</p>
     */
    private void applyHeadingRole(XWPFParagraph para, ParagraphNode node) {
        Integer level = headingLevelOf(node);
        if (level == null) {
            return;
        }
        CTPPr properties = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        CTString style = properties.isSetPStyle() ? properties.getPStyle() : properties.addNewPStyle();
        style.setVal("Heading" + (level + 1));
    }

    /**
     * The outline levels this document actually asks for.
     *
     * <p>Collected before the styles part is written, because that part comes first in the
     * package and a style a paragraph refers to has to exist. Only the levels in use are
     * defined: nine heading styles in a document with two headings is nine entries in
     * Word's style gallery that nothing in the document uses.</p>
     *
     * <p>A heading is read from what the document <em>states</em> — the outline level it
     * asked for when it declared a bookmark — and never inferred from how big the text is.
     * A large paragraph is a large paragraph; a document that never asked for an outline
     * does not get one invented from its typography.</p>
     */
    /**
     * Every anchor this export writes a bookmark for: a paragraph's, and a block's the export
     * writes (see {@link #blockAnchorOf}). A page reference is a live field only when its
     * anchor is one of these.
     */
    private static java.util.Set<String> bookmarkedAnchorsIn(DocumentGraph graph) {
        java.util.Set<String> anchors = new java.util.HashSet<>();
        // Each node with whether it lies in an overlay, which decides whether a rule is written.
        java.util.ArrayDeque<java.util.Map.Entry<DocumentNode, Boolean>> pending = new java.util.ArrayDeque<>();
        for (DocumentNode root : graph.roots()) {
            pending.push(java.util.Map.entry(root, false));
        }
        while (!pending.isEmpty()) {
            java.util.Map.Entry<DocumentNode, Boolean> next = pending.pop();
            DocumentNode node = next.getKey();
            boolean overlaid = next.getValue();
            String anchor = node instanceof ParagraphNode paragraph ? paragraph.anchor() : blockAnchorOf(node, !overlaid);
            if (anchor != null && !anchor.isBlank()) {
                anchors.add(anchor.trim());
            }
            boolean childrenOverlaid = overlaid || isOverlay(node);
            for (DocumentNode child : node.children()) {
                pending.push(java.util.Map.entry(child, childrenOverlaid));
            }
        }
        return anchors;
    }

    private static java.util.Set<Integer> headingLevelsIn(DocumentGraph graph) {
        java.util.Set<Integer> levels = new java.util.TreeSet<>();
        for (DocumentNode root : graph.roots()) {
            collectHeadingLevels(root, levels);
        }
        return levels;
    }

    private static void collectHeadingLevels(DocumentNode node, java.util.Set<Integer> levels) {
        if (node instanceof ParagraphNode paragraph) {
            Integer level = headingLevelOf(paragraph);
            if (level != null) {
                levels.add(level);
            }
        }
        for (DocumentNode child : node.children()) {
            collectHeadingLevels(child, levels);
        }
    }

    /** @return the paragraph's outline level, clamped to Word's nine, or null when it is not a heading */
    private static Integer headingLevelOf(ParagraphNode node) {
        DocumentBookmarkOptions bookmark = node.bookmarkOptions();
        return bookmark == null ? null : Math.min(bookmark.level(), MAX_HEADING_LEVEL);
    }

    /**
     * The text style the document is mostly written in.
     *
     * <p>Weighted by characters rather than by how many nodes use a style: headings are
     * numerous and short while body text is long, so counting nodes elects the heading
     * style as the document default and leaves every body run carrying a direct size.</p>
     *
     * @param graph the document being exported
     * @return the dominant style, or {@code null} when the graph carries no text
     */
    private static DocumentTextStyle dominantTextStyle(DocumentGraph graph) {
        java.util.Map<StyleKey, Long> weights = new java.util.HashMap<>();
        java.util.Map<StyleKey, DocumentTextStyle> byKey = new java.util.HashMap<>();
        for (DocumentNode root : graph.roots()) {
            weighTextStyles(root, weights, byKey);
        }
        return weights.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(entry -> byKey.get(entry.getKey()))
                .orElse(null);
    }

    private static void weighTextStyles(DocumentNode node,
                                        java.util.Map<StyleKey, Long> weights,
                                        java.util.Map<StyleKey, DocumentTextStyle> byKey) {
        if (node instanceof ParagraphNode paragraph && paragraph.textStyle() != null) {
            weigh(paragraph.textStyle(), textWeight(paragraph.text()), weights, byKey);
        } else if (node instanceof com.demcha.compose.document.node.ListNode list
                   && list.textStyle() != null) {
            long weight = list.items().stream().mapToLong(DocxSemanticBackend::textWeight).sum();
            weigh(list.textStyle(), weight, weights, byKey);
        }
        for (DocumentNode child : node.children()) {
            weighTextStyles(child, weights, byKey);
        }
    }

    private static void weigh(DocumentTextStyle style,
                              long weight,
                              java.util.Map<StyleKey, Long> weights,
                              java.util.Map<StyleKey, DocumentTextStyle> byKey) {
        StyleKey key = StyleKey.of(style);
        weights.merge(key, weight, Long::sum);
        byKey.putIfAbsent(key, style);
    }

    /**
     * What makes two text styles the same for the purpose of electing a document default.
     *
     * <p>{@code DocumentTextStyle} cannot be the key. It is a record, so its equality is
     * its components', and {@code DocumentColor} defines no {@code equals} — two colours
     * built from the same channels are unequal unless they are the same instance. Styles
     * built inline per paragraph, which is ordinary authoring, would each weigh alone and
     * the body's characters would never add up, electing whichever style happened to be
     * reused instead.</p>
     *
     * <p>The components are the three the styles part actually writes, compared as they
     * are written: the family by name, the size in half-points, the colour as packed
     * RGB.</p>
     *
     * @param fontName   the family, or {@code null} when the style names none
     * @param halfPoints the size as {@code w:sz} counts it
     * @param colour     packed RGB, or {@code null} when the style names no colour
     */
    private record StyleKey(FontName fontName, long halfPoints, Integer colour) {

        static StyleKey of(DocumentTextStyle style) {
            // By family, not by the name the style used: Helvetica and Helvetica-Bold are
            // written identically — the second resolves to the first and takes its weight
            // from the decoration — so weighing them apart would split one body style in
            // two and could elect the lighter half as Normal.
            return new StyleKey(FontLibrary.resolveFamily(style.fontName()),
                    Math.round(style.size() * HALF_POINTS_PER_POINT),
                    style.color() == null ? null : style.color().color().getRGB());
        }
    }

    /** At least one, so a style used only by empty text still counts as used. */
    private static long textWeight(String text) {
        return text == null ? 1L : Math.max(1L, text.length());
    }

    /** Reads the fill and borders off whichever wrapper kind this is, or an empty paint. */
    private static ContainerPaint paintOf(DocumentNode node) {
        if (node instanceof SectionNode section) {
            return new ContainerPaint(section.fillColor(),
                    bordersOf(section.borders(), section.stroke()));
        }
        if (node instanceof ContainerNode container) {
            return new ContainerPaint(container.fillColor(),
                    bordersOf(container.borders(), container.stroke()));
        }
        return new ContainerPaint(null, null);
    }

    /**
     * Per-side borders win; a uniform stroke stands in for all four when they are absent,
     * which is how the node model says "one outline round the whole box".
     */
    private static DocumentBorders bordersOf(DocumentBorders borders, DocumentStroke stroke) {
        if (borders != null && !DocumentBorders.NONE.equals(borders)) {
            return borders;
        }
        if (stroke != null && stroke.width() > 0) {
            return DocumentBorders.all(stroke);
        }
        return null;
    }

    /** One warning per export for the part of a container's design Word cannot hold. */
    private void warnContainerRadiusDropped(DocumentNode node) {
        boolean rounded = node instanceof SectionNode section
                ? hasRadius(section.cornerRadius())
                : node instanceof ContainerNode container && hasRadius(container.cornerRadius());
        if (rounded) {
            report.add(DocxExportReport.Severity.APPROXIMATED, "corner radius", layout.pathOf(node),
                    "a Word table cell is rectangular, so the panel keeps its fill and "
                    + "loses its rounded corners");
        }
        if (rounded && containerRadiusWarned.compareAndSet(false, true)) {
            LOG.warn("docx.export.container-radius-dropped node='{}' — a Word table cell "
                     + "is rectangular, so the panel renders with square corners. "
                     + "(One warning per export; use the PDF backend for the rounded form.)",
                    node.nodeKind());
        }
    }

    /**
     * Creates a body paragraph where content is being written — the body, or the cell being
     * filled — held in by the containers around it and carrying the space owed above it.
     */
    private XWPFParagraph newBodyParagraph(XWPFDocument document) {
        XWPFParagraph para;
        if (cellEndsWithItsTableCloser()) {
            // The paragraph closing the table above is where this one goes: a second one
            // would leave the closer as a gap the page does not have.
            para = tableCloser;
            para.getCTP().getPPr().getSpacing().unsetLineRule();
            para.getCTP().getPPr().getSpacing().unsetLine();
            tableCloser = null;
        } else {
            para = currentCell != null ? currentCell.addParagraph() : document.createParagraph();
        }
        applyInset(para);
        // Everything owed above this paragraph — the space the one before it holds below
        // itself, and any container edge — is written here, on one side of the gap.
        double above = carriedSpacingBefore + pendingSpacingAfter;
        if (above > 0) {
            addSpacing(para, above, 0);
        }
        carriedSpacingBefore = 0;
        pendingSpacingAfter = 0;
        lastBodyParagraph = para;
        return para;
    }

    /**
     * Holds the space below a paragraph until it is known what follows.
     *
     * <p>A gap between two blocks is one distance, and the exporter wrote it as two —
     * {@code w:after} on the block above and {@code w:before} on the one below — which is
     * only the same thing in an editor that adds them. LibreOffice takes the larger:
     * measured on the probe, a card holding 20pt below itself followed by a heading asking
     * for 16pt above rendered 20pt where the page shows 36, and the whole document below
     * it sat 16pt high.</p>
     *
     * <p>So the space is owed rather than written, and {@link #newBodyParagraph} pays it as
     * the next paragraph's {@code w:before} together with whatever that paragraph asks for
     * itself. One number on one side: an editor that adds and an editor that takes the
     * maximum then agree, because there is nothing to add it to.</p>
     *
     * <p>{@link #flushSpacingAfter} pays it the other way when what comes next is not a
     * paragraph — a table has no space above it in Word — or when nothing comes at all.</p>
     */
    private void owePendingSpacingAfter(double points) {
        if (points > 0) {
            pendingSpacingAfter += points;
        }
    }

    /** Writes the owed space onto the paragraph that owes it, for want of a later one. */
    private void flushSpacingAfter() {
        if (pendingSpacingAfter > 0 && lastBodyParagraph != null) {
            addSpacing(lastBodyParagraph, 0, pendingSpacingAfter);
        }
        pendingSpacingAfter = 0;
    }

    /**
     * Adds to the space above and below a paragraph, rather than replacing it.
     *
     * <p>Two things state it — the paragraph's own box, and the vertical edge of every
     * container it sits at the start or the end of — and on the page a reader sees their
     * sum. Setting it would mean whichever ran last silently won.</p>
     *
     * @param para   the Word paragraph
     * @param before points to add above, zero to leave it alone
     * @param after  points to add below, zero to leave it alone
     */
    private static void addSpacing(XWPFParagraph para, double before, double after) {
        CTPPr properties = para.getCTP().isSetPPr()
                ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        CTSpacing spacing = properties.isSetSpacing() ? properties.getSpacing() : properties.addNewSpacing();
        if (before > 0) {
            spacing.setBefore(BigInteger.valueOf(
                    twipsOf(spacing.isSetBefore() ? spacing.getBefore() : null) + toTwips(before)));
        }
        if (after > 0) {
            spacing.setAfter(BigInteger.valueOf(
                    twipsOf(spacing.isSetAfter() ? spacing.getAfter() : null) + toTwips(after)));
        }
    }

    /** Reads a twip measure back, treating an unset one as zero. */
    private static long twipsOf(Object measure) {
        Long twips = writtenTwips(measure);
        return twips == null ? 0 : twips;
    }

    /**
     * A twip value this export wrote, read back — or null when it is not a plain number.
     *
     * <p>XmlBeans hands a measure back as the schema's union, and a measure may legally be a
     * string with a unit ({@code 1in}) or a percentage. This export only ever writes plain
     * twips, which come back as a number; anything else is not one of its own values and is
     * reported as unknown rather than parsed and thrown on.</p>
     */
    private static Long writtenTwips(Object measure) {
        return measure instanceof Number number ? number.longValue() : null;
    }

    /**
     * The width content has where it is being written: the page's, or the cell's when a cell
     * of known width is being filled, less what the containers around it hold in from each
     * side. A picture or a row sized to the page's full width would run past the right margin
     * once the containers push it in, and past the edge of a card it sits in.
     */
    private double availableWidth() {
        double width = currentCell != null && Double.isFinite(currentCellWidth) ? currentCellWidth : contentWidth;
        return Double.isFinite(width) ? width - insetLeft - insetRight : width;
    }

    /**
     * Holds the paragraph in from the sides by every enclosing container's margin and padding.
     *
     * <p>Only the sides a container asked for are written, and none when no container asked,
     * so a paragraph outside any padded container is written as it always was.</p>
     *
     * <p>The sides are written as the page's. A right-to-left paragraph has them turned to
     * its flow when its direction is written, in {@link #applyDirection}.</p>
     */
    private void applyInset(XWPFParagraph para) {
        if (insetLeft <= 0 && insetRight <= 0) {
            return;
        }
        CTPPr properties = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        CTInd indent = properties.isSetInd() ? properties.getInd() : properties.addNewInd();
        if (insetLeft > 0) {
            indent.setLeft(BigInteger.valueOf(toTwips(insetLeft)));
        }
        if (insetRight > 0) {
            indent.setRight(BigInteger.valueOf(toTwips(insetRight)));
        }
    }

    private void writeShapeContainer(XWPFDocument document, ShapeContainerNode node) throws Exception {
        // POI/DOCX has no portable equivalent of a graphics-state path clip.
        // The fallback rule (recorded in docs/canonical-legacy-parity.md) is
        // to render the container's layers inline, in source order, without
        // the outline frame and without clipping. The resulting Word document
        // shows the layer content but not the shape boundary — authors who
        // need the boundary must export to PDF.
        report.add(DocxExportReport.Severity.APPROXIMATED, "clipped shape container",
                layout.pathOf(node),
                "DOCX has no graphics-state clip, so the layers are written inline, in source "
                + "order, without the outline and without being clipped to it");
        if (shapeContainerWarned.compareAndSet(false, true)) {
            LOG.warn("docx.export.shape-container-fallback "
                    + "outline='{}' clipPolicy={} — DOCX has no graphics-state clip; "
                    + "rendering layers inline without outline. "
                    + "(One warning per export; use the PDF backend for full fidelity.)",
                    node.outline().getClass().getSimpleName(),
                    node.clipPolicy());
        }
        for (DocumentNode child : node.children()) {
            writeNode(document, child);
        }
    }

    /**
     * Writes a page reference — a table of contents' page number, a "see page N" — as Word's
     * own {@code PAGEREF} field on the anchor's bookmark.
     *
     * <p>The export dropped the node, so a table of contents reached Word with its entries
     * and without a single page number. A number written as text would be right until the
     * reader edits the document; the field is the page Word counts, a hyperlink to it
     * ({@code \h}) as the entry's label already is. What it reads before an editor updates it
     * is the page the layout resolved, so the file opens showing the numbers the PDF does.</p>
     *
     * <p>A reference whose anchor this export writes no bookmark for is written as its text
     * alone: Word turns a {@code PAGEREF} to a missing bookmark into "Error! Bookmark not
     * defined." the first time the field updates, which is worse than a number that does not
     * move.</p>
     */
    private void writePageReference(XWPFDocument document,
                                    com.demcha.compose.document.node.PageReferenceNode node) {
        String shown = layout.laidOutText(node).orElse(node.placeholderText());
        // The layout lays a page reference out as this paragraph, so its properties are
        // written exactly as that paragraph's would be.
        ParagraphNode asLaidOut = new ParagraphNode(node.name(), shown, node.textStyle(), node.align(),
                0.0, node.padding(), node.margin());
        XWPFParagraph para = newBodyParagraph(document);
        applyParagraphProperties(para, asLaidOut);
        applyLineHeight(para, layout.lineHeight(node));
        String bookmark = bookmarkedAnchors.contains(node.anchor())
                ? bookmarkNames.nameFor(node.anchor())
                : null;
        XWPFRun run;
        if (bookmark == null) {
            run = para.createRun();
        } else {
            CTSimpleField field = para.getCTP().addNewFldSimple();
            field.setInstr(" PAGEREF " + bookmark + " \\h ");
            run = new XWPFRun(field.addNewR(), (IRunBody) para);
        }
        applyStyle(run, node.textStyle());
        run.setText(shown);
    }

    private void writeParagraph(XWPFDocument document, ParagraphNode node) {
        XWPFParagraph para = newBodyParagraph(document);
        boolean rightToLeft = applyParagraphProperties(para, node);
        applyHeadingRole(para, node);
        int anchor = openAnchor(para, node.anchor());
        writeParagraphRuns(para, node, rightToLeft);
        closeAnchor(para, anchor);
    }

    /**
     * Sets the paragraph's lines to the height the engine measured them at.
     *
     * <p>A line's height is the font's, and Word uses its own — measured against the
     * reference render, Word set a body line at 13.9pt and LibreOffice at 12.1 where the
     * document says 9.7. Over a page that difference is the largest single reason an
     * exported document stops matching: everything below the first paragraph sits lower
     * than it should, and the gap grows with every line.</p>
     *
     * <p>Written as {@code w:lineRule="exact"} rather than as a multiple: the number is a
     * measurement in points, and a multiple would be measured again by Word against
     * whichever font it substituted. Exact is also the only rule that can make a line
     * shorter than the font would like — which is the direction this always moves, since
     * the engine's line box is the face's ascent plus descent with no leading.</p>
     *
     * @param para   the Word paragraph
     * @param height the measured line height in points, empty when nothing measured it
     */
    private static void applyLineHeight(XWPFParagraph para, java.util.OptionalDouble height) {
        if (height.isEmpty()) {
            return;
        }
        CTPPr properties = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        CTSpacing spacing = properties.isSetSpacing() ? properties.getSpacing() : properties.addNewSpacing();
        spacing.setLineRule(STLineSpacingRule.EXACT);
        spacing.setLine(BigInteger.valueOf(Math.round(height.getAsDouble() * POINT_TO_TWIP)));
    }

    /**
     * Writes the properties a paragraph carries whatever it sits in.
     *
     * <p>One place on purpose. These were written where a paragraph is a document child
     * and not where it is a table cell's, which is how every right-to-left invoice line
     * came out undeclared; splitting them again would set the next property up for the
     * same fate. The measured line height is the next property, and it went the same way
     * once before landing here: written beside the call rather than inside it, it reached
     * the body and not the two columns of a row, whose paragraphs are a cell's.</p>
     *
     * <p>{@link TextDirection#AUTO} is resolved here rather than passed on. Leaving it
     * unwritten was leaving Word to guess, and Word guessing is the thing {@code w:bidi}
     * exists to prevent: an automatic paragraph that the page laid out right-to-left
     * reached Word with nothing saying so, and a line starting with a digit or a
     * parenthesis came out the other way round. The answer comes from the same resolver
     * the page used, so the two cannot part company — and because it is resolved once
     * here, the alignment mapping and the direction mark cannot disagree about it.</p>
     *
     * @param target the Word paragraph
     * @param source the node it was written from
     * @return whether the paragraph runs right to left, for its runs to declare too
     */
    private boolean applyParagraphProperties(XWPFParagraph target, ParagraphNode source) {
        boolean rightToLeft = ParagraphDirection.resolve(source) == TextDirection.RTL;
        target.setAlignment(toAlignment(source.align(), rightToLeft));
        applyDirection(target, rightToLeft);
        applyLineHeight(target, layout.lineHeight(source));
        applyVerticalSpacing(target, source);
        return rightToLeft;
    }

    /**
     * Carries the space a paragraph holds above and below itself.
     *
     * <p>A paragraph's own {@code margin} and {@code padding} are what separate one block
     * from the next, and none of it was written: every exported document ran its blocks
     * together and leaned on whatever Word puts between paragraphs instead. That was
     * invisible while the line height was Word's too — the lines were tall enough to stand
     * in for the gaps — and became the largest remaining difference the moment the lines
     * were right.</p>
     *
     * <p>Vertical space is one of the few pieces of a node's box Word holds natively, which
     * is why this is written and the horizontal half is not: {@code w:spacing} is the gap
     * above and below a paragraph, while the left and right insets of a shaded block have
     * no paragraph-level equivalent at all.</p>
     *
     * <p>Margin and padding are added together. They are different things to the engine —
     * one outside the box, one inside it — but Word has one gap, and a reader looking at
     * the page sees their sum.</p>
     *
     * <p>Only the space above is written here. The space below is owed until it is known
     * what follows it, so that one gap is written once rather than from both sides —
     * {@link #owePendingSpacingAfter} says why that matters.</p>
     */
    private void applyVerticalSpacing(XWPFParagraph target, DocumentNode source) {
        addSpacing(target, source.margin().top() + source.padding().top(), 0);
        owePendingSpacingAfter(source.margin().bottom() + source.padding().bottom());
    }

    /**
     * Marks a right-to-left paragraph so Word lays it out in that direction.
     *
     * <p>The text stays in logical order — unlike a fixed-layout backend, Word has its
     * own bidirectional engine and does the reordering and the Arabic joining itself.
     * What it cannot infer is the paragraph's base direction: without {@code w:bidi} a
     * line that starts with a neutral character, or one that mixes scripts, is laid out
     * as left-to-right text that happens to contain Hebrew.</p>
     *
     * <p>The direction reaches this method already resolved — {@link TextDirection#AUTO}
     * is settled by {@link #applyParagraphProperties}, so the mark and the alignment are
     * decided from one answer rather than two.</p>
     *
     * <p>The mark also changes what the paragraph's indents mean, so they are turned round
     * with it. Word and LibreOffice both read {@code w:ind}'s {@code left} and {@code right}
     * as the start and end of the flow, as they read {@code w:jc}: in a {@code w:bidi}
     * paragraph {@code left} is the right-hand side. The containers around the paragraph
     * hold it in by page sides, and {@link #applyInset} writes them as it finds them, so a
     * right-to-left paragraph in a section padded on the left came out pushed in from the
     * right — the text short of the edge the page runs it to, by exactly that padding, in
     * both editors. {@code w:start} and {@code w:end} are no way out: both editors read them
     * as {@code left} and {@code right}, measured. The sides are turned once, as the mark is
     * added, so every indent a paragraph carries has to be on it by then — the inset is,
     * since a body paragraph is created with it.</p>
     *
     * @param para        the Word paragraph
     * @param rightToLeft whether the page laid the paragraph out right to left
     */
    private static void applyDirection(XWPFParagraph para, boolean rightToLeft) {
        if (!rightToLeft) {
            return;
        }
        CTPPr properties = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        if (!properties.isSetBidi()) {
            properties.addNewBidi();
            if (properties.isSetInd()) {
                turnSidesToTheFlow(properties.getInd());
            }
        }
    }

    /**
     * Swaps an indent written by page side into the start and end of a right-to-left flow.
     *
     * <p>Only the two sides move. A first-line or hanging indent is measured from the start
     * of the flow already, and stays where it is.</p>
     */
    private static void turnSidesToTheFlow(CTInd indent) {
        Object left = indent.isSetLeft() ? indent.getLeft() : null;
        Object right = indent.isSetRight() ? indent.getRight() : null;
        if (right != null) {
            indent.setLeft(right);
        } else if (left != null) {
            indent.unsetLeft();
        }
        if (left != null) {
            indent.setRight(left);
        } else if (right != null) {
            indent.unsetRight();
        }
    }

    /**
     * Writes {@code node}'s text into {@code para}, one Word run per inline run.
     *
     * <p>A run's own style is used and the paragraph's is the fallback, which is the
     * contract {@link InlineTextRun} states: its style
     * "falls back to the paragraph style when null". Applying the fallback to every run
     * regardless is what flattened a bold segment, an accent-coloured segment and plain
     * text into one identical face.</p>
     *
     * <p>Runs win over {@code text} when both are present, matching how a paragraph is
     * rendered elsewhere. Nothing is lost by preferring them: when {@code text} is left
     * blank {@code ParagraphNode} fills it by concatenating exactly the runs that carry
     * text, highlight chips included. A paragraph whose only runs carry no text — an image,
     * a shape — still falls back to {@code text}, which is the whole of what it reads.</p>
     *
     * <p>The runs are walked as the document authored them rather than as the reduction to
     * text runs hands them back: the reduction answers what to write, and a chip is more
     * than its text. Its fill is read from the authored run beside the reduced one.</p>
     */
    private void writeParagraphRuns(XWPFParagraph para, ParagraphNode node, boolean rightToLeft) {
        warnDroppedInlineRuns(node);
        String path = layout.pathOf(node);
        java.util.Optional<com.demcha.compose.document.layout.payloads.ParagraphLine> line = layout.firstLine(node);
        boolean wroteARun = false;
        PictureReach pictures = PictureReach.NONE;
        for (InlineRun run : node.inlineRuns()) {
            InlineTextRun text = textOf(run);
            if (text == null) {
                PictureReach reach = writeInlinePicture(para, run, node.linkTarget(), path, line);
                if (reach != null) {
                    wroteARun = true;
                    pictures = pictures.max(reach);
                }
                continue;
            }
            // A run's own link wins over the paragraph's: a sentence with one linked phrase
            // in it is the ordinary case, and the paragraph's link is the fallback for the
            // rest of that sentence rather than something the phrase overrides away.
            DocumentLinkTarget target = text.linkTarget() != null ? text.linkTarget() : node.linkTarget();
            XWPFRun docRun = newRun(para, target);
            applyStyle(docRun, text.textStyle() == null ? node.textStyle() : text.textStyle());
            applyRunDirection(docRun, rightToLeft);
            applyInlineBackground(docRun, backgroundOf(run), path);
            docRun.setText(text.text() == null ? "" : text.text());
            wroteARun = true;
        }
        if (!wroteARun) {
            XWPFRun docRun = newRun(para, node.linkTarget());
            applyStyle(docRun, node.textStyle());
            applyRunDirection(docRun, rightToLeft);
            docRun.setText(node.text() == null ? "" : node.text());
        }
        makeRoomForPictures(para, pictures);
    }

    /**
     * Lets a line hold the pictures in it (see {@link PictureReach}).
     *
     * <p>A paragraph whose pictures stay within its text keeps its exact height. One holding a
     * picture that rises above the text is written with its lines <em>at least</em> the
     * height the picture reaches instead: the editor then grows the line to the picture rather
     * than clip it, whichever editor it is and wherever it puts its baseline, and where the
     * picture fits the line is the page's. What that costs: Word has one line height for a
     * paragraph, so every line of it is then at least the picture's reach, and otherwise the
     * editor's own measure of its text, which in LibreOffice is taller than the page's —
     * where the page makes only the line holding the picture taller. A paragraph written with
     * no exact height — a page zone's, one with no layout — grows to its pictures on its own
     * and is left alone.</p>
     */
    private static void makeRoomForPictures(XWPFParagraph para, PictureReach pictures) {
        CTPPr properties = para.getCTP().getPPr();
        if (pictures == null || !(pictures.reach() > 0) || properties == null || !properties.isSetSpacing()) {
            return;
        }
        CTSpacing spacing = properties.getSpacing();
        if (!spacing.isSetLineRule() || spacing.getLineRule() != STLineSpacingRule.EXACT) {
            return;
        }
        Long current = writtenTwips(spacing.getLine());
        long wanted = Math.round(pictures.reach() * POINT_TO_TWIP);
        long line = current == null ? wanted : Math.max(current, wanted);
        if (pictures.overText()) {
            spacing.setLineRule(STLineSpacingRule.AT_LEAST);
        }
        spacing.setLine(BigInteger.valueOf(line));
    }

    /**
     * Writes an inline picture or icon where it sits in the line, as a picture in its own run.
     *
     * <p>Both were dropped, so a contact line lost its phone and mail icons and a sentence its
     * emoji. A picture is written from its bytes; an SVG icon is drawn into a transparent
     * picture from the same layers the layout resolves ({@link
     * com.demcha.compose.document.layout.InlineSvgLayers}) by the raster the PPTX backend uses
     * ({@link com.demcha.compose.document.backend.fixed.pdf.handlers.InlineSvgRasters}), so it
     * looks as it does on the page; the text an icon stands for — an emoji's — is the picture's
     * description, and the report says it is a picture rather than a character. An inline
     * shape — a dot, an arrow, a checkbox — is drawn by the same raster ({@link
     * DocxShapePictures}), and is larger than its box by as far as its ink reaches past it
     * and a pixel, so it stands that much lower. A picture stands on the line's
     * baseline in Word, so it is raised or lowered by {@code w:position} to where the page's
     * alignment puts it, from the layout's measure of the line — except a picture this export
     * drew itself and the page raises, which carries the rise as transparent rows ({@link
     * DocxPictureLift}) because LibreOffice ignores {@code w:position} on a picture.</p>
     *
     * @return how far above the line's bottom the picture reaches — its height where the line
     *         is unmeasured — or {@code null} for a run this does not draw
     */
    private PictureReach writeInlinePicture(XWPFParagraph para, InlineRun run, DocumentLinkTarget fallbackLink,
                                      String path,
                                      java.util.Optional<com.demcha.compose.document.layout.payloads.ParagraphLine> line) {
        byte[] bytes;
        double width;
        double height;
        InlineImageAlignment alignment;
        double baselineOffset;
        DocumentLinkTarget link;
        String description = null;
        // The height of the box the page places, and how far below it the picture reaches — a
        // shape's stroke and frame — and so how much lower than that box's bottom it stands.
        double placedHeight;
        double below = 0;
        // The empty margin a drawn shape keeps around its ink.
        double frame = 0;
        if (run instanceof InlineShapeRun shape) {
            DocxShapePictures.Picture drawn = DocxShapePictures.of(shape);
            bytes = drawn.png();
            width = drawn.width();
            height = drawn.height();
            placedHeight = shape.height();
            below = drawn.below();
            frame = DocxShapePictures.EDGE;
            alignment = shape.alignment();
            baselineOffset = shape.baselineOffset();
            link = shape.linkTarget();
        } else if (run instanceof InlineImageRun image) {
            bytes = NodeDefinitionSupport.toImageData(image.imageData()).getBytes();
            width = image.width();
            height = image.height();
            placedHeight = height;
            alignment = image.alignment();
            baselineOffset = image.baselineOffset();
            link = image.linkTarget();
        } else if (run instanceof InlineSvgRun svg) {
            bytes = InlineSvgRasters.rasterize(InlineSvgLayers.of(svg.icon(), svg.width()),
                    svg.width(), svg.height()).getBytes();
            width = svg.width();
            height = svg.height();
            placedHeight = height;
            alignment = svg.alignment();
            baselineOffset = svg.baselineOffset();
            link = svg.linkTarget();
            description = svg.icon().text();
        } else {
            return null;
        }
        if (bytes.length == 0) {
            report.add(DocxExportReport.Severity.DROPPED, "inline image", path,
                    "the picture's data is empty, so there is nothing to write");
            return null;
        }
        // Where the picture's bottom stands, from the baseline: the page's placement of its box,
        // less what the picture reaches past it.
        double bottom = line.isEmpty() ? 0
                : inlineBottomFromBaseline(alignment, baselineOffset, placedHeight, line.get()) - below;
        if (bottom > 0 && !(run instanceof InlineImageRun)) {
            // A picture the page raises stands on the baseline in LibreOffice, which ignores
            // w:position on one. A picture this export drew carries the rise itself instead,
            // as transparent space below what it shows, so it stands where the page puts it
            // in either editor; an author's own picture is written as it was given.
            DocxPictureLift.Lifted lifted = DocxPictureLift.lift(bytes, height, bottom);
            bytes = lifted.png();
            height = lifted.height();
            bottom -= lifted.lift();
        }
        XWPFRun picture = newRun(para, link != null ? link : fallbackLink);
        try (InputStream stream = new java.io.ByteArrayInputStream(bytes)) {
            picture.addPicture(stream, pictureType(bytes), "inline",
                    Units.toEMU(width), Units.toEMU(height));
        } catch (Exception failure) {
            throw new IllegalStateException("could not write an inline picture", failure);
        }
        // POI describes a picture by the file name it is handed, which a screen reader then
        // reads out; the description is the text the icon stands for, or nothing.
        String alt = description == null ? "" : description;
        picture.getCTR().getDrawingArray(0).getInlineArray(0).getDocPr().setDescr(alt);
        picture.getEmbeddedPictures().get(0).getCTPicture().getNvPicPr().getCNvPr().setDescr(alt);
        if (description != null && !description.isBlank()) {
            report.add(DocxExportReport.Severity.APPROXIMATED, "inline icon", path,
                    "drawn as a picture, as on the page; the text it stands for (" + description
                    + ") is the picture's description rather than a character in the line");
        }
        if (line.isEmpty()) {
            return new PictureReach(height, false);
        }
        long raise = Math.round(bottom * 2);
        if (raise != 0) {
            CTRPr properties = picture.getCTR().isSetRPr() ? picture.getCTR().getRPr() : picture.getCTR().addNewRPr();
            properties.addNewPosition().setVal(BigInteger.valueOf(raise));
        }
        // The room the line owes is what the picture shows: a shape's transparent frame is not
        // ink, and counted as such it would take a shape that stays inside the text past it.
        return PictureReach.of(bottom, height, line.get(), frame);
    }

    /**
     * How far a line's pictures reach above its bottom, and whether any leaves its text.
     *
     * <p>A picture within the text's height sits inside the line as the page measured it and
     * needs nothing. One that rises above the text's ascent, or hangs below its descent, is
     * where an exact line clips: the editor sets its own descent and line gap below the
     * baseline and cuts what passes the line's edges. Measured in LibreOffice, a 14pt icon
     * centred in a list line over 9pt text lost its top at the page's 14pt and at 16.35pt and
     * met the line's top only at 17.6pt — each point of line height raising it 0.8pt, a rule of
     * the editor's own and not one to guess at.</p>
     *
     * <p>Where the picture stands is not the same in both editors: Word moves it by
     * {@code w:position}, and LibreOffice ignores that on a picture and stands it on the
     * baseline — measured, a picture written at 0, −2, −10 and +10pt stood in one place. So
     * its top is taken as the higher of the two, and a 12pt icon the page centres on a 14pt
     * line — below the text's ascent where Word puts it, above it on the baseline — counts as
     * rising: in LibreOffice it lost 1.7pt of its top at the exact height.</p>
     *
     * @param reach    how far above the line's bottom the highest picture reaches, in points
     * @param overText whether a picture passes the text's ascent or descent
     */
    record PictureReach(double reach, boolean overText) {

        /** No picture written. */
        static final PictureReach NONE = new PictureReach(0, false);

        static PictureReach of(double bottomFromBaseline, double height,
                               com.demcha.compose.document.layout.payloads.ParagraphLine line) {
            return of(bottomFromBaseline, height, line, 0);
        }

        /**
         * The reach of a picture whose ink is inset from its edges by an empty frame.
         *
         * <p>Either editor moves the whole picture, frame and all: Word by its position,
         * LibreOffice onto the baseline. The frame is not ink, so what reaches past the text
         * is the picture less its frame, wherever the editor stood it.</p>
         */
        static PictureReach of(double bottomFromBaseline, double height,
                               com.demcha.compose.document.layout.payloads.ParagraphLine line,
                               double inset) {
            double descent = line.baselineOffsetFromBottom();
            // Word's placement, and LibreOffice's on the baseline.
            double top = Math.max(bottomFromBaseline + height, height) - inset;
            boolean passes = top > line.textAscent() || -(bottomFromBaseline + inset) > descent;
            return new PictureReach(descent + top, passes);
        }

        PictureReach max(PictureReach other) {
            return other == null ? this
                    : new PictureReach(Math.max(reach, other.reach), overText || other.overText);
        }
    }

    /**
     * How far above the line's baseline the page puts an inline picture's bottom edge — the
     * rule {@code PdfParagraphFragmentRenderHandler} draws by, measured from the baseline,
     * which is where Word stands the picture before {@code w:position} moves it.
     */
    static double inlineBottomFromBaseline(InlineImageAlignment alignment, double baselineOffset, double height,
                                           com.demcha.compose.document.layout.payloads.ParagraphLine line) {
        double descent = line.baselineOffsetFromBottom();
        double bottom = switch (alignment == null ? InlineImageAlignment.CENTER : alignment) {
            case BASELINE -> 0;
            case CENTER -> (line.lineHeight() - height) / 2.0 - descent;
            case TEXT_TOP -> line.textAscent() - height;
            case TEXT_BOTTOM -> -descent;
        };
        return bottom + baselineOffset;
    }

    /**
     * One list item's first line, from the list's.
     *
     * <p>The layout measures a list's lines, and the export reads the first, so every item
     * would otherwise be placed by the first item's line height — which the first item's
     * pictures set. The text's metrics are the list's; the height is the one the layout gives
     * a line, the taller of the text's and the item's tallest inline graphic.</p>
     */
    static com.demcha.compose.document.layout.payloads.ParagraphLine itemLine(
            com.demcha.compose.document.layout.payloads.ParagraphLine listLine, List<InlineRun> runs) {
        double height = listLine.textLineHeight();
        for (InlineRun run : runs) {
            if (run instanceof InlineImageRun image) {
                height = Math.max(height, image.height());
            } else if (run instanceof InlineSvgRun svg) {
                height = Math.max(height, svg.height());
            } else if (run instanceof InlineShapeRun shape) {
                height = Math.max(height, shape.height());
            }
        }
        return new com.demcha.compose.document.layout.payloads.ParagraphLine(listLine.text(), listLine.width(),
                height, listLine.textLineHeight(), listLine.textAscent(), listLine.baselineOffsetFromBottom(),
                listLine.spans(), listLine.visualOrder());
    }

    /**
     * The text-carrying form of one inline run, or null for a run that carries no text.
     *
     * <p>Asks the one reduction — {@link InlineRun#textRuns} — about a single run rather
     * than repeating its rules here, so a chip's text arrives normalized exactly as it is
     * everywhere else. The runs are walked in their authored form because the reduction
     * answers what to <em>write</em> and drops what only the chip knows: its fill.</p>
     */
    private static InlineTextRun textOf(InlineRun run) {
        List<InlineTextRun> lowered = InlineRun.textRuns(List.of(run));
        return lowered.isEmpty() ? null : lowered.get(0);
    }

    /** The chip behind a run, or null for a run that is not one. */
    private static InlineBackground backgroundOf(InlineRun run) {
        return run instanceof InlineHighlightRun highlight ? highlight.background() : null;
    }

    /**
     * Shades a run with the chip its author put behind it.
     *
     * <p>An inline {@code code} span and a status badge both exported as bare text: the
     * reduction to text runs keeps the glyphs and drops the fill, and nothing downstream
     * put it back. A chip that carries meaning — a red badge reading "overdue" — came out
     * the same colour as the sentence around it.</p>
     *
     * <p>Word shades a run with {@code w:shd}, which takes any RGB. Its highlighter pen
     * ({@code w:highlight}) is the other candidate and takes one of sixteen named colours,
     * which no brand palette is a member of — a chip written with it is whichever of the
     * sixteen was nearest, and reads as text someone marked up rather than as design.</p>
     *
     * <p>A {@code w:shd} fill is opaque, and the chip this sugar reaches for most —
     * {@code code(...)} — is a fifth-opacity grey. Written at full strength it is a solid
     * slab where the page has a tint, so a translucent fill is flattened first against what
     * Word paints underneath it: the paragraph's own shading, the cell's, or the page. The
     * chip then agrees with the file it is in — including where that file already differs
     * from the page, since a translucent <em>container</em> fill lands opaque too. What it
     * stops being is translucent: recoloured underneath in Word, the chip no longer
     * follows.</p>
     *
     * <p>What Word cannot express is the chip's <em>shape</em>. Shading covers the glyph
     * box, so the rounded corners and the padding that widens the run on the page are not
     * in the file. All three are recorded rather than quietly approximated.</p>
     */
    private void applyInlineBackground(XWPFRun run, InlineBackground background, String path) {
        if (background == null) {
            return;
        }
        CTRPr properties = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
        // w:shd sits in a repeating choice in the schema, so the accessor is an array and
        // addNewShd() appends rather than replaces — a run carrying two shadings leaves
        // Word reading whichever it meets first.
        CTShd shading = properties.sizeOfShdArray() > 0
                ? properties.getShdArray(0)
                : properties.addNewShd();
        shading.setVal(STShd.CLEAR);
        shading.setColor("auto");
        shading.setFill(toHexColor(flatten(background.fill().color(), colourUnder(run))));
        String lost = chipLost(background);
        if (lost != null) {
            if (warnedNodeKinds.add("inline-background")) {
                LOG.warn("DocxSemanticBackend: an inline chip keeps its fill as run shading, "
                         + "but Word shades the glyph box — {}. (One warning per export.)", lost);
            }
            report.add(DocxExportReport.Severity.APPROXIMATED, "inline chip", path,
                    "the fill is written as run shading; " + lost);
        }
    }

    /** What a chip loses on the way to run shading, or null when the mapping is exact. */
    private static String chipLost(InlineBackground background) {
        List<String> lost = new ArrayList<>(3);
        if (background.cornerRadius() > 0) {
            lost.add("its rounded corners are square");
        }
        if (background.padding().horizontal() > 0 || background.padding().vertical() > 0) {
            lost.add("its padding is not in the file");
        }
        if (background.fill().color().getAlpha() < 255) {
            // The colour on the page is right. What is gone is the translucency itself:
            // shade the paragraph a different colour in Word and a chip that was a tint
            // over it stays the tint it was flattened to.
            lost.add("its fill is flattened against what sits under it, because run "
                     + "shading is opaque");
        }
        return lost.isEmpty() ? null : String.join(", ", lost);
    }

    /**
     * The colour Word will paint under {@code run} — the shading this export itself wrote
     * on the run's paragraph or on the cell holding it, then the fill of the panel around an
     * unshaded cell, and otherwise the page's white.
     *
     * <p>Read back from the file being written rather than tracked in a field, so it is
     * whatever was actually written and cannot drift from it. Read, and only read:
     * {@code cellProperties} would create the {@code w:tcPr} it cannot find, so an
     * unstyled cell holding a chip would come away carrying an empty one.</p>
     */
    private java.awt.Color colourUnder(XWPFRun run) {
        XWPFParagraph para = run.getParent() instanceof XWPFParagraph parent ? parent : null;
        CTPPr paragraphProperties = para == null || !para.getCTP().isSetPPr()
                ? null
                : para.getCTP().getPPr();
        java.awt.Color paragraphFill = shadingFillOf(
                paragraphProperties != null && paragraphProperties.isSetShd()
                        ? paragraphProperties.getShd() : null);
        if (paragraphFill != null) {
            return paragraphFill;
        }
        CTTcPr cellProperties = currentCell == null || !currentCell.getCTTc().isSetTcPr()
                ? null
                : currentCell.getCTTc().getTcPr();
        java.awt.Color cellFill = shadingFillOf(
                cellProperties != null && cellProperties.isSetShd() ? cellProperties.getShd() : null);
        if (cellFill != null) {
            return cellFill;
        }
        // A cell with no shading of its own — a row's, inside a card — shows the panel's.
        return surfaceBehind != null ? surfaceBehind.color() : java.awt.Color.WHITE;
    }

    /**
     * A shading's fill as a colour, or null when it is unset or Word's own {@code auto}.
     *
     * <p>The schema's hex colour is a union, and XmlBeans hands a written one back as the
     * three bytes rather than as the string it was set from — read as text it is an array's
     * identity, which parses as no colour at all and silently flattens against white.</p>
     */
    private static java.awt.Color shadingFillOf(CTShd shading) {
        // A written RGB comes back as its three bytes. Anything else — Word's "auto", or a
        // value this export did not write — is no colour it can composite against.
        Object fill = shading == null ? null : shading.getFill();
        if (!(fill instanceof byte[] rgb) || rgb.length != 3) {
            return null;
        }
        return new java.awt.Color(rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
    }

    /**
     * Composites a colour over what sits beneath it, so a translucent fill survives a
     * format that has no alpha. An opaque colour is returned untouched.
     */
    private static java.awt.Color flatten(java.awt.Color colour, java.awt.Color under) {
        int alpha = colour.getAlpha();
        if (alpha >= 255) {
            return colour;
        }
        double weight = alpha / 255.0;
        return new java.awt.Color(
                blend(colour.getRed(), under.getRed(), weight),
                blend(colour.getGreen(), under.getGreen(), weight),
                blend(colour.getBlue(), under.getBlue(), weight));
    }

    private static int blend(int over, int under, double weight) {
        return (int) Math.round(over * weight + under * (1 - weight));
    }

    /**
     * A run, inside a hyperlink when the thing being written is one.
     *
     * <p>Every link the document carried was dropped: a reader opened an exported document
     * and found the text of a link with nothing behind it, and a reference to another
     * section that went nowhere. Word owns both — {@code w:hyperlink} with a relationship
     * for an address, or with {@code w:anchor} for a bookmark in the same document — so
     * this is a mapping rather than an approximation.</p>
     *
     * <p>An external address goes through POI's own {@code createHyperlinkRun}, which makes
     * the external relationship the part needs. An internal one is built here: POI has no
     * helper for an anchor, and the run it would hand back is registered in a list this
     * export never reads — what matters is the XML, and reading the file back gives POI's
     * own hyperlink run either way.</p>
     *
     * @param para   the paragraph being filled
     * @param target the link this run carries, or null for ordinary text
     * @return the run to write text into
     */
    private XWPFRun newRun(XWPFParagraph para, DocumentLinkTarget target) {
        if (target instanceof ExternalLinkTarget external
            && external.options() != null && external.options().uri() != null
            && !external.options().uri().isBlank()) {
            return para.createHyperlinkRun(external.options().uri());
        }
        if (target instanceof InternalLinkTarget internal) {
            String name = bookmarkNames.nameFor(internal.anchor());
            if (name != null) {
                CTHyperlink link = para.getCTP().addNewHyperlink();
                link.setAnchor(name);
                // Through the IRunBody constructor: the XWPFParagraph overload is deprecated,
                // and an unqualified paragraph argument would pick it.
                return new XWPFRun(link.addNewR(), (IRunBody) para);
            }
        }
        return para.createRun();
    }

    /**
     * Marks the anchor a node declares, so a link can point at it.
     *
     * <p>Written as a bookmark around the paragraph rather than as an empty one before it:
     * a reader following the link lands on the text, and Word's own "go to bookmark" shows
     * the paragraph rather than an insertion point above it.</p>
     *
     * <p>A bookmark is not an outline entry. Word builds its Navigation Pane from heading
     * styles, and nothing here promotes an anchored paragraph to one — an anchor says where
     * a link goes, and inventing a heading from it would restyle the document.</p>
     */
    private int openAnchor(XWPFParagraph para, String anchor) {
        String name = bookmarkNames.nameFor(anchor);
        if (name == null) {
            return -1;
        }
        int id = bookmarkNames.nextId();
        CTBookmark start = para.getCTP().addNewBookmarkStart();
        start.setId(BigInteger.valueOf(id));
        start.setName(name);
        return id;
    }

    /**
     * Closes the bookmark {@link #openAnchor} opened, after the paragraph's runs.
     *
     * <p>Opened and closed in two calls on purpose: both elements append to the end of the
     * paragraph, so opening and closing in one leaves a bookmark wrapping nothing, and a
     * reader following the link lands before the text rather than on it.</p>
     */
    private static void closeAnchor(XWPFParagraph para, int id) {
        if (id >= 0) {
            para.getCTP().addNewBookmarkEnd().setId(BigInteger.valueOf(id));
        }
    }

    /**
     * Embeds an image at the size the node asks for, in the shape its fit mode asks for.
     *
     * <p>The box came from the node's literal {@code width} / {@code height} and fell back
     * to a hardcoded 100 × 100 pt when either was absent — so an image sized only by
     * {@code scale}, or by one dimension with the other implied by its aspect ratio, came
     * out at a size nothing had asked for. {@link NodeDefinitionSupport#resolveImageDimensions}
     * is the rule the layout pipeline applies for exactly this, including the clamp to the
     * page's content width, and is used here so the two agree.</p>
     *
     * <p>{@code fitMode} then decides how the image sits in that box, matching the PDF
     * handler: {@code CONTAIN} scales by the smaller ratio and is embedded at that size,
     * which needs no clipping because it is inside the box already; {@code COVER} scales by
     * the larger and crops the overflow away in source space through {@code a:srcRect},
     * centred, the way the PPTX backend expresses the same geometry; {@code STRETCH} fills
     * the box.</p>
     */
    private void writeImage(XWPFDocument document, ImageNode node) throws Exception {
        // One acquisition, and the bytes come from it. Reading the file separately was
        // not only a second read: the source cache keys on the path alone, so a file
        // rewritten between renders gave this method fresh bytes off disk and cached
        // metadata from the old one — a picture embedded at the previous version's
        // dimensions. The bytes and the size a frame is built from now come from the
        // same resolution.
        // Resolution failures are not caught here. The old readBytes swallowed one
        // narrow case — a path that would not read — and even that was a side effect of
        // its catch rather than a contract. Catching around the resolver instead would
        // widen the silence to every way it can fail: corrupt bytes, a format with no
        // reader, a metadata decode that gives up, a defect in the cache. An export that
        // quietly drops a picture for any of those is a worse answer than one that says
        // the image could not be read.
        ImageData resolved = NodeDefinitionSupport.toImageData(node.imageData());
        byte[] bytes = resolved.getBytes();
        if (bytes.length == 0) {
            return;
        }
        double sourceWidth = Math.max(1, resolved.getMetadata().width());
        double sourceHeight = Math.max(1, resolved.getMetadata().height());
        // Handed the data this method already resolved. Left to resolve it itself, the
        // sizing pass repeated the copy and the whole-array hash behind
        // ImageSourceCache.fromBytes, so one image cost two of each on the way out.
        NodeDefinitionSupport.ImageDimensions box =
                NodeDefinitionSupport.resolveImageDimensions(node, availableWidth(), resolved);

        DocumentImageFitMode fitMode =
                node.fitMode() == null ? DocumentImageFitMode.STRETCH : node.fitMode();

        double drawWidth = box.width();
        double drawHeight = box.height();
        if (fitMode == DocumentImageFitMode.CONTAIN) {
            double scale = Math.min(box.width() / sourceWidth, box.height() / sourceHeight);
            drawWidth = sourceWidth * scale;
            drawHeight = sourceHeight * scale;
        }

        XWPFParagraph para = newBodyParagraph(document);
        // A picture is a block: the space the node holds above and below itself is written
        // on the paragraph carrying it, the same as any other block's. Without it the
        // probe's image ran straight into the heading under it, 24pt short of the page.
        applyVerticalSpacing(para, node);
        XWPFRun run = para.createRun();
        try (InputStream stream = new java.io.ByteArrayInputStream(bytes)) {
            XWPFPicture picture = run.addPicture(stream,
                    pictureType(bytes),
                    "image",
                    Units.toEMU(drawWidth),
                    Units.toEMU(drawHeight));
            if (fitMode == DocumentImageFitMode.COVER) {
                applyCoverCrop(picture, sourceWidth, sourceHeight, box);
            }
        }
    }

    /**
     * Writes a barcode as a picture of the symbol at the size the page draws it.
     *
     * <p>It was dropped with the geometry-only nodes, so a receipt or a shipping label lost
     * the code a reader scans. The picture carries the same matrix the page draws (see
     * {@link DocxBarcodePictures}); what it does not carry is the data, which in Word is part
     * of a picture rather than something to edit, so the report says so — and says so of a
     * link or a transform on it too, which the picture does not carry either.</p>
     *
     * <p>A symbol drawn in two transparent colours is still written: it holds its space on the
     * page, and an anchor on it has to land somewhere.</p>
     */
    private void writeBarcode(XWPFDocument document, com.demcha.compose.document.node.BarcodeNode node)
            throws Exception {
        com.demcha.compose.engine.components.content.barcode.BarcodeData data =
                NodeDefinitionSupport.toBarcodeData(node.barcodeOptions());
        byte[] png = DocxBarcodePictures.png(data, node.width(), node.height());
        XWPFParagraph para = newBodyParagraph(document);
        applyVerticalSpacing(para, node);
        XWPFRun run = para.createRun();
        try (InputStream stream = new java.io.ByteArrayInputStream(png)) {
            run.addPicture(stream, PictureType.PNG, "barcode",
                    Units.toEMU(node.width()), Units.toEMU(node.height()));
        }
        // A reader's screen reader has only the picture's description to go on: the data is it.
        run.getCTR().getDrawingArray(0).getInlineArray(0).getDocPr().setDescr(data.getContent());
        StringBuilder message = new StringBuilder(
                "written as a picture of the symbol at its size, which scans as the page's does; "
                + "its data is part of the picture and is not editable in Word");
        if (node.linkTarget() != null) {
            message.append("; its link is not carried");
        }
        if (node.transform() != null && !node.transform().isIdentity()) {
            message.append("; its transform is not carried, so it is drawn upright at its size");
        }
        report.add(DocxExportReport.Severity.APPROXIMATED, "barcode", layout.pathOf(node), message.toString());
    }

    /**
     * Writes a horizontal rule as Word's own: an empty paragraph whose bottom border is the
     * rule (see {@link DocxRules}).
     *
     * <p>Lines and dividers were dropped with the rest of the drawing, so every rule a
     * template draws under a heading or between entries was missing from the Word file.
     * The paragraph is placed where the rule's box is: the space above the stroke is the
     * paragraph's height, held to a tenth of a point when there is none, the space below is
     * owed to what follows, and the rule's two ends are the paragraph's indents. Word draws
     * a border in its own dash lengths, so a dashed rule keeps its dash and not the pattern's
     * lengths, and the report says so.</p>
     */
    private void writeRule(XWPFDocument document, DocumentNode node, DocxRules.Rule rule) {
        double sideLeft = node.margin().left() + node.padding().left();
        double sideRight = node.margin().right() + node.padding().right();
        java.util.OptionalDouble placed = layout.placedWidth(node);
        double boxWidth = placed.isPresent()
                ? placed.getAsDouble() - node.padding().horizontal()
                : rule.fillsWidth() ? availableWidth() - sideLeft - sideRight : rule.boxWidth();
        double to = rule.fillsWidth() ? boxWidth : Math.min(rule.endX(), boxWidth);
        double from = Math.min(Math.max(0, rule.startX()), Math.max(0, to));

        XWPFParagraph para = newBodyParagraph(document);
        applyVerticalSpacing(para, node);
        CTPPr properties = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        CTInd indent = properties.isSetInd() ? properties.getInd() : properties.addNewInd();
        indent.setLeft(BigInteger.valueOf(toTwips(insetLeft + sideLeft + from)));
        // The right end is placed against the width the rule is written in, when that width is
        // known: a cell whose grid this export did not write has none, and measuring against the
        // page there put the rule's end past the cell's.
        boolean widthKnown = currentCell != null ? Double.isFinite(currentCellWidth) : contentWidth < Double.MAX_VALUE;
        if (widthKnown) {
            double rightGap = availableWidth() - sideLeft - to;
            indent.setRight(BigInteger.valueOf(toTwips(insetRight + Math.max(0, rightGap))));
        }

        // Word stacks the paragraph's line, then the border, then the space after; the page
        // draws the stroke across its box. A stroke thicker than its box — horizontal() sizes
        // the box from the stroke set before it, so a 2pt stroke set after sits in a 1pt box —
        // spills out of it on the page and takes no room, so the room it takes in Word comes
        // off the space owed below, where there is any.
        double above = Math.max(0, rule.centreFromTop() - rule.thickness() / 2);
        double below = Math.max(0, rule.boxHeight() - rule.centreFromTop() - rule.thickness() / 2);
        double line = Math.max(SEPARATOR_POINTS, above);
        CTSpacing spacing = properties.isSetSpacing() ? properties.getSpacing() : properties.addNewSpacing();
        spacing.setLineRule(STLineSpacingRule.EXACT);
        spacing.setLine(BigInteger.valueOf(Math.round(line * POINT_TO_TWIP)));
        owePendingSpacingAfter(below);
        double excess = line + rule.thickness() + below - rule.boxHeight();
        if (excess > 0) {
            pendingSpacingAfter = Math.max(0, pendingSpacingAfter - excess);
        }

        // A border is opaque, so a translucent rule is flattened against what lies under it, as a
        // chip is; one that is not drawn at all keeps its place and draws nothing.
        java.awt.Color colour = rule.colour().color();
        if (colour.getAlpha() > 0) {
            CTPBdr borders = properties.isSetPBdr() ? properties.getPBdr() : properties.addNewPBdr();
            CTBorder bottom = borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom();
            java.awt.Color under = surfaceBehind != null ? surfaceBehind.color() : java.awt.Color.WHITE;
            paintEdge(bottom, dashOf(rule), BigInteger.valueOf(ruleEighths(rule.thickness())),
                    toHexColor(flatten(colour, under)));
            bottom.setSpace(BigInteger.ZERO);
        }
        if (node instanceof com.demcha.compose.document.node.LineNode lineNode && lineNode.linkTarget() != null) {
            report.add(DocxExportReport.Severity.APPROXIMATED, "rule link", layout.pathOf(node),
                    "the rule is written as a paragraph border, which carries no link");
        }
        if (rule.dashed() != null) {
            report.add(DocxExportReport.Severity.APPROXIMATED, "dash pattern", layout.pathOf(node),
                    "drawn in Word's own dash for a border, which keeps the rule dashed but not "
                    + "the pattern's lengths");
        }
    }

    /** Word's border for a rule's dash: dots where the dashes are no longer than the stroke. */
    private static STBorder.Enum dashOf(DocxRules.Rule rule) {
        if (rule.dashed() == null) {
            return STBorder.SINGLE;
        }
        double on = rule.dashed().get(0);
        return on <= rule.thickness() * 1.5 ? STBorder.DOTTED : STBorder.DASHED;
    }

    /** A rule's thickness as {@code w:sz}: eighths of a point, from Word's thinnest to its thickest. */
    private static long ruleEighths(double thickness) {
        return Math.max(2, Math.min(Math.round(DocxRules.MAX_RULE_POINTS * 8), Math.round(thickness * 8)));
    }

    /**
     * Crops a {@code COVER} image to its box, centred, in source space.
     *
     * <p>Word has no clip for an inline picture, so the overflow the PDF backend clips away
     * is removed from the source instead: the picture is placed at the box's size and
     * {@code a:srcRect} names the fraction of each edge that is not shown.</p>
     */
    private void applyCoverCrop(XWPFPicture picture, double sourceWidth, double sourceHeight,
                                NodeDefinitionSupport.ImageDimensions box) {
        double scale = Math.max(box.width() / sourceWidth, box.height() / sourceHeight);
        double horizontal = (sourceWidth * scale - box.width()) / (sourceWidth * scale) / 2.0;
        double vertical = (sourceHeight * scale - box.height()) / (sourceHeight * scale) / 2.0;
        CTRelativeRect srcRect = picture.getCTPicture().getBlipFill().addNewSrcRect();
        srcRect.setL(toThousandthPercent(horizontal));
        srcRect.setR(toThousandthPercent(horizontal));
        srcRect.setT(toThousandthPercent(vertical));
        srcRect.setB(toThousandthPercent(vertical));
    }

    /** A crop fraction as the per-100000 integer DrawingML stores. */
    private static int toThousandthPercent(double fraction) {
        if (Double.isNaN(fraction) || fraction <= 0.0) {
            return 0;
        }
        return (int) Math.round(Math.min(fraction, 0.5) * 100_000);
    }

    /**
     * The picture type the bytes actually are.
     *
     * <p>Every image was declared {@code PNG} regardless of its content, so a JPEG went into
     * the package announced as something it is not. The signature is read instead; a format
     * with no signature here keeps the old answer and says so once.</p>
     */
    private PictureType pictureType(byte[] bytes) {
        if (startsWith(bytes, 0x89, 0x50, 0x4E, 0x47)) {
            return PictureType.PNG;
        }
        if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) {
            return PictureType.JPEG;
        }
        if (startsWith(bytes, 0x47, 0x49, 0x46)) {
            return PictureType.GIF;
        }
        if (startsWith(bytes, 0x42, 0x4D)) {
            return PictureType.BMP;
        }
        if (startsWith(bytes, 0x49, 0x49, 0x2A, 0x00) || startsWith(bytes, 0x4D, 0x4D, 0x00, 0x2A)) {
            return PictureType.TIFF;
        }
        if (warnedNodeKinds.add("image-signature")) {
            LOG.warn("DocxSemanticBackend: image bytes carry no recognised signature — declaring PNG, "
                     + "which is what Word will try to decode them as");
        }
        return PictureType.PNG;
    }

    private static boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Writes a table on the grid its cells actually occupy.
     *
     * <p>An authored row is not a row of columns: a {@code rowSpan} covers positions in the
     * rows below it and those rows do not repeat the covered cells, and a {@code colSpan}
     * makes the number of authored records differ from the number of columns. Sizing the
     * grid from the first row's record count therefore built a table too narrow whenever a
     * span was involved, and the loop that filled it stopped at the last column that
     * existed — so the cells past it were not written at all. {@link TableGrid} is the
     * layout pipeline's own resolution of that grid, used here so the two cannot disagree.
     * </p>
     *
     * <p>Word expresses the merges natively: {@code w:gridSpan} widens a cell, and
     * {@code w:vMerge} restarts on the cell that owns a vertical span and continues on the
     * ones it covers.</p>
     */
    /**
     * Writes a table-shaped node with the space it holds above and below itself.
     *
     * <p>A table is the one block whose own box had nowhere to go. Word has no space above
     * a table and none below one, so a table's {@code margin} and {@code padding} were
     * dropped — and a {@code RowNode} is exported as a table, so a row's padding went the
     * same way. Measured on the probe corpus, that is a row's 14pt lost twice over, once at
     * each edge, and the 6pt a billing table holds above itself.</p>
     *
     * <p>Neither edge needs an element of its own: the space above a table is the space
     * below the paragraph before it, and the space below one is the space above the
     * paragraph after. Both go through the debt every other gap goes through
     * ({@link #owePendingSpacingAfter}), so a table between two paragraphs reads the same as
     * two paragraphs with a gap between them — and a table with nothing above it loses that
     * edge, which is the one thing Word genuinely cannot hold.</p>
     */
    private void writeTableWithItsOwnSpacing(XWPFDocument document, DocumentNode node)
            throws Exception {
        owePendingSpacingAfter(node.margin().top() + node.padding().top());
        if (node instanceof RowNode row) {
            writeRow(document, row);
        } else {
            writeTable(document, (TableNode) node);
        }
        owePendingSpacingAfter(node.margin().bottom() + node.padding().bottom());
    }

    private void writeTable(XWPFDocument document, TableNode node) throws Exception {
        if (node.rows().isEmpty()) {
            return;
        }
        int rowCount = node.rows().size();
        int columnCount = TableGrid.columnCount(node);
        if (columnCount == 0) {
            // Nothing declares a column and no cell claims one, so the grid has no positions
            // to place anything in. Word still needs a cell in a table, so write the empty
            // one this used to produce — widening the count instead would leave a position
            // no placement covers, and reading it back is a crash rather than an empty cell.
            newTable(document, rowCount, 1);
            return;
        }
        TableGrid.Placement[][] cover = new TableGrid.Placement[rowCount][columnCount];
        for (List<TableGrid.Placement> sourceRow : TableGrid.resolve(node)) {
            for (TableGrid.Placement placement : sourceRow) {
                for (int r = placement.row(); r < placement.row() + placement.rowSpan(); r++) {
                    for (int c = placement.column(); c < placement.column() + placement.colSpan(); c++) {
                        cover[r][c] = placement;
                    }
                }
            }
        }

        // One cell per row to start with, then as many as that row actually needs: a merged
        // cell is one cell carrying a span, not several, so a row's physical count is not
        // the column count.
        XWPFTable table = newTable(document, rowCount, 1);
        applyTableWidth(table, node, columnCount);
        for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
            XWPFTableRow row = table.getRow(rowIdx);
            List<TableGrid.Placement> physical = new ArrayList<>();
            for (int col = 0; col < columnCount; ) {
                TableGrid.Placement placement = cover[rowIdx][col];
                physical.add(placement);
                col += placement.colSpan();
            }
            while (row.getTableCells().size() < physical.size()) {
                row.createCell();
            }
            for (int i = 0; i < physical.size(); i++) {
                TableGrid.Placement placement = physical.get(i);
                XWPFTableCell cell = row.getCell(i);
                applySpans(cell, placement, rowIdx);
                // The covered positions of a merge take the paint too, so a merged
                // region reads as one cell rather than as a striped run of them.
                DocumentColor fill = resolveCellFill(node, placement);
                applyCellPaint(cell, fill, resolveCellValue(node, placement, DocumentTableStyle::stroke));
                applyCellPadding(cell, resolveCellPadding(node, placement));
                applyVerticalAnchor(cell, resolveCellAnchor(node, placement));
                if (placement.row() != rowIdx) {
                    // A covered position carries the merge marker and no content of its own.
                    continue;
                }
                cell.removeParagraph(0);
                // What the cell holds sits on the cell's fill — a stripe, not the card around
                // the table — wherever it has no shading of its own.
                DocumentColor outerSurface = surfaceBehind;
                if (fill != null) {
                    surfaceBehind = fill;
                }
                try {
                    writeCellContent(cell, placement, node);
                } finally {
                    surfaceBehind = outerSurface;
                }
            }
        }
        breakRowsWhereTheLayoutDoes(table, node);
        indentTable(table);
    }

    /**
     * Lets Word break a table across pages only where the layout would.
     *
     * <p>The layout never breaks a row: a table splits between rows, and a row that does not
     * fit what is left of a page moves to the next one whole. It repeats the table's header
     * rows at the top of every page the table continues on, and it never leaves them at the
     * foot of a page with nothing under them. The export said none of this, so Word split
     * rows mid-line wherever its own page ended, and a long table's header appeared once.</p>
     *
     * <p>Word holds all three: {@code w:cantSplit} keeps a row whole, {@code w:tblHeader}
     * repeats a leading row, and keep-with-next on a row's paragraphs keeps it on the page of
     * the row after it.</p>
     *
     * <p>A row is kept whole only where the layout placed it. The layout refuses a row taller
     * than a page, so a placed row is one a page can hold; a document it could not lay out is
     * exported without one, and there a row may be taller than a page — which no page holds
     * whole, however Word is told to keep it.</p>
     */
    private void breakRowsWhereTheLayoutDoes(XWPFTable table, TableNode node) {
        int rowCount = table.getRows().size();
        int headerRows = Math.min(node.repeatedHeaderRowCount(), rowCount);
        for (int index = 0; index < rowCount; index++) {
            XWPFTableRow row = table.getRow(index);
            if (layout.placedRow(node, index)) {
                row.setCantSplitRow(true);
            }
            if (index < headerRows) {
                row.setRepeatHeader(true);
                if (headerRows < rowCount) {
                    keepRowWithTheNext(row);
                }
            }
        }
    }

    private static void keepRowWithTheNext(XWPFTableRow row) {
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                CTPPr properties = paragraph.getCTP().isSetPPr()
                        ? paragraph.getCTP().getPPr()
                        : paragraph.getCTP().addNewPPr();
                if (!properties.isSetKeepNext()) {
                    properties.addNewKeepNext();
                }
            }
        }
    }

    private void applySpans(XWPFTableCell cell, TableGrid.Placement placement, int rowIdx) {
        if (placement.colSpan() == 1 && placement.rowSpan() == 1) {
            return;
        }
        CTTcPr properties = cellProperties(cell);
        if (placement.colSpan() > 1) {
            properties.addNewGridSpan().setVal(BigInteger.valueOf(placement.colSpan()));
        }
        if (placement.rowSpan() > 1) {
            properties.addNewVMerge().setVal(
                    placement.row() == rowIdx ? STMerge.RESTART : STMerge.CONTINUE);
        }
    }

    /**
     * Paints a cell with the fill and the edges its style asks for.
     *
     * <p>A {@link DocumentTableStyle} carries a {@code fillColor} and a {@code stroke}, and
     * neither reached the file: a zebra body, a header band and a ruled grid all exported on
     * Word's defaults, which is to say with no fill and no borders. Word owns both —
     * {@code w:shd} for the fill and {@code w:tcBorders} for the four edges — so this is
     * mapping rather than approximation.</p>
     *
     * <p>What does not survive is transparency. A {@code w:shd} fill is opaque, so a colour
     * carrying an opacity below 1 lands at full strength; the alternative would be blending it
     * against a background this backend does not resolve, Word owning the flow.</p>
     */
    private void applyCellPaint(XWPFTableCell cell, DocumentColor fill, DocumentStroke stroke) {
        if (fill == null && stroke == null) {
            return;
        }
        CTTcPr properties = cellProperties(cell);
        if (fill != null) {
            CTShd shading = properties.isSetShd() ? properties.getShd() : properties.addNewShd();
            shading.setVal(STShd.CLEAR);
            shading.setFill(toHexColor(fill.color()));
        }
        if (stroke != null) {
            CTTcBorders borders = properties.isSetTcBorders()
                    ? properties.getTcBorders()
                    : properties.addNewTcBorders();
            if (stroke.width() > 0) {
                // w:sz counts eighths of a point, and rounds to at least one so a hairline
                // the author asked for stays a line rather than disappearing.
                BigInteger eighths = BigInteger.valueOf(
                        Math.max(1, Math.round(stroke.width() * 8.0)));
                String colour = toHexColor(stroke.color().color());
                paintEdge(borders.addNewTop(), STBorder.SINGLE, eighths, colour);
                paintEdge(borders.addNewBottom(), STBorder.SINGLE, eighths, colour);
                paintEdge(borders.addNewLeft(), STBorder.SINGLE, eighths, colour);
                paintEdge(borders.addNewRight(), STBorder.SINGLE, eighths, colour);
            } else {
                // A stroke of no width is how this codebase says "no border" — the fixed-layout
                // handler reads the same predicate as draw-nothing. Writing nothing here would
                // leave the cell on the table's default grid, so a deliberately borderless
                // design would export ruled. The cell says none of its own instead.
                paintEdge(borders.addNewTop(), STBorder.NIL, null, null);
                paintEdge(borders.addNewBottom(), STBorder.NIL, null, null);
                paintEdge(borders.addNewLeft(), STBorder.NIL, null, null);
                paintEdge(borders.addNewRight(), STBorder.NIL, null, null);
            }
        }
    }

    private static void paintEdge(CTBorder edge, STBorder.Enum kind, BigInteger eighths, String colour) {
        edge.setVal(kind);
        if (eighths != null) {
            edge.setSz(eighths);
        }
        if (colour != null) {
            edge.setColor(colour);
        }
    }

    /**
     * Keeps a cell's own space clear inside its edges.
     *
     * <p>A table's rows came out shorter than the page draws them: measured on the probe
     * corpus, every row of a five-row table was 8.1pt short, because the cell padding the
     * engine lays out with was never written and Word used its own — 5.4pt at each side and
     * <em>nothing</em> above or below. Word holds this natively as {@code w:tcMar}, so it
     * is a mapping rather than an approximation.</p>
     *
     * <p>All four sides are written, and written even when they are zero, because Word's
     * default is not zero: a table that asked for no padding would otherwise export with
     * Word's side margins and read wider than it is. The vertical pair is what a reader
     * sees as the row's height, since Word grows a row to fit its content and this is part
     * of that content's box.</p>
     */
    private static void applyCellPadding(XWPFTableCell cell, DocumentInsets padding) {
        CTTcPr properties = cellProperties(cell);
        CTTcMar margins = properties.isSetTcMar() ? properties.getTcMar() : properties.addNewTcMar();
        setCellMargin(margins.isSetTop() ? margins.getTop() : margins.addNewTop(), padding.top());
        setCellMargin(margins.isSetBottom() ? margins.getBottom() : margins.addNewBottom(),
                padding.bottom());
        setCellMargin(margins.isSetLeft() ? margins.getLeft() : margins.addNewLeft(), padding.left());
        setCellMargin(margins.isSetRight() ? margins.getRight() : margins.addNewRight(),
                padding.right());
    }

    /**
     * The padding a cell resolves to, most specific wins, with the engine's own default
     * underneath.
     *
     * <p>The default is the last step of the same cascade the layout pipeline runs, where
     * {@code TableCellLayoutStyle.DEFAULT} sits under the authored styles — so a table that
     * states no padding is laid out with 4pt and has to be written with 4pt.
     * {@code DocxCellPaddingTest} pins the two together, because the engine's copy is
     * internal and cannot be read from here.</p>
     */
    private DocumentInsets resolveCellPadding(TableNode node, TableGrid.Placement placement) {
        DocumentInsets authored = resolveCellValue(node, placement, DocumentTableStyle::padding);
        return authored != null ? authored : DocumentInsets.of(ENGINE_DEFAULT_CELL_PADDING_POINTS);
    }

    /** What the engine lays a cell out with when nothing states otherwise. */
    static final double ENGINE_DEFAULT_CELL_PADDING_POINTS = 4.0;

    /**
     * The fill a cell resolves to, with the engine's own default underneath where it shows.
     *
     * <p>The engine paints a cell no style fills in white. On a page that is invisible, and a
     * cell written with no shading looks the same, so nothing is written. On a filled panel,
     * or in a filled cell, it is not: an unshaded cell shows that colour through it, where the
     * page draws a white cell. There the default is written. {@code DocxContainerPaintTest}
     * pins it to the engine's, whose copy is internal.</p>
     */
    private DocumentColor resolveCellFill(TableNode node, TableGrid.Placement placement) {
        DocumentColor authored = resolveCellValue(node, placement, DocumentTableStyle::fillColor);
        return authored != null || surfaceBehind == null ? authored : ENGINE_DEFAULT_CELL_FILL;
    }

    /** What the engine fills a cell with when nothing states otherwise. */
    static final DocumentColor ENGINE_DEFAULT_CELL_FILL = DocumentColor.WHITE;

    /** The left and right margins written on a cell, or Word's own default for an unwritten one. */
    private static double horizontalMarginsOf(XWPFTableCell cell) {
        CTTcPr properties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : null;
        CTTcMar margins = properties != null && properties.isSetTcMar() ? properties.getTcMar() : null;
        if (margins == null) {
            return 2 * WORD_DEFAULT_CELL_MARGIN_POINTS;
        }
        return marginPoints(margins.isSetLeft() ? margins.getLeft() : null)
               + marginPoints(margins.isSetRight() ? margins.getRight() : null);
    }

    private static double marginPoints(CTTblWidth margin) {
        Long twips = margin == null ? null : writtenTwips(margin.getW());
        return twips == null ? WORD_DEFAULT_CELL_MARGIN_POINTS : twips / POINT_TO_TWIP;
    }

    /** Word keeps this much clear inside every cell edge unless a table says otherwise. */
    private static final double WORD_DEFAULT_CELL_MARGIN_POINTS = 5.4;

    private static CTTcPr cellProperties(XWPFTableCell cell) {
        return cell.getCTTc().isSetTcPr()
                ? cell.getCTTc().getTcPr()
                : cell.getCTTc().addNewTcPr();
    }

    private void writeCellContent(XWPFTableCell cell, TableGrid.Placement placement, TableNode node)
            throws Exception {
        DocumentTableCell source = placement.cell();
        if (source.content() != null) {
            // A composed cell keeps its node and leaves lines() empty, so reading lines()
            // exported it as an empty cell.
            double previous = currentCellWidth;
            currentCellWidth = usableWidthOf(cell, placement);
            try {
                writeCellBody(cell, source.content());
            } finally {
                currentCellWidth = previous;
            }
            return;
        }
        List<String> lines = source.lines();
        // Word has a bidirectional engine of its own: it reorders the line and joins the
        // Arabic itself, given the text as written. What it cannot work out is the base
        // direction, and without w:bidi it assumes left to right — which puts a Hebrew
        // cell's trailing punctuation on the wrong side and starts the line at the wrong
        // edge. So the cell is told, and the text goes over untouched.
        boolean rightToLeft = resolveCellDirection(node, placement, lines);
        ParagraphAlignment alignment = toAlignment(horizontalOf(resolveCellAnchor(node, placement, rightToLeft)),
                rightToLeft);
        // The height the row was sized with. Without it Word sets the cell at its own
        // spacing for the font — measured on the probe corpus, a totals row whose style
        // states a 14pt face came out 3pt taller than the page draws it.
        java.util.OptionalDouble lineHeight = layout.cellLineHeight(node, placement.row(), placement.column());
        DocumentTextStyle textStyle = resolveCellTextStyle(node, placement);
        // The layout sizes the row with the cell's line spacing between its lines. Word holds
        // no space between the lines of one paragraph but a taller line, which would add it
        // above the first line too and grow the row; so a cell whose lines stand apart is a
        // paragraph per line, with the spacing after each but the last.
        Double spacing = resolveCellValue(node, placement, DocumentTableStyle::lineSpacing);
        double lineSpacing = spacing == null ? 0.0 : spacing;
        boolean paragraphPerLine = lineSpacing > 0 && lines.size() > 1;
        XWPFParagraph para = newCellLine(cell, rightToLeft, alignment, lineHeight);
        XWPFRun run = newCellRun(para, textStyle, rightToLeft);
        int inRun = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0 && paragraphPerLine) {
                addSpacing(para, 0, lineSpacing);
                para = newCellLine(cell, rightToLeft, alignment, lineHeight);
                run = newCellRun(para, textStyle, rightToLeft);
                inRun = 0;
            } else if (i > 0) {
                // A joined "\n" is not a line break in Word; it renders as one line.
                run.addBreak();
            }
            run.setText(lines.get(i) == null ? "" : lines.get(i), inRun++);
        }
    }

    /** A paragraph of a text cell: its direction, its alignment and its line height. */
    private static XWPFParagraph newCellLine(XWPFTableCell cell, boolean rightToLeft,
                                             ParagraphAlignment alignment,
                                             java.util.OptionalDouble lineHeight) {
        XWPFParagraph para = cell.addParagraph();
        applyDirection(para, rightToLeft);
        if (alignment != ParagraphAlignment.LEFT) {
            // LEFT is where Word starts the line anyway, in either direction.
            para.setAlignment(alignment);
        }
        applyLineHeight(para, lineHeight);
        return para;
    }

    /** The run a text cell's words go in, in the cell's face and direction. */
    private XWPFRun newCellRun(XWPFParagraph para, DocumentTextStyle textStyle, boolean rightToLeft) {
        XWPFRun run = para.createRun();
        applyStyle(run, textStyle);
        applyRunDirection(run, rightToLeft);
        return run;
    }

    /**
     * Where the page places a cell's content inside its box, most specific style wins.
     *
     * <p>The same cascade the layout merges, and the same default when none states one: the
     * engine's cell style sets text at the vertical middle, on the left — or on the right for
     * a right-to-left cell, which the layout gives {@code CENTER_RIGHT} the same way. Word's
     * default is the top left, so a cell left to it put a single line at the top of a row a
     * taller neighbour had stretched, and an amount column the page right-aligns flush left.</p>
     */
    private DocumentTableTextAnchor resolveCellAnchor(TableNode node, TableGrid.Placement placement,
                                                      boolean rightToLeft) {
        DocumentTableTextAnchor authored = resolveCellValue(node, placement, DocumentTableStyle::textAnchor);
        if (authored != null) {
            return authored;
        }
        return rightToLeft ? DocumentTableTextAnchor.CENTER_RIGHT : DocumentTableTextAnchor.CENTER_LEFT;
    }

    /** The anchor for the vertical half, where direction does not matter. */
    private DocumentTableTextAnchor resolveCellAnchor(TableNode node, TableGrid.Placement placement) {
        return resolveCellAnchor(node, placement, false);
    }

    /**
     * Writes where a cell's content sits vertically.
     *
     * <p>{@code DEFAULT} is the bottom edge, as the page places it: the engine maps it to
     * {@code Anchor.defaultAnchor()}, whose vertical half the cell renderer and the composed
     * cell both treat as the bottom. Top is Word's own default and is not written.</p>
     */
    private static void applyVerticalAnchor(XWPFTableCell cell, DocumentTableTextAnchor anchor) {
        XWPFTableCell.XWPFVertAlign vertical = switch (anchor) {
            case TOP_LEFT, TOP_RIGHT -> null;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> XWPFTableCell.XWPFVertAlign.CENTER;
            case BOTTOM_LEFT, BOTTOM_RIGHT, DEFAULT -> XWPFTableCell.XWPFVertAlign.BOTTOM;
        };
        if (vertical != null) {
            cell.setVerticalAlignment(vertical);
        }
    }

    /**
     * The horizontal half of a cell's anchor, as the alignment the page draws a line with.
     * {@code DEFAULT} is the left, as the cell renderer places it.
     */
    private static TextAlign horizontalOf(DocumentTableTextAnchor anchor) {
        return switch (anchor) {
            case CENTER -> TextAlign.CENTER;
            case CENTER_RIGHT, TOP_RIGHT, BOTTOM_RIGHT -> TextAlign.RIGHT;
            case CENTER_LEFT, TOP_LEFT, BOTTOM_LEFT, DEFAULT -> TextAlign.LEFT;
        };
    }

    /**
     * Whether a cell's text runs right to left.
     *
     * <p>{@link TextDirection#AUTO} is read off the cell as a whole rather than off each
     * line, because the cell is what the bidirectional algorithm calls a paragraph — a
     * second line that happens to open on Latin must not run the other way from the first.
     * The PDF backend resolves it from the same text, which is what keeps the two files
     * agreeing about the same table.</p>
     */
    private boolean resolveCellDirection(TableNode node, TableGrid.Placement placement,
                                         List<String> lines) {
        TextDirection declared = resolveCellValue(node, placement, DocumentTableStyle::direction);
        if (declared == null) {
            return false;
        }
        // Read the lines as the layout reads them, a break inside a line flattened to a
        // space, so the two decide the same direction from the same text.
        List<String> asLaidOut = new ArrayList<>(lines.size());
        for (String line : lines) {
            asLaidOut.add(line == null ? "" : line.replace('\r', ' ').replace('\n', ' '));
        }
        return ParagraphDirection.resolve(String.join("\n", asLaidOut), declared) == TextDirection.RTL;
    }

    /**
     * The text style a cell resolves to, most specific wins.
     *
     * <p>The same order the layout pipeline merges in: the table's default, then the
     * column's, then the row's, then the cell's own.</p>
     */
    private DocumentTextStyle resolveCellTextStyle(TableNode node, TableGrid.Placement placement) {
        DocumentTextStyle authored = resolveCellValue(node, placement, DocumentTableStyle::textStyle);
        return authored != null ? authored : DEFAULT_CELL_FACE;
    }

    /**
     * The face a table cell is drawn in when nothing in its cascade states one.
     *
     * <p>Without it a cell with no authored text style was written with no run properties
     * at all and took the document's Normal — 10.5pt on the probe corpus — while the page
     * draws the same cell in the engine's default cell face, 14pt Helvetica. The row came
     * out the right height and the text in it visibly smaller than the page's.</p>
     *
     * <p>Read from the engine's own {@code TableCellLayoutStyle.DEFAULT}, the last step of
     * the cascade the layout runs, rather than restated here — so the two cannot drift.</p>
     */
    private static final DocumentTextStyle DEFAULT_CELL_FACE =
            documentFaceOf(TableCellLayoutStyle.DEFAULT.textStyle());

    private static DocumentTextStyle documentFaceOf(TextStyle face) {
        return DocumentTextStyle.builder()
                .fontName(face.fontName())
                .size(face.size())
                .decoration(documentDecorationOf(face.decoration()))
                .color(DocumentColor.of(face.color()))
                .build();
    }

    /** The document twin of an engine decoration, by name; plain text when there is none. */
    private static DocumentTextDecoration documentDecorationOf(TextDecoration decoration) {
        for (DocumentTextDecoration candidate : DocumentTextDecoration.values()) {
            if (decoration != null && candidate.name().equals(decoration.name())) {
                return candidate;
            }
        }
        return DocumentTextDecoration.DEFAULT;
    }

    /**
     * Resolves one field of a cell's style, most specific wins.
     *
     * <p>The cascade the layout pipeline merges in — the table's default, then the column's,
     * then the row's, then the cell's own — applied per field rather than per style object, so
     * a table-wide border survives a row that only overrides the fill.</p>
     */
    private <T> T resolveCellValue(TableNode node, TableGrid.Placement placement,
                                   Function<DocumentTableStyle, T> field) {
        T resolved = null;
        for (DocumentTableStyle candidate : List.of(
                orEmpty(node.defaultCellStyle()),
                orEmpty(node.columnStyles().get(placement.column())),
                orEmpty(node.rowStyles().get(placement.row())),
                orEmpty(placement.cell().style()))) {
            T value = field.apply(candidate);
            if (value != null) {
                resolved = value;
            }
        }
        return resolved;
    }

    private static DocumentTableStyle orEmpty(DocumentTableStyle style) {
        return style == null ? DocumentTableStyle.empty() : style;
    }

    private void writeRow(XWPFDocument document, RowNode node) throws Exception {
        // Represent rows as a single one-row table so downstream editors get a
        // visual side-by-side layout; each cell holds its child as it is written
        // anywhere else (writeCellBody).
        if (node.children().isEmpty()) {
            return;
        }
        XWPFTable table = newTable(document, 1, node.children().size());
        // A row is a layout device, not a table anybody asked to see. POI's createTable
        // ships Word's default single-line grid, so without this every two-column block —
        // a header pair, a label beside a value — exported with visible rules the PDF
        // never draws.
        hideTableGrid(table);
        applyRowGeometry(table, node);
        XWPFTableRow row = table.getRow(0);
        // A row is laid out as one piece, never across a page break, so Word keeps it whole
        // too, where the layout placed it; see breakRowsWhereTheLayoutDoes.
        if (layout.placed(node)) {
            row.setCantSplitRow(true);
        }
        // A row has no fill of its own: inside a panel it is a table nested in the panel's
        // cell, and a cell with no shading shows the panel's through it.
        for (int i = 0; i < node.children().size(); i++) {
            XWPFTableCell cell = row.getCell(i);
            cell.removeParagraph(0);
            DocumentNode child = node.children().get(i);
            // What the cell holds is sized to the cell, not to whatever surrounds the row.
            double previous = currentCellWidth;
            currentCellWidth = usableWidthOf(cell, i, 1);
            try {
                writeRowCellChild(cell, child);
            } finally {
                currentCellWidth = previous;
            }
            applyRowVerticalAlign(cell, node.verticalAlign());
        }
        indentTable(table);
    }

    /**
     * Writes where a row's children sit in its height.
     *
     * <p>The layout places a child shorter than its row at the row's top, middle or bottom,
     * and a cell holds its content at the top unless told otherwise. A table of contents
     * aligns its entries to the bottom so the leader — a line a point tall beside a line of
     * text — sits on the text's baseline; without it the leader rode at the top of the
     * entry. Top is Word's own default and is not written.</p>
     */
    private static void applyRowVerticalAlign(XWPFTableCell cell, RowVerticalAlign align) {
        if (align == RowVerticalAlign.CENTER) {
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        } else if (align == RowVerticalAlign.BOTTOM) {
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.BOTTOM);
        }
    }

    private void writeRowCellChild(XWPFTableCell cell, DocumentNode child) throws Exception {
        writeCellBody(cell, child);
    }

    /**
     * Gives the carrier the row's width, and its cells the row's slots.
     *
     * <p>A row occupies the whole width it is offered — {@code measureRow} returns the
     * available width unconditionally, whatever its children measure — so the carrier
     * gets the content width. Left at POI's size-to-content default the pair collapses
     * around its text instead.</p>
     *
     * <p>How that width divides is arithmetic the document already carries, for every
     * distribution except one. Weights, an even split and fixed columns are shares of the
     * width left after the gaps; only an {@code auto} column and the flex path ask what a
     * child's content naturally measures, which is the question this backend cannot
     * answer. So the grid is written for the first three and left to Word for the
     * others.</p>
     *
     * <p>The gap and the row's padding are not columns, and Word has nowhere to put them:
     * a table has no inter-column gap. They are folded into the neighbouring column's
     * width and taken back out as that cell's margin, so each cell's text box is exactly
     * its slot and starts exactly where the slot starts. The margins are written even
     * when they are zero, because Word's own default is not.</p>
     *
     * <p>The width used is the row's own when the layout placed it, and otherwise what the
     * containers around it leave of the page ({@link #availableWidth}); the row is moved in
     * by the same containers ({@link #indentTable}), so it starts where their text does.</p>
     */
    private void applyRowGeometry(XWPFTable table, RowNode node) {
        double[] starts = layout.rowChildStarts(node);
        if (starts != null) {
            // The layout placed each child, so every way a row can divide — the two that
            // measure their children included — is already answered.
            setTableWidth(table, starts[0]);
            writeRowColumns(table, withRowEditorSlack(node, placedColumns(node, starts)));
            return;
        }

        double available = availableWidth();
        if (!Double.isFinite(available) || available <= 0) {
            return;
        }
        setTableWidth(table, available);
        double[] slots = resolveRowSlots(node, available);
        if (slots == null) {
            return;
        }
        writeRowColumns(table, statedColumns(node, slots));
    }

    /**
     * One column of a row's carrier: the text box, and what sits either side of it inside
     * the same cell.
     *
     * @param width    the grid column's full width in points
     * @param leading  space before the text box, written as the cell's left margin
     * @param trailing space after it, written as the cell's right margin
     */
    private record CellColumn(double width, double leading, double trailing) {
    }

    /**
     * Columns from where the layout started each child.
     *
     * <p>A slot runs from its child's start to the next child's, less the gap the row puts
     * between them; the last runs to the row's edge, less its padding. That is the same
     * shape {@link #statedColumns} builds — the difference is only where the starts come
     * from, and these were measured rather than worked out.</p>
     */
    private static List<CellColumn> placedColumns(RowNode node, double[] starts) {
        int count = starts.length - 1;
        double rowWidth = starts[0];
        List<CellColumn> columns = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            double x = starts[1 + index];
            double columnStart = index == 0 ? 0.0 : x;
            double columnEnd = index == count - 1 ? rowWidth : starts[2 + index];
            double trailing = index == count - 1 ? node.padding().right() : node.gap();
            columns.add(new CellColumn(columnEnd - columnStart, x - columnStart,
                    Math.max(0.0, Math.min(trailing, columnEnd - x))));
        }
        return columns;
    }

    /**
     * Widens a row's auto columns by a point, taken from its weight columns.
     *
     * <p>An auto column is exactly as wide as its child's unwrapped content, and an editor
     * setting that content in its own substitute for the face wraps it — the reason a table's
     * auto columns get {@value #EDITOR_COLUMN_SLACK_POINTS}pt (see {@link #withEditorSlack}).
     * Measured through LibreOffice, a table of contents — auto label, weight leader, auto page
     * number — broke "Intro" into "Intr" and "o".</p>
     *
     * <p>A row is as wide as the layout placed it, so the point comes out of the weight
     * columns, which on the page take whatever the others leave: the row keeps its width, and
     * a fixed column its size. Each weight column gives in proportion to its text box and
     * never more than it has. Written as placed: a row with no auto column or no weight
     * column, and one with no stated columns — weights or an even split. A row cannot state
     * columns and be laid out as flex; {@code RowNode} refuses the pair.</p>
     *
     * <p>The point comes out of the weight column's text box, not only its empty part —
     * which the layout does not report, as a paragraph or a line fills its slot. Weight
     * columns in a row with auto columns hold leaders, rules and spacers; one holding text
     * that fills it to the point could wrap a line sooner in the editor.</p>
     */
    private static List<CellColumn> withRowEditorSlack(RowNode node, List<CellColumn> columns) {
        List<DocumentRowColumn> specs = node.columns();
        if (specs.size() != columns.size()) {
            return columns;
        }
        int autoColumns = 0;
        double spare = 0;
        for (int index = 0; index < columns.size(); index++) {
            DocumentRowColumn.Type type = specs.get(index).type();
            if (type == DocumentRowColumn.Type.AUTO) {
                autoColumns++;
            } else if (type == DocumentRowColumn.Type.WEIGHT) {
                spare += textBoxOf(columns.get(index));
            }
        }
        if (autoColumns == 0 || !(spare > 0)) {
            return columns;
        }
        double slack = Math.min(EDITOR_COLUMN_SLACK_POINTS, spare / autoColumns);
        double taken = slack * autoColumns;
        List<CellColumn> widened = new ArrayList<>(columns.size());
        for (int index = 0; index < columns.size(); index++) {
            CellColumn column = columns.get(index);
            DocumentRowColumn.Type type = specs.get(index).type();
            double change = type == DocumentRowColumn.Type.AUTO ? slack
                    : type == DocumentRowColumn.Type.WEIGHT ? -taken * textBoxOf(column) / spare
                    : 0;
            widened.add(new CellColumn(column.width() + change, column.leading(), column.trailing()));
        }
        return widened;
    }

    /** The width a row column leaves its content, between its margins. */
    private static double textBoxOf(CellColumn column) {
        return Math.max(0, column.width() - column.leading() - column.trailing());
    }

    /** Columns from the row's own arithmetic: the slot, plus the gap and padding beside it. */
    private static List<CellColumn> statedColumns(RowNode node, double[] slots) {
        List<CellColumn> columns = new ArrayList<>(slots.length);
        for (int index = 0; index < slots.length; index++) {
            double leading = index == 0 ? node.padding().left() : 0.0;
            double trailing = index == slots.length - 1 ? node.padding().right() : node.gap();
            columns.add(new CellColumn(slots[index] + leading + trailing, leading, trailing));
        }
        return columns;
    }

    /**
     * Writes a row carrier's grid, and the cell margins that hold its text box in place.
     *
     * <p>Word has no inter-column gap and no row padding, so both ride in the neighbouring
     * column's width and are taken back out as that cell's margin. The margins are written
     * even when they are zero: Word's own default is not.</p>
     */
    private static void writeRowColumns(XWPFTable table, List<CellColumn> columns) {
        CTTblGrid grid = table.getCTTbl().getTblGrid() != null
                ? table.getCTTbl().getTblGrid()
                : table.getCTTbl().addNewTblGrid();
        while (grid.sizeOfGridColArray() > 0) {
            grid.removeGridCol(0);
        }
        for (int index = 0; index < columns.size(); index++) {
            CellColumn column = columns.get(index);
            grid.addNewGridCol().setW(BigInteger.valueOf(Math.round(column.width() * POINT_TO_TWIP)));

            CTTcPr properties = cellProperties(table.getRow(0).getCell(index));
            CTTblWidth cellWidth = properties.isSetTcW() ? properties.getTcW() : properties.addNewTcW();
            cellWidth.setType(STTblWidth.DXA);
            cellWidth.setW(BigInteger.valueOf(Math.round(column.width() * POINT_TO_TWIP)));
            CTTcMar margins = properties.isSetTcMar() ? properties.getTcMar() : properties.addNewTcMar();
            setCellMargin(margins.isSetLeft() ? margins.getLeft() : margins.addNewLeft(),
                    column.leading());
            setCellMargin(margins.isSetRight() ? margins.getRight() : margins.addNewRight(),
                    column.trailing());
        }
        setFixedLayout(table);
    }

    /** Replaces a table's grid with the given column widths. */
    private static void writeGrid(XWPFTable table, double[] widths) {
        CTTblGrid grid = table.getCTTbl().getTblGrid() != null
                ? table.getCTTbl().getTblGrid()
                : table.getCTTbl().addNewTblGrid();
        while (grid.sizeOfGridColArray() > 0) {
            grid.removeGridCol(0);
        }
        for (double width : widths) {
            grid.addNewGridCol().setW(BigInteger.valueOf(Math.round(width * POINT_TO_TWIP)));
        }
    }

    /**
     * Makes the grid the answer rather than a suggestion.
     *
     * <p>Left to its default Word re-fits a table's columns to their content, which is the
     * behaviour every written grid exists to replace.</p>
     */
    private static void setFixedLayout(XWPFTable table) {
        CTTblPr properties = table.getCTTbl().getTblPr() != null
                ? table.getCTTbl().getTblPr()
                : table.getCTTbl().addNewTblPr();
        CTTblLayoutType type = properties.isSetTblLayout()
                ? properties.getTblLayout()
                : properties.addNewTblLayout();
        type.setType(STTblLayoutType.FIXED);
    }

    private static double sum(double[] values) {
        double total = 0;
        for (double value : values) {
            total += value;
        }
        return total;
    }

    /**
     * The width of each of a row's slots, or {@code null} when one of them is content's.
     *
     * <p>Mirrors {@code NodeDefinitionSupport.measureRow}: the gaps and the row's padding
     * come off the top, and what is left is split by columns, by weights, or evenly. The
     * two branches that measure — a non-START arrangement or a grow spacer, and an
     * {@code auto} column — return nothing instead.</p>
     *
     * @param node       the row being carried
     * @param outerWidth the width the row is laid out in
     * @return one width per child, or null when the split needs measuring
     */
    private static double[] resolveRowSlots(RowNode node, double outerWidth) {
        int count = node.children().size();
        if (count == 0) {
            return null;
        }
        if (node.arrangement() != RowArrangement.START || hasGrowChild(node)) {
            // The flex path gives every child without a grow factor its natural width.
            return null;
        }
        double inner = Math.max(0.0, outerWidth - node.padding().horizontal());
        double slotsTotal = Math.max(0.0, inner - node.gap() * Math.max(0, count - 1));
        double[] slots = new double[count];

        List<DocumentRowColumn> columns = node.columns();
        if (!columns.isEmpty()) {
            double used = 0.0;
            double totalWeight = 0.0;
            for (int index = 0; index < count; index++) {
                DocumentRowColumn column = columns.get(index);
                switch (column.type()) {
                    case FIXED -> {
                        slots[index] = column.value();
                        used += slots[index];
                    }
                    case WEIGHT -> totalWeight += column.value();
                    case AUTO -> {
                        return null;
                    }
                    default -> {
                        return null;
                    }
                }
            }
            double remaining = Math.max(0.0, slotsTotal - used);
            if (totalWeight > 0.0) {
                for (int index = 0; index < count; index++) {
                    if (columns.get(index).type() == DocumentRowColumn.Type.WEIGHT) {
                        slots[index] = remaining * (columns.get(index).value() / totalWeight);
                    }
                }
            }
            return slots;
        }

        List<Double> weights = node.weights();
        if (weights.isEmpty()) {
            for (int index = 0; index < count; index++) {
                slots[index] = slotsTotal / count;
            }
            return slots;
        }
        double total = 0.0;
        for (Double weight : weights) {
            total += weight;
        }
        for (int index = 0; index < count; index++) {
            slots[index] = total > 0.0 ? slotsTotal * (weights.get(index) / total) : slotsTotal / count;
        }
        return slots;
    }

    private static boolean hasGrowChild(RowNode node) {
        for (DocumentNode child : node.children()) {
            if (child instanceof SpacerNode spacer && spacer.grow() > 0.0) {
                return true;
            }
        }
        return false;
    }

    /** States one cell margin in points, so Word's own default does not apply instead. */
    private static void setCellMargin(CTTblWidth margin, double points) {
        margin.setType(STTblWidth.DXA);
        margin.setW(BigInteger.valueOf(Math.round(Math.max(0.0, points) * POINT_TO_TWIP)));
    }

    /**
     * Gives a table the width the fixed-layout render gives it, in the cases where that
     * width can be known without measuring anything.
     *
     * <p>Nothing used to write a width at all, so Word sized every table to its own
     * content while the reference spans much more — the single largest visual difference
     * between the two renders. But "as wide as the page" is not the rule the engine
     * follows either. A table with no stated width comes out as wide as its columns
     * naturally need ({@code TableLayoutSupport.resolveFinalColumnWidths}), which for an
     * {@code auto} column is the width of its widest unwrapped cell — a measurement, and
     * measuring is what this backend has no font runtime for. Writing the content width
     * there would be right for a table whose text fills the line and wrong for a table of
     * short values, in the same way the old shrink-to-fit was wrong in the other
     * direction.</p>
     *
     * <p>So a width is written when it is knowable and not otherwise: the width the
     * author stated, or the sum of the columns when every one of them is fixed. Both are
     * numbers the document already carries. A table with an {@code auto} column and no
     * stated width keeps Word's own sizing until resolved layout can supply the measured
     * widths.</p>
     */
    private void applyTableWidth(XWPFTable table, TableNode node, int columnCount) {
        double[] measured = layout.tableColumns(node, columnCount);
        if (measured != null && measured.length > 0) {
            // The layout resolved every column, an auto one included, so there is nothing
            // left to decide: write the widths it arrived at and stop Word re-fitting them —
            // with the editor's margin on the columns sized to their content.
            double[] columns = withEditorSlack(node, measured);
            writeGrid(table, columns);
            setTableWidth(table, sum(columns));
            setFixedLayout(table);
            return;
        }

        Double authored = node.width() != null && node.width() > 0 ? node.width() : null;
        List<Double> fixedColumns = fixedColumnWidths(node, columnCount);

        if (fixedColumns == null) {
            // One of the columns is as wide as its content needs. The split is Word's, and
            // so is the total unless the author stated one — or unless this table sits in a
            // cell, where leaving the total to Word is not neutral: it squeezes a nested
            // table to about one character a line.
            if (authored != null) {
                setTableWidth(table, authored);
            } else if (Double.isFinite(nestedTableWidth())) {
                setTableWidth(table, nestedTableWidth());
            }
            return;
        }

        double natural = fixedColumns.stream().mapToDouble(Double::doubleValue).sum();
        double width = authored != null ? Math.max(authored, natural) : natural;
        setTableWidth(table, width);

        double[] columns = new double[fixedColumns.size()];
        for (int index = 0; index < columns.length; index++) {
            columns[index] = fixedColumns.get(index);
        }
        // With no auto column to absorb it, the engine hands a stated width's surplus to
        // the last column. Splitting it evenly instead would put every column edge but the
        // first in a different place than the PDF draws it.
        columns[columns.length - 1] += width - natural;
        writeGrid(table, columns);
    }

    /**
     * Widens the columns sized to their content by a point, where there is room for it.
     *
     * <p>An auto column is exactly as wide as its widest unwrapped cell: the engine gives it
     * no slack, because on the page it needs none. An editor does. It sets the text in its
     * own substitute for the face and keeps a border's width clear inside the cell, and
     * either is enough to push the widest cell of a zero-slack column onto a second line —
     * measured on the probe corpus through LibreOffice, a billing table's widest item
     * wrapped and its row doubled the moment its cells were written at their true 14pt.</p>
     *
     * <p>So each auto column gets {@value #EDITOR_COLUMN_SLACK_POINTS}pt more than the page
     * gave it — below anything a reader compares by eye, and what keeps the page's line
     * breaks. Only where it costs nothing the document stated: a table with an authored
     * width keeps that width exactly, a fixed column keeps its size, and the slack never
     * takes the table past the width it sits in.</p>
     */
    private double[] withEditorSlack(TableNode node, double[] measured) {
        if (node.width() != null) {
            return measured;
        }
        List<DocumentTableColumn> specs = node.columns();
        int autoColumns = 0;
        for (int index = 0; index < measured.length; index++) {
            if (isAutoColumn(specs, index)) {
                autoColumns++;
            }
        }
        double available = Double.isFinite(nestedTableWidth()) ? nestedTableWidth() : availableWidth();
        double room = available - node.padding().horizontal() - sum(measured);
        if (autoColumns == 0 || !(room > 0)) {
            return measured;
        }
        double slack = Math.min(EDITOR_COLUMN_SLACK_POINTS, room / autoColumns);
        double[] columns = measured.clone();
        for (int index = 0; index < columns.length; index++) {
            if (isAutoColumn(specs, index)) {
                columns[index] += slack;
            }
        }
        return columns;
    }

    /** A column the author did not size is sized to its content — see {@code TableLayoutSupport}. */
    private static boolean isAutoColumn(List<DocumentTableColumn> specs, int index) {
        return index >= specs.size() || specs.get(index).type() == DocumentTableColumn.Type.AUTO;
    }

    /** The margin an editor needs on a column sized exactly to its widest cell. */
    static final double EDITOR_COLUMN_SLACK_POINTS = 1.0;

    /**
     * States a table's width in points, replacing the size-to-content default.
     *
     * <p>POI's {@code createTable} writes {@code w:tblW} as {@code w=0, type=auto}, which
     * is Word's instruction to shrink the table around whatever it holds. That is why an
     * exported table of short values came out narrow while the reference spans the text
     * column, and it applies equally to a row carried as a one-row table.</p>
     */
    private static void setTableWidth(XWPFTable table, double points) {
        CTTblPr properties = table.getCTTbl().getTblPr() != null
                ? table.getCTTbl().getTblPr()
                : table.getCTTbl().addNewTblPr();
        CTTblWidth width = properties.isSetTblW()
                ? properties.getTblW()
                : properties.addNewTblW();
        width.setType(STTblWidth.DXA);
        width.setW(BigInteger.valueOf(Math.round(points * POINT_TO_TWIP)));
    }

    /**
     * Every column's width in points, or {@code null} when one of them is not fixed.
     *
     * @param node        the table being written
     * @param columnCount positions the resolved grid actually has
     * @return the widths, or null when the split needs measuring
     */
    private static List<Double> fixedColumnWidths(TableNode node, int columnCount) {
        List<com.demcha.compose.document.table.DocumentTableColumn> columns = node.columns();
        // A grid position with no declared column has no width to write, so a table whose
        // spans reach past its column list is one of the cases Word has to divide itself.
        if (columns.size() != columnCount) {
            return null;
        }
        List<Double> widths = new ArrayList<>(columnCount);
        for (var column : columns) {
            if (column.type() != com.demcha.compose.document.table.DocumentTableColumn.Type.FIXED
                || column.fixedWidth() == null) {
                return null;
            }
            widths.add(column.fixedWidth());
        }
        return widths;
    }

    /**
     * Turns off a table's own grid, leaving each cell free to state its borders.
     *
     * <p>Used where the table is a carrier for a side-by-side layout rather than
     * something the author asked to see ruled.</p>
     *
     * <p>Each edge is replaced rather than appended to. POI's {@code createTable} already
     * writes a full set of single-line borders, and {@code addNew*} on top of them leaves
     * two elements per edge where {@code CT_TblBorders} permits one. Word reads the last
     * and draws nothing, which is why the output looked right, but the part is invalid
     * against the schema either way.</p>
     */
    private static void hideTableGrid(XWPFTable table) {
        CTTblPr properties = table.getCTTbl().getTblPr() != null
                ? table.getCTTbl().getTblPr()
                : table.getCTTbl().addNewTblPr();
        CTTblBorders borders = properties.isSetTblBorders()
                ? properties.getTblBorders()
                : properties.addNewTblBorders();
        paintEdge(borders.isSetTop() ? borders.getTop() : borders.addNewTop(),
                STBorder.NONE, null, null);
        paintEdge(borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom(),
                STBorder.NONE, null, null);
        paintEdge(borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft(),
                STBorder.NONE, null, null);
        paintEdge(borders.isSetRight() ? borders.getRight() : borders.addNewRight(),
                STBorder.NONE, null, null);
        paintEdge(borders.isSetInsideH() ? borders.getInsideH() : borders.addNewInsideH(),
                STBorder.NONE, null, null);
        paintEdge(borders.isSetInsideV() ? borders.getInsideV() : borders.addNewInsideV(),
                STBorder.NONE, null, null);
    }

    /**
     * Writes {@code child} into an emptied cell and leaves a paragraph behind either way.
     *
     * <p>A {@code w:tc} must hold at least one block-level element. POI puts a paragraph in
     * every cell it creates and the callers here remove it before writing their own, so a
     * node that contributes nothing — a wrapper that ended up with no children — would
     * otherwise leave the cell with no block child at all. Word tolerates less of that than
     * the schema validator notices.</p>
     */
    private void writeCellBody(XWPFTableCell cell, DocumentNode child) throws Exception {
        writeCellNode(cell, child);
        if (cell.getParagraphs().isEmpty()) {
            cell.addParagraph();
        }
    }

    /**
     * Writes a node into a table cell.
     *
     * <p>A wrapper contributes nothing of its own to a Word cell, so its children are
     * written in its place rather than the wrapper being dropped with them inside.</p>
     */
    /**
     * Whether a node exists only to say something about geometry, and so has nothing of its
     * own to write here.
     *
     * <p>A layout anchor reports where its child landed, an alignment says where in the
     * available width to put it, and a horizontal-bands wrapper publishes the columns of the
     * row it holds. Word lays text out itself, so none has an analogue — but each has exactly
     * one child, and dropping a wrapper takes the content with it: a timeline with its
     * markers on the rail lost every entry's header that way.</p>
     *
     * @param node the node being written
     * @return true when the node itself writes nothing and its children should be written
     */
    private static boolean isSemanticallyTransparent(DocumentNode node) {
        return node instanceof com.demcha.compose.document.layout.LayoutAnchorNode
               || node instanceof com.demcha.compose.document.node.AlignNode
               || node instanceof com.demcha.compose.document.layout.HorizontalBandsNode;
    }

    /**
     * Writes content the layout laid out inside a column another node resolved.
     *
     * <p>A timeline whose markers sit on the rail wraps each entry's header row so its columns
     * are published, and lays the entry's body out in the content column, below the row
     * rather than in it — a row cannot cross a page, and a body can be longer than one. Both
     * wrappers were unknown here and dropped as drawing, taking the entries' titles, dates and
     * text with them. The row is written as the row it wraps; the body is written in the flow
     * and indented to its column, where the layout placed it, as an enclosing container's
     * margin indents what is inside it.</p>
     */
    private void writeInBand(XWPFDocument document,
                             com.demcha.compose.document.layout.HorizontalBandContentNode band) throws Exception {
        double[] sides = layout.insideParent(band);
        double outerLeft = insetLeft;
        double outerRight = insetRight;
        if (sides != null) {
            insetLeft += sides[0];
            insetRight += sides[1];
        }
        try {
            writeNode(document, band.child());
        } finally {
            insetLeft = outerLeft;
            insetRight = outerRight;
        }
    }

    /**
     * Creates a table where the writer is currently pointing — the body, or a cell.
     *
     * <p>A table inside a cell is a real nested {@code w:tbl}, not a flattened copy of its
     * text. Word requires a cell to end with a paragraph, and a table is not one, so an
     * empty paragraph follows it: without that the cell is malformed and Word refuses the
     * file rather than showing the table.</p>
     *
     * @param document the document being written
     * @param rows     row count
     * @param columns  column count of the first row
     * @return the created table, already attached where it belongs
     */
    /**
     * How wide content can be inside one cell: the columns it spans, less its own margins.
     *
     * <p>Both are read back from what this export just wrote — the grid for the width and
     * {@code w:tcMar} for the margins — so a cell cannot disagree with the table it is in,
     * and a table whose padding the document stated is not measured against Word's.</p>
     *
     * @return the usable width in points, or {@code NaN} when the table has no written grid
     */
    private static double usableWidthOf(XWPFTableCell cell, TableGrid.Placement placement) {
        return usableWidthOf(cell, placement.column(), placement.colSpan());
    }

    private static double usableWidthOf(XWPFTableCell cell, int column, int span) {
        CTTblGrid grid = cell.getTableRow().getTable().getCTTbl().getTblGrid();
        if (grid == null || grid.sizeOfGridColArray() == 0) {
            return Double.NaN;
        }
        double twips = 0;
        int last = Math.min(column + span, grid.sizeOfGridColArray());
        for (int index = column; index < last; index++) {
            Long written = writtenTwips(grid.getGridColArray(index).getW());
            if (written == null) {
                // A column this export did not write as plain twips has no width to add up.
                return Double.NaN;
            }
            twips += written;
        }
        double points = twips / POINT_TO_TWIP - horizontalMarginsOf(cell);
        return points > 0 ? points : Double.NaN;
    }

    /**
     * The width a table nested in the current cell may take, or {@code NaN} outside a cell.
     *
     * <p>The column it sits in, less the margins Word keeps inside every cell. It is not
     * the width the page gives that table — the layout reports a composed cell's content
     * under the owner's path, so which measured row belongs to which nested table cannot be
     * told apart there — but it is a width, and a nested table without one is squeezed by
     * Word to about one character per line, which is not a document anybody can read.</p>
     */
    private double nestedTableWidth() {
        return currentCellWidth;
    }

    private XWPFTable newTable(XWPFDocument document, int rows, int columns) {
        if (currentCell == null ? endsWithATable(document.getBodyElements()) : cellEndsWithItsTableCloser()) {
            separateFromTheTableAbove(document);
        }
        // Word has no space above a table, so the paragraph before it has to carry it.
        flushSpacingAfter();
        // Nor can that paragraph carry the space below the table: it sits above it. Space
        // owed once the table is written goes to whatever paragraph follows, as space above
        // it — and nowhere, if nothing follows — rather than back above the table, which is
        // where it used to land: a card's bottom padding opened a gap over its last table.
        lastBodyParagraph = null;
        if (currentCell == null) {
            return document.createTable(rows, columns);
        }
        XWPFTable nested = new XWPFTable(currentCell.getCTTc().addNewTbl(), currentCell, rows, columns);
        // The XML already carries the table; this is what tells the cell's own lists about
        // it, so reading the cell back finds it. getTables() is unmodifiable on purpose —
        // adding to it throws rather than quietly leaving the model and the XML disagreeing.
        currentCell.insertTable(currentCell.getBodyElements().size(), nested);
        // Word ends a cell with a paragraph, so one follows the nested table — and being below
        // it, it is where space owed after the table goes: a padded card ending in a nested
        // table keeps its bottom padding inside the cell, as the page does. It holds no text,
        // and at a line's height it put an empty line under every table in a card, so it is
        // a hairline; the paragraph written next in the cell takes it over (see
        // newBodyParagraph), and a table written next makes it the separator between the two.
        XWPFParagraph closer = currentCell.addParagraph();
        holdToHairline(closer);
        tableCloser = closer;
        lastBodyParagraph = closer;
        return nested;
    }

    /**
     * Moves a table in from the page margin by what the containers around it hold in, as
     * its paragraphs are: a row or a table inside a padded section starts where the
     * section's text starts, not at the page margin beside it.
     *
     * <p>Called once the table is written, because the editor measures {@code w:tblInd} to
     * the first cell's text rather than to the table's edge: measured in LibreOffice, a
     * table indented by the inset alone drew its border the first cell's margin short of the
     * section's text. So the first cell's written margin is added, and the border lands at
     * the inset, where the page draws it. A cell's own content is not indented this way —
     * inside a cell the inset is always zero.</p>
     */
    private void indentTable(XWPFTable table) {
        if (insetLeft <= 0 || table.getRows().isEmpty() || table.getRow(0).getTableCells().isEmpty()) {
            return;
        }
        XWPFTableCell first = table.getRow(0).getCell(0);
        CTTcPr cellProperties = first.getCTTc().isSetTcPr() ? first.getCTTc().getTcPr() : null;
        CTTcMar margins = cellProperties != null && cellProperties.isSetTcMar() ? cellProperties.getTcMar() : null;
        // A table nested in a cell is placed by its edge, so nothing is added there.
        double firstCellMargin = currentCell != null
                ? 0
                : margins == null
                ? WORD_DEFAULT_CELL_MARGIN_POINTS
                : marginPoints(margins.isSetLeft() ? margins.getLeft() : null);
        var properties = table.getCTTbl().getTblPr() != null
                ? table.getCTTbl().getTblPr()
                : table.getCTTbl().addNewTblPr();
        CTTblWidth indent = properties.isSetTblInd() ? properties.getTblInd() : properties.addNewTblInd();
        indent.setType(STTblWidth.DXA);
        indent.setW(BigInteger.valueOf(toTwips(insetLeft + firstCellMargin)));
    }

    private static boolean endsWithATable(List<IBodyElement> elements) {
        return !elements.isEmpty() && elements.get(elements.size() - 1) instanceof XWPFTable;
    }

    /**
     * Puts a paragraph between a table and the one about to follow it.
     *
     * <p>Two tables with nothing between them are one table to Word and to LibreOffice: the
     * editor joins them, and the second table's rows are laid out on the first one's column
     * grid. Measured in LibreOffice, a zebra table followed by a narrower one came out at half
     * its width, its text broken letter by letter. A table, or a row carried as one, is often
     * followed by another, and on the page there is a gap between them.</p>
     *
     * <p>So the gap is written as a paragraph: a tenth of a point tall, with the rest of the
     * space the layout keeps between the two above it, so the second table starts where the
     * page starts it. Measured in LibreOffice, a one-point separator put 0.9pt more between two
     * touching tables than a tenth of a point does; below that the height stops mattering.
     * Word may hold a line that short to its own minimum, which is still under a point.</p>
     *
     * <p>It keeps with the next table, so a page never breaks between the gap and what it
     * opens — a block kept with the table below it stays kept, and a bookmark opened on the
     * separator counts the page the table lands on.</p>
     *
     * <p>In a cell the paragraph is already there: the one closing the table above (see
     * {@link #newTable}), which becomes the separator.</p>
     */
    private void separateFromTheTableAbove(XWPFDocument document) {
        pendingSpacingAfter = Math.max(0, pendingSpacingAfter - SEPARATOR_POINTS);
        XWPFParagraph separator;
        if (currentCell != null) {
            separator = tableCloser;
            tableCloser = null;
        } else {
            separator = newBodyParagraph(document);
            holdToHairline(separator);
        }
        CTPPr properties = separator.getCTP().isSetPPr()
                ? separator.getCTP().getPPr()
                : separator.getCTP().addNewPPr();
        if (!properties.isSetKeepNext()) {
            properties.addNewKeepNext();
        }
    }

    /** Makes a paragraph that holds no text as short as a separator between tables. */
    private static void holdToHairline(XWPFParagraph para) {
        CTPPr properties = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        CTSpacing spacing = properties.isSetSpacing() ? properties.getSpacing() : properties.addNewSpacing();
        spacing.setLineRule(STLineSpacingRule.EXACT);
        spacing.setLine(BigInteger.valueOf(Math.round(SEPARATOR_POINTS * POINT_TO_TWIP)));
    }

    /**
     * Whether the cell being filled ends with the paragraph that closes its last table, so the
     * next thing written there follows the table directly.
     */
    private boolean cellEndsWithItsTableCloser() {
        if (currentCell == null || tableCloser == null) {
            return false;
        }
        List<IBodyElement> elements = currentCell.getBodyElements();
        return !elements.isEmpty() && elements.get(elements.size() - 1) == tableCloser;
    }

    /** How tall the paragraph keeping two tables apart is. */
    private static final double SEPARATOR_POINTS = 0.1;

    /**
     * Writes one node into a cell, through the same writers that write it anywhere else.
     *
     * <p>A cell used to have a dispatcher of its own, and it had learned about paragraphs
     * and about the wrappers a paragraph can sit in — so a cell built from an image or a
     * list was warned about and left empty, silently losing content the page draws. The
     * cell is now a <em>destination</em> instead: {@link #newBodyParagraph} points at it,
     * and {@link #writeNode} does the rest, which is how everything that can be written at
     * all can be written here.</p>
     *
     * <p>The destination is restored afterwards rather than cleared, because a cell can
     * hold a table whose cells hold their own content, and the inner one must not leave
     * the outer one writing into the body.</p>
     */
    private void writeCellNode(XWPFTableCell cell, DocumentNode child) throws Exception {
        writeCellNodes(cell, List.of(child));
    }

    /** Writes several nodes into a cell, one after another, as a block of the cell's own. */
    private void writeCellNodes(XWPFTableCell cell, List<DocumentNode> children) throws Exception {
        XWPFTableCell previousCell = currentCell;
        XWPFParagraph previousParagraph = lastBodyParagraph;
        double previousCarried = carriedSpacingBefore;
        double previousOwed = pendingSpacingAfter;
        double previousInsetLeft = insetLeft;
        double previousInsetRight = insetRight;
        XWPFParagraph previousCloser = tableCloser;
        currentCell = cell;
        lastBodyParagraph = null;
        tableCloser = null;
        carriedSpacingBefore = 0;
        pendingSpacingAfter = 0;
        // A cell's content is measured from the cell's own edge, which its margins already
        // keep clear of the border; the containers around the table have nothing to add.
        insetLeft = 0;
        insetRight = 0;
        try {
            for (DocumentNode child : children) {
                writeNode(cell.getXWPFDocument(), child);
            }
            // A cell ends where it ends: its last gap cannot land on whatever the body
            // writes next, and the body's cannot land inside it.
            flushSpacingAfter();
        } finally {
            currentCell = previousCell;
            lastBodyParagraph = previousParagraph;
            carriedSpacingBefore = previousCarried;
            pendingSpacingAfter = previousOwed;
            insetLeft = previousInsetLeft;
            insetRight = previousInsetRight;
            tableCloser = previousCloser;
        }
    }

    private void writeSpacer(XWPFDocument document, SpacerNode node) {
        XWPFParagraph para = newBodyParagraph(document);
        para.createRun().setText("");
        owePendingSpacingAfter(node.height());
    }

    private void writePageBreak(XWPFDocument document) {
        flushSpacingAfter();
        XWPFParagraph para = document.createParagraph();
        XWPFRun run = para.createRun();
        run.addBreak(BreakType.PAGE);
    }

    /**
     * Marks a run as right-to-left text.
     *
     * <p>{@code w:bidi} on the paragraph settles which edge the line starts from. It does
     * not settle how Word resolves the characters inside a run: without {@code w:rtl} the
     * run is handled as Latin, and paired punctuation is not mirrored — measured in Word,
     * an Arabic cell ending in {@code (2026)} drew it as {@code )2026(} while the same
     * document as a PDF was correct. It is the run-level half of the same pair
     * {@code w:szCs} belongs to: Word takes complex scripts from properties of their own,
     * and a run that does not declare itself one gets the treatment Latin gets.</p>
     */
    private static void applyRunDirection(XWPFRun run, boolean rightToLeft) {
        if (!rightToLeft) {
            return;
        }
        CTRPr properties = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
        // The schema models w:rtl as a repeating element, so there is no isSet — an empty
        // element is the "on" form, and adding a second would be writing the flag twice.
        if (properties.sizeOfRtlArray() == 0) {
            properties.addNewRtl();
        }
    }

    /**
     * Writes tracking as Word's own run-level {@code w:spacing}, never as spaces
     * pushed into the text.
     *
     * <p>The unit is twentieths of a point. Measured rather than assumed:
     * exporting a probe document through Word itself and reading the glyph
     * positions out of the PDF it wrote, {@code w:spacing w:val="100"} widened
     * every step of {@code "JANE"} by 5.0pt — including the step onto a
     * following untracked run, which is the trailing unit — and {@code "-30"}
     * narrowed each by 1.5pt, with an ordinary space spaced like any other
     * character. Word spends the value the same way the PDF {@code Tc} operator
     * does.</p>
     *
     * <p>This is the one place the backend resolves the public unit itself: a
     * semantic export never passes through the engine's text style, which is
     * where a fixed-layout backend would have had it resolved already. Word owns
     * the layout here, so the contract is that the asked-for tracking arrives as
     * the right native value — not that any x coordinate matches the PDF.
     * Twentieths of a point quantise to 0.05pt, which is the format's own
     * granularity and not something to work around.</p>
     *
     * <p>No tracking writes no element, so a document that never asks for it
     * carries exactly the run properties it carried before.</p>
     *
     * <p>Out of range is refused rather than wrapped. The value goes out as an
     * {@code int} of twentieths, and a large enough tracking changes sign on the
     * cast &mdash; {@code 1e9} points becomes {@code -1474836480}, turning wide
     * tracking into tight. The limit is Word's, not the fixed backends': a
     * semantic document is not held to what DrawingML can spell.</p>
     */
    private static void applyLetterSpacing(XWPFRun run, DocumentTextStyle style) {
        double points = style.letterSpacing().resolve(style.size());
        if (points == 0.0) {
            return;
        }
        if (Math.abs(points) > MAX_TRACKING_POINTS) {
            throw new IllegalArgumentException(
                    "Letter spacing resolves to " + points + "pt, beyond the "
                            + MAX_TRACKING_POINTS + "pt a Word run can express "
                            + "(w:spacing is twentieths of a point, written as an int).");
        }
        run.setCharacterSpacing((int) Math.round(points * 20.0));
    }

    /**
     * The largest tracking that survives the conversion, in points &mdash; the
     * point at which twentieths stop fitting in the {@code int} the value is
     * written as. Not a typographic limit: Word renders nothing remotely near
     * it, and this exists only so an absurd value fails loudly instead of
     * wrapping into a negative.
     */
    private static final double MAX_TRACKING_POINTS = Integer.MAX_VALUE / 20.0;

    private void applyStyle(XWPFRun run, DocumentTextStyle style) {
        if (style == null) {
            return;
        }
        // A run that only restates the Normal style is left saying nothing, so Word's own
        // "change the Normal style" reaches it. Written out, the direct property wins over
        // the style and a global restyle silently does nothing — which is what this
        // exporter used to produce for every run in every document.
        DocumentTextStyle defaults = documentDefaultStyle;
        String family = wordFamilyOf(style.fontName());
        if (family != null
            && (defaults == null || !family.equals(wordFamilyOf(defaults.fontName())))) {
            run.setFontFamily(family);
        }
        // Complex-script size rides along with the ordinary one, so it is skipped for the
        // same reason when the style already carries it.
        // Compared as they are written, in half-points, rather than as raw doubles: two
        // sizes Word cannot tell apart must not produce a redundant direct w:sz.
        boolean sizeComesFromTheStyle = defaults != null
                && Math.round(style.size() * HALF_POINTS_PER_POINT)
                   == Math.round(defaults.size() * HALF_POINTS_PER_POINT);
        if (style.size() > 0 && !sizeComesFromTheStyle) {
            // Passed as a double, because w:sz counts half-points and rounding to whole
            // points first throws away a precision the format has: the timeline's 8.5pt
            // label was being written as 9pt.
            run.setFontSize(style.size());
            // Word sizes complex-script characters — Hebrew, Arabic — from w:szCs and not
            // from w:sz, so a run carrying both scripts draws its Latin at the asked size
            // and everything else at Word's own default until this is written too.
            run.setComplexScriptFontSize(style.size());
        }
        applyLetterSpacing(run, style);
        applyRunColourAndDecoration(run, style, defaults);
    }

    /** Colour and face, with the colour skipped when the Normal style already says it. */
    private void applyRunColourAndDecoration(XWPFRun run,
                                             DocumentTextStyle style,
                                             DocumentTextStyle defaults) {
        if (style.color() != null
            && (defaults == null || defaults.color() == null
                // By channel, not by instance: DocumentColor defines no equals, so two
                // colours built from the same channels are unequal unless they are the
                // same object, and a style built inline per paragraph would keep writing
                // a colour the Normal style already says.
                || style.color().color().getRGB() != defaults.color().color().getRGB())) {
            run.setColor(toHexColor(style.color().color()));
        }
        if (style.decoration() != null) {
            switch (style.decoration()) {
                case BOLD -> setBold(run);
                case ITALIC -> setItalic(run);
                case BOLD_ITALIC -> {
                    setBold(run);
                    setItalic(run);
                }
                case UNDERLINE ->
                        run.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);
                case STRIKETHROUGH -> run.setStrikeThrough(true);
                // DEFAULT carries no face of its own and is the only one left.
                default -> {
                }
            }
        }
    }

    private void applyPageGeometry(XWPFDocument document, LayoutCanvas canvas) {
        if (sectioned) {
            // Each section of a multi-section document counts its pages from 1, as the
            // section's own footer does on the page; Word would otherwise carry the count on.
            bodySectPr(document).addNewPgNumType().setStart(BigInteger.ONE);
        }
        if (canvas == null) {
            return;
        }
        CTSectPr sectPr = bodySectPr(document);
        CTPageSz pageSize = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(toTwips(canvas.width())));
        pageSize.setH(BigInteger.valueOf(toTwips(canvas.height())));
        // Word draws the page from w and h, but reads the orientation from w:orient — for
        // Page Setup, for printing, for the paper tray. A landscape page stated as portrait
        // opens the right shape and prints on its side. A square page is portrait, as Word
        // itself treats one.
        pageSize.setOrient(canvas.width() > canvas.height()
                ? STPageOrientation.LANDSCAPE
                : STPageOrientation.PORTRAIT);

        CTPageMar margin = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        margin.setTop(BigInteger.valueOf(toTwips(canvas.margin().top())));
        margin.setBottom(BigInteger.valueOf(toTwips(canvas.margin().bottom())));
        margin.setLeft(BigInteger.valueOf(toTwips(canvas.margin().left())));
        margin.setRight(BigInteger.valueOf(toTwips(canvas.margin().right())));
    }

    private static long toTwips(double points) {
        return Math.max(0, Math.round(points * POINT_TO_TWIP));
    }

    private static String toHexColor(java.awt.Color color) {
        if (color == null) {
            return "000000";
        }
        return String.format("%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Sets bold on both the ordinary and the complex-script slot.
     *
     * <p>{@code w:b} does not reach Hebrew or Arabic; {@code w:bCs} is what does. Written
     * apart, a bold right-to-left run comes out at book weight.</p>
     */
    private static void setBold(XWPFRun run) {
        run.setBold(true);
        run.setComplexScriptBold(true);
    }

    /** Sets italic on both slots, for the reason {@link #setBold(XWPFRun)} gives. */
    private static void setItalic(XWPFRun run) {
        run.setItalic(true);
        run.setComplexScriptItalic(true);
    }

    /**
     * Maps a page alignment onto the one Word will draw, given the paragraph's direction.
     *
     * <p>Word reads {@code w:jc}'s {@code left} and {@code right} as the <em>start</em> and
     * <em>end</em> of the text flow rather than as edges of the page. In a {@code w:bidi}
     * paragraph the flow starts at the right, so writing the alignment the page resolved —
     * flush right for a right-to-left paragraph — told Word to align to the flow's end and
     * drew it flush left, the one place it could not belong. Swapping the two for such a
     * paragraph is what makes the written value mean what it says.</p>
     *
     * @param align        the page's resolved alignment, or {@code null} for the default
     * @param rightToLeft  whether the paragraph is laid out right to left
     * @return the alignment to write
     */
    private static ParagraphAlignment toAlignment(TextAlign align, boolean rightToLeft) {
        ParagraphAlignment resolved = align == null ? ParagraphAlignment.LEFT : switch (align) {
            case CENTER -> ParagraphAlignment.CENTER;
            case RIGHT -> ParagraphAlignment.RIGHT;
            default -> ParagraphAlignment.LEFT;
        };
        if (!rightToLeft) {
            return resolved;
        }
        return switch (resolved) {
            case LEFT -> ParagraphAlignment.RIGHT;
            case RIGHT -> ParagraphAlignment.LEFT;
            default -> resolved;
        };
    }
}
