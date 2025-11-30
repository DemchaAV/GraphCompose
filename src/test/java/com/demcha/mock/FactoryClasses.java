package com.demcha.mock;

import com.demcha.components.components_builders.Canvas;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.style.Margin;
import com.demcha.mock.data.CanvasData;
import com.demcha.mock.data.MarginData;
import com.demcha.mock.data.OffsetData;
import com.demcha.mock.data.SizeData;
import com.demcha.system.utils.page_breaker.Offset;

import java.util.Optional;

import static org.mockito.Mockito.*;

public class FactoryClasses {

    public static Entity entityMock(ContentSize contentSize, Margin margin) {
        Entity e = mock(Entity.class);
        when(e.getComponent(ContentSize.class)).thenReturn(Optional.of(contentSize));
        when(e.getComponent(Margin.class)).thenReturn(Optional.of(margin));
        return e;
    }

    public static Offset offsetMock(OffsetData offsetData) {
        Offset offset = mock(Offset.class);
        when(offset.x()).thenReturn(offsetData.x());
        when(offset.y()).thenReturn(offsetData.y());
        return offset;
    }
    public static Offset offsetReal(OffsetData offsetData) {
       return new Offset(offsetData.y(), offsetData.x());
    }


    public static ContentSize mockContentSize(SizeData sizeData) {
        ContentSize contentSize = mock(ContentSize.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));

        when(contentSize.width()).thenReturn(sizeData.width());
        when(contentSize.height()).thenReturn(sizeData.height());
        return contentSize;
    }

    public static Margin margin(MarginData marginData) {
        // It is safer to use standard mock() instead of CALLS_REAL_METHODS
        // if you are going to stub all methods anyway.
        Margin margin = mock(Margin.class);

        // NOTICE: .when(margin).top()  <-- Parenthesis closes after 'margin'
        doReturn(marginData.top()).when(margin).top();
        doReturn(marginData.bottom()).when(margin).bottom();
        doReturn(marginData.right()).when(margin).right();
        doReturn(marginData.left()).when(margin).left();

        return margin;
    }

    public static Canvas canvasMock(CanvasData canvasData) {
        // 1. Prepare the sub-component (Margin) first
        // We reuse your existing 'margin' method to create a fully configured Margin mock
        Margin marginMock = margin(canvasData.margin());

        // 2. Create the Canvas mock
        Canvas canvas = mock(Canvas.class);

        // 3. Link the Margin mock to the Canvas mock
        // Correct Syntax: .when(mock).method()
        doReturn(marginMock).when(canvas).margin();

        // 4. Stub simple properties
        doReturn((float) canvasData.width()).when(canvas).width();
        doReturn((float) canvasData.height()).when(canvas).height();

        // 5. Stub calculated properties
        // Notice the parenthesis: .when(canvas) is closed before calling .innerHeigh()
        doReturn(canvasData.height() - canvasData.margin().top() - canvasData.margin().bottom())
                .when(canvas).innerHeigh();

        doReturn(canvasData.width() - canvasData.margin().left() - canvasData.margin().right())
                .when(canvas).innerWidth();

        // 6. Stub bounding lines
        doReturn((float) canvasData.margin().bottom()).when(canvas).boundingBottomLine();
        doReturn((float) (canvasData.height() - canvasData.margin().top())).when(canvas).boundingTopLine();
        doReturn((float) (canvasData.margin().left())).when(canvas).boundingLeftLine();
        doReturn((float) (canvasData.width() - canvasData.margin().right())).when(canvas).boundingRightLine();

        return canvas;
    }
}
