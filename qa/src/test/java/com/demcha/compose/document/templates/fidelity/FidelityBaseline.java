package com.demcha.compose.document.templates.fidelity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The measurements a DOCX export is held to: one per corpus document, and the drift of each of
 * its lines the editor set where the page does, as last accepted.
 *
 * <p>The baseline is the record of where the export stands, so a change may move the export
 * either way and must write the move into it. Against its baseline a document fails when the
 * editor sets it on another number of pages, when the pages or the lines the engine draws it on
 * change, when a line the baseline found is no longer found — the editor breaks it at another
 * word, or puts it on another page — when a line is found that the baseline does not hold, or
 * when a found line's drift moves by more than {@value #LINE_SLACK}pt, down or across,
 * <em>whichever way it moved</em>. Across as well as down: a table's left margin the export dropped stood
 * {@code PaymentsInvoice}'s bank details 36.7pt left of the page's and moved no line up or
 * down. Line by line, because a document already set a few points off throughout hides a new
 * defect in its counts. A document the baseline does not hold fails until the baseline is
 * written with it, and so does one the baseline holds and the corpus no longer has.</p>
 *
 * <p>Whichever way it moved, because a gate that only looks for lines set further from the page
 * cannot see the state it is holding drift out from under it. Held that way from #899 to #974,
 * 1609 of the Word baseline's 5165 rows went stale over 74 commits with no run ever going red:
 * 1388 moved down the page — 717 nearer it, 635 further, and 36 crossing it to stand the same
 * distance on the other side — and 229 moved across, 228 of them nearer by as much as 1.10pt,
 * 8 rows moving both ways. Of all that, 228 rows moved more than the slack: 226 across and 2
 * down, both of those crossing the page's line. And yet no row's <em>distance</em> from the page
 * grew by more than 0.48pt — 21 rows of {@code cv-serif_headline}, {@code cv-panel} and
 * {@code invoice-obsidian} by that much and the rest by 0.36pt or less — which is the whole
 * reason a rule reading distances against half a point of slack stayed quiet. Those same three
 * documents are the ones that ended 0.48pt further, where the next change to touch them would
 * have failed for drift it did not cause. A move nearer the page that goes unwritten is worse than
 * untidy: the baseline keeps the further value, so a later change may walk the line back to it
 * and spend the slack again on top. Comparing where a line is now against where it was, rather
 * than which of the two is further from the page, also catches a line that crosses the page's:
 * {@code +3.00 -> -3.00pt} stands the same distance away and has moved six points.</p>
 *
 * <p>So a change that moves documents — nearer the page included — rewrites the baseline in the
 * same commit ({@code -Dgraphcompose.docxFidelity.update=true}), and the diff of it is what
 * shows the reviewer what moved. The aggregates a row carries — its median, its p90, the lines
 * past 2pt — are not held to anything: they follow from the lines, which are held one by one,
 * and an aggregate hides the tail it is an average of. Nor is the count of lines found, which
 * is the set of them, held line by line above.</p>
 *
 * <p>The slack is read afresh against each rewrite, so it bounds no total: a line may be moved
 * half a point by every change, or walked back and forth, and each step passes as rounding. What
 * holding both ways buys is that every move past rounding is seen and written down — not that
 * drift cannot accumulate across many changes, which is what {@link FidelityAcceptance}'s limits
 * are for.</p>
 *
 * <p>The baseline is two files: {@code <name>.tsv}, a row per document, and
 * {@code <name>-lines.tsv}, a row per found line, named by its page and a digest of its
 * letters, with its drift down and across and a few of its letters to read it by. A line file
 * written before lines were measured across holds them to their drift down alone.</p>
 */
final class FidelityBaseline {

    /**
     * How far a line may move, in points, before the baseline must record it: rounding, not room
     * for a change to spend. One slack both ways, because rounding is the same size whichever
     * way a line rounds.
     */
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
        // Its lines were measured across, for the whole file, when its header says so or any
        // row holds a drift across: a row without one there is refused, not read as old.
        boolean across = !lineRows.isEmpty() && lineRows.get(0).startsWith("#")
                         && lineRows.get(0).contains(ACROSS_COLUMN)
                         || lineRows.stream().anyMatch(row -> !row.startsWith("#") && row.split("\t", 5).length == 5);
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

    /**
     * Writes the measurements as a baseline, under a note of where they were taken, keeping any
     * further note the file it replaces carried.
     *
     * <p>Those notes are what a row's reckoning is written down in — why a line stands where it
     * does, which run shot it, what was left unwritten — and a rewrite is now the ordinary answer
     * to a document moving, so dropping them would spend the record this gate exists to keep.
     * They are carried over as they stand, under the new header: a note the rewrite has made
     * stale is for the author to prune, which the file in front of them shows.</p>
     */
    static void write(Path documents, String note, Collection<FidelityMeasurement> measurements) throws IOException {
        write(documents, note, measurements, documents);
    }

    /**
     * Writes the measurements, taking the notes to keep from another baseline than the file being
     * written.
     *
     * <p>A measured run writes its own files in {@code target}, which carry no notes of their own,
     * and a run on a machine the baseline cannot be shot on — CI — is copied over the baseline
     * from there. Written with the baseline as its notes, that copy carries them, so the path the
     * contributor is told to take keeps the record as the {@code update} path does.</p>
     */
    static void write(Path documents, String note, Collection<FidelityMeasurement> measurements,
                      Path notesFrom) throws IOException {
        List<FidelityMeasurement> sorted = measurements.stream()
                .sorted(Comparator.comparing(FidelityMeasurement::stem)).toList();
        List<String> rows = new ArrayList<>(List.of("# " + note, FidelityMeasurement.HEADER));
        rows.addAll(1, notesOf(notesFrom));
        List<String> lines = new ArrayList<>(List.of("# " + note
                + " — document, line, drift below the page (pt), " + ACROSS_COLUMN + ", letters"));
        lines.addAll(notesOf(linesOf(notesFrom)));
        for (FidelityMeasurement measured : sorted) {
            rows.add(measured.row());
            lines.addAll(measured.lineRows());
        }
        Files.createDirectories(documents.toAbsolutePath().getParent());
        Files.write(documents, rows, StandardCharsets.UTF_8);
        Files.write(linesOf(documents), lines, StandardCharsets.UTF_8);
    }

    /**
     * Every note a baseline file carries under its header, which is its first line.
     *
     * <p>Where the header stands is the file's shape, not a guess: {@link #write} puts it there
     * and {@link #read} reads the line file's from there. The header cannot be told from a note
     * by its text — both are a {@code #} row of prose — so a note written above it could not be
     * kept without dropping the header in its place, and the file would come back naming a build
     * it was never shot on. A file that does not open with its header is therefore refused here
     * rather than half-read: notes are a block under the header, and a file shaped otherwise is
     * not one this wrote.</p>
     */
    private static List<String> notesOf(Path file) throws IOException {
        if (!Files.exists(file)) {
            return List.of();
        }
        List<String> rows = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (!rows.isEmpty() && !rows.get(0).startsWith("#")) {
            throw new IllegalStateException("the baseline " + file + " does not open with the note of"
                    + " where it was taken, so its own notes cannot be told from its header: " + rows.get(0));
        }
        return rows.stream().skip(1).filter(row -> row.startsWith("#")).toList();
    }

    /**
     * Where each measurement stands apart from its baseline, whichever way it moved; empty when
     * none does, which is the only state the gate passes.
     */
    List<String> divergences(Collection<FidelityMeasurement> measurements) {
        List<String> apart = new ArrayList<>();
        Set<String> measured = new HashSet<>();
        measurements.forEach(now -> measured.add(now.stem()));
        rows.keySet().stream().filter(stem -> !measured.contains(stem))
                .forEach(stem -> apart.add(stem + ": in the baseline, and no longer in the corpus"));
        for (FidelityMeasurement now : measurements) {
            FidelityMeasurement was = rows.get(now.stem());
            if (was == null) {
                apart.add(now.stem() + ": not in the baseline; measured " + now.row());
                continue;
            }
            if (now.editorPages() != was.editorPages()) {
                apart.add(now.stem() + ": set on " + pages(now.editorPages()) + ", and the baseline holds "
                          + pages(was.editorPages()) + " (the page's " + now.enginePages() + ")");
            }
            if (now.enginePages() != was.enginePages()) {
                apart.add(now.stem() + ": the page is drawn on " + pages(now.enginePages())
                          + ", and the baseline holds " + pages(was.enginePages()));
            }
            if (now.lines() != was.lines()) {
                apart.add(now.stem() + ": the page draws " + lineCount(now.lines())
                          + ", and the baseline holds " + was.lines());
            }
            List<String> lost = new ArrayList<>();
            List<String> moved = new ArrayList<>();
            was.found().forEach((id, line) -> {
                FidelityMeasurement.Found there = now.found().get(id);
                if (there == null) {
                    lost.add("\"" + line.preview() + "\"");
                    return;
                }
                // Named once, with every way it moved.
                List<String> ways = new ArrayList<>(2);
                // Both axes fail closed on a measurement that carries no number: Math.round(NaN)
                // is 0, so a step against it compares false and the line would pass unheld.
                if (Double.isNaN(there.drift()) || moved(line.drift(), there.drift())) {
                    ways.add(step(line.drift(), there.drift(), ""));
                }
                // A baseline holding a place across holds the line to it: a measurement without
                // one is a line that cannot be held, which fails rather than passing unheld.
                if (!Double.isNaN(line.across())
                    && (Double.isNaN(there.across()) || moved(line.across(), there.across()))) {
                    ways.add(step(line.across(), there.across(), " across"));
                }
                if (!ways.isEmpty()) {
                    moved.add("\"" + line.preview() + "\" " + String.join(", ", ways));
                }
            });
            List<String> gained = new ArrayList<>();
            now.found().forEach((id, line) -> {
                if (!was.found().containsKey(id)) {
                    gained.add("\"" + line.preview() + "\"");
                }
            });
            if (!lost.isEmpty()) {
                apart.add(now.stem() + ": " + lineCount(lost.size()) + " no longer found, set at other words or "
                          + "on another page: " + named(lost));
            }
            if (!gained.isEmpty()) {
                apart.add(now.stem() + ": " + lineCount(gained.size()) + " found that the baseline does not hold: "
                          + named(gained));
            }
            if (!moved.isEmpty()) {
                apart.add(now.stem() + ": " + lineCount(moved.size()) + " moved: " + named(moved));
            }
        }
        return apart;
    }

    /**
     * Whether a drift has moved more than {@link #LINE_SLACK}pt from where the baseline holds
     * it, nearer the page or further, counted in the hundredths both are written in: in doubles,
     * a step of exactly half a point came out over half a point for some drifts and not for
     * others.
     */
    private static boolean moved(double was, double now) {
        return Math.round(Math.abs(now - was) * 100) > Math.round(LINE_SLACK * 100);
    }

    private static String pages(int count) {
        return count + (count == 1 ? " page" : " pages");
    }

    /** A line's step, as the baseline holds it, as it is now, and the difference. */
    private static String step(double was, double now, String axis) {
        if (Double.isNaN(now)) {
            return String.format(Locale.ROOT, "%+.2fpt%s in the baseline, and no place%s measured",
                    was, axis, axis);
        }
        return String.format(Locale.ROOT, "%+.2f -> %+.2fpt%s (%+.2f)", was, now, axis, now - was);
    }

    /** A count of lines, named so it reads in a sentence. */
    private static String lineCount(int count) {
        return count + (count == 1 ? " line" : " lines");
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
