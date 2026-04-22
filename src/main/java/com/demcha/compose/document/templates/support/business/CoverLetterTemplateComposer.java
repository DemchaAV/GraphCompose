package com.demcha.compose.document.templates.support.business;

import com.demcha.compose.document.templates.support.common.*;

import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.data.coverletter.JobDetails;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.util.List;
import java.util.Objects;

/**
 * Shared scene composer for the standard cover-letter template.
 */
public final class CoverLetterTemplateComposer {
    private static final String KIND_REGARDS = "Kind regards,";
    private static final Margin HEADER_TRAILING_MARGIN = new Margin(0, 10, 0, 0);

    private final CvTheme theme;
    private final CvTheme signatureTheme;
    private final TemplateLayoutPolicy layout;

    public CoverLetterTemplateComposer(CvTheme theme, CvTheme signatureTheme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.signatureTheme = Objects.requireNonNull(signatureTheme, "signatureTheme");
        this.layout = TemplateLayoutPolicy.standardCv(this.theme);
    }

    public void compose(TemplateComposeTarget target, Header header, String wroteLetter, JobDetails jobDetails) {
        target.startDocument("MainVBoxContainer", layout.rootSpacing());
        addHeader(target, header);
        addBody(target, wroteLetter, jobDetails);
        addClosing(target, header);
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
                new Margin(0, 10, 5, 0)));

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
                    HEADER_TRAILING_MARGIN));
        }

        TemplateParagraphSpec linkRow = TemplateHeaderContactSupport.linkRow(
                "CoverLetterHeaderLinks",
                header,
                theme,
                TextAlign.RIGHT,
                HEADER_TRAILING_MARGIN);
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
                        "  ",
                        com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy.FIRST_LINE,
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
                        new Margin(20, 20, 0, 0))))));
    }
}
