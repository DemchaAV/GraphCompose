package com.demcha.compose.document.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Writes a rendered document to a file without destroying the previous
 * contents when the render fails.
 *
 * <p>Rendering straight into the destination truncates it the moment the
 * stream opens, long before the backend has produced a single byte. A render
 * that then fails — a missing backend, an unsupported payload, a full disk —
 * leaves the caller with an empty or half-written file where a good document
 * used to be. That matters most for the case this library is built for: a
 * server that overwrites a previously published document in place.</p>
 *
 * <p>So the bytes go to a temporary file in the destination's own directory
 * and are moved onto the destination only after the writer returns normally.
 * The move is atomic where the filesystem supports it, which also means a
 * concurrent reader never observes a partially written document. On failure
 * the temporary file is removed and the destination is left untouched.</p>
 *
 * <p><b>Permissions.</b> {@link Files#createTempFile} deliberately creates an
 * owner-only file, and {@link Files#move} carries those permissions onto the
 * destination — which would silently narrow a served file from world-readable
 * to owner-only. On POSIX filesystems the temporary file is therefore widened
 * before the move: to the permissions the destination already had when it
 * exists, and to {@code rw-r--r--} when it does not. Filesystems without POSIX
 * support (Windows) are unaffected.</p>
 *
 * <p><b>Symbolic links.</b> Publishing replaces the destination <em>entry</em>.
 * When the destination is a symlink, the link itself is replaced by the rendered
 * file rather than the render being written through it to the link's target.
 * Render to the real path when a symlink must survive.</p>
 *
 * @author Artem Demchyshyn
 */
final class AtomicFileOutput {

    private static final Logger LOG = LoggerFactory.getLogger("com.demcha.compose.document.lifecycle");

    private static final Set<PosixFilePermission> DEFAULT_PERMISSIONS =
            Set.copyOf(PosixFilePermissions.fromString("rw-r--r--"));

    private AtomicFileOutput() {
    }

    /**
     * Writes bytes into a caller-owned stream.
     */
    @FunctionalInterface
    interface StreamWriter {
        /**
         * @param output stream to write to; closed by the caller, not the writer
         * @throws Exception if rendering fails
         */
        void writeTo(OutputStream output) throws Exception;
    }

    /**
     * Renders through {@code writer} and publishes the result at {@code target}.
     *
     * @param target destination file, replaced only after a successful render
     * @param writer produces the document bytes
     * @throws NoSuchFileException if the destination's parent directory does not exist
     * @throws Exception           whatever the writer throws, with {@code target} untouched
     */
    static void write(Path target, StreamWriter writer) throws Exception {
        Path directory = target.toAbsolutePath().getParent();
        if (directory == null || !Files.isDirectory(directory)) {
            throw new NoSuchFileException(
                    target.toString(),
                    null,
                    "The parent directory does not exist. Create it before rendering.");
        }

        Path temporary = Files.createTempFile(directory, ".graphcompose-", ".tmp");
        boolean published = false;
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                writer.writeTo(output);
            }
            alignPermissions(temporary, target);
            move(temporary, target);
            published = true;
        } finally {
            if (!published) {
                discard(temporary);
            }
        }
    }

    /**
     * Removes the scratch file without masking the failure that caused it to be
     * abandoned. A cleanup {@link IOException} — a scanner still holding the
     * handle, a revoked mount — must not replace the render diagnostic the
     * caller actually needs.
     */
    private static void discard(Path temporary) {
        try {
            Files.deleteIfExists(temporary);
        } catch (IOException ex) {
            LOG.debug("document.output.scratch-cleanup-failed path={}", temporary, ex);
        }
    }

    private static void move(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            // Some network and FUSE filesystems reject ATOMIC_MOVE. Replacing
            // without the atomicity guarantee is still strictly better than
            // truncating the destination up front.
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void alignPermissions(Path temporary, Path target) {
        if (!temporary.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            return;
        }
        try {
            Set<PosixFilePermission> permissions = Files.exists(target)
                    ? Files.getPosixFilePermissions(target)
                    : DEFAULT_PERMISSIONS;
            Files.setPosixFilePermissions(temporary, permissions);
        } catch (IOException | UnsupportedOperationException ex) {
            // A render that succeeded must not fail over file metadata; the
            // document is still published, just with the temp file's mode.
        }
    }
}
