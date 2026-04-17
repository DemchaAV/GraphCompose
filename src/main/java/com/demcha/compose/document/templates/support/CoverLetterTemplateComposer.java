package com.demcha.compose.document.templates.support;

import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.document.templates.data.Header;
import com.demcha.compose.document.templates.data.JobDetails;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.util.Objects;

/**
 * Shared scene composer for the standard cover-letter template.
 */
public final class CoverLetterTemplateComposer {
    private static final String KIND_REGARDS = "Kind regards,";
    private static final Padding LEGACY_BLOCK_PADDING = new Padding(0, 5, 0, 20);
    private static final Margin LEGACY_HEADER_RIGHT_MARGIN = new Margin(0, 10, 0, 0);

    private final CvTheme theme;
    private final CvTheme signatureTheme;

    public CoverLetterTemplateComposer(CvTheme theme, CvTheme signatureTheme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.signatureTheme = Objects.requireNonNull(signatureTheme, "signatureTheme");
    }

    public void compose(TemplateComposeTarget target, Header header, String wroteLetter, JobDetails jobDetails) {
        target.startDocument("MainVBoxContainer", theme.spacingModuleName());
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
                    LEGACY_HEADER_RIGHT_MARGIN));
        }

        String links = TemplateSceneSupport.joinNonBlank(" | ",
                header.getEmail() == null ? "" : header.getEmail().getDisplayText(),
                header.getLinkedIn() == null ? "" : header.getLinkedIn().getDisplayText(),
                header.getGitHub() == null ? "" : header.getGitHub().getDisplayText());
        if (!links.isBlank()) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "CoverLetterHeaderLinks",
                    links,
                    theme.linkTextStyle(),
                    TextAlign.RIGHT,
                    1.0,
                    Padding.zero(),
                    LEGACY_HEADER_RIGHT_MARGIN));
        }
    }

    private void addBody(TemplateComposeTarget target, String wroteLetter, JobDetails jobDetails) {
        String company = jobDetails == null ? "" : Objects.requireNonNullElse(jobDetails.company(), "");
        String resolved = Objects.requireNonNullElse(wroteLetter, "").replace("${companyName}", company);
        target.addParagraph(TemplateSceneSupport.blockParagraph(
                "CoverLetterBody",
                resolved,
                theme.bodyTextStyle(),
                TextAlign.LEFT,
                theme.spacing(),
                "  ",
                com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy.FIRST_LINE,
                LEGACY_BLOCK_PADDING,
                Margin.zero()));
    }

    private void addClosing(TemplateComposeTarget target, Header header) {
        target.addParagraph(TemplateSceneSupport.paragraph(
                "CoverLetterClosing",
                KIND_REGARDS + "\n" + (header == null ? "" : Objects.requireNonNullElse(header.getName(), "")),
                signatureTheme.bodyTextStyle(),
                TextAlign.RIGHT,
                theme.spacing(),
                Padding.zero(),
                new Margin(20, 20, 0, 0)));
    }
}
