package com.demcha.markdown;

import com.demcha.loyaut_core.components.content.text.TextDataBody;
import com.demcha.loyaut_core.components.content.text.TextDecoration;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MarkDownParser {

    //TODO test
    public static void main(String[] args) {
        String markdown = "This is **Sparta**, and **it is** *cool*.";

        MarkDownParser parser = new MarkDownParser();
        List<TextDataBody> body = parser.getBody(markdown);

        body.stream().map(TextDataBody::text).forEach(System.out::print);
        System.out.println();

        // Visual check
        body.forEach(System.out::println);
    }

    public List<TextDataBody> getBody(String markdown) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        Node document = parser.parse(markdown);

        List<TextDataBody> resultList = new ArrayList<>();

        // 3. Visitor Pattern to traverse the AST
        NodeVisitor visitor = new NodeVisitor(
                new VisitHandler<>(com.vladsch.flexmark.ast.Text.class, textNode -> {

                    // Determine style based on parents
                    TextDecoration style = determineStyle(textNode);

                    // Get raw text
                    String rawText = textNode.getChars().toString();

                    // 4. Split by whitespace but keep the delimiter (Java 17 compatible)
                    // Regex explanation: Split before a space OR after a space
                    String[] chunks = rawText.split("((?<=\\s)|(?=\\s))");

                    Arrays.stream(chunks)
                            .filter(s -> !s.isEmpty()) // Safety check
                            .forEach(chunk -> resultList.add(new TextDataBody(chunk, style)));
                })
        );

        visitor.visit(document);
        return resultList;
    }

    private TextDecoration determineStyle(Node node) {
        boolean isBold = false;
        boolean isItalic = false;

        // Traverse up the tree to find styling containers
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
}



