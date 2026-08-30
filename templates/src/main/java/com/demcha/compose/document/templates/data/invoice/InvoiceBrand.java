package com.demcha.compose.document.templates.data.invoice;

import com.demcha.compose.document.image.DocumentImageData;

import java.util.Objects;

/**
 * The sender's brand lockup on a structured invoice: the logo image and
 * the wordmark lines beside it.
 *
 * <p>The logo is caller-supplied content, not template chrome — the caller
 * hands over a {@link DocumentImageData} built from its own bytes or path,
 * the way it would for any other image it owns. It is optional: with no
 * logo a preset falls back to the wordmark alone, so an invoice composes
 * without one.</p>
 *
 * @param logo      the logo image, or {@code null} for the wordmark-only
 *                  lockup
 * @param name      the brand name, set as the wordmark
 * @param qualifier the second wordmark line under the name (e.g. the
 *                  business type)
 * @param tagline   the tagline under the lockup
 */
public record InvoiceBrand(
        DocumentImageData logo,
        String name,
        String qualifier,
        String tagline) {

    /**
     * Normalizes the optional text fields; the logo stays nullable.
     */
    public InvoiceBrand {
        name = Objects.requireNonNullElse(name, "");
        qualifier = Objects.requireNonNullElse(qualifier, "");
        tagline = Objects.requireNonNullElse(tagline, "");
    }

    /**
     * Whether this lockup carries a logo image.
     *
     * @return {@code true} when a logo was supplied
     */
    public boolean hasLogo() {
        return logo != null;
    }
}
