package com.demcha.smoke;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario 5 — the consumer testing support {@code graph-compose-testing}
 * resolves and its documented helper ({@code LayoutSnapshotAssertions}) works
 * end to end: extract a layout snapshot, write a baseline in update mode, then
 * assert the freshly rendered layout matches it. Baselines are written under
 * {@code target/} so nothing is left in the source tree.
 */
class TestingHelperTest {

    @Test
    void layoutSnapshotAssertionsRoundTrips() throws Exception {
        Path expected = Path.of("target", "smoke-snapshots", "expected");
        Path actual = Path.of("target", "smoke-snapshots", "actual");
        Path out = Files.createTempFile("gc-smoke-testing", ".pdf");

        try (DocumentSession document = GraphCompose.document(out)
                .pageSize(DocumentPageSize.A4)
                .margin(36f, 36f, 36f, 36f)
                .create()) {
            document.add(document.dsl().paragraph()
                    .text("graph-compose-testing layout snapshot smoke.")
                    .build());

            // 1) update mode: extract + serialize the snapshot to the baseline root.
            System.setProperty(LayoutSnapshotAssertions.UPDATE_PROPERTY, "true");
            LayoutSnapshotAssertions.assertMatches(document, expected, actual, "release-smoke");

            // 2) compare mode: the freshly rendered layout must match the baseline.
            System.setProperty(LayoutSnapshotAssertions.UPDATE_PROPERTY, "false");
            LayoutSnapshotAssertions.assertMatches(document, expected, actual, "release-smoke");
        } finally {
            System.clearProperty(LayoutSnapshotAssertions.UPDATE_PROPERTY);
        }

        assertThat(Files.exists(expected.resolve("release-smoke.json"))).isTrue();
    }
}
