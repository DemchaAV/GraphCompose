package com.demcha.legacy.scene;

import com.demcha.legacy.core.Element;
import com.demcha.components.containers.abstract_builders.Container;
import com.demcha.legacy.layout.Layout;
import lombok.Getter;
import lombok.Setter;
import org.apache.pdfbox.pdmodel.PDPage;

import java.util.ArrayList;
import java.util.List;

/**
 * (0,0)                  (595,0)
 * ┌───────────────────────→ X
 * │
 * │
 * │
 * │
 * ↓
 * Y
 * (0,842) — верхняя левая точка
 */

@Setter
@Getter
//@RequiredArgsConstructor
public class Page implements Container {
    private final Element self = new Element();
    private final List<Element> children = new ArrayList<>();

    private final PDPage page;

    // Маргины страницы (в pt)
    private double marginTop = 36;
    private double marginRight = 36;
    private double marginBottom = 36;
    private double marginLeft = 36;

    private Layout layout;

    public Page(PDPage page) {
        this.page = page;
    }

    @Override
    public List<Element> getChildren() {
        return children;
    }

    @Override
    public Layout getLayout() {
        return layout;
    }

    @Override
    public void setLayout(Layout layout) {
        this.layout = layout;
        // ВАЖНО: только назначаем лейаут; измерение/расстановку лучше делать централизованно в движке,
        // чтобы порядок measure→arrange был единый для всего дерева.
    }

    @Override
    public Element getElement() {
        return self;
    }

    /**
     * Удобные геттеры размеров страницы
     */
    public double pageWidth() {
        return page.getMediaBox().getWidth();
    }

    public double pageHeight() {
        return page.getMediaBox().getHeight();
    }

    /**
     * Ширина/высота области контента с учётом маргинов
     */
    public double contentWidth() {
        return pageWidth() - marginLeft - marginRight;
    }

    public double contentHeight() {
        return pageHeight() - marginTop - marginBottom;
    }

    /**
     * Стартовая точка для Arrange (top-left логика)
     */
    public double startX() {
        return marginLeft;
    }

    public double startY() {
        return pageHeight() - marginTop;
    }
}


