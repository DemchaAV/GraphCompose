package com.demcha.compose.document.templates.support.cv;

import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.templates.data.common.EmailYaml;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.data.common.LinkYml;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.data.cv.CvModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ProfessionalCvTemplateSupport {
    private ProfessionalCvTemplateSupport() {
    }

    static CvModule findModule(CvDocumentSpec spec, String... keys) {
        if (spec == null || spec.modules() == null) {
            return null;
        }
        for (CvModule module : spec.modules()) {
            String normalized = normalize(safe(module.name()) + " " + safe(module.title()));
            for (String key : keys) {
                if (normalized.contains(normalize(key))) {
                    return module;
                }
            }
        }
        return null;
    }

    static List<String> moduleLines(CvModule module) {
        if (module == null) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (CvModule.BodyBlock block : module.bodyBlocks()) {
            switch (block.kind()) {
                case PARAGRAPH -> addLines(lines, block.text());
                case LIST -> block.items().forEach(item -> addLines(lines, item));
                default -> {
                    // Professional CV variants render the plain narrative blocks.
                }
            }
        }
        return List.copyOf(lines);
    }

    static List<ContactPart> contactParts(Header header) {
        if (header == null) {
            return List.of();
        }
        List<ContactPart> parts = new ArrayList<>();
        addPart(parts, safe(header.getAddress()), null);
        addPart(parts, safe(header.getPhoneNumber()), null);
        if (header.getEmail() != null) {
            addPart(parts, emailDisplay(header.getEmail()), emailLink(header.getEmail()));
        }
        if (header.getLinkedIn() != null) {
            addPart(parts, linkDisplay(header.getLinkedIn()), linkOptions(header.getLinkedIn()));
        }
        if (header.getGitHub() != null) {
            addPart(parts, linkDisplay(header.getGitHub()), linkOptions(header.getGitHub()));
        }
        return List.copyOf(parts);
    }

    static WorkEntry parseWorkEntry(String raw) {
        String item = stripMarkdown(raw);
        int pipeIndex = item.indexOf('|');
        if (pipeIndex < 0) {
            return new WorkEntry(item, "", "", "");
        }

        String headingText = item.substring(0, pipeIndex).trim();
        String afterPipe = item.substring(pipeIndex + 1).trim();
        String date = afterPipe;
        String description = "";
        int dashIdx = afterPipe.indexOf(" - ");
        if (dashIdx > 0) {
            date = afterPipe.substring(0, dashIdx).trim();
            description = afterPipe.substring(dashIdx + 3).trim();
        }

        String title = headingText;
        String subtitle = "";
        int comma = headingText.indexOf(", ");
        if (comma > 0) {
            title = headingText.substring(0, comma).trim();
            subtitle = headingText.substring(comma + 2).trim();
        } else {
            for (String separator : List.of(" \u2013 ", " \u2014 ", " - ")) {
                int idx = headingText.indexOf(separator);
                if (idx > 0) {
                    title = headingText.substring(0, idx).trim();
                    subtitle = headingText.substring(idx + separator.length()).trim();
                    break;
                }
            }
        }
        return new WorkEntry(title, subtitle, date, description);
    }

    static ProjectEntry parseProjectEntry(String raw) {
        String clean = stripMarkdown(raw).replaceAll("\\s+", " ").trim();
        String heading = clean;
        String description = "";
        for (String separator : List.of(" \u2013 ", " \u2014 ", " - ")) {
            int idx = clean.indexOf(separator);
            if (idx > 0) {
                heading = clean.substring(0, idx).trim();
                description = clean.substring(idx + separator.length()).trim();
                break;
            }
        }

        String title = heading;
        String context = "";
        int contextStart = heading.indexOf('(');
        int contextEnd = heading.lastIndexOf(')');
        if (contextStart > 0 && contextEnd > contextStart) {
            title = heading.substring(0, contextStart).trim();
            context = heading.substring(contextStart, contextEnd + 1).trim();
        }
        return new ProjectEntry(title, context, description);
    }

    static String stripMarkdown(String value) {
        return safe(value)
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("_", "");
    }

    static String excerpt(String value, int maxChars) {
        String clean = stripMarkdown(value).replaceAll("\\s+", " ").trim();
        if (clean.length() <= maxChars) {
            return clean;
        }
        int boundary = clean.lastIndexOf(' ', maxChars - 1);
        int end = boundary > maxChars / 2 ? boundary : maxChars - 1;
        return clean.substring(0, end).trim() + "...";
    }

    static String normalize(String value) {
        String safeValue = safe(value).toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < safeValue.length(); i++) {
            char current = safeValue.charAt(i);
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void addLines(List<String> lines, String value) {
        for (String line : safe(value).split("\\R")) {
            String clean = stripMarkdown(line).trim();
            if (!clean.isBlank()) {
                lines.add(clean);
            }
        }
    }

    private static void addPart(List<ContactPart> parts, String text, DocumentLinkOptions linkOptions) {
        if (text != null && !text.isBlank()) {
            parts.add(new ContactPart(text.trim(), linkOptions));
        }
    }

    private static DocumentLinkOptions emailLink(EmailYaml email) {
        String to = safe(email.getTo());
        return to.isBlank() ? null : new DocumentLinkOptions("mailto:" + to);
    }

    private static DocumentLinkOptions linkOptions(LinkYml link) {
        return link.getLinkUrl() == null || !link.getLinkUrl().isValid()
                ? null
                : new DocumentLinkOptions(link.getLinkUrl().getUrl());
    }

    private static String emailDisplay(EmailYaml email) {
        String display = safe(email.getDisplayText());
        return display.isBlank() ? safe(email.getTo()) : display;
    }

    private static String linkDisplay(LinkYml link) {
        String display = safe(link.getDisplayText());
        return display.isBlank() && link.getLinkUrl() != null ? safe(link.getLinkUrl().getUrl()) : display;
    }

    record ContactPart(String text, DocumentLinkOptions linkOptions) {
    }

    record WorkEntry(String title, String subtitle, String date, String description) {
    }

    record ProjectEntry(String title, String context, String description) {
    }
}
