package com.demcha.compose.testing.layout;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.engine.debug.LayoutSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * Assertion helpers for deterministic post-layout snapshots.
 *
 * <p>The intended workflow is:</p>
 * <ol>
 *   <li>compose the document into the canonical {@link DocumentSession}</li>
 *   <li>call one of the {@code assertMatches(...)} overloads</li>
 *   <li>optionally render the same session to PDF for human inspection</li>
 * </ol>
 *
 * <p>By default, expected baselines are resolved under
 * {@code src/test/resources/layout-snapshots}. On mismatch, an
 * {@code .actual.json} artifact is written under
 * {@code target/visual-tests/layout-snapshots}. Local baseline updates are
 * opt-in via {@value #UPDATE_PROPERTY}.</p>
 *
 * @author Artem Demchyshyn
 */
public final class LayoutSnapshotAssertions {
    /**
     * System property that enables overwriting expected JSON baselines.
     */
    public static final String UPDATE_PROPERTY = "graphcompose.updateSnapshots";

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
     * @param document composed document whose layout should be snapshotted
     * @param snapshotPath logical snapshot path relative to the default snapshot root
     * @throws Exception if snapshot extraction or comparison fails
     */
    public static void assertMatches(DocumentSession document, String snapshotPath) throws Exception {
        SnapshotTarget target = parseSnapshotPath(snapshotPath);
        assertMatches(document, target.snapshotName(), target.folders());
    }

    /**
     * Resolves and compares a snapshot using an explicit file name plus folders.
     *
     * @param document composed document whose layout should be snapshotted
     * @param snapshotName file name without the {@code .json} suffix
     * @param folders optional folder segments under the default snapshot root
     * @throws Exception if snapshot extraction or comparison fails
     */
    public static void assertMatches(DocumentSession document, String snapshotName, String... folders) throws Exception {
        assertMatches(
                document.layoutSnapshot(),
                EXPECTED_ROOT,
                ACTUAL_ROOT,
                Boolean.getBoolean(UPDATE_PROPERTY),
                snapshotName,
                folders);
    }

    /**
     * Resolves and compares a snapshot using a slash-delimited logical path and
     * caller-provided baseline roots.
     *
     * @param document composed document whose layout should be snapshotted
     * @param expectedRoot root folder that stores committed JSON baselines
     * @param actualRoot root folder for mismatch artifacts
     * @param snapshotPath logical snapshot path relative to {@code expectedRoot}
     * @throws Exception if snapshot extraction or comparison fails
     */
    public static void assertMatches(DocumentSession document,
                                     Path expectedRoot,
                                     Path actualRoot,
                                     String snapshotPath) throws Exception {
        SnapshotTarget target = parseSnapshotPath(snapshotPath);
        assertMatches(document, expectedRoot, actualRoot, target.snapshotName(), target.folders());
    }

    /**
     * Resolves and compares a snapshot using an explicit file name plus folders
     * and caller-provided baseline roots.
     *
     * @param document composed document whose layout should be snapshotted
     * @param expectedRoot root folder that stores committed JSON baselines
     * @param actualRoot root folder for mismatch artifacts
     * @param snapshotName file name without the {@code .json} suffix
     * @param folders optional folder segments under {@code expectedRoot}
     * @throws Exception if snapshot extraction or comparison fails
     */
    public static void assertMatches(DocumentSession document,
                                     Path expectedRoot,
                                     Path actualRoot,
                                     String snapshotName,
                                     String... folders) throws Exception {
        assertMatches(
                document.layoutSnapshot(),
                expectedRoot,
                actualRoot,
                Boolean.getBoolean(UPDATE_PROPERTY),
                snapshotName,
                folders);
    }

    /**
     * Resolves and compares a precomputed snapshot using a slash-delimited
     * logical path.
     *
     * @param snapshot resolved snapshot to compare
     * @param snapshotPath logical snapshot path relative to the default snapshot root
     * @throws IOException if reading or writing snapshot files fails
     */
    public static void assertMatches(LayoutSnapshot snapshot, String snapshotPath) throws IOException {
        SnapshotTarget target = parseSnapshotPath(snapshotPath);
        assertMatches(snapshot, target.snapshotName(), target.folders());
    }

    /**
     * Resolves and compares a precomputed snapshot using an explicit file name
     * plus folders.
     *
     * @param snapshot resolved snapshot to compare
     * @param snapshotName file name without the {@code .json} suffix
     * @param folders optional folder segments under the default snapshot root
     * @throws IOException if reading or writing snapshot files fails
     */
    public static void assertMatches(LayoutSnapshot snapshot, String snapshotName, String... folders) throws IOException {
        assertMatches(
                snapshot,
                EXPECTED_ROOT,
                ACTUAL_ROOT,
                Boolean.getBoolean(UPDATE_PROPERTY),
                snapshotName,
                folders);
    }

    /**
     * Compares a precomputed layout snapshot against a slash-delimited logical
     * path under caller-provided baseline roots.
     *
     * @param snapshot resolved snapshot to compare
     * @param expectedRoot root folder that stores committed JSON baselines
     * @param actualRoot root folder for mismatch artifacts
     * @param snapshotPath logical snapshot path relative to {@code expectedRoot}
     * @throws IOException if reading or writing snapshot files fails
     */
    public static void assertMatches(LayoutSnapshot snapshot,
                                     Path expectedRoot,
                                     Path actualRoot,
                                     String snapshotPath) throws IOException {
        SnapshotTarget target = parseSnapshotPath(snapshotPath);
        assertMatches(snapshot, expectedRoot, actualRoot, Boolean.getBoolean(UPDATE_PROPERTY), target.snapshotName(), target.folders());
    }

    /**
     * Compares a precomputed layout snapshot against an explicit file name plus
     * folders under caller-provided baseline roots.
     *
     * @param snapshot resolved snapshot to compare
     * @param expectedRoot root folder that stores committed JSON baselines
     * @param actualRoot root folder for mismatch artifacts
     * @param snapshotName file name without the {@code .json} suffix
     * @param folders optional folder segments under {@code expectedRoot}
     * @throws IOException if reading or writing snapshot files fails
     */
    public static void assertMatches(LayoutSnapshot snapshot,
                                     Path expectedRoot,
                                     Path actualRoot,
                                     String snapshotName,
                                     String... folders) throws IOException {
        assertMatches(snapshot, expectedRoot, actualRoot, Boolean.getBoolean(UPDATE_PROPERTY), snapshotName, folders);
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
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(expectedRoot, "expectedRoot");
        Objects.requireNonNull(actualRoot, "actualRoot");
        Objects.requireNonNull(snapshotName, "snapshotName");

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
            throw new AssertionError("Missing expected layout snapshot at %s. Generated actual snapshot at %s. Re-run with -D%s=true to accept it."
                    .formatted(expectedPath.toAbsolutePath(), actualPath.toAbsolutePath(), UPDATE_PROPERTY));
        }

        String expectedJson = LayoutSnapshotJson.normalizeLineEndings(Files.readString(expectedPath));
        if (!expectedJson.endsWith("\n")) {
            expectedJson = expectedJson + "\n";
        }

        if (!expectedJson.equals(actualJson)) {
            writeFile(actualPath, actualJson);
            throw new AssertionError("Layout snapshot mismatch for %s. Expected: %s Actual: %s. Re-run with -D%s=true to update the baseline."
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
