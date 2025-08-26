package com.demcha.legacy.scene;

import com.demcha.legacy.core.Element;
import com.demcha.components.containers.Container;
import com.demcha.legacy.layout.Layout;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Row implements Container {
    private final Element self = new Element();
    private final List<Element> children = new ArrayList<>();
    private Layout layout;

    public Row() {}
    public Row(Layout layout) { this.layout = layout; }

    @Override
    public Element getElement() { return self; }

    @Override
    public List<Element> getChildren() { return children; }

    @Override
    public Layout getLayout() { return layout; }

    @Override
    public void setLayout(Layout layout) { this.layout = layout; }
}
