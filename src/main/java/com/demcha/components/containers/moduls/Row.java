package com.demcha.components.containers.moduls;

import com.demcha.components.content.components_builders.BaseShapeBuilder;
import com.demcha.components.content.rectangle.Rectangle;
import com.demcha.components.core.Entity;
import com.demcha.core.PdfDocument;
import lombok.AllArgsConstructor;

// horizontall aligne
public class Row {
    PdfDocument pdfDocument;

    public Row(PdfDocument pdfDocument) {
        this.pdfDocument = pdfDocument;
    }

    public  RowBuilder create() {
        return new RowBuilder(pdfDocument);
    }

    @AllArgsConstructor
     class RowBuilder extends BaseShapeBuilder<RowBuilder> {
        final PdfDocument pdfDocument;

        @Override
        protected RowBuilder self() {
            return this;
        }

        public RowBuilder rectangle(Rectangle rectangle) {
            return addComponent(rectangle);
        }

        public Entity build() {
            return buildInto(pdfDocument);
        }
    }


}


