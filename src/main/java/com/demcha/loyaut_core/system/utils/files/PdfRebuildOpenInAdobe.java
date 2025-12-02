package com.demcha.loyaut_core.system.utils.files;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class PdfRebuildOpenInAdobe {

    /** Главный сценарий: закрыть Adobe -> пересобрать PDF -> открыть его в Adobe */
    public static void regenerateAndOpenInAdobe(Path target, Supplier<Path> supplier) throws Exception {
        // 1) Close Adobe
        closeAdobeFromResources("pdf_close-adobe.bat", 3, TimeUnit.SECONDS);
        Thread.sleep(700);

        // Resolve a safe directory for temp files (same dir as target)
        Path targetAbs = target.toAbsolutePath();
        Path dir = targetAbs.getParent();
        if (dir == null) {                       // target like "output.pdf"
            dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        }
        Files.createDirectories(dir);            // ensure it exists

        // 2) Generate into a temp file (or let supplier return it)
        Path tmp = supplier != null ? supplier.get() : null;
        if (tmp == null) {
            // fallback: create empty temp file in the same dir; your generator should write into it
            tmp = Files.createTempFile(dir, "pdf_", ".pdf");
            // MyPdfGenerator.generate(tmp); // <- your generation here if you don’t use supplier
        }
        if (!Files.exists(tmp)) {
            throw new IOException("PDF generation failed: temp file does not exist: " + tmp);
        }

        // 3) Atomic replace
        replaceWithRetries(tmp, targetAbs, 6, Duration.ofMillis(400));

        // 4) Open in Adobe
        openInAdobe(targetAbs);
    }

    /** Открыть PDF именно в Adobe Reader/Acrobat (ищем exe через реестр и типовые пути) */
    public static void openInAdobe(Path pdf) throws IOException {
        Optional<Path> exe = findAdobeExe();
        if (exe.isPresent()) {
            new ProcessBuilder(exe.get().toString(), pdf.toString()).start();
        } else {
            // Фолбэк: попробуем по имени (если Adobe в PATH). Иначе сообщим пользователю.
            try {
                new ProcessBuilder("AcroRd32.exe", pdf.toString()).start();
            } catch (IOException e1) {
                try {
                    new ProcessBuilder("Acrobat.exe", pdf.toString()).start();
                } catch (IOException e2) {
                    throw new IOException(
                            "Не нашёл Adobe Reader/Acrobat. Установите его или добавьте путь к exe в PATH.\n" +
                            "Либо откройте по умолчанию: " + pdf, e2);
                }
            }
        }
    }

    /** Пытаемся найти путь к Adobe через реестр и стандартные каталоги */
    public static Optional<Path> findAdobeExe() {
        // 1) Реестр: App Paths для AcroRd32.exe / Acrobat.exe
        Optional<Path> fromReg = queryAppPathFromRegistry("AcroRd32.exe");
        if (fromReg.isPresent()) return fromReg;
        fromReg = queryAppPathFromRegistry("Acrobat.exe");
        if (fromReg.isPresent()) return fromReg;

        // 2) Типовые директории в Program Files
        String pf = System.getenv("ProgramFiles");          // C:\Program Files
        String pf86 = System.getenv("ProgramFiles(x86)");   // C:\Program Files (x86)
        for (String base : new String[]{pf, pf86}) {
            if (base == null) continue;
            Path adobeDir = Paths.get(base, "Adobe");
            if (Files.isDirectory(adobeDir)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(adobeDir)) {
                    for (Path sub : ds) {
                        // Reader: ...\Adobe\Acrobat Reader*\Reader\AcroRd32.exe
                        Path reader = sub.resolve("Reader").resolve("AcroRd32.exe");
                        if (Files.isRegularFile(reader)) return Optional.of(reader);
                        // Acrobat: ...\Adobe\Acrobat*\Acrobat\Acrobat.exe
                        Path acrobat = sub.resolve("Acrobat").resolve("Acrobat.exe");
                        if (Files.isRegularFile(acrobat)) return Optional.of(acrobat);
                    }
                } catch (IOException ignored) {}
            }
        }
        return Optional.empty();
    }

    /** Читает HKLM\App Paths\<exe>\(Default) через reg.exe */
    private static Optional<Path> queryAppPathFromRegistry(String exeName) {
        // Пример ключа: HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\AcroRd32.exe
        String key = "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\" + exeName;
        try {
            Process p = new ProcessBuilder("reg", "query", key, "/ve")
                    .redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // Ожидаем что-то вроде: (Default)    REG_SZ    C:\...\AcroRd32.exe
                    if (line.contains("REG_SZ")) {
                        String path = line.substring(line.indexOf("REG_SZ") + 6).trim();
                        Path candidate = Paths.get(path);
                        if (Files.isRegularFile(candidate)) return Optional.of(candidate);
                    }
                }
            }
            p.waitFor(1, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private static void replaceWithRetries(Path tmp, Path target, int maxAttempts, Duration delay)
            throws IOException, InterruptedException {
        Files.createDirectories(target.getParent());
        IOException last = null;
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return;
            } catch (IOException e) {
                last = e;
                Thread.sleep(delay.toMillis());
            }
        }
        throw new IOException("Не удалось заменить файл: " + target, last);
    }

    /** Выполняет .bat из resources (например, "pdf_close-adobe.bat") */
    public static void closeAdobeFromResources(String resourcePath, long timeout, TimeUnit unit)
            throws IOException, InterruptedException {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (in == null) return; // нет скрипта — пропустить
        Path tmpBat = Files.createTempFile("close_adobe_", ".bat");
        tmpBat.toFile().deleteOnExit();
        String content = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                .replace("\r\n", "\n").replace("\n", "\r\n"); // CRLF для .bat
        Files.writeString(tmpBat, content, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        Process p = new ProcessBuilder("cmd", "/c", tmpBat.toString())
                .redirectErrorStream(true).start();
        p.waitFor(timeout, unit);
        Files.deleteIfExists(tmpBat);
    }

}
