package com.demcha.testing;

import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class VisualTestOutputs {

    private static final Path ROOT = Path.of("target", "visual-tests");
    private static final String PDF_EXTENSION = ".pdf";

    private VisualTestOutputs() {
    }

    public static Path preparePdf(String fileStem, String... folders) throws Exception {
        Path directory = ROOT;
        for (String folder : folders) {
            directory = directory.resolve(folder);
        }

        Files.createDirectories(directory);

        String fileName = fileStem.endsWith(PDF_EXTENSION) ? fileStem : fileStem + PDF_EXTENSION;
        Path outputFile = directory.resolve(fileName);

        try {
            Files.deleteIfExists(outputFile);
            return outputFile;
        } catch (FileSystemException ignored) {
            String safeStem = fileName.substring(0, fileName.length() - PDF_EXTENSION.length());
            return directory.resolve(safeStem + "_" + UUID.randomUUID() + PDF_EXTENSION);
        }
    }
}
