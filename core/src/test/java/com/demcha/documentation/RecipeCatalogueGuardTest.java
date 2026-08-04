package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every recipe page is reachable from the catalogue that indexes them.
 *
 * <p>{@code docs/recipes.md} is the one page the README and the documentation
 * index point at, so a recipe missing from it is a recipe with no inbound link:
 * it renders, it is correct, and nobody arrives at it. Adding a page under
 * {@code docs/recipes/} disturbs nothing when the catalogue is left alone —
 * every gate stays green and the omission surfaces only when someone goes
 * looking for a topic they were sure had been written up.</p>
 *
 * <p>The check runs the other way too. A catalogue row pointing at a file that
 * was renamed or removed is a dead link on the most-linked documentation page,
 * and nothing else in the build reads these paths.</p>
 */
class RecipeCatalogueGuardTest {

    private static final Path RECIPES_DIR = RepoRoot.get().resolve("docs/recipes");
    private static final Path CATALOGUE = RepoRoot.get().resolve("docs/recipes.md");

    /**
     * {@code README.md} is what GitHub renders when the directory is opened. It
     * points at the catalogue rather than being listed in it.
     */
    private static final String FOLDER_INDEX = "README.md";

    /**
     * A catalogue link, as written: {@code (recipes/<name>.md)}, optionally with an
     * anchor.
     *
     * <p>The file name is anything a path segment can hold, not the lower-case-and-
     * hyphens the current pages happen to use. Baking today's spelling in would make
     * a correctly linked {@code pdf_export.md} report as unlisted — a false alarm
     * rather than a missed page, but a guard that cries wolf over a legal file name
     * is one the next person edits until it stops complaining.</p>
     */
    private static final Pattern LINK = Pattern.compile("\\(recipes/([^/#)\\s]+\\.md)(?:#[^)]*)?\\)");

    @Test
    void everyRecipePageIsListedInTheCatalogue() throws IOException {
        Set<String> onDisk = recipeFiles();
        Set<String> linked = cataloguedFiles();

        TreeSet<String> unlisted = new TreeSet<>(onDisk);
        unlisted.removeAll(linked);

        assertThat(unlisted)
                .describedAs("recipe pages under docs/recipes/ that docs/recipes.md never links — "
                        + "an unlinked page is one readers cannot arrive at")
                .isEmpty();
    }

    @Test
    void theCatalogueLinksNoRecipeThatIsGone() throws IOException {
        Set<String> onDisk = recipeFiles();
        Set<String> linked = cataloguedFiles();

        TreeSet<String> dangling = new TreeSet<>(linked);
        dangling.removeAll(onDisk);

        assertThat(dangling)
                .describedAs("docs/recipes.md links files that no longer exist under docs/recipes/")
                .isEmpty();
    }

    private static Set<String> recipeFiles() throws IOException {
        try (Stream<Path> files = Files.list(RECIPES_DIR)) {
            Set<String> names = files
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith(".md"))
                    .filter(name -> !FOLDER_INDEX.equals(name))
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));

            assertThat(names)
                    .describedAs("no recipe pages found under %s — the guard is reading a folder "
                            + "that moved and would cover nothing", RECIPES_DIR)
                    .isNotEmpty();
            return names;
        }
    }

    private static Set<String> cataloguedFiles() throws IOException {
        assertThat(CATALOGUE)
                .describedAs("the recipe catalogue moved; this guard no longer reads the page it protects")
                .exists();

        List<String> lines = Files.readAllLines(CATALOGUE, StandardCharsets.UTF_8);
        TreeSet<String> linked = new TreeSet<>();
        for (String line : lines) {
            Matcher link = LINK.matcher(line);
            while (link.find()) {
                linked.add(link.group(1));
            }
        }
        return linked;
    }
}
