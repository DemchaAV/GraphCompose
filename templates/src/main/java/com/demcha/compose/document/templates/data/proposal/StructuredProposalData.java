package com.demcha.compose.document.templates.data.proposal;

/**
 * Display-oriented input for structured proposal documents.
 *
 * <p>The narrative model ({@link ProposalData}) is a titled run of prose
 * sections; this model is the structured business proposal: brand marks, an
 * authored multi-line title, an at-a-glance fact card, goal cells, a
 * numbered scope list, two deliverable columns, a phase grid, a priced
 * investment block with row roles, a terms block, and a signing card. The
 * sections own their headings and icon tokens (the glance card carries a
 * heading only — its icons live on the individual facts), so the section
 * wording is content, not a preset choice.</p>
 *
 * <p>Both models stay: a preset consumes the one whose shape it renders.
 * Every component normalizes {@code null} to its empty form, so a partial
 * document composes without null checks in preset code. The section records
 * construct positionally; this builder is where a document is assembled.</p>
 *
 * @param brand            the sender's brand marks
 * @param title            the authored title lines
 * @param meta             the prepared-for / prepared-by / date line
 * @param executiveSummary the executive-summary block
 * @param glance           the at-a-glance fact card
 * @param goals            the project-goals band
 * @param scope            the numbered scope-of-work list
 * @param deliverables     the two-column deliverables band
 * @param timeline         the phase grid (the structured counterpart of the
 *                         narrative model's {@code timeline()})
 * @param investment       the priced block
 * @param terms            the bulleted terms block
 * @param acceptance       the signing card
 * @param recipient        the addressed organisation, for the designs that open
 *                         with one; blank when the design names it in its header
 *                         line instead
 * @param attention        the person at the recipient the proposal is addressed
 *                         to; blank when the design addresses no one
 * @param footer           the issuer's own identity as the foot states it;
 *                         blank when the design's foot carries only a name
 */
public record StructuredProposalData(
        ProposalBrand brand,
        ProposalTitleLines title,
        ProposalMetaLine meta,
        ProposalSummaryBlock executiveSummary,
        ProposalGlance glance,
        ProposalGoals goals,
        ProposalScope scope,
        ProposalDeliverables deliverables,
        ProposalPhaseGrid timeline,
        ProposalInvestment investment,
        ProposalTermsBlock terms,
        ProposalAcceptance acceptance,
        ProposalRecipient recipient,
        ProposalAttention attention,
        ProposalFooter footer) {

    /**
     * Normalizes absent components to their empty forms.
     */
    public StructuredProposalData {
        brand = brand == null
                ? new ProposalBrand(null, null, null, null, null, null) : brand;
        title = title == null
                ? new ProposalTitleLines(null, null, null) : title;
        meta = meta == null
                ? new ProposalMetaLine(null, null, null) : meta;
        executiveSummary = executiveSummary == null
                ? new ProposalSummaryBlock(null, null, null) : executiveSummary;
        glance = glance == null
                ? new ProposalGlance(null, null) : glance;
        goals = goals == null
                ? new ProposalGoals(null, null, null) : goals;
        scope = scope == null
                ? new ProposalScope(null, null, null) : scope;
        deliverables = deliverables == null
                ? new ProposalDeliverables(null, null, null, null) : deliverables;
        timeline = timeline == null
                ? new ProposalPhaseGrid(null, null, null, null) : timeline;
        investment = investment == null
                ? new ProposalInvestment(null, null, null, null, null, null, null) : investment;
        terms = terms == null
                ? new ProposalTermsBlock(null, null, null) : terms;
        acceptance = acceptance == null
                ? new ProposalAcceptance(null, null, null, null) : acceptance;
        recipient = recipient == null
                ? new ProposalRecipient(null, null, null) : recipient;
        attention = attention == null
                ? new ProposalAttention(null, null, null, null, null) : attention;
        footer = footer == null
                ? new ProposalFooter(null, null, null, null) : footer;
    }

    /**
     * Backward-compatible constructor for callers that predate the addressed
     * block, the attention line and the foot's own identity.
     *
     * @param brand            the issuing brand
     * @param title            the proposal's title
     * @param meta             the header facts
     * @param executiveSummary the summary block
     * @param glance           the at-a-glance facts
     * @param goals            the goals block
     * @param scope            the scope block
     * @param deliverables     the deliverables block
     * @param timeline         the phase grid
     * @param investment       the investment table
     * @param terms            the terms block
     * @param acceptance       the signing card
     */
    public StructuredProposalData(ProposalBrand brand, ProposalTitleLines title,
                                  ProposalMetaLine meta,
                                  ProposalSummaryBlock executiveSummary,
                                  ProposalGlance glance, ProposalGoals goals,
                                  ProposalScope scope, ProposalDeliverables deliverables,
                                  ProposalPhaseGrid timeline, ProposalInvestment investment,
                                  ProposalTermsBlock terms, ProposalAcceptance acceptance) {
        this(brand, title, meta, executiveSummary, glance, goals, scope, deliverables,
                timeline, investment, terms, acceptance, null, null, null);
    }

    /**
     * Starts a fluent structured proposal data builder.
     *
     * @return structured proposal data builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for complete structured proposal content.
     */
    public static final class Builder {
        private ProposalBrand brand;
        private ProposalTitleLines title;
        private ProposalMetaLine meta;
        private ProposalSummaryBlock executiveSummary;
        private ProposalGlance glance;
        private ProposalGoals goals;
        private ProposalScope scope;
        private ProposalDeliverables deliverables;
        private ProposalPhaseGrid timeline;
        private ProposalInvestment investment;
        private ProposalTermsBlock terms;
        private ProposalAcceptance acceptance;
        private ProposalRecipient recipient;
        private ProposalAttention attention;
        private ProposalFooter footer;

        private Builder() {
        }

        /**
         * Sets the sender's brand marks.
         *
         * @param brand brand marks
         * @return this builder
         */
        public Builder brand(ProposalBrand brand) {
            this.brand = brand;
            return this;
        }

        /**
         * Sets the authored title lines.
         *
         * @param title title lines
         * @return this builder
         */
        public Builder title(ProposalTitleLines title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the prepared-for / prepared-by / date line.
         *
         * @param meta meta line
         * @return this builder
         */
        public Builder meta(ProposalMetaLine meta) {
            this.meta = meta;
            return this;
        }

        /**
         * Sets the executive-summary block.
         *
         * @param executiveSummary summary block
         * @return this builder
         */
        public Builder executiveSummary(ProposalSummaryBlock executiveSummary) {
            this.executiveSummary = executiveSummary;
            return this;
        }

        /**
         * Sets the at-a-glance fact card.
         *
         * @param glance fact card
         * @return this builder
         */
        public Builder glance(ProposalGlance glance) {
            this.glance = glance;
            return this;
        }

        /**
         * Sets the project-goals band.
         *
         * @param goals goals band
         * @return this builder
         */
        public Builder goals(ProposalGoals goals) {
            this.goals = goals;
            return this;
        }

        /**
         * Sets the numbered scope-of-work list.
         *
         * @param scope scope list
         * @return this builder
         */
        public Builder scope(ProposalScope scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Sets the two-column deliverables band.
         *
         * @param deliverables deliverables band
         * @return this builder
         */
        public Builder deliverables(ProposalDeliverables deliverables) {
            this.deliverables = deliverables;
            return this;
        }

        /**
         * Sets the phase grid.
         *
         * @param timeline phase grid
         * @return this builder
         */
        public Builder timeline(ProposalPhaseGrid timeline) {
            this.timeline = timeline;
            return this;
        }

        /**
         * Sets the priced block.
         *
         * @param investment priced block
         * @return this builder
         */
        public Builder investment(ProposalInvestment investment) {
            this.investment = investment;
            return this;
        }

        /**
         * Sets the bulleted terms block.
         *
         * @param terms terms block
         * @return this builder
         */
        public Builder terms(ProposalTermsBlock terms) {
            this.terms = terms;
            return this;
        }

        /**
         * Sets the signing card.
         *
         * @param acceptance signing card
         * @return this builder
         */
        public Builder acceptance(ProposalAcceptance acceptance) {
            this.acceptance = acceptance;
            return this;
        }

        /**
         * Sets the addressed organisation.
         *
         * @param recipient the addressed organisation
         * @return this builder
         */
        public Builder recipient(ProposalRecipient recipient) {
            this.recipient = recipient;
            return this;
        }

        /**
         * Sets the person at the recipient the proposal is addressed to.
         *
         * @param attention the addressed person
         * @return this builder
         */
        public Builder attention(ProposalAttention attention) {
            this.attention = attention;
            return this;
        }

        /**
         * Sets the issuer's own identity, as the foot states it.
         *
         * @param footer the issuer's identity
         * @return this builder
         */
        public Builder footer(ProposalFooter footer) {
            this.footer = footer;
            return this;
        }

        /**
         * Builds the normalized structured proposal data.
         *
         * @return structured proposal data
         */
        public StructuredProposalData build() {
            return new StructuredProposalData(brand, title, meta, executiveSummary,
                    glance, goals, scope, deliverables, timeline, investment,
                    terms, acceptance, recipient, attention, footer);
        }
    }
}
