package com.demcha.legacy.core;

import com.demcha.components.core.Component;

public interface OwnableComponent extends Component {
    void setOwner(Element owner);
    Element getOwner();
}
