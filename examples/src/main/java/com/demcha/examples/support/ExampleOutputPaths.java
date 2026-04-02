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
