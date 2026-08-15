package com.demcha.examples.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the README hero renders to a widescreen PNG at the path it is given.
 *
 * <p>The release script writes this file into the repository, so what it must not do is
 * fail quietly: an unwritten path or an empty image would leave the previous release's
 * hero in place with nothing to say so.</p>
 */
class ReadmeHeroRendererTest {

    @Test
    void writesAWidescreenPngToANestedPath(@TempDir Path tmp) throws Exception {
        Path png = tmp.resolve("hero").resolve("nested").resolve("banner.png");

        Path written = ReadmeHeroRenderer.render(png, 72);

        assertThat(written).isEqualTo(png);
        assertThat(png).exists();
        assertThat(Files.size(png)).isGreaterThan(2_000L);

        // The banner page is 960x540pt, which is 960x540px at 72 DPI. The bounds sit
        // slightly under that so the assertion catches an empty or mis-sized render
        // without pinning the preview to exact rasterisation details.
        BufferedImage image = ImageIO.read(png.toFile());
        assertThat(image.getWidth()).isGreaterThan(900);
        assertThat(image.getHeight()).isGreaterThan(500);
    }
}
