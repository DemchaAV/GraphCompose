package com.demcha.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CoverLetterMock {

    private static final Path LETTER_PATH = Path.of("src", "test", "resources", "data", "cover_leter.txt");
    public static String letter;

    static {
        try {
            letter = Files.readString(LETTER_PATH);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load cover_leter.txt",e);
        }
    }
}
