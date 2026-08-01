package com.demcha.examples.support;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

public final class ExampleOutputPaths {
    private ExampleOutputPaths() {
    }

    /** The tree every example writes into. */
    public static Path root() {
        return baseDirectory().resolve("target").resolve("generated-pdfs")
                .toAbsolutePath().normalize();
    }

    /**
     * Empties the output tree so what remains is exactly what this run produced.
     *
     * <p>The examples only write. Rename an example's output or delete the example and
     * its old file stays behind, indistinguishable from a current one — the showcase
     * then publishes a document nothing produces, and the guards that read the tree
     * count it as evidence. Clearing first is what makes the tree a statement about the
     * code rather than about the order in which commands were run.</p>
     *
     * @return the emptied root
     */
    public static Path clean() throws IOException {
        Path root = root();
        if (Files.isDirectory(root)) {
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    if (!path.equals(root)) {
                        delete(path);
                    }
                }
            }
        }
        Files.createDirectories(root);
        return root;
    }

    /**
     * Deletes one entry, translating the failure a reader can actually act on.
     *
     * <p>A document held open in a viewer is the way this fails on Windows, and it is
     * common enough that the runbook lists it. The raw {@code AccessDeniedException}
     * arrives with a path and nothing else, at the very start of a run that has produced
     * nothing yet, which reads like the generator is broken rather than like a window
     * needs closing.</p>
     */
    private static void delete(Path path) throws IOException {
        try {
            Files.delete(path);
        } catch (AccessDeniedException blocked) {
            throw new IOException("Cannot clear " + path + " before regenerating the examples."
                    + " Something is holding the file open — a PDF viewer is the usual answer on"
                    + " Windows. Close it and run again.", blocked);
        }
    }

    public static Path prepare(String fileName) throws Exception {
        Path root = root();
        Files.createDirectories(root);
        return root.resolve(fileName).toAbsolutePath().normalize();
    }

    /**
     * Category-aware variant — emits the PDF under
     * {@code target/generated-pdfs/<category>/<fileName>}. Used by the
     * showcase reorg (v1.6) so CV / invoice / proposal / cover-letter /
     * feature samples land in their own subfolders, ready for the
     * static showcase site to consume.
     *
     * @param category subfolder slug (e.g. {@code "templates/cv"}, {@code "features/lists"})
     * @param fileName output file name including extension
     * @return absolute path under the categorised generated-pdfs tree
     */
    public static Path prepare(String category, String fileName) throws Exception {
        Path root = root().resolve(category);
        Files.createDirectories(root);
        return root.resolve(fileName).toAbsolutePath().normalize();
    }

    private static Path baseDirectory() {
        try {
            Path classesDir = Paths.get(ExampleOutputPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path parent = classesDir.getParent();
            if (parent != null && parent.getParent() != null) {
                return parent.getParent().toAbsolutePath().normalize();
            }
        } catch (Exception ignored) {
            // Fall back to configured or current directory.
        }

        String configured = System.getProperty("graphcompose.examples.basedir");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        return Path.of(".").toAbsolutePath().normalize();
    }
}
