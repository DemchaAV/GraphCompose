package com.demcha.examples.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The reactor version, for examples that print it.
 *
 * <p>Read from the filtered {@code banner.properties} rather than written out,
 * because example documents are regenerated on every release: a literal keeps
 * announcing whichever line it was typed on. Shared so the value cannot drift
 * between the documents that show it.</p>
 */
public final class ExampleVersion {

    private static final Pattern MAJOR_MINOR = Pattern.compile("^(\\d+)\\.(\\d+)");

    private static final String CURRENT = load();

    private ExampleVersion() {
    }

    /**
     * The full reactor version, or {@code "dev"} when the resource is absent or
     * unfiltered (running straight from sources).
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

    private static String load() {
        Properties banner = new Properties();
        try (InputStream in = ExampleVersion.class.getResourceAsStream("/banner.properties")) {
            if (in != null) {
                banner.load(in);
            }
        } catch (IOException ignored) {
            // Fall through to the development label below.
        }
        String value = banner.getProperty("version");
        return value == null || value.isBlank() || value.startsWith("@") ? "dev" : value.trim();
    }
}
