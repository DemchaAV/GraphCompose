package com.demcha.compose.document.templates.fidelity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The measurements a DOCX export is held to: one per corpus document, and the drift of each of
 * its lines the editor set where the page does, as last accepted.
 *
 * <p>A change may move the export nearer the page, never further. Against its baseline a
 * document fails when the editor sets it on a page count further from the engine's, when a
 * line the baseline found is no longer found — the editor breaks it at another word, or puts it
 * on another page — or when a found line drifts more than {@value #LINE_SLACK}pt further from
 * the page than it did, down or across. Across as well as down: a table's left margin the
 * export dropped stood {@code PaymentsInvoice}'s bank details 36.7pt left of the page's and
 * moved no line up or down. Line by line, because a document already set a few points off
 * throughout hides a new defect in its counts. A document the baseline does not hold fails
 * until the baseline is written with it, and so does one the baseline holds and the corpus no
 * longer has. A document whose lines the editor mostly sets at other words is held to the few
 * it finds: the gate keeps what an export has, and adds nothing for what it lacks.</p>
 *
 * <p>The baseline is two files: {@code <name>.tsv}, a row per document, and
 * {@code <name>-lines.tsv}, a row per found line, named by its page and a digest of its
 * letters, with its drift down and across and a few of its letters to read it by. A line file
 * written before lines were measured across holds them to their drift down alone.</p>
 */
final class FidelityBaseline {

    /** How much further a line may drift, in points: rounding, not room for a change to spend. */
    static final double LINE_SLACK = 0.5;

    /** The line file's header names this column when its lines were measured across. */
    private static final String ACROSS_COLUMN = "drift right of the page (pt)";

    /** How many of a document's failing lines a failure names. */
    private static final int NAMED = 4;

    private final Map<String, FidelityMeasurement> rows;

    private FidelityBaseline(Map<String, FidelityMeasurement> rows) {
        this.rows = rows;
    }

    /**
     * Reads a baseline: no file at all is an empty baseline, which every document fails
     * against; a document file without its line file, or a line file that holds another number
     * of a document's lines than its row says it found, is refused rather than read as a
     * baseline that holds no lines.
     */
    static FidelityBaseline read(Path documents) throws IOException {
        Map<String, FidelityMeasurement> rows = new LinkedHashMap<>();
        if (!Files.exists(documents)) {
            return new FidelityBaseline(rows);
        }
        Path lineFile = linesOf(documents);
        if (!Files.exists(lineFile)) {
            throw new IllegalStateException("the baseline " + documents + " has no line file " + lineFile);
        }
        Map<String, Map<String, FidelityMeasurement.Found>> lines = new LinkedHashMap<>();
        List<String> lineRows = Files.readAllLines(lineFile, StandardCharsets.UTF_8);
        // Its header says whether its lines were measured across, for the whole file.
        boolean across = !lineRows.isEmpty() && lineRows.get(0).startsWith("#")
                         && lineRows.get(0).contains(ACROSS_COLUMN);
        for (String row : lineRows) {
            if (!row.isBlank() && !row.startsWith("#")) {
                FidelityMeasurement.parseLine(row, lines, across);
            }
        }
        for (String row : Files.readAllLines(documents, StandardCharsets.UTF_8)) {
            if (row.isBlank() || row.startsWith("#") || row.equals(FidelityMeasurement.HEADER)) {
                continue;
            }
            String stem = row.substring(0, Math.max(0, row.indexOf('\t')));
            FidelityMeasurement measured = FidelityMeasurement.parse(row, lines.getOrDefault(stem, Map.of()));
            if (measured.found().size() != measured.matched()) {
                throw new IllegalStateException(stem + " found " + measured.matched() + " lines, and "
                        + lineFile + " holds " + measured.found().size() + " of them");
            }
            rows.put(measured.stem(), measured);
        }
        return new FidelityBaseline(rows);
    }

    static FidelityBaseline of(Collection<FidelityMeasurement> measurements) {
        Map<String, FidelityMeasurement> rows = new LinkedHashMap<>();
        measurements.forEach(row -> rows.put(row.stem(), row));
        return new FidelityBaseline(rows);
    }

    /** Writes the measurements as a baseline, under a note of where they were taken. */
    static void write(Path documents, String note, Collection<FidelityMeasurement> measurements) throws IOException {
        List<FidelityMeasurement> sorted = measurements.stream()
                .sorted(Comparator.comparing(FidelityMeasurement::stem)).toList();
        List<String> rows = new ArrayList<>(List.of("# " + note, FidelityMeasurement.HEADER));
        List<String> lines = new ArrayList<>(List.of("# " + note
                + " — document, line, drift below the page (pt), " + ACROSS_COLUMN + ", letters"));
        for (FidelityMeasurement measured : sorted) {
            rows.add(measured.row());
            lines.addAll(measured.lineRows());
        }
        Files.createDirectories(documents.toAbsolutePath().getParent());
        Files.write(documents, rows, StandardCharsets.UTF_8);
        Files.write(linesOf(documents), lines, StandardCharsets.UTF_8);
    }

    /** What each measurement does worse than its baseline; empty when none does. */
    List<String> regressions(Collection<FidelityMeasurement> measurements) {
        List<String> found = new ArrayList<>();
        java.util.Set<String> measured = new java.util.HashSet<>();
        measurements.forEach(now -> measured.add(now.stem()));
        rows.keySet().stream().filter(stem -> !measured.contains(stem))
                .forEach(stem -> found.add(stem + ": in the baseline, and no longer in the corpus"));
        for (FidelityMeasurement now : measurements) {
            FidelityMeasurement was = rows.get(now.stem());
            if (was == null) {
                found.add(now.stem() + ": not in the baseline; measured " + now.row());
                continue;
            }
            int pagesOffWas = Math.abs(was.editorPages() - was.enginePages());
            int pagesOffNow = Math.abs(now.editorPages() - now.enginePages());
            if (pagesOffNow > pagesOffWas) {
                found.add(now.stem() + ": set on " + now.editorPages() + " pages against the page's "
                          + now.enginePages() + " (baseline " + was.editorPages() + ")");
            }
            List<String> lost = new ArrayList<>();
            List<String> further = new ArrayList<>();
            was.found().forEach((id, line) -> {
                FidelityMeasurement.Found there = now.found().get(id);
                if (there == null) {
                    lost.add("\"" + line.preview() + "\"");
                } else if (Math.abs(there.drift()) > Math.abs(line.drift()) + LINE_SLACK) {
                    further.add(String.format(Locale.ROOT, "\"%s\" %+.2f -> %+.2fpt",
                            line.preview(), line.drift(), there.drift()));
                } else if (!Double.isNaN(line.across())
                           && Math.abs(there.across()) > Math.abs(line.across()) + LINE_SLACK) {
                    further.add(String.format(Locale.ROOT, "\"%s\" %+.2f -> %+.2fpt across",
                            line.preview(), line.across(), there.across()));
                }
            });
            if (!lost.isEmpty()) {
                found.add(now.stem() + ": " + lost.size() + " lines no longer found, set at other words or "
                          + "on another page: " + named(lost));
            }
            if (!further.isEmpty()) {
                found.add(now.stem() + ": " + further.size() + " lines further from the page: " + named(further));
            }
        }
        return found;
    }

    private static String named(List<String> lines) {
        return String.join(", ", lines.subList(0, Math.min(NAMED, lines.size())))
               + (lines.size() > NAMED ? ", and " + (lines.size() - NAMED) + " more" : "");
    }

    private static Path linesOf(Path documents) {
        String name = documents.getFileName().toString();
        return documents.resolveSibling(name.replaceFirst("\\.tsv$", "") + "-lines.tsv");
    }
}
