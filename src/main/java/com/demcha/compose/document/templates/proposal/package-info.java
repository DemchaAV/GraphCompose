/**
 * Templates v2 proposal domain — layouts, presets, builder, and spec data types.
 *
 * <p>This package is the home of all proposal / statement-of-work
 * templates in the v2 architecture. Sub-packages partition the domain
 * by concern:</p>
 *
 * <ul>
 *   <li>{@code proposal.layouts} — slot caркасы.</li>
 *   <li>{@code proposal.presets} — flat copy-and-tweak preset classes
 *       (ModernProposal, MinimalProposal).</li>
 *   <li>{@code proposal.builder} — {@code ProposalBuilder} for users
 *       composing their own preset.</li>
 *   <li>{@code proposal.spec} — data records describing the proposal
 *       content.</li>
 * </ul>
 *
 * <p>Sub-packages will be populated during Phase F of the Templates v2
 * migration.</p>
 *
 * @since 1.6.0
 */
package com.demcha.compose.document.templates.proposal;
