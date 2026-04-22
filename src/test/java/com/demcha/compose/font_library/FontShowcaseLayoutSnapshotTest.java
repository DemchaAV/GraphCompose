package com.demcha.compose.font_library;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

class FontShowcaseLayoutSnapshotTest {

    @Test
    void shouldMatchAvailableFontsPreviewLayoutSnapshot() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PDRectangle.A4)
                .margin(28, 28, 28, 28)
                .markdown(false)
                .create()) {
            FontShowcase.buildShowcase(document, GraphCompose.availableFonts());
            LayoutSnapshotAssertions.assertMatches(document, "fonts/available_fonts_preview");
        }
    }
}
