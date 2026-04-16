package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.MainPageCV;
import com.demcha.compose.document.templates.data.MainPageCvDTO;
import com.demcha.compose.document.templates.support.LegacyTemplateMappers;
import com.demcha.compose.document.templates.support.LegacyTemplateSessionRenderer;
import com.demcha.compose.document.templates.theme.CvTheme;

import java.util.Objects;

/**
 * Canonical V2 implementation of the standard CV template.
 */
public final class CvTemplateV1 implements CvTemplate {
    private final com.demcha.templates.builtins.CvTemplateV1 legacyBridge;

    public CvTemplateV1() {
        this(CvTheme.defaultTheme());
    }

    public CvTemplateV1(CvTheme theme) {
        this.legacyBridge = new com.demcha.templates.builtins.CvTemplateV1(
                LegacyTemplateMappers.toLegacy(Objects.requireNonNullElseGet(theme, CvTheme::defaultTheme)));
    }

    @Override
    public String getTemplateId() {
        return "modern-professional";
    }

    @Override
    public String getTemplateName() {
        return "Modern Professional";
    }

    @Override
    public String getDescription() {
        return "A clean, professional template with centered header and well-organized sections.";
    }

    @Override
    public void compose(DocumentSession document, MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        LegacyTemplateSessionRenderer.renderInto(document, composer -> legacyBridge.compose(
                composer,
                LegacyTemplateMappers.toLegacy(originalCv),
                LegacyTemplateMappers.toLegacy(rewrittenCv)));
    }
}
