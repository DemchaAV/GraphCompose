package com.demcha.compose.engine.pagination;

import com.demcha.compose.engine.components.core.Component;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.geometry.InnerBoxSize;
import com.demcha.compose.engine.components.geometry.OuterBoxSize;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.exceptions.ContentSizeNotFoundException;
import com.demcha.compose.engine.components.geometry.Expendable;
import com.demcha.compose.engine.layout.container.ContainerExpander;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link ContainerExpander} class.
 * This class uses Mockito for mocking dependencies and AssertJ for fluent assertions.
 * It assumes that InnerBoxSize and OuterBoxSize are retrieved via static .from() methods.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContainerExpander Tests")
class ContainerExpanderTest {

    // A small value for floating-point comparisons.
    private static final double EPSILON = 1e-6;

    /** A concrete, named class for testing the Expendable marker interface. */
    private static class TestExpendable implements Expendable, Component {}

    /**
     * Helper method to create a child entity.
     * The OuterBoxSize is not added directly but should be mocked in tests.
     *
     * @return A new Entity representing a child.
     */
    private Entity createChild() {
        Entity child = new Entity();
        child.addComponent(Margin.zero()); // Add default margin
        return child;
    }

    @Nested
    @DisplayName("expandContentSizeByChildren() Method")
    class ExpandContentSizeByChildrenTests {

        private Entity parent;
        private MockedStatic<InnerBoxSize> mockedInnerBoxSize;
        private MockedStatic<OuterBoxSize> mockedOuterBoxSize;


        @BeforeEach
        void setUp() {
            // Mock static .from() methods before each test
            mockedInnerBoxSize = mockStatic(InnerBoxSize.class);
            mockedOuterBoxSize = mockStatic(OuterBoxSize.class);

            parent = new Entity();
            parent.addComponent(new ContentSize(100, 80));

            // Configure the mock for the parent's InnerBoxSize
            mockedInnerBoxSize.when(() -> InnerBoxSize.from(eq(parent)))
                    .thenReturn(Optional.of(new InnerBoxSize(100, 80)));
        }

        @AfterEach
        void tearDown() {
            // Close static mocks to prevent test pollution
            mockedInnerBoxSize.close();
            mockedOuterBoxSize.close();
        }

        @Test
        @DisplayName("Should throw NullPointerException if parent is null")
        void expandContentSize_withNullParent_shouldThrowException() {
            assertThrows(NullPointerException.class,
                    () -> ContainerExpander.expandContentSizeByChildren(null, Collections.emptySet()),
                    "parent cannot be null");
        }

        @Test
        @DisplayName("Should throw NullPointerException if children set is null")
        void expandContentSize_withNullChildren_shouldThrowException() {
            assertThrows(NullPointerException.class,
                    () -> ContainerExpander.expandContentSizeByChildren(parent, null),
                    "children cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalStateException if parent has no InnerBoxSize")
        void expandContentSize_parentMissingInnerBoxSize_shouldThrowException() {
            Entity parentWithoutInnerBox = new Entity();
            // Mock InnerBoxSize.from() to return empty for this specific entity
            mockedInnerBoxSize.when(() -> InnerBoxSize.from(eq(parentWithoutInnerBox))).thenReturn(Optional.empty());

            Set<Entity> children = Set.of(createChild());

            Exception exception = assertThrows(IllegalStateException.class,
                    () -> ContainerExpander.expandContentSizeByChildren(parentWithoutInnerBox, children));

            assertThat(exception.getMessage()).contains("Parent is missing InnerBoxSize");
        }

        @Test
        @DisplayName("Should throw ContentSizeNotFoundException if parent has no ContentSize")
        void expandContentSize_parentMissingContentSize_shouldThrowException() {
            Entity parentWithoutContentSize = new Entity();
            // Configure mock for its InnerBoxSize
            mockedInnerBoxSize.when(() -> InnerBoxSize.from(eq(parentWithoutContentSize)))
                    .thenReturn(Optional.of(new InnerBoxSize(50, 50)));

            Entity child = createChild();
            mockedOuterBoxSize.when(() -> OuterBoxSize.from(eq(child)))
                    .thenReturn(Optional.of(new OuterBoxSize(100, 100)));

            Set<Entity> children = Set.of(child); // Child is larger

            assertThrows(ContentSizeNotFoundException.class,
                    () -> ContainerExpander.expandContentSizeByChildren(parentWithoutContentSize, children));
        }

        @Test
        @DisplayName("Should return false and not expand for an empty set of children")
        void expandContentSize_withEmptyChildren_shouldReturnFalse() {
            boolean result = ContainerExpander.expandContentSizeByChildren(parent, Collections.emptySet());

            assertFalse(result);
            ContentSize finalSize = parent.getComponent(ContentSize.class).orElseThrow();
            assertThat(finalSize.width()).isEqualTo(100);
            assertThat(finalSize.height()).isEqualTo(80);
        }

        @Test
        @DisplayName("Should not expand if children fit within the parent's inner box")
        void expandContentSize_whenChildrenFit_shouldNotExpand() {
            Entity child1 = createChild();
            Entity child2 = createChild();
            mockedOuterBoxSize.when(() -> OuterBoxSize.from(eq(child1))).thenReturn(Optional.of(new OuterBoxSize(50, 40)));
            mockedOuterBoxSize.when(() -> OuterBoxSize.from(eq(child2))).thenReturn(Optional.of(new OuterBoxSize(99, 79)));

            Set<Entity> children = Set.of(child1, child2);

            boolean expanded = ContainerExpander.expandContentSizeByChildren(parent, children);

            assertFalse(expanded, "Should not expand as children fit");
            ContentSize finalSize = parent.getComponent(ContentSize.class).orElseThrow();
            assertThat(finalSize.width()).isEqualTo(100);
            assertThat(finalSize.height()).isEqualTo(80);
        }

        @Test
        @DisplayName("Should expand width when a child is wider")
        void expandContentSize_whenChildIsWider_shouldExpandWidth() {
            Entity child = createChild();
            mockedOuterBoxSize.when(() -> OuterBoxSize.from(eq(child)))
                    .thenReturn(Optional.of(new OuterBoxSize(120, 70)));
            Set<Entity> children = Set.of(child);

            boolean expanded = ContainerExpander.expandContentSizeByChildren(parent, children);

            assertTrue(expanded, "Should expand because a child is wider");
            ContentSize finalSize = parent.getComponent(ContentSize.class).orElseThrow();
            assertThat(finalSize.width()).isCloseTo(120, within(EPSILON));
            assertThat(finalSize.height()).isCloseTo(80, within(EPSILON));
        }

        @Test
        @DisplayName("Should expand height when a child is taller")
        void expandContentSize_whenChildIsTaller_shouldExpandHeight() {
            Entity child = createChild();
            mockedOuterBoxSize.when(() -> OuterBoxSize.from(eq(child)))
                    .thenReturn(Optional.of(new OuterBoxSize(90, 100)));
            Set<Entity> children = Set.of(child);

            boolean expanded = ContainerExpander.expandContentSizeByChildren(parent, children);

            assertTrue(expanded, "Should expand because a child is taller");
            ContentSize finalSize = parent.getComponent(ContentSize.class).orElseThrow();
            assertThat(finalSize.width()).isCloseTo(100, within(EPSILON));
            assertThat(finalSize.height()).isCloseTo(100, within(EPSILON));
        }

        @Test
        @DisplayName("Should expand both width and height when a child is larger in both dimensions")
        void expandContentSize_whenChildIsLarger_shouldExpandBothDimensions() {
            Entity child = createChild();
            mockedOuterBoxSize.when(() -> OuterBoxSize.from(eq(child)))
                    .thenReturn(Optional.of(new OuterBoxSize(150, 120)));
            Set<Entity> children = Set.of(child);

            boolean expanded = ContainerExpander.expandContentSizeByChildren(parent, children);

            assertTrue(expanded, "Should expand because a child is larger");
            ContentSize finalSize = parent.getComponent(ContentSize.class).orElseThrow();
            assertThat(finalSize.width()).isCloseTo(150, within(EPSILON));
            assertThat(finalSize.height()).isCloseTo(120, within(EPSILON));
        }

        @Test
        @DisplayName("Should expand to max width and height from different children")
        void expandContentSize_withMultipleChildren_shouldExpandToMaxDimensions() {
            Entity widestChild = createChild();
            Entity tallestChild = createChild();
            Entity smallestChild = createChild();

            mockedOuterBoxSize.when(() -> OuterBoxSize.from(eq(widestChild))).thenReturn(Optional.of(new OuterBoxSize(140, 70)));
            mockedOuterBoxSize.when(() -> OuterBoxSize.from(eq(tallestChild))).thenReturn(Optional.of(new OuterBoxSize(90, 110)));
            mockedOuterBoxSize.when(() -> OuterBoxSize.from(eq(smallestChild))).thenReturn(Optional.of(new OuterBoxSize(50, 50)));

            Set<Entity> children = Set.of(widestChild, tallestChild, smallestChild);

            boolean expanded = ContainerExpander.expandContentSizeByChildren(parent, children);

            assertTrue(expanded, "Should expand to max dimensions from different children");
            ContentSize finalSize = parent.getComponent(ContentSize.class).orElseThrow();
            assertThat(finalSize.width()).isCloseTo(140, within(EPSILON));
            assertThat(finalSize.height()).isCloseTo(110, within(EPSILON));
        }

        @Test
        @DisplayName("Should ignore children without an OuterBoxSize component")
        void expandContentSize_shouldIgnoreChildWithoutOuterBoxSize() {
            Entity childWithoutSize = createChild();
            Entity childWithSize = createChild();

            mockedOuterBoxSize.when(() -> OuterBoxSize.from(eq(childWithoutSize))).thenReturn(Optional.empty());
            mockedOuterBoxSize.when(() -> OuterBoxSize.from(eq(childWithSize))).thenReturn(Optional.of(new OuterBoxSize(50, 50)));

            Set<Entity> children = Set.of(childWithoutSize, childWithSize);

            boolean expanded = ContainerExpander.expandContentSizeByChildren(parent, children);

            assertFalse(expanded, "Should not expand as the only sized child fits");
        }
    }

    @Nested
    @DisplayName("process() Method")
    class ProcessMethodTests {

        @Mock
        private EntityManager entityManager;
        private MockedStatic<InnerBoxSize> mockedInnerBoxSize;
        private MockedStatic<OuterBoxSize> mockedOuterBoxSize;

        private UUID parentUuid;
        private Entity parentEntity;
        private Map<UUID, Set<UUID>> childrenByParents;

        @BeforeEach
        void setUp() {
            mockedInnerBoxSize = mockStatic(InnerBoxSize.class);
            mockedOuterBoxSize = mockStatic(OuterBoxSize.class);

            parentEntity = new Entity();
            // Assuming Entity has getUuid() to retrieve the auto-generated UUID
            parentUuid = parentEntity.getUuid();

            parentEntity.addComponent(new ContentSize(100, 100));
            mockedInnerBoxSize.when(() -> InnerBoxSize.from(eq(parentEntity)))
                    .thenReturn(Optional.of(new InnerBoxSize(100, 100)));

            childrenByParents = new LinkedHashMap<>(); // Use LinkedHashMap for predictable iteration
        }

        @AfterEach
        void tearDown() {
            mockedInnerBoxSize.close();
            mockedOuterBoxSize.close();
        }

        @Test
        @DisplayName("Should skip processing if parent entity is not found")
        void process_whenParentNotFound_shouldSkip() {
            when(entityManager.getEntity(parentUuid)).thenReturn(Optional.empty());
            childrenByParents.put(parentUuid, Collections.emptySet());

            ContainerExpander.process(childrenByParents, entityManager);

            verify(entityManager, never()).getSetEntitiesFromUuids(any());
        }

        @Test
        @DisplayName("Should skip parent that is not 'Expendable'")
        void process_whenParentIsNotExpendable_shouldSkip() {
            when(entityManager.getEntity(parentUuid)).thenReturn(Optional.of(parentEntity));
            childrenByParents.put(parentUuid, Collections.emptySet());

            ContainerExpander.process(childrenByParents, entityManager);

            verify(entityManager, never()).getSetEntitiesFromUuids(any());
            ContentSize finalSize = parentEntity.getComponent(ContentSize.class).orElseThrow();
            assertThat(finalSize.width()).as("Size should not change").isEqualTo(100);
        }

        @Test
        @DisplayName("Should process and expand an 'Expendable' parent")
        void process_withExpendableParent_shouldExpand() {
            // FIX: Added the Expendable component to ensure the parent is processed.
            parentEntity.addComponent(new TestExpendable());

            Entity childEntity1 = createChild();
            UUID childUuid1 = childEntity1.getUuid();
            Set<UUID> childUuids = Set.of(childUuid1);
            Set<Entity> childEntities = Set.of(childEntity1);

            // FIX: Used the parent's UUID as the key, not the child's.
            childrenByParents.put(parentUuid, childUuids);

            when(entityManager.getEntity(parentUuid)).thenReturn(Optional.of(parentEntity));
            when(entityManager.getSetEntitiesFromUuids(childUuids)).thenReturn(childEntities);
            mockedOuterBoxSize.when(() -> OuterBoxSize.from(eq(childEntity1)))
                    .thenReturn(Optional.of(new OuterBoxSize(150, 80)));

            ContainerExpander.process(childrenByParents, entityManager);

            ContentSize finalSize = parentEntity.getComponent(ContentSize.class).orElseThrow();
            assertThat(finalSize.width()).isCloseTo(150, within(EPSILON));
            assertThat(finalSize.height()).isCloseTo(100, within(EPSILON));
        }

        @Test
        @DisplayName("Should handle multiple parents correctly")
        void process_withMultipleParents_shouldProcessAll() {
            // --- Parent 1 (Expendable, should expand) ---
            // FIX: Added the Expendable component.
            parentEntity.addComponent(new TestExpendable());
            Entity childEntity1 = createChild();
            UUID childUuid1 = childEntity1.getUuid();
            childrenByParents.put(parentUuid, Set.of(childUuid1));
            when(entityManager.getEntity(parentUuid)).thenReturn(Optional.of(parentEntity));
            when(entityManager.getSetEntitiesFromUuids(Set.of(childUuid1))).thenReturn(Set.of(childEntity1));
            mockedOuterBoxSize.when(() -> OuterBoxSize.from(eq(childEntity1)))
                    .thenReturn(Optional.of(new OuterBoxSize(120, 90)));

            // --- Parent 2 (Not Expendable, should be skipped) ---
            Entity parent2Entity = new Entity();
            UUID parent2Uuid = parent2Entity.getUuid();
            parent2Entity.addComponent(new ContentSize(50, 50));
            mockedInnerBoxSize.when(() -> InnerBoxSize.from(eq(parent2Entity)))
                    .thenReturn(Optional.of(new InnerBoxSize(50, 50)));
            childrenByParents.put(parent2Uuid, Collections.emptySet());
            when(entityManager.getEntity(parent2Uuid)).thenReturn(Optional.of(parent2Entity));

            // --- Parent 3 (Expendable, but children fit, should not expand) ---
            Entity parent3Entity = new Entity();
            UUID parent3Uuid = parent3Entity.getUuid();
            // FIX: Added the Expendable component.
            parent3Entity.addComponent(new TestExpendable());
            parent3Entity.addComponent(new ContentSize(200, 200));
            mockedInnerBoxSize.when(() -> InnerBoxSize.from(eq(parent3Entity)))
                    .thenReturn(Optional.of(new InnerBoxSize(200, 200)));
            UUID childUuid3 = UUID.randomUUID();
            Entity childEntity3 = createChild();
            childrenByParents.put(parent3Uuid, Set.of(childUuid3));
            when(entityManager.getEntity(parent3Uuid)).thenReturn(Optional.of(parent3Entity));
            when(entityManager.getSetEntitiesFromUuids(Set.of(childUuid3))).thenReturn(Set.of(childEntity3));
            mockedOuterBoxSize.when(() -> OuterBoxSize.from(eq(childEntity3)))
                    .thenReturn(Optional.of(new OuterBoxSize(10, 10)));


            // Execute
            ContainerExpander.process(childrenByParents, entityManager);

            // Verify Parent 1 expanded
            ContentSize finalSize1 = parentEntity.getComponent(ContentSize.class).orElseThrow();
            assertThat(finalSize1.width()).isCloseTo(120, within(EPSILON));

            // Verify Parent 2 did not change
            ContentSize finalSize2 = parent2Entity.getComponent(ContentSize.class).orElseThrow();
            assertThat(finalSize2.width()).isEqualTo(50);

            // Verify Parent 3 did not change
            ContentSize finalSize3 = parent3Entity.getComponent(ContentSize.class).orElseThrow();
            assertThat(finalSize3.width()).isEqualTo(200);
        }
    }
}
