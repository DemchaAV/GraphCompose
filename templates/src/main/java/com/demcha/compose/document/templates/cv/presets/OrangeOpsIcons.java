package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.svg.SvgIcon;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.BADGE_GLYPH_SHARE;

/**
 * Classpath loader for the Orange Ops CV marks: the contact glyphs, the four
 * achievement marks, the four metric marks, the graduation cap and the four
 * marks the closing block draws.
 *
 * <p>{@link #WEBSITE} is the design's own globe under a second name. A link
 * that is not a network the preset recognises still needs a mark, and giving
 * the fallback its own token keeps the closing block's {@code systems} line
 * reading as what it is rather than doubling as the web glyph.</p>
 *
 * <p>They ship inside the templates artifact under
 * {@code templates/cv/orange-ops/icons/} as SVG, so they scale with the box
 * they are drawn into rather than being resampled. Sources and parsed icons are
 * cached per token, so a sheet that repeats a mark reads each file once per
 * JVM.</p>
 *
 * <p>{@link #ENTRY_TOKENS} is the vocabulary a document names through
 * {@code CvEntry.icon()}. It is scoped to this preset — another preset packages
 * its own set — and each mark carries the width it is drawn at, because an
 * icon's box is not its glyph and the sizes are per-kind.</p>
 */
final class OrangeOpsIcons {

    static final String PHONE = "phone";
    static final String EMAIL = "email";
    static final String LOCATION = "location";
    static final String LINKEDIN = "linkedin";
    static final String WEBSITE = "website";
    static final String GRADUATION = "graduation";

    private static final String ICON_ROOT = "/templates/cv/orange-ops/icons/";

    /** The width each mark is drawn at, as the design sizes it. */
    private static final Map<String, Double> SIZES = Map.ofEntries(
            Map.entry(PHONE, 8.5),
            Map.entry(EMAIL, 8.5),
            Map.entry(LOCATION, 8.5),
            Map.entry(LINKEDIN, 8.5),
            Map.entry(WEBSITE, 8.5),
            Map.entry("achievement-productivity", 16.0),
            Map.entry("achievement-accuracy", 16.0),
            Map.entry("achievement-safety", 16.5),
            Map.entry("achievement-savings", 16.0),
            Map.entry(GRADUATION, 15.0),
            Map.entry("kpi-productivity", 20.0),
            Map.entry("kpi-accuracy", 20.0),
            Map.entry("kpi-delivery", 20.0),
            Map.entry("kpi-safety", 20.0),
            Map.entry("systems", 9.5),
            Map.entry("languages", 9.5),
            Map.entry("driving", 9.5),
            Map.entry("interests", 9.5));

    /** The marks a document may name on an entry. */
    static final Set<String> ENTRY_TOKENS = Set.of(
            "achievement-productivity", "achievement-accuracy", "achievement-safety",
            "achievement-savings", GRADUATION,
            "kpi-productivity", "kpi-accuracy", "kpi-delivery", "kpi-safety",
            "systems", "languages", "driving", "interests");

    private static final Pattern VIEW_BOX = Pattern.compile("viewBox\\s*=\\s*\"([^\"]+)\"");

    private static final Map<String, String> SOURCES = new ConcurrentHashMap<>();
    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, SvgIcon> BADGES = new ConcurrentHashMap<>();

    private OrangeOpsIcons() {
    }

    /**
     * The width one mark is drawn at.
     *
     * @param token one of the packaged tokens
     * @return the drawn width in points
     * @throws IllegalArgumentException when the token names no packaged mark,
     *         listing the ones a document may choose — an icon token is data,
     *         so a wrong one is a data error
     */
    static double size(String token) {
        Double size = SIZES.get(token);
        if (size == null) {
            throw new IllegalArgumentException(
                    "Unknown orange ops CV icon token '" + token
                            + "'. This preset packages " + ENTRY_TOKENS.stream().sorted().toList()
                            + " for an entry mark.");
        }
        return size;
    }

    /**
     * Reads one mark.
     *
     * @param token one of the packaged tokens
     * @return the parsed icon
     */
    static SvgIcon icon(String token) {
        size(token);
        return CACHE.computeIfAbsent(token, t -> SvgIcon.parse(source(t)));
    }

    /**
     * One mark re-emitted inside a filled disc.
     *
     * <p>The design draws the achievement and education badges as a filled
     * circle owning an icon, and a shape container is what that is for. It is
     * not what the engine renders here: a shape container placed inside a table
     * cell — one level below the row cell the aside already is — reserves its
     * box and composes none of its children, which leaves bare discs. So the
     * disc is drawn <em>into</em> the mark: the glyph keeps its own user space
     * and the viewBox is widened around it with a filled circle behind, which
     * leaves one leaf node where there were two and keeps every table cell
     * holding nothing but a paragraph. Widening a viewBox also stays inside the
     * reader's documented subset in the way a transform would not.</p>
     *
     * @param token one of the packaged tokens
     * @param fill  the disc colour
     * @return the composed icon
     */
    static SvgIcon badge(String token, DocumentColor fill) {
        // Validated the same way an inline mark is: a badge reads the source
        // rather than the size, so without this an unknown token would surface
        // as a missing resource instead of the data error naming the set.
        size(token);
        Color rgb = fill.color();
        String key = token + '@' + rgb.getRGB();
        return BADGES.computeIfAbsent(key, ignored -> SvgIcon.parse(discAround(token, rgb)));
    }

    private static String discAround(String token, Color fill) {
        String source = source(token);
        double[] box = viewBox(source);
        double glyph = Math.max(box[2], box[3]);
        double disc = glyph / BADGE_GLYPH_SHARE;
        double pad = (disc - glyph) / 2.0;
        double cx = box[0] + box[2] / 2.0;
        double cy = box[1] + box[3] / 2.0;
        double r = disc / 2.0;
        String hex = String.format("#%02X%02X%02X", fill.getRed(), fill.getGreen(), fill.getBlue());
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\""
                + fmt(box[0] - pad) + " " + fmt(box[1] - pad) + " " + fmt(disc) + " " + fmt(disc)
                + "\"><path fill=\"" + hex + "\" d=\""
                + "M" + fmt(cx - r) + "," + fmt(cy)
                + " A" + fmt(r) + "," + fmt(r) + " 0 1,0 " + fmt(cx + r) + "," + fmt(cy)
                + " A" + fmt(r) + "," + fmt(r) + " 0 1,0 " + fmt(cx - r) + "," + fmt(cy)
                + " Z\"/>" + innerMarkup(source) + "</svg>";
    }

    /** The four viewBox numbers, or the square the mark set defaults to. */
    private static double[] viewBox(String svg) {
        Matcher matcher = VIEW_BOX.matcher(svg);
        if (!matcher.find()) {
            return new double[] {0, 0, 256, 256};
        }
        String[] parts = matcher.group(1).trim().split("[\\s,]+");
        if (parts.length != 4) {
            return new double[] {0, 0, 256, 256};
        }
        double[] box = new double[4];
        for (int i = 0; i < 4; i++) {
            box[i] = Double.parseDouble(parts[i]);
        }
        return box;
    }

    /** Everything the source draws, between its own svg tags. */
    private static String innerMarkup(String svg) {
        int open = svg.indexOf('>', svg.indexOf("<svg"));
        int close = svg.lastIndexOf("</svg>");
        if (open < 0 || close < 0 || close <= open) {
            throw new IllegalStateException("Orange ops CV icon markup is not an <svg> element");
        }
        return svg.substring(open + 1, close);
    }

    private static String fmt(double value) {
        return String.valueOf(Math.round(value * 100.0) / 100.0);
    }

    private static String source(String token) {
        return SOURCES.computeIfAbsent(token, OrangeOpsIcons::read);
    }

    private static String read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = OrangeOpsIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing orange ops CV icon: " + resourcePath);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read orange ops CV icon: " + resourcePath, e);
        }
    }
}
