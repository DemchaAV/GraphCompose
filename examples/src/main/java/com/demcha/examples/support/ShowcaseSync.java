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
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Build-time tool that mirrors generated example PDFs into the
 * GitHub Pages site under {@code docs/showcase/}.
 *
 * <p>For each PDF found under
 * {@code examples/target/generated-pdfs/<category>/<group>/<file>.pdf}
 * the tool:</p>
 *
 * <ol>
 *   <li>Copies the PDF to
 *       {@code docs/showcase/pdf/<category>/<group>/<file>.pdf}.</li>
 *   <li>Renders page 0 of the PDF to a {@code 1.5x} PNG and writes it to
 *       {@code docs/showcase/screenshots/<category>/<group>/<file>.png}
 *       — the static site uses these as preview thumbnails.</li>
 * </ol>
 *
 * <p>Then it writes a structured {@code docs/examples.json} manifest
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
        Path docsRoot = repoRoot.resolve("docs");
        Path showcaseRoot = docsRoot.resolve("showcase");
        Path manifestFile = docsRoot.resolve("examples.json");

        if (!Files.isDirectory(generatedPdfs)) {
            throw new IllegalStateException("No generated PDFs found at " + generatedPdfs
                    + ". Run GenerateAllExamples first.");
        }

        // category → group → list of entries
        Map<String, Map<String, List<ManifestEntry>>> tree = new TreeMap<>();
        int copied = 0;
        int rendered = 0;

        try (Stream<Path> walk = Files.walk(generatedPdfs)) {
            List<Path> pdfs = walk
                    .filter(p -> p.toString().endsWith(".pdf"))
                    .sorted()
                    .toList();
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

                ShowcaseMetadata.Entry meta = ShowcaseMetadata.lookup(basename, category, group);
                ManifestEntry entry = new ManifestEntry(
                        basename,
                        meta.title(),
                        meta.description(),
                        meta.tags(),
                        relativeUrl(showcaseRoot, pdfTarget, docsRoot),
                        relativeUrl(showcaseRoot, pngTarget, docsRoot),
                        meta.codeUrl());
                tree.computeIfAbsent(category, c -> new TreeMap<>())
                        .computeIfAbsent(group, g -> new ArrayList<>())
                        .add(entry);
            }
        }

        for (Map<String, List<ManifestEntry>> groups : tree.values()) {
            for (List<ManifestEntry> entries : groups.values()) {
                entries.sort(Comparator.comparing(ManifestEntry::id));
            }
        }

        String json = renderManifest(tree);
        Files.writeString(manifestFile, json);

        System.out.println("Synced " + copied + " PDFs and " + rendered
                + " preview PNGs into " + showcaseRoot);
        System.out.println("Wrote manifest to " + manifestFile);
    }

    private static void renderPreview(Path pdfPath, Path pngTarget) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImage(0, PREVIEW_SCALE, ImageType.RGB);
            ImageIO.write(image, "PNG", pngTarget.toFile());
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
            boolean firstGroup = true;
            for (Map.Entry<String, List<ManifestEntry>> ge : groups.entrySet()) {
                if (!firstGroup) sb.append(",\n");
                firstGroup = false;
                sb.append("        {\n");
                sb.append("          \"id\": ").append(jsonString(ge.getKey())).append(",\n");
                sb.append("          \"label\": ").append(jsonString(ShowcaseMetadata.groupLabel(catId, ge.getKey()))).append(",\n");
                sb.append("          \"examples\": [\n");
                boolean firstExample = true;
                for (ManifestEntry entry : ge.getValue()) {
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

    private record ManifestEntry(
            String id,
            String title,
            String description,
            List<String> tags,
            String pdf,
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
            sb.append("              \"screenshot\": ").append(jsonString(screenshot)).append(",\n");
            sb.append("              \"code\": ").append(jsonString(code)).append("\n");
            sb.append("            }");
            return sb.toString();
        }
    }
}
