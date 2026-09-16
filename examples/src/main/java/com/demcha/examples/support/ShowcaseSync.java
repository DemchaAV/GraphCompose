package com.demcha.examples.support;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
import java.util.regex.Pattern;
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
 * back to a sensible filename-derived default. A CV preset's card also
 * carries its {@code ats} classification — the status, whether it earns
 * the "ATS-friendly" badge, the parsers and date of the check, and what the
 * parsers still get wrong — which the page reads to show the badge.</p>
 *
 * <p>The manifest states the contract it was written to in {@code schemaVersion}, and
 * carries two things the site cannot work out for itself. Per family, a {@code snippets}
 * entry holds the compiled code block from that family's guide, copied in because the site
 * is served from {@code web/} alone and cannot reach a page under {@code docs/}. Per card,
 * {@code needsBundledFonts} says whether that document embeds a face of its own — measured
 * from the PDF, because it decides which coordinates a reader needs and it differs card by
 * card inside one family.</p>
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

    /**
     * How pages beyond the first are rendered.
     *
     * <p>Lower than the first page's scale because the viewer shows one page at a time in a
     * stage about 374 CSS pixels wide: 595px covers that, and 1.5x would publish pixels the
     * page never displays. Measured across all 33 multi-page documents, every page beyond the
     * first costs 6.27 MiB at 1.5x, 3.66 at 1.0x and 2.47 at 0.75x — and 0.75x is already soft
     * against that stage on a dense screen.</p>
     */
    private static final float PAGE_SCALE = 1.0f;

    /**
     * How wide a strip thumbnail is written.
     *
     * <p>The viewer's strip draws a document in a 54px slot, 46px on a narrow screen. Pointing
     * it at the card preview meant a whole page per slot — 5.2 MiB for the 27 CVs alone — so
     * each card also gets a thumbnail, and the strip reads that instead. Wide enough to stay
     * sharp on a dense screen, small enough that a family costs a fraction of one page.</p>
     */
    private static final int THUMBNAIL_WIDTH = 320;

    /**
     * What rendering a card's first page told us about the document: the preview's pixel
     * size, the page count, and whether the document draws with a face of its own.
     */
    private record Preview(int width, int height, int pages, boolean needsBundledFonts) {
    }

    /** Where a family's panel snippet is compiled: the page it lives on, and the block's id. */
    private record SnippetSource(String doc, String exampleId) {
    }

    /**
     * What a card's own example source says: whether a reader can run that class directly,
     * and which backends it reaches for beyond the engine.
     */
    private record SourceFacts(boolean runnable, List<String> extraArtifacts) {
    }

    /** A class a reader can run on its own, rather than one {@code GenerateAllExamples} drives. */
    private static final Pattern MAIN_METHOD = Pattern.compile("static\\s+void\\s+main\\s*\\(");

    /** The DOCX backend is named in the source: it is not discovered through the ServiceLoader. */
    private static final String DOCX_IMPORT = "import com.demcha.compose.document.backend.semantic.docx.";

    /** The PPTX backend is discovered by format, so the call is the only sign the source gives. */
    private static final List<String> PPTX_CALLS =
            List.of(".toPptxBytes(", ".buildPptx(", ".writePptx(");

    /**
     * The snippet the "Use this template" panel shows, per catalogue family.
     *
     * <p>The site is served from {@code web/} alone, so a block living under {@code docs/}
     * cannot be fetched by the page: it is copied into the manifest here, and
     * {@code ShowcaseSiteGuardTest} holds the copy equal to its source. The blocks are the
     * ones {@code DocumentationSnippetCompileTest} already compiles, so the text a reader
     * copies off the site is text a compiler has accepted.</p>
     *
     * <p>Only the families whose guide carries such a block appear. The rest show no snippet,
     * rather than one belonging to another family.</p>
     */
    private static final Map<String, SnippetSource> SNIPPETS = new LinkedHashMap<>();

    static {
        SNIPPETS.put("cv", new SnippetSource(
                "docs/templates/v2-layered/using-templates.md", "using-templates-pieces"));
        SNIPPETS.put("invoice", new SnippetSource(
                "docs/templates/business-templates.md", "business-invoice"));
        SNIPPETS.put("proposal", new SnippetSource(
                "docs/templates/business-templates.md", "business-proposal"));
    }

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
        for (String subtree : new String[] {"pdf", "pptx", "screenshots", "thumbnails", "pages"}) {
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
            Path thumbnailTarget = showcaseRoot.resolve("thumbnails").resolve(category).resolve(group)
                    .resolve(basename + ".png");
            Files.createDirectories(pdfTarget.getParent());
            Files.createDirectories(pngTarget.getParent());
            Files.createDirectories(thumbnailTarget.getParent());

            Path pagesDir = showcaseRoot.resolve("pages").resolve(category).resolve(group);

            Files.copy(pdf, pdfTarget, StandardCopyOption.REPLACE_EXISTING);
            copied++;
            Preview preview = renderPreview(pdf, pngTarget, thumbnailTarget, pagesDir, basename);
            rendered++;

            // Pages beyond the first, as the addresses the page will ask for. Derived from the
            // page count the render just reported, so the manifest cannot name a file the render
            // did not write.
            List<String> pageUrls = new ArrayList<>();
            for (int page = 2; page <= preview.pages(); page++) {
                pageUrls.add(relativeUrl(showcaseRoot,
                        pagesDir.resolve(basename + "-" + page + ".png"), siteRoot));
            }

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
            SourceFacts facts = sourceFacts(repoRoot, meta.sourcePath());
            List<String> artifacts = new ArrayList<>(meta.requiredArtifacts());
            for (String extra : facts.extraArtifacts()) {
                if (!artifacts.contains(extra)) {
                    artifacts.add(extra);
                }
            }
            // A card can publish a deck its own source never mentions: the flagship twins are
            // rendered by a sibling class. What a reader needs follows from the deck the card
            // offers them, not only from the call in the file the card links to.
            if (pptxUrl != null && !artifacts.contains("graph-compose-render-pptx")) {
                artifacts.add("graph-compose-render-pptx");
            }
            ManifestEntry entry = new ManifestEntry(
                    basename,
                    meta,
                    relativeUrl(showcaseRoot, pdfTarget, siteRoot),
                    pptxUrl,
                    relativeUrl(showcaseRoot, pngTarget, siteRoot),
                    relativeUrl(showcaseRoot, thumbnailTarget, siteRoot),
                    preview,
                    List.copyOf(pageUrls),
                    List.copyOf(artifacts),
                    facts.runnable(),
                    ShowcaseMetadata.ats(basename));
            tree.computeIfAbsent(category, c -> new TreeMap<>())
                    .computeIfAbsent(group, g -> new ArrayList<>())
                    .add(entry);
        }

        for (Map<String, List<ManifestEntry>> groups : tree.values()) {
            for (List<ManifestEntry> entries : groups.values()) {
                entries.sort(Comparator.comparing(ManifestEntry::id));
            }
        }

        String json = renderManifest(tree, repoRoot);
        Files.writeString(manifestFile, json);

        System.out.println("Synced " + copied + " documents (" + rendered + " PDFs, "
                + decks + " PPTX twins) and " + rendered + " preview PNGs into " + showcaseRoot);
        System.out.println("Wrote manifest to " + manifestFile);
    }

    /**
     * Renders a card's first page, writes the preview and its strip thumbnail, and reports what
     * the document turned out to be: the preview's pixel size, so the page can reserve the right
     * box before the image arrives, and how many pages there are to say so on the card.
     */
    private static Preview renderPreview(Path pdfPath, Path pngTarget, Path thumbnailTarget,
                                         Path pagesDir, String basename) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImage(0, PREVIEW_SCALE, ImageType.RGB);
            ImageIO.write(image, "PNG", pngTarget.toFile());
            writeThumbnail(image, thumbnailTarget);

            // The rest of the document, one file per page, so a reader can page through it
            // without downloading it. Named by the page number a reader sees — page 1 is the
            // preview written above, so these start at 2. Rendered from the document already
            // open here rather than in a second pass over the file.
            int pages = document.getNumberOfPages();
            for (int page = 1; page < pages; page++) {
                BufferedImage rest = renderer.renderImage(page, PAGE_SCALE, ImageType.RGB);
                Path target = pagesDir.resolve(basename + "-" + (page + 1) + ".png");
                Files.createDirectories(target.getParent());
                ImageIO.write(rest, "PNG", target.toFile());
            }

            return new Preview(image.getWidth(), image.getHeight(), pages,
                    embedsAFaceOfItsOwn(document));
        }
    }

    /**
     * Whether the document embeds a font of its own, rather than drawing only in the
     * Standard-14 set every PDF reader already has.
     *
     * <p>This is what decides the coordinates a reader needs. The bundled Google faces left
     * the engine in v1.8.0, so a card whose document embeds one cannot be reproduced from
     * the engine and templates alone — it needs the artifact carrying those faces, and that
     * companion is versioned independently of the release, so the published aggregate is
     * what supplies it at the release's own version.</p>
     *
     * <p>Read from the file rather than declared in the register: it is a property of the
     * document that was rendered, it differs card by card within one family, and a
     * hand-kept list of which 117 cards need fonts would be wrong the first time a preset
     * changed its theme.</p>
     */
    /**
     * What the example's own source says about reproducing it.
     *
     * <p>Two things the register cannot be trusted to carry, because both follow from the code
     * rather than from a decision somebody recorded. A class without a {@code main} is driven by
     * {@code GenerateAllExamples} and cannot be run on its own, so offering a reader an
     * {@code exec:java} command for it hands them a command that fails. And a document that
     * reaches a second backend needs that backend's artifact: the DOCX one is named in an
     * import, while the PPTX one is discovered by format and so is named nowhere at all — the
     * call is the only sign the source carries. Either way the code compiles for a reader and
     * then throws at render, which is the same shape as a missing font.</p>
     *
     * <p>The source is not the whole answer for decks, and this method does not pretend to give
     * it: a card can publish a deck rendered by a sibling class, which its own example never
     * mentions. The caller adds the PPTX artifact for any card that publishes one, so what a
     * reader is asked for follows from the document offered as well as from the code named.</p>
     *
     * @param repoRoot   the repository root the source path is resolved against
     * @param sourcePath the card's example source, as the register records it
     * @return what that source says; nothing claimed when the file is not there
     * @throws IOException if the source cannot be read
     */
    private static SourceFacts sourceFacts(Path repoRoot, String sourcePath) throws IOException {
        if (sourcePath == null) {
            return new SourceFacts(false, List.of());
        }
        Path source = repoRoot.resolve(sourcePath);
        if (!Files.isRegularFile(source)) {
            return new SourceFacts(false, List.of());
        }
        String text = Files.readString(source);
        List<String> extra = new ArrayList<>();
        if (text.contains(DOCX_IMPORT)) {
            extra.add("graph-compose-render-docx");
        }
        if (PPTX_CALLS.stream().anyMatch(text::contains)) {
            extra.add("graph-compose-render-pptx");
        }
        return new SourceFacts(MAIN_METHOD.matcher(text).find(), List.copyOf(extra));
    }

    private static boolean embedsAFaceOfItsOwn(PDDocument document) throws IOException {
        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            if (resources == null) {
                continue;
            }
            for (COSName name : resources.getFontNames()) {
                PDFont font = resources.getFont(name);
                if (font != null && font.isEmbedded()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The same page at strip size, halved a step at a time.
     *
     * <p>A page is nearly three times the width of its thumbnail, and bilinear sampling reads a
     * 2x2 neighbourhood: in one step it skips over most of the pixels and leaves fine strokes
     * aliased. Halving until the last step is within 2x keeps the shrunk page readable as a
     * page, which is the only reason to show one at 320px.</p>
     */
    private static void writeThumbnail(BufferedImage page, Path target) throws IOException {
        BufferedImage thumbnail = page;
        while (thumbnail.getWidth() > THUMBNAIL_WIDTH * 2) {
            thumbnail = scaledTo(thumbnail, Math.max(THUMBNAIL_WIDTH, thumbnail.getWidth() / 2));
        }
        if (thumbnail.getWidth() != THUMBNAIL_WIDTH) {
            thumbnail = scaledTo(thumbnail, THUMBNAIL_WIDTH);
        }
        ImageIO.write(thumbnail, "PNG", target.toFile());
    }

    private static BufferedImage scaledTo(BufferedImage source, int width) {
        int height = Math.max(1, Math.round(source.getHeight() * (width / (float) source.getWidth())));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D canvas = scaled.createGraphics();
        canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        canvas.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        canvas.drawImage(source, 0, 0, width, height, null);
        canvas.dispose();
        return scaled;
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

    private static String renderManifest(Map<String, Map<String, List<ManifestEntry>>> tree, Path repoRoot)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"schemaVersion\": 2,\n  \"categories\": [\n");
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
        sb.append("\n  ],\n");
        sb.append("  \"snippets\": ").append(snippetsJson(repoRoot)).append("\n");
        sb.append("}\n");
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
                // Everything else below U+0020 is a control character, which JSON forbids raw:
                // a manifest carrying one is refused by the page's own fetch and by the guard.
                default -> sb.append(c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    /**
     * The code of one {@code doc-example}-marked block on a documentation page.
     *
     * <p>The shape read here is the contract {@code DocumentationSnippetCompileTest} compiles
     * against: an HTML comment naming the block, immediately followed by a fenced {@code java}
     * block. Reading the same two lines is what keeps the snippet the site publishes and the
     * snippet a compiler checked the same text.</p>
     *
     * @param doc       the page to read
     * @param exampleId the {@code id=} the marker carries
     * @return the block's lines, joined with newlines
     * @throws IllegalStateException when the page carries no such block, so a marker that is
     *                               renamed or removed stops the sync instead of publishing a
     *                               catalogue that quietly lost its snippet
     */
    private static String readMarkedBlock(Path doc, String exampleId) throws IOException {
        List<String> lines = Files.readAllLines(doc);
        for (int i = 0; i < lines.size(); i++) {
            String marker = lines.get(i).trim();
            if (!marker.startsWith("<!--") || !marker.contains("doc-example:") || !names(marker, exampleId)) {
                continue;
            }
            if (i + 1 >= lines.size() || !lines.get(i + 1).trim().equals("```java")) {
                throw new IllegalStateException("doc-example '" + exampleId + "' in " + doc
                        + " is not followed by a java fence");
            }
            List<String> code = new ArrayList<>();
            for (int j = i + 2; j < lines.size(); j++) {
                if (lines.get(j).trim().equals("```")) {
                    return String.join("\n", code);
                }
                code.add(lines.get(j));
            }
            throw new IllegalStateException("doc-example '" + exampleId + "' in " + doc
                    + " opens a fence that is never closed");
        }
        throw new IllegalStateException("no doc-example '" + exampleId + "' in " + doc
                + " — the panel's snippet for that family has moved or been removed");
    }

    /** Whether a marker's attributes carry exactly this {@code id=}. */
    private static boolean names(String marker, String exampleId) {
        for (String token : marker.split("\\s+")) {
            if (token.equals("id=" + exampleId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The manifest's {@code snippets} object: one entry per family that has a compiled block,
     * carrying the page it came from, the block's id, and the code itself.
     *
     * @param repoRoot the repository root the documentation pages are resolved against
     * @return the object as JSON
     * @throws IOException if a page cannot be read
     */
    private static String snippetsJson(Path repoRoot) throws IOException {
        StringBuilder sb = new StringBuilder("{\n");
        boolean first = true;
        for (Map.Entry<String, SnippetSource> family : SNIPPETS.entrySet()) {
            if (!first) {
                sb.append(",\n");
            }
            first = false;
            SnippetSource source = family.getValue();
            sb.append("    ").append(jsonString(family.getKey())).append(": {\n");
            sb.append("      \"source\": ").append(jsonString(source.doc())).append(",\n");
            sb.append("      \"exampleId\": ").append(jsonString(source.exampleId())).append(",\n");
            sb.append("      \"code\": ")
                    .append(jsonString(readMarkedBlock(repoRoot.resolve(source.doc()), source.exampleId())))
                    .append("\n");
            sb.append("    }");
        }
        sb.append("\n  }");
        return sb.toString();
    }

    /**
     * The {@code ats} object on a CV card: the classification, whether it earns the badge,
     * what the sample was read with and when, what the parsers still get wrong, and the page
     * that explains the claim.
     *
     * <p>{@code badge} is decided here from the status, so the manifest states it outright.
     * The page checks the status as well, so a badge flag edited on its own shows nothing.</p>
     *
     * @param ats    the classification
     * @param indent the indentation of the line the object opens on
     * @return the object as JSON
     */
    static String atsJson(ShowcaseMetadata.Ats ats, String indent) {
        String inner = indent + "  ";
        return "{\n"
                + inner + "\"status\": " + jsonString(ats.status().name()) + ",\n"
                + inner + "\"badge\": " + ats.status().earnsBadge() + ",\n"
                + inner + "\"tested\": " + jsonArray(ats.tested()) + ",\n"
                + inner + "\"lastValidated\": " + jsonString(ats.lastValidated()) + ",\n"
                + inner + "\"knownLimitations\": " + jsonArray(ats.knownLimitations()) + ",\n"
                + inner + "\"details\": " + jsonString(ShowcaseMetadata.ATS_DETAILS_URL) + "\n"
                + indent + "}";
    }

    private static String jsonArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(jsonString(values.get(i)));
        }
        return sb.append("]").toString();
    }

    /**
     * One showcase card. {@code pptx} is {@code null} for the majority of
     * examples that render PDF only; the twin flagships carry both. {@code ats}
     * is {@code null} on every card but a CV preset's.
     */
    private record ManifestEntry(
            String id,
            ShowcaseMetadata.Entry meta,
            String pdf,
            String pptx,
            String screenshot,
            String thumbnail,
            Preview preview,
            List<String> pages,
            List<String> artifacts,
            boolean runnable,
            ShowcaseMetadata.Ats ats) {

        String toJson() {
            String indent = "              ";
            StringBuilder sb = new StringBuilder("{\n");
            sb.append(indent).append("\"id\": ").append(jsonString(id)).append(",\n");
            sb.append(indent).append("\"title\": ").append(jsonString(meta.title())).append(",\n");
            sb.append(indent).append("\"description\": ").append(jsonString(meta.description())).append(",\n");
            sb.append(indent).append("\"kind\": ").append(jsonString(meta.kind().name())).append(",\n");
            sb.append(indent).append("\"tags\": ").append(jsonArray(meta.tags())).append(",\n");
            sb.append(indent).append("\"pdf\": ").append(jsonString(pdf)).append(",\n");
            if (pptx != null) {
                sb.append(indent).append("\"pptx\": ").append(jsonString(pptx)).append(",\n");
            }
            sb.append(indent).append("\"screenshot\": ").append(jsonString(screenshot)).append(",\n");
            sb.append(indent).append("\"thumbnail\": ").append(jsonString(thumbnail)).append(",\n");
            // Only where there is more than one page, so the 84 single-page cards carry no empty
            // array. Page 1 is the screenshot above; these are the rest, in reading order.
            if (!pages.isEmpty()) {
                sb.append(indent).append("\"pages\": ").append(jsonArray(pages)).append(",\n");
            }
            // The page's own size, so a card can hold its shape before the image arrives.
            sb.append(indent).append("\"previewWidth\": ").append(preview.width()).append(",\n");
            sb.append(indent).append("\"previewHeight\": ").append(preview.height()).append(",\n");
            sb.append(indent).append("\"pageCount\": ").append(preview.pages()).append(",\n");
            // Measured from the document, not declared: a card that embeds a face needs the
            // artifact carrying the bundled faces, which the engine and templates do not.
            sb.append(indent).append("\"needsBundledFonts\": ")
                    .append(preview.needsBundledFonts()).append(",\n");
            // The register's list plus whatever the example's source reaches for: a backend a
            // reader would otherwise discover is missing only when the render throws.
            sb.append(indent).append("\"requiredArtifacts\": ").append(jsonArray(artifacts)).append(",\n");
            sb.append(indent).append("\"sourcePath\": ").append(jsonString(meta.sourcePath())).append(",\n");
            // Whether that class can be run on its own, or is one GenerateAllExamples drives.
            sb.append(indent).append("\"runnable\": ").append(runnable).append(",\n");
            if (meta.presetClass() != null) {
                sb.append(indent).append("\"presetClass\": ").append(jsonString(meta.presetClass())).append(",\n");
                sb.append(indent).append("\"dataModel\": ").append(jsonString(meta.dataModel())).append(",\n");
            }
            if (meta.variantOf() != null) {
                sb.append(indent).append("\"variantOf\": ").append(jsonString(meta.variantOf())).append(",\n");
            }
            sb.append(indent).append("\"code\": ").append(jsonString(meta.codeUrl()));
            if (ats != null) {
                sb.append(",\n").append(indent).append("\"ats\": ").append(atsJson(ats, indent));
            }
            sb.append("\n            }");
            return sb.toString();
        }
    }
}
