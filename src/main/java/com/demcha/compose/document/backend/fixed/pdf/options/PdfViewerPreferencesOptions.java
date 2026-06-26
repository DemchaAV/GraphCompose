package com.demcha.compose.document.backend.fixed.pdf.options;

import com.demcha.compose.document.style.DocumentPageLayout;
import com.demcha.compose.document.style.DocumentPageMode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * PDF-flavoured viewer preferences applied to the document catalog (page mode,
 * page layout, and window-chrome flags). The PDF-vocabulary mapping to PDFBox is
 * done where these are applied; every field is optional ({@code null} leaves the
 * reader default).
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PdfViewerPreferencesOptions {
    private final DocumentPageMode pageMode;
    private final DocumentPageLayout pageLayout;
    private final Boolean displayDocTitle;
    private final Boolean hideToolbar;
    private final Boolean hideMenubar;
    private final Boolean fitWindow;
    private final Boolean centerWindow;
}
