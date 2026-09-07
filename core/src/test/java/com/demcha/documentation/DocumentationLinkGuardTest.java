package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every relative link in the published documentation goes somewhere.
 *
 * <p>A dead link is the one documentation defect that costs nothing to make and
 * cannot be found by reading: the page renders, the prose is right, and the click
 * lands on a 404 or the top of a file. Renaming a heading breaks every jump to it
 * from anywhere in the tree, and moving a file breaks every link into it, and
 * nothing in this build noticed either — no test read a link, no CI step followed
 * one.</p>
 *
 * <p>This reads them. Two kinds, because they rot for different reasons:</p>
 *
 * <ul>
 *   <li><b>File links</b> — {@code [text](../recipes/tables.md)}. Broken by moving
 *       or deleting the target.</li>
 *   <li><b>Anchors</b> — {@code [text](#section)} or {@code [text](page.md#section)}.
 *       Broken by <em>editing a heading</em>, which is why they rot the fastest and
 *       the most quietly: the edit looks harmless and the link still looks like a
 *       link.</li>
 * </ul>
 *
 * <p>External {@code http(s)} and {@code mailto:} targets are left alone. They fail
 * for reasons this repository does not control, and a guard that goes red because
 * somebody else's server is down is a guard people learn to ignore.</p>
 */
class DocumentationLinkGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();

    /**
     * A markdown inline link's target: the {@code (...)} half of {@code [text](target)}.
     *
     * <p>Stops at whitespace so a titled link — {@code [text](page.md "Title")} —
     * yields the path rather than the path plus the title.</p>
     */
    private static final Pattern LINK = Pattern.compile("\\[[^]]*]\\(([^)\\s]+)");

    /**
     * An ATX heading, whose text GitHub turns into the anchor.
     *
     * <p>The run after the hashes is spaces and tabs, not {@code \s}: {@code \s}
     * matches a newline, so a lone {@code #} on its own line swallowed the break
     * and claimed the following line's text as a heading. GitHub renders that
     * {@code #} as an empty heading and the next line as a paragraph.</p>
     */
    private static final Pattern HEADING = Pattern.compile("(?m)^#{1,6}[ \\t]+(.+?)[ \\t]*$");

    /** A hand-written anchor: {@code <a name="x">} or {@code <a id="x">}. */
    private static final Pattern EXPLICIT_ANCHOR =
            Pattern.compile("<a\\s+(?:name|id)=[\"']([^\"']+)[\"']");

    /** A fenced code block, in which nothing is a link and nothing is a heading. */
    private static final Pattern FENCED_BLOCK =
            Pattern.compile("(?ms)^\\s*```.*?^\\s*```\\s*$");

    /**
     * An inline code span.
     *
     * <p>Stripped before looking for links, so prose that <em>describes</em> the
     * syntax — {@code `[text](#heading)`-style links} — is not read as a link to a
     * section called "heading". Not stripped before reading a heading: GitHub keeps
     * the text inside backticks when it builds the anchor, so
     * {@code ## `MissingBackendException` when opening a session} anchors as
     * {@code missingbackendexception-when-opening-a-session}, and dropping the code
     * span would invent a different one.</p>
     */
    private static final Pattern INLINE_CODE = Pattern.compile("`[^`\\n]*`");

    /**
     * Everything GitHub drops from an anchor.
     *
     * <p>The keep-set was read off GitHub rather than reasoned about, by posting
     * headings to {@code api.github.com/markdown} and reading the ids it
     * generates: letters of any script, combining marks, decimal digits,
     * {@code _}, {@code -} and the ASCII space survive, and everything else is
     * dropped — <em>including every other kind of whitespace</em>. A
     * non-breaking space, an em space and a tab are all removed rather than
     * hyphenated, and {@code ①} is removed because it is not a decimal digit,
     * leaving the spaces that surrounded it.</p>
     */
    private static final Pattern NOT_IN_ANCHOR = Pattern.compile("[^\\p{L}\\p{M}\\p{Nd}_ -]");

    /** A markdown link inside a heading — the anchor uses the text, not the target. */
    private static final Pattern HEADING_LINK = Pattern.compile("\\[([^]]*)]\\([^)]*\\)");

    /** An HTML tag inside a heading; GitHub renders it rather than anchoring it. */
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    @Test
    void everyRelativeLinkResolves() throws IOException {
        List<Path> pages = documentationPages();
        assertThat(pages)
                .describedAs("no markdown found under %s — this guard is reading a tree that "
                        + "moved, so it is no longer guarding anything", PROJECT_ROOT)
                .isNotEmpty();

        Map<Path, Set<String>> anchors = new HashMap<>();
        for (Path page : pages) {
            anchors.put(page.toAbsolutePath().normalize(), anchorsOf(page));
        }

        Map<String, String> broken = new TreeMap<>();
        int checked = 0;
        for (Path page : pages) {
            String body = INLINE_CODE.matcher(withoutFencedBlocks(read(page))).replaceAll("");
            Matcher link = LINK.matcher(body);
            while (link.find()) {
                String target = link.group(1);
                if (target.startsWith("http://") || target.startsWith("https://")
                        || target.startsWith("mailto:") || target.isEmpty()) {
                    continue;
                }

                checked++;
                int hash = target.indexOf('#');
                String filePart = hash < 0 ? target : target.substring(0, hash);
                String anchor = hash < 0 ? "" : target.substring(hash + 1);

                Path resolved = filePart.isEmpty()
                        ? page.toAbsolutePath().normalize()
                        : page.getParent().resolve(decode(filePart)).toAbsolutePath().normalize();

                if (!filePart.isEmpty() && !Files.exists(resolved)) {
                    broken.put(relative(page) + " -> " + target, "no such file");
                    continue;
                }
                // Only markdown has anchors this can compute; a link into a PDF or a
                // source file with a fragment is not something to judge here.
                if (!anchor.isEmpty() && anchors.containsKey(resolved)
                        && !anchors.get(resolved).contains(decode(anchor))) {
                    broken.put(relative(page) + " -> " + target,
                            "no heading in " + relative(resolved) + " anchors there");
                }
            }
        }

        assertThat(broken)
                .describedAs("a relative link that goes nowhere. Renaming a heading breaks every "
                        + "jump to it and moving a file breaks every link into it — both look "
                        + "harmless in the diff, and neither is visible without following the "
                        + "link. Fix the link or restore what it pointed at")
                .isEmpty();

        // "No broken links" and "no links read" are the same shade of green. A regex
        // that stops matching — a link syntax nobody anticipated, a scan root that
        // moved — would leave this passing over nothing at all, which is the state
        // the documentation was already in before this guard existed.
        assertThat(checked)
                .describedAs("only %d relative links were examined across %d pages; the scan is "
                        + "reading far less than this repository links, so a green result here "
                        + "means the matching broke rather than the links being sound",
                        checked, pages.size())
                .isGreaterThan(300);
    }

    /**
     * A heading reference written in Java: {@code docs/recipes/x.md#some-heading}
     * inside a Javadoc block.
     *
     * <p>The leading boundary keeps this off absolute URLs. Several sources
     * already link to {@code https://github.com/…/blob/main/docs/…md}; without
     * the lookbehind the {@code docs/…} tail of such a URL matches, and a
     * branch-pinned external link would be validated against the current
     * worktree — which {@link #everyRelativeLinkResolves()} deliberately does not
     * do for {@code https://} targets. The same guard is spelled as a lookbehind
     * in {@code knowledge/tools/routing/lib/anchors.mjs}, for the same reason.</p>
     */
    private static final Pattern JAVA_DOCS_ANCHOR =
            Pattern.compile("(?<![\\w./:-])(docs/[\\w./-]+\\.md)#([\\p{L}\\p{M}\\p{Nd}_-]+)");

    /**
     * Javadoc that points a reader at a section of the documentation points at a
     * section that exists.
     *
     * <p>The markdown half of this guard reads {@code [text](target)} links, which
     * is every reference a page can make. A Javadoc block cannot write one of
     * those — it names the path as prose — so a heading rename fixes every
     * markdown citation and the routing table's copies, and silently leaves the
     * Javadoc one dangling. That copy has the widest audience of the three: the
     * knowledge surfaces carry signatures and no prose, so for an API-first
     * reader the Javadoc <em>is</em> the documentation, and it ships to
     * javadoc.io and to every IDE tooltip.</p>
     *
     * @throws IOException when a source file or page cannot be read
     */
    @Test
    void everyDocsAnchorCitedFromJavaResolves() throws IOException {
        List<Path> sources = mainJavaSources();
        // Fail-closed on the scan root rather than on the number of citations:
        // there may legitimately be none one day, but "the tree moved and this
        // read nothing" must not look like "every citation resolves".
        assertThat(sources)
                .describedAs("no main Java sources found under %s — this guard is reading a tree "
                        + "that moved, so it is no longer guarding anything", PROJECT_ROOT)
                .hasSizeGreaterThan(100);

        Map<Path, Set<String>> anchors = new HashMap<>();
        for (Path page : documentationPages()) {
            anchors.put(page.toAbsolutePath().normalize(), anchorsOf(page));
        }

        Map<String, String> broken = new TreeMap<>();
        for (Path source : sources) {
            Matcher cited = JAVA_DOCS_ANCHOR.matcher(read(source));
            while (cited.find()) {
                String reference = cited.group(1) + "#" + cited.group(2);
                Path page = PROJECT_ROOT.resolve(cited.group(1)).toAbsolutePath().normalize();
                if (!Files.exists(page)) {
                    broken.put(relative(source) + " -> " + reference, "no such page");
                } else if (!anchors.getOrDefault(page, Set.of()).contains(cited.group(2))) {
                    broken.put(relative(source) + " -> " + reference,
                            "no heading in " + relative(page) + " anchors there");
                }
            }
        }

        assertThat(broken)
                .describedAs("Javadoc naming a documentation section that is not there. Renaming a "
                        + "heading is invisible to the compiler and to every markdown guard, and "
                        + "this citation ships to javadoc.io. Fix the citation or restore the "
                        + "heading")
                .isEmpty();
    }

    /** Every module's production Java, which is what gets published as Javadoc. */
    private static List<Path> mainJavaSources() throws IOException {
        List<Path> sources = new ArrayList<>();
        try (Stream<Path> tree = Files.walk(PROJECT_ROOT)) {
            tree.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toAbsolutePath().normalize().toString()
                            .replace('\\', '/').contains("/src/main/java/"))
                    .forEach(sources::add);
        }
        return sources;
    }

    /**
     * The anchors GitHub generates for a page.
     *
     * <p>The rule: take the heading text, drop HTML tags, keep a link's text rather
     * than its target, lowercase, remove everything that is not a letter, a
     * combining mark, a decimal digit, {@code _}, {@code -} or an ASCII space,
     * then replace each remaining ASCII space with a hyphen. Leading and
     * trailing hyphens survive — {@code ## 🚀 Start here} anchors as
     * {@code -start-here}, not {@code start-here} — and a repeated heading gets
     * {@code -1}, {@code -2} appended.</p>
     *
     * @param page the markdown file
     * @return every anchor a link in this repository could legitimately target
     * @throws IOException when the page cannot be read
     */
    private static Set<String> anchorsOf(Path page) throws IOException {
        String body = withoutFencedBlocks(read(page));
        Set<String> anchors = new LinkedHashSet<>();
        Map<String, Integer> seen = new HashMap<>();

        Matcher heading = HEADING.matcher(body);
        while (heading.find()) {
            String slug = slug(heading.group(1));
            int occurrence = seen.merge(slug, 1, Integer::sum) - 1;
            anchors.add(occurrence == 0 ? slug : slug + "-" + occurrence);
        }

        Matcher explicit = EXPLICIT_ANCHOR.matcher(body);
        while (explicit.find()) {
            anchors.add(explicit.group(1));
        }
        return anchors;
    }

    /** GitHub's heading-to-anchor rule. */
    private static String slug(String heading) {
        String text = HTML_TAG.matcher(heading.trim()).replaceAll("");
        text = HEADING_LINK.matcher(text).replaceAll("$1");
        text = text.replace("`", "").toLowerCase(java.util.Locale.ROOT);
        text = NOT_IN_ANCHOR.matcher(text).replaceAll("");
        // The ASCII space only. Every other kind of whitespace was removed above,
        // which is what GitHub does — hyphenating them put characters into the
        // anchor that the rendered page does not have.
        return text.replace(' ', '-');
    }

    /**
     * The same four inputs {@code knowledge/tools/routing/test/anchors.test.mjs}
     * pins on the JavaScript side.
     *
     * <p>GitHub's rule is implemented twice in this repository — here, and in
     * {@code knowledge/tools/routing/lib/anchors.mjs}, which the routing gate
     * resolves route citations with. Neither can call the other across the
     * language boundary, so they are held together by asserting the same table
     * from both sides: change one and its own fixture goes red. These four are
     * the inputs on which the two implementations were measured to disagree.</p>
     */
    @Test
    void theAnchorRuleAgreesWithItsJavaScriptTwin() {
        assertThat(slug("Zebra alternating rows"))
                .describedAs("a non-breaking space is dropped, not hyphenated")
                .isEqualTo("zebraalternating-rows");
        assertThat(slug("Row span"))
                .describedAs("an em space is dropped too")
                .isEqualTo("rowspan");
        assertThat(slug("A\tB"))
                .describedAs("and a tab — only the ASCII space becomes a hyphen")
                .isEqualTo("ab");
        assertThat(slug("Item ① first"))
                .describedAs("a circled numeral is not a decimal digit, so it goes and its spaces stay")
                .isEqualTo("item--first");
        assertThat(slug("Привет мир"))
                .describedAs("letters of any script survive")
                .isEqualTo("привет-мир");
        assertThat(slug("v1.8.0 fonts"))
                .describedAs("so do digits, while the dots between them go")
                .isEqualTo("v180-fonts");
        assertThat(slug("snake_case name"))
                .describedAs("an underscore survives")
                .isEqualTo("snake_case-name");
        assertThat(slug("A & B"))
                .describedAs("a dropped character leaves its spaces behind")
                .isEqualTo("a--b");
        assertThat(slug("Zebra — alternating row fills"))
                .describedAs("and the case that started all this: a spaced em dash is two spaces, "
                        + "so it is two hyphens")
                .isEqualTo("zebra--alternating-row-fills");
        assertThat(slug("Café rules"))
                .describedAs("a decomposed accent is a combining mark and stays with its letter (GitHub keeps it)")
                .isEqualTo("café-rules");
    }

    private static String withoutFencedBlocks(String markdown) {
        return FENCED_BLOCK.matcher(markdown).replaceAll("");
    }

    /** {@code %20} and friends, so an encoded path is compared as the file is named. */
    private static String decode(String target) {
        return java.net.URLDecoder.decode(target, StandardCharsets.UTF_8);
    }

    /**
     * The markdown this repository publishes: the documentation tree, the root
     * pages, and every module README. {@code docs/private} is working material,
     * ignored by git and not published, so it is not held to this.
     */
    private static List<Path> documentationPages() throws IOException {
        List<Path> pages = new ArrayList<>();
        Path docs = PROJECT_ROOT.resolve("docs");
        if (Files.isDirectory(docs)) {
            try (Stream<Path> tree = Files.walk(docs)) {
                tree.filter(p -> p.toString().endsWith(".md"))
                        .filter(p -> !p.toAbsolutePath().normalize().startsWith(
                                docs.resolve("private").toAbsolutePath().normalize()))
                        .forEach(pages::add);
            }
        }
        for (String root : new String[]{"README.md", "CONTRIBUTING.md", "SUPPORT.md", "SECURITY.md"}) {
            Path page = PROJECT_ROOT.resolve(root);
            if (Files.exists(page)) {
                pages.add(page);
            }
        }
        try (Stream<Path> modules = Files.list(PROJECT_ROOT)) {
            modules.filter(Files::isDirectory)
                    .map(module -> module.resolve("README.md"))
                    .filter(Files::exists)
                    .forEach(pages::add);
        }
        return pages;
    }

    private static String read(Path page) throws IOException {
        return Files.readString(page, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static String relative(Path path) {
        return PROJECT_ROOT.relativize(path.toAbsolutePath().normalize())
                .toString().replace('\\', '/');
    }
}
