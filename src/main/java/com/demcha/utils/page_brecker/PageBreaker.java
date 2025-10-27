package com.demcha.utils.page_brecker;

import com.demcha.components.LineTextData;
import com.demcha.components.components_builders.Canvas;
import com.demcha.components.content.text.BlockTextData;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.coordinator.ComputedPosition;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.layout.coordinator.RenderingPosition;
import com.demcha.components.renderable.BlockText;
import com.demcha.components.style.Margin;
import com.demcha.core.EntityManager;
import com.demcha.system.RenderingSystemECS;
import com.demcha.system.SystemECS;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Utility class for sorting graphical entities based on their Y-position
 * and for calculating their resulting page and in-page Y coordinates
 * based on a fixed page height.
 * <p>
 * This class is designed to help determine where entities should fall
 * when dividing a continuous coordinate system into discrete pages.
 */
@Slf4j
public class PageBreaker {
    private PageBreaker() {
    }


    // === Sorting =============================================================

    /**
     * Sorts the given map of entities based on their Y-position in **descending** order.
     * <p>
     * The sorting is based on the {@code RenderingPosition} of each {@link Entity}.
     * The result is a {@link Set} of {@link Map.Entry} objects, preserved in sorted order
     * by being collected into a {@link LinkedHashSet}.
     *
     * @param entities A map where keys are {@link UUID}s and values are {@link Entity} objects
     *                 to be sorted. Must not be null.
     * @return A {@code Set} of map entries sorted by Y-position, descending (top-to-bottom on a page).
     * @throws NullPointerException  if the input map is null.
     * @throws IllegalStateException if any entity does not have a defined {@code RenderingPosition}.
     */
    public static Set<Map.Entry<UUID, Entity>> sortByYPosition(Map<UUID, Entity> entities) {
        Objects.requireNonNull(entities, "entities must not be null");

        return entities.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<UUID, Entity> e) -> yOf(e.getValue()))
                        .reversed())
                .peek(entry -> {
                    Optional<RenderingPosition> pos = RenderingPosition.from(entry.getValue());
                    log.debug("{} -> {}", entry.getValue(), pos.orElse(null));
                })

                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Sorts the given map of entities based on their Y-position in **descending** order
     * and returns the result as a new {@link LinkedHashMap}.
     * <p>
     * This provides a convenient map-based view where the iteration order is the sorted order.
     *
     * @param entities A map where keys are {@link UUID}s and values are {@link Entity} objects
     *                 to be sorted. Must not be null.
     * @return A {@code LinkedHashMap} with the entities, ordered by Y-position, descending.
     * @throws NullPointerException  if the input map is null.
     * @throws IllegalStateException if any entity does not have a defined {@code RenderingPosition}.
     */
    public static LinkedHashMap<UUID, Entity> sortByYPositionToMap(Map<UUID, Entity> entities) {
        Objects.requireNonNull(entities, "entities must not be null");

        return entities.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<UUID, Entity> e) -> yOf(e.getValue()))
                        .reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public static LinkedHashMap<UUID, Entity> sortByYPositionToMap(EntityManager entityManager, Set<UUID> entityUuids) {
        Objects.requireNonNull(entityUuids, "entities must not be null");


        return sortByYPositionToMap(entityManager.getSetEntitiesFromUuids(entityUuids));
    }

    public static LinkedHashMap<UUID, Entity> sortByYPositionToMap(EntityManager entityManager, List<UUID> entityUuids) {
        Objects.requireNonNull(entityUuids, "entities must not be null");


        return sortByYPositionToMap(entityManager.getSetEntitiesFromUuids(new HashSet<>(entityUuids)));
    }

    public static LinkedHashMap<UUID, Entity> sortByYPositionToMap(Set<Entity> entities) {
        Objects.requireNonNull(entities, "entities must not be null");

        return entities.stream()
                .sorted(Comparator.comparingDouble(PageBreaker::yOf).reversed())
                .collect(Collectors.toMap(
                        Entity::getUuid,           // key mapper → UUID
                        e -> e,                  // value mapper → Entity
                        (a, b) -> a,             // merge function (shouldn't happen)
                        LinkedHashMap::new       // preserve order
                ));
    }


    private static double yOf(Entity e) {
        return RenderingPosition.from(e)
                .map(RenderingPosition::y)
                .orElseThrow(() -> new IllegalStateException(
                        "Entity " + e + " has no RenderingPosition"));
    }

    // === Paging math =========================================================

    public static void breakPages(@NonNull Map<UUID, Entity> entities, Canvas canvas) {
        AtomicReference<Double> move = new AtomicReference<>(0.0);
        entities.entrySet().stream()
                .forEach(e -> {
                    var r = RenderingPosition.from(e.getValue()).orElseThrow(
                            () -> new IllegalStateException("Entity " + e + " has no RenderingPosition"));
                    ContentSize contentSize = e.getValue().getComponent(ContentSize.class).orElseThrow(() -> {
                       return new IllegalStateException("Entity " + e + " has no ContentSize");
                    });

                    YPositionOnPage position = definePositionOnPage(r.y(), canvas.boundingTopLine(), contentSize.height(), 0,canvas.margin().orElse(Margin.zero()).top());
                    move.accumulateAndGet(defineMove(position, e.getValue(), canvas), Double::sum);
                    Placement placement = setYInPlacement(e.getValue(), position);
                    e.getValue().addComponent(placement);
                    if (e.getValue().hasAssignable(BlockText.class)) {
                        try {
                            blockTextDataPositions(e.getValue(), canvas);
                            if (log.isDebugEnabled()) {
                                BlockTextData blockTextData = e.getValue().getComponent(BlockTextData.class).orElseThrow();
                                blockTextData.lines().forEach(System.out::println);
                            }
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                });
    }

    private static double defineMove(YPositionOnPage position, Entity entity, Canvas canvas) {
        if (Breakable.class.isAssignableFrom(entity.getPdfRender().getClass())) {
            return 0;
        }
        var margin = entity.getComponent(Margin.class).orElse(Margin.zero());
        var size = entity.getComponent(ContentSize.class).orElseThrow();
        return defineMove(position.yPosition(), size.height(), margin.top(), canvas.boundingBottonLine());
    }

    private static double defineMove(double position, double sizeHeight, double topMargin, double canvasBoundingTopLine) {
        double requireSpace = position + sizeHeight + topMargin;
        if (requireSpace > canvasBoundingTopLine) {
            return requireSpace - canvasBoundingTopLine;
        }
        return 0;
    }


    public static void breakPages(@NonNull EntityManager entityManager) {
        log.info("Breaking pages");
        RenderingSystemECS renderingSystemECS = null;
        log.info("Definition a RenderingSystemECS");
        for (SystemECS system : entityManager.getSystems()) {
            if (RenderingSystemECS.class.isAssignableFrom(system.getClass())) {
                renderingSystemECS = (RenderingSystemECS) system;
                break;
            }

        }
        if (renderingSystemECS == null) {
            log.error("No RenderingSystemECS found");
            throw new IllegalStateException("No RenderingSystemECS found");
        }
        breakPages(entityManager.getEntities(), renderingSystemECS.getCanvas());
    }

    private static Placement setYInPlacement(Entity entity, double yPosition, int startPage, int endPage) {
        ComputedPosition computedPosition = entity.getComponent(ComputedPosition.class).orElseThrow();
        OuterBoxSize outerBoxSize = OuterBoxSize.from(entity).orElseThrow();
        return new Placement(computedPosition.x(), yPosition, outerBoxSize.width(), outerBoxSize.height(), startPage, endPage);
    }

    private static Placement setYInPlacement(Entity entity, YPositionOnPage position) {
        return setYInPlacement(entity, position.yPosition(), position.pageNumber(), position.pageNumber());
    }


    /**
     * Calculates a new Y position on the page and the resulting page number.
     * <p>
     * This method normalizes the {@code currentPositionY} into the range
     * {@code [0, pageHeight)} and calculates the page offset relative to
     * {@code currentPageNumber}.
     *
     * @param currentPositionY  The entity's current absolute Y-coordinate.
     * @param pageHeight        The height of a single page (must be positive and finite).
     * @param currentPageNumber The current page number (e.g., the page the entity is currently thought to be on).
     * @return A {@link YPositionOnPage} object containing the normalized Y-position and the final page number.
     * @throws IllegalArgumentException if {@code pageHeight} is not a finite positive number, or if the
     *                                  resulting page number is less than zero.
     */
    public static YPositionOnPage definePositionOnPage(double currentPositionY, double pageHeight, double objectHeight, int currentPageNumber, double topMargin) {
        if (!(pageHeight > 0.0) || Double.isNaN(pageHeight) || Double.isInfinite(pageHeight)) {
            throw new IllegalArgumentException("pageHeight must be a finite positive number; was " + pageHeight);
        }

        log.debug("Input: y={}, pageHeight={}, currentPage={}", currentPositionY, pageHeight, currentPageNumber);

        int offset = definePage(currentPositionY, pageHeight); // may be negative, zero, or positive
        int finalPage = Math.addExact(currentPageNumber, offset); // detect overflow
        if (finalPage < 0) {
            log.error("Invalid page number {}", finalPage);
            throw new IllegalArgumentException("Page number is less than zero after offset: " + finalPage);
        }


        // Normalize y into [0, pageHeight)
        double yInPage = positiveModulo(currentPositionY, pageHeight, topMargin);

        YPositionOnPage result = new YPositionOnPage(yInPage, finalPage);

        AtomicReference<Double> move = new AtomicReference<>(0.0);
        move.accumulateAndGet(defineMove(result.yPosition(), objectHeight, topMargin, pageHeight), Double::sum);

        log.debug("Defined position on page: {}", result);
        return result;
    }

    private static LocatedPages locatePages(Entity entity, YPositionOnPage position, Canvas canvas) {
        int startPage = 0;
        int endPage = 0;


        return new LocatedPages(startPage, endPage);
    }

    /**
     * Calculates the page offset relative to a starting page.
     * <p>
     * Given an absolute Y-coordinate and a fixed {@code pageHeight} ($H$), this
     * determines how many pages away the coordinate is from the page that
     * starts at Y=0. The resulting offset is defined such that:
     * <ul>
     * <li>$Y$ in $[0, H)$ $\rightarrow$ 0 (Same page)</li>
     * <li>$Y$ in $[H, 2H)$ $\rightarrow$ -1 (One page down, assuming a coordinate system where positive Y moves down)</li>
     * <li>$Y$ in $[-H, 0)$ $\rightarrow$ 1 (One page up)</li>
     * </ul>
     * The calculation is $- \lfloor \frac{Y}{H} \rfloor$.
     *
     * @param currentPositionY The entity's current absolute Y-coordinate.
     * @param pageHeight       The height of a single page (must be positive and finite).
     * @return The integer page offset.
     * @throws IllegalArgumentException if {@code pageHeight} is not a finite positive number.
     */
    public static int definePage(double currentPositionY, double pageHeight) {
        if (!(pageHeight > 0.0) || Double.isNaN(pageHeight) || Double.isInfinite(pageHeight)) {
            throw new IllegalArgumentException("pageHeight must be a finite positive number; was " + pageHeight);
        }
        log.debug("definePage: pageHeight={}, y={}", pageHeight, currentPositionY);

        // floor(y / H) is ... -2, -1, 0, 1, 2 ...
        // We want offset = -floor(y/H)
        int definedPage = (int) Math.floor(currentPositionY / pageHeight) * -1;

        log.debug("definePage -> offset={}", definedPage);
        return definedPage;
    }

    /**
     * Proper positive modulo for doubles: result in [0, m).
     */
    private static double positiveModulo(double a, double m, double topMargin) {
        double r = a % m;
        return (r < 0) ? r + m + topMargin : r;
    }

    private static BlockText.ValidatedTextData getValidatedTextData(Entity e) {
        var textValueOpt = e.getComponent(BlockTextData.class);
        var styleOpt = e.getComponent(TextStyle.class);

        BlockTextData textValue;
        TextStyle style;
        if (textValueOpt.isEmpty()) {
            log.info("TextComponent has no BlockTextData; skipping: {}", e);
            textValue = textValueOpt.orElse(BlockTextData.empty());
        } else {
            textValue = textValueOpt.get();
        }
        if (styleOpt.isEmpty()) {
            log.info("TextComponent has no TextStyle; skipping: {}", e);
            style = styleOpt.orElse(TextStyle.defaultStyle());
        } else {
            style = styleOpt.get();
        }

        return new BlockText.ValidatedTextData(style, textValue);
    }

    private static boolean blockTextDataPositions(Entity e, Canvas canvas) throws IOException {


        if (!e.hasAssignable(BlockTextData.class)) {
            log.debug("Entity doesn't have TextComponent; skipping: {}", e);
            return false;
        }

        var positionOpt = RenderingPosition.from(e);
        if (positionOpt.isEmpty()) {
            log.warn("TextComponent has no RenderingPosition; skipping: {}", e);
            return false;
        }
        var position = positionOpt.get();
        InnerBoxSize innerBoxSize = InnerBoxSize.from(e).orElseThrow();

        BlockText.ValidatedTextData validateText = getValidatedTextData(e);


        var style = validateText.style();
        float fontSize = (float) style.size();
        PDFont font = style.font();
        var textHeight = (float) style.getLineHeight();

        float scale = fontSize / 1000f;
        PDFontDescriptor fd = font.getFontDescriptor();

        float descentPx = Math.abs((fd != null ? fd.getDescent() : font.getBoundingBox().getLowerLeftY()) * scale);

        var blockTextData = validateText.textValue().lines();

        double spacing = e.getComponent(Align.class).orElseGet(() -> {
            log.warn("TextComponent has no Align; using default: {}", e);
            return Align.defaultAlign(2);
        }).spacing() * -1;

        log.debug("Rendering textBlock '{}' at position ({}, {}) fontSize={}  textStyle= ", blockTextData, position.x(), position.y());
        log.debug("fontSize={}  textStyle= {}", fontSize, style);

        int currentPage = 0;
        //TODO надо пеересмотреть нужно ли брать страницу с  с контейнера
//        int currentPage = e.getComponent(Placement.class).orElseThrow().startPage();


        // стартовая позиция (левый верх «абзаца»)
        float startX = (float) position.x() - descentPx;
        float startY = (float) (position.y() + innerBoxSize.hight()) - textHeight + descentPx; // if spacing will be negative

        boolean isStarted = false;
        BlockTextData newBlockTextData;
        List<LineTextData> assignPositionTextData = new ArrayList<>();

        for (LineTextData ltd : blockTextData) {
            if (!isStarted) {
                log.debug("Started print a block text, Position Y is {}", startY);
                isStarted = true;
            }

            float currenPosition = (float) ltd.x() + startX;

            LineTextData nltd = new LineTextData(ltd, currenPosition, startY, currentPage);
            assignPositionTextData.add(nltd);
            startY -= (float) (textHeight - spacing);


            YPositionOnPage yPositionOnPage = PageBreaker
                    .definePositionOnPage(startY, canvas.boundingTopLine(),textHeight, currentPage, canvas.margin().orElse(Margin.zero()).top());
            if (log.isDebugEnabled()) {
                log.debug(ltd.toString());
                log.debug("Line position {}, Current page: {}", ltd.x(), currentPage);
                log.debug(yPositionOnPage.toString());
            }


            startY = (float) yPositionOnPage.yPosition();
            if (currentPage != yPositionOnPage.pageNumber()) {
                currentPage = yPositionOnPage.pageNumber();

            }
        }
        newBlockTextData = new BlockTextData(assignPositionTextData, (float) spacing);
        e.addComponent(newBlockTextData);


        return true;
    }

    /**
     * Placeholder method for the main page-breaking logic.
     * <p>
     * **TODO:** The implementation should likely involve sorting entities
     * (using {@link #sortByYPositionToMap(Map)}) and then iterating through
     * them to calculate and potentially adjust their page and in-page
     * coordinates (using {@link #defineMove(double, double, double, double)} .
     */
    public void breakPages() {
        // TODO: implement page breaking using the helpers above
    }

    private static class LocatedPages {
        private int startPage;
        private int endPage;

        public LocatedPages(int startPage, int endPage) {
            this.startPage = startPage;
            this.endPage = endPage;
        }
    }
}
