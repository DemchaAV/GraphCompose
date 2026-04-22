package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;

/**
 * Display-oriented proposal document input.
 */
public record ProposalData(
        String title,
        String proposalNumber,
        String preparedDate,
        String validUntil,
        String projectTitle,
        String executiveSummary,
        ProposalParty sender,
        ProposalParty recipient,
        List<ProposalSection> sections,
        List<ProposalTimelineItem> timeline,
        List<ProposalPricingRow> pricingRows,
        List<String> acceptanceTerms,
        String footerNote) {

    public ProposalData {
        title = Objects.requireNonNullElse(title, "Proposal");
        proposalNumber = Objects.requireNonNullElse(proposalNumber, "");
        preparedDate = Objects.requireNonNullElse(preparedDate, "");
        validUntil = Objects.requireNonNullElse(validUntil, "");
        projectTitle = Objects.requireNonNullElse(projectTitle, "");
        executiveSummary = Objects.requireNonNullElse(executiveSummary, "");
        sections = List.copyOf(Objects.requireNonNullElse(sections, List.of()));
        timeline = List.copyOf(Objects.requireNonNullElse(timeline, List.of()));
        pricingRows = List.copyOf(Objects.requireNonNullElse(pricingRows, List.of()));
        acceptanceTerms = List.copyOf(Objects.requireNonNullElse(acceptanceTerms, List.of()));
        footerNote = Objects.requireNonNullElse(footerNote, "");
    }
}
