package com.demcha.markdown;

import com.demcha.loyaut_core.components.content.text.TextDataBody;
import com.demcha.loyaut_core.components.content.text.TextDecoration;
import com.demcha.loyaut_core.components.content.text.TextStyle;
import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MarkDownParser {

    public List<TextDataBody> getBody(String markdown, TextStyle style) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        Node document = parser.parse(markdown);

        List<TextDataBody> resultList = new ArrayList<>();

        final NodeVisitor[] visitor = new NodeVisitor[1];
        visitor[0] = new NodeVisitor(
                // 1) List items: add your own prefix (since '-' is not Text)
                new VisitHandler<>(ListItem.class, node -> {
                    TextStyle prefixStyle = new TextStyle(style.fontName(), style.size(), TextDecoration.DEFAULT, style.color());

                    // New line before each list item (optional; helps readability)
                    resultList.add(new TextDataBody("\n", prefixStyle));

                    // Use bullet or dash — your choice:
                    resultList.add(new TextDataBody("• ", prefixStyle));
                    // resultList.add(new TextDataBody("- ", prefixStyle));

                    // Continue visiting children so text inside item is collected
                    visitor[0].visitChildren(node);
                }),

                // 2) Preserve line breaks
                new VisitHandler<>(SoftLineBreak.class, br ->
                        resultList.add(new TextDataBody("\n", new TextStyle(style.fontName(), style.size(), TextDecoration.DEFAULT, style.color())))
                ),
                new VisitHandler<>(HardLineBreak.class, br ->
                        resultList.add(new TextDataBody("\n", new TextStyle(style.fontName(), style.size(), TextDecoration.DEFAULT, style.color())))
                ),

                // 3) Text nodes (your current logic)
                new VisitHandler<>(Text.class, textNode -> {
                    TextDecoration decoration = determineStyle(textNode);
                    TextStyle newTextStyle = new TextStyle(style.fontName(), style.size(), decoration, style.color());

                    String rawText = textNode.getChars().toString();
                    String[] chunks = rawText.split("((?<=\\s)|(?=\\s))");

                    Arrays.stream(chunks)
                            .filter(s -> !s.isEmpty())
                            .forEach(chunk -> resultList.add(new TextDataBody(chunk, newTextStyle)));
                })
        );

        visitor[0].visit(document);
        return resultList;
    }

    private TextDecoration determineStyle(Node node) {
        boolean isBold = false;
        boolean isItalic = false;

        Node parent = node.getParent();
        while (parent != null) {
            if (parent instanceof StrongEmphasis) isBold = true;
            if (parent instanceof Emphasis) isItalic = true;
            parent = parent.getParent();
        }

        if (isBold && isItalic) return TextDecoration.BOLD_ITALIC;
        if (isBold) return TextDecoration.BOLD;
        if (isItalic) return TextDecoration.ITALIC;
        return TextDecoration.DEFAULT;
    }

    public static void main(String[] args) {
        String  s = "*Portfolio Project*\nBuilt a secure e-commerce ";
        MarkDownParser parser = new MarkDownParser();
        parser.getBody(s, TextStyle.DEFAULT_STYLE).stream().map(TextDataBody::text).forEach(System.out::println);

    }
}
