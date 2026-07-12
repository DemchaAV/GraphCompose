package com.demcha.compose.document.image;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Public image source descriptor for semantic image nodes.
 *
 * <p>The descriptor keeps authoring code independent from the internal image
 * cache and PDF backend. Bytes are defensively copied; path sources are stored
 * as normalized absolute paths so runtime adapters can resolve them
 * deterministically.</p>
 *
 * @author Artem Demchyshyn
 */
public final class DocumentImageData {
    private final byte[] bytes;
    private final Path path;

    private DocumentImageData(byte[] bytes, Path path) {
        this.bytes = bytes;
        this.path = path;
    }

    /**
     * Creates an image descriptor from in-memory bytes.
     *
     * @param bytes encoded raster image bytes
     * @return image descriptor
     */
    public static DocumentImageData fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Image bytes cannot be null or empty.");
        }
        return new DocumentImageData(Arrays.copyOf(bytes, bytes.length), null);
    }

    /**
     * Creates an image descriptor from a filesystem path.
     *
     * @param path image path
     * @return image descriptor
     */
    public static DocumentImageData fromPath(Path path) {
        Path safePath = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        return new DocumentImageData(null, safePath);
    }

    /**
     * Creates an image descriptor from a filesystem path string.
     *
     * @param path image path
     * @return image descriptor
     */
    public static DocumentImageData fromPath(String path) {
        return fromPath(Path.of(Objects.requireNonNull(path, "path")));
    }

    /**
     * Returns a defensive copy of the encoded bytes when this is an in-memory source.
     *
     * @return optional image bytes
     */
    public Optional<byte[]> bytes() {
        return bytes == null ? Optional.empty() : Optional.of(Arrays.copyOf(bytes, bytes.length));
    }

    /**
     * Returns the normalized path when this is a filesystem source.
     *
     * @return optional image path
     */
    public Optional<Path> path() {
        return Optional.ofNullable(path);
    }
}
