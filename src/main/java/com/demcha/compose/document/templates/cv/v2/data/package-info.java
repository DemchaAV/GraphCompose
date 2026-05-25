/**
 * <h2>Layer 1 — pure CV data</h2>
 *
 * <p>This package holds the <strong>data model</strong> of a CV in the
 * v2 architecture. Records here:</p>
 * <ul>
 *   <li>describe the author's content — name, contact, sections;</li>
 *   <li>carry <strong>no</strong> styling, colour, font, or layout
 *       decisions;</li>
 *   <li>never reference {@code DocumentNode}, {@code DocumentColor},
 *       {@code DocumentTextStyle}, or anything from the rendering
 *       runtime.</li>
 * </ul>
 *
 * <p>The point: a CV's <em>content</em> is portable across themes and
 * presets. Rendering decisions live in {@code components} and
 * {@code theme} packages, not here.</p>
 *
 * <h3>Section catalog</h3>
 *
 * <p>The sealed {@link com.demcha.compose.document.templates.cv.v2.data.CvSection}
 * hierarchy intentionally has a small set of concrete
 * shapes — one per genuinely-different structural pattern, not one
 * per visual flavour:</p>
 *
 * <ul>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.data.ParagraphSection}
 *       — single block of prose (Professional Summary, Objective).</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.data.RowsSection}
 *       — list of two-field {@link com.demcha.compose.document.templates.cv.v2.data.CvRow}
 *       items. Visual decoration is picked via the
 *       {@link com.demcha.compose.document.templates.cv.v2.data.RowStyle}
 *       enum so Additional Information (plain) and Projects
 *       (bulleted, two-line) share one record.</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.data.EntriesSection}
 *       — list of timeline {@link com.demcha.compose.document.templates.cv.v2.data.CvEntry}
 *       items with title / subtitle / date / body. Used for Education
 *       and Professional Experience interchangeably.</li>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.data.SkillsSection}
 *       — grouped skills: category plus ordered skill labels. This
 *       keeps skills semantic so presets can render them as tables,
 *       sidebar chips, or inline rows without reparsing text.</li>
 * </ul>
 *
 * <h3>Placement</h3>
 *
 * <p>Sections live inside a {@link com.demcha.compose.document.templates.cv.v2.data.CvDocument}
 * as ordered {@link com.demcha.compose.document.templates.cv.v2.data.CvDocument.Placement}
 * entries. Each placement pairs a section with a
 * {@link com.demcha.compose.document.templates.cv.v2.data.Slot}
 * ({@code MAIN}, {@code SIDEBAR}, {@code FOOTER}). Single-column
 * presets read only {@code MAIN}; multi-column presets read multiple
 * slots. Sections built without an explicit slot default to
 * {@code MAIN}, so existing call sites stay valid.</p>
 *
 * <h3>Adding a new section type</h3>
 *
 * <ol>
 *   <li>Add a new record implementing
 *       {@link com.demcha.compose.document.templates.cv.v2.data.CvSection}.</li>
 *   <li>Add it to the {@code permits} list on {@code CvSection}.</li>
 *   <li>Add a case in
 *       {@code com.demcha.compose.document.templates.cv.v2.components.SectionDispatcher} —
 *       Java 17 makes this an {@code if/else if} branch; the final
 *       else throws so the omission fails loudly.</li>
 * </ol>
 */
package com.demcha.compose.document.templates.cv.v2.data;
