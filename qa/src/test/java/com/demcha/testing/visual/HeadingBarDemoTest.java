package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
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
 * Verifies {@code headingBar(...)} renders a filled title band: a brand-filled
 * bar paints a different pixel set than the default grey band. Writes a
 * human-review comparison PDF of the default / brand / outlined variants.
 */
class HeadingBarDemoTest {

    private static final DocumentColor BRAND = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor PAPER = DocumentColor.rgb(252, 248, 240);
    private static final PdfVisualRegression VISUAL = PdfVisualRegression.standard();

    @Test
    void filledHeadingBarRendersDifferentlyFromDefault() throws Exception {
        BufferedImage def = VISUAL.renderPages(bar(false)).get(0);
        BufferedImage filled = VISUAL.renderPages(bar(true)).get(0);

        ImageDiff.Result diff = ImageDiff.compare(def, filled, 6);
        assertThat(diff.mismatchedPixelCount())
                .as("a brand-filled heading bar must differ from the default grey band (%s)", diff.summary())
                .isGreaterThan(50L);

        Path out = Path.of("target/visual-tests/heading-bar/heading-bars.pdf");
        Files.createDirectories(out.getParent());
        Files.write(out, sheet());
    }

    private static byte[] bar(boolean brand) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(260, 70)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(16))
                .create()) {
            document.pageFlow()
                    .name("BarSheet")
                    .addSection("S", section -> {
                        if (brand) {
                            section.headingBar("EXPERIENCE", b -> b.fill(BRAND).textStyle(white()));
                        } else {
                            section.headingBar("EXPERIENCE");
                        }
                    })
                    .build();
            return document.toPdfBytes();
        }
    }

    private static byte[] sheet() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 230)
                .pageBackground(PAPER)
                .margin(DocumentInsets.of(18))
                .create()) {
            document.pageFlow()
                    .name("HeadingBars")
                    .spacing(10)
                    .headingBar("DEFAULT GREY BAND")
                    .headingBar("BRAND FILL", b -> b.fill(BRAND).textStyle(white()))
                    .headingBar("OUTLINED", b -> b
                            .fill(DocumentColor.WHITE)
                            .stroke(DocumentStroke.of(BRAND, 1.0)))
                    .build();
            return document.toPdfBytes();
        }
    }

    private static DocumentTextStyle white() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(11)
                .color(DocumentColor.WHITE)
                .build();
    }
}
