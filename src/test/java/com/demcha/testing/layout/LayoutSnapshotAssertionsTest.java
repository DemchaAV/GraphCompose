package com.demcha.testing.layout;

import com.demcha.compose.layout_core.debug.LayoutCanvasSnapshot;
import com.demcha.compose.layout_core.debug.LayoutInsetsSnapshot;
import com.demcha.compose.layout_core.debug.LayoutNodeSnapshot;
import com.demcha.compose.layout_core.debug.LayoutSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LayoutSnapshotAssertionsTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteActualSnapshotWhenExpectedBaselineIsMissing() {
        Path expectedRoot = tempDir.resolve("expected");
        Path actualRoot = tempDir.resolve("actual");
        LayoutSnapshot snapshot = sampleSnapshot(10);

        assertThatThrownBy(() -> LayoutSnapshotAssertions.assertMatches(
                snapshot,
                expectedRoot,
                actualRoot,
                false,
                "missing_baseline",
                "integration"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Missing expected layout snapshot");

        assertThat(actualRoot.resolve("integration").resolve("missing_baseline.actual.json"))
                .exists()
                .isRegularFile();
    }

    @Test
    void shouldWriteActualSnapshotOnMismatchWithoutUpdatingExpected() throws Exception {
        Path expectedRoot = tempDir.resolve("expected");
        Path actualRoot = tempDir.resolve("actual");
        Path expectedFile = expectedRoot.resolve("integration").resolve("mismatch_case.json");

        Files.createDirectories(expectedFile.getParent());
        Files.writeString(expectedFile, LayoutSnapshotJson.toJson(sampleSnapshot(10)));

        assertThatThrownBy(() -> LayoutSnapshotAssertions.assertMatches(
                sampleSnapshot(20),
                expectedRoot,
                actualRoot,
                false,
                "mismatch_case",
                "integration"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Layout snapshot mismatch");

        assertThat(actualRoot.resolve("integration").resolve("mismatch_case.actual.json"))
                .exists()
                .isRegularFile();
        assertThat(Files.readString(expectedFile))
                .isEqualTo(LayoutSnapshotJson.toJson(sampleSnapshot(10)));
    }

    @Test
    void shouldRewriteExpectedSnapshotWhenUpdateModeIsEnabled() throws Exception {
        Path expectedRoot = tempDir.resolve("expected");
        Path actualRoot = tempDir.resolve("actual");
        Path expectedFile = expectedRoot.resolve("templates").resolve("updated_case.json");

        Files.createDirectories(expectedFile.getParent());
        Files.writeString(expectedFile, LayoutSnapshotJson.toJson(sampleSnapshot(10)));

        LayoutSnapshotAssertions.assertMatches(
                sampleSnapshot(30),
                expectedRoot,
                actualRoot,
                true,
                "updated_case",
                "templates");

        assertThat(Files.readString(expectedFile))
                .isEqualTo(LayoutSnapshotJson.toJson(sampleSnapshot(30)));
        assertThat(actualRoot.resolve("templates").resolve("updated_case.actual.json"))
                .doesNotExist();
    }

    @Test
    void shouldSupportSlashSeparatedSnapshotPath() throws Exception {
        Path expectedRoot = tempDir.resolve("expected");
        Path actualRoot = tempDir.resolve("actual");

        LayoutSnapshotAssertions.assertMatches(
                sampleSnapshot(40),
                expectedRoot,
                actualRoot,
                true,
                "templates/cv/example_snapshot");

        assertThat(expectedRoot.resolve("templates").resolve("cv").resolve("example_snapshot.json"))
                .exists()
                .isRegularFile();
    }

    private LayoutSnapshot sampleSnapshot(double placementY) {
        LayoutInsetsSnapshot insets = new LayoutInsetsSnapshot(1.0, 1.0, 1.0, 1.0);
        LayoutNodeSnapshot node = new LayoutNodeSnapshot(
                "Root[0]",
                "Root",
                "VContainer",
                null,
                0,
                1,
                1,
                10.0,
                20.0,
                10.0,
                placementY,
                100.0,
                50.0,
                0,
                0,
                100.0,
                50.0,
                insets,
                insets);

        return new LayoutSnapshot(
                "1.0",
                new LayoutCanvasSnapshot(595.0, 842.0, 555.0, 802.0, insets),
                1,
                List.of(node));
    }
}
