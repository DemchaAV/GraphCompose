package com.demcha.compose.layout_core.components.content.text;

import com.demcha.compose.layout_core.components.content.text.LineTextData;
import com.demcha.compose.layout_core.components.core.Component;

import java.util.List;

/**
 * Immutable block-text payload consisting of positioned/measured lines plus
 * the block's inter-line spacing contract.
 */
public record BlockTextData(List<LineTextData> lines, float lineSpacing) implements Component {
    public static final BlockTextData EMPTY = new BlockTextData(List.of(), 0);

    public static BlockTextData empty() {
        return EMPTY;
    }

    public boolean contains(String text) {
        if (lines.isEmpty()) {
            return false;
        }
        for (LineTextData line : lines) {
            if (line.bodies().stream().anyMatch(word -> word.text().contains(text))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rebuilds the block payload with a new line list while preserving the
     * original spacing contract.
     */
    public BlockTextData withLines(List<LineTextData> updatedLines) {
        return new BlockTextData(updatedLines, lineSpacing);
    }
}


