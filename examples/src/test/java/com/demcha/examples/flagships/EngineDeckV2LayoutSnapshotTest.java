package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * Locks the geometry of the parallel module-first release deck after visual
 * review, including its shared full-bleed hero and all benchmark charts.
 */
class EngineDeckV2LayoutSnapshotTest {

    @Test
    void engineDeckV2LayoutMatchesBaseline() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(EngineDeckV2Example.PAGE_WIDTH, EngineDeckV2Example.PAGE_HEIGHT)
                .pageBackground(DocumentColor.rgb(246, 248, 252))
                .margin(34, 42, 28, 42)
                .create()) {
            EngineDeckV2Example.compose(document);
            LayoutSnapshotAssertions.assertMatches(document,
                    Path.of("src", "test", "resources", "layout-snapshots"),
                    Path.of("target", "visual-tests", "layout-snapshots"),
                    "engine-deck-v2", "flagships");
        }
    }
}
