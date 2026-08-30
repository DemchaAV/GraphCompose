/**
 * Shared, render-neutral proposal document specs and supporting data
 * records, consumed by the layered {@code proposal.presets} presets.
 *
 * <p>Two document models live here, and a preset consumes the one whose
 * shape it renders: the narrative model
 * ({@link com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec}
 * — a titled run of prose sections with a flat timeline and pricing list)
 * and the structured model
 * ({@link com.demcha.compose.document.templates.data.proposal.StructuredProposalDocumentSpec}
 * — brand marks, fact card, goal cells, numbered scope, phase grid, priced
 * block with row roles, and a signing card, the sections owning their
 * headings and icon tokens).</p>
 */
package com.demcha.compose.document.templates.data.proposal;
