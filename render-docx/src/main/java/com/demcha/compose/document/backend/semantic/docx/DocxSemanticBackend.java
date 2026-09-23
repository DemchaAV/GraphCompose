package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.backend.semantic.SemanticSection;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.NumberFormatSpec;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.document.layout.DocumentGraph;
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
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.InternalLinkTarget;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.node.RowArrangement;
import com.demcha.compose.document.node.RowNode;
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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr;
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
 * {@code graph-compose-render-pdf} at runtime scope — opening a session resolves
 * the font-metrics provider only that module publishes. Adding that one artifact
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
    // Fills and borders of the containers currently being written into, innermost first.
    // A paragraph carries the innermost one, because that is the panel it sits in.
    private final java.util.Deque<ContainerPaint> containerPaint = new java.util.ArrayDeque<>();
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
    // Where the finished report goes, when the caller configured somewhere for it to go.
    private final java.util.function.Consumer<DocxExportReport> reportSink;
    // The instant every clock in the package is pinned to, or null for live timestamps.
    private final Instant deterministicTimestamp;

    /** The instant deterministic output pins to by default — the PDF and PPTX backends' own. */
    private static final Instant DEFAULT_DETERMINISTIC_INSTANT = Instant.parse("2000-01-01T00:00:00Z");

    /**
     * A container's paint, reduced to what a Word paragraph can carry.
     *
     * @param fill    background, written as {@code w:shd}
     * @param borders per-side strokes, written as {@code w:pBdr}
     */
    private record ContainerPaint(DocumentColor fill, DocumentBorders borders) {

        /** @return true when there is nothing for a paragraph to carry */
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
        containerPaint.clear();
        listNumbering.clear();
        report = new DocxExportReport.Builder();
        bookmarkNames = new DocxBookmarkNames();
        headingLevels = headingLevelsIn(whole);
        wordFamilies = DocxFontTable.familiesByName(fonts);
        documentDefaultStyle = dominantTextStyle(whole);
        currentCell = null;
        currentCellWidth = Double.NaN;
        boolean anEarlierHeader = false;
        boolean anEarlierFooter = false;
        try (XWPFDocument document = new XWPFDocument()) {
            for (int index = 0; index < sections.size(); index++) {
                SemanticSection section = sections.get(index);
                SemanticExportContext context = section.context();
                if (index > 0) {
                    endSection(document);
                }
                beginSection(section);
                applyPageGeometry(document, context.canvas());
                if (index == 0) {
                    writeStylesPart(document);
                    DocxFontTable.write(document, whole, fonts, report);
                    applyMetadata(document, metadataOf(sections));
                }
                XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(document, bodySectPr(document));
                java.util.Set<DocumentHeaderFooterZone> written =
                        applyPageZones(document, policy, context.outputOptions().zones());
                boolean header = written.contains(DocumentHeaderFooterZone.HEADER);
                boolean footer = written.contains(DocumentHeaderFooterZone.FOOTER);
                // Word repeats the previous section's header and footer in a section that has
                // none of its own; the page this section draws has none, so it says so.
                if (!header && anEarlierHeader) {
                    policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT).createParagraph();
                }
                if (!footer && anEarlierFooter) {
                    policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT).createParagraph();
                }
                anEarlierHeader |= header;
                anEarlierFooter |= footer;
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
    private void beginSection(SemanticSection section) {
        SemanticExportContext context = section.context();
        layout = DocxLayoutMetrics.of(section.graph(), context.layoutGraph());
        if (layout.isEmpty()) {
            // Said once per section: without measurements the line height is Word's and so is
            // every auto column, and a caller comparing this file against the rendered page
            // deserves to know that before they look.
            report.add(DocxExportReport.Severity.APPROXIMATED, "measured geometry", null,
                    "this document could not be laid out, so line heights and auto column "
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
     * this section move onto its last paragraph and the body starts again empty. When the
     * section ends in a table there is no paragraph to carry them, and an empty one is
     * added: Word does not end a section on a table.</p>
     */
    private void endSection(XWPFDocument document) {
        CTBody body = document.getDocument().getBody();
        CTSectPr finished = (CTSectPr) bodySectPr(document).copy();
        List<IBodyElement> elements = document.getBodyElements();
        XWPFParagraph carrier = !elements.isEmpty()
                                && elements.get(elements.size() - 1) instanceof XWPFParagraph last
                ? last
                : document.createParagraph();
        CTPPr properties = carrier.getCTP().isSetPPr()
                ? carrier.getCTP().getPPr()
                : carrier.getCTP().addNewPPr();
        properties.setSectPr(finished);
        body.unsetSectPr();
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
     * <p>A page predicate is the other fixed-layout-only piece: {@code appliesTo}
     * tests a page, and no page exists here to test — Word owns pagination. The
     * zone is written on every page rather than silently skipped, content beating
     * absence, and the export says on the log what it could not honor.</p>
     *
     * @return the kinds of zone written, so a later section knows what it has to blank out
     */
    private java.util.Set<DocumentHeaderFooterZone> applyPageZones(XWPFDocument document,
                                                                  XWPFHeaderFooterPolicy policy,
                                                                  List<DocumentPageZone> zones) {
        // The page height the zones are measured against is the canvas's, which is what the
        // page geometry was written from — not a value parsed back out of the XML.
        java.util.Set<DocumentHeaderFooterZone> written =
                java.util.EnumSet.noneOf(DocumentHeaderFooterZone.class);
        if (zones == null || zones.isEmpty()) {
            return written;
        }
        for (int index = 0; index < zones.size(); index++) {
            DocumentPageZone zone = zones.get(index);
            if (zone.getAppliesTo() != null) {
                LOG.warn("docx.zone.pagePredicate zone={} — appliesTo cannot be evaluated in a"
                        + " semantic export: Word paginates the document, so there is no page to"
                        + " test. The zone is written on every page; per-page chrome needs a"
                        + " fixed-layout backend.", zone.getZone());
            }
            DocumentNode content = zone.getContent() == null
                    ? null
                    : zone.getContent().apply(PageContext.unpaginated());
            if (content == null) {
                continue;
            }
            boolean header = zone.getZone() == DocumentHeaderFooterZone.HEADER;
            XWPFHeaderFooter target = header
                    ? policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT)
                    : policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);
            writeZoneLine(target, content);
            placeZone(document, zone, index, header);
            written.add(zone.getZone());
        }
        return written;
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
        XWPFRun run = new XWPFRun(simple.addNewR(), para);
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
        if (!keepTogether && !keepWithNext) {
            writeNodeContent(document, node);
            return;
        }
        int first = document.getBodyElements().size();
        writeNodeContent(document, node);
        keepOnOnePage(document.getBodyElements().subList(first, document.getBodyElements().size()),
                keepWithNext);
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
        if (node instanceof ParagraphNode paragraph) {
            writeParagraph(document, paragraph);
        } else if (node instanceof ImageNode image) {
            writeImage(document, image);
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
        } else if (node instanceof ContainerNode || node instanceof SectionNode
                   || node instanceof com.demcha.compose.document.node.LayerStackNode
                   || node instanceof com.demcha.compose.document.node.CanvasLayerNode
                   || isSemanticallyTransparent(node)) {
            // Overlay/positioned wrappers have no DOCX analogue for their
            // geometry, but their children can be semantic (text, images) —
            // render them sequentially rather than dropping the subtree.
            // A fill or a border is the exception: Word paragraphs carry both, so a
            // panel travels with the paragraphs inside it instead of disappearing.
            writeContainerChildren(document, node);
        } else {
            // Geometry-only node kinds (line, ellipse, shape, path, polygon,
            // barcode) have no semantic Word analogue. Warn once per kind so a
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
     * {@code inlineTextRuns()} keeps text and highlight chips but drops
     * image / shape / SVG runs (emoji lower to SVG runs, so they drop too);
     * without this the paragraph text survives while its icons vanish with
     * no signal at all, weaker than the block-level drop path.
     */
    private void warnDroppedInlineRuns(ParagraphNode node) {
        warnDroppedInlineRuns(node.inlineRuns(), layout.pathOf(node));
    }

    private void warnDroppedInlineRuns(List<InlineRun> runs, String path) {
        for (InlineRun run : runs) {
            if (run instanceof InlineTextRun || run instanceof InlineHighlightRun) {
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
                        com.demcha.compose.document.node.ListItem.of(normalized), 0, lineHeight,
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
            writeRichListLine(document, list.textStyle(), marker, item, depth, lineHeight,
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
        }
        XWPFRun run = para.createRun();
        applyStyle(run, style);
        run.setText(numId != null ? text : "  ".repeat(depth) + text);
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
     * that draws a disc or an icon has no Word analogue at all, so it drops with
     * the export's usual per-kind warning and its item is written unmarked —
     * rather than substituting a glyph the author did not ask for.</p>
     */
    private void writeRichListLine(XWPFDocument document, DocumentTextStyle style,
                                   com.demcha.compose.document.node.ListMarker marker,
                                   com.demcha.compose.document.node.ListItem item,
                                   int depth,
                                   java.util.OptionalDouble lineHeight,
                                   String path) {
        warnDroppedInlineRuns(marker.runs(), path);
        warnDroppedInlineRuns(item.runs(), path);
        spaceBeforeTheNextItem();
        XWPFParagraph para = newBodyParagraph(document);
        applyLineHeight(para, lineHeight);
        XWPFRun leading = para.createRun();
        applyStyle(leading, style);
        leading.setText("  ".repeat(depth) + (marker.isRich() ? "" : marker.prefix()));
        if (marker.isRich()) {
            writeInlineTextRuns(para, style, marker.runs(), path);
            // The gap after a marker is markerGap, which is geometry and so not
            // available here; a space is what separates a marker from its item on
            // the text path, and it separates them here for the same reason.
            if (!com.demcha.compose.document.node.InlineRun.plainText(marker.runs()).isBlank()) {
                XWPFRun gap = para.createRun();
                applyStyle(gap, style);
                gap.setText(" ");
            }
        }
        if (item.isRich()) {
            writeInlineTextRuns(para, style, item.runs(), path);
        } else {
            XWPFRun label = para.createRun();
            applyStyle(label, style);
            label.setText(item.label());
        }
    }

    /**
     * Appends one Word run per text-carrying inline run, each in its own style
     * and falling back to {@code style} when it has none — and, for a chip, on the
     * fill it was given: a badge inside a list item is a badge for the same reason
     * it is one inside a paragraph.
     */
    private void writeInlineTextRuns(XWPFParagraph para, DocumentTextStyle style,
                                     List<InlineRun> runs, String path) {
        for (InlineRun run : runs) {
            InlineTextRun text = textOf(run);
            if (text == null) {
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
     * Writes a wrapper's children, carrying its fill and borders down to each paragraph.
     *
     * <p>Word has no box to put around a run of paragraphs, but it does shade and border
     * each one, and consecutive paragraphs sharing a fill render as a single band. That is
     * close enough to a panel to be worth having, and much better than what this exporter
     * used to do, which was to drop the paint without saying so.</p>
     *
     * <p>What does not survive: the corner radius, because Word paragraph shading is
     * rectangular, and the container's padding, because a paragraph's shading hugs its own
     * text. The radius is warned about once per export rather than pretended away.</p>
     */
    private void writeContainerChildren(XWPFDocument document, DocumentNode node) throws Exception {
        ContainerPaint paint = paintOf(node);
        if (paint.isEmpty()) {
            writeContainerBody(document, node);
            return;
        }
        warnContainerRadiusDropped(node);
        containerPaint.push(paint);
        try {
            writeContainerBody(document, node);
        } finally {
            containerPaint.pop();
        }
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
        for (DocumentNode child : node.children()) {
            writeNode(document, child);
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
                    "Word paragraph shading is rectangular, so the panel keeps its fill and "
                    + "loses its rounded corners");
        }
        if (rounded && containerRadiusWarned.compareAndSet(false, true)) {
            LOG.warn("docx.export.container-radius-dropped node='{}' — Word paragraph shading "
                     + "is rectangular, so the panel renders with square corners. "
                     + "(One warning per export; use the PDF backend for the rounded form.)",
                    node.nodeKind());
        }
    }

    /**
     * Creates a body paragraph already wearing the panel it sits in.
     *
     * <p>Every paragraph written straight into the body goes through here, so a
     * container's paint cannot be forgotten by a writer that creates its own. Three
     * writers create paragraphs elsewhere on purpose: a page break, which would draw a
     * band across the page; a table cell, which carries the author's own cell paint; and
     * a row's cells, which take the paint on the cell instead, since a paragraph inside a
     * table cannot reach the band the container is drawing.</p>
     */
    private XWPFParagraph newBodyParagraph(XWPFDocument document) {
        XWPFParagraph para = currentCell != null ? currentCell.addParagraph() : document.createParagraph();
        ContainerPaint paint = containerPaint.peek();
        if (paint != null) {
            applyContainerPaint(para, paint);
        }
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

    private static void applyContainerPaint(XWPFParagraph para, ContainerPaint paint) {
        CTPPr properties = para.getCTP().isSetPPr()
                ? para.getCTP().getPPr()
                : para.getCTP().addNewPPr();
        if (paint.fill() != null) {
            CTShd shading = properties.isSetShd() ? properties.getShd() : properties.addNewShd();
            shading.setVal(STShd.CLEAR);
            shading.setColor("auto");
            shading.setFill(toHexColor(paint.fill().color()));
        }
        DocumentBorders borders = paint.borders();
        if (borders == null) {
            return;
        }
        CTPBdr edges = properties.isSetPBdr() ? properties.getPBdr() : properties.addNewPBdr();
        paintParagraphEdge(borders.top(), edges::isSetTop, edges::getTop, edges::addNewTop);
        paintParagraphEdge(borders.bottom(), edges::isSetBottom, edges::getBottom, edges::addNewBottom);
        paintParagraphEdge(borders.left(), edges::isSetLeft, edges::getLeft, edges::addNewLeft);
        paintParagraphEdge(borders.right(), edges::isSetRight, edges::getRight, edges::addNewRight);
    }

    /**
     * Writes one paragraph border edge, reusing whichever edge element is already there.
     *
     * <p>A stroke of no width is how this codebase says "no border", the same predicate the
     * table painter reads, so such a side is left unwritten rather than drawn hairline.</p>
     */
    private static void paintParagraphEdge(DocumentStroke stroke,
                                           java.util.function.BooleanSupplier isSet,
                                           java.util.function.Supplier<CTBorder> get,
                                           java.util.function.Supplier<CTBorder> add) {
        if (stroke == null || stroke.width() <= 0) {
            return;
        }
        // w:sz counts eighths of a point, rounded to at least one so a hairline the author
        // asked for stays a line rather than vanishing.
        BigInteger eighths = BigInteger.valueOf(Math.max(1, Math.round(stroke.width() * 8.0)));
        paintEdge(isSet.getAsBoolean() ? get.get() : add.get(),
                STBorder.SINGLE, eighths, toHexColor(stroke.color().color()));
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
        boolean wroteARun = false;
        for (InlineRun run : node.inlineRuns()) {
            InlineTextRun text = textOf(run);
            if (text == null) {
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
     * on the run's paragraph or on the cell holding it, and otherwise the page's white.
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
        return cellFill != null ? cellFill : java.awt.Color.WHITE;
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
                NodeDefinitionSupport.resolveImageDimensions(node, contentWidth, resolved);

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
                applyCellPaint(cell,
                        resolveCellValue(node, placement, DocumentTableStyle::fillColor),
                        resolveCellValue(node, placement, DocumentTableStyle::stroke));
                applyCellPadding(cell, resolveCellPadding(node, placement));
                if (placement.row() != rowIdx) {
                    // A covered position carries the merge marker and no content of its own.
                    continue;
                }
                cell.removeParagraph(0);
                writeCellContent(cell, placement, node);
            }
        }
        breakRowsWhereTheLayoutDoes(table, node);
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
        XWPFParagraph para = cell.addParagraph();
        List<String> lines = source.lines();
        // Word has a bidirectional engine of its own: it reorders the line and joins the
        // Arabic itself, given the text as written. What it cannot work out is the base
        // direction, and without w:bidi it assumes left to right — which puts a Hebrew
        // cell's trailing punctuation on the wrong side and starts the line at the wrong
        // edge. So the cell is told, and the text goes over untouched.
        boolean rightToLeft = resolveCellDirection(node, placement, lines);
        applyDirection(para, rightToLeft);
        // The height the row was sized with. Without it Word sets the cell at its own
        // spacing for the font — measured on the probe corpus, a totals row whose style
        // states a 14pt face came out 3pt taller than the page draws it.
        applyLineHeight(para, layout.cellLineHeight(node, placement.row(), placement.column()));
        XWPFRun run = para.createRun();
        applyStyle(run, resolveCellTextStyle(node, placement));
        applyRunDirection(run, rightToLeft);
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                // A joined "\n" is not a line break in Word; it renders as one line.
                run.addBreak();
            }
            run.setText(lines.get(i) == null ? "" : lines.get(i), i);
        }
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
        return ParagraphDirection.resolve(String.join("\n", lines), declared) == TextDirection.RTL;
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
        // visual side-by-side layout. Cell content is restricted to atomic
        // children; richer composition is scheduled for a follow-up release.
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
        // A row inside a panel is still inside it. Its paragraphs live in table cells and
        // so cannot carry the paint themselves; without shading the cells the band breaks
        // into stripes wherever a two-column block sits in a filled container.
        ContainerPaint paint = containerPaint.peek();
        for (int i = 0; i < node.children().size(); i++) {
            XWPFTableCell cell = row.getCell(i);
            cell.removeParagraph(0);
            if (paint != null && paint.fill() != null) {
                CTShd shading = cellProperties(cell).addNewShd();
                shading.setVal(STShd.CLEAR);
                shading.setColor("auto");
                shading.setFill(toHexColor(paint.fill().color()));
            }
            DocumentNode child = node.children().get(i);
            writeRowCellChild(cell, child);
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
     * <p>The width used is the page's, not the row's parent's. A row inside a padded panel
     * is offered less than the page in the fixed-layout render — but the panel's padding
     * is not exported either, so in the file being written the row really does have the
     * whole width. The slots match the document this backend produces rather than the one
     * it was given.</p>
     */
    private void applyRowGeometry(XWPFTable table, RowNode node) {
        double[] starts = layout.rowChildStarts(node);
        if (starts != null) {
            // The layout placed each child, so every way a row can divide — the two that
            // measure their children included — is already answered.
            setTableWidth(table, starts[0]);
            writeRowColumns(table, placedColumns(node, starts));
            return;
        }

        if (!Double.isFinite(contentWidth) || contentWidth <= 0) {
            return;
        }
        setTableWidth(table, contentWidth);
        double[] slots = resolveRowSlots(node, contentWidth);
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
        double available = Double.isFinite(nestedTableWidth()) ? nestedTableWidth() : contentWidth;
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
     * <p>A layout anchor reports where its child landed and an alignment says where in the
     * available width to put it. Word lays text out itself, so neither has an analogue —
     * but both have exactly one child, and dropping a wrapper takes the content with it.
     * The two walkers below ask this rather than each keeping its own list, because a
     * wrapper missing from one of them loses a subtree the other would have kept.</p>
     *
     * @param node the node being written
     * @return true when the node itself writes nothing and its children should be written
     */
    private static boolean isSemanticallyTransparent(DocumentNode node) {
        return node instanceof com.demcha.compose.document.layout.LayoutAnchorNode
               || node instanceof com.demcha.compose.document.node.AlignNode;
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
        CTTblGrid grid = cell.getTableRow().getTable().getCTTbl().getTblGrid();
        if (grid == null || grid.sizeOfGridColArray() == 0) {
            return Double.NaN;
        }
        double twips = 0;
        int last = Math.min(placement.column() + placement.colSpan(), grid.sizeOfGridColArray());
        for (int index = placement.column(); index < last; index++) {
            Long column = writtenTwips(grid.getGridColArray(index).getW());
            if (column == null) {
                // A column this export did not write as plain twips has no width to add up.
                return Double.NaN;
            }
            twips += column;
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
        // Word has no space above a table, so the paragraph before it has to carry it.
        flushSpacingAfter();
        if (currentCell == null) {
            return document.createTable(rows, columns);
        }
        XWPFTable nested = new XWPFTable(currentCell.getCTTc().addNewTbl(), currentCell, rows, columns);
        // The XML already carries the table; this is what tells the cell's own lists about
        // it, so reading the cell back finds it. getTables() is unmodifiable on purpose —
        // adding to it throws rather than quietly leaving the model and the XML disagreeing.
        currentCell.insertTable(currentCell.getBodyElements().size(), nested);
        currentCell.addParagraph();
        return nested;
    }

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
        XWPFTableCell previousCell = currentCell;
        XWPFParagraph previousParagraph = lastBodyParagraph;
        double previousCarried = carriedSpacingBefore;
        double previousOwed = pendingSpacingAfter;
        currentCell = cell;
        lastBodyParagraph = null;
        carriedSpacingBefore = 0;
        pendingSpacingAfter = 0;
        try {
            writeNode(cell.getXWPFDocument(), child);
            // A cell ends where it ends: its last gap cannot land on whatever the body
            // writes next, and the body's cannot land inside it.
            flushSpacingAfter();
        } finally {
            currentCell = previousCell;
            lastBodyParagraph = previousParagraph;
            carriedSpacingBefore = previousCarried;
            pendingSpacingAfter = previousOwed;
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
