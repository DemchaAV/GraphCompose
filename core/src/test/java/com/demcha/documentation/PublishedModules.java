package com.demcha.documentation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The reactor's modules, resolved from the root {@code pom.xml} for the guards that
 * reason about them.
 *
 * <p>Two guards need the same answer to "what is a module, and which directory is it" —
 * one checks that every backend package is documented, the other that every compiled
 * module is scanned. Answering it twice is how the lists this repository keeps fixing
 * came apart in the first place.</p>
 */
final class PublishedModules {

    private static final Pattern MODULE = Pattern.compile("<module>\\s*([^<]+?)\\s*</module>");
    private static final Pattern ARTIFACT_ID = Pattern.compile("<artifactId>\\s*([^<]+?)\\s*</artifactId>");
    private static final Pattern PARENT_BLOCK =
            Pattern.compile("<parent>.*?</parent>", Pattern.DOTALL);

    private PublishedModules() {
    }

    /** A publish workflow's deploy step: {@code -f <module>/pom.xml … deploy}. */
    private static final Pattern DEPLOY_STEP =
            Pattern.compile("-f\\s+([\\w-]+)/pom\\.xml");

    /**
     * The modules a release actually deploys, read from the publish workflows.
     *
     * <p>The independent inventory. Comparing the scan against CI alone answers a
     * narrower question than the one that matters: a module added to the publish train
     * and forgotten in both CI and the scan is missing from both sides of that
     * comparison, which is precisely the shape that keeps it green.</p>
     */
    static List<String> deployed(Path repoRoot) throws IOException {
        Path workflows = repoRoot.resolve(".github/workflows");
        List<String> deployed = new ArrayList<>();
        try (var files = Files.list(workflows)) {
            for (Path workflow : files.sorted().toList()) {
                String name = workflow.getFileName().toString();
                if (!name.startsWith("publish") || !name.endsWith(".yml")) {
                    continue;
                }
                for (String line : Files.readAllLines(workflow)) {
                    if (!line.contains("deploy")) {
                        continue;
                    }
                    Matcher module = DEPLOY_STEP.matcher(line);
                    if (module.find() && !deployed.contains(module.group(1))) {
                        deployed.add(module.group(1));
                    }
                }
            }
        }
        return deployed;
    }

    /** The module directories the root reactor builds, in declaration order. */
    static List<String> of(Path repoRoot) throws IOException {
        String rootPom = Files.readString(repoRoot.resolve("pom.xml"));
        List<String> modules = new ArrayList<>();
        Matcher matcher = MODULE.matcher(rootPom);
        while (matcher.find()) {
            modules.add(matcher.group(1));
        }
        return modules;
    }

    /**
     * Each module's own artifact id, mapped to its directory.
     *
     * <p>Read from the module's own {@code <artifactId>} rather than by searching the
     * poms for a name: every pom that <em>depends</em> on a module also contains that
     * module's artifact id, so a search binds {@code graph-compose-testing} to whichever
     * dependent happens to come first in the reactor.</p>
     */
    static Map<String, Path> byArtifactId(Path repoRoot) throws IOException {
        Map<String, Path> modules = new LinkedHashMap<>();
        for (String module : of(repoRoot)) {
            Path pom = repoRoot.resolve(module).resolve("pom.xml");
            if (!Files.isRegularFile(pom)) {
                continue;
            }
            // The inherited coordinate sits in <parent> above the module's own; drop it
            // so the first remaining artifactId is the module speaking about itself.
            String ownCoordinates = PARENT_BLOCK.matcher(Files.readString(pom)).replaceFirst("");
            Matcher artifactId = ARTIFACT_ID.matcher(ownCoordinates);
            if (artifactId.find()) {
                modules.put(artifactId.group(1), repoRoot.resolve(module));
            }
        }
        return modules;
    }
}
