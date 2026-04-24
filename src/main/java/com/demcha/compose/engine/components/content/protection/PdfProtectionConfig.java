package com.demcha.compose.engine.components.content.protection;

import com.demcha.compose.engine.components.core.Component;
import lombok.Builder;
import lombok.Getter;

/**
 * PDF access permission flags.
 *
 * <p>These map to the standard PDF permission bits. When applied, the
 * document is encrypted and the specified permissions control what
 * viewers and tools can do with the file.</p>
 *
 * @author Artem Demchyshyn
 */
@Getter
@Builder
public final class PdfProtectionConfig implements Component {

    /** Password required to open the document (null = no user password). */
    private final String userPassword;

    /** Password required for full access / permission changes. */
    private final String ownerPassword;

    /** Allow printing the document. */
    @Builder.Default
    private final boolean canPrint = true;

    /** Allow copying text and images from the document. */
    @Builder.Default
    private final boolean canCopyContent = true;

    /** Allow modifying the document. */
    @Builder.Default
    private final boolean canModify = false;

    /** Allow filling in form fields. */
    @Builder.Default
    private final boolean canFillForms = true;

    /** Allow extracting text for accessibility purposes. */
    @Builder.Default
    private final boolean canExtractForAccessibility = true;

    /** Allow assembling (insert, rotate, delete pages). */
    @Builder.Default
    private final boolean canAssemble = false;

    /** Allow high-quality printing. */
    @Builder.Default
    private final boolean canPrintHighQuality = true;

    /** Encryption key length in bits (128 or 256). */
    @Builder.Default
    private final int keyLength = 128;
}
