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
 * {@value #FAR_SHARE_PERCENT} in a hundred more than 2pt off. It is a report of where each
 * document stands, not a gate: the baseline ({@link FidelityBaseline}) is what keeps a document
 * from moving further away.</p>
 */
final class FidelityAcceptance {

    static final int FOUND_SHARE_PERCENT = 95;
    static final double MEDIAN_POINTS = 1.0;
    static final int FAR_SHARE_PERCENT = 10;

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
        if (measured.over2() * 100L > (long) FAR_SHARE_PERCENT * Math.max(1, measured.matched())) {
            shortfalls.add(measured.over2() + " lines past 2pt");
        }
        return shortfalls;
    }

    /**
     * Writes the report: how many documents pass, then each document that does not, with what
     * keeps it from passing, the furthest first.
     */
    static void write(Path file, String note, Collection<FidelityMeasurement> measurements) throws IOException {
        List<FidelityMeasurement> failing = measurements.stream()
                .filter(measured -> !shortfalls(measured).isEmpty())
                .sorted(Comparator.comparingInt((FidelityMeasurement measured) -> measured.lines() - measured.matched()
                                + measured.over2()).reversed())
                .toList();
        List<String> lines = new ArrayList<>();
        lines.add("# DOCX fidelity: " + note);
        lines.add("");
        lines.add((measurements.size() - failing.size()) + " of " + measurements.size()
                  + " documents pass: the page's pages, " + FOUND_SHARE_PERCENT + "% of its lines found, a median of "
                  + MEDIAN_POINTS + "pt or less, no more than " + FAR_SHARE_PERCENT + "% past 2pt.");
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
