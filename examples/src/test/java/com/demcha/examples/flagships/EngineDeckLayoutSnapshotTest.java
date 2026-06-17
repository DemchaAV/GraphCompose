package com.demcha.examples.flagships;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * Locks the EngineDeck layout. Composes the exact pages {@code generate()} ships
 * (via {@link EngineDeckExample#compose}) and diffs the resolved geometry against
 * a committed baseline, so a layout regression shows up in a PR instead of in a
 * silently-changed PDF.
 *
 * <p>Re-bless after a deliberate layout change — or after refreshing the bundled
 * benchmark data (chart bar geometry is data-driven) — with
 * {@code ./mvnw -f examples/pom.xml test -Dtest=EngineDeckLayoutSnapshotTest
 * -Dgraphcompose.updateSnapshots=true}. Page size and margin mirror
 * {@code EngineDeckExample.generate()}.</p>
 */
class EngineDeckLayoutSnapshotTest {

    @Test
    void engineDeckLayoutMatchesBaseline() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4.landscape())
                .margin(16, 16, 30, 16)
                .create()) {
            EngineDeckExample.compose(document);
            LayoutSnapshotAssertions.assertMatches(document,
                    Path.of("src", "test", "resources", "layout-snapshots"),
                    Path.of("target", "visual-tests", "layout-snapshots"),
                    "engine-deck", "flagships");
        }
    }
}
