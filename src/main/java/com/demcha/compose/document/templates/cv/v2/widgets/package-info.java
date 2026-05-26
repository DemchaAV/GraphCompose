/**
 * <h2>Visual widgets — reusable LEGO bricks for presets</h2>
 *
 * <p>Widgets sit <strong>between</strong> the raw document DSL
 * (addParagraph / softPanel / accentBottom) and CV-specific
 * renderers in {@code components/}. Each widget captures one
 * <em>visual idea</em> (a headline, a contact line, a section
 * header) with a small set of named variants, so a preset author
 * composes a page by <strong>picking widgets</strong> instead of
 * writing rendering DSL by hand.</p>
 *
 * <h3>Three layers of customisation per widget</h3>
 *
 * <ol>
 *   <li><strong>Convenience factory</strong> — one line, no params
 *       beyond {@code (host, content, theme)}. Covers the common
 *       case. Example: {@code Headline.spacedCentered(host, name, theme)}.</li>
 *   <li><strong>Lower-level render</strong> — when you need to vary
 *       one knob (alignment, decoration, colour). Example:
 *       {@code Headline.render(host, name, theme, TextAlign.RIGHT, false)}.</li>
 *   <li><strong>Inline DSL</strong> — when no widget fits. Widgets
 *       are <em>optional</em>; the host {@link com.demcha.compose.document.dsl.SectionBuilder}
 *       always accepts direct {@code addParagraph} / {@code addRow}
 *       calls. Don't fight the widget — just bypass it.</li>
 * </ol>
 *
 * <h3>When to add a new widget</h3>
 *
 * <p>Don't predict. Extract.</p>
 *
 * <ul>
 *   <li>Same inline rendering in <strong>1 preset</strong> → keep
 *       inline.</li>
 *   <li>Same inline rendering in <strong>2 presets</strong> →
 *       extract a new factory method on an existing widget, or
 *       add a parameter.</li>
 *   <li>Same idea in <strong>3+ presets</strong> → it's a new
 *       widget; give it its own file.</li>
 * </ul>
 *
 * <h3>Current widget catalog</h3>
 *
 * <ul>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.widgets.Headline}
 *       — top-of-document name in 4 variants
 *       ({@code spacedCentered}, {@code uppercaseCentered},
 *       {@code uppercaseLeftAligned}, {@code rightAligned}).</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.widgets.Subheadline}
 *       — secondary tagline under the name in 1 variant
 *       ({@code centeredSpacedCaps}).</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.widgets.ContactLine}
 *       — contact + links row/stack in 5 variants
 *       ({@code centered}, {@code rightAligned},
 *       {@code leftAligned}, {@code rightAlignedStacked},
 *       {@code twoRowRightAligned}).</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.widgets.Masthead}
 *       — centred editorial identity block: name, optional title,
 *       compact metadata, and link row.</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.widgets.SectionHeader}
 *       — section title in 8 variants ({@code banner},
 *       {@code fullWidthBanner}, {@code underlined}, {@code flat},
 *       {@code flatSpacedCaps}, {@code tickLabel},
 *       {@code upperRule}, {@code spacedCapsRule}).</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.widgets.FlowSectionHeader}
 *       — page-flow-level section headers where rules live outside
 *       the section body.</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.widgets.ProfileBand}
 *       — tinted/ruled summary block with markdown-aware body text.</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.widgets.SectionModule}
 *       — named module wrapper that pairs a {@code SectionHeader}
 *       variant with caller-supplied body content.</li>
 * </ul>
 *
 * <p>Generic widgets that are useful beyond CVs live in
 * {@link com.demcha.compose.document.templates.widgets}; for example
 * {@link com.demcha.compose.document.templates.widgets.TableWidget}
 * provides configurable fixed-column and grid tables with border,
 * fill, zebra, padding, and typography options, while
 * {@link com.demcha.compose.document.templates.widgets.CardWidget}
 * provides a reusable styled card/container shell.</p>
 *
 * <p>Each widget delegates internally to the lower-level renderers
 * in {@code cv/v2/components/} where helpful, but its public face
 * is the small set of factory methods above.</p>
 */
package com.demcha.compose.document.templates.cv.v2.widgets;
