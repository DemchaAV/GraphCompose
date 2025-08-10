package com.demcha.scene;

import com.demcha.core.Element;
import com.demcha.layout.Container;
import com.demcha.layout.Layout;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Module implements Container {
    private final Element self = new Element();
    private final List<Element> children = new ArrayList<>();
    private Layout layout;

    public Module() {
    }

    public Module(Layout layout) {
        this.layout = layout;
    }

    @Override
    public Element getElement() {
        return self;
    }

    @Override
    public List<Element> getChildren() {
        return children;
    }

    @Override
    public Layout getLayout() {
        return layout;
    }

    @Override
    public void setLayout(Layout layout) {
        this.layout = layout;
    }
}
