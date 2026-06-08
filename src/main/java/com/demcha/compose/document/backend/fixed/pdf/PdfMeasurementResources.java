package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.engine.measurement.FontLibraryTextMeasurementSystem;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontLibrary;

import java.util.Collection;

/**
 * PDF-backed text measurement resources used by the canonical layout pipeline.
 *
 * <p>The public session depends on this lifecycle wrapper instead of importing
 * PDFBox classes directly. The implementation remains in the PDF backend layer
 * until GraphCompose has a backend-neutral font measurement implementation.</p>
 */
public final class PdfMeasurementResources implements AutoCloseable {
    private final FontLibrary fontLibrary;
    private final TextMeasurementSystem textMeasurementSystem;

    private PdfMeasurementResources(FontLibrary fontLibrary,
                                    TextMeasurementSystem textMeasurementSystem) {
        this.fontLibrary = fontLibrary;
        this.textMeasurementSystem = textMeasurementSystem;
    }

    /**
     * Resolves built-in plus custom fonts for the measurement pipeline.
     *
     * <p>Binary families resolve to per-thread cached measurement fonts rather than
     * embedding a subset into a throwaway PDF document (Finding 4), so opening these
     * resources owns no {@link org.apache.pdfbox.pdmodel.PDDocument}. Measured
     * metrics are byte-identical to the render font library.</p>
     *
     * @param customFontFamilies document-local font families
     * @return owned measurement resources
     */
    public static PdfMeasurementResources open(Collection<FontFamilyDefinition> customFontFamilies) {
        FontLibrary fontLibrary = PdfFontLibraryFactory.measurementLibrary(customFontFamilies);
        TextMeasurementSystem measurement = new FontLibraryTextMeasurementSystem(fontLibrary, PdfFont.class);
        return new PdfMeasurementResources(fontLibrary, measurement);
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
    }
}
