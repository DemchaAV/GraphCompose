package com.demcha.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CoverLetterMock {

    public static String letter;

    static {
        try {
            letter = Files.readString(Path.of("C:\\Users\\Demch\\OneDrive\\Java\\PDF_CV_CREATOR\\src\\test\\resources\\data\\cover_leter.txt"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load cover_leter.txt",e);
        }
    }
}
