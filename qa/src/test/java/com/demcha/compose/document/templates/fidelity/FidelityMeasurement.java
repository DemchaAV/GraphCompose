package com.demcha.compose.document.templates.fidelity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * How far an editor's rendering of a document's DOCX stands from the engine's PDF of it.
 *
 * <p>Each line of the page is looked for, by its letters, on the same page of the editor's
 * PDF; a line the editor breaks at another word, or puts on another page, is not found. Lines
 * of the same letters on one page — a rota's shifts, a CV's date ranges — are paired each with
 * the editor's nearest, so one lost or moved among them is not hidden behind another. A found
 * line's drift is how far its baseline stands below the page's, above when negative.</p>
 *
 * @param stem         the document
 * @param enginePages  the pages the engine draws
 * @param editorPages  the pages the editor sets the DOCX on
 * @param lines        the page's lines
 * @param matched      the page's lines found in the editor's
 * @param median       the median of the found lines' drift either way, in points
 * @param p90          the drift either way nine in ten found lines stay within, in points
 * @param over2        the found lines drifting more than 2pt either way
 * @param found        each found line's drift, by its line id: its page, a digest of its
 *                     letters, and which of the page's lines of those letters it is, in the
 *                     order the page draws them
 */
record FidelityMeasurement(String stem, int enginePages, int editorPages, int lines, int matched,
                           double median, double p90, int over2, Map<String, Found> found) {

    /** A found line: where it is, a few of its letters to name it by, and its drift. */
    record Found(int page, String preview, double drift) {
    }

    static final String HEADER = "document\tengine_pages\teditor_pages\tlines\tmatched\tmedian\tp90\tover2";

    FidelityMeasurement {
        found = Collections.unmodifiableMap(new LinkedHashMap<>(found));
    }

    /** Measures the editor's PDF of a document against the engine's. */
    static FidelityMeasurement of(String stem, Path enginePdf, Path editorPdf) throws IOException {
        return of(stem, PdfLines.of(enginePdf), PdfLines.of(editorPdf));
    }

    static FidelityMeasurement of(String stem, PdfLines engine, PdfLines editor) {
        Map<String, List<PdfLines.Line>> page = grouped(engine);
        Map<String, List<PdfLines.Line>> set = grouped(editor);
        Map<String, Found> found = new LinkedHashMap<>();
        List<Double> drifts = new ArrayList<>();
        int lines = 0;
        for (Map.Entry<String, List<PdfLines.Line>> group : page.entrySet()) {
            List<PdfLines.Line> ours = group.getValue();
            lines += ours.size();
            PdfLines.Line[] theirs = paired(ours, set.getOrDefault(group.getKey(), List.of()));
            for (int index = 0; index < ours.size(); index++) {
                if (theirs[index] != null) {
                    PdfLines.Line line = ours.get(index);
                    double drift = round(theirs[index].baseline() - line.baseline());
                    found.put(lineId(line, index), new Found(line.page(), preview(line.key()), drift));
                    drifts.add(Math.abs(drift));
                }
            }
        }
        Collections.sort(drifts);
        int over2 = (int) drifts.stream().filter(drift -> drift > 2).count();
        return new FidelityMeasurement(stem, engine.pages(), editor.pages(), lines, drifts.size(),
                quantile(drifts, 0.5), quantile(drifts, 0.9), over2, found);
    }

    /** A PDF's lines by page and letters, each group in the order the page draws them. */
    private static Map<String, List<PdfLines.Line>> grouped(PdfLines pdf) {
        Map<String, List<PdfLines.Line>> groups = new LinkedHashMap<>();
        for (PdfLines.Line line : pdf.lines()) {
            groups.computeIfAbsent(line.page() + "/" + line.key(), key -> new ArrayList<>()).add(line);
        }
        return groups;
    }

    /**
     * Pairs the page's lines of one group with the editor's, nearest first: each pair is the
     * closest of the lines still unpaired, so a group the editor sets in another order, or with
     * one line lost, pairs the lines that stand where they did.
     */
    private static PdfLines.Line[] paired(List<PdfLines.Line> ours, List<PdfLines.Line> theirs) {
        PdfLines.Line[] pairs = new PdfLines.Line[ours.size()];
        if (ours.size() == 1 && theirs.size() == 1) {
            pairs[0] = theirs.get(0);
            return pairs;
        }
        List<int[]> candidates = new ArrayList<>();
        for (int i = 0; i < ours.size(); i++) {
            for (int j = 0; j < theirs.size(); j++) {
                candidates.add(new int[]{i, j});
            }
        }
        candidates.sort(java.util.Comparator.comparingDouble(pair -> distance(ours.get(pair[0]), theirs.get(pair[1]))));
        boolean[] taken = new boolean[theirs.size()];
        for (int[] pair : candidates) {
            if (pairs[pair[0]] == null && !taken[pair[1]]) {
                pairs[pair[0]] = theirs.get(pair[1]);
                taken[pair[1]] = true;
            }
        }
        return pairs;
    }

    private static double distance(PdfLines.Line a, PdfLines.Line b) {
        return Math.hypot(a.x() - b.x(), a.baseline() - b.baseline());
    }

    /**
     * A line's identity across runs: its page, a digest of its letters, and which of the page's
     * lines of those letters it is.
     */
    static String lineId(PdfLines.Line line, int occurrence) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(line.key().getBytes(StandardCharsets.UTF_8));
            String id = line.page() + ":" + HexFormat.of().formatHex(digest, 0, 6);
            return occurrence == 0 ? id : id + "#" + occurrence;
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(missing);
        }
    }

    private static String preview(String key) {
        return key.codePointCount(0, key.length()) <= 32 ? key : key.substring(0, key.offsetByCodePoints(0, 32));
    }

    private static double round(double points) {
        return Math.round(points * 100) / 100.0;
    }

    private static double quantile(List<Double> sorted, double share) {
        if (sorted.isEmpty()) {
            return 0;
        }
        return round(sorted.get((int) Math.round(share * (sorted.size() - 1))));
    }

    /** The measurement as a row of the baseline's document file. */
    String row() {
        return String.join("\t", stem, Integer.toString(enginePages), Integer.toString(editorPages),
                Integer.toString(lines), Integer.toString(matched),
                String.format(Locale.ROOT, "%.2f", median), String.format(Locale.ROOT, "%.2f", p90),
                Integer.toString(over2));
    }

    /** The measurement's found lines as rows of the baseline's line file. */
    List<String> lineRows() {
        List<String> rows = new ArrayList<>();
        found.forEach((id, line) -> rows.add(String.join("\t", stem, id,
                String.format(Locale.ROOT, "%.2f", line.drift()), line.preview())));
        return rows;
    }

    /** Reads a row of the baseline's document file, with the lines the line file holds for it. */
    static FidelityMeasurement parse(String row, Map<String, Found> found) {
        String[] cells = row.split("\t");
        if (cells.length != 8) {
            throw new IllegalArgumentException("a baseline row has 8 cells, not " + cells.length + ": " + row);
        }
        try {
            return new FidelityMeasurement(cells[0], Integer.parseInt(cells[1]), Integer.parseInt(cells[2]),
                    Integer.parseInt(cells[3]), Integer.parseInt(cells[4]), Double.parseDouble(cells[5]),
                    Double.parseDouble(cells[6]), Integer.parseInt(cells[7]), found);
        } catch (NumberFormatException malformed) {
            throw new IllegalArgumentException("a baseline row holds a cell that is not a number: " + row, malformed);
        }
    }

    /** Reads a row of the baseline's line file into its document's lines. */
    static void parseLine(String row, Map<String, Map<String, Found>> byDocument) {
        String[] cells = row.split("\t", 4);
        if (cells.length < 3) {
            throw new IllegalArgumentException("a baseline line row has at least 3 cells: " + row);
        }
        try {
            int page = Integer.parseInt(cells[1].substring(0, cells[1].indexOf(':')));
            byDocument.computeIfAbsent(cells[0], stem -> new LinkedHashMap<>())
                    .put(cells[1], new Found(page, cells.length > 3 ? cells[3] : "", Double.parseDouble(cells[2])));
        } catch (NumberFormatException | StringIndexOutOfBoundsException malformed) {
            throw new IllegalArgumentException("a baseline line row is malformed: " + row, malformed);
        }
    }
}
