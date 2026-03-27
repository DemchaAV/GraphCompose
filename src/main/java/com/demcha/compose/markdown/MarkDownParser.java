package com.demcha.compose.markdown;

import com.demcha.compose.loyaut_core.components.content.text.TextDataBody;
import com.demcha.compose.loyaut_core.components.content.text.TextDecoration;
import com.demcha.compose.loyaut_core.components.content.text.TextStyle;
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
                    TextStyle prefixStyle = new TextStyle(style.fontName(), style.size(), TextDecoration.DEFAULT,
                            style.color());

                    // New line before each list item (optional; helps readability)
                    // resultList.add(new TextDataBody("\n", prefixStyle));

                    // Use bullet or dash — your choice:
                    resultList.add(new TextDataBody("• ", prefixStyle));
                    // resultList.add(new TextDataBody("- ", prefixStyle));

                    // Continue visiting children so text inside item is collected
                    visitor[0].visitChildren(node);
                }),

                // 2) Preserve line breaks
                new VisitHandler<>(SoftLineBreak.class,
                        br -> resultList.add(new TextDataBody(" ",
                                new TextStyle(style.fontName(), style.size(), TextDecoration.DEFAULT, style.color())))),
                new VisitHandler<>(HardLineBreak.class,
                        br -> resultList.add(new TextDataBody(" ",
                                new TextStyle(style.fontName(), style.size(), TextDecoration.DEFAULT, style.color())))),

                // 3) Headers
                new VisitHandler<>(Heading.class, node -> {
                    int level = node.getLevel();
                    double scale = switch (level) {
                        case 1 -> 2.0;
                        case 2 -> 1.5;
                        case 3 -> 1.25;
                        default -> 1.0;
                    };
                    double newSize = style.size() * scale;
                    TextStyle headerStyle = new TextStyle(style.fontName(), newSize, TextDecoration.BOLD,
                            style.color());

                    // Add newline before header for better separation
                    // resultList.add(new TextDataBody("\n",
                    //        new TextStyle(style.fontName(), style.size(), TextDecoration.DEFAULT, style.color())));

                    // We need to visit children (the text inside the header) but force our new
                    // headerStyle
                    // The problem is visitChildren(node) uses the global 'style' or doesn't pass
                    // context easily
                    // unless we refactor to pass style recursively.
                    // A simpler approach for this visitor structure: manually iterate children and
                    // apply style.

                    Node child = node.getFirstChild();
                    while (child != null) {
                        if (child instanceof Text) {
                            String text = child.getChars().toString();
                            resultList.add(new TextDataBody(text, headerStyle));
                        } else {
                            // If there are other nodes (like emphasis inside header), we might miss them if
                            // we don't recurse.
                            // But recursion here with a DIFFERENT style requires changing the getBody
                            // signature or helper.
                            // For now, let's just grab the text content of the header.
                            // Or better: use a helper method if we want to support bold inside header.
                            // Given existing recursive structure uses scope-scope variables or just 'style'
                            // param which is fixed for getBody...
                            // Actually, let's just grab the string content of the header node to keep it
                            // simple and robust for now.
                            resultList.add(new TextDataBody(child.getChars().toString(), headerStyle));
                        }
                        child = child.getNext();
                    }

                    // Add newline after header
                    // resultList.add(new TextDataBody("\n",
                    //        new TextStyle(style.fontName(), style.size(), TextDecoration.DEFAULT, style.color())));
                }),

                // 4) Text nodes (your current logic)
                new VisitHandler<>(Text.class, textNode -> {
                    TextDecoration decoration = determineStyle(textNode);
                    TextStyle newTextStyle = new TextStyle(style.fontName(), style.size(), decoration, style.color());

                    String rawText = textNode.getChars().toString();
                    String[] chunks = rawText.split("((?<=\\s)|(?=\\s))");

                    Arrays.stream(chunks)
                            .filter(s -> !s.isEmpty())
                            .forEach(chunk -> resultList.add(new TextDataBody(chunk, newTextStyle)));
                }));

        visitor[0].visit(document);
        return resultList;
    }

    private TextDecoration determineStyle(Node node) {
        boolean isBold = false;
        boolean isItalic = false;

        Node parent = node.getParent();
        while (parent != null) {
            if (parent instanceof StrongEmphasis)
                isBold = true;
            if (parent instanceof Emphasis)
                isItalic = true;
            parent = parent.getParent();
        }

        if (isBold && isItalic)
            return TextDecoration.BOLD_ITALIC;
        if (isBold)
            return TextDecoration.BOLD;
        if (isItalic)
            return TextDecoration.ITALIC;
        return TextDecoration.DEFAULT;
    }

}
