package com.demcha.loyaut_core.components;

import com.demcha.font_library.FontName;
import com.demcha.loyaut_core.components.content.text.TextDataBody;
import com.demcha.loyaut_core.components.content.text.TextDecoration;
import com.demcha.loyaut_core.components.content.text.TextStyle;
import com.demcha.loyaut_core.system.interfaces.Font;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a line of text with its associated width and x-coordinate for positioning.
 * This class is designed to hold immutable text and width data, while allowing the x-coordinate
 * to be mutable for layout purposes.
 * <p>
 * The {@code @Data} annotation from Lombok automatically generates
 * getters for all fields, setters for non-final fields,
 * a constructor for final fields, {@code equals()}, {@code hashCode()}, and {@code toString()} methods.
 * </p>
 */
@Data
@Accessors(fluent = true)

public final class LineTextData {
    /**
     * The actual text content of the line. This field is final and immutable.
     */
    private final List<TextDataBody> wordList = new ArrayList<>();
    private final int page;
    private double x;
    private double y;



    private LineTextData(int page) {
        this.page = page;
    }

    public LineTextData(List<TextDataBody> wordList, int page) {
        this.page = page;
        this.wordList.addAll(wordList);
    }
    public LineTextData(LineTextData ltd, double x, double y, int page) {
        this.page = page;
        this.x = x;
        this.y = y;
        this.wordList.addAll(ltd.wordList());
    }

    public static LineTextData createWithoutMarkdown(String text, TextStyle style, int page) {
        var ltd = new LineTextData(page);
        ltd.wordList.add(new TextDataBody(text, style));
        return ltd;
    }


    public <T extends Font<?>> double width(TextDataBody textDataBody, T font) {
        return  font.getTextWidth(textDataBody.textStyle(),textDataBody.text());
    }

    public <T extends Font<?>> double  width(T font) {
        return wordList.stream()
                .mapToDouble((textDataBody) -> width(textDataBody, font))
                .sum();
    }


}
