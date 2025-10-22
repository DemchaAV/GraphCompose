package com.demcha.components;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

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
@AllArgsConstructor
@Accessors(fluent = true)
public final class LineTextData {
    /**
     * The actual text content of the line. This field is final and immutable.
     */
    private final String line;
    /**
     * The calculated width of the text line in a specific font and font size. This field is final and immutable.
     */
    private final double width;
    /**
     * The x-coordinate (horizontal position) where the text line should be drawn.
     * This field is mutable as its value might be adjusted during layout calculations.
     */
    private double x;
    private double y;
    private int page;

    public LineTextData(LineTextData lineTextData, double y, int page) {
        this(lineTextData.line, lineTextData.width(), lineTextData.x(), y, page);
    }

    public LineTextData(LineTextData lineTextData, double x, double y, int page) {
        this(lineTextData.line, lineTextData.width(), x, y, page);
    }


    public LineTextData(String line, double width, double x) {
        this.line = line;
        this.width = width;
        this.x = x;
    }

    public LineTextData(String chunkText, double textWidth) {
        this.line = chunkText;
        this.width = textWidth;
    }
}
