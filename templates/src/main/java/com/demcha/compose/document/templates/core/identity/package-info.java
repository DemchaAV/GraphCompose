/**
 * <h2>Core — neutral identity (identity)</h2>
 *
 * <p>The family-neutral identity layer shared by every template family. A
 * masthead is the same shape everywhere — a name, an optional tagline, a
 * contact block, and a set of links — so the contract and the widgets that
 * draw it live here rather than in any one family.</p>
 *
 * <ul>
 *   <li>{@link com.demcha.compose.document.templates.core.identity.PartyIdentity}
 *       — the contract a family's identity record implements
 *       ({@code CvIdentity} for a CV, an org record for invoice / proposal).</li>
 *   <li>{@link com.demcha.compose.document.templates.core.identity.Contact} /
 *       {@link com.demcha.compose.document.templates.core.identity.Link}
 *       — the contact-block and labelled-link value records.</li>
 *   <li>{@link com.demcha.compose.document.templates.core.identity.Headline} /
 *       {@link com.demcha.compose.document.templates.core.identity.Subheadline} /
 *       {@link com.demcha.compose.document.templates.core.identity.ContactLine} /
 *       {@link com.demcha.compose.document.templates.core.identity.Masthead}
 *       — the header widgets that render a {@code PartyIdentity}.</li>
 *   <li>{@link com.demcha.compose.document.templates.core.identity.SvgGlyph}
 *       — a small inline-SVG glyph helper used by the header widgets.</li>
 *   <li>{@link com.demcha.compose.document.templates.core.identity.ContactUri}
 *       — turns a printed number, address or site into the target a reader's
 *       device can act on.</li>
 * </ul>
 */
package com.demcha.compose.document.templates.core.identity;
