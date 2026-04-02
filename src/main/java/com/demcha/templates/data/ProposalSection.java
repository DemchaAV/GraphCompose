package com.demcha.templates.data;

import java.util.List;
import java.util.Objects;

/**
 * Display-oriented proposal section with paragraph content.
 */
public record ProposalSection(
        String title,
        List<String> paragraphs) {

    public ProposalSection {
        title = Objects.requireNonNullElse(title, "");
        paragraphs = List.copyOf(Objects.requireNonNullElse(paragraphs, List.of()));
    }
}
