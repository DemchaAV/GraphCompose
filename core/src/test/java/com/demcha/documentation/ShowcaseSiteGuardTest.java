package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
            for (String key : List.of("pdf", "screenshot", "pptx")) {
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
        Set<String> categories = categoryIds(readManifest());
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
                boolean lands = anchor.endsWith(SECTION_SUFFIX)
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
                        + "<category id>%s only for a category web/examples.json has, and any other anchor "
                        + "needs an element with that id in index.html", SECTION_SUFFIX)
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
                + "<loc>" + SITE_URL + "#features-section</loc><loc>" + SITE_URL + "index.html#showcase</loc>"
                + "<loc>" + SITE_URL + "showcase/b.pdf#page=2</loc>";

        assertThat(anchors(page)).containsExactlyInAnyOrder("install", "top", "features-section", "showcase");
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
