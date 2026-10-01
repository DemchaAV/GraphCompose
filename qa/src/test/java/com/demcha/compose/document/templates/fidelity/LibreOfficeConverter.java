package com.demcha.compose.document.templates.fidelity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Converts DOCX files to PDF with LibreOffice, headless, in one process for the whole corpus.
 *
 * <p>The process runs under a profile of its own, so a LibreOffice the user has open neither
 * blocks it nor is changed by it.</p>
 */
final class LibreOfficeConverter {

    private static final long TIMEOUT_MINUTES = 15;

    private final Path soffice;

    private LibreOfficeConverter(Path soffice) {
        this.soffice = soffice;
    }

    /**
     * The LibreOffice to convert with: {@code -Dgraphcompose.soffice}, then {@code SOFFICE}, then
     * {@code soffice} on the path, then the Windows default installation.
     */
    static Optional<LibreOfficeConverter> find() {
        List<String> candidates = new ArrayList<>();
        String property = System.getProperty("graphcompose.soffice");
        if (property != null && !property.isBlank()) {
            candidates.add(property);
        }
        String environment = System.getenv("SOFFICE");
        if (environment != null && !environment.isBlank()) {
            candidates.add(environment);
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String directory : path.split(java.io.File.pathSeparator)) {
                candidates.add(Path.of(directory, "soffice").toString());
                candidates.add(Path.of(directory, "soffice.exe").toString());
            }
        }
        candidates.add("C:/Program Files/LibreOffice/program/soffice.exe");
        return candidates.stream().map(Path::of).filter(Files::isRegularFile).findFirst().map(LibreOfficeConverter::new);
    }

    /** Converts each DOCX into a PDF of the same stem in {@code outDir}. */
    void convert(List<Path> docxFiles, Path outDir, Path workDir) throws IOException, InterruptedException {
        Files.createDirectories(outDir);
        Path profile = Files.createDirectories(workDir.resolve("libreoffice-profile"));
        List<String> command = new ArrayList<>(List.of(soffice.toString(), "--headless", "--norestore",
                "-env:UserInstallation=" + profile.toUri(), "--convert-to", "pdf", "--outdir", outDir.toString()));
        docxFiles.forEach(file -> command.add(file.toString()));
        Path log = workDir.resolve("libreoffice.log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new IllegalStateException("LibreOffice did not convert the corpus within "
                                            + TIMEOUT_MINUTES + " minutes; see " + log);
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("LibreOffice exited with " + process.exitValue() + "; see " + log);
        }
    }

    /** The operating system the baseline of a LibreOffice run belongs to. */
    static String platform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win") ? "windows" : os.contains("mac") ? "mac" : "linux";
    }
}
