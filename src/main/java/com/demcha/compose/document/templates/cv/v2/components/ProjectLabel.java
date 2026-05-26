package com.demcha.compose.document.templates.cv.v2.components;

/**
 * Splits legacy project labels like "GraphCompose (Java, PDFBox)" into display title and stack.
 */
public record ProjectLabel(String title, String stack) {
    public ProjectLabel {
        title = title == null ? "" : title;
        stack = stack == null ? "" : stack;
    }

    public static ProjectLabel parse(String value) {
        String clean = MarkdownInline.plainText(value).trim();
        int stackOpen = clean.lastIndexOf('(');
        if (stackOpen > 0 && clean.endsWith(")")) {
            return new ProjectLabel(
                    clean.substring(0, stackOpen).trim(),
                    clean.substring(stackOpen + 1, clean.length() - 1).trim()
            );
        }
        return new ProjectLabel(clean, "");
    }
}
