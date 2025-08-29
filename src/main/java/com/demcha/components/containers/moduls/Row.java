package com.demcha.components.containers.moduls;

import com.demcha.components.containers.abstract_builders.ShapeBuilderBase;
import com.demcha.components.content.HContainer;
import com.demcha.components.content.rectangle.Rectangle;
import com.demcha.components.core.Entity;
import com.demcha.core.EntityManager;

// horizontall aligne
public class Row {
    EntityManager entityManager;

    public Row(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public  RowBuilder create() {
        return new RowBuilder(entityManager);
    }


     class RowBuilder extends ShapeBuilderBase<RowBuilder> {

        public RowBuilder(EntityManager document) {
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


