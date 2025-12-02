package com.demcha.markdown;


import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.ArrayList;
import java.util.List;

public class StyledTextSample {

    // 1. Define your Enum
    enum FontType {
        NORMAL, BOLD, ITALIC, BOLD_ITALIC
    }

    // 2. Define your Record (Java 17+)
    record TextDataBody(String text, FontType fontType) {}

    public static void main(String[] args) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();

        // Complex markdown with mixed styles
        String markdown = "This is **Sparta**, this is *italic*, and this is ***both***.";
        Node document = parser.parse(markdown);

        List<TextDataBody> resultList = new ArrayList<>();

        // 3. Create a Custom Visitor
        NodeVisitor visitor = new NodeVisitor(
                new VisitHandler<>(Text.class, textNode -> {
                    // Logic: When we find text, check its parents
                    FontType style = determineStyle(textNode);

                    // Add to our list
                    resultList.add(new TextDataBody(textNode.getChars().toString(), style));
                })
        );

        // 4. Run the visitor
        visitor.visit(document);

        // 5. Output Results
        System.out.println("--- Extracted Data ---");
        resultList.forEach(item ->
                System.out.printf("Text: '%-10s' | Style: %s%n", item.text(), item.fontType())
        );
    }

    /**
     * Helper method to look up the tree and find active styles
     */
    private static FontType determineStyle(Node node) {
        boolean isBold = false;
        boolean isItalic = false;

        // Walk up the tree parents until we hit the root (Document)
        Node parent = node.getParent();
        while (parent != null) {
            if (parent instanceof StrongEmphasis) isBold = true;
            if (parent instanceof Emphasis) isItalic = true;
            parent = parent.getParent();
        }

        if (isBold && isItalic) return FontType.BOLD_ITALIC;
        if (isBold) return FontType.BOLD;
        if (isItalic) return FontType.ITALIC;
        return FontType.NORMAL;
    }
}