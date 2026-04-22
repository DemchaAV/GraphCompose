package com.demcha.compose.layout_core.core;

import com.demcha.compose.testsupport.EngineComposerHarness;

import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EngineComposerHarnessContractTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExposeCanvasAndExportBytesThroughCommonComposerContract() throws Exception {
        byte[] pdfBytes;

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            assertThat(composer.canvas().innerWidth()).isPositive();

            composer.componentBuilder()
                    .text()
                    .textWithAutoSize("Common composer contract")
                    .textStyle(TextStyle.DEFAULT_STYLE)
                    .anchor(Anchor.topLeft())
                    .build();

            pdfBytes = composer.toBytes();
        }

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void shouldWritePdfFileWhenBuildIsCalledThroughCommonComposerContract() throws Exception {
        Path outputFile = tempDir.resolve("common-composer-build.pdf");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile).create()) {
            composer.componentBuilder()
                    .text()
                    .textWithAutoSize("Build to file")
                    .textStyle(TextStyle.DEFAULT_STYLE)
                    .anchor(Anchor.topLeft())
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists().isRegularFile().isNotEmptyFile();
        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThan(0);
        }
    }
}
