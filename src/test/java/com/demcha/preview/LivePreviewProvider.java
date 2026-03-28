package com.demcha.preview;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.devtool.DevToolPreviewProvider;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.templates.CvTheme;
import com.demcha.templates.TemplateBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 * Edit this class and save it to refresh the preview window.
 */
public final class LivePreviewProvider implements DevToolPreviewProvider {

    @Override
    public PDDocument buildPreview() throws Exception {
        var composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
//                .margin(32, 32, 32, 32)
                .guideLines(true)
                .markdown(true)
                .create();

        try {
            ComponentBuilder cb = composer.componentBuilder();
            CvTheme theme = CvTheme.defaultTheme();
            TemplateBuilder template = TemplateBuilder.from(cb, theme);

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
                            """, composer.canvas().innerWidth()))
                    .build();

            return composer.toPDDocument();
        } catch (Exception ex) {
            composer.close();
            throw ex;
        }
    }
}
