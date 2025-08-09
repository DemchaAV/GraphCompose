package com.demcha.structure;

import com.demcha.structure.interfaces.UiElement;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
public class Row {
    private final UiElement position = new UiElement();
    private  List<Element<?>> blocks;

    public Row(List<Element<?>> row) {
        this.blocks = row;
    }

    public Row() {
        this.blocks = new ArrayList<>();
    }
}
