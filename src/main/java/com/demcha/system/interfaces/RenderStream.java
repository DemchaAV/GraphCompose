package com.demcha.system.interfaces;

import com.demcha.components.core.Entity;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.*;
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
