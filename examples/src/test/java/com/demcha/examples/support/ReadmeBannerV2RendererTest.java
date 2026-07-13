package com.demcha.examples.support;

import com.demcha.examples.flagships.EngineDeckV2Example;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the parallel v2 README banner preview renders to an image and
 * can be written as a non-trivial PNG without changing the release banner.
 */
class ReadmeBannerV2RendererTest {

    @Test
    void rendersANonEmptyWidescreenBannerImage() throws Exception {
        BufferedImage image = EngineDeckV2Example.renderBannerImage(72);

        // The source page is 960x540pt, which is 960x540px at 72 DPI. Keep the
        // bounds slightly lower so the test catches empty or mis-sized output
        // without pinning the preview to exact rasterization details.
        assertThat(image.getWidth()).isGreaterThan(900);
        assertThat(image.getHeight()).isGreaterThan(500);
    }

    @Test
    void writesThePngToANestedPath(@TempDir Path tmp) throws Exception {
        Path png = tmp.resolve("v2-preview").resolve("nested").resolve("banner.png");

        Path written = ReadmeBannerV2Renderer.render(png, 72);

        assertThat(written).isEqualTo(png);
        assertThat(png).exists();
        assertThat(Files.size(png)).isGreaterThan(2_000L);
    }
}
