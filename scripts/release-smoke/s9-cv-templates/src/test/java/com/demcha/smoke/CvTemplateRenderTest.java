package com.demcha.smoke;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.presets.BoxedSections;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario 9 — the profile half of the templates path: a CV preset
 * ({@code BoxedSections}) composes a {@code CvDocument} and renders through the
 * PDF stack, from the published aggregate alone.
 *
 * <p>Scenario 4 already proves the business half ({@code ModernInvoice}) on
 * {@code graph-compose} + {@code graph-compose-templates}. What this adds is the other
 * data model — the showcase hands a reader {@code CvDocument} and a CV preset for 27 of
 * its cards — and the reason those two coordinates are not enough for it: a CV theme
 * draws in PT Serif, and the bundled Google faces live in a companion artifact that is
 * versioned independently of the release, so the pair compiles and then throws at render.
 * The aggregate is what carries them at the release's own version.</p>
 *
 * <p>The calls below are the ones the showcase publishes, deliberately: that snippet is
 * compiled against the development tree, while this resolves the published release, so an
 * API it uses that did not ship yet fails here rather than in a reader's project.</p>
 */
class CvTemplateRenderTest {

    @Test
    void cvPresetComposesAndRenders() throws Exception {
        CvIdentity identity = CvIdentity.builder()
                .name("Jane", "Doe")
                .jobTitle("Backend Engineer")
                .contact("+44 20 7946 0958", "jane.doe@example.com", "London, UK")
                .build();

        CvDocument cv = CvDocument.ofMainSections(identity, List.of(
                new ParagraphSection("Profile", "Ten years building document pipelines.")));

        BrandTheme theme = BrandTheme.boxedClassic();
        DocumentTemplate<CvDocument> template = BoxedSections.create(theme);

        Path out = Files.createTempFile("gc-smoke-cv", ".pdf");
        try (DocumentSession document = GraphCompose.document(out)
                .pageSize(DocumentPageSize.A4)
                .margin(36f, 36f, 36f, 36f)
                .create()) {
            template.compose(document, cv);
            document.buildPdf();
        }

        assertThat(Files.size(out)).isGreaterThan(0L);
        byte[] head = Arrays.copyOf(Files.readAllBytes(out), 5);
        assertThat(new String(head)).isEqualTo("%PDF-");
    }
}
