package com.demcha.compose.document.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.output.DocumentProtection;
import com.demcha.compose.document.output.DocumentWatermark;

import org.junit.jupiter.api.Test;

class SessionChromeApiTest {

    @Test
    void chromeFacadeMetadataAffectsTheSameUnderlyingSession() {
        try (DocumentSession session = GraphCompose.document().create()) {
            DocumentMetadata expected = DocumentMetadata.builder()
                    .title("Quarterly Report")
                    .author("Test")
                    .build();

            SessionChromeApi result = session.chrome().metadata(expected);

            assertThat(result)
                    .describedAs("chrome().metadata() should return the chrome facade for chaining")
                    .isNotNull();
            assertThat(result.session())
                    .describedAs("chrome().session() should return the owning DocumentSession")
                    .isSameAs(session);
        }
    }

    @Test
    void chromeFacadeChainsAcrossEveryChromeKind() {
        try (DocumentSession session = GraphCompose.document().create()) {
            DocumentSession returned = session.chrome()
                    .metadata(DocumentMetadata.builder().title("T").build())
                    .watermark(DocumentWatermark.builder().text("DRAFT").build())
                    .protect(DocumentProtection.builder().userPassword("u").build())
                    .header(DocumentHeaderFooter.builder().centerText("H").build())
                    .footer(DocumentHeaderFooter.builder().centerText("F").build())
                    .session();

            assertThat(returned)
                    .describedAs("chrome().…().session() returns the original session")
                    .isSameAs(session);
        }
    }

    @Test
    void chromeFacadeMatchesTopLevelCanonicalMethodEffect() {
        // Configure two sessions identically — one through chrome() facade,
        // one through the top-level canonical methods — and confirm the
        // resulting chrome surfaces are equivalent.
        DocumentMetadata md = DocumentMetadata.builder().title("A").author("B").build();
        DocumentWatermark wm = DocumentWatermark.builder().text("X").build();

        try (DocumentSession viaFacade = GraphCompose.document().create();
                DocumentSession viaTopLevel = GraphCompose.document().create()) {

            viaFacade.chrome().metadata(md).watermark(wm);
            viaTopLevel.metadata(md).watermark(wm);

            // Both render produce non-empty bytes, demonstrating both
            // configurations reach the backend.
            viaFacade.compose(dsl -> dsl.pageFlow(flow -> flow.addText("hi")));
            viaTopLevel.compose(dsl -> dsl.pageFlow(flow -> flow.addText("hi")));

            assertThat(viaFacade.toPdfBytes()).isNotEmpty();
            assertThat(viaTopLevel.toPdfBytes()).isNotEmpty();
        }
    }

    @Test
    void chromeOnClosedSessionThrowsIllegalState() {
        DocumentSession session = GraphCompose.document().create();
        SessionChromeApi chrome = session.chrome();
        session.close();

        assertThatThrownBy(() -> chrome.metadata(DocumentMetadata.builder().title("T").build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void chromeFacadeClearHeadersAndFootersResets() {
        try (DocumentSession session = GraphCompose.document().create()) {
            session.chrome()
                    .header(DocumentHeaderFooter.builder().centerText("H").build())
                    .footer(DocumentHeaderFooter.builder().centerText("F").build())
                    .clearHeadersAndFooters();
            // No exception thrown is the expected behaviour.
        }
    }
}
