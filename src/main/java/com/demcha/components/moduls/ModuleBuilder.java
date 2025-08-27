package com.demcha.components.moduls;


import com.demcha.components.containers.HContainerBuilder;
import com.demcha.components.containers.VContainerBuilder;
import com.demcha.components.content.Stroke;
import com.demcha.components.content.components_builders.RectangleBuilder;
import com.demcha.components.content.components_builders.TextBuilder;
import com.demcha.components.content.rectangle.Radius;
import com.demcha.components.content.rectangle.Rectangle;
import com.demcha.components.content.text.Text;
import com.demcha.components.content.text.TextDecoration;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.style.ColorComponent;
import com.demcha.components.style.Margin;
import com.demcha.core.PdfDocument;

import java.awt.*;
import java.util.ArrayList;

public class ModuleBuilder extends Module {
    private ModuleBuilder(PdfDocument pdfDocument) {
        super(pdfDocument);
    }

    public  ModuleBuilder create(PdfDocument pdfDocument) {
        return new ModuleBuilder(pdfDocument);
    }

    /**
     *
     *Method for modularBuilder
     *
     */

    private  Entity createABottom(String nameButton, String TextButton) {
        var button = RectangleBuilder.create()
                .entityName(new EntityName(nameButton))
                .rectangle(new Rectangle(new Radius(5)))
                .size(new ContentSize(100, 30))
//                .padding(Padding.of(10))
                .stroke(new Stroke(12.0))
                .strokeColor(new Color(133, 198, 198, 255))
                .anchor(Anchor.topLeft())
//                .margin(Margin.all(15))

                .buildInto(pdfDocument);
        var text3 = TextBuilder.create()
                .entityName(new EntityName("Text"))
                .parentComponent(button)
                .text(new Text(TextButton, new TextStyle(TextStyle.HELVETICA, 12, TextDecoration.DEFAULT, ColorComponent.MODULE_LINE_TEXT)))
                .anchor(Anchor.center())
                .buildInto(pdfDocument);
        return button;
    }

    private  Entity createRow(String rowName, int column) {
        var buttons = new ArrayList<Entity>();
        for (int i = 0; i < column; i++) {
            var cell = createABottom("row %s cell#%s".formatted(rowName, i), "cell# %s".formatted(i));
            buttons.add(cell);
        }

        var hContainer2 = HContainerBuilder.create(Align.middle(5))
                .entityName(new EntityName(rowName))
                .anchor(Anchor.centerBottom());

        for (int i = 0; i < buttons.size(); i++) {
            hContainer2.add(buttons.get(i));
        }

        var h = hContainer2.build(pdfDocument);
        return h;
    }

    private  Entity createTable(String tableName, int column, int rows) {
        var rowsList = new ArrayList<Entity>();
        for (int i = 0; i < rows; i++) {
            Entity row = createRow("row# " + i, column);
            rowsList.add(row);
        }

        var vContainer = VContainerBuilder.create(Align.middle(5))
                .entityName(new EntityName("vContainer"))
                .margin(Margin.all(15))
                .anchor(Anchor.centerBottom());
        for (int i = 0; i < rows; i++) {
            vContainer.add(rowsList.get(i));
        }
        return vContainer.build(pdfDocument);
    }

}
