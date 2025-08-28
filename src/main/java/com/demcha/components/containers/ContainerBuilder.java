package com.demcha.components.containers;

import com.demcha.components.containers.abstract_builders.Container;
import com.demcha.components.containers.abstract_builders.EmptyBox;
import com.demcha.components.containers.abstract_builders.EntityCreator;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.ParentComponent;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.style.Padding;
import com.demcha.core.PdfDocument;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j

public class ContainerBuilder extends EmptyBox<ContainerBuilder> implements EntityCreator<ContainerBuilder> {

    private final PdfDocument document;
    private final Axis axis;
    private final Set<Entity> children = new LinkedHashSet<>();
    private Align align = Align.middle(5);          // ваш дефолт
    private double spacing = 0.0;                   // расстояние между элементами
    private Entity container;                       // сущность контейнера
    private ParentComponent parent;                 // опционально
    private Anchor anchor = Anchor.center();        // по желанию
    private Position position = Position.zero();    // по желанию
    private Padding padding = Padding.zero();       // padding контейнера
    // Если хотите разные компоненты рендера:
    private boolean useSpecificComponent = false; // false => общий Container

    private ContainerBuilder(PdfDocument document, Axis axis) {
        super(document);
        this.document = document;
        this.axis = axis;
    }
    // или true => HContainer/VContainer в зависимости от оси

    public static ContainerBuilder horizontal(PdfDocument document) {
        return new ContainerBuilder(document, Axis.HORIZONTAL);
    }

    public static ContainerBuilder vertical(PdfDocument document) {
        return new ContainerBuilder(document, Axis.VERTICAL);
    }

    public enum Axis {HORIZONTAL, VERTICAL}

    public ContainerBuilder add(Entity child) {
        if (child != null) children.add(child);
        return this;
    }

    public ContainerBuilder useSpecificComponent(boolean flag) {
        this.useSpecificComponent = flag;
        return this;
    }

    // Создаём саму сущность контейнера (Entity)
    @Override
    public ContainerBuilder create() {
        this.container = new Entity();
        // Навешиваем базовые компоненты на контейнер:
        this.container.addComponent(padding);
        this.container.addComponent(anchor);
        this.container.addComponent(position);
        if (parent != null) this.container.addComponent(parent);

        // Тут можно добавить имя, слой, и т.д. по вашему шаблону
        return this;
    }

    public Entity build() {
        if (this.container == null) {
            create();
        }

        // Проставляем позиции детям и считаем итоговый размер контейнера
        double cursor = 0.0;
        double primaryExtent = 0.0; // по оси (W для H, H для V)
        double crossExtent = 0.0;   // перпендикулярная ось (H для V, W для H)

        for (Entity child : children) {
            OuterBoxSize sz = OuterBoxSize.from(child)
                    .orElseThrow(() -> new IllegalStateException("Child has no OuterBoxSize"));

            // Выравнивание по «перпендикулярной» оси:
            double crossPos = computeCrossPosition(sz, crossExtent, padding);

            // Позиционирование ребёнка:
            switch (axis) {
                case HORIZONTAL -> {
                    // child.x = padding.left + cursor
                    // child.y = padding.top + crossPos
                    child.addComponent(Position.of(
                            position.x() + padding.left() + cursor,
                            position.y() + padding.top() + crossPos
                    ));
                    cursor += sz.width();
                    primaryExtent = cursor;
                    crossExtent = Math.max(crossExtent, sz.height());
                }
                case VERTICAL -> {
                    // child.x = padding.left + crossPos
                    // child.y = padding.top + cursor
                    child.addComponent(Position.of(
                            position.x() + padding.left() + crossPos,
                            position.y() + padding.top() + cursor
                    ));
                    cursor += sz.height();
                    primaryExtent = cursor;
                    crossExtent = Math.max(crossExtent, sz.width());
                }
            }

            // Пробел между элементами (кроме последнего)
            cursor += spacing;
        }

        // Убираем лишний spacing в конце
        if (!children.isEmpty()) {
            cursor -= spacing;
            primaryExtent -= spacing;
        }

        // Итоговый размер контейнера:
        double width, height;
        if (axis == Axis.HORIZONTAL) {
            width  = primaryExtent + padding.horizontal();
            height = crossExtent   + padding.vertical();
        } else {
            width  = crossExtent   + padding.horizontal();
            height = primaryExtent + padding.vertical();
        }

        this.container.addComponent(new ContentSize(width, height));

        // Компонент отрисовки
        if (useSpecificComponent) {
            if (axis == Axis.HORIZONTAL) {
                this.container.addComponent(new HContainer()); // ваш существующий рендер
            } else {
                this.container.addComponent(new VContainer());
            }
        } else {
            this.container.addComponent(new Container());      // общий компонент рендера (если сделаете)
        }

        // Регистрируем сущности
        document.putEntity(this.container);
        for (Entity child : children) {
            document.putEntity(child);
        }
        return this.container;
    }

    // Пример «умного» вычисления кросс-позиции (для Align)
    private double computeCrossPosition(OuterBoxSize sz, double currentCrossExtent, Padding padding) {
        // Align можно использовать, чтобы центрировать или прижимать к началу/концу
        // Ниже — эскиз, подправьте под свою модель Align:
        return switch (align.type()) {
            case START   -> 0.0;
            case CENTER  -> Math.max(0, (currentCrossExtent - (axis == Axis.HORIZONTAL ? sz.height() : sz.width())) / 2.0);
            case END     -> Math.max(0, currentCrossExtent - (axis == Axis.HORIZONTAL ? sz.height() : sz.width()));
            case CUSTOM  -> Math.max(0, align.offset()); // для Align.middle(5) и т.п.
        };
    }

    @Override
    protected ContainerBuilder self() {
        return this;
    }
}
}
