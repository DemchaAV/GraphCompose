package com.demcha.compose.document.templates.fidelity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Whether an editor sets a document's DOCX as the page sets it, by four measures: the same
 * number of pages, its lines found, set close, and few set far.
 *
 * <p>A document passes when the editor sets it on the page's number of pages, finds at least
 * {@value #FOUND_SHARE_PERCENT} in a hundred of the page's lines, sets them a median of no more
 * than {@value #MEDIAN_POINTS}pt from the page, and sets no more than
 * {@value #FAR_SHARE_PERCENT} in a hundred of the lines it finds more than
 * {@value #FAR_POINTS}pt off. It is a report of where each document stands, not a gate: the
 * baseline ({@link FidelityBaseline}) is what keeps a document from moving at all, either way,
 * without the move being recorded.</p>
 *
 * <p>A line is that far off when it stands more than {@value #FAR_POINTS}pt above or below the
 * page's, or more than {@value #FAR_POINTS}pt left or right of it. Counting only the first let
 * a document pass whatever its lines did across: when across was added, CI's LibreOffice set
 * 778 of the corpus's lines more than 2pt across, and 14 documents passed with them,
 * {@code invoice-classic_long} with 136 of the 166 lines found. The median is still the drift
 * down the page alone. The report also counts the corpus's lines past {@value #FAR_POINTS}pt,
 * down and across apart, and the documents holding them.</p>
 */
final class FidelityAcceptance {

    static final int FOUND_SHARE_PERCENT = 95;
    static final double MEDIAN_POINTS = 1.0;
    static final int FAR_SHARE_PERCENT = 10;
    static final double FAR_POINTS = 2.0;

    private FidelityAcceptance() {
    }

    /** What keeps a document from passing; empty when it passes. */
    static List<String> shortfalls(FidelityMeasurement measured) {
        List<String> shortfalls = new ArrayList<>();
        if (measured.editorPages() != measured.enginePages()) {
            shortfalls.add(measured.editorPages() + " pages, not " + measured.enginePages());
        }
        if (measured.matched() * 100L < (long) FOUND_SHARE_PERCENT * measured.lines()) {
            shortfalls.add(measured.matched() + " of " + measured.lines() + " lines found");
        }
        if (measured.median() > MEDIAN_POINTS) {
            shortfalls.add(String.format(Locale.ROOT, "median %.2fpt", measured.median()));
        }
        int far = far(measured);
        if (far * 100L > (long) FAR_SHARE_PERCENT * Math.max(1, measured.matched())) {
            shortfalls.add(counted(far, "line") + " past " + points() + " (" + farDown(measured) + " down, "
                           + farAcross(measured) + " across)");
        }
        return shortfalls;
    }

    /** The found lines more than {@value #FAR_POINTS}pt off the page's either way, each once. */
    static int far(FidelityMeasurement measured) {
        return (int) measured.found().values().stream()
                .filter(line -> farDown(line) || farAcross(line)).count();
    }

    /** The found lines more than {@value #FAR_POINTS}pt above or below the page's. */
    static int farDown(FidelityMeasurement measured) {
        return (int) measured.found().values().stream().filter(FidelityAcceptance::farDown).count();
    }

    /** The found lines more than {@value #FAR_POINTS}pt left or right of the page's. */
    static int farAcross(FidelityMeasurement measured) {
        return (int) measured.found().values().stream().filter(FidelityAcceptance::farAcross).count();
    }

    private static boolean farDown(FidelityMeasurement.Found line) {
        return Math.abs(line.drift()) > FAR_POINTS;
    }

    private static boolean farAcross(FidelityMeasurement.Found line) {
        return Math.abs(line.across()) > FAR_POINTS;
    }

    private static int total(Collection<FidelityMeasurement> measurements,
                             java.util.function.ToIntFunction<FidelityMeasurement> count) {
        return measurements.stream().mapToInt(count).sum();
    }

    private static long documents(Collection<FidelityMeasurement> measurements,
                                  java.util.function.ToIntFunction<FidelityMeasurement> count) {
        return measurements.stream().filter(measured -> count.applyAsInt(measured) > 0).count();
    }

    private static String counted(long count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    private static String points() {
        return String.format(Locale.ROOT, "%.0fpt", FAR_POINTS);
    }

    /**
     * Writes the report: how many documents pass, how many of the corpus's lines stand far off
     * each way, then each document that does not pass, with what keeps it from passing, the
     * furthest first.
     */
    static void write(Path file, String note, Collection<FidelityMeasurement> measurements) throws IOException {
        List<FidelityMeasurement> failing = measurements.stream()
                .filter(measured -> !shortfalls(measured).isEmpty())
                .sorted(Comparator.comparingInt((FidelityMeasurement measured) -> measured.lines() - measured.matched()
                                + far(measured)).reversed())
                .toList();
        List<String> lines = new ArrayList<>();
        lines.add("# DOCX fidelity: " + note);
        lines.add("");
        lines.add((measurements.size() - failing.size()) + " of " + measurements.size()
                  + " documents pass: the page's pages, " + FOUND_SHARE_PERCENT + "% of its lines found, a median of "
                  + MEDIAN_POINTS + "pt or less, no more than " + FAR_SHARE_PERCENT + "% past " + points()
                  + " down or across.");
        lines.add("");
        lines.add("Lines past " + points() + ": " + total(measurements, FidelityAcceptance::farDown) + " down, in "
                  + counted(documents(measurements, FidelityAcceptance::farDown), "document") + "; "
                  + total(measurements, FidelityAcceptance::farAcross) + " across, in "
                  + counted(documents(measurements, FidelityAcceptance::farAcross), "document") + ".");
        if (!failing.isEmpty()) {
            lines.add("");
            lines.add("| Document | Short of it |");
            lines.add("|---|---|");
            failing.forEach(measured -> lines.add("| " + measured.stem() + " | "
                                                  + String.join("; ", shortfalls(measured)) + " |"));
        }
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.write(file, lines, StandardCharsets.UTF_8);
    }
}
