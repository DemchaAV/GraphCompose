package com.demcha.compose.font_library;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

class FontShowcaseLayoutSnapshotTest {

    @Test
    void shouldMatchAvailableFontsPreviewLayoutSnapshot() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(28, 28, 28, 28)
                .markdown(false)
                .create()) {
            FontShowcase.buildShowcase(composer, GraphCompose.availableFonts());
            LayoutSnapshotAssertions.assertMatches(composer, "fonts/available_fonts_preview");
        }
    }
}
