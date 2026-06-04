package com.demcha.examples.features.text;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Runnable showcase for inline shape runs ({@code @since 1.7.0}).
 *
 * <p>Geometric figures — rating dots, arrows, chevrons, diamonds, stars,
 * checkmarks, plus signs, regular polygons — drawn on the text baseline from
 * geometry (no font glyphs), used between text and as list bullets. Each row
 * pairs the rendered output with the {@code ParagraphBuilder} / {@code RichText}
 * call that produced it, so the PDF reads like a quick reference.</p>
 */
public final class InlineShapesExample {
    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final DocumentColor BRAND = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor ACCENT = DocumentColor.rgb(196, 153, 76);
    private static final DocumentColor GREEN = DocumentColor.rgb(34, 130, 92);
    private static final DocumentColor PANEL = DocumentColor.rgb(248, 244, 234);

    private InlineShapesExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/text", "inline-shapes.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(34, 34, 34, 34)
                .create()) {

            document.pageFlow()
                    .name("InlineShapesShowcase")
                    .spacing(14)
                    .addSection("Hero", section -> section
                            .softPanel(THEME.palette().surfaceMuted(), 10, 16)
                            .accentLeft(ACCENT, 4)
                            .spacing(6)
                            .addParagraph(p -> p
                                    .text("Inline shapes")
                                    .textStyle(THEME.text().h1())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Geometric figures drawn on the text baseline ")
                                    .accent("from geometry, not font glyphs", BRAND)
                                    .plain(" — between text and as list bullets, at any size and colour.")))
                    .addSection("Ratings", section -> labelledRow(section,
                            "dot(size, fill) — filled and outlined rating dots",
                            rich -> rich
                                    .plain("Java ")
                                    .dot(5, BRAND).dot(5, BRAND).dot(5, BRAND).dot(5, BRAND)
                                    .dot(5, null, DocumentStroke.of(BRAND, 0.6))
                                    .plain("     Kotlin ")
                                    .dot(5, BRAND).dot(5, BRAND).dot(5, BRAND)
                                    .dot(5, null, DocumentStroke.of(BRAND, 0.6))
                                    .dot(5, null, DocumentStroke.of(BRAND, 0.6))))
                    .addSection("Flows", section -> labelledRow(section,
                            "arrow(size, Direction, fill) — direction between text",
                            rich -> rich
                                    .plain("Draft ").arrow(8, ShapeOutline.Direction.RIGHT, ACCENT)
                                    .plain(" Review ").arrow(8, ShapeOutline.Direction.RIGHT, ACCENT)
                                    .plain(" Published")))
                    .addSection("Breadcrumb", section -> labelledRow(section,
                            "chevron(size, Direction, fill) — light directional separator",
                            rich -> rich
                                    .plain("Home ").chevron(6, ShapeOutline.Direction.RIGHT, MUTED)
                                    .plain(" Docs ").chevron(6, ShapeOutline.Direction.RIGHT, MUTED)
                                    .plain(" API ").chevron(6, ShapeOutline.Direction.RIGHT, MUTED)
                                    .plain(" InlineShapeRun")))
                    .addSection("Checklist", section -> section
                            .softPanel(PANEL, 6, 12)
                            .spacing(5)
                            .addParagraph(p -> p
                                    .text("shape(ShapeOutline.checkmark(...)/plus(...), fill) — checklist markers")
                                    .textStyle(caption())
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich.shape(ShapeOutline.checkmark(9, 9), GREEN)
                                    .plain("  Figures render from geometry"))
                            .addRich(rich -> rich.shape(ShapeOutline.checkmark(9, 9), GREEN)
                                    .plain("  They reuse the ShapeOutline taxonomy"))
                            .addRich(rich -> rich.shape(ShapeOutline.plus(9, 9), ACCENT)
                                    .plain("  A new figure is one factory away")))
                    .addSection("Bullets", section -> labelledRow(section,
                            "any ShapeOutline as a list bullet",
                            rich -> rich
                                    .diamond(7, ACCENT).plain(" Diamond     ")
                                    .star(8, ACCENT).plain(" Star     ")
                                    .triangle(7, BRAND).plain(" Triangle     ")
                                    .arrow(8, ShapeOutline.Direction.RIGHT, BRAND).plain(" Arrow     ")
                                    .shape(ShapeOutline.regularPolygon(8, 8, 6), MUTED).plain(" Hexagon")))
                    .addSection("Footer", section -> section
                            .accentTop(THEME.palette().rule(), 0.6)
                            .padding(new DocumentInsets(8, 0, 0, 0))
                            .addRich(rich -> rich
                                    .plain("Source: ")
                                    .style("examples/.../InlineShapesExample.java",
                                            DocumentTextStyle.builder()
                                                    .fontName(FontName.COURIER)
                                                    .size(8)
                                                    .color(MUTED)
                                                    .build())))
                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static void labelledRow(SectionBuilder section, String label, Consumer<RichText> body) {
        section
                .softPanel(PANEL, 6, 12)
                .spacing(4)
                .addParagraph(p -> p
                        .text(label)
                        .textStyle(caption())
                        .margin(DocumentInsets.zero()))
                .addRich(body::accept);
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(8.5)
                .color(MUTED)
                .build();
    }
}
