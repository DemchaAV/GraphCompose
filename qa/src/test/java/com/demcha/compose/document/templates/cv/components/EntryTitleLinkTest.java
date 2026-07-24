package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ExternalLinkTarget;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.presets.BlueBanner;
import com.demcha.compose.document.templates.cv.presets.EngineeringResume;
import com.demcha.compose.document.templates.cv.presets.ModernProfessional;
import com.demcha.compose.document.templates.cv.presets.MonogramSidebar;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end wiring for inline-link CV entry titles and subtitles: an entry
 * whose title or subtitle carries {@code [label](url)} markdown renders as a
 * clickable link across the shared renderers — the canonical two-column
 * {@code EntryRenderer} (via {@code ModernProfessional}) and the compact
 * upper-cased {@code EntryCompactRenderer} (via {@code BlueBanner}). Plain
 * titles stay plain, so the change is backward-compatible.
 *
 * <p>The {@link com.demcha.compose.document.templates.core.text.MarkdownInline}
 * link/transform primitives are unit-tested separately; this locks the preset
 * wiring so a refactor cannot silently revert a title to stripped plain text.</p>
 */
class EntryTitleLinkTest {

    private static CvDocument docWith(String title, String subtitle) {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Test", "User").jobTitle("Engineer")
                        .contact("+1 555 0100", "user@example.com", "City").build())
                .section(EntriesSection.builder("Professional Experience")
                        .entry(title, subtitle, "2020-2024", "Built things.")
                        .build())
                .build();
    }

    /** Collects every external-link run in the composed document. */
    private static List<InlineTextRun> linkRuns(DocumentTemplate<CvDocument> template,
                                                CvDocument doc) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4).margin(28, 28, 28, 28).create()) {
            template.compose(document, doc);
            List<ParagraphNode> paragraphs = new ArrayList<>();
            collectParagraphs(document.roots(), paragraphs);
            List<InlineTextRun> links = new ArrayList<>();
            for (ParagraphNode paragraph : paragraphs) {
                for (InlineRun run : paragraph.inlineRuns()) {
                    if (run instanceof InlineTextRun text
                            && text.linkTarget() instanceof ExternalLinkTarget) {
                        links.add(text);
                    }
                }
            }
            return links;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void collectParagraphs(List<DocumentNode> nodes, List<ParagraphNode> out) {
        for (DocumentNode node : nodes) {
            if (node instanceof ParagraphNode paragraph) {
                out.add(paragraph);
            }
            collectParagraphs(node.children(), out);
        }
    }

    private static String uri(InlineTextRun run) {
        return ((ExternalLinkTarget) run.linkTarget()).options().uri();
    }

    /** Canonical EntryRenderer: a link in the entry title reaches the output. */
    @Test
    void entryRendererTitleBecomesLink() {
        List<InlineTextRun> links = linkRuns(ModernProfessional.create(),
                docWith("[Acme Corp](https://acme.example)", "Senior Engineer"));
        assertThat(links).anySatisfy(run -> {
            assertThat(run.text()).isEqualTo("Acme Corp");
            assertThat(uri(run)).isEqualTo("https://acme.example");
        });
    }

    /** Canonical EntryRenderer: the subtitle is link-aware too. */
    @Test
    void entryRendererSubtitleBecomesLink() {
        List<InlineTextRun> links = linkRuns(ModernProfessional.create(),
                docWith("Senior Engineer", "[Acme Corp](https://acme.example)"));
        assertThat(links).anySatisfy(run ->
                assertThat(uri(run)).isEqualTo("https://acme.example"));
    }

    /** Compact upper-cased path: the link survives and its label is upper-cased, url intact. */
    @Test
    void compactUpperCasedTitleBecomesUpperCasedLink() {
        List<InlineTextRun> links = linkRuns(BlueBanner.create(),
                docWith("[Acme Corp](https://acme.example)", "Senior Engineer"));
        assertThat(links).anySatisfy(run -> {
            assertThat(run.text()).isEqualTo("ACME CORP");
            assertThat(uri(run)).isEqualTo("https://acme.example");
        });
    }

    /** Hand-rolled per-preset path (upper-cased title built outside the shared renderers). */
    @Test
    void handRolledPresetTitleBecomesLink() {
        List<InlineTextRun> links = linkRuns(MonogramSidebar.create(),
                docWith("[Acme Corp](https://acme.example)", "Senior Engineer"));
        assertThat(links).anySatisfy(run -> {
            assertThat(run.text()).isEqualTo("ACME CORP");
            assertThat(uri(run)).isEqualTo("https://acme.example");
        });
    }

    /** Hand-rolled EngineeringResume role title (non-upper-cased) is link-aware. */
    @Test
    void engineeringResumeRoleTitleBecomesLink() {
        List<InlineTextRun> links = linkRuns(EngineeringResume.create(),
                docWith("[Acme Corp](https://acme.example)", "Senior Engineer"));
        assertThat(links).anySatisfy(run -> {
            assertThat(run.text()).isEqualTo("Acme Corp");
            assertThat(uri(run)).isEqualTo("https://acme.example");
        });
    }

    /**
     * Backward-compatible: a plain title/subtitle is not rendered as a link. The
     * header contact block may render its own email/website links, so the control
     * asserts specifically that the plain entry title and subtitle are not linked.
     */
    @Test
    void plainTitleAndSubtitleAreNotLinks() {
        assertThat(titleOrSubtitleLinked(ModernProfessional.create())).isFalse();
        assertThat(titleOrSubtitleLinked(BlueBanner.create())).isFalse();
    }

    private static boolean titleOrSubtitleLinked(DocumentTemplate<CvDocument> template) {
        return linkRuns(template, docWith("Senior Engineer", "Acme Corp")).stream()
                .anyMatch(run -> run.text().equalsIgnoreCase("Senior Engineer")
                        || run.text().equalsIgnoreCase("Acme Corp"));
    }
}
