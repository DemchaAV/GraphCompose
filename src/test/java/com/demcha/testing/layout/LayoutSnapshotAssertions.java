package com.demcha.testing.layout;

import com.demcha.compose.layout_core.debug.LayoutSnapshot;
import com.demcha.compose.layout_core.core.PdfComposer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.fail;

/**
 * Assertion helper for deterministic post-layout snapshots.
 *
 * <p>The intended workflow is:</p>
 * <ol>
 *   <li>compose the document into a {@link PdfComposer}</li>
 *   <li>call {@link #assertMatches(PdfComposer, String)} or
 *       {@link #assertMatches(PdfComposer, String, String...)}</li>
 *   <li>optionally render the same composer to PDF for human inspection</li>
 * </ol>
 *
 * <p>Expected baselines are committed under {@code src/test/resources/layout-snapshots}.
 * On mismatch, an {@code .actual.json} artifact is written under
 * {@code target/visual-tests/layout-snapshots}. Local baseline updates are opt-in via
 * {@code -Dgraphcompose.updateSnapshots=true}.</p>
 */
public final class LayoutSnapshotAssertions {
    static final String UPDATE_PROPERTY = "graphcompose.updateSnapshots";
    private static final Path EXPECTED_ROOT = Path.of("src", "test", "resources", "layout-snapshots");
    private static final Path ACTUAL_ROOT = Path.of("target", "visual-tests", "layout-snapshots");

    private LayoutSnapshotAssertions() {
    }

    /**
     * Resolves and compares a snapshot using a slash-delimited logical path.
     *
     * <p>For example, passing {@code templates/invoice/invoice_standard_layout}
     * compares against
     * {@code src/test/resources/layout-snapshots/templates/invoice/invoice_standard_layout.json}.</p>
     *
     * @param composer composed document whose layout should be snapshotted
     * @param snapshotPath logical snapshot path relative to the snapshot root
     * @throws Exception if snapshot extraction or comparison fails
     */
    public static void assertMatches(PdfComposer composer, String snapshotPath) throws Exception {
        SnapshotTarget target = parseSnapshotPath(snapshotPath);
        assertMatches(composer, target.snapshotName(), target.folders());
    }

    /**
     * Resolves and compares a snapshot using an explicit file name plus folders.
     *
     * <p>This overload is useful when tests already carry the folder structure as
     * separate values.</p>
     *
     * @param composer composed document whose layout should be snapshotted
     * @param snapshotName file name without the {@code .json} suffix
     * @param folders optional folder segments under the snapshot root
     * @throws Exception if snapshot extraction or comparison fails
     */
    public static void assertMatches(PdfComposer composer, String snapshotName, String... folders) throws Exception {
        assertMatches(
                composer.layoutSnapshot(),
                EXPECTED_ROOT,
                ACTUAL_ROOT,
                Boolean.getBoolean(UPDATE_PROPERTY),
                snapshotName,
                folders);
    }

    static void assertMatches(LayoutSnapshot snapshot,
                              Path expectedRoot,
                              Path actualRoot,
                              boolean updateSnapshots,
                              String snapshotPath) throws IOException {
        SnapshotTarget target = parseSnapshotPath(snapshotPath);
        assertMatches(snapshot, expectedRoot, actualRoot, updateSnapshots, target.snapshotName(), target.folders());
    }

    static void assertMatches(LayoutSnapshot snapshot,
                              Path expectedRoot,
                              Path actualRoot,
                              boolean updateSnapshots,
                              String snapshotName,
                              String... folders) throws IOException {
        String actualJson = LayoutSnapshotJson.toJson(snapshot);
        Path expectedPath = resolveExpectedPath(expectedRoot, snapshotName, folders);
        Path actualPath = resolveActualPath(actualRoot, snapshotName, folders);

        if (updateSnapshots) {
            writeFile(expectedPath, actualJson);
            Files.deleteIfExists(actualPath);
            return;
        }

        if (Files.notExists(expectedPath)) {
            writeFile(actualPath, actualJson);
            fail("Missing expected layout snapshot at %s. Generated actual snapshot at %s. Re-run with -D%s=true to accept it."
                    .formatted(expectedPath.toAbsolutePath(), actualPath.toAbsolutePath(), UPDATE_PROPERTY));
        }

        String expectedJson = LayoutSnapshotJson.normalizeLineEndings(Files.readString(expectedPath));
        if (!expectedJson.endsWith("\n")) {
            expectedJson = expectedJson + "\n";
        }

        if (!expectedJson.equals(actualJson)) {
            writeFile(actualPath, actualJson);
            fail("Layout snapshot mismatch for %s. Expected: %s Actual: %s. Re-run with -D%s=true to update the baseline."
                    .formatted(snapshotName, expectedPath.toAbsolutePath(), actualPath.toAbsolutePath(), UPDATE_PROPERTY));
        }

        Files.deleteIfExists(actualPath);
    }

    private static Path resolveExpectedPath(Path root, String snapshotName, String... folders) {
        return resolvePath(root, snapshotName + ".json", folders);
    }

    private static Path resolveActualPath(Path root, String snapshotName, String... folders) {
        return resolvePath(root, snapshotName + ".actual.json", folders);
    }

    private static Path resolvePath(Path root, String fileName, String... folders) {
        Path current = root;
        for (String folder : folders) {
            current = current.resolve(folder);
        }
        return current.resolve(fileName);
    }

    private static void writeFile(Path path, String content) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, content);
    }

    private static SnapshotTarget parseSnapshotPath(String snapshotPath) {
        String normalized = snapshotPath == null ? "" : snapshotPath.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("snapshotPath must not be blank");
        }

        String[] segments = Arrays.stream(normalized.split("/"))
                .map(String::trim)
                .filter(segment -> !segment.isBlank())
                .toArray(String[]::new);

        if (segments.length == 0) {
            throw new IllegalArgumentException("snapshotPath must contain at least one segment");
        }

        String snapshotName = segments[segments.length - 1];
        String[] folders = Arrays.copyOf(segments, segments.length - 1);
        return new SnapshotTarget(snapshotName, folders);
    }

    private record SnapshotTarget(String snapshotName, String[] folders) {
    }
}
