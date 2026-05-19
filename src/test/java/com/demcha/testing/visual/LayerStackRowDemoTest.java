package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.testing.VisualTestOutputs;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end visual regression for R3: a {@code Row} inside a
 * {@code LayerStack} content layer renders successfully to PDF.
 *
 * <p>Reproduces the Noir corporate CV use case from the v1.6.2 audit
 * feedback: a full-width dark band sits as one layer, the document
 * body (sidebar column + main column) sits as a Row in the second
 * content layer. Pre-R3 this threw
 * {@code IllegalStateException("Row '...' cannot contain a nested
 * horizontal row")}; the relaxation in R3.b lets it render.</p>
 *
 * <p>Output PDF lands in
 * {@code target/visual-tests/layer-stack/LayerStackRowDemo.pdf} for
 * manual review.</p>
 *
 * @author Artem Demchyshyn
 */
class LayerStackRowDemoTest {

    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor DARK = DocumentColor.rgb(28, 32, 44);
    private static final DocumentColor SIDEBAR_BG = DocumentColor.rgb(244, 240, 232);

    @Test
    void layerStackWithRowContentLayerRendersFullCvShape() throws Exception {
        Path output = VisualTestOutputs.preparePdf("LayerStackRowDemo", "layer-stack");

        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(0))
                .create()) {

            // Layer 0: full-width dark hero band across the top of the
            // page. Pure background paint; no inner content needed.
            ShapeNode darkBand = new ShapeNode(
                    "DarkHeroBand",
                    595.0,
                    160.0,
                    new Color(DARK.color().getRed(), DARK.color().getGreen(), DARK.color().getBlue()),
                    DocumentStroke.of(DocumentColor.BLACK, 0),
                    DocumentInsets.zero(),
                    DocumentInsets.zero());

            // Layer 1: sidebar + main row as the page content layer.
            // Pre-R3 this row would have been rejected because the
            // layer slot was indistinguishable from a row band slot.
            RowNode contentRow = new RowNode(
                    "ContentRow",
                    List.of(
                            sidebarSection(),
                            mainSection()),
                    List.of(),
                    0.0,
                    DocumentInsets.zero(),
                    DocumentInsets.zero(),
                    null,
                    null,
                    null);

            LayerStackNode page = new LayerStackBuilder()
                    .name("NoirCorporateCv")
                    .layer(darkBand, LayerAlign.TOP_LEFT)
                    .layer(contentRow, LayerAlign.TOP_LEFT)
                    .build();

            document.add(page);

            Files.write(output, document.toPdfBytes());
        }

        byte[] bytes = Files.readAllBytes(output);
        assertThat(bytes)
                .describedAs("CV-style PDF should render to a real file")
                .hasSizeGreaterThan(500);
        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII))
                .describedAs("PDF must start with the %PDF- magic header")
                .isEqualTo("%PDF-");
    }

    private static SectionNode sidebarSection() {
        // Tinted sidebar column kept under page capacity (full A4 inner
        // height is 841.88977 pt; this section stays well under it).
        return new SectionNode(
                "Sidebar",
                List.of(
                        paragraph("CONTACT", THEME.text().h3()),
                        paragraph("hello@example.com", THEME.text().body()),
                        paragraph("+44 20 5555 1000", THEME.text().body())),
                6.0,
                DocumentInsets.of(20),
                DocumentInsets.zero(),
                SIDEBAR_BG,
                null);
    }

    private static SectionNode mainSection() {
        return new SectionNode(
                "Main",
                List.of(
                        paragraph("Jordan Rivera", THEME.text().h1()),
                        paragraph("Principal Engineer", THEME.text().h3()),
                        paragraph(
                                "Builds engine internals and document layouts that other "
                                        + "engineers can extend without reading the source.",
                                THEME.text().body())),
                10.0,
                DocumentInsets.of(30),
                DocumentInsets.zero(),
                null,
                null);
    }

    private static ParagraphNode paragraph(String text, DocumentTextStyle style) {
        return new ParagraphNode(
                "p",
                text,
                style,
                TextAlign.LEFT,
                1.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());
    }
}
