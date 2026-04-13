package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.testing.layout.LayoutSnapshotAssertions;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryShowcaseRenderTest {
    private static final Path COVER_ASSET = Path.of("assets", "GraphComposeCover.png");
    private static final Color TITLE_COLOR = new Color(18, 40, 74);
    private static final Color ACCENT_COLOR = new Color(37, 128, 197);
    private static final Color MUTED_COLOR = new Color(86, 98, 120);
    private static final Color HEADER_FILL = new Color(226, 238, 250);
    private static final Color FIRST_COLUMN_FILL = new Color(245, 249, 253);
    private static final Color TABLE_BORDER = new Color(172, 188, 205);
    private static final Color TABLE_BORDER_STRONG = new Color(136, 157, 181);

    @Test
    void shouldRenderRepositoryShowcaseOnSinglePageWithoutGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("repository_showcase_render", "clean", "integration");

        renderRepositoryShowcase(outputFile, false);

        assertSinglePagePdfLooksValid(outputFile);
    }

    @Test
    void shouldRenderRepositoryShowcaseOnSinglePageWithGuideLines() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("repository_showcase_render_guides", "guides", "integration");

        renderRepositoryShowcase(outputFile, true);

        assertSinglePagePdfLooksValid(outputFile);
    }

    private void renderRepositoryShowcase(Path outputFile, boolean guideLines) throws Exception {
        assertThat(COVER_ASSET).exists();

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(16, 16, 16, 16)
                .markdown(true)
                .guideLines(guideLines)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            double contentWidth = composer.canvas().innerWidth();

            Entity title = text(cb,
                    "GraphCompose",
                    TextStyle.builder()
                            .fontName(FontName.POPPINS)
                            .size(33)
                            .decoration(TextDecoration.BOLD)
                            .color(TITLE_COLOR)
                            .build(),
                    Anchor.topCenter());

            Entity heroImage = cb.image()
                    .image(COVER_ASSET)
                    .fitToBounds(contentWidth - 12, 158)
                    .padding(Padding.of(4))
                    .anchor(Anchor.topCenter())
                    .build();

            Entity subtitle = text(cb,
                    "A declarative layout engine for programmatic document generation",
                    TextStyle.builder()
                            .fontName(FontName.IBM_PLEX_SERIF)
                            .size(11.5)
                            .decoration(TextDecoration.DEFAULT)
                            .color(MUTED_COLOR)
                            .build(),
                    Anchor.topCenter());

            Entity intro = paragraph(
                    cb,
                    List.of(
                            "This render test reuses the visual rhythm of `README.md`, but rebuilds the page in a clean light style using only **GraphCompose** primitives. " +
                                    "The composition intentionally shows centered layout, a cover image from `assets`, wrapped markdown text, accent lines, and a styled table on the same document."
                    ),
                    contentWidth - 18,
                    9.7,
                    Anchor.topCenter(),
                    BlockIndentStrategy.FIRST_LINE);

            Entity separatorTop = separator(cb, contentWidth * 0.9);
            Entity sectionWhat = text(cb, "What GraphCompose is", sectionStyle(), Anchor.topLeft());
            Entity overview = bulletParagraph(
                    cb,
                    List.of(
                            "**Builders** create `Entity` trees instead of drawing directly to absolute PDF coordinates.",
                            "**Layout systems** resolve size and placement before rendering starts.",
                            "**Rendering systems** convert the final geometry into PDF output."
                    ),
                    contentWidth - 12);

            Entity sectionBenchmark = text(cb, "Comparative Benchmark", sectionStyle(), Anchor.topLeft());
            Entity benchmarkLead = paragraph(
                    cb,
                    List.of("A compact benchmark table keeps the `README.md` spirit while also demonstrating `table()` with fills, borders, padding, and aligned columns."),
                    contentWidth - 10,
                    9.0,
                    Anchor.topLeft(),
                    BlockIndentStrategy.FIRST_LINE);
            Entity featuresTable = showcaseTable(cb, contentWidth);
            Entity sectionLines = text(cb, "Primitive Showcase", sectionStyle(), Anchor.topLeft());
            Entity lineGallery = lineGallery(cb);
            Entity closingNote = paragraph(
                    cb,
                    List.of(
                            "The same page can be rendered with guide lines enabled, so this test works both as a polished sample and as a practical visual debug artifact while `GraphCompose` evolves."
                    ),
                    contentWidth - 12,
                    8.9,
                    Anchor.topLeft(),
                    BlockIndentStrategy.FIRST_LINE);

            cb.vContainer(Align.middle(9))
                    .entityName("RepositoryShowcaseRoot")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(4))
                    .addChild(title)
                    .addChild(heroImage)
                    .addChild(subtitle)
                    .addChild(intro)
                    .addChild(separatorTop)
                    .addChild(sectionWhat)
                    .addChild(overview)
                    .addChild(sectionBenchmark)
                    .addChild(benchmarkLead)
                    .addChild(featuresTable)
                    .addChild(sectionLines)
                    .addChild(lineGallery)
                    .addChild(closingNote)
                    .build();

            if (!guideLines) {
                LayoutSnapshotAssertions.assertMatches(composer, "repository_showcase_render", "integration");
            }
            composer.build();
        }
    }

    private Entity showcaseTable(ComponentBuilder cb, double contentWidth) {
        TextStyle bodyStyle = TextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8.8)
                .decoration(TextDecoration.DEFAULT)
                .color(TITLE_COLOR)
                .build();

        TextStyle headerStyle = TextStyle.builder()
                .fontName(FontName.POPPINS)
                .size(8.9)
                .decoration(TextDecoration.BOLD)
                .color(TITLE_COLOR)
                .build();

        return cb.table()
                .entityName("RepositoryCapabilitiesTable")
                .anchor(Anchor.topCenter())
                .columns(
                        TableColumnSpec.fixed(144),
                        TableColumnSpec.fixed(112),
                        TableColumnSpec.fixed(112))
                .width(contentWidth - 6)
                .defaultCellStyle(TableCellStyle.builder()
                        .padding(Padding.of(5))
                        .fillColor(ComponentColor.WHITE)
                        .stroke(new Stroke(TABLE_BORDER, 1.1))
                        .textStyle(bodyStyle)
                        .textAnchor(Anchor.centerLeft())
                        .build())
                .rowStyle(0, TableCellStyle.builder()
                        .fillColor(HEADER_FILL)
                        .textStyle(headerStyle)
                        .build())
                .columnStyle(0, TableCellStyle.builder()
                        .fillColor(FIRST_COLUMN_FILL)
                        .stroke(new Stroke(TABLE_BORDER_STRONG, 1.5))
                        .build())
                .columnStyle(1, TableCellStyle.builder()
                        .textAnchor(Anchor.center())
                        .build())
                .columnStyle(2, TableCellStyle.builder()
                        .textAnchor(Anchor.center())
                        .build())
                .row("Library", "Avg Time (ms)", "Avg Heap (MB)")
                .row("GraphCompose", "2.89", "0.21")
                .row("iText 5 (Old)", "1.80", "0.16")
                .row("JasperReports", "4.50", "0.19")
                .build();
    }

    private Entity lineGallery(ComponentBuilder cb) {
        Entity horizontal = cb.line()
                .horizontal()
                .size(120, 14)
                .padding(Padding.of(3))
                .stroke(new Stroke(new Color(38, 91, 168), 2.5))
                .anchor(Anchor.center())
                .build();

        Entity vertical = cb.line()
                .vertical()
                .size(14, 46)
                .padding(Padding.of(3))
                .stroke(new Stroke(new Color(204, 121, 47), 2.5))
                .anchor(Anchor.center())
                .build();

        Entity diagonal = cb.line()
                .diagonalAscending()
                .size(104, 38)
                .padding(Padding.of(4))
                .stroke(new Stroke(new Color(50, 135, 94), 2.5))
                .anchor(Anchor.center())
                .build();

        return cb.hContainer(Align.middle(18))
                .entityName("RepositoryShowcaseLineGallery")
                .anchor(Anchor.topCenter())
                .margin(Margin.top(2))
                .addChild(horizontal)
                .addChild(vertical)
                .addChild(diagonal)
                .build();
    }

    private Entity bulletParagraph(ComponentBuilder cb, List<String> lines, double width) {
        TextStyle style = TextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9.4)
                .decoration(TextDecoration.DEFAULT)
                .color(TITLE_COLOR)
                .build();

        return cb.blockText(Align.left(5), style)
                .size(width, 2)
                .padding(0, 0, 0, 0)
                .strategy(BlockIndentStrategy.ALL_LINES)
                .bulletOffset("•")
                .text(lines, style, null, null)
                .anchor(Anchor.topLeft())
                .build();
    }

    private Entity paragraph(ComponentBuilder cb,
                             List<String> lines,
                             double width,
                             double fontSize,
                             Anchor anchor,
                             BlockIndentStrategy strategy) {
        TextStyle style = TextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(fontSize)
                .decoration(TextDecoration.DEFAULT)
                .color(MUTED_COLOR)
                .build();

        return cb.blockText(Align.left(4), style)
                .size(width, 2)
                .padding(0, 0, 0, 0)
                .strategy(strategy)
                .text(lines, style, null, null)
                .anchor(anchor)
                .build();
    }

    private Entity separator(ComponentBuilder cb, double width) {
        return cb.line()
                .horizontal()
                .size(width, 10)
                .padding(Padding.of(2))
                .stroke(new Stroke(ACCENT_COLOR, 1.9))
                .anchor(Anchor.topCenter())
                .build();
    }

    private Entity text(ComponentBuilder cb, String value, TextStyle style, Anchor anchor) {
        return cb.text()
                .textWithAutoSize(value)
                .textStyle(style)
                .anchor(anchor)
                .build();
    }

    private TextStyle sectionStyle() {
        return TextStyle.builder()
                .fontName(FontName.POPPINS)
                .size(12.2)
                .decoration(TextDecoration.BOLD)
                .color(TITLE_COLOR)
                .build();
    }

    private void assertSinglePagePdfLooksValid(Path outputFile) throws Exception {
        assertThat(outputFile).exists();
        assertThat(outputFile).isRegularFile();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isEqualTo(1);
        }
    }
}
