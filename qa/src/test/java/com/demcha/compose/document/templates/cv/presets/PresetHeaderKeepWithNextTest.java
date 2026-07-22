package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the preset-level keep-with-next wiring: single-column CV presets keep a
 * section heading with the first line of its body, marking keep-with-next at the
 * level that binds the heading to the body — the header SECTION for standalone-header
 * presets, the nested header SUBSECTION for combined header+body presets — and
 * crucially NOT the combined body/module section itself, which would wrongly bind it
 * to the next module. The pagination mechanism is covered by {@code
 * SectionKeepWithNextTest}; the last test here exercises the nested (combined-preset)
 * shape end-to-end across a page break.
 */
class PresetHeaderKeepWithNextTest {

    private static final DocumentColor GREY = DocumentColor.rgb(220, 220, 220);
    private static final DocumentColor INK = DocumentColor.rgb(20, 80, 95);

    private static CvDocument sampleDoc() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Test", "User").jobTitle("Engineer")
                        .contact("+1 555 0100", "user@example.com", "City").build())
                .section(new ParagraphSection("Professional Summary", "Summary text goes here."))
                .section(SkillsSection.builder("Technical Skills")
                        .leveledGroup("Languages", List.of(CvSkill.of("Java", 0.9), CvSkill.of("Kotlin", 0.8)))
                        .build())
                .section(EntriesSection.builder("Professional Experience")
                        .entry("Senior Engineer", "ACME", "2020-2024", "Built things.")
                        .entry("Engineer", "Globex", "2018-2020", "Built more things.")
                        .build())
                .section(EntriesSection.builder("Education")
                        .entry("BSc Computer Science", "University", "2014-2018", "Studied.")
                        .build())
                .build();
    }

    private static List<DocumentNode> nodesOf(DocumentTemplate<CvDocument> template) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4).margin(28, 28, 28, 28).create()) {
            template.compose(document, sampleDoc());
            List<DocumentNode> out = new ArrayList<>();
            collect(document.roots(), out);
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void collect(List<DocumentNode> nodes, List<DocumentNode> out) {
        for (DocumentNode node : nodes) {
            out.add(node);
            collect(node.children(), out);
        }
    }

    /** Standalone-header preset: every banner section is kept with next; no body section is. */
    @Test
    void boxedSectionsKeepsBannerNotBody() {
        List<DocumentNode> nodes = nodesOf(BoxedSections.create());
        assertThat(nodes.stream().filter(n -> n.name().startsWith("CvV2Banner")).toList())
                .isNotEmpty().allMatch(DocumentNode::keepWithNext);
        assertThat(nodes.stream().filter(n -> n.name().startsWith("CvV2Body")).toList())
                .isNotEmpty().noneMatch(DocumentNode::keepWithNext);
    }

    /**
     * Combined header+body preset: the nested header subsection is kept with next, but
     * the module section that wraps header and body is NOT — marking it would bind the
     * whole module to the next module instead of keeping the title with its own body.
     */
    @Test
    void classicSerifKeepsHeaderSubsectionNotModule() {
        List<DocumentNode> nodes = nodesOf(ClassicSerif.create());
        assertThat(nodes.stream().filter(n -> n.name().startsWith("CvV2ClassicSerif")).toList())
                .isNotEmpty().noneMatch(DocumentNode::keepWithNext);
        assertThat(nodes.stream().filter(n -> "Header".equals(n.name())).toList())
                .isNotEmpty().allMatch(DocumentNode::keepWithNext);
    }

    /** Multi-node banner header: the whole rule + banner + rule title run is kept together. */
    @Test
    void blueBannerKeepsWholeTitleRun() {
        List<DocumentNode> nodes = nodesOf(BlueBanner.create());
        List<DocumentNode> firstRun = nodes.stream()
                .filter(n -> n.name().startsWith("BlueBannerTitle_0")).toList();
        assertThat(firstRun).hasSizeGreaterThanOrEqualTo(3).allMatch(DocumentNode::keepWithNext);
    }

    /**
     * End-to-end for the combined-preset shape: a module section whose header is a
     * nested keep-with-next subsection relocates the header with the first line of the
     * body across a page break, rather than stranding it at the page bottom.
     */
    @Test
    void nestedHeaderSubsectionStaysWithBodyAcrossBoundary() {
        int headerPage = combinedModulePages(true, "HeaderMark");
        int bodyPage = combinedModulePages(true, "BodyMark");
        assertThat(headerPage).isEqualTo(bodyPage);

        // Control: without the opt-in, the header strands on the earlier page.
        assertThat(combinedModulePages(false, "HeaderMark"))
                .isLessThan(combinedModulePages(false, "BodyMark"));
    }

    private static int combinedModulePages(boolean keepWithNext, String mark) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 400).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            document.pageFlow().name("Flow").spacing(12)
                    .addSection("Filler", s -> s.addShape(260, 250, GREY))
                    .addSection("Module", host -> {
                        host.spacing(12);
                        // Header nested in its own subsection (the ClassicSerif shape).
                        host.addSection("Header", header -> {
                            if (keepWithNext) {
                                header.keepWithNext();
                            }
                            header.addShape(shape -> shape.name("HeaderMark").size(260, 40).fillColor(INK));
                        });
                        host.addShape(shape -> shape.name("BodyMark").size(260, 80).fillColor(INK));
                    })
                    .build();
            LayoutGraph graph = document.layoutGraph();
            PlacedNode node = graph.nodes().stream()
                    .filter(n -> mark.equals(n.semanticName()))
                    .findFirst().orElseThrow();
            return node.startPage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
