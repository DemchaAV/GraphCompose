package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.containers.abstract_builders.EmptyBox;
import com.demcha.compose.layout_core.components.content.link.LinkUrl;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.geometry.OuterBoxSize;
import com.demcha.compose.layout_core.components.renderable.Link;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;

public class LinkBuilder extends EmptyBox<LinkBuilder> {
    LinkBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    public <T extends TextBuilder> LinkBuilder displayText(T text) {
        Entity built = text.build();
        var contentSize = OuterBoxSize.from(built).orElseThrow();
        Padding padding = entity().getComponent(Padding.class).orElse(Padding.zero());
        size(new ContentSize(contentSize.width() + padding.horizontal(), contentSize.height() + padding.vertical()));
        addChild(built);
        return self();
    }

    public <T extends LinkUrl> LinkBuilder linkUrl(T linkUrl) {
        entity().addComponent(linkUrl);
        return self();
    }


    @Override
    public void initialize() {
        entity().addComponent(new Link());
    }
}
