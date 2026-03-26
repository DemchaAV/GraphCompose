package com.demcha.compose.loyaut_core.components.content;

import com.demcha.compose.loyaut_core.components.core.Component;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Data
@AllArgsConstructor
public final class ImageData implements Component {
    private byte[] bytes;

    public static ImageData create(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Image bytes cannot be null or empty");
        }
        return new ImageData(Arrays.copyOf(bytes, bytes.length));
    }

    public static ImageData create(Path path) {

        log.info("Create an image from path: {}", path);
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("Failed to read image from path {}", path, e);
            throw new IllegalStateException("Failed to read image from path " + path, e);
        }
        return create(bytes);
    }

    public static ImageData create(String path) {
        return create(Path.of(path));
    }


}
