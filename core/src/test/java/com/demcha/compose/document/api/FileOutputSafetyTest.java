package com.demcha.compose.document.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A render that fails must not destroy the document that was already there.
 *
 * <p>Rendering used to open the destination directly, which truncates it before
 * the backend has produced a byte — so a mid-render failure replaced a good
 * published document with an empty file. These tests pin the contract of the
 * {@link AtomicFileOutput} helper in isolation; the session-level path that a
 * caller actually reaches is covered by {@code DocumentOutputFileSafetyTest} in
 * the qa module, which needs a real backend to get as far as rendering.</p>
 */
class FileOutputSafetyTest {

    private static final String SENTINEL = "previously published document";

    @Test
    void aSuccessfulWriteReplacesTheDestinationAndRemovesTheScratchFile(@TempDir Path tempDir) throws Exception {
        Path published = tempDir.resolve("report.bin");
        Files.writeString(published, SENTINEL);

        AtomicFileOutput.write(published, output -> output.write("fresh".getBytes(StandardCharsets.UTF_8)));

        assertThat(Files.readString(published)).isEqualTo("fresh");
        assertThat(listFiles(tempDir)).containsExactly(published);
    }

    @Test
    void aWriterFailingMidStreamPublishesNothing(@TempDir Path tempDir) throws Exception {
        Path published = tempDir.resolve("report.bin");
        Files.writeString(published, SENTINEL);

        assertThatThrownBy(() -> AtomicFileOutput.write(published, output -> {
            output.write("partial".getBytes(StandardCharsets.UTF_8));
            throw new IllegalStateException("backend blew up halfway");
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("halfway");

        assertThat(Files.readString(published))
                .describedAs("half-written bytes must never reach the destination")
                .isEqualTo(SENTINEL);
        assertThat(listFiles(tempDir)).containsExactly(published);
    }

    @Test
    void writingCreatesTheDestinationWhenItDoesNotExistYet(@TempDir Path tempDir) throws Exception {
        Path published = tempDir.resolve("new-report.bin");

        AtomicFileOutput.write(published, output -> output.write("fresh".getBytes(StandardCharsets.UTF_8)));

        assertThat(Files.readString(published)).isEqualTo("fresh");
    }

    @Test
    void publishingKeepsThePermissionsTheDestinationAlreadyHad(@TempDir Path tempDir) throws Exception {
        assumePosix(tempDir);
        Path published = tempDir.resolve("report.bin");
        Files.writeString(published, SENTINEL);
        Set<PosixFilePermission> readableByAll = PosixFilePermissions.fromString("rw-r--r--");
        Files.setPosixFilePermissions(published, readableByAll);

        AtomicFileOutput.write(published, output -> output.write("fresh".getBytes(StandardCharsets.UTF_8)));

        assertThat(Files.getPosixFilePermissions(published))
                .describedAs("re-rendering a served document must not narrow it to owner-only")
                .isEqualTo(readableByAll);
    }

    @Test
    void aBrandNewDocumentIsNotPublishedAsOwnerOnly(@TempDir Path tempDir) throws Exception {
        assumePosix(tempDir);
        Path published = tempDir.resolve("new-report.bin");

        AtomicFileOutput.write(published, output -> output.write("fresh".getBytes(StandardCharsets.UTF_8)));

        assertThat(Files.getPosixFilePermissions(published))
                .describedAs("the scratch file's owner-only mode must not leak onto the destination")
                .contains(PosixFilePermission.OTHERS_READ);
    }

    private static void assumePosix(Path directory) {
        assumeTrue(directory.getFileSystem().supportedFileAttributeViews().contains("posix"),
                "POSIX permissions are not a concept on this filesystem");
    }

    @Test
    void aMissingParentDirectoryFailsWithAnActionableMessage(@TempDir Path tempDir) {
        Path published = tempDir.resolve("nested").resolve("report.bin");

        assertThatThrownBy(() -> AtomicFileOutput.write(published, OutputStream::flush))
                .isInstanceOf(NoSuchFileException.class)
                .hasMessageContaining("parent directory");
    }

    private static List<Path> listFiles(Path directory) throws IOException {
        try (var entries = Files.list(directory)) {
            return entries.sorted().toList();
        }
    }
}
