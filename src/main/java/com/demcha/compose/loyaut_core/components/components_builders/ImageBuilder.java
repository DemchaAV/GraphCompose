package com.demcha.compose.loyaut_core.components.components_builders;

import com.demcha.compose.loyaut_core.components.containers.abstract_builders.EmptyBox;
import com.demcha.compose.loyaut_core.components.content.ImageData;
import com.demcha.compose.loyaut_core.components.content.ImageIntrinsicSize;
import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.geometry.ContentSize;
import com.demcha.compose.loyaut_core.components.renderable.ImageComponent;
import com.demcha.compose.loyaut_core.components.style.Padding;
import com.demcha.compose.loyaut_core.core.EntityManager;
import lombok.extern.slf4j.Slf4j;
import java.nio.file.Path;

@Slf4j
public class ImageBuilder extends EmptyBox<ImageBuilder> {
    private Double scaleX;
    private Double scaleY;
    private Double fitWidth;
    private Double fitHeight;

    public ImageBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    public ImageBuilder image(ImageData imageData) {
        return addComponent(imageData);
    }

    public ImageBuilder image(Path path) {
        return image(ImageData.create(path));
    }

    public ImageBuilder image(String path) {
        return image(ImageData.create(path));
    }

    public ImageBuilder image(byte[] bytes) {
        return image(ImageData.create(bytes));
    }

    public ImageBuilder scale(double factor) {
        validatePositive("scale", factor);
        this.scaleX = factor;
        this.scaleY = factor;
        return this;
    }

    public ImageBuilder scaleX(double factor) {
        validatePositive("scaleX", factor);
        this.scaleX = factor;
        return this;
    }

    public ImageBuilder scaleY(double factor) {
        validatePositive("scaleY", factor);
        this.scaleY = factor;
        return this;
    }

    public ImageBuilder fitToBounds(double maxWidth, double maxHeight) {
        validatePositive("fit width", maxWidth);
        validatePositive("fit height", maxHeight);
        this.fitWidth = maxWidth;
        this.fitHeight = maxHeight;
        return this;
    }

    public ImageBuilder fitToPage(Canvas canvas) {
        return fitToBounds(canvas.innerWidth(), canvas.innerHeigh());
    }

    @Override
    public void initialize() {
        entity.addComponent(new ImageComponent());
    }

    @Override
    public Entity build() {
        ImageData imageData = entity.getComponent(ImageData.class).orElseThrow(() ->
                new IllegalStateException("ImageBuilder requires ImageData before build()"));

        ImageIntrinsicSize intrinsicSize = imageData.getMetadata().intrinsicSize();
        entity.addComponent(intrinsicSize);

        if (!entity.has(ContentSize.class)) {
            Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
            entity.addComponent(resolveContentSize(intrinsicSize, padding));
        }

        manager().putEntity(entity);
        return entity;
    }

    private ContentSize resolveContentSize(ImageIntrinsicSize intrinsicSize, Padding padding) {
        double drawableWidth = intrinsicSize.width();
        double drawableHeight = intrinsicSize.height();

        if (fitWidth != null && fitHeight != null) {
            double availableWidth = Math.max(0.0, fitWidth - padding.horizontal());
            double availableHeight = Math.max(0.0, fitHeight - padding.vertical());

            if (availableWidth == 0.0 || availableHeight == 0.0) {
                drawableWidth = 0.0;
                drawableHeight = 0.0;
            } else {
                double scale = Math.min(availableWidth / intrinsicSize.width(), availableHeight / intrinsicSize.height());
                drawableWidth = intrinsicSize.width() * scale;
                drawableHeight = intrinsicSize.height() * scale;
            }
        } else if (scaleX != null || scaleY != null) {
            double resolvedScaleX = scaleX != null ? scaleX : 1.0;
            double resolvedScaleY = scaleY != null ? scaleY : 1.0;
            drawableWidth = intrinsicSize.width() * resolvedScaleX;
            drawableHeight = intrinsicSize.height() * resolvedScaleY;
        }

        return new ContentSize(drawableWidth + padding.horizontal(), drawableHeight + padding.vertical());
    }

    private void validatePositive(String name, double value) {
        if (value <= 0.0 || Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be a finite positive number: " + value);
        }
    }
}
