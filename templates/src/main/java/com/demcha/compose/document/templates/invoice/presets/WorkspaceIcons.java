package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.svg.SvgIcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Classpath loader for the Workspace invoice marks: the two party glyphs, the
 * three a service line may take, and the three the preset places itself — the
 * bank on the payment card, the information mark on its note, and the calendar
 * on the due panel.
 *
 * <p>They ship inside the templates artifact under
 * {@code templates/invoice/workspace/icons/} as SVG, so they scale with the box
 * they are drawn into rather than being resampled. Parsed icons are cached per
 * token, so a sheet that repeats a mark reads each file once per JVM.</p>
 *
 * <h2>The set is coloured, not tinted</h2>
 *
 * <p>Each glyph carries its colour in its own markup: white for the marks that
 * are knocked out of a filled disc or tile, accent for the ones that stand on
 * the page. A token therefore names a glyph <em>and</em> where it is meant to
 * sit.</p>
 *
 * <p>{@link #LINE_TOKENS} is the vocabulary a document names through
 * {@code InvoiceServiceLines.Line.icon()}. It is scoped to this preset —
 * another preset packages its own set.</p>
 */
final class WorkspaceIcons {

    static final String BILL_TO = "bill-to";
    static final String SHIP_TO = "ship-to";
    static final String BANK = "bank";
    static final String INFO = "info";
    static final String CALENDAR = "calendar";

    private static final String ICON_ROOT = "/templates/invoice/workspace/icons/";

    /** The marks a document may name on a service line. */
    static final Set<String> LINE_TOKENS = Set.of("search", "grid", "shield");

    private static final Set<String> TOKENS = Set.of(
            BILL_TO, SHIP_TO, BANK, INFO, CALENDAR, "search", "grid", "shield");

    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private WorkspaceIcons() {
    }

    /**
     * Reads one mark.
     *
     * @param token one of the packaged tokens
     * @return the parsed icon
     * @throws IllegalArgumentException when the token names no packaged mark,
     *         listing the ones a document may choose — an icon token is data,
     *         so a wrong one is a data error
     */
    static SvgIcon icon(String token) {
        if (!TOKENS.contains(token)) {
            throw new IllegalArgumentException(
                    "Unknown workspace invoice icon token '" + token
                            + "'. This preset packages " + LINE_TOKENS.stream().sorted().toList()
                            + " for a service line's mark.");
        }
        return CACHE.computeIfAbsent(token, WorkspaceIcons::read);
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = WorkspaceIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing workspace invoice icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read workspace invoice icon: " + resourcePath, e);
        }
    }
}
