package com.demcha.markdown;

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.TextCollectingVisitor;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.ArrayList;
import java.util.List;

public class BasicSample {
    public static void main(String[] args) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();

        // 2. Parse the Markdown into an AST (Abstract Syntax Tree)
        String markdown = "This is **Sparta** and [this is a link](http://google.com).";
        Node document = parser.parse(markdown);

        // 3. Use TextCollectingVisitor to extract plain text
        var textVisitor = new TextCollectingVisitor();
        String plainText = textVisitor.collectAndGetText(document);


        // 4. Output
        System.out.println("--- Original Markdown ---");
        System.out.println(markdown);

        System.out.println("\n--- Clean Data (Plain Text) ---");
        System.out.println(plainText);
        List<TextDataBody> textData = new ArrayList<>();


        // Expected Output: "This is Sparta and this is a link."
    }

    enum FontType {
        NORMAL,
        BOLD,
        ITALIC,
        BOLD_ITALIC
    }

    record TextDataBody(String text, FontType fontType){

    }
}
