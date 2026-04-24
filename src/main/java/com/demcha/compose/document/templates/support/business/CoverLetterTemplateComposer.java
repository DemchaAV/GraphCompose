package com.demcha.compose.document.templates.support.business;

import com.demcha.compose.document.templates.support.common.*;

import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.data.coverletter.CoverLetterDocumentSpec;
import com.demcha.compose.document.templates.data.coverletter.JobDetails;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.util.List;
import java.util.Objects;

/**
 * Shared scene composer for the standard cover-letter template.
 */
public final class CoverLetterTemplateComposer {
    private static final String KIND_REGARDS = "Kind regards,";

    private final CvTheme theme;
    private final CvTheme signatureTheme;
    private final CoverLetterLayoutPolicy coverLetterLayout;
    private final TemplateLayoutPolicy layout;

    /**
     * Creates a cover-letter scene composer.
     *
     * @param theme main letter theme
     * @param signatureTheme theme used for the closing/signature block
     */
    public CoverLetterTemplateComposer(CvTheme theme, CvTheme signatureTheme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.signatureTheme = Objects.requireNonNull(signatureTheme, "signatureTheme");
        this.coverLetterLayout = CoverLetterLayoutPolicy.standard(this.theme);
        this.layout = coverLetterLayout.rhythm();
    }

    /**
     * Composes a cover-letter spec into the active canonical target.
     *
     * @param target active template compose target
     * @param spec cover-letter document spec
     */
    public void compose(TemplateComposeTarget target, CoverLetterDocumentSpec spec) {
        CoverLetterDocumentSpec safe = Objects.requireNonNull(spec, "spec");
        target.startDocument("MainVBoxContainer", layout.rootSpacing());
        addHeader(target, safe.header());
        addBody(target, safe.body(), safe.jobDetails());
        addClosing(target, safe.header());
        target.finishDocument();
    }

    private void addHeader(TemplateComposeTarget target, Header header) {
        if (header == null) {
            return;
        }
        target.addParagraph(TemplateSceneSupport.paragraph(
                "CoverLetterHeaderName",
                Objects.requireNonNullElse(header.getName(), ""),
                theme.nameTextStyle(),
                TextAlign.RIGHT,
                1.0,
                Padding.zero(),
                coverLetterLayout.headerNameMargin()));

        String info = TemplateSceneSupport.joinNonBlank(" | ",
                header.getAddress(),
                header.getPhoneNumber());
        if (!info.isBlank()) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "CoverLetterHeaderInfo",
                    info,
                    theme.smallBodyTextStyle(),
                    TextAlign.RIGHT,
                    1.0,
                    Padding.zero(),
                    coverLetterLayout.headerTrailingMargin()));
        }

        TemplateParagraphSpec linkRow = TemplateHeaderContactSupport.linkRow(
                "CoverLetterHeaderLinks",
                header,
                theme,
                TextAlign.RIGHT,
                coverLetterLayout.headerTrailingMargin());
        if (linkRow != null) {
            target.addParagraph(linkRow);
        }
    }

    private void addBody(TemplateComposeTarget target, String wroteLetter, JobDetails jobDetails) {
        String company = jobDetails == null ? "" : Objects.requireNonNullElse(jobDetails.company(), "");
        String resolved = Objects.requireNonNullElse(wroteLetter, "").replace("${companyName}", company);
        target.addModule(new TemplateModuleSpec(
                "CoverLetterBodyModule",
                null,
                List.of(TemplateModuleBlock.paragraph(TemplateSceneSupport.blockParagraph(
                        "CoverLetterBody",
                        resolved,
                        theme.bodyTextStyle(),
                        TextAlign.LEFT,
                        layout.bodyLineSpacing(),
                        coverLetterLayout.bodyFirstLineIndent(),
                        com.demcha.compose.engine.components.content.text.TextIndentStrategy.FIRST_LINE,
                        layout.bodyPadding(),
                        Margin.zero())))));
    }

    private void addClosing(TemplateComposeTarget target, Header header) {
        target.addModule(new TemplateModuleSpec(
                "CoverLetterClosingModule",
                null,
                List.of(TemplateModuleBlock.paragraph(TemplateSceneSupport.paragraph(
                        "CoverLetterClosing",
                        KIND_REGARDS + "\n" + (header == null ? "" : Objects.requireNonNullElse(header.getName(), "")),
                        signatureTheme.bodyTextStyle(),
                        TextAlign.RIGHT,
                        layout.bodyLineSpacing(),
                        Padding.zero(),
                        coverLetterLayout.closingMargin())))));
    }
}
