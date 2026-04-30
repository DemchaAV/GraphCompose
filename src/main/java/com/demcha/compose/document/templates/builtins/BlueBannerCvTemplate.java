package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.BlueBannerCvTemplateComposer;
import com.demcha.compose.document.templates.theme.CvTheme;

/**
 * Conventional resume with a framed centered headline, pipe-delimited
 * contact line, and one light-blue banner per section bracketed by thin
 * black rules.
 *
 * <p>Modernised under v1.5: optionally accepts a {@link CvTheme} so the
 * body type, contact line, and work-entry colours can be re-skinned via
 * {@code CvTheme.fromBusinessTheme(BusinessTheme)} while the blue
 * banner identity stays template-owned.</p>
 */
public final class BlueBannerCvTemplate implements CvTemplate {
    private final BlueBannerCvTemplateComposer composer;

    /**
     * Default constructor — uses the conventional dark-grey ink + Lato
     * body theme that the original Word-style template shipped with.
     */
    public BlueBannerCvTemplate() {
        this.composer = new BlueBannerCvTemplateComposer();
    }

    /**
     * Constructs the template with a custom {@link CvTheme}. The
     * blue banner colour stays template-owned regardless of theme.
     *
     * @param theme CV theme driving body type and accent colour
     */
    public BlueBannerCvTemplate(CvTheme theme) {
        this.composer = new BlueBannerCvTemplateComposer(theme);
    }

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
