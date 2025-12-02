package com.demcha.loyaut_core.components.content.text;

import com.demcha.loyaut_core.components.LineTextData;
import com.demcha.loyaut_core.components.core.Component;

import java.util.List;

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
            if (line.line().contains(text)) {
                return true;
            }
        }
        return false;
    }
}

