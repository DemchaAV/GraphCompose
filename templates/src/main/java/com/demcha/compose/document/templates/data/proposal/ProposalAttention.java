package com.demcha.compose.document.templates.data.proposal;

import java.util.Objects;

/**
 * The person at the recipient a proposal is addressed to.
 *
 * <p>A sales proposal names a company and then a person inside it, with the
 * person's role and the two ways to reach them. Designs differ in how much of
 * that they set apart — some print the name in the caption, some give the role,
 * the address and the number their own lines — so each is carried separately and
 * a preset draws the ones its design has.</p>
 *
 * @param label the caption above the block, as the sheet prints it (e.g.
 *              {@code "ATTN"}); some designs fold the name into it, which is why
 *              this is the design's own string rather than a fixed word
 * @param name  the person addressed; blank when the label already names them
 * @param role  their role at the recipient; blank when the design omits it
 * @param email their address, as the sheet prints it; blank when absent
 * @param phone their number, as the sheet prints it; blank when absent
 */
public record ProposalAttention(String label, String name, String role,
                                String email, String phone) {

    /**
     * Normalizes optional fields.
     */
    public ProposalAttention {
        label = Objects.requireNonNullElse(label, "");
        name = Objects.requireNonNullElse(name, "");
        role = Objects.requireNonNullElse(role, "");
        email = Objects.requireNonNullElse(email, "");
        phone = Objects.requireNonNullElse(phone, "");
    }

    /**
     * Whether there is anything to draw.
     *
     * @return {@code true} when the block names a person or a way to reach them
     */
    public boolean isPresent() {
        return !name.isBlank() || !role.isBlank() || !email.isBlank() || !phone.isBlank();
    }
}
