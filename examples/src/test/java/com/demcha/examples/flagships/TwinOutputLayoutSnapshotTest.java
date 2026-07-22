package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * Locks the geometry of the dual-output hook page after visual review. The
 * PDF and the PPTX twin consume the same resolved layout, so pinning the
 * layout graph guards both artifacts at once.
 */
class TwinOutputLayoutSnapshotTest {

    @Test
    void twinOutputLayoutMatchesBaseline() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.SLIDE_16_9)
                .pageBackground(DocumentColor.rgb(9, 12, 27))
                .margin(DocumentInsets.zero())
                .create()) {
            TwinOutputExample.compose(document);
            LayoutSnapshotAssertions.assertMatches(document,
                    Path.of("src", "test", "resources", "layout-snapshots"),
                    Path.of("target", "visual-tests", "layout-snapshots"),
                    "twin-output", "flagships");
        }
    }
}
