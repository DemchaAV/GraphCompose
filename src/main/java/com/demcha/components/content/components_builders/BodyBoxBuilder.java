package com.demcha.components.content.components_builders;

public class BodyBoxBuilder extends ComponentBoxBuilder<BodyBoxBuilder>{
    @Override
    protected BodyBoxBuilder self() {
        return this;
    }
    private BodyBoxBuilder() {}
    public static BodyBoxBuilder create()
    {
        return new BodyBoxBuilder();
    }
}
