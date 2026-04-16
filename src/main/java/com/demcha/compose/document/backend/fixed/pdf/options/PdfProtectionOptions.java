package com.demcha.compose.document.backend.fixed.pdf.options;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
/**
 * Canonical PDF encryption and access-permission options.
 *
 * <p>The options affect the final PDF container only. They do not participate
 * in layout compilation.</p>
 */
public final class PdfProtectionOptions {
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
}
