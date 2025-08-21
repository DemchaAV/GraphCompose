package com.demcha.system;

import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.BoxSize;
import com.demcha.components.geometry.ContentBox;
import com.demcha.components.geometry.Size;
import com.demcha.components.layout.ComputedPosition;
import com.demcha.components.layout.ParentComponent;
import com.demcha.components.layout.Position;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.PdfDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Import AssertJ for fluent assertions
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

class LayoutSystemTest {

    @Test
    @DisplayName("Should calculate child's absolute position including parent's margin and padding")
    void shouldCalculateChildPositionCorrectly() {
        //  चरण 1: GIVEN (Arrange)
        // Set up the world: the document, the system, and the entities.
        LayoutSystem layoutSystem = new LayoutSystem();
        PdfDocument doc = new PdfDocument();
        doc.addSystem(layoutSystem);

        // Create a root entity with padding & margin
        var root = doc.createEntity();
        root.addComponent(new EntityName("Box"));
        root.addComponent(new BoxSize(500, 500));
        root.addComponent(new Padding(8, 8, 8, 8)); // top=8, left=8
        root.addComponent(new Margin(5, 9, 8, 0));  // top=5, left=0

        // Create a child entity positioned inside the root
        var child = doc.createEntity();
        child.addComponent(new EntityName("Rectangle"));
        child.addComponent(new ParentComponent(root.getId()));
        child.addComponent(new Position(24, 24));         // relative x=24, y=24
        child.addComponent(new Margin(5.5, 3, 0, 0)); // top=5.5, left=0
        child.addComponent(new BoxSize(300, 90));

        // चरण 2: WHEN (Act)
        // Execute the logic we want to test.
        doc.processSystems();

        // चरण 3: THEN (Assert)
        // Verify that the outcome is what we expect.
        Optional<ComputedPosition> childComputedPosition = child.getComponent(ComputedPosition.class);

        // Assert that the component was actually added

        assertThat(childComputedPosition).isNotNull();

        // Assert the calculated coordinates
        // X = parent_margin_left(0) + parent_padding_left(8) + child_pos_x(24) + child_margin_left(0) = 32.0
        assertThat(childComputedPosition.get().x()).isEqualTo(32.0);

        // Y = parent_margin_top(5) + parent_padding_top(8) + child_pos_y(24) + child_margin_top(5.5) = 42.5
        assertThat(childComputedPosition.get().y()).isEqualTo(42.5);

        // You can also check the parent's position for completeness
        Optional<ComputedPosition> rootComputedPosition = root.getComponent(ComputedPosition.class);
        assertThat(rootComputedPosition).isNotNull();
        assertThat(rootComputedPosition.get().x()).isEqualTo(0.0);
        assertThat(rootComputedPosition.get().y()).isEqualTo(5.0);
    }

    @Test
    void calculateContentBox(){
        LayoutSystem layoutSystem = new LayoutSystem();
        var entity = new Entity();
        entity.addComponent(new EntityName("Box"))
                        .addComponent(new Size( 200,300))
                                .addComponent(new Padding(8, 8, 8, 8));

        Optional<ContentBox> boxSize = layoutSystem.calculateContentBox(entity);
        assertThat(boxSize).isNotNull();
        assertThat(boxSize).isEqualTo(new ContentBox(184,284));
    }
    @Test
    void calculationBoxSize(){
        LayoutSystem layoutSystem = new LayoutSystem();
        var entity = new Entity();
        entity.addComponent(new EntityName("Box"))
                .addComponent(new Size(200,300))
                            .addComponent(new Margin(8, 8, 8, 8));
        Optional<BoxSize> boxSize = layoutSystem.calculateBoxSize(entity);
        assertThat(boxSize.get()).isNotNull();
        assertThat(boxSize.get()).isEqualTo(new BoxSize(216,316));
    }
}