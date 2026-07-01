/**
 * Layered proposal presets — thin orchestrators that compose a proposal
 * on a {@link com.demcha.compose.document.templates.core.theme.BrandTheme}
 * plus the canonical DSL.
 *
 * <p>This package mirrors {@code invoice.presets}: each preset is a
 * {@code final} class with a {@code create(BrandTheme)} factory returning
 * a {@link com.demcha.compose.document.templates.api.DocumentTemplate}
 * parameterised on
 * {@link com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec}.
 * The presets read every visual value from the theme and reuse the shared
 * {@code templates.core.*} layer; the proposal family does not own a theme
 * type of its own.</p>
 *
 * @since 2.0.0
 */
package com.demcha.compose.document.templates.proposal.v2.presets;
