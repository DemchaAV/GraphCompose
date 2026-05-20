package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.testing.VisualTestOutputs;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Personal sandbox / scratch test for manual GraphCompose API experimentation.
 *
 * <p>Renders a minimal document to
 * {@code target/visual-tests/develop/Develop.pdf}. Edit {@link #scratch()}
 * freely to try new helpers, layouts, theme tweaks, or upcoming v1.7+ APIs.
 * Open the rendered PDF in any viewer to see the result.</p>
 *
 * <p><b>Unicode safety</b>: until v1.6.2 ships (R1 glyph sanitizer), keep
 * to ASCII in the body. The current PDF font path crashes on any glyph
 * that Helvetica's WinAnsiEncoding cannot encode (arrows, dots, emoji).
 * After v1.6.2: replace ASCII placeholders with actual symbols freely.</p>
 *
 * <p>Run from repo root:
 * <pre>{@code
 * mvnw test -Dtest=DevelopTest -pl .
 * }</pre>
 *
 * <p>This file is intentionally committed as a working sandbox; rewrite the
 * body of {@link #scratch()} as needed — the only stable contract is that
 * the test produces a {@code Develop.pdf} starting with the {@code %PDF-}
 * header.</p>
 *
 * @author Artem Demchyshyn
 */
class DevelopTest {

    private static final BusinessTheme THEME = BusinessTheme.modern();

    @Test
    void scratch() throws Exception {
        Path output = VisualTestOutputs.preparePdf("Develop", "develop");

        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .pageBackground(THEME.pageBackground())
                .margin(DocumentInsets.of(28))
                .create()) {

            document.pageFlow(page -> page
                    .addSection("Hero", section -> section
                            .softPanel(THEME.palette().surfaceMuted(), 10, 14)
                            .accentLeft(THEME.palette().accent(), 4)
                            .addParagraph(p -> p.text("DevelopTest")
                                    .textStyle(THEME.text().h1()))
                            .addParagraph(p -> p.text(
                                            "Scratch space for manual API experiments. "
                                                    + "Render -> open -> think -> iterate.")
                                    .textStyle(THEME.text().body())))

                    .addSection("Body", section -> section
                            .addParagraph(p -> p.text(
                                            "Replace this body with anything you want to try. "
                                                    + "After v1.6.2: emoji, dots, unicode are safe. "
                                                    + "After v1.7+: .h1(), .addFieldRow(), .twoColumn(), "
                                                    + ".addHeadingBar(), .ratingDots(), .pageTopBand().")
                                    .textStyle(THEME.text().body()))));

            Files.write(output, document.toPdfBytes());
        }

        byte[] bytes = Files.readAllBytes(output);
        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }
}
