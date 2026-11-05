package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.chart.AxisSpec;
import com.demcha.compose.document.chart.BarGrouping;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartDefaults;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.chart.ChartStyle;
import com.demcha.compose.document.chart.LegendPosition;
import com.demcha.compose.document.chart.LineInterpolation;
import com.demcha.compose.document.chart.NiceScale;
import com.demcha.compose.document.chart.NumberFormatSpec;
import com.demcha.compose.document.chart.PointMarker;
import com.demcha.compose.document.chart.SliceLabelMode;
import com.demcha.compose.document.chart.ValueLabelMode;
import com.demcha.compose.document.node.ChartNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Writes a chart as Word's own: a chart part ({@code c:chartSpace}) drawn inline at the size the
 * page draws the chart, its numbers in a workbook embedded with it, so a reader edits them in
 * Word through "Edit Data" and the chart redraws.
 *
 * <p>The chart part is given what the page's chart is built from, not its strokes: the same
 * categories and series, the series' colours, the value axis's bounds and step as the page
 * computes them ({@link NiceScale}), its number format, the legend's side and the text styles.
 * Word lays the chart out itself inside that box — where the plot area stands, how far a label
 * sits from its bar — so the two agree in what they show and in the box they take, not stroke for
 * stroke. What Word's chart has no setting for is returned as phrases for the chart's note.</p>
 *
 * <p>The part is written as text, not through POI's chart classes: the schema jar this module
 * ships ({@code poi-ooxml-lite}) holds only the chart types POI's own tests touch, and writing a
 * bar overlap through those classes fails at run time for want of its compiled schema.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxNativeCharts {

    private static final String CHART_TYPE = "application/vnd.openxmlformats-officedocument.drawingml.chart+xml";
    private static final String CHART_RELATION =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/chart";
    private static final String WORKBOOK_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PACKAGE_RELATION =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/package";
    private static final String SHAPES_TYPE = "application/vnd.openxmlformats-officedocument.drawingml.chartshapes+xml";
    private static final String SHAPES_RELATION =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/chartUserShapes";
    private static final String SHEET = "Sheet1";

    /** Word's least and largest donut hole, as a percentage of the outer diameter. */
    private static final int LEAST_HOLE = 10;
    private static final int LARGEST_HOLE = 90;

    /** Word's least and largest point marker, in points. */
    private static final int LEAST_MARKER = 2;
    private static final int LARGEST_MARKER = 72;

    /** Word's widest gap between bar clusters, as a percentage of a bar's width. */
    private static final int WIDEST_GAP = 500;

    /** The share of its series' slot the page draws a grouped bar (BarChartLayout's INNER_BAR_RATIO). */
    private static final double INNER_BAR_RATIO = 0.86;

    /** The page's line width and area opacity where the style sets none (ChartLayoutSupport, LineChartLayout). */
    private static final double LINE_WIDTH = 1.5;
    private static final double AREA_OPACITY = 0.35;

    private static final int CATEGORY_AXIS = 500_000_001;
    private static final int VALUE_AXIS = 500_000_002;

    private DocxNativeCharts() {
    }

    /**
     * Writes {@code node} into {@code run} as a Word chart {@code width} by {@code height} points.
     *
     * @param ring      where the page draws a pie's disc in the chart's box, as fractions of its width
     *                  and height from its top left corner, or {@code null} where the layout gives none
     * @param drawingId the drawing's id in the document, unique among its drawings
     * @param family    the Word family a text style's font is written in, as the document's runs
     * @param pinned    the instant the embedded workbook's clocks are set to ({@link #workbookOf})
     * @return what the chart part does not carry, phrased for the chart's note; empty when nothing
     */
    static List<String> write(XWPFDocument document, XWPFRun run, ChartNode node, double width, double height,
                              Rectangle2D ring, long drawingId, Function<FontName, String> family, Instant pinned)
            throws IOException {
        ChartStyle style = ChartDefaults.DEFAULT_THEME.toChartStyle().mergedUnder(node.style());
        Part part = new Part(style, family, width, height, ring);
        String chartXml = part.chartSpace(node.spec());

        OPCPackage pack = document.getPackage();
        PackagePart chart = pack.createPart(freeName(pack, "/word/charts/chart", ".xml"), CHART_TYPE);
        try (OutputStream out = chart.getOutputStream()) {
            out.write(chartXml.getBytes(StandardCharsets.UTF_8));
        }
        PackagePart workbook = pack.createPart(freeName(pack, "/word/embeddings/Microsoft_Excel_Worksheet", ".xlsx"),
                WORKBOOK_TYPE);
        try (OutputStream out = workbook.getOutputStream()) {
            out.write(workbookOf(node.spec().data(), pinned));
        }
        chart.addRelationship(workbook.getPartName(), TargetMode.INTERNAL, PACKAGE_RELATION, "rId1");
        if (part.shapes != null) {
            PackagePart shapes = pack.createPart(freeName(pack, "/word/drawings/drawing", ".xml"), SHAPES_TYPE);
            try (OutputStream out = shapes.getOutputStream()) {
                out.write(part.shapes.getBytes(StandardCharsets.UTF_8));
            }
            chart.addRelationship(shapes.getPartName(), TargetMode.INTERNAL, SHAPES_RELATION, "rId2");
        }
        String relation = document.getPackagePart()
                .addRelationship(chart.getPartName(), TargetMode.INTERNAL, CHART_RELATION).getId();
        run.getCTR().addNewDrawing().set(inline(width, height, drawingId, relation));
        return new ArrayList<>(part.lost);
    }

    /** The first part name {@code prefix + n + suffix} the package does not hold. */
    private static PackagePartName freeName(OPCPackage pack, String prefix, String suffix) throws IOException {
        try {
            for (int n = 1; ; n++) {
                PackagePartName name = PackagingURIHelper.createPartName(prefix + n + suffix);
                if (!pack.containPart(name)) {
                    return name;
                }
            }
        } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException failure) {
            throw new IOException(failure);
        }
    }

    /** The run's drawing: the chart inline, the size the page draws it. */
    private static CTDrawing inline(double width, double height, long id, String relation) {
        String xml = "<w:drawing xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\""
                + " xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\""
                + " xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\""
                + " xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\""
                + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">"
                + "<wp:extent cx=\"" + Units.toEMU(width) + "\" cy=\"" + Units.toEMU(height) + "\"/>"
                + "<wp:effectExtent l=\"0\" t=\"0\" r=\"0\" b=\"0\"/>"
                // Named as the export names its other drawings, by kind and number: a reader of the
                // file tells a chart from a drawing a node paints and from a page's fill by it.
                + "<wp:docPr id=\"" + id + "\" name=\"Chart " + id + "\"/>"
                + "<wp:cNvGraphicFramePr/>"
                + "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">"
                + "<c:chart r:id=\"" + relation + "\"/></a:graphicData></a:graphic>"
                + "</wp:inline></w:drawing>";
        XmlOptions options = new XmlOptions();
        options.setLoadReplaceDocumentElement(null);
        try {
            return CTDrawing.Factory.parse(xml, options);
        } catch (XmlException failure) {
            throw new IllegalStateException("could not build a chart's drawing", failure);
        }
    }

    /**
     * The workbook a reader edits the chart's numbers in: the categories down column A, a series a
     * column from B with its name at the head, as Word lays out a chart's data itself. Its clocks —
     * its core properties and its entries' times — are set to {@code pinned}, so the workbook is
     * the same bytes for the same numbers.
     */
    static byte[] workbookOf(ChartData data, Instant pinned) throws IOException {
        Objects.requireNonNull(pinned, "pinned");
        byte[] bytes;
        try (XSSFWorkbook book = new XSSFWorkbook()) {
            XSSFSheet sheet = book.createSheet(SHEET);
            XSSFRow head = sheet.createRow(0);
            for (int s = 0; s < data.seriesCount(); s++) {
                head.createCell(s + 1).setCellValue(data.series().get(s).name());
            }
            for (int c = 0; c < data.categoryCount(); c++) {
                XSSFRow row = sheet.createRow(c + 1);
                row.createCell(0).setCellValue(data.categories().get(c));
                for (int s = 0; s < data.seriesCount(); s++) {
                    Double v = data.series().get(s).values().get(c);
                    if (v != null) {
                        row.createCell(s + 1).setCellValue(v);
                    }
                }
            }
            Optional<Date> at = Optional.of(Date.from(pinned));
            book.getProperties().getCoreProperties().setCreated(at);
            book.getProperties().getCoreProperties().setModified(at);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            book.write(out);
            bytes = out.toByteArray();
        }
        return DocxDeterminism.normalizeZipEntries(bytes, pinned);
    }

    /** One chart part being written. */
    private static final class Part {
        private final ChartStyle style;
        private final Function<FontName, String> family;
        private final double width;
        private final double height;
        private final Rectangle2D ring;
        // Once each: an axis and every labelled series may lose the same thing to one format.
        private final java.util.Set<String> lost = new java.util.LinkedHashSet<>();
        private final StringBuilder xml = new StringBuilder(4096);
        /** The chart's own drawing part, a donut's centre text, or {@code null} when it has none. */
        private String shapes;

        Part(ChartStyle style, Function<FontName, String> family, double width, double height, Rectangle2D ring) {
            this.style = style;
            this.family = family;
            this.width = width;
            this.height = height;
            this.ring = ring;
        }

        String chartSpace(ChartSpec spec) {
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                    .append("<c:chartSpace xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\"")
                    .append(" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\"")
                    .append(" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
                    .append("<c:date1904 val=\"0\"/><c:lang val=\"en-US\"/><c:roundedCorners val=\"0\"/>")
                    // No title: Word otherwise titles a chart of one series with the series' name.
                    .append("<c:chart><c:autoTitleDeleted val=\"1\"/><c:plotArea>");
            if (spec instanceof ChartSpec.Pie && ring != null) {
                // A pie's disc stands where the page draws it, so a text set over its hole meets it.
                xml.append("<c:layout><c:manualLayout><c:layoutTarget val=\"inner\"/><c:xMode val=\"edge\"/>")
                        .append("<c:yMode val=\"edge\"/><c:x val=\"").append(fraction(ring.getX()))
                        .append("\"/><c:y val=\"").append(fraction(ring.getY()))
                        .append("\"/><c:w val=\"").append(fraction(ring.getWidth()))
                        .append("\"/><c:h val=\"").append(fraction(ring.getHeight()))
                        .append("\"/></c:manualLayout></c:layout>");
            } else {
                xml.append("<c:layout/>");
            }
            if (spec instanceof ChartSpec.Bar bar) {
                bar(bar);
            } else if (spec instanceof ChartSpec.Line line) {
                line(line);
            } else if (spec instanceof ChartSpec.Pie pie) {
                pie(pie);
            }
            xml.append(NO_FRAME).append("</c:plotArea>");
            legend(spec);
            // A missing value is a gap, as the page leaves it.
            xml.append("<c:plotVisOnly val=\"1\"/><c:dispBlanksAs val=\"gap\"/></c:chart>")
                    .append(NO_FRAME)
                    .append("<c:externalData r:id=\"rId1\"><c:autoUpdate val=\"0\"/></c:externalData>")
                    .append(shapes == null ? "" : "<c:userShapes r:id=\"rId2\"/>")
                    .append("</c:chartSpace>");
            return xml.toString();
        }

        private static final String NO_FRAME = "<c:spPr><a:noFill/><a:ln><a:noFill/></a:ln></c:spPr>";

        private void bar(ChartSpec.Bar bar) {
            ChartData data = bar.data();
            boolean stacked = bar.grouping() == BarGrouping.STACKED;
            int count = data.seriesCount();
            xml.append("<c:barChart><c:barDir val=\"").append(bar.horizontal() ? "bar" : "col").append("\"/>")
                    .append("<c:grouping val=\"").append(stacked ? "stacked" : "clustered").append("\"/>")
                    .append("<c:varyColors val=\"0\"/>");
            boolean labelled = bar.valueLabels() == ValueLabelMode.OUTSIDE && !stacked;
            for (int s = 0; s < count; s++) {
                xml.append("<c:ser>");
                seriesHead(data, s);
                xml.append("<c:spPr>").append(fill(paintOf(s).primaryColor())).append("<a:ln><a:noFill/></a:ln></c:spPr>")
                        .append("<c:invertIfNegative val=\"0\"/>");
                if (labelled) {
                    valueLabels(bar.valueAxis().format(), "outEnd", shown(data.series().get(s).values()));
                }
                categoriesAndValues(data, s, false);
                xml.append("</c:ser>");
                if (!(paintOf(s) instanceof DocumentPaint.Solid)) {
                    lost.add("series " + (s + 1) + "'s gradient is written as its first colour");
                }
            }
            double ratio = style.barWidthRatio() == null ? ChartDefaults.BAR_WIDTH_RATIO : style.barWidthRatio();
            double gap;
            if (stacked) {
                gap = (1 - ratio) / ratio;
                xml.append("<c:gapWidth val=\"").append(gapWidth(gap)).append("\"/>")
                        .append("<c:overlap val=\"100\"/>");
            } else {
                // The page's slot holds the group, ratio of it; each series a share of the group; and
                // each bar INNER_BAR_RATIO of its share, the rest a gap between the bars.
                double share = ratio / count;
                double barWidth = share * INNER_BAR_RATIO;
                double spanned = share * (count - 1) + barWidth;
                gap = (1 - spanned) / barWidth;
                xml.append("<c:gapWidth val=\"").append(gapWidth(gap)).append("\"/>")
                        .append("<c:overlap val=\"").append(Math.round(-100 * (share - barWidth) / barWidth))
                        .append("\"/>");
            }
            if (Math.round(gap * 100) > WIDEST_GAP) {
                lost.add("its bars are drawn wider than the page's: the gap between their groups is past the "
                         + WIDEST_GAP + "% of a bar Word takes");
            }
            xml.append("<c:axId val=\"" + CATEGORY_AXIS + "\"/><c:axId val=\"" + VALUE_AXIS + "\"/></c:barChart>");
            // The page sets the first category at the top; Word's bar chart starts at the bottom
            // unless its categories run from the top, the value axis then crossing at their far end.
            categoryAxis(bar.horizontal() ? "l" : "b", bar.horizontal(), bar.showCategoryLabels());
            valueAxis(bar.horizontal() ? "b" : "l", bar.valueAxis(),
                    scaleOf(data, bar.valueAxis(), stacked, stacked && !bar.horizontal()), bar.horizontal());
            if (bar.valueLabels() == ValueLabelMode.OUTSIDE && stacked) {
                lost.add("its stack totals are not written: Word labels each part of a stacked bar, not their sum");
            }
            if (style.barCornerRadius() != null && !style.barCornerRadius().isZero()) {
                lost.add("its bars' rounded corners are not written: Word's bars are square");
            }
        }

        private void line(ChartSpec.Line line) {
            ChartData data = line.data();
            if (line.area()) {
                double opacity = style.areaOpacity() == null ? AREA_OPACITY : style.areaOpacity();
                xml.append("<c:areaChart><c:grouping val=\"standard\"/><c:varyColors val=\"0\"/>");
                for (int s = 0; s < data.seriesCount(); s++) {
                    DocumentColor colour = paintOf(s).primaryColor();
                    xml.append("<c:ser>");
                    seriesHead(data, s);
                    xml.append("<c:spPr>")
                            .append(fill(colour.withOpacity(opacity)))
                            .append("<a:ln><a:noFill/></a:ln></c:spPr>");
                    categoriesAndValues(data, s, false);
                    xml.append("</c:ser>");
                }
                xml.append("<c:axId val=\"" + CATEGORY_AXIS + "\"/><c:axId val=\"" + VALUE_AXIS + "\"/></c:areaChart>");
                if (data.series().stream().anyMatch(series -> series.values().contains(null))) {
                    // Measured: Word breaks the area at a missing value, as the page does.
                    lost.add("LibreOffice fills its area down to the axis at a missing value, where the page and "
                             + "Word leave a gap");
                }
                if (line.interpolation() == LineInterpolation.SMOOTH) {
                    // Measured: Word smooths the line and fills under straight lines, so the fill
                    // stands off the curve; LibreOffice draws this line straight as well.
                    lost.add("its area is filled under straight lines between the points while its line is "
                             + "smoothed, so the fill stands off the curve in wedges: Word does not smooth an area, "
                             + "and LibreOffice draws the line straight as well");
                }
            }
            double strokeWidth = style.lineWidth() == null ? LINE_WIDTH : style.lineWidth();
            PointMarker marker = style.pointMarker();
            // A monotone curve is written straight: Word's and LibreOffice's smoothing rises past the
            // points between them (measured below a point's value in both), which is what the page's
            // monotone curve is chosen not to do; straight segments keep to the numbers.
            boolean smooth = line.interpolation() == LineInterpolation.SMOOTH;
            xml.append("<c:lineChart><c:grouping val=\"standard\"/><c:varyColors val=\"0\"/>");
            for (int s = 0; s < data.seriesCount(); s++) {
                DocumentColor colour = paintOf(s).primaryColor();
                xml.append("<c:ser>");
                // A series' index is the chart's, not its plot's: with the area's series written
                // first, Word will not open a chart whose lines take the same indexes.
                seriesHead(data, s, line.area() ? data.seriesCount() + s : s);
                xml.append("<c:spPr><a:ln w=\"").append(Units.toEMU(strokeWidth)).append("\" cap=\"rnd\">")
                        .append(fill(colour)).append("<a:round/></a:ln></c:spPr>");
                if (marker == null) {
                    xml.append("<c:marker><c:symbol val=\"none\"/></c:marker>");
                } else {
                    long size = Math.max(LEAST_MARKER, Math.min(LARGEST_MARKER,
                            Math.round((marker.width() + marker.height()) / 2)));
                    xml.append("<c:marker><c:symbol val=\"circle\"/><c:size val=\"").append(size).append("\"/><c:spPr>")
                            .append(fill(marker.fill() == null ? colour : marker.fill().primaryColor()))
                            .append(marker.stroke() == null ? "<a:ln><a:noFill/></a:ln>" : stroke(marker.stroke()))
                            .append("</c:spPr></c:marker>");
                }
                if (line.valueLabels() == ValueLabelMode.OUTSIDE) {
                    valueLabels(line.valueAxis().format(), "t", shown(data.series().get(s).values()));
                }
                categoriesAndValues(data, s, false);
                xml.append("<c:smooth val=\"").append(smooth ? 1 : 0).append("\"/></c:ser>");
            }
            xml.append("<c:marker val=\"1\"/>")
                    .append("<c:axId val=\"" + CATEGORY_AXIS + "\"/><c:axId val=\"" + VALUE_AXIS + "\"/></c:lineChart>");
            // The page sets each point in the middle of its category's slot.
            categoryAxis("b", false, line.showCategoryLabels());
            valueAxis("l", line.valueAxis(), scaleOf(data, line.valueAxis(), false, false), false);
            if (line.interpolation() == LineInterpolation.MONOTONE) {
                lost.add("its monotone curve is written as straight segments between its points: Word's smoothed "
                         + "line would pass beyond them where the page's curve does not");
            }
            if (marker != null && marker.width() != marker.height()) {
                lost.add("its point markers are written round: Word's markers are circles");
            }
        }

        private void pie(ChartSpec.Pie pie) {
            ChartData data = pie.data();
            boolean donut = pie.donutRatio() > 0;
            xml.append(donut ? "<c:doughnutChart>" : "<c:pieChart>").append("<c:varyColors val=\"1\"/><c:ser>");
            seriesHead(data, 0);
            DocumentStroke separator = style.sliceStroke() == null ? ChartDefaults.SLICE_STROKE : style.sliceStroke();
            for (int c = 0; c < data.categoryCount(); c++) {
                xml.append("<c:dPt><c:idx val=\"").append(c).append("\"/><c:bubble3D val=\"0\"/><c:spPr>")
                        .append(fill(paintOf(c).primaryColor())).append(stroke(separator)).append("</c:spPr></c:dPt>");
            }
            if (pie.sliceLabels() != SliceLabelMode.NONE) {
                sliceLabels(pie, donut);
            }
            categoriesAndValues(data, 0, true);
            xml.append("</c:ser><c:firstSliceAng val=\"").append(firstSliceAngle(pie)).append("\"/>");
            if (donut) {
                int hole = (int) Math.round(pie.donutRatio() * 100);
                xml.append("<c:holeSize val=\"").append(Math.max(LEAST_HOLE, Math.min(LARGEST_HOLE, hole)))
                        .append("\"/></c:doughnutChart>");
                if (hole < LEAST_HOLE) {
                    lost.add("its hole is written at " + LEAST_HOLE + "% of its width, the least Word draws");
                }
            } else {
                xml.append("</c:pieChart>");
            }
            if (!pie.clockwise()) {
                lost.add("its slices are written clockwise: Word draws a pie only that way");
            }
            if (pie.centerText() != null) {
                centreText(pie);
            }
            if (style.sliceGapDegrees() != null && style.sliceGapDegrees() > 0) {
                lost.add("the gap between its slices is not written: Word sets slices edge to edge");
            }
        }

        /**
         * A donut's centre text, as a text box of the chart's own ({@code c:userShapes}) over its
         * hole: as wide as the hole, its text centred both ways, so the text's width needs no
         * measuring. The disc is pinned where the page draws it, and the box is set on its centre.
         */
        private void centreText(ChartSpec.Pie pie) {
            double centreX = 0.5;
            double centreY = 0.5;
            double holeWide = pie.donutRatio() * Math.min(width, height) / width;
            if (ring != null) {
                centreX = ring.getCenterX();
                centreY = ring.getCenterY();
                holeWide = pie.donutRatio() * ring.getWidth();
            } else {
                lost.add("the layout gives no place of its disc here — composed in a table cell, or with no "
                         + "layout behind the export — so the text in its hole is set in the middle of its box, "
                         + "where Word may not draw the hole");
            }
            DocumentTextStyle text = style.donutCenterTextStyle() == null
                    ? ChartDefaults.DONUT_CENTER_TEXT_STYLE : style.donutCenterTextStyle();
            double tall = Math.max(text.size(), 1) * 2 / height;
            String run = "<a:r>" + runProperties("a:rPr", " lang=\"en-US\"", text)
                    + "<a:t>" + escaped(pie.centerText()) + "</a:t></a:r>";
            shapes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                     + "<c:userShapes xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\""
                     + " xmlns:cdr=\"http://schemas.openxmlformats.org/drawingml/2006/chartDrawing\""
                     + " xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">"
                     + "<cdr:relSizeAnchor><cdr:from><cdr:x>" + fraction(centreX - holeWide / 2) + "</cdr:x>"
                     + "<cdr:y>" + fraction(centreY - tall / 2) + "</cdr:y></cdr:from>"
                     + "<cdr:to><cdr:x>" + fraction(centreX + holeWide / 2) + "</cdr:x>"
                     + "<cdr:y>" + fraction(centreY + tall / 2) + "</cdr:y></cdr:to>"
                     + "<cdr:sp macro=\"\" textlink=\"\"><cdr:nvSpPr><cdr:cNvPr id=\"2\" name=\"Centre text\"/>"
                     + "<cdr:cNvSpPr txBox=\"1\"/></cdr:nvSpPr><cdr:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/>"
                     + "<a:ext cx=\"0\" cy=\"0\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom>"
                     + "<a:noFill/><a:ln><a:noFill/></a:ln></cdr:spPr>"
                     + "<cdr:txBody><a:bodyPr wrap=\"none\" lIns=\"0\" tIns=\"0\" rIns=\"0\" bIns=\"0\" anchor=\"ctr\"/>"
                     + "<a:lstStyle/><a:p><a:pPr algn=\"ctr\"/>" + run + "</a:p></cdr:txBody></cdr:sp>"
                     + "</cdr:relSizeAnchor></c:userShapes>";
        }

        private void sliceLabels(ChartSpec.Pie pie, boolean donut) {
            SliceLabelMode mode = pie.sliceLabels();
            boolean percent = mode == SliceLabelMode.PERCENT || mode == SliceLabelMode.CATEGORY_PERCENT;
            boolean category = mode == SliceLabelMode.CATEGORY || mode == SliceLabelMode.CATEGORY_PERCENT;
            boolean value = mode == SliceLabelMode.VALUE;
            // The page labels a slice only where it draws one, above zero.
            double[] values = java.util.Arrays.stream(shown(pie.data().series().get(0).values()))
                    .filter(v -> v > 0).toArray();
            double total = java.util.Arrays.stream(values).sum();
            double[] shares = java.util.Arrays.stream(values).map(v -> v / total * 100).toArray();
            String code = percent ? percentCode(pie.percentFormat(), shares)
                    : value ? excelCode(pie.valueFormat(), values) : null;
            xml.append("<c:dLbls>");
            if (code != null) {
                xml.append("<c:numFmt formatCode=\"").append(escaped(code)).append("\" sourceLinked=\"0\"/>");
            }
            xml.append(labelChip()).append(textBody(style.valueLabelTextStyle()));
            if (!donut) {
                xml.append("<c:dLblPos val=\"outEnd\"/>");
            }
            xml.append("<c:showLegendKey val=\"0\"/><c:showVal val=\"").append(value ? 1 : 0).append("\"/>")
                    .append("<c:showCatName val=\"").append(category ? 1 : 0).append("\"/>")
                    .append("<c:showSerName val=\"0\"/><c:showPercent val=\"").append(percent ? 1 : 0).append("\"/>")
                    .append("<c:showBubbleSize val=\"0\"/>");
            if (category && percent) {
                xml.append("<c:separator> </c:separator>");
            }
            xml.append("<c:showLeaderLines val=\"0\"/></c:dLbls>");
            if (donut) {
                lost.add("its slice labels are written on the ring: Word sets a donut's labels on its slices, not "
                         + "outside them");
            }
        }

        private void valueLabels(NumberFormatSpec format, String position, double[] shown) {
            String code = excelCode(format, shown);
            xml.append("<c:dLbls>");
            if (code != null) {
                xml.append("<c:numFmt formatCode=\"").append(escaped(code)).append("\" sourceLinked=\"0\"/>");
            }
            xml.append(labelChip()).append(textBody(style.valueLabelTextStyle()))
                    .append("<c:dLblPos val=\"").append(position).append("\"/>")
                    .append("<c:showLegendKey val=\"0\"/><c:showVal val=\"1\"/><c:showCatName val=\"0\"/>")
                    .append("<c:showSerName val=\"0\"/><c:showPercent val=\"0\"/><c:showBubbleSize val=\"0\"/>")
                    .append("</c:dLbls>");
        }

        /** The page sets a label on a chip of the halo's colour, so a stroke through it does not cross the digits. */
        private String labelChip() {
            DocumentPaint halo = style.valueLabelHalo();
            return "<c:spPr>" + (halo == null ? "<a:noFill/>" : fill(halo.primaryColor()))
                   + "<a:ln><a:noFill/></a:ln></c:spPr>";
        }

        private void seriesHead(ChartData data, int s) {
            seriesHead(data, s, s);
        }

        private void seriesHead(ChartData data, int s, int index) {
            String name = data.series().get(s).name();
            xml.append("<c:idx val=\"").append(index).append("\"/><c:order val=\"").append(index).append("\"/>")
                    .append("<c:tx><c:strRef><c:f>").append(SHEET).append("!$").append(column(s + 1)).append("$1</c:f>")
                    .append("<c:strCache><c:ptCount val=\"1\"/><c:pt idx=\"0\"><c:v>").append(escaped(name))
                    .append("</c:v></c:pt></c:strCache></c:strRef></c:tx>");
        }

        /**
         * The series' categories and values, each a reference into the embedded workbook with its
         * cache. A pie draws no slice for a value of zero (the page refuses a negative one), and Word
         * would label one, so a pie's zero is written as no value.
         */
        private void categoriesAndValues(ChartData data, int s, boolean pie) {
            int count = data.categoryCount();
            xml.append("<c:cat><c:strRef><c:f>").append(SHEET).append("!$A$2:$A$").append(count + 1).append("</c:f>")
                    .append("<c:strCache><c:ptCount val=\"").append(count).append("\"/>");
            for (int c = 0; c < count; c++) {
                xml.append("<c:pt idx=\"").append(c).append("\"><c:v>").append(escaped(data.categories().get(c)))
                        .append("</c:v></c:pt>");
            }
            String col = column(s + 1);
            xml.append("</c:strCache></c:strRef></c:cat><c:val><c:numRef><c:f>").append(SHEET).append("!$").append(col)
                    .append("$2:$").append(col).append('$').append(count + 1).append("</c:f>")
                    .append("<c:numCache><c:formatCode>General</c:formatCode><c:ptCount val=\"").append(count).append("\"/>");
            List<Double> values = data.series().get(s).values();
            for (int c = 0; c < count; c++) {
                Double v = values.get(c);
                if (v != null && !(pie && v <= 0)) {
                    xml.append("<c:pt idx=\"").append(c).append("\"><c:v>").append(plain(v)).append("</c:v></c:pt>");
                }
            }
            xml.append("</c:numCache></c:numRef></c:val>");
        }

        private void categoryAxis(String side, boolean fromTheTop, boolean labels) {
            xml.append("<c:catAx><c:axId val=\"" + CATEGORY_AXIS + "\"/><c:scaling><c:orientation val=\"")
                    .append(fromTheTop ? "maxMin" : "minMax").append("\"/></c:scaling><c:delete val=\"0\"/>")
                    .append("<c:axPos val=\"").append(side).append("\"/>");
            DocumentStroke separators = style.grid() == null ? null : style.grid().vertical();
            if (separators != null) {
                xml.append("<c:majorGridlines><c:spPr>").append(stroke(separators)).append("</c:spPr></c:majorGridlines>");
            }
            xml.append("<c:numFmt formatCode=\"General\" sourceLinked=\"1\"/>")
                    .append("<c:majorTickMark val=\"none\"/><c:minorTickMark val=\"none\"/>")
                    .append("<c:tickLblPos val=\"").append(labels ? "nextTo" : "none").append("\"/>")
                    // The page draws no axis line.
                    .append("<c:spPr><a:noFill/><a:ln><a:noFill/></a:ln></c:spPr>")
                    .append(textBody(style.axisTextStyle()))
                    .append("<c:crossAx val=\"" + VALUE_AXIS + "\"/><c:crosses val=\"autoZero\"/>")
                    .append("<c:auto val=\"1\"/><c:lblAlgn val=\"ctr\"/><c:lblOffset val=\"100\"/>")
                    .append("<c:noMultiLvlLbl val=\"0\"/></c:catAx>");
        }

        private void valueAxis(String side, AxisSpec spec, NiceScale scale, boolean atTheFarEnd) {
            xml.append("<c:valAx><c:axId val=\"" + VALUE_AXIS + "\"/><c:scaling><c:orientation val=\"minMax\"/>")
                    .append("<c:max val=\"").append(plain(scale.niceMax())).append("\"/>")
                    .append("<c:min val=\"").append(plain(scale.niceMin())).append("\"/></c:scaling>")
                    .append("<c:delete val=\"0\"/><c:axPos val=\"").append(side).append("\"/>");
            DocumentStroke grid = style.grid() == null ? null : style.grid().horizontal();
            if (spec.showGridLines() && grid != null) {
                xml.append("<c:majorGridlines><c:spPr>").append(stroke(grid)).append("</c:spPr></c:majorGridlines>");
            }
            String code = excelCode(spec.format(), ticks(scale));
            xml.append("<c:numFmt formatCode=\"").append(escaped(code == null ? "General" : code))
                    .append("\" sourceLinked=\"0\"/>")
                    .append("<c:majorTickMark val=\"none\"/><c:minorTickMark val=\"none\"/>")
                    .append("<c:tickLblPos val=\"").append(spec.showTickLabels() ? "nextTo" : "none").append("\"/>")
                    .append("<c:spPr><a:noFill/><a:ln><a:noFill/></a:ln></c:spPr>")
                    .append(textBody(style.axisTextStyle()))
                    .append("<c:crossAx val=\"" + CATEGORY_AXIS + "\"/>")
                    .append("<c:crosses val=\"").append(atTheFarEnd ? "max" : "autoZero").append("\"/>")
                    .append("<c:crossBetween val=\"between\"/>")
                    .append("<c:majorUnit val=\"").append(plain(scale.tickStep())).append("\"/></c:valAx>");
        }

        /** The side the legend is written on; it lists the series, or a pie's categories. */
        private void legend(ChartSpec spec) {
            if (spec.legend() == LegendPosition.NONE) {
                return;
            }
            String side = switch (spec.legend()) {
                case TOP -> "t";
                case RIGHT -> "r";
                default -> "b";
            };
            xml.append("<c:legend><c:legendPos val=\"").append(side).append("\"/>");
            if (spec instanceof ChartSpec.Pie pie) {
                // The page lists a slice only where it draws one.
                List<Double> values = pie.data().series().get(0).values();
                for (int c = 0; c < values.size(); c++) {
                    Double v = values.get(c);
                    if (v == null || v <= 0) {
                        xml.append("<c:legendEntry><c:idx val=\"").append(c).append("\"/><c:delete val=\"1\"/></c:legendEntry>");
                    }
                }
            } else if (spec instanceof ChartSpec.Line line && line.area()) {
                // A series is listed once, by its line: its area's entry is taken out.
                for (int s = 0; s < line.data().seriesCount(); s++) {
                    xml.append("<c:legendEntry><c:idx val=\"").append(s).append("\"/><c:delete val=\"1\"/></c:legendEntry>");
                }
            }
            xml.append("<c:overlay val=\"0\"/>").append(NO_FRAME).append(textBody(style.legendTextStyle()))
                    .append("</c:legend>");
        }

        /** A text body whose default run is the style: its size, weight, colour and family. */
        private String textBody(DocumentTextStyle text) {
            return "<c:txPr><a:bodyPr/><a:lstStyle/><a:p><a:pPr>" + runProperties("a:defRPr", "", text)
                   + "</a:pPr><a:endParaRPr lang=\"en-US\"/></a:p></c:txPr>";
        }

        /** A run's properties, {@code <tag ...>}: the style's size, weight, colour and family. */
        private String runProperties(String tag, String attributes, DocumentTextStyle text) {
            StringBuilder run = new StringBuilder("<").append(tag).append(attributes);
            if (text != null && text.size() > 0) {
                // DrawingML holds a size from 1pt to 4000pt, in hundredths.
                run.append(" sz=\"").append(Math.max(100, Math.min(400_000, Math.round(text.size() * 100))))
                        .append('"');
            }
            DocumentTextDecoration decoration = text == null ? null : text.decoration();
            boolean bold = decoration == DocumentTextDecoration.BOLD || decoration == DocumentTextDecoration.BOLD_ITALIC;
            boolean italic = decoration == DocumentTextDecoration.ITALIC
                             || decoration == DocumentTextDecoration.BOLD_ITALIC;
            run.append(" b=\"").append(bold ? 1 : 0).append("\" i=\"").append(italic ? 1 : 0).append('"');
            if (decoration == DocumentTextDecoration.UNDERLINE) {
                run.append(" u=\"sng\"");
            } else if (decoration == DocumentTextDecoration.STRIKETHROUGH) {
                run.append(" strike=\"sngStrike\"");
            }
            run.append('>');
            if (text != null && text.color() != null) {
                run.append(fill(text.color()));
            }
            String face = text == null ? null : family.apply(text.fontName());
            if (face != null) {
                String typeface = escaped(face);
                run.append("<a:latin typeface=\"").append(typeface).append("\"/><a:ea typeface=\"").append(typeface)
                        .append("\"/><a:cs typeface=\"").append(typeface).append("\"/>");
            }
            return run.append("</").append(tag).append('>').toString();
        }

        private DocumentPaint paintOf(int index) {
            return style.paintForSeries(index, ChartDefaults.DEFAULT_THEME.palette());
        }

        /**
         * The number format a chart's numbers are shown in, as an Excel format code, or {@code null}
         * where the pattern has none. Excel's optional decimals ({@code #}) show as DecimalFormat's
         * do, but for one case: with no decimal it must show ({@code 0}), a whole number keeps its
         * decimal point, 1 reading "1.". So where the numbers shown are whole and fractional both,
         * they are written with as many fixed decimals as they need, and the note says so; whole,
         * with none; fractional, with the pattern as it is.
         *
         * @param shown the numbers shown in it: an axis's ticks, or a series' or a pie's values
         */
        private String excelCode(NumberFormatSpec format, double... shown) {
            String pattern = format.pattern();
            if (!pattern.matches("[#,]*0[0,]*(\\.0*#*)?")) {
                lost.add("its number format \"" + pattern + "\" is not written: Word shows its numbers as it would any");
                return null;
            }
            if (format.scaleDivisor() != 1.0) {
                lost.add("its numbers are shown unscaled: the format's divisor is not written");
            }
            java.text.DecimalFormat formatter = format.formatter();
            java.text.DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
            if (symbols.getDecimalSeparator() != '.' || symbols.getGroupingSeparator() != ',') {
                lost.add("its number format's separators are not written: Word shows the reader's own");
            }
            if (formatter.isGroupingUsed() && formatter.getGroupingSize() != 3) {
                lost.add("its digits are grouped in thousands: Excel groups no other way");
            }
            int dot = pattern.indexOf('.');
            String whole = dot < 0 ? pattern : pattern.substring(0, dot);
            String fraction = dot < 0 ? "" : pattern.substring(dot + 1);
            int least = (int) fraction.chars().filter(ch -> ch == '0').count();
            String code = whole;
            if (least > 0) {
                code = whole + "." + fraction;
            } else if (!fraction.isEmpty()) {
                int needed = decimalsNeeded(shown, fraction.length());
                boolean someWhole = java.util.Arrays.stream(shown).anyMatch(v -> decimalsNeeded(new double[]{v}, 1) == 0);
                boolean someNot = java.util.Arrays.stream(shown).anyMatch(v -> decimalsNeeded(new double[]{v}, 1) > 0);
                if (someWhole && someNot) {
                    code = whole + "." + "0".repeat(needed);
                    lost.add("its numbers are shown with " + needed + (needed == 1 ? " decimal" : " decimals")
                             + " each, where the page shows a whole number without one");
                } else if (someNot) {
                    code = whole + "." + fraction;
                }
            }
            return quoted(format.prefix()) + code + quoted(format.suffix());
        }

        /** A pie's percent labels: Word shows a slice's share as a fraction, so its code ends in {@code %}. */
        private String percentCode(NumberFormatSpec format, double[] shares) {
            String suffix = format.suffix();
            if (!suffix.startsWith("%")) {
                lost.add("its slices' shares are shown as percentages: the format's suffix is not one");
                return "0%";
            }
            NumberFormatSpec bare = new NumberFormatSpec(format.pattern(), format.locale(), format.prefix(),
                    "", format.scaleDivisor());
            String code = excelCode(bare, shares);
            return code == null ? "0%" : code + "%" + quoted(suffix.substring(1));
        }
    }

    /**
     * The value axis's scale as the page computes it: the data's range or a stack's sums, zero pulled
     * in where the axis asks for it, and the author's bounds (ChartLayoutSupport.domain).
     *
     * @param floorAtZero whether a stack's floor is held at zero whatever the author's bounds, as
     *                    ChartLayoutSupport.computeFrame holds a vertical stack's; the horizontal bar
     *                    chart's own frame holds none
     */
    static NiceScale scaleOf(ChartData data, AxisSpec axis, boolean stacked, boolean floorAtZero) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        if (stacked) {
            for (int c = 0; c < data.categoryCount(); c++) {
                double sum = 0.0;
                for (ChartData.Series s : data.series()) {
                    Double v = s.values().get(c);
                    if (v != null && v > 0) {
                        sum += v;
                    }
                }
                min = Math.min(min, sum);
                max = Math.max(max, sum);
            }
        } else {
            for (ChartData.Series s : data.series()) {
                for (Double v : s.values()) {
                    if (v != null) {
                        min = Math.min(min, v);
                        max = Math.max(max, v);
                    }
                }
            }
        }
        if (Double.isInfinite(min) || Double.isInfinite(max)) {
            min = 0.0;
            max = 1.0;
        }
        if (axis.baselineAtZero()) {
            min = Math.min(min, 0.0);
            max = Math.max(max, 0.0);
        }
        if (axis.min() != null) {
            min = axis.min();
        }
        if (axis.max() != null) {
            max = axis.max();
        }
        if (floorAtZero && min != 0.0) {
            min = 0.0;
        }
        return NiceScale.compute(min, max, axis.baselineAtZero() && axis.min() == null, ChartDefaults.TARGET_TICKS);
    }

    /**
     * Word's first slice angle, clockwise from twelve o'clock, for the page's start angle, counted
     * the mathematical way from three o'clock.
     */
    static int firstSliceAngle(ChartSpec.Pie pie) {
        return (int) Math.floorMod(Math.round(90 - pie.startAngleDegrees()), 360L);
    }

    private static int decimalsNeeded(double[] shown, int most) {
        if (shown.length == 0) {
            return most;
        }
        int needed = 0;
        for (double value : shown) {
            int d = 0;
            while (d < most && Math.abs(value * Math.pow(10, d) - Math.rint(value * Math.pow(10, d)))
                               > 1e-9 * Math.max(1, Math.abs(value * Math.pow(10, d)))) {
                d++;
            }
            needed = Math.max(needed, d);
        }
        return needed;
    }

    /** The values of a series there are, in order. */
    private static double[] shown(List<Double> values) {
        return values.stream().filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).toArray();
    }

    private static double[] ticks(NiceScale scale) {
        double[] ticks = new double[scale.tickCount()];
        for (int i = 0; i < ticks.length; i++) {
            ticks[i] = scale.niceMin() + i * scale.tickStep();
        }
        return ticks;
    }

    /** A gap as Word writes it, a percentage of a bar's width, held to what Word takes. */
    private static long gapWidth(double barsWide) {
        return Math.max(0, Math.min(WIDEST_GAP, Math.round(barsWide * 100)));
    }

    private static String fill(DocumentColor colour) {
        java.awt.Color awt = colour.color();
        String rgb = String.format(Locale.ROOT, "%02X%02X%02X", awt.getRed(), awt.getGreen(), awt.getBlue());
        return "<a:solidFill><a:srgbClr val=\"" + rgb + "\""
               + (awt.getAlpha() < 255 ? "><a:alpha val=\"" + Math.round(awt.getAlpha() / 255.0 * 100_000)
                                         + "\"/></a:srgbClr>" : "/>")
               + "</a:solidFill>";
    }

    private static String stroke(DocumentStroke stroke) {
        return "<a:ln w=\"" + Units.toEMU(stroke.width()) + "\">" + fill(stroke.color()) + "</a:ln>";
    }

    /** A spreadsheet column's letters, 0 being A. */
    private static String column(int index) {
        StringBuilder letters = new StringBuilder();
        for (int n = index + 1; n > 0; n = (n - 1) / 26) {
            letters.insert(0, (char) ('A' + (n - 1) % 26));
        }
        return letters.toString();
    }

    /** A share of the chart's box, as the chart part writes one. */
    private static String fraction(double share) {
        return String.format(Locale.ROOT, "%.5f", share);
    }

    private static String plain(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String quoted(String literal) {
        return literal.isEmpty() ? "" : "\"" + literal.replace("\"", "\"\"") + "\"";
    }

    private static String escaped(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                default -> out.append(ch < 0x20 && ch != '\t' && ch != '\n' && ch != '\r' ? ' ' : ch);
            }
        }
        return out.toString();
    }
}
