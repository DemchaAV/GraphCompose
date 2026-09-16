package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards the showcase site under {@code web/} against sending a visitor to something it
 * does not publish.
 *
 * <p>GitHub Pages serves {@code web/} exactly as committed, so a reference that resolves
 * to nothing reaches visitors as a dead link, a missing tile or a menu entry that moves
 * nothing. Each kind checked here had passed every other check: the featured strip skips
 * an id that is not a card in the manifest instead of reporting it, the menu and the
 * no-JavaScript index are written by hand, and a card's files are checked only while
 * {@code ShowcaseSync} writes them, not when a later change removes one.</p>
 *
 * <p>It reads the working tree, so the verify gate of a release cut runs it over the
 * catalogue the cut has just regenerated.</p>
 */
class ShowcaseSiteGuardTest {

    private static final Path WEB = RepoRoot.get().resolve("web");
    private static final Path SHOWCASE = WEB.resolve("showcase");

    /** The address {@code web/} is published at; a link that starts with it names a site file. */
    private static final String SITE_URL = "https://demchaav.github.io/GraphCompose/";

    /** The pages whose links are checked. {@code examples.js} builds its links from the manifest. */
    private static final List<String> PAGES = List.of("index.html", "sitemap.xml", "robots.txt");

    /** The pages whose anchors into {@code index.html} are checked. */
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
        }
        assertThat(missing)
                .describedAs("a card in web/examples.json names something that is not a file under web/showcase/, "
                        + "so its preview, PDF or deck is broken on the published site — regenerate with "
                        + "ShowcaseSync rather than editing the manifest")
                .isEmpty();
    }

    @Test
    void everySiteFileThePagesLinkToIsPublished() throws IOException {
        Map<String, Set<String>> broken = new LinkedHashMap<>();
        for (String page : PAGES) {
            Set<String> references = siteReferences(read(page));
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

        Map<String, Set<String>> broken = new LinkedHashMap<>();
        for (String page : ANCHOR_PAGES) {
            Set<String> anchors = anchors(read(page));
            assertThat(anchors)
                    .describedAs("found no anchor into index.html in web/%s — this guard is reading a page shape "
                            + "that moved, so it is no longer checking that page", page)
                    .isNotEmpty();

            Set<String> missing = new TreeSet<>();
            for (String anchor : anchors) {
                boolean lands = anchor.startsWith("/")
                        ? viewerAddressLands(anchor, families)
                        : anchor.endsWith(SECTION_SUFFIX)
                        ? categories.contains(anchor.substring(0, anchor.length() - SECTION_SUFFIX.length()))
                        : elementIds.contains(anchor);
                if (!lands) {
                    missing.add("#" + anchor);
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
                        + "anchor needs an element with that id in index.html", SECTION_SUFFIX)
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
                .isEqualTo(1);
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

    /**
     * The site files a page links to, as paths relative to {@code web/}: every relative
     * {@code href} and {@code src}, and every absolute address inside the site. A fragment
     * or query is dropped, so the site root is the empty path.
     */
    static Set<String> siteReferences(String page) {
        Set<String> references = new TreeSet<>();
        Matcher attribute = LINK_ATTRIBUTE.matcher(page);
        while (attribute.find()) {
            String value = attributeValue(attribute);
            if (value.startsWith("#") || value.startsWith("//") || SCHEME.matcher(value).find()) {
                continue;
            }
            references.add(withoutFragmentOrQuery(value));
        }
        Matcher address = SITE_ADDRESS.matcher(page);
        while (address.find()) {
            references.add(withoutFragmentOrQuery(address.group(1)));
        }
        return references;
    }

    /**
     * The anchors into {@code index.html} a page carries, without the {@code #}: an
     * in-page {@code href}, or a site address naming the root or {@code index.html}.
     */
    static Set<String> anchors(String page) {
        Set<String> anchors = new TreeSet<>();
        Matcher attribute = LINK_ATTRIBUTE.matcher(page);
        while (attribute.find()) {
            String value = attributeValue(attribute);
            if (value.startsWith("#") && value.length() > 1) {
                anchors.add(value.substring(1));
            }
        }
        Matcher address = SITE_ADDRESS.matcher(page);
        while (address.find()) {
            String path = address.group(1);
            for (String indexPage : List.of("#", "index.html#")) {
                if (path.startsWith(indexPage) && path.length() > indexPage.length()) {
                    anchors.add(path.substring(indexPage.length()));
                }
            }
        }
        return anchors;
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
        String path = reference.isEmpty() || reference.endsWith("/") ? reference + "index.html" : reference;
        Path file = WEB.resolve(path).normalize();
        return file.startsWith(WEB) && Files.isRegularFile(file);
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
