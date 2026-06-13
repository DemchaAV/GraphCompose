package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.NumberFormatSpec;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.node.ChartNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.output.DocumentOutputOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
 * <p><b>Dependencies:</b> requires {@code org.apache.poi:poi-ooxml} on the
 * classpath. Library consumers must add it explicitly because the dependency is
 * declared optional in the GraphCompose POM.</p>
 *
 * @author Artem Demchyshyn
 */
public final class DocxSemanticBackend implements SemanticBackend<byte[]> {
    private static final double POINT_TO_TWIP = 20.0;
    private static final Logger LOG = LoggerFactory.getLogger(DocxSemanticBackend.class);
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
        para.setAlignment(toAlignment(node.align()));
        if (!node.inlineTextRuns().isEmpty()) {
            node.inlineTextRuns().forEach(run -> {
                XWPFRun docRun = para.createRun();
                applyStyle(docRun, node.textStyle());
                docRun.setText(run.text());
            });
        } else {
            XWPFRun docRun = para.createRun();
            applyStyle(docRun, node.textStyle());
            docRun.setText(node.text() == null ? "" : node.text());
        }
    }

    private void writeImage(XWPFDocument document, ImageNode node) throws Exception {
        DocumentImageData data = node.imageData();
        byte[] bytes = data.bytes()
                .orElseGet(() -> data.path()
                        .map(this::readBytes)
                        .orElse(new byte[0]));
        if (bytes.length == 0) {
            return;
        }
        XWPFParagraph para = document.createParagraph();
        XWPFRun run = para.createRun();
        try (InputStream stream = new java.io.ByteArrayInputStream(bytes)) {
            int width = node.width() == null ? 100 : (int) Math.round(node.width());
            int height = node.height() == null ? 100 : (int) Math.round(node.height());
            run.addPicture(stream,
                    Document.PICTURE_TYPE_PNG,
                    "image",
                    Units.toEMU(width),
                    Units.toEMU(height));
        }
    }

    private byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private void writeTable(XWPFDocument document, TableNode node) {
        if (node.rows().isEmpty()) {
            return;
        }
        int columnCount = node.rows().get(0).size();
        XWPFTable table = document.createTable(node.rows().size(), Math.max(1, columnCount));
        for (int rowIdx = 0; rowIdx < node.rows().size(); rowIdx++) {
            List<DocumentTableCell> rowCells = node.rows().get(rowIdx);
            XWPFTableRow row = table.getRow(rowIdx);
            for (int columnIdx = 0; columnIdx < rowCells.size() && columnIdx < row.getTableCells().size(); columnIdx++) {
                XWPFTableCell cell = row.getCell(columnIdx);
                cell.removeParagraph(0);
                XWPFParagraph para = cell.addParagraph();
                XWPFRun run = para.createRun();
                String text = String.join("\n", rowCells.get(columnIdx).lines());
                run.setText(text);
            }
        }
    }

    private void writeRow(XWPFDocument document, RowNode node) {
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

    private void writeRowCellChild(XWPFTableCell cell, DocumentNode child) {
        if (child instanceof ParagraphNode paragraph) {
            XWPFParagraph para = cell.addParagraph();
            XWPFRun run = para.createRun();
            applyStyle(run, paragraph.textStyle());
            run.setText(paragraph.text() == null ? "" : paragraph.text());
        } else if (child instanceof SpacerNode) {
            cell.addParagraph();
        } else {
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
            run.setFontSize((int) Math.round(style.size()));
        }
        if (style.color() != null) {
            run.setColor(toHexColor(style.color().color()));
        }
        if (style.decoration() != null) {
            switch (style.decoration()) {
                case BOLD -> run.setBold(true);
                case ITALIC -> run.setItalic(true);
                case BOLD_ITALIC -> {
                    run.setBold(true);
                    run.setItalic(true);
                }
                case UNDERLINE ->
                        run.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);
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

    private static ParagraphAlignment toAlignment(TextAlign align) {
        if (align == null) {
            return ParagraphAlignment.LEFT;
        }
        return switch (align) {
            case CENTER -> ParagraphAlignment.CENTER;
            case RIGHT -> ParagraphAlignment.RIGHT;
            default -> ParagraphAlignment.LEFT;
        };
    }
}
