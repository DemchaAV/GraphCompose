package com.demcha.components.core;

import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.style.Margin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EntityTest {
    static Stream<Arguments> provideScenarios() {
        return Stream.of(
                Arguments.of()
        );
    }


}