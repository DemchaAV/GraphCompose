package com.demcha.preview;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.devtool.DevToolPreviewFileProvider;
import com.demcha.compose.devtool.DevToolPreviewProvider;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.compose.testsupport.EngineComposerHarness;
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

    private EngineComposerHarness createComposer(Path outputPath) {
        return com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputPath)
                .pageSize(PDRectangle.A4)
//                .margin(32, 32, 32, 32)
                .guideLines(true)
                .markdown(true)
                .create();
    }

    private void composeDocument(EngineComposerHarness composer) {
        var cb = composer.componentBuilder();
        var theme = CvTheme.defaultTheme();
        double width = composer.canvas().innerWidth();

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
                .addChild(buildBodyBlock(cb, theme, """
                        This preview is compiled on save and rendered directly from an in-memory `PDDocument`.

                        Try changing:
                        - container spacing
                        - margins and padding
                        - text content
                        - line styles
                        - your own GraphCompose modules
                        """, width))
                .addChild(buildBodyBlock(cb, theme, """
                        Replace this provider with your real layout code once the tool is running.
                        The first page will refresh after each successful save.
                        Use the dev tool buttons to save or open the current PDF.
                        """, width))
                .build();
    }

    private Entity buildBodyBlock(ComponentBuilder cb, CvTheme theme, String text, double width) {
        TextStyle bodyStyle = theme.bodyTextStyle();
        return cb.blockText(Align.left(theme.spacing()), bodyStyle)
                .size(width, 2)
                .padding(0, 5, 0, 25)
                .text(cb.text()
                        .textWithAutoSize(text)
                        .textStyle(bodyStyle))
                .anchor(Anchor.left())
                .build();
    }
}
