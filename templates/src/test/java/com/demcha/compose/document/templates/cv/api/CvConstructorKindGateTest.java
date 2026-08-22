package com.demcha.compose.document.templates.cv.api;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.cv.data.CvKind;
import com.demcha.compose.document.templates.cv.data.ModuleSection;
import com.demcha.compose.document.templates.cv.presets.CvTemplates;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The constructor contract is the kinds: adding a {@link CvKind} adds a
 * method, and every modular template has to declare it. A default on the
 * interface would let a template absorb a new shape without noticing.
 */
class CvConstructorKindGateTest {

    @ParameterizedTest
    @EnumSource(CvKind.class)
    void everyKindHasANonDefaultConstructorMethod(CvKind kind) throws NoSuchMethodException {
        Method method = CvConstructor.class.getMethod(
                CvConstructor.methodName(kind),
                SectionBuilder.class, ModuleSection.class, BrandTheme.class);
        assertThat(method.isDefault())
                .as("%s must not have a default — a new kind has to break every template",
                        method.getName())
                .isFalse();
        assertThat(Modifier.isAbstract(method.getModifiers()))
                .as("%s is the template's to implement", method.getName())
                .isTrue();
    }

    @Test
    void theInterfaceHasNoSpareKindMethods() {
        Set<String> expected = Arrays.stream(CvKind.values())
                .map(CvConstructor::methodName)
                .collect(Collectors.toSet());
        Set<String> declared = Arrays.stream(CvConstructor.class.getDeclaredMethods())
                .filter(method -> !method.isDefault() && !method.isSynthetic())
                .filter(method -> method.getParameterCount() == 3)
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertThat(declared).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void everyModularTemplateDeclaresEveryKind() throws NoSuchMethodException {
        assertThat(CvTemplates.modular()).isNotEmpty();
        for (ModularCvTemplate template : CvTemplates.modular()) {
            Class<?> type = template.getClass();
            for (CvKind kind : CvKind.values()) {
                Method method = type.getMethod(
                        CvConstructor.methodName(kind),
                        SectionBuilder.class, ModuleSection.class, BrandTheme.class);
                assertThat(method.getDeclaringClass())
                        .as("%s must implement %s rather than inherit a default",
                                template.id(), kind)
                        .isNotEqualTo(CvConstructor.class)
                        .isNotEqualTo(ModularCvTemplate.class);
            }
        }
    }
}
