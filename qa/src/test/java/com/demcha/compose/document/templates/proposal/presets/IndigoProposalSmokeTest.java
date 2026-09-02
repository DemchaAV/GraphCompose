package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.proposal.ProposalFooter;
import com.demcha.compose.document.templates.data.proposal.ProposalGlance;
import com.demcha.compose.document.templates.data.proposal.ProposalMetaLine;
import com.demcha.compose.document.templates.data.proposal.ProposalScope;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalData;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalDocumentSpec;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke test for {@link IndigoProposal} — proves the preset renders a
 * {@link StructuredProposalDocumentSpec} end-to-end with its packaged SVG icon
 * set on one page, renders an empty document through its guards, makes the
 * addressed person and the foot's channels followable, keeps a confidentiality
 * line on the foot's own last line, and reports an unknown icon token by name.
 */
class IndigoProposalSmokeTest {

    /** A notice as one is actually written: a sentence, and not a URI. */
    private static final String NOTICE =
            "Confidential — prepared for the addressee, not for onward circulation.";

    private static byte[] render(StructuredProposalDocumentSpec spec) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            IndigoProposal.create().compose(session, spec);
            assertThat(session.roots()).isNotEmpty();
            byte[] pdfBytes = session.toPdfBytes();
            assertThat(pdfBytes).isNotEmpty();
            return pdfBytes;
        }
    }

    private static String textOf(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static List<String> linkTargets(byte[] pdfBytes) throws Exception {
        List<String> targets = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            for (PDPage page : document.getPages()) {
                for (PDAnnotation annotation : page.getAnnotations()) {
                    if (annotation instanceof PDAnnotationLink link
                            && link.getAction() instanceof PDActionURI uri) {
                        targets.add(uri.getURI());
                    }
                }
            }
        }
        return targets;
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<StructuredProposalDocumentSpec> template = IndigoProposal.create();
        assertThat(template.id()).isEqualTo(IndigoProposal.ID);
        assertThat(template.displayName()).isEqualTo(IndigoProposal.DISPLAY_NAME);
    }

    @Test
    void rendersTheCanonicalProposalOnOnePage() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            IndigoProposal.create().compose(
                    session, IndigoProposalFixtures.canonicalProposal());
            // The design is a one-page sheet, and the fixture is sized for it.
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
        }
    }

    @Test
    void rendersEmptyProposal() throws Exception {
        // Exercises the guards: no monogram in the foot's mark, no channels, no
        // feature tiles, no steps, no priced rows, no total card, no notes.
        render(StructuredProposalDocumentSpec.from(StructuredProposalData.builder().build()));
    }

    @Test
    void canonicalRenderCarriesTheMoneyOnTheTextLayer() throws Exception {
        // The priced rows are composed table cells, which emit no placed node
        // of their own — the text layer is where their content is assertable.
        String text = textOf(render(IndigoProposalFixtures.canonicalProposal()));
        assertThat(text)
                .contains("Implementation & Onboarding")
                .contains("£2,500")
                .contains("Estimated First Year Investment")
                .contains("£5,500");
    }

    @Test
    void theAddressedPersonAndTheFootsChannelsAreFollowable() throws Exception {
        // A proposal names a person to reply to and the channels its issuer
        // answers on; a reader's device needs the form it can act on, and the
        // foot's channels say only how they are printed.
        assertThat(linkTargets(render(IndigoProposalFixtures.canonicalProposal())))
                .contains("mailto:priya@brightfuture.example")
                .contains("tel:+447700900123")
                .contains("https://kestrel.example/business")
                .contains("mailto:business@kestrel.example")
                .contains("tel:+442033228352");
    }

    @Test
    void aConfidentialityLineClosesTheFootWithoutOpeningAPage() throws Exception {
        // The design's foot is three lines and its last one sits against the
        // bottom margin, so the line closes the channels rather than opening a
        // fourth line that would carry the whole foot onto a second page.
        StructuredProposalData base = IndigoProposalFixtures.canonicalProposal().proposal();
        StructuredProposalDocumentSpec spec = StructuredProposalDocumentSpec.from(
                StructuredProposalData.builder()
                        .brand(base.brand())
                        .recipient(base.recipient())
                        .attention(base.attention())
                        .title(base.title())
                        .meta(base.meta())
                        .glance(base.glance())
                        .scope(base.scope())
                        .investment(base.investment())
                        .terms(base.terms())
                        .footer(new ProposalFooter(base.footer().name(),
                                base.footer().addressLines(), base.footer().contacts(),
                                NOTICE))
                        .build());
        try (DocumentSession session = GraphCompose.document().create()) {
            IndigoProposal.create().compose(session, spec);
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
            assertThat(textOf(session.toPdfBytes())).contains(NOTICE);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {NOTICE, "Commercial-in-confidence"})
    void aNoticeIsSetAsProseAndNotAsSomewhereToBeSent(String notice) throws Exception {
        // A confidentiality notice shares the foot's last line with the
        // channels, and a channel is linked from the shape of what it prints.
        // A notice is neither: the sentence would be a link target with spaces
        // in it, and the single word — which is a perfectly good host name —
        // would quietly become a site nobody meant to publish.
        StructuredProposalData base = IndigoProposalFixtures.canonicalProposal().proposal();
        StructuredProposalDocumentSpec spec = StructuredProposalDocumentSpec.from(
                StructuredProposalData.builder()
                        .brand(base.brand())
                        .footer(new ProposalFooter(base.footer().name(),
                                base.footer().addressLines(), List.of(), notice))
                        .build());
        byte[] pdfBytes = render(spec);
        assertThat(textOf(pdfBytes)).contains(notice);
        assertThat(linkTargets(pdfBytes)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 6, 7, 8, 14})
    void aPlanLongerThanTheDesignsNeverPrintsTheFootOverIt(int stepCount) throws Exception {
        // Every block is placed to a stated position, so a plan that runs past
        // where the map expects it would otherwise ask for a negative margin and
        // draw the closing rule over the last steps. Six and seven are the
        // counts that still fit the page — where an unclamped margin overlaps
        // silently — and fourteen is where it has to take a second page.
        StructuredProposalData base = IndigoProposalFixtures.canonicalProposal().proposal();
        List<ProposalScope.Item> steps = new ArrayList<>(base.scope().items());
        for (int i = steps.size(); i < stepCount; i++) {
            steps.add(new ProposalScope.Item(String.format("%02d", i + 1),
                    "Step " + (i + 1), "What the step covers"));
        }
        StructuredProposalDocumentSpec spec = StructuredProposalDocumentSpec.from(
                withScope(base, new ProposalScope(base.scope().heading(), "",
                        steps.subList(0, stepCount), base.scope().intro())));
        try (DocumentSession session = GraphCompose.document().create()) {
            IndigoProposal.create().compose(session, spec);
            List<LayoutNodeSnapshot> nodes = session.layoutSnapshot().nodes();
            LayoutNodeSnapshot plan = node(nodes, "LowerRow");
            LayoutNodeSnapshot closing = node(nodes, "FooterRule");
            if (closing.startPage() == plan.startPage()) {
                // y grows upward, so the rule's top edge must not reach above
                // the plan's bottom edge.
                assertThat(closing.computedY() + closing.placementHeight())
                        .isLessThanOrEqualTo(plan.computedY() + 0.01);
            }
            assertThat(textOf(session.toPdfBytes())).contains("Kestrel Payments Ltd");
        }
    }

    private static LayoutNodeSnapshot node(List<LayoutNodeSnapshot> nodes, String name) {
        return nodes.stream()
                .filter(n -> name.equals(n.entityName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No node named " + name));
    }

    private static StructuredProposalData withScope(StructuredProposalData base,
                                                    ProposalScope scope) {
        return StructuredProposalData.builder()
                .brand(base.brand())
                .recipient(base.recipient())
                .attention(base.attention())
                .title(base.title())
                .meta(base.meta())
                .glance(base.glance())
                .scope(scope)
                .investment(base.investment())
                .terms(base.terms())
                .footer(base.footer())
                .build();
    }

    @Test
    void aTileThatNamesNoMarkIsStillTheTileTheDesignDraws() throws Exception {
        // The shape is the design's and the mark inside it is the document's,
        // so a fact naming none keeps its tile — and the row of tiles keeps the
        // pitch the copy above it was measured against. Asserting the text would
        // pass with the tile deleted, so the tile itself is what is asked for.
        StructuredProposalDocumentSpec spec = StructuredProposalDocumentSpec.from(
                StructuredProposalData.builder()
                        .glance(new ProposalGlance("ABOUT", List.of(
                                new ProposalGlance.Fact("", "Global", "Scale", "")), ""))
                        .build());
        try (DocumentSession session = GraphCompose.document().create()) {
            IndigoProposal.create().compose(session, spec);
            LayoutNodeSnapshot tile = node(session.layoutSnapshot().nodes(), "FeatureTile_0");
            assertThat(tile.placementWidth()).isGreaterThan(0);
            assertThat(tile.placementHeight()).isGreaterThan(0);
            assertThat(textOf(session.toPdfBytes())).contains("Global").contains("Scale");
        }
    }

    @Test
    void rejectsUnknownIconTokenByName() {
        StructuredProposalDocumentSpec spec = StructuredProposalDocumentSpec.from(
                StructuredProposalData.builder()
                        .glance(new ProposalGlance("ABOUT", List.of(
                                new ProposalGlance.Fact("no-such-icon", "Global",
                                        "Scale", "")), ""))
                        .build());
        try (DocumentSession session = GraphCompose.document().create()) {
            assertThatThrownBy(() -> IndigoProposal.create().compose(session, spec))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no-such-icon");
        }
    }

    @Test
    void rejectsAMarkNamedOnTheBlockItWasNotCutFor() {
        // The header's marks are drawn in the accent for a pale disc and the
        // band's in white for a near-black tile, so a band token on a header
        // disc would render as nothing at all. It is refused by name instead.
        StructuredProposalDocumentSpec spec = StructuredProposalDocumentSpec.from(
                StructuredProposalData.builder()
                        .meta(new ProposalMetaLine(List.of(
                                new ProposalMetaLine.Entry("global", "DATE", "27 May 2026"))))
                        .build());
        try (DocumentSession session = GraphCompose.document().create()) {
            assertThatThrownBy(() -> IndigoProposal.create().compose(session, spec))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("global")
                    .hasMessageContaining("prepared-by");
        }
    }
}
