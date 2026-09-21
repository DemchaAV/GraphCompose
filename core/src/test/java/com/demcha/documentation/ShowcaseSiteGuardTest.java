package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards the showcase site under {@code web/} against sending a visitor to something it
 * does not publish.
 *
 * <p>GitHub Pages serves {@code web/} exactly as committed, so a reference that resolves
 * to nothing reaches visitors as a dead link, a missing tile or a menu entry that moves
 * nothing. Each kind checked here had passed every other check: the featured strip skips
 * an id that is not a card in the manifest instead of reporting it, a card's files are
 * checked only while {@code ShowcaseSync} writes them, not when a later change removes one,
 * and the document pages link back to the site through relative paths a moved file breaks
 * without a sign.</p>
 *
 * <p>It reads the working tree, so the verify gate of a release cut runs it over the
 * catalogue the cut has just regenerated.</p>
 */
class ShowcaseSiteGuardTest {

    private static final Path WEB = RepoRoot.get().resolve("web");
    private static final Path SHOWCASE = WEB.resolve("showcase");

    /** The address {@code web/} is published at; a link that starts with it names a site file. */
    private static final String SITE_URL = "https://demchaav.github.io/GraphCompose/";

    /**
     * The pages whose links are checked, beside every generated document page, which are found
     * rather than listed. {@code examples.js} builds its links from the manifest.
     */
    private static final List<String> PAGES = List.of("index.html", "sitemap.xml", "robots.txt");

    /** The pages whose anchors are checked, beside every generated document page. */
    private static final List<String> ANCHOR_PAGES = List.of("index.html", "sitemap.xml");

    /** The suffix {@code examples.js} gives the id of a rendered category section. */
    private static final String SECTION_SUFFIX = "-section";

    /** The featured-strip constant in {@code examples.js}, up to its closing bracket. */
    private static final Pattern FEATURED_LIST =
            Pattern.compile("const\\s+HIGHLIGHT_IDS\\s*=\\s*\\[([^\\]]*)\\]");

    /** One entry of that list, in either quote style. */
    private static final Pattern QUOTED = Pattern.compile("'([^']*)'|\"([^\"]*)\"");

    /** The value of an {@code href} or {@code src} attribute, in either quote style. */
    private static final Pattern LINK_ATTRIBUTE =
            Pattern.compile("(?<![\\w-])(?:href|src)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");

    /** An absolute address inside the site, wherever it is written: an attribute, JSON-LD, a sitemap entry. */
    private static final Pattern SITE_ADDRESS =
            Pattern.compile(Pattern.quote(SITE_URL) + "([^\"'<>\\s]*)");

    /** A scheme such as {@code https:} or {@code mailto:} marks a link that leaves the site. */
    private static final Pattern SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:");

    /** The value of an {@code id} attribute. */
    private static final Pattern ELEMENT_ID = Pattern.compile("(?<![\\w-])id\\s*=\\s*\"([^\"]+)\"");

    /** The category a filter pill selects. */
    private static final Pattern FILTER_PILL = Pattern.compile("data-category\\s*=\\s*\"([^\"]+)\"");

    /**
     * The widest a strip thumbnail may be. The strip draws a document in a 54px slot, and the
     * catalogue writes these at 320px; the ceiling is what stops a thumbnail from quietly
     * becoming the whole page again, which is the cost the strip has its own files to avoid.
     */
    private static final int THUMBNAIL_CEILING = 400;

    /** How many CV presets the page tells a visitor ship. */
    private static final Pattern CV_PRESET_CLAIM = Pattern.compile("(\\d+)\\s+CV presets");

    /** How many cover letters it claims beside them. */
    private static final Pattern LETTER_CLAIM =
            Pattern.compile("(\\d+)\\s+matching\\s+(?:cover\\s+)?letters");

    /**
     * A viewer address is read and shared as it stands, so category, family and card ids stay
     * lowercase words and hyphens: an id needing percent-encoding still works, but the address
     * stops being readable.
     */
    private static final Pattern ADDRESS_SAFE_ID = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    /**
     * The tallest the reproduction panel may be capped at, in {@code vh}.
     *
     * <p>A budget, not a style preference: the panel opens over the document it explains, so its
     * cap decides how much of the page a reader can still see. At 42vh the stage fell to 149px
     * and an A4 page rendered at 74×105. 34 is the narrow-screen cap; the desktop one is lower.</p>
     */
    private static final int PANEL_HEIGHT_CEILING = 34;

    /**
     * Each {@code max-height} declared on the panel itself, base rule and media queries alike.
     *
     * <p>The class may sit anywhere in a selector list, so a cap cannot be smuggled past this by
     * grouping the rule with another selector, and the whole value is captured rather than a
     * {@code vh} number — switching the cap to {@code px} has to fail here, not slip through as
     * "no cap found". The negative lookahead keeps {@code .gallery-viewer-panel-toggle} and the
     * other {@code -row} / {@code -label} rules out of it.</p>
     */
    private static final Pattern PANEL_MAX_HEIGHT =
            Pattern.compile("\\.gallery-viewer-panel(?![-\\w])[^{]*\\{[^}]*?max-height:\\s*([^;]+);",
                    Pattern.DOTALL);

    /** A cap expressed against the viewport, which is the only form this budget can read. */
    private static final Pattern VIEWPORT_HEIGHT = Pattern.compile("(\\d+)vh");

    /** The {@code flex} shorthand on the disclosure button, which must not let it grow. */
    private static final Pattern PANEL_TOGGLE_FLEX =
            Pattern.compile("\\.gallery-viewer-panel-toggle\\s*\\{[^}]*?flex:\\s*([^;]+);", Pattern.DOTALL);

    /** The family → guide map the panel links, in {@code gallery-viewer.js}. */
    private static final Pattern FAMILY_GUIDES_BLOCK =
            Pattern.compile("const FAMILY_GUIDES = \\{(.*?)\\};", Pattern.DOTALL);

    /** One entry of that map: the family id, and the page it points at, in either quote style. */
    private static final Pattern FAMILY_GUIDE_ENTRY =
            Pattern.compile("(\\w+):\\s*['\"]([^'\"]+)['\"]");

    @Test
    void everyFeaturedExampleIsACardInTheCatalogue() throws IOException {
        List<String> featured = featuredIds(read("examples.js"));
        Set<String> cardIds = new LinkedHashSet<>();
        for (Map<String, Object> card : cards(readManifest())) {
            cardIds.add(String.valueOf(card.get("id")));
        }

        assertThat(featured)
                .describedAs("the featured list in web/examples.js is empty — this guard would have nothing to check")
                .isNotEmpty();

        List<String> missing = featured.stream().filter(id -> !cardIds.contains(id)).toList();
        assertThat(missing)
                .describedAs("web/examples.js features ids that are not cards in web/examples.json; the strip "
                        + "skips such an id without a sign, so each one silently disappears from the page")
                .isEmpty();
    }

    @Test
    void everyFileACardNamesIsPublishedUnderShowcase() throws IOException {
        List<Map<String, Object>> cards = cards(readManifest());
        assertThat(cards)
                .describedAs("web/examples.json lists no cards — this guard would have nothing to check")
                .isNotEmpty();

        Set<String> missing = new TreeSet<>();
        for (Map<String, Object> card : cards) {
            for (String key : List.of("pdf", "screenshot", "thumbnail", "pptx")) {
                Object value = card.get(key);
                if (value == null && key.equals("pptx")) {
                    continue; // only the examples that render a deck ship one
                }
                if (!(value instanceof String path) || !isShowcaseFile(path)) {
                    missing.add(card.get("id") + " " + key + ": " + value);
                }
            }
            // The pages after the first are an array rather than one path, so they cannot join
            // the loop above. Absent is correct for the 84 single-page documents; present and
            // empty is not, because nothing would then be published under a name the page asks for.
            if (card.get("pages") != null) {
                if (!(card.get("pages") instanceof List<?> published) || published.isEmpty()) {
                    missing.add(card.get("id") + " pages: " + card.get("pages"));
                } else {
                    for (Object page : published) {
                        if (!(page instanceof String path) || !isShowcaseFile(path)) {
                            missing.add(card.get("id") + " pages: " + page);
                        }
                    }
                }
            }
        }
        assertThat(missing)
                .describedAs("a card in web/examples.json names something that is not a file under web/showcase/, "
                        + "so its preview, PDF or deck is broken on the published site — regenerate with "
                        + "ShowcaseSync rather than editing the manifest")
                .isEmpty();
    }

    @Test
    void everySiteFileThePagesLinkToIsPublished() throws IOException {
        List<String> generated = generatedPages();
        assertThat(generated)
                .describedAs("found no generated document page under web/ — scripts/site/build.mjs writes one per "
                        + "card, so either the build has not run or this guard no longer finds its pages")
                .isNotEmpty();

        List<String> pages = new ArrayList<>(PAGES);
        pages.addAll(generated);
        Map<String, Set<String>> broken = new LinkedHashMap<>();
        for (String page : pages) {
            Set<String> references = siteReferences(read(page), directoryOf(page));
            assertThat(references)
                    .describedAs("found no link to a site file in web/%s — this guard is reading a page shape "
                            + "that moved, so it is no longer checking that page", page)
                    .isNotEmpty();

            Set<String> missing = new TreeSet<>();
            for (String reference : references) {
                if (!isPublished(reference)) {
                    missing.add(reference.isEmpty() ? "(site root)" : reference);
                }
            }
            if (!missing.isEmpty()) {
                broken.put(page, missing);
            }
        }
        assertThat(broken)
                .describedAs("these pages link to files the site does not publish; GitHub Pages serves web/ as "
                        + "committed, so each one is a dead link")
                .isEmpty();
    }

    @Test
    void everyAnchorAndFilterNamesSomethingThePageShows() throws IOException {
        Object manifest = readManifest();
        Set<String> categories = categoryIds(manifest);
        Map<String, Map<String, Set<String>>> families = families(manifest);
        String index = read("index.html");
        Set<String> elementIds = matches(ELEMENT_ID, index);

        List<String> pages = new ArrayList<>(ANCHOR_PAGES);
        pages.addAll(generatedPages());
        Map<String, Set<String>> broken = new LinkedHashMap<>();
        for (String page : pages) {
            Map<String, Set<String>> anchorsByTarget = anchors(read(page), directoryOf(page));
            assertThat(anchorsByTarget)
                    .describedAs("found no anchor in web/%s — this guard is reading a page shape that moved, so "
                            + "it is no longer checking that page", page)
                    .isNotEmpty();

            Set<String> missing = new TreeSet<>();
            for (Map.Entry<String, Set<String>> target : anchorsByTarget.entrySet()) {
                // Anchors into the home page follow its rules; an anchor into any other page needs an
                // element with that id on that page, which has to be a page the site publishes.
                Set<String> targetIds = target.getKey().equals("index.html") || !isPublished(target.getKey())
                        ? Set.of()
                        : matches(ELEMENT_ID, read(target.getKey()));
                for (String anchor : target.getValue()) {
                    boolean lands = !target.getKey().equals("index.html")
                            ? targetIds.contains(anchor)
                            : anchor.startsWith("/")
                            ? viewerAddressLands(anchor, families)
                            : anchor.endsWith(SECTION_SUFFIX)
                            ? categories.contains(anchor.substring(0, anchor.length() - SECTION_SUFFIX.length()))
                            : elementIds.contains(anchor);
                    if (!lands) {
                        missing.add(target.getKey().equals("index.html") ? "#" + anchor : target.getKey() + "#" + anchor);
                    }
                }
            }
            if (!missing.isEmpty()) {
                broken.put(page, missing);
            }
        }
        assertThat(broken)
                .describedAs("these anchors land nowhere: examples.js renders a category section as "
                        + "<category id>%s only for a category web/examples.json has, the viewer opens "
                        + "#/<category>/<family>[/<card>] only for a family and card it has, and any other "
                        + "anchor needs an element with that id on the page it points into", SECTION_SUFFIX)
                .isEmpty();

        Set<String> pills = matches(FILTER_PILL, index);
        assertThat(pills)
                .describedAs("found no 'All' filter pill in web/index.html — this guard is reading a page shape that moved")
                .contains("all");
        pills.remove("all");
        pills.removeAll(categories);
        assertThat(pills)
                .describedAs("filter pills in web/index.html select categories web/examples.json does not have, "
                        + "so choosing one empties the gallery")
                .isEmpty();
    }

    @Test
    void everyCardAndFamilyIdIsUniqueAndReadsTheSameInAnAddress() throws IOException {
        Object manifest = readManifest();
        List<Map<String, Object>> cards = cards(manifest);
        assertThat(cards)
                .describedAs("web/examples.json lists no cards — this guard would have nothing to check")
                .isNotEmpty();

        Set<String> seen = new TreeSet<>();
        Set<String> duplicates = new TreeSet<>();
        Set<String> unsafe = new TreeSet<>();
        for (Map<String, Object> card : cards) {
            String id = String.valueOf(card.get("id"));
            if (!seen.add(id)) {
                duplicates.add(id);
            }
            if (!ADDRESS_SAFE_ID.matcher(id).matches()) {
                unsafe.add("card " + id);
            }
        }
        families(manifest).forEach((category, groups) -> {
            if (!ADDRESS_SAFE_ID.matcher(category).matches()) {
                unsafe.add("category " + category);
            }
            groups.keySet().stream()
                    .filter(group -> !ADDRESS_SAFE_ID.matcher(group).matches())
                    .forEach(group -> unsafe.add("family " + category + "/" + group));
        });

        assertThat(duplicates)
                .describedAs("card ids in web/examples.json must be unique: the viewer and the featured list "
                        + "find a card by its id, so a second card with the same id cannot be reached")
                .isEmpty();
        assertThat(unsafe)
                .describedAs("ids in web/examples.json must be lowercase words joined by hyphens: the viewer "
                        + "writes them unescaped into #/<category>/<family>/<card> addresses")
                .isEmpty();
    }

    @Test
    void theManifestSaysWhichContractItWasWrittenTo() throws IOException {
        Object manifest = readManifest();
        assertThat(manifest)
                .describedAs("web/examples.json is not an object — the page could not read it either")
                .isInstanceOf(Map.class);

        Object version = ((Map<?, ?>) manifest).get("schemaVersion");
        assertThat(version)
                .describedAs("web/examples.json carries no schemaVersion. The page and this guard read the "
                        + "manifest field by field, so a catalogue written to a different contract looks like "
                        + "one with fields missing rather than one that has moved on")
                .isInstanceOf(Number.class);
        assertThat(((Number) version).intValue())
                .describedAs("web/examples.json was written to a contract this site does not read")
                .isEqualTo(2);
    }

    @Test
    void theReproductionPanelLeavesTheDocumentRoomToBeRead() throws IOException {
        String css = read("styles.css");

        Set<String> tooTall = new TreeSet<>();
        Matcher caps = PANEL_MAX_HEIGHT.matcher(css);
        int capsFound = 0;
        while (caps.find()) {
            capsFound++;
            String cap = caps.group(1).trim();
            Matcher viewport = VIEWPORT_HEIGHT.matcher(cap);
            if (!viewport.matches()) {
                // A cap in pixels is not a budget against the document: on a short window it is
                // the whole dialog, and this guard could not read it as too tall either.
                tooTall.add(cap + " (not measured against the viewport)");
            } else if (Integer.parseInt(viewport.group(1)) > PANEL_HEIGHT_CEILING) {
                tooTall.add(cap);
            }
        }

        assertThat(capsFound)
                .describedAs("no max-height on .gallery-viewer-panel in web/styles.css: uncapped, the panel "
                        + "takes whatever the dialog has, and the document it explains gets the rest")
                .isGreaterThan(0);
        assertThat(tooTall)
                .describedAs("the panel opens over the document, so this cap is how much of the page a "
                        + "reader can still see. At 42vh the stage fell to 149px and an A4 page rendered "
                        + "at 74x105 — smaller than the regression the disclosure was added to fix")
                .isEmpty();

        Matcher flex = PANEL_TOGGLE_FLEX.matcher(css);
        assertThat(flex.find())
                .describedAs("no flex shorthand on .gallery-viewer-panel-toggle, so nothing here notices it "
                        + "becoming a growing flex item again")
                .isTrue();
        assertThat(flex.group(1).trim())
                .describedAs("the disclosure is a control, not a panel: as a growing flex item it asked for "
                        + "the dialog's whole height and left the stage 40px with the page at 0x0")
                .startsWith("0 0");
    }

    @Test
    void everyFamilyGuideThePanelLinksIsAPageThatIsThere() throws IOException {
        String viewer = read("gallery-viewer.js");
        Matcher block = FAMILY_GUIDES_BLOCK.matcher(viewer);
        assertThat(block.find())
                .describedAs("no 'const FAMILY_GUIDES = { … };' in web/gallery-viewer.js — the map moved, "
                        + "and this guard no longer reads it")
                .isTrue();

        Map<String, String> guides = new LinkedHashMap<>();
        Matcher entry = FAMILY_GUIDE_ENTRY.matcher(block.group(1));
        while (entry.find()) {
            guides.put(entry.group(1), entry.group(2));
        }
        assertThat(guides)
                .describedAs("the map holds no entry, so the panel offers a reader no guide for any family")
                .isNotEmpty();

        Set<String> groupIds = new TreeSet<>();
        for (Map<String, Set<String>> groups : families(readManifest()).values()) {
            groupIds.addAll(groups.keySet());
        }

        Set<String> wrong = new TreeSet<>();
        for (Map.Entry<String, String> guide : guides.entrySet()) {
            String page = withoutFragmentOrQuery(guide.getValue());
            Path file = RepoRoot.get().resolve(page);
            if (!Files.isRegularFile(file)) {
                wrong.add(guide.getKey() + " points at " + guide.getValue() + ", which is not a file");
            } else if (guide.getValue().indexOf('#') >= 0 && !hasHeadingFor(file, guide.getValue())) {
                wrong.add(guide.getKey() + " points at " + guide.getValue()
                        + ", and that page carries no heading answering to it");
            }
            // The panel looks these up by the family id the catalogue uses. A key that is not one
            // resolves to nothing, and every card of that family quietly loses its guide link
            // while the page it points at is still perfectly there.
            if (!groupIds.contains(guide.getKey())) {
                wrong.add(guide.getKey() + " is not a family the catalogue has: the panel would find "
                        + "no guide under that name");
            }
        }

        assertThat(wrong)
                .describedAs("the panel links these pages at the release tag, so a guide that is renamed or "
                        + "moved becomes a 404 the reader meets and nothing else here would notice — the "
                        + "documentation guards read markdown and Java sources, never this script")
                .isEmpty();
    }

    @Test
    void everyPublishedSnippetIsTheBlockItWasCompiledFrom() throws IOException {
        Map<String, Object> snippets =
                object(object(readManifest(), "the manifest").get("snippets"), "snippets");
        assertThat(snippets)
                .describedAs("web/examples.json carries no snippets, so the panel shows a reader no code "
                        + "for any family — and this guard would be checking nothing")
                .isNotEmpty();

        Set<String> wrong = new TreeSet<>();
        for (Map.Entry<String, Object> family : snippets.entrySet()) {
            Map<String, Object> snippet = object(family.getValue(), "a snippet");
            String source = String.valueOf(snippet.get("source"));
            String exampleId = String.valueOf(snippet.get("exampleId"));
            Path doc = RepoRoot.get().resolve(source);
            if (!Files.isRegularFile(doc)) {
                wrong.add(family.getKey() + " names a page that is not there: " + source);
                continue;
            }
            String block = markedBlock(Files.readAllLines(doc), exampleId);
            if (block == null) {
                wrong.add(family.getKey() + " names no doc-example '" + exampleId + "' in " + source);
            } else if (!block.equals(String.valueOf(snippet.get("code")))) {
                wrong.add(family.getKey() + " publishes code that is not the block in " + source);
            }
        }

        assertThat(wrong)
                .describedAs("the panel publishes code a reader pastes into their own project, and the only "
                        + "reason to trust it is that a compiler already accepted it: each block is read back "
                        + "from the page DocumentationSnippetCompileTest compiles, so a snippet edited in the "
                        + "manifest, or a block that moved out from under its marker, fails here rather than "
                        + "shipping code that no longer builds")
                .isEmpty();
    }

    @Test
    void everyPresetCardCarriesWhatThePanelShows() throws IOException {
        List<Map<String, Object>> cards = cards(readManifest());
        Set<String> presetCards = new TreeSet<>();
        Set<String> wrong = new TreeSet<>();
        for (Map<String, Object> card : cards) {
            if (!"PRESET".equals(card.get("kind"))) {
                continue;
            }
            String id = String.valueOf(card.get("id"));
            presetCards.add(id);
            if (!(card.get("presetClass") instanceof String preset) || preset.isBlank()) {
                wrong.add(id + " names no presetClass");
            }
            if (!(card.get("dataModel") instanceof String model) || model.isBlank()) {
                wrong.add(id + " names no dataModel");
            }
            if (!(card.get("requiredArtifacts") instanceof List<?> artifacts)
                    || !artifacts.contains("graph-compose-templates")) {
                wrong.add(id + " builds a preset without requiring graph-compose-templates");
            }
            String source = String.valueOf(card.get("sourcePath"));
            if (!source.startsWith("examples/src/main/java/") || !source.endsWith(".java")) {
                wrong.add(id + " has no runnable source path: " + source);
            } else if (!Files.isRegularFile(RepoRoot.get().resolve(source))) {
                wrong.add(id + " names a source file that is not there: " + source);
            }
        }

        assertThat(presetCards)
                .describedAs("no card in web/examples.json is a PRESET, so this guard would hold nothing to "
                        + "the panel's contract")
                .isNotEmpty();
        assertThat(wrong)
                .describedAs("a PRESET card's panel tells a reader which class to call, which record to fill, "
                        + "what to depend on and where the runnable source is. A card missing any of those "
                        + "renders a panel with a gap in it, and nothing else on the site would notice")
                .isEmpty();
    }

    @Test
    void everyCardMeasuresThePageItPublishes() throws IOException {
        List<Map<String, Object>> cards = cards(readManifest());
        assertThat(cards)
                .describedAs("web/examples.json lists no cards — this guard would have nothing to check")
                .isNotEmpty();

        Set<String> wrong = new TreeSet<>();
        for (Map<String, Object> card : cards) {
            String id = String.valueOf(card.get("id"));
            if (intOf(card.get("pageCount")) < 1) {
                wrong.add(id + " pageCount: " + card.get("pageCount"));
            }
            // Every page after the first is published as an image of its own, so the set a card
            // names is exactly one shorter than the count it declares. A card claiming eight
            // pages while publishing three sends a reader to an address with nothing behind it.
            int beyondFirst = card.get("pages") instanceof List<?> published ? published.size() : 0;
            if (beyondFirst != Math.max(0, intOf(card.get("pageCount")) - 1)) {
                wrong.add(id + " declares " + card.get("pageCount") + " pages and publishes "
                        + beyondFirst + " beyond the first");
            }
            int[] preview = pngSize(WEB.resolve(String.valueOf(card.get("screenshot"))));
            if (preview == null) {
                wrong.add(id + " preview is not a readable PNG: " + card.get("screenshot"));
                continue;
            }
            int width = intOf(card.get("previewWidth"));
            int height = intOf(card.get("previewHeight"));
            if (preview[0] != width || preview[1] != height) {
                wrong.add(id + " says " + width + "x" + height
                        + ", its preview is " + preview[0] + "x" + preview[1]);
            }
            int[] thumbnail = pngSize(WEB.resolve(String.valueOf(card.get("thumbnail"))));
            if (thumbnail == null) {
                wrong.add(id + " thumbnail is not a readable PNG: " + card.get("thumbnail"));
            } else if (thumbnail[0] > THUMBNAIL_CEILING || thumbnail[0] >= preview[0]) {
                wrong.add(id + " thumbnail is " + thumbnail[0] + "px wide beside a "
                        + preview[0] + "px preview");
            }
        }

        assertThat(wrong)
                .describedAs("a card's measurements are what the page holds space with before the image "
                        + "arrives, and a page count below one describes no document. The thumbnail has to "
                        + "stay a thumbnail: the viewer's strip has files of its own precisely so that "
                        + "opening a family does not fetch a whole page per slot, and nothing else would "
                        + "notice it quietly becoming one again")
                .isEmpty();
    }

    @Test
    void thePageCountsThePresetsTheCatalogueHas() throws IOException {
        Object manifest = readManifest();
        int cvPresets = presetsOf(manifest, "templates", "cv").size();
        int letters = presetsOf(manifest, "templates", "coverletter").size();
        assertThat(cvPresets)
                .describedAs("no CV card in web/examples.json names a preset — this guard would be holding "
                        + "the page against nothing")
                .isGreaterThan(0);

        String index = read("index.html");
        Set<String> cvClaims = matches(CV_PRESET_CLAIM, index);
        Set<String> letterClaims = matches(LETTER_CLAIM, index);
        assertThat(cvClaims)
                .describedAs("web/index.html no longer counts the CV presets in words this guard reads, so "
                        + "the count it shows a visitor is no longer being checked against the catalogue")
                .isNotEmpty();
        assertThat(letterClaims)
                .describedAs("web/index.html no longer counts the cover letters in words this guard reads")
                .isNotEmpty();

        assertThat(cvClaims)
                .describedAs("the page tells a visitor how many CV presets ship, and the catalogue holds %d "
                        + "distinct ones — a card that re-renders another's preset with different options is "
                        + "not a second preset", cvPresets)
                .containsExactly(String.valueOf(cvPresets));
        assertThat(letterClaims)
                .describedAs("the page tells a visitor how many cover letters ship, and the catalogue holds %d",
                        letters)
                .containsExactly(String.valueOf(letters));
    }

    /**
     * The distinct presets the cards of one family name. A variant re-renders another card's
     * preset with different options, so it is one more card and not one more preset.
     */
    @SuppressWarnings("unchecked")
    private static Set<String> presetsOf(Object manifest, String categoryId, String groupId) {
        Set<String> presets = new TreeSet<>();
        for (Object category : (List<Object>) ((Map<String, Object>) manifest).get("categories")) {
            Map<String, Object> asCategory = (Map<String, Object>) category;
            if (!categoryId.equals(asCategory.get("id"))) {
                continue;
            }
            for (Object group : (List<Object>) asCategory.get("groups")) {
                Map<String, Object> asGroup = (Map<String, Object>) group;
                if (!groupId.equals(asGroup.get("id"))) {
                    continue;
                }
                for (Object example : (List<Object>) asGroup.get("examples")) {
                    if (((Map<String, Object>) example).get("presetClass") instanceof String preset) {
                        presets.add(preset);
                    }
                }
            }
        }
        return presets;
    }

    /** A manifest number: the strict reader hands every one of them back as a double. */
    private static int intOf(Object value) {
        return value instanceof Number number ? number.intValue() : -1;
    }

    /**
     * The pixel size written in a PNG's IHDR, or {@code null} when the file is not a readable
     * PNG. Read from the header rather than decoded: the question is what the file says it is.
     */
    private static int[] pngSize(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        byte[] header;
        try (InputStream bytes = Files.newInputStream(file)) {
            header = bytes.readNBytes(24);
        }
        if (header.length < 24
                || (header[0] & 0xFF) != 0x89 || header[1] != 'P' || header[2] != 'N' || header[3] != 'G'
                || header[12] != 'I' || header[13] != 'H' || header[14] != 'D' || header[15] != 'R') {
            return null;
        }
        return new int[] {bigEndianInt(header, 16), bigEndianInt(header, 20)};
    }

    private static int bigEndianInt(byte[] bytes, int at) {
        return ((bytes[at] & 0xFF) << 24) | ((bytes[at + 1] & 0xFF) << 16)
                | ((bytes[at + 2] & 0xFF) << 8) | (bytes[at + 3] & 0xFF);
    }

    @Test
    void aViewerAddressLandsOnlyOnAFamilyAndACardItHolds() {
        Map<String, Map<String, Set<String>>> families =
                Map.of("templates", Map.of("cv", Set.of("cv-a", "cv-b")));

        assertThat(viewerAddressLands("/templates/cv", families)).isTrue();
        assertThat(viewerAddressLands("/templates/cv/cv-b", families)).isTrue();
        assertThat(viewerAddressLands("/templates/cv/cv-c", families)).isFalse();
        assertThat(viewerAddressLands("/templates/invoice", families)).isFalse();
        assertThat(viewerAddressLands("/features/cv", families)).isFalse();
        assertThat(viewerAddressLands("/templates", families)).isFalse();
        assertThat(viewerAddressLands("/templates//cv", families)).isFalse();
        assertThat(viewerAddressLands("/templates/cv/", families)).isFalse();
        assertThat(viewerAddressLands("/templates/cv/cv-a/extra", families)).isFalse();
    }

    @Test
    void theFeaturedListIsReadInEitherQuoteStyleAndRefusedWhenItMoves() {
        assertThat(featuredIds("const HIGHLIGHT_IDS = [\n  'first',\n  \"second\"\n];"))
                .containsExactly("first", "second");
        assertThatThrownBy(() -> featuredIds("const FEATURED = ['first'];"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aSiteReferenceIsAPathInsideTheSite() {
        String page = "<link href=\"styles.css\"><a href=\"showcase/pdf/a.pdf#page=2\"><a href='single.pdf'>"
                + "<a href=\"#install\"><a href=\"https://github.com/DemchaAV\">"
                + "<img src=\"//cdn.example.com/x.png\"><img data-src=\"lazy.png\">"
                + "<a href=\"mailto:someone@example.com\"><meta content=\"" + SITE_URL + "showcase/b.png\">"
                + "<loc>" + SITE_URL + "#features-section</loc>";

        assertThat(siteReferences(page))
                .containsExactlyInAnyOrder("styles.css", "showcase/pdf/a.pdf", "single.pdf", "showcase/b.png", "");
    }

    @Test
    void anAnchorIsReadFromInPageLinksAndSiteAddresses() {
        String page = "<a href=\"#install\"><a href='#top'><a href=\"#\"><a href=\"showcase/a.pdf#page=3\">"
                + "<a href=\"#/templates/cv/cv-a\">"
                + "<loc>" + SITE_URL + "#features-section</loc><loc>" + SITE_URL + "index.html#showcase</loc>"
                + "<loc>" + SITE_URL + "#/templates/cv</loc><loc>" + SITE_URL + "showcase/b.pdf#page=2</loc>";

        assertThat(anchors(page)).containsExactlyInAnyOrder(
                "install", "top", "features-section", "showcase", "/templates/cv/cv-a", "/templates/cv");
    }

    @Test
    void aLinkOnADocumentPageIsReadFromThatPagesOwnDirectory() {
        String page = "<link href=\"../../../styles.css\"><a href=\"../../../templates/cv/cv-b/\">"
                + "<img src=\"../../../showcase/screenshots/a.png\"><a href=\"../../../\">"
                + "<a href=\"../../../showcase/pdf/a.pdf#page=2\"><a href=\"../../../../outside.html\">"
                + "<link rel=\"canonical\" href=\"" + SITE_URL + "templates/cv/cv-a/\"><link href=\"/styles.css\">";

        assertThat(siteReferences(page, "templates/cv/cv-a/")).containsExactlyInAnyOrder(
                "styles.css", "templates/cv/cv-b/", "showcase/screenshots/a.png", "", "showcase/pdf/a.pdf",
                "../outside.html", "templates/cv/cv-a/", "/styles.css");
        assertThat(isPublished("../outside.html"))
                .describedAs("a link climbing out of web/ names nothing the site publishes")
                .isFalse();
        assertThat(isPublished("/styles.css"))
                .describedAs("the site is served under /GraphCompose/, so a root-relative link misses it")
                .isFalse();
    }

    @Test
    void aLinkThatIsNotAValidUriIsReportedRatherThanCrashingTheGuard() {
        // A browser encodes a raw space and follows the link; a percent-escape already present is
        // decoded, not encoded twice; and a character no file can be named with is simply unpublished.
        String page = "<a href=\"docs/my file.pdf\"><a href=\"docs/my%20file.pdf\"><a href=\"a|b.html\">"
                + "<a href=\"{brace}.png\"><a href=\"bad%zz.png\">";

        assertThat(siteReferences(page, "templates/cv/cv-a/")).containsExactlyInAnyOrder(
                "templates/cv/cv-a/docs/my file.pdf", "templates/cv/cv-a/a|b.html",
                "templates/cv/cv-a/{brace}.png", "templates/cv/cv-a/bad%zz.png");
        assertThat(isPublished("templates/cv/cv-a/a|b.html")).isFalse();
    }

    @Test
    void anAnchorOnADocumentPageIsReadAsAnAnchorIntoThePageItPointsAt() {
        String page = "<a href=\"../../../#install\"><a href=\"../../../#/templates/cv\"><a href=\"#page-2\">"
                + "<a href=\"../cv-b/#top\"><a href=\"../../../showcase/pdf/a.pdf#page=2\">";

        assertThat(anchors(page, "templates/cv/cv-a/")).isEqualTo(Map.of(
                "index.html", Set.of("install", "/templates/cv"),
                "templates/cv/cv-a/index.html", Set.of("page-2"),
                "templates/cv/cv-b/index.html", Set.of("top")));
    }

    /** The ids in the featured-strip constant of {@code examples.js}. */
    static List<String> featuredIds(String script) {
        Matcher list = FEATURED_LIST.matcher(script);
        if (!list.find()) {
            throw new IllegalStateException("no 'const HIGHLIGHT_IDS = [...]' in web/examples.js — the featured "
                    + "list moved, and this guard no longer reads it");
        }
        List<String> ids = new ArrayList<>();
        Matcher entry = QUOTED.matcher(list.group(1));
        while (entry.find()) {
            ids.add(entry.group(1) != null ? entry.group(1) : entry.group(2));
        }
        return ids;
    }

    /** The site files a page at the root of {@code web/} links to; see the two-argument form. */
    static Set<String> siteReferences(String page) {
        return siteReferences(page, "");
    }

    /**
     * The site files a page links to, as paths relative to {@code web/}: every relative
     * {@code href} and {@code src}, read from the page's own {@code directory} (empty for the
     * root, {@code templates/cv/cv-a/} for a document page), and every absolute address inside
     * the site. A fragment or query is dropped, so the site root is the empty path, and a link
     * that climbs out of {@code web/} keeps its leading {@code ../} so it can never be published.
     */
    static Set<String> siteReferences(String page, String directory) {
        Set<String> references = new TreeSet<>();
        Matcher attribute = LINK_ATTRIBUTE.matcher(page);
        while (attribute.find()) {
            String value = attributeValue(attribute);
            if (value.startsWith("#") || value.startsWith("//") || SCHEME.matcher(value).find()) {
                continue;
            }
            references.add(resolve(directory, withoutFragmentOrQuery(value)));
        }
        Matcher address = SITE_ADDRESS.matcher(page);
        while (address.find()) {
            references.add(withoutFragmentOrQuery(address.group(1)));
        }
        return references;
    }

    /** The anchors into {@code index.html} a page at the root of {@code web/} carries. */
    static Set<String> anchors(String page) {
        return anchors(page, "").getOrDefault("index.html", Set.of());
    }

    /**
     * The anchors a page carries, without the {@code #}, keyed by the page each one points into
     * as a path relative to {@code web/}: an in-page {@code href} points into the page itself, a
     * relative link into the page it resolves to from the page's own {@code directory}, and a site
     * address into the page it names. Only pages are keyed — a {@code #page=2} after a PDF is an
     * instruction to the PDF viewer, not an anchor.
     */
    static Map<String, Set<String>> anchors(String page, String directory) {
        Map<String, Set<String>> anchors = new TreeMap<>();
        Matcher attribute = LINK_ATTRIBUTE.matcher(page);
        while (attribute.find()) {
            String value = attributeValue(attribute);
            int hash = value.indexOf('#');
            if (hash < 0 || hash == value.length() - 1 || value.startsWith("//") || SCHEME.matcher(value).find()) {
                continue;
            }
            String target = hash == 0 ? directory : resolve(directory, value.substring(0, hash));
            addAnchor(anchors, target, value.substring(hash + 1));
        }
        Matcher address = SITE_ADDRESS.matcher(page);
        while (address.find()) {
            String path = address.group(1);
            int hash = path.indexOf('#');
            if (hash >= 0 && hash < path.length() - 1) {
                addAnchor(anchors, path.substring(0, hash), path.substring(hash + 1));
            }
        }
        return anchors;
    }

    private static void addAnchor(Map<String, Set<String>> anchors, String target, String anchor) {
        String page = pageFile(target);
        if (page.endsWith(".html")) {
            anchors.computeIfAbsent(page, key -> new TreeSet<>()).add(anchor);
        }
    }

    /**
     * A relative reference written on a page in {@code directory}, as a path relative to
     * {@code web/}. Resolved the way a browser resolves it, so a trailing slash — which is what
     * makes {@code ../cv-b/} a page — survives.
     */
    static String resolve(String directory, String reference) {
        if (reference.startsWith("/")) {
            // Root-relative: the site lives under /GraphCompose/, so this names nothing it publishes,
            // and it is kept as written for isPublished to refuse.
            return reference;
        }
        URI relative;
        try {
            relative = URI.create(reference);
        } catch (IllegalArgumentException notEncoded) {
            try {
                // A raw space or brace is not a URI, but a browser encodes it and follows the link,
                // so it is read the same way here rather than failing the whole guard on its syntax.
                relative = new URI(null, null, reference, null);
            } catch (URISyntaxException unreadable) {
                return reference;
            }
        }
        URI base = URI.create("https://site.invalid/" + directory);
        return base.resolve(relative).getPath().substring(1);
    }

    /** The file a site path names: a directory, the root included, means its index page. */
    private static String pageFile(String path) {
        return path.isEmpty() || path.endsWith("/") ? path + "index.html" : path;
    }

    /** The directory a page sits in, relative to {@code web/}: empty for the root, else ending in a slash. */
    private static String directoryOf(String page) {
        int slash = page.lastIndexOf('/');
        return slash < 0 ? "" : page.substring(0, slash + 1);
    }

    /**
     * The document pages scripts/site/build.mjs generates, found rather than listed: every
     * {@code index.html} under {@code web/} but the home page and anything under the catalogue's
     * own files.
     */
    private static List<String> generatedPages() throws IOException {
        try (Stream<Path> files = Files.walk(WEB)) {
            return files
                    .filter(file -> file.getFileName().toString().equals("index.html"))
                    .filter(file -> !file.getParent().equals(WEB) && !file.startsWith(SHOWCASE))
                    .filter(Files::isRegularFile)
                    .map(file -> WEB.relativize(file).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        }
    }

    /** Every card in the manifest: the members of each group's {@code examples}. */
    static List<Map<String, Object>> cards(Object manifest) {
        List<Map<String, Object>> cards = new ArrayList<>();
        for (Object category : array(object(manifest, "the manifest").get("categories"), "categories")) {
            for (Object group : array(object(category, "a category").get("groups"), "groups")) {
                for (Object card : array(object(group, "a group").get("examples"), "examples")) {
                    cards.add(object(card, "a card"));
                }
            }
        }
        return cards;
    }

    /** The ids of the manifest's categories. */
    static Set<String> categoryIds(Object manifest) {
        Set<String> ids = new TreeSet<>();
        for (Object category : array(object(manifest, "the manifest").get("categories"), "categories")) {
            ids.add(String.valueOf(object(category, "a category").get("id")));
        }
        return ids;
    }

    /** Each category's families, each with the ids of its cards. */
    static Map<String, Map<String, Set<String>>> families(Object manifest) {
        Map<String, Map<String, Set<String>>> families = new LinkedHashMap<>();
        for (Object category : array(object(manifest, "the manifest").get("categories"), "categories")) {
            Map<String, Object> categoryObject = object(category, "a category");
            Map<String, Set<String>> groups = families.computeIfAbsent(
                    String.valueOf(categoryObject.get("id")), key -> new LinkedHashMap<>());
            for (Object group : array(categoryObject.get("groups"), "groups")) {
                Map<String, Object> groupObject = object(group, "a group");
                Set<String> ids = groups.computeIfAbsent(
                        String.valueOf(groupObject.get("id")), key -> new LinkedHashSet<>());
                for (Object card : array(groupObject.get("examples"), "examples")) {
                    ids.add(String.valueOf(object(card, "a card").get("id")));
                }
            }
        }
        return families;
    }

    /**
     * Whether a viewer address, without its {@code #}, names a family the manifest has
     * and, when it names a card, a card of that family. It is read the way
     * {@code gallery-viewer.js} reads it: two or three non-empty segments.
     */
    static boolean viewerAddressLands(String address, Map<String, Map<String, Set<String>>> families) {
        if (!address.startsWith("/")) {
            return false;
        }
        String[] segments = address.substring(1).split("/", -1);
        if (segments.length < 2 || segments.length > 3 || Arrays.stream(segments).anyMatch(String::isEmpty)) {
            return false;
        }
        Set<String> cards = families.getOrDefault(segments[0], Map.of()).get(segments[1]);
        return cards != null && !cards.isEmpty() && (segments.length == 2 || cards.contains(segments[2]));
    }

    /**
     * The code of the {@code doc-example} block named by {@code exampleId}, or {@code null}
     * when the page carries no such block.
     *
     * <p>A second copy of the reader in {@code ShowcaseSync}, because this guard lives in core
     * and that generator lives in the examples module: core cannot depend on examples, so the
     * two cannot share one. What the copy buys is that the comparison starts from the markdown
     * rather than from the generator's output — a manifest edited by hand, or left behind by a
     * page that has since moved on, fails here.</p>
     */
    private static String markedBlock(List<String> lines, String exampleId) {
        for (int i = 0; i < lines.size(); i++) {
            String marker = lines.get(i).trim();
            if (!marker.startsWith("<!--") || !marker.contains("doc-example:")) {
                continue;
            }
            if (!Arrays.asList(marker.split("\\s+")).contains("id=" + exampleId)) {
                continue;
            }
            if (i + 1 >= lines.size() || !lines.get(i + 1).trim().equals("```java")) {
                return null;
            }
            List<String> code = new ArrayList<>();
            for (int j = i + 2; j < lines.size(); j++) {
                if (lines.get(j).trim().equals("```")) {
                    return String.join("\n", code);
                }
                code.add(lines.get(j));
            }
            return null;
        }
        return null;
    }

    /**
     * Whether the page carries a heading the link's {@code #fragment} could be addressing.
     *
     * <p>Deliberately coarse: GitHub computes the real anchor, and deriving it here would be
     * guessing at rules this repository has been bitten by before. Comparing on letters and
     * digits alone is a necessary condition, not the anchor itself — it cannot say a link
     * works, but a page whose heading has been renamed away has nothing left that matches,
     * and that is the failure a reader meets as a jump to the top of the page.</p>
     */
    private static boolean hasHeadingFor(Path page, String reference) throws IOException {
        String fragment = normalizeAnchor(reference.substring(reference.indexOf('#') + 1));
        if (fragment.isEmpty()) {
            return true;
        }
        for (String line : Files.readAllLines(page)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#") && normalizeAnchor(trimmed.replaceAll("^#+", "")).equals(fragment)) {
                return true;
            }
        }
        return false;
    }

    /** Letters and digits only, lowercased — enough to notice a heading that has moved on. */
    private static String normalizeAnchor(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static Set<String> matches(Pattern pattern, String text) {
        Set<String> values = new TreeSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    private static String attributeValue(Matcher attribute) {
        return attribute.group(1) != null ? attribute.group(1) : attribute.group(2);
    }

    private static String withoutFragmentOrQuery(String reference) {
        int end = reference.length();
        for (char marker : new char[] {'#', '?'}) {
            int at = reference.indexOf(marker);
            if (at >= 0 && at < end) {
                end = at;
            }
        }
        return reference.substring(0, end);
    }

    /** Whether a site path names a file under {@code web/}; a directory path, the root included, means its index page. */
    private static boolean isPublished(String reference) {
        try {
            Path file = WEB.resolve(pageFile(reference)).normalize();
            return file.startsWith(WEB) && Files.isRegularFile(file);
        } catch (InvalidPathException notAPathHere) {
            // A character this file system cannot name (a '|' on Windows) names no published file.
            return false;
        }
    }

    /** Whether a card's path names a file under {@code web/showcase/}; an empty path never does. */
    private static boolean isShowcaseFile(String path) {
        Path file = WEB.resolve(path).normalize();
        return !path.isBlank() && file.startsWith(SHOWCASE) && Files.isRegularFile(file);
    }

    private static String read(String name) throws IOException {
        return Files.readString(WEB.resolve(name));
    }

    private static Object readManifest() throws IOException {
        return StrictJsonReader.read(read("examples.json"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String what) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalStateException(what + " in web/examples.json is not an object: " + value);
    }

    private static List<?> array(Object value, String key) {
        if (value instanceof List<?> list) {
            return list;
        }
        throw new IllegalStateException("'" + key + "' in web/examples.json is not an array: " + value);
    }
}
