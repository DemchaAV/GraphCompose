package com.demcha.compose.document.output;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A semantic export's context answers for the kind of page a zone is built for and still
 * refuses the numbers, which the consuming format sets.
 *
 * @author Artem Demchyshyn
 */
class PageContextTest {

    @Test
    void aSemanticContextAnswersForTheKindOfPageItIsBuiltFor() {
        assertKind(PageContext.unpaginated(true, true), true, true);
        assertKind(PageContext.unpaginated(true, false), true, false);
        assertKind(PageContext.unpaginated(false, true), false, true);
        assertKind(PageContext.unpaginated(false, false), false, false);
    }

    @Test
    void theContextForNoPageInParticularIsTheFirstAndTheLast() {
        assertKind(PageContext.unpaginated(), true, true);
    }

    @Test
    void aSemanticContextOfAnyKindRefusesTheNumbers() {
        PageContext later = PageContext.unpaginated(false, false);

        assertThat(later.isPaginated()).isFalse();
        assertThatThrownBy(later::number).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(later::total).isInstanceOf(UnsupportedOperationException.class);
    }

    private static void assertKind(PageContext page, boolean first, boolean last) {
        assertThat(page.isFirst()).as("isFirst").isEqualTo(first);
        assertThat(page.isLast()).as("isLast").isEqualTo(last);
        assertThat(page.isPaginated()).isFalse();
    }
}
