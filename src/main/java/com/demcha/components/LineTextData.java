package com.demcha.components;

import lombok.Data;

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

}
