package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalSurfaceGuardTest {
    private static final Path PROJECT_ROOT = RepoRoot.get();
    private static final List<String> FORBIDDEN_TOKENS = List.of(
            "com.demcha.templates",
            "com.demcha.compose.v2",
            "TemplateBuilder",
            "GraphCompose.pdf(",
            "PdfComposer",
            "MainPageCV",
            "MainPageCvDTO",
            "ModuleYml",
            "ModuleSummary");

    /**
     * Names the 2.0 line removed — types, and the packages that held them. Every one is
     * absent from every {@code src/main} tree, so a contributor-facing document naming
     * one is teaching code that cannot compile — which is how the template-authoring
     * and engine-primitive sections of CONTRIBUTING went stale for a full release
     * cycle, and how two dead package names outlived the types that lived in them.
     *
     * <p>Deliberately absent: {@code BusinessTheme} survives as an examples-local
     * theme helper the example catalogue still uses, and {@code PptxSemanticBackend}
     * still ships beside the fixed-layout PPTX backend. Forbidding either would fail
     * the build on text that is correct.</p>
     */
    private static final List<String> RETIRED_IN_2_0_TOKENS = List.of(
            "InvoiceTemplateV2",
            "ProposalTemplateV2",
            "WeeklyScheduleTemplateV1",
            "EntityBounds",
            "ParentContainerUpdater",
            "ParentComponent",
            "Breakable",
            "hasRender(",
            "CvSpec",
            "CvBuilder",
            // Package names, not types. The retired-type list could not catch a
            // contributing guide that routed new template code into
            // `templates.builtins` and `templates.support` — two packages the 2.0
            // split left behind and nothing has occupied since, so the instruction
            // read as current and produced code that does not compile. Both spellings
            // are listed: a doc names a package either as a dotted coordinate or as the
            // path it lives at, and the sentence that carried this one for a release
            // used the path.
            "templates.builtins",
            "templates.support",
            "templates/builtins/",
            "templates/support/");

    /**
     * Types a contributor must not be pointed at when told how to build against the
     * library, <em>even though the name still resolves somewhere in this repository</em>.
     *
     * <p>{@code BusinessTheme} is the case that motivated the list: 2.0 removed it from
     * the published surface, but the examples keep a local helper of the same name, so a
     * repository-wide existence check cannot tell "this type is gone" from "this type is
     * an example's private business". The distinction that matters to a reader is
     * whether the type ships in a Maven artifact, and for API guidance it does not.</p>
     *
     * <p>Scanned over API guidance only — issue templates, the contributing guide, the
     * template docs — and deliberately not over {@code examples/README.md}, which
     * documents that local helper correctly.</p>
     */
    private static final List<String> FORBIDDEN_IN_API_GUIDANCE = List.of(
            "BusinessTheme");

    /**
     * Documents whose job is to record history. A migration guide, an ADR, an archived
     * page or the changelog names retired surface on purpose, so the retired-token scan
     * skips them by path rather than by a file list — a new historical page is then
     * covered the day it is added, instead of failing the build until someone
     * remembers to allowlist it.
     */
    private static final List<String> HISTORICAL_RECORD_PREFIXES = List.of(
            "CHANGELOG.md",
            "docs/adr/",
            "docs/archive/",
            "docs/migration/",
            "docs/private/",
            "docs/roadmaps/",
            "docs/templates/v1-classic/");

    /**
     * The exact sentences allowed to name the removed engine architecture, because they
     * name it <em>as removed</em> — the one thing such prose may still do.
     *
     * <p>Exempting the whole file was the first attempt and it was too coarse: it would
     * have let a fresh claim into {@code ROADMAP.md}'s live "Current stable" section,
     * which is precisely the sentence a reader trusts most. Matching the wording pins
     * the exemption to the historical statement itself, so any other mention in the same
     * file still fails.</p>
     *
     * <p>Keyed by file, so an exemption applies where the sentence IS the record and
     * nowhere else. Listed rather than pattern-matched: a guard that recognised "gone"
     * or "removed" would pass any sentence containing the word, including a new false
     * one.</p>
     */
    private static final Map<String, List<String>> ENGINE_HISTORY_SENTENCES = Map.of(
            // ROADMAP's 2.0 section, recording what that GA dropped — the same job the
            // changelog does one entry at a time.
            "ROADMAP.md", List.of(
                    "the dead Entity-Component-System execution layer and the deprecated"),
            // The post-2.0 roadmap's opening paragraph, naming what that line removed.
            // Kept to one physical line: the sentence wraps in the source, and the
            // line ending it wraps with is not the same on every checkout.
            "docs/roadmaps/post-2.0-engineering.md", List.of(
                    "Entity-Component-System code, retiring the deprecated API surface"),
            // A dated benchmark log, measured while that engine still existed. Rewriting
            // it would falsify the record it is kept for.
            "baselines/COMPARISON.md", List.of(
                    "too small to expose ECS lookup overhead",
                    "that go through the legacy ECS"));

    private static final Set<String> MAIN_CANONICAL_SOURCE_ALLOWLIST = Set.of();

    private static final Set<String> DOCUMENTATION_ALLOWLIST = Set.of(
            "core/src/test/java/com/demcha/documentation/CanonicalSurfaceGuardTest.java",
            "core/src/test/java/com/demcha/documentation/DocumentationCoverageTest.java");

    private static final Set<String> CANONICAL_DOCUMENT_TEST_ALLOWLIST = Set.of();

    private static final Set<String> CANONICAL_BENCHMARK_ALLOWLIST = Set.of();

    // Add an entry here only when a public markdown document genuinely
    // needs to name retired legacy surface (com.demcha.templates.*,
    // com.demcha.compose.v2.*, GraphCompose.pdf(...), PdfComposer) —
    // i.e. an audit / migration / parity log. Internal planning docs
    // should live outside the public docs surface (see .gitignore →
    // docs/private/).
    private static final Set<String> PUBLIC_MARKDOWN_ALLOWLIST = Set.of(
            // Lists every retired V1 CV / cover-letter class so callers
            // can find the v2 replacement. Naming the legacy surface is
            // the explicit purpose of a migration log.
            "docs/roadmaps/migration-v1-5-to-v1-6.md",
            // Decision guide for "classic vs layered template surface".
            // The deprecation-inventory section names GraphCompose.pdf(...),
            // PdfComposer, MainPageCV, MainPageCvDTO, ModuleYml,
            // TemplateBuilder, and com.demcha.compose.v2.* / com.demcha.templates.*
            // so callers can identify legacy imports in their own code and
            // see the canonical-DSL replacement. Same purpose as the
            // migration log above.
            "docs/templates/which-template-system.md",
            // User-facing API stability policy. The package-tier lookup
            // table names com.demcha.templates.* and com.demcha.compose.v2.*
            // explicitly so callers can classify any import as Stable /
            // Extension SPI / Internal / Legacy. The deprecation example
            // also shows the legacy `pdf(Path)` factory paired with its
            // canonical-DSL replacement. Same audit-log rationale.
            "docs/api-stability.md");
    private static final List<String> FORBIDDEN_PUBLIC_AUTHORING_IMPORTS = List.of(
            "import com.demcha.compose.engine.");

    @Test
    void canonicalMainSourcesShouldAvoidLegacySurfaceOutsideTransitionMappers() throws IOException {
        assertNoForbiddenReferences(
                PROJECT_ROOT.resolve("core/src/main/java/com/demcha/compose/document"),
                MAIN_CANONICAL_SOURCE_ALLOWLIST);
    }

    @Test
    void runnableExamplesShouldStayOnCanonicalSurface() throws IOException {
        assertNoForbiddenReferences(
                PROJECT_ROOT.resolve("examples/src/main/java/com/demcha/examples"),
                Set.of());
    }

    @Test
    void documentationTestsShouldAvoidLegacySurfaceOutsideGuardFiles() throws IOException {
        assertNoForbiddenReferences(
                PROJECT_ROOT.resolve("core/src/test/java/com/demcha/documentation"),
                DOCUMENTATION_ALLOWLIST);
    }

    @Test
    void canonicalDocumentTestsShouldAvoidLegacySurfaceOutsideCompatibilityParity() throws IOException {
        assertNoForbiddenReferences(
                PROJECT_ROOT.resolve("core/src/test/java/com/demcha/compose/document"),
                CANONICAL_DOCUMENT_TEST_ALLOWLIST);
    }

    @Test
    void canonicalBenchmarkEntryPointsShouldAvoidLegacySurface() throws IOException {
        assertNoForbiddenReferences(
                PROJECT_ROOT.resolve("core/src/test/java/com/demcha/compose"),
                path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.endsWith("Benchmark.java")
                            || fileName.endsWith("StressTest.java")
                            || fileName.endsWith("EnduranceTest.java");
                },
                CANONICAL_BENCHMARK_ALLOWLIST);
    }

    @Test
    void publicMarkdownDocsShouldAvoidLegacySurfaceOutsideHistoricalAuditNotes() throws IOException {
        assertNoForbiddenMarkdownReferences(
                List.of(
                        PROJECT_ROOT.resolve("README.md"),
                        PROJECT_ROOT.resolve("CONTRIBUTING.md"),
                        PROJECT_ROOT.resolve("examples/README.md"),
                        PROJECT_ROOT.resolve("docs")),
                PUBLIC_MARKDOWN_ALLOWLIST);
    }

    /**
     * The contributor-facing surface must not teach types 2.0 removed.
     *
     * <p>Scans wider than the legacy check above: {@code SECURITY.md},
     * {@code SUPPORT.md}, {@code ROADMAP.md} and {@code .github/} sit outside every
     * existing guard, which is why an issue template could route reporters to a theme
     * class that no longer exists and a pull-request template could offer a lane the
     * repository dropped.</p>
     */
    @Test
    void contributorFacingDocsShouldNotNameSurfaceRetiredIn2_0() throws IOException {
        List<Path> roots = List.of(
                PROJECT_ROOT.resolve("README.md"),
                PROJECT_ROOT.resolve("CONTRIBUTING.md"),
                PROJECT_ROOT.resolve("SECURITY.md"),
                PROJECT_ROOT.resolve("SUPPORT.md"),
                PROJECT_ROOT.resolve("ROADMAP.md"),
                PROJECT_ROOT.resolve("examples/README.md"),
                PROJECT_ROOT.resolve("docs"),
                PROJECT_ROOT.resolve(".github"));

        Set<String> violations = new TreeSet<>();
        for (Path root : roots) {
            for (Path doc : markdownUnder(root)) {
                String rel = relative(doc);
                if (isHistoricalRecord(rel) || PUBLIC_MARKDOWN_ALLOWLIST.contains(rel)) {
                    continue;
                }
                String source = Files.readString(doc);
                RETIRED_IN_2_0_TOKENS.stream()
                        .filter(source::contains)
                        .forEach(token -> violations.add(rel + " names " + token));
            }
        }

        assertThat(violations)
                .describedAs("these documents name a type or package 2.0 removed, so anyone "
                        + "following them writes code that does not compile. A document whose "
                        + "purpose is to record the removal belongs under one of %s.",
                        HISTORICAL_RECORD_PREFIXES)
                .isEmpty();
    }

    /**
     * API guidance must not name a type that no longer ships, even when the name still
     * resolves inside this repository.
     *
     * <p>Separate from the retired-token scan because the question is different: not
     * "does this identifier exist anywhere" but "can a reader of a published artifact
     * use it". {@code BusinessTheme} answers yes to the first and no to the second,
     * which is exactly how an issue template came to offer it as the theming entry
     * point months after 2.0 removed it.</p>
     */
    @Test
    void apiGuidanceShouldNotOfferTypesThatNoLongerShip() throws IOException {
        List<Path> roots = List.of(
                PROJECT_ROOT.resolve("README.md"),
                PROJECT_ROOT.resolve("CONTRIBUTING.md"),
                PROJECT_ROOT.resolve("docs/templates"),
                PROJECT_ROOT.resolve("docs/getting-started.md"),
                PROJECT_ROOT.resolve("docs/first-document.md"),
                PROJECT_ROOT.resolve(".github"));

        Set<String> violations = new TreeSet<>();
        for (Path root : roots) {
            for (Path doc : markdownUnder(root)) {
                String rel = relative(doc);
                if (isHistoricalRecord(rel) || PUBLIC_MARKDOWN_ALLOWLIST.contains(rel)) {
                    continue;
                }
                String source = Files.readString(doc);
                FORBIDDEN_IN_API_GUIDANCE.stream()
                        .filter(source::contains)
                        .forEach(token -> violations.add(rel + " offers " + token));
            }
        }

        assertThat(violations)
                .describedAs("these documents tell a reader to build against a type that no "
                        + "longer ships in any published artifact. A name that survives as an "
                        + "examples-local helper is still unusable by a consumer.")
                .isEmpty();
    }

    /**
     * Every relative link in the public documentation resolves on disk.
     *
     * <p>Needs no token list and cannot go stale: it reads what the documents actually
     * point at. Renaming a test, archiving a page or deleting an example breaks the
     * links to it here rather than for a reader.</p>
     */
    @Test
    void publicMarkdownLinksShouldResolve() throws IOException {
        List<Path> roots = List.of(
                PROJECT_ROOT.resolve("README.md"),
                PROJECT_ROOT.resolve("CONTRIBUTING.md"),
                PROJECT_ROOT.resolve("SECURITY.md"),
                PROJECT_ROOT.resolve("SUPPORT.md"),
                PROJECT_ROOT.resolve("ROADMAP.md"),
                PROJECT_ROOT.resolve("examples/README.md"),
                PROJECT_ROOT.resolve("docs"),
                // Issue and pull-request templates carry relative links out of
                // .github/ISSUE_TEMPLATE/, two levels deep — the shape most likely
                // to break silently when a target moves.
                PROJECT_ROOT.resolve(".github"));

        Pattern link = Pattern.compile("\\]\\(([^)\\s]+)\\)");
        Set<String> broken = new TreeSet<>();
        for (Path root : roots) {
            for (Path doc : markdownUnder(root)) {
                if (isHistoricalRecord(relative(doc))) {
                    continue;
                }
                Matcher matcher = link.matcher(Files.readString(doc));
                while (matcher.find()) {
                    String target = matcher.group(1);
                    if (target.startsWith("http") || target.startsWith("mailto:") || target.startsWith("#")) {
                        continue;
                    }
                    String file = target.split("#", 2)[0];
                    if (file.isEmpty()) {
                        continue;
                    }
                    if (!Files.exists(doc.getParent().resolve(file).normalize())) {
                        broken.add(relative(doc) + " -> " + target);
                    }
                }
            }
        }

        assertThat(broken)
                .describedAs("a relative link in the public docs points at a file that does "
                        + "not exist; the reader gets a 404 on GitHub")
                .isEmpty();
    }

    @Test
    void publicAuthoringDocsAndExamplesShouldNotImportEngineInternals() throws IOException {
        assertNoForbiddenAuthoringImports(
                List.of(
                        PROJECT_ROOT.resolve("README.md"),
                        PROJECT_ROOT.resolve("docs/getting-started.md"),
                        PROJECT_ROOT.resolve("docs/recipes.md"),
                        PROJECT_ROOT.resolve("docs/operations/layout-snapshot-testing.md"),
                        PROJECT_ROOT.resolve("examples/src/main/java/com/demcha/examples"),
                        PROJECT_ROOT.resolve("qa/src/test/java/com/demcha/documentation/DocumentationExamplesTest.java")));
    }

    @Test
    void semanticAuthoringValuePackagesShouldNotImportEngineInternals() throws IOException {
        assertNoForbiddenAuthoringImports(
                List.of(
                        PROJECT_ROOT.resolve("core/src/main/java/com/demcha/compose/document/node"),
                        PROJECT_ROOT.resolve("core/src/main/java/com/demcha/compose/document/dsl"),
                        PROJECT_ROOT.resolve("core/src/main/java/com/demcha/compose/document/style"),
                        PROJECT_ROOT.resolve("core/src/main/java/com/demcha/compose/document/table"),
                        PROJECT_ROOT.resolve("core/src/main/java/com/demcha/compose/document/image")));
    }

    /**
     * The entity-component-system engine is gone, and no prose may say otherwise.
     *
     * <p>Nothing named {@code SystemECS}, {@code Entity} or {@code ComponentSystem}
     * survives in any {@code src/main} tree, and no {@code ecs} sub-package exists. Yet
     * this repository carried, for a full release line, javadoc telling readers the
     * adapters "talk to the ECS-based engine", that a watermark "is not an ECS entity",
     * and that the legacy renderer had "moved to the {@code ecs} sub-package" — a
     * package a reader can never open. The contributor guide sent people around an
     * "engine ECS" that is not there.</p>
     *
     * <p>Every existing scan missed all of it, and the reasons are worth keeping,
     * because each is a different blind spot. The retired-token scan reads markdown
     * only. The one scan that reads main sources is scoped to {@code document/**}, while
     * these claims lived in engine and render-backend javadoc. And a module's {@code
     * pom.xml} description — read by anyone browsing the artifact on Central — is not
     * source or markdown, so nothing had ever looked at one. This scan reads all three.</p>
     *
     * <p>{@code docs/roadmaps/} is deliberately <em>not</em> skipped here, unlike in the
     * retired-token scan. The root roadmap links {@code post-2.0-engineering.md} as
     * committed engineering direction, so it is a live document that happened to sit
     * under a prefix treated as archival — and it went on describing a live entity model
     * that had already been removed.</p>
     *
     * <p>Matched on word boundaries rather than as a substring, so {@code SPECS} and
     * {@code RECS} do not trip it. A changelog entry, an ADR, or one of the sentences in
     * {@link #ENGINE_HISTORY_SENTENCES} names the architecture on purpose.</p>
     */
    @Test
    void nothingShouldDescribeTheEngineAsEntityComponentSystem() throws IOException {
        Pattern ecs = Pattern.compile("\\bECS\\b|\\bentity[- ]component[- ]system\\b",
                Pattern.CASE_INSENSITIVE);
        List<Path> roots = List.of(
                PROJECT_ROOT.resolve("README.md"),
                PROJECT_ROOT.resolve("CONTRIBUTING.md"),
                PROJECT_ROOT.resolve("ROADMAP.md"),
                PROJECT_ROOT.resolve("docs"),
                PROJECT_ROOT.resolve("baselines"),
                PROJECT_ROOT.resolve("pom.xml"),
                PROJECT_ROOT.resolve("core"),
                PROJECT_ROOT.resolve("render-pdf"),
                PROJECT_ROOT.resolve("render-pptx"),
                PROJECT_ROOT.resolve("render-docx"),
                PROJECT_ROOT.resolve("templates"));

        Set<String> violations = new TreeSet<>();
        for (Path root : roots) {
            for (Path file : proseBearingFilesUnder(root)) {
                String rel = relative(file);
                // The guard files themselves spell the tokens they forbid, as they do for
                // the retired-surface scan.
                if (rel.startsWith("CHANGELOG.md") || rel.startsWith("docs/adr/")
                        || rel.startsWith("docs/archive/") || rel.startsWith("docs/migration/")
                        || rel.startsWith("docs/private/")
                        || DOCUMENTATION_ALLOWLIST.contains(rel)) {
                    continue;
                }
                String source = Files.readString(file);
                // Keyed by file: a sentence is exempt where it is the record, not
                // wherever anyone repeats it. Applied globally, any live document could
                // have quoted one of these and walked through the guard.
                for (String allowed : ENGINE_HISTORY_SENTENCES.getOrDefault(rel, List.of())) {
                    source = source.replace(allowed, "");
                }
                Matcher matcher = ecs.matcher(source);
                if (matcher.find()) {
                    violations.add(rel + " says \"" + matcher.group() + "\"");
                }
            }
        }

        assertThat(violations)
                .describedAs("the entity-component-system engine was removed, so prose "
                        + "naming it as current describes an architecture this code does "
                        + "not have. A sentence recording the removal belongs in the "
                        + "changelog, an ADR, or %s.", ENGINE_HISTORY_SENTENCES)
                .isEmpty();
    }

    /**
     * Markdown, Java and pom files under a root, or the root itself when it is one.
     *
     * <p>Build output is skipped explicitly: the roots here are whole modules rather
     * than {@code src/main/java}, so that a module's {@code pom.xml} description is
     * read, and walking a module reaches its {@code target/} tree — where a stale
     * generated copy would report a violation nobody can fix in source.</p>
     */
    private static List<Path> proseBearingFilesUnder(Path root) throws IOException {
        if (Files.isRegularFile(root)) {
            return List.of(root);
        }
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.toString();
                        return name.endsWith(".md") || name.endsWith(".java")
                                || name.endsWith(".xml");
                    })
                    .filter(path -> !path.toString().contains(File.separator + "target"
                            + File.separator))
                    .sorted()
                    .toList();
        }
    }

    /** Markdown files under a root, or the root itself when it is one. */
    private static List<Path> markdownUnder(Path root) throws IOException {
        if (Files.isRegularFile(root)) {
            return root.toString().endsWith(".md") ? List.of(root) : List.of();
        }
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".md"))
                    .sorted()
                    .toList();
        }
    }

    private static boolean isHistoricalRecord(String relativePath) {
        return HISTORICAL_RECORD_PREFIXES.stream().anyMatch(relativePath::startsWith);
    }

    private void assertNoForbiddenReferences(Path root, Set<String> allowlist) throws IOException {
        assertNoForbiddenReferences(root, path -> true, allowlist);
    }

    private void assertNoForbiddenReferences(Path root,
                                             Predicate<Path> include,
                                             Set<String> allowlist) throws IOException {
        try (var paths = Files.walk(root)) {
            List<String> violations = new TreeSet<>(paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(include)
                    .filter(path -> !allowlist.contains(relative(path)))
                    .filter(this::containsForbiddenToken)
                    .map(this::relative)
                    .collect(Collectors.toList()))
                    .stream()
                    .toList();

            assertThat(violations)
                    .describedAs("Files under %s must stay on the canonical document surface", relative(root))
                    .isEmpty();
        }
    }

    private void assertNoForbiddenMarkdownReferences(List<Path> roots, Set<String> allowlist) throws IOException {
        Set<String> violations = new TreeSet<>();

        for (Path root : roots) {
            if (Files.isRegularFile(root)) {
                if (!allowlist.contains(relative(root)) && containsForbiddenToken(root)) {
                    violations.add(relative(root));
                }
                continue;
            }

            try (var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".md"))
                        // docs/private/ is gitignored — it holds local-only
                        // planning notes, audits, and roadmap drafts that
                        // are not part of the public docs surface and so
                        // are not subject to this guard.
                        .filter(path -> !relative(path).startsWith("docs/private/"))
                        .filter(path -> !allowlist.contains(relative(path)))
                        .filter(this::containsForbiddenToken)
                        .map(this::relative)
                        .forEach(violations::add);
            }
        }

        assertThat(violations)
                .describedAs("Public markdown docs must stay on the canonical document surface")
                .isEmpty();
    }

    private boolean containsForbiddenToken(Path path) {
        try {
            String source = Files.readString(path);
            return FORBIDDEN_TOKENS.stream().anyMatch(source::contains);
        } catch (IOException e) {
            throw new RuntimeException("Failed to inspect " + path, e);
        }
    }

    private void assertNoForbiddenAuthoringImports(List<Path> roots) throws IOException {
        Set<String> violations = new TreeSet<>();
        for (Path root : roots) {
            if (Files.notExists(root)) {
                continue;
            }
            if (Files.isRegularFile(root)) {
                if (containsForbiddenAuthoringImport(root)) {
                    violations.add(relative(root));
                }
                continue;
            }
            try (var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java") || path.toString().endsWith(".md"))
                        .filter(this::containsForbiddenAuthoringImport)
                        .map(this::relative)
                        .forEach(violations::add);
            }
        }

        assertThat(violations)
                .describedAs("Public authoring docs and runnable examples should not import engine internals")
                .isEmpty();
    }

    private boolean containsForbiddenAuthoringImport(Path path) {
        try {
            String source = Files.readString(path);
            return FORBIDDEN_PUBLIC_AUTHORING_IMPORTS.stream().anyMatch(source::contains);
        } catch (IOException e) {
            throw new RuntimeException("Failed to inspect " + path, e);
        }
    }

    private String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
