package com.demcha.compose.layout_core.system.interfaces;

import com.demcha.compose.layout_core.components.core.Entity;

import java.io.IOException;

/**
 * This interface serve to open a stream classes for our systems
 *
 * @param <T> type of stream which will be open
 */

public interface RenderStream<T> {
    T openContentStream(int pageIndex) throws IOException;

    T openContentStream(Entity entity) throws IOException;


}
