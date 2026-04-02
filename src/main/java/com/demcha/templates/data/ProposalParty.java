package com.demcha.templates.data;

import java.util.List;
import java.util.Objects;

/**
 * Display-oriented proposal party details.
 */
public record ProposalParty(
        String name,
        List<String> addressLines,
        String email,
        String phone,
        String website) {

    public ProposalParty {
        name = Objects.requireNonNullElse(name, "");
        addressLines = List.copyOf(Objects.requireNonNullElse(addressLines, List.of()));
        email = Objects.requireNonNullElse(email, "");
        phone = Objects.requireNonNullElse(phone, "");
        website = Objects.requireNonNullElse(website, "");
    }
}
