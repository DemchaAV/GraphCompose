package com.demcha.components.layout.coordinator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlacementTest {

    public static Placement mockPlacement() {
        Placement placement = mock(Placement.class);
        when(placement.x()).thenReturn(0.0);
        return placement;
    }

}