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
 * <p>Several guards need the same answer to "what is a module, and which directory is
 * it" — one checks that every backend package is documented, one that every compiled
 * module is scanned, one that a release publishes exactly the lockstep train. Answering
 * it more than once is how the lists this repository keeps fixing came apart in the
 * first place.</p>
 */
final class PublishedModules {

    private static final Pattern MODULE = Pattern.compile("<module>\\s*([^<]+?)\\s*</module>");
    private static final Pattern ARTIFACT_ID = Pattern.compile("<artifactId>\\s*([^<]+?)\\s*</artifactId>");
    private static final Pattern PARENT_BLOCK =
            Pattern.compile("<parent>.*?</parent>", Pattern.DOTALL);

    private PublishedModules() {
    }

    /**
     * A {@code mvnw} invocation that runs the {@code deploy} goal, with or without the
     * {@code run:} key in front. A comment that mentions deploying is not one, which is
     * why the line has to start with the command rather than merely contain it.
     */
    private static final Pattern DEPLOY_COMMAND =
            Pattern.compile("^\\s*(?:run:\\s*)?\\./mvnw\\b.*\\sdeploy(?:\\s|$).*");

    /** A standalone deploy's module: {@code -f <module>/pom.xml}. */
    private static final Pattern DEPLOY_STEP =
            Pattern.compile("-f\\s+([\\w-]+)/pom\\.xml");

    /** A reactor deploy's module selection: {@code -pl :a,:b,…}. */
    private static final Pattern DEPLOY_SELECTION =
            Pattern.compile("\\s-pl\\s+(\\S+)");

    private static final Pattern VERSION = Pattern.compile("<version>\\s*([^<]+?)\\s*</version>");

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
        Map<String, Path> byArtifactId = byArtifactId(repoRoot);
        Map<String, List<String>> byWorkflow = new LinkedHashMap<>();
        try (var files = Files.list(workflows)) {
            for (Path workflow : files.sorted().toList()) {
                String name = workflow.getFileName().toString();
                if (!name.startsWith("publish") || !name.endsWith(".yml")) {
                    continue;
                }
                List<String> modules = new ArrayList<>();
                for (String command : deployCommands(workflow)) {
                    for (String module : modulesOf(command, byArtifactId)) {
                        if (!modules.contains(module)) {
                            modules.add(module);
                        }
                    }
                }
                byWorkflow.put(name, modules);
            }
        }
        return byWorkflow;
    }

    /**
     * Every {@code mvnw … deploy} invocation in a workflow, one per element.
     *
     * @param workflow the workflow file
     * @return the deploy command lines, in file order
     * @throws IOException when the workflow cannot be read
     */
    static List<String> deployCommands(Path workflow) throws IOException {
        List<String> commands = new ArrayList<>();
        for (String line : Files.readAllLines(workflow)) {
            if (DEPLOY_COMMAND.matcher(line).matches()) {
                commands.add(line.strip());
            }
        }
        return commands;
    }

    /**
     * The module directories one deploy command ships: the {@code -f} module of a
     * standalone deploy, and each {@code -pl} selector of a reactor deploy.
     *
     * <p>A selector that names no reactor module is kept as written rather than dropped.
     * Dropping it would shrink the inventory, and a shorter inventory is exactly what a
     * guard comparing against it wants to see; kept, it fails every comparison it enters.</p>
     */
    private static List<String> modulesOf(String command, Map<String, Path> byArtifactId) {
        List<String> modules = new ArrayList<>();
        Matcher standalone = DEPLOY_STEP.matcher(command);
        if (standalone.find()) {
            modules.add(standalone.group(1));
        }
        Matcher selection = DEPLOY_SELECTION.matcher(command);
        if (selection.find()) {
            for (String selector : selection.group(1).split(",")) {
                String artifactId = selector.strip().replaceFirst("^:", "");
                if (artifactId.isEmpty()) {
                    continue;
                }
                Path directory = byArtifactId.get(artifactId);
                modules.add(directory == null ? selector.strip() : directory.getFileName().toString());
            }
        }
        return modules;
    }

    /**
     * The modules that move with the engine version and publish to Maven Central — the
     * train a GraphCompose release ships — read from the poms rather than from any
     * workflow, so it can be held against the workflow without restating either.
     *
     * <p>A module belongs when its pom stands alone (no {@code <parent>}, so it is not one
     * of the aggregator's build-only children), declares the
     * {@code central-publishing-maven-plugin}, and carries the reactor's own version. The
     * independently versioned companions (fonts, emoji) fail the last test by design,
     * which is what keeps them out of the train.</p>
     *
     * @param repoRoot the repository root
     * @return the train's module directories, in reactor declaration order
     * @throws IOException when a pom cannot be read
     */
    static List<String> lockstepPublished(Path repoRoot) throws IOException {
        String reactorVersion = ownVersion(Files.readString(repoRoot.resolve("pom.xml")));
        List<String> train = new ArrayList<>();
        for (String module : of(repoRoot)) {
            Path pom = repoRoot.resolve(module).resolve("pom.xml");
            if (!Files.isRegularFile(pom)) {
                continue;
            }
            String text = Files.readString(pom);
            if (PARENT_BLOCK.matcher(text).find()
                    || !text.contains("<artifactId>central-publishing-maven-plugin</artifactId>")) {
                continue;
            }
            if (reactorVersion.equals(ownVersion(text))) {
                train.add(module);
            }
        }
        return train;
    }

    /**
     * The modules the root aggregator builds but never publishes — its own children,
     * which inherit {@code maven.deploy.skip} and carry no publishing setup.
     *
     * @param repoRoot the repository root
     * @return the build-only module directories, in reactor declaration order
     * @throws IOException when a pom cannot be read
     */
    static List<String> buildOnly(Path repoRoot) throws IOException {
        List<String> children = new ArrayList<>();
        for (String module : of(repoRoot)) {
            Path pom = repoRoot.resolve(module).resolve("pom.xml");
            if (Files.isRegularFile(pom) && PARENT_BLOCK.matcher(Files.readString(pom)).find()) {
                children.add(module);
            }
        }
        return children;
    }

    /** A pom's own {@code <version>}, ignoring the one inside {@code <parent>}. */
    private static String ownVersion(String pom) {
        Matcher version = VERSION.matcher(PARENT_BLOCK.matcher(pom).replaceFirst(""));
        return version.find() ? version.group(1) : "";
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
