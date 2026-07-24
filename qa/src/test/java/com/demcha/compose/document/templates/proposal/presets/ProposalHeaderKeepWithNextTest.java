package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.proposal.ProposalData;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the proposal preset-level keep-with-next wiring: every flowing
 * section heading in {@link ModernProposal} — the body sections, the Timeline
 * and Investment tables, and the Acceptance terms — keeps its title with the
 * first line of the block it introduces across a page break, by marking the
 * title SECTION keep-with-next and (crucially) NOT the body section, which
 * would wrongly bind the heading to the next block.
 *
 * <p>The pagination mechanism itself is covered by {@code
 * SectionKeepWithNextTest}; this test locks the wiring so a refactor cannot
 * silently drop the flag or move it to the wrong node.</p>
 */
class ProposalHeaderKeepWithNextTest {

    private static final DocumentColor GREY = DocumentColor.rgb(220, 220, 220);
    private static final DocumentColor INK = DocumentColor.rgb(20, 80, 95);

    private static final List<String> TITLE_SECTIONS = List.of(
            "ProposalSectionTitle",
            "ProposalTimelineTitle",
            "ProposalPricingTitle",
            "ProposalAcceptanceTitle");

    private static final List<String> BODY_SECTIONS = List.of(
            "ProposalSectionBody",
            "ProposalAcceptanceBody");

    private static ProposalDocumentSpec sampleSpec() {
        return ProposalDocumentSpec.from(ProposalData.builder()
                .title("Proposal")
                .proposalNumber("GC-P-2026-014")
                .preparedDate("02 Apr 2026")
                .validUntil("30 Apr 2026")
                .projectTitle("Document platform consolidation")
                .executiveSummary("A phased engagement to retire per-team PDF scripts.")
                .sender(from -> from.name("GraphCompose Studio"))
                .recipient(to -> to.name("Northwind Systems"))
                .section("Scope", "Discovery, architecture, and a reference rollout.")
                .section("Approach", "Iterative delivery with weekly checkpoints.")
                .timelineItem("Discovery", "2 weeks", "Stakeholder interviews + audit")
                .timelineItem("Build", "6 weeks", "Engine + template migration")
                .pricingRow("Discovery", "Workshops + audit", "GBP 6,000")
                .emphasizedPricingRow("Total", "", "GBP 30,000")
                .acceptanceTerm("50% on signature, 50% on delivery.")
                .acceptanceTerm("Valid for 30 days from the prepared date.")
                .build());
    }

    private static List<DocumentNode> nodesOf(DocumentTemplate<ProposalDocumentSpec> template) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4).margin(DocumentInsets.of(28)).create()) {
            template.compose(document, sampleSpec());
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

    /** Every flowing section/table heading binds to the block it introduces. */
    @Test
    void keepsEveryFlowingTitleWithItsBody() {
        List<DocumentNode> nodes = nodesOf(ModernProposal.create());
        for (String titleName : TITLE_SECTIONS) {
            assertThat(nodes.stream().filter(n -> titleName.equals(n.name())).toList())
                    .as("title section %s should exist and be keep-with-next", titleName)
                    .isNotEmpty().allMatch(DocumentNode::keepWithNext);
        }
    }

    /** The body sections are NOT kept-with-next — that would bind a heading to the next block. */
    @Test
    void doesNotKeepBodySectionsWithNext() {
        List<DocumentNode> nodes = nodesOf(ModernProposal.create());
        for (String bodyName : BODY_SECTIONS) {
            assertThat(nodes.stream().filter(n -> bodyName.equals(n.name())).toList())
                    .as("body section %s should exist and not be keep-with-next", bodyName)
                    .isNotEmpty().noneMatch(DocumentNode::keepWithNext);
        }
    }

    /**
     * End-to-end for the proposal shape: the template emits each section as its own
     * {@code pageFlow().build()} group, so this reproduces that structure — a filler
     * group that nearly fills the page, then a separate title + body group — and proves
     * the title relocates to the body's page instead of stranding at the page bottom.
     * Guards that keep-with-next fires across the separate-flow-group boundary, not only
     * within a single continuous flow.
     */
    @Test
    void titleRelocatesWithBodyAcrossSeparatePageFlowGroups() {
        assertThat(titleMarkPage(true)).isEqualTo(bodyMarkPage(true));

        // Control: without the opt-in, the title strands on the earlier page.
        assertThat(titleMarkPage(false)).isLessThan(bodyMarkPage(false));
    }

    private static int titleMarkPage(boolean keepWithNext) {
        return markPage(keepWithNext, "TitleMark");
    }

    private static int bodyMarkPage(boolean keepWithNext) {
        return markPage(keepWithNext, "BodyMark");
    }

    private static int markPage(boolean keepWithNext, String mark) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 400).margin(DocumentInsets.of(20)).create()) {
            // Group 1: a separate flow group that nearly fills the page.
            document.dsl().pageFlow().name("Filler")
                    .addSection("FillerBody", s -> s.addShape(260, 250, GREY))
                    .build();
            // Group 2: a separate flow group in the proposal's title + body shape.
            document.dsl().pageFlow().name("SectionGroup").spacing(12)
                    .addSection("Title", s -> {
                        if (keepWithNext) {
                            s.keepWithNext();
                        }
                        s.addShape(shape -> shape.name("TitleMark").size(260, 40).fillColor(INK));
                    })
                    .addSection("Body", s -> s
                            .addShape(shape -> shape.name("BodyMark").size(260, 80).fillColor(INK)))
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
