package com.demcha.compose.font_library;

import lombok.extern.slf4j.Slf4j;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class Pdf_FontLoader {

    // 1. Глобальный кэш сырых байтов (читаем с диска 1 раз на всё приложение)
    private static final Map<String, byte[]> RAW_FONT_CACHE = new ConcurrentHashMap<>();

    // 2. МАГИЯ: Локальный кэш для КАЖДОГО ПОТОКА отдельно.
    // Поток парсит шрифт 1 раз для себя и переиспользует его. Нет конфликтов курсоров!
    private static final ThreadLocal<Map<String, TrueTypeFont>> THREAD_LOCAL_TTF_CACHE =
            ThreadLocal.withInitial(HashMap::new);

    private Pdf_FontLoader() {}

    public static PDType0Font loadFont(PDDocument document, Path path) {
        String absolutePath = path.toAbsolutePath().toString();

        try {
            // 1. Берем байты из общего кэша
            byte[] fontBytes = RAW_FONT_CACHE.computeIfAbsent(absolutePath, key -> {
                try {
                    return Files.readAllBytes(path);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read font bytes", e);
                }
            });

            // 2. Берем распарсенный TTF из ЛИЧНОГО кэша текущего потока
            TrueTypeFont ttf = THREAD_LOCAL_TTF_CACHE.get().computeIfAbsent(absolutePath, key -> {
                try {
                    // Создаем личный буфер и парсим (выполнится ровно 50 раз - по 1 разу на поток)
                    RandomAccessReadBuffer buffer = new RandomAccessReadBuffer(fontBytes);
                    return new TTFParser().parse(buffer);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to parse TTF", e);
                }
            });

            // 3. Мгновенно привязываем к документу
            return PDType0Font.load(document, ttf, true);

        } catch (IOException e) {
            log.error("Unable to load font into document from path {}", path, e);
            throw new RuntimeException(e);
        }
    }

    public static PDType0Font loadFont(PDDocument document, InputStream inputStream, String sourceDescription) {
        try (InputStream streamToClose = inputStream) {
            // Для InputStream применяем ту же магию ThreadLocal
            byte[] fontBytes = RAW_FONT_CACHE.computeIfAbsent(sourceDescription, key -> {
                try {
                    return streamToClose.readAllBytes();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            TrueTypeFont ttf = THREAD_LOCAL_TTF_CACHE.get().computeIfAbsent(sourceDescription, key -> {
                try {
                    RandomAccessReadBuffer buffer = new RandomAccessReadBuffer(fontBytes);
                    return new TTFParser().parse(buffer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            return PDType0Font.load(document, ttf, true);
        } catch (IOException e) {
            log.error("Unable to load font from {}", sourceDescription, e);
            throw new RuntimeException(e);
        }
    }
}