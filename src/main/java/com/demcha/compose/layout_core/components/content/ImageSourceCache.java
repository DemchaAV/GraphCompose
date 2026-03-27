package com.demcha.compose.layout_core.components.content;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
final class ImageSourceCache {
    private static final Map<String, CachedImageSource> SOURCE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ImageMetadata> METADATA_CACHE = new ConcurrentHashMap<>();
    private static final AtomicInteger METADATA_DECODE_COUNT = new AtomicInteger();

    private ImageSourceCache() {
    }

    static ImageData fromPath(Path path) {
        Path absolutePath = path.toAbsolutePath().normalize();
        String sourceKey = absolutePath.toString();
        CachedImageSource source = SOURCE_CACHE.computeIfAbsent(sourceKey, key -> loadFromPath(absolutePath));
        return new ImageData(source.bytes(), sourceKey, source.fingerprint(), source.metadata());
    }

    static ImageData fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Image bytes cannot be null or empty");
        }
        byte[] safeCopy = Arrays.copyOf(bytes, bytes.length);
        String fingerprint = fingerprint(safeCopy);
        ImageMetadata metadata = METADATA_CACHE.computeIfAbsent(fingerprint, key -> decodeMetadata(safeCopy));
        return new ImageData(safeCopy, "memory:" + fingerprint, fingerprint, metadata);
    }

    static void clearForTests() {
        SOURCE_CACHE.clear();
        METADATA_CACHE.clear();
        METADATA_DECODE_COUNT.set(0);
    }

    static int sourceCacheSize() {
        return SOURCE_CACHE.size();
    }

    static int metadataCacheSize() {
        return METADATA_CACHE.size();
    }

    static int metadataDecodeCount() {
        return METADATA_DECODE_COUNT.get();
    }

    private static CachedImageSource loadFromPath(Path absolutePath) {
        try {
            byte[] bytes = Files.readAllBytes(absolutePath);
            String fingerprint = fingerprint(bytes);
            ImageMetadata metadata = METADATA_CACHE.computeIfAbsent(fingerprint, key -> decodeMetadata(bytes));
            return new CachedImageSource(bytes, fingerprint, metadata);
        } catch (IOException e) {
            log.error("Failed to read image bytes from {}", absolutePath, e);
            throw new IllegalStateException("Failed to read image bytes from " + absolutePath, e);
        }
    }

    private static ImageMetadata decodeMetadata(byte[] bytes) {
        METADATA_DECODE_COUNT.incrementAndGet();

        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (imageInputStream == null) {
                throw new IllegalArgumentException("Unsupported or undecodable raster image");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("No ImageReader found for raster image");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                String format = reader.getFormatName();
                return new ImageMetadata(width, height, format);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to decode image metadata", e);
        }
    }

    private static String fingerprint(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private record CachedImageSource(byte[] bytes, String fingerprint, ImageMetadata metadata) {
    }
}
