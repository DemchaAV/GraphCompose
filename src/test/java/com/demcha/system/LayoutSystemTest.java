package com.demcha.system;

import com.demcha.components.components_builders.ElementBuilder;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.HAnchor;
import com.demcha.components.layout.ParentComponent;
import com.demcha.components.layout.VAnchor;
import com.demcha.components.layout.coordinator.ComputedPosition;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutSystemTest {


    private static Mockh getMockh() {
        EntityManager pdf = new EntityManager();

        var parent = ElementBuilder.create()
                .entityName(new EntityName("ParentBox"))
                .size(new ContentSize(300, 220))
                .padding(Padding.of(10))
                .position(Position.zero())
                .anchor(new Anchor(HAnchor.RIGHT, VAnchor.TOP))
                .margin(new Margin(5, 5, 5, 5))
                .buildComponents();

        var child = ElementBuilder.create()
                .entityName(new EntityName("ChildBox"))
                .parentComponent(new ParentComponent(parent.getId()))
                .size(new ContentSize(180, 140))
                .padding(Padding.of(10))
                .position(new Position(15, 20))
                .anchor(new Anchor(HAnchor.RIGHT, VAnchor.TOP))
                .margin(new Margin(5, 5, 5, 5))
                .buildComponents();

        var grandChild = ElementBuilder.create()
                .entityName(new EntityName("GrandChildBox"))
                .parentComponent(new ParentComponent(child.getId()))
                .size(new ContentSize(100, 80))
                .padding(Padding.of(5))
                .position(new Position(10, 12))
                .anchor(new Anchor(HAnchor.RIGHT, VAnchor.TOP))
                .margin(new Margin(3, 3, 3, 3))
                .buildComponents();
        pdf.putEntity(parent);
        pdf.putEntity(child);
        pdf.putEntity(grandChild);

        Mockh result = new Mockh(pdf, parent, child, grandChild);
        return result;
    }

    @Test
    void calculateInnerBoxSize() {
        LayoutSystem layoutSystem = new LayoutSystem(new PDPage(PDRectangle.A4));
        var entity = new Entity();
        entity.addComponent(new EntityName("Box"))
                .addComponent(new ContentSize(200, 300))
                .addComponent(new Padding(8, 8, 8, 8));

        Optional<InnerBoxSize> boxSize = InnerBoxSize.from(entity);
        assertThat(boxSize.get()).isNotNull();
        assertThat(boxSize.get()).isEqualTo(new InnerBoxSize(184, 284));
    }

    @Test
    void expendBoxSizeByChildren() {
        Mockh mockh = getMockh();
        var parent = mockh.parent();
        var child = mockh.child();
        var layoutSystem = new LayoutSystem(new PDPage(PDRectangle.A4));

        child.addComponent(new ContentSize(500, 300));

        layoutSystem.expendBoxSizeByChildren(parent, Set.of(child));
        var outerBoxSize = InnerBoxSize.from(parent).orElse(null);
        assertThat(outerBoxSize).isNotNull();
        assertThat(outerBoxSize).isEqualTo(new InnerBoxSize(520, 325));

    }

    @Test
    void calculationBoxSize() {
        LayoutSystem layoutSystem = new LayoutSystem(new PDPage(PDRectangle.A4));
        var entity = new Entity();
        entity.addComponent(new EntityName("Box"))
                .addComponent(new ContentSize(200, 300))
                .addComponent(new Margin(8, 8, 8, 8));
        Optional<OuterBoxSize> boxSize = OuterBoxSize.from(entity);
        assertThat(boxSize.get()).isNotNull();
        assertThat(boxSize.get()).isEqualTo(new OuterBoxSize(216, 316));
    }

    @Test
    void computesParentAndChildAbsolutePositions() {
        EntityManager pdf = new EntityManager();
        var layoutSystem = new LayoutSystem(new PDPage(PDRectangle.A4));

        Mockh mockh = getMockh();
        var parent = mockh.parent();
        var child = mockh.child();

        pdf.putEntity(parent);
        pdf.putEntity(child);


        var boxPos = layoutSystem.calculatePositionFromParent(parent, null, pdf).orElseThrow();
        var childPos = layoutSystem.calculatePositionFromParent(child, parent, pdf).orElseThrow();

        // Presence
        assertTrue(parent.getComponent(ComputedPosition.class).isPresent(), "Box must have ComputedPosition");
        assertTrue(child.getComponent(ComputedPosition.class).isPresent(), "Child must have ComputedPosition");

        // Idempotency
        var boxPos2 = layoutSystem.calculatePositionFromParent(parent, null, pdf).orElseThrow();
        var childPos2 = layoutSystem.calculatePositionFromParent(child, parent, pdf).orElseThrow();

        assertEquals(boxPos.x(), boxPos2.x(), 1e-9);
        assertEquals(boxPos.y(), boxPos2.y(), 1e-9);
        assertEquals(childPos.x(), childPos2.x(), 1e-9);
        assertEquals(childPos.y(), childPos2.y(), 1e-9);
    }

    @Test
    void parent_child_grandchild_positions_are_computed_and_idempotent() {

        Mockh mockh = getMockh();
        var child = mockh.child();
        var parent = mockh.parent();
        var grandChild = mockh.grandChild();
        var layoutSystem = new LayoutSystem(new PDPage(PDRectangle.A4));


        mockh.pdf().putEntity(parent);
        mockh.pdf().putEntity(mockh.child());
        mockh.pdf().putEntity(grandChild);


        var p1 = layoutSystem.calculatePositionFromParent(parent, null, mockh.pdf()).orElseThrow();
        var c1 = layoutSystem.calculatePositionFromParent(child, parent, mockh.pdf()).orElseThrow();
        var g1 = layoutSystem.calculatePositionFromParent(grandChild, child, mockh.pdf()).orElseThrow();

        // Компоненты должны быть сохранены
        assertTrue(parent.getComponent(ComputedPosition.class).isPresent());
        assertTrue(child.getComponent(ComputedPosition.class).isPresent());
        assertTrue(grandChild.getComponent(ComputedPosition.class).isPresent());

        // Идемпотентность
        var p2 = layoutSystem.calculatePositionFromParent(parent, null, mockh.pdf()).orElseThrow();
        var c2 = layoutSystem.calculatePositionFromParent(child, parent, mockh.pdf()).orElseThrow();
        var g2 = layoutSystem.calculatePositionFromParent(grandChild, child, mockh.pdf()).orElseThrow();

        assertEquals(p1.x(), p2.x(), 1e-9);
        assertEquals(p1.y(), p2.y(), 1e-9);
        assertEquals(c1.x(), c2.x(), 1e-9);
        assertEquals(c1.y(), c2.y(), 1e-9);
        assertEquals(g1.x(), g2.x(), 1e-9);
        assertEquals(g1.y(), g2.y(), 1e-9);
    }

    private record Mockh(EntityManager pdf, Entity parent, Entity child, Entity grandChild) {
    }
}