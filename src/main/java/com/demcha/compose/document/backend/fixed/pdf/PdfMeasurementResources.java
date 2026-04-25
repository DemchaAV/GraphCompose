package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.engine.measurement.FontLibraryTextMeasurementSystem;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontLibrary;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.util.Collection;

/**
 * PDF-backed text measurement resources used by the canonical layout pipeline.
 *
 * <p>The public session depends on this lifecycle wrapper instead of importing
 * PDFBox classes directly. The implementation remains in the PDF backend layer
 * until GraphCompose has a backend-neutral font measurement implementation.</p>
 */
public final class PdfMeasurementResources implements AutoCloseable {
    private final PDDocument document;
    private final FontLibrary fontLibrary;
    private final TextMeasurementSystem textMeasurementSystem;

    private PdfMeasurementResources(PDDocument document,
                                    FontLibrary fontLibrary,
                                    TextMeasurementSystem textMeasurementSystem) {
        this.document = document;
        this.fontLibrary = fontLibrary;
        this.textMeasurementSystem = textMeasurementSystem;
    }

    /**
     * Opens a fresh measurement document and resolves built-in plus custom fonts.
     *
     * @param customFontFamilies document-local font families
     * @return owned measurement resources
     */
    public static PdfMeasurementResources open(Collection<FontFamilyDefinition> customFontFamilies) {
        PDDocument document = new PDDocument();
        FontLibrary fontLibrary = PdfFontLibraryFactory.library(document, customFontFamilies);
        TextMeasurementSystem measurement = new FontLibraryTextMeasurementSystem(fontLibrary, PdfFont.class);
        return new PdfMeasurementResources(document, fontLibrary, measurement);
    }

    /**
     * Returns the resolved document font library.
     *
     * @return document-scoped fonts
     */
    public FontLibrary fontLibrary() {
        return fontLibrary;
    }

    /**
     * Returns the text measurement service backed by the PDF font metrics.
     *
     * @return text measurement service
     */
    public TextMeasurementSystem textMeasurementSystem() {
        return textMeasurementSystem;
    }

    @Override
    public void close() throws Exception {
        textMeasurementSystem.clearCaches();
        document.close();
    }
}
