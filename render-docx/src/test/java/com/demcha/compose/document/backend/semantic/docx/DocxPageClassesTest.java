package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.backend.semantic.docx.DocxPageClasses.PageClass;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.output.PageContext;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A zone's page predicate, sorted into the kinds of page Word gives a header of its own.
 *
 * @author Artem Demchyshyn
 */
class DocxPageClassesTest {

    @Test
    void aZoneWithNoPredicateIsOnEveryKindOfPage() {
        DocumentPageZone zone = DocumentPageZone.footer(20, page -> null);

        assertThat(DocxPageClasses.of(zone, 3)).isEqualTo(EnumSet.allOf(PageClass.class));
    }

    @Test
    void predicatesWordCanStateAreSortedIntoItsKinds() {
        assertThat(classes(PageContext::isFirst)).containsExactly(PageClass.FIRST);
        assertThat(classes(page -> !page.isFirst())).containsExactly(PageClass.EVEN, PageClass.LATER_ODD);
        assertThat(classes(page -> page.number() % 2 == 0)).containsExactly(PageClass.EVEN);
        assertThat(classes(page -> page.number() % 2 == 1)).containsExactly(PageClass.FIRST, PageClass.LATER_ODD);
        assertThat(classes(page -> false)).isEmpty();
    }

    @Test
    void predicatesThatPickPagesWithinAKindHaveNoWordEquivalent() {
        assertThat(classes(PageContext::isLast)).isNull();
        assertThat(classes(page -> page.number() == 3)).isNull();
        assertThat(classes(page -> page.number() <= 4)).isNull();
    }

    @Test
    void aPredicateThatRefusesASamplePageSaysNothingWordCanUse() {
        java.util.List<String> titles = java.util.List.of("Cover", "Body", "Back");
        Predicate<PageContext> byTitle = page -> !titles.get(page.number() - 1).isEmpty();

        assertThat(DocxPageClasses.of(zone(byTitle), 3))
                .as("asked about a sixth page of a three-page document, it throws, and the export does not")
                .isNull();
    }

    @Test
    void thePredicateIsAskedAboutAsManyPagesAsTheDocumentHas() {
        // True on the first twelve pages: every page of a twelve-page document, but not of the
        // longer sample a shorter document would be asked about.
        Predicate<PageContext> firstTwelve = page -> page.number() <= 12;

        assertThat(DocxPageClasses.of(zone(firstTwelve), 12)).isEqualTo(EnumSet.allOf(PageClass.class));
        assertThat(DocxPageClasses.of(zone(firstTwelve), 20)).isNull();
    }

    @Test
    void pageNumbersAreFiledAsWordFilesThem() {
        assertThat(DocxPageClasses.classOf(1)).isEqualTo(PageClass.FIRST);
        assertThat(DocxPageClasses.classOf(2)).isEqualTo(PageClass.EVEN);
        assertThat(DocxPageClasses.classOf(3)).isEqualTo(PageClass.LATER_ODD);
        assertThat(DocxPageClasses.classOf(4)).isEqualTo(PageClass.EVEN);
    }

    private static java.util.Set<PageClass> classes(Predicate<PageContext> appliesTo) {
        return DocxPageClasses.of(zone(appliesTo), 2);
    }

    private static DocumentPageZone zone(Predicate<PageContext> appliesTo) {
        return DocumentPageZone.footer(20, page -> null)
                .toBuilder()
                .appliesTo(appliesTo)
                .build();
    }
}
