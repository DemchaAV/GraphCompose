package com.demcha.compose.font_library;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.components.components_builders.ModuleBuilder;
import com.demcha.compose.layout_core.components.components_builders.TextBuilder;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.core.PdfComposer;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Generates a preview PDF for bundled or custom font families so callers can
 * visually inspect the catalog.
 */
public final class FontShowcase {

    private static final String SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog 0123456789";

    private FontShowcase() {
    }

    public static void renderAvailableFontsPreview(Path outputFile) throws Exception {
        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(28, 28, 28, 28)
                .markdown(false)
                .create()) {

            buildShowcase(composer, DefaultFonts.bundledFontNames());
            composer.build();
        }
    }

    public static byte[] renderAvailableFontsPreview() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(28, 28, 28, 28)
                .markdown(false)
                .create()) {

            buildShowcase(composer, DefaultFonts.bundledFontNames());
            return composer.toBytes();
        }
    }

    public static byte[] renderFontsPreview(Collection<FontName> fonts) throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(28, 28, 28, 28)
                .markdown(false)
                .create()) {

            buildShowcase(composer, fonts);
            return composer.toBytes();
        }
    }

    public static void buildShowcase(PdfComposer composer, Collection<FontName> fonts) {
        EntityManager entityManager = composer.entityManager();

        Entity title = text(entityManager, "Available Fonts Preview", FontName.HELVETICA, 22, TextDecoration.BOLD,
                new Color(34, 34, 34), Margin.bottom(6));
        Entity subtitle = text(entityManager,
                "Each section shows the family name and sample lines in regular, bold, italic and bold-italic styles.",
                FontName.HELVETICA, 10, TextDecoration.DEFAULT, new Color(90, 90, 90), Margin.bottom(12));

        ModuleBuilder root = new ModuleBuilder(entityManager, Align.middle(16), composer.canvas())
                .entityName("AvailableFontsPreview")
                .anchor(Anchor.topLeft());

        root.addChild(title);
        root.addChild(subtitle);

        for (FontName fontName : fonts) {
            root.addChild(fontSection(entityManager, composer, fontName));
        }

        root.build();
    }

    private static Entity fontSection(EntityManager entityManager, PdfComposer composer, FontName fontName) {
        Entity label = text(entityManager, fontName.name(), FontName.HELVETICA, 12, TextDecoration.BOLD,
                new Color(25, 25, 25), Margin.bottom(2));
        Entity regular = text(entityManager, "Regular: " + SAMPLE_TEXT, fontName, 11, TextDecoration.DEFAULT,
                Color.BLACK, Margin.bottom(1));
        Entity bold = text(entityManager, "Bold: " + SAMPLE_TEXT, fontName, 11, TextDecoration.BOLD, Color.BLACK,
                Margin.bottom(1));
        Entity italic = text(entityManager, "Italic: " + SAMPLE_TEXT, fontName, 11, TextDecoration.ITALIC,
                Color.BLACK, Margin.bottom(1));
        Entity boldItalic = text(entityManager, "Bold Italic: " + SAMPLE_TEXT, fontName, 11,
                TextDecoration.BOLD_ITALIC, Color.BLACK, Margin.bottom(4));

        return new ModuleBuilder(entityManager, Align.middle(2), composer.canvas())
                .entityName("FontSection_" + fontName.name())
                .anchor(Anchor.topLeft())
                .margin(Margin.bottom(8))
                .addChild(label)
                .addChild(regular)
                .addChild(bold)
                .addChild(italic)
                .addChild(boldItalic)
                .build();
    }

    private static Entity text(EntityManager entityManager,
            String value,
            FontName fontName,
            double size,
            TextDecoration decoration,
            Color color,
            Margin margin) {

        return new TextBuilder(entityManager)
                .textWithAutoSize(value)
                .textStyle(new TextStyle(fontName, size, decoration, color))
                .anchor(Anchor.topLeft())
                .margin(margin)
                .build();
    }
}
