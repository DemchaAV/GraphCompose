package com.demcha.compose.document.templates.cv.data;

import com.demcha.compose.document.templates.core.identity.Link;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * One entry inside a {@link ModuleSection} — the universal record every
 * runtime-assembled module is built from.
 *
 * <p>A job, a degree, a project, a skill category, a paragraph of a
 * summary: all of them are a title plus some optional context plus a
 * description. Rather than a record per shape, this carries every
 * optional field and lets the section's {@link CvKind} decide which
 * ones it reads — a {@code period} is drawn by
 * {@link CvKind#ENTRIES_DATED} and ignored by {@link CvKind#ENTRIES},
 * with the same item on both sides. Each kind documents exactly what
 * it reads.</p>
 *
 * <p>Only {@code title} is required, and only because a module entry
 * with nothing to name it has nothing to render. Everything else is
 * blank, {@code null}, or empty when the author has nothing to say —
 * no placeholder text, no {@code "—"} stand-ins.</p>
 *
 * <p>Build one through {@link #of(String)} and the {@code with}-style
 * methods, which read in the order the fields render:</p>
 *
 * <pre>{@code
 * CvItem.of("Senior Backend Engineer")
 *       .at("Acme GmbH")
 *       .in("Berlin, DE")
 *       .period("2021 - Present")
 *       .bullets("Cut p99 latency 40%", "Led the payments migration");
 * }</pre>
 *
 * @param title     what the entry is called; required, non-blank. May
 *                  carry inline markdown, including {@code [text](url)}
 * @param link      optional click target for the title; {@code null}
 *                  when the title is not a link. A {@code link} and a
 *                  markdown link inside {@code title} do the same job —
 *                  prefer this one, which needs no escaping
 * @param subtitle  employer, institution, client; blank when absent
 * @param period    date or range as the author wants it written
 *                  ({@code "2021 - Present"}, {@code "2019"}); blank
 *                  when absent, and read only by dated kinds
 * @param location  city, country, or "Remote"; blank when absent
 * @param body      description lines; empty when the entry is a
 *                  heading only. One line renders as one paragraph or
 *                  one bullet, per {@code bodyStyle}
 * @param bodyStyle whether {@code body} reads as prose or as bullets
 * @since 2.3.0
 */
public record CvItem(String title, Link link, String subtitle, String period,
                     String location, List<String> body, BodyStyle bodyStyle) {

    /**
     * Validates the required {@code title}, normalises every optional
     * text field from {@code null} to blank, drops null or blank body
     * lines, and defensively copies the body list.
     */
    public CvItem {
        Objects.requireNonNull(title, "title");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        title = title.trim();
        subtitle = subtitle == null ? "" : subtitle.trim();
        period = period == null ? "" : period.trim();
        location = location == null ? "" : location.trim();
        bodyStyle = bodyStyle == null ? BodyStyle.PARAGRAPH : bodyStyle;

        List<String> cleaned = new ArrayList<>(body == null ? 0 : body.size());
        if (body != null) {
            for (String line : body) {
                if (line != null && !line.isBlank()) {
                    cleaned.add(line.trim());
                }
            }
        }
        body = List.copyOf(cleaned);
    }

    /**
     * An item with nothing but its title. Chain the {@code with}-style
     * methods below to add what the entry actually has.
     *
     * @param title what the entry is called; required, non-blank
     * @return a new item carrying only {@code title}
     */
    public static CvItem of(String title) {
        return new CvItem(title, null, "", "", "", List.of(), BodyStyle.PARAGRAPH);
    }

    /**
     * Returns a copy whose title links to {@code url}.
     *
     * @param url click target; blank or null clears the link
     * @return a copy carrying the link
     */
    public CvItem linkedTo(String url) {
        Link target = url == null || url.isBlank() ? null : Link.of(title, url);
        return new CvItem(title, target, subtitle, period, location, body, bodyStyle);
    }

    /**
     * Returns a copy with the employer / institution / client line.
     *
     * @param value subtitle text; null or blank leaves the line out
     * @return a copy carrying the subtitle
     */
    public CvItem at(String value) {
        return new CvItem(title, link, value, period, location, body, bodyStyle);
    }

    /**
     * Returns a copy with the location line.
     *
     * @param value city, country, or "Remote"; null or blank leaves it out
     * @return a copy carrying the location
     */
    public CvItem in(String value) {
        return new CvItem(title, link, subtitle, period, value, body, bodyStyle);
    }

    /**
     * Returns a copy with the date or range, written as the author
     * wants it. Read only by {@link CvKind#ENTRIES_DATED}.
     *
     * @param value date or range; null or blank leaves it out
     * @return a copy carrying the period
     */
    public CvItem period(String value) {
        return new CvItem(title, link, subtitle, value, location, body, bodyStyle);
    }

    /**
     * Returns a copy whose description reads as prose, one paragraph
     * per line.
     *
     * @param lines description lines; null or blank lines are dropped
     * @return a copy carrying the description
     */
    public CvItem paragraphs(String... lines) {
        return withBody(lines, BodyStyle.PARAGRAPH);
    }

    /**
     * Returns a copy whose description reads as a bulleted list, one
     * bullet per line.
     *
     * @param lines description lines; null or blank lines are dropped
     * @return a copy carrying the description
     */
    public CvItem bullets(String... lines) {
        return withBody(lines, BodyStyle.BULLETS);
    }

    /**
     * Returns a copy with an explicit body list and style — the
     * variant for callers holding a {@code List} they did not build
     * literally (an import layer, a JSON mapper).
     *
     * @param lines description lines; null or blank lines are dropped
     * @param style whether the lines read as prose or as bullets
     * @return a copy carrying the description
     */
    public CvItem body(List<String> lines, BodyStyle style) {
        return new CvItem(title, link, subtitle, period, location, lines, style);
    }

    /**
     * The click target for this item's title, if it has one.
     *
     * @return the link URL, or blank when the title is not a link
     */
    public String url() {
        return link == null ? "" : link.url();
    }

    private CvItem withBody(String[] lines, BodyStyle style) {
        // Arrays.asList, not List.of: a null line is dropped by the canonical
        // constructor, and List.of would throw before it ever got there.
        List<String> values = lines == null ? List.of() : Arrays.asList(lines);
        return new CvItem(title, link, subtitle, period, location, values, style);
    }
}
