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
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextIndent;
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
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.output.PageContext;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.xmlbeans.impl.xb.xmlschema.SpaceAttribute;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
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

    /**
     * How far short of the top of the page's line a picture may reach and still fill it, in
     * points (see {@link #letThePicturesSetTheLine}).
     */
    private static final double PICTURE_FILLS_ITS_LINE = 0.5;

    /** A one-point font size, as Word writes sizes: in half points. */
    private static final long ONE_POINT_IN_HALF_POINTS = 2;
    private static final double POINT_TO_TWIP = 20.0;
    /** The least difference between Word's baseline and the page's that is moved: one half point. */
    private static final double LEAST_BASELINE_SHIFT_POINTS = 0.5;
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
    // How many of those overlays are written as bands (see writeOverlayBand).
    private int bandDepth;
    // How many nodes laid over the flow the writer is inside: their paragraphs go in text boxes
    // (see laidOverTheFlow).
    private int overTheFlowDepth;
    // How many of those overlays are layer stacks of one layer, which lay nothing over anything.
    private int oneLayerDepth;
    // The overlays being written, innermost first (see drawsInFront).
    private final java.util.Deque<DocumentNode> openOverlays = new java.util.ArrayDeque<>();
    // How far the text of the band written last hangs below the band, in points: space the
    // next block's gap above already has on the page (see writeLinePair). Then the paragraph
    // that took what it could of it from the space owed, and what is left for its own margin.
    private double hangingBelow;
    private XWPFParagraph hangingOver;
    private double hangingOverBy;
    // How far the last line of the cell written last hangs below everything the cell writes
    // under it, in points: Word's cell is that much taller than its content on the page (see
    // writeInCell). A row reads it for each of its cells (writeRow); a panel or a stack written
    // as columns does not, and that much still makes its table taller.
    private double cellOverhang;

    /**
     * The tables whose first row text stands above, by cell — see {@link #standsAboveItsCell}.
     * In the order they were written, so the output does not depend on hash order; neither
     * POI type defines equality, so the maps key on identity.
     */
    private final java.util.Map<XWPFTable, java.util.Map<XWPFTableCell, Double>> raisedRows =
            new java.util.LinkedHashMap<>();
    // The spacers that keep the place of another column layer's content, left out while the
    // layer stack they sit in is written as columns (DocxLayerColumns).
    private final java.util.Set<DocumentNode> standIns =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    // What the stacks being written write in a stand-in's place instead (DocxLayerColumns.Moves),
    // and the blocks being written there now rather than skipped in their own layer.
    private final List<DocxLayerColumns.Moves> moves = new ArrayList<>();
    private final java.util.Set<DocumentNode> writingInAStandIn =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    // The line a layer of text is written at when it overlaps the layer above it, in points:
    // see holdStackedLines.
    private final java.util.Map<ParagraphNode, DocxStackedLines.Line> stackedLineHeights =
            new java.util.IdentityHashMap<>();
    // How far each paragraph a container pulled above its cell rises inside its own line: see
    // riseIntoItsLine.
    private final java.util.Map<ParagraphNode, Double> risenLines = new java.util.IdentityHashMap<>();
    // The cells of a table's grid that hold a composed node, whose row the table holds at the
    // page's height: see writeTableWithItsOwnSpacing.
    private final java.util.Set<org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc> tablesCells =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    // How far above the page's first line a paragraph's Word line starts, in points, where its
    // lines took the space between them from the space above it: see applyLineGap.
    private final java.util.Map<org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP, Double> lineTopsTakenIn =
            new java.util.IdentityHashMap<>();
    // Table cells whose top padding a paragraph opening them may take its line gap from: see
    // takeFromTheTopOfItsCell.
    private final java.util.Set<org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc> cellsGivingTheirPadding =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    // Icons drawn beside the text they label rather than written: see drawnBesideItsText.
    private final java.util.Set<ImageNode> picturesDrawnBeside =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    // The space above the next block, in points, in place of everything owed above it: set
    // when a column layer follows another in its cell, NaN otherwise.
    private double resumeSpacing = Double.NaN;
    // How far the containers being written hold their content in from each side, in points:
    // every enclosing margin and padding, counted from the page margin or the cell's edge.
    private double insetLeft;
    private double insetRight;
    // Identifiers for the shapes this export draws itself, kept clear of the ones POI numbers
    // its pictures with: a drawing's id has to be unique in the document.
    private long nextDrawingId;
    // Where the shapes this export draws are anchored — see DocxDrawingAnchors.
    private final DocxDrawingAnchors anchors = new DocxDrawingAnchors(() -> nextDrawingId++);
    // The page the node being written starts on, as the layout placed it, counted within the
    // section being written.
    private int currentPage;

    /** The page the last block written in the flow ended on, -1 before the first. */
    private int lastEndPage = -1;

    /** A paragraph whose own top edge a line above it holds: {@link #holdAParagraphsTopEdgeOnItsPage}. */
    private DocumentNode topEdgeHeldAbove;

    /** How much of that paragraph's own top edge the line holds, in points. */
    private double topEdgeHeld;

    /** The line holding that edge, until the paragraph's line gap is written. */
    private XWPFParagraph topEdgeLine;

    /** The Word paragraph that line holds the edge of, once written. */
    private XWPFParagraph topEdgeLineOver;
    private int sectionIndex;
    // Where the section being written starts among the body's elements.
    private int sectionFirstElement;
    // The section's page backgrounds drawn from its page in the body, not from a header.
    private List<DocxPageBackgrounds.Fill> backgroundsInBody = List.of();
    // The shape container being written that clips its content to its outline, null outside one.
    private ShapeContainerNode clipContainer;
    // The table cell a drawing is written alone in, whose paragraph carries its shapes; null
    // outside one (see drawingCellFor).
    private DrawingCell drawingCell;
    // Whether the shapes last queued went into that cell rather than onto the page.
    private boolean drewInCell;
    // What the tables being written drew of their composed cells (see drawCellDrawing): a drawing
    // node inside such a cell is then drawn, not lost.
    private CellDrawing cellDrawing = CellDrawing.NONE;
    // The drawings the composed cells of the table being written paint, not yet anchored, in the
    // order the layout emitted them; null outside such a table (see anchorComposedDrawing).
    private List<CellFragment> tableDrawings;
    // Where the layout placed the cell of that table being written, first placement first; empty
    // outside one.
    private List<DocxLayoutMetrics.CellBox> composedCellBoxes = List.of();
    // The shapes composed in a cell that anchorComposedDrawing anchored there, so not lost.
    private final java.util.Set<DocumentNode> anchoredInCells =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    // The paragraph a badge being drawn holds, as w:p markup (see textBadgeParagraph); null
    // otherwise.
    private String badgeText;
    // The cell of the panel being written, painted or framed; null outside a panel.
    private XWPFTableCell panelCell;
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

    /**
     * Paragraphs and tables written so far, counted only to tell whether a container wrote
     * anything at all — see {@link #writeContainerBody}.
     */
    private long blocksWritten;

    /** Space the last body paragraph holds below itself, not yet written — see {@link #owePendingSpacingAfter}. */
    private double pendingSpacingAfter;

    /**
     * How far the paragraph just written pulls the next one up into itself, by a negative bottom
     * edge, in points: taken off the space written above the next paragraph, as the page sums
     * the two edges. Where no paragraph takes it — before a table, at a cell's end, a page break
     * or the document's end — it comes out of the space owed below instead. A pull no space can
     * give stays unwritten, as an edge Word cannot write. Layers that overlap on the page take
     * none of it from one another: the space between them is measured from their boxes.
     */
    private double pullBelow;

    /** What of {@link #pullBelow} the space owed above a paragraph did not give, for its own top edge. */
    private double pullLeft;

    /** The paragraph {@link #pullLeft} is left for. */
    private XWPFParagraph pullLeftOn;

    /**
     * How far the bottom border of the panel just written stands below its box beyond the space
     * the panel holds under itself, see {@link #writePanelPiece}. The next panel, paragraph or
     * table takes it from the space above itself; a row carries the most any of its cells ends
     * with; a page break or a new section clears it.
     */
    private double borderBelow;

    /** The page's height in points, or {@code NaN} when the export has no canvas. */
    private double canvasHeight = Double.NaN;

    /** The page's top margin in points, or {@code NaN} when the export has no canvas. */
    private double canvasTopMargin = Double.NaN;

    /** Whether this export writes more than one section — see {@link #exportSections}. */
    private boolean sectioned;

    /** The gap the list being written puts between its items. */
    private double pendingItemSpacing;

    /** The gap between the wrapped lines of an item of the list being written. */
    private double listLineGap;

    /**
     * How many lines each item of the list being written still to come was laid out on,
     * first item first; empty when they cannot be told apart, and then no item gets the gap.
     */
    private java.util.ArrayDeque<Integer> listItemLines = new java.util.ArrayDeque<>();

    /** Whether the list being written has an item above the one about to be written. */
    private boolean anItemWasWritten;
    // The last paragraph written into the body, so a container can hand it the space it
    // holds below itself once its children are done.
    private XWPFParagraph lastBodyParagraph;
    // The paragraph writeParagraph wrote last, and the Word paragraph it wrote it in: a paragraph
    // pulled up into it takes the pull off its foot (takeFromTheLineAbove), while nothing else has
    // been written since — lastBodyParagraph is that same Word paragraph then.
    private ParagraphNode lastWrittenNode;
    private XWPFParagraph lastWrittenParagraph;
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
    // The row child whose left margin its cell already holds: a row the layout placed starts
    // each cell's text where the child starts, its left margin included.
    private DocumentNode leftMarginInCell;
    // What a row nested in a cell still hangs left once its first column has given what it can
    // (takeHang), handed to each of its cells: Word keeps a nested table inside its cell
    // whatever the table's indent.
    private double cellHang;
    // The cell being filled takes its row's hang as cellTextShift (a negative number): its
    // paragraphs' text moves left by it as far as each one's own indent goes, and never past
    // the cell's edge (applyInset). Only the text moves: the insets and the widths measured
    // from them stay the cell's, so a rule, a picture or a table in it keeps the cell's size.
    private double cellTextShift;
    // Every family this export can name, by the logical name a style asks for. The
    // session's own registrations win over the bundled ones, the way they do everywhere.
    private java.util.Map<FontName, FontFamilyDefinition> wordFamilies = java.util.Map.of();
    // The families the layout measured with, and the fonts it measured them in, loaded when a
    // line seated off its baseline first asks for a cap height (see seatShift) or a stack for its
    // letters' reach (see inkOf).
    private List<FontFamilyDefinition> measuredFamilies = List.of();
    private FontLibrary seatFonts;
    // Those fonts' line heights, for a line the layout placed none of (see styleLineHeight).
    private com.demcha.compose.document.chart.ChartTextMetrics styleMetrics;
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
         * <p>A {@code {date}} token in a text header or footer is written as the date of the
         * export, so a document using it stays byte-identical only within one day unless
         * {@code -Dgraphcompose.renderDate} pins it — the same limitation the PDF and PPTX
         * backends have.</p>
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
        bandDepth = 0;
        overTheFlowDepth = 0;
        oneLayerDepth = 0;
        openOverlays.clear();
        forgetTheHang();
        raisedRows.clear();
        insetLeft = 0;
        insetRight = 0;
        cellHang = 0;
        cellTextShift = 0;
        nextDrawingId = 100_000;
        anchors.reset();
        currentPage = 0;
        lastEndPage = -1;
        topEdgeHeldAbove = null;
        topEdgeHeld = 0;
        topEdgeLine = null;
        topEdgeLineOver = null;
        clipContainer = null;
        cellDrawing = CellDrawing.NONE;
        tableDrawings = null;
        composedCellBoxes = List.of();
        anchoredInCells.clear();
        panelCell = null;
        moves.clear();
        writingInAStandIn.clear();
        stackedLineHeights.clear();
        lineTopsTakenIn.clear();
        cellsGivingTheirPadding.clear();
        risenLines.clear();
        tablesCells.clear();
        picturesDrawnBeside.clear();
        listNumbering.clear();
        report = new DocxExportReport.Builder();
        bookmarkNames = new DocxBookmarkNames();
        headingLevels = headingLevelsIn(whole);
        bookmarkedAnchors = bookmarkedAnchorsIn(whole);
        wordFamilies = DocxFontTable.familiesByName(fonts);
        measuredFamilies = List.copyOf(fonts);
        seatFonts = null;
        styleMetrics = null;
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
                sectionFirstElement = document.getBodyElements().size();
                backgroundsInBody = List.of();
                applyPageGeometry(document, context.canvas());
                if (index == 0) {
                    writeStylesPart(document);
                    DocxFontTable.write(document, whole, fonts, report);
                    applyMetadata(document, metadataOf(sections));
                }
                earlierZones.addAll(applyPageZones(document, context.outputOptions().zones(),
                        context.outputOptions().headersAndFooters(), evenAndOdd, earlierZones));
                if (applyPageBackgrounds(document, context.layoutGraph(), evenAndOdd)) {
                    earlierZones.add(DocumentHeaderFooterZone.HEADER);
                }
                // Drawing the layout paints in a pass of its own — a timeline's rail — belongs to
                // no node, so it waits for the first paragraph on its page.
                for (com.demcha.compose.document.layout.PlacedFragment pass : layout.passFragments()) {
                    if (queueDrawing(pass, drawsInFront())) {
                        report.add(DocxExportReport.Severity.APPROXIMATED, "timeline rail", pass.path(),
                                "drawn as a line anchored to the page where the layout puts it: it stays "
                                + "there when the text around it is edited");
                    }
                }
                for (DocumentNode root : section.graph().roots()) {
                    writeNode(document, root);
                }
                raiseRows();
                // The paragraph closing a section that ends with a table is also the one that
                // carries the drawings nothing else on the last page carried.
                reportDrawingsLeftOver(document, dropTheSpaceAtTheEnd(document));
            }
            hideTheClosingMark(document);
            hideTheCellClosingMarks(document.getTables());
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
        sectionIndex = index;
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
        pullBelow = 0;
        pullLeft = 0;
        pullLeftOn = null;
        borderBelow = 0;
        pendingItemSpacing = 0;
        anItemWasWritten = false;
        forgetTheHang();
        // The layout counts a section's pages from its own first.
        lastEndPage = -1;
        lastBodyParagraph = null;
        lastWrittenNode = null;
        lastWrittenParagraph = null;
        contentWidth = context.canvas() == null ? Double.MAX_VALUE : context.canvas().innerWidth();
        canvasHeight = context.canvas() == null ? Double.NaN : context.canvas().height();
        canvasTopMargin = context.canvas() == null ? Double.NaN : context.canvas().margin().top();
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
     *
     * <p>Against the edge it still reaches a point into the page, past a margin narrower than
     * that, and Word moves the body down by what it reaches past — {@code NavySidebar}'s whole
     * page, under the header carrying its backgrounds, stood a point low. That margin is
     * written negative, which holds the body at it (see {@link #placeBand}).</p>
     */
    private static void blankZone(XWPFHeaderFooterPolicy policy, CTSectPr sectPr, boolean header,
                                  org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr.Enum type,
                                  boolean againstTheEdge) {
        XWPFHeaderFooter blank = header ? policy.createHeader(type) : policy.createFooter(type);
        collapsed(blank.createParagraph());
        if (againstTheEdge && sectPr.isSetPgMar()) {
            CTPageMar margin = sectPr.getPgMar();
            if (header) {
                margin.setHeader(BigInteger.ZERO);
                if (reachedPast(margin.getTop())) {
                    margin.setTop(BigInteger.valueOf(-Math.max(1, twipsOf(margin.getTop()))));
                }
            } else {
                margin.setFooter(BigInteger.ZERO);
                if (reachedPast(margin.getBottom())) {
                    margin.setBottom(BigInteger.valueOf(-Math.max(1, twipsOf(margin.getBottom()))));
                }
            }
        }
    }

    /** Whether a page margin is narrower than the point a blank zone against its edge reaches. */
    private static boolean reachedPast(Object pageMargin) {
        return pageMargin instanceof Number twips && twips.longValue() >= 0
               && twips.longValue() < Math.round(POINT_TO_TWIP);
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

    /**
     * Hides the mark of the empty paragraph a document ending in a table must end with.
     *
     * <p>Word ends a document on a paragraph, so one follows a closing table, a point tall.
     * Where the table ends a point from the page's foot, that point does not fit, and the
     * paragraph opened a page of its own: {@code ModernReceipt}'s QR code ends 0.5pt above the
     * margin, and once its panels held the page's height the receipt ran to a blank second page
     * in LibreOffice. A hidden mark is not laid out. Only a paragraph that is structure alone is
     * hidden: one holding anything — a run, a field, a drawing's anchor, a bookmark — or drawing
     * anything — a rule is a paragraph's border, a band its shading — or carrying a section's
     * properties is left as it is.</p>
     */
    private static void hideTheClosingMark(XWPFDocument document) {
        List<IBodyElement> body = document.getBodyElements();
        if (body.size() < 2 || !(body.get(body.size() - 1) instanceof XWPFParagraph last)
            || !(body.get(body.size() - 2) instanceof XWPFTable) || !structureAlone(last)) {
            return;
        }
        hide(last);
    }

    /**
     * Hides the mark of the empty paragraph a table cell ending in a nested table must end with,
     * in every table of the document, nested ones included.
     *
     * <p>Word ends a cell on a paragraph, so a hairline one closes a nested table there (see
     * {@link #newTable}). Word lays it out at no height that shows; LibreOffice gives it its tenth
     * of a point, so every row of {@code CobaltRota}, a chip in a table in a table in each cell,
     * stood up to 0.3pt taller there than on the page, and its last row 5.2pt low; hidden, it is
     * not laid out, and its last row stands 3.5pt low. Only that hairline is hidden: a paragraph that
     * is structure alone, a hairline tall and holding no space above or below it, which would go
     * with it.</p>
     */
    private static void hideTheCellClosingMarks(List<XWPFTable> tables) {
        for (XWPFTable table : tables) {
            for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    hideTheCellClosingMarks(cell.getTables());
                    List<IBodyElement> elements = cell.getBodyElements();
                    if (elements.size() < 2 || !(elements.get(elements.size() - 1) instanceof XWPFParagraph last)
                        || !(elements.get(elements.size() - 2) instanceof XWPFTable) || !structureAlone(last)) {
                        continue;
                    }
                    CTSpacing spacing = last.getCTP().getPPr() != null && last.getCTP().getPPr().isSetSpacing()
                            ? last.getCTP().getPPr().getSpacing() : null;
                    boolean hairline = spacing != null && spacing.isSetLineRule()
                                       && spacing.getLineRule() == STLineSpacingRule.EXACT
                                       && twipsOf(spacing.getLine()) == Math.round(SEPARATOR_POINTS * POINT_TO_TWIP);
                    if (!hairline || twipsOf(spacing.getBefore()) != 0 || twipsOf(spacing.getAfter()) != 0) {
                        continue;
                    }
                    hide(last);
                }
            }
        }
    }

    /**
     * Whether a paragraph is structure alone: it holds nothing — a run, a field, a drawing's
     * anchor, a bookmark — and draws nothing — a rule is a paragraph's border, a band its
     * shading — nor carries a section's properties.
     */
    private static boolean structureAlone(XWPFParagraph paragraph) {
        org.w3c.dom.NodeList children = paragraph.getCTP().getDomNode().getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            org.w3c.dom.Node child = children.item(index);
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE && !"pPr".equals(child.getLocalName())) {
                return false;
            }
        }
        CTPPr properties = paragraph.getCTP().getPPr();
        return properties == null || !(properties.isSetSectPr() || properties.isSetPBdr() || properties.isSetShd());
    }

    private static void hide(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTParaRPr mark =
                properties.isSetRPr() ? properties.getRPr() : properties.addNewRPr();
        if (mark.sizeOfVanishArray() == 0) {
            mark.addNewVanish();
        }
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
        // The watermark and protection are still ignored. The text header and footer are
        // written with the page zones — see applyPageZones.
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
     * <p>A text band — {@link DocumentHeaderFooter}, the three slots and their page tokens — is
     * written into the same parts (see {@link #writeBand}): Word has one header and one footer
     * per kind of page, so a band and a page zone of one kind share it, the band in a frame at its
     * own height.</p>
     *
     * @param bands        the section's text headers and footers
     * @param evenAndOdd   whether the document states different even and odd pages
     * @param earlierZones the kinds of zone an earlier section wrote
     * @return the kinds of zone this section wrote
     */
    private java.util.Set<DocumentHeaderFooterZone> applyPageZones(
            XWPFDocument document,
            List<DocumentPageZone> zones,
            List<DocumentHeaderFooter> bands,
            boolean evenAndOdd,
            java.util.Set<DocumentHeaderFooterZone> earlierZones) {
        // The page height the zones are measured against is the canvas's, which is what the
        // page geometry was written from — not a value parsed back out of the XML.
        java.util.Set<DocumentHeaderFooterZone> written =
                java.util.EnumSet.noneOf(DocumentHeaderFooterZone.class);
        List<DocumentPageZone> sectionZones = zones == null ? List.of() : zones;
        List<DocumentHeaderFooter> sectionBands = bands == null ? List.of() : bands;
        if (sectionZones.isEmpty() && sectionBands.isEmpty() && earlierZones.isEmpty()) {
            return written;
        }
        // Text bands first: a page zone of the same kind then states the distance from the edge,
        // since its content is what the layout measured.
        List<ZoneWriter> writers = new ArrayList<>();
        for (DocumentHeaderFooter band : sectionBands) {
            // A band that shares its kind with another band or a page zone stands at its own
            // height on the page: stacked in the part as lines, they keep the order they were
            // added in rather than the page's order by height.
            boolean framed = sectionBands.stream().filter(other -> other.getZone() == band.getZone()).count() > 1
                             || sectionZones.stream().anyMatch(zone -> zone.getZone() == band.getZone()
                                                                       && zone.getContent() != null);
            writers.add(new ZoneWriter(band.getZone(), bandPageClasses(band),
                    part -> writeBand(part, band, framed), () -> placeBand(document, band, framed)));
        }
        for (int index = 0; index < sectionZones.size(); index++) {
            DocumentPageZone zone = sectionZones.get(index);
            DocumentNode content = zone.getContent() == null
                    ? null
                    : zone.getContent().apply(PageContext.unpaginated());
            if (content == null) {
                continue;
            }
            int at = index;
            boolean header = zone.getZone() == DocumentHeaderFooterZone.HEADER;
            writers.add(new ZoneWriter(zone.getZone(), pageClassesOf(zone),
                    part -> writeZoneLine(part, content), () -> placeZone(document, zone, at, header)));
        }
        boolean titlePage = false;
        for (ZoneWriter writer : writers) {
            if (writer.drawnOn().contains(DocxPageClasses.PageClass.FIRST)
                != writer.drawnOn().contains(DocxPageClasses.PageClass.LATER_ODD)) {
                titlePage = true;
            }
        }
        // Bound to the section being written, whose properties are the body's until it ends.
        CTSectPr sectPr = bodySectPr(document);
        if (titlePage && !sectPr.isSetTitlePg()) {
            sectPr.addNewTitlePg();
        }
        XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(document, sectPr);
        java.util.Map<String, XWPFHeaderFooter> parts = new java.util.HashMap<>();
        for (ZoneWriter writer : writers) {
            if (writer.drawnOn().isEmpty()) {
                continue;
            }
            boolean header = writer.kind() == DocumentHeaderFooterZone.HEADER;
            for (org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr.Enum type
                    : partTypes(titlePage, evenAndOdd)) {
                if (writer.drawnOn().contains(pageClassOf(type))) {
                    // A band and a zone of one kind share the part: Word has one header per page.
                    XWPFHeaderFooter part = parts.computeIfAbsent(writer.kind() + "/" + type,
                            key -> header ? policy.createHeader(type) : policy.createFooter(type));
                    writer.write().accept(part);
                }
            }
            writer.place().run();
            written.add(writer.kind());
        }
        // A part that ends in a framed band ends with a paragraph of its own flow, a point tall:
        // the frame is placed from the paragraph that follows it.
        for (XWPFHeaderFooter part : parts.values()) {
            List<XWPFParagraph> paragraphs = part.getParagraphs();
            XWPFParagraph last = paragraphs.isEmpty() ? null : paragraphs.get(paragraphs.size() - 1);
            if (last != null && last.getCTP().isSetPPr() && last.getCTP().getPPr().isSetFramePr()) {
                collapsed(part.createParagraph());
            }
        }
        for (DocumentHeaderFooterZone kind : DocumentHeaderFooterZone.values()) {
            if (!written.contains(kind) && !earlierZones.contains(kind)) {
                continue;
            }
            for (org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr.Enum type
                    : partTypes(titlePage, evenAndOdd)) {
                if (!parts.containsKey(kind + "/" + type)) {
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
     * What a header or footer writes into a part, the kinds of page it is drawn on, and how the
     * section places it from the page edge.
     */
    private record ZoneWriter(DocumentHeaderFooterZone kind,
                              java.util.Set<DocxPageClasses.PageClass> drawnOn,
                              java.util.function.Consumer<XWPFHeaderFooter> write,
                              Runnable place) {
    }

    /**
     * The kinds of page a text band is drawn on: every one, or all but the first when its
     * numbering keeps it off the first page or counts from page 2 or later. A band held off
     * more pages than the first has no Word part to say so, and is written on every page but the
     * first, reported; so are page numbers that do not count from 1 on page 1, which Word's
     * fields do.
     */
    private java.util.Set<DocxPageClasses.PageClass> bandPageClasses(DocumentHeaderFooter band) {
        com.demcha.compose.document.output.DocumentPageNumbering numbering = band.getNumbering();
        java.util.Set<DocxPageClasses.PageClass> classes = java.util.EnumSet.allOf(DocxPageClasses.PageClass.class);
        if (numbering == null) {
            return classes;
        }
        if (!numbering.isShowOnFirstPage() || numbering.getCountFrom() >= 2) {
            classes.remove(DocxPageClasses.PageClass.FIRST);
        }
        String section = sectioned ? "section " + (sectionIndex + 1) : null;
        if (numbering.getCountFrom() > 2) {
            report.add(DocxExportReport.Severity.APPROXIMATED, "page " + zoneName(band), section,
                    "it starts on page " + numbering.getCountFrom() + ", and Word has a separate "
                    + "header and footer only for the first page, so it is written on every page "
                    + "but the first");
        }
        if (numbering.getStartAt() != numbering.getCountFrom()) {
            report.add(DocxExportReport.Severity.APPROXIMATED, "page " + zoneName(band), section,
                    "its page numbers count from " + numbering.getStartAt() + " on page "
                    + numbering.getCountFrom() + "; Word's fields number the pages from 1, and "
                    + "its page total is off by as much");
        }
        return classes;
    }

    private static String zoneName(DocumentHeaderFooter band) {
        return band.getZone() == DocumentHeaderFooterZone.HEADER ? "header" : "footer";
    }

    /**
     * Writes a text band as one line of a Word header or footer (see {@link DocxTextBands}).
     *
     * <p>The left slot starts the line, the centre slot stands at a centre tab in the middle of
     * the margins and the right one at a right tab against the right margin, as the page sets
     * them; {@code {page}} and {@code {pages}} are Word's page fields, and {@code {date}} is the
     * date of the export, as the page prints the date it was rendered. The separator is the
     * paragraph's border, below a header and above a footer, at the page's distance from the
     * text.</p>
     *
     * <p>A band that shares its kind with another band or a page zone is framed: the page sets
     * each at its own height from the edge, and one line after another in the part they keep the
     * order they were added in rather than the page's order by height. Framed, each line stands
     * at its own height on the page, where the part's flow does not move it, and moves nothing in
     * the body. Two frames side by side in the part are kept apart by a hairline paragraph: Word
     * takes adjacent paragraphs with the same frame for one frame, and two bands at one height
     * would share a frame a line tall, the second line cut off.</p>
     *
     * @param framed whether the band stands in a frame at its height on the page
     */
    private void writeBand(XWPFHeaderFooter part, DocumentHeaderFooter band, boolean framed) {
        boolean inFrame = framed && !Double.isNaN(canvasHeight);
        double line = DocxTextBands.lineHeight(band);
        boolean separated = band.isShowSeparator() && band.getSeparatorColor() != null
                            && band.getSeparatorColor().color().getAlpha() > 0 && band.getSeparatorThickness() > 0;
        // The frame holds the separator too, below a header's line and above a footer's, as the
        // border is written: its space in whole points, its width in eighths of a point.
        double border = separated
                ? Math.round(DocxTextBands.separatorSpace(band)) + ruleEighths(band.getSeparatorThickness()) / 8.0
                : 0;
        long frameTop = toTwips(band.getZone() == DocumentHeaderFooterZone.HEADER
                ? DocxTextBands.distanceFromEdge(band)
                : canvasHeight - DocxTextBands.distanceFromEdge(band) - line - border);
        List<XWPFParagraph> before = part.getParagraphs();
        if (inFrame && !before.isEmpty() && sameFrameHeight(before.get(before.size() - 1), frameTop)) {
            // Word takes adjacent paragraphs with the same frame for one frame.
            collapsed(part.createParagraph());
        }
        XWPFParagraph para = part.createParagraph();
        CTPPr properties = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        if (inFrame) {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFramePr frame = properties.addNewFramePr();
            frame.setW(BigInteger.valueOf(toTwips(contentWidth)));
            frame.setH(BigInteger.valueOf(toTwips(line + border)));
            frame.setHRule(org.openxmlformats.schemas.wordprocessingml.x2006.main.STHeightRule.EXACT);
            frame.setHAnchor(org.openxmlformats.schemas.wordprocessingml.x2006.main.STHAnchor.MARGIN);
            frame.setX(BigInteger.ZERO);
            frame.setVAnchor(org.openxmlformats.schemas.wordprocessingml.x2006.main.STVAnchor.PAGE);
            frame.setY(BigInteger.valueOf(frameTop));
            frame.setWrap(org.openxmlformats.schemas.wordprocessingml.x2006.main.STWrap.THROUGH);
        }
        CTSpacing spacing = properties.isSetSpacing() ? properties.getSpacing() : properties.addNewSpacing();
        spacing.setBefore(BigInteger.ZERO);
        spacing.setAfter(BigInteger.ZERO);
        spacing.setLineRule(STLineSpacingRule.EXACT);
        spacing.setLine(BigInteger.valueOf(Math.round(line * POINT_TO_TWIP)));
        boolean centre = !DocxTextBands.segments(band.getCenterText()).isEmpty();
        boolean right = !DocxTextBands.segments(band.getRightText()).isEmpty();
        if (centre || right) {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabs tabs = properties.addNewTabs();
            if (centre) {
                CTTabStop stop = tabs.addNewTab();
                stop.setVal(STTabJc.CENTER);
                stop.setPos(BigInteger.valueOf(Math.round(contentWidth / 2 * TWIPS_PER_POINT)));
            }
            if (right) {
                CTTabStop stop = tabs.addNewTab();
                stop.setVal(STTabJc.RIGHT);
                stop.setPos(BigInteger.valueOf(Math.round(contentWidth * TWIPS_PER_POINT)));
            }
        }
        DocumentTextStyle style = new DocumentTextStyle(
                band.getFontName() == null ? FontName.HELVETICA : band.getFontName(),
                band.getFontSize(), DocumentTextDecoration.DEFAULT,
                band.getTextColor() == null ? DocumentColor.GRAY : band.getTextColor());
        com.demcha.compose.document.output.DocumentPageNumberStyle numbers =
                band.getNumbering() == null ? null : band.getNumbering().getStyle();
        appendBandSlot(para, band.getLeftText(), style, numbers);
        if (centre) {
            appendTab(para, style);
            appendBandSlot(para, band.getCenterText(), style, numbers);
        }
        if (right) {
            appendTab(para, style);
            appendBandSlot(para, band.getRightText(), style, numbers);
        }
        if (separated) {
            CTPBdr borders = properties.isSetPBdr() ? properties.getPBdr() : properties.addNewPBdr();
            CTBorder edge = band.getZone() == DocumentHeaderFooterZone.HEADER
                    ? (borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom())
                    : (borders.isSetTop() ? borders.getTop() : borders.addNewTop());
            paintEdge(edge, STBorder.SINGLE, BigInteger.valueOf(ruleEighths(band.getSeparatorThickness())),
                    toHexColor(flatten(band.getSeparatorColor().color(), java.awt.Color.WHITE)));
            edge.setSpace(BigInteger.valueOf(Math.round(DocxTextBands.separatorSpace(band))));
        }
    }

    /** Whether a paragraph stands in a frame at a height from the top of the page. */
    private static boolean sameFrameHeight(XWPFParagraph paragraph, long top) {
        if (!paragraph.getCTP().isSetPPr() || !paragraph.getCTP().getPPr().isSetFramePr()) {
            return false;
        }
        Object y = paragraph.getCTP().getPPr().getFramePr().getY();
        return y instanceof Number number && number.longValue() == top;
    }

    private void appendTab(XWPFParagraph para, DocumentTextStyle style) {
        XWPFRun tab = para.createRun();
        applyStyle(tab, style);
        tab.addTab();
    }

    /**
     * Writes one slot's text: its literal pieces and {@code {date}} as runs, its page tokens as
     * fields, whose results read
     * in the band's number style — an editor that does not update a field shows them as written.
     */
    private void appendBandSlot(XWPFParagraph para, String text, DocumentTextStyle style,
                                com.demcha.compose.document.output.DocumentPageNumberStyle numbers) {
        String format = DocxTextBands.numberFormat(numbers);
        com.demcha.compose.engine.components.content.header_footer.PageNumberStyle numerals =
                com.demcha.compose.engine.components.content.header_footer.PageNumberStyle.valueOf(
                        (numbers == null ? com.demcha.compose.document.output.DocumentPageNumberStyle.DECIMAL : numbers)
                                .name());
        for (DocxTextBands.Segment segment : DocxTextBands.segments(text)) {
            switch (segment.kind()) {
                case TEXT -> {
                    XWPFRun run = para.createRun();
                    applyStyle(run, style);
                    run.setText(segment.text());
                }
                case PAGE -> appendField(para, " PAGE" + format + " ", numerals.format(1), style);
                case PAGES -> appendField(para, (sectioned ? " SECTIONPAGES" : " NUMPAGES") + format + " ",
                        numerals.format(Math.max(1, layout.pageCount())), style);
                case DATE -> {
                    XWPFRun run = para.createRun();
                    applyStyle(run, style);
                    // The date the page prints, pinned the way the page pins it.
                    run.setText(com.demcha.compose.engine.components.content.header_footer.HeaderFooterConfig
                            .resolvePlaceholders("{date}", 1, 1));
                }
            }
        }
    }

    /**
     * Places a text band's paragraph as far from its page edge as the page sets its text (see
     * {@link DocxTextBands#distanceFromEdge}).
     */
    private void placeBand(XWPFDocument document, DocumentHeaderFooter band, boolean framed) {
        CTSectPr sectPr = bodySectPr(document);
        CTPageMar margin = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        BigInteger distance = BigInteger.valueOf(toTwips(DocxTextBands.distanceFromEdge(band)));
        boolean header = band.getZone() == DocumentHeaderFooterZone.HEADER;
        if (header) {
            margin.setHeader(distance);
        } else {
            margin.setFooter(distance);
        }
        // The page lets a band reach into the body; past a positive margin Word moves the body
        // clear of its header and footer instead, which can add a page. A framed band stands
        // beside the flow and moves nothing.
        if (framed) {
            return;
        }
        if (reachesPastTheMargin(document, band)) {
            // The page lets the band overlap the body. Word moves the body clear of a header or
            // footer taller than its margin — MerchantInvoice's footer row, set down to its 3.4pt
            // margin, went to a second page under a footer reaching 9.8pt — unless the margin is
            // written negative, which holds the body at it whatever the band reaches.
            // No margin has no negative: the least one stands for it.
            if (header) {
                margin.setTop(BigInteger.valueOf(-Math.max(1, twipsOf(margin.getTop()))));
            } else {
                margin.setBottom(BigInteger.valueOf(-Math.max(1, twipsOf(margin.getBottom()))));
            }
            report.add(DocxExportReport.Severity.APPROXIMATED, "page " + zoneName(band),
                    sectioned ? "section " + (sectionIndex + 1) : null,
                    "it reaches " + Math.round(reachOf(band) * 10) / 10.0 + "pt from the page edge, past the "
                    + "page margin, which is written negative so that Word holds the body at the margin, as "
                    + "the page does; LibreOffice moves the body clear of it");
        }
    }

    /** How far a text band reaches from its page edge, its separator included, in points. */
    private static double reachOf(DocumentHeaderFooter band) {
        return DocxTextBands.distanceFromEdge(band) + DocxTextBands.lineHeight(band)
               + (band.isShowSeparator() ? DocxTextBands.separatorSpace(band) + band.getSeparatorThickness() : 0);
    }

    /**
     * Whether a text band reaches past the page margin on its edge, into the body, where the page
     * lets the two overlap. A twentieth of a point of grace is rounding.
     */
    private boolean reachesPastTheMargin(XWPFDocument document, DocumentHeaderFooter band) {
        CTSectPr sectPr = bodySectPr(document);
        if (!sectPr.isSetPgMar()) {
            return false;
        }
        CTPageMar margin = sectPr.getPgMar();
        Object edge = band.getZone() == DocumentHeaderFooterZone.HEADER ? margin.getTop() : margin.getBottom();
        return edge instanceof Number pageMargin && pageMargin.longValue() >= 0
               && toTwips(reachOf(band)) > pageMargin.longValue() + 1;
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
        boolean hasFooter = policy.getDefaultFooter() != null || policy.getFirstPageFooter() != null
                            || policy.getEvenPageFooter() != null;
        if (!hasHeader && !hasFooter && graph.totalPages() == 1) {
            // A section of one page with no header or footer draws them from that page instead
            // (reportDrawingsLeftOver): LibreOffice gives a header a height of its own however
            // little it holds, and set the body that much lower — SlateOrange, CharcoalGold,
            // MidnightNavy and the other sidebar CVs, their column a background, stood 2.6 to
            // 3.3pt low on every line. Not under a footer: Word paints the body's shapes over a
            // header's and a footer's text, and MeteredInvoice's footer band hid its footer. The
            // fills are on that page alone: text an editor adds past it gets a page without them.
            backgroundsInBody = fills;
            return false;
        }
        for (org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr.Enum type
                : partTypes(sectPr.isSetTitlePg(), evenAndOdd)) {
            if (headerOf(policy, type) == null) {
                blankZone(policy, sectPr, true, type, !hasHeader);
            }
            org.apache.poi.xwpf.usermodel.XWPFHeader header = headerOf(policy, type);
            // A paragraph of the header's flow: one in a text band's frame would hold the fills
            // inside the frame in LibreOffice.
            XWPFParagraph carrier = header.getParagraphs().stream()
                    .filter(paragraph -> !paragraph.getCTP().isSetPPr() || !paragraph.getCTP().getPPr().isSetFramePr())
                    .findFirst()
                    .orElseGet(() -> collapsed(header.createParagraph()));
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
        // A multi-section document numbers each section from 1, so the total a zone states is
        // its section's; in a document of one section the two are the same count.
        String total = sectioned ? " SECTIONPAGES " : " NUMPAGES ";
        appendField(para, field.kind() == PageFieldKind.TOTAL ? total : " PAGE ",
                fieldPlaceholder(field.kind()), field.textStyle());
    }

    /**
     * Appends a Word field and the result it reads before an editor updates it.
     *
     * <p>Word repaints the field on open; the placeholder run is what a reader sees before that
     * happens, and what a text extractor finds.</p>
     *
     * <p>Written as a complex field — begin, instruction, separator, result, end — each run
     * carrying the text style. A simple field's result is repainted without its run's style:
     * {@code MeteredInvoice}'s white page number on its navy footer band came out in the
     * document's ink in both editors, so the band read "Page of". A complex field's result is
     * set as its instruction is, and the instruction is styled.</p>
     */
    private void appendField(XWPFParagraph para, String instruction, String placeholder, DocumentTextStyle style) {
        fieldRun(para, style).getCTR().addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText code = fieldRun(para, style).getCTR().addNewInstrText();
        code.setStringValue(instruction);
        code.setSpace(org.apache.xmlbeans.impl.xb.xmlschema.SpaceAttribute.Space.PRESERVE);
        fieldRun(para, style).getCTR().addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
        fieldRun(para, style).setText(placeholder);
        fieldRun(para, style).getCTR().addNewFldChar().setFldCharType(STFldCharType.END);
    }

    private XWPFRun fieldRun(XWPFParagraph para, DocumentTextStyle style) {
        XWPFRun run = para.createRun();
        applyStyle(run, style);
        return run;
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
        if (!writingInAStandIn.contains(node) && movedIntoAStandIn(node)) {
            // Written already, in the place an earlier layer held for it.
            return;
        }
        com.demcha.compose.document.layout.PlacedNode placed = layout.placement(node);
        if (placed != null) {
            currentPage = placed.startPage();
        }
        boolean inTheBody = currentCell == null;
        try {
            writePlacedNode(document, node);
        } finally {
            if (placed != null && inTheBody) {
                lastEndPage = Math.max(lastEndPage, placed.endPage());
            }
        }
    }

    private void writePlacedNode(XWPFDocument document, DocumentNode node) throws Exception {
        boolean keepTogether = node.keepTogether() && layout.onOnePage(node);
        boolean keepWithNext = node.keepWithNext() && layout.onOnePage(node);
        String anchor = blockAnchorOf(node, inTheFlow(node, overlayDepth, oneLayerDepth));
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
        ParagraphNode initials = node instanceof ShapeContainerNode badge ? textBadgeParagraph(badge) : null;
        if (initials != null) {
            writeTextBadge(document, (ShapeContainerNode) node, initials);
            return;
        }
        if (laidOverTheFlow(node)) {
            writeOverTheFlow(document, node);
            return;
        }
        if (overTheFlowDepth > 0 && !node.children().isEmpty()) {
            // Inside a node laid over the flow every container is only what it holds: its text
            // goes in text boxes, not in columns, a line pair, a band or a panel in the flow.
            drawOutlineOf(node);
            for (DocumentNode child : inPaintOrder(node)) {
                writeNode(document, child);
            }
            return;
        }
        if (node instanceof com.demcha.compose.document.node.LayerStackNode stack) {
            DocxLayerColumns.Plan columns = DocxLayerColumns.of(stack, layout,
                    candidate -> (candidate instanceof SectionNode || candidate instanceof ContainerNode)
                                 && paintOf(candidate).isEmpty(),
                    candidate -> (candidate instanceof SectionNode || candidate instanceof ContainerNode)
                                 && !paintOf(candidate).isEmpty(),
                    this::drawnOverTheColumns);
            if (columns != null) {
                // Side by side, nothing in the stack overlaps: it is not an overlay.
                writeLayerColumns(document, stack, columns);
                return;
            }
        }
        // Columns lay nothing over anything, and are not an overlay a shape is drawn under (see
        // drawsInFront). A stack of one layer is kept as one: a template nests a row of icons
        // and labels in it, and where the editor sets the labels a little differently from the
        // page, an icon drawn in front of the text would stand over them.
        boolean overlay = isOverlay(node);
        if (overlay) {
            openOverlays.push(node);
        }
        try {
            writeNodeContentOf(document, node);
        } finally {
            if (overlay) {
                openOverlays.pop();
            }
        }
    }

    /**
     * Whether a node in the flow is laid over it: an overlay the page gives no room, its margins
     * taking back its whole height, and holding text that a text box can set where the page does.
     *
     * <p>{@code LumaStudioInvoice}'s sidebar — the brand block, its lockup and the ornament under
     * them — is one such container, pulled up over the page's top margin and handing its height
     * back below. Its drawings were drawn where the page puts them, but the lockup's five lines
     * were written in the flow, and the masthead beside them stood under them in Word, 120pt low,
     * and the invoice ran to a second page.</p>
     */
    private boolean laidOverTheFlow(DocumentNode node) {
        // In a cell — a panel's, whose shading both editors paint over a box behind the text —
        // it is written as before.
        if (overlayDepth != 0 || currentCell != null || surfaceBehind != null || !isOverlay(node)
            || Double.isNaN(canvasHeight)) {
            return false;
        }
        // No room at all: margins taking back more than the box pull the flow up, which nothing
        // written out of it could do.
        com.demcha.compose.document.layout.PlacedNode box = layout.placement(node);
        if (box == null || box.startPage() != box.endPage()
            || Math.abs(node.margin().top() + box.placementHeight() + node.margin().bottom()) > 0.5) {
            return false;
        }
        boolean[] holdsText = {false};
        return floatable(node, holdsText) && holdsText[0];
    }

    /**
     * Whether every leaf under a node is drawn where the page puts it or is a paragraph a text
     * box holds as the page sets it: plain runs, on one page, with no link, bookmark or anchor,
     * and no block under it anchored or transformed.
     *
     * @param holdsText set when a paragraph is found
     */
    private boolean floatable(DocumentNode node, boolean[] holdsText) {
        // A bookmark goes round what a block writes in the flow, and this one writes nothing there.
        com.demcha.compose.document.style.DocumentTransform transform = transformOf(node);
        if (transform != null && !transform.isIdentity() || blockAnchorOf(node, true) != null) {
            return false;
        }
        if (node instanceof ParagraphNode paragraph) {
            com.demcha.compose.document.layout.PlacedNode placed = layout.placement(paragraph);
            if (placed == null || placed.startPage() != placed.endPage()
                || paragraph.linkTarget() != null || paragraph.bookmarkOptions() != null
                || paragraph.anchor() != null && !paragraph.anchor().isBlank()) {
                return false;
            }
            for (InlineRun run : paragraph.inlineRuns()) {
                if (!(run instanceof InlineTextRun text) || text.linkTarget() != null) {
                    return false;
                }
            }
            holdsText[0] = true;
            return true;
        }
        if (node.children().isEmpty()) {
            return isDrawing(node);
        }
        if (!(node instanceof com.demcha.compose.document.node.LayerStackNode
              || node instanceof ShapeContainerNode || node instanceof SectionNode || node instanceof ContainerNode)) {
            return false;
        }
        for (DocumentNode child : node.children()) {
            if (!floatable(child, holdsText)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Writes a node laid over the flow ({@link #laidOverTheFlow}): its drawings where the page
     * draws them, and each of its paragraphs in a text box where the page sets it. Nothing of it
     * is written in the flow, which it takes no room in: the containers inside it are only what
     * they hold (see {@link #writeNodeContent}), so none of their edges is owed as space.
     */
    private void writeOverTheFlow(XWPFDocument document, DocumentNode node) throws Exception {
        drawOutlineOf(node);
        overlayDepth++;
        overTheFlowDepth++;
        try {
            for (DocumentNode child : inPaintOrder(node)) {
                writeNode(document, child);
            }
        } finally {
            overlayDepth--;
            overTheFlowDepth--;
        }
    }

    /**
     * A node's children in the order the page paints them: a stack's or a container's layers
     * by their z-index, the ones sharing one in the order they were given. Drawings are stacked
     * in the order they are queued, so a fill a later layer lies over is drawn under it.
     */
    private static List<DocumentNode> inPaintOrder(DocumentNode node) {
        List<com.demcha.compose.document.node.LayerStackNode.Layer> layers =
                node instanceof com.demcha.compose.document.node.LayerStackNode stack ? stack.layers()
                : node instanceof ShapeContainerNode container ? container.layers()
                : null;
        if (layers == null) {
            return node.children();
        }
        return layers.stream()
                .sorted(java.util.Comparator.comparingInt(com.demcha.compose.document.node.LayerStackNode.Layer::zIndex))
                .map(com.demcha.compose.document.node.LayerStackNode.Layer::node)
                .toList();
    }

    /** How far a line set in a text box over the flow may run past its box before Word breaks it. */
    private static final double TEXT_BOX_SLACK_RATIO = 0.25;

    /**
     * Sets a paragraph laid over the flow in a text box where the page sets it, with nothing
     * drawn round it (see {@link #laidOverTheFlow}).
     *
     * <p>The box is the paragraph's content box. A line set in one is given a quarter again of
     * the box's width, and a few points, away from the side it is aligned to — both ways for a
     * centred one: an editor sets text a little wider than the page, and the line would
     * otherwise break inside the box. A paragraph of several lines keeps its width but for the
     * editor's couple of points, as it breaks where the page breaks it — and the box is a line
     * taller than its lines, as a text box shows nothing past its foot and a word the editor
     * sets on one more line would be lost. The box draws nothing, so the room it has past the
     * text is nowhere to be seen, and it stands in front of the text: a fill or a panel's
     * shading the page lays under it cannot cover it.</p>
     */
    private void writeTextOverTheFlow(XWPFDocument document, ParagraphNode node) {
        com.demcha.compose.document.layout.PlacedNode placed = layout.placement(node);
        XWPFParagraph para = detachedParagraph(document);
        boolean rightToLeft = ParagraphDirection.resolve(node) == TextDirection.RTL;
        para.setAlignment(toAlignment(node.align(), rightToLeft));
        applyDirection(para, rightToLeft);
        applyLineHeight(para, layout.lineHeight(node));
        applyLineGap(para, layout.lineGap(node), layout.lineCount(node));
        writeParagraphRuns(para, node, rightToLeft);

        double x = placed.placementX() + node.padding().left();
        double width = Math.max(1, placed.placementWidth() - node.padding().horizontal());
        double height = Math.max(1, placed.placementHeight() - node.padding().vertical());
        double top = canvasHeight - placed.placementY() - placed.placementHeight() + node.padding().top();
        int lines = Math.max(1, layout.lineCount(node));
        double spare = lines == 1 ? width * TEXT_BOX_SLACK_RATIO + EDITOR_SLACK_POINTS : EDITOR_SLACK_POINTS;
        height += height / lines;
        // The alignment is a side of the page's, whichever way the text runs (see toAlignment).
        if (node.align() == TextAlign.CENTER) {
            x -= spare / 2;
        } else if (node.align() == TextAlign.RIGHT) {
            x -= spare;
        }
        anchors.queue(List.of(DocxDrawings.Shape.textBox(x, top, width + spare, height, placed.startPage(),
                paragraphXml(para))));
        report.add(DocxExportReport.Severity.APPROXIMATED, node.nodeKind(), layout.pathOf(node),
                "laid over the flow, which gives it no room: set in a text box where the page sets it");
    }

    /**
     * Whether a shape drawn now may stand in front of the text: inside a painted panel, whose
     * shading both editors paint over what lies behind the text, and only when nothing of the
     * overlays it is drawn in is text or a picture — a disc under its initials, a ring round a
     * photo, a pill under its label stay behind what they frame.
     */
    private boolean drawsInFront() {
        if (surfaceBehind == null) {
            return false;
        }
        for (DocumentNode overlay : openOverlays) {
            // An empty container is its outline alone — a lone ring — and frames nothing.
            if (!overlay.children().isEmpty() && !onlyDrawn(overlay)) {
                return false;
            }
        }
        return true;
    }

    private void writeNodeContentOf(XWPFDocument document, DocumentNode node) throws Exception {
        // A title and its dates at either end of one band are one line before they are layers.
        if (node instanceof com.demcha.compose.document.node.LayerStackNode
            || node instanceof ShapeContainerNode) {
            DocxLinePair.Pair pair = DocxLinePair.of(node, layout);
            if (pair != null) {
                // A pill's outline round its title and dates is drawn as writeShapeContainer draws it.
                drawOutlineOf(node);
                writeLinePair(document, node, pair);
                return;
            }
        }
        if (node instanceof com.demcha.compose.document.node.LayerStackNode || node instanceof ShapeContainerNode) {
            for (DocumentNode layer : node.children()) {
                if (layer instanceof ImageNode image && drawnBesideItsText(image, node)) {
                    picturesDrawnBeside.add(image);
                }
            }
        }
        if (node instanceof com.demcha.compose.document.node.LayerStackNode
            || node instanceof ShapeContainerNode && writesOneLayer(node)) {
            DocxLayerColumns.Band band = DocxLayerColumns.band(node, layout, drawnIn(node));
            if (band != null) {
                if (node instanceof ShapeContainerNode container) {
                    writeShapeContainer(document, container, band);
                } else {
                    writeOverlayBand(document, node, band);
                }
                return;
            }
        }
        // Only in the flow: inside an overlay, the overlay's own place already holds it. Inside
        // layer stacks of one layer only it is in the flow — a template wraps a row in one to
        // nest it — and a drawing with a sibling after it takes its room there as anywhere else:
        // IndigoProposal's meta tiles set a disc over each label, and without the disc's room
        // every label stood 24pt high. The last in its parent, an icon in a section of its own
        // beside a heading, is held by its row (holdRowAtLeast).
        if ((node instanceof com.demcha.compose.document.node.LayerStackNode || node instanceof ShapeContainerNode)
            && onlyDrawn(node)
            && (overlayDepth == 0 || overlayDepth == oneLayerDepth && layout.followedInItsParent(node))
            && holdTheSpaceOf(node)) {
            // Its drawing is drawn and takes no room: the space held above is its room.
            drawOutlineOf(node);
            ShapeContainerNode outerClip = clipContainer;
            if (node instanceof ShapeContainerNode badge
                && badge.clipPolicy() == com.demcha.compose.document.style.ClipPolicy.CLIP_PATH) {
                clipContainer = badge;
            }
            overlayDepth++;
            try {
                for (DocumentNode child : node.children()) {
                    writeNode(document, child);
                }
            } finally {
                overlayDepth--;
                clipContainer = outerClip;
            }
            return;
        }
        boolean overlay = isOverlay(node);
        boolean oneLayer = isOneLayer(node);
        if (overlay) {
            overlayDepth++;
        }
        if (oneLayer) {
            oneLayerDepth++;
        }
        try {
            dispatchNode(document, node);
        } finally {
            if (overlay) {
                overlayDepth--;
            }
            if (oneLayer) {
                oneLayerDepth--;
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

    /** Whether a node is a layer stack of one layer, which lays nothing over anything. */
    private static boolean isOneLayer(DocumentNode node) {
        return node instanceof com.demcha.compose.document.node.LayerStackNode stack && stack.layers().size() == 1;
    }

    /**
     * Whether a drawing node stands in the flow, so that a rule is written as one.
     *
     * <p>Outside every overlay it does. Inside layer stacks of one layer only, a line does too:
     * such a stack lays nothing over anything, and its lines are rules — taken for drawing,
     * {@code CharcoalGold}'s rules between its certifications were shapes that took no room,
     * and each entry stood their height and the space under them too high. Its other shapes
     * stay drawing: a template wraps a row in one to nest it, and a small accent bar in that
     * row is not a divider.</p>
     */
    private static boolean inTheFlow(DocumentNode node, int overlays, int oneLayerOverlays) {
        return overlays == 0
               || node instanceof com.demcha.compose.document.node.LineNode && overlays == oneLayerOverlays;
    }

    /** The rule a node is in the flow, or {@code null} — over something else it is not one. */
    private DocxRules.Rule ruleOf(DocumentNode node) {
        return inTheFlow(node, overlayDepth, oneLayerDepth) ? DocxRules.of(node) : null;
    }

    private void dispatchNode(XWPFDocument document, DocumentNode node) throws Exception {
        DocxRules.Rule rule = ruleOf(node);
        if (overTheFlowDepth == 0 && (node instanceof ParagraphNode
                || node instanceof com.demcha.compose.document.node.PageReferenceNode || rule != null)) {
            standsIntoTheSpaceAbove(node);
        }
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
            writeShapeContainer(document, shapeContainer, null);
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
            // Geometry-only node kinds (line, ellipse, shape, path, polygon) have no
            // semantic Word analogue: what a shape can show is drawn where the page puts
            // it, and the rest is warned about once per kind, so a dropped path or icon is
            // visible in the log instead of silently missing.
            warnUnsupported(node);
            // In the flow it still takes its room; over something else it takes none.
            if (overlayDepth == 0 && isDrawing(node)) {
                holdTheSpaceOf(node);
            }
        }
    }

    /**
     * Draws a node's drawing as shapes where the page puts it, or, when no shape can show it,
     * drops it with one warning per kind, deduplicated across the export.
     *
     * <p>The report is told about every one of them, not one per kind: a caller asking
     * what the document lost wants the three charts it lost, and which three. The log is
     * the summary and the report is the record.</p>
     */
    private void warnUnsupported(DocumentNode node) {
        if (drawOwnFragments(node) || drawnByItsTable(node)) {
            // Drawn: by the node, or, composed in a cell, by its table.
            return;
        }
        if (warnedNodeKinds.add(node.nodeKind())) {
            LOG.warn("DocxSemanticBackend: dropping '{}' node(s) — geometry has no semantic "
                     + "Word analogue; use the PDF backend for pixel-perfect output", node.nodeKind());
        }
        report.add(DocxExportReport.Severity.DROPPED, node.nodeKind(), layout.pathOf(node),
                "geometry has no semantic Word analogue, so it is not in the document at all");
    }

    /**
     * Draws the shapes a node paints itself where the page puts them, anchored in a paragraph
     * written on their page (see {@link DocxDrawings} and {@link DocxDrawingAnchors}).
     *
     * @return whether the node painted anything a shape shows
     */
    private boolean drawOwnFragments(DocumentNode node) {
        boolean overlayFront = drawsInFront();
        boolean front = false;
        boolean drew = false;
        drewInCell = false;
        for (com.demcha.compose.document.layout.PlacedFragment fragment : layout.ownFragments(node)) {
            if (badgeText != null && !Double.isNaN(canvasHeight)) {
                // A badge holding its initials: in front, the text being its own.
                List<DocxDrawings.Shape> shapes = DocxDrawings.of(fragment, canvasHeight).stream()
                        .map(shape -> shape.holding(badgeText).inFront()).toList();
                queueDrawings(shapes, false);
                drew |= !shapes.isEmpty();
                front |= !shapes.isEmpty();
                continue;
            }
            // On a painted surface — a panel, or a filled cell — whose shading both editors paint
            // over what lies behind the text, a shape stands in front unless it frames a line of
            // text or a picture on its page: a template sets an icon beside its heading through a
            // stack of one layer, which kept every payment panel's heading icon behind its
            // shading. Elsewhere the overlays it sits in decide (drawsInFront). A badge whose
            // glyph is drawn over it in front (drawnOverItsBadge) stands in front too: the
            // picture it frames is its own, not the flow's.
            boolean inFront = surfaceBehind != null && !Double.isNaN(canvasHeight)
                    ? holdsItsDrawnGlyph(node) || !framesText(fragment, layout.textOnPage(fragment.pageIndex()))
                    : overlayFront;
            if (queueDrawing(fragment, inFront)) {
                drew = true;
                front |= inFront;
            }
        }
        if (drew) {
            StringBuilder message = new StringBuilder(drewInCell
                    ? "drawn as a shape anchored in the table cell it fills, where the layout puts it "
                      + "in the cell: it moves with the row"
                    : "drawn as a shape anchored to the page where the "
                      + "layout puts it: it stays there when the text around it is edited");
            if (badgeText != null) {
                message.append("; its text is held in the shape, which stands in front of the text");
            } else if (front) {
                message.append("; in front of the text, since the panel's shading is painted over "
                        + "shapes behind it");
            } else if (surfaceBehind != null) {
                message.append("; behind the text it frames, where the panel's shading hides it");
            }
            com.demcha.compose.document.style.DocumentTransform transform = transformOf(node);
            if (transform != null && !transform.isIdentity()) {
                message.append("; its transform is not carried, so it is drawn upright at its size");
            }
            if (node instanceof com.demcha.compose.document.node.PathNode path && losesStyle(path)) {
                message.append("; its gradient paint, dash pattern, caps and joins are not carried");
            }
            report.add(DocxExportReport.Severity.APPROXIMATED, node.nodeKind(), layout.pathOf(node),
                    message.toString());
        }
        return drew;
    }

    /**
     * Draws the outline a shape container paints round its layers, or reports it lost when no
     * shape shows it — a path, a polygon, a star.
     */
    private void drawOutlineOf(DocumentNode node) {
        if (!drawOwnFragments(node) && !drawnByItsTable(node)
            && node instanceof ShapeContainerNode container
            && (container.fillColor() != null || container.stroke() != null && container.stroke().width() > 0)) {
            String outline = container.outline().getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
            report.add(DocxExportReport.Severity.DROPPED, "shape container outline", layout.pathOf(node),
                    composedInACell(node)
                            ? "a " + outline + " outline composed inside a table cell has no place in the "
                              + "layout to be drawn at, so it is not in the document"
                            : "a " + outline + " outline has no shape this export draws, so it is not in the "
                              + "document");
        }
    }

    /** Whether a path is painted or stroked in a way custom geometry does not say. */
    private static boolean losesStyle(com.demcha.compose.document.node.PathNode path) {
        return path.fillPaint() != null && !(path.fillPaint() instanceof com.demcha.compose.document.style.DocumentPaint.Solid)
               || path.strokePaint() != null
                  && !(path.strokePaint() instanceof com.demcha.compose.document.style.DocumentPaint.Solid)
               || path.dashPattern() != null && !path.dashPattern().isSolid()
               || path.stroke() != null
                  && (path.lineCap() != com.demcha.compose.document.style.DocumentLineCap.BUTT
                      || path.lineJoin() != com.demcha.compose.document.style.DocumentLineJoin.MITER);
    }

    private static com.demcha.compose.document.style.DocumentTransform transformOf(DocumentNode node) {
        if (node instanceof com.demcha.compose.document.node.ShapeNode shape) {
            return shape.transform();
        }
        if (node instanceof com.demcha.compose.document.node.EllipseNode ellipse) {
            return ellipse.transform();
        }
        if (node instanceof com.demcha.compose.document.node.LineNode line) {
            return line.transform();
        }
        return node instanceof ShapeContainerNode container ? container.transform() : null;
    }

    private boolean queueDrawing(com.demcha.compose.document.layout.PlacedFragment fragment, boolean front) {
        if (Double.isNaN(canvasHeight)) {
            return false;
        }
        return queueDrawings(DocxDrawings.of(fragment, canvasHeight), front);
    }

    private boolean queueDrawings(List<DocxDrawings.Shape> shapes, boolean front) {
        if (front) {
            // A painted panel is a shaded cell, and both editors paint a cell's shading over
            // what lies behind the text: in front of it, the shape shows.
            shapes = shapes.stream().map(DocxDrawings.Shape::inFront).toList();
        }
        DrawingCell cell = drawingCell;
        if (cell != null && !shapes.isEmpty() && shapes.stream().allMatch(cell::holds)) {
            anchors.anchorInCell(cell.carrier(), anchors.ordered(shapes), cell.origin());
            drewInCell = true;
            return true;
        }
        anchors.queue(shapes);
        return !shapes.isEmpty();
    }

    /**
     * A table cell holding one drawing and nothing else, and where the page puts it.
     *
     * @param carrier the cell's paragraph, held at the drawing's height
     * @param origin  where the page puts the drawing's top-left corner, which the paragraph's
     *                top and the cell's text column stand at
     * @param width   the drawing's width, in points
     * @param height  the drawing's height, in points
     * @param page    the page it is laid out on, counted within the section
     */
    private record DrawingCell(XWPFParagraph carrier, DocxDrawings.CellOrigin origin, double width, double height,
                               int page) {

        /** Whether a shape lies in the drawing's box, a point's stroke either side allowed. */
        boolean holds(DocxDrawings.Shape shape) {
            double slack = 1;
            return shape.page() == page
                   && shape.x() >= origin.x() - slack && shape.x() + shape.width() <= origin.x() + width + slack
                   && shape.top() >= origin.top() - slack
                   && shape.top() + shape.height() <= origin.top() + height + slack;
        }
    }

    /**
     * The cell a drawing is written alone in, when its shapes can be anchored in that cell's
     * paragraph rather than on the page; {@code null} when they cannot.
     *
     * <p>Every other shape is placed from the page's edges (see {@link DocxDrawingAnchors}), which
     * holds it where the page puts it — and, in a table, off the row it belongs to wherever Word
     * sets the rows above a little taller or shorter than the page. {@code CobaltRota}'s band
     * icons, each alone in the first column of its navy strip, stood 4pt, 9pt and 14pt above
     * their labels, the last out of its strip. A drawing that is all its cell holds is placed
     * from that cell's paragraph instead, held at the drawing's height: the row carries it.</p>
     *
     * <p>Only a layer stack or shape container that only draws and holds no line (a rule), with no
     * margins, laid out on one page, painting nothing outside its box, in a cell holding nothing
     * else yet: the cell's text column then starts where the drawing does, the paragraph's top
     * where its top is, and the cell holds all of it.</p>
     */
    private DrawingCell drawingCellFor(XWPFTableCell cell, DocumentNode node) {
        if (Double.isNaN(canvasHeight)
            || !(node instanceof com.demcha.compose.document.node.LayerStackNode || node instanceof ShapeContainerNode)
            || !onlyDrawn(node) || DocxCellDrawings.holdsALine(node)) {
            return null;
        }
        com.demcha.compose.document.style.DocumentInsets margin = node.margin();
        if (margin != null && (margin.top() != 0 || margin.right() != 0 || margin.bottom() != 0 || margin.left() != 0)) {
            return null;
        }
        com.demcha.compose.document.layout.PlacedNode placed = layout.placement(node);
        if (placed == null || placed.startPage() != placed.endPage() || !(placed.placementHeight() > 0)
            || !holdsNothingYet(cell) || !paintsInsideItsBox(node, placed)) {
            return null;
        }
        XWPFParagraph carrier = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
        CTPPr properties = carrier.getCTP().isSetPPr() ? carrier.getCTP().getPPr() : carrier.getCTP().addNewPPr();
        CTSpacing spacing = properties.isSetSpacing() ? properties.getSpacing() : properties.addNewSpacing();
        spacing.setBefore(BigInteger.ZERO);
        spacing.setAfter(BigInteger.ZERO);
        spacing.setLineRule(STLineSpacingRule.EXACT);
        spacing.setLine(BigInteger.valueOf(Math.max(2, toTwips(placed.placementHeight()))));
        double top = canvasHeight - placed.placementY() - placed.placementHeight();
        return new DrawingCell(carrier, new DocxDrawings.CellOrigin(placed.placementX(), top),
                placed.placementWidth(), placed.placementHeight(), placed.startPage());
    }

    /**
     * Whether everything a node and the nodes inside it paint lies in its box on its page, a
     * point either side allowed: the cell then holds the whole drawing, or none of it.
     */
    private boolean paintsInsideItsBox(DocumentNode node, com.demcha.compose.document.layout.PlacedNode box) {
        for (com.demcha.compose.document.layout.PlacedFragment fragment : layout.ownFragments(node)) {
            double slack = 1;
            if (fragment.pageIndex() != box.startPage()
                || fragment.x() < box.placementX() - slack
                || fragment.x() + fragment.width() > box.placementX() + box.placementWidth() + slack
                || fragment.y() < box.placementY() - slack
                || fragment.y() + fragment.height() > box.placementY() + box.placementHeight() + slack) {
                return false;
            }
        }
        for (DocumentNode child : node.children()) {
            if (!paintsInsideItsBox(child, box)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A paragraph a hairline tall before a table in the body, holding no space of its own:
     * the body paragraph a page laid out entirely in a table otherwise lacks.
     */
    private static XWPFParagraph openBefore(XWPFDocument document, XWPFTable table) {
        try (org.apache.xmlbeans.XmlCursor cursor = table.getCTTbl().newCursor()) {
            XWPFParagraph opening = document.insertNewParagraph(cursor);
            CTPPr properties = opening.getCTP().isSetPPr() ? opening.getCTP().getPPr() : opening.getCTP().addNewPPr();
            CTSpacing spacing = properties.isSetSpacing() ? properties.getSpacing() : properties.addNewSpacing();
            spacing.setBefore(BigInteger.ZERO);
            spacing.setAfter(BigInteger.ZERO);
            spacing.setLineRule(STLineSpacingRule.EXACT);
            spacing.setLine(BigInteger.valueOf(2));
            return opening;
        }
    }

    /**
     * Anchors the drawings no body paragraph on their page carried (see
     * {@link DocxDrawingAnchors}): on the section's first page in a hairline paragraph opened
     * before the table it starts with; on its last in the paragraph closing the section, or one
     * of their own a point tall; on any other in a cell's paragraph, reported since Word may
     * print them clipped to the cell. Any left are reported as dropped.
     *
     * @param closer the paragraph closing a section that ends with a table, or null
     */
    private void reportDrawingsLeftOver(XWPFDocument document, XWPFParagraph closer) {
        int lastPage = Math.max(0, layout.pageCount() - 1);
        List<IBodyElement> body = document.getBodyElements();
        XWPFTable opensWith = sectionFirstElement < body.size() && body.get(sectionFirstElement) instanceof XWPFTable first
                ? first : null;
        XWPFParagraph firstOnThePage = anchors.bodyParagraphOn(0);
        XWPFParagraph firstInACell = anchors.cellParagraphOn(0);
        // A page of one table whose row is all its height has no room for a hairline over it:
        // LibreOffice moved MidnightNavy's row onto a second page under one. Its backgrounds and
        // shapes are carried by its first cell's paragraph instead, laid out from the page —
        // measured in Word 16.0.20430, PDF and screen, a shape anchored there out of the cell's
        // bounds is drawn whole.
        boolean inTheFirstCell = !backgroundsInBody.isEmpty() && opensWith != null && firstInACell != null;
        XWPFParagraph[] opened = {inTheFirstCell ? firstInACell : null};
        java.util.function.Supplier<XWPFParagraph> opening = opensWith == null ? null
                : () -> opened[0] != null ? opened[0] : (opened[0] = openBefore(document, opensWith));
        XWPFParagraph[] closed = {closer};
        java.util.function.Supplier<XWPFParagraph> closing =
                () -> closed[0] != null ? closed[0] : (closed[0] = collapsed(document.createParagraph()));
        DocxDrawingAnchors.Leftovers leftovers = anchors.endSection(lastPage, opening, closing);
        if (!backgroundsInBody.isEmpty()) {
            XWPFParagraph carrier = firstOnThePage != null ? firstOnThePage
                    : opening != null ? opening.get()
                    : closing.get();
            XWPFRun run = carrier.insertNewRun(0);
            for (int order = 0; order < backgroundsInBody.size(); order++) {
                run.getCTR().addNewDrawing().set(
                        DocxPageBackgrounds.drawing(backgroundsInBody.get(order), nextDrawingId++, order, false));
            }
        }
        String where = sectioned ? "section " + (sectionIndex + 1) + ", " : "";
        leftovers.inCells().forEach((page, count) -> report.add(DocxExportReport.Severity.APPROXIMATED,
                "drawing", where + "page " + (page + 1),
                count + " shape(s) anchored in a table cell, the page having no other paragraph: Word may "
                + "print them clipped to the cell"));
        leftovers.dropped().forEach((page, count) -> report.add(DocxExportReport.Severity.DROPPED, "drawing",
                where + "page " + (page + 1),
                count + " shape(s) on a page no paragraph was written on, so nothing carries them"));
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
        double previousLineGap = listLineGap;
        java.util.ArrayDeque<Integer> previousItemLines = listItemLines;
        // The list's lineSpacing stands between the lines of an item that wraps, and only
        // there: each item is a paragraph of its own, so each is given it by its own lines
        // (see applyLineGap). The items are matched to the layout's in order; where the
        // two do not count the same items, none is given it.
        listLineGap = layout.lineGap(list);
        List<Integer> laidOut = layout.itemLineCounts(list);
        listItemLines = laidOut.size() == itemCount(list)
                ? new java.util.ArrayDeque<>(laidOut)
                : new java.util.ArrayDeque<>();
        // Its own sides hold its items in, as a paragraph's hold its text (see writeParagraph):
        // NavySidebar indents its closing lists to clear the badge beside their heading, and
        // without it their markers stood under the badge, the text a line short in Word.
        double outerLeft = insetLeft;
        double outerRight = insetRight;
        if (overlayDepth == 0) {
            double spare = currentCell != null ? EDITOR_SLACK_POINTS : 0;
            double left = (list == leftMarginInCell ? 0 : list.margin().left()) + list.padding().left();
            insetLeft += Math.max(0, left - spare);
            insetRight += Math.max(0, list.margin().right() + list.padding().right() - spare);
        }
        try {
            writeListItems(document, list, numId);
        } finally {
            insetLeft = outerLeft;
            insetRight = outerRight;
            pendingItemSpacing = previousItemSpacing;
            anItemWasWritten = previousItemWritten;
            listLineGap = previousLineGap;
            listItemLines = previousItemLines;
        }
        owePendingSpacingAfter(list.margin().bottom() + list.padding().bottom());
    }

    /** Puts the list's gap between the lines of the item just started, if it wraps. */
    private void applyItemLineGap(XWPFParagraph item) {
        Integer lines = listItemLines.poll();
        if (lines != null) {
            applyLineGap(item, listLineGap, lines);
        }
    }

    /** How many items a list writes, nested ones included. */
    private static int itemCount(com.demcha.compose.document.node.ListNode list) {
        int count = 0;
        for (String item : list.items()) {
            if (!com.demcha.compose.document.node.ListMarker.normalizeItemText(item, list.normalizeMarkers()).isBlank()) {
                count++;
            }
        }
        java.util.ArrayDeque<com.demcha.compose.document.node.ListItem> nested = new java.util.ArrayDeque<>(list.nestedItems());
        while (!nested.isEmpty()) {
            com.demcha.compose.document.node.ListItem item = nested.pop();
            count++;
            nested.addAll(item.children());
        }
        return count;
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
                        layout.pathOf(list), layout.markerToText(list));
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
            // A nested item stands after its depth's indent, and an item may carry a marker of
            // its own: the list's measure of its first item's marker is only a top-level item's
            // that carries the list's.
            java.util.OptionalDouble markerToText = depth == 0 && marker.equals(list.marker())
                    ? layout.markerToText(list) : java.util.OptionalDouble.empty();
            writeRichListLine(document, list.textStyle(), marker, item, depth, lineHeight, layout.firstLine(list),
                    layout.pathOf(list), markerToText);
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
        applyItemLineGap(para);
        if (numId != null) {
            para.setNumID(numId);
            para.setNumILvl(BigInteger.valueOf(depth));
            indentListItemInside(para, depth);
        }
        XWPFRun run = para.createRun();
        applyStyle(run, style);
        setTextBrokenAtLines(run, numId != null ? text : "  ".repeat(depth) + text);
        styleTheMark(para, style);
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
            // As applyInset writes it: a list in a container hanging left hangs with it, and in a
            // cell of a row hanging left it moves left by the hang, as a paragraph beside it does.
            indent.setLeft(BigInteger.valueOf(leftIndentTwips(insetLeft + cellTextShift) + levelLeft));
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
     * marker column, the shared content origin — is not written, for the reason
     * {@code hangingIndent} documents; the gap after a marker that is a picture
     * alone is, as a tab to where the layout starts the text. Which piece of an item is bold
     * is not geometry: Word carries a style per run inside a paragraph, so
     * writing the item's plain reading in one face would be dropping something
     * Word can hold.</p>
     *
     * <p>The same is true of a marker. A marker written as text keeps the colour
     * and face it was given, because those are run properties Word has. A marker
     * that draws a disc or an icon is written as the picture it draws, the way a
     * shape or an icon in a line is, rather than as a glyph the author did not ask
     * for — and is followed by that tab, or, where the layout measured no gap or the
     * picture reaches the stop, by the same space as a text marker.</p>
     *
     * @param markerToText how far right of its marker the layout starts the item's text, or
     *                     empty when that is not this item's — a nested item, or one with a
     *                     marker of its own
     */
    private void writeRichListLine(XWPFDocument document, DocumentTextStyle style,
                                   com.demcha.compose.document.node.ListMarker marker,
                                   com.demcha.compose.document.node.ListItem item,
                                   int depth,
                                   java.util.OptionalDouble lineHeight,
                                   java.util.Optional<com.demcha.compose.document.layout.payloads.ParagraphLine> line,
                                   String path, java.util.OptionalDouble markerToText) {
        warnDroppedInlineRuns(marker.runs(), path);
        warnDroppedInlineRuns(item.runs(), path);
        line = line.map(listLine -> itemLine(listLine, item.runs()));
        spaceBeforeTheNextItem();
        XWPFParagraph para = newBodyParagraph(document);
        applyLineHeight(para, lineHeight);
        applyItemLineGap(para);
        XWPFRun leading = para.createRun();
        applyStyle(leading, style);
        leading.setText("  ".repeat(depth) + (marker.isRich() ? "" : marker.prefix()));
        PictureReach pictures = PictureReach.NONE;
        if (marker.isRich()) {
            pictures = writeInlineTextRuns(para, style, marker.runs(), path, line);
            // A space is what separates a marker of text from its item, as on the text path:
            // Word sets the marker in its own widths. A marker that drew a picture is followed
            // by a tab where the layout measured its gap; one whose picture had no data drew
            // nothing, and gets no space either.
            boolean drawnOnly = com.demcha.compose.document.node.InlineRun.plainText(marker.runs()).isBlank();
            if (!drawnOnly || pictures.reach() > 0) {
                XWPFRun gap = para.createRun();
                applyStyle(gap, style);
                // A picture alone: a blank run beside it is no text the page keeps — the layout
                // drops it — and is a space Word does set, which the stop is not measured past.
                boolean pictureAlone = marker.runs().stream().allMatch(run -> textOf(run) == null);
                if (pictureAlone && markerToText.isPresent()
                    && markerToText.getAsDouble() > drawnWidth(para) + MARKER_TAB_CLEARANCE) {
                    // A marker that is a picture alone is drawn at the size it is written, so its
                    // text can stand where the page sets it, the marker's width and markerGap past
                    // it: a tab to a stop there. A space put TealPulse's skills and highlights
                    // 6.3 to 6.7pt left of the page's, the gap after their dots 9.2pt where a
                    // space is 2.5. Not where the picture, its edges included, reaches the stop: the tab
                    // would run on to Word's next default stop, half an inch on.
                    gap.addTab();
                    tabTo(para, markerToText.getAsDouble());
                } else {
                    gap.setText(" ");
                }
            }
        }
        if (item.isRich()) {
            pictures = pictures.max(writeInlineTextRuns(para, style, item.runs(), path, line));
        } else {
            XWPFRun label = para.createRun();
            applyStyle(label, style);
            setTextBrokenAtLines(label, item.label());
        }
        makeRoomForPictures(para, pictures);
        styleTheMark(para, style);
    }

    /** How far past a list marker's picture its tab stop must stand for the tab to reach it, in points. */
    private static final double MARKER_TAB_CLEARANCE = 0.1;

    /** How wide the pictures written inline in a paragraph so far are, in points. */
    private static double drawnWidth(XWPFParagraph para) {
        double width = 0;
        for (XWPFRun run : para.getRuns()) {
            for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing drawing : run.getCTR().getDrawingList()) {
                for (var inline : drawing.getInlineList()) {
                    if (inline.getExtent() != null) {
                        width += inline.getExtent().getCx() / (double) org.apache.poi.util.Units.EMU_PER_POINT;
                    }
                }
            }
        }
        return width;
    }

    /**
     * Sets a left tab stop a distance right of where a paragraph's first line starts: past its
     * left indent and its first-line indent, as Word measures a tab stop from the column's edge.
     *
     * @param points how far right of the first line's start, in points
     */
    private static void tabTo(XWPFParagraph para, double points) {
        CTPPr properties = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        long start = 0;
        if (properties.isSetInd()) {
            CTInd indent = properties.getInd();
            start = twipsOf(indent.isSetLeft() ? indent.getLeft() : null)
                    + twipsOf(indent.isSetFirstLine() ? indent.getFirstLine() : null)
                    - twipsOf(indent.isSetHanging() ? indent.getHanging() : null);
        }
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabs tabs =
                properties.isSetTabs() ? properties.getTabs() : properties.addNewTabs();
        CTTabStop stop = tabs.addNewTab();
        stop.setVal(STTabJc.LEFT);
        stop.setPos(BigInteger.valueOf(start + Math.round(points * POINT_TO_TWIP)));
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
            setTextBrokenAtLines(docRun, text.text());
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
        // Nothing takes a wrapper's last line's overhang from the gap under it, as
        // hangBelowItsBox does a shape container's, so that line keeps its own height.
        holdStackedLines(node.children(), Double.NaN);
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

    /**
     * The room a panel in a cell leaves past its right border, in points: Word draws the cell's
     * gridline on screen at the cell's edge, over a border that meets it.
     */
    private static final double CLEAR_OF_THE_GRIDLINE_POINTS = 0.5;

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
        double topNotTaken = 0;
        if (first) {
            // Word draws a row's top and bottom borders outside its shading, so the table is as
            // much taller than the panel as its borders are thick. The page strokes them on the
            // box's edge, taking no room. So the border comes out of the space above, and so
            // does the one a panel just above could not take out of its own space below.
            topNotTaken = Math.max(0, strokeWidth(borders.top()) - Math.max(0, pendingSpacingAfter - borderBelow));
            pendingSpacingAfter = Math.max(0, pendingSpacingAfter - strokeWidth(borders.top()) - borderBelow);
            borderBelow = 0;
        }
        holdTheSpaceAboveATable(document);
        double width = panelWidth(node);

        double edge = insetLeft + margin.left();
        double indent = currentCell == null ? edge + padding.left() - halfLeft : edge - halfLeft;
        // In a cell, Word starts a nested table no further left than the cell's text, draws its
        // right border outside the table's right edge — measured in its PDF, a table ending at
        // 566.0pt had its right border from 566.2 to 566.9 — and on screen cuts off whatever
        // passes the cell's edge and draws the cell's gridline over it: a panel as wide as its
        // cell lost its right border, EditorialProposal's glance card. A panel with a right
        // border that reaches its cell's text edge ends that border and a little more short of
        // it, the points taken from its right margin and no more than it holds, so its text keeps
        // its place and its width.
        double shortOfTheEdge = 0;
        double room = currentCell != null && Double.isFinite(currentCellWidth) && strokeWidth(borders.right()) > 0
                ? currentCellWidth - insetRight - Math.max(0, indent) - strokeWidth(borders.right())
                  - CLEAR_OF_THE_GRIDLINE_POINTS
                : Double.NaN;

        XWPFTable table = newTable(document, 1, 1);
        hideTableGrid(table);
        XWPFTableCell cell = table.getRow(0).getCell(0);
        if (Double.isFinite(width) && width > 0) {
            double outer = width + halfLeft + halfRight;
            if (room > 0 && outer > room) {
                // No more than the right margin holds: past that the text would narrow and wrap,
                // which is worse than a border the cell's edge covers.
                shortOfTheEdge = Math.min(outer - room, insideTheBorders(padding, borders).right());
                outer -= shortOfTheEdge;
            }
            setTableWidth(table, outer);
            writeGrid(table, new double[]{outer});
            CTTcPr properties = cellProperties(cell);
            CTTblWidth cellWidth = properties.isSetTcW() ? properties.getTcW() : properties.addNewTcW();
            cellWidth.setType(STTblWidth.DXA);
            cellWidth.setW(BigInteger.valueOf(toTwips(outer)));
        }
        applyCellPaint(cell, paint.fill(), null);
        paintCellSides(cell, paint.borders());
        DocumentInsets margins = insideTheBorders(padding, borders);
        applyCellPadding(cell, new DocumentInsets(margins.top(), Math.max(0, margins.right() - shortOfTheEdge),
                margins.bottom(), margins.left()));
        if (node.keepTogether() && layout.onOnePage(node)) {
            table.getRow(0).setCantSplitRow(true);
        }
        if (node instanceof ShapeContainerNode) {
            // The page centres a shape's layers in it, in the outline's height the row is held to.
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        }

        cell.removeParagraph(0);
        DocumentColor outerSurface = surfaceBehind;
        double outerCellWidth = currentCellWidth;
        if (paint.fill() != null) {
            surfaceBehind = paint.fill();
        }
        currentCellWidth = Double.isFinite(width) ? width - padding.left() - padding.right() : Double.NaN;
        XWPFTableCell outerPanelCell = panelCell;
        panelCell = cell;
        if (first && last) {
            cutTheLabelToItsOutline(node, borders, cell);
        }
        try {
            writeInCell(cell, () -> writeChildren(cell.getXWPFDocument(), children, spacingOf(node)));
        } finally {
            surfaceBehind = outerSurface;
            currentCellWidth = outerCellWidth;
            panelCell = outerPanelCell;
        }
        if (cell.getParagraphs().isEmpty()) {
            // A panel with nothing Word can hold inside is its padding tall on the page, not a
            // line of text taller.
            holdToHairline(cell.addParagraph());
        }
        // Word starts the cell's content below its top border, or its top margin where that is
        // wider, the border then standing inside the margin; the page starts it the padding below
        // the panel's edge. What of the border no space above took, less the padding, is how far
        // low the content would stand: a padding as wide as the border holds it.
        double low = topNotTaken - padding.top();
        takeTheTopBorderInside(cell, low);
        // Where its padding does not hold its top border, Word draws both borders outside the
        // row's height, the panel's top where the space above put it: so the height held is the
        // page's less the part of the top border no space above took and less the bottom border.
        // holdRowAtLeast already takes the heavier of the two off; the rest comes off here. A
        // padding that holds the border leaves the height as it was: Word draws the border inside
        // the margin, and InvoiceMetered's card moved its top down taking it off.
        double bordersOutside = low > 0
                ? Math.max(0, topNotTaken + strokeWidth(borders.bottom())
                              - Math.max(strokeWidth(borders.top()), strokeWidth(borders.bottom())))
                : 0;
        com.demcha.compose.document.layout.PlacedNode placed = first && last && layout.onOnePage(node) ? layout.placement(node) : null;
        if (placed != null && placed.placementHeight() > 0) {
            // Its height is the page's: what makes a panel taller than its text — an icon drawn
            // where the page puts it, a fixed outline — is not in the cell. MerchantInvoice's
            // due-date card closed from 59.4pt to its text's 26, and its calendar hung below it.
            // Measured on MerchantInvoice's payment panel, Word drew it 0.8pt taller than the page
            // without the borders outside taken off, and as tall with them; LibreOffice, whose
            // height there its content sets, draws it that border's width shorter.
            holdRowAtLeast(table.getRow(0), placed.placementHeight() - bordersOutside);
        } else if (first && last && layout.placement(node) == null && node instanceof ShapeContainerNode shape
                   && shape.outline().height() > 0) {
            // Composed in a table cell, it has no placement; its outline states its height, as it
            // states its width (panelWidth). CobaltRota's shift chips, 17.5pt outlines round a
            // line of text, closed to the text's 12.7pt in Word. Less the borders drawn outside:
            // its outlined chips, 9.2pt outlines with a 1.125pt border, stood 10.3pt tall in Word
            // and 10.2pt in LibreOffice, each row holding one 1.1pt taller than the page's.
            holdRowAtLeast(table.getRow(0), shape.outline().height() - bordersOutside);
        }

        // In the body it is written even when it is 0. The indent places the cell's text, less
        // half its left border, and with none Word 16 places the table's edge on the margin
        // instead, its text a padding further in (measured): IndigoProposal's about band, bled to the paper's edge by a negative margin its
        // padding takes back, stood 27.6pt right of the page's, fill and text.
        if (indent != 0 || currentCell == null) {
            CTTblPr tableProperties = table.getCTTbl().getTblPr();
            CTTblWidth tableIndent = tableProperties.isSetTblInd()
                    ? tableProperties.getTblInd()
                    : tableProperties.addNewTblInd();
            tableIndent.setType(STTblWidth.DXA);
            // Signed: a nested panel with no margin starts half its border left of the cell, and
            // Word starts it at the cell's text.
            tableIndent.setW(BigInteger.valueOf(Math.round(indent * POINT_TO_TWIP)));
        }
        if (last) {
            double below = strokeWidth(borders.bottom());
            owePendingSpacingAfter(Math.max(0, margin.bottom() - below));
            borderBelow = Math.max(0, below - margin.bottom());
        }
    }

    /**
     * Takes how far a panel's content would stand low in Word out of the space above its first
     * paragraph, as far as that holds.
     *
     * <p>Word draws a cell's top border above its content unless the cell's top margin is wider;
     * the page strokes it on the panel's edge. Where the panel has no padding to hold the border
     * and less space above it than the border — it opens a cell, or follows nothing — every line
     * inside stood the border's width low: {@code MerchantInvoice}'s payment panel, first in its
     * row's cell, stood 0.9pt low in Word and pushed its footer onto a second page. The space above
     * its first paragraph — the room the page leaves over its heading — takes it instead.</p>
     *
     * @param low how far low the content would stand, in points
     */
    private static void takeTheTopBorderInside(XWPFTableCell cell, double low) {
        if (!(low > 0) || cell.getBodyElements().isEmpty()
            || !(cell.getBodyElements().get(0) instanceof XWPFParagraph first)) {
            return;
        }
        CTPPr properties = first.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()) {
            return;
        }
        CTSpacing spacing = properties.getSpacing();
        long before = twipsOf(spacing.getBefore());
        long taken = Math.min(before, toTwips(low));
        if (taken > 0) {
            spacing.setBefore(BigInteger.valueOf(before - taken));
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
        // The space owed is the block's own edge and the edges of the containers opened
        // around it, which are carried down to it rather than owed.
        if (pendingSpacingAfter + carriedSpacingBefore - borderBelow > 0 && lastBodyParagraph == null && !tableAbove) {
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
        if (placed.isEmpty() && node instanceof ShapeContainerNode shape) {
            // Composed in a table cell, it has no placement; its outline states its size, within
            // the cell it is composed in, and it gets the same slack below.
            placed = java.util.OptionalDouble.of(Double.isFinite(available)
                    ? Math.min(shape.outline().width(), available)
                    : shape.outline().width());
        }
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
    /**
     * Writes a container's children one after another, with the container's spacing between
     * each two of them.
     *
     * <p>The layout puts a container's {@code spacing} between every two neighbouring
     * children, whatever they are, and the export left it out: a sidebar laid out with 9pt
     * between its blocks came out with its blocks touching. It is space below the child above,
     * owed like any other, so it lands above whatever the next child writes first — a
     * paragraph's space above, or the paragraph before a table. Before a page break it is
     * not owed, as the page ends there; after one it is, as the layout starts the next page
     * that far down.</p>
     */
    private void writeChildren(XWPFDocument document, List<DocumentNode> children, double spacing)
            throws Exception {
        for (int index = 0; index < children.size(); index++) {
            DocumentNode child = children.get(index);
            if (index > 0 && spacing > 0 && !(child instanceof PageBreakNode)) {
                owePendingSpacingAfter(spacing);
            }
            writeNode(document, child);
        }
    }

    /** The space a container puts between its children, zero for any other node. */
    private static double spacingOf(DocumentNode node) {
        if (node instanceof SectionNode section) {
            return section.spacing();
        }
        if (node instanceof ContainerNode container) {
            return container.spacing();
        }
        return 0;
    }

    private void writeContainerBody(XWPFDocument document, DocumentNode node) throws Exception {
        double carriedFromOutside = carriedSpacingBefore;
        long blocksBefore = blocksWritten;
        carriedSpacingBefore += node.margin().top() + node.padding().top();
        riseIntoItsLine(node);
        // The sides are carried the same way, as the indent of every paragraph inside: a
        // container's content starts inside its margin and its padding on the page, and was
        // written flush with the page margin, the card's text touching the card's edge.
        double outerLeft = insetLeft;
        double outerRight = insetRight;
        insetLeft += node.margin().left() + node.padding().left();
        insetRight += node.margin().right() + node.padding().right();
        // An alignment's child is narrower than the width it is set in, and stands where the
        // alignment puts it: a portrait centred in a sidebar stood at the sidebar's left edge.
        if (node instanceof com.demcha.compose.document.node.AlignNode align) {
            placeAcross(align, align.child());
        }
        try {
            writeChildren(document, node.children(), spacingOf(node));
        } finally {
            insetLeft = outerLeft;
            insetRight = outerRight;
        }
        owePendingSpacingAfter(node.margin().bottom() + node.padding().bottom());
        // A container that wrote something has had its top edge taken — by its first
        // paragraph, or owed above its first table (see newTable) — and none of it is left
        // waiting. A container that wrote nothing at all — empty, or holding nothing but a
        // drawing inside an overlay, where the drawing holds no room of its own — stood above
        // nothing, and the containers around it are still waiting for their first paragraph:
        // only its own edge goes, and theirs is handed back. Dropping theirs too lost a
        // sidebar's top padding under the portrait that opened it. A drawing in the flow
        // counts as a block (holdTheSpaceOf).
        carriedSpacingBefore = blocksWritten == blocksBefore ? carriedFromOutside : 0;
    }

    /**
     * Lets a container in a cell that pulls its first line up above the cell — a negative top
     * edge — do so inside that line.
     *
     * <p>A Word paragraph starts no higher than its cell. {@code WorkspaceInvoice}'s title is set
     * 4pt above its masthead row by the cell's padding, and in Word stood 4pt low with the whole
     * page under it; the row opens the page, and there is no space above to lift it into (see
     * {@link #standsAboveItsCell}). A first child of one line is written that much shorter
     * instead, its text seated where the page sets it (see {@link DocxStackedLines.Line}), so
     * the row is as tall as the page's and the title stands where the page puts it. That holds
     * wherever the container stands in its cell: after other blocks, the space owed above it is
     * written, and the line rises from there as the page's does.</p>
     *
     * <p>Word draws an exact line's text on screen only inside the line, so the line gives up no
     * more than the room above its letters (see {@link DocxInk}): an accent on a capital pulled
     * up further would be cut. What it cannot give stays where it did, the line starting at the
     * cell's top. A line whose letters cannot be read — a picture in it — is left as it was.</p>
     */
    private void riseIntoItsLine(DocumentNode node) {
        if (currentCell == null || node.children().isEmpty() || !(node.children().get(0) instanceof ParagraphNode first)) {
            return;
        }
        Double risen = risenLines.get(first);
        if (risen != null) {
            // Written again — a header on every page class: the line is already cut, and the
            // edge it rises by is taken as it was the first time.
            carriedSpacingBefore += risen;
            return;
        }
        // What the paragraph would have above it before its own edge, which is written apart
        // (applyVerticalSpacing, or standsIntoTheSpaceAbove for a negative one, after this): the
        // edges carried down to it and the space the block before it owes, less a border
        // standing below that block. Only what that comes short of zero is a rise; the rest is
        // written as space.
        double rise = -(carriedSpacingBefore + pendingSpacingAfter - borderBelow);
        if (!(rise > 0.01) || layout.lineCount(first) != 1 || stackedLineHeights.containsKey(first)) {
            return;
        }
        java.util.OptionalDouble own = layout.lineHeight(first);
        java.util.Optional<com.demcha.compose.document.layout.payloads.ParagraphLine> line = layout.firstLine(first);
        double[] ink = inkOf(first);
        if (own.isEmpty() || line.isEmpty() || ink == null) {
            return;
        }
        double aboveTheLetters = line.get().lineHeight() - line.get().baselineOffsetFromBottom() - ink[0]
                                 - DocxStackedLines.INK_MARGIN;
        double taken = Math.min(rise, aboveTheLetters);
        if (!(taken > 0.01) || taken >= own.getAsDouble()) {
            return;
        }
        stackedLineHeights.put(first, new DocxStackedLines.Line(own.getAsDouble() - taken, -taken, 0));
        risenLines.put(first, taken);
        carriedSpacingBefore += taken;
    }

    /**
     * Cuts the one line of text a shape composed in a table cell centres in it to the room its
     * outline leaves, where the page lets the line pass the outline and its letters fit inside.
     *
     * <p>The page centres the label's line in the shape and draws it past the outline where the
     * line is taller. Word grows the panel's row to the line: {@code CobaltRota}'s stacked shift
     * chips, 9.2pt outlines round 8.2pt text on a 10pt line, each came out 0.8pt taller, and an
     * outlined one its borders taller again, as Word keeps a cell's borders outside its content —
     * every staff row with two shifts in a day stood 3.8pt taller than the page's. The line is
     * written as tall as the room Word leaves the cell's content — the outline less the margins
     * written and the borders — its text seated where the page sets it
     * ({@link DocxStackedLines.Line}). Both sides are cut alike, since the cell centres the line,
     * and no closer to the letters on either side than {@link DocxStackedLines#INK_MARGIN}: an
     * outlined chip, its borders leaving less room than its letters, is cut as far as they allow
     * and stays that much taller. Only a label the shape centres top to bottom is cut: one set
     * from an edge overflows on one side.</p>
     */
    private void cutTheLabelToItsOutline(DocumentNode node, DocumentBorders borders, XWPFTableCell cell) {
        if (!(node instanceof ShapeContainerNode shape) || layout.placement(node) != null) {
            return;
        }
        ParagraphNode label = null;
        for (com.demcha.compose.document.node.LayerStackNode.Layer layer : shape.layers()) {
            if (layer.node() instanceof ParagraphNode paragraph) {
                // Only a line the page centres top to bottom passes the outline equally above
                // and below it; one set from an edge, or moved, overflows on one side.
                com.demcha.compose.document.node.LayerAlign align = layer.align();
                boolean centred = align == com.demcha.compose.document.node.LayerAlign.CENTER
                                  || align == com.demcha.compose.document.node.LayerAlign.CENTER_LEFT
                                  || align == com.demcha.compose.document.node.LayerAlign.CENTER_RIGHT;
                if (label != null || !centred || layer.offsetY() != 0) {
                    return;
                }
                label = paragraph;
            } else if (!isDrawing(layer.node())) {
                return;
            }
        }
        if (label == null || layout.lineCount(label) != 1 || stackedLineHeights.containsKey(label)
            || label.margin().top() != 0 || label.margin().bottom() != 0
            || label.padding().top() != 0 || label.padding().bottom() != 0) {
            return;
        }
        java.util.Optional<com.demcha.compose.document.layout.payloads.ParagraphLine> line = layout.firstLine(label);
        double[] ink = inkOf(label);
        if (line.isEmpty() || ink == null) {
            return;
        }
        // What Word leaves the content of the cell the panel is: the outline less the margins
        // written — its padding, less the half of each border the page draws inside it — and the
        // borders Word keeps outside the content.
        double room = shape.outline().height() - (cellMargin(cell, true) + cellMargin(cell, false)) / POINT_TO_TWIP
                      - strokeWidth(borders.top()) - strokeWidth(borders.bottom());
        double lineHeight = line.get().lineHeight();
        double over = (lineHeight - room) / 2;
        if (!(over > 0.01) || !(room > 0)) {
            return;
        }
        double baseline = line.get().baselineOffsetFromBottom();
        // Both sides are cut alike, as far as the page passes the outline and no closer to the
        // letters than the margin on either side: the cell centres the line, so a cut deeper on one
        // side stood the text off the page's by half the difference — 0.42pt high for an outlined
        // chip whose foot the letters left less room at.
        double cut = Math.min(over, Math.min(lineHeight - baseline - ink[0], baseline - ink[1])
                                    - DocxStackedLines.INK_MARGIN);
        if (!(cut > 0.01)) {
            return;
        }
        stackedLineHeights.put(label, new DocxStackedLines.Line(lineHeight - 2 * cut, -cut, 0));
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
     * <p>The part also states the paragraph defaults: no space after a paragraph, single
     * lines. The export writes the space around a block only where the page has some, and
     * Word fills whatever a document leaves unsaid from its own new-document template —
     * 8pt after every paragraph and lines 1.08 tall. That put 8pt under each paragraph
     * written without a {@code w:after}, the last line of every table cell among them, in
     * Word only: LibreOffice reads the missing value as none, as the page does.</p>
     *
     * <p>When the graph carries no text to take a default from, the part holds the
     * paragraph defaults alone.</p>
     */
    private void writeStylesPart(XWPFDocument document) {
        CTStyles styles = CTStyles.Factory.newInstance();
        var docDefaults = styles.addNewDocDefaults();
        CTSpacing spacing = docDefaults.addNewPPrDefault().addNewPPr().addNewSpacing();
        spacing.setAfter(BigInteger.ZERO);
        spacing.setLine(BigInteger.valueOf(240));
        spacing.setLineRule(STLineSpacingRule.AUTO);

        DocumentTextStyle defaults = documentDefaultStyle;
        if (defaults == null) {
            document.createStyles().setStyles(styles);
            return;
        }
        applyDefaultRunProperties(docDefaults.addNewRPrDefault().addNewRPr(), defaults);

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
        // Each node with how many overlays it lies in, and how many of those are stacks of one
        // layer, which decide whether a rule is written.
        java.util.ArrayDeque<java.util.Map.Entry<DocumentNode, int[]>> pending = new java.util.ArrayDeque<>();
        for (DocumentNode root : graph.roots()) {
            pending.push(java.util.Map.entry(root, new int[]{0, 0}));
        }
        while (!pending.isEmpty()) {
            java.util.Map.Entry<DocumentNode, int[]> next = pending.pop();
            DocumentNode node = next.getKey();
            int overlays = next.getValue()[0];
            int oneLayers = next.getValue()[1];
            String anchor = node instanceof ParagraphNode paragraph
                    ? paragraph.anchor()
                    : blockAnchorOf(node, inTheFlow(node, overlays, oneLayers));
            if (anchor != null && !anchor.isBlank()) {
                anchors.add(anchor.trim());
            }
            // As writeNodeContent counts them: a stack or a shape holding only drawing is an
            // overlay of its own before it is a stack of one layer.
            int[] below;
            if (overlays == 0 && onlyDrawing(node) && !node.children().isEmpty()
                && (node instanceof com.demcha.compose.document.node.LayerStackNode
                    || node instanceof ShapeContainerNode)) {
                below = new int[]{1, oneLayers};
            } else {
                below = new int[]{overlays + (isOverlay(node) ? 1 : 0), oneLayers + (isOneLayer(node) ? 1 : 0)};
            }
            for (DocumentNode child : node.children()) {
                pending.push(java.util.Map.entry(child, below));
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

    /**
     * Whether a shape container is written as a panel — a table of one cell carrying its fill and
     * outline — rather than drawn.
     *
     * <p>A container the layout composes inside a table cell has no place of its own in the
     * layout, so its outline has no position to be drawn at, and the export dropped it: every
     * shift chip of a rota, a filled pill with its hours inside, came out as bare text. A
     * rectangle or a rounded rectangle, filled or outlined, is a panel as a card is, with its
     * text inside a cell painted its colour, and its corners squared.</p>
     */
    private boolean writtenAsAPanel(ShapeContainerNode node) {
        boolean boxed = node.outline() instanceof com.demcha.compose.document.style.ShapeOutline.Rectangle
                        || node.outline() instanceof com.demcha.compose.document.style.ShapeOutline.RoundedRectangle
                        || node.outline() instanceof com.demcha.compose.document.style.ShapeOutline.RoundedRectanglePerCorner;
        boolean painted = node.fillColor() != null || node.stroke() != null && node.stroke().width() > 0;
        // One holding only drawing — an icon's tile — has nothing a cell can hold, and a panel
        // with nothing inside is a sliver, or, held to the tile's height, a row taller than the
        // page's on every line: an invoice ran onto a second page. It stays unwritten. One holding
        // an empty container or a spacer is a panel a hairline tall, its colour still there.
        return boxed && painted && !onlyDrawn(node) && composedInACell(node);
    }

    /**
     * Whether a node is laid out but has no place of its own in the layout — content composed
     * inside a table cell. A document with no layout at all has no place for anything, and is
     * not this.
     */
    private boolean composedInACell(DocumentNode node) {
        return !layout.isEmpty() && layout.placement(node) == null && layout.ownFragments(node).isEmpty();
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
                : node instanceof ContainerNode container
                  ? hasRadius(container.cornerRadius())
                  : node instanceof ShapeContainerNode shape
                    && (shape.outline() instanceof com.demcha.compose.document.style.ShapeOutline.RoundedRectangle round
                        && round.cornerRadius() > 0
                        || shape.outline() instanceof com.demcha.compose.document.style.ShapeOutline.RoundedRectanglePerCorner corners
                           && hasRadius(corners.corners()));
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
        resumeHere();
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
        blocksWritten++;
        applyInset(para);
        // Everything owed above this paragraph — the space the one before it holds below
        // itself, and any container edge — is written here, on one side of the gap.
        // A card's border standing below the card took as much of the gap (writePanelPiece).
        double owed = carriedSpacingBefore + pendingSpacingAfter - borderBelow;
        double above = Math.max(0, owed - pullBelow);
        // What the space owed cannot give of the pull, the paragraph's own top edge gives.
        pullLeft = Math.max(0, pullBelow - Math.max(0, owed));
        pullLeftOn = pullLeft > 0 ? para : null;
        borderBelow = 0;
        pullBelow = 0;
        // Text that hung below the band above this paragraph already took that much of the
        // gap (see writeLinePair); whatever the owed space cannot give back, the paragraph's
        // own margin gives (applyVerticalSpacing).
        double overhang = hangingBelow;
        hangingBelow = 0;
        double taken = Math.min(Math.max(0, above), overhang);
        above -= taken;
        hangingOver = overhang - taken > 0 ? para : null;
        hangingOverBy = overhang - taken;
        if (above > 0) {
            addSpacing(para, above, 0);
        }
        carriedSpacingBefore = 0;
        pendingSpacingAfter = 0;
        lastBodyParagraph = para;
        anchors.paragraphOn(currentPage, para, currentCell == null);
        return para;
    }

    /**
     * Leaves out the space a section still holds below its last block.
     *
     * <p>A section ends its page, so nothing below the last block needs the space the block
     * holds under itself: it is blank paper either way. Written, it can only push that block
     * onto a page of its own. A two-column CV whose column ends with 36.5pt of padding runs
     * its last line exactly to the page's foot on the page; LibreOffice set the line 0.9pt
     * lower, found no room for the line and its space together, and moved the line to a
     * second page. So the space owed at the end is dropped, and so is the space held below the
     * last line of each cell in the last row of a table the section ends with, and of the
     * tables such a cell ends with in turn — unless the cell is painted or has a bottom edge
     * drawn, where that space is part of the box the reader sees.</p>
     *
     * @return the paragraph written to close a section that ends with a table, or null
     */
    private XWPFParagraph dropTheSpaceAtTheEnd(XWPFDocument document) {
        pendingSpacingAfter = 0;
        carriedSpacingBefore = 0;
        pullBelow = 0;
        List<IBodyElement> body = document.getBodyElements();
        if (!body.isEmpty() && body.get(body.size() - 1) instanceof XWPFTable table) {
            dropTheSpaceBelow(table);
            // Word cannot end a section with a table: it puts a paragraph of its own after it,
            // a line of the document's text tall, and one that finds no room under a table
            // reaching the page's foot opens a blank page. This one is a point tall.
            return collapsed(document.createParagraph());
        }
        return null;
    }

    /** Leaves out the space below the last line of each cell of a table's last row that shows none of it. */
    private static void dropTheSpaceBelow(XWPFTable table) {
        if (table.getRows().isEmpty() || drawn(tableBottom(table))) {
            return;
        }
        for (XWPFTableCell cell : table.getRow(table.getRows().size() - 1).getTableCells()) {
            CTTcPr properties = cell.getCTTc().getTcPr();
            if (properties != null && (properties.isSetShd()
                    || properties.isSetTcBorders() && drawn(properties.getTcBorders().getBottom()))) {
                continue;
            }
            List<IBodyElement> content = cell.getBodyElements();
            int last = content.size() - 1;
            if (last < 0) {
                continue;
            }
            if (content.get(last) instanceof XWPFParagraph paragraph) {
                CTPPr lastProperties = paragraph.getCTP().getPPr();
                if (lastProperties != null && lastProperties.isSetSpacing() && lastProperties.getSpacing().isSetAfter()) {
                    lastProperties.getSpacing().unsetAfter();
                }
                // A cell cannot end with a table in Word, so one it ends with is followed by
                // the paragraph that closes it.
                if (last > 0 && content.get(last - 1) instanceof XWPFTable inner && paragraph.getText().isEmpty()) {
                    dropTheSpaceBelow(inner);
                }
            } else if (content.get(last) instanceof XWPFTable inner) {
                dropTheSpaceBelow(inner);
            }
        }
    }

    private static CTBorder tableBottom(XWPFTable table) {
        CTTblPr properties = table.getCTTbl().getTblPr();
        return properties != null && properties.isSetTblBorders() ? properties.getTblBorders().getBottom() : null;
    }

    /** Whether a border is one the reader sees. */
    private static boolean drawn(CTBorder border) {
        return border != null && border.getVal() != STBorder.NONE && border.getVal() != STBorder.NIL;
    }

    /**
     * Takes a paragraph's, page reference's or rule's negative top edge out of the space owed
     * above it.
     *
     * <p>Word has no negative space above a paragraph, and {@link #applyVerticalSpacing}
     * writes an edge only where it is positive, so an edge pulling the block up was dropped
     * and the block stood that much low, with everything after it. {@code PaymentsInvoice}'s
     * header rule is pulled 16.4pt up from the foot of the stack above it, whose band runs
     * past its content: the pull was lost, and the rule and the page under it stood 10pt low,
     * the 16.4pt less the 6.3pt its metadata grid had lost above it. The pull is added
     * to the edges carried down to the block, as a container's negative edge already is, so
     * {@link #newBodyParagraph} nets it against everything owed above, as the page sums it.
     * Where that cannot give it all, a paragraph takes the rest off the foot of the line written
     * just before it ({@link #takeFromTheLineAbove}), then off the top of its own line
     * ({@link #riseInsideItsOwnLine}); what neither can give stays unwritten. Text laid over the flow owes no
     * space ({@link #writeOverTheFlow}), so its edges move nothing.</p>
     */
    private void standsIntoTheSpaceAbove(DocumentNode node) {
        double edge = node.margin().top() + node.padding().top();
        if (edge < 0) {
            carriedSpacingBefore += edge;
            double shortBy = -(carriedSpacingBefore + pendingSpacingAfter - borderBelow);
            if (shortBy > 0.01 && node instanceof ParagraphNode paragraph) {
                shortBy -= takeFromTheLineAbove(shortBy, paragraph);
                if (shortBy > 0.01) {
                    riseInsideItsOwnLine(paragraph, shortBy);
                }
            }
        }
    }

    /**
     * Takes up to {@code points} off the foot of the one line written just before, where its
     * letters leave that room, keeping its text where it stood; how much it took is given back to
     * the space above the next block.
     *
     * <p>A paragraph pulled up into the one above it, with nothing above it to give the pull —
     * the two lines of a lockup in a table cell, {@code CobaltRota}'s subtitle 6.5pt up under
     * its 21pt wordmark — stood that much low in Word, and the row and the page under it with
     * it. The line above is written shorter by what its letters do not reach, and its text,
     * which Word would raise with the baseline of a shorter exact line, lowered as much.</p>
     *
     * <p>Only an exact line of text alone in its paragraph, not one of a stack, with no space
     * written below it and no run shaded or underlined: a shaded run fills the line in Word, and
     * an underline is drawn below the letters. Where both are laid out, the line must end on the
     * page the pulled paragraph starts on: the last line of a page shortened would move where
     * Word breaks it.</p>
     *
     * @return how far the line above was shortened, in points
     */
    private double takeFromTheLineAbove(double points, ParagraphNode pulled) {
        ParagraphNode above = lastWrittenNode;
        XWPFParagraph para = lastWrittenParagraph;
        if (above == null || para == null || para != lastBodyParagraph || layout.lineCount(above) != 1
            || stackedLineHeights.containsKey(above)) {
            return 0;
        }
        com.demcha.compose.document.layout.PlacedNode abovePlaced = layout.placement(above);
        com.demcha.compose.document.layout.PlacedNode pulledPlaced = layout.placement(pulled);
        if (abovePlaced != null && pulledPlaced != null && abovePlaced.endPage() != pulledPlaced.startPage()) {
            return 0;
        }
        for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run : runsIn(para)) {
            // A shaded run fills the line; an underline is drawn below the letters' ink.
            if (run.isSetRPr() && (run.getRPr().sizeOfShdArray() > 0 || run.getRPr().sizeOfUArray() > 0
                                   && run.getRPr().getUArray(0).getVal() != org.openxmlformats.schemas.wordprocessingml.x2006.main.STUnderline.NONE)) {
                return 0;
            }
        }
        CTPPr properties = para.getCTP().getPPr();
        CTSpacing spacing = properties != null && properties.isSetSpacing() ? properties.getSpacing() : null;
        Long line = spacing != null && spacing.isSetLineRule() && spacing.getLineRule() == STLineSpacingRule.EXACT
                ? writtenTwips(spacing.getLine()) : null;
        java.util.Optional<com.demcha.compose.document.layout.payloads.ParagraphLine> laid = layout.firstLine(above);
        // The letters' reach off their own baseline, before any seat: the position written on
        // the runs below carries the seat, as far as it was written.
        double[] ink = laid.isEmpty() ? null : DocxInk.of(laid.get(), measuredFonts());
        if (line == null || ink == null || spacing.isSetAfter()) {
            return 0;
        }
        // The room under the letters where Word draws them: its baseline stands a fifth of the
        // line above the foot, raised by the runs' written position; less the margin, and the
        // quarter point the lowering below can round away.
        List<org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR> runs = runsIn(para);
        long seated = 0;
        if (!runs.isEmpty() && runs.get(0).isSetRPr() && runs.get(0).getRPr().sizeOfPositionArray() > 0
            && runs.get(0).getRPr().getPositionArray(0).getVal() instanceof Number number) {
            seated = number.longValue();
        }
        double wordFoot = (1 - DocxTextBands.BASELINE_SHARE) * line / POINT_TO_TWIP + seated / HALF_POINTS_PER_POINT;
        double room = wordFoot - ink[1] - DocxStackedLines.INK_MARGIN - 0.5 / HALF_POINTS_PER_POINT;
        long taken = Math.round(Math.min(points, room) * POINT_TO_TWIP);
        if (taken <= 0 || taken >= line) {
            return 0;
        }
        spacing.setLine(BigInteger.valueOf(line - taken));
        // Word stands an exact line's baseline four fifths of the way down it: shorter by the
        // taken twips, its text would rise by four fifths of them.
        long lowered = Math.round(taken / POINT_TO_TWIP * DocxTextBands.BASELINE_SHARE * HALF_POINTS_PER_POINT);
        for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run : runsIn(para)) {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr runProperties =
                    run.isSetRPr() ? run.getRPr() : run.addNewRPr();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSignedHpsMeasure position =
                    runProperties.sizeOfPositionArray() > 0 ? runProperties.getPositionArray(0) : runProperties.addNewPosition();
            long raised = position.getVal() instanceof Number number ? number.longValue() : 0;
            position.setVal(BigInteger.valueOf(raised - lowered));
        }
        double takenPoints = taken / POINT_TO_TWIP;
        carriedSpacingBefore += takenPoints;
        return takenPoints;
    }

    /**
     * Rises a one-line paragraph inside its own line by up to {@code points}, where the room above
     * its letters allows, as {@link #riseIntoItsLine} does for a container's first line.
     */
    private void riseInsideItsOwnLine(ParagraphNode paragraph, double points) {
        if (layout.lineCount(paragraph) != 1 || stackedLineHeights.containsKey(paragraph)) {
            return;
        }
        java.util.OptionalDouble own = layout.lineHeight(paragraph);
        java.util.Optional<com.demcha.compose.document.layout.payloads.ParagraphLine> line = layout.firstLine(paragraph);
        double[] ink = inkOf(paragraph);
        if (own.isEmpty() || line.isEmpty() || ink == null) {
            return;
        }
        double aboveTheLetters = line.get().lineHeight() - line.get().baselineOffsetFromBottom() - ink[0]
                                 - DocxStackedLines.INK_MARGIN;
        double taken = Math.min(points, aboveTheLetters);
        if (!(taken > 0.01) || taken >= own.getAsDouble()) {
            return;
        }
        stackedLineHeights.put(paragraph, new DocxStackedLines.Line(own.getAsDouble() - taken, -taken, 0));
        carriedSpacingBefore += taken;
    }

    /**
     * Gives back space already owed above the next block, most recent first: text standing
     * above its band took it on the page.
     *
     * @return what the space owed could not give, in points
     */
    private double takeBackSpaceAbove(double points) {
        double fromOwed = Math.min(pendingSpacingAfter, points);
        pendingSpacingAfter -= fromOwed;
        double fromCarried = Math.min(carriedSpacingBefore, points - fromOwed);
        carriedSpacingBefore -= fromCarried;
        return points - fromOwed - fromCarried;
    }

    /**
     * Notes that text opening a cell of a table's first row stands above the cell on the page,
     * by {@code points}, for {@link #raiseRows} to lift the row once the table is written.
     *
     * <p>A Word paragraph cannot reach above the top of its cell. A CV entry laid out as a row
     * — a timeline marker beside the entry — raises its title out of its band to centre it on
     * the marker, so the title stood that much lower in Word, and every entry after it lower
     * again.</p>
     */
    private void standsAboveItsCell(double points) {
        if (!(points > 0) || currentCell == null || !currentCell.getBodyElements().isEmpty()) {
            return;
        }
        // A padded cell — a card's — has the room above its text inside it, and the page takes
        // the text up into that padding while the box stays where it is: so does the cell.
        CTTcPr cellProperties = currentCell.getCTTc().isSetTcPr() ? currentCell.getCTTc().getTcPr() : null;
        if (cellProperties != null && cellProperties.isSetTcMar() && cellProperties.getTcMar().isSetTop()) {
            var top = cellProperties.getTcMar().getTop();
            long padding = twipsOf(top.getW());
            long taken = Math.min(padding, toTwips(points));
            if (taken > 0) {
                top.setW(BigInteger.valueOf(padding - taken));
                points -= taken / POINT_TO_TWIP;
            }
        }
        // A painted or framed cell is a box on the page, and the box does not move for the
        // text in it: what its padding cannot take stays low rather than lift the box.
        if (!(points > 0.01) || cellProperties != null && (cellProperties.isSetShd()
                || cellProperties.isSetTcBorders() && drawn(cellProperties.getTcBorders().getTop()))) {
            return;
        }
        XWPFTableRow row = currentCell.getTableRow();
        XWPFTable table = row.getTable();
        if (table.getRow(0) != row) {
            return;
        }
        raisedRows.computeIfAbsent(table, key -> new java.util.LinkedHashMap<>())
                .merge(currentCell, points, Math::max);
    }

    /**
     * Lifts each noted table's first row by what its highest text stands above it, as far as
     * the gap above the table allows: that gap is held by the paragraph just before the table —
     * below it, or above it when it is an empty separator — or, when that paragraph only keeps
     * two tables apart and holds none of it, by the foot of the table above
     * ({@link #takeFromTheFoot}). Every other cell of the row then starts that much lower inside,
     * and the cell whose text stood out by less than the lift starts lower by the difference.
     */
    private void raiseRows() {
        for (java.util.Map.Entry<XWPFTable, java.util.Map<XWPFTableCell, Double>> noted : raisedRows.entrySet()) {
            XWPFTable table = noted.getKey();
            List<IBodyElement> around = table.getBody().getBodyElements();
            int index = around.indexOf(table);
            if (index <= 0 || !(around.get(index - 1) instanceof XWPFParagraph above)) {
                continue;
            }
            CTPPr properties = above.getCTP().getPPr();
            if (properties == null || !properties.isSetSpacing()) {
                continue;
            }
            CTSpacing gap = properties.getSpacing();
            long after = gap.isSetAfter() ? twipsOf(gap.getAfter()) : 0;
            // An empty paragraph between two blocks is a separator, except a rule: its space
            // above is where the page draws the line.
            boolean separator = above.getText().isEmpty() && !properties.isSetPBdr();
            long before = separator && gap.isSetBefore() ? twipsOf(gap.getBefore()) : 0;
            long wanted = toTwips(java.util.Collections.max(noted.getValue().values()));
            long lift = Math.min(after + before, wanted);
            // The paragraph keeping two tables apart takes no room — a drawing anchored to the page
            // may hang on it — and keeps with the table after.
            boolean betweenTables = separator && !takesRoom(above) && properties.isSetKeepNext()
                    && index >= 2 && around.get(index - 2) instanceof XWPFTable;
            if (lift <= 0 && betweenTables && around.get(index - 2) instanceof XWPFTable previous) {
                // Between two tables the gap is the one above's: its cells end with it.
                lift = takeFromTheFoot(previous, wanted);
                if (lift > 0) {
                    liftEachCell(table, noted.getValue(), lift);
                }
                continue;
            }
            if (lift <= 0) {
                continue;
            }
            long fromAfter = Math.min(after, lift);
            if (fromAfter > 0) {
                gap.setAfter(BigInteger.valueOf(after - fromAfter));
            }
            if (lift > fromAfter) {
                gap.setBefore(BigInteger.valueOf(before - (lift - fromAfter)));
            }
            liftEachCell(table, noted.getValue(), lift);
        }
        raisedRows.clear();
    }

    /**
     * Starts each cell of a lifted table's first row that much lower inside, less what its own
     * text stood above the row: the cell whose text stood out by the whole lift keeps its place.
     */
    private void liftEachCell(XWPFTable table, java.util.Map<XWPFTableCell, Double> standing, long lift) {
        for (XWPFTableCell cell : table.getRow(0).getTableCells()) {
            long own = Math.min(lift, toTwips(standing.getOrDefault(cell, 0.0)));
            if (lift - own > 0 && !cell.getBodyElements().isEmpty()
                && cell.getBodyElements().get(0) instanceof XWPFParagraph first) {
                addSpacing(first, (lift - own) / POINT_TO_TWIP, 0);
            }
        }
    }

    /**
     * Takes up to {@code wanted} twips from the space the table above ends with — the space below
     * the last paragraph of each cell of its last row — and returns what was taken.
     *
     * <p>Two tables in a row are held apart by an empty paragraph that carries nothing, so the
     * gap between them is the first one's: its cells end with it. A timeline's entries are one
     * table each, and the space under an entry's separator rule is the gap the next entry's
     * title rises into on the page: {@code SerifHeadline}'s entries each stood a further 3pt
     * lower in Word without it.</p>
     *
     * <p>A row is as tall as its tallest cell, so every cell ending with space gives the same
     * amount — the least any of them has — and the row is shorter by exactly that. Nothing is
     * taken when a cell that takes room ends with no space under it: it may be the tallest, and
     * the row would not shrink at all. A cell that takes none — a timeline's marker, drawn where
     * the page draws it — does not count. Nor from a row held to a height, which the space it
     * gives would not shorten, nor from a painted or framed one: the box does not move for the
     * text after it, as it does not for the text in it ({@link #standsAboveItsCell}).</p>
     */
    private static long takeFromTheFoot(XWPFTable previous, long wanted) {
        if (previous.getRows().isEmpty() || drawn(tableBottom(previous))) {
            return 0;
        }
        XWPFTableRow lastRow = previous.getRow(previous.getRows().size() - 1);
        if (lastRow.getCtRow().isSetTrPr() && lastRow.getCtRow().getTrPr().sizeOfTrHeightArray() > 0) {
            return 0;
        }
        List<CTSpacing> feet = new ArrayList<>();
        long amount = wanted;
        for (XWPFTableCell cell : lastRow.getTableCells()) {
            if (paintedOrFramed(cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : null)) {
                return 0;
            }
            List<IBodyElement> content = cell.getBodyElements();
            CTSpacing foot = content.isEmpty() || !(content.get(content.size() - 1) instanceof XWPFParagraph last)
                    ? null : spacingBelow(last);
            if (foot == null) {
                if (takesRoom(cell)) {
                    return 0;
                }
                continue;
            }
            feet.add(foot);
            amount = Math.min(amount, twipsOf(foot.getAfter()));
        }
        if (amount <= 0 || feet.isEmpty()) {
            return 0;
        }
        for (CTSpacing foot : feet) {
            foot.setAfter(BigInteger.valueOf(twipsOf(foot.getAfter()) - amount));
        }
        return amount;
    }

    /** A paragraph's spacing when it holds space below it, or {@code null}. */
    private static CTSpacing spacingBelow(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetAfter()
            || twipsOf(properties.getSpacing().getAfter()) <= 0) {
            return null;
        }
        return properties.getSpacing();
    }

    /**
     * Whether a cell's content takes room in it: text, a picture set in a line, a table nested in
     * it. A shape anchored to the page takes none.
     */
    private static boolean takesRoom(XWPFTableCell cell) {
        for (IBodyElement element : cell.getBodyElements()) {
            if (!(element instanceof XWPFParagraph paragraph) || takesRoom(paragraph)) {
                return true;
            }
        }
        return false;
    }

    /** Whether a paragraph holds text or a picture set in its line; a drawing anchored to the page takes none. */
    private static boolean takesRoom(XWPFParagraph paragraph) {
        if (!paragraph.getText().isBlank()) {
            return true;
        }
        for (XWPFRun run : paragraph.getRuns()) {
            for (var drawing : run.getCTR().getDrawingList()) {
                if (drawing.sizeOfInlineArray() > 0) {
                    return true;
                }
            }
            if (run.getCTR().sizeOfPictArray() > 0 || run.getCTR().sizeOfObjectArray() > 0) {
                return true;
            }
        }
        return false;
    }

    /** Whether a cell is a box the reader sees: filled, or with any side drawn. */
    private static boolean paintedOrFramed(CTTcPr properties) {
        if (properties == null) {
            return false;
        }
        if (properties.isSetShd()) {
            return true;
        }
        if (!properties.isSetTcBorders()) {
            return false;
        }
        var borders = properties.getTcBorders();
        return drawn(borders.getTop()) || drawn(borders.getBottom()) || drawn(borders.getLeft())
               || drawn(borders.getRight()) || drawn(borders.getStart()) || drawn(borders.getEnd());
    }

    /**
     * Makes the space owed above the block about to be written the resumed one, when a
     * column layer has just started after another in its cell (see {@link #writeLayerColumns}).
     */
    private void resumeHere() {
        if (!Double.isNaN(resumeSpacing)) {
            carriedSpacingBefore = 0;
            pendingSpacingAfter = resumeSpacing;
            resumeSpacing = Double.NaN;
            forgetTheHang();
        }
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
        // A pull out of the paragraph above comes out of what it owes below, as the page sums them.
        double owed = pendingSpacingAfter - pullBelow;
        if (owed > 0 && lastBodyParagraph != null) {
            addSpacing(lastBodyParagraph, 0, owed);
        }
        pendingSpacingAfter = 0;
        pullBelow = 0;
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
     * The points a paragraph's line is given past the room it takes on the page: Word sets a
     * line a little wider than the page does, and a word that just fits there would break in
     * Word. Twice a table column's {@value #EDITOR_COLUMN_SLACK_POINTS}pt: a column's room is
     * its widest content, a paragraph's a line of whole words in a face Word may set wider.
     */
    private static final double EDITOR_SLACK_POINTS = 2;

    /**
     * Lets a line the page sets past its box's right edge stand out the same way in Word.
     *
     * <p>A word with nowhere to break — {@code SerifHeadline}'s "linkedin.com/in/alexmorgan" —
     * longer than its box is set whole on the page, 1.2pt out of the contact column. Word breaks
     * such a word between two letters instead: the column took a line more, and the whole page
     * under it stood that line lower. The paragraph's right indent gives the line the room it
     * takes on the page, and a couple of points more for an editor's slightly wider face.</p>
     *
     * <p>Only a paragraph each line of which is one word, set flush left: the indent is the
     * whole paragraph's, and it would give a line of several words room to take more of them,
     * or move a centred or right-aligned line off where the page sets it.</p>
     *
     * @param room the width the paragraph's text is written in, in points
     * @return whether the paragraph was given room past its box
     */
    private boolean letTheLineStandOut(XWPFParagraph para, ParagraphNode node, double room) {
        if (node.align() == TextAlign.CENTER || node.align() == TextAlign.RIGHT) {
            return false;
        }
        double overhang = layout.unbrokenWidth(node) - room;
        if (!Double.isFinite(overhang) || !(overhang > 0.01)) {
            return false;
        }
        CTPPr properties = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        CTInd indent = properties.isSetInd() ? properties.getInd() : properties.addNewInd();
        long right = twipsOf(indent.isSetRight() ? indent.getRight() : null);
        indent.setRight(BigInteger.valueOf(right - toTwips(overhang + EDITOR_SLACK_POINTS)));
        return true;
    }

    /**
     * Sets a paragraph's lines in a measure as much wider or narrower than the page's as Word
     * sets its text, so they break at the words the page breaks them.
     *
     * <p>Word states a type size in half points, so a size the page sets to the tenth is set a
     * little larger or smaller, and its lines that much wider or narrower: {@code
     * EngineeringResume}'s 6.9pt skills, set at 7pt, broke "SQL" onto a line of its own, and its
     * 7.8pt profile, set at 8pt, took a line more, each column standing 8 to 9pt low under it.
     * The glyphs are left as Word sets them — a scale would stay on the text a reader types
     * next — and the right indent gives the line the same share more room, or takes it.</p>
     *
     * <p>A paragraph set flush left has its measure moved at its right edge. A centred or
     * right-aligned one is given room only for a line of its own that Word sets past its box
     * or within three hundredths of it ({@link #ONE_LINE_FACE_SLACK}), at both edges, half each,
     * or at its left, so the line stays where the page sets it — in a cell, where Word draws no
     * text past the left edge, the right edge gives what the left indent cannot:
     * {@code VioletGrid}'s 6.8pt "INFORMATION ARCHITECTURE", set at 7pt and centred in a tile
     * it fills, broke onto two lines, and the tile's text stood 7.5pt low under it. A centred
     * paragraph of several lines is left as it is — a wider measure there takes another word
     * onto a line Word already breaks elsewhere than the page — and so is a short line, whose
     * box has room for it: measured, {@code MerchantInvoice}'s centred amounts, their cells'
     * indents taken past the cells' edges, stood 0.12pt off their place. A list's items, a line pair, text over the flow and
     * a header's or footer's line are written elsewhere and keep the page's measure.</p>
     *
     * @param room the width the paragraph's text is written in, in points
     */
    private void measureAtWordsSize(XWPFParagraph para, ParagraphNode node, double room) {
        boolean flushLeft = node.align() != TextAlign.CENTER && node.align() != TextAlign.RIGHT;
        if (!Double.isFinite(room) || !flushLeft && layout.lineCount(node) != 1) {
            return;
        }
        double more = (flushLeft ? wordsMeasure(node, room) : oneLinesMeasure(node, room)) - room;
        if (!Double.isFinite(more) || Math.abs(more) < 0.05) {
            return;
        }
        CTPPr properties = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        CTInd indent = properties.isSetInd() ? properties.getInd() : properties.addNewInd();
        long twips = Math.round(more * POINT_TO_TWIP);
        // The edge the text does not lean on moves: a centred line's two, half each.
        long fromTheLeft = node.align() == TextAlign.RIGHT ? twips
                : node.align() == TextAlign.CENTER ? twips / 2
                : 0;
        long left = twipsOf(indent.isSetLeft() ? indent.getLeft() : null);
        if (currentCell != null) {
            // Word draws no text past a cell's left edge (see leftIndentTwips): what the left
            // indent cannot give, the right one does, and the line moves by that much. A hanging
            // first line already starts that much further left.
            long hanging = twipsOf(indent.isSetHanging() ? indent.getHanging() : null);
            fromTheLeft = Math.min(fromTheLeft, Math.max(0, left - hanging));
        }
        long fromTheRight = twips - fromTheLeft;
        if (fromTheLeft != 0) {
            indent.setLeft(BigInteger.valueOf(left - fromTheLeft));
        }
        if (fromTheRight != 0) {
            long right = twipsOf(indent.isSetRight() ? indent.getRight() : null);
            indent.setRight(BigInteger.valueOf(right - fromTheRight));
        }
    }

    /**
     * The measure a paragraph of one line needs in Word, in points: its box, or its line as wide
     * as Word sets it and a few hundredths more ({@link #ONE_LINE_FACE_SLACK}), whichever is
     * wider.
     */
    private double oneLinesMeasure(ParagraphNode node, double room) {
        double set = 0;
        for (com.demcha.compose.document.layout.payloads.ParagraphLine line : layout.lines(node)) {
            // A line of pictures alone is set at their size, in Word as on the page.
            boolean text = line.spans().stream().anyMatch(span -> span.width() > 0
                    && span instanceof com.demcha.compose.document.layout.payloads.ParagraphTextSpan run
                    && run.textStyle() != null && run.textStyle().size() > 0);
            if (text) {
                set = Math.max(set, widthAtWordsSize(line));
            }
        }
        return Math.max(room, set * (1 + ONE_LINE_FACE_SLACK));
    }

    /**
     * How wide Word sets a line the page laid out: its text grown or shrunk to Word's size, its
     * tracking, pictures and shapes as the page has them.
     */
    private static double widthAtWordsSize(com.demcha.compose.document.layout.payloads.ParagraphLine line) {
        double set = 0;
        for (com.demcha.compose.document.layout.payloads.ParagraphSpan span : line.spans()) {
            if (!(span.width() > 0)) {
                continue;
            }
            if (span instanceof com.demcha.compose.document.layout.payloads.ParagraphTextSpan run
                && run.textStyle() != null && run.textStyle().size() > 0) {
                double tracking = run.textStyle().letterSpacing() * run.text().codePointCount(0, run.text().length());
                set += (span.width() - tracking) * wordsSize(run.textStyle().size()) / run.textStyle().size() + tracking;
            } else {
                set += span.width();
            }
        }
        return set;
    }

    /** How far inside the page's measure, grown or shrunk as Word sets it, Word's is held, in points. */
    private static final double WORDS_MEASURE_CLEARANCE = 1;

    /**
     * The share wider than the page sets it a line of one is given room for: Word sets a face
     * other than the page's, or emboldens one, a little wider. {@code OrangeOps}' headings are
     * Oswald SemiBold, a face whose file does not say it is bold; Word set "ACHIEVEMENTS" about
     * 1% wider, past the box it fills, and broke it onto a second line — three such headings
     * put the CV on two pages.
     */
    private static final double ONE_LINE_FACE_SLACK = 0.03;

    /**
     * The measure Word is to set a paragraph's text in, in points: the page's, as much wider or
     * narrower as Word sets the line that grows most, a point short of that in a paragraph of
     * several lines unless one of its lines needs more, and never less than a point past the
     * widest line.
     *
     * <p>Each line the page laid out is weighed by its own text: a line of a 7.35pt title set at
     * 7.5 grows, the 7.1pt lines under it set at 7 shrink, and a share averaged over the
     * paragraph would narrow the measure the title's line no longer fits. A picture or a shape
     * in a line is written at its own size and takes the same room in Word, and tracking is
     * written in points, so neither grows with the size.</p>
     *
     * <p>A line the page broke because its next word did not fit may have missed by a fraction
     * of a point, and grown in the same proportion it misses by as little in Word, where it can
     * fit: {@code CompactMono}'s "and", 0.1pt from fitting on the page, fitted in Word. Held a
     * point short, the measure misses such a word by that much more. The widest line, grown,
     * still has a point to spare, which wins where the two meet. A paragraph of one line broke
     * no word: it keeps its measure or the share it grows by, whichever is wider, so a line as
     * wide as its column is never narrowed onto two — {@code OrangeOps}' phone number broke in
     * LibreOffice a point narrower — and room for its line a few hundredths wider
     * ({@link #ONE_LINE_FACE_SLACK}), for a face Word sets wider than the page. Without the
     * page's lines — an export with no layout — the paragraph's runs are weighed by their
     * letters.</p>
     */
    private double wordsMeasure(ParagraphNode node, double room) {
        double share = Double.NaN;
        double fits = 0;
        int broken = -1;
        for (com.demcha.compose.document.layout.payloads.ParagraphLine line : layout.lines(node)) {
            broken++;
            double asked = 0;
            boolean text = false;
            for (com.demcha.compose.document.layout.payloads.ParagraphSpan span : line.spans()) {
                if (span.width() > 0) {
                    asked += span.width();
                    text |= span instanceof com.demcha.compose.document.layout.payloads.ParagraphTextSpan run
                            && run.textStyle() != null && run.textStyle().size() > 0;
                }
            }
            double set = widthAtWordsSize(line);
            if (text) {
                share = Double.isNaN(share) ? set / asked : Math.max(share, set / asked);
                fits = Math.max(fits, set);
            }
        }
        if (Double.isNaN(share)) {
            return room * lettersShare(node);
        }
        if (broken < 1) {
            // One line breaks no word to keep out, and broken in Word it is a line more.
            return Math.max(room, Math.max(room * share, fits * (1 + ONE_LINE_FACE_SLACK)));
        }
        if (Math.abs(share - 1) < 1e-9) {
            // Word sets every line at the page's size: the page's measure is Word's.
            return room;
        }
        return Math.max(room * share - WORDS_MEASURE_CLEARANCE, fits + WORDS_MEASURE_CLEARANCE);
    }

    /** How much wider Word sets a paragraph's runs, weighed by their letters, 1 for as wide. */
    private double lettersShare(ParagraphNode node) {
        double asked = 0;
        double set = 0;
        for (InlineRun run : node.inlineRuns()) {
            InlineTextRun text = textOf(run);
            DocumentTextStyle style = text == null ? null : text.textStyle() == null ? node.textStyle() : text.textStyle();
            if (style != null && style.size() > 0) {
                asked += text.text().length() * style.size();
                set += text.text().length() * wordsSize(style.size());
            }
        }
        if (node.inlineRuns().isEmpty() && node.textStyle() != null && node.textStyle().size() > 0) {
            // No text runs: the paragraph's own text, in its own style.
            asked = node.textStyle().size();
            set = wordsSize(asked);
        }
        return asked > 0 ? set / asked : 1;
    }

    /** The size Word sets a size in, to the half point, as {@code w:sz} states it. */
    private static double wordsSize(double size) {
        return Math.max(1, Math.round(size * HALF_POINTS_PER_POINT)) / HALF_POINTS_PER_POINT;
    }

    /**
     * A paragraph's left indent in twips: below zero in the body, where a container hanging
     * left by a negative margin takes its text out past the margin as the page does; never
     * below zero in a cell, where Word draws no text past the cell's left edge — measured, a
     * section title hung 8pt out of its cell lost its first letter.
     */
    private long leftIndentTwips(double points) {
        return currentCell != null ? toTwips(points) : Math.round(points * POINT_TO_TWIP);
    }

    /**
     * Holds the paragraph in from the sides by every enclosing container's margin and padding.
     *
     * <p>Only the sides a container asked for are written, and none when no container asked,
     * so a paragraph outside any padded container is written as it always was. In a cell of a
     * row hanging left, the text moves left by the hang as far as its indent goes
     * ({@link #cellTextShift}).</p>
     *
     * <p>The sides are written as the page's. A right-to-left paragraph has them turned to
     * its flow when its direction is written, in {@link #applyDirection}.</p>
     */
    private void applyInset(XWPFParagraph para) {
        double left = insetLeft + cellTextShift;
        if (!(Math.abs(insetLeft) > 0.01) && insetRight <= 0) {
            return;
        }
        CTPPr properties = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        CTInd indent = properties.isSetInd() ? properties.getInd() : properties.addNewInd();
        if (Math.abs(insetLeft) > 0.01) {
            indent.setLeft(BigInteger.valueOf(leftIndentTwips(left)));
        }
        if (insetRight > 0) {
            indent.setRight(BigInteger.valueOf(toTwips(insetRight)));
        }
    }

    /**
     * Writes an overlay's left and right paragraph as one line, the right one after a tab stop:
     * a right-aligned one where it ends on the page, or a left-aligned one where it starts when
     * it is set from its start (see {@link DocxLinePair}).
     *
     * <p>The line starts where the left paragraph does and runs from the top of the higher
     * text to the bottom of the lower, so the band keeps its place in the flow: the space
     * the band leaves above and below its text is owed around the line, and text standing
     * out of the band — a title the page pulls up to centre it on a marker smaller than its
     * line — takes that much back from the gaps on either side, as it does on the page. A
     * tab stop is measured from the text area, not from the paragraph's indent. Drawing among
     * the layers is drawn as shapes where the page puts it, as in any overlay.</p>
     */
    private void writeLinePair(XWPFDocument document, DocumentNode overlay, DocxLinePair.Pair pair)
            throws Exception {
        double text = pair.line();
        double above = overlay.margin().top() + pair.above();
        if (above >= 0) {
            carriedSpacingBefore += above;
        } else {
            standsAboveItsCell(takeBackSpaceAbove(-above));
        }
        double outerLeft = insetLeft;
        double lineStart = outerLeft + overlay.margin().left();
        insetLeft = lineStart + pair.leftOffset();
        XWPFParagraph para;
        try {
            para = newBodyParagraph(document);
        } finally {
            insetLeft = outerLeft;
        }
        para.setAlignment(ParagraphAlignment.LEFT);
        if (text > 0) {
            applyLineHeight(para, java.util.OptionalDouble.of(text));
        }
        CTPPr properties = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        CTTabStop tab = (properties.isSetTabs() ? properties.getTabs() : properties.addNewTabs()).addNewTab();
        // A right text set from its start holds that start; one set against its end, as a date
        // at the right of a band is, holds its end (see DocxLinePair.Pair#fromItsStart).
        tab.setVal(pair.fromItsStart() ? STTabJc.LEFT : STTabJc.RIGHT);
        tab.setPos(BigInteger.valueOf(toTwips(lineStart + pair.tabStop())));
        // Each half is still the paragraph it was: its outline level, its bookmark around its
        // own text, and whether it keeps with what follows.
        applyHeadingRole(para, headingLevelOf(pair.left()) != null ? pair.left() : pair.right());
        // The line starts at the higher text's top; each half is seated from there.
        com.demcha.compose.document.layout.PlacedNode box = layout.placement(overlay);
        double lineTop = box.placementY() + box.placementHeight() - pair.above();
        int leftAnchor = openAnchor(para, pair.left().anchor());
        writeParagraphRuns(para, pair.left(), false, lineTopAbove(lineTop, pair.left()), false);
        closeAnchor(para, leftAnchor);
        para.createRun().addTab();
        int rightAnchor = openAnchor(para, pair.right().anchor());
        writeParagraphRuns(para, pair.right(), false, lineTopAbove(lineTop, pair.right()), false);
        closeAnchor(para, rightAnchor);
        if ((pair.left().keepWithNext() && layout.onOnePage(pair.left()))
            || (pair.right().keepWithNext() && layout.onOnePage(pair.right()))) {
            para.setKeepNext(true);
        }
        double below = overlay.margin().bottom() + pair.below();
        if (below >= 0) {
            owePendingSpacingAfter(below);
        } else {
            hangingBelow = -below;
        }
        overlayDepth++;
        try {
            for (DocumentNode child : overlay.children()) {
                if (child != pair.left() && child != pair.right()) {
                    writeNode(document, child);
                }
            }
        } finally {
            overlayDepth--;
        }
    }

    /**
     * How far a line starting at {@code lineTop} starts above a paragraph's first line on the
     * page, in points; 0 when the paragraph laid out no line.
     *
     * @param lineTop   the line's top, measured up from the foot of the page
     * @param paragraph the paragraph set in the line
     */
    private double lineTopAbove(double lineTop, ParagraphNode paragraph) {
        java.util.OptionalDouble first = layout.firstLineTop(paragraph);
        return first.isPresent() ? lineTop - first.getAsDouble() : 0;
    }

    /**
     * Writes a shape container's layers, drawing its outline where the page draws it. A
     * container whose one written layer is laid beside drawing is written as a band (see
     * {@link #writeOverlayBand}, {@link #writesOneLayer}): a section title beside its badge
     * stands where the page places it in the header, with the page's space above and below it.
     *
     * @param band the container's layers as a band, or {@code null} to write them one after the
     *             other
     */
    private void writeShapeContainer(XWPFDocument document, ShapeContainerNode node, DocxLayerColumns.Band band)
            throws Exception {
        if (writtenAsAPanel(node)) {
            warnContainerRadiusDropped(node);
            if (node.transform() != null && !node.transform().isIdentity()) {
                report.add(DocxExportReport.Severity.APPROXIMATED, "shape container", layout.pathOf(node),
                        "written as a panel in its table cell, which carries no transform, so it stands "
                        + "upright at its size");
            }
            writePanel(document, node, new ContainerPaint(node.fillColor(), bordersOf(null, node.stroke())));
            return;
        }
        // POI/DOCX has no portable equivalent of a graphics-state path clip.
        // The fallback rule (recorded in docs/canonical-legacy-parity.md) is
        // to render the container's layers inline, in source order, without
        // clipping; a picture clipped to an ellipse takes the ellipse's shape.
        report.add(DocxExportReport.Severity.APPROXIMATED, "clipped shape container",
                layout.pathOf(node),
                "DOCX has no graphics-state clip, so the layers are written inline, in source "
                + "order, without being clipped to the outline");
        // The outline itself is drawing — a badge's circle, a ring round a portrait — and is
        // drawn where the page draws it, behind the layers now held in to where it sets them.
        drawOutlineOf(node);
        if (shapeContainerWarned.compareAndSet(false, true)) {
            LOG.warn("docx.export.shape-container-fallback "
                    + "outline='{}' clipPolicy={} — DOCX has no graphics-state clip; "
                    + "rendering layers inline without clipping. "
                    + "(One warning per export; use the PDF backend for full fidelity.)",
                    node.outline().getClass().getSimpleName(),
                    node.clipPolicy());
        }
        double outerLeft = insetLeft;
        double outerRight = insetRight;
        // Its layers are measured from inside its own edges, as a band's are.
        double innerLeft = insetLeft + node.margin().left() + node.padding().left();
        double innerRight = insetRight + node.margin().right() + node.padding().right();
        ShapeContainerNode outerClip = clipContainer;
        if (node.clipPolicy() == com.demcha.compose.document.style.ClipPolicy.CLIP_PATH) {
            clipContainer = node;
        }
        try {
            if (band != null) {
                writeOverlayBand(document, node, band);
                return;
            }
            holdStackedLines(node.children(), contentFoot(node));
            // Its edges are space above and below what it holds, as a section's are: a
            // SerifHeadline section heading — a row in a container set its gap below the block
            // above — stood that gap high in Word, and everything under it with it.
            // One that writes nothing — its layers all drawn — stands above nothing, and its
            // edges are no space in the flow, as writeContainerBody hands them back.
            double carriedFromOutside = carriedSpacingBefore;
            long blocksBefore = blocksWritten;
            // What the container sets in from its edges — a layer centred, smaller than its
            // outline, or lines written one under another — stands that far in from them on the
            // page: NavySidebar's photo sits 1.6pt inside its ring, top and bottom, and written
            // flush with the ring's top the column under it stood twice the ring's width high.
            double[] setIn = layerSetIn(node);
            carriedSpacingBefore += node.margin().top() + node.padding().top() + setIn[0];
            for (DocumentNode child : node.children()) {
                insetLeft = innerLeft;
                insetRight = innerRight;
                placeAcross(node, child);
                writeNode(document, child);
            }
            if (blocksWritten == blocksBefore) {
                carriedSpacingBefore = carriedFromOutside;
            } else {
                carriedSpacingBefore = 0;
                owePendingSpacingAfter(setIn[1] + node.padding().bottom() + node.margin().bottom());
                hangBelowItsBox(node);
            }
        } finally {
            insetLeft = outerLeft;
            insetRight = outerRight;
            clipContainer = outerClip;
        }
    }

    /**
     * How far a shape container sets its layers in from the top and the bottom of its content,
     * where the layout placed them all on its page; {@code {0, 0}} for one the layout did not
     * place, or one of several layers that do not stand one under another.
     *
     * <p>Several layers are set in as one where they are paragraphs written one under another:
     * each starting at or below the foot of the one before, or the two held as a stack of lines
     * ({@link #holdStackedLines}), whose lines meet. The set-in runs from the top of the first to
     * the foot of the last. {@code MidnightNavy}'s monogram centres its two initials in a 76pt
     * ring; written from the ring's top, the initials stood 9.6pt high in Word, and the name
     * under the ring 19.3pt, with the whole column under it. Layers set side by side, or
     * overlapping without being a stack, are written taller than the page has them, and are
     * not set in.</p>
     */
    private double[] layerSetIn(ShapeContainerNode node) {
        List<DocumentNode> layers = node.children();
        if (layers.isEmpty()) {
            return new double[]{0, 0};
        }
        com.demcha.compose.document.layout.PlacedNode box = layout.placement(node);
        if (box == null || box.startPage() != box.endPage()) {
            return new double[]{0, 0};
        }
        DocumentNode previous = null;
        double previousFoot = Double.POSITIVE_INFINITY;
        for (DocumentNode child : layers) {
            com.demcha.compose.document.layout.PlacedNode placed = layout.placement(child);
            if (placed == null || placed.startPage() != box.startPage() || placed.endPage() != box.startPage()
                || layers.size() > 1 && !(child instanceof ParagraphNode)) {
                return new double[]{0, 0};
            }
            double top = placed.placementY() + placed.placementHeight();
            boolean stacked = previous instanceof ParagraphNode above && stackedLineHeights.containsKey(above)
                              && child instanceof ParagraphNode below && stackedLineHeights.containsKey(below);
            if (previous != null && !(top <= previousFoot + 0.01) && !stacked) {
                return new double[]{0, 0};
            }
            previous = child;
            previousFoot = placed.placementY();
        }
        com.demcha.compose.document.layout.PlacedNode first = layout.placement(layers.get(0));
        com.demcha.compose.document.layout.PlacedNode last = layout.placement(layers.get(layers.size() - 1));
        // Measured up from the foot of the page: the box's content runs from its padding up.
        double contentTop = box.placementY() + box.placementHeight() - node.padding().top();
        double contentFoot = box.placementY() + node.padding().bottom();
        // A layer's own margins are its own edges, written with it.
        double above = contentTop - (first.placementY() + first.placementHeight()) - layers.get(0).margin().top();
        double below = last.placementY() - contentFoot - layers.get(layers.size() - 1).margin().bottom();
        // A layer moved past either edge leaves the other side no more than the two hold together.
        // A paragraph's line past the foot is written where the page puts it: hangBelowItsBox
        // takes its overhang from the gap under the container.
        double together = Math.max(0, above + below);
        double setAbove = node.children().get(0) instanceof ParagraphNode
                ? Math.max(0, above)
                : Math.min(Math.max(0, above), together);
        return new double[]{setAbove, Math.max(0, together - setAbove)};
    }

    /**
     * Holds the lines of text a container lays over one another to lines that each end between
     * its letters and the next one's, the last at the container's foot, so written one after
     * the other they hold their letters whole and end where the container does (see
     * {@link DocxStackedLines}).
     *
     * @param layers a container's children, in the order they are written
     * @param foot   the foot of the container's content, measured up from the foot of the page,
     *               or {@code NaN} to end the last line where its own line ends
     */
    private void holdStackedLines(List<DocumentNode> layers, double foot) {
        stackedLineHeights.putAll(DocxStackedLines.of(layers, foot, layout, this::inkOf));
    }

    /**
     * How far a paragraph's first line's letters reach above and below its baseline (see
     * {@link DocxInk}), where the page seats them: text raised to its line's top reaches as much
     * further up and less far down.
     */
    private double[] inkOf(ParagraphNode paragraph) {
        java.util.Optional<com.demcha.compose.document.layout.payloads.ParagraphLine> line = layout.firstLine(paragraph);
        double[] reach = line.isEmpty() ? null : DocxInk.of(line.get(), measuredFonts());
        if (reach == null) {
            return null;
        }
        double seated = seatShift(paragraph);
        return new double[]{reach[0] + seated, reach[1] - seated};
    }

    /** The fonts the layout measured with, loaded when first asked for. */
    private FontLibrary measuredFonts() {
        if (seatFonts == null) {
            seatFonts = com.demcha.compose.document.backend.fixed.pdf.PdfFontLibraryFactory
                    .measurementLibrary(measuredFamilies);
        }
        return seatFonts;
    }

    /**
     * The height the layout gives a line set in a style, in points; 0 for no style, and for a
     * face the measured fonts do not hold — an export whose layout failed is written without
     * one, and measuring here must not fail it in its stead.
     */
    private double styleLineHeight(DocumentTextStyle style) {
        if (style == null) {
            return 0;
        }
        try {
            return styleMetrics().lineHeight(style);
        } catch (RuntimeException unknownFace) {
            return 0;
        }
    }

    /** The measured fonts' metrics for a style, built when first asked for. */
    private com.demcha.compose.document.chart.ChartTextMetrics styleMetrics() {
        if (styleMetrics == null) {
            styleMetrics = new com.demcha.compose.document.layout.ChartTextMetricsSupport(
                    new com.demcha.compose.engine.measurement.FontLibraryTextMeasurementSystem(
                            measuredFonts(), com.demcha.compose.engine.render.pdf.PdfFont.class));
        }
        return styleMetrics;
    }

    /**
     * The foot of a shape container's content, measured up from the foot of its page, or
     * {@code NaN} when it is not laid out on one page. Its bottom padding is owed below it
     * (writeShapeContainer), so the content ends above it — but not in a band, which drops what
     * its layers owe and sets its own space below.
     */
    private double contentFoot(ShapeContainerNode node) {
        com.demcha.compose.document.layout.PlacedNode box = layout.placement(node);
        return box == null || box.startPage() != box.endPage()
                ? Double.NaN : box.placementY() + (bandDepth > 0 ? 0 : node.padding().bottom());
    }

    /**
     * Lets the last line of a container that runs past the container's foot hang below it, as
     * text below a band does (see {@link #writeLinePair}): the page sets what follows under the
     * container, and the line's overhang is taken from the gap above it.
     *
     * <p>A line whose own foot runs past its container's pushed what follows that much lower.
     * The last line of a stack ends at the container's foot or below its own letters, and hangs
     * as far as those run past the foot (see {@link #holdStackedLines}).</p>
     */
    private void hangBelowItsBox(ShapeContainerNode node) {
        List<DocumentNode> layers = node.children();
        if (layers.isEmpty() || !(layers.get(layers.size() - 1) instanceof ParagraphNode last)) {
            return;
        }
        DocxStackedLines.Line stacked = stackedLineHeights.get(last);
        if (stacked != null) {
            if (stacked.hang() > 0.01) {
                hangingBelow = Math.max(hangingBelow, stacked.hang());
            }
            return;
        }
        com.demcha.compose.document.layout.PlacedNode box = layout.placement(node);
        com.demcha.compose.document.layout.PlacedNode line = layout.placement(last);
        if (box == null || line == null || box.startPage() != box.endPage()
            || line.startPage() != box.startPage() || line.endPage() != box.startPage()) {
            return;
        }
        // The page's y runs up from a box's foot.
        double overhang = contentFoot(node) - line.placementY();
        if (overhang > 0.01) {
            hangingBelow = Math.max(hangingBelow, overhang);
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
        if (bookmark == null) {
            XWPFRun run = para.createRun();
            applyStyle(run, node.textStyle());
            run.setText(shown);
        } else {
            // A complex field, as a page field is (see appendField): a simple one's number is
            // repainted without its style when the field updates.
            appendField(para, " PAGEREF " + bookmark + " \\h ", shown, node.textStyle());
        }
    }

    private void writeParagraph(XWPFDocument document, ParagraphNode node) {
        if (overTheFlowDepth > 0) {
            writeTextOverTheFlow(document, node);
            return;
        }
        if (currentCell == null && overlayDepth == 0 && startsAPageOfItsOwn(node)) {
            holdAParagraphsTopEdgeOnItsPage(document, node);
        }
        // Its own sides hold its text in, as a container's do: SerifHeadline's summary stops at
        // the column divider through its right margin, and without it ran the page's width in
        // Word — a line short, and everything under it that much high.
        double outerLeft = insetLeft;
        double outerRight = insetRight;
        // Not under an overlay, where holdIn places each layer's margin box and a paragraph
        // there keeps the width it was given. In a cell, a couple of points of each side stay
        // the editor's: Word sets a line a little wider than the page, and VioletGrid's narrow
        // centred cells broke a word more, running the CV to a second page. A cell of a row the
        // layout placed already starts where the paragraph does, past its left margin.
        if (overlayDepth == 0) {
            double spare = currentCell != null ? EDITOR_SLACK_POINTS : 0;
            double left = (node == leftMarginInCell ? 0 : node.margin().left()) + node.padding().left();
            insetLeft += Math.max(0, left - spare);
            insetRight += Math.max(0, node.margin().right() + node.padding().right() - spare);
        }
        XWPFParagraph para;
        double room;
        try {
            para = newBodyParagraph(document);
            room = availableWidth();
        } finally {
            insetLeft = outerLeft;
            insetRight = outerRight;
        }
        boolean standsOut = letTheLineStandOut(para, node, room);
        boolean rightToLeft = applyParagraphProperties(para, node);
        indentAsThePrefixDoes(para, node);
        // A line that stands out was given its room, and a couple of points more.
        if (!rightToLeft && !standsOut) {
            measureAtWordsSize(para, node, room);
        }
        applyHeadingRole(para, node);
        int anchor = openAnchor(para, node.anchor());
        // A line of a stack starts where its letters and the ones above leave room, not where
        // the page's line does (see DocxStackedLines).
        DocxStackedLines.Line stacked = stackedLineHeights.get(node);
        writeParagraphRuns(para, node, rightToLeft, stacked == null ? 0 : stacked.topAbove(), stacked == null);
        closeAnchor(para, anchor);
        lastWrittenNode = node;
        lastWrittenParagraph = para;
    }

    /**
     * Indents a paragraph's lines by the blank prefix the page sets them after.
     *
     * <p>A paragraph's {@code bulletOffset} is text the page puts before its first line, its
     * wrapped lines or both ({@code indentStrategy}), and none of it is in the paragraph's
     * text. {@code EditorialProposal}'s bullets are a dot and three spaces before the text, and
     * a prefix of three spaces before each wrapped line: in Word those lines started under the
     * dot, 11.6pt left of the page's. A prefix of spaces is only a distance, so it is written as
     * one — the left indent for the wrapped lines and the first line's difference from it —
     * measured in the paragraph's style, not its runs', as the page measures it. It is written
     * once a right-to-left paragraph's sides are turned, so it goes on {@code w:left}, the
     * start of the flow in either direction.</p>
     *
     * <p>A prefix with letters in it is drawn only before the first line; the wrapped lines
     * start after as many spaces as cover it ({@link #spacesCovering}), a distance written as any
     * other. Its letters are text the export does not write, so the first line is not moved for
     * them. An auto-sized paragraph's prefix is measured at the size the export writes its text
     * in, not the one the page fits it to.</p>
     */
    private void indentAsThePrefixDoes(XWPFParagraph para, ParagraphNode node) {
        String prefix = node.bulletOffset();
        DocumentTextIndent strategy = node.indentStrategy();
        if (prefix.isEmpty() || strategy == DocumentTextIndent.NONE) {
            return;
        }
        boolean blank = prefix.isBlank();
        boolean first = blank && (strategy == DocumentTextIndent.FIRST_LINE || strategy == DocumentTextIndent.ALL_LINES);
        boolean wrapped = strategy == DocumentTextIndent.FROM_SECOND_LINE || strategy == DocumentTextIndent.ALL_LINES;
        DocumentTextStyle style = node.textStyle();
        long wrappedTwips = wrapped ? toTwips(styleWidth(style, blank ? prefix : spacesCovering(style, prefix))) : 0;
        long firstTwips = first ? toTwips(styleWidth(style, prefix)) : 0;
        if (wrappedTwips <= 0 && firstTwips <= 0) {
            return;
        }
        CTPPr properties = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        CTInd indent = properties.isSetInd() ? properties.getInd() : properties.addNewInd();
        if (wrappedTwips > 0) {
            long left = twipsOf(indent.isSetLeft() ? indent.getLeft() : null);
            indent.setLeft(BigInteger.valueOf(left + wrappedTwips));
        }
        if (firstTwips > wrappedTwips) {
            indent.setFirstLine(BigInteger.valueOf(firstTwips - wrappedTwips));
        } else if (wrappedTwips > firstTwips) {
            indent.setHanging(BigInteger.valueOf(wrappedTwips - firstTwips));
        }
    }

    /**
     * The spaces the page sets a wrapped line after for a prefix with letters in it: as many as
     * cover the prefix and the space that ends it, as the layout counts them
     * ({@code ParagraphWrapping.computeIndentFromPrefix}); empty for a face the measured fonts
     * do not hold.
     */
    private String spacesCovering(DocumentTextStyle style, String prefix) {
        String ended = Character.isWhitespace(prefix.charAt(prefix.length() - 1)) ? prefix : prefix + " ";
        double space = styleWidth(style, " ");
        if (!(space > 1e-6)) {
            return "";
        }
        return " ".repeat((int) Math.ceil(styleWidth(style, ended) / space));
    }

    /**
     * The width the layout gives text set in a style, in points; 0 for no style, and for a
     * face the measured fonts do not hold, as {@link #styleLineHeight} answers.
     */
    private double styleWidth(DocumentTextStyle style, String text) {
        if (style == null) {
            return 0;
        }
        try {
            return styleMetrics().width(style, text);
        } catch (RuntimeException unknownFace) {
            return 0;
        }
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
        DocxStackedLines.Line stacked = stackedLineHeights.get(source);
        applyLineHeight(target, stacked != null ? java.util.OptionalDouble.of(stacked.height()) : layout.lineHeight(source));
        applyVerticalSpacing(target, source);
        applyLineGap(target, layout.lineGap(source), layout.lineCount(source), layout.linePitch(source));
        topEdgeLine = null;
        topEdgeLineOver = null;
        if (stacked == null) {
            oweWhatTheLinesFallShort(target, source);
        }
        return rightToLeft;
    }

    /**
     * Owes below a paragraph what its lines are shorter in Word than on the page.
     *
     * <p>Word sets a paragraph's lines one height apart, and the page sets each line its own
     * height. Where they differ the written height is the page's distance between lines
     * ({@code DocxLayoutMetrics.lineHeight}), and the paragraph comes out short by what its
     * first line's top and its last line's foot hold beyond that: {@code EditorialProposal}'s
     * terms, whose wrapped lines carry a taller prefix than their first, stood 0.6pt higher
     * with every item. Only a paragraph written at a height other than its tallest laid-out
     * line is made up to the page; one written at its tallest line, to keep its letters whole,
     * comes out no shorter than the page and keeps that height.</p>
     */
    private void oweWhatTheLinesFallShort(XWPFParagraph target, ParagraphNode source) {
        java.util.OptionalDouble page = layout.linesHeight(source);
        CTPPr properties = target.getCTP().getPPr();
        if (page.isEmpty() || properties == null || !hasAnExactLine(target) || !layout.lineIsNotTheTallest(source)) {
            return;
        }
        Long line = writtenTwips(properties.getSpacing().getLine());
        if (line == null) {
            return;
        }
        double written = layout.lineCount(source) * (line / POINT_TO_TWIP)
                         - lineTopsTakenIn.getOrDefault(target.getCTP(), 0.0);
        double shortBy = page.getAsDouble() - written;
        if (shortBy > LINES_SHORT_TOLERANCE) {
            owePendingSpacingAfter(shortBy);
        }
    }

    /** What a paragraph's lines may fall short of the page by, twips rounding, before it is owed. */
    private static final double LINES_SHORT_TOLERANCE = 0.1;

    /**
     * Sets a paragraph's mark in its text's size and face, where the editor may grow the line.
     *
     * <p>The mark closing a paragraph is a character on its last line, and its size counts
     * towards that line's height. Left unstyled it takes the document's own size and face, so a
     * line of half-point text — a coloured cell a hairline tall, the way a heading rule is drawn
     * in a table — came out as tall as a line of body text in both editors: {@code SlateOrange}'s
     * rules under its credentials headings were 8pt bars. A line written at an exact height
     * does not grow for the mark and is left alone; one the layout did not measure, and one
     * written at least a picture's height, are the ones it reaches. Called once the line's rule
     * is settled, pictures included.</p>
     *
     * @param target the paragraph, its runs written
     * @param style  the text style of the paragraph's text
     */
    private void styleTheMark(XWPFParagraph target, DocumentTextStyle style) {
        if (style == null || !(style.size() > 0) || hasAnExactLine(target)) {
            return;
        }
        DocumentTextStyle defaults = documentDefaultStyle;
        boolean sameSize = defaults != null && Math.round(style.size() * HALF_POINTS_PER_POINT)
                                               == Math.round(defaults.size() * HALF_POINTS_PER_POINT);
        String family = wordFamilyOf(style.fontName());
        boolean sameFace = family == null
                           || defaults != null && family.equals(wordFamilyOf(defaults.fontName()));
        if (sameSize && sameFace) {
            return;
        }
        CTPPr properties = target.getCTP().isSetPPr() ? target.getCTP().getPPr() : target.getCTP().addNewPPr();
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTParaRPr mark =
                properties.isSetRPr() ? properties.getRPr() : properties.addNewRPr();
        if (!sameFace) {
            // The face the runs are set in, the way applyStyle names it on them: a mark in the
            // document's face grows the line by that face's height, not the text's.
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts fonts =
                    mark.sizeOfRFontsArray() > 0 ? mark.getRFontsArray(0) : mark.addNewRFonts();
            fonts.setAscii(family);
            fonts.setHAnsi(family);
            fonts.setCs(family);
            fonts.setEastAsia(family);
        }
        if (!sameSize) {
            BigInteger halfPoints = BigInteger.valueOf(Math.max(1, Math.round(style.size() * HALF_POINTS_PER_POINT)));
            (mark.sizeOfSzArray() > 0 ? mark.getSzArray(0) : mark.addNewSz()).setVal(halfPoints);
            (mark.sizeOfSzCsArray() > 0 ? mark.getSzCsArray(0) : mark.addNewSzCs()).setVal(halfPoints);
        }
    }

    /** Whether a paragraph's lines are written at an exact height, which no mark changes. */
    private static boolean hasAnExactLine(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        return properties != null && properties.isSetSpacing() && properties.getSpacing().isSetLineRule()
               && properties.getSpacing().getLineRule() == STLineSpacingRule.EXACT;
    }

    /**
     * Puts the layout's gap between a paragraph's lines, which Word has no word for.
     *
     * <p>The page sets each line after the one above it at the line's height plus the
     * paragraph's {@code lineSpacing}; the export wrote the line's height alone, so every
     * wrapped line of a CV's body text stood a point or two higher than on the page, and a
     * section of entries ran several points short. Word has one line height for a paragraph
     * and no space between its lines, so the gap goes into the line.</p>
     *
     * <p>A paragraph of {@code n} lines has {@code n - 1} gaps on the page and would get
     * {@code n} in Word; the one too many comes off the space above the paragraph. The
     * editor puts most of an exact line's spare height above its text (measured in
     * LibreOffice: 8pt of 10 above), so taking it from above keeps the first line nearly
     * where the page sets it; the line then starts that much above the page's, and the text
     * is seated from there (see {@link #shiftToThePagesBaseline}). A paragraph opening a cell
     * takes what the space above cannot give from the cell's top padding
     * ({@link #takeFromTheTopOfItsCell}). Where the two are less than a gap — right under the
     * block before it, or in a cell padded less — what they cannot give is not put into the
     * lines at all: the {@code n - 1} gaps are shared out over {@code n} lines, so the
     * paragraph is as tall as on the page, its lines a little closer than there.</p>
     *
     * @param target the Word paragraph, its line height and space above already written
     * @param gap    the gap between two of its lines on the page, in points
     * @param lines  how many lines the page set it on
     */
    private void applyLineGap(XWPFParagraph target, double gap, int lines) {
        applyLineGap(target, gap, lines, java.util.OptionalDouble.empty());
    }

    /**
     * {@link #applyLineGap(XWPFParagraph, double, int)}, knowing the page's mean distance
     * between the lines' baselines: a paragraph opening a cell takes from the cell's padding
     * only what steps its lines that far apart.
     */
    private void applyLineGap(XWPFParagraph target, double gap, int lines, java.util.OptionalDouble pitch) {
        if (!(gap > 0) || lines < 2) {
            return;
        }
        CTPPr properties = target.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing()) {
            return;
        }
        CTSpacing spacing = properties.getSpacing();
        if (!spacing.isSetLineRule() || spacing.getLineRule() != STLineSpacingRule.EXACT) {
            return;
        }
        Long line = writtenTwips(spacing.getLine());
        if (line == null) {
            return;
        }
        long twips = toTwips(gap);
        long before = twipsOf(spacing.isSetBefore() ? spacing.getBefore() : null);
        long taken = Math.min(before, twips);
        if (taken > 0) {
            spacing.setBefore(BigInteger.valueOf(before - taken));
        }
        // The paragraph's top edge held by a line above it is the space above it all the same.
        if (target == topEdgeLineOver && taken < twips) {
            taken += takeFromTheLineHoldingItsEdge(twips - taken);
        }
        // The cell's padding gives no more than steps the lines the page's distance apart: n lines
        // of (line + extra) are that distance apart where extra = pitch - line. Lines of
        // different heights can stand closer than their tallest and the gap.
        if (pitch.isPresent()) {
            long stepped = Math.round(lines * (pitch.getAsDouble() * POINT_TO_TWIP - line)
                                      - (lines - 1) * (double) twips) - taken;
            taken += takeFromTheTopOfItsCell(target, Math.min(twips - taken, stepped));
        }
        if (taken > 0) {
            lineTopsTakenIn.put(target.getCTP(), taken / POINT_TO_TWIP);
        }
        // n lines of (line + extra), less what came off above, are n lines and n - 1 gaps.
        long extra = Math.round(((lines - 1) * (double) twips + taken) / lines);
        spacing.setLine(BigInteger.valueOf(line + extra));
    }

    /**
     * Whether every cell of a table row can have its top margin evened down to the row's
     * smallest ({@link #evenTheRowsMargins}): none is merged down over rows, and none opens with
     * a table, which has no paragraph above it to hold the padding.
     *
     * <p>Word gives every cell of a row the row's largest top margin. Where one stays as it is,
     * a cell whose paragraph took the gap from its padding would be set at that margin
     * whatever its own says: the padding would come back, and the line grown for it stand that
     * much low. Such a row is written as before.</p>
     */
    private static boolean everyMarginEvens(List<TableGrid.Placement> row) {
        for (TableGrid.Placement placement : row) {
            if (placement.rowSpan() > 1 || opensWithATable(placement.cell().content())) {
                return false;
            }
        }
        return true;
    }

    /** Whether writing a node writes a table first: the node is one, or its first child does. */
    private static boolean opensWithATable(DocumentNode node) {
        for (DocumentNode current = node; current != null;
             current = current.children().isEmpty() ? null : current.children().get(0)) {
            if (current instanceof TableNode) {
                return true;
            }
        }
        return false;
    }

    /**
     * Takes up to {@code twips} off the top padding of the cell a paragraph opens, for the gap
     * its first line is given above it.
     *
     * <p>The cell's padding is space above the paragraph as much as the paragraph's own: the
     * row is as tall as the padding and the lines together, and the line starts that much
     * higher in it. {@code EditorialProposal}'s timeline describes each phase in a cell of two
     * lines 13.85pt apart on the page; with nothing above the paragraph to take the gap from,
     * Word set them 12.35pt apart, the first 1.85pt low and the second 0.36pt.</p>
     *
     * <p>Only a table's cell gives its padding, in a row whose margins all even
     * ({@link #everyMarginEvens}). A panel's cell keeps its: its margin is the padding less half
     * its border, and Word sets the content no nearer the border than the border's width.</p>
     *
     * @return the twips taken, 0 when the paragraph does not open a padded cell
     */
    private long takeFromTheTopOfItsCell(XWPFParagraph target, long twips) {
        if (twips <= 0 || currentCell == null || !cellsGivingTheirPadding.contains(currentCell.getCTTc())
            || currentCell.getBodyElements().isEmpty()
            || !(currentCell.getBodyElements().get(0) instanceof XWPFParagraph first)
            || first.getCTP() != target.getCTP()) {
            return 0;
        }
        CTTcPr cellProperties = currentCell.getCTTc().isSetTcPr() ? currentCell.getCTTc().getTcPr() : null;
        if (cellProperties == null || !cellProperties.isSetTcMar() || !cellProperties.getTcMar().isSetTop()) {
            return 0;
        }
        var top = cellProperties.getTcMar().getTop();
        long padding = twipsOf(top.getW());
        long taken = Math.min(padding, twips);
        if (taken > 0) {
            top.setW(BigInteger.valueOf(padding - taken));
        }
        return Math.max(0, taken);
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
        double before = source.margin().top() + source.padding().top();
        if (source == topEdgeHeldAbove) {
            // Held by the line above it, on the page the layout starts it on.
            before -= topEdgeHeld;
            topEdgeHeldAbove = null;
            topEdgeHeld = 0;
            topEdgeLineOver = target;
        }
        if (target == hangingOver) {
            before = Math.max(0, before - hangingOverBy);
        }
        hangingOver = null;
        hangingOverBy = 0;
        if (target == pullLeftOn) {
            before = Math.max(0, before - pullLeft);
        }
        pullLeftOn = null;
        pullLeft = 0;
        addSpacing(target, before, 0);
        double below = source.margin().bottom() + source.padding().bottom();
        // OrangeOps' role bar is pulled 3.8pt up into its name's line; dropped, the pull set
        // the bar and the whole page under it that much low in Word, and onto a second page.
        if (below < 0) {
            pullBelow -= below;
        } else {
            owePendingSpacingAfter(below);
        }
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
     * added, so every indent written by page side has to be on it by then — the inset is,
     * since a body paragraph is created with it. An indent measured from the flow's start, as
     * a blank prefix's is ({@link #indentAsThePrefixDoes}), goes on after the turn.</p>
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
        writeParagraphRuns(para, node, rightToLeft, 0, false);
    }

    /**
     * Writes a paragraph's runs into a Word paragraph whose line starts above the page's first
     * line of it: a line pair's line starts at the higher of its two texts.
     *
     * @param lineTopAbove how far above the page's first line of the paragraph the Word line
     *                     starts, in points
     * @param ownLine      whether the Word paragraph is this paragraph's alone, its lines the
     *                     page's — not a line pair's, a stack's or a text box's
     */
    private void writeParagraphRuns(XWPFParagraph para, ParagraphNode node, boolean rightToLeft,
                                    double lineTopAbove, boolean ownLine) {
        int runsBefore = para.getCTP().sizeOfRArray() == 0 && para.getCTP().sizeOfHyperlinkArray() == 0
                ? 0 : runsIn(para).size();
        warnDroppedInlineRuns(node);
        String path = layout.pathOf(node);
        java.util.Optional<com.demcha.compose.document.layout.payloads.ParagraphLine> line = layout.firstLine(node);
        boolean wroteARun = false;
        PictureReach pictures = PictureReach.NONE;
        // The mark closes the last line, so it is sized as the text that ends it.
        DocumentTextStyle markStyle = node.textStyle();
        // A shape's picture is wider than its box on either side: the text just before it gives
        // back the room on the left, so its ink starts where the page starts it, and the text
        // just after it the room on the right (giveBackWidth).
        double widthOwed = 0;
        XWPFRun textBefore = null;
        java.util.Map<XWPFRun, Double> givenBack = new java.util.LinkedHashMap<>();
        java.util.Map<XWPFRun, String> lettersOf = new java.util.HashMap<>();
        for (InlineRun run : node.inlineRuns()) {
            InlineTextRun text = textOf(run);
            if (text == null) {
                PictureReach reach = writeInlinePicture(para, run, node.linkTarget(), path, line);
                if (reach != null) {
                    wroteARun = true;
                    pictures = pictures.max(reach);
                    if (run instanceof InlineShapeRun shape && !rightToLeft) {
                        if (textBefore != null) {
                            givenBack.merge(textBefore, DocxShapePictures.widthBeforeItsBox(shape), Double::sum);
                        }
                        widthOwed += DocxShapePictures.widthAfterItsBox(shape);
                    }
                }
                textBefore = null;
                continue;
            }
            // A run's own link wins over the paragraph's: a sentence with one linked phrase
            // in it is the ordinary case, and the paragraph's link is the fallback for the
            // rest of that sentence rather than something the phrase overrides away.
            DocumentLinkTarget target = text.linkTarget() != null ? text.linkTarget() : node.linkTarget();
            XWPFRun docRun = newRun(para, target);
            markStyle = text.textStyle() == null ? node.textStyle() : text.textStyle();
            applyStyle(docRun, markStyle);
            applyRunDirection(docRun, rightToLeft);
            applyInlineBackground(docRun, backgroundOf(run), path);
            setTextBrokenAtLines(docRun, text.text());
            if (widthOwed > 0) {
                givenBack.merge(docRun, widthOwed, Double::sum);
            }
            widthOwed = 0;
            textBefore = docRun;
            lettersOf.put(docRun, text.text());
            wroteARun = true;
        }
        givenBack.forEach((textRun, room) -> giveBackWidth(textRun, lettersOf.get(textRun), room));
        if (!wroteARun) {
            XWPFRun docRun = newRun(para, node.linkTarget());
            applyStyle(docRun, node.textStyle());
            applyRunDirection(docRun, rightToLeft);
            setTextBrokenAtLines(docRun, node.text());
        }
        // A line pair's or a stack's line is cut to its own measure, not the paragraph's. A line
        // with no text is not seated on the page's baseline (shiftToThePagesBaseline), and held
        // exact its picture would stand where Word's baseline puts it, cut by the line's top.
        double heldAbove = ownLine && layout.lineCount(node) == 1 && holdsText(node)
                ? holdPicturesInTheLine(para, pictures, seatShift(node)) : Double.NaN;
        // A line held to its pictures has their ink within half a point of its edges.
        boolean cutToTheInk = !Double.isNaN(heldAbove);
        if (Double.isNaN(heldAbove)) {
            makeRoomForPictures(para, pictures);
            heldAbove = 0;
        }
        styleTheMark(para, markStyle);
        // A paragraph the layout laid out none of — composed in a table cell, a page zone's, or
        // one of an export without a layout — has its style's line.
        double pageLine = pictures.pageLine() > 0 ? pictures.pageLine()
                : pictures.pageLine() == 0 ? styleLineHeight(node.textStyle()) : 0;
        if (pageLine > 0 && pictures.reach() >= pageLine - PICTURE_FILLS_ITS_LINE) {
            letThePicturesSetTheLine(para);
            if (ownLine) {
                takeThePicturesEdgesFromAround(para, pictures);
            }
        }
        seatInTheLine(para, node, runsBefore, lineTopAbove + heldAbove, cutToTheInk);
    }

    /**
     * Lets a line of pictures and no text be as tall as its pictures, where Word sizes the line.
     *
     * <p>A line Word sizes itself — no exact or least height written, as a paragraph the layout
     * laid out no line of is written — is its tallest run's height, and the paragraph's mark is
     * a run: its font's depth below the baseline went under the picture. {@code SlateOrange}'s
     * skills, a 12.4pt icon in a cell of its own beside each label, came out 0.2pt taller each in
     * Word, its tenth 2.5pt low and the column under them with it. The mark and the picture runs
     * are set at a point, whose depth is a fraction of one. A paragraph holding anything else —
     * a letter, a break, a tab, a field, a link's text — keeps its sizes.</p>
     *
     * <p>Only where the pictures fill the page's line ({@link #PICTURE_FILLS_ITS_LINE}): there
     * the line is the pictures' height on the page too. A smaller picture stands in a line the
     * page makes as tall as its font, and that line is the mark's: {@code VioletGrid}'s bullet
     * dots, their lines cut to the dots, set every bullet 5.8pt high.</p>
     */
    private static void letThePicturesSetTheLine(XWPFParagraph para) {
        CTPPr properties = para.getCTP().getPPr();
        if (!holdsOnlyPictures(para)
            || properties != null && properties.isSetSpacing() && properties.getSpacing().isSetLineRule()
               && properties.getSpacing().getLineRule() != STLineSpacingRule.AUTO) {
            return;
        }
        if (properties == null) {
            properties = para.getCTP().addNewPPr();
        }
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTParaRPr mark =
                properties.isSetRPr() ? properties.getRPr() : properties.addNewRPr();
        BigInteger point = BigInteger.valueOf(ONE_POINT_IN_HALF_POINTS);
        (mark.sizeOfSzArray() > 0 ? mark.getSzArray(0) : mark.addNewSz()).setVal(point);
        (mark.sizeOfSzCsArray() > 0 ? mark.getSzCsArray(0) : mark.addNewSzCs()).setVal(point);
        for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run : runsIn(para)) {
            CTRPr size = run.isSetRPr() ? run.getRPr() : run.addNewRPr();
            (size.sizeOfSzArray() > 0 ? size.getSzArray(0) : size.addNewSz()).setVal(point);
            (size.sizeOfSzCsArray() > 0 ? size.getSzCsArray(0) : size.addNewSzCs()).setVal(point);
        }
    }

    /**
     * Keeps a line of pictures and no text, grown to its pictures, as tall as the page's.
     *
     * <p>Word makes such a line, written at least the pictures' reach, as tall as the pictures
     * themselves — a drawn shape's transparent frame ({@link DocxShapePictures#EDGE}) above and
     * below its ink included. {@code MonogramSidebar}'s contact icons, a 22pt glyph each over
     * its line of text, stood in 22.5pt lines, and every contact under them half a point lower
     * than the one above. What the pictures' edges reach past the page's line is taken from the
     * space written above the line and from the space above what follows ({@link #hangingBelow}),
     * as a line held to its pictures takes its ink's ({@link #holdPicturesInTheLine}); the ink
     * then stands where the page draws it, and what follows where the page sets it.</p>
     *
     * <p>Only a drawn shape's frame, and only where the pictures' edges pass the line written:
     * where they stay within it Word keeps that height, and taking the room would lift what
     * follows. A line of its own only: half of a line pair shares its line with the other half.</p>
     */
    private void takeThePicturesEdgesFromAround(XWPFParagraph para, PictureReach pictures) {
        CTPPr properties = para.getCTP().getPPr();
        if (!holdsOnlyPictures(para) || properties == null || !properties.isSetSpacing()
            || !properties.getSpacing().isSetLineRule()
            || properties.getSpacing().getLineRule() != STLineSpacingRule.AT_LEAST
            || !Double.isFinite(pictures.boxAbove()) || !Double.isFinite(pictures.boxBelow())
            || !(pictures.boxAbove() > pictures.above() + 1e-9 || pictures.boxBelow() > pictures.below() + 1e-9)) {
            return;
        }
        Long written = writtenTwips(properties.getSpacing().getLine());
        // The pictures' box, edge to edge, and how much taller than the line written Word makes
        // the line for it: that much is taken, from above as far as the box passes the line's
        // top, the rest from below.
        double box = pictures.pageLine() + pictures.boxAbove() + pictures.boxBelow();
        double grown = written == null ? 0 : box - written / POINT_TO_TWIP;
        if (!(grown * POINT_TO_TWIP > 0.5)) {
            return;
        }
        CTSpacing spacing = properties.getSpacing();
        long before = spacing.isSetBefore() ? twipsOf(spacing.getBefore()) : 0;
        long up = Math.min(before, Math.round(Math.min(grown, Math.max(0, pictures.boxAbove())) * POINT_TO_TWIP));
        if (up > 0) {
            spacing.setBefore(BigInteger.valueOf(before - up));
        }
        double down = grown - up / POINT_TO_TWIP;
        if (down > 0) {
            hangingBelow = Math.max(hangingBelow, down);
        }
    }

    /**
     * Whether a paragraph's runs, a link's included, hold pictures and nothing else: no
     * letter, break, tab, symbol or field. An internal link's runs are not among the
     * paragraph's own, so its text is not in {@link XWPFParagraph#getText()}.
     */
    private static boolean holdsOnlyPictures(XWPFParagraph para) {
        if (para.getCTP().sizeOfFldSimpleArray() > 0) {
            return false;
        }
        boolean picture = false;
        for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run : runsIn(para)) {
            if (run.sizeOfBrArray() > 0 || run.sizeOfTabArray() > 0 || run.sizeOfSymArray() > 0
                || run.sizeOfFldCharArray() > 0 || run.sizeOfInstrTextArray() > 0) {
                return false;
            }
            for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText text : run.getTArray()) {
                if (text.getStringValue() != null && !text.getStringValue().isEmpty()) {
                    return false;
                }
            }
            picture |= run.sizeOfDrawingArray() > 0 || run.sizeOfPictArray() > 0;
        }
        return picture;
    }

    /** Whether a paragraph laid out a line holding text, which its seat is read from. */
    private boolean holdsText(ParagraphNode node) {
        return layout.lines(node).stream().anyMatch(line -> line.spans().stream().anyMatch(
                span -> span instanceof com.demcha.compose.document.layout.payloads.ParagraphTextSpan));
    }

    /**
     * Raises or lowers a paragraph's text in its lines by as much as the page seats it off its
     * baseline ({@link TextVerticalAlign}), as a run position Word keeps inside the line.
     *
     * <p>A line's height is written as the page's; where in it the text sits is not.
     * {@code LumaStudioInvoice}'s title sets "INVOICE" against the top of a line far taller
     * than its capitals, and Word set it on the line's foot: 20pt low, its rule through the
     * letters and everything under it lower. Its lockup's "L" stood on the "&amp;Co." set under
     * it.</p>
     *
     * <p>Only this paragraph's runs move — a line pair writes another's in the same Word
     * paragraph — and a picture's own raise is added to, as the page moves a picture with its
     * line's seated baseline. The room made for a picture in the line is its unseated reach. A
     * page zone's paragraph has no laid-out lines here, and is written on its baseline.</p>
     *
     * <p>The baseline the page seats off is not where Word puts it either (see
     * {@link #shiftToThePagesBaseline}), and the two moves are one position.</p>
     *
     * @param runsBefore   how many runs the Word paragraph held before this one's were written
     * @param lineTopAbove how far above the page's first line of the paragraph the Word line
     *                     starts, in points
     * @param heldExact    whether the line was held to its pictures' reach
     *                     ({@link #holdPicturesInTheLine}), and so is seated however little
     */
    private void seatInTheLine(XWPFParagraph para, ParagraphNode node, int runsBefore, double lineTopAbove,
                               boolean heldExact) {
        long halfPoints = Math.round((seatShift(node) + shiftToThePagesBaseline(para, node, lineTopAbove, heldExact))
                                     * HALF_POINTS_PER_POINT);
        if (halfPoints == 0) {
            return;
        }
        List<org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR> runs = runsIn(para);
        for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run : runs.subList(runsBefore, runs.size())) {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr properties =
                    run.isSetRPr() ? run.getRPr() : run.addNewRPr();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSignedHpsMeasure position =
                    properties.sizeOfPositionArray() > 0 ? properties.getPositionArray(0) : properties.addNewPosition();
            long raised = position.getVal() instanceof Number number ? number.longValue() : 0;
            position.setVal(BigInteger.valueOf(raised + halfPoints));
        }
    }

    /**
     * A paragraph's runs in document order, the ones inside its links included: an internal
     * link's runs are not among {@link XWPFParagraph#getRuns()}.
     */
    private static List<org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR> runsIn(XWPFParagraph para) {
        List<org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR> runs = new ArrayList<>();
        try (org.apache.xmlbeans.XmlCursor cursor = para.getCTP().newCursor()) {
            cursor.selectPath("declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' "
                              + "./w:r | ./w:hyperlink/w:r");
            while (cursor.toNextSelection()) {
                if (cursor.getObject() instanceof org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run) {
                    runs.add(run);
                }
            }
        }
        return runs;
    }

    /**
     * How far the page moves a paragraph's text off its baseline to seat it by its cap band, in
     * points, positive up: the PDF backend's own correction
     * ({@link com.demcha.compose.document.backend.fixed.pdf.handlers.ParagraphSeating}), from the
     * fonts the layout measured with. Read off the first line holding text — a picture alone on
     * the first line seats nothing — and 0 when the paragraph sits on its baseline. One Word
     * paragraph takes one shift: the page seats each line by its own, which differs only for
     * lines in different sizes.
     */
    private double seatShift(ParagraphNode node) {
        if (node.verticalAlign() == null || node.verticalAlign() == TextVerticalAlign.DEFAULT) {
            return 0;
        }
        for (com.demcha.compose.document.layout.payloads.ParagraphLine line : layout.lines(node)) {
            if (line.spans().stream().anyMatch(
                    span -> span instanceof com.demcha.compose.document.layout.payloads.ParagraphTextSpan)) {
                return com.demcha.compose.document.backend.fixed.pdf.handlers.ParagraphSeating
                        .shift(line, measuredFonts(), node.verticalAlign());
            }
        }
        return 0;
    }

    /**
     * How far Word's baseline in a paragraph's exact line stands below the page's, in points,
     * positive when Word's is lower: the raise that stands the text where the page sets it.
     *
     * <p>The page sets a line's text its ascent below the line's top. Word stands the baseline of
     * an exact line four fifths of the way down it whatever the face (see
     * {@link DocxTextBands#BASELINE_SHARE}). The two agree for a face whose ascent is about four
     * fifths of its line, as Lato's is, and not for one with a deep descent: Spectral's 46pt
     * title line, 70pt tall, stood 7pt low in Word. A line written shorter than the page's own
     * — a title's lines stacked a pitch apart — moves Word's baseline up with it, and a line
     * that starts above the page's, as a line pair's does, takes that distance with it.</p>
     *
     * <p>Read off the first line holding text and the height the paragraph was written at, the
     * gap between lines included, at the paragraph's middle line: where the space above could not
     * give up a whole gap, Word's lines step a little closer than the page's, and the error is
     * shared by the first and last. 0 for a paragraph not written at an exact height: Word
     * then seats it by its own measure of the face. A difference under
     * {@link #LEAST_BASELINE_SHIFT_POINTS} — a quarter point for a line of Lato body text — is
     * left as Word sets it: the position counts in half points, and every line of body text
     * moved by one would win a quarter point at most.</p>
     *
     * @param lineTopAbove how far above the page's first line the Word line starts, in points
     * @param heldExact    whether the line was cut to its pictures' reach, and so is seated
     *                     however little
     */
    private double shiftToThePagesBaseline(XWPFParagraph para, ParagraphNode node, double lineTopAbove,
                                           boolean heldExact) {
        CTPPr properties = para.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing()) {
            return 0;
        }
        CTSpacing spacing = properties.getSpacing();
        Long written = spacing.isSetLineRule() && spacing.getLineRule() == STLineSpacingRule.EXACT
                ? writtenTwips(spacing.getLine()) : null;
        if (written == null) {
            return 0;
        }
        for (com.demcha.compose.document.layout.payloads.ParagraphLine line : layout.lines(node)) {
            if (line.spans().stream().anyMatch(
                    span -> span instanceof com.demcha.compose.document.layout.payloads.ParagraphTextSpan)) {
                // Lines whose gaps Word shares out step closer than the page's; the middle one is
                // matched, so the first and last are off by as little as the steps allow.
                double wordLine = written / POINT_TO_TWIP;
                double middle = Math.max(0, layout.lineCount(node) - 1) / 2.0;
                double pageStep = line.lineHeight() + layout.lineGap(node);
                double pages = lineTopAbove + lineTopsTakenIn.getOrDefault(para.getCTP(), 0.0)
                               + middle * pageStep + line.lineHeight() - line.baselineOffsetFromBottom();
                double shift = middle * wordLine + wordLine * DocxTextBands.BASELINE_SHARE - pages;
                // A line starting elsewhere than the page's — a stack's, a line pair's — was cut
                // to fit its letters where the page sets them, and is seated however little; so
                // is one cut to its pictures' reach, whose ink stands at its edges.
                boolean cutToFit = lineTopAbove != 0 || stackedLineHeights.containsKey(node) || heldExact;
                return Math.abs(shift) < LEAST_BASELINE_SHIFT_POINTS && !cutToFit ? 0 : shift;
            }
        }
        return 0;
    }

    /**
     * Sets a run's letters closer by the room the shapes beside it take past their boxes.
     *
     * <p>A shape is written as a picture wider than its box on either side, by its ink's
     * overhang and an edge ({@link DocxShapePictures#widthBeforeItsBox},
     * {@link DocxShapePictures#widthAfterItsBox}). Word sets a picture at its width whatever
     * spacing its run states, and letters closer when theirs is condensed: a run gives back the
     * room the shape before it takes on its right and the shape after it on its left, spread
     * over its letters. Word sets a letter's spacing after every letter in tenths of a point —
     * measured, two quarter points on two letters of a gap came out three tenths each — and
     * LibreOffice moved nothing for a space condensed in a run of its own; so each letter of the
     * run is set closer by whole tenths, as near the room as they come. A run too long for a
     * tenth a letter gives back nothing. {@code MidnightNavy}'s language dots, five to a line
     * with four spaces between each, stood 2.5pt wider than the page's in Word, and the fifth
     * broke onto a second line. A run holding a line break is left as it is, its letters on two
     * lines.</p>
     *
     * @param points the room the shapes beside the run take past their boxes, in points
     */
    private static void giveBackWidth(XWPFRun run, String text, double points) {
        if (text == null || text.isEmpty() || LINE_BREAK.matcher(text).find()) {
            return;
        }
        long tenths = Math.round(points * 10 / text.codePointCount(0, text.length()));
        if (tenths != 0) {
            run.setCharacterSpacing((int) (run.getCharacterSpacing() - tenths * 2));
        }
    }

    /** A line break in text, as the page breaks lines at it (see {@code ParagraphWrapping}). */
    private static final java.util.regex.Pattern LINE_BREAK = java.util.regex.Pattern.compile("\r\n|\r|\n");

    /**
     * Writes a run's text with each line break the page makes as Word's own.
     *
     * <p>Word reads a {@code "\n"} inside {@code w:t} as a space: an invoice's addressee —
     * name, street, city, email and phone, one paragraph whose lines the page breaks at its
     * {@code "\n"}s — came out as one wrapped line in {@code ClassicInvoice}, and everything
     * under it stood as much higher as the lines it lost. Each break is a {@code w:br}.</p>
     */
    private static void setTextBrokenAtLines(XWPFRun run, String text) {
        String[] lines = LINE_BREAK.split(text == null ? "" : text, -1);
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                run.addBreak();
            }
            run.setText(lines[index], index);
        }
    }

    /**
     * Lets a line hold the pictures in it (see {@link PictureReach}).
     *
     * <p>A paragraph whose pictures stay within its text keeps its exact height. One holding a
     * picture that rises above the text is written with its lines <em>at least</em> the
     * height the picture reaches where Word puts it instead: the editor then grows the line to
     * the picture rather
     * than clip it, whichever editor it is and wherever it puts its baseline, and where the
     * picture fits the line is the page's. What that costs: Word has one line height for a
     * paragraph, so every line of it is then at least the picture's reach, and otherwise the
     * editor's own measure of its text, which in LibreOffice is taller than the page's —
     * where the page makes only the line holding the picture taller. A paragraph written with
     * no exact height — a page zone's, one with no layout — grows to its pictures on its own
     * and is left alone. A paragraph of one line is held to the page's line instead where it
     * can be ({@link #holdPicturesInTheLine}).</p>
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
     * Holds a paragraph of one line and its pictures at the page's height, where Word sets them
     * as the page does.
     *
     * <p>The line stays exact, at the page's height of it: Word then sets its text on the
     * page's baseline ({@link #seatInTheLine}) and the pictures with it, where the page puts
     * them. Grown "at least" to a picture instead ({@link #makeRoomForPictures}), Word made the
     * line its own height — {@code TimelineMinimal}'s contact lines, a 10.5pt icon beside
     * smaller text, each came out 0.9pt taller, and the page under them 4.4pt low; LibreOffice,
     * measuring its text taller still, 11pt low.</p>
     *
     * <p>A picture's ink reaching past the page's line is drawn on the page in the gaps around
     * it; an exact Word line cuts it off. So the line reaches as far, taking that room from the
     * space written above it and from the space above what follows ({@link #hangingBelow}),
     * and keeps its pitch. The picture's raise and the line's seat are each rounded to a half
     * point, so the line keeps {@link #INK_ROOM_POINTS} past the ink on either side, taken the
     * same way, as far as the space above goes: an icon as tall as its line lost 0.2 to 0.4pt at
     * an edge without it. A line whose space above is shorter than the ink's own reach is grown
     * as before. The vertical seat
     * ({@link #seatShift}) moves the pictures with the text, as on the page. LibreOffice stands
     * a picture on the baseline, higher than the page does, and cuts what passes the line's
     * top.</p>
     *
     * @param seat how far the page seats the line's text and pictures off its baseline, in
     *             points, raised
     * @return how far above the page's line the Word line now starts, in points, or NaN when
     *         the line is left to {@link #makeRoomForPictures}
     */
    private double holdPicturesInTheLine(XWPFParagraph para, PictureReach pictures, double seat) {
        CTPPr properties = para.getCTP().getPPr();
        // A picture inside its text keeps the exact line it has (makeRoomForPictures).
        if (pictures == null || !pictures.overText() || !(pictures.reach() > 0) || !(pictures.pageLine() > 0)
            || properties == null || !properties.isSetSpacing()) {
            return Double.NaN;
        }
        CTSpacing spacing = properties.getSpacing();
        if (!spacing.isSetLineRule() || spacing.getLineRule() != STLineSpacingRule.EXACT) {
            return Double.NaN;
        }
        long before = spacing.isSetBefore() ? twipsOf(spacing.getBefore()) : 0;
        // The ink itself must fit under the space above; the room past it is taken as far as
        // that space goes — a line opening its cell has none.
        if (Math.round(Math.max(0, pictures.above() + seat) * POINT_TO_TWIP) > before) {
            return Double.NaN;
        }
        long up = Math.min(before, Math.round(Math.max(0, pictures.above() + seat + INK_ROOM_POINTS) * POINT_TO_TWIP));
        Long current = writtenTwips(spacing.getLine());
        long page = Math.round(pictures.pageLine() * POINT_TO_TWIP);
        long down = Math.round(Math.max(0, pictures.below() - seat + INK_ROOM_POINTS) * POINT_TO_TWIP);
        spacing.setLine(BigInteger.valueOf((current == null ? page : Math.max(current, page)) + up + down));
        if (up > 0) {
            spacing.setBefore(BigInteger.valueOf(before - up));
        }
        if (down > 0) {
            hangingBelow = Math.max(hangingBelow, down / POINT_TO_TWIP);
        }
        return up / POINT_TO_TWIP;
    }

    /**
     * The room a line held to its pictures keeps past their ink on either side, in points: the
     * picture's raise and the line's seat are rounded to a half point each.
     */
    private static final double INK_ROOM_POINTS = 0.5;

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
            return PictureReach.unplaced(height);
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
     * <p>The page's own line holds most of a picture already: a contact line's icon, taller
     * than its text and hanging below it, makes the line as tall as the icon, and what its ink
     * reaches past the line the page draws in the gaps around it ({@link #holdPicturesInTheLine}).</p>
     *
     * @param reach    how far above the line's bottom the highest picture reaches, in points
     * @param overText whether a picture passes the text's ascent or descent
     * @param pageLine the page's height of the line, 0 when where the pictures stand is not
     *                 known, and NaN when no picture was written
     * @param above    how far the highest picture's ink reaches above the page's line where Word
     *                 puts it, in points; negative when it stays that far inside
     * @param below    how far the lowest picture's ink reaches below it, in points; negative when
     *                 it stays that far inside
     * @param boxAbove how far the highest picture's own edge reaches above the page's line, a
     *                 drawn shape's transparent frame included; {@code above} for a picture with
     *                 no frame
     * @param boxBelow how far the lowest picture's own edge reaches below it, likewise
     */
    record PictureReach(double reach, boolean overText, double pageLine, double above, double below,
                        double boxAbove, double boxBelow) {

        PictureReach(double reach, boolean overText, double pageLine, double above, double below) {
            this(reach, overText, pageLine, above, below, above, below);
        }

        /** No picture written. */
        static final PictureReach NONE = new PictureReach(0, false, Double.NaN,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

        /** A picture in a line the page laid out none of: where it stands is not known. */
        static PictureReach unplaced(double height) {
            return new PictureReach(height, false, 0, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        }

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
            // Word's placement, and LibreOffice's on the baseline. Either one passing the text
            // makes the line "at least": the editor then grows it to what it placed. The height
            // asked for is Word's, where the picture is where the page puts it; asking for
            // LibreOffice's made every such line taller than the page in Word as well.
            double wordTop = bottomFromBaseline + height - inset;
            double top = Math.max(wordTop, height - inset);
            boolean passes = top > line.textAscent() || -(bottomFromBaseline + inset) > descent;
            double above = descent + wordTop - line.lineHeight();
            double below = -(bottomFromBaseline + inset) - descent;
            // The picture's own edges, frame and all: what a line Word sizes to it spans.
            return new PictureReach(descent + wordTop, passes, line.lineHeight(), above, below,
                    above + inset, below + inset);
        }

        PictureReach max(PictureReach other) {
            if (other == null) {
                return this;
            }
            double line = Double.isNaN(pageLine) ? other.pageLine
                    : Double.isNaN(other.pageLine) ? pageLine
                    : pageLine > 0 && other.pageLine > 0 ? Math.max(pageLine, other.pageLine) : 0;
            return new PictureReach(Math.max(reach, other.reach), overText || other.overText, line,
                    Math.max(above, other.above), Math.max(below, other.below),
                    Math.max(boxAbove, other.boxAbove), Math.max(boxBelow, other.boxBelow));
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
     * page's content width, and is used here so the two agree where the layout did not place
     * the image; where it did, the box is the one it placed, less its padding.</p>
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

        // Laid out, the picture is the size the page draws it: the width left where it is written
        // narrows by every container's insets round it, and a ring wider than the column it
        // stands in — NavySidebar's portrait, 127pt in a 123.8pt column — shrank its photo by
        // the ring's width on both sides, 120.6pt for 123.8.
        // Its placement holds its padding: the top and the bottom are written as its paragraph's
        // space, the sides are not written.
        com.demcha.compose.document.layout.PlacedNode laidOut = layout.placement(node);
        if (laidOut != null) {
            DocumentInsets padding = node.padding();
            double placedWidth = laidOut.placementWidth() - padding.left() - padding.right();
            double placedHeight = laidOut.placementHeight() - padding.top() - padding.bottom();
            if (placedWidth > 0 && placedHeight > 0) {
                box = new NodeDefinitionSupport.ImageDimensions(placedWidth, placedHeight);
            }
        }
        double drawWidth = box.width();
        double drawHeight = box.height();
        if (fitMode == DocumentImageFitMode.CONTAIN) {
            double scale = Math.min(box.width() / sourceWidth, box.height() / sourceHeight);
            drawWidth = sourceWidth * scale;
            drawHeight = sourceHeight * scale;
        }
        if (drawnOverItsBadge(node, clipContainer)) {
            drawWhereThePagePutsIt(document, node, bytes, sourceWidth, sourceHeight,
                    "drawn over the badge holding it");
            return;
        }
        if (picturesDrawnBeside.contains(node)) {
            drawWhereThePagePutsIt(document, node, bytes, sourceWidth, sourceHeight,
                    "drawn beside the text it labels");
            return;
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
            // A portrait clipped to a circle: the picture takes the circle's shape, which both
            // editors crop it to, instead of standing square over the ring drawn round it.
            if (fillsItsEllipse(node, clipContainer) && picture.getCTPicture().getSpPr().isSetPrstGeom()) {
                picture.getCTPicture().getSpPr().getPrstGeom()
                        .setPrst(org.openxmlformats.schemas.drawingml.x2006.main.STShapeType.ELLIPSE);
            }
        }
    }

    /**
     * Whether a picture is what a shape container clips to an ellipse, filling it: the ellipse
     * inscribed in the picture's box is then the one the page clips it to. A logo smaller than
     * its round badge stands whole inside the circle, and stays square.
     */
    private boolean fillsItsEllipse(ImageNode image, ShapeContainerNode container) {
        if (container == null
            || !(container.outline() instanceof com.demcha.compose.document.style.ShapeOutline.Ellipse)) {
            return false;
        }
        com.demcha.compose.document.layout.PlacedNode clip = layout.placement(container);
        com.demcha.compose.document.layout.PlacedNode placed = layout.placement(image);
        if (clip == null || placed == null) {
            return false;
        }
        double tolerance = 1;
        return Math.abs(placed.placementX() - clip.placementX()) <= tolerance
               && Math.abs(placed.placementY() - clip.placementY()) <= tolerance
               && Math.abs(placed.placementWidth() - clip.placementWidth()) <= tolerance
               && Math.abs(placed.placementHeight() - clip.placementHeight()) <= tolerance;
    }

    /**
     * Whether a picture is a glyph its badge holds, drawn over the badge rather than written in
     * the flow: a layer of a painted shape container that clips it to its outline and holds
     * nothing else but drawing, smaller than the badge and standing inside it on one page.
     * Written as a paragraph of its own, the glyph took a line above the title set beside the
     * badge, and stood that line's height and its leading off the circle's middle.
     *
     * <p>A photo filling its frame or its circle is the picture the flow is written round, and
     * stays in it; so does a logo in a card that holds text, a picture cropped to cover its box,
     * and one carrying an anchor, whose bookmark needs a paragraph. Inside a painted panel the
     * badge and its glyph stand in front of the text: a panel is a shaded cell, and both editors
     * paint its shading over a drawing behind the text. Kept in the flow there, the glyph was
     * written white on the panel's grey and its disc hid under the shading —
     * {@code NorthlineProposal}'s acceptance heading lost its badge.</p>
     */
    private boolean drawnOverItsBadge(ImageNode image, ShapeContainerNode badge) {
        if (badge == null || badge.clipPolicy() != com.demcha.compose.document.style.ClipPolicy.CLIP_PATH
            || badge.fillColor() == null && badge.stroke() == null
            || image.anchor() != null && !image.anchor().isBlank()
            || image.fitMode() == DocumentImageFitMode.COVER
            || Double.isNaN(canvasHeight)
            || badge.children().stream().noneMatch(layer -> layer == image)
            || !badge.children().stream().allMatch(layer -> layer instanceof ImageNode || onlyDrawing(layer))) {
            return false;
        }
        com.demcha.compose.document.layout.PlacedNode box = layout.placement(badge);
        com.demcha.compose.document.layout.PlacedNode placed = layout.placement(image);
        if (box == null || placed == null || placed.startPage() != placed.endPage()) {
            return false;
        }
        double edge = 0.5;
        boolean inside = placed.placementX() >= box.placementX() - edge
                         && placed.placementX() + placed.placementWidth() <= box.placementX() + box.placementWidth() + edge
                         && placed.placementY() >= box.placementY() - edge
                         && placed.placementY() + placed.placementHeight() <= box.placementY() + box.placementHeight() + edge;
        boolean smaller = placed.placementWidth() < box.placementWidth() - 1
                          || placed.placementHeight() < box.placementHeight() - 1;
        return inside && smaller;
    }

    /** The most characters a badge's text is drawn in it rather than written: initials, a monogram. */
    private static final int BADGE_TEXT_LIMIT = 4;

    /**
     * The paragraph a badge holds when its initials are drawn in it rather than written: the
     * badge's one layer, a line of plain text a few characters long, the badge filled or stroked
     * and placed on one page.
     *
     * <p>Written apart, the initials stood in the flow and the badge where the page puts it, and
     * the two parted wherever the editor set the line differently — {@code ObsidianInvoice}'s
     * footer "K" sat below its disc's corner — and inside a painted panel the disc, drawn behind
     * the letters it frames, was hidden under the panel's shading. Held in one shape the two
     * cannot part, and the shape can stand in front, the text being its own.</p>
     *
     * @return the paragraph, or {@code null} when the badge is written as before
     */
    private ParagraphNode textBadgeParagraph(ShapeContainerNode badge) {
        boolean filled = badge.fillColor() != null && badge.fillColor().color().getAlpha() > 0;
        boolean stroked = badge.stroke() != null && badge.stroke().width() > 0 && badge.stroke().color() != null
                          && badge.stroke().color().color().getAlpha() > 0;
        if (!filled && !stroked
            || badge.children().size() != 1 || !(badge.children().get(0) instanceof ParagraphNode paragraph)
            || badge.transform() != null && !badge.transform().isIdentity()
            || Double.isNaN(canvasHeight)) {
            return null;
        }
        // The shape centres one run of text left to right in a rectangle, a rounded rectangle
        // or an ellipse: a layer set in a corner, initials in two styles, a right-to-left line
        // or an outline drawn as a path is written as before, its text in the flow.
        com.demcha.compose.document.node.LayerStackNode.Layer layer = badge.layers().get(0);
        if (layer.align() != com.demcha.compose.document.node.LayerAlign.CENTER
            || layer.offsetX() != 0 || layer.offsetY() != 0
            || badge.outline() instanceof com.demcha.compose.document.style.ShapeOutline.Polygon
            || badge.outline() instanceof com.demcha.compose.document.style.ShapeOutline.Path
            || paragraph.direction() != TextDirection.LTR
            || paragraph.inlineRuns() != null && paragraph.inlineRuns().stream()
                    .map(run -> run instanceof InlineTextRun text ? text.textStyle() : null)
                    .distinct().count() > 1) {
            return null;
        }
        // A link, a bookmark or an anchor on the text is the paragraph's, which a shape does not
        // carry: such a badge is written as before.
        if (paragraph.linkTarget() != null || paragraph.bookmarkOptions() != null
            || paragraph.anchor() != null && !paragraph.anchor().isBlank()
            || paragraph.inlineRuns() != null && paragraph.inlineRuns().stream()
                    .anyMatch(run -> run instanceof InlineTextRun text && text.linkTarget() != null)) {
            return null;
        }
        String text = badgeTextOf(paragraph);
        if (text == null || text.isBlank() || text.strip().length() > BADGE_TEXT_LIMIT) {
            return null;
        }
        com.demcha.compose.document.layout.PlacedNode box = layout.placement(badge);
        if (box == null || box.startPage() != box.endPage() || layout.lineCount(paragraph) != 1) {
            return null;
        }
        return paragraph;
    }

    /**
     * Writes a badge as one shape holding its initials, where the page draws it (see
     * {@link #textBadgeParagraph}). In the flow it keeps the room the page gives it; inside
     * something already drawn, that holds the room.
     */
    private void writeTextBadge(XWPFDocument document, ShapeContainerNode badge, ParagraphNode initials) {
        if (overlayDepth == 0) {
            holdTheSpaceOf(badge);
        } else if (bandDepth > 0) {
            // A band measures its room above and below the text it writes, the initials among it:
            // a line as tall as theirs stands in their place, or the band came out that line short.
            com.demcha.compose.document.layout.PlacedNode line = layout.placement(initials);
            if (line != null && line.placementHeight() > 0) {
                XWPFParagraph standIn = newBodyParagraph(document);
                applyLineHeight(standIn, java.util.OptionalDouble.of(line.placementHeight()));
            }
        }
        badgeText = badgeParagraphXml(document, initials);
        try {
            drawOutlineOf(badge);
        } finally {
            badgeText = null;
        }
    }

    /** A paragraph's text when it is text alone, or {@code null} when it holds anything else. */
    private static String badgeTextOf(ParagraphNode paragraph) {
        if (paragraph.inlineRuns() == null || paragraph.inlineRuns().isEmpty()) {
            return paragraph.text();
        }
        StringBuilder text = new StringBuilder();
        for (InlineRun run : paragraph.inlineRuns()) {
            if (!(run instanceof InlineTextRun textRun) || textRun.text() == null) {
                return null;
            }
            text.append(textRun.text());
        }
        return text.toString();
    }

    /**
     * A badge's text as the paragraph its shape holds, styled as a run of the body is.
     *
     * @return the paragraph as {@code w:p} markup
     */
    private String badgeParagraphXml(XWPFDocument document, ParagraphNode paragraph) {
        XWPFParagraph para = detachedParagraph(document);
        para.setAlignment(ParagraphAlignment.CENTER);
        DocumentTextStyle style = paragraph.textStyle();
        if (paragraph.inlineRuns() != null && !paragraph.inlineRuns().isEmpty()
            && paragraph.inlineRuns().get(0) instanceof InlineTextRun first && first.textStyle() != null) {
            style = first.textStyle();
        }
        XWPFRun run = para.createRun();
        applyStyle(run, style);
        run.setText(badgeTextOf(paragraph).strip());
        return paragraphXml(para);
    }

    /** A paragraph held in no body, with no space above or below it: a shape's text. */
    private static XWPFParagraph detachedParagraph(XWPFDocument document) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP markup =
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP.Factory.newInstance();
        XWPFParagraph para = new XWPFParagraph(markup, document);
        CTPPr properties = markup.isSetPPr() ? markup.getPPr() : markup.addNewPPr();
        CTSpacing spacing = properties.isSetSpacing() ? properties.getSpacing() : properties.addNewSpacing();
        spacing.setBefore(BigInteger.ZERO);
        spacing.setAfter(BigInteger.ZERO);
        return para;
    }

    /** A detached paragraph as {@code w:p} markup, for a shape's text body. */
    private static String paragraphXml(XWPFParagraph para) {
        String main = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
        org.apache.xmlbeans.XmlOptions options = new org.apache.xmlbeans.XmlOptions();
        options.setSaveSyntheticDocumentElement(new javax.xml.namespace.QName(main, "p", "w"));
        options.setSaveSuggestedPrefixes(java.util.Map.of(main, "w"));
        options.setSaveAggressiveNamespaces();
        return para.getCTP().xmlText(options);
    }

    /**
     * Draws a picture where the page draws it, in the box the layout gave it: a badge's glyph
     * over the outline drawn before it ({@link #drawnOverItsBadge}), an icon beside its text
     * ({@link #drawnBesideItsText}). On a painted surface it stands in front of the text: the
     * surface's shading would hide it behind, and it frames no text of the flow.
     *
     * @param how what it is drawn as, for the report
     */
    private void drawWhereThePagePutsIt(XWPFDocument document, ImageNode image, byte[] bytes,
                                        double sourceWidth, double sourceHeight, String how) throws Exception {
        boolean inFront = surfaceBehind != null;
        com.demcha.compose.document.layout.PlacedNode placed = layout.placement(image);
        double width = placed.placementWidth();
        double height = placed.placementHeight();
        if (image.fitMode() == DocumentImageFitMode.CONTAIN) {
            double scale = Math.min(width / sourceWidth, height / sourceHeight);
            width = sourceWidth * scale;
            height = sourceHeight * scale;
        }
        String relationship = document.addPictureData(bytes, pictureType(bytes));
        double x = placed.placementX() + (placed.placementWidth() - width) / 2;
        double top = canvasHeight - placed.placementY() - (placed.placementHeight() + height) / 2;
        DocxDrawings.Shape picture = DocxDrawings.Shape.picture(x, top, width, height, placed.startPage(),
                relationship);
        drewInCell = false;
        queueDrawings(List.of(picture), inFront);
        report.add(DocxExportReport.Severity.APPROXIMATED, image.nodeKind(), layout.pathOf(image),
                drewInCell
                        ? how + ", anchored in the table cell it fills, where the layout puts it in the cell: "
                          + "it moves with the row"
                        : how + ", anchored to the page where the layout puts it: it stays "
                          + "there when the text around it is edited");
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
        // A rule in a container hanging left starts out past the margin with its text.
        indent.setLeft(BigInteger.valueOf(leftIndentTwips(insetLeft + sideLeft + from)));
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
     * two paragraphs with a gap between them. With nothing above it — opening the body, or a
     * row opening any cell but a table's, or a table opening a cell where a band or column layer
     * resumes — a paragraph a tenth of a point tall holds that edge
     * ({@link #holdTheSpaceAboveATable}); any other table opening a cell still loses it.</p>
     */
    private void writeTableWithItsOwnSpacing(XWPFDocument document, DocumentNode node)
            throws Exception {
        // The layout starts a block it moves to a new page at the block's own top edge: what
        // the page above holds below its last block, and the gap between the two, stay there.
        boolean onANewPage = currentCell == null && startsAPageOfItsOwn(node);
        if (onANewPage) {
            // A band's resumed gap is the page above's too: made the one owed first, so it is
            // written there rather than taken into the line below.
            resumeHere();
            flushSpacingAfter();
            // A border or a line hanging below the last block stands on the page above, as a
            // page break leaves it (writePageBreak).
            borderBelow = 0;
            forgetTheHang();
        }
        // A table a band's or a column's layer opens is that layer's first block, so it starts
        // where the layer resumes, as a paragraph does, and its own top margin comes after:
        // VioletGrid's education lines, beside a badge, lost their 2.3pt and stood 2.4pt high.
        // A row is not a first block: the band measures to the first one inside it, and the
        // row's cells write what stands above that block.
        boolean resumed = node instanceof TableNode && !Double.isNaN(resumeSpacing);
        if (resumed) {
            resumeHere();
        }
        owePendingSpacingAfter(node.margin().top() + node.padding().top());
        if (onANewPage) {
            holdTheSpaceAboveOnItsPage(document);
        }
        if ((node instanceof RowNode || resumed) && currentCell != null
            && !tablesCells.contains(currentCell.getCTTc())) {
            // At the top of a cell nothing above holds that space: MerchantInvoice's due-date
            // row lost its 16.7pt of top padding and stood against the card's top edge, once the
            // card held the page's height, and PaymentsInvoice's metadata grid the 6.2pt its
            // column is padded down by, standing 6.3pt above the issuer beside it. A row in a
            // table's cell keeps to the row height the table holds (holdRowHeight), and a table
            // is left as it was: holding the space of either moved ObsidianInvoice's line items
            // 6pt below the page's.
            holdTheSpaceAboveATable(document);
        }
        // Its side margins hold it in from the edges it is written between, as a paragraph's do:
        // VioletGrid's education lines, a table held 51.3pt clear of the badge beside them,
        // started under the badge, and PaymentsInvoice's bank details 36.7pt left of the page's.
        double outerLeft = insetLeft;
        double outerRight = insetRight;
        // A row's column already starts past the margin of the block the layout placed in it.
        insetLeft += node == leftMarginInCell ? 0 : node.margin().left();
        insetRight += node.margin().right();
        try {
            if (node instanceof RowNode row) {
                writeRow(document, row);
            } else {
                writeTable(document, (TableNode) node);
            }
        } finally {
            insetLeft = outerLeft;
            insetRight = outerRight;
        }
        owePendingSpacingAfter(node.margin().bottom() + node.padding().bottom());
    }

    private void writeTable(XWPFDocument document, TableNode node) throws Exception {
        if (node.rows().isEmpty()) {
            return;
        }
        CellDrawing outerCellDrawing = cellDrawing;
        List<CellFragment> outerDrawings = tableDrawings;
        CellDrawing drawn = drawCellDrawing(node);
        cellDrawing = new CellDrawing(outerCellDrawing.drew() || drawn.drew(),
                outerCellDrawing.skippedBoxes() || drawn.skippedBoxes(), List.of());
        if (!drawn.pending().isEmpty()) {
            tableDrawings = new ArrayList<>(drawn.pending());
        }
        try {
            writeTableRows(document, node);
        } finally {
            // What no cell took is drawn where the page puts it, in the order the table paints it.
            if (tableDrawings != outerDrawings) {
                for (CellFragment waiting : tableDrawings) {
                    anchors.queueOrdered(waiting.shapes());
                }
            }
            tableDrawings = outerDrawings;
            cellDrawing = outerCellDrawing;
        }
    }

    /**
     * What the tables being written drew of their composed cells.
     *
     * @param drew         whether they drew anything
     * @param skippedBoxes whether they left a box framing text to the panel it is written as
     * @param pending      the drawings one table's cells paint, waiting to be anchored
     */
    private record CellDrawing(boolean drew, boolean skippedBoxes, List<CellFragment> pending) {
        static final CellDrawing NONE = new CellDrawing(false, false, List.of());
    }

    /**
     * Whether a node composed in a cell was drawn by its table, so that it is not lost.
     *
     * <p>A path, an ellipse, a polygon or a line composed in a cell is always among what its table
     * draws once it draws anything. A box may be a box framing text left to its panel; while a
     * table skipped any, a box node is still reported, rather than one lost in silence.</p>
     */
    private boolean drawnByItsTable(DocumentNode node) {
        if (anchoredInCells.contains(node)) {
            return true;
        }
        if (!cellDrawing.drew() || !composedInACell(node)
            || !(isDrawing(node) || node instanceof ShapeContainerNode)) {
            // A node kind the table's drawing does not cover is reported as ever.
            return false;
        }
        if (node instanceof com.demcha.compose.document.node.PathNode path && !drawsAFlatColour(path)) {
            // A path painted only with a gradient is no shape the table draws.
            return false;
        }
        boolean box = node instanceof com.demcha.compose.document.node.ShapeNode
                      || node instanceof ShapeContainerNode container
                         && !(container.outline() instanceof com.demcha.compose.document.style.ShapeOutline.Ellipse);
        return !box || !cellDrawing.skippedBoxes();
    }

    /**
     * Whether a path's fragment carries a flat fill or a stroke. The layout turns a solid
     * {@code fillPaint} into the flat fill and otherwise uses {@code fillColor}; a gradient fill
     * travels as a paint only, which the drawing leaves out.
     */
    private static boolean drawsAFlatColour(com.demcha.compose.document.node.PathNode path) {
        boolean flatFill = path.fillPaint() == null
                ? path.fillColor() != null
                : path.fillPaint() instanceof com.demcha.compose.document.style.DocumentPaint.Solid;
        return flatFill || path.stroke() != null;
    }

    /**
     * Draws what a table's composed cells paint — an icon, a disc, a tile — where the page draws
     * it.
     *
     * <p>Content composed in a cell has no place of its own in the layout: its fragments belong
     * to the table, so no node of the cell had any to draw, and the export dropped every icon a
     * table's cells held — sixteen on one invoice. The table's own fragments are drawn instead,
     * all but a box framing text or a picture: that is a panel, written as a table of one cell
     * (see {@link #writtenAsAPanel}).</p>
     *
     * <p>Each is drawn behind the text or in front of it on its own account. One framing text —
     * a disc under a number — stays behind what it frames. Any other stands in front: both
     * editors paint a cell's shading over a drawing behind the text, and a cell is shaded
     * wherever its style names a fill, white included — the icons of an invoice's white lines
     * disappeared under it as surely as those of a rota's navy band. An icon in a cell stands in
     * a place of its own there, beside its label rather than under it.</p>
     */
    private CellDrawing drawCellDrawing(TableNode table) {
        List<com.demcha.compose.document.layout.PlacedFragment> fragments = layout.ownFragments(table);
        List<com.demcha.compose.document.layout.PlacedFragment> content = fragments.stream()
                .filter(fragment -> fragment.payload()
                        instanceof com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload
                        || fragment.payload()
                        instanceof com.demcha.compose.document.layout.payloads.ImageFragmentPayload)
                .toList();
        boolean drew = false;
        boolean skipped = false;
        List<CellFragment> pending = new ArrayList<>();
        for (com.demcha.compose.document.layout.PlacedFragment fragment : fragments) {
            boolean frames = framesText(fragment, content);
            if (frames && fragment.payload() instanceof com.demcha.compose.document.layout.payloads.ShapeFragmentPayload) {
                skipped = true;
                continue;
            }
            if (Double.isNaN(canvasHeight)) {
                continue;
            }
            List<DocxDrawings.Shape> shapes = DocxDrawings.of(fragment, canvasHeight);
            if (!shapes.isEmpty() || DocxCellDrawings.drawnKind(fragment) != null) {
                // Its place in the paint order is the table's, wherever it is anchored.
                pending.add(new CellFragment(fragment, anchors.ordered(
                        frames ? shapes : shapes.stream().map(DocxDrawings.Shape::inFront).toList())));
                drew |= !shapes.isEmpty();
            }
        }
        if (drew) {
            report.add(DocxExportReport.Severity.APPROXIMATED, "cell drawing", layout.pathOf(table),
                    "what its cells draw is drawn as shapes where the layout puts it — anchored in the "
                    + "cell a drawing is all of, and to the page otherwise, where it stays when the text "
                    + "around it is edited; a clip, a transform, a gradient or a dash on it is not carried");
        }
        return new CellDrawing(drew, skipped, pending);
    }

    /**
     * A drawing a table's composed cell paints, waiting to be anchored.
     *
     * @param fragment the table's fragment
     * @param shapes   the shapes it draws, each with its place in the paint order
     */
    private record CellFragment(com.demcha.compose.document.layout.PlacedFragment fragment,
                                List<DocxDrawingAnchors.Ordered> shapes) {
    }

    /**
     * Anchors a drawing composed alone in a table cell in that cell, where the page puts it in
     * the cell, rather than on the page.
     *
     * <p>A composed cell's content has no place of its own in the layout: its drawings are the
     * table's fragments (see {@link #drawCellDrawing}), placed from the page's edges, and so off
     * their row wherever Word sets the rows above a little taller or shorter than the page.
     * {@code CobaltRota}'s band icons, each alone in the first column of its navy strip, stood
     * 4pt, 9pt and 14pt above their labels, the last out of its strip. Its drawing is the first
     * of the table's waiting fragments inside the table's cell being written, where the layout
     * first placed it, when those are its shapes and the only drawing the cell holds (see
     * {@link DocxCellDrawings}). The cell's paragraph is held at the height of their box and
     * carries them, placed from its top and the cell's text column: the row carries them. Word
     * repeats a repeated header row with what is anchored in it, so the matching copies in the
     * cell's boxes on later pages are dropped.</p>
     *
     * <p>Only a layer stack of shapes — no container outline, no picture, no line — with no
     * margin or padding, in a cell holding nothing else yet; the cell's text column is taken to
     * start where its shapes do. Anything else is left to the page, as before.</p>
     */
    private void anchorComposedDrawing(XWPFTableCell cell, DocumentNode node) {
        if (tableDrawings == null || tableDrawings.isEmpty() || composedCellBoxes.isEmpty() || Double.isNaN(canvasHeight)
            || !composedInACell(node) || !holdsNothingYet(cell)) {
            return;
        }
        List<DocumentNode> shapes = node instanceof com.demcha.compose.document.node.LayerStackNode stack
                ? DocxCellDrawings.shapesOf(stack) : null;
        if (shapes == null || DocxCellDrawings.holdsALine(node)) {
            return;
        }
        // Only what the table's cell being written holds, where the layout first placed it — a
        // header's copies on later pages, and other cells' drawings, are not this one's — and
        // from the first of those still waiting: a drawing no cell took before this one is
        // never passed over and given to the next.
        // The only drawing the table's cell holds: where it holds others, which of them a later
        // node paints is not known, and one left to the page would be taken for the next.
        List<CellFragment> inTheCell = waitingIn(composedCellBoxes.get(0));
        if (inTheCell.size() != shapes.size()
            || !DocxCellDrawings.opensWith(inTheCell.stream().map(CellFragment::fragment).toList(), shapes)) {
            return;
        }
        List<CellFragment> own = inTheCell.subList(0, shapes.size());
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        List<DocxDrawingAnchors.Ordered> drawn = new ArrayList<>();
        for (CellFragment waiting : own) {
            com.demcha.compose.document.layout.PlacedFragment fragment = waiting.fragment();
            double fragmentTop = canvasHeight - fragment.y() - fragment.height();
            left = Math.min(left, fragment.x());
            top = Math.min(top, fragmentTop);
            right = Math.max(right, fragment.x() + fragment.width());
            bottom = Math.max(bottom, fragmentTop + fragment.height());
            drawn.addAll(waiting.shapes());
        }
        if (!(bottom - top > 0)) {
            return;
        }
        XWPFParagraph carrier = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
        CTPPr properties = carrier.getCTP().isSetPPr() ? carrier.getCTP().getPPr() : carrier.getCTP().addNewPPr();
        CTSpacing spacing = properties.isSetSpacing() ? properties.getSpacing() : properties.addNewSpacing();
        spacing.setBefore(BigInteger.ZERO);
        spacing.setAfter(BigInteger.ZERO);
        spacing.setLineRule(STLineSpacingRule.EXACT);
        spacing.setLine(BigInteger.valueOf(Math.max(2, toTwips(bottom - top))));
        if (!drawn.isEmpty()) {
            anchors.anchorInCell(carrier, drawn, new DocxDrawings.CellOrigin(left, top));
        }
        // A shape whose fragment draws nothing — a path painted only with a gradient — is still
        // reported as lost.
        for (int i = 0; i < shapes.size(); i++) {
            if (!own.get(i).shapes().isEmpty()) {
                anchoredInCells.add(shapes.get(i));
            }
        }
        java.util.Set<CellFragment> taken = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        taken.addAll(own);
        // A header repeated on later pages repeats its cell there in Word, the drawing anchored
        // in it with it: the page's copies of the drawing would draw it twice.
        int firstPage = composedCellBoxes.get(0).page();
        for (DocxLayoutMetrics.CellBox repeat : composedCellBoxes.subList(1, composedCellBoxes.size())) {
            List<CellFragment> copy = waitingIn(repeat);
            if (repeat.page() > firstPage && copy.size() == shapes.size()
                && DocxCellDrawings.opensWith(copy.stream().map(CellFragment::fragment).toList(), shapes)) {
                taken.addAll(copy);
            }
        }
        tableDrawings.removeIf(taken::contains);
    }

    /** The table's drawings still waiting that lie in one of its cells' boxes, in their order. */
    private List<CellFragment> waitingIn(DocxLayoutMetrics.CellBox box) {
        return tableDrawings.stream().filter(waiting -> box.holds(waiting.fragment())).toList();
    }

    /** Whether a cell holds nothing yet: no element, or one paragraph with nothing in it. */
    private static boolean holdsNothingYet(XWPFTableCell cell) {
        List<IBodyElement> elements = cell.getBodyElements();
        return elements.isEmpty()
               || elements.size() == 1 && elements.get(0) instanceof XWPFParagraph only && only.getRuns().isEmpty()
                  && only.getCTP().sizeOfHyperlinkArray() == 0;
    }

    /** Whether a box holds a line of text or a picture of its table: the centre of one stands inside it. */
    private static boolean framesText(com.demcha.compose.document.layout.PlacedFragment box,
                                      List<com.demcha.compose.document.layout.PlacedFragment> text) {
        for (com.demcha.compose.document.layout.PlacedFragment line : text) {
            double x = line.x() + line.width() / 2;
            double y = line.y() + line.height() / 2;
            if (line.pageIndex() == box.pageIndex()
                && x >= box.x() && x <= box.x() + box.width() && y >= box.y() && y <= box.y() + box.height()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Keeps a row at least as tall as the page made it.
     *
     * <p>A row is as tall as its tallest cell, and a cell's padding is written with it — but a
     * cell composed from a row or a stack carries space the cell does not: {@code MerchantInvoice}
     * centres each line's content in a 37.7pt row through its inner row's padding, and Word,
     * given only the content, closed every row to 31.3pt. A drawing the table anchors where the
     * page puts it then lands a row lower each row. At least, not exactly: where Word's text
     * needs more room than the page's, the row still grows to hold it.</p>
     *
     * <p>The height is written less the most any of the row's cells takes above and below its
     * content — top and bottom margins, and a horizontal border's width, with half of the rule
     * above the table and of the one below it more in the first row and the last
     * ({@link #outerRulesBeyondOne}). LibreOffice reads a row's height as its cells' content and
     * adds those to it: written whole, {@code PlatformInvoice}'s 36pt rows, padded 6.1pt above
     * and below, came out 48pt. Word does too, measured on a ruled table. Less them, a row is the
     * page's height in both.</p>
     */
    private void holdRowHeight(XWPFTable table, TableNode node, int rowIdx) {
        java.util.OptionalDouble height = layout.rowHeight(node, rowIdx);
        if (height.isPresent()) {
            XWPFTableRow row = table.getRow(rowIdx);
            holdRowAtLeast(row, height.getAsDouble(), outerRulesBeyondOne(table, rowIdx));
            fitBlankLinesToTheRow(row);
        }
    }

    /**
     * What a table's first or last row holds of its rules beyond the one {@link #verticalMargins}
     * counts, in twips.
     *
     * <p>Word gives a rule between two rows half to each — the lower row's, where the two differ
     * — and the rule above the table and the one below it whole to their row; it reads a row's
     * written height as its cells' content alone. So the first row carries its own rule and half
     * the next row's, and the last row half its own and its own again: written less one rule,
     * each was held half a rule taller than its content needs. Measured, a table of four rows ruled
     * at 0.75pt, held this way, stood 0.46pt taller in its first row and 0.36pt in its last.
     * {@code EditorialProposal}'s timeline and investment tables each stood that much taller in
     * Word, and the page under them that much low.</p>
     */
    private static long outerRulesBeyondOne(XWPFTable table, int rowIdx) {
        int rows = table.getNumberOfRows();
        boolean first = rowIdx == 0;
        boolean last = rowIdx == rows - 1;
        if (!first && !last) {
            return 0;
        }
        if (first && !last) {
            // Its own rule above the table counts; half the next row's rule is the rest.
            return Math.round(mostRule(table.getRow(1), true) / 2.0);
        }
        long most = 0;
        for (XWPFTableCell cell : table.getRow(rowIdx).getTableCells()) {
            long top = first ? rule(cell, true) : 0;
            most = Math.max(most, Math.round((top + rule(cell, false)) / 2.0));
        }
        return most;
    }

    /** The heaviest top (or bottom) rule of a row's cells, in twips. */
    private static long mostRule(XWPFTableRow row, boolean top) {
        long most = 0;
        for (XWPFTableCell cell : row.getTableCells()) {
            most = Math.max(most, rule(cell, top));
        }
        return most;
    }

    /** A cell's own top (or bottom) rule, in twips. */
    private static long rule(XWPFTableCell cell, boolean top) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr properties = cell.getCTTc().getTcPr();
        if (properties == null || !properties.isSetTcBorders()) {
            return 0;
        }
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders borders = properties.getTcBorders();
        return top ? borderTwips(borders.isSetTop() ? borders.getTop() : null)
                : borderTwips(borders.isSetBottom() ? borders.getBottom() : null);
    }

    /**
     * Cuts the line of a cell that holds no letters to the room its row leaves it.
     *
     * <p>A row that is only a rule takes its thickness from an empty cell's font: on the page
     * the rule's borders are drawn across that line, and in Word they stand outside it. The row
     * is written less its borders (see {@link #holdRowAtLeast}), but the line is not, and Word
     * grows the row to the line: {@code CobaltRota}'s two rules, a 2.3pt and a 1.95pt line under
     * a 0.9pt border, stood 0.9pt taller each, and the whole sheet under them 1.8pt low.
     * LibreOffice did the same. A line with no letters has nothing to cut into; one with
     * letters keeps its line.</p>
     *
     * <p>The row's height is written less the most any of its cells takes above and below its
     * content, the cells' content and margins filling the row's whole around it. So a
     * cell's room is that height, as much again as the row's written height leaves out, less
     * what the cell itself takes and its paragraph's space above and below — the page's row,
     * less the cell's own margins and border, and in a table's first or last row the half rule
     * more it holds ({@link #outerRulesBeyondOne}). Taken off the written height alone, a blank row's
     * line would lose its padding twice.</p>
     */
    private static void fitBlankLinesToTheRow(XWPFTableRow row) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrPr properties = row.getCtRow().getTrPr();
        if (properties == null || properties.sizeOfTrHeightArray() == 0) {
            return;
        }
        Long rowTwips = writtenTwips(properties.getTrHeightArray(0).getVal());
        if (rowTwips == null) {
            return;
        }
        long leftOut = 0;
        for (XWPFTableCell cell : row.getTableCells()) {
            leftOut = Math.max(leftOut, verticalMargins(cell));
        }
        for (XWPFTableCell cell : row.getTableCells()) {
            List<IBodyElement> content = cell.getBodyElements();
            if (content.size() != 1 || !(content.get(0) instanceof XWPFParagraph para) || !holdsNoLetters(para)) {
                continue;
            }
            CTPPr paragraph = para.getCTP().getPPr();
            CTSpacing spacing = paragraph != null && paragraph.isSetSpacing() ? paragraph.getSpacing() : null;
            if (spacing == null || !spacing.isSetLineRule() || spacing.getLineRule() != STLineSpacingRule.EXACT) {
                continue;
            }
            Long line = writtenTwips(spacing.getLine());
            long room = rowTwips + leftOut - verticalMargins(cell)
                        - twipsOf(spacing.isSetBefore() ? spacing.getBefore() : null)
                        - twipsOf(spacing.isSetAfter() ? spacing.getAfter() : null);
            if (line != null && line > room) {
                // A hairline, as the paragraph a table is opened with (openBefore).
                spacing.setLine(BigInteger.valueOf(Math.max(2, room)));
            }
        }
    }

    /**
     * Whether a paragraph draws nothing: no letters, no picture, shape or object in its runs,
     * and no border or shading of its own — a rule written as a paragraph's border stands on its
     * line (writeRule).
     */
    private static boolean holdsNoLetters(XWPFParagraph para) {
        if (!para.getText().isEmpty() || para.getCTP().sizeOfHyperlinkArray() > 0) {
            return false;
        }
        CTPPr properties = para.getCTP().getPPr();
        if (properties != null && (properties.isSetPBdr() || properties.isSetShd())) {
            return false;
        }
        for (XWPFRun run : para.getRuns()) {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR ctr = run.getCTR();
            if (ctr.sizeOfDrawingArray() > 0 || ctr.sizeOfPictArray() > 0 || ctr.sizeOfObjectArray() > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gives every cell of a row the row's smallest top and bottom margins, the rest of each
     * cell's padding written as space above its first paragraph and below its last.
     *
     * <p>Word and LibreOffice give every cell of a row the largest top margin of any cell in it,
     * and the largest bottom margin: measured, a row whose day cells were padded 5.5pt above and
     * 10.25pt below and whose label cell 0.75pt stood 60.3pt tall in both, where its tallest
     * cell's content and padding came to 46 — the label's content padded as the day cells were.
     * {@code CobaltRota}'s masthead row stood 20.8pt taller than the page's, and each staff row,
     * its name padded 2.55pt against its days' 1.35, 2.4pt taller. Space in a cell's paragraphs is
     * the cell's own.</p>
     *
     * <p>Some margins stay as they are, and the row's then comes to the largest of them: a cell
     * opening with a table has no paragraph above it to hold its padding, and a cell in a
     * vertical merge spans rows whose margins are evened apart. A table the page ends with
     * keeps the space moved below its cells' text: {@link #dropTheSpaceBelow} leaves a table
     * with a drawn bottom alone, and every table written here has one.</p>
     */
    private static void evenTheRowsMargins(XWPFTableRow row) {
        long top = Long.MAX_VALUE;
        long bottom = Long.MAX_VALUE;
        long keptTop = 0;
        long keptBottom = 0;
        for (XWPFTableCell cell : row.getTableCells()) {
            top = Math.min(top, cellMargin(cell, true));
            bottom = Math.min(bottom, cellMargin(cell, false));
            if (!canMoveItsPadding(cell, true)) {
                keptTop = Math.max(keptTop, cellMargin(cell, true));
            }
            if (!canMoveItsPadding(cell, false)) {
                keptBottom = Math.max(keptBottom, cellMargin(cell, false));
            }
        }
        if (top == Long.MAX_VALUE) {
            return;
        }
        top = Math.max(top, keptTop);
        bottom = Math.max(bottom, keptBottom);
        for (XWPFTableCell cell : row.getTableCells()) {
            List<IBodyElement> content = cell.getBodyElements();
            long extraTop = cellMargin(cell, true) - top;
            if (extraTop > 0 && canMoveItsPadding(cell, true)) {
                if (!content.isEmpty()) {
                    addSpacingTwips((XWPFParagraph) content.get(0), extraTop, 0);
                }
                setCellMarginTwips(cell, true, top);
            }
            long extraBottom = cellMargin(cell, false) - bottom;
            if (extraBottom > 0 && canMoveItsPadding(cell, false)) {
                if (!content.isEmpty()) {
                    addSpacingTwips((XWPFParagraph) content.get(content.size() - 1), 0, extraBottom);
                }
                setCellMarginTwips(cell, false, bottom);
            }
        }
    }

    /**
     * Whether a cell's top or bottom padding can be written in its paragraphs: it is in no
     * vertical merge, and a paragraph opens (or closes) it — a cell written here always holds
     * one, a table in it closed by a paragraph after it.
     */
    private static boolean canMoveItsPadding(XWPFTableCell cell, boolean top) {
        CTTcPr properties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : null;
        if (properties != null && properties.isSetVMerge()) {
            return false;
        }
        List<IBodyElement> content = cell.getBodyElements();
        return content.isEmpty() || content.get(top ? 0 : content.size() - 1) instanceof XWPFParagraph;
    }

    /** A cell's top or bottom margin as written, in twips; 0 where none is. */
    private static long cellMargin(XWPFTableCell cell, boolean top) {
        CTTcPr properties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : null;
        if (properties == null || !properties.isSetTcMar()) {
            return 0;
        }
        CTTcMar margins = properties.getTcMar();
        if (top ? !margins.isSetTop() : !margins.isSetBottom()) {
            return 0;
        }
        return twipsOf((top ? margins.getTop() : margins.getBottom()).getW());
    }

    private static void setCellMarginTwips(XWPFTableCell cell, boolean top, long twips) {
        CTTcPr properties = cellProperties(cell);
        CTTcMar margins = properties.isSetTcMar() ? properties.getTcMar() : properties.addNewTcMar();
        var side = top ? (margins.isSetTop() ? margins.getTop() : margins.addNewTop())
                : (margins.isSetBottom() ? margins.getBottom() : margins.addNewBottom());
        side.setType(STTblWidth.DXA);
        side.setW(BigInteger.valueOf(twips));
    }

    /** Adds space above and below a paragraph, in twips, to what it already has. */
    private static void addSpacingTwips(XWPFParagraph para, long before, long after) {
        CTPPr properties = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        CTSpacing spacing = properties.isSetSpacing() ? properties.getSpacing() : properties.addNewSpacing();
        if (before > 0) {
            spacing.setBefore(BigInteger.valueOf(twipsOf(spacing.isSetBefore() ? spacing.getBefore() : null) + before));
        }
        if (after > 0) {
            spacing.setAfter(BigInteger.valueOf(twipsOf(spacing.isSetAfter() ? spacing.getAfter() : null) + after));
        }
    }

    /**
     * Whether a row's tallest child on the page, margins included as the row is sized by them,
     * is one whose cell Word holds nothing in, taller by more than half a point than every child
     * it writes.
     */
    private boolean aDrawingMakesTheRow(RowNode node, XWPFTableRow row) {
        double drawn = 0;
        double written = 0;
        for (int i = 0; i < node.children().size() && i < row.getTableCells().size(); i++) {
            DocumentNode child = node.children().get(i);
            com.demcha.compose.document.layout.PlacedNode placed = layout.placement(child);
            if (placed == null) {
                continue;
            }
            double height = placed.placementHeight() + child.margin().top() + child.margin().bottom();
            if (holdsNothing(row.getCell(i))) {
                drawn = Math.max(drawn, height);
            } else {
                written = Math.max(written, height);
            }
        }
        return drawn > written + 0.5;
    }

    /**
     * Whether Word holds nothing in a cell that gives it height: no table, and no paragraph with
     * a run. A rule's paragraph counts as nothing; the row it stands in is held no taller than
     * the page makes it, which that child already sets. A spacer's paragraph carries an empty run
     * and counts: it is as tall as the spacer.
     */
    private static boolean holdsNothing(XWPFTableCell cell) {
        return cell.getTables().isEmpty()
               && cell.getParagraphs().stream().allMatch(paragraph -> paragraph.getCTP().sizeOfRArray() == 0
                                                                      && paragraph.getCTP().sizeOfHyperlinkArray() == 0);
    }

    /**
     * Writes a row at least a height, less what its cells take above and below their content
     * (see {@link #holdRowHeight}).
     *
     * @param row    the row, its cells' margins and borders written
     * @param points the height the page gives it
     */
    private static void holdRowAtLeast(XWPFTableRow row, double points) {
        holdRowAtLeast(row, points, 0);
    }

    /**
     * {@link #holdRowAtLeast(XWPFTableRow, double)}, less {@code beyond} twips more of rules the
     * row holds besides the one its cells' margins count ({@link #outerRulesBeyondOne}).
     */
    private static void holdRowAtLeast(XWPFTableRow row, double points, long beyond) {
        long margins = 0;
        for (XWPFTableCell cell : row.getTableCells()) {
            margins = Math.max(margins, verticalMargins(cell));
        }
        long lessMargins = Math.round(points * POINT_TO_TWIP) - margins;
        if (lessMargins <= 0) {
            return;
        }
        // Rules the row holds besides take it down to a twip at the least: written, the height
        // still cuts a blank line to the room the row leaves it (fitBlankLinesToTheRow).
        long twips = Math.max(1, lessMargins - beyond);
        row.setHeight((int) twips);
        row.setHeightRule(org.apache.poi.xwpf.usermodel.TableRowHeightRule.AT_LEAST);
    }

    /**
     * What the editors add to a row's written height for one cell, in twips: its top and bottom
     * margins, and the width of its heavier horizontal border. A panel's cell, its borders its
     * own, has the other one drawn outside the height too; writePanelPiece takes that off where
     * the panel's padding does not hold its top border.
     */
    private static long verticalMargins(XWPFTableCell cell) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr properties = cell.getCTTc().getTcPr();
        if (properties == null) {
            return 0;
        }
        long margins = 0;
        if (properties.isSetTcMar()) {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar mar = properties.getTcMar();
            margins = twipsOf(mar.isSetTop() ? mar.getTop() : null)
                      + twipsOf(mar.isSetBottom() ? mar.getBottom() : null);
        }
        if (properties.isSetTcBorders()) {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders borders = properties.getTcBorders();
            margins += Math.max(borderTwips(borders.isSetTop() ? borders.getTop() : null),
                    borderTwips(borders.isSetBottom() ? borders.getBottom() : null));
        }
        return margins;
    }

    /** A drawn border's width in twips; its size is in eighths of a point. */
    private static long borderTwips(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder border) {
        if (!drawn(border) || !border.isSetSz() || border.getSz() == null) {
            return 0;
        }
        return Math.round(border.getSz().doubleValue() * POINT_TO_TWIP / 8.0);
    }

    private static long twipsOf(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth width) {
        return width == null || !(width.getW() instanceof Number number) ? 0 : number.longValue();
    }

    private void writeTableRows(XWPFDocument document, TableNode node) throws Exception {
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
            boolean marginsEven = everyMarginEvens(physical);
            for (int i = 0; i < physical.size(); i++) {
                TableGrid.Placement placement = physical.get(i);
                XWPFTableCell cell = row.getCell(i);
                applySpans(cell, placement, rowIdx);
                // The covered positions of a merge take the paint too, so a merged
                // region reads as one cell rather than as a striped run of them.
                DocumentColor fill = resolveCellFill(node, placement);
                DocumentStroke stroke = resolveCellStroke(node, placement);
                applyCellPaint(cell, fill, stroke);
                int next = placement.row() + placement.rowSpan();
                DocumentStroke underneath = next < rowCount ? resolveCellStroke(node, cover[next][placement.column()]) : null;
                applyCellPadding(cell, clearOfTheRules(resolveCellPadding(node, placement), stroke,
                        placement.row() == 0, next >= rowCount, underneath));
                applyVerticalAnchor(cell, resolveCellAnchor(node, placement));
                if (placement.row() != rowIdx) {
                    // A covered position carries the merge marker and no content of its own.
                    continue;
                }
                cell.removeParagraph(0);
                if (marginsEven) {
                    cellsGivingTheirPadding.add(cell.getCTTc());
                }
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
            evenTheRowsMargins(row);
            // The first row's share of the rule under it is the next row's rule, written next.
            if (rowIdx > 0 || rowCount == 1) {
                holdRowHeight(table, node, rowIdx);
            }
        }
        if (rowCount > 1) {
            holdRowHeight(table, node, 0);
        }
        breakRowsWhereTheLayoutDoes(table, node);
        indentTable(table);
        carryDrawingsInRows(table, node);
    }

    /**
     * Offers each row's first paragraph to the drawings of the page the layout put the row on:
     * a page a long table fills holds no other paragraph to anchor them in.
     */
    private void carryDrawingsInRows(XWPFTable table, TableNode node) {
        for (int index = 0; index < table.getRows().size(); index++) {
            int page = layout.rowPage(node, index);
            if (page < 0) {
                continue;
            }
            for (XWPFTableCell cell : table.getRow(index).getTableCells()) {
                if (!cell.getParagraphs().isEmpty()) {
                    anchors.paragraphOn(page, cell.getParagraphs().get(0), false);
                    break;
                }
            }
        }
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
     * Word's defaults, which is to say with no fill and Word's own thin grid. Word owns both —
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
     * A table cell's padding as its margins, with room for the rules above and below it.
     *
     * <p>The page draws a cell's rules on its edges and steps its rows by their padding and
     * content alone. Word gives the rules room: a rule between two rows half to each, the rule
     * above the table and the one below it whole to their row. Measured, a row of 12.35pt text
     * and 7pt padding stepped 26.35pt unruled, 27.1 with 0.75pt rules and 27.85 with 1.5pt
     * ones, and a one-row table with 1.5pt rules stood 3pt taller, its text 1.5pt lower.
     * {@code EditorialProposal}'s timeline and investment tables grew that much row by row, and
     * their last block went onto a third page. Between two rows ruled differently, Word makes
     * room for the lower row's rule: measured, a 1.5pt header over 0.5pt rows stepped as 0.5pt
     * rules do, and a 1.5pt row under 0.5pt ones as 1.5pt rules do. So each rule comes off the
     * padding where Word puts it; in a table ruled alike throughout, with padding enough, every
     * row then stands where the page sets it. The sides keep their padding, as the columns'
     * widths are fixed. Padding thinner than its share gives what it has, and the row stands
     * that much taller.</p>
     *
     * @param padding    the cell's padding on the page
     * @param stroke     the cell's own rule
     * @param firstRow   whether the cell starts the table's first row, under the rule above the table
     * @param lastRow    whether the cell ends the table's last row, over the rule below it
     * @param underneath the rule of the cell under this one, whose width the edge between them
     *                   takes; ignored for the last row
     */
    private static DocumentInsets clearOfTheRules(DocumentInsets padding, DocumentStroke stroke,
                                                  boolean firstRow, boolean lastRow, DocumentStroke underneath) {
        double rule = strokeWidth(stroke);
        double below = lastRow ? rule : strokeWidth(underneath) / 2;
        return new DocumentInsets(Math.max(0, padding.top() - (firstRow ? rule : rule / 2)), padding.right(),
                Math.max(0, padding.bottom() - below), padding.left());
    }

    /**
     * The rule a cell resolves to, most specific wins, with the engine's own default underneath:
     * a table that states no rule is drawn with it on the page, and the table's own grid Word
     * would otherwise draw is thinner and gives its rows other heights.
     */
    private DocumentStroke resolveCellStroke(TableNode node, TableGrid.Placement placement) {
        DocumentStroke authored = resolveCellValue(node, placement, DocumentTableStyle::stroke);
        return authored != null ? authored : ENGINE_DEFAULT_CELL_STROKE;
    }

    /** What the engine rules a cell with when nothing states otherwise. */
    static final DocumentStroke ENGINE_DEFAULT_CELL_STROKE = DocumentStroke.of(DocumentColor.BLACK, 1.0);

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
            List<DocxLayoutMetrics.CellBox> outerBoxes = composedCellBoxes;
            currentCellWidth = usableWidthOf(cell, placement);
            tablesCells.add(cell.getCTTc());
            // A table nested in a composed cell has no rows of its own in the layout: its cells
            // stand in the outer table's cell.
            List<DocxLayoutMetrics.CellBox> boxes = layout.cellBoxes(node, placement.row(), placement.column());
            if (!boxes.isEmpty()) {
                composedCellBoxes = boxes;
            }
            try {
                writeCellBody(cell, source.content());
            } finally {
                currentCellWidth = previous;
                composedCellBoxes = outerBoxes;
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
                styleTheMark(para, textStyle);
                para = newCellLine(cell, rightToLeft, alignment, lineHeight);
                run = newCellRun(para, textStyle, rightToLeft);
                inRun = 0;
            } else if (i > 0) {
                // A joined "\n" is not a line break in Word; it renders as one line.
                run.addBreak();
            }
            run.setText(lines.get(i) == null ? "" : lines.get(i), inRun++);
        }
        styleTheMark(para, textStyle);
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
        // Nested in a cell, a row hanging left cannot move out of it: Word keeps a nested table
        // in its cell whatever its indent. Its first column gives the hang what it can, and its
        // cells' text takes the rest (see cellHang). SerifHeadline hangs each column's section
        // heading left by its dash, and in Word the titles stood that far right of the page's,
        // 17pt in the main column.
        double hang = currentCell != null ? Math.max(0, -insetLeft) : 0;
        RowGeometry geometry = applyRowGeometry(table, node, hang);
        boolean placedColumns = geometry.placed();
        hang -= geometry.hangTaken();
        XWPFTableRow row = table.getRow(0);
        // A row is laid out as one piece, never across a page break, so Word keeps it whole
        // too, where the layout placed it; see breakRowsWhereTheLayoutDoes.
        if (layout.placed(node)) {
            row.setCantSplitRow(true);
        }
        // Inside a painted panel the row is held as tall as the page made it: its padding and a
        // drawing standing in it are what make it taller than its text, as a panel's are, and
        // MerchantInvoice's due-date text stood against its card's top without it.
        boolean inAPanel = surfaceBehind != null && panelCell != null;
        com.demcha.compose.document.layout.PlacedNode placedRow = layout.placement(node);
        double rowOverhang = 0;
        // A row has no fill of its own: inside a panel it is a table nested in the panel's
        // cell, and a cell with no shading shows the panel's through it.
        for (int i = 0; i < node.children().size(); i++) {
            XWPFTableCell cell = row.getCell(i);
            cell.removeParagraph(0);
            DocumentNode child = node.children().get(i);
            // What the cell holds is sized to the cell, not to whatever surrounds the row.
            double previous = currentCellWidth;
            DocumentNode previousHeld = leftMarginInCell;
            currentCellWidth = usableWidthOf(cell, i, 1);
            leftMarginInCell = placedColumns ? child : null;
            cellHang = -hang;
            cellOverhang = 0;
            try {
                writeRowCellChild(cell, child);
            } finally {
                currentCellWidth = previous;
                leftMarginInCell = previousHeld;
                cellHang = 0;
            }
            rowOverhang = Math.max(rowOverhang, cellOverhang - roomBelowInItsRow(child, placedRow, node));
            cellOverhang = 0;
            applyRowVerticalAlign(cell, node.verticalAlign());
        }
        giveTheLastCellItsInk(table, node, placedColumns);
        // Anywhere, a row whose tallest child is one Word holds nothing of in its cell — a
        // picture drawn where the page puts it, a badge beside a heading — is only as tall there
        // as its text: WorkspaceInvoice's bill-to heading stood beside a 20pt badge, and the
        // address under it stood 10pt high in Word. Held to the page's height, whose margins are
        // written around the table. A drawing no taller than its neighbours' text — a timeline's rail
        // beside its entry — changes nothing and is left to Word.
        if (placedRow != null && placedRow.startPage() == placedRow.endPage()
            && (inAPanel || aDrawingMakesTheRow(node, row))) {
            holdRowAtLeast(row, placedRow.placementHeight() - node.padding().top() - node.padding().bottom());
        }
        indentTable(table);
        // A cell's last line hanging past the row's foot makes Word's row that much taller, and
        // takes as much of the gap under it, as text hanging below a band does (writeLinePair):
        // TimelineMinimal's last contact line, held to its icon, stood 1.8pt below the row, and
        // the page under the header with it.
        // A card's border below another cell (borderBelow) makes the row taller too: Word's row
        // is as tall as the taller of the two, so only what passes the border is added to it.
        if (rowOverhang - borderBelow > 0) {
            hangingBelow = Math.max(hangingBelow, rowOverhang - borderBelow);
        }
    }

    /**
     * How much shorter a row's child is than the row's content box on the page, in points: room a
     * line hanging below the child has before it makes Word's row taller. {@code +∞} when the
     * layout does not say, so nothing is taken for it.
     */
    private double roomBelowInItsRow(DocumentNode child, com.demcha.compose.document.layout.PlacedNode placedRow,
                                     RowNode row) {
        com.demcha.compose.document.layout.PlacedNode placed = layout.placement(child);
        if (placedRow == null || placed == null || placedRow.startPage() != placedRow.endPage()
            || placed.startPage() != placed.endPage()) {
            return Double.POSITIVE_INFINITY;
        }
        // The row's room less the child's height, wherever the row aligns it: Word grows a row
        // only when a cell holds more than the row, however the cell sets its content in it.
        return Math.max(0, placedRow.placementHeight() - row.padding().top() - row.padding().bottom()
                           - placed.placementHeight() - child.margin().top() - child.margin().bottom());
    }

    /**
     * Widens a row's last cell by as far as the shape ending its line is drawn past it.
     *
     * <p>The page draws a shape's ink past its box — half a ring's stroke — and past the cell
     * the box ends at. Word cuts a cell's content at the cell's edge, the transparent edge
     * round a shape's picture with it: {@code MidnightNavy}'s fifth language dot, a ring at its
     * column's right edge, lost the right of its stroke. Its column, and the table, are made as
     * much wider; the row's other columns keep their widths. Only a paragraph of one line set
     * from the left is measured: its line ends where the page ends it, wider by the room its
     * shapes take where no text beside them gives it back (see {@link #giveBackWidth}).</p>
     *
     * @param placedColumns whether the row's columns are the layout's, its children's margins
     *                      already outside them
     */
    private void giveTheLastCellItsInk(XWPFTable table, RowNode node, boolean placedColumns) {
        int last = node.children().size() - 1;
        if (last < 0 || !(node.children().get(last) instanceof ParagraphNode paragraph)
            || paragraph.align() == TextAlign.CENTER || paragraph.align() == TextAlign.RIGHT
            || ParagraphDirection.resolve(paragraph) == TextDirection.RTL
            || layout.lineCount(paragraph) != 1 || paragraph.inlineRuns().isEmpty()
            || !(paragraph.inlineRuns().get(paragraph.inlineRuns().size() - 1) instanceof InlineShapeRun)) {
            return;
        }
        java.util.Optional<com.demcha.compose.document.layout.payloads.ParagraphLine> line = layout.firstLine(paragraph);
        XWPFTableCell cell = table.getRow(0).getCell(last);
        double room = usableWidthOf(cell, last, 1)
                      - paragraph.padding().left() - paragraph.padding().right()
                      - (placedColumns ? 0 : paragraph.margin().left() + paragraph.margin().right());
        if (line.isEmpty() || !Double.isFinite(room)) {
            return;
        }
        double past = line.get().width() + roomNotGivenBack(paragraph.inlineRuns()) - room;
        if (!(past > 0)) {
            return;
        }
        long more = (long) Math.ceil(past * POINT_TO_TWIP);
        var column = table.getCTTbl().getTblGrid().getGridColArray(last);
        Long columnTwips = writtenTwips(column.getW());
        if (columnTwips == null) {
            return;
        }
        column.setW(BigInteger.valueOf(columnTwips + more));
        CTTcPr properties = cellProperties(cell);
        if (properties.isSetTcW() && writtenTwips(properties.getTcW().getW()) != null) {
            properties.getTcW().setW(BigInteger.valueOf(writtenTwips(properties.getTcW().getW()) + more));
        }
        CTTblPr tableProperties = table.getCTTbl().getTblPr();
        if (tableProperties != null && tableProperties.isSetTblW() && writtenTwips(tableProperties.getTblW().getW()) != null) {
            tableProperties.getTblW().setW(BigInteger.valueOf(writtenTwips(tableProperties.getTblW().getW()) + more));
        }
    }

    /**
     * The room a paragraph's shapes take past their boxes that no text beside them gives back, in
     * points: a shape opening its line, the side of one standing against another, the end of
     * one closing the line (see {@link #giveBackWidth}).
     */
    private double roomNotGivenBack(List<InlineRun> runs) {
        double room = 0;
        for (int index = 0; index < runs.size(); index++) {
            if (!(runs.get(index) instanceof InlineShapeRun shape)) {
                continue;
            }
            if (index == 0 || !givesBackWidth(runs.get(index - 1))) {
                room += DocxShapePictures.widthBeforeItsBox(shape);
            }
            if (index == runs.size() - 1 || !givesBackWidth(runs.get(index + 1))) {
                room += DocxShapePictures.widthAfterItsBox(shape);
            }
        }
        return room;
    }

    /** Whether a run is text that sets its letters closer for a shape beside it (giveBackWidth). */
    private boolean givesBackWidth(InlineRun run) {
        InlineTextRun text = textOf(run);
        return text != null && text.text() != null && !text.text().isEmpty()
               && !LINE_BREAK.matcher(text.text()).find();
    }

    /**
     * Whether a layer of a stack holds only what is drawn where the page puts it, and may be
     * left out of the stack's columns (see {@link DocxLayerColumns#of}): drawing, or a picture
     * alone in a container that places it, in a layer that paints nothing of its own and holds
     * no badge's initials, which a band would write a line for.
     */
    private boolean drawnOverTheColumns(DocumentNode layer) {
        if ((layer instanceof SectionNode || layer instanceof ContainerNode) && !paintOf(layer).isEmpty()
            || holdsATextBadge(layer)) {
            return false;
        }
        return onlyDrawn(layer) || aPictureAlone(layer) != null;
    }

    private boolean holdsATextBadge(DocumentNode node) {
        return node instanceof ShapeContainerNode badge && textBadgeParagraph(badge) != null
               || node.children().stream().anyMatch(this::holdsATextBadge);
    }

    /**
     * Draws the layers of a stack written as columns that belong to no column — a card's mark,
     * the rule between two cards — where the page puts them, writing nothing into the flow.
     * Whatever space a layer would owe around itself is not the flow's: it is put back.
     */
    private void drawLayersOverTheColumns(XWPFDocument document, List<DocumentNode> layers) throws Exception {
        if (layers.isEmpty()) {
            return;
        }
        double carried = carriedSpacingBefore;
        double owed = pendingSpacingAfter;
        double hanging = hangingBelow;
        double pull = pullBelow;
        double outerLeft = insetLeft;
        double outerRight = insetRight;
        overlayDepth++;
        try {
            for (DocumentNode layer : layers) {
                ImageNode picture = aPictureAlone(layer);
                if (picture != null) {
                    picturesDrawnBeside.add(picture);
                }
                writeNode(document, layer);
            }
        } finally {
            overlayDepth--;
            carriedSpacingBefore = carried;
            pendingSpacingAfter = owed;
            hangingBelow = hanging;
            pullBelow = pull;
            insetLeft = outerLeft;
            insetRight = outerRight;
        }
    }

    /**
     * The picture a layer holds alone — a card's mark in a sleeve that places it — or
     * {@code null} when it holds anything else, or paints anything of its own.
     */
    private ImageNode aPictureAlone(DocumentNode layer) {
        if (Double.isNaN(canvasHeight) || layer instanceof ImageNode) {
            return null;
        }
        DocumentNode node = layer;
        while (node instanceof SectionNode || node instanceof ContainerNode) {
            if (!paintOf(node).isEmpty() || node.children().size() != 1) {
                return null;
            }
            node = node.children().get(0);
        }
        if (!(node instanceof ImageNode picture) || picture.anchor() != null && !picture.anchor().isBlank()
            || picture.fitMode() == DocumentImageFitMode.COVER) {
            return null;
        }
        com.demcha.compose.document.layout.PlacedNode placed = layout.placement(picture);
        return placed != null && placed.startPage() == placed.endPage() ? picture : null;
    }

    /**
     * Writes a layer stack whose layers are side-by-side columns as a one-row table.
     *
     * <p>Each column is a cell as wide as its band, and holds the content of its layers — the
     * layers' side padding is the band, so it is not written again; the first layer's top
     * edge and the last one's bottom edge are space in the cell. A gap between two bands is
     * the next cell's left margin, and whatever the last band leaves on the right is its
     * cell's right margin. The bands are measured from the stack's content box, so the table
     * starts inside the stack's own left margin and padding. The row may break across pages,
     * as a column longer than a page does. See {@link DocxLayerColumns}.</p>
     */
    private void writeLayerColumns(XWPFDocument document,
                                   com.demcha.compose.document.node.LayerStackNode stack,
                                   DocxLayerColumns.Plan plan) throws Exception {
        owePendingSpacingAfter(stack.margin().top() + stack.padding().top());
        List<DocxLayerColumns.Column> columns = plan.columns();
        XWPFTable table = newTable(document, 1, columns.size());
        hideTableGrid(table);
        setTableWidth(table, plan.width());
        List<CellColumn> cells = new ArrayList<>(columns.size());
        for (int index = 0; index < columns.size(); index++) {
            DocxLayerColumns.Column column = columns.get(index);
            double start = index == 0 ? 0.0 : columns.get(index - 1).right();
            double end = index == columns.size() - 1 ? plan.width() : column.right();
            cells.add(new CellColumn(end - start, column.left() - start, end - column.right()));
        }
        writeRowColumns(table, cells);
        standIns.addAll(plan.standIns());
        moves.add(plan.moves());
        try {
            XWPFTableRow row = table.getRow(0);
            for (int index = 0; index < columns.size(); index++) {
                XWPFTableCell cell = row.getCell(index);
                cell.removeParagraph(0);
                List<DocumentNode> layers = columns.get(index).layers();
                double previous = currentCellWidth;
                currentCellWidth = usableWidthOf(cell, index, 1);
                try {
                    writeInCell(cell, () -> {
                        for (int layer = 0; layer < layers.size(); layer++) {
                            DocumentNode node = layers.get(layer);
                            double owedAbove = pendingSpacingAfter;
                            long blocksBefore = blocksWritten;
                            if (layer == 0) {
                                carriedSpacingBefore += node.margin().top() + node.padding().top();
                            } else {
                                // What the layers above still owe below themselves is space
                                // the page does not have: the gap to this one is the page's.
                                pendingSpacingAfter = 0;
                                pullBelow = 0;
                                resumeSpacing = plan.resume(node);
                            }
                            writeChildren(document, node.children(), spacingOf(node));
                            if (layer > 0 && blocksWritten == blocksBefore && holdsMovedContent(node)) {
                                // A layer that wrote nothing — its content all written in an
                                // earlier layer's stand-ins — owes nothing of its own: what the
                                // layers above owed is still what the cell ends with.
                                pendingSpacingAfter = owedAbove;
                                resumeSpacing = Double.NaN;
                                continue;
                            }
                            if (layer == layers.size() - 1) {
                                owePendingSpacingAfter(node.margin().bottom() + node.padding().bottom());
                            }
                        }
                    });
                } finally {
                    currentCellWidth = previous;
                    resumeSpacing = Double.NaN;
                }
                if (cell.getParagraphs().isEmpty()) {
                    cell.addParagraph();
                }
            }
        } finally {
            standIns.removeAll(plan.standIns());
            moves.remove(plan.moves());
        }
        // After the columns, as the stack lays them over the columns: a card's mark stands over
        // whatever its card draws.
        drawLayersOverTheColumns(document, plan.drawn());
        com.demcha.compose.document.layout.PlacedNode placed = layout.placement(stack);
        if (!plan.drawn().isEmpty() && placed != null && placed.startPage() == placed.endPage()) {
            // What is drawn over the columns — a rule as tall as the band — takes no room in
            // them, and was what held the row to the page's height: a divider SlateOrange's
            // header row lost, and the page under it rose 22pt.
            holdRowAtLeast(table.getRow(0), placed.placementHeight() - stack.padding().top() - stack.padding().bottom());
        }
        double outerLeft = insetLeft;
        insetLeft += stack.margin().left() + stack.padding().left();
        try {
            indentTable(table);
        } finally {
            insetLeft = outerLeft;
        }
        owePendingSpacingAfter(stack.margin().bottom() + stack.padding().bottom());
    }

    /**
     * Writes a stack of overlapping layers one after the other, as the page places their
     * content (see {@link DocxLayerColumns#band}).
     *
     * <p>The space above the first block written is the page's distance from the stack's
     * top, together with whatever was owed above the stack; each later layer resumes the
     * page's distance below the blocks above it; and the space owed below is the page's
     * distance from the last block to the stack's bottom. Each replaces what the containers
     * in between would have carried, which on the page is space the other layers already
     * take. A later layer that follows only layers that wrote nothing keeps the space the
     * stack opened with. Drawing among the layers is drawn as shapes, as in any overlay, and
     * writes no paragraph.</p>
     */
    private void writeOverlayBand(XWPFDocument document, DocumentNode stack, DocxLayerColumns.Band band)
            throws Exception {
        // A band inside a band that has written nothing yet: the outer band measured its space
        // to the first block written, which is this one's first too, so that space stands.
        if (Double.isNaN(resumeSpacing)) {
            double above = carriedSpacingBefore + pendingSpacingAfter + stack.margin().top() + band.above();
            // Text hanging below the block before — a band's last line past its foot, a row's
            // icon line — takes its place out of that space, as out of the gap above a paragraph
            // (newBodyParagraph); the band's first block forgets it (resumeHere). ConsultingInvoice's
            // address runs 2.3pt past its band, and the contact bands under it stood 2.9pt low.
            // A band the layout starts on a new page leaves the hang on the page above, as a
            // paragraph or a table there does.
            double hang = currentCell == null && overlayDepth == 0 && startsAPageOfItsOwn(stack) ? 0 : hangingBelow;
            resumeSpacing = above - Math.min(Math.max(0, above), hang);
        }
        carriedSpacingBefore = 0;
        pendingSpacingAfter = 0;
        standIns.addAll(band.standIns());
        double outerLeft = insetLeft;
        double outerRight = insetRight;
        insetLeft += stack.margin().left() + stack.padding().left();
        insetRight += stack.margin().right() + stack.padding().right();
        overlayDepth++;
        bandDepth++;
        try {
            List<DocumentNode> layers = band.layers();
            for (int index = 0; index < layers.size(); index++) {
                double resume = band.resume(layers.get(index));
                if (index > 0 && !Double.isNaN(resume) && Double.isNaN(resumeSpacing)) {
                    pendingSpacingAfter = 0;
                    carriedSpacingBefore = 0;
                    pullBelow = 0;
                    resumeSpacing = resume;
                }
                double bandLeft = insetLeft;
                double bandRight = insetRight;
                placeAcross(stack, layers.get(index));
                try {
                    writeNode(document, layers.get(index));
                } finally {
                    insetLeft = bandLeft;
                    insetRight = bandRight;
                }
            }
        } finally {
            overlayDepth--;
            bandDepth--;
            insetLeft = outerLeft;
            insetRight = outerRight;
            standIns.removeAll(band.standIns());
        }
        // Nothing was written after all: the space the stack opened with is still owed.
        double unwritten = Double.isNaN(resumeSpacing) ? 0 : resumeSpacing;
        resumeSpacing = Double.NaN;
        carriedSpacingBefore = 0;
        // Text running past the band's foot hangs below it and takes that much of the gap under
        // it, as a line pair's does (writeLinePair): LumaStudioInvoice's "INVOICE" is set in a
        // line 13pt deeper than its title block, and the invoice's details under it, and
        // everything after them, stood that much lower in Word.
        // The band measures its space below from its lowest text, a nested band's included: what
        // a band inside it left hanging is already in that number, and is not taken twice.
        double below = unwritten + band.below() + stack.margin().bottom();
        // The band measures that from its boxes on the page, a layer's pull already in it.
        pullBelow = 0;
        pendingSpacingAfter = Math.max(0, below);
        hangingBelow = below < 0 ? -below : 0;
    }

    /**
     * Holds a layer of an overlay — a band, or a shape container — in across to where the page
     * places it inside the overlay.
     *
     * <p>The layers of a band are written one after the other, and each ran from the stack's left
     * edge to its right: initials the page centres in a badge's ring stood at the left of the
     * column, and a section's title set beside its badge started under it. The layer's box is the
     * layout's, so the insets put it there; the paragraph's own alignment then sets its text in
     * that box as the page does. A box a line of text fills exactly is widened by a few points on
     * the side its text does not lean on, since an editor sets text a little wider than the page
     * and a word that no longer fits would break across lines.</p>
     */
    private void placeAcross(DocumentNode stack, DocumentNode layer) {
        com.demcha.compose.document.layout.PlacedNode box = layout.placement(stack);
        com.demcha.compose.document.layout.PlacedNode placed = layout.placement(layer);
        if (box == null || placed == null) {
            return;
        }
        double contentLeft = box.placementX() + stack.padding().left();
        double contentRight = box.placementX() + box.placementWidth() - stack.padding().right();
        holdIn(layer, placed, contentLeft, contentRight);
    }

    /**
     * Holds a block in across, from the edges it is written between to where the layout placed
     * it, with room to spare for a paragraph on the side its text does not lean on.
     *
     * @param layer        the block
     * @param placed       where the layout placed it
     * @param contentLeft  the page position of the left edge it is written from
     * @param contentRight the page position of the right edge it is written to
     */
    private void holdIn(DocumentNode layer, com.demcha.compose.document.layout.PlacedNode placed,
                        double contentLeft, double contentRight) {
        double left = placed.placementX() - layer.margin().left() - contentLeft;
        double right = contentRight - (placed.placementX() + placed.placementWidth() + layer.margin().right());
        if (!(left > 0.5) && !(right > 0.5)) {
            return;
        }
        // Only a paragraph's own box needs room to spare: a container's paragraphs take theirs
        // inside it, and spare room taken at every level would add up across nested stacks.
        double slack = layer instanceof ParagraphNode ? Math.max(2, placed.placementWidth() * 0.05) : 0;
        TextAlign align = layer instanceof ParagraphNode paragraph ? paragraph.align() : TextAlign.LEFT;
        if (align == TextAlign.CENTER) {
            left -= slack / 2;
            right -= slack / 2;
        } else if (align == TextAlign.RIGHT) {
            left -= slack;
        } else {
            right -= slack;
        }
        insetLeft += Math.max(0, left);
        insetRight += Math.max(0, right);
    }

    /**
     * Whether every leaf under a node is drawn rather than written: drawing, or a glyph drawn
     * over the badge holding it (see {@link #drawnOverItsBadge}).
     */
    private boolean onlyDrawn(DocumentNode node) {
        if (node.children().isEmpty()) {
            return isDrawing(node);
        }
        if (node instanceof ShapeContainerNode badge && textBadgeParagraph(badge) != null) {
            // Its initials are drawn in it, not written (see textBadgeParagraph).
            return true;
        }
        for (DocumentNode child : node.children()) {
            boolean drawn = child instanceof ImageNode image
                    ? drawnPicture(image, node)
                    : onlyDrawn(child);
            if (!drawn) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a container writes one of its layers at most, the others being drawn: a title
     * beside its badge or its icon, an icon picture drawn beside its label
     * ({@link #drawnBesideItsText}). Its layers stand over one another only as the text and
     * what is drawn round it; two written layers side by side would come out one under the
     * other as a band.
     */
    private boolean writesOneLayer(DocumentNode node) {
        int written = 0;
        for (DocumentNode layer : node.children()) {
            boolean drawn = layer instanceof ImageNode image
                    ? drawnPicture(image, node)
                    : onlyDrawn(layer);
            if (!drawn) {
                written++;
            }
        }
        return written <= 1;
    }

    /**
     * Which leaves under a node are drawn rather than written: drawing, and the pictures drawn
     * where the page puts them (see {@link #drawnPicture}) — neither is content a band's text is
     * measured from. A badge's initials are measured from: the band writes a line in their place
     * (see writeTextBadge).
     */
    private java.util.function.Predicate<DocumentNode> drawnIn(DocumentNode node) {
        java.util.Set<DocumentNode> pictures = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        collectDrawnPictures(node, pictures);
        return leaf -> isDrawing(leaf) || pictures.contains(leaf);
    }

    private void collectDrawnPictures(DocumentNode node, java.util.Set<DocumentNode> pictures) {
        for (DocumentNode child : node.children()) {
            if (child instanceof ImageNode image && drawnPicture(image, node)) {
                pictures.add(child);
            }
            collectDrawnPictures(child, pictures);
        }
    }

    /** Whether a node is a paragraph of text or holds one. */
    private static boolean holdsText(DocumentNode node) {
        return node instanceof ParagraphNode || node.children().stream().anyMatch(DocxSemanticBackend::holdsText);
    }

    /** Whether a node is a badge holding a glyph drawn over it (see {@link #drawnOverItsBadge}). */
    private boolean holdsItsDrawnGlyph(DocumentNode node) {
        return node instanceof ShapeContainerNode badge
               && badge.children().stream().anyMatch(layer -> layer instanceof ImageNode glyph
                                                              && drawnOverItsBadge(glyph, badge));
    }

    /**
     * Whether a picture is drawn where the page puts it rather than written in the flow: a
     * glyph over its badge ({@link #drawnOverItsBadge}), or an icon beside its text
     * ({@link #drawnBesideItsText}).
     */
    private boolean drawnPicture(ImageNode image, DocumentNode parent) {
        return parent instanceof ShapeContainerNode badge && drawnOverItsBadge(image, badge)
               || drawnBesideItsText(image, parent);
    }

    /**
     * Whether a picture is an icon laid beside the text it labels, drawn where the page puts
     * it: a layer of an unpainted shape container or a layer stack, standing on one page clear
     * of every other layer across, beside a layer that is written.
     *
     * <p>Written as a paragraph of its own, the icon took a line above its text:
     * {@code NorthlineProposal}'s "project at a glance" facts — an icon left of a label over a
     * value — each came out an icon's line taller in Word, 22pt a fact. Drawn, the container
     * writes its text alone, as a band placing it where the page does (see
     * {@link #writesOneLayer}).</p>
     */
    private boolean drawnBesideItsText(ImageNode image, DocumentNode parent) {
        if (Double.isNaN(canvasHeight)
            || !(parent instanceof ShapeContainerNode || parent instanceof com.demcha.compose.document.node.LayerStackNode)
            || parent instanceof ShapeContainerNode container
               && (container.fillColor() != null || container.stroke() != null
                   || container.clipPolicy() == com.demcha.compose.document.style.ClipPolicy.CLIP_PATH
                   || container.transform() != null && !container.transform().isIdentity())
            || image.anchor() != null && !image.anchor().isBlank()
            || image.fitMode() == DocumentImageFitMode.COVER
            || parent.children().size() < 2) {
            return false;
        }
        com.demcha.compose.document.layout.PlacedNode icon = layout.placement(image);
        if (icon == null || icon.startPage() != icon.endPage()) {
            return false;
        }
        boolean besideText = false;
        double edge = 0.5;
        for (DocumentNode layer : parent.children()) {
            if (layer == image) {
                continue;
            }
            com.demcha.compose.document.layout.PlacedNode placed = layout.placement(layer);
            if (placed == null || placed.startPage() != icon.startPage() || placed.endPage() != icon.startPage()) {
                return false;
            }
            boolean clear = placed.placementX() >= icon.placementX() + icon.placementWidth() - edge
                            || placed.placementX() + placed.placementWidth() <= icon.placementX() + edge;
            if (!clear) {
                return false;
            }
            // An icon, at most twice as tall as the text it labels: a photo beside a line of
            // text is what the flow is written round, and stays in it; so is a logo beside
            // another picture, a barcode or a spacer, which label nothing.
            if (holdsText(layer)) {
                besideText |= icon.placementHeight() <= 2 * placed.placementHeight() + edge;
            }
        }
        return besideText;
    }

    /** Whether every leaf under a node is drawing, so that nothing of it is written. */
    private static boolean onlyDrawing(DocumentNode node) {
        if (node.children().isEmpty()) {
            return isDrawing(node);
        }
        for (DocumentNode child : node.children()) {
            if (!onlyDrawing(child)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Keeps the place of a block this export writes no paragraph for, as the space it takes on
     * the page.
     *
     * <p>A portrait drawn as paths, a badge's ring, a decorative shape: at most a shape anchored
     * to the page, which takes no room in the text, and the room it took on the page was not
     * kept, so everything under it moved up by its height —
     * a sidebar opening with a 98pt portrait started its contact lines 98pt high. Its placed
     * height and its margins are owed as space below whatever came before, where the next
     * block written takes them.</p>
     *
     * @return true when the layout placed the node, so its space is known and owed
     */
    private boolean holdTheSpaceOf(DocumentNode node) {
        com.demcha.compose.document.layout.PlacedNode placed = layout.placement(node);
        if (placed == null) {
            return false;
        }
        // It stands where a block would: a column layer it opens resumes above it, the
        // container edges waiting above it are above it, and the containers around it have had
        // their top edge taken, as by any block.
        resumeHere();
        owePendingSpacingAfter(carriedSpacingBefore);
        carriedSpacingBefore = 0;
        owePendingSpacingAfter(node.margin().top() + placed.placementHeight() + node.margin().bottom());
        blocksWritten++;
        return true;
    }

    /** Whether a node is drawing, which in an overlay this export does not write. */
    private static boolean isDrawing(DocumentNode node) {
        return node instanceof com.demcha.compose.document.node.ShapeNode
               || node instanceof com.demcha.compose.document.node.LineNode
               || node instanceof com.demcha.compose.document.node.EllipseNode
               || node instanceof com.demcha.compose.document.node.PathNode
               || node instanceof com.demcha.compose.document.node.PolygonNode;
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
     * gets the content width, unless fixed columns add up to less and leave the rest of
     * the row empty ({@link #holdTheLastFixedColumn}): then it is as wide as its columns.
     * Left at POI's size-to-content default the pair collapses around its text instead.</p>
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
     *
     * @param hang how far the row hangs left out of the cell holding it, in points; the first
     *             column gives up what it can of it ({@link #takeHang})
     * @return whether the columns are the layout's, each cell's text starting where its child
     *         does — past the child's left margin — and how much of the hang they took
     */
    private RowGeometry applyRowGeometry(XWPFTable table, RowNode node, double hang) {
        double[] starts = layout.rowChildStarts(node);
        if (starts != null) {
            // The layout placed each child, so every way a row can divide — the two that
            // measure their children included — is already answered.
            List<CellColumn> columns = new ArrayList<>(withRowEditorSlack(node, placedColumns(node, starts)));
            holdTheLastFixedColumn(node, columns);
            double taken = takeHang(node, columns, hang);
            setTableWidth(table, Math.min(starts[0], widthOf(columns) + taken) - taken);
            writeRowColumns(table, columns);
            return new RowGeometry(true, taken);
        }

        double available = availableWidth();
        if (!Double.isFinite(available) || available <= 0) {
            return new RowGeometry(false, 0);
        }
        double[] slots = resolveRowSlots(node, available);
        if (slots == null) {
            setTableWidth(table, available);
            return new RowGeometry(false, 0);
        }
        List<CellColumn> columns = new ArrayList<>(statedColumns(node, slots));
        double taken = takeHang(node, columns, hang);
        setTableWidth(table, Math.min(available, widthOf(columns) + taken) - taken);
        writeRowColumns(table, columns);
        return new RowGeometry(false, taken);
    }

    /**
     * Holds a row's last column to its fixed width, where the row states one, rather than to
     * the row's edge.
     *
     * <p>Fixed columns that add up to less than the row leave the rest of it empty, and the
     * last column's text wraps at its own width: {@code EditorialProposal}'s deliverables, a
     * column of 194pt in a band of 535, wrapped "Responsive design for desktop, tablet &amp;
     * mobile" onto two lines. Run to the row's edge, the cell was 272pt wide, and Word set the
     * item on one line and every item under it a line higher.</p>
     */
    private static void holdTheLastFixedColumn(RowNode node, List<CellColumn> columns) {
        List<DocumentRowColumn> specs = node.columns();
        if (columns.isEmpty() || specs.size() != columns.size()
            || specs.get(specs.size() - 1).type() != DocumentRowColumn.Type.FIXED) {
            return;
        }
        int last = columns.size() - 1;
        CellColumn column = columns.get(last);
        // The column starts where its child does, past the child's left margin; its slot
        // starts that margin earlier.
        double margin = node.children().size() == columns.size() ? node.children().get(last).margin().left() : 0;
        double held = column.leading() - margin + specs.get(last).value() + column.trailing();
        if (held < column.width() - 0.01) {
            columns.set(last, new CellColumn(held, column.leading(), column.trailing()));
        }
    }

    /** The width a row's columns take together, in points. */
    private static double widthOf(List<CellColumn> columns) {
        double width = 0;
        for (CellColumn column : columns) {
            width += column.width();
        }
        return width;
    }

    /**
     * How a row's carrier was divided.
     *
     * @param placed    whether the columns are the layout's (see {@link #applyRowGeometry})
     * @param hangTaken how much of the row's hang its first column gave up, in points
     */
    private record RowGeometry(boolean placed, double hangTaken) {
    }

    /**
     * Takes a row's hang out of its first column, so the columns after it start where the page
     * starts them while the table stays in its cell.
     *
     * <p>The empty space before the first cell's text goes first. Its text box goes only when
     * the cell writes nothing — a heading's dash, drawn where the page draws it, not a rule
     * written across its cell — and never below {@value #MIN_COLUMN_POINTS}pt: a cell with text
     * would wrap it. What the column cannot give the cells' text takes out of its own indent
     * ({@link #cellHang}); Word draws no text past a cell's left edge, so the column gives what
     * it can first.</p>
     *
     * @return the points taken, the table's width shrinking by as much
     */
    private double takeHang(RowNode node, List<CellColumn> columns, double hang) {
        if (!(hang > 0.01) || columns.isEmpty() || node.children().isEmpty()) {
            return 0;
        }
        CellColumn first = columns.get(0);
        double fromLeading = Math.min(hang, first.leading());
        DocumentNode lead = node.children().get(0);
        double spare = onlyDrawn(lead) && !holdsARule(lead, overlayDepth, oneLayerDepth)
                ? Math.max(0, textBoxOf(first) - MIN_COLUMN_POINTS)
                : 0;
        double fromBox = Math.min(hang - fromLeading, spare);
        double taken = fromLeading + fromBox;
        columns.set(0, new CellColumn(first.width() - taken, first.leading() - fromLeading, first.trailing()));
        return taken;
    }

    /**
     * Whether writing a node writes a rule: a line in the flow is a paragraph whose border spans
     * its cell ({@link #ruleOf}), not a drawing where the page puts it, and a column narrowed
     * under it would shorten it.
     *
     * @param overlays         the overlays the node is written inside
     * @param oneLayerOverlays how many of them are stacks of one layer
     */
    private static boolean holdsARule(DocumentNode node, int overlays, int oneLayerOverlays) {
        if (node.children().isEmpty()) {
            return inTheFlow(node, overlays, oneLayerOverlays) && DocxRules.of(node) != null;
        }
        int inside = overlays + (isOverlay(node) ? 1 : 0);
        int insideOneLayer = oneLayerOverlays + (isOneLayer(node) ? 1 : 0);
        for (DocumentNode child : node.children()) {
            if (holdsARule(child, inside, insideOneLayer)) {
                return true;
            }
        }
        return false;
    }

    /** The narrowest a column is left when it gives up its width to a hang. */
    private static final double MIN_COLUMN_POINTS = 1;

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
     *
     * <p>A cell left with nothing in it at all — its content drawn where the page draws it, as
     * a skill's meter beside its label — has that paragraph a hairline tall: left to Word's
     * own line it was a line of the document's font, and {@code MidnightNavy}'s skills each
     * stood 2.1pt taller than the page's.</p>
     */
    private void writeCellBody(XWPFTableCell cell, DocumentNode child) throws Exception {
        writeCellNode(cell, child);
        if (cell.getParagraphs().isEmpty()) {
            boolean empty = cell.getBodyElements().isEmpty();
            XWPFParagraph closing = cell.addParagraph();
            if (empty) {
                holdToHairline(closing);
            }
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
        // Less what the table is held in by, its own margins included (writeTableWithItsOwnSpacing).
        return currentCellWidth - Math.max(0, insetLeft) - Math.max(0, insetRight);
    }

    private XWPFTable newTable(XWPFDocument document, int rows, int columns) {
        resumeHere();
        // Text hanging below the band above took that much of the gap above this table, and
        // so did a card's border standing below the card (writePanelPiece).
        pendingSpacingAfter = Math.max(0, pendingSpacingAfter - hangingBelow - borderBelow);
        hangingBelow = 0;
        borderBelow = 0;
        if (currentCell == null ? endsWithATable(document.getBodyElements()) : cellEndsWithItsTableCloser()) {
            separateFromTheTableAbove(document);
        }
        // A container edge still waiting for a paragraph stood above this table, and a table
        // carries no space above itself: it is owed with the rest of the space above, which
        // the paragraph before the table holds below itself. Left waiting, it landed on the
        // paragraph below the table instead, a gap the page does not have; dropped, it took
        // the space out of the page and everything below the table moved up.
        owePendingSpacingAfter(carriedSpacingBefore);
        carriedSpacingBefore = 0;
        // With no paragraph before it in the body — the table opens the document or a section,
        // or follows a page break — a hairline one carries it: ClassicInvoice opens with a row
        // under its page padding, and without it stood against the paper's top edge in Word.
        // Not in a cell, where it was measured to set content lower than the page does:
        // ObsidianInvoice's line items each stood 8.5pt lower and VioletGrid ran to two pages.
        if (currentCell == null) {
            holdTheSpaceAboveATable(document);
        }
        // Word has no space above a table, so the paragraph before it has to carry it.
        flushSpacingAfter();
        // Nor can that paragraph carry the space below the table: it sits above it. Space
        // owed once the table is written goes to whatever paragraph follows, as space above
        // it — and nowhere, if nothing follows — rather than back above the table, which is
        // where it used to land: a card's bottom padding opened a gap over its last table.
        lastBodyParagraph = null;
        blocksWritten++;
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
     *
     * <p>An inset below zero is written too: a container hanging left by a negative margin
     * moves what it holds out past the text beside it. {@code SerifHeadline} hangs each section
     * heading's row left by its dash, and in Word every title stood that far right of the
     * page's, 17pt in the main column.</p>
     */
    private void indentTable(XWPFTable table) {
        // Nested in a cell, a hang is its cells' (writeRow): Word keeps the table in the cell.
        if (!(Math.abs(insetLeft) > 0.01) || currentCell != null && insetLeft < 0
            || table.getRows().isEmpty() || table.getRow(0).getTableCells().isEmpty()) {
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
        // Signed: toTwips holds a width at zero, and an indent may reach out past the margin.
        indent.setW(BigInteger.valueOf(Math.round((insetLeft + firstCellMargin) * POINT_TO_TWIP)));
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

    /**
     * Whether the layout starts a block on a page after the one the block before it ended on:
     * the page broke between them, and the block's space above went with it.
     */
    private boolean startsAPageOfItsOwn(DocumentNode node) {
        com.demcha.compose.document.layout.PlacedNode placed = layout.placement(node);
        return placed != null && lastEndPage >= 0 && placed.startPage() > lastEndPage;
    }

    /**
     * Holds the space owed above a table the layout starts on a new page, on that page.
     *
     * <p>The page keeps a block's space above it where a page break moves the block down: the
     * closing pair of the long {@code LumaStudioInvoice} starts its third page 12pt below the
     * margin. Word has no space above a table, so the space was written below the paragraph
     * before it, at the foot of the page above, and the pair stood 12pt high. A paragraph's
     * space above at the top of a page Word drops too, measured; the height of a line it keeps.
     * So the space is the height of a line of its own, kept with the table.</p>
     */
    private void holdTheSpaceAboveOnItsPage(XWPFDocument document) {
        if (!(carriedSpacingBefore + pendingSpacingAfter - borderBelow - pullBelow > 0.01)) {
            return;
        }
        XWPFParagraph line = newBodyParagraph(document);
        CTPPr properties = line.getCTP().isSetPPr() ? line.getCTP().getPPr() : line.getCTP().addNewPPr();
        CTSpacing spacing = properties.isSetSpacing() ? properties.getSpacing() : properties.addNewSpacing();
        long held = twipsOf(spacing.isSetBefore() ? spacing.getBefore() : null);
        spacing.setBefore(BigInteger.ZERO);
        holdInALineKeptWithTheNext(line, held);
    }

    /** Makes a paragraph a line of exactly {@code twips} with no text, kept with what follows. */
    private static void holdInALineKeptWithTheNext(XWPFParagraph line, long twips) {
        CTPPr properties = line.getCTP().isSetPPr() ? line.getCTP().getPPr() : line.getCTP().addNewPPr();
        CTSpacing spacing = properties.isSetSpacing() ? properties.getSpacing() : properties.addNewSpacing();
        spacing.setLineRule(STLineSpacingRule.EXACT);
        spacing.setLine(BigInteger.valueOf(Math.max(1, twips)));
        if (!properties.isSetKeepNext()) {
            properties.addNewKeepNext();
        }
        // The mark of a line this tall in the document's size would not grow it; a point keeps
        // it from standing out of the line in an editor that measures the mark.
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTParaRPr mark =
                properties.isSetRPr() ? properties.getRPr() : properties.addNewRPr();
        (mark.sizeOfSzArray() > 0 ? mark.getSzArray(0) : mark.addNewSz()).setVal(BigInteger.valueOf(ONE_POINT_IN_HALF_POINTS));
    }

    /**
     * Holds the top edge of a paragraph the layout moves to a new page, on that page.
     *
     * <p>The page starts a block a page break moves down at its own top edge — the containers
     * opening there with it. Word drops a paragraph's space above at the top of a page, the
     * paragraph's own edge with the gap: {@code ModernProfessional}'s second page opens with a
     * heading its section pads 8pt down, and in Word the heading and everything under it stood
     * 7.7pt high. A line as tall as that edge, kept with the paragraph, holds it; Word keeps a
     * line's height there.</p>
     *
     * <p>The line is as tall as the layout leaves above the paragraph's text on that page: the
     * space above its box ({@link #spaceAboveOnItsPage}) and its own padding. That is the gap
     * too where the gap did not fit at the foot of the page above, and less than the edges
     * where the layout spent some on the page above — a heading kept with what follows, hoisted
     * to the next page out of a section begun on this one, leaves the section's padding behind.
     * The rest of the space owed stays above the line, so where Word breaks the page is
     * unchanged: at the foot of the page above, the line and that space together take what the
     * paragraph's space above did. Written on the page above instead, the gap let
     * {@code ClassicSerif}'s second page's first line fit on its first in Word.</p>
     */
    private void holdAParagraphsTopEdgeOnItsPage(XWPFDocument document, ParagraphNode node) {
        double own = Math.max(0, node.margin().top() + node.padding().top());
        double edges = Math.max(0, carriedSpacingBefore);
        double above = spaceAboveOnItsPage(node);
        double kept = Double.isNaN(above) ? own + edges : Math.max(0, above) + Math.max(0, node.padding().top());
        if (!(kept > 0.01)) {
            return;
        }
        // Text hanging below a band, a pull out of the block before and a card's border below
        // it stay on the page above with them, as at a table moved to a new page: the layout
        // starts the new page from its top.
        forgetTheHang();
        pullBelow = 0;
        borderBelow = 0;
        carriedSpacingBefore += own;
        XWPFParagraph line = newBodyParagraph(document);
        pullLeftOn = null;
        pullLeft = 0;
        CTPPr properties = line.getCTP().isSetPPr() ? line.getCTP().getPPr() : line.getCTP().addNewPPr();
        CTSpacing spacing = properties.isSetSpacing() ? properties.getSpacing() : properties.addNewSpacing();
        long before = twipsOf(spacing.isSetBefore() ? spacing.getBefore() : null);
        long held = Math.min(before, toTwips(kept));
        spacing.setBefore(BigInteger.valueOf(before - held));
        holdInALineKeptWithTheNext(line, held);
        topEdgeHeldAbove = node;
        topEdgeHeld = own;
        topEdgeLine = line;
    }

    /**
     * Takes up to {@code twips} out of the space above a paragraph whose top edge a line holds,
     * for the gap between the paragraph's lines (see {@link #applyLineGap}): out of that line,
     * and what the line cannot give out of the space above the line, as it came out of the
     * paragraph's own space above before the line held it. Returns how much it took.
     *
     * <p>Word drops that space above at the top of a page, so what comes out of it moves
     * nothing there and the first line stands that much low; it keeps the paragraph's lines as
     * tall as where the whole gap comes off the space above it on one page.</p>
     */
    private long takeFromTheLineHoldingItsEdge(long twips) {
        CTSpacing spacing = topEdgeLine == null ? null : topEdgeLine.getCTP().getPPr().getSpacing();
        Long line = spacing == null ? null : writtenTwips(spacing.getLine());
        if (line == null) {
            return 0;
        }
        long taken = Math.max(0, Math.min(twips, line - 1));
        spacing.setLine(BigInteger.valueOf(line - taken));
        long before = twipsOf(spacing.isSetBefore() ? spacing.getBefore() : null);
        long rest = Math.min(before, twips - taken);
        if (rest > 0) {
            spacing.setBefore(BigInteger.valueOf(before - rest));
            taken += rest;
        }
        return taken;
    }

    /**
     * How far below the top of its page the layout starts a block it moves to a new page, in
     * points: the space above the block's box on that page — its own top margin, the edges of
     * the containers opening with it and any gap carried there; the box holds its padding. NaN
     * when the export has no canvas, or the block runs over more than one page, whose placement
     * pairs its first piece's place with the whole block's height.
     */
    private double spaceAboveOnItsPage(DocumentNode node) {
        com.demcha.compose.document.layout.PlacedNode placed = layout.placement(node);
        if (placed == null || placed.startPage() != placed.endPage()
                || Double.isNaN(canvasHeight) || Double.isNaN(canvasTopMargin)) {
            return Double.NaN;
        }
        return canvasHeight - canvasTopMargin - placed.placementY() - placed.placementHeight();
    }

    /**
     * Moves the space above a spacer's line the layout starts on a new page into the line, as
     * far as the layout keeps that space on the page.
     *
     * <p>The layout carries the gap between two blocks onto the next page when the gap itself
     * does not fit at the foot of the page: {@code Executive}'s spacer between two entries opens
     * its second page 3pt below the margin. Word drops a paragraph's space above at the top of a
     * page and keeps its line's height, so the entries under it stood 3pt high. The line and the
     * space above it together are as tall as before, so Word breaks the page where it did.</p>
     */
    private void holdTheGapAboveInTheLine(XWPFParagraph para, DocumentNode node) {
        double onItsPage = spaceAboveOnItsPage(node);
        CTSpacing spacing = para.getCTP().isSetPPr() && para.getCTP().getPPr().isSetSpacing()
                ? para.getCTP().getPPr().getSpacing() : null;
        if (!(onItsPage > 0.01) || spacing == null || !spacing.isSetBefore()
                || spacing.getLineRule() != STLineSpacingRule.EXACT) {
            return;
        }
        long before = twipsOf(spacing.getBefore());
        long held = Math.min(before, toTwips(onItsPage));
        if (held <= 0) {
            return;
        }
        spacing.setBefore(BigInteger.valueOf(before - held));
        spacing.setLine(BigInteger.valueOf(twipsOf(spacing.getLine()) + held));
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
        DrawingCell alone = children.size() == 1 ? drawingCellFor(cell, children.get(0)) : null;
        if (children.size() == 1 && alone == null) {
            anchorComposedDrawing(cell, children.get(0));
        }
        writeInCell(cell, () -> {
            DrawingCell outer = drawingCell;
            drawingCell = alone;
            try {
                for (DocumentNode child : children) {
                    writeNode(cell.getXWPFDocument(), child);
                }
            } finally {
                drawingCell = outer;
            }
        });
    }

    /** Something written into a cell. */
    @FunctionalInterface
    private interface CellContent {
        void write() throws Exception;
    }

    /** Writes into a cell as a block of the cell's own, the writer's place kept around it. */
    private void writeInCell(XWPFTableCell cell, CellContent content) throws Exception {
        XWPFTableCell previousCell = currentCell;
        XWPFParagraph previousParagraph = lastBodyParagraph;
        double previousCarried = carriedSpacingBefore;
        double previousOwed = pendingSpacingAfter;
        double previousPull = pullBelow;
        double previousBorderBelow = borderBelow;
        double previousHangingBelow = hangingBelow;
        XWPFParagraph previousHangingOver = hangingOver;
        double previousHangingOverBy = hangingOverBy;
        double previousInsetLeft = insetLeft;
        double previousInsetRight = insetRight;
        double previousTextShift = cellTextShift;
        XWPFParagraph previousCloser = tableCloser;
        currentCell = cell;
        lastBodyParagraph = null;
        tableCloser = null;
        carriedSpacingBefore = 0;
        pendingSpacingAfter = 0;
        pullBelow = 0;
        borderBelow = 0;
        forgetTheHang();
        // A cell's content is measured from the cell's own edge, which its margins already
        // keep clear of the border; the containers around the table have nothing to add. A row
        // hanging left out of the cell holding it moves this cell's text only (see cellHang).
        insetLeft = 0;
        insetRight = 0;
        cellTextShift = cellHang;
        cellHang = 0;
        double overhang = 0;
        try {
            content.write();
            // A cell of space and nothing written — padding round a drawing the export keeps
            // only the place of, as a masthead's hairline column — still has its height, and
            // no paragraph to hold it: a hairline one holds it, or the row lost it.
            if (lastBodyParagraph == null && !cellEndsWithItsTableCloser()
                && carriedSpacingBefore + pendingSpacingAfter > 0 && cell.getBodyElements().isEmpty()) {
                holdToHairline(newBodyParagraph(cell.getXWPFDocument()));
            }
            // A line hanging below the cell's last block takes the space the cell owes under it
            // first, as it takes the gap above the next block in the flow; what it reaches past
            // that makes Word's cell taller than its content on the page.
            double hang = hangingBelow;
            // What a pull keeps from being written is no room for it (flushSpacingAfter).
            double taken = Math.min(Math.max(0, pendingSpacingAfter - pullBelow), hang);
            pendingSpacingAfter -= taken;
            overhang = hang - taken;
            // A cell ends where it ends: its last gap cannot land on whatever the body
            // writes next, and the body's cannot land inside it.
            flushSpacingAfter();
        } finally {
            currentCell = previousCell;
            lastBodyParagraph = previousParagraph;
            carriedSpacingBefore = previousCarried;
            pendingSpacingAfter = previousOwed;
            pullBelow = previousPull;
            // A card's border below the last thing in a cell stands below the row too, where
            // Word makes the row as tall as its tallest cell; the cell beside it starts clear.
            borderBelow = Math.max(previousBorderBelow, borderBelow);
            hangingBelow = previousHangingBelow;
            hangingOver = previousHangingOver;
            hangingOverBy = previousHangingOverBy;
            insetLeft = previousInsetLeft;
            insetRight = previousInsetRight;
            cellTextShift = previousTextShift;
            tableCloser = previousCloser;
        }
        cellOverhang = overhang;
    }

    /** Whether anything under a node was written in an earlier layer's stand-in. */
    private boolean holdsMovedContent(DocumentNode node) {
        if (movedIntoAStandIn(node)) {
            return true;
        }
        for (DocumentNode child : node.children()) {
            if (holdsMovedContent(child)) {
                return true;
            }
        }
        return false;
    }

    private boolean movedIntoAStandIn(DocumentNode node) {
        for (DocxLayerColumns.Moves stack : moves) {
            if (stack.moved(node)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes in a stand-in's place the content a later layer lays there (see
     * {@link DocxLayerColumns.Moves}), with the page's gaps: from the stand-in's top to the
     * first block, between the blocks, and from the last block to the stand-in's foot, each
     * block's own margins aside, which it writes itself. The stand-in keeps its height.
     */
    private void writeInPlaceOf(XWPFDocument document, SpacerNode standIn) throws Exception {
        List<DocumentNode> contents = new ArrayList<>();
        for (DocxLayerColumns.Moves stack : moves) {
            contents.addAll(stack.into(standIn));
        }
        if (contents.isEmpty()) {
            return;
        }
        // Top to bottom on the page, as the gaps between them are measured.
        contents.sort(java.util.Comparator.comparingDouble(content -> {
            com.demcha.compose.document.layout.PlacedNode placed = layout.placement(content);
            return placed == null ? 0 : -(placed.placementY() + placed.placementHeight());
        }));
        com.demcha.compose.document.layout.PlacedNode place = layout.placement(standIn);
        double edge = place == null ? Double.NaN : place.placementY() + place.placementHeight();
        // The edges the panel writes between, as the page places them: each block is held in to
        // where its own layer put it, whatever that layer's padding was.
        double[] across = layout.parentContent(standIn);
        double outerLeft = insetLeft;
        double outerRight = insetRight;
        // Only these blocks are let through: a stack nested in one still skips its own moved
        // content, which it writes in its own stand-ins.
        writingInAStandIn.addAll(contents);
        try {
            for (DocumentNode content : contents) {
                com.demcha.compose.document.layout.PlacedNode placed = layout.placement(content);
                if (placed != null && !Double.isNaN(edge)) {
                    owePendingSpacingAfter(Math.max(0, edge
                            - (placed.placementY() + placed.placementHeight() + content.margin().top())));
                    edge = placed.placementY() - content.margin().bottom();
                }
                insetLeft = outerLeft;
                insetRight = outerRight;
                if (placed != null && across != null) {
                    holdIn(content, placed, across[0], across[1]);
                }
                writeNode(document, content);
            }
        } finally {
            writingInAStandIn.removeAll(contents);
            insetLeft = outerLeft;
            insetRight = outerRight;
        }
        if (place != null && !Double.isNaN(edge)) {
            owePendingSpacingAfter(Math.max(0, edge - place.placementY()));
        }
    }

    private void writeSpacer(XWPFDocument document, SpacerNode node) throws Exception {
        if (standIns.contains(node)) {
            writeInPlaceOf(document, node);
            return;
        }
        boolean opensAPage = currentCell == null && overlayDepth == 0 && startsAPageOfItsOwn(node);
        if (opensAPage) {
            // Text hanging below the block before — a row's last line held to its icon — a pull
            // out of it and a card's border below it stay on the page above with it, as at a
            // paragraph moved to a new page (holdAParagraphsTopEdgeOnItsPage), whether or not the
            // spacer's line then holds any of the gap.
            forgetTheHang();
            pullBelow = 0;
            borderBelow = 0;
        }
        XWPFParagraph para = newBodyParagraph(document);
        para.createRun().setText("");
        // The spacer is its height and nothing more. An empty paragraph at Word's own line
        // height is a line of text tall, and it stood on top of that height: between two CV
        // entries held apart by a 4.5pt spacer, the page shows 16pt and LibreOffice drew 23.
        holdToHairline(para);
        if (opensAPage) {
            holdTheGapAboveInTheLine(para, node);
        }
        // The hairline is part of the height, as a separator's is of the gap it stands in: owed
        // whole below it, every spacer stood a tenth of a point taller than the page's, and
        // EditorialBlue's experience ran a point and more low by the foot of its first page.
        double height = Math.max(0, node.height() - SEPARATOR_POINTS);
        // Text that hung below the band above takes its place out of this height first.
        if (hangingOver == para) {
            height = Math.max(0, height - hangingOverBy);
            forgetTheHang();
        }
        // So does a pull out of the paragraph above that the space above it did not give.
        if (pullLeftOn == para) {
            height = Math.max(0, height - pullLeft);
            pullLeftOn = null;
            pullLeft = 0;
        }
        owePendingSpacingAfter(height);
    }

    /** Clears the text hanging below a band: it reaches the next block only (see {@link #writeLinePair}). */
    private void forgetTheHang() {
        hangingBelow = 0;
        hangingOver = null;
        hangingOverBy = 0;
    }

    private void writePageBreak(XWPFDocument document) {
        flushSpacingAfter();
        XWPFParagraph para = document.createParagraph();
        blocksWritten++;
        XWPFRun run = para.createRun();
        run.addBreak(BreakType.PAGE);
        // Space owed from here on is above what the next page opens with. The paragraph before
        // the break is on the page before, and space written below it would stay there; so is
        // text hanging below a band there.
        lastBodyParagraph = null;
        borderBelow = 0;
        forgetTheHang();
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
