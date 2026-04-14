package com.demcha.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.mock.MainPageCVMock;
import com.demcha.templates.CvTheme;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.data.MainPageCV;
import com.demcha.testing.VisualTestOutputs;
import com.demcha.testing.fixtures.CvTestFixtures;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CvTemplateV1LayoutSnapshotTest {

    private final CvTemplateV1 template = new CvTemplateV1();
    private final MainPageCV original = new MainPageCVMock().getMainPageCV();

    @Test
    void shouldMatchStandardCvLayoutSnapshot() throws Exception {
        MainPageCvDTO rewritten = MainPageCvDTO.from(original);

        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .markdown(true)
                .create()) {
            template.compose(composer, original, rewritten);
            LayoutSnapshotAssertions.assertMatches(composer, "template_cv_1_standard", "templates", "cv");
        }
    }

    @Test
    void shouldMatchRichCvLayoutSnapshotAndRenderPdf() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_layout_snapshot_rich", "clean", "templates", "cv");
        MainPageCV expanded = CvTestFixtures.createExpandedCvForOneAndHalfPages(original);
        MainPageCvDTO rewritten = MainPageCvDTO.from(expanded);

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .markdown(true)
                .create()) {
            template.compose(composer, expanded, rewritten);
            LayoutSnapshotAssertions.assertMatches(composer, "template_cv_1_rich_one_and_half_pages", "templates", "cv");
            composer.build();
        }

        assertThat(outputFile).exists().isRegularFile().isNotEmptyFile();
        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            assertThat(document.getNumberOfPages()).isEqualTo(2);
        }
    }

    @ParameterizedTest(name = "font theme {0}")
    @MethodSource("nonDefaultFontThemes")
    void shouldMatchFontThemeLayoutSnapshot(FontName fontName) throws Exception {
        MainPageCvDTO rewritten = MainPageCvDTO.from(original);

        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(15, 10, 15, 15)
                .markdown(true)
                .create()) {
            new CvTemplateV1(themeWith(fontName)).compose(composer, original, rewritten);
            LayoutSnapshotAssertions.assertMatches(
                    composer,
                    "template_cv_1_" + snapshotSlug(fontName),
                    "templates",
                    "cv",
                    "font-themes");
        }
    }

    private static Stream<FontName> nonDefaultFontThemes() {
        return Stream.of(
                FontName.TIMES_ROMAN,
                FontName.LATO,
                FontName.PT_SERIF,
                FontName.POPPINS,
                FontName.IBM_PLEX_SERIF,
                FontName.SPECTRAL,
                FontName.KANIT,
                FontName.VOLKHOV,
                FontName.ANDIKA);
    }

    private static CvTheme themeWith(FontName fontName) {
        CvTheme base = CvTheme.defaultTheme();
        return new CvTheme(
                base.primaryColor(),
                base.secondaryColor(),
                base.bodyColor(),
                base.accentColor(),
                fontName,
                fontName,
                base.nameFontSize(),
                base.headerFontSize(),
                base.bodyFontSize(),
                base.spacing(),
                base.moduleMargin(),
                base.spacingModuleName());
    }

    private static String snapshotSlug(FontName fontName) {
        return fontName.name()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }

}
