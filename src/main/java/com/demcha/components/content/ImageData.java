package com.demcha.components.content;

import com.demcha.components.core.Component;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Data
@AllArgsConstructor
public final class ImageData implements Component {
    private byte[] bytes;

    public static ImageData create(Path path) {

        log.info("Create an image from path: {}", path);
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            log.error(e.getMessage());
            log.error(e.getStackTrace().toString());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return new ImageData(bytes);
    }

    public static ImageData create(String path) {
        return create(Path.of(path));
    }


}
