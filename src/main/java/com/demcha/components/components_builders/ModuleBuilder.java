package com.demcha.components.components_builders;


import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.style.Margin;
import com.demcha.core.EntityManager;

import java.util.ArrayList;

public class ModuleBuilder extends VContainerBuilder {
    private ModuleBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    public ModuleBuilder create(EntityManager entityManager) {
        return new ModuleBuilder(entityManager);
    }

    /**
     *
     * Method for modularBuilder
     *
     */

    private Entity createABottom(String nameButton, String TextButton) {
        var entity = new Entity();
        return entity;
    }

    private Entity createRow(String rowName, int column) {
        var buttons = new ArrayList<Entity>();
        for (int i = 0; i < column; i++) {
            var cell = createABottom("row %s cell#%s".formatted(rowName, i), "cell# %s".formatted(i));
            buttons.add(cell);
        }


        var hContainer2 = new HContainerBuilder(entityManager).create(Align.middle(5))
                .entityName(new EntityName(rowName))
                .anchor(Anchor.centerBottom());

        for (int i = 0; i < buttons.size(); i++) {
            hContainer2.addChild(buttons.get(i));
        }

        var h = hContainer2.build();
        return h;
    }

    private Entity createTable(String tableName, int column, int rows) {
        var rowsList = new ArrayList<Entity>();
        for (int i = 0; i < rows; i++) {
            Entity row = createRow("row# " + i, column);
            rowsList.add(row);
        }

        var vContainer = new VContainerBuilder(entityManager).create(Align.middle(5))
                .entityName(new EntityName("vContainer"))
                .margin(Margin.of(15))
                .anchor(Anchor.centerBottom());
        for (int i = 0; i < rows; i++) {
            vContainer.addChild(rowsList.get(i));
        }
        return vContainer.build();
    }


}
