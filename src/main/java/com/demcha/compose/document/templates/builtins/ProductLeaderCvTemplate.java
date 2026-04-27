package com.demcha.compose.document.templates.builtins;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.CvTemplate;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.support.cv.PanelCvTemplateComposer;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.font.FontName;
import com.demcha.compose.document.node.TextAlign;

import java.awt.Color;
import java.util.Objects;

/**
 * Editorial CV variant for product-minded engineers and technical leads.
 */
public final class ProductLeaderCvTemplate implements CvTemplate {
    private final PanelCvTemplateComposer composer;

    public ProductLeaderCvTemplate() {
        this(null);
    }

    public ProductLeaderCvTemplate(CvTheme theme) {
        CvTheme resolvedTheme = Objects.requireNonNullElseGet(theme, ProductLeaderCvTemplate::defaultTheme);
        this.composer = new PanelCvTemplateComposer(
                "ProductLeaderPanelRoot",
                resolvedTheme,
                new PanelCvTemplateComposer.Palette(
                        new Color(231, 246, 244),
                        new Color(20, 44, 66),
                        new Color(54, 68, 84),
                        new Color(244, 249, 248),
                        Color.WHITE,
                        new Color(179, 214, 211),
                        new Color(0, 128, 128),
                        new Color(54, 68, 84)),
                PanelCvTemplateComposer.Layout.stacked(TextAlign.CENTER));
    }

    @Override
    public String getTemplateId() {
        return "product-leader";
    }

    @Override
    public String getTemplateName() {
        return "Product Leader";
    }

    @Override
    public String getDescription() {
        return "A polished editorial CV with product-oriented rhythm, skills grid, and confident blue-green accents.";
    }

    @Override
    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        BuiltInCvTemplateSupport.composeDirect(getTemplateId(), document, documentSpec, composer::compose);
    }

    private static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(20, 44, 66),
                new Color(0, 128, 128),
                new Color(54, 68, 84),
                new Color(0, 128, 128),
                FontName.POPPINS,
                FontName.LATO,
                22,
                10.4,
                9.4,
                3,
                Margin.top(3),
                0);
    }
}
