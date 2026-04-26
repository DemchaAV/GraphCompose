package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.SectionNode;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
        try (XWPFDocument document = new XWPFDocument()) {
            applyPageGeometry(document, context.canvas());
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
        } else if (node instanceof ContainerNode || node instanceof SectionNode) {
            for (DocumentNode child : node.children()) {
                writeNode(document, child);
            }
        }
        // Unsupported node kinds (line, ellipse, shape, barcode) are silently
        // skipped in the semantic export. Authors who need pixel-perfect output
        // should use the PDF fixed-layout backend.
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
