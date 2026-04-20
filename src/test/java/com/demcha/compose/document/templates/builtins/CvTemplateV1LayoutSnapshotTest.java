package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import com.demcha.compose.document.templates.data.MainPageCV;
import com.demcha.compose.document.templates.data.MainPageCvDTO;
import com.demcha.compose.font_library.FontName;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CvTemplateV1LayoutSnapshotTest {

    private final CvTemplateV1 template = new CvTemplateV1();

    @Test
    void shouldMatchStandardCvLayoutSnapshot() throws Exception {
        MainPageCV original = TemplateTestSupport.canonicalCv();
        MainPageCvDTO rewritten = TemplateTestSupport.rewrite(original);

        try (DocumentSession document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 15, 10, 15, 15)) {
            template.compose(document, original, rewritten);
            TemplateTestSupport.assertCanonicalSnapshot(document, "template_cv_1_standard", "cv");
        }
    }

    @Test
    void shouldMatchRichCvLayoutSnapshotAndRenderPdf() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("template_cv_1_layout_snapshot_rich", "clean", "templates", "cv");
        MainPageCV expanded = TemplateTestSupport.expandedCanonicalCv();
        MainPageCvDTO rewritten = TemplateTestSupport.rewrite(expanded);

        try (DocumentSession document = TemplateTestSupport.openFileDocument(outputFile, PDRectangle.A4, 15, 10, 15, 15, false)) {
            template.compose(document, expanded, rewritten);
            TemplateTestSupport.assertCanonicalSnapshot(document, "template_cv_1_rich_one_and_half_pages", "cv");
            document.buildPdf();
        }

        TemplateTestSupport.assertPdfFileLooksValid(outputFile, 2);
        TemplateTestSupport.assertPdfPageCount(outputFile, 2);
    }

    @Test
    void shouldNormalizeTechnicalSkillsWithVisibleBulletsIntoPerItemCanonicalBlocks() throws Exception {
        MainPageCV original = TemplateTestSupport.canonicalCv();
        original.getTechnicalSkills().setModulePoints(List.of(
                "• **Languages:** Java, SQL, Kotlin",
                "• **Backend:** Spring Boot, Spring Security, MapStruct",
                "• **Tools:** IntelliJ IDEA, Git, Maven"));

        try (DocumentSession document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 15, 10, 15, 15)) {
            template.compose(document, original, null);

            List<String> technicalSkillBodies = document.layoutSnapshot().nodes().stream()
                    .filter(node -> "ListNode".equals(node.entityKind()))
                    .map(node -> node.entityName())
                    .filter("TechnicalSkillsBody"::equals)
                    .toList();

            assertThat(technicalSkillBodies).containsExactly("TechnicalSkillsBody");
        }
    }

    @ParameterizedTest(name = "font theme {0}")
    @MethodSource("nonDefaultFontThemes")
    void shouldMatchFontThemeLayoutSnapshot(FontName fontName) throws Exception {
        MainPageCV original = TemplateTestSupport.canonicalCv();
        MainPageCvDTO rewritten = TemplateTestSupport.rewrite(original);

        try (DocumentSession document = TemplateTestSupport.openInMemoryDocument(PDRectangle.A4, 15, 10, 15, 15)) {
            new CvTemplateV1(TemplateTestSupport.cvThemeWith(fontName)).compose(document, original, rewritten);
            TemplateTestSupport.assertCanonicalSnapshot(
                    document,
                    "template_cv_1_" + TemplateTestSupport.snapshotSlug(fontName),
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
}
