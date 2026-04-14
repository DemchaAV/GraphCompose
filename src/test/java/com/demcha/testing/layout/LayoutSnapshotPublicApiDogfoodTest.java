package com.demcha.testing.layout;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.core.PdfComposer;
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
            try (PdfComposer composer = createComposer()) {
                composeSimpleDocument(composer);
                LayoutSnapshotAssertions.assertMatches(composer, expectedRoot, actualRoot, snapshotPath);
            }
        } finally {
            restoreSystemProperty(previous);
        }

        try (PdfComposer composer = createComposer()) {
            composeSimpleDocument(composer);
            LayoutSnapshotAssertions.assertMatches(composer, expectedRoot, actualRoot, snapshotPath);
        }

        assertThat(expectedRoot.resolve("consumer").resolve("simple_document.json"))
                .exists()
                .isRegularFile();
        assertThat(actualRoot.resolve("consumer").resolve("simple_document.actual.json"))
                .doesNotExist();
    }

    private PdfComposer createComposer() {
        return GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(18, 18, 18, 18)
                .create();
    }

    private void composeSimpleDocument(PdfComposer composer) {
        ComponentBuilder cb = composer.componentBuilder();

        cb.vContainer(Align.left(8))
                .entityName("ConsumerRoot")
                .anchor(Anchor.topLeft())
                .addChild(cb.text()
                        .entityName("Title")
                        .textWithAutoSize("Consumer snapshot baseline")
                        .textStyle(TextStyle.DEFAULT_STYLE)
                        .anchor(Anchor.topLeft())
                        .build())
                .addChild(cb.rectangle()
                        .entityName("AccentBox")
                        .size(120, 32)
                        .anchor(Anchor.topLeft())
                        .build())
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
