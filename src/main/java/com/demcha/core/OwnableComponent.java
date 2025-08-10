package com.demcha.core;

public interface OwnableComponent extends Component {
    void setOwner(Element owner);
    Element getOwner();
}
