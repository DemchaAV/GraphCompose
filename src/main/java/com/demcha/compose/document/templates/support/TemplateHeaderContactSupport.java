package com.demcha.compose.document.templates.support;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfLinkOptions;
import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.document.templates.data.EmailYaml;
import com.demcha.compose.document.templates.data.Header;
import com.demcha.compose.document.templates.data.LinkYml;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared header-contact helpers for canonical CV-style templates.
 */
final class TemplateHeaderContactSupport {
    private TemplateHeaderContactSupport() {
    }

    static List<TemplateParagraphSpec> linkParagraphs(String namePrefix,
                                                      Header header,
                                                      CvTheme theme,
                                                      Margin margin) {
        List<TemplateParagraphSpec> links = new ArrayList<>(3);
        addHeaderLink(links, namePrefix, 0, displayText(header == null ? null : header.getEmail()), emailLinkOptions(header == null ? null : header.getEmail()), theme, margin);
        addHeaderLink(links, namePrefix, 1, displayText(header == null ? null : header.getLinkedIn()), externalLinkOptions(header == null ? null : header.getLinkedIn()), theme, margin);
        addHeaderLink(links, namePrefix, 2, displayText(header == null ? null : header.getGitHub()), externalLinkOptions(header == null ? null : header.getGitHub()), theme, margin);
        return List.copyOf(links);
    }

    private static void addHeaderLink(List<TemplateParagraphSpec> links,
                                      String namePrefix,
                                      int index,
                                      String text,
                                      PdfLinkOptions linkOptions,
                                      CvTheme theme,
                                      Margin margin) {
        if (text.isBlank() || linkOptions == null) {
            return;
        }
        String name = links.isEmpty() ? namePrefix : namePrefix + "_" + index;
        links.add(TemplateSceneSupport.paragraph(
                name,
                text,
                theme.linkTextStyle(),
                TextAlign.RIGHT,
                1.0,
                linkOptions,
                Padding.zero(),
                margin));
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
