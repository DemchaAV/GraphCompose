package com.demcha.compose.engine.components.content;

import com.demcha.compose.engine.components.core.Component;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
@Getter
@ToString(exclude = "bytes")
@EqualsAndHashCode(of = "fingerprint")
public final class ImageData implements Component {
    private final byte[] bytes;
    private final String sourceKey;
    private final String fingerprint;
    private final ImageMetadata metadata;

    ImageData(byte[] bytes, String sourceKey, String fingerprint, ImageMetadata metadata) {
        this.bytes = bytes;
        this.sourceKey = sourceKey;
        this.fingerprint = fingerprint;
        this.metadata = metadata;
    }

    public static ImageData create(byte[] bytes) {
        return ImageSourceCache.fromBytes(bytes);
    }

    public static ImageData create(Path path) {
        log.info("Create an image from path: {}", path);
        return ImageSourceCache.fromPath(path);
    }

    public static ImageData create(String path) {
        return create(Path.of(path));
    }
}
