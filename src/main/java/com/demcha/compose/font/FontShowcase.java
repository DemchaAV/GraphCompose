package com.demcha.compose.font;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ModuleBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

/**
 * Generates a preview PDF for bundled or custom font families so callers can
 * visually inspect the catalog.
 *
 * @author Artem Demchyshyn
 */
public final class FontShowcase {

    private static final String SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog 0123456789";

    private FontShowcase() {
    }

    public static void renderAvailableFontsPreview(Path outputFile) throws Exception {
        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(28, 28, 28, 28)
                .markdown(false)
                .create()) {

            buildShowcase(document, DefaultFonts.bundledFontNames());
            document.buildPdf();
        }
    }

    public static byte[] renderAvailableFontsPreview() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PDRectangle.A4)
                .margin(28, 28, 28, 28)
                .markdown(false)
                .create()) {

            buildShowcase(document, DefaultFonts.bundledFontNames());
            return document.toPdfBytes();
        }
    }

    public static byte[] renderFontsPreview(Collection<FontName> fonts) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PDRectangle.A4)
                .margin(28, 28, 28, 28)
                .markdown(false)
                .create()) {

            buildShowcase(document, fonts);
            return document.toPdfBytes();
        }
    }

    public static void buildShowcase(DocumentSession document, Collection<FontName> fonts) {
        Objects.requireNonNull(document, "document").pageFlow(flow -> {
            flow.name("AvailableFontsPreview")
                    .spacing(16)
                    .addParagraph(paragraph -> paragraph
                            .name("AvailableFontsPreviewTitle")
                            .text("Available Fonts Preview")
                            .textStyle(style(FontName.HELVETICA, 22, DocumentTextDecoration.BOLD, new Color(34, 34, 34)))
                            .margin(DocumentInsets.bottom(6)))
                    .addParagraph(paragraph -> paragraph
                            .name("AvailableFontsPreviewSubtitle")
                            .text("Each section shows the family name and sample lines in regular, bold, italic and bold-italic styles.")
                            .textStyle(style(FontName.HELVETICA, 10, DocumentTextDecoration.DEFAULT, new Color(90, 90, 90)))
                            .margin(DocumentInsets.bottom(12)));

            Collection<FontName> safeFonts = fonts == null ? DefaultFonts.bundledFontNames() : fonts;
            for (FontName fontName : safeFonts) {
                addFontSection(flow, fontName);
            }
        });
    }

    private static void addFontSection(PageFlowBuilder flow, FontName fontName) {
        flow.module(fontName.name(), module -> {
            module.name("FontSection_" + fontName.name())
                    .spacing(2)
                    .margin(DocumentInsets.bottom(8))
                    .titleStyle(style(FontName.HELVETICA, 12, DocumentTextDecoration.BOLD, new Color(25, 25, 25)))
                    .titleMargin(DocumentInsets.bottom(2));
            sampleLine(module, "Regular: ", fontName, DocumentTextDecoration.DEFAULT, DocumentInsets.bottom(1));
            sampleLine(module, "Bold: ", fontName, DocumentTextDecoration.BOLD, DocumentInsets.bottom(1));
            sampleLine(module, "Italic: ", fontName, DocumentTextDecoration.ITALIC, DocumentInsets.bottom(1));
            sampleLine(module, "Bold Italic: ", fontName, DocumentTextDecoration.BOLD_ITALIC, DocumentInsets.bottom(4));
        });
    }

    private static void sampleLine(ModuleBuilder module,
                                   String label,
                                   FontName fontName,
                                   DocumentTextDecoration decoration,
                                   DocumentInsets margin) {
        module.paragraph(paragraph -> paragraph
                .text(label + SAMPLE_TEXT)
                .textStyle(style(fontName, 11, decoration, Color.BLACK))
                .margin(margin));
    }

    private static DocumentTextStyle style(FontName fontName, double size, DocumentTextDecoration decoration, Color color) {
        return new DocumentTextStyle(fontName, size, decoration, DocumentColor.of(color));
    }
}
