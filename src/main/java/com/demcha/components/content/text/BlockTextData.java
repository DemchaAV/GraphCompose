package com.demcha.components.content.text;

import com.demcha.components.LineTextData;
import com.demcha.components.core.Component;

import java.util.List;

public record BlockTextData(List<LineTextData> lines, float lineSpacing)  implements Component {
    public static final BlockTextData EMPTY = new BlockTextData(List.of(), 0);

    public static BlockTextData empty() {
        return EMPTY;
    }
}
