package com.demcha.testing.layout;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LayoutSnapshotPublicApiDogfoodTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSupportConsumerWorkflowThroughPublicApi() throws Exception {
        Path expectedRoot = tempDir.resolve("expected");
        Path actualRoot = tempDir.resolve("actual");
        String snapshotPath = "consumer/simple_document";
        String previous = System.getProperty(LayoutSnapshotAssertions.UPDATE_PROPERTY);

        try {
            System.setProperty(LayoutSnapshotAssertions.UPDATE_PROPERTY, "true");
            try (DocumentSession document = createDocument()) {
                composeSimpleDocument(document);
                LayoutSnapshotAssertions.assertMatches(document, expectedRoot, actualRoot, snapshotPath);
            }
        } finally {
            restoreSystemProperty(previous);
        }

        try (DocumentSession document = createDocument()) {
            composeSimpleDocument(document);
            LayoutSnapshotAssertions.assertMatches(document, expectedRoot, actualRoot, snapshotPath);
        }

        assertThat(expectedRoot.resolve("consumer").resolve("simple_document.json"))
                .exists()
                .isRegularFile();
        assertThat(actualRoot.resolve("consumer").resolve("simple_document.actual.json"))
                .doesNotExist();
    }

    private DocumentSession createDocument() {
        return GraphCompose.document()
                .pageSize(PDRectangle.A4)
                .margin(18, 18, 18, 18)
                .create();
    }

    private void composeSimpleDocument(DocumentSession document) {
        document.dsl()
                .pageFlow()
                .name("ConsumerRoot")
                .spacing(8)
                .addParagraph(paragraph -> paragraph
                        .name("Title")
                        .text("Consumer snapshot baseline")
                        .textStyle(DocumentTextStyle.DEFAULT))
                .addShape(shape -> shape
                        .name("AccentBox")
                        .size(120, 32))
                .build();
    }

    private void restoreSystemProperty(String previous) {
        if (previous == null) {
            System.clearProperty(LayoutSnapshotAssertions.UPDATE_PROPERTY);
            return;
        }
        System.setProperty(LayoutSnapshotAssertions.UPDATE_PROPERTY, previous);
    }
}
