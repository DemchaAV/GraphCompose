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
 * guessed at from its text.</p>
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
     * @param path     the authored node's path, or null when it belongs to the whole export
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

        void add(Severity severity, String subject, String path, String detail) {
            notes.add(new Note(severity, subject, path, detail));
        }

        DocxExportReport build() {
            return notes.isEmpty() ? EMPTY : new DocxExportReport(notes);
        }
    }
}
