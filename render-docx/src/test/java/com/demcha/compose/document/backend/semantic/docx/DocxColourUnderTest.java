package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageBackgroundFill;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PathBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.PolygonNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTransform;
import com.demcha.compose.document.style.ShapePoint;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The colour a translucent panel is flattened against, read off what the page paints under the
 * panel's centre before it: an ellipse, a polygon, a path and a table's cells in their fill colours
 * where they cover that point, and nothing of them where they do not; a gradient or a drawing under
 * a transform covering the point leaves the colour unknown, and the panel is then flattened against
 * the white it is written on. A picture beside the point leaves it known; one covering it is
 * {@code DocxTranslucencyTest}'s.
 *
 * <p>Each panel stands over a navy page, so a shape that does not cover the point shows as the
 * navy around it, and an unknown colour as the white the export falls back to. A channel is
 * {@code round(over × a + under × (1 − a))}, {@code a} the alpha over 255: half-blue
 * (0, 90, 200 at 128) gives {@code 7FACE3} over white, {@code 0E4184} over navy (28, 39, 64) and
 * {@code 644178} over red (200, 40, 40).</p>
 */
class DocxColourUnderTest {

    private static final DocumentColor NAVY = DocumentColor.rgb(28, 39, 64);
    private static final DocumentColor RED = DocumentColor.rgb(200, 40, 40);
    private static final DocumentColor HALF_BLUE = DocumentColor.rgba(0, 90, 200, 128);
    private static final String OVER_WHITE = "7FACE3";
    private static final String OVER_NAVY = "0E4184";
    private static final String OVER_RED = "644178";
    /** A diamond touching the middle of each side of its box. */
    private static final List<ShapePoint> DIAMOND = List.of(new ShapePoint(0.5, 0), new ShapePoint(1, 0.5),
            new ShapePoint(0.5, 1), new ShapePoint(0, 0.5));

    @Test
    void anEllipseUnderThePanelsCentreIsItsFillAndBesideItThePageShows() throws Exception {
        assertThat(panelOver(new EllipseBuilder().name("Disc").size(200, 80).fillColor(RED).build(), LayerAlign.CENTER))
                .isEqualTo(OVER_RED);
        assertThat(panelOver(new EllipseBuilder().name("Disc").size(20, 20).fillColor(RED).build(), LayerAlign.TOP_LEFT))
                .as("an ellipse in the stack's corner, clear of the centre").isEqualTo(OVER_NAVY);
    }

    @Test
    void anEllipseDrawnOnlyAsARingPaintsNothingUnderThePanel() throws Exception {
        assertThat(panelOver(new EllipseBuilder().name("Ring").size(200, 80)
                .stroke(DocumentStroke.of(RED, 1)).build(), LayerAlign.CENTER)).isEqualTo(OVER_NAVY);
    }

    @Test
    void aPolygonUnderThePanelsCentreIsItsFillAndBesideItThePageShows() throws Exception {
        assertThat(panelOver(polygon(200, 80), LayerAlign.CENTER)).isEqualTo(OVER_RED);
        assertThat(panelOver(polygon(20, 20), LayerAlign.TOP_LEFT)).isEqualTo(OVER_NAVY);
    }

    @Test
    void aPathUnderThePanelsCentreIsItsFillAndBesideItThePageShows() throws Exception {
        assertThat(panelOver(square(200, 80).fillColor(RED).build(), LayerAlign.CENTER)).isEqualTo(OVER_RED);
        assertThat(panelOver(square(20, 20).fillColor(RED).build(), LayerAlign.TOP_LEFT)).isEqualTo(OVER_NAVY);
    }

    @Test
    void aGradientBoxUnderThePanelLeavesTheColourUnknownAndBesideItThePageShows() throws Exception {
        // The layout carries a gradient as a paint with no one colour: the panel is flattened
        // against the white it is written on, not against the navy page under the gradient.
        assertThat(panelOver(new ShapeBuilder().name("Glow").size(200, 80)
                .fill(DocumentPaint.linear(RED, NAVY)).build(), LayerAlign.CENTER)).isEqualTo(OVER_WHITE);
        assertThat(panelOver(new ShapeBuilder().name("Glow").size(20, 20)
                .fill(DocumentPaint.linear(RED, NAVY)).build(), LayerAlign.TOP_LEFT)).isEqualTo(OVER_NAVY);
    }

    @Test
    void aGradientPathUnderThePanelLeavesTheColourUnknownAndBesideItThePageShows() throws Exception {
        assertThat(panelOver(square(200, 80).fill(DocumentPaint.linear(RED, NAVY)).build(), LayerAlign.CENTER))
                .isEqualTo(OVER_WHITE);
        assertThat(panelOver(square(20, 20).fill(DocumentPaint.linear(RED, NAVY)).build(), LayerAlign.TOP_LEFT))
                .isEqualTo(OVER_NAVY);
    }

    @Test
    void aFillDrawnUnderATransformLeavesTheColourUnknown() throws Exception {
        // Turned, the box's fill no longer stands in the box the layout gives it.
        assertThat(panelOver(new ShapeBuilder().name("Turned").size(200, 80).fillColor(RED)
                .transform(DocumentTransform.rotate(10)).build(), LayerAlign.CENTER)).isEqualTo(OVER_WHITE);
    }

    @Test
    void aTablesCellIsItsFillWhereItCoversThePoint() throws Exception {
        DocumentTableStyle red = DocumentTableStyle.builder().fillColor(RED).build();
        assertThat(panelOver(table(200, red), LayerAlign.CENTER)).as("a red cell under the centre").isEqualTo(OVER_RED);
        // Beside the centre across, then above it: a bound across and a bound down are read.
        assertThat(panelOver(table(20, red), LayerAlign.CENTER_LEFT))
                .as("a red cell left of the centre").isEqualTo(OVER_NAVY);
        assertThat(panelOver(table(200, red), LayerAlign.TOP_CENTER))
                .as("a red cell above the centre").isEqualTo(OVER_NAVY);
    }

    @Test
    void aPictureBesideThePanelsCentreLeavesThePageShowing() throws Exception {
        assertThat(panelOver(picture(20, 20), LayerAlign.CENTER_LEFT)).as("left of the centre").isEqualTo(OVER_NAVY);
        assertThat(panelOver(picture(200, 20), LayerAlign.TOP_CENTER)).as("above the centre").isEqualTo(OVER_NAVY);
    }

    private static DocumentNode picture(double width, double height) {
        return new ImageBuilder().name("Photo").source(DocumentImageData.fromBytes(png())).size(width, height).build();
    }

    private static PolygonNode polygon(double width, double height) {
        return new PolygonNode("Diamond", width, height, DIAMOND, RED, null, DocumentInsets.zero(),
                DocumentInsets.zero());
    }

    private static PathBuilder square(double width, double height) {
        return new PathBuilder().name("Square").size(width, height)
                .moveTo(0, 0).lineTo(width, 0).lineTo(width, height).lineTo(0, height).closePath();
    }

    private static DocumentNode table(double width, DocumentTableStyle style) {
        return new TableBuilder().name("Under").columns(DocumentTableColumn.fixed(width))
                .rowCells(DocumentTableCell.text("U").withStyle(style)).build();
    }

    /**
     * The shading the panel is written with, standing over a navy page and a layer set in the
     * stack where it is aligned, a 200 by 80 frame giving the stack its size and the panel centred.
     */
    private static String panelOver(DocumentNode under, LayerAlign align) throws Exception {
        byte[] bytes;
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400).margin(DocumentInsets.of(20))
                .create()) {
            session.pageBackgrounds(List.of(PageBackgroundFill.fullPage(NAVY)));
            session.pageFlow(page -> page.addLayerStack(stack -> {
                stack.name("Stack").back(new ShapeBuilder().name("Frame").size(200, 80).build());
                stack.layer(under, align);
                stack.center(new SectionBuilder().name("Over").fillColor(HALF_BLUE).addParagraph("Over").build());
            }));
            bytes = session.export(new DocxSemanticBackend(report::set));
        }
        // With no layout every panel is flattened against white: a fixture the page cannot lay out
        // would pass for an unknown colour.
        assertThat(report.get().bySubject()).as("laid out").doesNotContainKey("measured geometry");
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            List<String> shadings = new ArrayList<>();
            document.getTables().forEach(table -> collectShadings(table, shadings));
            // The panel is the one shaded cell holding its paragraph.
            assertThat(shadings).as("the panel's shading").hasSize(1);
            return shadings.get(0);
        }
    }

    private static void collectShadings(XWPFTable table, List<String> shadings) {
        table.getRows().forEach(row -> row.getTableCells().forEach(cell -> {
            String fill = shading(cell);
            if (fill != null && cell.getText().contains("Over")) {
                shadings.add(fill);
            }
            cell.getTables().forEach(nested -> collectShadings(nested, shadings));
        }));
    }

    private static String shading(XWPFTableCell cell) {
        var properties = cell.getCTTc().getTcPr();
        if (properties == null || !properties.isSetShd()) {
            return null;
        }
        Object fill = properties.getShd().getFill();
        if (fill instanceof byte[] bytes) {
            StringBuilder text = new StringBuilder();
            for (byte part : bytes) {
                text.append(String.format("%02X", part & 0xFF));
            }
            return text.toString();
        }
        return String.valueOf(fill);
    }

    private static byte[] png() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(20, 20,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
