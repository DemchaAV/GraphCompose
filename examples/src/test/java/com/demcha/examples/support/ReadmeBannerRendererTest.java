package com.demcha.examples.support;

import com.demcha.examples.flagships.EngineDeckExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the README hero renders straight to an image through the engine's
 * {@code toImage} path — no intermediate PDF, no external rasterizer — and that
 * the writer lands a non-trivial PNG.
 */
class ReadmeBannerRendererTest {

    @Test
    void rendersANonEmptyBannerImageWithoutAPdfRoundTrip() throws Exception {
        BufferedImage image = EngineDeckExample.renderBannerImage(96);

        // The banner page is 801x525pt; at 96 DPI that is ~1068x700px. Assert
        // generous lower bounds so the test guards "rendered something real"
        // without pinning exact pixels (DPI/metric drift would make that brittle).
        assertThat(image.getWidth()).isGreaterThan(600);
        assertThat(image.getHeight()).isGreaterThan(300);
    }

    @Test
    void writesThePngToTheGivenPath(@TempDir Path tmp) throws Exception {
        Path png = tmp.resolve("nested").resolve("banner.png");

        Path written = ReadmeBannerRenderer.render(png, 72);

        assertThat(written).isEqualTo(png);
        assertThat(png).exists();
        assertThat(Files.size(png)).isGreaterThan(2_000L);
    }

    @Test
    void bannerPropertiesVersionIsFilledByMavenResourceFiltering() throws Exception {
        // The on-classpath copy is the build's filtered output: if examples/pom.xml
        // ever loses the filtering, version stays the raw @project.version@ token
        // and the hero silently stops carrying the real release version.
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/banner.properties")) {
            assertThat(in).describedAs("banner.properties must be on the classpath").isNotNull();
            props.load(in);
        }
        assertThat(props.getProperty("version"))
                .describedAs("banner version must be filtered to the project version, not the raw @project.version@ token")
                .doesNotContain("@")
                .matches("\\d.*");
    }
}
