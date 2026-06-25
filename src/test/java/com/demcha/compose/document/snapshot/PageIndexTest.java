package com.demcha.compose.document.snapshot;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract for {@link DocumentSession#pageIndex()}: every declared
 * {@code anchor(...)} resolves to its final page in one pass, a duplicate keeps
 * its last registration, a split node resolves to its start page, unknown/null
 * anchors return empty, and the result is cached per layout revision.
 */
class PageIndexTest {

    private DocumentSession smallPage() {
        return GraphCompose.document()
                .pageSize(220, 140)
                .margin(DocumentInsets.of(18))
                .create();
    }

    private DocumentSession multiPage() {
        DocumentSession session = smallPage();
        session.pageFlow(page -> {
            page.addSection(s -> s.anchor("intro").addParagraph("Introduction"));
            page.addPageBreak(b -> b.name("toBody"));
            page.addSection(s -> s.anchor("body").addParagraph("Body"));
        });
        return session;
    }

    @Test
    void resolvesEachAnchorToItsFinalPage() {
        try (DocumentSession session = multiPage()) {
            PageIndex index = session.pageIndex();

            assertThat(index.pageNumberOf("intro")).hasValue(1);
            assertThat(index.pageNumberOf("body")).hasValue(2);
            assertThat(index.pageOf("body")).hasValue(1);          // zero-based
            assertThat(index.forAnchor("intro")).hasValue(new PageReference("intro", 0));
            assertThat(index.all().keySet()).containsExactly("intro", "body");
            assertThat(index.totalPages()).isEqualTo(session.layoutGraph().totalPages());
        }
    }

    @Test
    void unknownOrNullAnchorResolvesEmpty() {
        try (DocumentSession session = multiPage()) {
            PageIndex index = session.pageIndex();

            assertThat(index.forAnchor("missing")).isEmpty();
            assertThat(index.pageOf("missing")).isEmpty();
            assertThat(index.pageNumberOf("missing")).isEmpty();
            // null must be lenient (empty), not an NPE — on a populated index...
            assertThat(index.forAnchor(null)).isEmpty();
            assertThat(index.pageNumberOf(null)).isEmpty();
        }
    }

    @Test
    void documentWithNoAnchorsYieldsEmptyIndexWithoutThrowing() {
        try (DocumentSession session = smallPage()) {
            session.pageFlow(page -> page.addParagraph("No anchors here"));
            PageIndex index = session.pageIndex();

            assertThat(index.all()).isEmpty();
            assertThat(index.totalPages()).isEqualTo(1);
            assertThat(index.forAnchor("x")).isEmpty();
            // ...and on an empty index, null is still lenient (the Map.of()-NPE regression).
            assertThat(index.pageOf(null)).isEmpty();
        }
    }

    @Test
    void duplicateAnchorResolvesToLastRegistration() {
        try (DocumentSession session = smallPage()) {
            session.pageFlow(page -> {
                page.addSection(s -> s.anchor("dup").addParagraph("First"));
                page.addPageBreak(b -> b.name("brk"));
                page.addSection(s -> s.anchor("dup").addParagraph("Second"));
            });
            PageIndex index = session.pageIndex();

            // Last registration wins — the same destination linkTo("dup") jumps to.
            assertThat(index.pageNumberOf("dup")).hasValue(2);
            assertThat(index.all()).hasSize(1);
        }
    }

    @Test
    void anchorOnSectionSplitAcrossPagesResolvesToStartPage() {
        try (DocumentSession session = smallPage()) {
            session.pageFlow(page -> page.addSection(s -> {
                s.anchor("report");
                for (int i = 0; i < 16; i++) {
                    s.addParagraph("Line " + i);
                }
            }));
            PageIndex index = session.pageIndex();

            assertThat(session.layoutGraph().totalPages()).isGreaterThan(1);   // the section actually split
            assertThat(index.pageNumberOf("report")).hasValue(1);              // resolves to its top, not its tail
        }
    }

    @Test
    void leafAnchorIsResolved() {
        try (DocumentSession session = smallPage()) {
            session.pageFlow(page -> page.addLine(l -> l.horizontal(60).anchor("rule")));
            PageIndex index = session.pageIndex();

            assertThat(index.pageNumberOf("rule")).hasValue(1);
        }
    }

    @Test
    void cachedPerRevisionAndRecomputesCorrectlyAfterMutation() {
        try (DocumentSession session = multiPage()) {
            PageIndex first = session.pageIndex();
            assertThat(session.pageIndex()).isSameAs(first);        // cached on the same revision
            assertThat(first.all()).containsOnlyKeys("intro", "body");

            session.add(new SpacerNode("Tail", 40, 40, DocumentInsets.zero(), DocumentInsets.zero()));
            PageIndex recomputed = session.pageIndex();
            assertThat(recomputed).isNotSameAs(first);              // recomputed after a mutation
            assertThat(recomputed.all()).containsOnlyKeys("intro", "body");   // and is correct, not stale/empty
        }
    }
}
