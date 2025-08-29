package com.demcha.components.containers.moduls;

import com.demcha.components.containers.abstract_builders.ShapeBuilderBase;
import com.demcha.components.content.HContainer;
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


     class RowBuilder extends ShapeBuilderBase<RowBuilder> {

        public RowBuilder(PdfDocument document) {
            super(document);
        }



        public RowBuilder rectangle(Rectangle rectangle) {
            return addComponent(rectangle);
        }

        public Entity build() {
            return entity;
        }

         @Override
         public void initialize() {
             entity.addComponent(new HContainer());
         }
     }


}


