package com.demcha.compose.testsupport.engine.assembly.container;

import com.demcha.compose.engine.components.core.Component;

public enum StackAxis implements Component {
    VERTICAL,   // элементы идут сверху вниз (как колонка)
    HORIZONTAL, // элементы идут слева направо (как строка)
    REVERSE_VERTICAL,
    REVERSE_HORIZONTAL,
    DEFAULT
}
