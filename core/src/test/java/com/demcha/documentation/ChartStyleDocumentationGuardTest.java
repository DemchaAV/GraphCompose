package com.demcha.documentation;

import com.demcha.compose.document.chart.ChartStyle;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every knob on {@link ChartStyle.Builder} is named in the chart recipe.
 *
 * <p>A styling setter nobody documents is a setter nobody finds. The recipe is
 * the only page that describes chart styling, and a new field added to the
 * builder does not disturb it — the page keeps rendering, every guard keeps
 * passing, and the option stays invisible until someone reads the source. Five
 * had accumulated that way: the three text styles, the donut centre style, and
 * the bar width ratio, all of them used by the flagship examples and named
 * nowhere a reader would look.</p>
 *
 * <p>The check is deliberately shallow — it asserts the setter's name occurs on
 * the page, not that the prose around it is any good. That is enough to make
 * the omission loud at the moment it is introduced, which is the only moment
 * it is cheap to fix.</p>
 */
class ChartStyleDocumentationGuardTest {

    private static final Path RECIPE = RepoRoot.get().resolve("docs/recipes/charts.md");

    @Test
    void everyChartStyleSetterIsNamedInTheChartRecipe() throws IOException {
        assertThat(RECIPE)
                .describedAs("the chart recipe moved; this guard is no longer reading the page it protects")
                .exists();

        String page = Files.readString(RECIPE, StandardCharsets.UTF_8);

        List<String> setters = builderSetterNames();
        assertThat(setters)
                .describedAs("no setters found on ChartStyle.Builder — the guard would cover nothing")
                .isNotEmpty();

        TreeSet<String> undocumented = new TreeSet<>();
        for (String setter : setters) {
            if (!page.contains(setter + "(")) {
                undocumented.add(setter);
            }
        }

        assertThat(undocumented)
                .describedAs("ChartStyle.Builder setters missing from docs/recipes/charts.md — a "
                        + "styling option that is not on the page is one readers cannot discover")
                .isEmpty();
    }

    /** Public builder methods that configure the style, i.e. everything but {@code build}. */
    private static List<String> builderSetterNames() {
        return Arrays.stream(ChartStyle.Builder.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> !m.isSynthetic())
                .map(Method::getName)
                .filter(name -> !"build".equals(name))
                .distinct()
                .sorted()
                .toList();
    }
}
