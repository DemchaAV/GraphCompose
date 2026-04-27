package com.demcha.compose.document.output;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Backend-neutral document protection (passwords + permissions).
 *
 * <p>The PDF backend translates these options into PDFBox encryption settings;
 * other backends may simply ignore protection that they do not support.</p>
 *
 * @author Artem Demchyshyn
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocumentProtection {
    private final String userPassword;
    private final String ownerPassword;

    @Builder.Default
    private final boolean canPrint = true;

    @Builder.Default
    private final boolean canCopyContent = true;

    @Builder.Default
    private final boolean canModify = false;

    @Builder.Default
    private final boolean canFillForms = true;

    @Builder.Default
    private final boolean canExtractForAccessibility = true;

    @Builder.Default
    private final boolean canAssemble = false;

    @Builder.Default
    private final boolean canPrintHighQuality = true;

    @Builder.Default
    private final int keyLength = 128;

    private DocumentProtection() {
        this.userPassword = null;
        this.ownerPassword = null;
        this.canPrint = true;
        this.canCopyContent = true;
        this.canModify = false;
        this.canFillForms = true;
        this.canExtractForAccessibility = true;
        this.canAssemble = false;
        this.canPrintHighQuality = true;
        this.keyLength = 128;
    }
}
