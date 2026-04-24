package com.demcha.compose.font;

import com.demcha.compose.engine.font.Font;
import lombok.experimental.Accessors;

/**
 * Represents a set of font information, including the font name and its corresponding type.
 * This class is used to define the properties of a specific font.
 *
 * @param name The name of the font.
 * @param font The class representing the font, which implements the {@link Font} interface.
 */
@Accessors(fluent = true)
public record FontSet(FontName name, Font<?> font) {
}
