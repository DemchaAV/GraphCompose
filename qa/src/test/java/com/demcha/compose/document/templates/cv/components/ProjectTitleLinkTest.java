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
import com.demcha.compose.document.templates.cv.data.RowStyle;
import com.demcha.compose.document.templates.cv.data.RowsSection;
import com.demcha.compose.document.templates.cv.presets.EngineeringResume;
import com.demcha.compose.document.templates.cv.presets.SidebarPortrait;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end wiring for inline-link CV project-card titles: a project row whose
 * label carries {@code [label](url)} markdown renders as a clickable link in the
 * two presets that hand-roll their project card outside the shared
 * {@code ProjectRenderer} — {@code EngineeringResume} (bordered card in the main
 * column) and {@code SidebarPortrait} (stacked row in the main column). Plain
 * labels stay plain and the trailing {@code " (stack)"} run is preserved, so the
 * change is backward-compatible.
 *
 * <p>The {@link com.demcha.compose.document.templates.core.text.MarkdownInline}
 * link primitive and {@link ProjectLabel} split are unit-tested separately; this
 * locks the preset wiring so a refactor cannot silently revert a project title to
 * a flat styled run (the state before this change), which would drop the link.</p>
 */
class ProjectTitleLinkTest {

    private static CvDocument docWithProject(String label) {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Test", "User").jobTitle("Engineer")
                        .contact("+1 555 0100", "user@example.com", "City").build())
                .section(RowsSection.builder("Projects", RowStyle.BULLETED_STACKED)
                        .row(label, "Did meaningful work.")
                        .build())
                .build();
    }

    /** Collects every inline text run in the composed document. */
    private static List<InlineTextRun> textRuns(DocumentTemplate<CvDocument> template,
                                                CvDocument doc) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4).margin(28, 28, 28, 28).create()) {
            template.compose(document, doc);
            List<ParagraphNode> paragraphs = new ArrayList<>();
            collectParagraphs(document.roots(), paragraphs);
            List<InlineTextRun> runs = new ArrayList<>();
            for (ParagraphNode paragraph : paragraphs) {
                for (InlineRun run : paragraph.inlineRuns()) {
                    if (run instanceof InlineTextRun text) {
                        runs.add(text);
                    }
                }
            }
            return runs;
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

    private static boolean isExternalLink(InlineTextRun run) {
        return run.linkTarget() instanceof ExternalLinkTarget;
    }

    private static String uri(InlineTextRun run) {
        return ((ExternalLinkTarget) run.linkTarget()).options().uri();
    }

    /**
     * EngineeringResume's bordered project card: a link in the project title
     * reaches the output, and the trailing {@code " (stack)"} run is preserved.
     */
    @Test
    void engineeringResumeProjectTitleBecomesLink() {
        List<InlineTextRun> runs = textRuns(EngineeringResume.create(),
                docWithProject("[Acme Corp](https://acme.example) (Java, PDFBox)"));
        assertThat(runs).filteredOn(ProjectTitleLinkTest::isExternalLink)
                .anySatisfy(run -> {
                    assertThat(run.text()).isEqualTo("Acme Corp");
                    assertThat(uri(run)).isEqualTo("https://acme.example");
                });
        assertThat(runs).anySatisfy(run ->
                assertThat(run.text()).contains("Java, PDFBox"));
    }

    /**
     * SidebarPortrait's stacked project row: a link in the project title reaches
     * the output, and the trailing {@code " (stack)"} run is preserved.
     */
    @Test
    void sidebarPortraitProjectTitleBecomesLink() {
        List<InlineTextRun> runs = textRuns(SidebarPortrait.create(),
                docWithProject("[Acme Corp](https://acme.example) (Java, PDFBox)"));
        assertThat(runs).filteredOn(ProjectTitleLinkTest::isExternalLink)
                .anySatisfy(run -> {
                    assertThat(run.text()).isEqualTo("Acme Corp");
                    assertThat(uri(run)).isEqualTo("https://acme.example");
                });
        assertThat(runs).anySatisfy(run ->
                assertThat(run.text()).contains("Java, PDFBox"));
    }

    /**
     * Backward-compatible: a plain project title is not rendered as a link. The
     * header contact block may render its own email/website links, so the control
     * asserts specifically that the plain project title run is not linked.
     */
    @Test
    void plainProjectTitleIsNotLink() {
        assertThat(projectTitleLinked(EngineeringResume.create())).isFalse();
        assertThat(projectTitleLinked(SidebarPortrait.create())).isFalse();
    }

    private static boolean projectTitleLinked(DocumentTemplate<CvDocument> template) {
        return textRuns(template, docWithProject("Acme Corp (Java, PDFBox)")).stream()
                .filter(ProjectTitleLinkTest::isExternalLink)
                .anyMatch(run -> run.text().equalsIgnoreCase("Acme Corp"));
    }
}
