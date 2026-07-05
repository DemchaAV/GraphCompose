package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.compose.testing.visual.ImageDiff;
import com.demcha.compose.testing.visual.PdfVisualRegression;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@code LineBuilder.dashed(...)} actually changes the rendered stroke:
 * a dashed horizontal rule paints a different pixel set than the same solid
 * rule. Writes a human-review comparison PDF of solid / dashed / fine-dash /
 * dotted dividers.
 */
class LineDashedDemoTest {

    private static final DocumentColor INK = DocumentColor.rgb(20, 60, 75);
    private static final DocumentColor PAPER = DocumentColor.rgb(252, 248, 240);
    private static final DocumentColor MUTED = DocumentColor.rgb(102, 106, 118);
    private static final PdfVisualRegression VISUAL = PdfVisualRegression.standard();

    @Test
    void dashedRuleRendersDifferentlyFromSolid() throws Exception {
        BufferedImage solid = VISUAL.renderPages(rule(false)).get(0);
        BufferedImage dashed = VISUAL.renderPages(rule(true)).get(0);

        ImageDiff.Result diff = ImageDiff.compare(solid, dashed, 6);
        assertThat(diff.mismatchedPixelCount())
                .as("a dashed rule must paint a different pixel set than a solid rule (%s)", diff.summary())
                .isGreaterThan(50L);

        Path out = Path.of("target/visual-tests/line-dashed/dividers.pdf");
        Files.createDirectories(out.getParent());
        Files.write(out, comparisonSheet());
    }

    private static byte[] rule(boolean dashed) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(220, 60)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(16))
                .create()) {
            document.pageFlow()
                    .name("RuleSheet")
                    .addLine(line -> {
                        line.name("Rule").horizontal(188).thickness(3).color(INK);
                        if (dashed) {
                            line.dashed(8, 5);
                        }
                    })
                    .build();
            return document.toPdfBytes();
        }
    }

    private static byte[] comparisonSheet() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(320, 230)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow()
                    .name("Dividers")
                    .addParagraph(p -> p.text("solid (default)").textStyle(caption()).margin(DocumentInsets.zero()))
                    .addLine(l -> l.name("Solid").horizontal(280).thickness(2).color(INK))
                    .addParagraph(p -> p.text("dashed(8, 5)").textStyle(caption()).margin(DocumentInsets.top(10)))
                    .addLine(l -> l.name("Dashed").horizontal(280).thickness(2).color(INK).dashed(8, 5))
                    .addParagraph(p -> p.text("dashed()  // 3pt on, 2pt off").textStyle(caption()).margin(DocumentInsets.top(10)))
                    .addLine(l -> l.name("FineDash").horizontal(280).thickness(2).color(INK).dashed())
                    .addParagraph(p -> p.text("dashed(1, 4)  // dotted").textStyle(caption()).margin(DocumentInsets.top(10)))
                    .addLine(l -> l.name("Dotted").horizontal(280).thickness(2).color(INK).dashed(1, 4))
                    .build();
            return document.toPdfBytes();
        }
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8)
                .color(MUTED)
                .build();
    }
}
