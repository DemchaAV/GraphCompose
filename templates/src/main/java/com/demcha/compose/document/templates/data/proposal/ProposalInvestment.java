package com.demcha.compose.document.templates.data.proposal;

import java.util.List;
import java.util.Objects;

/**
 * The priced block of a structured proposal: an icon-badged heading, the
 * authored table headers, the priced rows with their roles, and the total
 * band.
 *
 * <p>Distinct from the narrative model's {@link ProposalPricingRow}: a row
 * here names its role through {@link Role} — presets are expected to set
 * subtotal and optional rows apart — and the grand total is its own
 * labelled band rather than the last row of the list.</p>
 *
 * @param heading      the section heading
 * @param icon         the icon token of the section badge
 * @param itemHeader   the header label of the item column
 * @param amountHeader the header label of the amount column
 * @param rows         the priced rows, in order
 * @param totalLabel   the label of the total band
 * @param totalAmount  the amount of the total band
 */
public record ProposalInvestment(
        String heading,
        String icon,
        String itemHeader,
        String amountHeader,
        List<Row> rows,
        String totalLabel,
        String totalAmount) {

    /**
     * Normalizes optional fields and freezes the row list.
     */
    public ProposalInvestment {
        heading = Objects.requireNonNullElse(heading, "");
        icon = Objects.requireNonNullElse(icon, "");
        itemHeader = Objects.requireNonNullElse(itemHeader, "");
        amountHeader = Objects.requireNonNullElse(amountHeader, "");
        rows = List.copyOf(Objects.requireNonNullElse(rows, List.of()));
        totalLabel = Objects.requireNonNullElse(totalLabel, "");
        totalAmount = Objects.requireNonNullElse(totalAmount, "");
    }

    /**
     * One priced row.
     *
     * @param label  the row label
     * @param amount the row amount text
     * @param role   the row's role; {@code null} normalizes to
     *               {@link Role#NONE}
     */
    public record Row(String label, String amount, Role role) {

        /**
         * Normalizes optional fields.
         */
        public Row {
            label = Objects.requireNonNullElse(label, "");
            amount = Objects.requireNonNullElse(amount, "");
            role = Objects.requireNonNullElse(role, Role.NONE);
        }
    }

    /**
     * The role of a priced row — named for what the row is, not for how it
     * looks, so a theme change moves the styling without touching the data.
     */
    public enum Role {
        /** An ordinary priced row. */
        NONE,
        /** A subtotal row; presets are expected to set it apart. */
        SUBTOTAL,
        /** An optional line item; presets are expected to mark its amount. */
        OPTIONAL
    }
}
