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
 * Compact engineering CV with mono-inspired typography and sharp blue accents.
 */
public final class CompactMonoCvTemplate implements CvTemplate {
    private final PanelCvTemplateComposer composer;

    public CompactMonoCvTemplate() {
        this(null);
    }

    public CompactMonoCvTemplate(CvTheme theme) {
        CvTheme resolvedTheme = Objects.requireNonNullElseGet(theme, CompactMonoCvTemplate::defaultTheme);
        this.composer = new PanelCvTemplateComposer(
                "CompactMonoPanelRoot",
                resolvedTheme,
                new PanelCvTemplateComposer.Palette(
                        new Color(22, 27, 34),
                        Color.WHITE,
                        new Color(202, 211, 222),
                        new Color(242, 246, 250),
                        Color.WHITE,
                        new Color(198, 207, 218),
                        new Color(0, 95, 184),
                        new Color(42, 49, 58)),
                PanelCvTemplateComposer.Layout.sidebar(TextAlign.LEFT));
    }

    @Override
    public String getTemplateId() {
        return "compact-mono";
    }

    @Override
    public String getTemplateName() {
        return "Compact Mono";
    }

    @Override
    public String getDescription() {
        return "A dense, technical CV for backend engineers with crisp hierarchy and mono-style metadata.";
    }

    @Override
    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        BuiltInCvTemplateSupport.composeDirect(getTemplateId(), document, documentSpec, composer::compose);
    }

    private static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(22, 27, 34),
                new Color(22, 27, 34),
                new Color(49, 57, 67),
                new Color(0, 95, 184),
                FontName.IBM_PLEX_MONO,
                FontName.HELVETICA,
                23,
                10.4,
                9.3,
                2,
                Margin.top(4),
                0);
    }
}
