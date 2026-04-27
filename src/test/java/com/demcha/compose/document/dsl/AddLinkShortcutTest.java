package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddLinkShortcutTest {

    @Test
    void addLinkRendersClickableUriAndVisibleText() throws Exception {
        byte[] pdfBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(12))
                .create()) {

            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addLink("Documentation", "https://example.com/docs")
                    .build();

            pdfBytes = session.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getPage(0).getAnnotations()).isNotEmpty();
            assertThat(document.getPage(0).getAnnotations().getFirst()).isInstanceOf(PDAnnotationLink.class);
            PDAnnotationLink link = (PDAnnotationLink) document.getPage(0).getAnnotations().getFirst();
            assertThat(link.getAction()).isInstanceOf(PDActionURI.class);
            assertThat(((PDActionURI) link.getAction()).getURI()).isEqualTo("https://example.com/docs");
            assertThat(new PDFTextStripper().getText(document)).contains("Documentation");
        }
    }

    @Test
    void addLinkAcceptsDocumentLinkOptions() throws Exception {
        byte[] pdfBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 200)
                .margin(DocumentInsets.of(12))
                .create()) {

            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addLink("Email me", new DocumentLinkOptions("mailto:author@example.dev"))
                    .build();

            pdfBytes = session.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDAnnotationLink link = (PDAnnotationLink) document.getPage(0).getAnnotations().getFirst();
            assertThat(((PDActionURI) link.getAction()).getURI()).isEqualTo("mailto:author@example.dev");
        }
    }
}
