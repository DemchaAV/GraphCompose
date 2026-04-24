package com.demcha.compose.testsupport.engine.assembly;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.engine.components.content.barcode.BarcodeData;
import com.demcha.compose.engine.components.content.barcode.BarcodeType;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.renderable.BarcodeComponent;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.layout.Align;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit and visual integration tests for {@link BarcodeBuilder}.
 */
class BarcodeBuilderTest {

    @TempDir
    Path tempDir;

    // ===== Unit Tests =====

    @Test
    void shouldCreateBarcodeEntityWithQrCodeTypeByDefault() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            Entity entity = composer.componentBuilder()
                    .barcode()
                    .data("https://example.com")
                    .size(100, 100)
                    .anchor(Anchor.topLeft())
                    .build();

            assertThat(entity.hasAssignable(BarcodeComponent.class)).isTrue();
            assertThat(entity.getComponent(BarcodeData.class))
                    .hasValueSatisfying(data -> {
                        assertThat(data.getContent()).isEqualTo("https://example.com");
                        assertThat(data.getType()).isEqualTo(BarcodeType.QR_CODE);
                        assertThat(data.getForeground()).isEqualTo(Color.BLACK);
                        assertThat(data.getBackground()).isEqualTo(Color.WHITE);
                    });
            assertThat(entity.getComponent(ContentSize.class))
                    .hasValueSatisfying(size -> {
                        assertThat(size.width()).isEqualTo(100);
                        assertThat(size.height()).isEqualTo(100);
                    });
        }
    }

    @Test
    void shouldSetBarcodeTypeViaConvenienceMethods() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            Entity entity = composer.componentBuilder()
                    .barcode()
                    .data("12345678")
                    .code128()
                    .size(200, 80)
                    .anchor(Anchor.topLeft())
                    .build();

            assertThat(entity.getComponent(BarcodeData.class))
                    .hasValueSatisfying(data ->
                            assertThat(data.getType()).isEqualTo(BarcodeType.CODE_128));
        }
    }

    @Test
    void shouldSetCustomColorsAndQuietZone() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            Entity entity = composer.componentBuilder()
                    .barcode()
                    .data("TEST")
                    .type(BarcodeType.CODE_39)
                    .foreground(Color.BLUE)
                    .background(Color.YELLOW)
                    .quietZone(4)
                    .size(200, 60)
                    .anchor(Anchor.topLeft())
                    .build();

            assertThat(entity.getComponent(BarcodeData.class))
                    .hasValueSatisfying(data -> {
                        assertThat(data.getForeground()).isEqualTo(Color.BLUE);
                        assertThat(data.getBackground()).isEqualTo(Color.YELLOW);
                        assertThat(data.getMargin()).isEqualTo(4);
                    });
        }
    }

    @Test
    void shouldFailWithoutData() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            BarcodeBuilder builder = composer.componentBuilder()
                    .barcode()
                    .size(100, 100)
                    .anchor(Anchor.topLeft());

            assertThatThrownBy(builder::build)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("data content");
        }
    }

    // ===== Visual Integration Tests =====

    @Test
    void shouldRenderQrCodeToPdf() throws Exception {
        Path outputFile = tempDir.resolve("barcode-qr.pdf");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .margin(40, 40, 40, 40)
                .create()) {
            var cb = composer.componentBuilder();

            cb.barcode()
                    .data("https://github.com/DemchaAV/GraphCompose")
                    .qrCode()
                    .size(150, 150)
                    .anchor(Anchor.topLeft())
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists().isRegularFile().isNotEmptyFile();
        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThan(0);
        }
    }

    @Test
    void shouldRenderCode128ToPdf() throws Exception {
        Path outputFile = tempDir.resolve("barcode-code128.pdf");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .margin(40, 40, 40, 40)
                .create()) {
            var cb = composer.componentBuilder();

            cb.barcode()
                    .data("INV-2026-001234")
                    .code128()
                    .size(250, 80)
                    .anchor(Anchor.topLeft())
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists().isNotEmptyFile();
        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThan(0);
        }
    }

    @Test
    void shouldRenderEan13ToPdf() throws Exception {
        Path outputFile = tempDir.resolve("barcode-ean13.pdf");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .margin(40, 40, 40, 40)
                .create()) {
            var cb = composer.componentBuilder();

            cb.barcode()
                    .data("4006381333931")
                    .ean13()
                    .size(200, 80)
                    .foreground(ComponentColor.DARK_BLUE)
                    .anchor(Anchor.topLeft())
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists().isNotEmptyFile();
    }

    @Test
    void shouldRenderMultipleBarcodesInModule() throws Exception {
        Path outputFile = tempDir.resolve("barcode-multi.pdf");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .margin(40, 40, 40, 40)
                .create()) {
            var cb = composer.componentBuilder();

            var module = cb.moduleBuilder(Align.middle(8), composer.canvas())
                    .anchor(Anchor.topLeft())
                    .addChild(
                            cb.barcode()
                                    .data("https://graphcompose.dev")
                                    .qrCode()
                                    .size(120, 120)
                                    .build()
                    )
                    .addChild(
                            cb.barcode()
                                    .data("SHIP-99887766")
                                    .code128()
                                    .size(220, 70)
                                    .build()
                    )
                    .addChild(
                            cb.barcode()
                                    .data("5901234123457")
                                    .ean13()
                                    .size(200, 80)
                                    .build()
                    )
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists().isNotEmptyFile();
        try (PDDocument saved = Loader.loadPDF(outputFile.toFile())) {
            assertThat(saved.getNumberOfPages()).isGreaterThan(0);
        }
    }
}
