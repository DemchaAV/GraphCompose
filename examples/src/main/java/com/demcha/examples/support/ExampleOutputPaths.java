package com.demcha.examples.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ExampleOutputPaths {
    private ExampleOutputPaths() {
    }

    public static Path prepare(String fileName) throws Exception {
        Path root = baseDirectory().resolve("target").resolve("generated-pdfs");
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
        Path root = baseDirectory().resolve("target").resolve("generated-pdfs").resolve(category);
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
