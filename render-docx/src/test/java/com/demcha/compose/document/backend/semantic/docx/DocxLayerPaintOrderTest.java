package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drawings stack in Word as the page paints them.
 *
 * <p>Word stacks a document's drawings by the place each is given, a later one over an earlier
 * one; the export gives each the place the page paints its fragment at, whatever order it writes
 * them in. Measured in Word and LibreOffice on Windows: a white dot given after a disc, the dot at
 * z-index 0 and the disc at 1, stood over the disc in both when the places followed the order the
 * export drew them in, where the page hides it under the disc; given the page's places, it stood
 * under it in both, in a stack and in a shape container that only draw, in a band of layers whose
 * text is written in the order given, and in a stack that only draws nested in such a band.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxLayerPaintOrderTest {

    private static final DocumentColor ACCENT = DocumentColor.rgb(30, 90, 200);
    private static final DocumentColor LIGHT = DocumentColor.rgb(220, 230, 245);
    private static final Pattern ANCHOR = Pattern.compile(
            "relativeHeight=\"(\\d+)\".*?<wp:extent cx=\"(\\d+)\"", Pattern.DOTALL);

    @Test
    void aStackThatOnlyDrawsStacksItsLayersByTheirZIndex() throws Exception {
        Exported exported = export(page -> page.addLayerStack(stack -> discAndDot(stack)));

        assertThat(exported.heightOf(8)).as("the dot under the disc").isLessThan(exported.heightOf(36));
    }

    @Test
    void aShapeContainerThatOnlyDrawsStacksItsLayersByTheirZIndex() throws Exception {
        Exported exported = export(page -> page.addContainer(container -> container.rectangle(80, 60)
                .fillColor(LIGHT)
                .layer(disc(36, ACCENT), LayerAlign.CENTER, 1)
                .layer(disc(8, DocumentColor.WHITE), LayerAlign.CENTER, 0)));

        assertThat(exported.heightOf(80)).as("the outline under its layers").isLessThan(exported.heightOf(8));
        assertThat(exported.heightOf(8)).as("the dot under the disc").isLessThan(exported.heightOf(36));
    }

    @Test
    void layersOfOneZIndexStackInTheOrderGiven() throws Exception {
        Exported exported = export(page -> page.addLayerStack(stack -> stack
                .layer(disc(36, ACCENT), LayerAlign.CENTER, 0)
                .layer(disc(8, DocumentColor.WHITE), LayerAlign.CENTER, 0)));

        assertThat(exported.heightOf(36)).isLessThan(exported.heightOf(8));
    }

    @Test
    void aBandStacksItsDrawnLayersByTheirZIndexWhileItsTextKeepsItsOrder() throws Exception {
        Exported exported = export(page -> page.addLayerStack(stack -> stack
                .layer(new ShapeBuilder().size(120, 40).fillColor(ACCENT).build(), LayerAlign.TOP_LEFT, 1)
                .layer(new ShapeBuilder().size(60, 20).fillColor(DocumentColor.WHITE).build(), LayerAlign.TOP_LEFT, 0)
                .position(new ParagraphBuilder().text("First").build(), 0, 10, LayerAlign.TOP_LEFT, 3)
                .position(new ParagraphBuilder().text("Second").build(), 0, 24, LayerAlign.TOP_LEFT, 2)));

        assertThat(exported.heightOf(60)).as("the white box under the accent one").isLessThan(exported.heightOf(120));
        assertThat(exported.document().getDocument().getBody().xmlText().indexOf(">First<"))
                .as("its text written in the order given")
                .isLessThan(exported.document().getDocument().getBody().xmlText().indexOf(">Second<"));
    }

    @Test
    void aStackThatOnlyDrawsInsideABandStacksByItsZIndex() throws Exception {
        Exported exported = export(page -> page.addLayerStack(stack -> stack
                .layer(new ParagraphBuilder().text("Experience").build(), LayerAlign.TOP_LEFT)
                .layer(new LayerStackBuilder().name("Icon").layer(disc(36, ACCENT), LayerAlign.CENTER, 1)
                        .layer(disc(8, DocumentColor.WHITE), LayerAlign.CENTER, 0).build(), LayerAlign.TOP_RIGHT)));

        assertThat(exported.heightOf(8)).as("the dot under the disc").isLessThan(exported.heightOf(36));
    }

    @Test
    void aStackComposedInATableCellStacksByItsZIndex() throws Exception {
        Exported exported = export(page -> page.addTable(table -> table
                .columns(com.demcha.compose.document.table.DocumentTableColumn.fixed(80),
                        com.demcha.compose.document.table.DocumentTableColumn.fixed(120))
                .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(
                                new LayerStackBuilder().layer(disc(36, ACCENT), LayerAlign.CENTER, 1)
                                        .layer(disc(8, DocumentColor.WHITE), LayerAlign.CENTER, 0).build()),
                        com.demcha.compose.document.table.DocumentTableCell.text("Beside"))));

        assertThat(exported.heightOf(8)).as("the dot under the disc").isLessThan(exported.heightOf(36));
    }

    @Test
    void aTableCellsDrawingStandsUnderALayerThePageLaysOverTheTable() throws Exception {
        // The bar is given first and drawn first, the table's cell drawing after it; the page paints
        // the table under the bar.
        Exported exported = export(page -> page.addLayerStack(stack -> stack
                .layer(new ShapeBuilder().size(200, 40).fillColor(LIGHT).build(), LayerAlign.TOP_LEFT, 1)
                .layer(new com.demcha.compose.document.dsl.TableBuilder()
                        .columns(com.demcha.compose.document.table.DocumentTableColumn.fixed(80),
                                com.demcha.compose.document.table.DocumentTableColumn.fixed(120))
                        .rowCells(com.demcha.compose.document.table.DocumentTableCell.node(disc(20, ACCENT)),
                                com.demcha.compose.document.table.DocumentTableCell.text("Beside"))
                        .build(), LayerAlign.TOP_LEFT, 0)));

        // The cell sizes its disc to its own width: the disc is the one drawing besides the bar.
        long bar = exported.heightOf(200);
        assertThat(exported.heights()).hasSize(2);
        assertThat(exported.heights().entrySet()).as("the cell's disc under the bar")
                .filteredOn(drawing -> drawing.getKey() != Units.toEMU(200))
                .singleElement().satisfies(disc -> assertThat(disc.getValue()).isLessThan(bar));
    }

    @Test
    void aTitleAndItsDatesStackTheirDrawingsByTheirZIndex() throws Exception {
        Exported exported = export(page -> page.addLayerStack(stack -> stack
                .layer(new ShapeBuilder().size(300, 18).fillColor(ACCENT).build(), LayerAlign.TOP_LEFT, 1)
                .layer(new ShapeBuilder().size(340, 22).fillColor(LIGHT).build(), LayerAlign.TOP_LEFT, 0)
                .layer(new ParagraphBuilder().text("Engineer").build(), LayerAlign.TOP_LEFT, 2)
                .layer(new ParagraphBuilder().text("2020 - 2024").build(), LayerAlign.TOP_RIGHT, 2)));

        assertThat(exported.heightOf(340)).as("the background under the highlight").isLessThan(exported.heightOf(300));
    }

    @Test
    void columnsStackTheirDrawnLayersWhereThePagePaintsThem() throws Exception {
        // A backdrop given first under the columns, and two marks over them, the later one under.
        Exported exported = export(page -> page.addLayerStack(stack -> stack.name("Columns")
                .layer(new ShapeBuilder().size(100, 50).fillColor(LIGHT).build(), LayerAlign.TOP_LEFT, 0)
                .layer(column("Sidebar", 0, 240, side -> side.addParagraph("Contact")
                        .addCircle(30, ACCENT)), LayerAlign.TOP_LEFT, 1)
                .layer(column("Main", 120, 0, main -> main.addParagraph("Experience")), LayerAlign.TOP_LEFT, 1)
                .layer(new ShapeBuilder().size(40, 12).fillColor(ACCENT).build(), LayerAlign.TOP_RIGHT, 3)
                .layer(new ShapeBuilder().size(20, 6).fillColor(DocumentColor.WHITE).build(), LayerAlign.TOP_RIGHT, 2)));

        assertThat(exported.document().getTables()).as("written as columns").hasSize(1);
        assertThat(exported.heightOf(100)).as("the backdrop under the sidebar's mark").isLessThan(exported.heightOf(30));
        assertThat(exported.heightOf(30)).as("the sidebar's mark under the marks over the columns")
                .isLessThan(exported.heightOf(20));
        assertThat(exported.heightOf(20)).as("the later mark under the earlier").isLessThan(exported.heightOf(40));
    }

    private static LayerStackBuilder discAndDot(LayerStackBuilder stack) {
        return stack.layer(disc(36, ACCENT), LayerAlign.CENTER, 1)
                .layer(disc(8, DocumentColor.WHITE), LayerAlign.CENTER, 0);
    }

    private static DocumentNode disc(double diameter, DocumentColor fill) {
        return new EllipseBuilder().circle(diameter).fillColor(fill).build();
    }

    private static DocumentNode column(String name, double insetLeft, double insetRight,
                                       Consumer<SectionBuilder> content) {
        SectionBuilder layer = new SectionBuilder();
        layer.name(name).spacing(0).padding(new DocumentInsets(0, insetRight, 0, insetLeft));
        layer.addSection(name + "Content", section -> content.accept(section.spacing(0)));
        return layer.build();
    }

    private record Exported(XWPFDocument document, List<DocxExportReport.Note> notes, Map<Long, Long> heights) {

        /** The stacking height of the drawing as wide as so many points. */
        long heightOf(double width) {
            Long height = heights.get(Units.toEMU(width) * 1L);
            assertThat(height).as("a drawing %spt wide, among drawings %s EMU wide", width, heights.keySet()).isNotNull();
            return height;
        }
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> {
                page.addParagraph("Before");
                content.accept(page);
                page.addParagraph("After");
            });
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        assertThat(report.get().notes()).noneMatch(note -> note.subject().equals("measured geometry"));
        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));
        Map<Long, Long> heights = new HashMap<>();
        Matcher anchor = ANCHOR.matcher(document.getDocument().xmlText());
        while (anchor.find()) {
            heights.put(Long.parseLong(anchor.group(2)), Long.parseLong(anchor.group(1)));
        }
        return new Exported(document, report.get().notes(), heights);
    }
}
