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

    /** The {@code order="core render-pdf ..."} line publish.yml validates its resume against. */
    private static final Pattern TRAIN_ORDER =
            Pattern.compile("order=\"([^\"]+)\"");

    /**
     * The modules a release actually deploys, read from the publish workflows.
     *
     * <p>The independent inventory. Comparing the scan against CI alone answers a
     * narrower question than the one that matters: a module added to the publish train
     * and forgotten in both CI and the scan is missing from both sides of that
     * comparison, which is precisely the shape that keeps it green.</p>
     */
    static List<String> deployed(Path repoRoot) throws IOException {
        List<String> deployed = new ArrayList<>();
        deployedByWorkflow(repoRoot).values().forEach(modules -> modules.forEach(module -> {
            if (!deployed.contains(module)) {
                deployed.add(module);
            }
        }));
        return deployed;
    }

    /**
     * The modules each publish workflow deploys, keyed by the workflow's file name —
     * including the workflows that deploy none.
     *
     * <p>Attribution is what lets a caller tell "this workflow publishes nothing" from
     * "this workflow was not read". A flat list cannot: both look like a shorter list,
     * and a shorter list is exactly what a guard comparing against it wants to see.</p>
     *
     * @param repoRoot the repository root
     * @return every {@code publish*.yml}, mapped to the module directories it deploys
     * @throws IOException when a workflow cannot be read
     */
    static Map<String, List<String>> deployedByWorkflow(Path repoRoot) throws IOException {
        Path workflows = repoRoot.resolve(".github/workflows");
        Map<String, List<String>> byWorkflow = new LinkedHashMap<>();
        try (var files = Files.list(workflows)) {
            for (Path workflow : files.sorted().toList()) {
                String name = workflow.getFileName().toString();
                if (!name.startsWith("publish") || !name.endsWith(".yml")) {
                    continue;
                }
                List<String> modules = new ArrayList<>();
                for (String line : Files.readAllLines(workflow)) {
                    if (!line.contains("deploy")) {
                        continue;
                    }
                    Matcher module = DEPLOY_STEP.matcher(line);
                    if (module.find() && !modules.contains(module.group(1))) {
                        modules.add(module.group(1));
                    }
                }
                byWorkflow.put(name, modules);
            }
        }
        return byWorkflow;
    }

    /**
     * The publish train {@code publish.yml} declares for itself, in order.
     *
     * <p>The workflow states its module set twice — once as the {@code order} the resume
     * input is validated against, and once as the deploy steps themselves. Neither is
     * derived from the other, so holding them together catches the step list drifting
     * away from the train without anything having to restate it a third time.</p>
     *
     * @param repoRoot the repository root
     * @return the module directories the train names, or an empty list when the
     *         declaration is absent
     * @throws IOException when the workflow cannot be read
     */
    static List<String> declaredTrain(Path repoRoot) throws IOException {
        Path workflow = repoRoot.resolve(".github/workflows/publish.yml");
        if (!Files.isRegularFile(workflow)) {
            return List.of();
        }
        Matcher order = TRAIN_ORDER.matcher(Files.readString(workflow));
        return order.find() ? List.of(order.group(1).strip().split("\\s+")) : List.of();
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
