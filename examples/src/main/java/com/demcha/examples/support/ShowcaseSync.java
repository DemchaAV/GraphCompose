package com.demcha.examples.support;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Build-time tool that mirrors generated example PDFs into the
 * GitHub Pages site under {@code web/showcase/}.
 *
 * <p>For each PDF found under
 * {@code examples/target/generated-pdfs/<category>/<group>/<file>.pdf}
 * the tool:</p>
 *
 * <ol>
 *   <li>Copies the PDF to
 *       {@code web/showcase/pdf/<category>/<group>/<file>.pdf}.</li>
 *   <li>Renders page 0 of the PDF to a {@code 1.5x} PNG and writes it to
 *       {@code web/showcase/screenshots/<category>/<group>/<file>.png}
 *       — the static site uses these as preview thumbnails.</li>
 *   <li>Copies a same-named {@code .pptx} sitting beside the PDF to
 *       {@code web/showcase/pptx/<category>/<group>/<file>.pptx}, published as a
 *       second download on the same card. The twin shares the PDF's preview:
 *       both backends draw the same resolved layout, and POI has no page
 *       rasterizer to build a separate thumbnail with.</li>
 * </ol>
 *
 * <p>Then it writes a structured {@code web/examples.json} manifest
 * grouping every example by category and group. Per-example titles,
 * descriptions, and tags come from the hand-curated
 * {@link ShowcaseMetadata} catalogue; entries without metadata fall
 * back to a sensible filename-derived default.</p>
 *
 * <p>Run via Maven:</p>
 * <pre>{@code
 * cd examples
 * ../mvnw exec:java -Dexec.mainClass=com.demcha.examples.support.ShowcaseSync
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class ShowcaseSync {

    private static final float PREVIEW_SCALE = 1.5f;

    private ShowcaseSync() {
    }

    public static void main(String[] args) throws Exception {
        Path repoRoot = locateRepoRoot();
        Path generatedPdfs = repoRoot.resolve("examples/target/generated-pdfs");
        Path siteRoot = repoRoot.resolve("web");
        Path showcaseRoot = siteRoot.resolve("showcase");
        Path manifestFile = siteRoot.resolve("examples.json");

        if (!Files.isDirectory(generatedPdfs)) {
            throw new IllegalStateException("No generated PDFs found at " + generatedPdfs
                    + ". Run GenerateAllExamples first.");
        }

        // Collect BEFORE clearing. The directory existing is not evidence that anything
        // was produced: a generation run that succeeds while writing nothing would
        // otherwise delete the whole published site and replace the manifest with an
        // empty one, and the failure would only be visible once the site was live.
        List<Path> pdfs;
        try (Stream<Path> walk = Files.walk(generatedPdfs)) {
            pdfs = walk
                    .filter(p -> p.toString().endsWith(".pdf"))
                    .sorted()
                    .toList();
        }
        if (pdfs.isEmpty()) {
            throw new IllegalStateException("No PDFs under " + generatedPdfs
                    + ". Run GenerateAllExamples first — refusing to clear the published"
                    + " showcase and write an empty manifest.");
        }
        requireCompleteCatalogue(pdfs, generatedPdfs);

        // The copy below replaces files but never removes them, so an example that was
        // renamed or deleted kept its artifact published on GitHub Pages indefinitely —
        // reachable by URL, absent from the manifest, and rendered from source that no
        // longer exists. Clearing the three output trees first makes the published site
        // a pure function of what GenerateAllExamples just produced.
        for (String subtree : new String[] {"pdf", "pptx", "screenshots"}) {
            deletePublishedFiles(showcaseRoot.resolve(subtree));
        }

        // category → group → list of entries
        Map<String, Map<String, List<ManifestEntry>>> tree = new TreeMap<>();
        int copied = 0;
        int rendered = 0;
        int decks = 0;

        for (Path pdf : pdfs) {
            Path rel = generatedPdfs.relativize(pdf);
            String[] parts = rel.toString().replace('\\', '/').split("/");
            if (parts.length < 2) {
                continue;
            }
            String category;
            String group;
            String fileName = parts[parts.length - 1];
            String basename = fileName.substring(0, fileName.length() - 4);
            if (parts.length == 2) {
                category = parts[0];
                group = "default";
            } else {
                category = parts[0];
                group = parts[1];
            }

            Path pdfTarget = showcaseRoot.resolve("pdf").resolve(category).resolve(group).resolve(fileName);
            Path pngTarget = showcaseRoot.resolve("screenshots").resolve(category).resolve(group)
                    .resolve(basename + ".png");
            Files.createDirectories(pdfTarget.getParent());
            Files.createDirectories(pngTarget.getParent());

            Files.copy(pdf, pdfTarget, StandardCopyOption.REPLACE_EXISTING);
            copied++;
            renderPreview(pdf, pngTarget);
            rendered++;

            // A twin flagship renders the same composition to a deck beside its
            // PDF. The deck is published as a second download on the same card:
            // it shares the PDF's preview by construction — both backends draw
            // the same resolved layout — and POI has no page rasterizer to make
            // a separate one with.
            String pptxUrl = null;
            Path pptxSource = pdf.resolveSibling(basename + ".pptx");
            if (Files.isRegularFile(pptxSource)) {
                Path pptxTarget = showcaseRoot.resolve("pptx").resolve(category).resolve(group)
                        .resolve(basename + ".pptx");
                Files.createDirectories(pptxTarget.getParent());
                Files.copy(pptxSource, pptxTarget, StandardCopyOption.REPLACE_EXISTING);
                copied++;
                decks++;
                pptxUrl = relativeUrl(showcaseRoot, pptxTarget, siteRoot);
            }

            ShowcaseMetadata.Entry meta = ShowcaseMetadata.lookup(basename, category, group);
            ManifestEntry entry = new ManifestEntry(
                    basename,
                    meta.title(),
                    meta.description(),
                    meta.tags(),
                    relativeUrl(showcaseRoot, pdfTarget, siteRoot),
                    pptxUrl,
                    relativeUrl(showcaseRoot, pngTarget, siteRoot),
                    meta.codeUrl());
            tree.computeIfAbsent(category, c -> new TreeMap<>())
                    .computeIfAbsent(group, g -> new ArrayList<>())
                    .add(entry);
        }

        for (Map<String, List<ManifestEntry>> groups : tree.values()) {
            for (List<ManifestEntry> entries : groups.values()) {
                entries.sort(Comparator.comparing(ManifestEntry::id));
            }
        }

        String json = renderManifest(tree);
        Files.writeString(manifestFile, json);

        System.out.println("Synced " + copied + " documents (" + rendered + " PDFs, "
                + decks + " PPTX twins) and " + rendered + " preview PNGs into " + showcaseRoot);
        System.out.println("Wrote manifest to " + manifestFile);
    }

    private static void renderPreview(Path pdfPath, Path pngTarget) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImage(0, PREVIEW_SCALE, ImageType.RGB);
            ImageIO.write(image, "PNG", pngTarget.toFile());
        }
    }

    /**
     * Refuses to publish a catalogue that is missing documents the register describes.
     *
     * <p>"Not empty" stopped being enough the moment generation began by clearing the
     * tree. A run that dies partway now leaves exactly the documents written before the
     * throw, and that passes an emptiness check — so the sync would mirror the remains,
     * delete every published file the run never got to, and rewrite the manifest to
     * match. The site ends up internally consistent and quietly missing half its
     * examples, which is worse than an obvious failure.</p>
     *
     * <p>The register is the yardstick because it is the one list that says what the
     * catalogue is meant to contain. Deleting an example legitimately shrinks the
     * catalogue, and that stays possible — the entry goes with it, which the coverage
     * guard requires anyway.</p>
     */
    private static void requireCompleteCatalogue(List<Path> pdfs, Path generatedPdfs) {
        Set<String> produced = pdfs.stream()
                .map(pdf -> pdf.getFileName().toString())
                .map(name -> name.substring(0, name.length() - ".pdf".length()))
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> missing = new TreeSet<>(ShowcaseMetadata.registeredEntries().keySet());
        missing.removeAll(produced);
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "The generated catalogue is missing " + missing.size() + " document(s) the"
                    + " showcase register describes: " + missing + ". Generation starts by"
                    + " emptying " + generatedPdfs + ", so a run that failed partway leaves"
                    + " exactly what it managed to write. Re-run GenerateAllExamples and let it"
                    + " finish — publishing now would delete the missing documents from the site"
                    + " and rewrite the manifest without them. If an example was removed on"
                    + " purpose, remove its register entry too.");
        }
    }

    /**
     * Empties a published subtree. A missing tree is a no-op — the normal case on a
     * fresh checkout.
     *
     * <p>Every file goes, and a failure there stops the sync: a published file that
     * survives is a document the site still serves and the manifest no longer lists.</p>
     *
     * <p>Directories are removed too, deepest first, but only where the filesystem
     * allows. On Windows a just-closed handle can still fail the parent's removal with
     * {@code AccessDeniedException} — observed here on
     * {@code web/showcase/pdf/templates/schedule} — and an empty folder left behind is
     * a cosmetic blemish on the deployed tree, invisible to git and to every reader who
     * arrives through the manifest. Failing the whole publish over one would trade a
     * real problem for a tidiness preference.</p>
     */
    private static void deletePublishedFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                if (path.equals(root)) {
                    continue;
                }
                if (Files.isDirectory(path)) {
                    try {
                        Files.delete(path);
                    } catch (IOException locked) {
                        System.out.println("Left in place (the filesystem would not remove it): "
                                + path + " — " + locked.getClass().getSimpleName());
                    }
                } else {
                    Files.delete(path);
                }
            }
        }
    }

    private static String relativeUrl(Path showcaseRoot, Path target, Path docsRoot) {
        Path rel = docsRoot.relativize(target);
        return rel.toString().replace('\\', '/');
    }

    private static Path locateRepoRoot() {
        Path classes;
        try {
            classes = Path.of(ShowcaseSync.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            classes = Path.of(".").toAbsolutePath();
        }
        Path candidate = classes;
        while (candidate != null && candidate.getFileName() != null) {
            if (Files.isRegularFile(candidate.resolve("pom.xml"))
                    && Files.isDirectory(candidate.resolve("docs"))
                    && Files.isDirectory(candidate.resolve("examples"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        return Path.of(".").toAbsolutePath().normalize();
    }

    private static String renderManifest(Map<String, Map<String, List<ManifestEntry>>> tree) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"categories\": [\n");
        Map<String, String> categoryLabels = new LinkedHashMap<>();
        categoryLabels.put("templates", "Templates");
        categoryLabels.put("features", "Features");
        categoryLabels.put("flagships", "Flagship Examples");

        // Per-category group ordering. The bare TreeMap sort would
        // surface "coverletter" first inside Templates (15 plain
        // text-only letters) — which is the weakest first impression.
        // Lead with the visually striking groups (CV gallery,
        // cinematic invoices/proposals) and tail with the supporting
        // ones. Features groups are arranged by "show-off" weight.
        // Groups not listed here fall back to alphabetical order.
        Map<String, List<String>> groupOrder = new LinkedHashMap<>();
        groupOrder.put("templates", List.of(
                "cv", "proposal", "invoice", "schedule", "coverletter"));
        groupOrder.put("features", List.of(
                "canvas", "tables", "lists", "shapes", "transforms",
                "text", "themes", "barcodes", "chrome", "streaming",
                "snapshots"));
        groupOrder.put("flagships", List.of("default"));

        boolean firstCategory = true;
        for (Map.Entry<String, String> ce : categoryLabels.entrySet()) {
            String catId = ce.getKey();
            Map<String, List<ManifestEntry>> groups = tree.get(catId);
            if (groups == null || groups.isEmpty()) {
                continue;
            }
            if (!firstCategory) sb.append(",\n");
            firstCategory = false;
            sb.append("    {\n");
            sb.append("      \"id\": ").append(jsonString(catId)).append(",\n");
            sb.append("      \"label\": ").append(jsonString(ce.getValue())).append(",\n");
            sb.append("      \"groups\": [\n");

            // Order groups: explicit list first, then any leftovers
            // that weren't pinned (so a newly added group still shows
            // up rather than being silently dropped).
            List<String> preferred = groupOrder.getOrDefault(catId, List.of());
            List<String> orderedKeys = new ArrayList<>();
            for (String key : preferred) {
                if (groups.containsKey(key)) orderedKeys.add(key);
            }
            for (String key : groups.keySet()) {
                if (!orderedKeys.contains(key)) orderedKeys.add(key);
            }

            boolean firstGroup = true;
            for (String groupKey : orderedKeys) {
                List<ManifestEntry> entries = groups.get(groupKey);
                if (entries == null || entries.isEmpty()) continue;
                if (!firstGroup) sb.append(",\n");
                firstGroup = false;
                sb.append("        {\n");
                sb.append("          \"id\": ").append(jsonString(groupKey)).append(",\n");
                sb.append("          \"label\": ").append(jsonString(ShowcaseMetadata.groupLabel(catId, groupKey))).append(",\n");
                sb.append("          \"examples\": [\n");
                boolean firstExample = true;
                for (ManifestEntry entry : entries) {
                    if (!firstExample) sb.append(",\n");
                    firstExample = false;
                    sb.append("            ").append(entry.toJson());
                }
                sb.append("\n          ]\n");
                sb.append("        }");
            }
            sb.append("\n      ]\n");
            sb.append("    }");
        }
        sb.append("\n  ]\n}\n");
        return sb.toString();
    }

    private static String jsonString(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    /**
     * One showcase card. {@code pptx} is {@code null} for the majority of
     * examples that render PDF only; the twin flagships carry both.
     */
    private record ManifestEntry(
            String id,
            String title,
            String description,
            List<String> tags,
            String pdf,
            String pptx,
            String screenshot,
            String code) {

        String toJson() {
            StringBuilder sb = new StringBuilder("{\n");
            sb.append("              \"id\": ").append(jsonString(id)).append(",\n");
            sb.append("              \"title\": ").append(jsonString(title)).append(",\n");
            sb.append("              \"description\": ").append(jsonString(description)).append(",\n");
            sb.append("              \"tags\": [");
            for (int i = 0; i < tags.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(jsonString(tags.get(i)));
            }
            sb.append("],\n");
            sb.append("              \"pdf\": ").append(jsonString(pdf)).append(",\n");
            if (pptx != null) {
                sb.append("              \"pptx\": ").append(jsonString(pptx)).append(",\n");
            }
            sb.append("              \"screenshot\": ").append(jsonString(screenshot)).append(",\n");
            sb.append("              \"code\": ").append(jsonString(code)).append("\n");
            sb.append("            }");
            return sb.toString();
        }
    }
}
