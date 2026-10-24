package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.api.Beta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * What an export could not carry, and where in the document it was.
 *
 * <p>A Word document cannot hold everything a page can draw, and this export says so
 * rather than approximating in silence — but until now it said so to the log, one line per
 * kind, with no way for the calling program to find out. A service generating documents
 * for other people has no log to read: it needs to know whether the file it is about to
 * send dropped a chart, and which one.</p>
 *
 * <p>Two kinds of note, and the difference matters. Something {@link Severity#DROPPED} is
 * not in the file: the page draws it and the document does not. Something
 * {@link Severity#APPROXIMATED} is in the file as the nearest thing Word owns — a panel
 * with square corners where the page rounds them. Neither is an error; an export that
 * cannot proceed throws instead, and a report is never a substitute for that.</p>
 *
 * <p>Every note carries the path of the node it came from, the same path the layout graph
 * addresses that node by, so a note can be traced back to the authoring code rather than
 * guessed at from its text. What a table's cell is composed of, which the layout lays out under
 * the table and gives no path of its own, is named under the table's path by its cell —
 * {@code row[r]/cell[c]}, the row's index among the table's rows and the cell's among the cells
 * authored for that row, from 0, not its column in the grid — then by its own segment, as in
 * {@code Invoice[0]/Lines[2]/row[3]/cell[1]/Total[0]}.</p>
 *
 * <p>A note about no one node says what it is about instead: {@code page N} for a page, and in a
 * document of several sections {@code section N} for one of them or {@code section N, page N}. A
 * note about the whole document has no path. One about a page zone, or about a node of one, which
 * the layout lays out apart from the body, has none either, or names its section in a document of
 * several.</p>
 *
 * <p><b>Experimental</b> ({@code @Beta}) — see {@code docs/api-stability.md}.</p>
 *
 * @param notes everything worth telling the caller, in the order it was found
 * @author Artem Demchyshyn
 * @since 2.5.0
 */
@Beta
public record DocxExportReport(List<Note> notes) {

    /** An empty report: the whole document was written as it was authored. */
    public static final DocxExportReport EMPTY = new DocxExportReport(List.of());

    /**
     * Freezes the notes.
     */
    public DocxExportReport {
        notes = List.copyOf(notes);
    }

    /** How much of the thing survived. */
    public enum Severity {
        /** The page draws it and the document does not carry it at all. */
        DROPPED,
        /** It is in the document as the nearest thing Word owns, which is not the same. */
        APPROXIMATED
    }

    /**
     * One thing the export could not carry as authored.
     *
     * @param severity whether it is missing or merely different
     * @param subject  what it was, in a few words — {@code "chart"}, {@code "corner radius"}
     * @param path     the authored node's path; a page's or a section's where the note is about one
     *                 ({@code page N}, {@code section N}, {@code section N, page N}); or null where
     *                 it belongs to the whole export; about a page zone or a node of one, null or,
     *                 in a document of several sections, its section
     * @param detail   what happened and what it means for the document
     */
    public record Note(Severity severity, String subject, String path, String detail) {

        /**
         * Validates the parts a reader needs.
         */
        public Note {
            if (severity == null || subject == null || subject.isBlank()) {
                throw new IllegalArgumentException("a note states its severity and its subject");
            }
            detail = detail == null ? "" : detail;
        }

        @Override
        public String toString() {
            return severity + " " + subject + (path == null ? "" : " at " + path)
                   + (detail.isEmpty() ? "" : ": " + detail);
        }
    }

    /** @return true when nothing was dropped or approximated */
    public boolean isEmpty() {
        return notes.isEmpty();
    }

    /**
     * @param severity the severity to count
     * @return how many notes carry it
     */
    public long count(Severity severity) {
        return notes.stream().filter(note -> note.severity() == severity).count();
    }

    /**
     * The notes grouped by what they are about, for a caller summarising rather than
     * listing.
     *
     * @return subject to its notes, in the order each subject was first seen
     */
    public Map<String, List<Note>> bySubject() {
        return notes.stream().collect(Collectors.groupingBy(Note::subject,
                java.util.LinkedHashMap::new, Collectors.toList()));
    }

    /** Builds a report while an export runs. Not thread-safe; one export owns one. */
    static final class Builder {

        private final List<Note> notes = new ArrayList<>();

        /** Notes said already, which an equal one added now repeats: each stands for one. */
        private final List<Note> saidAlready = new ArrayList<>();

        void add(Severity severity, String subject, String path, String detail) {
            Note note = new Note(severity, subject, path, detail);
            if (!saidAlready.remove(note)) {
                notes.add(note);
            }
        }

        /**
         * Takes the notes added from one count to another as said already: until
         * {@link #stopSkipping}, a note equal to one of them is not added again, once for each.
         * A page zone built otherwise for the first page is written twice, and a note both its
         * lines give alike is given once, while two losses of the second line alike stay two.
         *
         * @param from the count before the first line was written
         * @param to   the count after it
         */
        void skipRepeatsOf(int from, int to) {
            saidAlready.clear();
            saidAlready.addAll(notes.subList(from, to));
        }

        /** Ends {@link #skipRepeatsOf}: every note is added again. */
        void stopSkipping() {
            saidAlready.clear();
        }

        /** How many notes the report holds so far. */
        int size() {
            return notes.size();
        }

        /** Puts a note in the place of the one at an index: what the export said before it knew better. */
        void set(int index, Severity severity, String subject, String path, String detail) {
            notes.set(index, new Note(severity, subject, path, detail));
        }

        /** Puts a note in at an index, where the node it is about was written among the others. */
        void insert(int index, Severity severity, String subject, String path, String detail) {
            notes.add(index, new Note(severity, subject, path, detail));
        }

        /**
         * Puts a note in the place of each one on the same subject and path from the given one on —
         * what the export said of it before it knew better — or adds it where there is none.
         *
         * @param from the first note it may replace: a section's first, as paths are a section's own
         */
        void replace(int from, Severity severity, String subject, String path, String detail) {
            boolean replaced = false;
            for (int index = from; index < notes.size(); index++) {
                Note note = notes.get(index);
                if (note.subject().equals(subject) && java.util.Objects.equals(note.path(), path)) {
                    notes.set(index, new Note(severity, subject, path, detail));
                    replaced = true;
                }
            }
            if (!replaced) {
                add(severity, subject, path, detail);
            }
        }

        /**
         * Adds a phrase to the note on a node among those added while it was written, after what
         * that note says already, or adds a note of its own where the node has none: one node, one
         * note. A node with no path is not told from the others, and has a note of its own. No note
         * moves: the places of those a later step puts in or replaces stay where they were counted.
         *
         * @param from   the first note that may be the node's
         * @param to     the note after the last that may be: a later one with its path may be a later
         *               section's node, as paths are a section's own
         * @param phrase what the node also loses, added to its note
         * @param alone  the whole note, where the node has none
         */
        void addTo(int from, int to, Severity severity, String subject, String path, String phrase, String alone) {
            for (int index = from; path != null && index < Math.min(to, notes.size()); index++) {
                Note note = notes.get(index);
                if (note.subject().equals(subject) && path.equals(note.path())) {
                    notes.set(index, new Note(note.severity(), subject, path, note.detail() + "; " + phrase));
                    return;
                }
            }
            add(severity, subject, path, alone);
        }

        DocxExportReport build() {
            return notes.isEmpty() ? EMPTY : new DocxExportReport(notes);
        }
    }
}
