package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.NumberFormatSpec;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.image.DocumentImageData;
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
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.node.RowNode;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.common.usermodel.PictureType;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRelativeRect;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
 * {@code org.apache.poi:poi-ooxml} transitively — adding that one artifact is
 * all a DOCX consumer needs.</p>
 *
 * @author Artem Demchyshyn
 */
public final class DocxSemanticBackend implements SemanticBackend<byte[]> {
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

    /**
     * Creates a DOCX semantic backend.
     */
    public DocxSemanticBackend() {
    }

    @Override
    public String name() {
        return "docx-semantic";
    }

    @Override
    public byte[] export(DocumentGraph graph, SemanticExportContext context) throws Exception {
        shapeContainerWarned.set(false);
        chartWarned.set(false);
        warnedNodeKinds.clear();
        contentWidth = context.canvas() == null ? Double.MAX_VALUE : context.canvas().innerWidth();
        try (XWPFDocument document = new XWPFDocument()) {
            applyPageGeometry(document, context.canvas());
            applyOutputOptions(document, context.outputOptions());
            for (DocumentNode root : graph.roots()) {
                writeNode(document, root);
            }
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                document.write(output);
                byte[] bytes = output.toByteArray();
                if (context.outputFile() != null) {
                    Files.write(context.outputFile(), bytes);
                }
                return bytes;
            }
        }
    }

    private void applyOutputOptions(XWPFDocument document, DocumentOutputOptions options) {
        if (options == null) {
            return;
        }
        DocumentMetadata metadata = options.metadata();
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
        // Headers/footers, watermark, and protection are ignored by the v1.3
        // DOCX exporter. Word documents do not have a direct analogue for the
        // PDF chrome model, so these values are reserved for future expansion.
    }

    private void writeNode(XWPFDocument document, DocumentNode node) throws Exception {
        if (node instanceof ParagraphNode paragraph) {
            writeParagraph(document, paragraph);
        } else if (node instanceof ImageNode image) {
            writeImage(document, image);
        } else if (node instanceof TableNode table) {
            writeTable(document, table);
        } else if (node instanceof SpacerNode spacer) {
            writeSpacer(document, spacer);
        } else if (node instanceof PageBreakNode) {
            writePageBreak(document);
        } else if (node instanceof RowNode row) {
            writeRow(document, row);
        } else if (node instanceof ShapeContainerNode shapeContainer) {
            writeShapeContainer(document, shapeContainer);
        } else if (node instanceof ChartNode chart) {
            writeChartFallback(document, chart);
        } else if (node instanceof com.demcha.compose.document.node.ListNode list) {
            writeList(document, list);
        } else if (node instanceof ContainerNode || node instanceof SectionNode
                   || node instanceof com.demcha.compose.document.node.LayerStackNode
                   || node instanceof com.demcha.compose.document.node.CanvasLayerNode) {
            // Overlay/positioned wrappers have no DOCX analogue for their
            // geometry, but their children can be semantic (text, images) —
            // render them sequentially rather than dropping the subtree.
            for (DocumentNode child : node.children()) {
                writeNode(document, child);
            }
        } else {
            // Geometry-only node kinds (line, ellipse, shape, path, polygon,
            // barcode) have no semantic Word analogue. Warn once per kind so a
            // dropped chart-line or icon is visible in the log instead of
            // silently missing; authors needing pixel-perfect output use the
            // PDF fixed-layout backend.
            warnUnsupported(node);
        }
    }

    /** One warning per dropped node kind, deduplicated across the export. */
    private void warnUnsupported(DocumentNode node) {
        if (warnedNodeKinds.add(node.nodeKind())) {
            LOG.warn("DocxSemanticBackend: dropping '{}' node(s) — geometry has no semantic "
                     + "Word analogue; use the PDF backend for pixel-perfect output", node.nodeKind());
        }
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
        for (String item : list.items()) {
            // Same normalization as the fixed-layout pipeline: strip an
            // author-typed leading marker and skip items with no content.
            String normalized = com.demcha.compose.document.node.ListMarker
                    .normalizeItemText(item, list.normalizeMarkers());
            if (normalized.isBlank()) {
                continue;
            }
            writeListLine(document, list.textStyle(),
                    list.marker().prefix() + normalized, 0);
        }
        for (com.demcha.compose.document.node.ListItem item : list.nestedItems()) {
            writeNestedItem(document, list, item, 0);
        }
    }

    private void writeNestedItem(XWPFDocument document,
                                 com.demcha.compose.document.node.ListNode list,
                                 com.demcha.compose.document.node.ListItem item,
                                 int depth) {
        // prefix() carries its own trailing space (and is empty for
        // markerless lists). Items without an explicit (or markerFor-baked)
        // marker fall back to the same depth cascade the fixed-layout
        // pipeline uses — never to the flat-list marker.
        com.demcha.compose.document.node.ListMarker marker =
                item.marker() != null
                        ? item.marker()
                        : com.demcha.compose.document.node.ListMarker.defaultForDepth(depth);
        writeListLine(document, list.textStyle(), marker.prefix() + item.label(), depth);
        for (com.demcha.compose.document.node.ListItem child : item.children()) {
            writeNestedItem(document, list, child, depth + 1);
        }
    }

    private void writeListLine(XWPFDocument document, DocumentTextStyle style,
                               String text, int depth) {
        XWPFParagraph para = document.createParagraph();
        XWPFRun run = para.createRun();
        applyStyle(run, style);
        run.setText("  ".repeat(depth) + text);
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

    private void writeShapeContainer(XWPFDocument document, ShapeContainerNode node) throws Exception {
        // POI/DOCX has no portable equivalent of a graphics-state path clip.
        // The fallback rule (recorded in docs/canonical-legacy-parity.md) is
        // to render the container's layers inline, in source order, without
        // the outline frame and without clipping. The resulting Word document
        // shows the layer content but not the shape boundary — authors who
        // need the boundary must export to PDF.
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
        XWPFParagraph para = document.createParagraph();
        applyParagraphProperties(para, node);
        writeParagraphRuns(para, node);
    }

    /**
     * Writes the properties a paragraph carries whatever it sits in.
     *
     * <p>One place on purpose. These were written where a paragraph is a document child
     * and not where it is a table cell's, which is how every right-to-left invoice line
     * came out undeclared; splitting them again would set the next property up for the
     * same fate.</p>
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
     */
    private static void applyParagraphProperties(XWPFParagraph target, ParagraphNode source) {
        boolean rightToLeft = ParagraphDirection.resolve(source) == TextDirection.RTL;
        target.setAlignment(toAlignment(source.align(), rightToLeft));
        applyDirection(target, rightToLeft);
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
     * blank {@code ParagraphNode} fills it by concatenating exactly the runs
     * {@code inlineTextRuns()} returns, highlight chips included.</p>
     */
    private void writeParagraphRuns(XWPFParagraph para, ParagraphNode node) {
        List<InlineTextRun> runs = node.inlineTextRuns();
        if (runs.isEmpty()) {
            XWPFRun docRun = para.createRun();
            applyStyle(docRun, node.textStyle());
            docRun.setText(node.text() == null ? "" : node.text());
            return;
        }
        for (InlineTextRun run : runs) {
            XWPFRun docRun = para.createRun();
            applyStyle(docRun, run.textStyle() == null ? node.textStyle() : run.textStyle());
            docRun.setText(run.text() == null ? "" : run.text());
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
        DocumentImageData data = node.imageData();
        byte[] bytes = data.bytes()
                .orElseGet(() -> data.path()
                        .map(this::readBytes)
                        .orElse(new byte[0]));
        if (bytes.length == 0) {
            return;
        }
        ImageData resolved = NodeDefinitionSupport.toImageData(node.imageData());
        double sourceWidth = Math.max(1, resolved.getMetadata().width());
        double sourceHeight = Math.max(1, resolved.getMetadata().height());
        NodeDefinitionSupport.ImageDimensions box =
                NodeDefinitionSupport.resolveImageDimensions(node, contentWidth);

        DocumentImageFitMode fitMode =
                node.fitMode() == null ? DocumentImageFitMode.STRETCH : node.fitMode();
        double drawWidth = box.width();
        double drawHeight = box.height();
        if (fitMode == DocumentImageFitMode.CONTAIN) {
            double scale = Math.min(box.width() / sourceWidth, box.height() / sourceHeight);
            drawWidth = sourceWidth * scale;
            drawHeight = sourceHeight * scale;
        }

        XWPFParagraph para = document.createParagraph();
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

    private byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (Exception e) {
            return new byte[0];
        }
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
            document.createTable(rowCount, 1);
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
        XWPFTable table = document.createTable(rowCount, 1);
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
                if (placement.row() != rowIdx) {
                    // A covered position carries the merge marker and no content of its own.
                    continue;
                }
                cell.removeParagraph(0);
                writeCellContent(cell, placement, node);
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
            writeCellBody(cell, source.content());
            return;
        }
        XWPFParagraph para = cell.addParagraph();
        XWPFRun run = para.createRun();
        applyStyle(run, resolveCellTextStyle(node, placement));
        List<String> lines = source.lines();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                // A joined "\n" is not a line break in Word; it renders as one line.
                run.addBreak();
            }
            run.setText(lines.get(i) == null ? "" : lines.get(i), i);
        }
    }

    /**
     * The text style a cell resolves to, most specific wins.
     *
     * <p>The same order the layout pipeline merges in: the table's default, then the
     * column's, then the row's, then the cell's own.</p>
     */
    private DocumentTextStyle resolveCellTextStyle(TableNode node, TableGrid.Placement placement) {
        return resolveCellValue(node, placement, DocumentTableStyle::textStyle);
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
        XWPFTable table = document.createTable(1, node.children().size());
        XWPFTableRow row = table.getRow(0);
        for (int i = 0; i < node.children().size(); i++) {
            XWPFTableCell cell = row.getCell(i);
            cell.removeParagraph(0);
            DocumentNode child = node.children().get(i);
            writeRowCellChild(cell, child);
        }
    }

    private void writeRowCellChild(XWPFTableCell cell, DocumentNode child) throws Exception {
        writeCellBody(cell, child);
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
    private void writeCellNode(XWPFTableCell cell, DocumentNode child) throws Exception {
        if (child instanceof ParagraphNode paragraph) {
            // Same walk as writeParagraph, all of it: a cell paragraph keeps per-run
            // styling instead of being flattened into one style — and its alignment and
            // direction too. Writing only the runs left every right-to-left paragraph
            // inside a table undeclared, which is where an invoice keeps its line items.
            XWPFParagraph cellParagraph = cell.addParagraph();
            applyParagraphProperties(cellParagraph, paragraph);
            writeParagraphRuns(cellParagraph, paragraph);
        } else if (child instanceof ContainerNode container) {
            for (DocumentNode grandChild : container.children()) {
                writeCellNode(cell, grandChild);
            }
        } else if (child instanceof SectionNode section) {
            for (DocumentNode grandChild : section.children()) {
                writeCellNode(cell, grandChild);
            }
        } else if (child instanceof SpacerNode) {
            cell.addParagraph();
        } else {
            warnUnsupported(child);
            // Unsupported cell content gets an empty paragraph placeholder.
            cell.addParagraph();
        }
    }

    private void writeSpacer(XWPFDocument document, SpacerNode node) {
        XWPFParagraph para = document.createParagraph();
        para.createRun().setText("");
        if (node.height() > 0) {
            para.setSpacingAfter((int) Math.round(node.height() * POINT_TO_TWIP));
        }
    }

    private void writePageBreak(XWPFDocument document) {
        XWPFParagraph para = document.createParagraph();
        XWPFRun run = para.createRun();
        run.addBreak(BreakType.PAGE);
    }

    private void applyStyle(XWPFRun run, DocumentTextStyle style) {
        if (style == null) {
            return;
        }
        if (style.fontName() != null) {
            run.setFontFamily(style.fontName().name());
        }
        if (style.size() > 0) {
            // Passed as a double, because w:sz counts half-points and rounding to whole
            // points first throws away a precision the format has: the timeline's 8.5pt
            // label was being written as 9pt.
            run.setFontSize(style.size());
            // Word sizes complex-script characters — Hebrew, Arabic — from w:szCs and not
            // from w:sz, so a run carrying both scripts draws its Latin at the asked size
            // and everything else at Word's own default until this is written too.
            run.setComplexScriptFontSize(style.size());
        }
        if (style.color() != null) {
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
        if (canvas == null) {
            return;
        }
        CTSectPr sectPr = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();
        CTPageSz pageSize = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(toTwips(canvas.width())));
        pageSize.setH(BigInteger.valueOf(toTwips(canvas.height())));
        pageSize.setOrient(STPageOrientation.PORTRAIT);

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
