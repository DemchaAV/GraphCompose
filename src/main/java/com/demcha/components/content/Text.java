package com.demcha.components.content;
import com.demcha.components.core.Component;

public record Text(String text) implements Box, Component {
}
