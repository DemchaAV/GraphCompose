package com.demcha.compose.document.templates.support.common;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.templates.data.common.EmailYaml;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.data.common.LinkYml;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared header-contact helpers for canonical CV-style templates.
 *
 * @author Artem Demchyshyn
 */
public final class TemplateHeaderContactSupport {
    private TemplateHeaderContactSupport() {
    }

    public static TemplateParagraphSpec linkRow(String name,
                                                Header header,
                                                CvTheme theme,
                                                TextAlign align,
                                                Margin margin) {
        List<InlineTextRun> runs = new ArrayList<>(5);
        addHeaderLink(runs, displayText(header == null ? null : header.getEmail()), emailLinkOptions(header == null ? null : header.getEmail()), theme);
        addHeaderLink(runs, displayText(header == null ? null : header.getLinkedIn()), externalLinkOptions(header == null ? null : header.getLinkedIn()), theme);
        addHeaderLink(runs, displayText(header == null ? null : header.getGitHub()), externalLinkOptions(header == null ? null : header.getGitHub()), theme);
        if (runs.isEmpty()) {
            return null;
        }
        return TemplateSceneSupport.inlineParagraph(
                name,
                runs,
                theme.linkTextStyle(),
                align,
                1.0,
                Padding.zero(),
                margin);
    }

    private static void addHeaderLink(List<InlineTextRun> runs,
                                      String text,
                                      PdfLinkOptions linkOptions,
                                      CvTheme theme) {
        if (text.isBlank() || linkOptions == null) {
            return;
        }
        if (!runs.isEmpty()) {
            runs.add(new InlineTextRun(" | ", theme.smallBodyTextStyle(), null));
        }
        runs.add(new InlineTextRun(text, theme.linkTextStyle(), linkOptions));
    }

    private static PdfLinkOptions emailLinkOptions(EmailYaml email) {
        if (email == null) {
            return null;
        }
        String to = safe(email.getTo());
        if (to.isBlank()) {
            return null;
        }

        List<String> queryParts = new ArrayList<>(2);
        appendMailQuery(queryParts, "subject", safe(email.getSubject()));
        appendMailQuery(queryParts, "body", safe(email.getBody()));
        String query = queryParts.isEmpty() ? "" : "?" + String.join("&", queryParts);
        return new PdfLinkOptions("mailto:" + to + query);
    }

    private static PdfLinkOptions externalLinkOptions(LinkYml link) {
        if (link == null || link.getLinkUrl() == null || !link.getLinkUrl().isValid()) {
            return null;
        }
        String uri = safe(link.getLinkUrl().getUrl());
        return uri.isBlank() ? null : new PdfLinkOptions(uri);
    }

    private static void appendMailQuery(List<String> queryParts, String key, String value) {
        if (value.isBlank()) {
            return;
        }
        queryParts.add(key + "=" + encodeQueryValue(value));
    }

    private static String displayText(EmailYaml email) {
        if (email == null) {
            return "";
        }
        String display = safe(email.getDisplayText());
        return display.isBlank() ? safe(email.getTo()) : display;
    }

    private static String displayText(LinkYml link) {
        if (link == null) {
            return "";
        }
        String display = safe(link.getDisplayText());
        if (!display.isBlank()) {
            return display;
        }
        if (link.getLinkUrl() == null) {
            return "";
        }
        return safe(link.getLinkUrl().getUrl());
    }

    private static String encodeQueryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
