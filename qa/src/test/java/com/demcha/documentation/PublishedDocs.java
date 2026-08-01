package com.demcha.documentation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The documentation a reader actually lands on, resolved once for every guard that
 * checks it.
 *
 * <p>Each guard used to carry its own idea of the set, and two of them had drifted:
 * one named nine READMEs literally, the other walked the docs tree including the
 * gitignored planning notes. A module added to the build then inherited one guard and
 * not the other, silently. The module list comes from the root {@code pom.xml}, so
 * "a module" means the same thing here as it does to Maven.</p>
 */
final class PublishedDocs {

    private static final Pattern MODULE = Pattern.compile("<module>\\s*([^<]+?)\\s*</module>");

    private PublishedDocs() {
    }

    /** The root README plus one per Maven module, in reactor order. */
    static List<Path> readmes(Path repoRoot) throws IOException {
        List<Path> readmes = new ArrayList<>();
        Path rootReadme = repoRoot.resolve("README.md");
        if (Files.isRegularFile(rootReadme)) {
            readmes.add(rootReadme);
        }
        for (String module : modules(repoRoot)) {
            Path readme = repoRoot.resolve(module).resolve("README.md");
            if (Files.isRegularFile(readme)) {
                readmes.add(readme);
            }
        }
        return readmes;
    }

    /**
     * Every page under {@code docs/} except {@code docs/private/} — gitignored planning
     * material that never reaches a reader, and whose failures CI could not reproduce.
     */
    static List<Path> docsPages(Path repoRoot) throws IOException {
        Path docsRoot = repoRoot.resolve("docs");
        if (!Files.isDirectory(docsRoot)) {
            return List.of();
        }
        Path privateDocs = docsRoot.resolve("private");
        try (Stream<Path> paths = Files.walk(docsRoot)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".md"))
                    .filter(path -> !path.startsWith(privateDocs))
                    .sorted()
                    .toList();
        }
    }

    /** The docs tree and the READMEs together, without duplicates. */
    static List<Path> all(Path repoRoot) throws IOException {
        List<Path> pages = new ArrayList<>(docsPages(repoRoot));
        for (Path readme : readmes(repoRoot)) {
            if (!pages.contains(readme)) {
                pages.add(readme);
            }
        }
        return pages;
    }

    /** The module directories the root reactor builds. */
    static List<String> modules(Path repoRoot) throws IOException {
        String pom = Files.readString(repoRoot.resolve("pom.xml"));
        List<String> modules = new ArrayList<>();
        Matcher matcher = MODULE.matcher(pom);
        while (matcher.find()) {
            modules.add(matcher.group(1));
        }
        return modules;
    }
}
