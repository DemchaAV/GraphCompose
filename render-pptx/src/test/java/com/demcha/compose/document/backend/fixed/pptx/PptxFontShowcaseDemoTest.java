package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Font-identity specimen: four embedded families (sans, geometric sans, mono,
 * serif) with regular/bold/italic runs and a size ramp, rendered to the
 * PDF/PPTX pair. Both outputs embed the same TTF programs, so a real viewer
 * shows identical glyphs; the specimen exists so that claim can be checked by
 * eye, page against slide.
 */
class PptxFontShowcaseDemoTest {

    private static final String SPECIMEN = "Grumpy wizards 1234567890 fjord-quiz";

    private record Family(FontName name, String folder, String file, String viewerFamily) {
    }

    private static final List<Family> FAMILIES = List.of(
            new Family(FontName.of("ShowcaseLato"), "lato", "Lato", "Lato"),
            new Family(FontName.of("ShowcasePoppins"), "poppins", "Poppins", "Poppins"),
            new Family(FontName.of("ShowcaseJetBrainsMono"), "jetbrainsmono", "JetBrainsMono",
                    "JetBrains Mono"),
            new Family(FontName.of("ShowcaseSpectral"), "spectral", "Spectral", "Spectral"));

    @Test
    void writesTheFontIdentitySpecimenPair() throws Exception {
        Path output = Path.of("target", "visual-tests", "pptx-parity", "font-identity");
        Files.createDirectories(output);

        try (DocumentSession session = composeSpecimen()) {
            byte[] pdf = session.toPdfBytes();
            Files.write(output.resolve("font-identity.pdf"), pdf);
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            Files.write(output.resolve("font-identity.pptx"), pptx);

            var pdfPages = session.toImages(144);
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                assertThat(show.getSlides()).hasSameSizeAs(pdfPages);
                for (int i = 0; i < pdfPages.size(); i++) {
                    ImageIO.write(pdfPages.get(i), "png",
                            output.resolve("font-identity-page-" + (i + 1) + ".pdf.png").toFile());
                }
            }
        }
    }

    private static DocumentSession composeSpecimen() {
        var builder = GraphCompose.document()
                .pageSize(560, 420)
                .margin(DocumentInsets.of(28));
        for (Family family : FAMILIES) {
            builder.registerFontFamily(FontFamilyDefinition.classpath(
                            family.name(), "fonts/google/" + family.folder()
                                    + "/" + family.file() + "-Regular.ttf")
                    .boldResource("fonts/google/" + family.folder()
                            + "/" + family.file() + "-Bold.ttf")
                    .italicResource("fonts/google/" + family.folder()
                            + "/" + family.file() + "-Italic.ttf")
                    .boldItalicResource("fonts/google/" + family.folder()
                            + "/" + family.file() + "-BoldItalic.ttf")
                    .wordFamily(family.viewerFamily())
                    .build());
        }
        DocumentSession session = builder.create();
        session.pageFlow(page -> page.module("specimen", module -> {
            module.paragraph(p -> p.text("Embedded font identity — PDF vs PPTX")
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FAMILIES.get(0).name()).size(20)
                            .decoration(DocumentTextDecoration.BOLD)
                            .color(DocumentColor.rgb(15, 23, 42)).build()));
            for (Family family : FAMILIES) {
                module.paragraph(p -> p.text(family.viewerFamily())
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(family.name()).size(9)
                                .color(DocumentColor.GRAY).build()));
                module.paragraph(p -> p.rich(r -> r
                                .plain(SPECIMEN + "  ")
                                .bold("bold")
                                .plain("  ")
                                .italic("italic"))
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(family.name()).size(13).build()));
            }
            module.paragraph(p -> p.text("Size ramp:")
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FAMILIES.get(0).name()).size(9)
                            .color(DocumentColor.GRAY).build()));
            for (int size : new int[]{10, 14, 18, 24}) {
                module.paragraph(p -> p.text(size + " pt  " + SPECIMEN)
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FAMILIES.get(0).name()).size(size).build()));
            }
        }));
        return session;
    }
}
