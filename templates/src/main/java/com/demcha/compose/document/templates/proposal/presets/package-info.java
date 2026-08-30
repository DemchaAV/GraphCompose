/**
 * Layered proposal presets — thin orchestrators that compose a proposal on
 * the canonical DSL.
 *
 * <p>Each preset is a {@code final} class whose {@code create} factory
 * returns a
 * {@link com.demcha.compose.document.templates.api.DocumentTemplate}, and
 * the package holds two shapes of them:</p>
 *
 * <ul>
 *   <li><b>Narrative presets</b> take a {@code create(BrandTheme)} factory
 *       and are parameterised on
 *       {@link com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec},
 *       reading every visual value from the theme
 *       ({@code ModernProposal}).</li>
 *   <li><b>Structured presets</b> take a no-argument {@code create()} and
 *       are parameterised on
 *       {@link com.demcha.compose.document.templates.data.proposal.StructuredProposalDocumentSpec},
 *       carrying their measured geometry and palette as preset-local tokens
 *       because each reproduces one designed look rather than a themeable
 *       family ({@code NorthlineProposal}, {@code EditorialProposal}).</li>
 * </ul>
 *
 * <p>Both shapes reuse the shared {@code templates.core.*} layer; the
 * proposal family does not own a theme type of its own.</p>
 *
 * @since 2.0.0
 */
package com.demcha.compose.document.templates.proposal.presets;
