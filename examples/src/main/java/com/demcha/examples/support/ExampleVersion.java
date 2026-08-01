package com.demcha.examples.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The version examples print.
 *
 * <p>Read from the filtered {@code banner.properties} rather than written out,
 * because example documents are regenerated on every release: a literal keeps
 * announcing whichever line it was typed on. Shared so the value cannot drift
 * between the documents that show it.</p>
 *
 * <p>The reactor is the default, not the only answer:
 * {@link #DISPLAY_VERSION_PROPERTY} overrides it. Between releases the reactor
 * sits on the next patch, so a render taken from a development branch names a
 * version nobody can depend on yet — the override is how a published document
 * gets reproduced from a branch that has moved past it.</p>
 */
public final class ExampleVersion {

    private static final Pattern MAJOR_MINOR = Pattern.compile("^(\\d+)\\.(\\d+)");

    private static final String CURRENT = load();

    private ExampleVersion() {
    }

    /**
     * The full version to display — the override when one is set, otherwise the
     * reactor's, or {@code "dev"} when the resource is absent or unfiltered
     * (running straight from sources).
     *
     * @return version string
     */
    public static String current() {
        return CURRENT;
    }

    /**
     * The {@code major.minor} of {@link #current()}, for prose that names a line
     * rather than a patch.
     *
     * @return the leading {@code major.minor}, or the version unchanged when it
     *         carries no dotted numeric prefix
     */
    public static String currentLine() {
        return majorMinor(CURRENT);
    }

    /**
     * {@link #current()} without any pre-release qualifier: {@code "2.1.1"} for
     * {@code "2.1.1-SNAPSHOT"}.
     *
     * <p>Example documents are regenerated whenever their content changes, not
     * only at a release, so between cuts the reactor version carries a
     * {@code -SNAPSHOT} suffix. Rendering that suffix publishes a coordinate
     * nobody can resolve, and the committed decks did exactly that after an
     * off-cycle refresh. Anything a reader sees goes through here.</p>
     *
     * @return the version up to the first {@code -}
     */
    public static String withoutQualifier() {
        return CURRENT.replaceFirst("-.*$", "");
    }

    /**
     * Reduces a version to its {@code major.minor} prefix.
     *
     * @param version version string to reduce
     * @return the leading {@code major.minor}, or {@code version} unchanged when
     *         it carries no dotted numeric prefix
     */
    public static String majorMinor(String version) {
        Matcher matcher = MAJOR_MINOR.matcher(version);
        return matcher.find() ? matcher.group(1) + "." + matcher.group(2) : version;
    }

    /**
     * The version a render should display, when it is not the reactor's.
     *
     * <p>Between cuts the reactor sits on the next patch — {@code 2.1.1-SNAPSHOT} while
     * {@code 2.1.0} is what people can depend on — so a render taken from {@code develop}
     * names a version that does not exist yet. Stripping the qualifier is not enough:
     * the patch number itself has moved. Anything that needs to reproduce a published
     * document passes the published version here.</p>
     */
    public static final String DISPLAY_VERSION_PROPERTY = "graphcompose.examples.displayVersion";

    private static String load() {
        Properties banner = new Properties();
        try (InputStream in = ExampleVersion.class.getResourceAsStream("/banner.properties")) {
            if (in != null) {
                banner.load(in);
            }
        } catch (IOException ignored) {
            // Fall through to the development label below.
        }
        return resolve(System.getProperty(DISPLAY_VERSION_PROPERTY), banner.getProperty("version"));
    }

    /**
     * Chooses between the override and the filtered value.
     *
     * <p>Separate from {@link #load()} because the result is cached in a static
     * field: once any code has touched this class the property can no longer change
     * the answer, so a test that sets it proves nothing. The decision is testable;
     * the caching is not, and does not need to be.</p>
     *
     * @param override the {@link #DISPLAY_VERSION_PROPERTY} value, may be null
     * @param filtered the {@code version} entry from {@code banner.properties}
     * @return the version to display
     */
    static String resolve(String override, String filtered) {
        // The override wins, and it is the only way to render a version other than
        // the one the build carries. banner.properties keeps sourcing
        // @project.version@ — a guard requires that — so the reactor stays the
        // default answer.
        if (override != null && !override.isBlank()) {
            // A leading "v" is how a version is written in prose and how half the
            // render sites print it. Accepting it and stripping it once keeps the two
            // spellings from reaching the page as "vv2.1.0".
            return override.trim().replaceFirst("^[vV]", "");
        }
        return filtered == null || filtered.isBlank() || filtered.startsWith("@")
                ? "dev"
                : filtered.trim();
    }
}
