package com.demcha.components.containers.moduls;

import com.demcha.components.containers.LayoutBuilder;
import com.demcha.core.PdfDocument;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public class EmptyBox<T> extends LayoutBuilder<T> {
    protected final PdfDocument document;


}

