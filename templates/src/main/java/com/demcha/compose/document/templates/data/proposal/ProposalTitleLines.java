package com.demcha.compose.document.templates.data.proposal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A proposal's title, as the sheet sets it.
 *
 * <p>Designs break a title across a different number of lines — two, three or
 * four — and some set a short phrase above it and a standfirst under it. The
 * lines are therefore a list, and {@link #lead()}, {@link #second()} and
 * {@link #third()} are the first three of that list: one statement, two ways of
 * reading it, kept consistent by construction rather than by the caller.</p>
 *
 * <p>A title breaks where the design breaks it. The lines are the document's,
 * not a paragraph the engine wraps, because where a headline turns is a
 * typographic decision and not a consequence of the column it happens to sit
 * in.</p>
 *
 * @param lead       the first line, or blank when the title has none
 * @param second     the second line, or blank
 * @param third      the third line, or blank
 * @param eyebrow    the short phrase some designs set above the title (e.g.
 *                   {@code "Proposal for"}); blank when the title carries it
 *                   inside its own first line instead
 * @param lines      every line of the title, in order — the canonical reading
 * @param standfirst the paragraph under the title, one entry per printed line;
 *                   empty when the design sets none
 */
public record ProposalTitleLines(String lead, String second, String third,
                                 String eyebrow, List<String> lines,
                                 List<String> standfirst) {

    /**
     * Normalizes optional fields and keeps the list and the first three lines
     * saying the same thing.
     *
     * <p>Given lines, the three are read from it. Given no lines, the lines are
     * those of the three that are set. Given both — which only the canonical
     * constructor allows, and which neither convenience form can produce — the
     * list wins and the three are read from it, because the list is the one that
     * can hold a title of any length and reading it back is the only resolution
     * that loses nothing the caller stated.</p>
     */
    public ProposalTitleLines {
        eyebrow = Objects.requireNonNullElse(eyebrow, "");
        standfirst = List.copyOf(Objects.requireNonNullElse(standfirst, List.of()));
        List<String> stated = Objects.requireNonNullElse(lines, List.of());
        if (stated.isEmpty()) {
            lead = Objects.requireNonNullElse(lead, "");
            second = Objects.requireNonNullElse(second, "");
            third = Objects.requireNonNullElse(third, "");
            List<String> derived = new ArrayList<>();
            for (String line : List.of(lead, second, third)) {
                if (!line.isBlank()) {
                    derived.add(line);
                }
            }
            lines = List.copyOf(derived);
        } else {
            lines = List.copyOf(stated);
            lead = lines.size() > 0 ? lines.get(0) : "";
            second = lines.size() > 1 ? lines.get(1) : "";
            third = lines.size() > 2 ? lines.get(2) : "";
        }
    }

    /**
     * A title of up to three lines.
     *
     * @param lead   the first line
     * @param second the second line
     * @param third  the third line
     */
    public ProposalTitleLines(String lead, String second, String third) {
        this(lead, second, third, "", List.of(), List.of());
    }

    /**
     * A title of any number of lines, with the phrase above it and the paragraph
     * under it.
     *
     * <p>A factory rather than a second three-argument constructor: one taking
     * {@code (String, List, List)} beside one taking three strings is ambiguous
     * for a caller passing nulls, and a title is not the place to make someone
     * cast an argument to find out which they meant.</p>
     *
     * @param eyebrow    the phrase above the title, or blank
     * @param lines      every line of the title, in order
     * @param standfirst the paragraph under it, one entry per printed line
     * @return the title
     */
    public static ProposalTitleLines of(String eyebrow, List<String> lines,
                                        List<String> standfirst) {
        return new ProposalTitleLines("", "", "", eyebrow, lines, standfirst);
    }
}
