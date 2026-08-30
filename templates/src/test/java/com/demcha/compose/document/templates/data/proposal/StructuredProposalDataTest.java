package com.demcha.compose.document.templates.data.proposal;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Normalization and immutability contract of the structured proposal model:
 * absent components become their empty forms, leaf strings never surface as
 * {@code null}, row roles default to {@code NONE}, and every collection is
 * frozen at construction.
 */
class StructuredProposalDataTest {

    @Test
    void emptyBuilderYieldsEmptyFormsForEveryComponent() {
        StructuredProposalData data = StructuredProposalData.builder().build();

        assertThat(data.brand().monogram()).isEmpty();
        assertThat(data.title().lead()).isEmpty();
        assertThat(data.meta().preparedFor()).isEmpty();
        assertThat(data.executiveSummary().paragraphs()).isEmpty();
        assertThat(data.glance().facts()).isEmpty();
        assertThat(data.goals().items()).isEmpty();
        assertThat(data.scope().items()).isEmpty();
        assertThat(data.deliverables().leftColumn()).isEmpty();
        assertThat(data.deliverables().rightColumn()).isEmpty();
        assertThat(data.timeline().columnHeaders()).isEmpty();
        assertThat(data.timeline().phases()).isEmpty();
        assertThat(data.investment().rows()).isEmpty();
        assertThat(data.terms().items()).isEmpty();
        assertThat(data.acceptance().fields()).isEmpty();
    }

    @Test
    void leafRecordsNormalizeNullStringsToEmpty() {
        ProposalGlance.Fact fact = new ProposalGlance.Fact(null, null, null, null);
        assertThat(fact.icon()).isEmpty();
        assertThat(fact.label()).isEmpty();
        assertThat(fact.value()).isEmpty();
        assertThat(fact.note()).isEmpty();

        ProposalGoals.Goal goal = new ProposalGoals.Goal(null, null);
        assertThat(goal.icon()).isEmpty();
        assertThat(goal.text()).isEmpty();

        ProposalScope.Item item = new ProposalScope.Item(null, null, null);
        assertThat(item.number()).isEmpty();
        assertThat(item.title()).isEmpty();
        assertThat(item.description()).isEmpty();

        ProposalPhaseGrid.Phase phase = new ProposalPhaseGrid.Phase(null, null, null, null, null);
        assertThat(phase.number()).isEmpty();
        assertThat(phase.name()).isEmpty();
        assertThat(phase.focus()).isEmpty();
        assertThat(phase.duration()).isEmpty();
        assertThat(phase.output()).isEmpty();

        ProposalInvestment.Row row = new ProposalInvestment.Row(null, null, null);
        assertThat(row.label()).isEmpty();
        assertThat(row.amount()).isEmpty();

        ProposalMetaLine meta = new ProposalMetaLine(null, null, null);
        assertThat(meta.preparedFor()).isEmpty();
        assertThat(meta.preparedBy()).isEmpty();
        assertThat(meta.date()).isEmpty();

        ProposalTitleLines title = new ProposalTitleLines(null, null, null);
        assertThat(title.lead()).isEmpty();
        assertThat(title.second()).isEmpty();
        assertThat(title.third()).isEmpty();
    }

    @Test
    void sectionHeadingsAndIconsNormalizeNullToEmpty() {
        assertThat(new ProposalSummaryBlock(null, null, null).heading()).isEmpty();
        assertThat(new ProposalSummaryBlock(null, null, null).icon()).isEmpty();
        assertThat(new ProposalGlance(null, null).heading()).isEmpty();
        assertThat(new ProposalGoals(null, null, null).icon()).isEmpty();
        assertThat(new ProposalScope(null, null, null).heading()).isEmpty();
        assertThat(new ProposalDeliverables(null, null, null, null).icon()).isEmpty();
        assertThat(new ProposalPhaseGrid(null, null, null, null).heading()).isEmpty();
        assertThat(new ProposalInvestment(null, null, null, null, null, null, null)
                .amountHeader()).isEmpty();
        assertThat(new ProposalTermsBlock(null, null, null).icon()).isEmpty();
        assertThat(new ProposalAcceptance(null, null, null, null).statement()).isEmpty();
        assertThat(new ProposalBrand(null, null, null, null, null, null).footerName()).isEmpty();
    }

    @Test
    void investmentRowRoleDefaultsToNone() {
        ProposalInvestment.Row row = new ProposalInvestment.Row("Design", "$1,000", null);
        assertThat(row.role()).isEqualTo(ProposalInvestment.Role.NONE);
    }

    @Test
    void everyCollectionIsFrozenAtConstruction() {
        List<String> source = new ArrayList<>(List.of("seed"));
        ProposalDeliverables deliverables =
                new ProposalDeliverables("DELIVERABLES", "badge-deliverables", source, source);
        source.add("added after construction");
        assertThat(deliverables.leftColumn()).containsExactly("seed");
        assertThat(deliverables.rightColumn()).containsExactly("seed");

        List<List<?>> frozen = List.of(
                deliverables.leftColumn(),
                deliverables.rightColumn(),
                new ProposalSummaryBlock("S", "i", new ArrayList<>()).paragraphs(),
                new ProposalGlance("G", new ArrayList<>()).facts(),
                new ProposalGoals("G", "i", new ArrayList<>()).items(),
                new ProposalScope("S", "i", new ArrayList<>()).items(),
                new ProposalPhaseGrid("T", "i", new ArrayList<>(), new ArrayList<>()).columnHeaders(),
                new ProposalPhaseGrid("T", "i", new ArrayList<>(), new ArrayList<>()).phases(),
                new ProposalInvestment("I", "i", "ITEM", "AMOUNT",
                        new ArrayList<>(), "TOTAL", "$0").rows(),
                new ProposalTermsBlock("T", "i", new ArrayList<>()).items(),
                new ProposalAcceptance("A", "i", "s", new ArrayList<>()).fields());
        for (List<?> list : frozen) {
            assertThatThrownBy(() -> list.remove(0))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    void builderPassesEveryComponentThroughUnchanged() {
        ProposalBrand brand = new ProposalBrand("N", "Northline", "Digital Studio",
                "Proposal", "northline.example", "Northline Digital");
        ProposalTitleLines title = new ProposalTitleLines("A proposal", "for the", "redesign");
        ProposalMetaLine meta = new ProposalMetaLine("Client", "Studio", "2026-08-30");
        ProposalSummaryBlock summary = new ProposalSummaryBlock("SUMMARY", "badge-summary",
                List.of("First paragraph."));
        ProposalGlance glance = new ProposalGlance("AT A GLANCE",
                List.of(new ProposalGlance.Fact("fact-start", "START", "Sep 2026", "")));
        ProposalGoals goals = new ProposalGoals("GOALS", "badge-goals",
                List.of(new ProposalGoals.Goal("goal-brand", "Sharpen the brand")));
        ProposalScope scope = new ProposalScope("SCOPE", "badge-scope",
                List.of(new ProposalScope.Item("01", "Discovery", "Interviews and audit")));
        ProposalDeliverables deliverables = new ProposalDeliverables("DELIVERABLES",
                "badge-deliverables", List.of("Style guide"), List.of("Launch support"));
        ProposalPhaseGrid timeline = new ProposalPhaseGrid("TIMELINE", "badge-timeline",
                List.of("PHASE", "FOCUS", "DURATION", "OUTPUT"),
                List.of(new ProposalPhaseGrid.Phase("1", "Discovery", "Research",
                        "2 weeks", "Findings deck")));
        ProposalInvestment investment = new ProposalInvestment("INVESTMENT",
                "badge-investment", "ITEM", "AMOUNT",
                List.of(new ProposalInvestment.Row("Subtotal", "$9,000",
                        ProposalInvestment.Role.SUBTOTAL)),
                "TOTAL", "$9,900");
        ProposalTermsBlock terms = new ProposalTermsBlock("TERMS", "badge-terms",
                List.of("Net 14."));
        ProposalAcceptance acceptance = new ProposalAcceptance("ACCEPTANCE",
                "badge-acceptance", "Signing below accepts this proposal.",
                List.of("Name", "Date", "Signature"));

        StructuredProposalData data = StructuredProposalData.builder()
                .brand(brand)
                .title(title)
                .meta(meta)
                .executiveSummary(summary)
                .glance(glance)
                .goals(goals)
                .scope(scope)
                .deliverables(deliverables)
                .timeline(timeline)
                .investment(investment)
                .terms(terms)
                .acceptance(acceptance)
                .build();

        assertThat(data.brand()).isSameAs(brand);
        assertThat(data.title()).isSameAs(title);
        assertThat(data.meta()).isSameAs(meta);
        assertThat(data.executiveSummary()).isSameAs(summary);
        assertThat(data.glance()).isSameAs(glance);
        assertThat(data.goals()).isSameAs(goals);
        assertThat(data.scope()).isSameAs(scope);
        assertThat(data.deliverables()).isSameAs(deliverables);
        assertThat(data.timeline()).isSameAs(timeline);
        assertThat(data.investment()).isSameAs(investment);
        assertThat(data.terms()).isSameAs(terms);
        assertThat(data.acceptance()).isSameAs(acceptance);
        assertThat(data.investment().rows().get(0).role())
                .isEqualTo(ProposalInvestment.Role.SUBTOTAL);
    }

    @Test
    void documentSpecNormalizesNullProposalToEmptyData() {
        assertThat(StructuredProposalDocumentSpec.from(null).proposal()).isNotNull();
        assertThat(new StructuredProposalDocumentSpec(null).proposal().glance().facts()).isEmpty();

        StructuredProposalData data = StructuredProposalData.builder().build();
        assertThat(StructuredProposalDocumentSpec.from(data).proposal()).isSameAs(data);
    }
}
