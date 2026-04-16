package com.demcha.compose.document.templates.support;

import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.document.templates.data.Header;
import com.demcha.compose.document.templates.data.JobDetails;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Shared scene composer for the standard cover-letter template.
 */
public final class CoverLetterTemplateComposer {
    private static final String KIND_REGARDS = "Kind regards,";

    private final CvTheme theme;
    private final CvTheme signatureTheme;

    public CoverLetterTemplateComposer(CvTheme theme, CvTheme signatureTheme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.signatureTheme = Objects.requireNonNull(signatureTheme, "signatureTheme");
    }

    public void compose(TemplateComposeTarget target, Header header, String wroteLetter, JobDetails jobDetails) {
        target.startDocument("MainVBoxContainer", theme.spacing());
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
                TextAlign.CENTER,
                1.0,
                Padding.zero(),
                Margin.bottom(5)));

        String info = TemplateSceneSupport.joinNonBlank(" | ",
                header.getAddress(),
                header.getPhoneNumber(),
                header.getEmail() == null ? "" : header.getEmail().getDisplayText());
        if (!info.isBlank()) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "CoverLetterHeaderInfo",
                    info,
                    theme.smallBodyTextStyle(),
                    TextAlign.CENTER,
                    1.0,
                    Padding.zero(),
                    Margin.zero()));
        }
    }

    private void addBody(TemplateComposeTarget target, String wroteLetter, JobDetails jobDetails) {
        String company = jobDetails == null ? "" : Objects.requireNonNullElse(jobDetails.company(), "");
        String resolved = Objects.requireNonNullElse(wroteLetter, "").replace("${companyName}", company);
        List<String> paragraphs = Arrays.stream(resolved.replace("\r\n", "\n").split("\\n\\s*\\n"))
                .map(TemplateSceneSupport::stripBasicMarkdown)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        for (int index = 0; index < paragraphs.size(); index++) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "CoverLetterBody_" + index,
                    paragraphs.get(index),
                    theme.bodyTextStyle(),
                    TextAlign.LEFT,
                    2.0,
                    Padding.zero(),
                    index == 0 ? Margin.top(12) : Margin.top(8)));
        }
    }

    private void addClosing(TemplateComposeTarget target, Header header) {
        target.addParagraph(TemplateSceneSupport.paragraph(
                "CoverLetterClosing",
                KIND_REGARDS,
                signatureTheme.bodyTextStyle(),
                TextAlign.RIGHT,
                1.0,
                Padding.zero(),
                Margin.top(18)));
        target.addParagraph(TemplateSceneSupport.paragraph(
                "CoverLetterSignature",
                header == null ? "" : Objects.requireNonNullElse(header.getName(), ""),
                signatureTheme.bodyTextStyle(),
                TextAlign.RIGHT,
                1.0,
                Padding.zero(),
                Margin.top(4)));
    }
}
