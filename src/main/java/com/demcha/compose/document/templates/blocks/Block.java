package com.demcha.compose.document.templates.blocks;

/**
 * Sealed value-record hierarchy describing the body content of a
 * Templates v2 module.
 *
 * <p>Each implementation captures the user's data without rendering
 * decisions; the surrounding {@code Module} composer translates a
 * {@code Block} into one or more {@code DocumentNode} instances using
 * the active typography, palette, and spacing tokens.</p>
 *
 * <p>The sealed permit list is intentionally exhaustive: every body
 * shape that a CV / cover-letter / invoice / proposal preset can
 * declare today is one of the eight concrete records. To add a new
 * body shape, extend the {@code permits} list and update the Module
 * composer to handle the new variant.</p>
 *
 * <p>Block records are immutable and safe to reuse across documents.</p>
 */
public sealed interface Block
        permits ParagraphBlock,
                BulletListBlock,
                NumberedListBlock,
                IndentedBlock,
                KeyValueBlock,
                MultiParagraphBlock,
                WorkHistoryBlock,
                EducationBlock {
}
