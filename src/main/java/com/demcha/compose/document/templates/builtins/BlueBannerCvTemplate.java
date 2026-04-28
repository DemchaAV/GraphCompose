package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.BlueBannerCvTemplateComposer;

/**
 * Conventional resume with a framed centered headline, pipe-delimited
 * contact line, and one light-blue banner per section bracketed by thin
 * black rules.
 */
public final class BlueBannerCvTemplate implements CvTemplate {
    private final BlueBannerCvTemplateComposer composer = new BlueBannerCvTemplateComposer();

    @Override
    public String getTemplateId() {
        return "blue-banner";
    }

    @Override
    public String getTemplateName() {
        return "Blue Banner";
    }

    @Override
    public String getDescription() {
        return "A classic Word resume with a framed name, pipe-delimited contact line, and light-blue section banners.";
    }

    @Override
    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        BuiltInCvTemplateSupport.composeDirect(getTemplateId(), document, documentSpec, composer::compose);
    }
}
