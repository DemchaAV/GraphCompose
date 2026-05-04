package com.demcha.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.document.templates.theme.WeeklyScheduleTheme;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Architecture guard for {@code com.demcha.compose.document.templates.theme.*}.
 *
 * <p>Public methods on canonical theme classes should return canonical
 * {@code com.demcha.compose.document.style.*} types (or AWT/Java built-ins),
 * not engine value types. Returning an engine type from a public theme
 * method makes that engine type a de-facto public API surface and blocks
 * the planned v2.0 migration of {@code com.demcha.compose.engine.*} content
 * out of the package tree.</p>
 *
 * <p>v1.6 ships with two known leaks: {@link CvTheme} returns
 * {@code engine.components.content.text.TextStyle} from five style
 * accessors and {@code engine.components.style.Margin} from one. The
 * {@link WeeklyScheduleTheme} returns {@code TextStyle} from nine
 * accessors. These leaks are explicitly allow-listed below; the goal of
 * this guard is to <strong>prevent new theme classes</strong> from
 * adding similar leaks while the existing two are migrated separately
 * (planned for v1.7 / v2.0; tracked under audit finding C2).</p>
 *
 * <p>Removing an entry from the allow-list and shipping the
 * corresponding migration must happen together; otherwise this guard
 * fails the build.</p>
 */
class CanonicalThemeReturnTypeTest {

    private static final List<Class<?>> THEME_CLASSES = List.of(
            CvTheme.class,
            WeeklyScheduleTheme.class);

    /**
     * Documented engine-typed return values that exist in v1.6 and are
     * scheduled for migration in a later release. The pair is
     * (declaring class simple name, method name). Any signature change
     * on a listed method should remove the entry once migrated.
     */
    private static final Set<String> KNOWN_ENGINE_RETURN_LEAKS = Set.of(
            "CvTheme#nameTextStyle",
            "CvTheme#sectionHeaderTextStyle",
            "CvTheme#bodyTextStyle",
            "CvTheme#smallBodyTextStyle",
            "CvTheme#linkTextStyle",
            "CvTheme#moduleMargin",
            "CvTheme#modulMargin",
            "WeeklyScheduleTheme#titleStyle",
            "WeeklyScheduleTheme#weekLabelStyle",
            "WeeklyScheduleTheme#dayLabelStyle",
            "WeeklyScheduleTheme#noteStyle",
            "WeeklyScheduleTheme#metricStyle",
            "WeeklyScheduleTheme#personNameStyle",
            "WeeklyScheduleTheme#cellTextStyle",
            "WeeklyScheduleTheme#categoryLabelStyle",
            "WeeklyScheduleTheme#footerStyle");

    @Test
    void publicThemeMethodsMustNotReturnEngineTypesUnlessAllowListed() {
        List<String> unexpectedLeaks = new ArrayList<>();

        for (Class<?> themeClass : THEME_CLASSES) {
            for (Method method : themeClass.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                if (method.isSynthetic() || method.isBridge()) {
                    continue;
                }
                Class<?> returnType = method.getReturnType();
                if (!returnType.getName().startsWith("com.demcha.compose.engine.")) {
                    continue;
                }
                String key = themeClass.getSimpleName() + "#" + method.getName();
                if (KNOWN_ENGINE_RETURN_LEAKS.contains(key)) {
                    continue;
                }
                unexpectedLeaks.add(
                        key
                                + " returns engine type "
                                + returnType.getName()
                                + " — either migrate to a com.demcha.compose.document.style.* "
                                + "type or, if intentional during the v1.6 → v2.0 migration, "
                                + "add the entry to KNOWN_ENGINE_RETURN_LEAKS.");
            }
        }

        assertThat(unexpectedLeaks)
                .describedAs(
                        "New theme methods must not introduce engine-typed return values")
                .isEmpty();
    }

    @Test
    void allowListEntriesMustStillExistOnTheirThemeClass() {
        // If a method in the allow-list is removed or renamed, the entry
        // becomes stale: it would fail to enforce a guard on the new API
        // and would silently mask a regression on a different method
        // sharing the old name. Detect that proactively.
        List<String> staleEntries = new ArrayList<>();

        for (String key : KNOWN_ENGINE_RETURN_LEAKS) {
            int hash = key.indexOf('#');
            String simpleClassName = key.substring(0, hash);
            String methodName = key.substring(hash + 1);
            Class<?> themeClass = THEME_CLASSES.stream()
                    .filter(c -> c.getSimpleName().equals(simpleClassName))
                    .findFirst()
                    .orElse(null);
            if (themeClass == null) {
                staleEntries.add(key + " — class not in THEME_CLASSES list");
                continue;
            }
            boolean found = false;
            for (Method method : themeClass.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && method.getName().equals(methodName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                staleEntries.add(key + " — method no longer exists or is no longer public");
            }
        }

        assertThat(staleEntries)
                .describedAs(
                        "Stale KNOWN_ENGINE_RETURN_LEAKS entries should be removed when the leak is fixed")
                .isEmpty();
    }
}
