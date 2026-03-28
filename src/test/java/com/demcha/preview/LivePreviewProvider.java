package com.demcha.preview;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.devtool.DevToolPreviewFileProvider;
import com.demcha.compose.devtool.DevToolPreviewProvider;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.templates.CvTheme;
import com.demcha.templates.TemplateBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Edit this class and save it to refresh the preview window.
 */
public final class LivePreviewProvider implements DevToolPreviewProvider, DevToolPreviewFileProvider {
    private static final Path OUTPUT_PATH = Path.of("target", "live-preview", "Template1_test.pdf");

    @Override
    public PDDocument buildPreview() throws Exception {
        var composer = createComposer(null);

        try {
            composeDocument(composer);
            return composer.toPDDocument();
        } catch (Exception ex) {
            composer.close();
            throw ex;
        }
    }

    @Override
    public Path savePreviewDocument() throws Exception {
        Path outputPath = OUTPUT_PATH.toAbsolutePath().normalize();
        Files.createDirectories(outputPath.getParent());

        try (var composer = createComposer(outputPath)) {
            composeDocument(composer);
            composer.build();
        }

        return outputPath;
    }

    private PdfComposer createComposer(Path outputPath) {
        return GraphCompose.pdf(outputPath)
                .pageSize(PDRectangle.A4)
//                .margin(32, 32, 32, 32)
                .guideLines(true)
                .markdown(true)
                .create();
    }

    private void composeDocument(DocumentComposer composer) {
        var cb = composer.componentBuilder();
        var theme = CvTheme.defaultTheme();
        var template = TemplateBuilder.from(cb, theme);

        var headline = cb.text()
                .textWithAutoSize("GraphCompose Live Preview")
                .textStyle(theme.nameTextStyle())
                .anchor(Anchor.topCenter())
                .build();

        var subtitle = cb.text()
                .textWithAutoSize("Edit com.demcha.preview.LivePreviewProvider and save the file.")
                .textStyle(TextStyle.DEFAULT_STYLE)
                .anchor(Anchor.topCenter())
                .build();

        var divider = cb.line()
                .horizontal()
                .size(composer.canvas().innerWidth(), 12)
                .padding(Padding.of(4))
                .stroke(new Stroke(ComponentColor.ROYAL_BLUE, 2.5))
                .anchor(Anchor.topLeft())
                .build();

        cb.vContainer(Align.left(16))
                .entityName("LivePreviewRoot")
                .anchor(Anchor.topCenter())
                .margin(Margin.of(6))
                .addChild(headline)
                .addChild(subtitle)
                .addChild(divider)
                .addChild(template.blockText("""
                        This preview is compiled on save and rendered directly from an in-memory `PDDocument`.

                        Try changing:
                        - container spacing
                        - margins and padding
                        - text content
                        - line styles
                        - your own GraphCompose modules
                        """, composer.canvas().innerWidth()))
                .addChild(template.blockText("""
                        Replace this provider with your real layout code once the tool is running.
                        The first page will refresh after each successful save.
                        Use the dev tool buttons to save or open the current PDF.
                        """, composer.canvas().innerWidth()))
                .build();
    }
}
