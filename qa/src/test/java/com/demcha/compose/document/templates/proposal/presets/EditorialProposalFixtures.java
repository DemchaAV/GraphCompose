package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.templates.data.proposal.ProposalAcceptance;
import com.demcha.compose.document.templates.data.proposal.ProposalBrand;
import com.demcha.compose.document.templates.data.proposal.ProposalDeliverables;
import com.demcha.compose.document.templates.data.proposal.ProposalGlance;
import com.demcha.compose.document.templates.data.proposal.ProposalGoals;
import com.demcha.compose.document.templates.data.proposal.ProposalInvestment;
import com.demcha.compose.document.templates.data.proposal.ProposalMetaLine;
import com.demcha.compose.document.templates.data.proposal.ProposalPhaseGrid;
import com.demcha.compose.document.templates.data.proposal.ProposalScope;
import com.demcha.compose.document.templates.data.proposal.ProposalSummaryBlock;
import com.demcha.compose.document.templates.data.proposal.ProposalTermsBlock;
import com.demcha.compose.document.templates.data.proposal.ProposalTitleLines;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalData;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalDocumentSpec;

import java.util.List;

/**
 * Shared fixture data for the Editorial proposal gates — the SAME spec
 * feeds the smoke, layout-snapshot, and pixel gates, so a geometry shift
 * the pixel budget absorbs still trips the exact snapshot, and vice versa.
 *
 * <p>The canonical fixture is the preset's reference content: a two-page
 * brand-refresh proposal exercising every band — all four glance facts
 * (one with the optional note), four goal cells, five scope rows, both
 * deliverable columns, the four-phase grid, an investment table with
 * ordinary, subtotal and optional rows plus the total band, four terms,
 * and the three-field signing card.</p>
 *
 * <p>Kept in lockstep with the examples module's
 * {@code EditorialProposalSampleData} — the two modules cannot share a
 * source file, so a content change here belongs there too.</p>
 */
final class EditorialProposalFixtures {

    private EditorialProposalFixtures() {
    }

    static StructuredProposalDocumentSpec canonicalProposal() {
        return StructuredProposalDocumentSpec.from(StructuredProposalData.builder()
                .brand(new ProposalBrand("N", "NORTHLINE", "STUDIO", "PROPOSAL",
                        "northlinestudio.com", "NORTHLINE STUDIO"))
                .title(new ProposalTitleLines("Proposal —", "Brand Refresh &",
                        "Website Redesign"))
                .meta(new ProposalMetaLine("PREPARED FOR ASTERA HEALTH CO.",
                        "PREPARED BY NORTHLINE STUDIO", "25 AUGUST 2026"))
                .executiveSummary(new ProposalSummaryBlock("Executive Summary", "", List.of(
                        "Thank you for the opportunity to partner with Astera Health Co. "
                                + "on your next chapter. This proposal outlines our approach to "
                                + "refreshing your brand identity and redesigning your website to "
                                + "better reflect your mission, elevate credibility, and deliver a "
                                + "seamless experience for the people you serve.",
                        "Our process blends strategic thinking with thoughtful design to "
                                + "create a cohesive brand system and a modern, conversion-focused "
                                + "website. The outcome will be a clear, confident presence that "
                                + "strengthens trust, improves engagement, and supports your "
                                + "growth goals.")))
                .glance(new ProposalGlance("", List.of(
                        new ProposalGlance.Fact("fact-duration", "PROJECT DURATION",
                                "6 weeks", null),
                        new ProposalGlance.Fact("fact-start", "START WINDOW",
                                "September 2026", null),
                        new ProposalGlance.Fact("fact-contact", "PRIMARY CONTACT",
                                "Project Lead", null),
                        new ProposalGlance.Fact("fact-validity", "PROPOSAL VALIDITY",
                                "14 days", "(until 8 September 2026)"))))
                .goals(new ProposalGoals("Project Goals", "goal-check", List.of(
                        new ProposalGoals.Goal("",
                                "Strengthen brand credibility and visual consistency."),
                        new ProposalGoals.Goal("",
                                "Improve website usability and drive meaningful action."),
                        new ProposalGoals.Goal("",
                                "Communicate your value clearly across all touchpoints."),
                        new ProposalGoals.Goal("",
                                "Build a scalable digital foundation for future growth."))))
                .scope(new ProposalScope("Scope of Work", "", List.of(
                        new ProposalScope.Item("01", "Discovery & Research",
                                "Stakeholder interviews, brand audit, competitor review, and "
                                        + "audience insights to inform strategy."),
                        new ProposalScope.Item("02", "Visual Identity Refinement",
                                "Refine logo system, color palette, typography, and visual "
                                        + "guidelines for a cohesive, modern identity."),
                        new ProposalScope.Item("03", "Website UX/UI Design",
                                "Design a responsive, accessibility-minded website with "
                                        + "intuitive navigation and clear conversion pathways."),
                        new ProposalScope.Item("04", "Content Structure & Messaging",
                                "Define site architecture, key messaging, and content "
                                        + "hierarchy to communicate value and build trust."),
                        new ProposalScope.Item("05", "Launch Support",
                                "Developer handoff, QA support, training, and go-live "
                                        + "assistance to ensure a smooth launch."))))
                .deliverables(new ProposalDeliverables("Deliverables", "",
                        List.of("Brand strategy summary",
                                "Refined logo suite & brand guidelines",
                                "Color, typography & design system",
                                "Website UX wireframes",
                                "High-fidelity website designs"),
                        List.of("Responsive design for desktop, tablet & mobile",
                                "Content strategy & page templates",
                                "Developer-ready assets & specifications",
                                "Launch checklist & training session")))
                .timeline(new ProposalPhaseGrid("Timeline", "",
                        List.of("PHASE", "FOCUS", "DURATION", "OUTPUT"), List.of(
                        new ProposalPhaseGrid.Phase("01", "Discover",
                                "Research, audit, insights, and strategy", "1 week",
                                "Discovery report & creative direction"),
                        new ProposalPhaseGrid.Phase("02", "Design",
                                "Brand refinement and website UX/UI design", "2 weeks",
                                "Design system & high-fidelity designs"),
                        new ProposalPhaseGrid.Phase("03", "Develop Prep",
                                "Content structure, developer handoff & asset preparation",
                                "1 week", "Specs, assets & content framework"),
                        new ProposalPhaseGrid.Phase("04", "Launch Support",
                                "QA, training & go-live assistance", "2 weeks",
                                "QA sign-off & successful launch"))))
                .investment(new ProposalInvestment("Investment", "",
                        "ITEM", "AMOUNT (GBP)", List.of(
                        new ProposalInvestment.Row("Discovery & Strategy", "£2,500",
                                ProposalInvestment.Role.NONE),
                        new ProposalInvestment.Row("Brand Identity Refinement", "£4,500",
                                ProposalInvestment.Role.NONE),
                        new ProposalInvestment.Row("Website UX/UI Design", "£7,000",
                                ProposalInvestment.Role.NONE),
                        new ProposalInvestment.Row("Content Structure & Messaging", "£2,000",
                                ProposalInvestment.Role.NONE),
                        new ProposalInvestment.Row("Launch Support", "£1,500",
                                ProposalInvestment.Role.NONE),
                        new ProposalInvestment.Row("Subtotal", "£17,500",
                                ProposalInvestment.Role.SUBTOTAL),
                        new ProposalInvestment.Row("Optional: 3 Months Post-Launch Support",
                                "£1,750", ProposalInvestment.Role.OPTIONAL)),
                        "TOTAL INVESTMENT", "£19,250"))
                .terms(new ProposalTermsBlock("Terms", "", List.of(
                        "50% deposit is required to secure the project and schedule the "
                                + "kick-off.",
                        "Includes two rounds of revisions per major deliverable.",
                        "Final files and handoff delivered upon final payment.",
                        "Ownership of all final deliverables transfers to Astera Health Co. "
                                + "after full payment is received.")))
                .acceptance(new ProposalAcceptance("Acceptance", "",
                        "By signing below, you agree to the scope, timeline, investment, "
                                + "and terms outlined in this proposal.",
                        List.of("Accepted by:", "Signature:", "Date:")))
                .build());
    }
}
