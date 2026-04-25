package com.demcha.compose.font;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.junit.jupiter.api.Test;

class FontShowcaseLayoutSnapshotTest {

    @Test
    void shouldMatchAvailableFontsPreviewLayoutSnapshot() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(28, 28, 28, 28)
                .markdown(false)
                .create()) {
            FontShowcase.buildShowcase(document, GraphCompose.availableFonts());
            LayoutSnapshotAssertions.assertMatches(document, "fonts/available_fonts_preview");
        }
    }
}
